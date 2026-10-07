package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.TextPrimary

data class QuickActionItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val commandHint: String
)

val DefaultQuickActions = listOf(
    QuickActionItem("talk", "Talk", Icons.Default.Mic, "Hey JARVIS"),
    QuickActionItem("call", "Call", Icons.Default.Phone, "Call Maroof"),
    QuickActionItem("whatsapp", "WhatsApp", Icons.Default.Chat, "WhatsApp open karo"),
    QuickActionItem("weather", "Weather", Icons.Default.WbSunny, "Srinagar ka weather"),
    QuickActionItem("maps", "Maps", Icons.Default.Map, "Ghar ka route"),
    QuickActionItem("camera", "Camera", Icons.Default.CameraAlt, "Camera kholo"),
    QuickActionItem("youtube", "YouTube", Icons.Default.PlayArrow, "YouTube kholo"),
    QuickActionItem("alarm", "Alarm", Icons.Default.Alarm, "7 AM alarm"),
    QuickActionItem("timer", "Timer", Icons.Default.Timer, "10 min timer"),
    QuickActionItem("settings", "Settings", Icons.Default.Settings, "Device settings")
)

@Composable
fun QuickActionsGrid(
    actions: List<QuickActionItem> = DefaultQuickActions,
    onActionClick: (QuickActionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        actions.chunked(5).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowActions.forEach { action ->
                    QuickActionTile(
                        action = action,
                        onClick = { onActionClick(action) },
                        modifier = Modifier.weight(1f)
                    )
                }
                val remaining = 5 - rowActions.size
                for (i in 0 until remaining) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun QuickActionTile(
    action: QuickActionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        JarvisSurfaceVariant.copy(alpha = 0.85f),
                        JarvisSurfaceVariant.copy(alpha = 0.45f)
                    )
                )
            )
            .border(1.dp, JarvisBorderGlow.copy(alpha = 0.35f), shape)
            .clickable(
                onClick = onClick,
                indication = ripple(bounded = true, color = JarvisCyanPrimary),
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(vertical = 10.dp, horizontal = 2.dp)
            .testTag("quick_action_${action.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = JarvisCyanPrimary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = action.label,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
