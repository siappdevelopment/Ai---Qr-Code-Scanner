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
    private const val BEEP_MS = 200

    private val mainHandler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null
    private var toneStream: Int = AudioManager.STREAM_MUSIC

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
            beepOnce(context.applicationContext)
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

    private fun beepOnce(context: Context) {
        mainHandler.post {
            try {
                val stream = audibleStream(context)
                if (toneGenerator == null || toneStream != stream) {
                    toneGenerator?.release()
                    toneGenerator = ToneGenerator(stream, 100)
                    toneStream = stream
                }
                val started = toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_MS) == true
                if (!started) {
                    toneGenerator?.release()
                    toneGenerator = ToneGenerator(stream, 100)
                    toneStream = stream
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_MS)
                }
            } catch (t: Throwable) {
                toneGenerator = null
                Log.w(TAG, "beep failed", t)
            }
        }
    }

    private fun audibleStream(context: Context): Int {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return AudioManager.STREAM_MUSIC
        return when {
            audio.getStreamVolume(AudioManager.STREAM_MUSIC) > 0 -> AudioManager.STREAM_MUSIC
            audio.getStreamVolume(AudioManager.STREAM_RING) > 0 -> AudioManager.STREAM_RING
            audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION) > 0 -> AudioManager.STREAM_NOTIFICATION
            else -> AudioManager.STREAM_MUSIC
        }
    }
}
