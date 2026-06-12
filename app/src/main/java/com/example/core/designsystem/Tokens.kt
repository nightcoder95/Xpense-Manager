package com.example.core.designsystem

import androidx.compose.ui.graphics.Color

/** Single source of truth for the app palette (pixel-matched to reference screenshots). */
object XColors {
    val Background = Color(0xFF0B0D10)
    val Surface = Color(0xFF16191F)
    val SurfaceVariant = Color(0xFF1E222A)
    val Outline = Color(0xFF262B33)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9AA1AC)
    val Spending = Color(0xFFF2706B)
    val Income = Color(0xFF43C59E)
    val AccentGold = Color(0xFFE3B341)
    val Indigo = Color(0xFF6C5DD3)
    val OnAccent = Color(0xFF0B0D10)
}

/** Parses a stored hex color string, falling back to Indigo on malformed input. */
fun String.toComposeColor(): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(XColors.Indigo)
