package com.example.engine

sealed class JarvisState {
    object Idle : JarvisState()
    data class Listening(val rmsDb: Float = 0f) : JarvisState()
    data class Thinking(val query: String) : JarvisState()
    data class Speaking(val responseText: String) : JarvisState()
    data class Executing(val actionName: String) : JarvisState()
    data class ConfirmationNeeded(
        val confirmationTitle: String,
        val confirmationMessage: String,
        val pendingIntent: JarvisIntent,
        val confirmAction: () -> Unit,
        val cancelAction: () -> Unit
    ) : JarvisState()
    data class Error(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : JarvisState()
}

enum class AiCoreMode {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING,
    CONFIRMING,
    ERROR
}
