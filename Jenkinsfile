pipeline {
    agent {
        node {
            label "maven"
        }
    }

    tools {
        maven 'maven-3.9'

    }

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean install'
            }
        }

        stage('Deploy') {
            steps {
                sh 'mvn deploy'
            }
        }

        stage('SonarQube Analysis') {
            environment {
                scannerHome = tool 'sagar171414-sonar-scanner'
            }
            steps {
                withSonarQubeEnv('sagar171414-sonarqube-server') {
                    sh "${scannerHome}/bin/sonar-scanner"
                }
            }
        }
    }
}
