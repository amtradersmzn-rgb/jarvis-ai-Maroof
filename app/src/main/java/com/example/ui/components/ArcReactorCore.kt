package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.engine.AiCoreMode
import com.example.ui.theme.JarvisAmberCore
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisDangerRed
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisSuccessGreen
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorCore(
    mode: AiCoreMode,
    audioRmsDb: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorRotation")

    // Continuous smooth rotation for outer ring
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRingRotation"
    )

    // Counter-rotation for inner ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (mode == AiCoreMode.THINKING) 3000 else 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRingRotation"
    )

    // Breathing pulse for core
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (mode) {
                    AiCoreMode.LISTENING -> 800
                    AiCoreMode.SPEAKING -> 600
                    AiCoreMode.THINKING -> 500
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Smooth audio reactivity animation
    val audioScale = remember { Animatable(1f) }
    LaunchedEffect(audioRmsDb, mode) {
        val target = if (mode == AiCoreMode.LISTENING) {
            1f + (audioRmsDb.coerceIn(0f, 10f) / 10f) * 0.25f
        } else {
            1f
        }
        audioScale.animateTo(target, tween(60))
    }

    val primaryColor = when (mode) {
        AiCoreMode.IDLE -> JarvisCyanPrimary
        AiCoreMode.LISTENING -> JarvisCyanBright
        AiCoreMode.THINKING -> JarvisAmberCore
        AiCoreMode.SPEAKING -> JarvisElectricBlue
        AiCoreMode.EXECUTING -> JarvisSuccessGreen
        AiCoreMode.CONFIRMING -> JarvisAmberCore
        AiCoreMode.ERROR -> JarvisDangerRed
    }

    val secondaryColor = when (mode) {
        AiCoreMode.IDLE -> JarvisElectricBlue
        AiCoreMode.LISTENING -> JarvisCyanPrimary
        AiCoreMode.THINKING -> JarvisCyanBright
        AiCoreMode.SPEAKING -> JarvisCyanPrimary
        AiCoreMode.EXECUTING -> JarvisCyanBright
        AiCoreMode.CONFIRMING -> JarvisCyanPrimary
        AiCoreMode.ERROR -> JarvisDangerRed.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = primaryColor),
                onClick = onClick
            )
            .testTag("jarvis_arc_reactor_core"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.width / 2f
            val dynamicCoreRadius = radius * 0.38f * pulseScale * audioScale.value

            // 1. Soft radial background aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (mode == AiCoreMode.LISTENING) 0.35f else 0.18f),
                        primaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.05f
                ),
                center = center,
                radius = radius
            )

            // 2. Outer segmented arc reactor ring
            val outerArcCount = 12
            val segmentSweep = 22f
            val gapSweep = (360f / outerArcCount) - segmentSweep

            for (i in 0 until outerArcCount) {
                val startAngle = outerRotation + i * (segmentSweep + gapSweep)
                drawArc(
                    color = primaryColor.copy(alpha = 0.75f),
                    startAngle = startAngle,
                    sweepAngle = segmentSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 0.92f, center.y - radius * 0.92f),
                    size = Size(radius * 1.84f, radius * 1.84f),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Middle calibration ring with hash marks
            val hashCount = 36
            val hashInnerRadius = radius * 0.70f
            val hashOuterRadius = radius * 0.78f
            for (i in 0 until hashCount) {
                val angleRad = Math.toRadians((innerRotation + i * (360.0 / hashCount)))
                val isMajor = i % 3 == 0
                val lengthFactor = if (isMajor) 1.0f else 0.6f
                val startX = (center.x + hashInnerRadius * cos(angleRad)).toFloat()
                val startY = (center.y + hashInnerRadius * sin(angleRad)).toFloat()
                val endX = (center.x + (hashInnerRadius + (hashOuterRadius - hashInnerRadius) * lengthFactor) * cos(angleRad)).toFloat()
                val endY = (center.y + (hashInnerRadius + (hashOuterRadius - hashInnerRadius) * lengthFactor) * sin(angleRad)).toFloat()

                drawLine(
                    color = if (isMajor) primaryColor else secondaryColor.copy(alpha = 0.45f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // 4. Inner rotating triangular power node ring
            drawArc(
                color = secondaryColor.copy(alpha = 0.8f),
                startAngle = -innerRotation,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.55f, center.y - radius * 0.55f),
                size = Size(radius * 1.10f, radius * 1.10f),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 5. Central Reactor Core Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        secondaryColor.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicCoreRadius * 1.2f
                ),
                center = center,
                radius = dynamicCoreRadius
            )

            // 6. Central Arc Reactor Emblem Ring
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = dynamicCoreRadius * 0.55f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            drawCircle(
                color = primaryColor,
                radius = dynamicCoreRadius * 0.28f,
                center = center
            )
        }
    }
}
