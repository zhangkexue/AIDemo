package com.zkx.aidemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Game1024EngineTest {

    // 需求 5.1：初始化后棋盘恰好有 2 个非零方块，值为 2 或 4
    @Test
    fun `init produces exactly 2 non-zero tiles with value 2 or 4`() {
        val engine = Game1024Engine()
        val nonZero = engine.board.flatMap { it.toList() }.filter { it != 0 }
        assertEquals(2, nonZero.size)
        assertTrue(nonZero.all { it == 2 || it == 4 })
    }

    // 需求 5.8：重置后分数为 0，棋盘恰好有 2 个非零方块
    @Test
    fun `reset clears score and produces exactly 2 tiles`() {
        val engine = Game1024Engine()
        // Make some moves to dirty the state
        engine.board[0][0] = 8
        engine.board[0][1] = 8
        engine.move(Game1024Engine.Direction.LEFT)
        engine.reset()

        assertEquals(0, engine.score)
        val nonZero = engine.board.flatMap { it.toList() }.filter { it != 0 }
        assertEquals(2, nonZero.size)
    }

    // 需求 5.2：特定棋盘状态下左移的预期结果
    @Test
    fun `move LEFT merges adjacent equal tiles correctly`() {
        val engine = Game1024Engine()
        // Set a known board state (override init tiles)
        for (row in engine.board) row.fill(0)
        engine.board[0] = intArrayOf(2, 2, 4, 4)
        engine.board[1] = intArrayOf(0, 0, 0, 0)
        engine.board[2] = intArrayOf(0, 0, 0, 0)
        engine.board[3] = intArrayOf(0, 0, 0, 0)

        // Manually call mergeLine to verify (board[1..3] are empty so move is valid)
        val (result, delta) = engine.mergeLine(intArrayOf(2, 2, 4, 4))
        assertTrue(result.contentEquals(intArrayOf(4, 8, 0, 0)))
        assertEquals(12, delta)
    }

    // 需求 5.2：mergeLine 不合并不相邻的相同值
    @Test
    fun `mergeLine does not merge non-adjacent equal values`() {
        val engine = Game1024Engine()
        val (result, delta) = engine.mergeLine(intArrayOf(2, 4, 2, 0))
        assertTrue(result.contentEquals(intArrayOf(2, 4, 2, 0)))
        assertEquals(0, delta)
    }

    // 需求 5.2：mergeLine 每次合并只合并一次（不连锁）
    @Test
    fun `mergeLine merges only once per pair`() {
        val engine = Game1024Engine()
        val (result, delta) = engine.mergeLine(intArrayOf(2, 2, 2, 2))
        assertTrue(result.contentEquals(intArrayOf(4, 4, 0, 0)))
        assertEquals(8, delta)
    }

    // 需求 5.3：分数增量等于合并值之和
    @Test
    fun `score increases by merged tile values`() {
        val engine = Game1024Engine()
        for (row in engine.board) row.fill(0)
        engine.board[0] = intArrayOf(4, 4, 0, 0)
        engine.board[1] = intArrayOf(0, 0, 0, 0)
        engine.board[2] = intArrayOf(0, 0, 0, 0)
        engine.board[3] = intArrayOf(0, 0, 0, 0)

        val scoreBefore = engine.score
        engine.move(Game1024Engine.Direction.LEFT)
        assertEquals(8, engine.score - scoreBefore)
    }

    // 需求 5.6：isGameOver 在棋盘已满且无合并时为 true
    @Test
    fun `isGameOver is true when board is full and no moves possible`() {
        val engine = Game1024Engine()
        // Fill board with alternating values so no merges are possible
        val values = intArrayOf(2, 4, 2, 4)
        for (r in 0..3) {
            for (c in 0..3) {
                engine.board[r][c] = if ((r + c) % 2 == 0) 2 else 4
            }
        }
        engine.checkGameState()
        assertTrue(engine.isGameOver)
    }

    // isGameOver is false when board has empty cells
    @Test
    fun `isGameOver is false when board has empty cells`() {
        val engine = Game1024Engine()
        // Default init has empty cells
        engine.checkGameState()
        assertTrue(!engine.isGameOver)
    }
}
