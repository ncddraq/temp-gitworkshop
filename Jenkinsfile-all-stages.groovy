library identifier: 'neom-jenkins-sharedlib-helm-charts_nix@master', retriever: modernSCM([$class: 'GitSCMSource',
remote: 'https://github.com/NEOM-KSA/neom-jenkins-sharedlib-helm-charts_nix.git',
credentialsId: 'github_token_ak'])

library identifier: 'neom-jenkins-sharedlib-trufflehog_nix@master', retriever: modernSCM([$class: 'GitSCMSource',
   remote: 'https://github.com/NEOM-KSA/neom-jenkins-sharedlib-trufflehog_nix.git',
   credentialsId: 'github_token_ak'])

library identifier: 'neom-jenkins-sharedlib-sonarQube_multi@master', retriever: modernSCM([$class: 'GitSCMSource',
remote: 'https://github.com/NEOM-KSA/neom-jenkins-sharedlib-sonarQube_multi.git',
credentialsId: 'github_token_ak'])

library identifier: 'neom-jenkins-sharedlib-dockerbuild@feature/ocir-change', retriever: modernSCM([$class: 'GitSCMSource',
   remote: 'https://github.com/NEOM-KSA/neom-jenkins-sharedlib-dockerbuild.git',
   credentialsId: 'github_token_ak'])

library identifier: 'neom-jenkins-sharedlib-changelog@master', retriever: modernSCM([$class: 'GitSCMSource',
   remote: 'https://github.com/NEOM-KSA/neom-jenkins-sharedlib-changelog.git',
   credentialsId: 'github_token_ak'])

library identifier: 'neom-jenkins-sharedlib-owasp-zap-scan-jenkins-sharedlib@master', retriever: modernSCM([$class: 'GitSCMSource',
remote: 'https://github.com/NEOM-KSA/neom-jenkins-sharedlib-owasp-zap-scan-jenkins-sharedlib.git',
credentialsId: 'github_token_ak'])

library identifier: 'neom-jenkins-sharedlib-terraform@feature/oci-tf-shared-lib', retriever: modernSCM([$class: 'GitSCMSource',
   remote: 'https://github.com/NEOM-KSA/neom-jenkins-sharedlib-terraform.git',
   credentialsId: 'github_token_ak'])

library identifier: 'neom-jenkins-sharedlib-release-lifecycle@master', retriever: modernSCM([$class: 'GitSCMSource',
remote: 'https://github.com/NEOM-KSA/neom-jenkins-sharedlib-release-lifecycle.git',
credentialsId: 'github_token_ak'])

