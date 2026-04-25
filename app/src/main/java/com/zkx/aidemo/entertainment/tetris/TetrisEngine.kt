package com.zkx.aidemo.entertainment.tetris

import kotlin.random.Random

@Suppress("MagicNumber")
class TetrisEngine(val cols: Int = 10, val rows: Int = 20) {

    enum class TickResult { MOVED, LOCKED, GAME_OVER }

    companion object {
        const val SCORE_PER_LEVEL = 1000
        const val POINTS_1_LINE = 100
        const val POINTS_2_LINES = 300
        const val POINTS_3_LINES = 500
        const val POINTS_4_LINES = 800
        const val MIN_DROP_INTERVAL = 100L
        const val BASE_DROP_INTERVAL = 1000L
        const val INTERVAL_STEP = 100L
        const val ROTATIONS = 4
    }

    val board: Array<IntArray> = Array(rows) { IntArray(cols) }
    var currentPiece: TetrisPiece = randomPiece()
    var nextPiece: TetrisPiece = randomPiece()
    var score: Int = 0
    var level: Int = 1
    var isGameOver: Boolean = false
    var isPaused: Boolean = false

    val dropInterval: Long get() = maxOf(MIN_DROP_INTERVAL, BASE_DROP_INTERVAL - (level - 1) * INTERVAL_STEP)

    fun start() { reset() }

    fun reset() {
        for (row in board) row.fill(0)
        score = 0
        level = 1
        isGameOver = false
        isPaused = false
        currentPiece = randomPiece()
        nextPiece = randomPiece()
    }

    fun pause() { isPaused = true }
    fun resume() { isPaused = false }

    fun moveLeft(): Boolean {
        if (isGameOver || isPaused) return false
        return if (canMove(currentPiece, -1, 0)) {
            currentPiece.x--
            true
        } else false
    }

    fun moveRight(): Boolean {
        if (isGameOver || isPaused) return false
        return if (canMove(currentPiece, 1, 0)) {
            currentPiece.x++
            true
        } else false
    }

    fun rotate(): Boolean {
        if (isGameOver || isPaused) return false
        return if (canRotate(currentPiece)) {
            currentPiece.rotation = (currentPiece.rotation + 1) % ROTATIONS
            true
        } else false
    }

    fun hardDrop() {
        if (isGameOver || isPaused) return
        while (canMove(currentPiece, 0, 1)) {
            currentPiece.y++
        }
        lockPiece()
    }

    fun tick(): TickResult {
        if (isGameOver || isPaused) return TickResult.MOVED
        return if (canMove(currentPiece, 0, 1)) {
            currentPiece.y++
            TickResult.MOVED
        } else {
            lockPiece()
        }
    }

    private fun lockPiece(): TickResult {
        val colorIndex = currentPiece.shape.ordinal + 1
        for (cell in currentPiece.cells()) {
            val r = cell[0]
            val c = cell[1]
            if (r in 0 until rows && c in 0 until cols) {
                board[r][c] = colorIndex
            }
        }

        val cleared = clearLines()
        val points = when (cleared) {
            1 -> POINTS_1_LINE * level
            2 -> POINTS_2_LINES * level
            3 -> POINTS_3_LINES * level
            4 -> POINTS_4_LINES * level
            else -> 0
        }
        score += points

        while (score >= level * SCORE_PER_LEVEL) {
            level++
        }

        currentPiece = nextPiece
        nextPiece = randomPiece()

        if (!canMove(currentPiece, 0, 0)) {
            isGameOver = true
            return TickResult.GAME_OVER
        }

        return TickResult.LOCKED
    }

    private fun clearLines(): Int {
        var cleared = 0
        var r = rows - 1
        while (r >= 0) {
            if (board[r].all { it != 0 }) {
                for (above in r downTo 1) {
                    board[above] = board[above - 1].copyOf()
                }
                board[0] = IntArray(cols)
                cleared++
            } else {
                r--
            }
        }
        return cleared
    }

    private fun canMove(piece: TetrisPiece, dx: Int, dy: Int): Boolean {
        for (cell in piece.cells()) {
            val nr = cell[0] + dy
            val nc = cell[1] + dx
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) return false
            if (board[nr][nc] != 0) return false
        }
        return true
    }

    private fun canRotate(piece: TetrisPiece): Boolean {
        val rotated = piece.copy(rotation = (piece.rotation + 1) % ROTATIONS)
        for (cell in rotated.cells()) {
            val r = cell[0]
            val c = cell[1]
            if (r < 0 || r >= rows || c < 0 || c >= cols) return false
            if (board[r][c] != 0) return false
        }
        return true
    }

    private fun randomPiece(): TetrisPiece {
        val shape = PieceShape.entries[Random.nextInt(PieceShape.entries.size)]
        return TetrisPiece(shape = shape, x = cols / 2 - 2, y = 0)
    }
}
