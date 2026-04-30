pipeline {
    agent any

    // Define parameters so you can choose which service to deploy from the UI
    parameters {
        Choice(name: 'SERVICE_NAME', choices: ['auth-service', 'api-gateway', 'eureka-server', 'project-service', 'file-service', 'collab-service', 'execution-service', 'comment-service', 'notification-service', 'payment-service', 'version-service', 'admin-server'], description: 'Select the microservice to build and deploy')
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
                        // Build the image
                        dockerImage = docker.build("${DOCKER_IMAGE}:${IMAGE_TAG}")
                        // Also tag it as latest
                        sh "docker tag ${DOCKER_IMAGE}:${IMAGE_TAG} ${DOCKER_IMAGE}:latest"
                    }
                }
            }
        }

        stage('Docker Push') {
            steps {
                echo "Pushing ${DOCKER_IMAGE} to Docker Hub..."
                script {
                    docker.withRegistry('https://index.docker.io/v1/', "${DOCKER_HUB_CREDENTIALS_ID}") {
                        dockerImage.push("${IMAGE_TAG}")
                        dockerImage.push('latest')
                    }
                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                echo "Updating ${params.SERVICE_NAME} on the deployment server..."
                // Since Jenkins is running on the SAME EC2 instance via Docker socket,
                // we can interact with docker directly to restart the specific service.
                script {
                    sh """
                        # Pull the latest image
                        docker pull ${DOCKER_IMAGE}:latest
                        
                        # Navigate to the directory containing docker-compose.yml on the host
                        # (We assume Jenkins has checked out the repo containing docker-compose.yml here in the workspace)
                        
                        # Recreate and start only the updated service in the background
                        docker-compose up -d --no-deps --build ${params.SERVICE_NAME}
                        
                        # Clean up dangling images to save disk space
                        docker image prune -f
                    """
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
