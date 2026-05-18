pipeline {
    agent any

    parameters {
        choice(name: 'SERVICE_NAME', 
               choices: ['all', 'auth-service', 'api-gateway', 'eureka-server', 'project-service', 'file-service', 'collab-service', 'execution-service', 'comment-service', 'notification-service', 'payment-service', 'version-service', 'admin-server'], 
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
                    def detected = []
                    
                    if (isWebhook) {
                        echo "Triggered by GitHub Webhook. Detecting changed folders..."
                        def changedFiles = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim()
                        echo "Files changed: \n${changedFiles}"

                        def services = ['auth-service', 'api-gateway', 'eureka-server', 'project-service', 'file-service', 'collab-service', 'execution-service', 'comment-service', 'notification-service', 'payment-service', 'version-service', 'admin-server']
                        
                        for (service in services) {
                            if (changedFiles.contains("${service}/")) {
                                detected.add(service)
                                echo "Auto-detected change in: ${service}"
                            }
                        }
                        
                        // Rebuild all if root config changed
                        if (changedFiles.contains("docker-compose.yml") || changedFiles.contains("Jenkinsfile") || changedFiles.contains(".env")) {
                            echo "Core infrastructure files changed. Rebuilding all services."
                            detected = ['all']
                        }
                    } 

                    if (isWebhook) {
                        if (detected.size() > 0) {
                            env.SELECTED_SERVICE = detected.join(',')
                        } else {
                            echo "No service changes detected for webhook push. Skipping build."
                            env.SELECTED_SERVICE = "SKIP"
                        }
                    } else {
                        echo "Manual build. Using parameter: ${params.SERVICE_NAME}"
                        env.SELECTED_SERVICE = params.SERVICE_NAME ? params.SERVICE_NAME.toString() : "all"
                    }
                    
                    echo "FINAL SELECTED SERVICE(S): ${env.SELECTED_SERVICE}"
                }
            }
        }

        stage('Maven Build') {
            when {
                expression { env.SELECTED_SERVICE != 'SKIP' }
            }
            steps {
                script {
                    def servicesToBuild = env.SELECTED_SERVICE == 'all' ? ['auth-service', 'api-gateway', 'eureka-server', 'project-service', 'file-service', 'collab-service', 'execution-service', 'comment-service', 'notification-service', 'payment-service', 'version-service', 'admin-server'] : env.SELECTED_SERVICE.split(',')
                    for (def serviceName : servicesToBuild) {
                        serviceName = serviceName.trim()
                        dir(serviceName) {
                            echo "Building ${serviceName} with Maven..."
                            sh 'chmod +x mvnw || true'
                            sh './mvnw clean package -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Docker Build & Tag') {
            when {
                expression { env.SELECTED_SERVICE != 'SKIP' }
            }
            steps {
                script {
                    def servicesToBuild = env.SELECTED_SERVICE == 'all' ? ['auth-service', 'api-gateway', 'eureka-server', 'project-service', 'file-service', 'collab-service', 'execution-service', 'comment-service', 'notification-service', 'payment-service', 'version-service', 'admin-server'] : env.SELECTED_SERVICE.split(',')
                    for (def serviceName : servicesToBuild) {
                        serviceName = serviceName.trim()
                        dir(serviceName) {
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
                            def imageName = map[serviceName] ?: "abhays2004/codesync-${serviceName}"
                            
                            echo "Building Docker image for ${imageName}..."
                            sh "docker build -t ${imageName}:${IMAGE_TAG} -t ${imageName}:latest ."
                        }
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
                        
                        def servicesToBuild = env.SELECTED_SERVICE == 'all' ? ['auth-service', 'api-gateway', 'eureka-server', 'project-service', 'file-service', 'collab-service', 'execution-service', 'comment-service', 'notification-service', 'payment-service', 'version-service', 'admin-server'] : env.SELECTED_SERVICE.split(',')
                        for (def serviceName : servicesToBuild) {
                            serviceName = serviceName.trim()
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
                            def imageName = map[serviceName] ?: "abhays2004/codesync-${serviceName}"
                            
                            sh "docker push ${imageName}:${IMAGE_TAG}"
                            sh "docker push ${imageName}:latest"
                        }
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
                        """
                        
                        def servicesToBuild = env.SELECTED_SERVICE == 'all' ? ['auth-service', 'api-gateway', 'eureka-server', 'project-service', 'file-service', 'collab-service', 'execution-service', 'comment-service', 'notification-service', 'payment-service', 'version-service', 'admin-server'] : env.SELECTED_SERVICE.split(',')
                        
                        for (def serviceName : servicesToBuild) {
                            serviceName = serviceName.trim()
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
                            def imageName = map[serviceName] ?: "abhays2004/codesync-${serviceName}"
                            
                            sh """
                                docker rmi \$(docker images -q ${imageName}:latest) || true
                                for i in {1..3}; do
                                    docker pull ${imageName}:latest && break || sleep 10
                                done
                                docker stop ${serviceName} || true
                                docker rm ${serviceName} || true
                            """
                        }
                        
                        if (env.SELECTED_SERVICE == 'all') {
                            sh "docker-compose -p codesync up -d"
                        } else {
                            def composeServices = servicesToBuild.join(' ')
                            sh "docker-compose -p codesync up -d --no-deps ${composeServices}"
                        }
                        sh "docker image prune -f"
                    }
                }
            }
        }
    }
}