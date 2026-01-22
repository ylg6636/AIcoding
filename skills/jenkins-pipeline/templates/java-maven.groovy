// Java Maven 项目 - 脚本式 Jenkinsfile 模板
// 适用于使用 Maven 构建的 Java 项目

node('maven') {
    // ==================== 环境变量 ====================
    def appName = 'java-app'
    def mavenOpts = '-Dmaven.test.failure.ignore=false'
    def dockerImage = ""

    echo "=== ${appName} 构建开始 ==="

    // ==================== 阶段 1: 代码检出 ====================
    stage('Checkout') {
        echo '检出源代码...'
        checkout scm
        sh "git rev-parse HEAD > .git/commit-id"
        def commitId = readFile('.git/commit-id').trim()
        echo "当前提交: ${commitId}"
    }

    // ==================== 阶段 2: 依赖分析 ====================
    stage('Dependency Check') {
        echo '检查依赖更新...'
        sh "mvn versions:display-dependency-updates ${mavenOpts}"
    }

    // ==================== 阶段 3: 编译 ====================
    stage('Compile') {
        echo '编译项目...'
        sh "mvn clean compile ${mavenOpts}"
    }

    // ==================== 阶段 4: 测试 ====================
    stage('Test') {
        echo '运行单元测试...'

        try {
            // 运行测试并生成报告
            sh "mvn test ${mavenOpts}"

            // 发布测试结果
            junit 'target/surefire-reports/*.xml'

            // 发布代码覆盖率
            // jacoco execPattern: 'target/jacoco.exec'

            echo '测试完成'
        } catch (Exception e) {
            echo "测试失败: ${e.message}"
            currentBuild.result = 'UNSTABLE'
        }
    }

    // ==================== 阶段 5: 代码质量检查 ====================
    stage('Code Quality') {
        echo '运行代码质量检查...'

        try {
            // SonarQube 分析
            // withSonarQubeEnv('sonar-server') {
            //     sh "mvn sonar:sonar ${mavenOpts}"
            // }

            // 或使用 Checkstyle
            sh "mvn checkstyle:check ${mavenOpts}"

            echo '质量检查完成'
        } catch (Exception e) {
            echo "质量检查警告: ${e.message}"
            // 不中断构建
        }
    }

    // ==================== 阶段 6: 打包 ====================
    stage('Package') {
        echo '打包应用...'
        sh "mvn package -DskipTests ${mavenOpts}"

        // 归档构建产物
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true

        echo '打包完成'
    }

    // ==================== 阶段 7: Docker 构建（可选）====================
    stage('Docker Build') {
        if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop') {
            echo '构建 Docker 镜像...'

            def dockerImageName = "myregistry/${appName}:${env.BUILD_NUMBER}"
            dockerImage = dockerImageName

            // 构建 Docker 镜像
            sh """
                docker build -t ${dockerImageName} .
                docker tag ${dockerImageName} myregistry/${appName}:latest
            """

            echo 'Docker 镜像构建完成'
        } else {
            echo '跳过 Docker 构建（仅 main/develop 分支）'
        }
    }

    // ==================== 阶段 8: 部署 ====================
    stage('Deploy') {
        if (env.BRANCH_NAME == 'main') {
            // 生产环境部署
            echo '部署到生产环境...'

            withCredentials([
                usernamePassword(
                    credentialsId: 'docker-registry',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )
            ]) {
                sh """
                    echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin myregistry
                    docker push myregistry/${appName}:${env.BUILD_NUMBER}
                    docker push myregistry/${appName}:latest
                """
            }

            // 触发 Kubernetes 更新
            // sh 'kubectl set image deployment/myapp myapp=myregistry/${appName}:${env.BUILD_NUMBER}'

            echo '生产部署完成'

        } else if (env.BRANCH_NAME == 'develop') {
            // 开发环境部署
            echo '部署到开发环境...'

            sh """
                docker push myregistry/${appName}:${env.BUILD_NUMBER}
            """

            echo '开发部署完成'

        } else {
            echo '跳过部署（特性分支）'
        }
    }

    // ==================== 清理 ====================
    stage('Cleanup') {
        echo '清理 Docker 镜像...'
        sh "docker rmi ${dockerImage} || true"

        echo '清理工作空间...'
        cleanWs()

        echo "=== ${appName} 构建完成 ==="
    }
}
