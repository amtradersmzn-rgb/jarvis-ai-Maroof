package com.example.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(
    private val context: Context,
    private val onSpeakingStarted: () -> Unit,
    private val onSpeakingDone: () -> Unit
) {
    private var tts: TextToSpeech? = null
    var isInitialized: Boolean = false
        private set

    var speechRate: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    var speechPitch: Float = 0.95f // Slightly deep JARVIS futuristic pitch
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                val result = tts?.setLanguage(Locale("hi", "IN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                tts?.setPitch(speechPitch)
                tts?.setSpeechRate(speechRate)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingStarted()
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingDone()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeakingDone()
                    }
                })
            }
        }
    }

    fun speak(text: String, utteranceId: String = "JARVIS_RESPONSE") {
        if (!isInitialized || tts == null) {
            onSpeakingDone()
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        onSpeakingDone()
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
