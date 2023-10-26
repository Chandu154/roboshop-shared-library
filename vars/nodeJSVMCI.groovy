// this is our CI job
def call(Map configMap) {
    //refering the variable usiong key from configmap and assigning the component variable 
    def component = configMap.get("component")
    echo "component is $component"
    pipeline {
        agent { node { label 'AGENT-1' } }
        
        environment{
            //here if we create any variables we will have global access, since it is environment no need pf def
        packageVersion= ''
        }
    
        stages {
            stage('Get version'){
                steps{
                    script{
                    def packageJson = readJSON(file: 'package.json')
                     packageVersion = packageJson.version
                    echo "version: ${packageVersion}"
                }
                
                }
            }
            stage('Install depdencies') {
                steps {
                    sh 'npm install'
                }
            }
            stage('Unit test') {
                steps {
                    echo "unit testing is done here"
                }
            }
            //sonar-scanner command expect sonar-project.properties should be available
            // stage('Sonar Scan') {
            //     steps {
            //         sh 'ls -ltr'
            //         sh 'sonar-scanner'
            //     }
            // }
            stage('Build') {
                steps {
                    sh 'ls -ltr'
                    sh "zip -r ${component}.zip ./* --exclude=.git --exclude=.zip"
                }
            }
    
            stage('Publish Artifact') {
                steps {
                    nexusArtifactUploader(
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        nexusUrl: '172.31.88.69:8081/',
                        groupId: 'com.roboshop',
                        version: "$packageVersion",
                        repository: "${component}",
                        credentialsId: 'nexus-auth',
                        artifacts: [
                            [artifactId: "${component}",
                            classifier: '',
                            file: "${component}.zip",
                            type: 'zip']
                        ]
                    )
                }
            }
    
             // upstream job is CI 
          // here  I need to configure downstream job.  I have to pass package version for catalogue-deployment for which one to deploy
          // This job will wait untill downstream job is over  
          // by default when a non-master branch CI is done , we can go for DEV deployment
            stage('Deploy') {
                steps {
                    script{
                        echo "Deployment"
                        def params = [
                            string(name: 'version', value: "$packageVersion"),
                            string(name: 'environment', value: "dev")
                        ]
                    build job: "../${component}-deploy", wait: true, parameters: params
                }
                       }
            }
        }
    
        post{
            always{
                echo 'cleaning up workspace'
                deleteDir()
            }
        }
    }

}