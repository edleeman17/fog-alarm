package com.fogalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService

class CheckScheduler(private val context: Context) {
    private val alarmManager: AlarmManager = context.getSystemService()!!

    fun schedule(intervalMinutes: Int = 60) {
        val triggerAt = System.currentTimeMillis() + intervalMinutes * 60_000L
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent())
        DebugLogger.log(context, "SCHEDULE", "Next check in ${intervalMinutes}min")
    }

    fun scheduleImmediate() {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 2_000L,
            pendingIntent()
        )
    }

    fun cancel() {
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE, Intent(context, CheckReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { alarmManager.cancel(it) }
        DebugLogger.log(context, "SCHEDULE", "Cancelled")
    }

    private fun pendingIntent() = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, CheckReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        private const val REQUEST_CODE = 42
    }
}
