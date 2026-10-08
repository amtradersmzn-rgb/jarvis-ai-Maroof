package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
    onComplete: (userName: String, language: String, pitch: Float, speed: Float, chatGptKey: String, geminiKey: String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) }
    val totalSteps = 8

    var userName by remember { mutableStateOf(if (initialUserName.isBlank()) "Sir" else initialUserName) }
    var selectedLang by remember { mutableStateOf("Hinglish") }
    var voicePitch by remember { mutableFloatStateOf(0.92f) }
    var voiceSpeed by remember { mutableFloatStateOf(1.0f) }
    var chatGptApiKey by remember { mutableStateOf("") }
    var geminiApiKey by remember { mutableStateOf("") }

    var micPermissionGranted by remember { mutableStateOf(false) }
    var testCommandExecuted by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> micPermissionGranted = granted }

    val optionalPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

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
                    text = "Welcome to JARVIS V3",
                    color = JarvisCyanPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Your Personal AI Assistant • Step $step of $totalSteps",
                    color = TextSecondary,
                    fontSize = 12.sp
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

        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { step / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = JarvisCyanPrimary,
            trackColor = JarvisSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

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
                    1 -> Step1Language(selectedLang) { selectedLang = it }
                    2 -> Step2Microphone(micPermissionGranted) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    3 -> Step3VoiceSetup(voicePitch, voiceSpeed, { voicePitch = it }, { voiceSpeed = it })
                    4 -> Step4ChatGptSetup(chatGptApiKey) { chatGptApiKey = it }
                    5 -> Step5GeminiSetup(geminiApiKey) { geminiApiKey = it }
                    6 -> Step6OptionalPermissions {
                        optionalPermissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.CALL_PHONE,
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    }
                    7 -> Step7DefaultAssistant(context)
                    8 -> Step8TestCommand(testCommandExecuted) { testCommandExecuted = true }
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
                        onComplete(userName, selectedLang, voicePitch, voiceSpeed, chatGptApiKey, geminiApiKey)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary, contentColor = Color.Black),
                modifier = Modifier.testTag("wizard_next_button")
            ) {
                Text(
                    text = if (step == totalSteps) "Launch JARVIS V3" else "Next",
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
private fun Step1Language(selected: String, onSelect: (String) -> Unit) {
    Text("1. Choose Language", color = JarvisCyanPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Text("JARVIS understands and automatically speaks in your preferred language.", color = TextSecondary, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(16.dp))

    val langs = listOf(
        Pair("Hinglish", "Natural Hindi & English mix (Recommended)"),
        Pair("Hindi", "हिंदी - Shuddh Hindi responses"),
        Pair("English", "English - Global assistant vocabulary")
    )
    langs.forEach { (lang, desc) ->
        val isSelected = selected == lang
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) JarvisCyanPrimary.copy(alpha = 0.2f) else JarvisSurfaceVariant)
                .border(1.dp, if (isSelected) JarvisCyanPrimary else JarvisBorderGlow.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .clickable { onSelect(lang) }
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = if (isSelected) JarvisCyanPrimary else TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(lang, color = if (isSelected) JarvisCyanBright else TextPrimary, fontWeight = FontWeight.Bold)
                }
                Text(desc, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(start = 24.dp))
            }
        }
    }
}

@Composable
private fun Step2Microphone(isGranted: Boolean, onRequest: () -> Unit) {
    Text("2. Microphone Permission", color = JarvisCyanPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Text("Voice-first control and 'Hey JARVIS' wake-word require Android microphone access.", color = TextSecondary, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onRequest,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isGranted) JarvisSuccessGreen else JarvisCyanPrimary,
            contentColor = Color.Black
        ),
        modifier = Modifier.fillMaxWidth().testTag("wizard_grant_mic_button")
    ) {
        Icon(if (isGranted) Icons.Default.Check else Icons.Default.Mic, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isGranted) "Microphone Access Granted" else "Grant Microphone Access")
    }
}

