package com.zkx.aidemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

@Suppress("MagicNumber")
class Game1024BoardView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    companion object {
        private const val BOARD_SIZE = 4
        private const val PADDING_DP = 8f
        private const val CORNER_RADIUS_DP = 8f
        private const val GRID_CELLS = 5f
        private const val FONT_SCALE_SMALL = 0.45f
        private const val FONT_SCALE_MED = 0.35f
        private const val FONT_SCALE_LARGE = 0.28f
        private const val THRESHOLD_2_DIGITS = 100
        private const val THRESHOLD_3_DIGITS = 1000
    }

    var engine: Game1024Engine? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BBADA0")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val tileRect = RectF()

    private fun tileColor(value: Int): Int = when (value) {
        0 -> Color.parseColor("#CDC1B4")
        2 -> Color.parseColor("#EEE4DA")
        4 -> Color.parseColor("#EDE0C8")
        8 -> Color.parseColor("#F2B179")
        16 -> Color.parseColor("#F59563")
        32 -> Color.parseColor("#F67C5F")
        64 -> Color.parseColor("#F65E3B")
        128 -> Color.parseColor("#EDCF72")
        256 -> Color.parseColor("#EDCC61")
        512 -> Color.parseColor("#EDC850")
        1024 -> Color.parseColor("#EDC53F")
        else -> Color.parseColor("#3C3A32")
    }

    private fun textColor(value: Int): Int =
        if (value <= 4) Color.parseColor("#776E65") else Color.parseColor("#F9F6F0")

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val density = resources.displayMetrics.density
        val padding = PADDING_DP * density
        val cornerRadius = CORNER_RADIUS_DP * density

        // Draw board background
        canvas.drawRoundRect(0f, 0f, w, h, cornerRadius * 2, cornerRadius * 2, backgroundPaint)

        val tileSize = minOf((w - padding * GRID_CELLS) / BOARD_SIZE, (h - padding * GRID_CELLS) / BOARD_SIZE)

        val boardWidth = tileSize * BOARD_SIZE + padding * GRID_CELLS
        val boardHeight = tileSize * BOARD_SIZE + padding * GRID_CELLS
        val offsetX = (w - boardWidth) / 2f
        val offsetY = (h - boardHeight) / 2f

        val board = engine?.board ?: return

        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val value = board[row][col]
                val left = offsetX + padding + col * (tileSize + padding)
                val top = offsetY + padding + row * (tileSize + padding)
                val right = left + tileSize
                val bottom = top + tileSize

                tileRect.set(left, top, right, bottom)
                tilePaint.color = tileColor(value)
                canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, tilePaint)

                if (value != 0) {
                    val fontSize = when {
                        value < THRESHOLD_2_DIGITS -> tileSize * FONT_SCALE_SMALL
                        value < THRESHOLD_3_DIGITS -> tileSize * FONT_SCALE_MED
                        else -> tileSize * FONT_SCALE_LARGE
                    }
                    textPaint.textSize = fontSize
                    textPaint.color = textColor(value)
                    textPaint.isFakeBoldText = true
                    val textY = top + tileSize / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
                    canvas.drawText(value.toString(), left + tileSize / 2f, textY, textPaint)
                }
            }
        }
    }
}
