# 实现计划：首页卡片与游戏（home-cards-and-games）

## 概述

基于需求文档和设计文档，将 AIDemo 应用从 1.0.0 升级至 1.0.1，实现首页 2×2 卡片布局、翻转动画、三个占位页面、Compose 娱乐页面、1024 游戏和俄罗斯方块游戏。

## 任务

- [x] 1. 升级版本号并新增 Jetpack Compose 依赖
  - 修改 `gradle/libs.versions.toml`：新增 `compose-bom = "2024.05.00"`、`compose-activity = "1.9.0"` 版本，新增 `kotlin-compose-compiler` 插件，新增 `compose-ui`、`compose-material3`、`compose-activity` 库条目
  - 修改 `app/build.gradle.kts`：`versionCode` 改为 2，`versionName` 改为 `"1.0.1"`，启用 `buildFeatures { compose = true }`，添加 `composeOptions`，引用新增的 Compose 依赖，添加 `kotlin-compose-compiler` 插件
  - _需求：1.1（Compose 用于娱乐页面）、4.1_

- [x] 2. 创建 HomeActivity 及首页布局
  - [x] 2.1 创建 `res/layout/activity_home.xml`（2 列 GridLayout，4 个 MaterialCardView，圆角 12dp，elevation 4dp，每张卡片内含居中 TextView）
  - [x] 2.2 创建 `res/layout-w360dp/activity_home.xml`（同上，2 列）；默认 `activity_home.xml` 改为 `columnCount=1` 单列布局
  - [x] 2.3 将 `MainActivity.kt` 重命名为 `HomeActivity.kt`，更新类名，在 `onCreate` 中 `setContentView(R.layout.activity_home)`，为 4 张卡片设置点击监听（暂留空，待 FlipAnimator 实现后接入）
  - [x] 2.4 在 `res/values/strings.xml` 中新增卡片标题字符串资源（`card_news`、`card_finance`、`card_ai`、`card_entertainment`）及各页面标题
  - _需求：1.1、1.2、1.3、1.4_

- [x] 3. 实现 FlipAnimator 并接入首页点击逻辑
  - [x] 3.1 创建 `FlipAnimator.kt`（`object FlipAnimator`），实现 `flip(view, onMidpoint, onEnd)` 方法：第一段 `rotationY 0f→90f` 150ms，第二段 `rotationY -90f→0f` 150ms，通过 `isAnimating` 标志位防重复点击
  - [x] 3.2 在 `HomeActivity` 中接入 `FlipAnimator`：点击卡片时调用 `flip()`，`onEnd` 回调中用 `try-catch` 包裹 `startActivity`，失败时将 `rotationY` 重置为 0f
  - _需求：2.1、2.2、2.3、2.4_

- [x] 4. 创建资讯、财经、AI 占位页面
  - [x] 4.1 创建 `NewsActivity.kt`、`activity_news.xml`（ConstraintLayout + 居中 TextView 显示"资讯"），`onCreate` 中启用返回导航
  - [x] 4.2 创建 `FinanceActivity.kt`、`activity_finance.xml`（同上，标题"财经"）
  - [x] 4.3 创建 `AiActivity.kt`、`activity_ai.xml`（同上，标题"AI"）
  - _需求：3.1、3.2、3.3、3.4_

- [x] 5. 创建 EntertainmentActivity（Jetpack Compose）
  - [x] 5.1 创建 `EntertainmentActivity.kt`，继承 `ComponentActivity`，`setContent` 调用 `EntertainmentScreen`
  - [x] 5.2 在同文件或独立文件中实现 `EntertainmentScreen` Composable：`Scaffold(topBar)` + `LazyColumn`，包含"1024"和"俄罗斯方块"两个 `GameListItem`，点击分别跳转 `Game1024Activity`、`TetrisActivity`
  - _需求：4.1、4.2、4.3、4.4、4.5_

- [x] 6. 实现 Game1024Engine 游戏逻辑
  - [x] 6.1 创建 `Game1024Engine.kt`：定义 `Direction` 枚举，实现 `board: Array<IntArray>`（4×4）、`score`、`isGameOver`、`isWin` 属性，实现 `reset()` 和私有 `spawnTile()` 方法（初始化时生成 2 个方块）
  - [x] 6.2 实现 `move(direction: Direction): Boolean`：对每行/列过滤非零元素、合并相邻相同值、补零，棋盘变化时调用 `spawnTile()` 生成新方块（2: 90%，4: 10%），更新 `score`
  - [x] 6.3 实现 `isGameOver()` 和 `isWin()`：`isWin` 检测棋盘中是否存在 ≥ 1024 的值；`isGameOver` 检测棋盘已满且四方向均无有效移动
  - [ ]* 6.4 编写 `Game1024EngineTest.kt` 单元测试：初始化后恰好 2 个非零方块（需求 5.1）；重置后分数为 0（需求 5.8）；特定棋盘左移预期结果（需求 5.2）
  - _需求：5.1、5.2、5.3、5.4、5.6、5.8_

- [x] 7. 编写 Game1024Engine 属性测试
  - [ ]* 7.1 创建 `Game1024PropertyTest.kt`（Kotest `StringSpec`，`PropTestConfig(iterations = 100)`），编写属性测试：
    - **Property 1：移动后无相邻相同数字** — 验证需求 5.2
    - **Property 2：分数增量等于合并值之和** — 验证需求 5.3
    - **Property 3：有效移动后方块数增加一** — 验证需求 5.4
    - **Property 4：棋盘数值总和单调不减** — 验证需求 5.9
    - **Property 5：游戏结束判定正确性** — 验证需求 5.6
  - _需求：5.2、5.3、5.4、5.6、5.9_

