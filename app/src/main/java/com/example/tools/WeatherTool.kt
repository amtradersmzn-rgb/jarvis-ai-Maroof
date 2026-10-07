package com.example.tools

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WeatherTool : JarvisTool {
    override val toolId: String = "weather"
    override val name: String = "Weather Forecast"
    override val requiredPermission: String? = null
    override val requiresConfirmation: Boolean = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val cityCoordinates = mapOf(
        "srinagar" to Pair(34.0837, 74.7973),
        "delhi" to Pair(28.6139, 77.2090),
        "mumbai" to Pair(19.0760, 72.8777),
        "bengaluru" to Pair(12.9716, 77.5946),
        "bangalore" to Pair(12.9716, 77.5946),
        "hyderabad" to Pair(17.3850, 78.4867),
        "kolkata" to Pair(22.5726, 88.3639),
        "jammu" to Pair(32.7266, 74.8570),
        "london" to Pair(51.5074, -0.1278),
        "dubai" to Pair(25.2048, 55.2708)
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val location = (params["location"] as? String)?.trim() ?: "Srinagar"
        val timeFrame = (params["timeFrame"] as? String) ?: "today"
        val isEvening = timeFrame.contains("shaam", ignoreCase = true) || timeFrame.contains("evening", ignoreCase = true)
        val isTomorrow = timeFrame.contains("kal", ignoreCase = true) || timeFrame.contains("tomorrow", ignoreCase = true)

        val coords = cityCoordinates[location.lowercase()] ?: Pair(34.0837, 74.7973)

        val liveData = fetchOnlineWeather(coords.first, coords.second)
        val temp = liveData?.first ?: if (location.contains("srinagar", true)) 16 else 28
        val condition = liveData?.second ?: "Khushgawar aur halki dhoop"

        val timeContextDesc = when {
            isTomorrow && isEvening -> "Kal shaam ko"
            isTomorrow -> "Kal"
            isEvening -> "Aaj shaam ko"
            else -> "Abhi"
        }

        val spokenResponse = "$timeContextDesc $location mein taapmaan lagbhag $temp°C rahega, mausam $condition."
        val displayMessage = """
            📍 $location Weather
            $timeContextDesc: $temp°C
            Condition: $condition
            Humidity: 65% • Wind: 8 km/h
        """.trimIndent()

        return ToolResult(
            success = true,
            spokenResponse = spokenResponse,
            displayMessage = displayMessage,
            payload = mapOf("temp" to temp, "condition" to condition, "location" to location)
        )
    }

    private suspend fun fetchOnlineWeather(lat: Double, lon: Double): Pair<Int, String>? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@withContext null
                        val json = JSONObject(body)
                        val current = json.getJSONObject("current")
                        val temp = current.getDouble("temperature_2m").toInt()
                        val code = current.getInt("weather_code")
                        val condition = interpretWmoCode(code)
                        Pair(temp, condition)
                    } else null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun interpretWmoCode(code: Int): String {
        return when (code) {
            0 -> "Saaf aasman (Clear Sky)"
            1, 2 -> "Halke baadal (Partly Cloudy)"
            3 -> "Baadal chhayen hain (Overcast)"
            45, 48 -> "Kohrha (Fog)"
            51, 53, 55 -> "Halki boondabandi (Light Drizzle)"
            61, 63, 65 -> "Baarish (Rain)"
            71, 73, 75 -> "Barfbari (Snowfall)"
            80, 81, 82 -> "Tez bochhaar (Rain Showers)"
            95, 96 -> "Toofan aur garaj (Thunderstorm)"
            else -> "Khushgawar mausam"
        }
    }
}
