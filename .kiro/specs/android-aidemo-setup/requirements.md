# 需求文档

## 简介

本功能旨在搭建一个名为 AIDemo 的 Android 工程基础环境，包含：使用 Kotlin 创建标准 Android 项目、同步至 GitHub 远程仓库并打版本标签、配置 Kiro IDE 的 Agent Hooks 与 Agent Skills（代码格式化、静态检测、Code Review、PR push），以及配置 MCP Services（GitHub、Android/Gradle 文档、代码搜索）。

## 词汇表

- **AIDemo**：本次创建的 Android 工程名称
- **Package_Name**：Android 应用包名，值为 `com.zkx.aidemo`
- **GitHub_Repo**：远程 GitHub 仓库，地址为 `https://github.com/zhangkexue/AIDemo.git`
- **Agent_Hook**：Kiro IDE 中在特定时机自动触发 AI Agent 执行任务的配置
- **Agent_Skill**：Kiro IDE 中定义 Agent 可执行能力的配置单元
- **MCP_Service**：Model Context Protocol 服务，为 AI Agent 提供外部工具和数据访问能力
- **ktlint**：Kotlin 官方推荐的代码风格检查与格式化工具
- **detekt**：Kotlin 静态代码分析工具，支持代码质量和复杂度检测
- **Android_Lint**：Android 官方静态检测工具，检查 Android 特定问题

---

## 需求

### 需求 1：创建 Android 工程

**用户故事：** 作为开发者，我希望使用 Kotlin 创建一个标准 Android 工程，以便作为 AI 功能演示的基础项目。

#### 验收标准

1. THE Android_Project SHALL 使用 Kotlin 作为主要开发语言创建，工程名为 AIDemo，包名为 `com.zkx.aidemo`
2. THE Android_Project SHALL 采用 minSdk 21、targetSdk 35、compileSdk 35 的 SDK 版本配置
3. THE Android_Project SHALL 使用 Gradle（Kotlin DSL）作为构建系统
4. THE Android_Project SHALL 包含标准的 `app` 模块结构（src/main、AndroidManifest.xml、res 目录）
5. WHEN Android_Project 创建完成后，THE Android_Project SHALL 能够成功执行 `./gradlew assembleDebug` 构建

---

### 需求 2：同步至 GitHub 远程仓库并打版本标签

**用户故事：** 作为开发者，我希望将初始工程推送到 GitHub 仓库并打上 v1.0.0 标签，以便作为基础版本进行版本管理。

#### 验收标准

1. THE Git_Repository SHALL 初始化本地 git 仓库，并关联远程仓库 `https://github.com/zhangkexue/AIDemo.git`
2. THE Git_Repository SHALL 包含合适的 `.gitignore` 文件，忽略 Android 工程的构建产物（build/、.gradle/、*.apk 等）
3. WHEN 初始代码提交完成后，THE Git_Repository SHALL 将所有文件推送至远程仓库的 `main` 分支
4. WHEN 代码推送成功后，THE Git_Repository SHALL 在当前提交上创建 `v1.0.0` 标签并推送至远程仓库
5. IF 远程仓库推送失败，THEN THE Git_Repository SHALL 输出明确的错误信息，说明失败原因

---

### 需求 3：配置 Agent Hooks

**用户故事：** 作为开发者，我希望配置 Kiro IDE 的 Agent Hooks，以便在文件保存、代码生成、提交和推送等关键时机自动触发 AI 质量检查。

#### 验收标准

1. THE Agent_Hook_Config SHALL 配置 `on-save` Hook，在文件保存时自动触发代码格式化检查
2. THE Agent_Hook_Config SHALL 配置 `post-generate` Hook，在 AI 生成代码后自动触发静态检测
3. THE Agent_Hook_Config SHALL 配置 `pre-commit` Hook，在 git commit 前自动触发 Code Review 和格式检查
4. THE Agent_Hook_Config SHALL 配置 `pre-push` Hook，在 git push 前自动触发完整的质量检查流程
5. THE Agent_Hook_Config SHALL 以 `.kiro/hooks/` 目录下的 YAML 配置文件形式存储
6. WHEN 任意 Hook 触发失败，THEN THE Agent_Hook_Config SHALL 输出可读的错误信息并阻止后续操作继续执行

---

### 需求 4：配置 Agent Skills

**用户故事：** 作为开发者，我希望配置 Agent Skills，以便 AI Agent 能够执行代码格式化、静态检测、Code Review 和 PR push 等标准化任务。

#### 验收标准

1. THE Agent_Skill_Config SHALL 配置代码格式化 Skill，使用 ktlint 对 Kotlin 源文件执行格式化
2. THE Agent_Skill_Config SHALL 配置静态检测 Skill，集成 detekt 和 Android Lint 对代码进行质量分析
3. THE Agent_Skill_Config SHALL 配置 Code Review Skill，对变更文件进行 AI 辅助代码审查并输出审查报告
4. THE Agent_Skill_Config SHALL 配置 PR Push Skill，支持创建 Pull Request 并推送至 GitHub 远程仓库
5. THE Agent_Skill_Config SHALL 以 `.kiro/skills/` 目录下的配置文件形式存储
6. WHEN 静态检测 Skill 执行完成后，THE Agent_Skill_Config SHALL 输出包含问题数量、严重级别和文件位置的检测报告

---

### 需求 5：配置 MCP Services

**用户故事：** 作为开发者，我希望配置 MCP Services，以便 AI Agent 能够访问 GitHub、Android/Gradle 文档和代码搜索等外部服务。

#### 验收标准

1. THE MCP_Config SHALL 配置 GitHub MCP 服务，支持 PR 创建、Issue 管理、仓库信息查询等 GitHub 操作
2. THE MCP_Config SHALL 配置 Android/Gradle 文档 MCP 服务，支持查询 Android API 文档和 Gradle 构建文档
3. THE MCP_Config SHALL 配置代码搜索 MCP 服务（Context7），支持在代码库中进行语义搜索和分析
4. THE MCP_Config SHALL 以 `.kiro/mcp.json` 配置文件形式存储所有 MCP 服务配置
5. IF MCP 服务连接失败，THEN THE MCP_Config SHALL 输出服务名称和失败原因，并允许其他服务继续正常运行
6. WHERE GitHub MCP 服务已配置，THE MCP_Config SHALL 使用环境变量 `GITHUB_TOKEN` 进行身份认证，避免在配置文件中硬编码凭证
