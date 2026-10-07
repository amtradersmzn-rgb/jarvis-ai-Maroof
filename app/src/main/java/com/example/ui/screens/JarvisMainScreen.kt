package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CommandHistoryEntity
import com.example.engine.AiCoreMode
import com.example.ui.JarvisUiState
import com.example.ui.ScreenNav
import com.example.ui.components.ArcReactorCore
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DefaultQuickActions
import com.example.ui.components.QuickActionItem
import com.example.ui.components.QuickActionsGrid
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

@Composable
fun JarvisMainScreen(
    uiState: JarvisUiState,
    history: List<CommandHistoryEntity>,
    onToggleMic: () -> Unit,
    onStopSpeaking: () -> Unit,
    onSubmitText: (String) -> Unit,
    onQuickAction: (QuickActionItem) -> Unit,
    onNavigate: (ScreenNav) -> Unit,
    onConfirmAction: () -> Unit,
    onCancelAction: () -> Unit,
    onSelectDisambiguatedContact: (com.example.tools.ContactMatch) -> Unit = {},
    onCancelDisambiguation: () -> Unit = {},
    onSimulateVoiceCommand: (String) -> Unit = {}
) {
    var textInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        JarvisSurfaceVariant.copy(alpha = 0.5f),
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
            // 1. Top HUD Bar
            item {
                TopHudBar(
                    userName = uiState.userName,
                    greeting = uiState.greeting,
                    onOpenColorOS = { onNavigate(ScreenNav.COLOROS_HUB) },
                    onOpenSettings = { onNavigate(ScreenNav.SETTINGS) }
                )
            }

            // 2. Central Arc Reactor AI Core & Status
            item {
                CentralAiCoreSection(
                    mode = uiState.coreMode,
                    audioRmsDb = uiState.audioRmsDb,
                    isSpeaking = uiState.isSpeaking,
                    onToggleMic = onToggleMic,
                    onStopSpeaking = onStopSpeaking
                )
            }

            // 3. Audio Waveform Visualizer
            item {
                AudioWaveformVisualizer(
                    isActive = uiState.isListening || uiState.isSpeaking,
                    audioRmsDb = uiState.audioRmsDb
                )
            }

            // 4. Voice Commands Row (Direct live voice test fallback for Preview & Quick Voice)
            item {
                VoiceTestCommandsRow(
                    onCommandSelected = { cmd -> onSimulateVoiceCommand(cmd) }
                )
            }

            // 5. Response Display Card
            item {
                ResponseDisplayCard(
                    query = uiState.lastUserQuery,
                    response = uiState.lastJarvisResponse,
                    toolUsed = uiState.lastToolUsed,
                    mode = uiState.coreMode,
                    liveTranscript = uiState.liveSpeechTranscript
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

            // 7. Navigation Hub Chips
            item {
                NavigationPillRow(
                    onNavigate = onNavigate
                )
            }

            // 7. Quick Actions Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUICK ACTIONS",
                            color = JarvisCyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "OPPO Reno14 5G Mode",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    QuickActionsGrid(
                        actions = DefaultQuickActions,
                        onActionClick = onQuickAction
                    )
                }
            }

            // 8. Recent Commands History
            if (history.isNotEmpty()) {
                item {
                    Text(
                        text = "RECENT TELEMETRY & COMMANDS",
                        color = JarvisCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                items(history.take(4).size) { index ->
                    val item = history[index]
                    RecentCommandTile(item = item)
                }
            }
        }

        // Disambiguation Dialog
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
private fun TopHudBar(
    userName: String,
    greeting: String,
    onOpenColorOS: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "JARVIS AI",
                color = JarvisCyanPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                text = "$greeting, $userName.",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Reno14 5G Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisSurfaceVariant)
                    .border(1.dp, JarvisBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { onOpenColorOS() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("device_badge_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = JarvisCyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ColorOS 16",
                        color = JarvisCyanBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Settings button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisSurfaceVariant)
                    .testTag("main_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = JarvisCyanPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CentralAiCoreSection(
    mode: AiCoreMode,
    audioRmsDb: Float,
    isSpeaking: Boolean,
    onToggleMic: () -> Unit,
    onStopSpeaking: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_animation")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // State label badge (Requirements: "JARVIS is ready", "Listening...", "Processing...")
        val (stateText, stateColor) = when (mode) {
            AiCoreMode.IDLE -> Pair("JARVIS is ready", JarvisCyanPrimary)
            AiCoreMode.LISTENING -> Pair("Listening...", JarvisCyanBright)
            AiCoreMode.THINKING, AiCoreMode.EXECUTING -> Pair("Processing...", JarvisAmberCore)
            AiCoreMode.SPEAKING -> Pair("Speaking...", JarvisElectricBlue)
            AiCoreMode.CONFIRMING -> Pair("Waiting Confirmation", JarvisAmberCore)
            AiCoreMode.ERROR -> Pair("Action Required", JarvisDangerRed)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(stateColor.copy(alpha = 0.15f))
                .border(1.dp, stateColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = stateText,
                color = stateColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Arc Reactor Circular AI Core
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

        // Tap to talk action button with visible animation while listening
        Button(
            onClick = onToggleMic,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (mode == AiCoreMode.LISTENING) JarvisDangerRed else JarvisCyanPrimary,
                contentColor = Color.Black
            ),
            border = if (mode == AiCoreMode.LISTENING) BorderStroke(2.dp, JarvisCyanBright.copy(alpha = glowAlpha)) else null,
            modifier = Modifier
                .height(44.dp)
                .scale(if (mode == AiCoreMode.LISTENING) micScale else 1f)
                .testTag("tap_to_talk_button")
        ) {
            Icon(
                imageVector = if (mode == AiCoreMode.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Microphone",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (mode == AiCoreMode.LISTENING) "Listening... (Tap to Stop)" else "Tap to Speak",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun VoiceTestCommandsRow(
    onCommandSelected: (String) -> Unit
) {
    val sampleCommands = listOf(
        "YouTube kholo",
        "WhatsApp kholo",
        "Chrome kholo",
        "Mera battery percentage batao",
        "Time kya hua hai",
        "Ammi ko call karo"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface.copy(alpha = 0.9f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisCyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = JarvisCyanPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "VOICE COMMANDS (TAP TO SPEAK)",
                    color = JarvisCyanPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(sampleCommands) { cmd ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(JarvisSurfaceVariant)
                            .border(1.dp, JarvisBorderGlow.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .clickable { onCommandSelected(cmd) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "🎙️ $cmd",
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
    mode: AiCoreMode,
    liveTranscript: String = ""
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .testTag("jarvis_response_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Live recognized speech while listening
            if (mode == AiCoreMode.LISTENING && liveTranscript.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LISTENING:",
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "JARVIS:",
                    color = JarvisCyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (toolUsed != "none") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(JarvisSurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = toolUsed,
                            color = JarvisCyanBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
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
            placeholder = { Text("Type command (e.g. YouTube kholo, Srinagar weather)", fontSize = 13.sp) },
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
                .size(50.dp)
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
private fun NavigationPillRow(
    onNavigate: (ScreenNav) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PillButton("Contacts", Modifier.weight(1f)) { onNavigate(ScreenNav.CONTACTS) }
        PillButton("ColorOS", Modifier.weight(1f)) { onNavigate(ScreenNav.COLOROS_HUB) }
        PillButton("Vault", Modifier.weight(1f)) { onNavigate(ScreenNav.MEMORY_VAULT) }
        PillButton("Routines", Modifier.weight(1f)) { onNavigate(ScreenNav.ROUTINES) }
        PillButton("Wizard", Modifier.weight(1f)) { onNavigate(ScreenNav.SETUP_WIZARD) }
    }
}

@Composable
private fun PillButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(JarvisSurfaceVariant)
            .border(1.dp, JarvisBorderGlow.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = JarvisCyanPrimary,
            fontSize = 11.sp,
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
