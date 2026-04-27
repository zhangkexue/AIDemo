package com.zkx.aidemo.entertainment.tetris

enum class PieceShape { I, O, T, S, Z, J, L }

@Suppress("MagicNumber")
val PIECE_SHAPES: Map<PieceShape, Array<Array<IntArray>>> = mapOf(
    PieceShape.I to arrayOf(
        arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(1, 3)),
        arrayOf(intArrayOf(0, 2), intArrayOf(1, 2), intArrayOf(2, 2), intArrayOf(3, 2)),
        arrayOf(intArrayOf(2, 0), intArrayOf(2, 1), intArrayOf(2, 2), intArrayOf(2, 3)),
        arrayOf(intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(3, 1))
    ),
    PieceShape.O to arrayOf(
        arrayOf(intArrayOf(0, 0), intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, 1)),
        arrayOf(intArrayOf(0, 0), intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, 1)),
        arrayOf(intArrayOf(0, 0), intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, 1)),
        arrayOf(intArrayOf(0, 0), intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, 1))
    ),
    PieceShape.T to arrayOf(
        arrayOf(intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, 2)),
        arrayOf(intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(2, 1)),
        arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(2, 1)),
        arrayOf(intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(2, 1))
    ),
    PieceShape.S to arrayOf(
        arrayOf(intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(1, 0), intArrayOf(1, 1)),
        arrayOf(intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(2, 2)),
        arrayOf(intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(2, 0), intArrayOf(2, 1)),
        arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(2, 1))
    ),
    PieceShape.Z to arrayOf(
        arrayOf(intArrayOf(0, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 2)),
        arrayOf(intArrayOf(0, 2), intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(2, 1)),
        arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(2, 2)),
        arrayOf(intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(2, 0))
    ),
    PieceShape.J to arrayOf(
        arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, 2)),
        arrayOf(intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(1, 1), intArrayOf(2, 1)),
        arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(2, 2)),
        arrayOf(intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(2, 0), intArrayOf(2, 1))
    ),
    PieceShape.L to arrayOf(
        arrayOf(intArrayOf(0, 2), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, 2)),
        arrayOf(intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(2, 2)),
        arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(2, 0)),
        arrayOf(intArrayOf(0, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(2, 1))
    )
)

data class TetrisPiece(
    val shape: PieceShape,
    var x: Int,
    var y: Int,
    var rotation: Int = 0
) {
    @Suppress("MagicNumber")
    fun cells(): Array<IntArray> {
        val offsets = PIECE_SHAPES[shape]!![rotation % 4]
        return Array(4) { i -> intArrayOf(y + offsets[i][0], x + offsets[i][1]) }
    }
}
