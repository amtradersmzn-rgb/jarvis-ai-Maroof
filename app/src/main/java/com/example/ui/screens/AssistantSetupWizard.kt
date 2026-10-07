package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.engine.ColorOSHelper
import com.example.ui.components.ArcReactorCore
import com.example.engine.AiCoreMode
import com.example.ui.theme.JarvisAmberCore
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisSuccessGreen
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AssistantSetupWizard(
    initialUserName: String,
    onComplete: (userName: String, language: String, pitch: Float, speed: Float) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) }
    val totalSteps = 10

    var userName by remember { mutableStateOf(if (initialUserName.isBlank()) "Maroof" else initialUserName) }
    var selectedLang by remember { mutableStateOf("Hinglish") }
    var voicePitch by remember { mutableFloatStateOf(0.95f) }
    var voiceSpeed by remember { mutableFloatStateOf(1.05f) }

    var micPermissionGranted by remember { mutableStateOf(false) }
    var notifPermissionGranted by remember { mutableStateOf(false) }
    var testCommandExecuted by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> micPermissionGranted = granted }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> notifPermissionGranted = granted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("assistant_setup_wizard_screen")
    ) {
        // Top header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SETUP WIZARD",
                    color = JarvisCyanPrimary,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Step $step of $totalSteps",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.testTag("wizard_close_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = JarvisCyanPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { step / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = JarvisCyanPrimary,
            trackColor = JarvisSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Step Content Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                when (step) {
                    1 -> Step1UserName(userName) { userName = it }
                    2 -> Step2Language(selectedLang) { selectedLang = it }
                    3 -> Step3VoiceTuning(voicePitch, voiceSpeed, { voicePitch = it }, { voiceSpeed = it })
                    4 -> Step4Microphone(micPermissionGranted) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    5 -> Step5Notification(notifPermissionGranted) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notifPermissionGranted = true
                        }
                    }
                    6 -> Step6NotificationListener(context)
                    7 -> Step7DefaultAssistant(context)
                    8 -> Step8ColorOSBattery(context)
                    9 -> Step9WakeWord()
                    10 -> Step10TestCommand(userName, testCommandExecuted) { testCommandExecuted = true }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(20.dp))

        // Navigation Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (step > 1) {
                OutlinedButton(
                    onClick = { step-- },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("wizard_prev_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    if (step < totalSteps) {
                        step++
                    } else {
                        onComplete(userName, selectedLang, voicePitch, voiceSpeed)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary, contentColor = Color.Black),
                modifier = Modifier.testTag("wizard_next_button")
            ) {
                Text(
                    text = if (step == totalSteps) "Complete Setup" else "Next Step",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (step == totalSteps) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun Step1UserName(name: String, onNameChange: (String) -> Unit) {
    Text("Welcome to JARVIS", color = JarvisCyanPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Your personal AI mobile assistant.", color = TextSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(16.dp))
    Text("What should JARVIS call you?", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = { Text("Enter your name (e.g. Maroof)") },
        modifier = Modifier.fillMaxWidth().testTag("wizard_user_name_input"),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisCyanPrimary,
            unfocusedBorderColor = JarvisBorderGlow,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        singleLine = true
    )
}

@Composable
private fun Step2Language(selected: String, onSelect: (String) -> Unit) {
    Text("Language Preference", color = JarvisCyanPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("JARVIS seamlessly supports multilingual conversations.", color = TextSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(16.dp))

    val langs = listOf("Hinglish (Recommended)", "Hindi", "English")
    langs.forEach { lang ->
        val isSelected = selected.startsWith(lang.substringBefore(" "))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) JarvisCyanPrimary.copy(alpha = 0.2f) else JarvisSurfaceVariant)
                .border(1.dp, if (isSelected) JarvisCyanPrimary else JarvisBorderGlow.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .clickable { onSelect(lang.substringBefore(" ")) }
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Translate, contentDescription = null, tint = if (isSelected) JarvisCyanPrimary else TextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(lang, color = if (isSelected) JarvisCyanBright else TextPrimary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun Step3VoiceTuning(pitch: Float, speed: Float, onPitchChange: (Float) -> Unit, onSpeedChange: (Float) -> Unit) {
    Text("Voice Calibration", color = JarvisCyanPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Tune JARVIS's futuristic synthetic voice.", color = TextSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Text("Pitch: ${String.format("%.2f", pitch)}x", color = TextPrimary)
    Slider(
        value = pitch,
        onValueChange = onPitchChange,
        valueRange = 0.7f..1.3f,
        colors = SliderDefaults.colors(thumbColor = JarvisCyanPrimary, activeTrackColor = JarvisCyanPrimary)
    )

    Spacer(modifier = Modifier.height(12.dp))
    Text("Speed: ${String.format("%.2f", speed)}x", color = TextPrimary)
    Slider(
        value = speed,
        onValueChange = onSpeedChange,
        valueRange = 0.8f..1.4f,
        colors = SliderDefaults.colors(thumbColor = JarvisCyanPrimary, activeTrackColor = JarvisCyanPrimary)
    )
}

@Composable
private fun Step4Microphone(isGranted: Boolean, onRequest: () -> Unit) {
    Text("Microphone Access", color = JarvisCyanPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Voice recognition requires standard RECORD_AUDIO permission.", color = TextSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onRequest,
        colors = ButtonDefaults.buttonColors(containerColor = if (isGranted) JarvisSuccessGreen else JarvisCyanPrimary, contentColor = Color.Black),
        modifier = Modifier.fillMaxWidth().testTag("wizard_grant_mic_button")
    ) {
        Icon(if (isGranted) Icons.Default.Check else Icons.Default.Mic, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isGranted) "Permission Granted" else "Grant Microphone Permission")
    }
}

@Composable
private fun Step5Notification(isGranted: Boolean, onRequest: () -> Unit) {
    Text("Notifications Permission", color = JarvisCyanPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Enables persistent status and quick activation tile notifications.", color = TextSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onRequest,
        colors = ButtonDefaults.buttonColors(containerColor = if (isGranted) JarvisSuccessGreen else JarvisCyanPrimary, contentColor = Color.Black),
        modifier = Modifier.fillMaxWidth().testTag("wizard_grant_notif_button")
    ) {
        Icon(if (isGranted) Icons.Default.Check else Icons.Default.Notifications, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isGranted) "Notifications Allowed" else "Allow Notifications")
    }
}

@Composable
private fun Step6NotificationListener(context: Context) {
    Text("Notification Reader (Optional)", color = JarvisCyanPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Allows JARVIS to safely summarize incoming WhatsApp and Gmail notifications on command. Stays completely local to your device.", color = TextSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            val intent = ColorOSHelper.getNotificationListenerSettingsIntent()
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant, contentColor = JarvisCyanPrimary),
        modifier = Modifier.fillMaxWidth().testTag("wizard_open_notif_listener_button")
    ) {
        Text("Open Notification Access Settings")
    }
}

@Composable
private fun Step7DefaultAssistant(context: Context) {
    Text("Default Digital Assistant", color = JarvisCyanPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Set JARVIS as your phone's default voice assistant for hold-home or swipe-corner shortcuts.", color = TextSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            val intent = ColorOSHelper.getDefaultAssistantIntent(context)
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary, contentColor = Color.Black),
        modifier = Modifier.fillMaxWidth().testTag("wizard_set_default_assistant_button")
    ) {
        Icon(Icons.Default.Assistant, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Select JARVIS as Default Assistant")
    }
}

@Composable
private fun Step8ColorOSBattery(context: Context) {
    Text("ColorOS 16 Optimization", color = JarvisCyanPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("ColorOS aggressively saves battery. To prevent terminating JARVIS, allow background activity.", color = TextSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            val intent = ColorOSHelper.getBatteryOptimizationIntent(context)
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(containerColor = JarvisAmberCore, contentColor = Color.Black),
        modifier = Modifier.fillMaxWidth().testTag("wizard_fix_coloros_battery_button")
    ) {
        Icon(Icons.Default.BatteryAlert, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Fix Background Operation")
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("Tip: Also lock JARVIS in recent apps overview for uninterrupted performance.", color = TextSecondary, fontSize = 12.sp)
}

@Composable
private fun Step9WakeWord() {
    Text("Wake-Word Activation", color = JarvisCyanPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("You can wake JARVIS anytime saying 'Hey JARVIS'. Android requires a persistent foreground notification during continuous listening.", color = TextSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Supported Activation Paths:", color = JarvisCyanBright, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("• 'Hey JARVIS' voice wake-up\n• Large AI Core tap-to-talk\n• Quick Settings notification shade tile\n• Default assistant hold gesture", color = TextPrimary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Step10TestCommand(userName: String, isTested: Boolean, onTest: () -> Unit) {
    Text("Final Voice Test", color = JarvisCyanPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Let's perform your inaugural interaction with JARVIS.", color = TextSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
        modifier = Modifier.fillMaxWidth().border(1.dp, JarvisCyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("You:", color = TextSecondary, fontSize = 12.sp)
            Text("“Hey Jarvis, who are you?”", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("JARVIS:", color = JarvisCyanPrimary, fontSize = 12.sp)
            Text("“Main JARVIS hoon — aapka personal AI mobile assistant.”", color = JarvisCyanBright, fontSize = 15.sp)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onTest,
        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary, contentColor = Color.Black),
        modifier = Modifier.fillMaxWidth().testTag("wizard_test_voice_button")
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isTested) "Test Verified! Ready to Launch" else "Simulate Voice Command")
    }
}
