package com.example.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

class SpeechManager(
    private val context: Context,
    private val onListeningStarted: () -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onPartialResult: ((String) -> Unit)? = null
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    var isListening: Boolean = false
        private set

    init {
        initializeRecognizer()
    }

    fun isNativeRecognitionSupported(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Exception) {
            false
        }
    }

    private fun initializeRecognizer() {
        if (!isNativeRecognitionSupported()) return

        mainHandler.post {
            try {
                if (recognizer == null) {
                    recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                isListening = true
                                this@SpeechManager.onListeningStarted.invoke()
                            }

                            override fun onBeginningOfSpeech() {
                                isListening = true
                            }

                            override fun onRmsChanged(rmsdB: Float) {
                                this@SpeechManager.onRmsChanged.invoke(rmsdB)
                            }

                            override fun onBufferReceived(buffer: ByteArray?) {}

                            override fun onEndOfSpeech() {
                                isListening = false
                            }

                            override fun onError(error: Int) {
                                isListening = false
                                val errorMsg = when (error) {
                                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                        "Microphone permission denied. Settings mein permission allow karein."
                                    SpeechRecognizer.ERROR_NO_MATCH ->
                                        "No speech detected. Kuch samajh nahi aaya, dobara boliye."
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                        "Recognition timeout. Aapki aawaz nahi aayi, tap to speak again."
                                    SpeechRecognizer.ERROR_AUDIO ->
                                        "Audio recording error. Microphone issue."
                                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                        "Network error during speech recognition. Internet slow hai."
                                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                                        "Speech recognizer busy hai. Please try again."
                                    SpeechRecognizer.ERROR_SERVER ->
                                        "Speech recognition server error."
                                    SpeechRecognizer.ERROR_CLIENT ->
                                        "Speech recognition service unavailable on this system."
                                    else ->
                                        "Voice recognition error ($error)"
                                }
                                this@SpeechManager.onError.invoke(errorMsg)
                            }

                            override fun onResults(results: Bundle?) {
                                isListening = false
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    this@SpeechManager.onResult.invoke(matches[0])
                                } else {
                                    this@SpeechManager.onError.invoke("No speech detected. Kuch samajh nahi aaya.")
                                }
                            }

                            override fun onPartialResults(partialResults: Bundle?) {
                                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    this@SpeechManager.onPartialResult?.invoke(matches[0])
                                }
                            }

                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }
                }
            } catch (e: Exception) {
                recognizer = null
            }
        }
    }

    fun startListening(languageCode: String = "hi-IN") {
        mainHandler.post {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                this@SpeechManager.onError.invoke("Microphone permission denied. Please allow microphone access.")
                return@post
            }

            if (!isNativeRecognitionSupported()) {
                isListening = false
                this@SpeechManager.onError.invoke("Speech recognition service unavailable on this device/environment.")
                return@post
            }

            if (recognizer == null) {
                initializeRecognizer()
            }

            if (recognizer == null) {
                isListening = false
                this@SpeechManager.onError.invoke("Speech recognition service is not available on this device.")
                return@post
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US", "hi-IN"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            try {
                isListening = true
                this@SpeechManager.onListeningStarted.invoke()
                recognizer?.startListening(intent)
            } catch (e: Exception) {
                isListening = false
                this@SpeechManager.onError.invoke("Voice recognition error: ${e.message}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                recognizer?.stopListening()
            } catch (_: Exception) {}
            isListening = false
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                recognizer?.destroy()
            } catch (_: Exception) {}
            recognizer = null
            isListening = false
        }
    }
}
