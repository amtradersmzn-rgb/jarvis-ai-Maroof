package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

data class SafeNotificationSummary(
    val totalCount: Int,
    val appCounts: Map<String, Int>
)

class JarvisNotificationListenerService : NotificationListenerService() {

    companion object {
        var isConnected: Boolean = false
            private set

        private val savedNotificationMap = mutableMapOf<String, String>() // key -> appName

        fun getSummary(): SafeNotificationSummary {
            val counts = mutableMapOf<String, Int>()
            synchronized(savedNotificationMap) {
                for (appName in savedNotificationMap.values) {
                    counts[appName] = (counts[appName] ?: 0) + 1
                }
            }
            val total = counts.values.sum()
            return SafeNotificationSummary(total, counts)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val appName = getFriendlyAppName(sbn.packageName)
        synchronized(savedNotificationMap) {
            savedNotificationMap[sbn.key] = appName
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        synchronized(savedNotificationMap) {
            savedNotificationMap.remove(sbn.key)
        }
    }

    private fun refreshActiveNotifications() {
        try {
            val sbnsList: Array<StatusBarNotification>? = try { activeNotifications } catch (_: Exception) { null }
            if (sbnsList != null) {
                synchronized(savedNotificationMap) {
                    savedNotificationMap.clear()
                    for (sbn in sbnsList) {
                        val name = getFriendlyAppName(sbn.packageName)
                        savedNotificationMap[sbn.key] = name
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun getFriendlyAppName(pkg: String): String {
        return when (pkg) {
            "com.whatsapp", "com.whatsapp.w4b" -> "WhatsApp"
            "com.google.android.gm" -> "Gmail"
            "com.instagram.android" -> "Instagram"
            "com.google.android.apps.messaging", "com.android.mms" -> "Messages"
            "com.google.android.youtube" -> "YouTube"
            else -> "App"
        }
    }
}
