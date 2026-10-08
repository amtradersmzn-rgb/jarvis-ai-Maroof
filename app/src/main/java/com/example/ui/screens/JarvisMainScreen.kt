package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CommandHistoryEntity
import com.example.engine.ActiveAiEngine
import com.example.engine.AiCoreMode
import com.example.ui.JarvisUiState
import com.example.ui.ScreenNav
import com.example.ui.components.ArcReactorCore
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.ConfirmationDialog
import com.example.ui.theme.JarvisAmberCore
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisDangerRed
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisSuccessGreen
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JarvisMainScreen(
    uiState: JarvisUiState,
    history: List<CommandHistoryEntity>,
    onToggleMic: () -> Unit,
    onStopSpeaking: () -> Unit,
    onSubmitText: (String) -> Unit,
    onNavigate: (ScreenNav) -> Unit,
    onConfirmAction: () -> Unit,
    onCancelAction: () -> Unit,
    onSelectDisambiguatedContact: (com.example.tools.ContactMatch) -> Unit = {},
    onCancelDisambiguation: () -> Unit = {},
    onSimulateVoiceCommand: (String) -> Unit = {},
    onToggleSimulatedGuest: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var currentTimeStr by remember { mutableStateOf("") }

    // Real-time HUD Clock
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTimeStr = sdf.format(Date())
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        JarvisSurfaceVariant.copy(alpha = 0.45f),
                        JarvisBackground
                    ),
                    radius = 1200f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("jarvis_main_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Futuristic Telemetry Display (Time, Battery, Network, Current AI)
            item {
                FuturisticHudDisplay(
                    currentTime = currentTimeStr,
                    isOnline = uiState.isOnline,
                    activeEngine = uiState.activeAiEngine,
                    onOpenSettings = { onNavigate(ScreenNav.SETTINGS) }
                )
            }

            // 2. Center: Large Animated AI Orb with Status Indicator
            item {
                CentralAiOrbSection(
                    mode = uiState.coreMode,
                    audioRmsDb = uiState.audioRmsDb,
                    isSpeaking = uiState.isSpeaking,
                    onToggleMic = onToggleMic,
                    onStopSpeaking = onStopSpeaking
                )
            }

            // 3. Optimized Voice Visualizer (Listening waves / Rotating thinking / Pulsing speak / Idle breath)
            item {
                AudioWaveformVisualizer(
                    isActive = uiState.isListening || uiState.isSpeaking,
                    audioRmsDb = uiState.audioRmsDb
                )
            }

            // 3b. Voice Biometrics & Hands-Free OS Action Status
            item {
                VoiceBiometricsHudCard(
                    isVerified = uiState.isVoiceBiometricVerified,
                    score = uiState.voiceBiometricScore,
                    isGuest = uiState.isSimulatedGuest,
                    isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                    onToggleGuest = onToggleSimulatedGuest,
                    onCommand = { cmd -> onSimulateVoiceCommand(cmd) },
                    onOpenAccessibility = {
                        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // 4. Quick Sample Natural Voice Commands Row (Hindi, English, Hinglish)
            item {
                NaturalVoiceCommandsPills(
                    onCommandSelected = { cmd -> onSimulateVoiceCommand(cmd) }
                )
            }

            // 5. Response & Dialogue Card
            item {
                ResponseDisplayCard(
                    query = uiState.lastUserQuery,
                    response = uiState.lastJarvisResponse,
                    toolUsed = uiState.lastToolUsed,
                    aiUsed = uiState.lastAiEngineUsed,
                    mode = uiState.coreMode,
                    liveTranscript = uiState.liveSpeechTranscript,
                    onCopy = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cb?.setPrimaryClip(ClipData.newPlainText("JARVIS", uiState.lastJarvisResponse))
                        Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, uiState.lastJarvisResponse)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share with").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    },
                    onSpeakAgain = { onSimulateVoiceCommand(uiState.lastUserQuery.ifBlank { "Hello" }) },
                    onStopSpeaking = onStopSpeaking
                )
            }

            // 6. Text Command Input Bar
            item {
                CommandInputBar(
                    text = textInput,
                    onTextChange = { textInput = it },
                    onSubmit = {
                        onSubmitText(textInput)
                        textInput = ""
                    },
                    isListening = uiState.isListening,
                    onToggleMic = onToggleMic
                )
            }

            // 7. Section 17 Primary Dashboard Buttons:
            // 🎙️ Voice, 💬 Chat, 🤖 ChatGPT, ✨ Gemini, ⚡ Compare, 📱 Phone, 🌐 Search, ⚙️ Settings
            item {
                DashboardButtonsGrid(
                    onVoiceClick = onToggleMic,
                    onChatClick = {
                        // Focus on text bar or simulate conversational prompt
                        onSubmitText("Jarvis, what can you do?")
                    },
                    onChatGptClick = {
                        onSubmitText("ChatGPT se pucho ki 25 square meter kitne square feet hote hain")
                    },
                    onGeminiClick = {
                        onSubmitText("Gemini se pucho ki OPPO Reno 14 5G ke key features kya hain")
                    },
                    onCompareClick = {
                        onNavigate(ScreenNav.COMPARE)
                    },
                    onPhoneClick = {
                        onSubmitText("Phone dialer kholo")
                    },
                    onSearchClick = {
                        onSubmitText("Search web for today's news")
                    },
                    onSettingsClick = {
                        onNavigate(ScreenNav.SETTINGS)
                    }
                )
            }

            // 8. Secondary Quick Hub Navigation
            item {
                SecondaryNavigationRow(
                    onNavigate = onNavigate
                )
            }

            // 9. Recent Commands Telemetry Section
            if (history.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT TELEMETRY & ACTIONS",
                            color = JarvisCyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "View All",
                            color = JarvisCyanBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onNavigate(ScreenNav.HISTORY) }
                        )
                    }
                }

                items(history.take(3).size) { index ->
                    val item = history[index]
                    RecentCommandTile(item = item)
                }
            }
        }

        // Contact Disambiguation Dialog
        if (uiState.disambiguationPending != null) {
            com.example.ui.components.ContactDisambiguationDialog(
                queryName = uiState.disambiguationPending.queryName,
                candidates = uiState.disambiguationPending.candidates,
                onSelectContact = onSelectDisambiguatedContact,
                onCancel = onCancelDisambiguation
            )
        }

        // Confirmation Dialog
        if (uiState.confirmationPending != null) {
            ConfirmationDialog(
                title = uiState.confirmationPending.title,
                message = uiState.confirmationPending.message,
                onConfirm = onConfirmAction,
                onCancel = onCancelAction
            )
        }
    }
}

