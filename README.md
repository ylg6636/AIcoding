# AIcoding

AI Coding Skills - 存储 Claude Code skills 的仓库。

## 目录结构

```
skills/
├── skill-name/           # 具体 skill 目录
│   ├── skill.md         # skill 的主要配置文件
│   └── references/      # 可选：参考文档和模板
│       └── example.md
```

## Skill 格式

每个 skill 应包含以下内容：

1. **skill.md** - skill 的主文件，定义：
   - Skill 名称和描述
   - 触发关键词
   - 配置说明
   - 使用示例

2. **references/** (可选) - 参考文档：
   - 模板文件
   - 示例代码
   - 最佳实践

## 如何使用

1. 将此仓库克隆到本地
2. 将 skills 目录链接到您的 `.claude/skills/` 目录
3. 或直接复制需要的 skills 到您的项目中

## 贡献

欢迎添加更多有用的 skills！

## 许可证

MIT License
