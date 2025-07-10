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
        registry = "https://trialvl2jw6.jfrog.io"
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
                slackSend(channel: 'jenkins-alerts', color: 'good',
                    message: "✅ Maven Build & Unit Test completed for *${env.JOB_NAME}* #${env.BUILD_NUMBER}")
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
                    slackSend(channel: 'jenkins-alerts', color: '#439FE0',
                        message: "🔍 SonarQube scan submitted for *${env.JOB_NAME}* #${env.BUILD_NUMBER}")
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
                            slackSend(channel: 'jenkins-alerts', color: 'warning',
                                message: "⚠️ SonarQube Quality Gate *failed* for *${env.JOB_NAME}* #${env.BUILD_NUMBER}: ${qg.status}")
                        } else {
                            echo "✅ Quality Gate passed."
                            slackSend(channel: 'jenkins-alerts', color: 'good',
                                message: "✅ SonarQube Quality Gate *passed* for *${env.JOB_NAME}* #${env.BUILD_NUMBER}")
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

                    def server = Artifactory.newServer(
                        url: "${env.registry}/artifactory",
                        credentialsId: "artifact-cred"
                    )

                    def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    def properties = "buildid=${env.BUILD_ID},commitid=${commitId}"

                    def uploadSpec = """{
                        "files": [
                            {
                                "pattern": "target/*.jar",
                                "target": "devops-libs-release-local/",
                                "flat": true,
                                "props": "${properties}",
                                "exclusions": ["*.sha1", "*.md5"]
                            }
                        ]
                    }"""

                    def buildInfo = server.upload(uploadSpec)
                    buildInfo.env.collect()
                    server.publishBuildInfo(buildInfo)

                    echo '<--------------- Jar Publish Ended --------------->'
                    slackSend(channel: 'jenkins-alerts', color: '#36a64f',
                        message: "📦 JAR uploaded to Artifactory for *${env.JOB_NAME}* #${env.BUILD_NUMBER}")
                }
            }
        }

        stage("Docker Build") {
            steps {
                script {
                    echo "<--------------- Docker Build Started --------------->"
                    def tag = "${env.BUILD_NUMBER ?: '0'}-manual"
                    def imageFullPath = "trialvl2jw6.jfrog.io/devops-docker-local/ttrend:${tag}"

                    dockerImage = docker.build(imageFullPath, "--memory=512m .")
                    env.DOCKER_IMAGE_NAME = imageFullPath

                    echo "🟢 Docker build complete: ${imageFullPath}"
                    slackSend(channel: 'jenkins-alerts', color: '#36a64f',
                        message: "🐳 Docker image *${env.DOCKER_IMAGE_NAME}* built for *${env.JOB_NAME}*")
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
                        slackSend(channel: 'jenkins-alerts', color: 'warning',
                            message: "⚠️ Trivy found HIGH/CRITICAL vulnerabilities in *${env.DOCKER_IMAGE_NAME}*")
                    } else if (trivyExitCode == 2) {
                        error "❌ Trivy failed to execute (exit code 2)"
                    } else {
                        echo "✅ No critical vulnerabilities found by Trivy."
                        slackSend(channel: 'jenkins-alerts', color: 'good',
                            message: "✅ Trivy scan clean for *${env.DOCKER_IMAGE_NAME}*")
                    }
                }
            }
        }

        stage("Docker Push to Artifactory") {
            steps {
                script {
                    echo "<--------------- Docker Push Started --------------->"
                    docker.withRegistry("${registry}", "artifact-cred") {
                        dockerImage.push()
                    }
                    slackSend(channel: 'jenkins-alerts', color: '#439FE0',
                        message: "🚀 Docker image *${env.DOCKER_IMAGE_NAME}* pushed to Artifactory")
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

                    slackSend(channel: 'jenkins-alerts', color: '#00bfff',
                        message: "🌐 Branch *${env.BRANCH_NAME}* deployed → http://<your-public-ip>:${port}")
                }
            }
        }

        stage("Cleanup") {
            steps {
                cleanWs()
                sh 'sync; echo 3 > /proc/sys/vm/drop_caches || true'
                slackSend(channel: 'jenkins-alerts', color: '#cccccc',
                    message: "🧹 Workspace cleaned after build *${env.JOB_NAME}* #${env.BUILD_NUMBER}")
            }
        }
    }

    post {
        success {
            slackSend(channel: 'jenkins-alerts', color: 'good',
                message: "✅ *${env.JOB_NAME}* #${env.BUILD_NUMBER} succeeded!\n🔗 ${env.BUILD_URL}")
            mail to: 'sagarsaswade31@gmail.com',
                subject: "✅ SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Build succeeded!\n\n🔗 ${env.BUILD_URL}"
        }

        failure {
            slackSend(channel: 'jenkins-alerts', color: 'danger',
                message: "❌ *${env.JOB_NAME}* #${env.BUILD_NUMBER} failed!\n🔗 ${env.BUILD_URL}")
            mail to: 'sagarsaswade31@gmail.com',
                subject: "❌ FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Build failed.\n\n🔗 ${env.BUILD_URL}"
        }

        unstable {
            slackSend(channel: 'jenkins-alerts', color: 'warning',
                message: "⚠️ *${env.JOB_NAME}* #${env.BUILD_NUMBER} is UNSTABLE.\n🔗 ${env.BUILD_URL}")
            mail to: 'sagarsaswade31@gmail.com',
                subject: "⚠️ UNSTABLE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Build marked as UNSTABLE.\nLikely reasons: vulnerabilities or SonarQube quality gate failure.\n\n🔗 ${env.BUILD_URL}"
        }
    }
}
