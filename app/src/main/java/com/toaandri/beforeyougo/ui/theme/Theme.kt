package com.toaandri.beforeyougo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.toaandri.beforeyougo.ThemePreference

private val DarkColorScheme = darkColorScheme(
    primary = MintNight,
    onPrimary = Night,
    secondary = Amber,
    onSecondary = Night,
    tertiary = Amber,
    primaryContainer = Color(0xFF184C40),
    onPrimaryContainer = MintNight,
    secondaryContainer = Color(0xFF25463F),
    onSecondaryContainer = MintNight,
    background = Night,
    surface = Color(0xFF102A43),
    surfaceVariant = Color(0xFF17384B),
    onBackground = Color(0xFFF4F7F5),
    onSurface = Color(0xFFF4F7F5),
    onSurfaceVariant = Color(0xFFC4D4DB),
    error = Coral
)

private val LightColorScheme = lightColorScheme(
    primary = Jade,
    onPrimary = Color.White,
    secondary = Ink,
    onSecondary = Color.White,
    primaryContainer = JadeLight,
    onPrimaryContainer = Ink,
    secondaryContainer = Color(0xFFD7EBDF),
    onSecondaryContainer = Ink,
    tertiary = Amber,
    background = Sand,
    surface = Paper,
    surfaceVariant = Color(0xFFE6F1EA),
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF4A6259),
    error = Coral
)

@Composable
fun BeforeYouGoTheme(
    preference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    // La palette de la marque reste stable : Android ne la remplace pas par la couleur du fond d'écran.
    val darkTheme = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
