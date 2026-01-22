---
name: jenkins-pipeline
description: 专门用于创建和管理 Jenkins 脚本式流水线（Scripted Pipeline）。提供完整语法参考、最佳实践、常用模板和验证工具。当用户提及"Jenkins"、"Jenkinsfile"、"流水线"、"CI/CD"、"pipeline"等关键词时激活。
---

# Jenkins 脚本式流水线管理

创建符合最佳实践的 Jenkins 脚本式流水线（Scripted Pipeline）。

## 触发关键词

- Jenkins / Jenkinsfile / 流水线 / pipeline / CI/CD
- 脚本式流水线 / scripted pipeline
- 构建流水线 / 部署流水线

## 核心概念

### 脚本式 vs 声明式

| 特性 | 脚本式 (Scripted) | 声明式 (Declarative) |
|------|-------------------|---------------------|
| 语法 | Groovy DSL | 限制性块结构 |
| 灵活性 | 高（编程式） | 低（结构化） |
| 适用场景 | 复杂逻辑、条件执行 | 简单标准流程 |
| **本 Skill 聚焦** | ✅ 脚本式 | ❌ |

### 基本结构

```groovy
node {
    // 1. 环境准备
    stage('Checkout') {
        // 代码检出
    }

    // 2. 构建
    stage('Build') {
        // 构建步骤
    }

    // 3. 测试
    stage('Test') {
        // 测试步骤
    }

    // 4. 部署
    stage('Deploy') {
        // 部署步骤
    }
}
```

## 快速开始

### 创建新的 Jenkinsfile

```
创建一个名为 my-app 的 Jenkinsfile，使用 Maven 构建
```

### 验证现有 Jenkinsfile

```
验证我的 Jenkinsfile 是否符合脚本式规范
```

## 语法参考

### 核心元素

#### 1. node 块

```groovy
node('label') {
    // 在指定 agent 上执行
}
```

#### 2. stage 块

```groovy
stage('Stage Name') {
    // 阶段步骤
}
```

#### 3. 常用步骤

```groovy
// 代码检出
checkout scm

// 或使用 git
git 'https://github.com/user/repo.git'

// Shell 命令
sh 'make build'

// Windows batch
bat 'build.bat'

// 设置环境变量
withEnv(['PATH+TOOLS=/opt/tools']) {
    sh 'command'
}
```

### 环境变量

```groovy
// 定义环境变量
env.VAR_NAME = 'value'

// 使用环境变量
sh "echo ${env.VAR_NAME}"

// 凭证处理
withCredentials([usernamePassword(
    credentialsId: 'my-creds',
    usernameVariable: 'USER',
    passwordVariable: 'PASS'
)]) {
    sh "login ${USER} ${PASS}"
}
```

### 条件执行

```groovy
// if/else
if (env.BRANCH_NAME == 'main') {
    stage('Production Deploy') {
        // 生产部署
    }
} else {
    stage('Dev Deploy') {
        // 开发部署
    }
}

// try/catch 错误处理
try {
    stage('Build') {
        sh 'make build'
    }
} catch (Exception e) {
    currentBuild.result = 'FAILURE'
    throw e
} finally {
    // 清理操作
    cleanWs()
}
```

### 并行执行

```groovy
parallel(
    "单元测试": {
        stage('Unit Tests') {
            sh 'mvn test'
        }
    },
    "静态分析": {
        stage('Static Analysis') {
            sh 'mvn checkstyle:check'
        }
    }
)
```

### 循环迭代

```groovy
// 多平台构建
def platforms = ['linux', 'windows', 'macos']
platforms.each { platform ->
    stage("Build ${platform}") {
        sh "./build.sh ${platform}"
    }
}
```

## 常用模式

### 1. 参数化构建

```groovy
properties([
    parameters([
        string(name: 'VERSION', defaultValue: '1.0.0'),
        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod']),
        booleanParam(name: 'SKIP_TESTS', defaultValue: false)
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

    stage("Deploy to ${params.ENVIRONMENT}") {
        // 部署逻辑
    }
}
```

