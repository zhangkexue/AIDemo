package com.zkx.aidemo.entertainment.game1024

import kotlin.random.Random

@Suppress("MagicNumber")
class Game1024Engine {
    enum class Direction { UP, DOWN, LEFT, RIGHT }

    companion object {
        const val BOARD_SIZE = 4
        const val WIN_TILE = 1024
        const val NEW_TILE_4_CHANCE = 0.9f
    }

    val board: Array<IntArray> = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) }
    var score: Int = 0
    var isWin: Boolean = false
    var isGameOver: Boolean = false

    init {
        reset()
    }

    fun reset() {
        for (row in board) row.fill(0)
        score = 0
        isWin = false
        isGameOver = false
        spawnTile()
        spawnTile()
    }

    fun move(direction: Direction): Boolean {
        val snapshot = Array(BOARD_SIZE) { board[it].copyOf() }

        when (direction) {
            Direction.LEFT -> {
                for (r in 0 until BOARD_SIZE) {
                    val (newLine, delta) = mergeLine(board[r])
                    board[r] = newLine
                    score += delta
                }
            }
            Direction.RIGHT -> {
                for (r in 0 until BOARD_SIZE) {
                    val reversed = board[r].reversedArray()
                    val (merged, delta) = mergeLine(reversed)
                    board[r] = merged.reversedArray()
                    score += delta
                }
            }
            Direction.UP -> {
                transpose()
                for (r in 0 until BOARD_SIZE) {
                    val (newLine, delta) = mergeLine(board[r])
                    board[r] = newLine
                    score += delta
                }
                transpose()
            }
            Direction.DOWN -> {
                transpose()
                for (r in 0 until BOARD_SIZE) {
                    val reversed = board[r].reversedArray()
                    val (merged, delta) = mergeLine(reversed)
                    board[r] = merged.reversedArray()
                    score += delta
                }
                transpose()
            }
        }

        val changed = snapshot.indices.any { r -> !snapshot[r].contentEquals(board[r]) }
        if (changed) {
            spawnTile()
            checkGameState()
        }
        return changed
    }

    fun checkGameState() {
        isWin = board.any { row -> row.any { it >= WIN_TILE } }

        val full = board.all { row -> row.all { it != 0 } }
        if (!full) {
            isGameOver = false
            return
        }

        // Board is full — check if any direction allows a merge
        isGameOver = Direction.entries.none { dir -> canMove(dir) }
    }

    private fun canMove(direction: Direction): Boolean {
        return when (direction) {
            Direction.LEFT -> (0 until BOARD_SIZE).any { r ->
                val (newLine, _) = mergeLine(board[r])
                !newLine.contentEquals(board[r])
            }
            Direction.RIGHT -> (0 until BOARD_SIZE).any { r ->
                val reversed = board[r].reversedArray()
                val (merged, _) = mergeLine(reversed)
                !merged.reversedArray().contentEquals(board[r])
            }
            Direction.UP -> {
                val temp = Array(BOARD_SIZE) { board[it].copyOf() }
                transposeArray(temp)
                (0 until BOARD_SIZE).any { r ->
                    val (newLine, _) = mergeLine(temp[r])
                    !newLine.contentEquals(temp[r])
                }
            }
            Direction.DOWN -> {
                val temp = Array(BOARD_SIZE) { board[it].copyOf() }
                transposeArray(temp)
                (0 until BOARD_SIZE).any { r ->
                    val reversed = temp[r].reversedArray()
                    val (merged, _) = mergeLine(reversed)
                    !merged.reversedArray().contentEquals(temp[r])
                }
            }
        }
    }

    private fun spawnTile() {
        val empty = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (board[r][c] == 0) empty.add(r to c)
            }
        }
        if (empty.isEmpty()) return
        val (r, c) = empty[Random.nextInt(empty.size)]
        board[r][c] = if (Random.nextFloat() < NEW_TILE_4_CHANCE) 2 else 4
    }

    internal fun mergeLine(line: IntArray): Pair<IntArray, Int> {
        val nonZero = line.filter { it != 0 }.toMutableList()
        val result = IntArray(BOARD_SIZE)
        var delta = 0
        var i = 0
        var j = 0
        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val merged = nonZero[i] * 2
                result[j++] = merged
                delta += merged
                i += 2
            } else {
                result[j++] = nonZero[i]
                i++
            }
        }
        return result to delta
    }

    private fun transpose() {
        transposeArray(board)
    }

    private fun transposeArray(arr: Array<IntArray>) {
        for (r in 0 until BOARD_SIZE) {
            for (c in r + 1 until BOARD_SIZE) {
                val tmp = arr[r][c]
                arr[r][c] = arr[c][r]
                arr[c][r] = tmp
            }
        }
    }
}
