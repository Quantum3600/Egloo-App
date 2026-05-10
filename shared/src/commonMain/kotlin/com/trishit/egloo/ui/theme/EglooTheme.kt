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
    primary          = EglooColors.BeakAmberDark,
    onPrimary        = Color.Black,
    primaryContainer = EglooColors.MidnightGlass.copy(alpha = 0.6f),
    secondary        = EglooColors.IceBlue,
    onSecondary      = Color.Black,
    background       = EglooColors.MidnightVoid,
    surface          = EglooColors.MidnightSurface,
    surfaceVariant   = EglooColors.MidnightGlass,
    onBackground     = EglooColors.SilverCrisp,
    onSurface        = EglooColors.SilverCrisp,
    onSurfaceVariant = EglooColors.MutedFrost,
    error            = EglooColors.Error,
    tertiary         = EglooColors.TealPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary          = EglooColors.BeakAmber,
    onPrimary        = Color.White,
    primaryContainer = EglooColors.SnowGlass.copy(alpha = 0.7f),
    secondary        = EglooColors.MidnightBlue,
    onSecondary      = Color.White,
    background       = EglooColors.SnowDay,
    surface          = EglooColors.SnowPure,
    surfaceVariant   = EglooColors.SnowGlass,
    onBackground     = EglooColors.NavyPrimary,
    onSurface        = EglooColors.NavyPrimary,
    onSurfaceVariant = EglooColors.FrostyGray,
    error            = EglooColors.Error,
    tertiary         = EglooColors.TealDark,
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
