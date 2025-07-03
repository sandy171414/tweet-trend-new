pipeline {
    agent {
        node {
            label "maven"
        }
    }

    environment {
        PATH = "/opt/apache-maven-3.9.9/bin:$PATH"
        MAVEN_OPTS="-Xmx256m -Xms128m -XX:+UseSerialGC"
        registry = "https://trialvl2jw6.jfrog.io"
        version = "2.0.2"
    }

    stages {
        stage("Build") {
            steps {
                echo "-------- Build Started --------"
                sh 'mvn clean install -DskipTests=true -Dmaven.compiler.fork=false'
                echo "-------- Build Completed --------"
            }
        }

        stage("Test") {
            steps {
                echo "----------- Unit Test Started ----------"
                sh 'mvn surefire-report:report'
                echo "----------- Unit Test Completed ----------"
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

        /*
        stage("SonarQube Quality Gate") {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
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
        */

        stage("Jar Publish") {
            steps {
                script {
                    echo '<--------------- Jar Publish Started --------------->'
                    sh 'echo "--- Verifying JAR before upload ---"'
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
                    app = docker.build("valaxy-docker-docker-local/ttrend:${tag}", "--memory=512m .")
                    env.DOCKER_IMAGE_TAG = tag  // Store tag for next stages
                    echo "<--------------- Docker Build Ended --------------->"
                }
            }
        }

        stage("Docker Image Scan") {
            steps {
                script {
                    def image = "valaxy-docker-docker-local/ttrend:${env.DOCKER_IMAGE_TAG}"
                    echo "<--------------- Docker Scan Started [Trivy] --------------->"
                    
                    sh """
                        trivy image \
                          --severity HIGH,CRITICAL \
                          --exit-code 1 \
                          --no-progress \
                          --ignore-unfixed \
                          ${image}
                    """

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
    }
}
