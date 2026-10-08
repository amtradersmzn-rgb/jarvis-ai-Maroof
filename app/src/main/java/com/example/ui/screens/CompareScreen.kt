package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun CompareScreen(
    currentQuery: String,
    chatGptResponse: String,
    geminiResponse: String,
    summaryResponse: String,
    isLoading: Boolean,
    onCompareQuery: (String) -> Unit,
    onSummarizeBoth: () -> Unit,
    onSpeakText: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var queryInput by remember { mutableStateOf(currentQuery) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(16.dp)
            .testTag("compare_screen")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("compare_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = JarvisCyanPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = JarvisAmberCore,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Compare Mode",
                        color = JarvisCyanPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "ChatGPT (OpenAI) + Gemini (Google) Side-by-Side",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input query
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = queryInput,
                onValueChange = { queryInput = it },
                placeholder = { Text("Ask both ChatGPT & Gemini...", fontSize = 13.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyanPrimary,
                    unfocusedBorderColor = JarvisBorderGlow,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = JarvisSurface,
                    unfocusedContainerColor = JarvisSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("compare_input_field")
            )
            IconButton(
                onClick = {
                    if (queryInput.isNotBlank()) {
                        onCompareQuery(queryInput)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(JarvisCyanPrimary, RoundedCornerShape(12.dp))
                    .testTag("compare_submit_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Compare", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = JarvisCyanPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Querying ChatGPT & Gemini simultaneously...",
                        color = JarvisCyanBright,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ChatGPT Response Card
            ModelResponseCard(
                modelName = "ChatGPT (OpenAI)",
                icon = Icons.Default.SmartToy,
                brandColor = JarvisElectricBlue,
                responseText = chatGptResponse.ifBlank { "Awaiting query or API response..." },
                onCopy = { copyToClipboard(context, "ChatGPT", chatGptResponse) },
                onShare = { shareText(context, "ChatGPT Answer", chatGptResponse) },
                onSpeak = { onSpeakText(chatGptResponse) },
                onRegenerate = { onCompareQuery(queryInput) }
            )

            // Gemini Response Card
            ModelResponseCard(
                modelName = "Gemini (Google)",
                icon = Icons.Default.AutoAwesome,
                brandColor = JarvisCyanPrimary,
                responseText = geminiResponse.ifBlank { "Awaiting query or API response..." },
                onCopy = { copyToClipboard(context, "Gemini", geminiResponse) },
                onShare = { shareText(context, "Gemini Answer", geminiResponse) },
                onSpeak = { onSpeakText(geminiResponse) },
                onRegenerate = { onCompareQuery(queryInput) }
            )

            // Summarize Button
            if (chatGptResponse.isNotBlank() && geminiResponse.isNotBlank()) {
                Button(
                    onClick = onSummarizeBoth,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisAmberCore,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("summarize_both_button")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("JARVIS, Summarize Both Answers", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Summary Card
            if (summaryResponse.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, JarvisAmberCore.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .testTag("compare_summary_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚡ JARVIS SYNTHESIS & CONCLUSION",
                                color = JarvisAmberCore,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row {
                                IconButton(
                                    onClick = { onSpeakText(summaryResponse) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = JarvisAmberCore, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { copyToClipboard(context, "JARVIS Summary", summaryResponse) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = JarvisAmberCore, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = summaryResponse,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelResponseCard(
    modelName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    brandColor: Color,
    responseText: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSpeak: () -> Unit,
    onRegenerate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, brandColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = brandColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = modelName,
                        color = brandColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onSpeak, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onRegenerate, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = responseText,
                color = TextPrimary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "$label response copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, subject: String, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share with")
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)
}