### 2. 超时控制

```groovy
timeout(time: 30, unit: 'MINUTES') {
    node {
        stage('Build') {
            sh 'mvn package'
        }
    }
}
```

### 3. 重试机制

```groovy
retry(3) {
    stage('Download Dependencies') {
        sh 'mvn dependency:resolve'
    }
}
```

### 4. 通知

```groovy
node {
    try {
        // 构建步骤
    } finally {
        emailext(
            subject: "Build ${currentBuild.result}: ${env.JOB_NAME}",
            body: "Build details: ${env.BUILD_URL}",
            to: 'team@example.com'
        )
    }
}
```

## 最佳实践

### ✅ 推荐做法

1. **使用 node 块包装所有操作**
   ```groovy
   node {
       // 所有步骤在 node 内
   }
   ```

2. **使用 withCredentials 处理敏感信息**
   ```groovy
   withCredentials([string(credentialsId: 'api-key', variable: 'KEY')]) {
       sh "curl -H 'Authorization: ${KEY}' api.example.com"
   }
   ```

3. **添加错误处理**
   ```groovy
   try {
       // 操作
   } catch (e) {
       currentBuild.result = 'FAILURE'
       throw e
   }
   ```

4. **使用共享库**（→ 详见 [shared-libraries](references/shared-libraries.md)）

5. **保持幂等性** - 流水线可安全重复执行

6. **使用超时控制** - 防止流水线挂起

### ❌ 避免做法

1. **不要在 node 外执行操作**
2. **不要硬编码敏感信息** - 使用 credentials
3. **不要忽略错误处理**
4. **不要使用过长的 Groovy 代码** - 移到共享库

## 模板库

### 基础模板

→ 使用 `templates/basic-scripted.groovy`

### Java Maven 模板

→ 使用 `templates/java-maven.groovy`

### Node.js 模板

→ 使用 `templates/nodejs.groovy`

### Docker 构建部署模板

→ 使用 `templates/docker-deploy.groovy`

### 多阶段并行模板

→ 使用 `templates/parallel-stages.groovy`

## 验证工具

### Jenkinsfile 语法检查

```bash
# 使用 Jenkins 在线验证
# 访问：Jenkins > Pipeline > Pipeline Syntax

# 或使用本地工具（如安装了 Jenkins linter）
java -jar jenkins-cli.jar -s http://jenkins-server declarative-linter < Jenkinsfile
```

### 最佳实践检查

→ 使用本 skill 提供的检查清单

## 故障排除

### 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `GroovyCastException` | 类型转换错误 | 检查变量类型，使用显式转换 |
| `NullPointerException` | 未初始化变量 | 添加 null 检查或默认值 |
| `org.jenkinsci.plugins.scriptsecurity.scripts.*` | 脚本安全限制 | 在 "Script Approval" 中批准 |
| `hudson.AbortException: script returned exit code` | Shell 命令失败 | 添加错误处理或 `|| true` |

### 调试技巧

```groovy
// 打印变量
echo "DEBUG: var = ${var}"

// 打印环境变量
sh 'printenv'

// 查看当前构建信息
echo "Build: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
```

## 检查清单

创建新的 Jenkinsfile 时，确保：

- [ ] 使用 `node {}` 包装所有内容
- [ ] 每个 stage 有明确的名称
- [ ] 添加错误处理（try/catch）
- [ ] 敏感信息使用 credentials
- [ ] 长时间操作添加超时控制
- [ ] 可重试的操作添加 retry
- [ ] 遵循项目命名规范
- [ ] 添加适当的注释

## 参考资料

- [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Pipeline Best Practices](https://www.jenkins.io/doc/book/pipeline/pipeline-best-practices/)
- [Pipeline Examples](https://www.jenkins.io/doc/pipeline/examples/)
- [Pipeline: Basic Steps](https://www.jenkins.io/doc/pipeline/steps/workflow-basic-steps/)
