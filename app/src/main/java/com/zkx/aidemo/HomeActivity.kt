package com.zkx.aidemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupCard(R.id.card_news) {
            startActivity(Intent(this, NewsActivity::class.java))
        }
        setupCard(R.id.card_finance) {
            startActivity(Intent(this, FinanceActivity::class.java))
        }
        setupCard(R.id.card_ai) {
            startActivity(Intent(this, AiActivity::class.java))
        }
        setupCard(R.id.card_entertainment) {
            startActivity(Intent(this, EntertainmentActivity::class.java))
        }
    }

    private fun setupCard(cardId: Int, navigate: () -> Unit) {
        val card = findViewById<MaterialCardView>(cardId)
        card.setOnClickListener { view ->
            FlipAnimator.flip(view) {
                try {
                    navigate()
                } catch (e: Exception) {
                    view.rotationY = 0f
                    FlipAnimator.isAnimating = false
                }
            }
        }
    }
}
