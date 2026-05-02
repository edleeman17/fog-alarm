package com.fogalarm

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class HourlyForecast(
    val time: List<String>,
    val weather_code: List<Int>,
    val visibility: List<Double>
)

data class WeatherResponse(
    val hourly: HourlyForecast
)

data class FogEvent(
    val hourIndex: Int,
    val isoTime: String
)

class WeatherRepository(private val baseUrl: String = "https://api.open-meteo.com") {

    companion object {
        fun create(context: android.content.Context): WeatherRepository {
            val prefs = context.getSharedPreferences("fog_alarm", android.content.Context.MODE_PRIVATE)
            val debugUrl = prefs.getString("debug_server_url", null)
            return if (debugUrl != null) WeatherRepository(debugUrl) else WeatherRepository()
        }
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    fun checkForFog(lat: Double, lon: Double): FogEvent? {
        val url = "$baseUrl/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&hourly=weather_code,visibility" +
            "&forecast_days=1&timezone=auto&forecast_hours=3"

        val response = client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")

        val body = response.body?.string() ?: throw Exception("Empty response body")
        return parseFogEvent(body)
    }

    internal fun parseFogEvent(json: String): FogEvent? {
        val weather = gson.fromJson(json, WeatherResponse::class.java)
        val codes = weather.hourly.weather_code
        val visibilities = weather.hourly.visibility
        val times = weather.hourly.time

        for (i in 0..minOf(2, codes.size - 1)) {
            val code = codes[i]
            val visibility = visibilities.getOrElse(i) { Double.MAX_VALUE }
            if (code == 45 || code == 48 || visibility < 1000.0) {
                return FogEvent(i, times.getOrElse(i) { "" })
            }
        }
        return null
    }
}
