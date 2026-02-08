// Jenkins Pipeline for Enterprise Microservices
// Multi-branch pipeline supporting dev, staging, and production

pipeline {
    agent any

    environment {
        // Docker registry
        DOCKER_REGISTRY = credentials('docker-registry-url')
        DOCKER_CREDENTIALS = credentials('docker-registry-credentials')

        // SonarQube
        SONARQUBE_SERVER = 'SonarQube'

        // Git
        GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()

        // Determine environment based on branch
        ENVIRONMENT = "${env.BRANCH_NAME == 'main' ? 'production' : env.BRANCH_NAME == 'staging' ? 'staging' : 'development'}"
        IMAGE_TAG = "${env.BRANCH_NAME == 'main' ? env.BUILD_NUMBER : env.BRANCH_NAME}-${GIT_COMMIT_SHORT}"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    echo "Building for environment: ${ENVIRONMENT}"
                    echo "Image tag: ${IMAGE_TAG}"
                }
            }
        }

        stage('Build & Test Backend') {
            parallel {
                stage('Common Library') {
                    steps {
                        dir('common-lib') {
                            sh 'mvn clean install -DskipTests'
                        }
                    }
                }

                stage('Gateway Service') {
                    steps {
                        dir('gateway-service') {
                            sh 'mvn clean package -DskipTests'
                            sh 'mvn test'
                        }
                    }
                }

                stage('IAM Service') {
                    steps {
                        dir('iam-service') {
                            sh 'mvn clean package -DskipTests'
                            sh 'mvn test'
                        }
                    }
                }

                stage('Business Service') {
                    steps {
                        dir('business-service') {
                            sh 'mvn clean package -DskipTests'
                            sh 'mvn test'
                        }
                    }
                }
            }
        }

        stage('Build & Test Frontend') {
            parallel {
                stage('Enterprise Frontend') {
                    steps {
                        dir('frontend') {
                            sh 'npm ci --legacy-peer-deps'
                            sh 'npm run build -- --configuration production'
                        }
                    }
                }

                stage('Admin UI') {
                    steps {
                        dir('admin-ui') {
                            sh 'npm ci --legacy-peer-deps'
                            sh 'npm run build'
                        }
                    }
                }
            }
        }

        stage('Code Quality Analysis') {
            parallel {
                stage('SonarQube - Backend') {
                    when {
                        branch 'main'
                    }
                    steps {
                        script {
                            def scannerHome = tool 'SonarQube Scanner'
                            withSonarQubeEnv(SONARQUBE_SERVER) {
                                sh """
                                    ${scannerHome}/bin/sonar-scanner \
                                    -Dsonar.projectKey=enterprise-backend \
                                    -Dsonar.sources=. \
                                    -Dsonar.java.binaries=target/classes
                                """
                            }
                        }
                    }
                }

                stage('SonarQube - Frontend') {
                    when {
                        branch 'main'
                    }
                    steps {
                        dir('frontend') {
                            script {
                                def scannerHome = tool 'SonarQube Scanner'
                                withSonarQubeEnv(SONARQUBE_SERVER) {
                                    sh """
                                        ${scannerHome}/bin/sonar-scanner \
                                        -Dsonar.projectKey=enterprise-frontend \
                                        -Dsonar.sources=src \
                                        -Dsonar.typescript.lcov.reportPaths=coverage/lcov.info
                                    """
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Build Docker Images') {
            parallel {
                stage('Gateway Service Image') {
                    steps {
                        dir('gateway-service') {
                            script {
                                docker.build("${DOCKER_REGISTRY}/gateway-service:${IMAGE_TAG}")
                            }
                        }
                    }
                }

                stage('IAM Service Image') {
                    steps {
                        dir('iam-service') {
                            script {
                                docker.build("${DOCKER_REGISTRY}/iam-service:${IMAGE_TAG}")
                            }
                        }
                    }
                }

                stage('Business Service Image') {
                    steps {
                        dir('business-service') {
                            script {
                                docker.build("${DOCKER_REGISTRY}/business-service:${IMAGE_TAG}")
                            }
                        }
                    }
                }

                stage('Frontend Image') {
                    steps {
                        dir('frontend') {
                            script {
                                docker.build("${DOCKER_REGISTRY}/frontend:${IMAGE_TAG}")
                            }
                        }
                    }
                }

                stage('Admin UI Image') {
                    steps {
                        dir('admin-ui') {
                            script {
                                docker.build("${DOCKER_REGISTRY}/admin-ui:${IMAGE_TAG}")
                            }
                        }
                    }
                }
            }
        }

        stage('Security Scan') {
            parallel {
                stage('Trivy Scan') {
                    steps {
                        script {
                            def services = ['gateway-service', 'iam-service', 'business-service', 'frontend', 'admin-ui']
                            services.each { service ->
                                sh """
                                    trivy image --severity HIGH,CRITICAL \
                                    --exit-code 0 \
                                    ${DOCKER_REGISTRY}/${service}:${IMAGE_TAG}
                                """
                            }
                        }
                    }
                }
            }
        }

        stage('Push Docker Images') {
            steps {
                script {
                    docker.withRegistry("https://${DOCKER_REGISTRY}", DOCKER_CREDENTIALS) {
                        def services = ['gateway-service', 'iam-service', 'business-service', 'frontend', 'admin-ui']
                        services.each { service ->
                            docker.image("${DOCKER_REGISTRY}/${service}:${IMAGE_TAG}").push()

                            // Tag as latest for main branch
                            if (env.BRANCH_NAME == 'main') {
                                docker.image("${DOCKER_REGISTRY}/${service}:${IMAGE_TAG}").push('latest')
                            }
                        }
                    }
                }
            }
        }

        stage('Update Kubernetes Manifests') {
            when {
                anyOf {
                    branch 'main'
                    branch 'staging'
                    branch 'develop'
                }
            }
            steps {
                script {
                    // Update kustomization.yaml with new image tags
                    sh """
                        cd k8s/overlays/${ENVIRONMENT}
                        kustomize edit set image \\
                            enterprise/gateway-service=${DOCKER_REGISTRY}/gateway-service:${IMAGE_TAG} \\
                            enterprise/iam-service=${DOCKER_REGISTRY}/iam-service:${IMAGE_TAG} \\
                            enterprise/business-service=${DOCKER_REGISTRY}/business-service:${IMAGE_TAG} \\
                            enterprise/frontend=${DOCKER_REGISTRY}/frontend:${IMAGE_TAG}
                    """

                    // Commit and push changes to trigger ArgoCD sync
                    sh """
                        git config user.email "jenkins@enterprise.com"
                        git config user.name "Jenkins CI"
                        git add k8s/overlays/${ENVIRONMENT}/kustomization.yaml
                        git commit -m "Update ${ENVIRONMENT} images to ${IMAGE_TAG}" || true
                        git push origin HEAD:${env.BRANCH_NAME} || true
                    """
                }
            }
        }

        stage('Trigger ArgoCD Sync') {
            when {
                anyOf {
                    branch 'main'
                    branch 'staging'
                }
            }
            steps {
                script {
                    def appName = "enterprise-${ENVIRONMENT}"
                    sh """
                        argocd app sync ${appName} --prune --force
                        argocd app wait ${appName} --health --timeout 600
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
            // Send notification (Slack, Teams, Email, etc.)
        }
        failure {
            echo 'Pipeline failed!'
            // Send failure notification
        }
        always {
            // Clean up workspace
            cleanWs()
        }
    }
}
