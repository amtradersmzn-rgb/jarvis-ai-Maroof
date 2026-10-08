package com.example.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

class TtsManager(
    private val context: Context,
    private val onSpeakingStarted: () -> Unit,
    private val onSpeakingDone: () -> Unit
) {
    private var tts: TextToSpeech? = null
    var isInitialized: Boolean = false
        private set

    var currentLanguageCode: String = "auto" // "hi", "en", "auto"

    var speechRate: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    var speechPitch: Float = 0.92f // Calm, slightly deeper futuristic male tone
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                configureVoice()
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

    private fun configureVoice(locale: Locale = Locale("hi", "IN")) {
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.US)
        }
        // Try to pick a male voice if available
        try {
            val voices = tts?.voices
            if (voices != null) {
                val maleVoice = voices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    (name.contains("male") || name.contains("man") || name.contains("#male") || name.contains("en-us-x-sfg#male")) &&
                            !name.contains("female")
                }
                if (maleVoice != null) {
                    tts?.voice = maleVoice
                }
            }
        } catch (_: Exception) {}
    }

    fun setLanguage(lang: String) {
        currentLanguageCode = lang
        when (lang) {
            "hi" -> configureVoice(Locale("hi", "IN"))
            "en" -> configureVoice(Locale.US)
            else -> configureVoice(Locale("hi", "IN"))
        }
    }

    fun speak(text: String, utteranceId: String = "JARVIS_RESPONSE") {
        if (!isInitialized || tts == null) {
            onSpeakingDone()
            return
        }

        // Auto language selection if mode is "auto"
        if (currentLanguageCode == "auto") {
            // If text contains predominantly English words vs Devanagari or Hinglish
            val containsHindiScript = text.any { it in '\u0900'..'\u097F' }
            val isHinglish = text.contains("hai") || text.contains("kar") || text.contains("karo") ||
                    text.contains("hoon") || text.contains("ji") || text.contains("kripya")
            if (containsHindiScript || isHinglish) {
                tts?.setLanguage(Locale("hi", "IN"))
            } else {
                tts?.setLanguage(Locale.US)
            }
        }

        tts?.setPitch(speechPitch)
        tts?.setSpeechRate(speechRate)
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
