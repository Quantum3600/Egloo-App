package com.trishit.egloo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
//  Typography
// ─────────────────────────────────────────────

val EglooTypography = Typography(
    displaySmall  = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.W500, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.W500),
    headlineMedium= TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W500),
    titleLarge    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W500),
    titleMedium   = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W500),
    bodyLarge     = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelSmall    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W500, letterSpacing = 0.06.sp),
)

// ─────────────────────────────────────────────
//  Theme colors
// ─────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary          = EglooColors.TealPrimary,
    onPrimary        = Color.White,
    primaryContainer = EglooColors.TealDark,
    secondary        = EglooColors.BlueAccent,
    onSecondary      = Color.White,
    background       = EglooColors.NightDeep,
    surface          = EglooColors.NightMid,
    onBackground     = Color.White,
    onSurface        = Color.White,
    onSurfaceVariant = EglooColors.SnowWhite,
    error            = EglooColors.Error,
    tertiary         = EglooColors.BeakAmber,
)

private val LightColorScheme = lightColorScheme(
    primary          = EglooColors.TealDark,
    onPrimary        = Color.White,
    primaryContainer = EglooColors.TealLighter,
    secondary        = EglooColors.BlueDark,
    onSecondary      = Color.White,
    background       = EglooColors.SnowPure,
    surface          = Color.White,
    onBackground     = EglooColors.NightDeep,
    onSurface        = EglooColors.NightDeep,
    onSurfaceVariant = Color(0xFF44474E),
    error            = EglooColors.Error,
    tertiary         = EglooColors.BeakDark,
)

// ─────────────────────────────────────────────
//  Theme composable
// ─────────────────────────────────────────────

@Composable
fun EglooTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = EglooTypography,
        content     = content
    )
}
