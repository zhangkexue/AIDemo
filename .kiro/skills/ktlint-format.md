---
name: ktlint-format
description: 使用 ktlint 对 Kotlin 源文件执行格式化或格式检查
inclusion: manual
---

## 概述

使用 ktlint 对项目中的 Kotlin 源文件执行代码格式检查或自动格式化。

## 执行步骤

1. 确认 ktlint 已通过 Gradle 任务集成（`./gradlew ktlintCheck` 或 `./gradlew ktlintFormat`）
2. 根据 mode 参数决定执行模式：
   - `check`（默认）：执行 `./gradlew ktlintCheck`，仅检查不修改文件
   - `format`：执行 `./gradlew ktlintFormat`，自动修复格式问题
3. 解析输出，统计问题数量和文件列表

## 输出格式

- 成功：`✅ ktlint：所有文件格式正确`
- 失败：`❌ ktlint：发现 N 处格式问题，请运行 ./gradlew ktlintFormat 修复`，并列出问题文件路径

## 常用命令

```bash
# 仅检查格式
./gradlew ktlintCheck

# 自动修复格式
./gradlew ktlintFormat
```
