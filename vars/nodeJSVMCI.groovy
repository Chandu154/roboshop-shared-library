// this is our CI job

pipeline {
    agent { node { label 'AGENT-1' } }
    
    environment{
        //here if we create any variables we will have global access, since it is environment no need pf def
    packageVersion= ''
    }

     parameters {
                    string(name: 'component', defaultValue: '', description: 'Which component ?')
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
                sh "zip -r ${params.component}.zip ./* --exclude=.git --exclude=.zip"
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
                    repository: "${params.component}",
                    credentialsId: 'nexus-auth',
                    artifacts: [
                        [artifactId: "${params.component}",
                        classifier: '',
                        file: "${params.component}.zip",
                        type: 'zip']
                    ]
                )
            }
        }

         // upstream job is CI 
      // here  I need to configure downstream job.  I have to pass package version for catalogue-deployment for which one to deploy
      // This job will wait untill downstream job is over  
        stage('Deploy') {
            steps {
                script{
                    echo "Deployment"
                    def params = [
                        string(name: 'version', value: "$packageVersion")
                    ]
                build job: "../${params.component}-deploy", wait: true, parameters: params
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

