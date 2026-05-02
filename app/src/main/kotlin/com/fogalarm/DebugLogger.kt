package com.fogalarm

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    private const val LOG_FILE = "fog_alarm_debug.log"
    private const val MAX_LINES = 500
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun log(context: Context, tag: String, message: String) {
        val entry = "${fmt.format(Date())} [$tag] $message\n"
        try {
            logFile(context).appendText(entry)
            trimIfNeeded(context)
        } catch (_: Exception) {}
    }

    fun getLog(context: Context): String = try {
        logFile(context).readText()
    } catch (_: Exception) { "" }

    fun clear(context: Context) = try {
        logFile(context).writeText("")
    } catch (_: Exception) {}

    private fun logFile(context: Context) = File(context.filesDir, LOG_FILE)

    private fun trimIfNeeded(context: Context) {
        val file = logFile(context)
        val lines = file.readLines()
        if (lines.size > MAX_LINES) {
            file.writeText(lines.takeLast(MAX_LINES).joinToString("\n") + "\n")
        }
    }
}
