package com.example.ui.theme

import com.example.core.designsystem.XColors

// Legacy color names now alias the semantic design tokens so the whole app
// adopts the new palette from a single source of truth.
val ObsidianBackground = XColors.Background
val SurfaceCard = XColors.Surface
val SurfaceCardElevated = XColors.SurfaceVariant
val NeutralMutedText = XColors.TextSecondary
val SoftWhiteText = XColors.TextPrimary

// High Contrast Modern Accent Highlights
val IncomeNeonGreen = XColors.Income
val ExpenseNeonCoral = XColors.Spending
val PremiumAccentGold = XColors.AccentGold
val IndigoSpark = XColors.Indigo

// Legacy scheme references to stay compatible with themes
val Purple80 = IndigoSpark
val PurpleGrey80 = NeutralMutedText
val Pink80 = ExpenseNeonCoral

val Purple40 = IndigoSpark
val PurpleGrey40 = NeutralMutedText
val Pink40 = ExpenseNeonCoral
