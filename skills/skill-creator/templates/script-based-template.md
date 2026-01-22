---
name: {{SKILL_NAME}}
description: {{SKILL_DESCRIPTION}}
---

# {{SKILL_TITLE}}

基于脚本的自动化 skill。

## 触发关键词

- 关键词 1
- 关键词 2

## 依赖

### 系统要求

- Python 3.8+
- 必要的系统包

### Python 包

```bash
pip install package1 package2
```

## 脚本工具

### 核心脚本

#### script-name.py

**功能**：脚本功能描述

**用法**：
```bash
python scripts/script-name.py [参数]
```

**参数**：
- `--input` : 输入文件路径
- `--output` : 输出文件路径
- `--option` : 可选参数说明

**示例**：
```bash
# 基础用法
python scripts/script-name.py --input data.json --output result.json

# 带选项
python scripts/script-name.py --input data.json --output result.json --option value
```

### 验证脚本

#### validate.py

验证输出结果：
```bash
python scripts/validate.py [输出文件]
```

## 工作流

### 标准流程

1. **准备阶段**
   ```bash
   python scripts/prepare.py input.txt
   ```

2. **执行处理**
   ```bash
   python scripts/process.py prepared_data.json
   ```

3. **验证结果**
   ```bash
   python scripts/validate.py output.json
   ```

### 反馈循环

1. 执行处理脚本
2. **立即运行验证**
3. 如果验证失败：
   - 检查错误信息
   - 修复问题
   - 重新运行
4. **仅在验证通过后继续**

## 输出格式

### 标准输出

脚本生成以下格式的输出：

```json
{
  "status": "success",
  "result": { ... },
  "metadata": { ... }
}
```

### 错误输出

```json
{
  "status": "error",
  "message": "错误描述",
  "details": { ... }
}
```

## 故障排除

### 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| Error 1 | 原因说明 | 解决方案 |
| Error 2 | 原因说明 | 解决方案 |

## 示例

### 完整示例

**场景**：描述场景

**步骤**：
```bash
# 1. 准备输入
python scripts/script-name.py --input input.txt --output prepared.json

# 2. 验证准备结果
python scripts/validate.py prepared.json

# 3. 继续处理
python scripts/process.py prepared.json --output final.json
```

**结果**：预期结果说明