@Composable
private fun FuturisticHudDisplay(
    currentTime: String,
    isOnline: Boolean,
    activeEngine: ActiveAiEngine,
    onOpenSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface.copy(alpha = 0.9f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisBorderGlow.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Display
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentTime.ifBlank { "12:00" },
                    color = JarvisCyanBright,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .background(JarvisSurfaceVariant, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Reno 14 5G",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Status Indicators
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Online/Offline status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = if (isOnline) JarvisSuccessGreen else JarvisDangerRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isOnline) "ONLINE" else "OFFLINE",
                        color = if (isOnline) JarvisSuccessGreen else JarvisDangerRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // AI Engine Indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(JarvisSurfaceVariant)
                        .border(1.dp, JarvisBorderGlow.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    val label = when (activeEngine) {
                        ActiveAiEngine.AUTO -> "AUTO AI"
                        ActiveAiEngine.CHATGPT -> "CHATGPT"
                        ActiveAiEngine.GEMINI -> "GEMINI"
                        ActiveAiEngine.COMPARE -> "COMPARE"
                    }
                    Text(
                        text = label,
                        color = JarvisCyanPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CentralAiOrbSection(
    mode: AiCoreMode,
    audioRmsDb: Float,
    isSpeaking: Boolean,
    onToggleMic: () -> Unit,
    onStopSpeaking: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status: Listening, Thinking, Speaking, Ready
        val (statusText, statusColor) = when (mode) {
            AiCoreMode.IDLE -> Pair("Ready", JarvisCyanPrimary)
            AiCoreMode.LISTENING -> Pair("Listening", JarvisCyanBright)
            AiCoreMode.THINKING, AiCoreMode.EXECUTING -> Pair("Thinking", JarvisAmberCore)
            AiCoreMode.SPEAKING -> Pair("Speaking", JarvisElectricBlue)
            AiCoreMode.CONFIRMING -> Pair("Confirming", JarvisAmberCore)
            AiCoreMode.ERROR -> Pair("Attention Required", JarvisDangerRed)
        }

        // Orb Title & Subtitle
        Text(
            text = "JARVIS V3",
            color = JarvisCyanPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp
        )
        Text(
            text = "Your Personal AI Assistant",
            color = TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Large Animated AI Orb
        ArcReactorCore(
            mode = mode,
            audioRmsDb = audioRmsDb,
            size = 210.dp,
            onClick = {
                if (isSpeaking) {
                    onStopSpeaking()
                } else {
                    onToggleMic()
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = "STATUS: $statusText",
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tap-to-Talk action button
        Button(
            onClick = onToggleMic,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (mode == AiCoreMode.LISTENING) JarvisDangerRed else JarvisCyanPrimary,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .height(42.dp)
                .scale(if (mode == AiCoreMode.LISTENING) pulseScale else 1f)
                .testTag("tap_to_talk_button")
        ) {
            Icon(
                imageVector = if (mode == AiCoreMode.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Microphone",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (mode == AiCoreMode.LISTENING) "Listening... Tap to Stop" else "Tap to Speak",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun VoiceBiometricsHudCard(
    isVerified: Boolean,
    score: Float,
    isGuest: Boolean,
    isAccessibilityEnabled: Boolean,
    onToggleGuest: (Boolean) -> Unit,
    onCommand: (String) -> Unit,
    onOpenAccessibility: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface.copy(alpha = 0.92f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isVerified) JarvisSuccessGreen.copy(alpha = 0.5f) else JarvisDangerRed.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .testTag("voice_biometrics_hud_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isVerified) Icons.Default.Security else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isVerified) JarvisSuccessGreen else JarvisDangerRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isVerified) "VOICE BIOMETRIC: VERIFIED" else "VOICE BIOMETRIC: GUEST REJECTED",
                        color = if (isVerified) JarvisSuccessGreen else JarvisDangerRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                // Similarity score badge
                Box(
                    modifier = Modifier
                        .background(
                            if (isVerified) JarvisSuccessGreen.copy(alpha = 0.15f) else JarvisDangerRed.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", score)}% (Req: ≥85%)",
                        color = if (isVerified) JarvisSuccessGreen else JarvisDangerRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isVerified)
                        "Hands-free OS actions (Lock, Unlock, Calling) authorized."
                    else
                        "Voice similarity <85%. High-privilege OS actions are blocked.",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Guest mode test toggle chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isGuest) JarvisDangerRed.copy(alpha = 0.2f) else JarvisSurfaceVariant)
                        .border(
                            1.dp,
                            if (isGuest) JarvisDangerRed else JarvisBorderGlow.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onToggleGuest(!isGuest) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGuest) "Guest Mode: ON" else "Test Guest Voice",
                        color = if (isGuest) JarvisDangerRed else JarvisCyanBright,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Voice-Trigger OS Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Lock Phone
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisSurfaceVariant)
                        .border(1.dp, JarvisBorderGlow.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable { onCommand("Phone lock karo") }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒 Lock Phone",
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Unlock Phone
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisSurfaceVariant)
                        .border(1.dp, JarvisBorderGlow.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable { onCommand("Phone unlock karo") }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔓 Unlock Phone",
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Call Contact
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisSurfaceVariant)
                        .border(1.dp, JarvisBorderGlow.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable { onCommand("Abdul ko call karo") }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📞 Direct Call",
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Torch
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisSurfaceVariant)
                        .border(1.dp, JarvisBorderGlow.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable { onCommand("Torch on") }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💡 Torch",
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun NaturalVoiceCommandsPills(
    onCommandSelected: (String) -> Unit
) {
    val naturalCommands = listOf(
        "Phone lock karo",
        "Phone unlock karo",
        "Abdul ko call karo",
        "Torch on",
        "Torch off",
        "Volume kam karo",
        "Volume badhao",
        "Bhai ko phone laga do",
        "WhatsApp kholo",
        "Kal subah 7 baje mujhe utha dena",
        "10 minute ka timer laga do",
        "Bluetooth settings kholo",
        "ChatGPT se pucho ki 25 square meter kitne square feet hote hain",
        "Gemini se iska answer lo",
        "Search web for today's news",
        "Delhi ka weather batao"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface.copy(alpha = 0.85f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisBorderGlow.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "SPOKEN COMMANDS (HINDI / ENGLISH / HINGLISH)",
                color = JarvisCyanPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(naturalCommands) { cmd ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(JarvisSurfaceVariant)
                            .border(1.dp, JarvisBorderGlow.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                            .clickable { onCommandSelected(cmd) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "“$cmd”",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponseDisplayCard(
    query: String,
    response: String,
    toolUsed: String,
    aiUsed: String,
    mode: AiCoreMode,
    liveTranscript: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSpeakAgain: () -> Unit,
    onStopSpeaking: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisBorderGlow.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .testTag("jarvis_response_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Live recognized speech while listening
            if (mode == AiCoreMode.LISTENING && liveTranscript.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "HEARING:",
                        color = JarvisCyanBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "“$liveTranscript”",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (query.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "YOU:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "“$query”",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Top response header with AI Engine Badge & Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "JARVIS V3:",
                        color = JarvisCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Active AI Engine used
                    Box(
                        modifier = Modifier
                            .background(JarvisSurfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = aiUsed,
                            color = JarvisCyanBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Action buttons: Speak, Copy, Share, Stop
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (mode == AiCoreMode.SPEAKING) {
                        IconButton(onClick = onStopSpeaking, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = JarvisDangerRed, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        IconButton(onClick = onSpeakAgain, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = response,
                color = JarvisCyanBright,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CommandInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isListening: Boolean,
    onToggleMic: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Speak or type (e.g. Torch on, Kal ka weather, Call Abdul)", fontSize = 12.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisCyanPrimary,
                unfocusedBorderColor = JarvisBorderGlow.copy(alpha = 0.4f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = JarvisSurface,
                unfocusedContainerColor = JarvisSurface
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("command_text_input")
        )

        IconButton(
            onClick = {
                if (text.isNotBlank()) onSubmit() else onToggleMic()
            },
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(JarvisCyanPrimary)
                .testTag("submit_command_button")
        ) {
            Icon(
                imageVector = if (text.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                contentDescription = "Send Command",
                tint = Color.Black
            )
        }
    }
}

@Composable
private fun DashboardButtonsGrid(
    onVoiceClick: () -> Unit,
    onChatClick: () -> Unit,
    onChatGptClick: () -> Unit,
    onGeminiClick: () -> Unit,
    onCompareClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "ASSISTANT CAPABILITIES",
            color = JarvisCyanPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardActionButton("🎙️ Voice", JarvisCyanPrimary, Modifier.weight(1f), onVoiceClick)
            DashboardActionButton("💬 Chat", JarvisCyanBright, Modifier.weight(1f), onChatClick)
            DashboardActionButton("🤖 ChatGPT", JarvisElectricBlue, Modifier.weight(1f), onChatGptClick)
            DashboardActionButton("✨ Gemini", JarvisCyanPrimary, Modifier.weight(1f), onGeminiClick)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardActionButton("⚡ Compare", JarvisAmberCore, Modifier.weight(1f), onCompareClick)
            DashboardActionButton("📱 Phone", JarvisSuccessGreen, Modifier.weight(1f), onPhoneClick)
            DashboardActionButton("🌐 Search", JarvisCyanBright, Modifier.weight(1f), onSearchClick)
            DashboardActionButton("⚙️ Settings", TextSecondary, Modifier.weight(1f), onSettingsClick)
        }
    }
}

@Composable
private fun DashboardActionButton(
    label: String,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(JarvisSurfaceVariant)
            .border(1.dp, tintColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SecondaryNavigationRow(
    onNavigate: (ScreenNav) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PillButton("Contacts", Modifier.weight(1f)) { onNavigate(ScreenNav.CONTACTS) }
        PillButton("Permissions", Modifier.weight(1f)) { onNavigate(ScreenNav.PERMISSION_CENTER) }
        PillButton("Memory Vault", Modifier.weight(1f)) { onNavigate(ScreenNav.MEMORY_VAULT) }
        PillButton("ColorOS Hub", Modifier.weight(1f)) { onNavigate(ScreenNav.COLOROS_HUB) }
        PillButton("Wizard", Modifier.weight(1f)) { onNavigate(ScreenNav.SETUP_WIZARD) }
    }
}

@Composable
private fun PillButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisSurfaceVariant)
            .border(1.dp, JarvisBorderGlow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = JarvisCyanPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun RecentCommandTile(item: CommandHistoryEntity) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisBorderGlow.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.userQuery,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.jarvisResponse,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (item.success) JarvisSuccessGreen.copy(alpha = 0.15f) else JarvisDangerRed.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (item.success) "SUCCESS" else "FAILED",
                    color = if (item.success) JarvisSuccessGreen else JarvisDangerRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
