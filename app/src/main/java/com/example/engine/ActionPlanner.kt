package com.example.engine

import android.content.Context
import com.example.tools.AlarmTool
import com.example.tools.AppLauncherTool
import com.example.tools.BatteryTool
import com.example.tools.CallTool
import com.example.tools.CameraTool
import com.example.tools.DeviceInfoTool
import com.example.tools.DeviceSettingsTool
import com.example.tools.JarvisTool
import com.example.tools.MapsNavigationTool
import com.example.tools.NotificationTool
import com.example.tools.TimerTool
import com.example.tools.ToolResult
import com.example.tools.WeatherTool
import com.example.tools.WebSearchTool
import com.example.tools.WhatsAppTool

class ActionPlanner(private val context: Context) {

    private val tools: Map<String, JarvisTool> = mapOf(
        "open_app" to AppLauncherTool(),
        "make_call" to CallTool(),
        "open_whatsapp" to WhatsAppTool(),
        "create_alarm" to AlarmTool(),
        "create_timer" to TimerTool(),
        "open_camera" to CameraTool(),
        "open_maps" to MapsNavigationTool(),
        "web_search" to WebSearchTool(),
        "weather" to WeatherTool(),
        "battery_status" to BatteryTool(),
        "device_info" to DeviceInfoTool(),
        "open_settings" to DeviceSettingsTool(),
        "notification_reader" to NotificationTool()
    )

    fun getTool(toolId: String): JarvisTool? = tools[toolId]

    suspend fun planAndExecute(intent: JarvisIntent): ToolResult {
        return when (intent) {
            is JarvisIntent.OpenApp -> {
                val tool = if (intent.target == AppTarget.CAMERA) {
                    tools["open_camera"]
                } else {
                    tools["open_app"]
                }
                tool?.execute(context, mapOf("target" to intent.target, "appName" to intent.appName))
                    ?: ToolResult(false, "App open karne mein problem aayi.")
            }

            is JarvisIntent.MakeCall -> {
                val tool = tools["make_call"] as? CallTool
                tool?.execute(
                    context,
                    mapOf(
                        "contactName" to intent.contactName,
                        "phoneNumber" to intent.phoneNumber,
                        "isConfirmed" to intent.isConfirmed,
                        "isTrustedBypass" to intent.isTrustedBypass
                    )
                ) ?: ToolResult(false, "Call tool unavailable.")
            }

            is JarvisIntent.OpenWhatsApp -> {
                val tool = tools["open_whatsapp"] as? WhatsAppTool
                tool?.execute(
                    context,
                    mapOf(
                        "contactName" to intent.contactName,
                        "message" to intent.message,
                        "isConfirmed" to intent.isConfirmed
                    )
                ) ?: ToolResult(false, "WhatsApp tool unavailable.")
            }

            is JarvisIntent.SetAlarm -> {
                val tool = tools["create_alarm"] as? AlarmTool
                tool?.execute(
                    context,
                    mapOf(
                        "hour" to intent.hour,
                        "minute" to intent.minute,
                        "message" to intent.message
                    )
                ) ?: ToolResult(false, "Alarm tool unavailable.")
            }

            is JarvisIntent.SetTimer -> {
                val tool = tools["create_timer"] as? TimerTool
                tool?.execute(
                    context,
                    mapOf(
                        "seconds" to intent.seconds,
                        "message" to intent.message
                    )
                ) ?: ToolResult(false, "Timer tool unavailable.")
            }

            is JarvisIntent.Weather -> {
                val tool = tools["weather"] as? WeatherTool
                tool?.execute(
                    context,
                    mapOf(
                        "location" to intent.location,
                        "timeFrame" to intent.timeFrame
                    )
                ) ?: ToolResult(false, "Weather tool unavailable.")
            }

            is JarvisIntent.BatteryStatus -> {
                tools["battery_status"]?.execute(context, emptyMap())
                    ?: ToolResult(false, "Battery check unavailable.")
            }

            is JarvisIntent.CurrentTime -> {
                val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val dateFormat = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.getDefault())
                val now = java.util.Date()
                val currentTime = timeFormat.format(now)
                val currentDate = dateFormat.format(now)
                ToolResult(
                    success = true,
                    spokenResponse = "Abhi time $currentTime ho raha hai, $currentDate.",
                    displayMessage = "Time: $currentTime • $currentDate"
                )
            }

            is JarvisIntent.DeviceInfo -> {
                tools["device_info"]?.execute(context, emptyMap())
                    ?: ToolResult(false, "Device info unavailable.")
            }

            is JarvisIntent.OpenSetting -> {
                tools["open_settings"]?.execute(context, mapOf("type" to intent.type))
                    ?: ToolResult(false, "Settings tool unavailable.")
            }

            is JarvisIntent.NavigateMaps -> {
                tools["open_maps"]?.execute(context, mapOf("destination" to intent.destination))
                    ?: ToolResult(false, "Maps tool unavailable.")
            }

            is JarvisIntent.WebSearch -> {
                tools["web_search"]?.execute(context, mapOf("query" to intent.query))
                    ?: ToolResult(false, "Search tool unavailable.")
            }

            is JarvisIntent.NotificationSummary -> {
                tools["notification_reader"]?.execute(context, emptyMap())
                    ?: ToolResult(false, "Notification reader unavailable.")
            }

            is JarvisIntent.TriggerRoutine -> {
                executeRoutine(intent.routineName)
            }

            is JarvisIntent.GeneralChat -> {
                ToolResult(
                    success = true,
                    spokenResponse = intent.query,
                    displayMessage = intent.query
                )
            }

            is JarvisIntent.Unknown -> {
                ToolResult(
                    success = false,
                    spokenResponse = "Mujhe samajh nahi aaya. Aap 'weather batao', 'call karo', ya 'WhatsApp kholo' bol sakte hain.",
                    displayMessage = "Command not recognized. Try asking for weather, calling a contact, or opening an app."
                )
            }
        }
    }

    private suspend fun executeRoutine(routineName: String): ToolResult {
        return when (routineName.lowercase()) {
            "work mode" -> {
                // Open WhatsApp or Maps, inform user
                tools["open_app"]?.execute(context, mapOf("target" to AppTarget.WHATSAPP, "appName" to "WhatsApp"))
                ToolResult(
                    success = true,
                    spokenResponse = "Work Mode activate ho gaya hai. Aapke work apps open kar diye gaye hain.",
                    displayMessage = "Work Mode activated: WhatsApp & Work tools initiated."
                )
            }
            "good night" -> {
                // Set default alarm for 7:00 AM
                tools["create_alarm"]?.execute(context, mapOf("hour" to 7, "minute" to 0, "message" to "JARVIS Morning"))
                ToolResult(
                    success = true,
                    spokenResponse = "Good night Maroof! Kal subah 7:00 baje ka alarm set kar diya hai. Aaraam karein.",
                    displayMessage = "Good Night routine complete: Morning alarm set for 7:00 AM."
                )
            }
            else -> {
                ToolResult(
                    success = true,
                    spokenResponse = "$routineName routine execute ho raha hai.",
                    displayMessage = "Executing routine: $routineName"
                )
            }
        }
    }
}
