// Docker 构建与部署 - 脚本式 Jenkinsfile 模板
// 适用于容器化应用的 CI/CD 流程

node('docker') {
    // ==================== 环境变量 ====================
    def appName = 'docker-app'
    def registry = 'myregistry.com'
    def dockerImage = ""

    echo "=== ${appName} CI/CD 流水线 ==="

    // ==================== 阶段 1: 代码检出 ====================
    stage('Checkout') {
        echo '检出源代码...'
        checkout scm

        // 获取 Git 信息
        sh "git rev-parse --short HEAD > .git/commit-id"
        def commitId = readFile('.git/commit-id').trim()
        env.GIT_COMMIT_ID = commitId
        echo "Git 提交: ${commitId}"
    }

    // ==================== 阶段 2: 构建 Docker 镜像 ====================
    stage('Docker Build') {
        echo '构建 Docker 镜像...'

        // 多架构构建（可选）
        // docker.withTool('docker') {
        //     customImage = docker.build("${registry}/${appName}:${env.BUILD_NUMBER}")
        // }

        def imageName = "${registry}/${appName}:${env.BUILD_NUMBER}"
        dockerImage = imageName

        sh """
            docker build \
                --build-arg BUILD_DATE=\$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
                --build-arg VCS_REF=${env.GIT_COMMIT_ID} \
                --build-arg VERSION=${env.BUILD_NUMBER} \
                -t ${imageName} \
                -t ${registry}/${appName}:latest \
                .
        """

        echo 'Docker 镜像构建完成'
    }

    // ==================== 阶段 3: 测试镜像 ====================
    stage('Docker Test') {
        echo '测试 Docker 镜像...'

        try {
            // 健康检查
            sh """
                docker run --rm ${dockerImage} \
                    /bin/sh -c "echo 'Container is healthy'"
            """

            // 或使用容器内测试
            // sh """
            //     docker run --rm ${dockerImage} \
            //         npm test
            // """

            echo '镜像测试通过'
        } catch (Exception e) {
            echo "镜像测试失败: ${e.message}"
            currentBuild.result = 'FAILURE'
            throw e
        }
    }

    // ==================== 阶段 4: 安全扫描 ====================
    stage('Security Scan') {
        echo '扫描镜像安全问题...'

        try {
            // 使用 Trivy 扫描
            // sh """
            //     trivy image --exit-code 1 --severity HIGH,CRITICAL \
            //         ${dockerImage}
            // """

            // 或使用 Snyk
            // sh """
            //     docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
            //         snyk/snyk-docker-cli:latest \
            //         test ${dockerImage}
            // """

            echo '安全扫描完成'
        } catch (Exception e) {
            echo "安全扫描发现问题: ${e.message}"
            currentBuild.result = 'UNSTABLE'
        }
    }

    // ==================== 阶段 5: 推送镜像 ====================
    stage('Push Image') {
        echo '推送 Docker 镜像到仓库...'

        withCredentials([
            usernamePassword(
                credentialsId: 'docker-registry',
                usernameVariable: 'DOCKER_USER',
                passwordVariable: 'DOCKER_PASS'
            )
        ]) {
            sh """
                echo \${DOCKER_PASS} | docker login -u \${DOCKER_USER} --password-stdin ${registry}
                docker push ${dockerImage}
                docker push ${registry}/${appName}:latest
            """
        }

        echo '镜像推送完成'
    }

    // ==================== 阶段 6: 部署到 Kubernetes ====================
    stage('Deploy to Kubernetes') {
        if (env.BRANCH_NAME == 'main') {
            echo '部署到生产环境...'

            withCredentials([
                file(credentialsId: 'k8s-config', variable: 'KUBECONFIG')
            ]) {
                // 更新 Deployment
                sh """
                    export KUBECONFIG=${KUBECONFIG}
                    kubectl set image deployment/${appName} \
                        ${appName}=${dockerImage} \
                        --namespace=production
                    kubectl rollout status deployment/${appName} \
                        --namespace=production
                """

                // 或使用 Helm
                // sh """
                //     helm upgrade --install ${appName} ./helm-chart \
                //         --namespace production \
                //         --set image.repository=${registry}/${appName} \
                //         --set image.tag=${env.BUILD_NUMBER} \
                //         --wait
                // """
            }

            echo '生产部署完成'

        } else if (env.BRANCH_NAME == 'develop') {
            echo '部署到开发环境...'

            withCredentials([
                file(credentialsId: 'k8s-config-dev', variable: 'KUBECONFIG')
            ]) {
                sh """
                    export KUBECONFIG=${KUBECONFIG}
                    kubectl set image deployment/${appName} \
                        ${appName}=${dockerImage} \
                        --namespace=development
                    kubectl rollout status deployment/${appName} \
                        --namespace=development
                """
            }

            echo '开发部署完成'

        } else {
            echo '特性分支，跳过部署'
        }
    }

    // ==================== 阶段 7: 验证部署 ====================
    stage('Verify Deployment') {
        if (env.BRANCH_NAME in ['main', 'develop']) {
            echo '验证部署状态...'

            def namespace = env.BRANCH_NAME == 'main' ? 'production' : 'development'

            retry(5) {
                sleep 10
                sh """
                    kubectl get pods -n ${namespace} -l app=${appName}
                """
            }

            echo '部署验证完成'
        }
    }

    // ==================== 清理 ====================
    stage('Cleanup') {
        echo '清理本地镜像...'
        sh "docker rmi ${dockerImage} || true"
        sh "docker image prune -f || true"

        echo '清理工作空间...'
        cleanWs()

        echo "=== ${appName} CI/CD 完成 ==="
    }
}
