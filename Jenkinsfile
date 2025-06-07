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
        stage("build") {
            steps {
                echo "-------- build started --------"
                sh 'mvn clean deploy -Dmaven.test.skip=true'
                echo "-------- build Completed --------"
            }
        }

        stage("test") {
            steps {
               echo "-------- unit test started --------"
               sh  'mvn surefire-report:report' 
               echo "-------- unit test Completed --------"
            } 
        }

        stage('SonarQube analysis') {
            environment {
                scannerHome = tool 'sagar171414-sonar-scanner'
            }
            steps {
                withSonarQubeEnv('sagar171414-sonarqube-server') {
                    sh """
                        ${scannerHome}/bin/sonar-scanner \
                        -Dsonar.scanner.cache.enable=false \
                        -Dsonar.nodejs.executable=/usr/bin/node
                    """
                }
            }
        }

        stage('SonarQube Quality Gate') {
            steps {
                timeout(time: 2, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            echo "⚠️  Quality Gate failed: ${qg.status}"
                        } else {
                            echo "✅ Quality Gate passed."
                        }
                    }
                }
            }
        }
    }
}