- [x] 8. 检查点 — 确保 Game1024Engine 所有测试通过
  - 确保所有测试通过，如有疑问请向用户确认。

- [x] 9. 创建 Game1024Activity 和 Game1024BoardView
  - [x] 9.1 创建 `Game1024BoardView.kt`，继承 `View`，实现 `onDraw`：绘制 4×4 网格背景、各数值方块（不同数值对应不同背景色）、数值文字
  - [x] 9.2 创建 `activity_game1024.xml`：ConstraintLayout 包含分数 TextView、`Game1024BoardView`、重新开始 Button
  - [x] 9.3 创建 `Game1024Activity.kt`：持有 `Game1024Engine` 实例，通过 `GestureDetector` 检测滑动方向并调用 `engine.move()`，刷新 `boardView`，胜利/结束时弹出 AlertDialog
  - _需求：5.1、5.5、5.6、5.7、5.8_

- [x] 10. 实现 TetrisEngine 游戏逻辑
  - [x] 10.1 创建 `TetrisPiece.kt`：定义 `PieceShape` 枚举（I/O/T/S/Z/J/L）、`TetrisPiece` data class，以及 7 种形状各 4 个旋转状态的坐标偏移数组
  - [x] 10.2 创建 `TetrisEngine.kt`：定义 `TickResult` 枚举，实现 `board`（rows×cols）、`currentPiece`、`nextPiece`、`score`、`level`、`isGameOver`、`isPaused` 属性，实现 `start()`、`reset()`、`pause()`、`resume()`
  - [x] 10.3 实现 `moveLeft()`、`moveRight()`、`rotate()`：碰撞检测（边界 + 已固定方块），不合法时返回 `false` 且位置不变
  - [x] 10.4 实现 `hardDrop()`：将当前方块直接下落至最低合法位置并锁定
  - [x] 10.5 实现 `tick(): TickResult`：自动下落一格；若无法下落则锁定方块、消行、计分（1行=100×level，2行=300×level，3行=500×level，4行=800×level）、升级检测（每 1000 分升一级）、生成下一方块；顶部重叠则返回 `GAME_OVER`
  - [ ]* 10.6 编写 `TetrisEngineTest.kt` 单元测试：7 种形状均可生成（需求 6.2）；等级提升阈值（需求 6.10）；暂停/继续状态切换（需求 6.12）；重置后状态归零（需求 6.13）
  - _需求：6.2、6.3、6.4、6.5、6.6、6.7、6.8、6.9、6.10、6.11、6.12、6.13_

- [x] 11. 编写 TetrisEngine 属性测试
  - [ ]* 11.1 创建 `TetrisPropertyTest.kt`（Kotest `StringSpec`，`PropTestConfig(iterations = 100)`），编写属性测试：
    - **Property 6：俄罗斯方块操作合法性** — 验证需求 6.4、6.5、6.6
    - **Property 7：消行后无完整行** — 验证需求 6.9
    - **Property 8：重力属性（消行后无悬空空行）** — 验证需求 6.14
  - _需求：6.4、6.5、6.6、6.9、6.14_

- [x] 12. 检查点 — 确保 TetrisEngine 所有测试通过
  - 确保所有测试通过，如有疑问请向用户确认。

- [x] 13. 创建 TetrisBoardView 和 TetrisActivity
  - [x] 13.1 创建 `TetrisBoardView.kt`，继承 `View`：`onDraw` 绘制背景网格、已固定方块（`engine.board`）、当前活动方块（`engine.currentPiece`），不同颜色索引对应不同方块颜色
  - [x] 13.2 创建 `activity_tetris.xml`：ConstraintLayout 包含 `TetrisBoardView`、分数 TextView、等级 TextView、下一方块预览 View、左移/右移/旋转/硬降/暂停 Button
  - [x] 13.3 创建 `TetrisActivity.kt`：持有 `TetrisEngine` 和 `Handler`，通过 `Handler.postDelayed` 实现游戏循环（间隔 `dropInterval`），按钮点击调用对应 engine 方法，`TickResult.GAME_OVER` 时弹出 AlertDialog，`onPause`/`onResume` 时暂停/恢复游戏循环
  - _需求：6.1、6.3、6.4、6.5、6.6、6.7、6.11、6.12、6.13_

- [x] 14. 注册所有 Activity 到 AndroidManifest.xml
  - 将 `AndroidManifest.xml` 中 `.MainActivity` 替换为 `.HomeActivity`（保留 LAUNCHER intent-filter）
  - 新增 `.NewsActivity`、`.FinanceActivity`、`.AiActivity`、`.EntertainmentActivity`、`.Game1024Activity`、`.TetrisActivity` 的 `<activity>` 声明
  - _需求：1.1、3.1、3.2、3.3、4.1、5.1、6.1_

- [-] 15. 最终检查点 — 确保所有测试通过
  - 确保所有测试通过，如有疑问请向用户确认。

## 备注

- 标有 `*` 的子任务为可选项，可跳过以加快 MVP 进度
- 每个任务均引用具体需求条款以保证可追溯性
- 属性测试验证普遍性正确性，单元测试验证具体示例和边界条件
- `Game1024Engine` 和 `TetrisEngine` 均为纯 Kotlin 类，无 Android 依赖，可直接在 JVM 单元测试中运行
- 下落速度公式：`dropInterval = max(100, 1000 - level * 100)` ms
