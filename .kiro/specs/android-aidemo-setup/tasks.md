# 实现计划：android-aidemo-setup

## 概述

按照设计文档，依次创建 Android 工程骨架、配置 Git 版本管理、编写 Kiro Agent Hooks / Skills 配置文件、配置 MCP Services，最后编写属性测试验证关键正确性属性。

## 任务列表

- [x] 1. 创建 Android 工程骨架
  - [x] 1.1 创建 Gradle Kotlin DSL 根目录配置文件
    - 创建 `settings.gradle.kts`，配置 pluginManagement、dependencyResolutionManagement，rootProject.name = "AIDemo"，include(":app")
    - 创建根目录 `build.gradle.kts`，声明 android.application、kotlin.android、detekt 插件（apply false）
    - 创建 `gradle.properties`，设置 org.gradle.jvmargs、kotlin.code.style=official
    - _需求：1.3_

  - [x] 1.2 创建 app 模块 Gradle 配置
    - 创建 `app/build.gradle.kts`，配置 namespace="com.zkx.aidemo"、compileSdk=35、minSdk=21、targetSdk=35、versionCode=1、versionName="1.0.0"
    - 配置 compileOptions（Java 11）、kotlinOptions（jvmTarget="11"）
    - 注册 ktlintCheck / ktlintFormat Gradle 任务（Exec 类型，调用 ktlint CLI）
    - 配置 detekt 插件，指向 `$rootDir/detekt.yml`
    - 添加 dependencies：androidx.core.ktx、appcompat、material、junit、espresso
    - _需求：1.1, 1.2, 1.3_

  - [x] 1.3 创建 app 模块源码与资源文件
    - 创建 `app/src/main/java/com/zkx/aidemo/MainActivity.kt`（继承 AppCompatActivity，setContentView）
    - 创建 `app/src/main/AndroidManifest.xml`（声明 MainActivity 为 LAUNCHER）
    - 创建 `app/src/main/res/layout/activity_main.xml`（基础 ConstraintLayout）
    - 创建 `app/src/main/res/values/strings.xml`（app_name = "AIDemo"）
    - 创建 `app/src/main/res/values/themes.xml`（继承 Material3 主题）
    - 创建 `app/proguard-rules.pro`（空文件占位）
    - _需求：1.1, 1.4_

  - [x] 1.4 配置 Gradle Wrapper 与版本目录
    - 创建 `gradle/wrapper/gradle-wrapper.properties`，指定 Gradle 8.x 发行版 URL
    - 创建 `gradle/libs.versions.toml`，声明 AGP、Kotlin、AndroidX 依赖版本与别名
    - 创建 `gradlew` / `gradlew.bat` 启动脚本
    - _需求：1.3, 1.5_

  - [x] 1.5 创建 detekt 配置文件
    - 创建 `detekt.yml`，基于默认配置，启用 complexity、style、potential-bugs 规则集
    - _需求：4.2_

- [ ] 2. 配置 .gitignore 并初始化 Git 仓库
  - [x] 2.1 创建 .gitignore 文件
    - 创建 `.gitignore`，包含以下忽略规则：
      - Android 构建产物：`*.iml`、`.gradle/`、`/local.properties`、`/.idea/`、`.DS_Store`、`/build/`、`/captures/`、`.externalNativeBuild/`、`.cxx/`、`*.apk`、`*.aab`、`*.ap_`
      - 模块构建产物：`app/build/`
      - 保留 Gradle Wrapper：`!gradle/wrapper/gradle-wrapper.jar`、`!gradle/wrapper/gradle-wrapper.properties`
      - Kiro 本地缓存：`.kiro/cache/`
    - _需求：2.2_

  - [-] 2.2 初始化本地 Git 仓库并关联远程
    - 执行 `git init`，设置默认分支为 main
    - 执行 `git remote add origin https://github.com/zhangkexue/AIDemo.git`
    - 执行 `git add .` 和 `git commit -m "chore: initial Android project setup"`
    - _需求：2.1, 2.3_

  - [~] 2.3 推送 main 分支并打 v1.0.0 标签
    - 执行 `git push -u origin main`
    - 执行 `git tag v1.0.0` 和 `git push origin v1.0.0`
    - _需求：2.3, 2.4_

- [~] 3. 检查点 —— 确认工程结构与 Git 状态
  - 确认所有 Gradle 配置文件存在且语法正确，确认 `.gitignore` 已提交，确认远程仓库关联正确。如有疑问请告知。

- [ ] 4. 创建 Kiro Agent Hooks 配置文件
  - [~] 4.1 创建 on-save Hook
    - 创建 `.kiro/hooks/on-save.yaml`
    - trigger.type = fileEdited，filter = "**/*.kt"
    - action.skill = ktlint-format，args.mode = check
    - onError.block = false，输出中文错误提示
    - _需求：3.1, 3.5_

  - [~] 4.2 创建 post-generate Hook
    - 创建 `.kiro/hooks/post-generate.yaml`
    - trigger.type = postToolUse，filter = "**/*.kt"
    - action.skill = static-analysis
    - onError.block = false，输出中文错误提示
    - _需求：3.2, 3.5_

  - [~] 4.3 创建 pre-commit Hook
    - 创建 `.kiro/hooks/pre-commit.yaml`
    - trigger.type = userTriggered，event = pre-commit
    - action.skill = code-review，args.includeFormat = true
    - onError.block = true，输出中文错误提示（阻止提交）
    - _需求：3.3, 3.5, 3.6_

  - [~] 4.4 创建 pre-push Hook
    - 创建 `.kiro/hooks/pre-push.yaml`
    - trigger.type = userTriggered，event = pre-push
    - action.skill = static-analysis，args.full = true
    - onError.block = true，输出中文错误提示（阻止推送）
    - _需求：3.4, 3.5, 3.6_

