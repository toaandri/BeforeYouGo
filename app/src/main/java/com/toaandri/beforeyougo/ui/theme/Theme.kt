package com.toaandri.beforeyougo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.toaandri.beforeyougo.ThemePreference

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF0AA90), onPrimary = Color(0xFF482216),
    primaryContainer = Color(0xFF573329), onPrimaryContainer = Color(0xFFFFD9C9),
    secondary = Color(0xFFB7CDB5), onSecondary = Color(0xFF263C2B),
    secondaryContainer = Color(0xFF334736), onSecondaryContainer = Color(0xFFD8E8D4),
    tertiary = Amber, onTertiary = Ink,
    background = Night, onBackground = Color(0xFFF5EBDD),
    surface = Color(0xFF2D2620), onSurface = Color(0xFFF5EBDD),
    surfaceVariant = Color(0xFF3D342C), onSurfaceVariant = Color(0xFFD1C2B2),
    outline = Color(0xFFA79889), outlineVariant = Color(0xFF51463A),
    error = Color(0xFFFFB4AB)
)
private val LightColorScheme = lightColorScheme(
    primary = Terracotta, onPrimary = Color.White,
    primaryContainer = Color(0xFFF6DDCF), onPrimaryContainer = Color(0xFF6C3021),
    secondary = Sage, onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E8D9), onSecondaryContainer = Color(0xFF304A35),
    tertiary = Color(0xFF79582B), onTertiary = Color.White,
    background = Sand, onBackground = Ink,
    surface = Paper, onSurface = Ink,
    surfaceVariant = Color(0xFFECE2D3), onSurfaceVariant = Color(0xFF6A5D50),
    outline = Color(0xFF857666), outlineVariant = Color(0xFFD9CCBB),
    error = Color(0xFFB3261E)
)
@Composable
fun BeforeYouGoTheme(preference: ThemePreference = ThemePreference.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColorScheme else LightColorScheme, typography = Typography, content = content)
}