pipeline {
  agent {
    kubernetes {
      yaml '''
        apiVersion: v1
        kind: Pod
        spec:
          containers:
          - name: podman
            imagePullPolicy: IfNotPresent
            image: anilkintala123/podman-trivy-oci:latest
            command:
            - cat
            tty: true
            securityContext:
              privileged: true
            volumeMounts:
            - mountPath: /var/lib/containers
              name: podman-volume
            - mountPath: /dev/shm
              name: devshm-volume
            - mountPath: /var/run
              name: varrun-volume
            - mountPath: /tmp
              name: tmp-volume
          - name: trufflehog
            imagePullPolicy: IfNotPresent
            image: anilkintala123/trufflehog:latest
            command:
            - cat
            tty: true
          - name: ubuntu
            imagePullPolicy: IfNotPresent
            image: anilkintala123/ubuntu:latest
            command:
            - cat
            tty: true
          - name: git
            imagePullPolicy: IfNotPresent
            image: anilkintala123/git:latest
            command:
            - cat
            tty: true
          - name: build
            imagePullPolicy: IfNotPresent
            image: gradle:latest
            command:
            - cat
            tty: true
          - name: helm
            imagePullPolicy: IfNotPresent
            image: anilkintala123/helm-oci:latest
            command:
            - cat
            tty: true
          - name: owasp-zap
            imagePullPolicy: IfNotPresent
            image: anilkintala123/zap:0.1.0
            command:
            - cat
            tty: true
          - name: changelog
            imagePullPolicy: IfNotPresent
            image: anilkintala123/changlog:0.1.0
            command:
            - cat
            tty: true
          - name: mysql
            imagePullPolicy: IfNotPresent
            image: anilkintala123/mysql:latest
            command:
            - cat
            tty: true
          restartPolicy: Never
          volumes:
          - name: podman-volume
            emptyDir: {}
          - emptyDir:
            medium: Memory
            name: devshm-volume
          - emptyDir: {}
            name: varrun-volume
          - emptyDir: {}
            name: tmp-volume
          imagePullSecrets:
          - name: default-secret
        '''
    }
  }
  environment{
    userpat= credentials('github_token_ak')
    //nexus_username = credentials('nexus_sbx_username')
    //nexus_password = credentials('nexus_sbx_password')
    // DB_USERNAME = credentials('db_username')
    // DB_PASS = credentials('db_pass')
    // DB_HOST=credentials('db_host')
    // REDIS_PASSWORD= credentials('redis-password')

    def environmentName = 'bld'

    def projectName = 'hello-world-spring-boot'
    def releaseName = 'hello-world-spring-boot'
    def namespace = "hello-world-spring-boot"
    def imagetype = "neompay"
    def compartment_id='ocid1.compartment.oc1..aaaaaaaa45cmrlocnau2lkvm7zx2gsutp6b7snrb2rmzndwisuv4mtfsxpzq'
    def tag = "sampletag"
  }
  stages {
  stage('Run Repo Scan') {
          steps {
            container('trufflehog') {
                repoScan([branchName: env.BRANCH_NAME])
	    }
      }
    }
    stage('Workspace clean') {
                steps {
                    // Cleanup before starting the stage
                        cleanWs()
       }
    }
    stage('Checkout Code') {
      steps {
             script{
              container('git') {
                checkout scm
                		(tag,flag) = semverVersion()
                		env.version = tag
                	}
           }
      }
    }

//     stage('Run Prerequisite Tasks') {
//             when {
//              	      branch 'master'
//                }
//        steps {
//             script {
//                  def config = readYaml  file: 'helm/neom-fss-neompay-wallet-api/values.yaml'
//                     for(env_var in config.env){
//                           if(env_var.name == 'DB_NAME'){
//                              data_base_name=env_var.value;
//                                 env.APP_DATABASE_NAME = data_base_name
//                             }
//                      }
//                 }
//             script{
//                def data_base_name=''
//                container('mysql') {
//                   sh(script:'''
//                     mysql -N -u $DB_USERNAME -p$DB_PASS -h $DB_HOST -e "CREATE DATABASE IF NOT EXISTS $APP_DATABASE_NAME"
//                     mysql -N -u $DB_USERNAME -p$DB_PASS $APP_DATABASE_NAME -h $DB_HOST <library/sql/CREATE_DATABASECHANGELOG.sql
//                   ''')
//             }
//         }
//     }
//   }

    stage('Sonar Quality Report') {
      steps {
        container('build') {
            withSonarQubeEnv('sonar-server') {
          sh '''
            ./gradlew clean jacocoTestReport sonarqube
          '''
        }
      }
    }
    }
    stage('Quality Gate') {
      steps {
        container('ubuntu') {
            qualityGates()
        }
      }
    }
    stage('Code Build') {
      steps {
        container('build') {
          sh "/usr/bin/gradlew clean build"
        }
      }
    }


    stage('Docker Image Build') {
      steps {
        script{
               container('podman') {
               podmanBuild("jed.ocir.io", "axssefozft8h", "${projectName}", "${tag}", "${imagetype}")
               }
            }
        }
    }

     stage('Docker Image scan') {
              steps {
                script{
                      container('podman') {
                            trivyScanocr("jed.ocir.io", "axssefozft8h", "${projectName}", "${tag}", "${imagetype}")
                     }
                }
             }
     }

    stage("Docker Image push") {
           when{
           	     branch 'master'
               }
          steps {
                  script{
                      container('podman') {
                       tfci.config()
                          podmanPush("bld-neompay-image-pushers", "jed.ocir.io", "axssefozft8h", "${projectName}", "${tag}", "${imagetype}")

                      }
                }
           }
    }
    stage('Starting Zap Proxy') {
	      when{
	          branch 'main'
         }
                    steps {
                        echo "*********************** Starting ZAP proxy ***********************************"
                        script {
                            container("owasp-zap")  {
                                runOwaspZapProxy()
                            }
                        }
                    }
                }
   stage('Deploy') {
    when{
	      branch 'master'
      }
      steps {
        container('helm') {
		withCredentials([file(credentialsId: 'PRIVATE_KEY', variable: 'key'),file(credentialsId: 'config', variable: 'CONFIG')]) {
		          sh "mkdir -p ~/.oci/"
		          sh "cp \$key ~/.oci/private-key.pem"
		          sh "cp \$CONFIG ~/.oci/config"
              	    withKubeCredentials(kubectlCredentials: [[caCertificate: '', clusterName: 'bld-neompay-domain', contextName: 'bld-neompay-domain', credentialsId: 'bld-neompay', namespace: '', serverUrl: '']]) {

		        dir("helm"){
	  	          upgradeReleaseV3(
	  	          chartName: "${projectName}",
	  	          releaseName: "${releaseName}",
	  	          namespace: "${namespace}",
	  	          options: "--set image.tag=${tag}"
	  	          )

	  	       upgradeReleaseV3(
                chartName: "istio-${releaseName}",
                releaseName: "istio-${releaseName}",
                 namespace: "${namespace}"
            )
        	  }
	          }
	          }
     	     }
    	  }
  	}
     /*stage('BDD Tests') {
           when{
              branch 'master'
           }
           steps{
            echo "*********************** Wallet app domain bdd trigger ${env.DOMAIN_BDD_TEST_DIR}***********************************"
            build job: "${env.DOMAIN_BDD_TEST_DIR}/${projectName}-bdd-test",
            parameters:[string(name: 'Environment',value: 'BUILD')],
            propagate: true, wait: true
        }
     }*/
	stage('Zap Proxy') {
           when{
	          branch 'master'
     	      }
                    steps {
                        echo "*********************** ZAP proxy scanning ***********************************"
                        script {
                            container("owasp-zap")  {
                                runActivescan()
                                generateHtmlReport()

                            }
                        }
                    }
                }
     stage('Release Artifact') {
                   steps {
                    script{
                     container('helm') {
          		            def config = [
                  			  // Must contain imagename:imagetag
                			        images: ["jed.ocir.io/axssefozft8h/${imagetype}/${projectName}:${tag}"],
                  			      // Any folder with Chart.yaml will be released as a tarball by default
                  			      releaseFileList: ["helm/*/*.yaml, helm-overrides/*.yaml"],
                  			      dryRun: true,
                			        version: "${env.version}",
                  			      jenkinsCredentials: "github_token_ak",
                		       ]
                			    releaseRTLHandler(config)
          	    }
            }
         }
     }
	}
}