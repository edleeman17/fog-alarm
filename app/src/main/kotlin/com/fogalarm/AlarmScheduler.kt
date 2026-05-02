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
        val alarmTime = calculateAlarmTime(fogStartEpochMs, leadTimeMinutes)
        val now = System.currentTimeMillis()

        if (alarmTime < now + 5 * 60_000L) {
            DebugLogger.log(context, "ALARM", "Skipped scheduling — alarm time too soon ($alarmTime)")
            return
        }

        val existing = prefs.getLong("scheduled_alarm_ms", 0L)
        if (existing == alarmTime) {
            DebugLogger.log(context, "ALARM", "Skipped — same alarm already scheduled")
            return
        }

        val pendingIntent = buildPendingIntent(fogStartEpochMs)
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(alarmTime, pendingIntent), pendingIntent)
        prefs.edit().putLong("scheduled_alarm_ms", alarmTime).apply()
        DebugLogger.log(context, "ALARM", "Scheduled for $alarmTime (fog at $fogStartEpochMs, lead ${leadTimeMinutes}min)")
    }

    fun scheduleDebugAlarm() {
        val alarmTime = System.currentTimeMillis() + 5_000L
        val pendingIntent = buildPendingIntent(alarmTime)
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(alarmTime, pendingIntent), pendingIntent)
        prefs.edit().putLong("scheduled_alarm_ms", alarmTime).apply()
        DebugLogger.log(context, "ALARM", "Debug alarm scheduled in 5s")
    }

    fun cancelAlarm() {
        alarmManager.cancel(buildPendingIntent(0L))
        prefs.edit().remove("scheduled_alarm_ms").apply()
        DebugLogger.log(context, "ALARM", "Cancelled")
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

    companion object {
        internal fun calculateAlarmTime(fogStartEpochMs: Long, leadTimeMinutes: Int): Long =
            fogStartEpochMs - (leadTimeMinutes * 60_000L)
    }
}
