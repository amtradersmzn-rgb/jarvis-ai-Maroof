package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.JarvisViewModel
import com.example.ui.ScreenNav
import com.example.ui.screens.AssistantSetupWizard
import com.example.ui.screens.ColorOSSettingsScreen
import com.example.ui.screens.CommandHistoryScreen
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.ContactManagerScreen
import com.example.ui.screens.JarvisMainScreen
import com.example.ui.screens.MemoryManagerScreen
import com.example.ui.screens.PermissionCenterScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure Lock Screen Overlay (Siri-style over lockscreen actions)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        handleLaunchIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = JarvisBackground
                ) {
                    JarvisAppContent(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        val autoListen = intent?.getBooleanExtra("EXTRA_AUTO_START_LISTENING", false) ?: false
        if (autoListen || intent?.action == Intent.ACTION_ASSIST || intent?.action == Intent.ACTION_VOICE_COMMAND) {
            viewModel.toggleListening()
        }
    }
}

@Composable
fun JarvisAppContent(viewModel: JarvisViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val commandHistory by viewModel.commandHistory.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val routines by viewModel.routines.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    // Handle Phone Unlock Event via KeyguardManager dismiss
    val activity = context as? androidx.activity.ComponentActivity
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.unlockEvent.collect { score ->
            com.example.engine.KeyguardUnlockHelper.unlockPhone(
                activity = activity,
                biometricScore = score,
                onDismissed = {
                    viewModel.speak("Keyguard dismissed. Screen unlocked.")
                },
                onError = { err ->
                    viewModel.speak(err)
                }
            )
        }
    }

    // Runtime permission launcher for SpeechRecognizer
    val micPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleListening()
        } else {
            viewModel.speak("Microphone permission required hai JARVIS voice recognition ke liye.")
        }
    }

    val safeToggleMic = {
        val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            viewModel.toggleListening()
        } else {
            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // Handle back button on secondary screens
    if (currentScreen != ScreenNav.MAIN) {
        BackHandler {
            viewModel.navigateTo(ScreenNav.MAIN)
        }
    }

    when (currentScreen) {
        ScreenNav.MAIN -> {
            JarvisMainScreen(
                uiState = uiState,
                history = commandHistory,
                onToggleMic = safeToggleMic,
                onStopSpeaking = { viewModel.cancelSpeaking() },
                onSubmitText = { text -> viewModel.processTextInput(text) },
                onNavigate = { screen -> viewModel.navigateTo(screen) },
                onConfirmAction = {
                    val pending = uiState.confirmationPending
                    pending?.onConfirm?.invoke()
                },
                onCancelAction = { viewModel.cancelConfirmation() },
                onSelectDisambiguatedContact = { chosen ->
                    viewModel.selectDisambiguatedContact(chosen)
                },
                onCancelDisambiguation = {
                    viewModel.cancelDisambiguation()
                },
                onSimulateVoiceCommand = { cmd ->
                    viewModel.simulateVoiceCommand(cmd)
                },
                onToggleSimulatedGuest = { isGuest ->
                    viewModel.setSimulatedGuestMode(isGuest)
                }
            )
        }

        ScreenNav.COMPARE -> {
            CompareScreen(
                currentQuery = uiState.compareQuery,
                chatGptResponse = uiState.compareChatGptResponse,
                geminiResponse = uiState.compareGeminiResponse,
                summaryResponse = uiState.compareSummaryResponse,
                isLoading = uiState.isComparing,
                onCompareQuery = { query -> viewModel.handleCompareAiQuery(query) },
                onSummarizeBoth = { viewModel.summarizeCompareResponses() },
                onSpeakText = { text -> viewModel.speak(text) },
                onStopSpeaking = { viewModel.cancelSpeaking() },
                onBack = { viewModel.navigateTo(ScreenNav.MAIN) }
            )
        }

        ScreenNav.PERMISSION_CENTER -> {
            PermissionCenterScreen(
                onBack = { viewModel.navigateTo(ScreenNav.MAIN) }
            )
        }

        ScreenNav.HISTORY -> {
            CommandHistoryScreen(
                history = commandHistory,
                onClearHistory = { viewModel.clearAllHistory() },
                onBack = { viewModel.navigateTo(ScreenNav.MAIN) }
            )
        }

        ScreenNav.CONTACTS -> {
            ContactManagerScreen(
                contacts = contacts,
                isTrustedCallingEnabled = uiState.isTrustedCallingEnabled,
                onToggleTrustedCalling = { enabled ->
                    viewModel.toggleTrustedCallingEnabled(enabled)
                },
                onAddContact = { name, phone, rel, trusted ->
                    viewModel.addContact(name, phone, rel, trusted)
                },
                onToggleContactTrusted = { contact ->
                    viewModel.toggleContactTrusted(contact)
                },
                onDeleteContact = { contact ->
                    viewModel.deleteContact(contact)
                },
                onInitiateTestCall = { prompt ->
                    viewModel.navigateTo(ScreenNav.MAIN)
                    viewModel.processTextInput(prompt)
                },
                onBack = { viewModel.navigateTo(ScreenNav.MAIN) }
            )
        }

        ScreenNav.SETUP_WIZARD -> {
            AssistantSetupWizard(
                initialUserName = uiState.userName,
                onComplete = { name, lang, pitch, speed, chatGptKey, geminiKey ->
                    viewModel.completeSetup(name, lang, pitch, speed, chatGptKey, geminiKey)
                },
                onClose = { viewModel.navigateTo(ScreenNav.MAIN) }
            )
        }

        ScreenNav.COLOROS_HUB -> {
            ColorOSSettingsScreen(
                onBack = { viewModel.navigateTo(ScreenNav.MAIN) }
            )
        }

        ScreenNav.MEMORY_VAULT -> {
            MemoryManagerScreen(
                memories = memories,
                onSaveMemory = { key, value, cat ->
                    viewModel.saveMemory(key, value, cat)
                },
                onDeleteMemory = { key ->
                    viewModel.deleteMemory(key)
                },
                onBack = { viewModel.navigateTo(ScreenNav.MAIN) }
            )
        }

        ScreenNav.ROUTINES -> {
            RoutinesScreen(
                routines = routines,
                onAddRoutine = { name, trigger, desc, actions ->
                    viewModel.addRoutine(name, trigger, desc, actions)
                },
                onToggleRoutine = { routine ->
                    viewModel.toggleRoutine(routine)
                },
                onDeleteRoutine = { routine ->
                    viewModel.deleteRoutine(routine)
                },
                onExecuteRoutine = { routine ->
                    viewModel.executeRoutine(routine)
                },
                onBack = { viewModel.navigateTo(ScreenNav.MAIN) }
            )
        }

        ScreenNav.SETTINGS -> {
            SettingsScreen(
                currentChatGptKey = uiState.chatGptApiKey,
                currentGeminiKey = uiState.geminiApiKey,
                currentAiEngine = uiState.activeAiEngine,
                speechRate = uiState.speechRate,
                speechPitch = uiState.speechPitch,
                currentLanguage = uiState.speechLanguage,
                wakeWordEnabled = uiState.isWakeWordActive,
                conversationModeEnabled = uiState.isConversationModeEnabled,
                confirmationModeEnabled = uiState.isConfirmationModeEnabled,
                memoryEnabled = uiState.isMemoryEnabled,
                onSaveApiKeys = { chatGpt, gemini -> viewModel.saveApiKeys(chatGpt, gemini) },
                onSaveAiEngine = { engine -> viewModel.saveAiEngine(engine) },
                onSaveVoiceParams = { rate, pitch, lang -> viewModel.updateVoiceParams(rate, pitch, lang) },
                onToggleWakeWord = { enabled -> viewModel.toggleWakeWord(enabled) },
                onToggleConversationMode = { enabled -> viewModel.toggleConversationMode(enabled) },
                onToggleConfirmationMode = { enabled -> viewModel.toggleConfirmationMode(enabled) },
                onToggleMemory = { enabled -> viewModel.toggleMemory(enabled) },
                onClearHistory = { viewModel.clearAllHistory() },
                onClearMemory = { viewModel.clearAllMemory() },
                onNavigateToPermissions = { viewModel.navigateTo(ScreenNav.PERMISSION_CENTER) },
                onNavigateToMemoryVault = { viewModel.navigateTo(ScreenNav.MEMORY_VAULT) },
                onBack = { viewModel.navigateTo(ScreenNav.MAIN) }
            )
        }
    }
}
