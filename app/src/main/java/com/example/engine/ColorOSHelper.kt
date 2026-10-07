package com.example.engine

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object ColorOSHelper {

    fun isOppoHardware(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL.lowercase()
        return manufacturer.contains("oppo") ||
                brand.contains("oppo") ||
                manufacturer.contains("oneplus") ||
                manufacturer.contains("realme") ||
                model.contains("reno")
    }

    @SuppressLint("PrivateApi")
    private fun getSystemProperty(propName: String): String? {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java)
            val value = getMethod.invoke(null, propName) as? String
            if (!value.isNullOrBlank()) value else null
        } catch (_: Exception) {
            null
        }
    }

    fun getColorOsVersion(): String? {
        val oppoRom = getSystemProperty("ro.build.version.opporom")
        if (!oppoRom.isNullOrBlank()) return oppoRom

        val oplusRom = getSystemProperty("ro.build.version.oplusrom")
        if (!oplusRom.isNullOrBlank()) return oplusRom

        val romDiff = getSystemProperty("ro.rom.different.version")
        if (!romDiff.isNullOrBlank()) return romDiff

        return if (isOppoHardware()) "ColorOS 16 (Detected)" else null
    }

    fun isOppoOrColorOS(): Boolean {
        return isOppoHardware() ||
                getSystemProperty("ro.build.version.opporom") != null ||
                getSystemProperty("ro.build.version.oplusrom") != null
    }

    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}"
    }

    fun getAndroidVersionInfo(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    @SuppressLint("BatteryLife")
    fun getBatteryOptimizationIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            intent
        } else {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        }
    }

    fun getAutoLaunchIntent(context: Context): Intent {
        val colorOsAutoLaunchIntents = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.oplus.battery",
                    "com.oplus.battery.BatteryActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaard.PowerUsageModelActivity"
                )
            )
        )

        for (intent in colorOsAutoLaunchIntents) {
            if (intent.resolveActivity(context.packageManager) != null) {
                return intent
            }
        }

        // Safe universal fallback: App settings page
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun getDefaultAssistantIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
        return if (intent.resolveActivity(context.packageManager) != null) {
            intent
        } else {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        }
    }

    fun getNotificationListenerSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    fun getColorOSGuidanceSteps(): List<String> = listOf(
        "Auto-Launch: Go to Settings > Apps > Auto-launch > Enable JARVIS AI.",
        "Background Activity: Go to Settings > Battery > More settings > App battery management > JARVIS > Allow background activity.",
        "Default Assistant: Go to Settings > Apps > Default apps > Digital assistant app > Select JARVIS AI.",
        "Lock in Recents: Open recent apps overview > Tap 3-dots on JARVIS AI card > Select 'Lock' to prevent ColorOS aggressive memory cleanup."
    )
}
