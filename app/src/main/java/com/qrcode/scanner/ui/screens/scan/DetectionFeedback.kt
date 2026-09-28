package com.qrcode.scanner.ui.screens.scan

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * One-shot detection feedback for an accepted live-camera scan.
 * Preference gating is the caller's responsibility; this never loops.
 */
internal object DetectionFeedback {
    private const val TAG = "DetectionFeedback"
    private const val VIBRATE_MS = 60L
    private const val BEEP_MS = 150
    private const val TONE_RELEASE_DELAY_MS = 220L

    fun onAcceptedDetection(
        context: Context,
        vibrateEnabled: Boolean,
        beepEnabled: Boolean
    ) {
        Log.i(TAG, "accepted detection vibrate=$vibrateEnabled beep=$beepEnabled")
        if (vibrateEnabled) {
            vibrateOnce(context.applicationContext)
        }
        if (beepEnabled) {
            beepOnce()
        }
    }

    private fun vibrateOnce(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(VIBRATE_MS, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VIBRATE_MS)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "vibrate failed", t)
        }
    }

    private fun beepOnce() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_MS)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    toneGenerator.release()
                } catch (_: Throwable) {
                    // already released
                }
            }, TONE_RELEASE_DELAY_MS)
        } catch (t: Throwable) {
            Log.w(TAG, "beep failed", t)
        }
    }
}
