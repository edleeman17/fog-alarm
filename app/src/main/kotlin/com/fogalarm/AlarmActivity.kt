package com.fogalarm

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force past lock screen and wake the display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(R.layout.activity_alarm)

        val fogStartMs = intent.getLongExtra("fog_start_ms", 0L)
        val fogTime = if (fogStartMs > 0) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(fogStartMs))
        } else "soon"

        findViewById<TextView>(R.id.alarm_fog_time).text = "Fog expected at $fogTime"
        findViewById<Button>(R.id.btn_dismiss).setOnClickListener { dismiss() }

        startAlarmSound()
        startVibration()
    }

    private fun startAlarmSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlarmActivity, uri)
            isLooping = true
            prepare()
            start()
        }
    }

    private fun startVibration() {
        vibrator = getSystemService()
        val pattern = longArrayOf(0, 1000, 500, 1000, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun dismiss() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        vibrator?.cancel()
    }
}
