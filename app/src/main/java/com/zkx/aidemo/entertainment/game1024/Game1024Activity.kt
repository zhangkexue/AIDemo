package com.zkx.aidemo.entertainment.game1024

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class Game1024Activity : AppCompatActivity() {

    private val engine = Game1024Engine()
    private lateinit var boardView: Game1024BoardView
    private lateinit var tvScore: TextView
    private lateinit var btnRestart: Button
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game1024)

        tvScore = findViewById(R.id.tv_score)
        boardView = findViewById(R.id.board_view)
        btnRestart = findViewById(R.id.btn_restart)

        boardView.engine = engine
        updateScore()

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val dx = e2.x - (e1?.x ?: e2.x)
                val dy = e2.y - (e1?.y ?: e2.y)
                val direction = if (abs(dx) > abs(dy)) {
                    if (dx > 0) Game1024Engine.Direction.RIGHT else Game1024Engine.Direction.LEFT
                } else {
                    if (dy > 0) Game1024Engine.Direction.DOWN else Game1024Engine.Direction.UP
                }
                engine.move(direction)
                boardView.invalidate()
                updateScore()
                checkGameState()
                return true
            }
        })

        boardView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        btnRestart.setOnClickListener {
            restartGame()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun updateScore() {
        tvScore.text = getString(R.string.title_game_1024) + "  " + engine.score
    }

    private fun checkGameState() {
        if (engine.isWin) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_win_title))
                .setMessage(getString(R.string.dialog_win_message))
                .setPositiveButton(getString(R.string.btn_continue)) { dialog, _ -> dialog.dismiss() }
                .setNegativeButton(getString(R.string.btn_restart)) { _, _ -> restartGame() }
                .show()
        } else if (engine.isGameOver) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_game_over))
                .setMessage(getString(R.string.title_game_1024) + "  " + engine.score)
                .setPositiveButton(getString(R.string.btn_restart)) { _, _ -> restartGame() }
                .show()
        }
    }

    private fun restartGame() {
        engine.reset()
        boardView.invalidate()
        updateScore()
    }
}
