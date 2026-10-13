package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF80F2FF),
    secondary = CyberPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3700B3),
    onSecondaryContainer = Color(0xFFD1C4E9),
    tertiary = NeonGreen,
    onTertiary = Color.Black,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder,
    error = DangerRed,
    onError = Color.White
)

private val LightColorScheme = darkColorScheme(
    primary = CyberCyanDim,
    onPrimary = Color.White,
    secondary = CyberPurple,
    tertiary = NeonGreen,
    background = DarkBg,
    surface = DarkSurface
)

@Composable
fun KeyMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
