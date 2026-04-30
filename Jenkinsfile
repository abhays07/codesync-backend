pipeline {
    agent any

    // Define parameters so you can choose which service to deploy from the UI
    parameters {
        choice(name: 'SERVICE_NAME', choices: ['auth-service', 'api-gateway', 'eureka-server', 'project-service', 'file-service', 'collab-service', 'execution-service', 'comment-service', 'notification-service', 'payment-service', 'version-service', 'admin-server'], description: 'Select the microservice to build and deploy')
    }

    environment {
        DOCKER_HUB_CREDENTIALS_ID = 'docker-hub-credentials'
        DOCKER_IMAGE = "abhays2004/codesync-${params.SERVICE_NAME}"
        IMAGE_TAG = "${BUILD_NUMBER}"
        // The directory of the chosen service
        SERVICE_DIR = "${params.SERVICE_NAME}"
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Maven Build') {
            steps {
                dir("${SERVICE_DIR}") {
                    echo "Building ${params.SERVICE_NAME} with Maven..."
                    sh 'chmod +x mvnw'
                    // Execute Maven wrapper to build the JAR
                    sh './mvnw clean package -DskipTests'
                }
            }
        }

        stage('Docker Build & Tag') {
            steps {
                dir("${SERVICE_DIR}") {
                    echo "Building Docker image for ${params.SERVICE_NAME}..."
                    script {
                        // Build and tag the image using native shell commands
                        sh "docker build -t ${DOCKER_IMAGE}:${IMAGE_TAG} ."
                        sh "docker tag ${DOCKER_IMAGE}:${IMAGE_TAG} ${DOCKER_IMAGE}:latest"
                    }
                }
            }
        }

        stage('Docker Push') {
            steps {
                echo "Pushing ${DOCKER_IMAGE} to Docker Hub..."
                script {
                    withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_CREDENTIALS_ID}", passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        // Login to Docker Hub securely
                        sh "echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USERNAME --password-stdin"
                        // Push images
                        sh "docker push ${DOCKER_IMAGE}:${IMAGE_TAG}"
                        sh "docker push ${DOCKER_IMAGE}:latest"
                    }
                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                echo "Updating ${params.SERVICE_NAME} on the deployment server..."
                script {
                    // Bring in the .env file securely from Jenkins Credentials
                    withCredentials([file(credentialsId: 'production-env-file', variable: 'ENV_FILE')]) {
                        sh """
                            # Copy the secret .env file into our workspace
                            cp \$ENV_FILE .env
                            
                            # Pull the latest image
                            docker pull ${DOCKER_IMAGE}:latest
                            
                            # Force remove the existing container to prevent naming conflicts
                            docker stop ${params.SERVICE_NAME} || true
                            docker rm ${params.SERVICE_NAME} || true
                            
                            # Recreate and start only the updated service using the workspace docker-compose
                            # We use -p codesync to force a uniform project name
                            docker-compose -p codesync up -d --no-deps ${params.SERVICE_NAME}
                            
                            # Clean up dangling images to save disk space
                            docker image prune -f
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Successfully built and deployed ${params.SERVICE_NAME}!"
        }
        failure {
            echo "❌ Pipeline failed for ${params.SERVICE_NAME}. Please check the logs."
        }
    }
}
