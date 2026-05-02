package com.fogalarm

import android.content.Context
import androidx.work.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class FogCheckWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("fog_alarm", Context.MODE_PRIVATE)
        val location = LocationHelper(applicationContext).getLastKnownLocation()
            ?: return Result.retry()

        val fogEvent = try {
            WeatherRepository().checkForFog(location.latitude, location.longitude)
        } catch (e: Exception) {
            return Result.retry()
        }

        prefs.edit()
            .putString("last_check", SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date()))
            .apply()

        if (fogEvent != null) {
            val fogStartMs = parseFogTime(fogEvent.isoTime, fogEvent.hourIndex)
            val leadTime = prefs.getInt("lead_time_minutes", 60)
            AlarmScheduler(applicationContext).scheduleAlarm(fogStartMs, leadTime)
            prefs.edit().putString("status", "Fog expected at ${fogEvent.isoTime.takeLast(5)}").apply()
        } else {
            prefs.edit().putString("status", "No fog in next 3 hours").apply()
        }

        return Result.success()
    }

    private fun parseFogTime(isoTime: String, hourIndex: Int): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                .parse(isoTime)?.time
                ?: fallbackFogTime(hourIndex)
        } catch (e: Exception) {
            fallbackFogTime(hourIndex)
        }
    }

    private fun fallbackFogTime(hourIndex: Int) =
        System.currentTimeMillis() + (hourIndex + 1) * 3_600_000L

    companion object {
        private const val WORK_NAME = "fog_check"

        fun schedule(context: Context, intervalMinutes: Long = 60) {
            val request = PeriodicWorkRequestBuilder<FogCheckWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
