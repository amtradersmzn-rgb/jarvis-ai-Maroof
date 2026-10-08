package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisDangerRed
import com.example.ui.theme.JarvisSuccessGreen
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class PermissionItem(
    val id: String,
    val title: String,
    val permissionKey: String,
    val icon: ImageVector,
    val description: String,
    val isSystemSetting: Boolean = false
)

@Composable
fun PermissionCenterScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val permissionsList = remember {
        listOf(
            PermissionItem(
                id = "mic",
                title = "Microphone",
                permissionKey = Manifest.permission.RECORD_AUDIO,
                icon = Icons.Default.Mic,
                description = "Required for speech recognition, 'Hey JARVIS' wake-word, and real-time audio visualization."
            ),
            PermissionItem(
                id = "contacts",
                title = "Contacts",
                permissionKey = Manifest.permission.READ_CONTACTS,
                icon = Icons.Default.Contacts,
                description = "Required to search contacts, disambiguate duplicate names, and call loved ones naturally."
            ),
            PermissionItem(
                id = "phone",
                title = "Phone Calling",
                permissionKey = Manifest.permission.CALL_PHONE,
                icon = Icons.Default.Call,
                description = "Required to place hands-free direct voice calls through the system dialer."
            ),
            PermissionItem(
                id = "notifications",
                title = "Notifications",
                permissionKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    ""
                },
                icon = Icons.Default.Notifications,
                description = "Required to display assistant status notifications and run foreground wake-word service."
            ),
            PermissionItem(
                id = "location",
                title = "Location",
                permissionKey = Manifest.permission.ACCESS_FINE_LOCATION,
                icon = Icons.Default.LocationOn,
                description = "Required to provide accurate local weather forecasts and route directions on Maps."
            ),
            PermissionItem(
                id = "bluetooth",
                title = "Bluetooth / Device",
                permissionKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Manifest.permission.BLUETOOTH_CONNECT
                } else {
                    Manifest.permission.BLUETOOTH
                },
                icon = Icons.Default.Bluetooth,
                description = "Required to toggle audio routing and check paired headset status."
            )
        )
    }

    val permissionStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            permissionsList.forEach { item ->
                put(item.id, checkPermission(context, item.permissionKey))
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsList.forEach { item ->
            if (item.permissionKey.isNotBlank()) {
                permissionStates[item.id] = checkPermission(context, item.permissionKey)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(16.dp)
            .testTag("permission_center_screen")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("permission_center_back")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = JarvisCyanPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Permission Center",
                    color = JarvisCyanPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Transparent permissions control for JARVIS V3",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(permissionsList) { item ->
                val isGranted = permissionStates[item.id] ?: false
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isGranted) JarvisSuccessGreen.copy(alpha = 0.35f) else JarvisBorderGlow.copy(alpha = 0.35f),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (isGranted) JarvisSuccessGreen else JarvisCyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.title,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // ON/OFF status badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isGranted) JarvisSuccessGreen.copy(alpha = 0.15f) else JarvisDangerRed.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isGranted) JarvisSuccessGreen else JarvisDangerRed,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isGranted) "GRANTED (ON)" else "DISABLED (OFF)",
                                        color = if (isGranted) JarvisSuccessGreen else JarvisDangerRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!isGranted && item.permissionKey.isNotBlank()) {
                                Button(
                                    onClick = {
                                        launcher.launch(arrayOf(item.permissionKey))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = JarvisCyanPrimary,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Grant Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open Settings", fontSize = 12.sp, color = JarvisCyanBright)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun checkPermission(context: Context, permissionKey: String): Boolean {
    if (permissionKey.isBlank()) return true
    return ContextCompat.checkSelfPermission(context, permissionKey) == PackageManager.PERMISSION_GRANTED
}
