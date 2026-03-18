---
name: code-review
description: 对变更文件进行 AI 辅助代码审查，输出 Markdown 格式审查报告
inclusion: manual
---

## 概述

获取当前 git 变更内容，对每个变更文件进行 AI 代码审查，输出结构化审查报告。

## 执行步骤

1. 获取当前 git diff：`git diff --staged`（已暂存）或 `git diff HEAD`（所有变更）
2. 对每个变更文件进行审查，关注：
   - 代码逻辑正确性
   - Kotlin 最佳实践（空安全、扩展函数、协程使用等）
   - Android 性能问题（主线程操作、内存泄漏、过度绘制等）
   - 安全隐患（硬编码凭证、不安全的网络请求等）
3. 如 `includeFormat=true`，同时执行 `./gradlew ktlintCheck`
4. 输出 Markdown 格式审查报告

## 输出格式

```markdown
## Code Review 报告

### 审查摘要
- 变更文件数：N
- 发现问题数：N（严重: X, 警告: Y, 建议: Z）

### 问题列表

#### `文件路径`
- [严重] 行号：问题描述
- [警告] 行号：问题描述

### 建议改进项
- 改进建议描述
```

如无问题：`✅ Code Review 通过，未发现问题`

## 常用命令

```bash
# 查看已暂存的变更
git diff --staged

# 查看所有未提交变更
git diff HEAD
```
