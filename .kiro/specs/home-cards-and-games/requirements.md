# 需求文档

## 简介

本功能为 AIDemo 应用 1.0.1 版本，在首页添加 4 个 Material Design 风格的圆角卡片（资讯、财经、AI、娱乐），每个卡片点击后触发翻转动画并跳转至对应二级页面。娱乐页面包含两个小游戏入口：1024 数字合并游戏（2048 风格）和俄罗斯方块游戏，两款游戏均需完整实现、可直接游玩。

## 词汇表

- **HomeActivity**: 应用首页 Activity，展示 4 个功能卡片
- **CardView**: Material Design 圆角卡片组件，用于首页的 4 个功能入口
- **FlipAnimator**: 负责执行卡片翻转动画的组件
- **NewsActivity**: 资讯二级页面
- **FinanceActivity**: 财经二级页面
- **AiActivity**: AI 二级页面
- **EntertainmentActivity**: 娱乐二级页面，使用 Jetpack Compose 实现
- **EntertainmentList**: 娱乐页面中的竖向列表组件
- **Game1024Activity**: 1024 数字合并游戏页面（2048 风格）
- **Game1024Engine**: 1024 游戏的核心逻辑引擎
- **TetrisActivity**: 俄罗斯方块游戏页面
- **TetrisEngine**: 俄罗斯方块游戏的核心逻辑引擎
- **TetrisRenderer**: 负责渲染俄罗斯方块游戏画面的组件

---

## 需求

### 需求 1：首页 Material Design 卡片布局

**用户故事：** 作为用户，我希望在首页看到 4 个风格统一的圆角卡片，以便快速进入各功能模块。

#### 验收标准

1. THE HomeActivity SHALL 以 Material Design（Android 5.0）风格展示 4 个圆角卡片，卡片分别标注"资讯"、"财经"、"AI"、"娱乐"。
2. THE HomeActivity SHALL 使用 Material 主题，卡片圆角半径不小于 8dp，并带有阴影（elevation）效果。
3. THE HomeActivity SHALL 在竖屏模式下以 2×2 网格排列 4 个卡片。
4. WHEN 屏幕宽度小于 360dp，THE HomeActivity SHALL 以单列方式排列 4 个卡片。

---

### 需求 2：卡片翻转动画与页面跳转

**用户故事：** 作为用户，我希望点击卡片时看到翻转动画，然后跳转至对应页面，以便获得流畅的交互体验。

#### 验收标准

1. WHEN 用户点击任意卡片，THE FlipAnimator SHALL 在 300ms 内完成卡片翻转动画（Y 轴旋转 0° → 90°，再由 90° → 0°）。
2. WHEN 翻转动画完成后，THE HomeActivity SHALL 跳转至对应的二级页面（资讯→NewsActivity，财经→FinanceActivity，AI→AiActivity，娱乐→EntertainmentActivity）。
3. WHILE 翻转动画正在播放，THE HomeActivity SHALL 禁止重复点击同一卡片。
4. IF 页面跳转失败，THEN THE HomeActivity SHALL 保持当前页面并恢复卡片至初始状态。

---

### 需求 3：资讯、财经、AI 空白页面

**用户故事：** 作为开发者，我希望资讯、财经、AI 三个页面作为占位页面存在，以便后续版本填充内容。

#### 验收标准

1. THE NewsActivity SHALL 展示标题为"资讯"的空白页面，并提供返回首页的导航。
2. THE FinanceActivity SHALL 展示标题为"财经"的空白页面，并提供返回首页的导航。
3. THE AiActivity SHALL 展示标题为"AI"的空白页面，并提供返回首页的导航。
4. WHEN 用户点击返回按钮，THE NewsActivity、FinanceActivity、AiActivity SHALL 各自返回 HomeActivity。

---

### 需求 4：娱乐页面竖向列表

**用户故事：** 作为用户，我希望在娱乐页面看到游戏入口列表，以便选择想玩的游戏。

#### 验收标准

1. THE EntertainmentActivity SHALL 使用 Jetpack Compose 实现，展示标题为"娱乐"的页面。
2. THE EntertainmentList SHALL 以竖向列表形式展示至少两个游戏入口，第一项为"1024"，第二项为"俄罗斯方块"。
3. WHEN 用户点击"1024"列表项，THE EntertainmentActivity SHALL 跳转至 Game1024Activity。
4. WHEN 用户点击"俄罗斯方块"列表项，THE EntertainmentActivity SHALL 跳转至 TetrisActivity。
5. WHEN 用户点击返回按钮，THE EntertainmentActivity SHALL 返回 HomeActivity。

