# Jenkins 共享库指南

使用 Jenkins 共享库（Shared Libraries）来重用代码并简化流水线。

## 概述

Jenkins 共享库允许您：
- 在多个流水线间共享 Groovy 代码
- 封装复杂的逻辑
- 创建自定义步骤
- 统一流水线标准

## 目录结构

```
(root)
+- src/                     # Groovy 源代码
|   +- org/foo/
|       +- Utilities.groovy # 库类
+- vars/                    # 全局变量/自定义步骤
|   +- sayHello.groovy     # 可在流水线中直接调用
+- resources/               # 静态资源
    +- org/foo/data.json   # 可通过 libraryResource 加载
```

## 创建共享库

### 1. 创建 Git 仓库

```bash
mkdir jenkins-shared-library
cd jenkins-shared-library
git init
```

### 2. 创建目录结构

```bash
mkdir -p src/org/devops vars resources
```

### 3. 编写库代码

#### src/org/devops/Utilities.groovy

```groovy
package org.devops

class Utilities implements Serializable {
    def steps

    Utilities(steps) {
        this.steps = steps
    }

    def mavenBuild(String goals) {
        steps.sh "mvn ${goals}"
    }

    def deploy(String env) {
        steps.sh "./deploy.sh ${env}"
    }
}
```

#### vars/sayHello.groovy

```groovy
def call(String name = 'human') {
    echo "Hello, ${name}!"
}
```

#### vars/mavenBuild.groovy

```groovy
def call(String goals = 'clean package') {
    sh "mvn ${goals}"
}
```

## 使用共享库

### 在 Jenkinsfile 中声明

```groovy
@Library('my-shared-lib@main') _

pipeline {
    agent any
    stages {
        stage('Example') {
            steps {
                sayHello 'Developer'
            }
        }
    }
}
```

### 脚本式流水线中使用

```groovy
@Library('my-shared-lib@main') _

node {
    // 使用自定义步骤
    sayHello 'Developer'

    // 使用类
    def utils = new org.devops.Utilities(this)
    utils.mavenBuild 'clean package'
}
```

## 常用模式

### 1. 标准化构建步骤

```groovy
// vars/standardBuild.groovy
def call(String type) {
    node {
        stage('Checkout') {
            checkout scm
        }

        stage('Build') {
            if (type == 'maven') {
                sh 'mvn package'
            } else if (type == 'npm') {
                sh 'npm run build'
            }
        }

        stage('Test') {
            if (type == 'maven') {
                sh 'mvn test'
            } else if (type == 'npm') {
                sh 'npm test'
            }
        }
    }
}
```

### 2. 通知封装

```groovy
// vars/notify.groovy
def call(String status) {
    emailext(
        subject: "Build ${status}: ${env.JOB_NAME}",
        body: """
            Build ${status}
            Job: ${env.JOB_NAME}
            Build: ${env.BUILD_NUMBER}
            URL: ${env.BUILD_URL}
        """,
        to: 'team@example.com'
    )
}
```

### 3. Docker 部署封装

```groovy
// vars/dockerDeploy.groovy
def call(String registry, String imageName, String tag) {
    sh """
        docker build -t ${registry}/${imageName}:${tag} .
        docker push ${registry}/${imageName}:${tag}
    """
}
```

## 最佳实践

1. **保持简单** - 每个函数只做一件事
2. **文档化** - 添加注释说明用法
3. **版本控制** - 使用分支或标签管理版本
4. **测试** - 单独测试库代码
5. **命名空间** - 使用包名避免冲突

## 配置 Jenkins

在 Jenkins 系统配置中添加共享库：

1. 进入 "Manage Jenkins" → "System"
2. 找到 "Global Pipeline Libraries"
3. 添加库：
   - Name: `my-shared-lib`
   - Default Version: `main`
   - Repository URL: `https://github.com/user/jenkins-shared-library.git`
   - Credentials: （如需要）

## 参考资料

- [Jenkins Shared Libraries](https://www.jenkins.io/doc/book/pipeline/shared-libraries/)
- [Extending with Shared Libraries](https://www.jenkins.io/doc/book/pipeline/development/#shared-libraries)
