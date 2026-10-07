package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CommandHistoryEntity
import com.example.data.JarvisDatabase
import com.example.data.MemoryEntity
import com.example.data.RoutineEntity
import com.example.data.TrustedContactEntity
import com.example.engine.ActionPlanner
import com.example.engine.AiCoreMode
import com.example.engine.GeminiBrainClient
import com.example.engine.IntentClassifier
import com.example.engine.JarvisIntent
import com.example.engine.JarvisState
import com.example.service.JarvisForegroundService
import com.example.speech.SpeechManager
import com.example.speech.TtsManager
import com.example.tools.ContactMatch
import com.example.tools.ContactResolver
import com.example.tools.ToolResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ScreenNav {
    MAIN,
    SETUP_WIZARD,
    COLOROS_HUB,
    MEMORY_VAULT,
    ROUTINES,
    SETTINGS,
    CONTACTS
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
    val userName: String = "Maroof",
    val assistantState: JarvisState = JarvisState.Idle,
    val coreMode: AiCoreMode = AiCoreMode.IDLE,
    val audioRmsDb: Float = 0f,
    val lastUserQuery: String = "",
    val lastJarvisResponse: String = "Main ready hoon. Aap kya karna chahte hain?",
    val lastToolUsed: String = "none",
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isWakeWordActive: Boolean = false,
    val speechRate: Float = 1.05f,
    val speechPitch: Float = 0.95f,
    val apiKey: String = "",
    val isSetupComplete: Boolean = true,
    val greeting: String = "Good Evening",
    val confirmationPending: PendingConfirmation? = null,
    val disambiguationPending: ContactDisambiguationPending? = null,
    val isTrustedCallingEnabled: Boolean = false,
    val isNativeSpeechAvailable: Boolean = true,
    val liveSpeechTranscript: String = ""
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getInstance(application)
    private val memoryDao = db.memoryDao()
    private val routineDao = db.routineDao()
    private val historyDao = db.commandHistoryDao()
    private val contactDao = db.trustedContactDao()

    private val classifier = IntentClassifier()
    private val actionPlanner = ActionPlanner(application)
    private val geminiClient = GeminiBrainClient()

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
        initAudioEngines()
        loadPersistedConfig()
        seedDefaultRoutinesIfEmpty()
        seedDefaultContactsIfEmpty()
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
            }
        )
    }

    private fun loadPersistedConfig() {
        viewModelScope.launch {
            val name = memoryDao.getValueByKey("user_name") ?: "Maroof"
            val key = memoryDao.getValueByKey("api_key") ?: ""
            val rateStr = memoryDao.getValueByKey("voice_rate")
            val pitchStr = memoryDao.getValueByKey("voice_pitch")
            val setupDone = memoryDao.getValueByKey("setup_completed") == "true"
            val trustedCalling = memoryDao.getValueByKey("trusted_calling_enabled") != "false"

            val rate = rateStr?.toFloatOrNull() ?: 1.05f
            val pitch = pitchStr?.toFloatOrNull() ?: 0.95f

            ttsManager?.speechRate = rate
            ttsManager?.speechPitch = pitch
            geminiClient.updateApiKey(key)

            _uiState.value = _uiState.value.copy(
                userName = name,
                apiKey = key,
                speechRate = rate,
                speechPitch = pitch,
                isSetupComplete = setupDone,
                isTrustedCallingEnabled = trustedCalling
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
                        name = "Ali Khan",
                        phoneNumber = "+91 98765 00002",
                        relationship = "Friend",
                        isTrusted = false
                    )
                )
                contactDao.insert(
                    TrustedContactEntity(
                        name = "Ali Ahmad",
                        phoneNumber = "+91 98765 00003",
                        relationship = "Colleague",
                        isTrusted = false
                    )
                )
                contactDao.insert(
                    TrustedContactEntity(
                        name = "Maroof",
                        phoneNumber = "+91 98765 11111",
                        relationship = "Self",
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
                // Preview Fallback: Activate listening state with visualizer and test options
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
            // 1. Listening state
            _uiState.value = _uiState.value.copy(
                assistantState = JarvisState.Listening(),
                coreMode = AiCoreMode.LISTENING,
                isListening = true,
                liveSpeechTranscript = command
            )
            for (i in 1..4) {
                delay(120)
                _uiState.value = _uiState.value.copy(audioRmsDb = 5f + i * 2.5f)
            }
            delay(150)
            // 2. Processing state
            _uiState.value = _uiState.value.copy(
                isListening = false,
                audioRmsDb = 0f,
                assistantState = JarvisState.Thinking(command),
                coreMode = AiCoreMode.THINKING,
                liveSpeechTranscript = ""
            )
            delay(150)
            // 3. Process recognized speech
            processInput(command)
        }
    }

    fun processTextInput(text: String) {
        if (text.isBlank()) return
        processInput(text.trim())
    }

    private fun processInput(rawText: String) {
        viewModelScope.launch {
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

            // Step 2: Handle Calling Logic with Disambiguation and Confirmation
            if (intent is JarvisIntent.MakeCall) {
                handleCallIntent(intent)
                return@launch
            }

            // Step 3: Handle WhatsApp Confirmation Check
            if (intent is JarvisIntent.OpenWhatsApp && !intent.isConfirmed) {
                handleWhatsAppIntent(intent)
                return@launch
            }

            // Step 4: Action Planning & Execution
            _uiState.value = _uiState.value.copy(
                assistantState = JarvisState.Executing("Processing command"),
                coreMode = AiCoreMode.EXECUTING
            )

            val toolResult: ToolResult = if (intent is JarvisIntent.GeneralChat && _uiState.value.apiKey.isNotBlank()) {
                val onlineAnswer = geminiClient.queryAi(
                    userPrompt = rawText,
                    systemContext = "You are JARVIS, an intelligent, polite, futuristic personal AI mobile assistant optimized for OPPO Reno14 5G. Respond in concise Hinglish."
                )
                if (onlineAnswer != null) {
                    ToolResult(true, onlineAnswer, onlineAnswer)
                } else {
                    actionPlanner.planAndExecute(intent)
                }
            } else {
                actionPlanner.planAndExecute(intent)
            }

            // Step 5: Record Audit History
            historyDao.insert(
                CommandHistoryEntity(
                    userQuery = rawText,
                    jarvisResponse = toolResult.spokenResponse,
                    toolUsed = intent.javaClass.simpleName,
                    success = toolResult.success
                )
            )

            // Step 6: Voice Output & State Update
            _uiState.value = _uiState.value.copy(
                lastJarvisResponse = toolResult.spokenResponse,
                lastToolUsed = intent.javaClass.simpleName,
                assistantState = if (toolResult.success) {
                    JarvisState.Speaking(toolResult.spokenResponse)
                } else {
                    JarvisState.Error(toolResult.spokenResponse)
                },
                coreMode = if (toolResult.success) AiCoreMode.SPEAKING else AiCoreMode.ERROR
            )

            speak(toolResult.spokenResponse)
        }
    }

    private suspend fun handleCallIntent(intent: JarvisIntent.MakeCall) {
        if (intent.isConfirmed) {
            // Already confirmed, place call immediately
            executeCallDirectly(intent.contactName, intent.phoneNumber ?: "")
            return
        }

        val queryName = intent.contactName
        val matches = ContactResolver.resolveContacts(getApplication(), queryName)
        val exactMatches = matches.filter { it.name.equals(queryName.trim(), ignoreCase = true) }

        when {
            exactMatches.size == 1 -> {
                proceedWithResolvedContact(exactMatches[0])
            }

            matches.size > 1 -> {
                // Ambiguous Contact Case: NEVER call silently!
                val candidateNames = matches.joinToString(" ya ") { it.name }
                val spoken = "Kaunsa contact? $candidateNames?"
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
                val match = matches[0]
                proceedWithResolvedContact(match)
            }

            else -> {
                // Contact not found
                val notFoundMsg = "Mujhe '$queryName' naam ka koi contact nahi mila. Kya phone dialer open karun?"
                _uiState.value = _uiState.value.copy(
                    disambiguationPending = null,
                    confirmationPending = null,
                    assistantState = JarvisState.Error(notFoundMsg),
                    coreMode = AiCoreMode.ERROR,
                    lastJarvisResponse = notFoundMsg,
                    lastToolUsed = "CallTool"
                )
                speak(notFoundMsg)
            }
        }
    }

    private suspend fun proceedWithResolvedContact(match: ContactMatch) {
        val isTrustedCalling = _uiState.value.isTrustedCallingEnabled
        val isContactTrusted = match.isTrusted || contactDao.getTrustedContactByName(match.name) != null

        if (isTrustedCalling && isContactTrusted) {
            // Trusted-Contact Calling enabled: Initiate call directly without extra confirmation!
            executeCallDirectly(match.name, match.number)
        } else {
            // Confirmation Required
            val confirmSpoken = "${match.name} ko call lagau?"
            val confirmMsg = "Do you want JARVIS to call ${match.name} (${match.number})?"
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

            historyDao.insert(
                CommandHistoryEntity(
                    userQuery = "Call $contactName",
                    jarvisResponse = result.spokenResponse,
                    toolUsed = "CallTool",
                    success = result.success
                )
            )

            _uiState.value = _uiState.value.copy(
                lastJarvisResponse = result.spokenResponse,
                assistantState = JarvisState.Speaking(result.spokenResponse),
                coreMode = AiCoreMode.SPEAKING
            )
            speak(result.spokenResponse)
        }
    }

    private suspend fun handleWhatsAppIntent(intent: JarvisIntent.OpenWhatsApp) {
        val target = intent.contactName
        if (target.isNullOrBlank()) {
            executeWhatsAppDirectly(intent.copy(isConfirmed = true))
            return
        }

        // Find contact
        val matches = ContactResolver.resolveContacts(getApplication(), target)
        val resolvedMatch = matches.firstOrNull { it.name.equals(target, ignoreCase = true) }
            ?: matches.firstOrNull()

        val contactName = resolvedMatch?.name ?: target
        val displayPhone = resolvedMatch?.number?.let { " ($it)" } ?: ""
        val confirmTitle = "Confirm WhatsApp"
        val confirmMsg = if (!intent.message.isNullOrBlank()) {
            "Do you want JARVIS to send message to $contactName$displayPhone:\n'${intent.message}'?"
        } else {
            "Do you want JARVIS to open WhatsApp for $contactName$displayPhone?"
        }
        val confirmSpoken = if (!intent.message.isNullOrBlank()) {
            "$contactName ko ye message bhejun: '${intent.message}'?"
        } else {
            "$contactName ke sath WhatsApp open karun?"
        }

        val confirmedIntent = intent.copy(contactName = contactName, isConfirmed = true)
        _uiState.value = _uiState.value.copy(
            confirmationPending = PendingConfirmation(
                title = confirmTitle,
                message = confirmMsg,
                intent = confirmedIntent,
                onConfirm = { executeWhatsAppDirectly(confirmedIntent) }
            ),
            assistantState = JarvisState.ConfirmationNeeded(
                confirmationTitle = confirmTitle,
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
            _uiState.value = _uiState.value.copy(
                assistantState = JarvisState.Executing("Opening WhatsApp"),
                coreMode = AiCoreMode.EXECUTING
            )
            val result = actionPlanner.planAndExecute(intent)
            historyDao.insert(
                CommandHistoryEntity(
                    userQuery = if (!intent.contactName.isNullOrBlank()) "WhatsApp ${intent.contactName}" else "Open WhatsApp",
                    jarvisResponse = result.spokenResponse,
                    toolUsed = "WhatsAppTool",
                    success = result.success
                )
            )
            _uiState.value = _uiState.value.copy(
                lastJarvisResponse = result.spokenResponse,
                lastToolUsed = "WhatsAppTool",
                assistantState = if (result.success) JarvisState.Speaking(result.spokenResponse) else JarvisState.Error(result.spokenResponse),
                coreMode = if (result.success) AiCoreMode.SPEAKING else AiCoreMode.ERROR
            )
            speak(result.spokenResponse)
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
            memoryDao.insertOrUpdate(
                MemoryEntity(
                    key = "trusted_calling_enabled",
                    value = enabled.toString(),
                    category = "config"
                )
            )
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

    fun updateApiKey(newKey: String) {
        viewModelScope.launch {
            memoryDao.insertOrUpdate(MemoryEntity(key = "api_key", value = newKey, category = "config"))
            geminiClient.updateApiKey(newKey)
            _uiState.value = _uiState.value.copy(apiKey = newKey)
        }
    }

    fun updateVoiceParams(rate: Float, pitch: Float) {
        ttsManager?.speechRate = rate
        ttsManager?.speechPitch = pitch
        _uiState.value = _uiState.value.copy(speechRate = rate, speechPitch = pitch)
        viewModelScope.launch {
            memoryDao.insertOrUpdate(MemoryEntity(key = "voice_rate", value = rate.toString(), category = "config"))
            memoryDao.insertOrUpdate(MemoryEntity(key = "voice_pitch", value = pitch.toString(), category = "config"))
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

    fun completeSetup(name: String, language: String, pitch: Float, speed: Float) {
        viewModelScope.launch {
            memoryDao.insertOrUpdate(MemoryEntity(key = "user_name", value = name, category = "profile"))
            memoryDao.insertOrUpdate(MemoryEntity(key = "language", value = language, category = "profile"))
            memoryDao.insertOrUpdate(MemoryEntity(key = "setup_completed", value = "true", category = "config"))
            updateVoiceParams(speed, pitch)
            _uiState.value = _uiState.value.copy(userName = name, isSetupComplete = true)
            _currentScreen.value = ScreenNav.MAIN
            speak("Ji $name! Main JARVIS hoon. Setup complete ho gaya hai. Main aapki kya madad kar sakta hoon?")
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        ttsManager?.destroy()
    }
}
