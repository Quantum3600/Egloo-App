package com.trishit.egloo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import egloo.shared.generated.resources.*
import org.jetbrains.compose.resources.Font

// ─────────────────────────────────────────────
//  Fonts
// ─────────────────────────────────────────────

@Composable
fun vinaSansFontFamily() = FontFamily(
    Font(Res.font.VinaSans_Regular, FontWeight.Normal)
)

@Composable
fun parkinsansFontFamily() = FontFamily(
    Font(Res.font.Parkinsans_VariableFont_wght, FontWeight.Normal)
)

// ─────────────────────────────────────────────
//  Typography
// ─────────────────────────────────────────────

@Composable
fun eglooTypography(): Typography {
    val vinaSans = vinaSansFontFamily()
    val parkinsans = parkinsansFontFamily()

    return Typography(
        displayLarge = TextStyle(
            fontFamily = vinaSans,
            fontWeight = FontWeight.W500,
            fontSize = 52.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = vinaSans,
            fontWeight = FontWeight.W500,
            fontSize = 38.sp,
            lineHeight = 46.sp,
            letterSpacing = (-0.5).sp
        ),
        displaySmall = TextStyle(
            fontFamily = vinaSans,
            fontWeight = FontWeight.W500,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = vinaSans,
            fontWeight = FontWeight.W500,
            fontSize = 32.sp,
            lineHeight = 40.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = vinaSans,
            fontWeight = FontWeight.W500,
            fontSize = 28.sp,
            lineHeight = 34.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = vinaSans,
            fontWeight = FontWeight.W500,
            fontSize = 24.sp,
            lineHeight = 30.sp
        ),
        titleLarge = TextStyle(
            fontFamily = vinaSans,
            fontWeight = FontWeight.W500,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = vinaSans,
            fontWeight = FontWeight.W500,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        titleSmall = TextStyle(
            fontFamily = vinaSans,
            fontWeight = FontWeight.W500,
            fontSize = 18.sp,
            lineHeight = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = parkinsans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = parkinsans,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = parkinsans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = parkinsans,
            fontWeight = FontWeight.W500,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelMedium = TextStyle(
            fontFamily = parkinsans,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = parkinsans,
            fontWeight = FontWeight.W500,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.06.sp
        )
    )
}

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
        typography  = eglooTypography(),
        content     = content
    )
}
