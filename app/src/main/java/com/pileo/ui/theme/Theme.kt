package com.pileo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette Pileo : positive, calme et claire — cyan dominant.
// Le cyan est réservé aux boutons de confirmation / enregistrement.
private val CyanPrimary = Color(0xFF00ACC1)
private val CyanOnPrimary = Color(0xFFFFFFFF)
private val CyanContainer = Color(0xFFC5EEF4)
private val CyanOnContainer = Color(0xFF083E45)
private val TealSecondary = Color(0xFF4FA3A0)
private val TealContainer = Color(0xFFD2ECEB)
private val TealOnContainer = Color(0xFF0B3534)
private val CyanTertiary = Color(0xFF00ACC1)
private val CyanTertiaryContainer = Color(0xFFC5EEF4)
private val CyanOnTertiaryContainer = Color(0xFF083E45)
private val CalmBackground = Color(0xFFF4FAFB)
private val CalmSurface = Color(0xFFFFFFFF)
private val CalmSurfaceVariant = Color(0xFFDFEFF2)
private val CalmOnSurfaceVariant = Color(0xFF45666B)
private val SoftError = Color(0xFFC25E5E)

private val LightColors = lightColorScheme(
    primary = CyanPrimary,
    onPrimary = CyanOnPrimary,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanOnContainer,
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = TealOnContainer,
    tertiary = CyanTertiary,
    onTertiary = Color.White,
    tertiaryContainer = CyanTertiaryContainer,
    onTertiaryContainer = CyanOnTertiaryContainer,
    background = CalmBackground,
    onBackground = Color(0xFF16241C),
    surface = CalmSurface,
    onSurface = Color(0xFF16241C),
    surfaceVariant = CalmSurfaceVariant,
    onSurfaceVariant = CalmOnSurfaceVariant,
    error = SoftError
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FDCE9),
    onPrimary = Color(0xFF083E45),
    primaryContainer = Color(0xFF0E5A63),
    onPrimaryContainer = Color(0xFFC5EEF4),
    secondary = Color(0xFF93CECC),
    secondaryContainer = Color(0xFF1E4A49),
    tertiary = Color(0xFF6FDCE9),
    tertiaryContainer = Color(0xFF0E5A63),
    background = Color(0xFF0F1619),
    surface = Color(0xFF0F1619),
    error = Color(0xFFE08A8A)
)

@Composable
fun PileoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
