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

enum class VolumeDirection {
    UP,
    DOWN,
    MUTE
}

enum class ActiveAiEngine {
    AUTO,
    CHATGPT,
    GEMINI,
    COMPARE
}

sealed class JarvisIntent {
    data class LockPhone(val biometricScore: Float = 94.0f) : JarvisIntent()
    data class UnlockPhone(val biometricScore: Float = 94.0f) : JarvisIntent()
    data class OpenApp(val target: AppTarget, val appName: String) : JarvisIntent()
    data class MakeCall(
        val contactName: String,
        val phoneNumber: String? = null,
        val isConfirmed: Boolean = false,
        val isTrustedBypass: Boolean = false,
        val isVoiceVerified: Boolean = false
    ) : JarvisIntent()
    data class OpenWhatsApp(
        val contactName: String? = null,
        val message: String? = null,
        val isConfirmed: Boolean = false
    ) : JarvisIntent()
    data class SendSms(
        val contactName: String? = null,
        val phoneNumber: String? = null,
        val message: String? = null,
        val isConfirmed: Boolean = false
    ) : JarvisIntent()
    data class ToggleTorch(val enable: Boolean) : JarvisIntent()
    data class AdjustVolume(val direction: VolumeDirection) : JarvisIntent()
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
    data class WebSearch(val query: String, val isLiveSearch: Boolean = false) : JarvisIntent()
    object NotificationSummary : JarvisIntent()
    data class TriggerRoutine(val routineName: String) : JarvisIntent()
    data class AskChatGpt(val query: String) : JarvisIntent()
    data class AskGemini(val query: String) : JarvisIntent()
    data class CompareAi(val query: String) : JarvisIntent()
    object SummarizeComparison : JarvisIntent()
    data class SmartHome(val deviceCommand: String) : JarvisIntent()
    data class GeneralChat(val query: String) : JarvisIntent()
    data class Unknown(val rawQuery: String) : JarvisIntent()
}
