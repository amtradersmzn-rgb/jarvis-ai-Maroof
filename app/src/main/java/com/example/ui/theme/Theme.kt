package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = JarvisSurfaceVariant,
    onPrimaryContainer = JarvisCyanBright,
    secondary = JarvisElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = JarvisBlueDark,
    onSecondaryContainer = JarvisCyanBright,
    tertiary = JarvisAmberCore,
    onTertiary = Color.Black,
    background = JarvisBackground,
    onBackground = TextPrimary,
    surface = JarvisSurface,
    onSurface = TextPrimary,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = JarvisBorderGlow,
    error = JarvisDangerRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // JARVIS is inherently a dark futuristic interface
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = Typography,
        content = content
    )
}
