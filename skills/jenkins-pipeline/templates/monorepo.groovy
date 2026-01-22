// Monorepo 项目 - 脚本式 Jenkinsfile 模板
// 适用于包含多个服务的单一仓库（微服务架构）

node('build-node') {
    def projectName = 'monorepo-app'
    def changedServices = []
    def dockerImages = [:]

    echo "=== ${projectName} Monorepo CI/CD ==="

    // ==================== 阶段 1: 代码检出 ====================
    stage('Checkout') {
        echo '检出源代码...'
        checkout scm

        // 检测变更的服务
        def changedFiles = sh(
            script: 'git diff --name-only HEAD~1',
            returnStdout: true
        ).trim().split('\n')

        // 映射文件到服务
        def serviceMap = [
            'services/api/': 'api',
            'services/web/': 'web',
            'services/worker/': 'worker',
            'services/auth/': 'auth'
        ]

        changedFiles.each { file ->
            serviceMap.each { path, service ->
                if (file.startsWith(path) && !changedServices.contains(service)) {
                    changedServices.add(service)
                }
            }
        }

        echo "变更的服务: ${changedServices.join(', ')}"
    }

    // ==================== 阶段 2: 并行构建变更的服务 ====================
    stage('Build Changed Services') {
        if (changedServices.isEmpty()) {
            echo '没有服务变更，跳过构建'
            return
        }

        def buildTasks = [:]

        changedServices.each { service ->
            buildTasks["构建 ${service}"] = {
                node('build-node') {
                    stage("Build ${service}") {
                        dir("services/${service}") {
                            echo "构建 ${service} 服务..."

                            try {
                                // 根据服务类型选择构建命令
                                if (fileExists('pom.xml')) {
                                    sh 'mvn clean package -DskipTests'
                                } else if (fileExists('package.json')) {
                                    sh 'npm run build'
                                } else if (fileExists('build.gradle')) {
                                    sh './gradlew build -x test'
                                }

                                echo "${service} 构建成功"

                                // 归档构建产物
                                archiveArtifacts artifacts: 'dist/**/*', fingerprint: true

                            } catch (Exception e) {
                                echo "${service} 构建失败: ${e.message}"
                                currentBuild.result = 'FAILURE'
                                throw e
                            }
                        }
                    }
                }
            }
        }

        // 并行执行所有构建任务
        parallel buildTasks
    }

    // ==================== 阶段 3: 并行测试变更的服务 ====================
    stage('Test Changed Services') {
        if (changedServices.isEmpty()) {
            return
        }

        def testTasks = [:]

        changedServices.each { service ->
            testTasks["测试 ${service}"] = {
                node('test-node') {
                    stage("Test ${service}") {
                        dir("services/${service}") {
                            echo "测试 ${service} 服务..."

                            try {
                                // 单元测试
                                if (fileExists('pom.xml')) {
                                    sh 'mvn test'
                                    junit 'target/surefire-reports/*.xml'
                                } else if (fileExists('package.json')) {
                                    sh 'npm run test:unit'
                                    junit 'test-results/*.xml'
                                }

                                // 集成测试（仅主分支）
                                if (env.BRANCH_NAME == 'main') {
                                    if (fileExists('pom.xml')) {
                                        sh 'mvn verify'
                                    } else if (fileExists('package.json')) {
                                        sh 'npm run test:integration'
                                    }
                                }

                                echo "${service} 测试通过"

                            } catch (Exception e) {
                                echo "${service} 测试失败: ${e.message}"
                                currentBuild.result = 'UNSTABLE'
                            }
                        }
                    }
                }
            }
        }

        // 并行执行所有测试任务
        parallel testTasks
    }

    // ==================== 阶段 4: 并行 Docker 构建 ====================
    stage('Docker Build') {
        if (changedServices.isEmpty()) {
            return
        }

        // 仅在主分支或开发分支构建镜像
        if (!(env.BRANCH_NAME in ['main', 'develop'])) {
            echo '特性分支跳过 Docker 构建'
            return
        }

        def dockerTasks = [:]

        changedServices.each { service ->
            dockerTasks["Docker ${service}"] = {
                node('docker') {
                    stage("Docker Build ${service}") {
                        def imageName = "myregistry/${projectName}-${service}:${env.BUILD_NUMBER}"
                        dockerImages[service] = imageName

                        dir("services/${service}") {
                            echo "构建 ${service} Docker 镜像..."

                            sh """
                                docker build -t ${imageName} .
                                docker tag ${imageName} myregistry/${projectName}-${service}:latest
                            """

                            echo "${service} Docker 镜像构建完成"
                        }
                    }
                }
            }
        }

        // 并行执行所有 Docker 构建
        parallel dockerTasks
    }

    // ==================== 阶段 5: 并行推送镜像 ====================
    stage('Push Docker Images') {
        if (dockerImages.isEmpty()) {
            return
        }

        withCredentials([
            usernamePassword(
                credentialsId: 'docker-registry',
                usernameVariable: 'DOCKER_USER',
                passwordVariable: 'DOCKER_PASS'
            )
        ]) {
            def pushTasks = [:]

            dockerImages.each { service, imageName ->
                pushTasks["推送 ${service}"] = {
                    sh """
                        echo \${DOCKER_PASS} | docker login -u \${DOCKER_USER} --password-stdin myregistry
                        docker push ${imageName}
                        docker push myregistry/${projectName}-${service}:latest
                    """
                }
            }

            // 并行推送所有镜像
            parallel pushTasks
        }

        echo '所有镜像推送完成'
    }

    // ==================== 阶段 6: 并行部署服务 ====================
    stage('Deploy Services') {
        if (dockerImages.isEmpty()) {
            return
        }

        if (env.BRANCH_NAME == 'main') {
            echo '部署到生产环境...'

            withCredentials([
                file(credentialsId: 'k8s-config', variable: 'KUBECONFIG')
            ]) {
                def deployTasks = [:]

                dockerImages.each { service, imageName ->
                    deployTasks["部署 ${service}"] = {
                        sh """
                            export KUBECONFIG=${KUBECONFIG}
                            kubectl set image deployment/${service} \
                                ${service}=myregistry/${projectName}-${service}:${env.BUILD_NUMBER} \
                                --namespace=production
                            kubectl rollout status deployment/${service} \
                                --namespace=production
                        """
                    }
                }

                // 并行部署所有服务
                parallel deployTasks
            }

            echo '生产部署完成'

        } else if (env.BRANCH_NAME == 'develop') {
            echo '部署到开发环境...'

            withCredentials([
                file(credentialsId: 'k8s-config-dev', variable: 'KUBECONFIG')
            ]) {
                def deployTasks = [:]

                dockerImages.each { service, imageName ->
                    deployTasks["部署 ${service}"] = {
                        sh """
                            export KUBECONFIG=${KUBECONFIG}
                            kubectl set image deployment/${service} \
                                ${service}=myregistry/${projectName}-${service}:${env.BUILD_NUMBER} \
                                --namespace=development
                            kubectl rollout status deployment/${service} \
                                --namespace=development
                        """
                    }
                }

                // 并行部署所有服务
                parallel deployTasks
            }

            echo '开发部署完成'
        }
    }

    // ==================== 阶段 7: 验证部署 ====================
    stage('Verify Deployments') {
        if (env.BRANCH_NAME in ['main', 'develop'] && !changedServices.isEmpty()) {
            def namespace = env.BRANCH_NAME == 'main' ? 'production' : 'development'

            echo "验证 ${namespace} 环境部署..."

            retry(5) {
                sleep 10
                changedServices.each { service ->
                    sh "kubectl get pods -n ${namespace} -l app=${service}"
                }
            }

            echo '部署验证完成'
        }
    }

    // ==================== 清理 ====================
    stage('Cleanup') {
        echo '清理 Docker 镜像...'
        dockerImages.each { service, imageName ->
            sh "docker rmi ${imageName} || true"
        }

        echo '清理工作空间...'
        cleanWs()

        echo "=== ${projectName} Monorepo CI/CD 完成 ==="
    }
}

// ==================== 变更检测增强（可选）====================
/*
// 使用 GitHub API 获取变更文件（适用于 PR 构建）
stage('Detect Changes via GitHub API') {
    withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
        def prNumber = env.CHANGE_ID
        def changedFilesJson = sh(
            script: """
                curl -H "Authorization: token \${GITHUB_TOKEN}" \
                "https://api.github.com/repos/user/repo/pulls/${prNumber}/files" \
                | jq -r '.[].filename'
            """,
            returnStdout: true
        ).trim()

        def changedFiles = changedFilesJson.split('\n')
        // 处理变更文件...
    }
}
*/
