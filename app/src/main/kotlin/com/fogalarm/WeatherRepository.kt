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

class WeatherRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    fun checkForFog(lat: Double, lon: Double): FogEvent? {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&hourly=weather_code,visibility" +
            "&forecast_days=1&timezone=auto&forecast_hours=3"

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) return null

        val body = response.body?.string() ?: return null
        val weather = gson.fromJson(body, WeatherResponse::class.java)

        for (i in 0..minOf(2, weather.hourly.weather_code.size - 1)) {
            val code = weather.hourly.weather_code[i]
            val visibility = weather.hourly.visibility.getOrElse(i) { Double.MAX_VALUE }
            if (code == 45 || code == 48 || visibility < 1000.0) {
                return FogEvent(i, weather.hourly.time.getOrElse(i) { "" })
            }
        }
        return null
    }
}
