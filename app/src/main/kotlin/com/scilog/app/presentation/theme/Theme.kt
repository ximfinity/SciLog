package com.scilog.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Provides whether the app is currently in dark mode (manual toggle, not always system). */
val LocalAppIsDark = staticCompositionLocalOf { true }

// Must be declared before DarkColorScheme to avoid uninitialized-value ordering bug
private val Color_Dark_Background = Color(0xFF0D1B2A)
private val Color_Dark_Surface = Color(0xFF122436)
private val Color_Dark_OnBackground = Color(0xFFE0F7FA)
private val Color_Dark_OnSurface = Color(0xFFB2EBF2)
private val Color_Dark_SurfaceVariant = Color(0xFF1C3448)
private val Color_Dark_OutlineVariant = Color(0xFF2C4A60)

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Navy20,
    primaryContainer = Teal40,
    onPrimaryContainer = Teal80,
    secondary = Purple80,
    onSecondary = Navy20,
    secondaryContainer = Color(0xFF3B2F6E),
    onSecondaryContainer = Purple80,
    background = Color_Dark_Background,
    surface = Color_Dark_Surface,
    onBackground = Color_Dark_OnBackground,
    onSurface = Color_Dark_OnSurface,
    surfaceVariant = Color_Dark_SurfaceVariant,
    onSurfaceVariant = Color(0xFF8EADC4),
    outline = Color(0xFF4A7090),
    outlineVariant = Color_Dark_OutlineVariant,
    error = ErrorRed,
    errorContainer = Color(0xFF5C0011),
    onErrorContainer = Color(0xFFFFDAD6),
    tertiaryContainer = Color(0xFF4A3800),
    onTertiaryContainer = Color(0xFFFEF3C7)
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Navy20,
    secondary = Purple40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7F6),
    onSecondaryContainer = Color(0xFF21005D),
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF37474F),
    outline = Color(0xFF6B9696),
    outlineVariant = Color(0xFFB2DFDB),
    error = ErrorRed,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    tertiaryContainer = Color(0xFFFFF3CD),
    onTertiaryContainer = Color(0xFF856404)
)

@Composable
fun SciLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SciLogTypography,
        content = content
    )
}
