# Jenkins 脚本式流水线最佳实践

遵循这些最佳实践来创建健壮、可维护的 Jenkins 流水线。

## 核心原则

### 1. Pipeline as Code

✅ 将 Jenkinsfile 存储在代码仓库中
✅ 通过代码审查管理流水线变更
✅ 与应用代码一起版本控制

```groovy
// good
// Jenkinsfile 在仓库根目录
node {
    checkout scm
    // ...
}
```

❌ 避免在 Jenkins UI 中配置流水线

### 2. 幂等性

流水线应该可以安全地重复执行，不产生副作用。

```groovy
// good
stage('Deploy') {
    sh '''
        if ! kubectl get deployment myapp; then
            kubectl create deployment myapp --image=nginx
        else
            kubectl set image deployment myapp nginx
        fi
    '''
}
```

### 3. 失败快速反馈

尽早发现问题，不要等待到最后。

```groovy
// good
stage('Pre-flight Checks') {
    sh 'npm --version'
    sh 'node --version'
    sh './check-environment.sh'
}

node {
    stage('Checkout') { checkout scm }
    stage('Install') { sh 'npm ci' }
    stage('Lint') { sh 'npm run lint' }      // 早期检查
    stage('Test') { sh 'npm test' }          // 然后测试
    stage('Build') { sh 'npm run build' }    // 最后构建
}
```

## 错误处理

### 使用 try/catch/finally

```groovy
node {
    try {
        stage('Build') {
            sh './build.sh'
        }
    } catch (Exception e) {
        currentBuild.result = 'FAILURE'
        // 发送失败通知
        emailext subject: 'Build Failed', body: e.message, to: 'team@example.com'
        throw e
    } finally {
        // 清理资源
        cleanWs()
    }
}
```

### 设置超时

```groovy
timeout(time: 30, unit: 'MINUTES') {
    node {
        stage('Build') {
            sh './build.sh'
        }
    }
}
```

### 添加重试

```groovy
retry(3) {
    stage('Download Dependencies') {
        sh 'npm ci'
    }
}
```

## 凭证管理

### 使用 withCredentials

```groovy
// good
withCredentials([usernamePassword(
    credentialsId: 'docker-registry',
    usernameVariable: 'DOCKER_USER',
    passwordVariable: 'DOCKER_PASS'
)]) {
    sh """
        echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin
    """
}

// ❌ 不好 - 硬编码凭证
sh 'echo password123 | docker login -u user'
```

### 使用 secret text

```groovy
withCredentials([string(
    credentialsId: 'api-key',
    variable: 'API_KEY'
)]) {
    sh "curl -H 'Authorization: ${API_KEY}' api.example.com"
}
```

## 并行执行

使用并行执行来加快构建速度。

```groovy
parallel(
    "单元测试": {
        stage('Unit Tests') {
            sh 'npm run test:unit'
        }
    },
    "静态分析": {
        stage('Static Analysis') {
            sh 'npm run lint'
        }
    },
    failFast: false  // 不要在一个任务失败时立即取消其他任务
)
```

## 资源清理

### 清理工作空间

```groovy
node {
    try {
        stage('Build') {
            sh './build.sh'
        }
    } finally {
        cleanWs()  // 清理工作空间
    }
}
```

### 清理 Docker 镜像

```groovy
stage('Cleanup Docker') {
    sh 'docker system prune -f'
    sh 'docker image prune -a -f'
}
```

## 环境隔离

### 使用不同的 Agent

```groovy
// Linux 构建
node('linux') {
    stage('Linux Build') {
        sh './build.sh linux'
    }
}

// Windows 构建
node('windows') {
    stage('Windows Build') {
        bat 'build.bat'
    }
}
```

### 使用 Docker 容器

```groovy
node {
    docker.image('node:18').inside {
        stage('Build') {
            sh 'npm run build'
        }
    }
}
```

## 参数化

### 使用参数使流水线更灵活

```groovy
properties([
    parameters([
        string(name: 'VERSION', defaultValue: '1.0.0', description: 'Build version'),
        choice(name: 'ENV', choices: ['dev', 'staging', 'prod'], description: 'Target environment'),
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip tests')
    ])
])

node {
    stage('Build') {
        sh "mvn package -Dversion=${params.VERSION}"
    }

    if (!params.SKIP_TESTS) {
        stage('Test') {
            sh 'mvn test'
        }
    }

    stage("Deploy to ${params.ENV}") {
        sh "./deploy.sh ${params.ENV}"
    }
}
```

## 通知

### 发送构建状态通知

```groovy
node {
    try {
        // 构建步骤
        currentBuild.result = 'SUCCESS'
    } catch (e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
        // 通知
        emailext(
            subject: "Build ${currentBuild.result}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: """
                Status: ${currentBuild.result}
                Job: ${env.JOB_NAME}
                Build: ${env.BUILD_NUMBER}
                URL: ${env.BUILD_URL}
            """,
            to: 'team@example.com',
            mimeType: 'text/html'
        )

        // Slack 通知
        // slackSend(
        //     color: currentBuild.result == 'SUCCESS' ? 'good' : 'danger',
        //     message: "Build ${currentBuild.result}: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
        // )
    }
}
```

## 性能优化

### 使用轻量级节点

```groovy
// 对于不需要构建工具的步骤
node {
    stage('SCM Checkout') {
        checkout scm
    }
}

// 只有需要构建时才使用重量级节点
node('build-node') {
    stage('Build') {
        sh 'mvn package'
    }
}
```

### 缓存依赖

```groovy
stage('Install Dependencies') {
    // 使用缓存加速
    sh 'npm ci --prefer-offline --no-audit'
}
```

## 安全

### 不要在日志中打印敏感信息

```groovy
// ❌ 不好
echo "Using password: ${PASSWORD}"

// ✅ 好
echo "Using credentials from vault"
```

### 限制脚本权限

使用 "Script Approval" 管理允许的脚本操作。

## 文档

### 添加注释

```groovy
// 阶段：构建应用
// 使用 Maven 编译并打包
stage('Build') {
    // 设置 Maven 选项
    def mavenOpts = '-Dmaven.test.failure.ignore=false'

    // 执行构建
    sh "mvn package ${mavenOpts}"
}
```

## 参考资料

- [Jenkins Pipeline Best Practices](https://www.jenkins.io/doc/book/pipeline/pipeline-best-practices/)
- [Pipeline Examples](https://www.jenkins.io/doc/pipeline/examples/)
- [Shared Libraries Best Practices](https://www.jenkins.io/doc/book/pipeline/shared-libraries/)
