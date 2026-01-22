// Node.js 项目 - 脚本式 Jenkinsfile 模板
// 适用于使用 Node.js/npm 的前端或后端项目

node('nodejs') {
    // ==================== 环境变量 ====================
    def appName = 'nodejs-app'
    def nodeVersion = '18'

    echo "=== ${appName} 构建开始 ==="

    // ==================== 阶段 1: 代码检出 ====================
    stage('Checkout') {
        echo '检出源代码...'
        checkout scm

        // 读取 package.json 版本
        def packageJson = readJSON file: 'package.json'
        echo "应用版本: ${packageJson.version}"
    }

    // ==================== 阶段 2: 安装依赖 ====================
    stage('Install Dependencies') {
        echo '安装 Node.js 依赖...'

        // 使用 npm cache 加速构建
        sh '''
            echo "Node 版本: $(node --version)"
            echo "npm 版本: $(npm --version)"
            npm ci --prefer-offline --no-audit
        '''

        echo '依赖安装完成'
    }

    // ==================== 阶段 3: 代码检查 ====================
    stage('Lint') {
        echo '运行代码检查...'

        try {
            sh 'npm run lint'

            echo '代码检查通过'
        } catch (Exception e) {
            echo "代码检查失败: ${e.message}"
            currentBuild.result = 'UNSTABLE'
        }
    }

    // ==================== 阶段 4: 单元测试 ====================
    stage('Unit Tests') {
        echo '运行单元测试...'

        try {
            // 运行测试并生成覆盖率报告
            sh 'npm run test:coverage'

            // 发布测试结果（需要生成 JUnit 格式）
            // junit 'coverage/junit.xml'

            // 发布覆盖率报告
            // publishHTML([
            //     reportDir: 'coverage',
            //     reportFiles: 'index.html',
            //     reportName: 'Coverage Report'
            // ])

            echo '单元测试完成'
        } catch (Exception e) {
            echo "单元测试失败: ${e.message}"
            currentBuild.result = 'FAILURE'
            throw e
        }
    }

    // ==================== 阶段 5: 构建 ====================
    stage('Build') {
        echo '构建应用...'

        try {
            // 生产构建
            sh 'npm run build'

            echo '构建成功'
        } catch (Exception e) {
            echo "构建失败: ${e.message}"
            currentBuild.result = 'FAILURE'
            throw e
        }
    }

    // ==================== 阶段 6: E2E 测试（可选）====================
    stage('E2E Tests') {
        if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop') {
            echo '运行端到端测试...'

            try {
                // 启动应用
                sh 'npm run start &'
                sleep 10

                // 运行 E2E 测试
                sh 'npm run test:e2e'

                echo 'E2E 测试完成'
            } catch (Exception e) {
                echo "E2E 测试失败: ${e.message}"
                currentBuild.result = 'UNSTABLE'
            } finally {
                sh 'pkill -f "npm run start" || true'
            }
        } else {
            echo '跳过 E2E 测试（仅 main/develop 分支）'
        }
    }

    // ==================== 阶段 7: Docker 构建 ====================
    stage('Docker Build') {
        echo '构建 Docker 镜像...'

        def dockerImageName = "myregistry/${appName}:${env.BUILD_NUMBER}"

        sh """
            docker build -t ${dockerImageName} .
            docker tag ${dockerImageName} myregistry/${appName}:latest
        """

        echo 'Docker 镜像构建完成'
    }

    // ==================== 阶段 8: 部署 ====================
    stage('Deploy') {
        if (env.BRANCH_NAME == 'main') {
            echo '部署到生产环境...'

            withCredentials([
                usernamePassword(
                    credentialsId: 'docker-registry',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )
            ]) {
                sh """
                    echo \${DOCKER_PASS} | docker login -u \${DOCKER_USER} --password-stdin myregistry
                    docker push myregistry/${appName}:${env.BUILD_NUMBER}
                    docker push myregistry/${appName}:latest
                """
            }

            // 触发部署（根据您的部署方式）
            // sh 'kubectl set image deployment/nodejs-app app=myregistry/${appName}:${env.BUILD_NUMBER}'
            // 或
            // sh 'helm upgrade --install nodejs-app ./helm-chart --set image.tag=${env.BUILD_NUMBER}'

            echo '生产部署完成'

        } else if (env.BRANCH_NAME == 'develop') {
            echo '部署到开发环境...'

            withCredentials([
                usernamePassword(
                    credentialsId: 'docker-registry',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )
            ]) {
                sh """
                    echo \${DOCKER_PASS} | docker login -u \${DOCKER_USER} --password-stdin myregistry
                    docker push myregistry/${appName}:${env.BUILD_NUMBER}
                """
            }

            echo '开发部署完成'

        } else {
            echo '特性分支，跳过部署'
        }
    }

    // ==================== 清理 ====================
    stage('Cleanup') {
        echo '清理工作空间...'

        // 清理 node_modules 以节省空间
        // sh 'rm -rf node_modules'

        cleanWs()

        echo "=== ${appName} 构建完成 ==="
    }
}
