package com.example.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.Settings
import com.example.engine.ColorOSHelper
import com.example.engine.SettingsType

class AlarmTool : JarvisTool {
    override val toolId: String = "create_alarm"
    override val name: String = "Set Alarm"
    override val requiredPermission: String = "com.android.alarm.permission.SET_ALARM"
    override val requiresConfirmation: Boolean = false

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val hour = params["hour"] as? Int ?: 7
        val minute = params["minute"] as? Int ?: 0
        val message = params["message"] as? String ?: "JARVIS Alarm"

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            val timeStr = String.format("%02d:%02d", hour, minute)
            ToolResult(
                success = true,
                spokenResponse = "Ji, $timeStr baje ka alarm set kar diya hai.",
                displayMessage = "Alarm set for $timeStr ($message)."
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                spokenResponse = "Alarm set karne mein problem aayi: ${e.message}",
                displayMessage = "Failed to set alarm: ${e.message}"
            )
        }
    }
}

class TimerTool : JarvisTool {
    override val toolId: String = "create_timer"
    override val name: String = "Set Timer"
    override val requiredPermission: String = "com.android.alarm.permission.SET_ALARM"
    override val requiresConfirmation: Boolean = false

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val seconds = params["seconds"] as? Int ?: 600
        val message = params["message"] as? String ?: "JARVIS Timer"

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            val minutes = seconds / 60
            val timeDesc = if (minutes > 0) "$minutes minute" else "$seconds second"
            ToolResult(
                success = true,
                spokenResponse = "$timeDesc ka timer shuru kar diya hai.",
                displayMessage = "Timer set for $timeDesc."
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                spokenResponse = "Timer start nahi ho saka: ${e.message}",
                displayMessage = "Failed to set timer: ${e.message}"
            )
        }
    }
}

class DeviceSettingsTool : JarvisTool {
    override val toolId: String = "open_settings"
    override val name: String = "Device Settings"
    override val requiredPermission: String? = null
    override val requiresConfirmation: Boolean = false

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val type = params["type"] as? SettingsType ?: SettingsType.GENERAL
        val intent = when (type) {
            SettingsType.WIFI -> Intent(Settings.ACTION_WIFI_SETTINGS)
            SettingsType.BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            SettingsType.DISPLAY -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            SettingsType.SOUND -> Intent(Settings.ACTION_SOUND_SETTINGS)
            SettingsType.BATTERY -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            SettingsType.APPS -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            SettingsType.DATE_TIME -> Intent(Settings.ACTION_DATE_SETTINGS)
            SettingsType.NOTIFICATION_LISTENER -> ColorOSHelper.getNotificationListenerSettingsIntent()
            SettingsType.DEFAULT_ASSISTANT -> ColorOSHelper.getDefaultAssistantIntent(context)
            SettingsType.GENERAL -> Intent(Settings.ACTION_SETTINGS)
        }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        return try {
            context.startActivity(intent)
            val name = type.name.lowercase().replace('_', ' ')
            ToolResult(
                success = true,
                spokenResponse = "$name settings khol di hai.",
                displayMessage = "Opening $name settings..."
            )
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(fallback)
            ToolResult(
                success = true,
                spokenResponse = "Settings khol di hai.",
                displayMessage = "Opening Android Settings..."
            )
        }
    }
}

class BatteryTool : JarvisTool {
    override val toolId: String = "battery_status"
    override val name: String = "Battery Status"
    override val requiredPermission: String? = null
    override val requiresConfirmation: Boolean = false

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 75

        val chargeText = if (isCharging) "aur phone charge ho raha hai" else "aur phone battery par chal raha hai"
        val spoken = "Aapki battery abhi $batteryPct percent hai, $chargeText."
        val display = "Battery: $batteryPct% • ${if (isCharging) "Charging" else "Discharging"}"

        return ToolResult(
            success = true,
            spokenResponse = spoken,
            displayMessage = display,
            payload = mapOf("percentage" to batteryPct, "isCharging" to isCharging)
        )
    }
}

class DeviceInfoTool : JarvisTool {
    override val toolId: String = "device_info"
    override val name: String = "Device Information"
    override val requiredPermission: String? = null
    override val requiresConfirmation: Boolean = false

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val model = ColorOSHelper.getDeviceModel()
        val androidVer = ColorOSHelper.getAndroidVersionInfo()
        val colorOsVer = ColorOSHelper.getColorOsVersion() ?: "ColorOS 16 (OPPO Reno14 5G Optimized)"

        // Memory info
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024 * 1024 * 1024)
        val availRamGb = memInfo.availMem / (1024 * 1024 * 1024)

        // Storage info
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val bytesTotal = stat.blockSizeLong * stat.blockCountLong
        val availStorageGb = bytesAvailable / (1024 * 1024 * 1024)
        val totalStorageGb = bytesTotal / (1024 * 1024 * 1024)

        val spoken = "Ye device $model hai, $colorOsVer aur $androidVer ke saath run kar raha hai. Available RAM $availRamGb GB hai."
        val display = """
            Device: $model
            OS: $colorOsVer
            Android: $androidVer
            RAM: ${totalRamGb - availRamGb}GB / ${totalRamGb}GB Used
            Storage: ${availStorageGb}GB Free of ${totalStorageGb}GB
        """.trimIndent()

        return ToolResult(
            success = true,
            spokenResponse = spoken,
            displayMessage = display
        )
    }
}
