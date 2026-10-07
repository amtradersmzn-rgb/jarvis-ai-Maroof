package com.example.tools

import android.content.Context
import com.example.service.JarvisNotificationListenerService

class NotificationTool : JarvisTool {
    override val toolId: String = "notification_reader"
    override val name: String = "Notification Reader"
    override val requiredPermission: String? = null
    override val requiresConfirmation: Boolean = false

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        if (!JarvisNotificationListenerService.isConnected) {
            return ToolResult(
                success = false,
                spokenResponse = "Notification access permission enabled nahi hai. Kripya Settings se Notification Listener ko allow karein.",
                displayMessage = "Notification access permission required. Tap Settings to enable.",
                requiresUiInteraction = true,
                payload = "open_notification_settings"
            )
        }

        val summary = JarvisNotificationListenerService.getSummary()
        if (summary.totalCount == 0) {
            return ToolResult(
                success = true,
                spokenResponse = "Abhi koi nayi notification nahi hai.",
                displayMessage = "No active notifications."
            )
        }

        val details = summary.appCounts.entries.joinToString(" aur ") { "${it.value} ${it.key}" }
        val spoken = "${summary.totalCount} notifications hain: $details."
        val display = "Active Notifications (${summary.totalCount}):\n" +
                summary.appCounts.entries.joinToString("\n") { "• ${it.key}: ${it.value}" }

        return ToolResult(
            success = true,
            spokenResponse = spoken,
            displayMessage = display
        )
    }
}
