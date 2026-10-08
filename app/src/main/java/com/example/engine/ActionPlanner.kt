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
import com.example.tools.SmsTool
import com.example.tools.TimerTool
import com.example.tools.ToolResult
import com.example.tools.TorchTool
import com.example.tools.VolumeTool
import com.example.tools.WeatherTool
import com.example.tools.WebSearchTool
import com.example.tools.WhatsAppTool

class ActionPlanner(private val context: Context) {

    private val tools: Map<String, JarvisTool> = mapOf(
        "open_app" to AppLauncherTool(),
        "make_call" to CallTool(),
        "open_whatsapp" to WhatsAppTool(),
        "send_sms" to SmsTool(),
        "toggle_torch" to TorchTool(),
        "adjust_volume" to VolumeTool(),
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
            is JarvisIntent.LockPhone -> {
                val resp = KeyguardUnlockHelper.lockPhone(context, intent.biometricScore)
                val isSuccess = resp.contains("Phone lock kar diya", true)
                ToolResult(isSuccess, resp, resp)
            }

            is JarvisIntent.UnlockPhone -> {
                if (intent.biometricScore < VoiceBiometricManager.BIOMETRIC_SIMILARITY_THRESHOLD) {
                    val errMsg = "Voice biometric verification failed (${String.format("%.1f", intent.biometricScore)}% < 85%). Phone unlock action rejected for security."
                    ToolResult(false, errMsg, errMsg)
                } else {
                    val successMsg = "Voice verified (${String.format("%.1f", intent.biometricScore)}%). Dismissing keyguard for Face Unlock."
                    ToolResult(true, successMsg, successMsg, requiresUiInteraction = true, payload = mapOf("action" to "dismiss_keyguard"))
                }
            }

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
                        "isTrustedBypass" to intent.isTrustedBypass,
                        "isVoiceVerified" to intent.isVoiceVerified
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

            is JarvisIntent.SendSms -> {
                val tool = tools["send_sms"] as? SmsTool
                tool?.execute(
                    context,
                    mapOf(
                        "contactName" to intent.contactName,
                        "phoneNumber" to intent.phoneNumber,
                        "message" to intent.message,
                        "isConfirmed" to intent.isConfirmed
                    )
                ) ?: ToolResult(false, "SMS tool unavailable.")
            }

            is JarvisIntent.ToggleTorch -> {
                val tool = tools["toggle_torch"] as? TorchTool
                tool?.execute(context, mapOf("enable" to intent.enable))
                    ?: ToolResult(false, "Torch tool unavailable.")
            }

            is JarvisIntent.AdjustVolume -> {
                val tool = tools["adjust_volume"] as? VolumeTool
                tool?.execute(context, mapOf("direction" to intent.direction))
                    ?: ToolResult(false, "Volume tool unavailable.")
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

            is JarvisIntent.SmartHome -> {
                ToolResult(
                    success = true,
                    spokenResponse = "Smart Home module: '${intent.deviceCommand}' command received. Smart home integration connect karne ke liye ready hai.",
                    displayMessage = "Smart Home: Ready for IoT bridge integration."
                )
            }

            is JarvisIntent.GeneralChat -> {
                ToolResult(
                    success = true,
                    spokenResponse = intent.query,
                    displayMessage = intent.query
                )
            }

            is JarvisIntent.AskChatGpt, is JarvisIntent.AskGemini, is JarvisIntent.CompareAi, is JarvisIntent.SummarizeComparison -> {
                // Handled directly in ViewModel by AI Engine Manager
                ToolResult(true, "AI Engine processing...", "AI Engine processing...")
            }

            is JarvisIntent.Unknown -> {
                ToolResult(
                    success = false,
                    spokenResponse = "Mujhe samajh nahi aaya. Aap 'torch jala do', 'volume kam karo', 'weather batao', ya 'ChatGPT se pucho' bol sakte hain.",
                    displayMessage = "Command not recognized. Try torch, volume, weather, calling, or asking ChatGPT/Gemini."
                )
            }
        }
    }

    private suspend fun executeRoutine(routineName: String): ToolResult {
        return when (routineName.lowercase()) {
            "work mode" -> {
                tools["open_app"]?.execute(context, mapOf("target" to AppTarget.WHATSAPP, "appName" to "WhatsApp"))
                ToolResult(
                    success = true,
                    spokenResponse = "Work Mode activate ho gaya hai. Aapke work apps open kar diye gaye hain.",
                    displayMessage = "Work Mode activated: WhatsApp & Work tools initiated."
                )
            }
            "good night" -> {
                tools["create_alarm"]?.execute(context, mapOf("hour" to 7, "minute" to 0, "message" to "JARVIS Morning"))
                ToolResult(
                    success = true,
                    spokenResponse = "Good night! Kal subah 7:00 baje ka alarm set kar diya hai. Aaraam karein.",
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
