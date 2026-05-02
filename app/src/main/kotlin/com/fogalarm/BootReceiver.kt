package com.fogalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("fog_alarm", Context.MODE_PRIVATE)
        if (prefs.getBoolean("enabled", false)) {
            val interval = prefs.getInt("interval_minutes", 60).toLong()
            FogCheckWorker.schedule(context, interval)
        }
    }
}
