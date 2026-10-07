package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ColorOSHelper
import com.example.service.JarvisNotificationListenerService
import com.example.ui.theme.JarvisAmberCore
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    currentApiKey: String,
    speechRate: Float,
    speechPitch: Float,
    wakeWordEnabled: Boolean,
    onSaveApiKey: (String) -> Unit,
    onSaveVoiceParams: (rate: Float, pitch: Float) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onNavigateToContacts: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var rate by remember { mutableFloatStateOf(speechRate) }
    var pitch by remember { mutableFloatStateOf(speechPitch) }
    var wakeWord by remember { mutableStateOf(wakeWordEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
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
                    text = "System & AI Settings",
                    color = JarvisCyanPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure AI Provider, Voice Engine & Permissions",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Provider Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = JarvisCyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Provider Configuration",
                        color = JarvisCyanBright,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Configure an optional Gemini or OpenAI API Key for advanced general knowledge answers. When blank or offline, JARVIS seamlessly executes via the built-in on-device NLP engine.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text("Enter API Key (Optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyanPrimary,
                        unfocusedBorderColor = JarvisBorderGlow,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onSaveApiKey(apiKey.trim()) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyanPrimary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("save_api_key_button")
                ) {
                    Text("Save Provider Settings", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Engine Tuning
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = JarvisCyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Speech Synthesis (TTS) Tuning",
                        color = JarvisCyanBright,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Speech Pitch: ${String.format("%.2f", pitch)}x", color = TextPrimary, fontSize = 13.sp)
                Slider(
                    value = pitch,
                    onValueChange = {
                        pitch = it
                        onSaveVoiceParams(rate, pitch)
                    },
                    valueRange = 0.7f..1.3f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisCyanPrimary,
                        activeTrackColor = JarvisCyanPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Speaking Speed: ${String.format("%.2f", rate)}x", color = TextPrimary, fontSize = 13.sp)
                Slider(
                    value = rate,
                    onValueChange = {
                        rate = it
                        onSaveVoiceParams(rate, pitch)
                    },
                    valueRange = 0.8f..1.4f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisCyanPrimary,
                        activeTrackColor = JarvisCyanPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Integrations & Toggles
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "System & Permissions",
                    color = JarvisCyanBright,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Wake-word toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Continuous Wake-Word ('Hey JARVIS')", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Runs foreground service with transparent notification indicator", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = wakeWord,
                        onCheckedChange = {
                            wakeWord = it
                            onToggleWakeWord(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyanPrimary, checkedTrackColor = JarvisSurfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Default Assistant
                Button(
                    onClick = {
                        val intent = ColorOSHelper.getDefaultAssistantIntent(context)
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant, contentColor = JarvisCyanPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("open_assistant_settings_button")
                ) {
                    Icon(Icons.Default.Assistant, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set as Default Digital Assistant")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Notification Listener
                Button(
                    onClick = {
                        val intent = ColorOSHelper.getNotificationListenerSettingsIntent()
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant, contentColor = JarvisCyanPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("open_notif_listener_settings_button")
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Notification Listener Access Settings")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Manage Contacts & Trusted Calling
                Button(
                    onClick = onNavigateToContacts,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant, contentColor = JarvisCyanPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("open_contacts_manager_settings_button")
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Contacts & Trusted Calling")
                }
            }
        }
    }
}
