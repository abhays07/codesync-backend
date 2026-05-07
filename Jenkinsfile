pipeline {
    agent any

    parameters {
        choice(name: 'SERVICE_NAME', 
               choices: ['auth-service', 'api-gateway', 'eureka-server', 'project-service', 'file-service', 'collab-service', 'execution-service', 'comment-service', 'notification-service', 'payment-service', 'version-service', 'admin-server'], 
               description: 'Select the microservice to build and deploy (Ignored if triggered by Webhook)')
    }

    environment {
        DOCKER_HUB_CREDENTIALS_ID = 'docker-hub-credentials'
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Detect Changes') {
            steps {
                script {
                    def isWebhook = currentBuild.buildCauses.toString().contains('GitHubPushCause')
                    def detected = ""
                    
                    if (isWebhook) {
                        echo "Triggered by GitHub Webhook. Detecting changed folders..."
                        def changedFiles = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim()
                        echo "Files changed: \n${changedFiles}"

                        def services = ['auth-service', 'api-gateway', 'eureka-server', 'project-service', 'file-service', 'collab-service', 'execution-service', 'comment-service', 'notification-service', 'payment-service', 'version-service', 'admin-server']
                        
                        for (service in services) {
                            if (changedFiles.contains("${service}/")) {
                                detected = service
                                echo "Auto-detected change in: ${detected}"
                                break
                            }
                        }
                    } 

                    if (isWebhook) {
                        if (detected != "") {
                            env.SELECTED_SERVICE = detected
                        } else {
                            echo "No service changes detected for webhook push. Skipping build."
                            env.SELECTED_SERVICE = "SKIP"
                        }
                    } else {
                        echo "Manual build. Using parameter: ${params.SERVICE_NAME}"
                        env.SELECTED_SERVICE = params.SERVICE_NAME ? params.SERVICE_NAME.toString() : "auth-service"
                    }
                    
                    echo "FINAL SELECTED SERVICE: ${env.SELECTED_SERVICE}"
                }
            }
        }

        stage('Maven Build') {
            when {
                expression { env.SELECTED_SERVICE != 'SKIP' }
            }
            steps {
                // Use env.SELECTED_SERVICE here
                dir("${env.SELECTED_SERVICE}") {
                    echo "Building ${env.SELECTED_SERVICE} with Maven..."
                    sh 'chmod +x mvnw || true'
                    sh './mvnw clean package -DskipTests'
                }
            }
        }

        stage('Docker Build & Tag') {
            when {
                expression { env.SELECTED_SERVICE != 'SKIP' }
            }
            steps {
                dir("${env.SELECTED_SERVICE}") {
                    script {
                        def map = [
                            'auth-service': 'abhays2004/codesync-auth',
                            'api-gateway': 'abhays2004/codesync-gateway',
                            'eureka-server': 'abhays2004/codesync-eureka',
                            'project-service': 'abhays2004/codesync-project',
                            'file-service': 'abhays2004/codesync-file',
                            'collab-service': 'abhays2004/codesync-collab',
                            'execution-service': 'abhays2004/codesync-execution',
                            'comment-service': 'abhays2004/codesync-comment',
                            'notification-service': 'abhays2004/codesync-notification',
                            'payment-service': 'abhays2004/codesync-payment',
                            'version-service': 'abhays2004/codesync-version',
                            'admin-server': 'abhays2004/codesync-admin'
                        ]
                        def imageName = map[env.SELECTED_SERVICE] ?: "abhays2004/codesync-${env.SELECTED_SERVICE}"
                        
                        echo "Building Docker image for ${imageName}..."
                        sh "docker build -t ${imageName}:${IMAGE_TAG} -t ${imageName}:latest ."
                        env.CURRENT_IMAGE = imageName
                    }
                }
            }
        }

        stage('Docker Push') {
            when {
                expression { env.SELECTED_SERVICE != 'SKIP' }
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_CREDENTIALS_ID}", passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        sh "echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USERNAME --password-stdin"
                        sh "docker push ${env.CURRENT_IMAGE}:${IMAGE_TAG}"
                        sh "docker push ${env.CURRENT_IMAGE}:latest"
                    }
                }
            }
        }

        stage('Deploy to EC2') {
            when {
                expression { env.SELECTED_SERVICE != 'SKIP' }
            }
            steps {
                script {
                    withCredentials([file(credentialsId: 'production-env-file', variable: 'ENV_FILE')]) {
                        sh """
                            cp \$ENV_FILE .env
                            echo "SPRING_PROFILES_ACTIVE=prod" >> .env
                            docker rmi \$(docker images -q ${env.CURRENT_IMAGE}:latest) || true
                            for i in {1..3}; do
                                docker pull ${env.CURRENT_IMAGE}:latest && break || sleep 10
                            done
                            docker stop ${env.SELECTED_SERVICE} || true
                            docker rm ${env.SELECTED_SERVICE} || true
                            docker-compose -p codesync up -d --no-deps ${env.SELECTED_SERVICE}
                            docker image prune -f
                        """
                    }
                }
            }
        }
    }
}