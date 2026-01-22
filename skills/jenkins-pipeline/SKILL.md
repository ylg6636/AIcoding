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

## 专注领域 (Focus Areas)

本 Skill 专注于以下 Jenkins CI/CD 领域：

- ✅ **Jenkins Pipeline 创建和优化** - 脚本式流水线设计与实现
- ✅ **Jenkinsfile 语法和最佳实践** - 符合规范的语法和模式
- ✅ **CI/CD 工作流自动化** - 自动化构建、测试、部署流程
- ✅ **插件管理和自定义** - Jenkins 插件配置和管理
- ✅ **构建触发器和作业调度** - Webhook、定时任务等触发方式
- ✅ **外部工具和服务集成** - Git、Docker、Kubernetes 等集成
- ✅ **安全和访问控制** - 凭证管理、权限配置
- ✅ **Agent 和节点配置** - 分布式构建节点管理
- ✅ **制品管理和归档** - 构建产物存储和版本管理
- ✅ **监控和日志记录** - Jenkins 活动监控和日志分析

## 实施方法 (Approach)

### 核心原则

1. **使用脚本式 Pipeline 提供灵活性**
   - 适用于复杂逻辑和条件执行
   - 支持完整的 Groovy 编程能力
   - 适合需要动态决策的场景

2. **将 Jenkinsfile 模块化为共享库**
   - 提取可重用的代码片段
   - 创建标准化函数
   - 简化流水线维护

3. **利用 Jenkins 可视化工具**
   - 使用 Blue Ocean 查看流水线
   - 监控构建状态和趋势

4. **自动化插件更新和备份**
   - 定期更新插件到安全版本
   - 备份 Jenkins 配置和作业

5. **使用 Jenkins 凭证管理密钥**
   - 永不硬编码敏感信息
   - 使用 credentials 存储密码和令牌

6. **配置并行阶段提高效率**
   - 识别可并行执行的任务
   - 使用 parallel 步骤加速构建

7. **使用 Webhook 实现事件驱动作业**
   - Git push 触发构建
   - PR 合并请求触发测试

8. **实现构建状态通知**
   - 邮件通知
   - Slack/Teams 集成
   - 构建失败告警

9. **持续重构 Jenkins 作业简化**
   - 删除废弃的作业
   - 合并重复的流水线
   - 优化构建步骤

10. **水平扩展 Jenkins 基础设施**
    - 添加更多构建节点
    - 使用容器化 Agent
    - 负载均衡构建任务

## 质量检查清单 (Quality Checklist)

创建或审查 Jenkinsfile 时，验证以下项目：

### 语法和结构

- [ ] **使用 linter 验证 Jenkinsfile 语法**
  - 在 Jenkins UI 中使用 "Pipeline Syntax" 检查
  - 或使用命令行工具：`jenkins-cli declarative-linter < Jenkinsfile`

- [ ] **确保所有作业有适当的触发器**
  - SCM 轮询或 Webhook
  - 定时构建 (cron)
  - 上游作业触发

- [ ] **每个 stage 使用 node 块包装**
  ```groovy
  node('agent-label') {
      stage('Stage Name') {
          // steps
      }
  }
  ```

### 安全和访问控制

- [ ] **定期验证访问控制策略**
  - 检查用户权限
  - 审查 API 令牌
  - 验证代理通信

- [ ] **升级前确认插件兼容性**
  - 查看插件更新日志
  - 在测试环境验证
  - 准备回滚计划

- [ ] **使用 credentials 存储敏感信息**
  ```groovy
  withCredentials([usernamePassword(
      credentialsId: 'my-creds',
      usernameVariable: 'USER',
      passwordVariable: 'PASS'
  )]) {
      // use credentials
  }
  ```

### 测试和验证

- [ ] **在暂存环境测试流水线变更**
  - 不要直接在生产环境修改
  - 使用测试分支验证

- [ ] **监控构建时间回归**
  - 记录构建耗时
  - 识别性能下降
  - 优化慢速步骤

### 运维和维护

- [ ] **执行定期 Jenkins 备份**
  - 配置文件：`JENKINS_HOME`
  - 作业配置
  - 凭证存储

- [ ] **审计 Jenkins 日志查找异常活动**
  - 登录失败
  - 权限变更
  - 异常 API 调用

- [ ] **维护 CI/CD 流程的清晰文档**
  - 流水线用途说明
  - 依赖关系
  - 故障排除步骤

- [ ] **定期审查安全设置**
  - CSRF 保护
  - 代理配置
  - 脚本安全

## 交付物 (Output)

使用本 Skill 创建的内容：

- ✅ 经过验证和测试的 Jenkinsfiles
- ✅ Jenkins 作业定义和配置
- ✅ 自动化部署流水线
- ✅ 安全策略文档
- ✅ 新 Jenkins Agent 的设置指南
- ✅ 故障排除日志和报告
- ✅ 构建产物归档
- ✅ Jenkins 作业性能指标
- ✅ Jenkins 配置合规报告
- ✅ Jenkins 平台用户文档

## 常见陷阱和解决方案 (Common Pitfalls)

### ⚠️ Pitfall 1: Groovy Sandbox 问题

**问题**: Script Security 拒绝执行未批准的方法

**解决方案**:
```groovy
// 使用白名单方法
sh 'echo "approved method"'

// 或在 "Manage Jenkins" → "Script Approval" 中批准签名
// 或使用共享库中的已批准代码
```

### ⚠️ Pitfall 2: 凭证泄露

**问题**: 日志中打印敏感信息

