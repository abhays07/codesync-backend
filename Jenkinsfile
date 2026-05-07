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
        // We will set this dynamically in the first stage
        SELECTED_SERVICE = ""
    }

    stages {
        stage('Detect Changes') {
            steps {
                script {
                    // Check if the build was triggered by a GitHub Webhook
                    def isWebhook = currentBuild.buildCauses.toString().contains('GitHubPushCause')
                    
                    if (isWebhook) {
                        echo "Triggered by GitHub Webhook. Detecting changed folders..."
                        // Get list of changed files in the last commit
                        def changedFiles = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim()
                        echo "Files changed: \n${changedFiles}"

                        def services = ['auth-service', 'api-gateway', 'eureka-server', 'project-service', 'file-service', 'collab-service', 'execution-service', 'comment-service', 'notification-service', 'payment-service', 'version-service', 'admin-server']
                        
                        for (service in services) {
                            if (changedFiles.contains("${service}/")) {
                                env.SELECTED_SERVICE = service
                                echo "Auto-detected change in: ${service}"
                                break
                            }
                        }
                        
                        if (!env.SELECTED_SERVICE) {
                            echo "No specific service folder changes detected. Defaulting to first change or manual parameter."
                            env.SELECTED_SERVICE = params.SERVICE_NAME
                        }
                    } else {
                        echo "Manual build detected. Using parameter: ${params.SERVICE_NAME}"
                        env.SELECTED_SERVICE = params.SERVICE_NAME
                    }
                    
                    // Set the directory for subsequent stages
                    env.SERVICE_DIR = env.SELECTED_SERVICE
                }
            }
        }

        stage('Maven Build') {
            steps {
                dir("${env.SERVICE_DIR}") {
                    echo "Building ${env.SELECTED_SERVICE} with Maven..."
                    sh 'chmod +x mvnw || true'
                    sh './mvnw clean package -DskipTests'
                }
            }
        }

        stage('Docker Build & Tag') {
            steps {
                dir("${env.SERVICE_DIR}") {
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
            steps {
                script {
                    withCredentials([file(credentialsId: 'production-env-file', variable: 'ENV_FILE')]) {
                        sh """
                            # 1. Copy env file
                            cp \$ENV_FILE .env
                            echo "SPRING_PROFILES_ACTIVE=prod" >> .env
                            
                            # 2. Cleanup old local image
                            docker rmi \$(docker images -q ${env.CURRENT_IMAGE}:latest) || true
                            
                            # 3. Pull new image with retry
                            for i in {1..3}; do
                                docker pull ${env.CURRENT_IMAGE}:latest && break || sleep 10
                            done
                            
                            # 4. Deploy using Docker Compose
                            docker stop ${env.SELECTED_SERVICE} || true
                            docker rm ${env.SELECTED_SERVICE} || true
                            docker-compose -p codesync up -d --no-deps ${env.SELECTED_SERVICE}
                            
                            # 5. Cleanup
                            docker image prune -f
                        """
                    }
                }
            }
        }
    }
}