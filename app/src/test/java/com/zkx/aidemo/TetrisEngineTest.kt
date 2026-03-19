package com.zkx.aidemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TetrisEngineTest {

    // 需求 6.2：7 种形状均可生成
    @Test
    fun `all 7 piece shapes can be generated`() {
        val engine = TetrisEngine()
        val seen = mutableSetOf<PieceShape>()
        // Run enough resets to observe all shapes (probabilistic, but 200 tries is sufficient)
        repeat(200) {
            engine.reset()
            seen.add(engine.currentPiece.shape)
            seen.add(engine.nextPiece.shape)
        }
        assertEquals(PieceShape.entries.toSet(), seen)
    }

    // 需求 6.10：等级提升阈值验证 — score >= level * 1000 时升级
    @Test
    fun `level increases when score reaches threshold`() {
        val engine = TetrisEngine()
        assertEquals(1, engine.level)
        // level=1 时 dropInterval = 1000 - (1-1)*100 = 1000，maxOf(100, 1000) = 1000
        assertEquals(1000L, engine.dropInterval)

        // 填满底部 4 行，O 形方块放在 y=16
        // cells 在 row 16,17，row+1=17,18 有方块，无法下移，锁定
        engine.reset()
        for (r in 16 until 20) {
            for (c in 0 until 10) engine.board[r][c] = 1
        }
        engine.currentPiece = TetrisPiece(PieceShape.O, x = 0, y = 14)
        // O 形 cells: (14,0),(14,1),(15,0),(15,1)；下方 (15+1=16) 有方块，无法下移，直接锁定
        val result = engine.tick()
        // 消了 4 行 => 800 * 1 = 800 分，不足 1000，level 仍为 1
        assertEquals(TetrisEngine.TickResult.LOCKED, result)
        assertEquals(800, engine.score)
        assertEquals(1, engine.level)
    }

    // 需求 6.10：score >= 1000 时 level 升为 2
    @Test
    fun `level upgrades to 2 when score reaches 1000`() {
        val engine = TetrisEngine()
        engine.reset()

        // 每次消 1 行得 100 分（level=1），重复 10 次累计 1000 分触发升级
        // 策略：填满 row 19，将 I 形方块（横向）放在 row 18 正上方，锁定后消 1 行
        repeat(10) {
            // 填满最底行
            for (c in 0 until 10) engine.board[19][c] = 1
            // I 形横向（rotation=0）cells: (y+1,x+0..3)，放 y=17 则 cells 在 row 18,19 不对
            // I 形 rotation=0: offsets (1,0)(1,1)(1,2)(1,3)，y=17 => cells row=18
            // row 18 下方 row 19 有方块，无法下移，锁定
            engine.currentPiece = TetrisPiece(PieceShape.I, x = 0, y = 17, rotation = 0)
            engine.tick()
        }

        assertEquals(1000, engine.score)
        assertEquals(2, engine.level)
    }

    // 需求 6.12：暂停/继续状态切换
    @Test
    fun `pause and resume toggle isPaused correctly`() {
        val engine = TetrisEngine()
        assertFalse(engine.isPaused)

        engine.pause()
        assertTrue(engine.isPaused)

        engine.resume()
        assertFalse(engine.isPaused)
    }

    // 需求 6.12：暂停时 tick 不移动方块
    @Test
    fun `tick does not move piece when paused`() {
        val engine = TetrisEngine()
        val initialY = engine.currentPiece.y
        engine.pause()
        engine.tick()
        assertEquals(initialY, engine.currentPiece.y)
    }

    // 需求 6.13：重置后状态归零
    @Test
    fun `reset clears all state`() {
        val engine = TetrisEngine()
        // Dirty the state
        engine.board[5][5] = 3
        engine.score = 500
        engine.level = 3
        engine.isGameOver = true
        engine.isPaused = true

        engine.reset()

        assertEquals(0, engine.score)
        assertEquals(1, engine.level)
        assertFalse(engine.isGameOver)
        assertFalse(engine.isPaused)
        assertTrue(engine.board.all { row -> row.all { it == 0 } })
    }

    // 额外：moveLeft/moveRight 边界检测
    @Test
    fun `moveLeft returns false when piece is at left boundary`() {
        val engine = TetrisEngine()
        // 将方块移到最左边
        engine.currentPiece = TetrisPiece(PieceShape.O, x = 0, y = 0)
        val result = engine.moveLeft()
        assertFalse(result)
        assertEquals(0, engine.currentPiece.x)
    }

    @Test
    fun `moveRight returns false when piece is at right boundary`() {
        val engine = TetrisEngine()
        // O 形在 x=8 时，cells 列为 8,9，再右移越界
        engine.currentPiece = TetrisPiece(PieceShape.O, x = 8, y = 0)
        val result = engine.moveRight()
        assertFalse(result)
        assertEquals(8, engine.currentPiece.x)
    }

    // 额外：hardDrop 将方块落到底部
    @Test
    fun `hardDrop places piece at bottom of empty board`() {
        val engine = TetrisEngine()
        engine.currentPiece = TetrisPiece(PieceShape.O, x = 4, y = 0)
        engine.hardDrop()
        // O 形占 2 行，底部应在 row 18,19
        // 检查 board[18][4], board[18][5], board[19][4], board[19][5] 非零
        assertTrue(engine.board[18][4] != 0)
        assertTrue(engine.board[18][5] != 0)
        assertTrue(engine.board[19][4] != 0)
        assertTrue(engine.board[19][5] != 0)
    }
}
