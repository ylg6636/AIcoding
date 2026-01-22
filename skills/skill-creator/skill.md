---
name: skill-creator
description: 交互式引导创建新的 Claude Code Skills。提供标准化目录结构生成、SKILL.md 模板创建、skill 验证等功能。当用户提及"创建 skill"、"新建 skill"、"skill 模板"、"生成 skill"等关键词时激活。
---

# Skill Creator

快速创建符合最佳实践的 Claude Code Skills。

## 触发关键词

- 创建 skill / 新建 skill / skill 模板 / 生成 skill
- 初始化 skill / skill scaffolding
- skill 向导 / skill 生成器

## 功能

### 1. 交互式创建向导

引导用户完成以下步骤：

1. **Skill 基本信息**
   - Skill 名称（小写字母、数字、连字符）
   - Skill 描述（清晰说明功能和使用时机）
   - Skill 类型选择

2. **Skill 类型选择**

   | 类型 | 适用场景 | 模板 |
   |------|----------|------|
   | **基础型** | 简单的指导性技能 | `basic-template.md` |
   | **工作流型** | 多步骤流程任务 | `workflow-template.md` |
   | **脚本型** | 依赖可执行脚本的技能 | `script-based-template.md` |
   | **专业型** | 特定领域知识库 | `domain-knowledge-template.md` |

3. **目录结构生成**

   自动创建以下结构：
   ```
   skill-name/
   ├── SKILL.md             # 主技能文件（必须大写）
   ├── references/          # 参考文档（可选）
   │   └── (可选)
   ├── scripts/             # 可执行脚本（可选）
   │   └── (可选)
   └── assets/              # 资源文件（可选）
       └── (可选)
   ```

4. **SKILL.md 内容生成**

   根据 YAML frontmatter 规范生成：
   ```yaml
   ---
   name: skill-name
   description: Clear description of what this skill does and when to use it
   ---
   ```

### 2. 模板库

提供 4 种预置模板，位于 `templates/` 目录：

- `basic-template.md` - 基础技能模板
- `workflow-template.md` - 工作流技能模板（含检查清单）
- `script-based-template.md` - 脚本驱动模板
- `domain-knowledge-template.md` - 领域知识库模板

### 3. Skill 验证

运行验证脚本检查 skill 结构：
```bash
python scripts/validate-skill.py /path/to/skill
```

验证项目：
- ✅ SKILL.md 存在且格式正确
- ✅ YAML frontmatter 有效
- ✅ name 符合命名规范
- ✅ description 长度符合要求
- ✅ 引用文件存在
- ✅ 路径格式正确（Unix 风格）

## 使用流程

### 方式一：交互式创建

```
用户：帮我创建一个 skill
Claude：[激活 skill-creator]
      1. 询问 skill 名称
      2. 询问 skill 描述
      3. 选择 skill 类型
      4. 创建目录结构
      5. 生成 SKILL.md
      6. （可选）添加参考资料
```

### 方式二：快速创建

```
用户：创建一个名为 pdf-analyzer 的 skill，用于分析 PDF 文档
Claude：[激活 skill-creator]
      → 使用基础模板
      → 创建目录：pdf-analyzer/
      → 生成 SKILL.md
      → 完成
```

### 方式三：验证现有 skill

```
用户：验证我的 skill 是否符合规范
Claude：[激活 skill-creator]
      → 运行 validate-skill.py
      → 报告验证结果
```

## 设计原则

### 渐进式披露（Progressive Disclosure）

- **元数据预加载**：只加载 name 和 description
- **按需读取**：需要时读取完整 SKILL.md
- **延迟加载**：其他文件仅在引用时加载

### 内容质量标准

1. **简洁优先**
   - SKILL.md 主体不超过 500 行
   - 只添加 Claude 不知道的信息
   - 质疑每一部分的必要性

2. **适当的自由度**
   - 高自由度：文本指令（多种有效方法）
   - 中等自由度：伪代码或带参数脚本
   - 低自由度：特定脚本（脆弱操作）

3. **避免嵌套引用**
   - 保持引用深度为 1 层
   - 所有参考文件直接从 SKILL.md 链接

## 最佳实践

### 命名约定

- ✅ 使用动名词形式：`processing-pdfs`、`analyzing-spreadsheets`
- ✅ 小写字母 + 数字 + 连字符
- ❌ 避免：`helper`、`utils`、`tools` 等模糊名称

### 描述写作

- ✅ 第三人称："Processes Excel files and generates reports"
- ❌ 第一人称："I can help you process Excel files"
- 必须说明：做什么 + 何时使用

### 文件路径

- ✅ Unix 风格：`scripts/helper.py`
- ❌ Windows 风格：`scripts\\helper.py`

## 常见模式

### 1. 检查清单模式

适用于多步骤工作流：

```markdown
## Task Progress:
- [ ] Step 1: Analyze requirements
- [ ] Step 2: Create implementation
- [ ] Step 3: Validate output
- [ ] Step 4: Deploy solution
```

### 2. 条件工作流模式

```markdown
1. Determine the type:

   **Creating new?** → Follow "Creation workflow"
   **Editing existing?** → Follow "Editing workflow"
```

### 3. 反馈循环模式

```markdown
1. Make changes
2. **Validate immediately**: `python scripts/validate.py`
3. If validation fails, fix and re-validate
4. Only proceed when validation passes
```

## 参考资源

- [Claude Code Skills Official Docs](https://code.claude.com/docs/en/skills)
- [Skill Authoring Best Practices](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices)
- [anthropics/skills](https://github.com/anthropics/skills) - 官方示例
