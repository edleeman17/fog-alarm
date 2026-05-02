package com.fogalarm

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        val prefs = getSharedPreferences("fog_alarm", MODE_PRIVATE)
        val logText = findViewById<TextView>(R.id.log_text)
        val scrollView = findViewById<ScrollView>(R.id.log_scroll)
        val debugUrlField = findViewById<EditText>(R.id.debug_server_url)
        val debugToggle = findViewById<Switch>(R.id.debug_server_toggle)
        val triggerButton = findViewById<Button>(R.id.btn_trigger_check)

        // Restore saved debug URL
        val savedUrl = prefs.getString("debug_server_url", null)
        debugToggle.isChecked = savedUrl != null
        if (savedUrl != null) debugUrlField.setText(savedUrl)

        debugToggle.setOnCheckedChangeListener { _, enabled ->
            if (enabled) {
                val url = debugUrlField.text.toString().trim()
                if (url.isBlank()) {
                    debugToggle.isChecked = false
                    Toast.makeText(this, "Enter server URL first", Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit().putString("debug_server_url", url).apply()
                    DebugLogger.log(this, "DEBUG", "Debug server set: $url")
                    Toast.makeText(this, "Debug server active", Toast.LENGTH_SHORT).show()
                }
            } else {
                prefs.edit().remove("debug_server_url").apply()
                DebugLogger.log(this, "DEBUG", "Debug server disabled")
                Toast.makeText(this, "Using real weather API", Toast.LENGTH_SHORT).show()
            }
            refreshLog(logText, scrollView)
        }

        triggerButton.setOnClickListener {
            // Run a one-off check immediately
            androidx.work.OneTimeWorkRequestBuilder<FogCheckWorker>().build().also {
                androidx.work.WorkManager.getInstance(this).enqueue(it)
            }
            Toast.makeText(this, "Check triggered — log updates in ~5s", Toast.LENGTH_SHORT).show()
        }

        refreshLog(logText, scrollView)

        findViewById<Button>(R.id.btn_clear_log).setOnClickListener {
            DebugLogger.clear(this)
            logText.text = "(log cleared)"
        }
    }

    private fun refreshLog(textView: TextView, scrollView: ScrollView) {
        val log = DebugLogger.getLog(this)
        textView.text = if (log.isBlank()) "(no log entries)" else log
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
