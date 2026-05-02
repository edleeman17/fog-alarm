package com.fogalarm

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FogCheckWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("fog_alarm", Context.MODE_PRIVATE)
        DebugLogger.log(applicationContext, "WORKER", "Check started")

        val location = LocationHelper(applicationContext).getLastKnownLocation()
        if (location == null) {
            DebugLogger.log(applicationContext, "WORKER", "No location available — retrying")
            return Result.retry()
        }
        DebugLogger.log(applicationContext, "WORKER", "Location: ${location.latitude}, ${location.longitude}")

        val fogEvent = try {
            WeatherRepository.create(applicationContext).checkForFog(location.latitude, location.longitude)
        } catch (e: Exception) {
            DebugLogger.log(applicationContext, "WORKER", "API error: ${e.message}")
            return Result.retry()
        }

        val timestamp = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date())
        prefs.edit().putString("last_check", timestamp).apply()

        if (fogEvent != null) {
            val scheduler = AlarmScheduler(applicationContext)
            if (prefs.getString("debug_server_url", null) != null) {
                scheduler.scheduleDebugAlarm()
            } else {
                val fogStartMs = parseFogTime(fogEvent.isoTime, fogEvent.hourIndex)
                val leadTime = prefs.getInt("lead_time_minutes", 60)
                scheduler.scheduleAlarm(fogStartMs, leadTime)
            }
            val status = "Fog expected at ${fogEvent.isoTime.takeLast(5)}"
            prefs.edit().putString("status", status).apply()
            DebugLogger.log(applicationContext, "WORKER", "Fog detected: $status (code index ${fogEvent.hourIndex})")
        } else {
            prefs.edit().putString("status", "No fog in next 3 hours").apply()
            DebugLogger.log(applicationContext, "WORKER", "No fog detected")
        }

        // Check for app update once per day
        val lastUpdateCheck = prefs.getLong("last_update_check", 0L)
        if (System.currentTimeMillis() - lastUpdateCheck > 24 * 3600_000L) {
            UpdateChecker(applicationContext).check()
            prefs.edit().putLong("last_update_check", System.currentTimeMillis()).apply()
        }

        return Result.success()
    }

    private fun parseFogTime(isoTime: String, hourIndex: Int): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                .parse(isoTime)?.time ?: fallbackFogTime(hourIndex)
        } catch (e: Exception) {
            DebugLogger.log(applicationContext, "WORKER", "Failed to parse fog time '$isoTime': ${e.message}")
            fallbackFogTime(hourIndex)
        }
    }

    private fun fallbackFogTime(hourIndex: Int) =
        System.currentTimeMillis() + (hourIndex + 1) * 3_600_000L

}
