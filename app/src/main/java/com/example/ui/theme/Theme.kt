package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.example.core.designsystem.XColors

private val DarkColorScheme = darkColorScheme(
    primary = XColors.Indigo,
    onPrimary = XColors.TextPrimary,
    secondary = XColors.TextSecondary,
    onSecondary = XColors.TextPrimary,
    tertiary = XColors.AccentGold,
    onTertiary = XColors.OnAccent,
    background = XColors.Background,
    onBackground = XColors.TextPrimary,
    surface = XColors.Surface,
    onSurface = XColors.TextPrimary,
    surfaceVariant = XColors.SurfaceVariant,
    onSurfaceVariant = XColors.TextSecondary,
    outline = XColors.Outline,
    error = XColors.Spending,
    onError = XColors.OnAccent
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Force DarkColorScheme for permanent premium dark mode styling
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
