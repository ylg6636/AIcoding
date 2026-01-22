// 基础脚本式 Jenkinsfile 模板
// 适用于简单的构建-测试-部署流程

node {
    // ==================== 环境变量 ====================
    // 设置项目特定的环境变量
    def projectName = 'my-project'
    def buildVersion = "${env.BUILD_NUMBER}"

    echo "开始构建: ${projectName} v${buildVersion}"

    // ==================== 阶段 1: 代码检出 ====================
    stage('Checkout') {
        echo '检出源代码...'
        checkout scm

        // 或者使用 git 直接检出
        // git branch: 'main', url: 'https://github.com/user/repo.git'

        echo "当前分支: ${env.BRANCH_NAME}"
        echo "当前提交: ${env.GIT_COMMIT}"
    }

    // ==================== 阶段 2: 构建 ====================
    stage('Build') {
        echo '开始构建...'

        try {
            // 示例：使用 shell 命令构建
            sh './build.sh'

            // 或使用 make
            // sh 'make build'

            echo '构建成功'
        } catch (Exception e) {
            echo "构建失败: ${e.message}"
            currentBuild.result = 'FAILURE'
            throw e
        }
    }

    // ==================== 阶段 3: 测试 ====================
    stage('Test') {
        echo '运行测试...'

        try {
            // 示例：运行测试套件
            sh './run-tests.sh'

            // 或使用测试框架
            // sh 'mvn test'
            // sh 'npm test'

            echo '测试通过'
        } catch (Exception e) {
            echo "测试失败: ${e.message}"
            currentBuild.result = 'FAILURE'
            throw e
        }
    }

    // ==================== 阶段 4: 部署（可选）====================
    stage('Deploy') {
        // 仅在主分支部署
        if (env.BRANCH_NAME == 'main') {
            echo '部署到生产环境...'

            try {
                // 部署命令
                sh './deploy.sh production'

                echo '部署成功'
            } catch (Exception e) {
                echo "部署失败: ${e.message}"
                currentBuild.result = 'FAILURE'
                throw e
            }
        } else {
            echo '跳过部署（非主分支）'
        }
    }

    // ==================== 清理 ====================
    stage('Cleanup') {
        echo '清理工作空间...'
        cleanWs()
    }

    echo "构建 ${projectName} v${buildVersion} 完成!"
}
