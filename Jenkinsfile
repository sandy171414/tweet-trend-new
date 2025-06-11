pipeline {
    agent {
        node {
            label "maven"
        }
    }

    environment {
        PATH = "/opt/apache-maven-3.9.9/bin:$PATH"
    }

    stages {
        stage("Build") {
            steps {
                echo "-------- Build Started --------"
                sh 'mvn clean install -DskipTests=true'
                echo "-------- Build Completed --------"
            }
        }

        stage("Test & Coverage Report") {
            steps {
                echo "-------- Running Unit Tests --------"
                sh 'mvn test verify'
                echo "-------- Unit Tests Completed --------"
            }
        }

        stage('SonarQube Analysis') {
            environment {
                scannerHome = tool 'sagar171414-sonar-scanner'
            }
            steps {
                withSonarQubeEnv('sagar171414-sonarqube-server') {
                    echo "-------- Running Sonar Scanner (properties file mode) --------"
                    sh "${scannerHome}/bin/sonar-scanner"
                }
            }
        }

        stage('SonarQube Quality Gate') {
            steps {
                timeout(time: 2, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "❌ Quality Gate failed: ${qg.status}"
                        } else {
                            echo "✅ Quality Gate passed."
                        }
                    }
                }
            }
        }
    }

    post {
        failure {
            echo "❌ Pipeline failed. Check the logs for errors."
        }
        success {
            echo "✅ Pipeline completed successfully."
        }
    }
}
