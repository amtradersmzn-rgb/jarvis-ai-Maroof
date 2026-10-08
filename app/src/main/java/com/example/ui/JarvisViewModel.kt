package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CommandHistoryEntity
import com.example.data.JarvisDatabase
import com.example.data.MemoryEntity
import com.example.data.RoutineEntity
import com.example.data.TrustedContactEntity
import com.example.engine.ActionPlanner
import com.example.engine.ActiveAiEngine
import com.example.engine.AiCoreMode
import com.example.engine.AiResponse
import com.example.engine.GeminiBrainClient
import com.example.engine.IntentClassifier
import com.example.engine.JarvisIntent
import com.example.engine.JarvisState
import com.example.engine.OpenAiClient
import com.example.engine.VolumeDirection
import com.example.service.JarvisForegroundService
import com.example.speech.SpeechManager
import com.example.speech.TtsManager
import com.example.tools.ContactMatch
import com.example.tools.ContactResolver
import com.example.tools.ToolResult
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ScreenNav {
    MAIN,
    SETUP_WIZARD,
    COLOROS_HUB,
    MEMORY_VAULT,
    ROUTINES,
    SETTINGS,
    CONTACTS,
    PERMISSION_CENTER,
    HISTORY,
    COMPARE
}

data class ContactDisambiguationPending(
    val queryName: String,
    val candidates: List<ContactMatch>,
    val onSelect: (ContactMatch) -> Unit
)

data class PendingConfirmation(
    val title: String,
    val message: String,
    val intent: JarvisIntent,
    val onConfirm: () -> Unit
)

