// 并行执行模板 - 脚本式 Jenkinsfile
// 适用于需要并行执行多个任务以加快构建速度的场景

node('build-node') {
    def appName = 'parallel-app'
    def buildStatus = [:]

    echo "=== ${appName} 并行构建开始 ==="

    // ==================== 阶段 1: 代码检出 ====================
    stage('Checkout') {
        echo '检出源代码...'
        checkout scm
        echo "当前分支: ${env.BRANCH_NAME}"
    }

    // ==================== 阶段 2: 并行构建多平台 ====================
    stage('Parallel Build') {
        echo '并行构建多个平台版本...'

        parallel(
            "Linux 构建": {
                node('linux') {
                    stage('Linux Build') {
                        echo '构建 Linux 版本...'
                        sh './build.sh linux'
                        archiveArtifacts artifacts: 'dist/*-linux.*', fingerprint: true
                    }
                }
            },
            "Windows 构建": {
                node('windows') {
                    stage('Windows Build') {
                        echo '构建 Windows 版本...'
                        bat 'build.bat windows'
                        archiveArtifacts artifacts: 'dist/*-windows.*', fingerprint: true
                    }
                }
            },
            "macOS 构建": {
                node('macos') {
                    stage('macOS Build') {
                        echo '构建 macOS 版本...'
                        sh './build.sh macos'
                        archiveArtifacts artifacts: 'dist/*-macos.*', fingerprint: true
                    }
                }
            },
            failFast: false  // 一个失败不影响其他
        )

        echo '多平台构建完成'
    }

    // ==================== 阶段 3: 并行测试 ====================
    stage('Parallel Tests') {
        echo '并行运行多个测试套件...'

        parallel(
            "单元测试": {
                stage('Unit Tests') {
                    echo '运行单元测试...'
                    sh 'npm run test:unit'
                    junit 'test-results/unit/*.xml'
                }
            },
            "集成测试": {
                stage('Integration Tests') {
                    echo '运行集成测试...'
                    sh 'npm run test:integration'
                    junit 'test-results/integration/*.xml'
                }
            },
            "E2E 测试": {
                stage('E2E Tests') {
                    echo '运行端到端测试...'
                    sh 'npm run test:e2e'
                    junit 'test-results/e2e/*.xml'
                }
            },
            "性能测试": {
                stage('Performance Tests') {
                    echo '运行性能测试...'
                    sh 'npm run test:performance'
                    // 发布性能报告
                    // performanceReport pattern: 'performance/*.json'
                }
            }
        )

        echo '所有测试完成'
    }

    // ==================== 阶段 4: 并行质量检查 ====================
    stage('Parallel Quality Checks') {
        echo '并行运行质量检查...'

        parallel(
            "代码风格": {
                stage('Code Style') {
                    echo '检查代码风格...'
                    sh 'npm run lint'
                    // publishLint results
                }
            },
            "安全扫描": {
                stage('Security Scan') {
                    echo '扫描安全问题...'
                    sh 'npm audit'
                    sh 'snyk test'
                }
            },
            "依赖检查": {
                stage('Dependency Check') {
                    echo '检查依赖更新...'
                    sh 'npm outdated || true'
                }
            },
            "许可证检查": {
                stage('License Check') {
                    echo '检查许可证兼容性...'
                    sh 'license-checker --production --onlyAllow "MIT;Apache-2.0;BSD-3-Clause"'
                }
            }
        )

        echo '质量检查完成'
    }

    // ==================== 阶段 5: 并行 Docker 构建 ====================
    stage('Parallel Docker Builds') {
        if (env.BRANCH_NAME == 'main') {
            echo '并行构建多个 Docker 镜像...'

            parallel(
                "API 镜像": {
                    stage('Build API Image') {
                        echo '构建 API 服务镜像...'
                        sh """
                            docker build -t myregistry/api:${env.BUILD_NUMBER} ./api
                            docker push myregistry/api:${env.BUILD_NUMBER}
                        """
                    }
                },
                "Worker 镜像": {
                    stage('Build Worker Image') {
                        echo '构建 Worker 服务镜像...'
                        sh """
                            docker build -t myregistry/worker:${env.BUILD_NUMBER} ./worker
                            docker push myregistry/worker:${env.BUILD_NUMBER}
                        """
                    }
                },
                "Frontend 镜像": {
                    stage('Build Frontend Image') {
                        echo '构建前端应用镜像...'
                        sh """
                            docker build -t myregistry/frontend:${env.BUILD_NUMBER} ./frontend
                            docker push myregistry/frontend:${env.BUILD_NUMBER}
                        """
                    }
                }
            )

            echo 'Docker 镜像构建完成'
        } else {
            echo '跳过 Docker 构建（非主分支）'
        }
    }

    // ==================== 阶段 6: 部署 ====================
    stage('Deploy') {
        if (env.BRANCH_NAME == 'main') {
            echo '部署到生产环境...'

            // 并行部署多个服务
            parallel(
                "部署 API": {
                    sh 'kubectl set image deployment/api api=myregistry/api:${env.BUILD_NUMBER}'
                },
                "部署 Worker": {
                    sh 'kubectl set image deployment/worker worker=myregistry/worker:${env.BUILD_NUMBER}'
                },
                "部署 Frontend": {
                    sh 'kubectl set image deployment/frontend frontend=myregistry/frontend:${env.BUILD_NUMBER}'
                }
            )

            // 等待所有部署就绪
            sh 'kubectl rollout status deployment/api'
            sh 'kubectl rollout status deployment/worker'
            sh 'kubectl rollout status deployment/frontend'

            echo '生产部署完成'

        } else if (env.BRANCH_NAME == 'develop') {
            echo '部署到开发环境...'
            sh 'kubectl set image deployment/api api=myregistry/api:${env.BUILD_NUMBER} --namespace=dev'
            echo '开发部署完成'

        } else {
            echo '特性分支，跳过部署'
        }
    }

    // ==================== 清理 ====================
    stage('Cleanup') {
        echo '清理工作空间...'
        cleanWs()

        echo "=== ${appName} 并行构建完成 ==="
    }
}

// ==================== 矩阵构建示例（高级用法）====================
/*
def platforms = ['linux', 'windows', 'macos']
def testSuites = ['unit', 'integration', 'e2e']

def builds = [:]
platforms.each { platform ->
    builds["${platform}"] = {
        node(platform) {
            stage("Build ${platform}") {
                sh "./build.sh ${platform}"
            }
            testSuites.each { suite ->
                stage("${platform} - ${suite}") {
                    sh "./test.sh ${platform} ${suite}"
                }
            }
        }
    }
}

parallel builds
*/
