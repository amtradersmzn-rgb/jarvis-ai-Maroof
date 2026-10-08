package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ActiveAiEngine
import com.example.engine.ColorOSHelper
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
fun SettingsScreen(
    currentChatGptKey: String,
    currentGeminiKey: String,
    currentAiEngine: ActiveAiEngine,
    speechRate: Float,
    speechPitch: Float,
    currentLanguage: String,
    wakeWordEnabled: Boolean,
    conversationModeEnabled: Boolean,
    confirmationModeEnabled: Boolean,
    memoryEnabled: Boolean,
    onSaveApiKeys: (chatGpt: String, gemini: String) -> Unit,
    onSaveAiEngine: (ActiveAiEngine) -> Unit,
    onSaveVoiceParams: (rate: Float, pitch: Float, language: String) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleConversationMode: (Boolean) -> Unit,
    onToggleConfirmationMode: (Boolean) -> Unit,
    onToggleMemory: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onClearMemory: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToMemoryVault: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Assistant", "AI Brains", "Voice", "Privacy", "Appearance")

    var chatGptKey by remember { mutableStateOf(currentChatGptKey) }
    var geminiKey by remember { mutableStateOf(currentGeminiKey) }
    var selectedAiEngine by remember { mutableStateOf(currentAiEngine) }
    var rate by remember { mutableFloatStateOf(speechRate) }
    var pitch by remember { mutableFloatStateOf(speechPitch) }
    var lang by remember { mutableStateOf(currentLanguage) }
    var wakeWord by remember { mutableStateOf(wakeWordEnabled) }
    var conversationMode by remember { mutableStateOf(conversationModeEnabled) }
    var confirmationMode by remember { mutableStateOf(confirmationModeEnabled) }
    var memoryMode by remember { mutableStateOf(memoryEnabled) }
    var animationIntensity by remember { mutableFloatStateOf(1.0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = JarvisCyanPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "JARVIS V3 Settings",
                    color = JarvisCyanPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Assistant, AI Engines, Voice, Privacy & Permissions",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = JarvisSurface,
            contentColor = JarvisCyanPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, JarvisBorderGlow.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) JarvisCyanBright else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Assistant Settings
                    AssistantSettingsCard(
                        wakeWord = wakeWord,
                        conversationMode = conversationMode,
                        confirmationMode = confirmationMode,
                        onToggleWakeWord = {
                            wakeWord = it
                            onToggleWakeWord(it)
                        },
                        onToggleConversationMode = {
                            conversationMode = it
                            onToggleConversationMode(it)
                        },
                        onToggleConfirmationMode = {
                            confirmationMode = it
                            onToggleConfirmationMode(it)
                        },
                        onOpenDefaultAssistant = {
                            val intent = ColorOSHelper.getDefaultAssistantIntent(context)
                            context.startActivity(intent)
                        },
                        onOpenPermissions = onNavigateToPermissions
                    )
                }

                1 -> {
                    // AI Brains Settings
                    AiBrainSettingsCard(
                        chatGptKey = chatGptKey,
                        geminiKey = geminiKey,
                        activeEngine = selectedAiEngine,
                        onChatGptKeyChange = { chatGptKey = it },
                        onGeminiKeyChange = { geminiKey = it },
                        onEngineChange = {
                            selectedAiEngine = it
                            onSaveAiEngine(it)
                        },
                        onSaveKeys = {
                            onSaveApiKeys(chatGptKey.trim(), geminiKey.trim())
                            Toast.makeText(context, "API Keys saved securely", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                2 -> {
                    // Voice Settings
                    VoiceSettingsCard(
                        rate = rate,
                        pitch = pitch,
                        language = lang,
                        onRateChange = {
                            rate = it
                            onSaveVoiceParams(rate, pitch, lang)
                        },
                        onPitchChange = {
                            pitch = it
                            onSaveVoiceParams(rate, pitch, lang)
                        },
                        onLanguageChange = {
                            lang = it
                            onSaveVoiceParams(rate, pitch, lang)
                        }
                    )
                }

                3 -> {
                    // Privacy & Memory
                    PrivacySettingsCard(
                        memoryEnabled = memoryMode,
                        onToggleMemory = {
                            memoryMode = it
                            onToggleMemory(it)
                        },
                        onOpenMemoryVault = onNavigateToMemoryVault,
                        onClearMemory = {
                            onClearMemory()
                            Toast.makeText(context, "User memory cleared", Toast.LENGTH_SHORT).show()
                        },
                        onClearHistory = {
                            onClearHistory()
                            Toast.makeText(context, "Command history cleared", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                4 -> {
                    // Appearance Settings
                    AppearanceSettingsCard(
                        animationIntensity = animationIntensity,
                        onAnimationChange = { animationIntensity = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantSettingsCard(
    wakeWord: Boolean,
    conversationMode: Boolean,
    confirmationMode: Boolean,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleConversationMode: (Boolean) -> Unit,
    onToggleConfirmationMode: (Boolean) -> Unit,
    onOpenDefaultAssistant: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        modifier = Modifier.fillMaxWidth().border(1.dp, JarvisBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Assistant Controls", color = JarvisCyanPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            // Wake word
            SettingToggleRow(
                title = "Hey JARVIS Wake-Word",
                subtitle = "Listens in foreground service with persistent transparent status indicator",
                checked = wakeWord,
                onCheckedChange = onToggleWakeWord
            )

            // Conversational mode
            SettingToggleRow(
                title = "Continuous Conversation Mode",
                subtitle = "Keeps listening window open for follow-up questions without repeating wake-word",
                checked = conversationMode,
                onCheckedChange = onToggleConversationMode
            )

            // Confirmation mode
            SettingToggleRow(
                title = "Strict Confirmation Mode",
                subtitle = "Always asks for voice/UI confirmation before calls, SMS, and critical actions",
                checked = confirmationMode,
                onCheckedChange = onToggleConfirmationMode
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onOpenDefaultAssistant,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().testTag("settings_set_default_assistant_btn")
            ) {
                Icon(Icons.Default.Assistant, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set as Default Android Assistant")
            }

            OutlinedButton(
                onClick = onOpenPermissions,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings_open_permissions_center_btn")
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = JarvisCyanBright)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Permission Center", color = JarvisCyanBright)
            }
        }
    }
}

@Composable
private fun AiBrainSettingsCard(
    chatGptKey: String,
    geminiKey: String,
    activeEngine: ActiveAiEngine,
    onChatGptKeyChange: (String) -> Unit,
    onGeminiKeyChange: (String) -> Unit,
    onEngineChange: (ActiveAiEngine) -> Unit,
    onSaveKeys: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        modifier = Modifier.fillMaxWidth().border(1.dp, JarvisBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("AI Brains & Routing", color = JarvisCyanPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            Text("Default AI Engine:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                val engines = listOf(
                    Pair(ActiveAiEngine.AUTO, "⚡ Auto AI"),
                    Pair(ActiveAiEngine.CHATGPT, "🤖 ChatGPT"),
                    Pair(ActiveAiEngine.GEMINI, "✨ Gemini"),
                    Pair(ActiveAiEngine.COMPARE, "⚖️ Compare")
                )
                engines.forEach { (engine, label) ->
                    val isSelected = activeEngine == engine
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) JarvisCyanPrimary.copy(alpha = 0.25f) else JarvisSurfaceVariant)
                            .border(1.dp, if (isSelected) JarvisCyanPrimary else JarvisBorderGlow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { onEngineChange(engine) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) JarvisCyanBright else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ChatGPT Key
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = JarvisElectricBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OpenAI API Key (ChatGPT)", color = JarvisElectricBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = chatGptKey,
                    onValueChange = onChatGptKeyChange,
                    placeholder = { Text("sk-...", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisElectricBlue,
                        unfocusedBorderColor = JarvisBorderGlow,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("chatgpt_key_input")
                )
            }

            // Gemini Key
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = JarvisCyanPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Google Gemini API Key", color = JarvisCyanPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = onGeminiKeyChange,
                    placeholder = { Text("Gemini API Key...", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyanPrimary,
                        unfocusedBorderColor = JarvisBorderGlow,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("gemini_key_input")
                )
            }

            Button(
                onClick = onSaveKeys,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().testTag("save_ai_keys_btn")
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save API Credentials Securely", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VoiceSettingsCard(
    rate: Float,
    pitch: Float,
    language: String,
    onRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        modifier = Modifier.fillMaxWidth().border(1.dp, JarvisBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Voice Synthesis Engine", color = JarvisCyanPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            Text("Language:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Auto", "Hinglish", "Hindi", "English").forEach { l ->
                    val isSelected = language.equals(l, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) JarvisCyanPrimary.copy(alpha = 0.25f) else JarvisSurfaceVariant)
                            .border(1.dp, if (isSelected) JarvisCyanPrimary else JarvisBorderGlow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { onLanguageChange(l) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = l,
                            color = if (isSelected) JarvisCyanBright else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text("Speaking Pitch: ${String.format("%.2f", pitch)}x (Calm Futuristic)", color = TextPrimary, fontSize = 13.sp)
            Slider(
                value = pitch,
                onValueChange = onPitchChange,
                valueRange = 0.75f..1.25f,
                colors = SliderDefaults.colors(thumbColor = JarvisCyanPrimary, activeTrackColor = JarvisCyanPrimary)
            )

            Text("Speaking Speed: ${String.format("%.2f", rate)}x", color = TextPrimary, fontSize = 13.sp)
            Slider(
                value = rate,
                onValueChange = onRateChange,
                valueRange = 0.8f..1.3f,
                colors = SliderDefaults.colors(thumbColor = JarvisCyanPrimary, activeTrackColor = JarvisCyanPrimary)
            )
        }
    }
}

@Composable
private fun PrivacySettingsCard(
    memoryEnabled: Boolean,
    onToggleMemory: (Boolean) -> Unit,
    onOpenMemoryVault: () -> Unit,
    onClearMemory: () -> Unit,
    onClearHistory: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        modifier = Modifier.fillMaxWidth().border(1.dp, JarvisBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Privacy & Memory Controls", color = JarvisCyanPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            SettingToggleRow(
                title = "Assistant Memory",
                subtitle = "Only remembers information that you explicitly allow. Stored strictly on device.",
                checked = memoryEnabled,
                onCheckedChange = onToggleMemory
            )

            OutlinedButton(
                onClick = onOpenMemoryVault,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings_view_memory_btn")
            ) {
                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp), tint = JarvisCyanBright)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Stored Memory Vault", color = JarvisCyanBright)
            }

            Button(
                onClick = onClearMemory,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant, contentColor = JarvisDangerRed),
                modifier = Modifier.fillMaxWidth().testTag("settings_clear_memory_btn")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Memory")
            }

            Button(
                onClick = onClearHistory,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant, contentColor = JarvisDangerRed),
                modifier = Modifier.fillMaxWidth().testTag("settings_clear_history_btn")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Command Telemetry History")
            }
        }
    }
}

@Composable
private fun AppearanceSettingsCard(
    animationIntensity: Float,
    onAnimationChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        modifier = Modifier.fillMaxWidth().border(1.dp, JarvisBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Appearance & Theme", color = JarvisCyanPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = JarvisCyanPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Theme: JARVIS Dark Futuristic Glow", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Optimized for AMOLED & ColorOS 16 Dark Mode", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text("AI Orb & Waveform Animation Intensity: ${String.format("%.1f", animationIntensity)}x", color = TextPrimary, fontSize = 13.sp)
            Slider(
                value = animationIntensity,
                onValueChange = onAnimationChange,
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(thumbColor = JarvisCyanPrimary, activeTrackColor = JarvisCyanPrimary)
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyanPrimary, checkedTrackColor = JarvisSurfaceVariant)
        )
    }
}
