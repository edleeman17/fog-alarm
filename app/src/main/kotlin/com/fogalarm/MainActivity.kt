package com.fogalarm

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var toggleSwitch: Switch
    private lateinit var leadTimeSpinner: Spinner
    private lateinit var intervalSpinner: Spinner
    private lateinit var statusText: TextView
    private lateinit var testAlarmButton: Button

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("fog_alarm", MODE_PRIVATE)
    }

    // Step 1: fine location only
    private val fineLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) requestBackgroundLocation()
        else {
            toggleSwitch.isChecked = false
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // Step 2: background location — must be separate request on Android 11+
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { requestNotificationPerm() } // proceed regardless — background loc is best-effort

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { requestBatteryExemption() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toggleSwitch = findViewById(R.id.toggle_enabled)
        leadTimeSpinner = findViewById(R.id.spinner_lead_time)
        intervalSpinner = findViewById(R.id.spinner_interval)
        statusText = findViewById(R.id.text_status)
        testAlarmButton = findViewById(R.id.btn_test_alarm)

        setupSpinners()
        loadPrefs()
        updateStatus()

        toggleSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("enabled", checked).apply()
            if (checked) requestPermissionsAndEnable() else disableMonitoring()
        }

        leadTimeSpinner.onItemSelectedListener = spinnerListener { pos ->
            prefs.edit().putInt("lead_time_minutes", LEAD_TIME_OPTIONS[pos]).apply()
        }

        intervalSpinner.onItemSelectedListener = spinnerListener { pos ->
            val mins = INTERVAL_OPTIONS[pos]
            prefs.edit().putInt("interval_minutes", mins).apply()
            if (prefs.getBoolean("enabled", false)) FogCheckWorker.schedule(this, mins.toLong())
        }

        testAlarmButton.setOnClickListener {
            startActivity(Intent(this, AlarmActivity::class.java).apply {
                putExtra("fog_start_ms", System.currentTimeMillis() + 3_600_000L)
            })
        }

        findViewById<Button>(R.id.btn_view_log).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun setupSpinners() {
        leadTimeSpinner.adapter = arrayAdapter(LEAD_TIME_OPTIONS.map { "$it min" })
        intervalSpinner.adapter = arrayAdapter(INTERVAL_OPTIONS.map { "$it min" })
    }

    private fun loadPrefs() {
        toggleSwitch.isChecked = prefs.getBoolean("enabled", false)
        leadTimeSpinner.setSelection(LEAD_TIME_OPTIONS.indexOf(prefs.getInt("lead_time_minutes", 60)).coerceAtLeast(0))
        intervalSpinner.setSelection(INTERVAL_OPTIONS.indexOf(prefs.getInt("interval_minutes", 60)).coerceAtLeast(0))
    }

    private fun updateStatus() {
        val alarmMs = AlarmScheduler(this).getScheduledAlarmMs()
        val lastCheck = prefs.getString("last_check", null)
        val status = prefs.getString("status", if (prefs.getBoolean("enabled", false)) "Waiting for first check…" else "Disabled")

        val sb = StringBuilder()
        if (lastCheck != null) sb.append("Last check: $lastCheck\n")
        sb.append(status ?: "")
        if (alarmMs > System.currentTimeMillis()) {
            val alarmTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(alarmMs))
            sb.append("\nAlarm set: $alarmTime")
        }
        statusText.text = sb.toString()
    }

    private fun requestPermissionsAndEnable() {
        if (!hasPerm(Manifest.permission.ACCESS_FINE_LOCATION))
            fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        else requestBackgroundLocation()
    }

    private fun requestBackgroundLocation() {
        if (!hasPerm(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else requestNotificationPerm()
    }

    private fun requestNotificationPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPerm(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestBatteryExemption()
        }
    }

    private fun requestBatteryExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService<AlarmManager>()!!
            if (!am.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
        val pm = getSystemService<PowerManager>()!!
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
        enableMonitoring()
    }

    private fun enableMonitoring() {
        FogCheckWorker.schedule(this, prefs.getInt("interval_minutes", 60).toLong())
        updateStatus()
    }

    private fun disableMonitoring() {
        FogCheckWorker.cancel(this)
        AlarmScheduler(this).cancelAlarm()
        prefs.edit().remove("status").remove("last_check").apply()
        updateStatus()
    }

    private fun hasPerm(perm: String) =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    private fun arrayAdapter(items: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    private fun spinnerListener(onSelected: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, pos: Int, id: Long) = onSelected(pos)
        override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    companion object {
        private val LEAD_TIME_OPTIONS = listOf(30, 60, 90, 120)
        private val INTERVAL_OPTIONS = listOf(30, 60, 120)
    }
}
