package com.zkx.aidemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

@Suppress("MagicNumber")
class TetrisBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val CELL_PADDING = 1f
    }

    var engine: TetrisEngine? = null

    private val bgPaint = Paint().apply { color = Color.parseColor("#1A1A2E") }
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#2A2A4E")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val pieceColors = intArrayOf(
        Color.parseColor("#1A1A2E"), // 0 = empty
        Color.parseColor("#00BCD4"), // 1 = I (cyan)
        Color.parseColor("#FFEB3B"), // 2 = O (yellow)
        Color.parseColor("#9C27B0"), // 3 = T (purple)
        Color.parseColor("#4CAF50"), // 4 = S (green)
        Color.parseColor("#F44336"), // 5 = Z (red)
        Color.parseColor("#2196F3"), // 6 = J (blue)
        Color.parseColor("#FF9800")  // 7 = L (orange)
    )

    private val cellPaint = Paint()
    private val borderPaint = Paint().apply {
        color = Color.parseColor("#00000040")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val eng = engine ?: return

        val cellW = width.toFloat() / eng.cols
        val cellH = height.toFloat() / eng.rows

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        for (r in 0 until eng.rows) {
            for (c in 0 until eng.cols) {
                val colorIdx = eng.board[r][c].coerceIn(0, pieceColors.size - 1)
                drawCell(canvas, r, c, cellW, cellH, colorIdx)
            }
        }

        if (!eng.isGameOver) {
            val colorIdx = (eng.currentPiece.shape.ordinal + 1).coerceIn(0, pieceColors.size - 1)
            for (cell in eng.currentPiece.cells()) {
                val r = cell[0]
                val c = cell[1]
                if (r in 0 until eng.rows && c in 0 until eng.cols) {
                    drawCell(canvas, r, c, cellW, cellH, colorIdx)
                }
            }
        }

        for (r in 0..eng.rows) {
            canvas.drawLine(0f, r * cellH, width.toFloat(), r * cellH, gridPaint)
        }
        for (c in 0..eng.cols) {
            canvas.drawLine(c * cellW, 0f, c * cellW, height.toFloat(), gridPaint)
        }
    }

    private fun drawCell(canvas: Canvas, row: Int, col: Int, cellW: Float, cellH: Float, colorIdx: Int) {
        val left = col * cellW + CELL_PADDING
        val top = row * cellH + CELL_PADDING
        val right = left + cellW - CELL_PADDING * 2
        val bottom = top + cellH - CELL_PADDING * 2
        cellPaint.color = pieceColors[colorIdx]
        canvas.drawRect(left, top, right, bottom, cellPaint)
        if (colorIdx != 0) {
            canvas.drawRect(left, top, right, bottom, borderPaint)
        }
    }
}
