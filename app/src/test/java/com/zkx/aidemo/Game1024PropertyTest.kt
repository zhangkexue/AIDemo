// Feature: home-cards-and-games, Property 1: 移动后无相邻相同数字
// Feature: home-cards-and-games, Property 2: 分数增量等于合并值之和
// Feature: home-cards-and-games, Property 3: 有效移动后方块数增加一
// Feature: home-cards-and-games, Property 4: 棋盘数值总和单调不减
// Feature: home-cards-and-games, Property 5: 游戏结束判定正确性
package com.zkx.aidemo

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.next
import io.kotest.property.checkAll

/**
 * **Validates: Requirements 5.2, 5.3, 5.4, 5.6, 5.9**
 *
 * 属性测试：验证 Game1024Engine 的核心游戏逻辑属性。
 */
class Game1024PropertyTest : StringSpec({

    // 合法方块值：2 的幂次，范围 2~1024
    val tilePowers = listOf(2, 4, 8, 16, 32, 64, 128, 256, 512, 1024)

    // 辅助函数：生成随机合法棋盘（0~12 个非零方块）
    fun randomBoard(): Array<IntArray> {
        val board = Array(4) { IntArray(4) }
        val positions = (0 until 16).shuffled()
        val count = (0..12).random()
        for (i in 0 until count) {
            val pos = positions[i]
            board[pos / 4][pos % 4] = tilePowers.random()
        }
        return board
    }

    // 辅助函数：计算棋盘所有值之和
    fun boardSum(board: Array<IntArray>): Int = board.sumOf { row -> row.sum() }

    // 辅助函数：计算非零方块数量
    fun nonZeroCount(board: Array<IntArray>): Int = board.sumOf { row -> row.count { it != 0 } }

    // 辅助函数：将随机棋盘加载到引擎（绕过 init 的 spawnTile）
    fun engineWithBoard(board: Array<IntArray>): Game1024Engine {
        val engine = Game1024Engine()
        for (r in 0..3) engine.board[r] = board[r].copyOf()
        engine.score = 0
        return engine
    }

    // 随机方向生成器
    val directions = Game1024Engine.Direction.entries

    // 随机棋盘 Arb
    val boardArb = arbitrary { randomBoard() }

    // -------------------------------------------------------------------------
    // Property 1：移动后无相邻相同数字（验证需求 5.2）
    // -------------------------------------------------------------------------
    "Property 1: 移动后沿移动方向不存在相邻相同非零数字" {
        checkAll(PropTestConfig(iterations = 100), boardArb) { board ->
            val direction = directions.random()
            val engine = engineWithBoard(board)
            val moved = engine.move(direction)

            if (moved) {
                when (direction) {
                    Game1024Engine.Direction.LEFT, Game1024Engine.Direction.RIGHT -> {
                        // 检查每行中不存在相邻相同非零值
                        for (r in 0..3) {
                            for (c in 0..2) {
                                val a = engine.board[r][c]
                                val b = engine.board[r][c + 1]
                                if (a != 0 && b != 0) {
                                    (a == b) shouldBe false
                                }
                            }
                        }
                    }
                    Game1024Engine.Direction.UP, Game1024Engine.Direction.DOWN -> {
                        // 检查每列中不存在相邻相同非零值
                        for (c in 0..3) {
                            for (r in 0..2) {
                                val a = engine.board[r][c]
                                val b = engine.board[r + 1][c]
                                if (a != 0 && b != 0) {
                                    (a == b) shouldBe false
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Property 2：分数增量等于合并值之和（验证需求 5.3）
    // -------------------------------------------------------------------------
    "Property 2: 分数增量等于本次移动所有合并值之和" {
        checkAll(PropTestConfig(iterations = 100), boardArb) { board ->
            val direction = directions.random()
            val engine = engineWithBoard(board)
            val scoreBefore = engine.score

            // 手动计算该方向所有行/列的 mergeLine 分数增量之和
            val expectedDelta = when (direction) {
                Game1024Engine.Direction.LEFT -> {
                    (0..3).sumOf { r -> engine.mergeLine(board[r].copyOf()).second }
                }
                Game1024Engine.Direction.RIGHT -> {
                    (0..3).sumOf { r -> engine.mergeLine(board[r].reversedArray()).second }
                }
                Game1024Engine.Direction.UP -> {
                    // 转置后按行计算
                    val transposed = Array(4) { c -> IntArray(4) { r -> board[r][c] } }
                    (0..3).sumOf { r -> engine.mergeLine(transposed[r].copyOf()).second }
                }
                Game1024Engine.Direction.DOWN -> {
                    val transposed = Array(4) { c -> IntArray(4) { r -> board[r][c] } }
                    (0..3).sumOf { r -> engine.mergeLine(transposed[r].reversedArray()).second }
                }
            }

            engine.move(direction)
            val actualDelta = engine.score - scoreBefore

            actualDelta shouldBe expectedDelta
        }
    }

    // -------------------------------------------------------------------------
    // Property 3：有效移动后方块数增加一（验证需求 5.4）
    // -------------------------------------------------------------------------
    "Property 3: 有效移动后非零方块数 = 移动前非零方块数 - 合并次数 + 1" {
        checkAll(PropTestConfig(iterations = 100), boardArb) { board ->
            // 确保棋盘有空格（有效移动后才能生成新方块）
            if (nonZeroCount(board) < 16) {
                val direction = directions.random()
                val engine = engineWithBoard(board)
                val countBefore = nonZeroCount(board)

                // 计算合并次数（每次合并减少1个方块）
                val mergeCount = when (direction) {
                    Game1024Engine.Direction.LEFT -> {
                        (0..3).sumOf { r -> countMerges(board[r].copyOf()) }
                    }
                    Game1024Engine.Direction.RIGHT -> {
                        (0..3).sumOf { r -> countMerges(board[r].reversedArray()) }
                    }
                    Game1024Engine.Direction.UP -> {
                        val transposed = Array(4) { c -> IntArray(4) { r -> board[r][c] } }
                        (0..3).sumOf { r -> countMerges(transposed[r].copyOf()) }
                    }
                    Game1024Engine.Direction.DOWN -> {
                        val transposed = Array(4) { c -> IntArray(4) { r -> board[r][c] } }
                        (0..3).sumOf { r -> countMerges(transposed[r].reversedArray()) }
                    }
                }

                val moved = engine.move(direction)
                if (moved) {
                    val countAfter = nonZeroCount(engine.board)
                    val expected = countBefore - mergeCount + 1
                    countAfter shouldBe expected
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Property 4：棋盘数值总和单调不减（验证需求 5.9）
    // -------------------------------------------------------------------------
    "Property 4: 移动后棋盘数值总和 >= 移动前总和" {
        checkAll(PropTestConfig(iterations = 100), boardArb) { board ->
            val direction = directions.random()
            val engine = engineWithBoard(board)
            val sumBefore = boardSum(board)

            engine.move(direction)
            val sumAfter = boardSum(engine.board)

            (sumAfter >= sumBefore) shouldBe true
        }
    }

    // -------------------------------------------------------------------------
    // Property 5：游戏结束判定正确性（验证需求 5.6）
    // -------------------------------------------------------------------------
    "Property 5: isGameOver 当且仅当棋盘已满且四方向均无有效移动" {
        checkAll(PropTestConfig(iterations = 100), boardArb) { board ->
            val engine = engineWithBoard(board)
            engine.checkGameState()

            if (engine.isGameOver) {
                // 棋盘必须已满（无0）
                val isFull = board.all { row -> row.all { it != 0 } }
                isFull shouldBe true

                // 四方向均无有效移动：克隆引擎验证
                for (dir in directions) {
                    val clone = engineWithBoard(board)
                    clone.score = 0
                    val canMove = clone.move(dir)
                    canMove shouldBe false
                }
            } else {
                // 棋盘有空格 OR 存在可合并的相邻方块
                val hasEmpty = board.any { row -> row.any { it == 0 } }
                val hasMergeable = hasMergeableAdjacentTiles(board)
                (hasEmpty || hasMergeable) shouldBe true
            }
        }
    }
})

// -------------------------------------------------------------------------
// 辅助函数（顶层）
// -------------------------------------------------------------------------

/** 计算一行中合并次数（不修改原数组） */
private fun countMerges(line: IntArray): Int {
    val nonZero = line.filter { it != 0 }
    var merges = 0
    var i = 0
    while (i < nonZero.size) {
        if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
            merges++
            i += 2
        } else {
            i++
        }
    }
    return merges
}

/** 检查棋盘中是否存在可合并的相邻方块（水平或垂直） */
private fun hasMergeableAdjacentTiles(board: Array<IntArray>): Boolean {
    for (r in 0..3) {
        for (c in 0..3) {
            val v = board[r][c]
            if (v == 0) continue
            if (c + 1 <= 3 && board[r][c + 1] == v) return true
            if (r + 1 <= 3 && board[r + 1][c] == v) return true
        }
    }
    return false
}
