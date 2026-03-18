---
name: static-analysis
description: 集成 detekt 和 Android Lint 对代码进行质量分析，输出包含问题数量、严重级别和文件位置的检测报告
inclusion: manual
---

## 概述

依次执行 detekt 和 Android Lint，汇总输出统一的质量检测报告。

## 执行步骤

1. 执行 detekt：`./gradlew detekt`
2. 执行 Android Lint：`./gradlew lint`
3. 汇总两个工具的报告，生成统一输出

## 输出格式

报告必须包含以下字段：
- 问题总数
- 按严重级别分类（error / warning / info）
- 每个问题的文件路径和行号

示例输出：
```
静态检测报告：
  detekt: 3 个问题（1 error, 2 warning）
    - [error] app/src/main/java/com/zkx/aidemo/MainActivity.kt:42 - ComplexMethod
    - [warning] app/src/main/java/com/zkx/aidemo/MainActivity.kt:15 - MagicNumber
  Android Lint: 1 个问题（0 error, 1 warning）
    - [warning] app/src/main/res/layout/activity_main.xml:8 - HardcodedText
```

如无问题：`✅ 静态检测通过，未发现问题`

## 常用命令

```bash
# 运行 detekt
./gradlew detekt

# 运行 Android Lint
./gradlew lint

# 同时运行两者
./gradlew detekt lint
```
