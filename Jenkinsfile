pipeline {
    agent {
        node {
            label "maven"
        }
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
                sh 'mvn clean verify -Dmaven.compiler.fork=false'
                echo "-------- Build & Test Completed --------"
            }
        }

        stage("SonarQube Analysis") {
            environment {
                scannerHome = tool 'sagar171414-sonar-scanner'
            }
            steps {
                withSonarQubeEnv('sagar171414-sonarqube-server') {
                    sh "${scannerHome}/bin/sonar-scanner"
                }
            }
        }

        stage("SonarQube Quality Gate") {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        echo "⏳ Waiting for SonarQube Quality Gate result..."
                        sleep(time: 10, unit: 'SECONDS') // buffer delay

                        def qg = waitForQualityGate()
                        echo "🔍 SonarQube Quality Gate status: ${qg.status}"

                        // Ignore Quality Gate failure, only log the result
                        if (qg.status != 'OK') {
                            echo "⚠️ Quality Gate failed: ${qg.status} — Ignoring for now."
                            // Do NOT mark build as UNSTABLE
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
                }
            }
        }

        stage("Docker Build") {
            steps {
                script {
                    echo "<--------------- Docker Build Started --------------->"
                    def tag = "${env.BUILD_NUMBER ?: '0'}-manual"
                    def imageFullPath = "trialvl2jw6.jfrog.io/devops-docker-local/ttrend:${tag}"

                    app = docker.build("${imageFullPath}", "--memory=512m .")
                    env.DOCKER_IMAGE_TAG = tag
                    env.DOCKER_IMAGE_NAME = imageFullPath
                    echo "<--------------- Docker Build Ended --------------->"
                }
            }
        }

        stage("Docker Image Scan by Trivy") {
            steps {
                script {
                    def image = env.DOCKER_IMAGE_NAME
                    echo "<--------------- Docker Scan Started [Trivy] --------------->"

                    def trivyExitCode = sh(
                        script: """
                            trivy image \
                              --severity HIGH,CRITICAL \
                              --exit-code 1 \
                              --no-progress \
                              --ignore-unfixed \
                              ${image}
                        """,
                        returnStatus: true
                    )

                    // Ignore vulnerabilities for now, only log them
                    if (trivyExitCode == 1) {
                        echo "⚠️ Trivy found HIGH or CRITICAL vulnerabilities (ignored for now)."
                        // Do NOT mark build as UNSTABLE
                    } else if (trivyExitCode == 2) {
                        error "❌ Trivy failed to execute properly (exit code 2)."
                    } else {
                        echo "✅ No critical vulnerabilities found by Trivy."
                    }

                    echo "<--------------- Docker Scan Completed --------------->"
                }
            }
        }

        stage("Docker Publish") {
            steps {
                script {
                    echo "<--------------- Docker Publish Started --------------->"
                    docker.withRegistry("${registry}", "artifact-cred") {
                        app.push()
                    }
                    echo "<--------------- Docker Publish Ended --------------->"
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
        failure {
            echo "❌ Build failed."
        }
        success {
            echo "✅ Build succeeded."
        }
        unstable {
            echo "⚠️ Build was marked as UNSTABLE earlier, but now we ignore it for learning purposes."
        }
    }
}