**解决方案**:
```groovy
// ❌ 不好
echo "Using password: ${PASSWORD}"

// ✅ 好
withCredentials([string(credentialsId: 'api-key', variable: 'KEY')]) {
    sh 'curl -H "Authorization: Bearer $KEY" api.example.com'
}
```

### ⚠️ Pitfall 3: 硬编码路径

**问题**: 不同环境路径不同

**解决方案**:
```groovy
// ❌ 不好
sh '/opt/tools/build.sh'

// ✅ 好
def toolPath = env.TOOL_PATH ?: '/usr/local/bin/build.sh'
sh "${toolPath}"
```

### ⚠️ Pitfall 4: 忘记清理资源

**问题**: 工作空间填满磁盘

**解决方案**:
```groovy
try {
    // build steps
} finally {
    cleanWs()  // 清理工作空间
}
```

### ⚠️ Pitfall 5: 并行阶段冲突

**问题**: 并行任务同时写入同一资源

**解决方案**:
```groovy
parallel(
    "task1": {
        lock('resource-name') {
            // 安全访问共享资源
        }
    },
    "task2": {
        lock('resource-name') {
            // 安全访问共享资源
        }
    }
)
```

## 性能优化建议

### 1. 并行化独立任务

```groovy
parallel(
    "单元测试": { sh 'npm run test:unit' },
    "静态分析": { sh 'npm run lint' },
    "文档生成": { sh 'npm run docs' }
)
```

### 2. 使用 Docker 缓存

```groovy
docker.image('node:18').inside('-v $HOME/.npm:/root/.npm') {
    sh 'npm ci'  // 使用缓存的依赖
}
```

### 3. 增量构建

```groovy
stage('Incremental Build') {
    // 只构建变更的模块
    def changedModules = sh(
        script: 'git diff --name-only HEAD~1 | cut -d/ -f1 | sort -u',
        returnStdout: true
    ).trim().split('\n')

    changedModules.each { module ->
        sh "mvn install -pl ${module} -am"
    }
}
```

### 4. 资源清理策略

```groovy
// 定期清理旧构建
def jobName = env.JOB_NAME
def maxBuilds = 10

sh """
    curl -X POST "http://jenkins/job/${jobName}/[1-${maxBuilds}]/doDelete"
"""
```

## 安全检查清单

### 凭证管理

- [ ] 使用 Jenkins Credentials Store
- [ ] 定期轮换密码和令牌
- [ ] 使用不同环境的独立凭证
- [ ] 限制凭证的作用域

### 密钥扫描

```groovy
stage('Secret Scanning') {
    sh '''
        # 扫描硬编码密钥
        git log --all --full-history -S "password" --source
        git log --all --full-history -S "api_key" --source
    '''
}
```

### 依赖漏洞检查

```groovy
stage('Dependency Check') {
    sh 'npm audit'
    sh 'snyk test'
}
```

### 签名验证

```groovy
stage('Verify Signatures') {
    sh 'gpg --verify file.asc file'
}
```

### 审计日志

```groovy
stage('Audit Trail') {
    sh '''
        # 记录谁修改了什么
        echo "User: ${env.BUILD_USER}"
        echo "Changes: ${env.CHANGES}"
    '''
}
```

## 故障排除指南 (Troubleshooting Guide)

### Pipeline 在 Build Stage 失败

**检查清单**:
1. 查看完整错误日志
2. 验证构建节点连通性
3. 确认依赖工具已安装
4. 检查环境变量配置

**调试命令**:
```groovy
node {
    sh 'env | sort'           // 打印所有环境变量
    sh 'which java'           // 验证工具路径
    sh 'java -version'        // 验证工具版本
}
```

### 插件冲突

**解决方案**:
1. 查看 [插件兼容性矩阵](https://www.jenkins.io/doc/developer/plugin-development/dependencies/)
2. 在隔离环境测试
3. 更新到兼容版本
4. 使用 `jhipster:validate` 检查

### 构建挂起

**可能原因**:
- 死锁或资源竞争
- 无限循环
- 等待用户输入

**解决方案**:
```groovy
timeout(time: 30, unit: 'MINUTES') {
    // 添加超时
}

// 或使用非阻塞输入
def input = input message: 'Continue?', ok: 'Proceed'
```

### 内存不足

**解决方案**:
```groovy
// 在节点上增加堆大小
node('-Xmx4g') {
    // pipeline steps
}
```

## 检查清单总结

创建新的 Jenkinsfile 时，确保：

### 基础结构
- [ ] 使用 `node {}` 包装所有内容
- [ ] 每个 stage 有明确的名称
- [ ] 遵循项目命名规范

### 错误处理
- [ ] 添加 try/catch/finally 块
- [ ] 长时间操作添加超时控制
- [ ] 可重试的操作添加 retry

### 安全
- [ ] 敏感信息使用 credentials
- [ ] 不在日志中打印凭证
- [ ] 定期轮换密钥

### 性能
- [ ] 识别可并行执行的任务
- [ ] 使用缓存加速构建
- [ ] 清理临时资源

### 文档
- [ ] 添加适当的注释
- [ ] 维护 README 说明用途
- [ ] 记录依赖关系

## 参考资料

- [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Pipeline Best Practices](https://www.jenkins.io/doc/book/pipeline/pipeline-best-practices/)
- [Pipeline Examples](https://www.jenkins.io/doc/pipeline/examples/)
- [Pipeline: Basic Steps](https://www.jenkins.io/doc/pipeline/steps/workflow-basic-steps/)