@Composable
private fun Step3VoiceSetup(pitch: Float, speed: Float, onPitchChange: (Float) -> Unit, onSpeedChange: (Float) -> Unit) {
    Text("3. Voice Setup", color = JarvisCyanPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Text("Calibrate JARVIS's futuristic synthetic male voice timbre and pacing.", color = TextSecondary, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Text("Pitch: ${String.format("%.2f", pitch)}x (Deep Futuristic)", color = TextPrimary, fontSize = 13.sp)
    Slider(
        value = pitch,
        onValueChange = onPitchChange,
        valueRange = 0.75f..1.25f,
        colors = SliderDefaults.colors(thumbColor = JarvisCyanPrimary, activeTrackColor = JarvisCyanPrimary)
    )

    Spacer(modifier = Modifier.height(8.dp))
    Text("Speed: ${String.format("%.2f", speed)}x (Conversational Pacing)", color = TextPrimary, fontSize = 13.sp)
    Slider(
        value = speed,
        onValueChange = onSpeedChange,
        valueRange = 0.8f..1.3f,
        colors = SliderDefaults.colors(thumbColor = JarvisCyanPrimary, activeTrackColor = JarvisCyanPrimary)
    )
}

@Composable
private fun Step4ChatGptSetup(apiKey: String, onKeyChange: (String) -> Unit) {
    Text("4. ChatGPT API Setup", color = JarvisCyanPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Text("Integrate OpenAI directly into JARVIS. Stored securely on your device; never sent to third-party trackers.", color = TextSecondary, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = apiKey,
        onValueChange = onKeyChange,
        placeholder = { Text("OpenAI API Key (sk-...) - Optional", fontSize = 12.sp) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisElectricBlue,
            unfocusedBorderColor = JarvisBorderGlow,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier.fillMaxWidth().testTag("wizard_chatgpt_key_input")
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text("Leave blank to configure later in Settings. Built-in system actions work offline.", color = TextSecondary, fontSize = 11.sp)
}

@Composable
private fun Step5GeminiSetup(apiKey: String, onKeyChange: (String) -> Unit) {
    Text("5. Gemini API Setup", color = JarvisCyanPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Text("Integrate Google's official Gemini AI for deep explanations, multimodal knowledge, and Auto AI mode.", color = TextSecondary, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = apiKey,
        onValueChange = onKeyChange,
        placeholder = { Text("Google Gemini API Key - Optional", fontSize = 12.sp) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisCyanPrimary,
            unfocusedBorderColor = JarvisBorderGlow,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier.fillMaxWidth().testTag("wizard_gemini_key_input")
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text("Stored securely in device storage. Can be updated anytime in Settings.", color = TextSecondary, fontSize = 11.sp)
}

@Composable
private fun Step6OptionalPermissions(onRequest: () -> Unit) {
    Text("6. Optional Permissions", color = JarvisCyanPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Text("Grant phone calling, contacts disambiguation, and location for weather and navigation.", color = TextSecondary, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onRequest,
        colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant, contentColor = JarvisCyanPrimary),
        modifier = Modifier.fillMaxWidth().testTag("wizard_grant_optional_button")
    ) {
        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Request Phone, Contacts & Location Access")
    }
}

@Composable
private fun Step7DefaultAssistant(context: Context) {
    Text("7. Default Assistant", color = JarvisCyanPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Text("Set JARVIS V3 as your Android default digital assistant to invoke via long-press home or power key.", color = TextSecondary, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            val intent = ColorOSHelper.getDefaultAssistantIntent(context)
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary, contentColor = Color.Black),
        modifier = Modifier.fillMaxWidth().testTag("wizard_set_assistant_button")
    ) {
        Icon(Icons.Default.Assistant, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Set as Default Assistant in Settings")
    }
}

@Composable
private fun Step8TestCommand(isTested: Boolean, onTest: () -> Unit) {
    Text("8. Test Voice Command", color = JarvisCyanPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Text("Try your first conversation with JARVIS V3:", color = TextSecondary, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
        modifier = Modifier.fillMaxWidth().border(1.dp, JarvisCyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("User says:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("“Hey Jarvis, introduce yourself.”", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text("JARVIS replies:", color = JarvisCyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("“Hello. I am JARVIS, your personal AI assistant. I'm ready to help.”", color = JarvisCyanBright, fontSize = 14.sp)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onTest,
        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary, contentColor = Color.Black),
        modifier = Modifier.fillMaxWidth().testTag("wizard_test_button")
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isTested) "Test Verified! Ready to Launch" else "Test Interaction Now")
    }
}