- [ ] 5. 创建 Kiro Agent Skills 配置文件
  - [~] 5.1 创建 ktlint-format Skill
    - 创建 `.kiro/skills/ktlint-format.md`，包含 YAML frontmatter（name、description）
    - 描述执行步骤：check 模式调用 `./gradlew ktlintCheck`，format 模式调用 `./gradlew ktlintFormat`
    - 定义输出格式：成功/失败的中文提示模板
    - _需求：4.1, 4.5_

  - [~] 5.2 创建 static-analysis Skill
    - 创建 `.kiro/skills/static-analysis.md`，包含 YAML frontmatter
    - 描述执行步骤：依次执行 `./gradlew detekt` 和 `./gradlew lint`，汇总报告
    - 定义输出格式：必须包含问题总数、每个问题的严重级别（error/warning/info）、文件路径和行号
    - _需求：4.2, 4.5, 4.6_

  - [~] 5.3 创建 code-review Skill
    - 创建 `.kiro/skills/code-review.md`，包含 YAML frontmatter
    - 描述执行步骤：获取 `git diff --staged`，AI 审查逻辑正确性/Kotlin 最佳实践/Android 性能/安全隐患
    - 若 includeFormat=true，同时执行 ktlint-format（check 模式）
    - 定义输出格式：Markdown 报告（审查摘要、问题列表、建议改进项）
    - _需求：4.3, 4.5_

  - [~] 5.4 创建 pr-push Skill
    - 创建 `.kiro/skills/pr-push.md`，包含 YAML frontmatter
    - 描述执行步骤：确认非 main 分支 → `git push origin <branch>` → 通过 GitHub MCP 创建 PR（base: main）
    - 声明依赖：GitHub MCP 服务（需要 GITHUB_TOKEN 环境变量）
    - _需求：4.4, 4.5_

- [ ] 6. 创建 MCP Services 配置
  - [~] 6.1 创建 .kiro/mcp.json
    - 创建 `.kiro/mcp.json`，包含三个 MCP 服务：
      - `github`：command=npx，args=["-y","@modelcontextprotocol/server-github"]，env.GITHUB_PERSONAL_ACCESS_TOKEN="${GITHUB_TOKEN}"
      - `context7`：command=npx，args=["-y","@upstash/context7-mcp@latest"]
      - `android-docs`：command=npx，args=["-y","@modelcontextprotocol/server-fetch"]，env.ALLOWED_DOMAINS="developer.android.com,docs.gradle.org,kotlinlang.org"
    - 确保所有凭证字段使用 `${ENV_VAR}` 格式，不含硬编码字面量
    - _需求：5.1, 5.2, 5.3, 5.4, 5.6_

- [~] 7. 检查点 —— 确认 Kiro 配置完整性
  - 确认 4 个 Hook YAML 文件、4 个 Skill Markdown 文件、mcp.json 均存在且内容正确。如有疑问请告知。

- [ ] 8. 编写属性测试（Kotest）
  - [~] 8.1 配置 Kotest 属性测试依赖
    - 在 `app/build.gradle.kts` 的 testImplementation 中添加 `io.kotest:kotest-runner-junit5` 和 `io.kotest:kotest-property`（版本 5.x）
    - 在 `app/build.gradle.kts` 中配置 `tasks.withType<Test> { useJUnitPlatform() }`
    - _需求：1.3_

  - [~] 8.2 编写属性 1 测试：.gitignore 覆盖所有 Android 构建产物
    - 创建 `app/src/test/java/com/zkx/aidemo/GitignorePropertyTest.kt`
    - 使用 `StringSpec` + `forAll`，遍历标准构建产物路径列表（build/、app/build/、.gradle/、*.apk、*.aab 等）
    - 对每个路径调用 `git check-ignore -q <path>`，断言退出码为 0
    - 注释标注：`// Feature: android-aidemo-setup, Property 1: .gitignore 覆盖所有 Android 构建产物`
    - _需求：2.2，属性 1_

  - [ ]* 8.3 编写属性 2 测试：静态检测报告包含必要字段
    - 创建 `app/src/test/java/com/zkx/aidemo/StaticAnalysisReportPropertyTest.kt`
    - 使用 `checkAll(100, Arb.string())` 生成任意代码内容，调用 `runStaticAnalysis()` 辅助函数
    - 断言 report.totalIssues >= 0，每个 issue 的 severity 在 [error, warning, info] 中，filePath 非空，lineNumber > 0
    - 注释标注：`// Feature: android-aidemo-setup, Property 2: 静态检测报告包含必要字段`
    - _需求：4.6，属性 2_

  - [ ]* 8.4 编写属性 3 测试：MCP 配置不包含硬编码凭证
    - 创建 `app/src/test/java/com/zkx/aidemo/McpConfigSecurityPropertyTest.kt`
    - 读取 `.kiro/mcp.json`，解析 JSON，递归查找 token/key/secret/password/PERSONAL_ACCESS_TOKEN 等字段
    - 使用 `forAll` 遍历凭证键列表，断言对应值匹配正则 `\$\{[A-Z_]+\}`
    - 注释标注：`// Feature: android-aidemo-setup, Property 3: MCP 配置不包含硬编码凭证`
    - _需求：5.6，属性 3_

- [~] 9. 最终检查点 —— 确认所有测试通过
  - 确保所有测试通过，如有疑问请告知。

## 备注

- 标有 `*` 的子任务为可选项，可在 MVP 阶段跳过
- 每个任务均引用具体需求条款，确保可追溯性
- 属性测试最少运行 100 次迭代（Kotest 默认值）
- 检查点任务用于阶段性验证，确保增量进展正确
