package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisElectricBlue
import kotlin.math.sin

@Composable
fun AudioWaveformVisualizer(
    isActive: Boolean,
    audioRmsDb: Float = 0f,
    modifier: Modifier = Modifier,
    barCount: Int = 24
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val animatedRms = remember { Animatable(0f) }
    LaunchedEffect(audioRmsDb, isActive) {
        val target = if (isActive) audioRmsDb.coerceIn(0.5f, 10f) else 0f
        animatedRms.animateTo(target, tween(80))
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f
        val barWidth = 3.5.dp.toPx()
        val spacing = (canvasWidth - (barCount * barWidth)) / (barCount + 1)

        val brush = Brush.verticalGradient(
            colors = listOf(
                JarvisCyanBright,
                JarvisCyanPrimary,
                JarvisElectricBlue
            )
        )

        for (i in 0 until barCount) {
            val x = spacing + i * (barWidth + spacing)
            val normalizedIdx = (i - barCount / 2f) / (barCount / 2f)
            val bellCurve = (1f - (normalizedIdx * normalizedIdx)).coerceAtLeast(0.15f)

            val wave = (sin(phase + i * 0.45f) + 1f) / 2f
            val baseHeight = if (isActive) (8.dp.toPx() + animatedRms.value * 3.5f * bellCurve * wave) else 4.dp.toPx()
            val halfH = (baseHeight / 2f).coerceAtMost(canvasHeight / 2f)

            drawLine(
                brush = brush,
                start = Offset(x, centerY - halfH),
                end = Offset(x, centerY + halfH),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
