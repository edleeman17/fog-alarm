package com.fogalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val fogStartMs = intent.getLongExtra("fog_start_ms", 0L)
        val fogTime = if (fogStartMs > 0) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(fogStartMs))
        } else "soon"

        ensureNotificationChannel(context)

        val openAppIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Fog Alert")
            .setContentText("Fog expected around $fogTime — time to get up")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(openAppIntent, true)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .build()

        context.getSystemService<NotificationManager>()!!.notify(NOTIFICATION_ID, notification)

        @Suppress("DEPRECATION")
        context.getSystemService<Vibrator>()?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 1000, 500, 1000), -1)
        )
    }

    private fun ensureNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Fog Alarms", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when fog is expected"
            enableVibration(true)
        }
        context.getSystemService<NotificationManager>()!!.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "fog_alarm_channel"
        const val NOTIFICATION_ID = 1
    }
}