data class JarvisUiState(
    val userName: String = "Sir",
    val assistantState: JarvisState = JarvisState.Idle,
    val coreMode: AiCoreMode = AiCoreMode.IDLE,
    val audioRmsDb: Float = 0f,
    val lastUserQuery: String = "",
    val lastJarvisResponse: String = "JARVIS V3 initialized. Ready for your command.",
    val lastToolUsed: String = "none",
    val lastAiEngineUsed: String = "System",
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isWakeWordActive: Boolean = false,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 0.92f,
    val speechLanguage: String = "Hinglish",
    val chatGptApiKey: String = "",
    val geminiApiKey: String = "",
    val activeAiEngine: ActiveAiEngine = ActiveAiEngine.AUTO,
    val isSetupComplete: Boolean = true,
    val greeting: String = "Good Day",
    val confirmationPending: PendingConfirmation? = null,
    val disambiguationPending: ContactDisambiguationPending? = null,
    val isTrustedCallingEnabled: Boolean = false,
    val isConversationModeEnabled: Boolean = true,
    val isConfirmationModeEnabled: Boolean = true,
    val isMemoryEnabled: Boolean = true,
    val isOnline: Boolean = true,
    val isNativeSpeechAvailable: Boolean = true,
    val liveSpeechTranscript: String = "",
    // Voice Biometric Verification State
    val voiceBiometricScore: Float = 94.5f,
    val isVoiceBiometricVerified: Boolean = true,
    val isVoiceBiometricEnrolled: Boolean = true,
    val isSimulatedGuest: Boolean = false,
    val unlockScreenRequested: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    // Compare Mode State
    val compareQuery: String = "",
    val compareChatGptResponse: String = "",
    val compareGeminiResponse: String = "",
    val compareSummaryResponse: String = "",
    val isComparing: Boolean = false
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getInstance(application)
    private val memoryDao = db.memoryDao()
    private val routineDao = db.routineDao()
    private val historyDao = db.commandHistoryDao()
    private val contactDao = db.trustedContactDao()

    private val classifier = IntentClassifier()
    private val actionPlanner = ActionPlanner(application)
    private val openAiClient = OpenAiClient()
    private val geminiClient = GeminiBrainClient()
    private val voiceBiometricManager = com.example.engine.VoiceBiometricManager(application)

    private val _unlockEvent = kotlinx.coroutines.flow.MutableSharedFlow<Float>(extraBufferCapacity = 1)
    val unlockEvent: kotlinx.coroutines.flow.SharedFlow<Float> = _unlockEvent

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    private val _currentScreen = MutableStateFlow(ScreenNav.MAIN)
    val currentScreen: StateFlow<ScreenNav> = _currentScreen.asStateFlow()

    val memories: StateFlow<List<MemoryEntity>> = memoryDao.getAllMemory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routines: StateFlow<List<RoutineEntity>> = routineDao.getAllRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commandHistory: StateFlow<List<CommandHistoryEntity>> = historyDao.getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<TrustedContactEntity>> = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var speechManager: SpeechManager? = null
    private var ttsManager: TtsManager? = null

    init {
        updateGreeting()
        checkNetworkStatus()
        initAudioEngines()
        loadPersistedConfig()
        seedDefaultRoutinesIfEmpty()
        seedDefaultContactsIfEmpty()
        viewModelScope.launch {
            voiceBiometricManager.loadBiometricProfile()
            checkAccessibilityStatus()
        }
    }

    fun checkAccessibilityStatus() {
        val enabled = com.example.service.JarvisAccessibilityService.isAccessibilityEnabled(getApplication())
        _uiState.value = _uiState.value.copy(isAccessibilityEnabled = enabled)
    }

    fun setSimulatedGuestMode(isGuest: Boolean) {
        voiceBiometricManager.setSimulatedGuestMode(isGuest)
        val score = if (isGuest) 62.4f else 96.5f
        _uiState.value = _uiState.value.copy(
            isSimulatedGuest = isGuest,
            voiceBiometricScore = score,
            isVoiceBiometricVerified = !isGuest
        )
    }

    fun triggerVoiceBiometricVerification(spokenText: String): com.example.engine.VoiceVerificationResult {
        val result = voiceBiometricManager.verifyVoice(
            spokenText = spokenText,
            rmsDb = _uiState.value.audioRmsDb
        )
        _uiState.value = _uiState.value.copy(
            voiceBiometricScore = result.similarityScore,
            isVoiceBiometricVerified = result.isVerified
        )
        return result
    }

    fun checkNetworkStatus() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val isConnected = cm?.activeNetwork?.let { network ->
            cm.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: false
        _uiState.value = _uiState.value.copy(isOnline = isConnected)
    }

    private fun updateGreeting() {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 4..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
        _uiState.value = _uiState.value.copy(greeting = greeting)
    }

    private fun initAudioEngines() {
        speechManager = SpeechManager(
            context = getApplication(),
            onListeningStarted = {
                _uiState.value = _uiState.value.copy(
                    assistantState = JarvisState.Listening(),
                    coreMode = AiCoreMode.LISTENING,
                    isListening = true,
                    liveSpeechTranscript = ""
                )
            },
            onRmsChanged = { rms ->
                _uiState.value = _uiState.value.copy(audioRmsDb = rms)
            },
            onPartialResult = { partial ->
                _uiState.value = _uiState.value.copy(liveSpeechTranscript = partial)
            },
            onResult = { text ->
                _uiState.value = _uiState.value.copy(
                    isListening = false,
                    liveSpeechTranscript = "",
                    assistantState = JarvisState.Thinking(text),
                    coreMode = AiCoreMode.THINKING
                )
                processInput(text)
            },
            onError = { errMsg ->
                _uiState.value = _uiState.value.copy(
                    assistantState = JarvisState.Error(errMsg),
                    coreMode = AiCoreMode.ERROR,
                    lastJarvisResponse = errMsg,
                    isListening = false,
                    liveSpeechTranscript = ""
                )
            }
        )

        val nativeSupported = speechManager?.isNativeRecognitionSupported() ?: false
        _uiState.value = _uiState.value.copy(isNativeSpeechAvailable = nativeSupported)

        ttsManager = TtsManager(
            context = getApplication(),
            onSpeakingStarted = {
                _uiState.value = _uiState.value.copy(
                    assistantState = JarvisState.Speaking(_uiState.value.lastJarvisResponse),
                    coreMode = AiCoreMode.SPEAKING,
                    isSpeaking = true
                )
            },
            onSpeakingDone = {
                _uiState.value = _uiState.value.copy(
                    assistantState = JarvisState.Idle,
                    coreMode = AiCoreMode.IDLE,
                    isSpeaking = false
                )
                // Conversational Mode: Keep listening briefly for follow-ups
                if (_uiState.value.isConversationModeEnabled) {
                    viewModelScope.launch {
                        delay(500)
                        // Trigger brief follow-up prompt
                    }
                }
            }
        )
    }

    private fun loadPersistedConfig() {
        viewModelScope.launch {
            val name = memoryDao.getValueByKey("user_name") ?: "Sir"
            val chatGptKey = memoryDao.getValueByKey("chatgpt_api_key") ?: ""
            val geminiKey = memoryDao.getValueByKey("gemini_api_key") ?: memoryDao.getValueByKey("api_key") ?: ""
            val engineStr = memoryDao.getValueByKey("default_ai_engine")
            val rateStr = memoryDao.getValueByKey("voice_rate")
            val pitchStr = memoryDao.getValueByKey("voice_pitch")
            val langStr = memoryDao.getValueByKey("language") ?: "Hinglish"
            val setupDone = memoryDao.getValueByKey("setup_completed") == "true"
            val trustedCalling = memoryDao.getValueByKey("trusted_calling_enabled") != "false"
            val conversationMode = memoryDao.getValueByKey("conversation_mode_enabled") != "false"
            val confirmationMode = memoryDao.getValueByKey("confirmation_mode_enabled") != "false"
            val memoryEnabled = memoryDao.getValueByKey("memory_enabled") != "false"

            val rate = rateStr?.toFloatOrNull() ?: 1.0f
            val pitch = pitchStr?.toFloatOrNull() ?: 0.92f
            val engine = try {
                if (engineStr != null) ActiveAiEngine.valueOf(engineStr) else ActiveAiEngine.AUTO
            } catch (_: Exception) {
                ActiveAiEngine.AUTO
            }

            ttsManager?.speechRate = rate
            ttsManager?.speechPitch = pitch
            openAiClient.updateApiKey(chatGptKey)
            geminiClient.updateApiKey(geminiKey)

            _uiState.value = _uiState.value.copy(
                userName = name,
                chatGptApiKey = chatGptKey,
                geminiApiKey = geminiKey,
                activeAiEngine = engine,
                speechRate = rate,
                speechPitch = pitch,
                speechLanguage = langStr,
                isSetupComplete = setupDone,
                isTrustedCallingEnabled = trustedCalling,
                isConversationModeEnabled = conversationMode,
                isConfirmationModeEnabled = confirmationMode,
                isMemoryEnabled = memoryEnabled
            )
        }
    }

    private fun seedDefaultRoutinesIfEmpty() {
        viewModelScope.launch {
            val active = routineDao.getActiveRoutines()
            if (active.isEmpty()) {
                routineDao.insert(
                    RoutineEntity(
                        name = "Work Mode",
                        triggerPhrase = "work mode",
                        description = "Launch work apps and navigation",
                        actionsSummary = "Opens WhatsApp and launches Maps route"
                    )
                )
                routineDao.insert(
                    RoutineEntity(
                        name = "Good Night",
                        triggerPhrase = "good night",
                        description = "Sleep routine",
                        actionsSummary = "Sets 7:00 AM alarm and activates sleep status"
                    )
                )
            }
        }
    }

    private fun seedDefaultContactsIfEmpty() {
        viewModelScope.launch {
            val all = contactDao.getTrustedContacts()
            if (all.isEmpty()) {
                contactDao.insert(
                    TrustedContactEntity(
                        name = "Ali",
                        phoneNumber = "+91 98765 00001",
                        relationship = "Friend",
                        isTrusted = false
                    )
                )
                contactDao.insert(
                    TrustedContactEntity(
                        name = "Ammi",
                        phoneNumber = "+91 98765 43210",
                        relationship = "Mother",
                        isTrusted = true
                    )
                )
                contactDao.insert(
                    TrustedContactEntity(
                        name = "Abdul",
                        phoneNumber = "+91 98765 11111",
                        relationship = "Brother",
                        isTrusted = false
                    )
                )
                contactDao.insert(
                    TrustedContactEntity(
                        name = "Abdul Mateen",
                        phoneNumber = "+91 98765 22222",
                        relationship = "Colleague",
                        isTrusted = false
                    )
                )
                contactDao.insert(
                    TrustedContactEntity(
                        name = "Ahmed",
                        phoneNumber = "+91 98765 33333",
                        relationship = "Friend",
                        isTrusted = true
                    )
                )
            }
        }
    }

    fun navigateTo(screen: ScreenNav) {
        _currentScreen.value = screen
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            speechManager?.stopListening()
            _uiState.value = _uiState.value.copy(
                assistantState = JarvisState.Idle,
                coreMode = AiCoreMode.IDLE,
                isListening = false,
                liveSpeechTranscript = ""
            )
        } else {
            ttsManager?.stop()
            val nativeAvailable = speechManager?.isNativeRecognitionSupported() ?: false
            if (nativeAvailable) {
                speechManager?.startListening("hi-IN")
            } else {
                _uiState.value = _uiState.value.copy(
                    assistantState = JarvisState.Listening(),
                    coreMode = AiCoreMode.LISTENING,
                    isListening = true,
                    liveSpeechTranscript = "Listening... (Preview Mode)"
                )
            }
        }
    }

    fun simulateVoiceCommand(command: String) {
        viewModelScope.launch {
            ttsManager?.stop()
            _uiState.value = _uiState.value.copy(
                assistantState = JarvisState.Listening(),
                coreMode = AiCoreMode.LISTENING,
                isListening = true,
                liveSpeechTranscript = command
            )
            for (i in 1..3) {
                delay(100)
                _uiState.value = _uiState.value.copy(audioRmsDb = 5f + i * 3f)
            }
            delay(100)
            _uiState.value = _uiState.value.copy(
                isListening = false,
                audioRmsDb = 0f,
                assistantState = JarvisState.Thinking(command),
                coreMode = AiCoreMode.THINKING,
                liveSpeechTranscript = ""
            )
            delay(100)
            processInput(command)
        }
    }

    fun processTextInput(text: String) {
        if (text.isBlank()) return
        processInput(text.trim())
    }

    private fun processInput(rawText: String) {
        viewModelScope.launch {
            checkNetworkStatus()
            checkAccessibilityStatus()

            // Run voice biometric verification against enrolled owner voiceprint
            val verification = triggerVoiceBiometricVerification(rawText)

            _uiState.value = _uiState.value.copy(
                lastUserQuery = rawText,
                assistantState = JarvisState.Thinking(rawText),
                coreMode = AiCoreMode.THINKING,
                isListening = false
            )

            // Step 0: Check if waiting for Contact Disambiguation
            val pendingDisambig = _uiState.value.disambiguationPending
            if (pendingDisambig != null) {
                val matched = pendingDisambig.candidates.firstOrNull {
                    rawText.contains(it.name, ignoreCase = true) ||
                            it.name.contains(rawText, ignoreCase = true)
                }
                if (matched != null) {
                    selectDisambiguatedContact(matched)
                    return@launch
                }
            }

            // Step 1: NLP Intent Classification
            val intent = classifier.classify(rawText)

            // Security Gate: Phone Lock (Requires Biometric Similarity >= 85%)
            if (intent is JarvisIntent.LockPhone) {
                if (verification.similarityScore < com.example.engine.VoiceBiometricManager.BIOMETRIC_SIMILARITY_THRESHOLD) {
                    val rejectMsg = "Voice biometric verification failed (${String.format(Locale.US, "%.1f", verification.similarityScore)}% < 85%). Phone lock action rejected for security."
                    recordHistoryAndSpeak(rawText, rejectMsg, "VoiceBiometricSecurity", "OS Security", false)
                    return@launch
                }
                val toolResult = actionPlanner.planAndExecute(intent.copy(biometricScore = verification.similarityScore))
                recordHistoryAndSpeak(rawText, toolResult.spokenResponse, "JarvisAccessibilityService", "OS Action", toolResult.success)
                return@launch
            }

            // Security Gate: Phone Unlock (Requires Biometric Similarity >= 85%)
            if (intent is JarvisIntent.UnlockPhone) {
                if (verification.similarityScore < com.example.engine.VoiceBiometricManager.BIOMETRIC_SIMILARITY_THRESHOLD) {
                    val rejectMsg = "Voice biometric verification failed (${String.format(Locale.US, "%.1f", verification.similarityScore)}% < 85%). Phone unlock action rejected for security."
                    recordHistoryAndSpeak(rawText, rejectMsg, "VoiceBiometricSecurity", "OS Security", false)
                    return@launch
                }
                _unlockEvent.emit(verification.similarityScore)
                val unlockMsg = "Voice verified (${String.format(Locale.US, "%.1f", verification.similarityScore)}%). Dismissing keyguard for Face Unlock."
                recordHistoryAndSpeak(rawText, unlockMsg, "KeyguardUnlockHelper", "OS Action", true)
                return@launch
            }

            // Step 2: Handle Explicit AI Requests & Compare
            when (intent) {
                is JarvisIntent.AskChatGpt -> {
                    handleDirectChatGptQuery(intent.query)
                    return@launch
                }
                is JarvisIntent.AskGemini -> {
                    handleDirectGeminiQuery(intent.query)
                    return@launch
                }
                is JarvisIntent.CompareAi -> {
                    handleCompareAiQuery(intent.query)
                    return@launch
                }
                is JarvisIntent.SummarizeComparison -> {
                    summarizeCompareResponses()
                    return@launch
                }
                is JarvisIntent.MakeCall -> {
                    handleCallIntent(intent.copy(isVoiceVerified = verification.isVerified))
                    return@launch
                }
                is JarvisIntent.OpenWhatsApp -> {
                    if (!intent.isConfirmed && !intent.message.isNullOrBlank() && _uiState.value.isConfirmationModeEnabled && !verification.isVerified) {
                        handleWhatsAppConfirmation(intent)
                        return@launch
                    }
                }
                is JarvisIntent.SendSms -> {
                    if (!intent.isConfirmed && _uiState.value.isConfirmationModeEnabled && !verification.isVerified) {
                        handleSmsConfirmation(intent)
                        return@launch
                    }
                }
                else -> {}
            }

            // Step 3: Handle Auto AI or System Actions
            if (intent is JarvisIntent.GeneralChat) {
                handleGeneralChatOrAutoAi(rawText, intent)
                return@launch
            }

            // Step 4: Execute Native Android Actions
            _uiState.value = _uiState.value.copy(
                assistantState = JarvisState.Executing("Executing command"),
                coreMode = AiCoreMode.EXECUTING
            )

            val toolResult: ToolResult = actionPlanner.planAndExecute(intent)

            recordHistoryAndSpeak(
                query = rawText,
                response = toolResult.spokenResponse,
                toolUsed = intent.javaClass.simpleName,
                aiUsed = "Auto [Action]",
                success = toolResult.success
            )
        }
    }

    private suspend fun handleDirectChatGptQuery(query: String) {
        if (!_uiState.value.isOnline) {
            val offlineMsg = "ChatGPT use karne ke liye internet connection required hai."
            recordHistoryAndSpeak(query, offlineMsg, "ChatGptClient", "ChatGPT", false)
            return
        }

        if (_uiState.value.chatGptApiKey.isBlank()) {
            val keyMissingMsg = "ChatGPT connection unavailable hai. Kripya Settings se OpenAI API key enter karein."
            recordHistoryAndSpeak(query, keyMissingMsg, "ChatGptClient", "ChatGPT", false)
            return
        }

        _uiState.value = _uiState.value.copy(
            assistantState = JarvisState.Thinking("Querying ChatGPT..."),
            coreMode = AiCoreMode.THINKING
        )

        val sysPrompt = "You are JARVIS V3, a calm, intelligent, concise, futuristic personal assistant for OPPO Reno 14 5G. Provide short, clear answers in natural ${_uiState.value.speechLanguage}."
        val resp = openAiClient.queryAi(query, sysPrompt)
        when (resp) {
            is AiResponse.Success -> {
                recordHistoryAndSpeak(query, resp.text, "ChatGptClient", "ChatGPT", true)
            }
            is AiResponse.Error -> {
                recordHistoryAndSpeak(query, resp.errorMessage, "ChatGptClient", "ChatGPT", false)
            }
        }
    }

    private suspend fun handleDirectGeminiQuery(query: String) {
        if (!_uiState.value.isOnline) {
            val offlineMsg = "Gemini use karne ke liye internet connection required hai."
            recordHistoryAndSpeak(query, offlineMsg, "GeminiClient", "Gemini", false)
            return
        }

        if (_uiState.value.geminiApiKey.isBlank()) {
            val keyMissingMsg = "Gemini connection unavailable hai. Kripya Settings se Gemini API key enter karein."
            recordHistoryAndSpeak(query, keyMissingMsg, "GeminiClient", "Gemini", false)
            return
        }

        _uiState.value = _uiState.value.copy(
            assistantState = JarvisState.Thinking("Querying Gemini..."),
            coreMode = AiCoreMode.THINKING
        )

        val sysPrompt = "You are JARVIS V3, a calm, intelligent, concise, futuristic personal assistant for OPPO Reno 14 5G. Provide short, clear answers in natural ${_uiState.value.speechLanguage}."
        val resp = geminiClient.queryAi(query, sysPrompt)
        when (resp) {
            is AiResponse.Success -> {
                recordHistoryAndSpeak(query, resp.text, "GeminiClient", "Gemini", true)
            }
            is AiResponse.Error -> {
                recordHistoryAndSpeak(query, resp.errorMessage, "GeminiClient", "Gemini", false)
            }
        }
    }

    private suspend fun handleGeneralChatOrAutoAi(rawText: String, fallbackIntent: JarvisIntent.GeneralChat) {
        val currentEngine = _uiState.value.activeAiEngine

        when (currentEngine) {
            ActiveAiEngine.CHATGPT -> {
                handleDirectChatGptQuery(rawText)
                return
            }
            ActiveAiEngine.GEMINI -> {
                handleDirectGeminiQuery(rawText)
                return
            }
            ActiveAiEngine.COMPARE -> {
                handleCompareAiQuery(rawText)
                return
            }
            ActiveAiEngine.AUTO -> {
                // Intelligent routing:
                // If offline -> local fallback
                if (!_uiState.value.isOnline) {
                    val offlineMsg = "Internet connection nahi hai. Main local functions jaise torch, volume, alarm, apps execute kar sakta hoon."
                    recordHistoryAndSpeak(rawText, offlineMsg, "OfflineBrain", "Auto [Local]", false)
                    return
                }

                // If Gemini key is available -> use Gemini
                if (_uiState.value.geminiApiKey.isNotBlank()) {
                    val sysPrompt = "You are JARVIS V3, a calm, intelligent, concise, futuristic personal assistant for OPPO Reno 14 5G. Answer briefly in natural ${_uiState.value.speechLanguage}."
                    val resp = geminiClient.queryAi(rawText, sysPrompt)
                    if (resp is AiResponse.Success) {
                        recordHistoryAndSpeak(rawText, resp.text, "GeminiBrain", "Auto [Gemini]", true)
                        return
                    }
                }

                // If ChatGPT key is available -> use ChatGPT
                if (_uiState.value.chatGptApiKey.isNotBlank()) {
                    val sysPrompt = "You are JARVIS V3, a calm, intelligent, concise, futuristic personal assistant for OPPO Reno 14 5G. Answer briefly in natural ${_uiState.value.speechLanguage}."
                    val resp = openAiClient.queryAi(rawText, sysPrompt)
                    if (resp is AiResponse.Success) {
                        recordHistoryAndSpeak(rawText, resp.text, "ChatGptBrain", "Auto [ChatGPT]", true)
                        return
                    }
                }

                // Local intelligent assistant fallback
                val localAnswer = when {
                    rawText.contains("who are you") || rawText.contains("kaun ho") ->
                        "Hello. I am JARVIS V3, your personal AI assistant. I'm ready to help."
                    rawText.contains("battery") ->
                        "Aapki battery status check kar raha hoon."
                    else ->
                        "Ji, main ready hoon. Aap 'torch on', 'alarm lagao', 'ChatGPT se pucho', ya 'call karo' bol sakte hain."
                }
                recordHistoryAndSpeak(rawText, localAnswer, "LocalEngine", "Auto [System]", true)
            }
        }
    }

    fun handleCompareAiQuery(query: String) {
        viewModelScope.launch {
            checkNetworkStatus()
            _uiState.value = _uiState.value.copy(
                compareQuery = query,
                compareChatGptResponse = "Querying ChatGPT...",
                compareGeminiResponse = "Querying Gemini...",
                compareSummaryResponse = "",
                isComparing = true,
                assistantState = JarvisState.Thinking("Comparing ChatGPT & Gemini..."),
                coreMode = AiCoreMode.THINKING
            )
            _currentScreen.value = ScreenNav.COMPARE

            val sysPrompt = "Answer concisely and factually in natural ${_uiState.value.speechLanguage}."

            val chatGptDeferred = async {
                if (_uiState.value.chatGptApiKey.isNotBlank()) {
                    openAiClient.queryAi(query, sysPrompt)
                } else {
                    AiResponse.Error("ChatGPT API key not configured in Settings.")
                }
            }

            val geminiDeferred = async {
                if (_uiState.value.geminiApiKey.isNotBlank()) {
                    geminiClient.queryAi(query, sysPrompt)
                } else {
                    AiResponse.Error("Gemini API key not configured in Settings.")
                }
            }

            val chatGptRes = chatGptDeferred.await()
            val geminiRes = geminiDeferred.await()

            val chatGptText = when (chatGptRes) {
                is AiResponse.Success -> chatGptRes.text
                is AiResponse.Error -> chatGptRes.errorMessage
            }

            val geminiText = when (geminiRes) {
                is AiResponse.Success -> geminiRes.text
                is AiResponse.Error -> geminiRes.errorMessage
            }

            _uiState.value = _uiState.value.copy(
                compareChatGptResponse = chatGptText,
                compareGeminiResponse = geminiText,
                isComparing = false,
                assistantState = JarvisState.Speaking("Both ChatGPT and Gemini have answered. You can review both or ask me to summarize."),
                coreMode = AiCoreMode.SPEAKING,
                lastJarvisResponse = "ChatGPT and Gemini comparison complete."
            )

            speak("Both ChatGPT and Gemini have answered. You can compare or ask me to summarize.")
        }
    }

    fun summarizeCompareResponses() {
        viewModelScope.launch {
            val chatGptAns = _uiState.value.compareChatGptResponse
            val geminiAns = _uiState.value.compareGeminiResponse
            val query = _uiState.value.compareQuery

            if (chatGptAns.isBlank() && geminiAns.isBlank()) {
                speak("Pehle koi question compare karein.")
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                assistantState = JarvisState.Thinking("Summarizing both responses..."),
                coreMode = AiCoreMode.THINKING
            )

            val summaryPrompt = """
                Question: $query
                ChatGPT Answer: $chatGptAns
                Gemini Answer: $geminiAns
                
                Please summarize both answers into a concise, unified conclusion in 2-3 sentences in natural ${_uiState.value.speechLanguage}.
            """.trimIndent()

            var summary = ""
            if (_uiState.value.geminiApiKey.isNotBlank()) {
                val res = geminiClient.queryAi(summaryPrompt, "You are JARVIS V3 synthesizer.")
                if (res is AiResponse.Success) summary = res.text
            } else if (_uiState.value.chatGptApiKey.isNotBlank()) {
                val res = openAiClient.queryAi(summaryPrompt, "You are JARVIS V3 synthesizer.")
                if (res is AiResponse.Success) summary = res.text
            } else {
                summary = "Dono AI models ne milte julte results diye hain. ChatGPT aur Gemini dono answer provide kar chuke hain."
            }

            _uiState.value = _uiState.value.copy(
                compareSummaryResponse = summary,
                assistantState = JarvisState.Speaking(summary),
                coreMode = AiCoreMode.SPEAKING,
                lastJarvisResponse = summary
            )

            speak(summary)
        }
    }

    private suspend fun handleCallIntent(intent: JarvisIntent.MakeCall) {
        val queryName = intent.contactName
        val matches = ContactResolver.resolveContacts(getApplication(), queryName)

        when {
            matches.size > 1 -> {
                // Section 5: Multiple contacts with same name:
                // "Do Abdul contacts mile hain. Kaun sa?" Then show selectable contacts.
                val countText = when (matches.size) {
                    2 -> "Do"
                    3 -> "Teen"
                    4 -> "Chaar"
                    else -> "${matches.size}"
                }
                val spoken = "$countText $queryName contacts mile hain. Kaun sa?"
                val disambig = ContactDisambiguationPending(
                    queryName = queryName,
                    candidates = matches,
                    onSelect = { selectedMatch -> selectDisambiguatedContact(selectedMatch) }
                )
                _uiState.value = _uiState.value.copy(
                    disambiguationPending = disambig,
                    confirmationPending = null,
                    assistantState = JarvisState.ConfirmationNeeded(
                        confirmationTitle = "Clarify Contact",
                        confirmationMessage = spoken,
                        pendingIntent = intent,
                        confirmAction = {},
                        cancelAction = { cancelDisambiguation() }
                    ),
                    coreMode = AiCoreMode.CONFIRMING,
                    lastJarvisResponse = spoken,
                    lastToolUsed = "CallTool (Disambiguation)"
                )
                speak(spoken)
            }

            matches.size == 1 -> {
                proceedWithResolvedContact(matches[0])
            }

            else -> {
                val notFoundMsg = "Mujhe '$queryName' naam ka koi contact nahi mila. Phone dialer open kar raha hoon."
                actionPlanner.planAndExecute(JarvisIntent.OpenApp(com.example.engine.AppTarget.PHONE, "Phone"))
                recordHistoryAndSpeak(intent.contactName, notFoundMsg, "CallTool", "System", false)
            }
        }
    }

    private suspend fun proceedWithResolvedContact(match: ContactMatch) {
        val isTrustedCalling = _uiState.value.isTrustedCallingEnabled
        val isContactTrusted = match.isTrusted || contactDao.getTrustedContactByName(match.name) != null
        val isVoiceVerified = _uiState.value.isVoiceBiometricVerified

        if (isVoiceVerified || (isTrustedCalling && isContactTrusted)) {
            executeCallDirectly(match.name, match.number)
        } else {
            val confirmSpoken = "Calling ${match.name}?"
            val confirmMsg = "Call ${match.name} (${match.number})?"
            val callIntent = JarvisIntent.MakeCall(
                contactName = match.name,
                phoneNumber = match.number,
                isConfirmed = true
            )
            _uiState.value = _uiState.value.copy(
                disambiguationPending = null,
                confirmationPending = PendingConfirmation(
                    title = "Confirm Phone Call",
                    message = confirmMsg,
                    intent = callIntent,
                    onConfirm = { executeCallDirectly(match.name, match.number) }
                ),
                assistantState = JarvisState.ConfirmationNeeded(
                    confirmationTitle = "Confirm Call",
                    confirmationMessage = confirmSpoken,
                    pendingIntent = callIntent,
                    confirmAction = { executeCallDirectly(match.name, match.number) },
                    cancelAction = { cancelConfirmation() }
                ),
                coreMode = AiCoreMode.CONFIRMING,
                lastJarvisResponse = confirmSpoken,
                lastToolUsed = "CallTool"
            )
            speak(confirmSpoken)
        }
    }

    fun selectDisambiguatedContact(match: ContactMatch) {
        _uiState.value = _uiState.value.copy(disambiguationPending = null)
        viewModelScope.launch {
            proceedWithResolvedContact(match)
        }
    }

    fun cancelDisambiguation() {
        _uiState.value = _uiState.value.copy(
            disambiguationPending = null,
            assistantState = JarvisState.Idle,
            coreMode = AiCoreMode.IDLE
        )
        speak("Call cancel kar di gayi hai.")
    }

    private fun executeCallDirectly(contactName: String, phoneNumber: String) {
        _uiState.value = _uiState.value.copy(
            confirmationPending = null,
            disambiguationPending = null
        )
        viewModelScope.launch {
            val result = actionPlanner.planAndExecute(
                JarvisIntent.MakeCall(
                    contactName = contactName,
                    phoneNumber = phoneNumber,
                    isConfirmed = true,
                    isTrustedBypass = true
                )
            )

            recordHistoryAndSpeak(
                query = "Call $contactName",
                response = result.spokenResponse,
                toolUsed = "CallTool",
                aiUsed = "System",
                success = result.success
            )
        }
    }

    private fun handleSmsConfirmation(intent: JarvisIntent.SendSms) {
        val contact = intent.contactName ?: "Contact"
        val msg = intent.message ?: ""
        val confirmSpoken = "Send message to $contact: '$msg'?"
        val confirmedIntent = intent.copy(isConfirmed = true)

        _uiState.value = _uiState.value.copy(
            confirmationPending = PendingConfirmation(
                title = "Review Message -> Send",
                message = "Send to $contact:\n\n“$msg”",
                intent = confirmedIntent,
                onConfirm = { executeSmsDirectly(confirmedIntent) }
            ),
            assistantState = JarvisState.ConfirmationNeeded(
                confirmationTitle = "Review Message",
                confirmationMessage = confirmSpoken,
                pendingIntent = confirmedIntent,
                confirmAction = { executeSmsDirectly(confirmedIntent) },
                cancelAction = { cancelConfirmation() }
            ),
            coreMode = AiCoreMode.CONFIRMING,
            lastJarvisResponse = confirmSpoken,
            lastToolUsed = "SmsTool"
        )
        speak(confirmSpoken)
    }

    private fun executeSmsDirectly(intent: JarvisIntent.SendSms) {
        _uiState.value = _uiState.value.copy(confirmationPending = null)
        viewModelScope.launch {
            val res = actionPlanner.planAndExecute(intent)
            recordHistoryAndSpeak(
                query = "Message ${intent.contactName}: ${intent.message}",
                response = res.spokenResponse,
                toolUsed = "SmsTool",
                aiUsed = "System",
                success = res.success
            )
        }
    }

    private fun handleWhatsAppConfirmation(intent: JarvisIntent.OpenWhatsApp) {
        val target = intent.contactName ?: "Contact"
        val confirmSpoken = "$target ko message bhejun: '${intent.message}'?"
        val confirmedIntent = intent.copy(isConfirmed = true)

        _uiState.value = _uiState.value.copy(
            confirmationPending = PendingConfirmation(
                title = "Review WhatsApp Message",
                message = "Send to $target:\n'${intent.message}'",
                intent = confirmedIntent,
                onConfirm = { executeWhatsAppDirectly(confirmedIntent) }
            ),
            assistantState = JarvisState.ConfirmationNeeded(
                confirmationTitle = "Confirm WhatsApp",
                confirmationMessage = confirmSpoken,
                pendingIntent = confirmedIntent,
                confirmAction = { executeWhatsAppDirectly(confirmedIntent) },
                cancelAction = { cancelConfirmation() }
            ),
            coreMode = AiCoreMode.CONFIRMING,
            lastJarvisResponse = confirmSpoken,
            lastToolUsed = "WhatsAppTool"
        )
        speak(confirmSpoken)
    }

    private fun executeWhatsAppDirectly(intent: JarvisIntent.OpenWhatsApp) {
        _uiState.value = _uiState.value.copy(confirmationPending = null)
        viewModelScope.launch {
            val res = actionPlanner.planAndExecute(intent)
            recordHistoryAndSpeak(
                query = "WhatsApp ${intent.contactName}",
                response = res.spokenResponse,
                toolUsed = "WhatsAppTool",
                aiUsed = "System",
                success = res.success
            )
        }
    }

    fun cancelConfirmation() {
        _uiState.value = _uiState.value.copy(
            confirmationPending = null,
            assistantState = JarvisState.Idle,
            coreMode = AiCoreMode.IDLE
        )
        speak("Action cancel kar diya gaya hai.")
    }

    private fun recordHistoryAndSpeak(
        query: String,
        response: String,
        toolUsed: String,
        aiUsed: String,
        success: Boolean
    ) {
        viewModelScope.launch {
            historyDao.insert(
                CommandHistoryEntity(
                    userQuery = query,
                    jarvisResponse = response,
                    toolUsed = toolUsed,
                    aiUsed = aiUsed,
                    success = success
                )
            )

            _uiState.value = _uiState.value.copy(
                lastJarvisResponse = response,
                lastToolUsed = toolUsed,
                lastAiEngineUsed = aiUsed,
                assistantState = if (success) JarvisState.Speaking(response) else JarvisState.Error(response),
                coreMode = if (success) AiCoreMode.SPEAKING else AiCoreMode.ERROR
            )

            speak(response)
        }
    }

    fun speak(text: String) {
        ttsManager?.speak(text)
    }

    fun cancelSpeaking() {
        ttsManager?.stop()
        _uiState.value = _uiState.value.copy(
            assistantState = JarvisState.Idle,
            coreMode = AiCoreMode.IDLE,
            isSpeaking = false
        )
    }

    fun saveApiKeys(chatGptKey: String, geminiKey: String) {
        viewModelScope.launch {
            memoryDao.insertOrUpdate(MemoryEntity(key = "chatgpt_api_key", value = chatGptKey, category = "config"))
            memoryDao.insertOrUpdate(MemoryEntity(key = "gemini_api_key", value = geminiKey, category = "config"))
            openAiClient.updateApiKey(chatGptKey)
            geminiClient.updateApiKey(geminiKey)
            _uiState.value = _uiState.value.copy(chatGptApiKey = chatGptKey, geminiApiKey = geminiKey)
        }
    }

    fun saveAiEngine(engine: ActiveAiEngine) {
        viewModelScope.launch {
            memoryDao.insertOrUpdate(MemoryEntity(key = "default_ai_engine", value = engine.name, category = "config"))
            _uiState.value = _uiState.value.copy(activeAiEngine = engine)
        }
    }

    fun updateVoiceParams(rate: Float, pitch: Float, language: String) {
        ttsManager?.speechRate = rate
        ttsManager?.speechPitch = pitch
        ttsManager?.setLanguage(if (language.equals("Hindi", true)) "hi" else if (language.equals("English", true)) "en" else "auto")
        _uiState.value = _uiState.value.copy(speechRate = rate, speechPitch = pitch, speechLanguage = language)
        viewModelScope.launch {
            memoryDao.insertOrUpdate(MemoryEntity(key = "voice_rate", value = rate.toString(), category = "config"))
            memoryDao.insertOrUpdate(MemoryEntity(key = "voice_pitch", value = pitch.toString(), category = "config"))
            memoryDao.insertOrUpdate(MemoryEntity(key = "language", value = language, category = "config"))
        }
    }

    fun toggleWakeWord(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isWakeWordActive = enabled)
        if (enabled) {
            JarvisForegroundService.startService(getApplication())
        } else {
            JarvisForegroundService.stopService(getApplication())
        }
    }

    fun toggleConversationMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isConversationModeEnabled = enabled)
        viewModelScope.launch {
            memoryDao.insertOrUpdate(MemoryEntity(key = "conversation_mode_enabled", value = enabled.toString(), category = "config"))
        }
    }

    fun toggleConfirmationMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isConfirmationModeEnabled = enabled)
        viewModelScope.launch {
            memoryDao.insertOrUpdate(MemoryEntity(key = "confirmation_mode_enabled", value = enabled.toString(), category = "config"))
        }
    }

    fun toggleMemory(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isMemoryEnabled = enabled)
        viewModelScope.launch {
            memoryDao.insertOrUpdate(MemoryEntity(key = "memory_enabled", value = enabled.toString(), category = "config"))
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyDao.clearHistory()
        }
    }

    fun clearAllMemory() {
        viewModelScope.launch {
            memoryDao.clearAll()
        }
    }

    fun saveMemory(key: String, value: String, category: String) {
        viewModelScope.launch {
            memoryDao.insertOrUpdate(MemoryEntity(key = key, value = value, category = category))
            if (key == "user_name") {
                _uiState.value = _uiState.value.copy(userName = value)
            }
        }
    }

    fun deleteMemory(key: String) {
        viewModelScope.launch {
            memoryDao.deleteByKey(key)
        }
    }

    fun addContact(name: String, phone: String, relationship: String, isTrusted: Boolean) {
        viewModelScope.launch {
            contactDao.insert(
                TrustedContactEntity(
                    name = name,
                    phoneNumber = phone,
                    relationship = relationship,
                    isTrusted = isTrusted
                )
            )
        }
    }

    fun toggleContactTrusted(contact: TrustedContactEntity) {
        viewModelScope.launch {
            contactDao.update(contact.copy(isTrusted = !contact.isTrusted))
        }
    }

    fun deleteContact(contact: TrustedContactEntity) {
        viewModelScope.launch {
            contactDao.delete(contact)
        }
    }

    fun toggleTrustedCallingEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isTrustedCallingEnabled = enabled)
        viewModelScope.launch {
            memoryDao.insertOrUpdate(MemoryEntity(key = "trusted_calling_enabled", value = enabled.toString(), category = "config"))
        }
    }

    fun addRoutine(name: String, triggerPhrase: String, description: String, actionsSummary: String) {
        viewModelScope.launch {
            routineDao.insert(
                RoutineEntity(
                    name = name,
                    triggerPhrase = triggerPhrase,
                    description = description,
                    actionsSummary = actionsSummary,
                    isEnabled = true
                )
            )
        }
    }

    fun toggleRoutine(routine: RoutineEntity) {
        viewModelScope.launch {
            routineDao.update(routine.copy(isEnabled = !routine.isEnabled))
        }
    }

    fun deleteRoutine(routine: RoutineEntity) {
        viewModelScope.launch {
            routineDao.delete(routine)
        }
    }

    fun executeRoutine(routine: RoutineEntity) {
        processInput("Jarvis, ${routine.triggerPhrase}")
    }

    fun completeSetup(name: String, language: String, pitch: Float, speed: Float, chatGptKey: String, geminiKey: String) {
        viewModelScope.launch {
            memoryDao.insertOrUpdate(MemoryEntity(key = "user_name", value = name, category = "profile"))
            memoryDao.insertOrUpdate(MemoryEntity(key = "language", value = language, category = "profile"))
            memoryDao.insertOrUpdate(MemoryEntity(key = "setup_completed", value = "true", category = "config"))
            if (chatGptKey.isNotBlank()) {
                memoryDao.insertOrUpdate(MemoryEntity(key = "chatgpt_api_key", value = chatGptKey, category = "config"))
                openAiClient.updateApiKey(chatGptKey)
            }
            if (geminiKey.isNotBlank()) {
                memoryDao.insertOrUpdate(MemoryEntity(key = "gemini_api_key", value = geminiKey, category = "config"))
                geminiClient.updateApiKey(geminiKey)
            }
            updateVoiceParams(speed, pitch, language)
            _uiState.value = _uiState.value.copy(
                userName = name,
                chatGptApiKey = chatGptKey,
                geminiApiKey = geminiKey,
                isSetupComplete = true
            )
            _currentScreen.value = ScreenNav.MAIN
            speak("Hello. I am JARVIS, your personal AI assistant. I'm ready to help.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        ttsManager?.destroy()
    }
}
