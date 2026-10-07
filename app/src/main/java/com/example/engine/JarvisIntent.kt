package com.example.engine

enum class AppTarget {
    YOUTUBE,
    INSTAGRAM,
    WHATSAPP,
    CAMERA,
    MAPS,
    CHROME,
    GMAIL,
    SETTINGS,
    GALLERY,
    PHONE,
    MESSAGES,
    UNKNOWN
}

enum class SettingsType {
    GENERAL,
    WIFI,
    BLUETOOTH,
    DISPLAY,
    SOUND,
    BATTERY,
    APPS,
    DATE_TIME,
    NOTIFICATION_LISTENER,
    DEFAULT_ASSISTANT
}

sealed class JarvisIntent {
    data class OpenApp(val target: AppTarget, val appName: String) : JarvisIntent()
    data class MakeCall(
        val contactName: String,
        val phoneNumber: String? = null,
        val isConfirmed: Boolean = false,
        val isTrustedBypass: Boolean = false
    ) : JarvisIntent()
    data class OpenWhatsApp(
        val contactName: String? = null,
        val message: String? = null,
        val isConfirmed: Boolean = false
    ) : JarvisIntent()
    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val message: String = "JARVIS Alarm"
    ) : JarvisIntent()
    data class SetTimer(
        val seconds: Int,
        val message: String = "JARVIS Timer"
    ) : JarvisIntent()
    data class Weather(
        val location: String,
        val timeFrame: String = "today",
        val isFollowUp: Boolean = false
    ) : JarvisIntent()
    object BatteryStatus : JarvisIntent()
    object CurrentTime : JarvisIntent()
    object DeviceInfo : JarvisIntent()
    data class OpenSetting(val type: SettingsType) : JarvisIntent()
    data class NavigateMaps(val destination: String) : JarvisIntent()
    data class WebSearch(val query: String) : JarvisIntent()
    object NotificationSummary : JarvisIntent()
    data class TriggerRoutine(val routineName: String) : JarvisIntent()
    data class GeneralChat(val query: String) : JarvisIntent()
    data class Unknown(val rawQuery: String) : JarvisIntent()
}
