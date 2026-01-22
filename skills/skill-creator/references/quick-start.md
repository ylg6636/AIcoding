# Skill Creator 快速开始

使用 skill-creator 快速创建新的 Claude Code Skills。

## 创建新 Skill

### 交互式创建

```
帮我创建一个 skill
```

### 快速创建

```
创建一个名为 pdf-extractor 的 skill，用于从 PDF 中提取文本
```

## Skill 类型

| 类型 | 适用场景 |
|------|----------|
| **基础型** | 简单指导性技能 |
| **工作流型** | 多步骤流程任务 |
| **脚本型** | 依赖可执行脚本的技能 |
| **专业型** | 特定领域知识库 |

## 目录结构

```
skill-name/
├── SKILL.md              # 主技能文件（必需，必须大写）
├── references/           # 参考文档（可选）
│   ├── guide.md
│   └── examples.md
├── scripts/              # 可执行脚本（可选）
│   ├── validate.py
│   └── process.py
└── assets/               # 资源文件（可选）
    └── templates/
```

## 验证 Skill

```bash
python scripts/validate-skill.py /path/to/skill
```

## 最佳实践

### 命名

- 使用动名词形式：`processing-pdfs`
- 小写字母 + 数字 + 连字符
- 避免模糊名称：`helper`、`utils`

### 描述

- 第三人称："Processes Excel files"
- 说明功能 + 使用时机
- 最多 1024 字符

### 内容

- 保持简洁（< 500 行）
- 避免嵌套引用
- 使用 Unix 风格路径
