package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPurple

@Composable
fun RadarAnimation(
    modifier: Modifier = Modifier,
    isScanning: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val wave1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    val wave2 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    val wave3 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )

    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing)
        ),
        label = "radar_sweep"
    )

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2

            // Base concentric rings
            for (i in 1..4) {
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.12f),
                    radius = maxRadius * (i / 4f),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Crosshair lines
            drawLine(
                color = CyberCyan.copy(alpha = 0.18f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = CyberCyan.copy(alpha = 0.18f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )

            if (isScanning) {
                // Expanding ripple waves
                listOf(wave1, wave2, wave3).forEach { wave ->
                    val alpha = (1.0f - wave).coerceIn(0f, 1f) * 0.45f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberCyan.copy(alpha = alpha), Color.Transparent),
                            center = center,
                            radius = maxRadius * wave
                        ),
                        radius = maxRadius * wave,
                        center = center
                    )
                    drawCircle(
                        color = CyberCyan.copy(alpha = alpha * 0.8f),
                        radius = maxRadius * wave,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Rotating radar sweep line
                val rad = Math.toRadians(rotationAngle.toDouble())
                val endX = center.x + (maxRadius * Math.cos(rad)).toFloat()
                val endY = center.y + (maxRadius * Math.sin(rad)).toFloat()
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(CyberPurple, CyberCyan),
                        start = center,
                        end = Offset(endX, endY)
                    ),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }

        // Center NFC Icon
        Icon(
            imageVector = Icons.Default.Nfc,
            contentDescription = "NFC Scanner",
            tint = CyberCyan,
            modifier = Modifier.size(44.dp)
        )
    }
}
