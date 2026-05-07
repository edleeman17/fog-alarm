package com.fogalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val fogStartMs = intent.getLongExtra("fog_start_ms", 0L)
        DebugLogger.log(context, "ALARM_RX", "Received — fogStartMs=$fogStartMs")

        // Launch full-screen alarm activity — wakes screen, loops sound, shows on lock screen
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("fog_start_ms", fogStartMs)
        }

        // Notification required on Android 10+ to start activity from background
        ensureNotificationChannel(context)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Fog Alert")
            .setContentText("Tap to view")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .build()

        context.getSystemService<NotificationManager>()!!.notify(NOTIFICATION_ID, notification)
        try { context.startActivity(alarmIntent) } catch (_: Exception) {}
    }

    private fun ensureNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Fog Alarms", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when fog is expected"
            enableVibration(false) // AlarmActivity handles vibration
        }
        context.getSystemService<NotificationManager>()!!.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "fog_alarm_channel"
        const val NOTIFICATION_ID = 1
    }
}
