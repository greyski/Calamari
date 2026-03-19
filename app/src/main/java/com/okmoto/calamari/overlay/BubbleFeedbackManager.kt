package com.okmoto.calamari.overlay

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central place for sound and haptic feedback used by the floating bubble.
 *
 * This keeps [MainBubbleService] ignorant of the underlying Android
 * audio/vibration APIs and makes it easy to tweak the UX in one location.
 */
interface BubbleFeedbackPlayer {
    fun playWakeFeedback(context: Context)
    fun playTitleCapturedFeedback(context: Context)
}

@Singleton
class BubbleFeedbackManager @Inject constructor() : BubbleFeedbackPlayer {
    /**
     * Feedback used when the hot word has been detected and we transition
     * into [ListeningState.AWAITING_EVENT].
     */
    override fun playWakeFeedback(context: Context) {
        playHaptic(context)
    }

    /**
     * Feedback used when we have successfully captured an event title and
     * transition into [ListeningState.IDLE_SEND] (awaiting send).
     */
    override fun playTitleCapturedFeedback(context: Context) {
        playHaptic(context)
    }

    private fun playHaptic(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return

        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        } else {
            VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        @Suppress("DEPRECATION")
        vibrator.vibrate(effect)
    }
}

