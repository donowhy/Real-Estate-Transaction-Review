pipeline {
    agent any

    environment {
        DOCKER_HUB_USER = 'a1rt'
        DOCKER_HUB_CREDENTIALS_ID = 'docker-hub-credentials'
        SERVICES = 'discovery-service config-service gateway-service user-service review-service'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build JARs') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean build -x test'
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    def serviceList = SERVICES.split(' ')
                    docker.withRegistry('https://index.docker.io/v1/', DOCKER_HUB_CREDENTIALS_ID) {
                        serviceList.each { serviceName ->
                            echo "Building and Pushing Image for: ${serviceName}"
                            def image = docker.build("${DOCKER_HUB_USER}/${serviceName}:${env.BUILD_NUMBER}")
                            image.push()
                            image.push('latest')
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker-compose down'
                sh 'docker-compose up -d'
                echo 'Deployment Complete!'
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished.'
        }
    }
}
