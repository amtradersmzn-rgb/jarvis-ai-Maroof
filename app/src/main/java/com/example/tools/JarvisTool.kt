package com.example.tools

import android.content.Context

data class ToolResult(
    val success: Boolean,
    val spokenResponse: String,
    val displayMessage: String = spokenResponse,
    val requiresUiInteraction: Boolean = false,
    val missingPermission: String? = null,
    val payload: Any? = null
)

interface JarvisTool {
    val toolId: String
    val name: String
    val requiredPermission: String?
    val requiresConfirmation: Boolean
    val minSupportedSdk: Int
        get() = 24

    suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult

    fun getFailureExplanation(reason: String): String {
        return "JARVIS action failed: $reason"
    }
}
