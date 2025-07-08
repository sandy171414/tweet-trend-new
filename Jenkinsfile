pipeline {
    agent {
        node {
            label "maven"
        }
    }

    options {
        timestamps()
        ansiColor('xterm')
    }

    environment {
        PATH = "/opt/apache-maven-3.9.9/bin:$PATH"
        MAVEN_OPTS = "-Xmx256m -Xms128m -XX:+UseSerialGC"
        registry = "trialvl2jw6.jfrog.io"   // Note: registry URL should not include "https://"
        version = "2.0.2"
    }

    stages {

        stage("Build & Unit Test with Coverage") {
            steps {
                echo "-------- Build & Test Started --------"
                sh '''
                    echo -e "\\033[1;34m🔵 Running mvn clean verify...\\033[0m"
                    mvn clean verify -Dmaven.compiler.fork=false
                    echo -e "\\033[1;32m🟢 Build & Unit Tests passed!\\033[0m"
                '''
            }
        }

        stage("SonarQube Analysis") {
            environment {
                scannerHome = tool 'sagar171414-sonar-scanner'
            }
            steps {
                withSonarQubeEnv('sagar171414-sonarqube-server') {
                    sh '''
                        echo -e "\\033[1;34m🔍 Starting SonarQube scan...\\033[0m"
                        ${scannerHome}/bin/sonar-scanner
                        echo -e "\\033[1;32m🟢 SonarQube scan submitted!\\033[0m"
                    '''
                }
            }
        }

        stage("SonarQube Quality Gate") {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        echo "⏳ Waiting for SonarQube Quality Gate result..."
                        sleep(time: 10, unit: 'SECONDS')
                        def qg = waitForQualityGate()
                        echo "🔍 SonarQube Quality Gate status: ${qg.status}"
                        if (qg.status != 'OK') {
                            echo "⚠️ Quality Gate failed: ${qg.status}"
                        } else {
                            echo "✅ Quality Gate passed."
                        }
                    }
                }
            }
        }

        stage("Jar Publish") {
            steps {
                script {
                    echo '<--------------- Jar Publish Started --------------->'
                    sh 'ls -lh target/*.jar || echo "❌ No JAR found!"'
                }
            }
        }

        stage("Docker Build & Push") {
            steps {
                script {
                    echo "<--------------- Docker Build & Push Started --------------->"
                    def tag = "${env.BUILD_NUMBER ?: '0'}-manual"
                    def imageFullPath = "${registry}/devops-docker-local/ttrend:${tag}"

                    // Build the image
                    sh """
                        echo -e "\\033[1;34m🐳 Building Docker image...\\033[0m"
                        docker build -t ${imageFullPath} --memory=512m .
                        echo -e "\\033[1;32m🟢 Docker build complete: ${imageFullPath}\\033[0m"
                    """

                    // Login to JFrog
                    sh """
                        echo -e "\\033[1;34m🔐 Logging into JFrog...\\033[0m"
                        docker login -u sagarsaswade31@gmail.com -p <your-api-key> ${registry}
                    """

                    // Push the image
                    sh """
                        echo -e "\\033[1;34m🚀 Pushing Docker image...\\033[0m"
                        docker push ${imageFullPath}
                    """

                    env.DOCKER_IMAGE_NAME = imageFullPath

                    echo "<--------------- Docker Build & Push Completed --------------->"
                }
            }
        }

        stage("Docker Image Scan by Trivy") {
            steps {
                script {
                    echo "<--------------- Docker Scan Started [Trivy] --------------->"
                    def trivyExitCode = sh(
                        script: """
                            trivy image \
                              --severity HIGH,CRITICAL \
                              --exit-code 1 \
                              --no-progress \
                              --ignore-unfixed \
                              ${env.DOCKER_IMAGE_NAME}
                        """,
                        returnStatus: true
                    )
                    if (trivyExitCode == 1) {
                        echo "⚠️ Trivy found HIGH or CRITICAL vulnerabilities"
                    } else if (trivyExitCode == 2) {
                        error "❌ Trivy failed to execute (exit code 2)"
                    } else {
                        echo "✅ No critical vulnerabilities found by Trivy."
                    }
                }
            }
        }

        stage("Run Docker Container per Branch") {
            steps {
                script {
                    def portMap = ['main': '8001', 'dev': '8002', 'stage': '8003']
                    def port = portMap.get(env.BRANCH_NAME, '8000')
                    def containerName = "ttrend-${env.BRANCH_NAME}"

                    sh """
                        echo -e "\\033[1;34m🧹 Cleaning old containers...\\033[0m"
                        docker rm -f ${containerName} || true

                        echo -e "\\033[1;34m🚀 Running container on port ${port}...\\033[0m"
                        docker run -d -p ${port}:8080 -e SPRING_PROFILES_ACTIVE=${env.BRANCH_NAME} --name ${containerName} ${env.DOCKER_IMAGE_NAME}
                    """
                }
            }
        }

        stage("Cleanup") {
            steps {
                cleanWs()
                sh 'sync; echo 3 > /proc/sys/vm/drop_caches || true'
            }
        }
    }

    post {
        success {
            echo "✅ Build succeeded!"
        }
        failure {
            echo "❌ Build failed!"
        }
        unstable {
            echo "⚠️ Build marked unstable!"
        }
    }
}
