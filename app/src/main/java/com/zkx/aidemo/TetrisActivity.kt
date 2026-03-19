package com.zkx.aidemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class TetrisActivity : AppCompatActivity() {

    private val engine = TetrisEngine()
    private lateinit var boardView: TetrisBoardView
    private lateinit var nextPieceView: NextPieceView
    private lateinit var tvScore: TextView
    private lateinit var tvLevel: TextView
    private lateinit var btnPause: Button

    private val handler = Handler(Looper.getMainLooper())
    private var gameLoopRunning = false

    private val gameLoop = object : Runnable {
        override fun run() {
            if (!gameLoopRunning) return
            val result = engine.tick()
            refreshUi()
            when (result) {
                TetrisEngine.TickResult.GAME_OVER -> showGameOverDialog()
                else -> handler.postDelayed(this, engine.dropInterval)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tetris)

        boardView = findViewById(R.id.board_view)
        nextPieceView = findViewById(R.id.next_piece_view)
        tvScore = findViewById(R.id.tv_score)
        tvLevel = findViewById(R.id.tv_level)
        btnPause = findViewById(R.id.btn_pause)

        boardView.engine = engine

        findViewById<Button>(R.id.btn_left).setOnClickListener {
            engine.moveLeft(); refreshUi()
        }
        findViewById<Button>(R.id.btn_right).setOnClickListener {
            engine.moveRight(); refreshUi()
        }
        findViewById<Button>(R.id.btn_rotate).setOnClickListener {
            engine.rotate(); refreshUi()
        }
        findViewById<Button>(R.id.btn_hard_drop).setOnClickListener {
            engine.hardDrop()
            refreshUi()
            if (engine.isGameOver) showGameOverDialog()
        }
        btnPause.setOnClickListener { togglePause() }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        engine.start()
        startGameLoop()
        refreshUi()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onPause() {
        super.onPause()
        stopGameLoop()
        engine.pause()
    }

    override fun onResume() {
        super.onResume()
        if (!engine.isGameOver && engine.isPaused) {
            engine.resume()
            startGameLoop()
            updatePauseButton()
        }
    }

    private fun startGameLoop() {
        if (gameLoopRunning) return
        gameLoopRunning = true
        handler.postDelayed(gameLoop, engine.dropInterval)
    }

    private fun stopGameLoop() {
        gameLoopRunning = false
        handler.removeCallbacks(gameLoop)
    }

    private fun togglePause() {
        if (engine.isGameOver) return
        if (engine.isPaused) {
            engine.resume()
            startGameLoop()
        } else {
            engine.pause()
            stopGameLoop()
        }
        updatePauseButton()
    }

    private fun updatePauseButton() {
        btnPause.text = if (engine.isPaused) {
            getString(R.string.btn_resume)
        } else {
            getString(R.string.btn_pause)
        }
    }

    private fun refreshUi() {
        tvScore.text = getString(R.string.label_score, engine.score)
        tvLevel.text = getString(R.string.label_level, engine.level)
        nextPieceView.piece = engine.nextPiece
        nextPieceView.invalidate()
        boardView.invalidate()
    }

    private fun showGameOverDialog() {
        stopGameLoop()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_game_over))
            .setMessage(getString(R.string.dialog_tetris_game_over_message, engine.score))
            .setPositiveButton(getString(R.string.btn_restart)) { _, _ -> restartGame() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun restartGame() {
        stopGameLoop()
        engine.reset()
        updatePauseButton()
        refreshUi()
        startGameLoop()
    }
}
