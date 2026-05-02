package com.fogalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class UpdateChecker(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun check() {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/edleeman17/fog-alarm/releases/latest")
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            val body = client.newCall(request).execute().body?.string() ?: return
            val tag = JsonParser.parseString(body).asJsonObject.get("tag_name")?.asString ?: return
            val latestCode = tag.removePrefix("v").toIntOrNull() ?: return

            DebugLogger.log(context, "UPDATE", "Latest release: $tag (current: v${BuildConfig.VERSION_CODE})")

            if (latestCode > BuildConfig.VERSION_CODE) {
                notifyUpdate(tag)
            }
        } catch (e: Exception) {
            DebugLogger.log(context, "UPDATE", "Check failed: ${e.message}")
        }
    }

    private fun notifyUpdate(tag: String) {
        ensureChannel()
        val intent = Intent(Intent.ACTION_VIEW,
            Uri.parse("https://github.com/edleeman17/fog-alarm/releases/latest"))
        val pi = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Fog Alarm update available")
            .setContentText("$tag is available — tap to download")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        context.getSystemService<NotificationManager>()!!.notify(NOTIF_ID, notification)
    }

    private fun ensureChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "App Updates", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService<NotificationManager>()!!.createNotificationChannel(ch)
    }

    companion object {
        const val CHANNEL_ID = "fog_alarm_updates"
        const val NOTIF_ID = 2
    }
}
