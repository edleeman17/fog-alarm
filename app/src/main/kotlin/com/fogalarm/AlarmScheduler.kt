package com.fogalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService

class AlarmScheduler(private val context: Context) {
    private val prefs = context.getSharedPreferences("fog_alarm", Context.MODE_PRIVATE)
    private val alarmManager: AlarmManager = context.getSystemService()!!

    fun scheduleAlarm(fogStartEpochMs: Long, leadTimeMinutes: Int) {
        val alarmTime = fogStartEpochMs - (leadTimeMinutes * 60_000L)
        val now = System.currentTimeMillis()

        if (alarmTime < now + 5 * 60_000L) return

        val existing = prefs.getLong("scheduled_alarm_ms", 0L)
        if (existing == alarmTime) return

        val pendingIntent = buildPendingIntent(fogStartEpochMs)
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(alarmTime, pendingIntent), pendingIntent)
        prefs.edit().putLong("scheduled_alarm_ms", alarmTime).apply()
    }

    fun cancelAlarm() {
        alarmManager.cancel(buildPendingIntent(0L))
        prefs.edit().remove("scheduled_alarm_ms").apply()
    }

    fun getScheduledAlarmMs(): Long = prefs.getLong("scheduled_alarm_ms", 0L)

    private fun buildPendingIntent(fogStartMs: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("fog_start_ms", fogStartMs)
        }
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
