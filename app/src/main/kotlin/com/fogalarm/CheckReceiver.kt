package com.fogalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

class CheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("fog_alarm", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return

        val request = OneTimeWorkRequestBuilder<FogCheckWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("fog_check_one_shot", ExistingWorkPolicy.KEEP, request)

        val interval = prefs.getInt("interval_minutes", 60)
        CheckScheduler(context).schedule(interval)
        DebugLogger.log(context, "SCHEDULE", "Woken by alarm, next in ${interval}min")
    }
}
