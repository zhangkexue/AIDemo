package com.zkx.aidemo.entertainment.tetris

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

@Suppress("MagicNumber")
class NextPieceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val GRID_SIZE = 4
        private const val CELL_PADDING = 1f
    }

    var piece: TetrisPiece? = null

    private val bgPaint = Paint().apply { color = Color.parseColor("#2A2A4E") }
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val pieceColors = intArrayOf(
        Color.parseColor("#2A2A4E"), // 0 = empty
        Color.parseColor("#00BCD4"), // 1 = I
        Color.parseColor("#FFEB3B"), // 2 = O
        Color.parseColor("#9C27B0"), // 3 = T
        Color.parseColor("#4CAF50"), // 4 = S
        Color.parseColor("#F44336"), // 5 = Z
        Color.parseColor("#2196F3"), // 6 = J
        Color.parseColor("#FF9800")  // 7 = L
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        val p = piece ?: return

        // Render in a GRID_SIZE×GRID_SIZE grid centred in the view
        val cellSize = minOf(width, height) / GRID_SIZE.toFloat()
        val offsets = PIECE_SHAPES[p.shape]!![0]
        val colorIdx = (p.shape.ordinal + 1).coerceIn(0, pieceColors.size - 1)
        cellPaint.color = pieceColors[colorIdx]

        // Find bounding box to centre the piece
        val minRow = offsets.minOf { it[0] }
        val maxRow = offsets.maxOf { it[0] }
        val minCol = offsets.minOf { it[1] }
        val maxCol = offsets.maxOf { it[1] }
        val pieceH = (maxRow - minRow + 1) * cellSize
        val pieceW = (maxCol - minCol + 1) * cellSize
        val startX = (width - pieceW) / 2f
        val startY = (height - pieceH) / 2f

        for (cell in offsets) {
            val left = startX + (cell[1] - minCol) * cellSize + CELL_PADDING
            val top = startY + (cell[0] - minRow) * cellSize + CELL_PADDING
            canvas.drawRect(left, top, left + cellSize - CELL_PADDING * 2, top + cellSize - CELL_PADDING * 2, cellPaint)
        }
    }
}
