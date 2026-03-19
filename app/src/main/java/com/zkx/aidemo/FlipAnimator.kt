package com.zkx.aidemo

import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View

object FlipAnimator {
    private const val FLIP_DURATION = 150L
    private const val ROTATION_START = 0f
    private const val ROTATION_MID_OUT = 90f
    private const val ROTATION_MID_IN = -90f

    var isAnimating: Boolean = false

    fun flip(view: View, onEnd: () -> Unit) {
        if (isAnimating) return
        isAnimating = true

        val firstHalf = ObjectAnimator.ofFloat(view, "rotationY", ROTATION_START, ROTATION_MID_OUT).apply {
            duration = FLIP_DURATION
        }
        val secondHalf = ObjectAnimator.ofFloat(view, "rotationY", ROTATION_MID_IN, ROTATION_START).apply {
            duration = FLIP_DURATION
        }

        firstHalf.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                secondHalf.start()
            }
        })

        secondHalf.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isAnimating = false
                onEnd()
            }
        })

        firstHalf.start()
    }
}
