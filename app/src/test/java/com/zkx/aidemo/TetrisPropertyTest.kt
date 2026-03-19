// Feature: home-cards-and-games, Property 6: 俄罗斯方块操作合法性
// Feature: home-cards-and-games, Property 7: 消行后无完整行
// Feature: home-cards-and-games, Property 8: 重力属性（消行后无悬空空行）
package com.zkx.aidemo

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.checkAll

/**
 * 属性测试：验证 TetrisEngine 的核心游戏逻辑属性。
 * 验证需求：6.4, 6.5, 6.6, 6.9, 6.14
 */
class TetrisPropertyTest : StringSpec({

    // 辅助：创建一个干净的引擎并填充随机棋盘（保留顶部2行为空，避免立即 game over）
    fun engineWithRandomBoard(): TetrisEngine {
        val engine = TetrisEngine()
        engine.reset()
        // Fill rows 4..19 with random non-zero values, leaving rows 0..3 empty
        for (r in 4 until engine.rows) {
            for (c in 0 until engine.cols) {
                engine.board[r][c] = if ((r + c) % 3 == 0) 0 else (1..7).random()
            }
        }
        return engine
    }

    // 辅助：检查方块所有格子是否在合法范围内且不与已有方块重叠
    fun isValidPosition(engine: TetrisEngine, piece: TetrisPiece): Boolean {
        for (cell in piece.cells()) {
            val r = cell[0]
            val c = cell[1]
            if (r < 0 || r >= engine.rows || c < 0 || c >= engine.cols) return false
            if (engine.board[r][c] != 0) return false
        }
        return true
    }

    val engineArb = arbitrary { engineWithRandomBoard() }

    // -------------------------------------------------------------------------
    // Property 6：俄罗斯方块操作合法性（验证需求 6.4, 6.5, 6.6）
    // -------------------------------------------------------------------------
    "Property 6: 移动/旋转后方块始终在合法位置" {
        checkAll(PropTestConfig(iterations = 100), engineArb) { engine ->
            // Test moveLeft
            val beforeLeft = engine.currentPiece.copy()
            val movedLeft = engine.moveLeft()
            if (movedLeft) {
                isValidPosition(engine, engine.currentPiece) shouldBe true
            } else {
                // Position unchanged
                engine.currentPiece.x shouldBe beforeLeft.x
                engine.currentPiece.y shouldBe beforeLeft.y
            }

            // Test moveRight
            val beforeRight = engine.currentPiece.copy()
            val movedRight = engine.moveRight()
            if (movedRight) {
                isValidPosition(engine, engine.currentPiece) shouldBe true
            } else {
                engine.currentPiece.x shouldBe beforeRight.x
                engine.currentPiece.y shouldBe beforeRight.y
            }

            // Test rotate
            val beforeRotate = engine.currentPiece.copy()
            val rotated = engine.rotate()
            if (rotated) {
                isValidPosition(engine, engine.currentPiece) shouldBe true
            } else {
                engine.currentPiece.rotation shouldBe beforeRotate.rotation
            }
        }
    }

    // -------------------------------------------------------------------------
    // Property 7：消行后无完整行（验证需求 6.9）
    // -------------------------------------------------------------------------
    "Property 7: tick 后游戏区域中不存在完全填满的行" {
        checkAll(PropTestConfig(iterations = 100), engineArb) { engine ->
            // Fill the bottom row completely to guarantee a clear
            for (c in 0 until engine.cols) {
                engine.board[engine.rows - 1][c] = 1
            }

            // Drop current piece to bottom via hardDrop
            if (!engine.isGameOver) {
                engine.hardDrop()
            }

            // After hardDrop, no row should be completely filled
            for (r in 0 until engine.rows) {
                val isFullRow = engine.board[r].all { it != 0 }
                isFullRow shouldBe false
            }
        }
    }

    // -------------------------------------------------------------------------
    // Property 8：重力属性（消行后无悬空空行）（验证需求 6.14）
    // -------------------------------------------------------------------------
    "Property 8: 消行后不存在空行位于非空行之上" {
        checkAll(PropTestConfig(iterations = 100), engineArb) { engine ->
            // Fill multiple rows to trigger line clears
            for (c in 0 until engine.cols) {
                engine.board[engine.rows - 1][c] = 1
                engine.board[engine.rows - 2][c] = 2
            }

            if (!engine.isGameOver) {
                engine.hardDrop()
            }

            // Find the first non-empty row from top
            var firstNonEmpty = -1
            for (r in 0 until engine.rows) {
                if (engine.board[r].any { it != 0 }) {
                    firstNonEmpty = r
                    break
                }
            }

            // All rows above firstNonEmpty must be empty
            if (firstNonEmpty > 0) {
                for (r in 0 until firstNonEmpty) {
                    val isEmpty = engine.board[r].all { it == 0 }
                    isEmpty shouldBe true
                }
            }

            // All rows from firstNonEmpty downward must not have an empty row
            // followed by a non-empty row (no floating gaps)
            if (firstNonEmpty >= 0) {
                var seenNonEmpty = false
                for (r in firstNonEmpty until engine.rows) {
                    val rowEmpty = engine.board[r].all { it == 0 }
                    if (!rowEmpty) seenNonEmpty = true
                    // Once we've seen a non-empty row, we should not see empty rows
                    // above non-empty rows — but since we scan top-down, check:
                    // if this row is empty but a later row is non-empty, that's a violation
                    // We check this by verifying no empty row exists between two non-empty rows
                }
                // Simpler check: after the first non-empty row, there should be no
                // empty row that has a non-empty row below it
                for (r in firstNonEmpty until engine.rows - 1) {
                    val currentEmpty = engine.board[r].all { it == 0 }
                    if (currentEmpty) {
                        // All rows below must also be empty
                        for (below in r + 1 until engine.rows) {
                            val belowEmpty = engine.board[below].all { it == 0 }
                            belowEmpty shouldBe true
                        }
                        break
                    }
                }
            }
        }
    }
})