---

### 需求 5：1024 数字合并游戏

**用户故事：** 作为用户，我希望能在应用内玩 1024 数字合并游戏（2048 风格），以便享受休闲娱乐。

#### 验收标准

1. THE Game1024Activity SHALL 展示一个 4×4 的游戏棋盘，初始时随机放置 2 个数字方块（值为 2 或 4）。
2. WHEN 用户向上、下、左、右滑动，THE Game1024Engine SHALL 将所有方块沿滑动方向移动并合并相邻的相同数字。
3. WHEN 两个相同数字方块合并，THE Game1024Engine SHALL 将合并后的方块数值设为两者之和，并将该值加入当前分数。
4. WHEN 一次有效移动完成后，THE Game1024Engine SHALL 在棋盘空白位置随机生成一个值为 2（概率 90%）或 4（概率 10%）的新方块。
5. WHEN 棋盘上出现值为 1024 的方块，THE Game1024Activity SHALL 显示胜利提示，并提供继续游戏或返回的选项。
6. WHEN 棋盘已满且不存在任何可合并的相邻方块，THE Game1024Engine SHALL 判定游戏结束，THE Game1024Activity SHALL 显示游戏结束提示及最终分数。
7. THE Game1024Activity SHALL 实时显示当前分数。
8. WHEN 用户点击重新开始按钮，THE Game1024Engine SHALL 重置棋盘和分数至初始状态。
9. FOR ALL 合法的棋盘状态，THE Game1024Engine 执行移动后再执行反向移动 SHALL 保持棋盘中所有方块数值之和不减少（单调性属性）。

---

### 需求 6：俄罗斯方块游戏

**用户故事：** 作为用户，我希望能在应用内玩完整的俄罗斯方块游戏，以便享受经典休闲娱乐。

#### 验收标准

1. THE TetrisActivity SHALL 展示标准俄罗斯方块游戏界面，包含游戏区域（10列×20行）、当前分数、当前等级和下一个方块预览。
2. THE TetrisEngine SHALL 支持全部 7 种标准俄罗斯方块形状（I、O、T、S、Z、J、L）。
3. WHEN 游戏开始，THE TetrisEngine SHALL 从顶部中央随机生成一个方块并开始自动下落。
4. WHEN 用户点击左移按钮，THE TetrisEngine SHALL 将当前方块向左移动一格（若未碰壁）。
5. WHEN 用户点击右移按钮，THE TetrisEngine SHALL 将当前方块向右移动一格（若未碰壁）。
6. WHEN 用户点击旋转按钮，THE TetrisEngine SHALL 将当前方块顺时针旋转 90°（若旋转后不越界且不与已有方块重叠）。
7. WHEN 用户点击加速下落按钮，THE TetrisEngine SHALL 将当前方块加速向下移动直至落地。
8. WHEN 当前方块触底或与已有方块接触，THE TetrisEngine SHALL 将该方块固定在游戏区域，并检测是否有完整行。
9. WHEN 检测到一行或多行已填满，THE TetrisEngine SHALL 消除这些行，将上方方块下移，并按消除行数更新分数（1行=100分，2行=300分，3行=500分，4行=800分）。
10. WHEN 累计分数达到当前等级阈值，THE TetrisEngine SHALL 提升等级并加快方块下落速度。
11. WHEN 新生成的方块与已有方块重叠（游戏区域顶部已满），THE TetrisEngine SHALL 判定游戏结束，THE TetrisActivity SHALL 显示游戏结束提示及最终分数。
12. THE TetrisActivity SHALL 提供暂停/继续按钮；WHEN 用户点击暂停，THE TetrisEngine SHALL 停止方块下落；WHEN 用户点击继续，THE TetrisEngine SHALL 恢复方块下落。
13. WHEN 用户点击重新开始按钮，THE TetrisEngine SHALL 重置游戏区域、分数和等级至初始状态。
14. FOR ALL 合法的游戏状态，THE TetrisEngine 消除完整行后 SHALL 保证游戏区域中不存在空行位于非空行之上（重力属性）。
