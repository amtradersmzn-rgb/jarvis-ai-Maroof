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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.engine.AppTarget
import com.example.ui.JarvisViewModel
import com.example.ui.ScreenNav
import com.example.ui.screens.AssistantSetupWizard
import com.example.ui.screens.ColorOSSettingsScreen
import com.example.ui.screens.JarvisMainScreen
import com.example.ui.screens.MemoryManagerScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                onQuickAction = { action ->
                    when (action.id) {
                        "talk" -> safeToggleMic()
                        "call" -> viewModel.processTextInput("Ammi ko call karo")
                        "whatsapp" -> viewModel.processTextInput("WhatsApp kholo")
                        "weather" -> viewModel.processTextInput("Srinagar ka weather batao")
                        "maps" -> viewModel.processTextInput("Google Maps mein ghar ka route dikhao")
                        "camera" -> viewModel.processTextInput("Camera kholo")
                        "youtube" -> viewModel.processTextInput("YouTube kholo")
                        "alarm" -> viewModel.processTextInput("Kal subah 7 baje alarm laga do")
                        "timer" -> viewModel.processTextInput("10 minute ka timer lagao")
                        "settings" -> viewModel.processTextInput("Settings kholo")
                    }
                },
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
                }
            )
        }

        ScreenNav.CONTACTS -> {
            com.example.ui.screens.ContactManagerScreen(
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
                onComplete = { name, lang, pitch, speed ->
                    viewModel.completeSetup(name, lang, pitch, speed)
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
                currentApiKey = uiState.apiKey,
                speechRate = uiState.speechRate,
                speechPitch = uiState.speechPitch,
                wakeWordEnabled = uiState.isWakeWordActive,
                onSaveApiKey = { key -> viewModel.updateApiKey(key) },
                onSaveVoiceParams = { rate, pitch -> viewModel.updateVoiceParams(rate, pitch) },
                onToggleWakeWord = { enabled -> viewModel.toggleWakeWord(enabled) },
                onNavigateToContacts = { viewModel.navigateTo(ScreenNav.CONTACTS) },
                onBack = { viewModel.navigateTo(ScreenNav.MAIN) }
            )
        }
    }
}
