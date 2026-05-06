package com.trishit.egloo.ui.theme

import androidx.compose.ui.graphics.Color

// ── Egloo brand palette ──────────────────────────────────────────────────────

object EglooColors {

    // Primary — Pingo Teal
    val TealPrimary     = Color(0xFF1D9E75)
    val TealDark        = Color(0xFF0F6E56)
    val TealDarker      = Color(0xFF085041)
    val TealLight       = Color(0xFF5DCAA5)
    val TealLighter     = Color(0xFF9FE1CB)
    val TealSurface     = Color(0xFFE1F5EE)

    // Secondary — Arctic Blue
    val BlueAccent      = Color(0xFF378ADD)
    val BlueDark        = Color(0xFF185FA5)
    val BlueDarker      = Color(0xFF0C447C)
    val BlueLight       = Color(0xFF85B7EB)
    val BlueLighter     = Color(0xFFB5D4F4)
    val BlueSurface     = Color(0xFFE6F1FB)

    // Night background
    val NightDeep       = Color(0xFF0E1A2E)
    val NightMid        = Color(0xFF162336)
    val NightSurface    = Color(0xFF1E2F42)
    val NightCard       = Color(0xFF243548)

    // Snow (light theme surfaces)
    val SnowWhite       = Color(0xFFDAF0FA)
    val SnowPure        = Color(0xFFF0F8FF)
    val SnowLight       = Color(0xFFCCE8F4)
    val SnowMid         = Color(0xFFAAD4E8)

    // Beak Amber (CTA / accent)
    val BeakAmber       = Color(0xFFF5A623)
    val BeakDark        = Color(0xFFBA7517)
    val BeakSurface     = Color(0xFFFAEEDA)

    // Semantic
    val Success         = TealPrimary
    val Warning         = BeakAmber
    val Error           = Color(0xFFE24B4A)
    val Info            = BlueAccent

    // Source badge colors
    val GmailRed        = Color(0xFFEA4335)
    val SlackPurple     = Color(0xFF611f69)
    val DriveBlue       = Color(0xFF1967D2)
    val NotionGray      = Color(0xFF37352F)
    val PdfOrange       = Color(0xFFFF5722)
}
