pipeline {
    agent any

    parameters {
        choice(name: 'SERVICE_NAME', choices: ['auth-service', 'api-gateway', 'eureka-server', 'project-service', 'file-service', 'collab-service', 'execution-service', 'comment-service', 'notification-service', 'payment-service', 'version-service', 'admin-server'], description: 'Select the microservice to build and deploy')
    }

    environment {
        DOCKER_HUB_CREDENTIALS_ID = 'docker-hub-credentials'
        IMAGE_TAG = "${BUILD_NUMBER}"
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
                    sh 'chmod +x mvnw || true'
                    sh './mvnw clean package -DskipTests'
                }
            }
        }

        stage('Docker Build & Tag') {
            steps {
                dir("${SERVICE_DIR}") {
                    script {
                        // Dynamic mapping for image names
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
                        // Determine DOCKER_IMAGE
                        def imageName = map[params.SERVICE_NAME] ?: "abhays2004/codesync-${params.SERVICE_NAME}"
                        
                        echo "Building Docker image for ${imageName}..."
                        sh "docker build -t ${imageName}:${IMAGE_TAG} -t ${imageName}:latest ."
                        
                        // We store the image name in env for the next stages
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
                            cp \$ENV_FILE .env
                            docker pull ${env.CURRENT_IMAGE}:latest
                            docker stop ${params.SERVICE_NAME} || true
                            docker rm ${params.SERVICE_NAME} || true
                            docker-compose -p codesync up -d --no-deps ${params.SERVICE_NAME}
                            docker image prune -f
                        """
                    }
                }
            }
        }
    }
}