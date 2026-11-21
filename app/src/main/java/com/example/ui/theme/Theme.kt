package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Esquema de cores escuro exclusivo para manter a identidade visual azul-marinho do console
private val Ps5DarkColorScheme =
  darkColorScheme(
    primary = PsCyanGlow,
    onPrimary = PsNavyVoid,
    primaryContainer = PsNavySurfaceElevated,
    onPrimaryContainer = PsIceWhite,
    secondary = PsCobaltAccent,
    onSecondary = PsIceWhite,
    secondaryContainer = PsNavySurface,
    onSecondaryContainer = PsSlateText,
    tertiary = PsSuccessGreen,
    onTertiary = PsNavyVoid,
    background = PsNavyVoid,
    onBackground = PsIceWhite,
    surface = PsNavyDeep,
    onSurface = PsIceWhite,
    surfaceVariant = PsNavySurface,
    onSurfaceVariant = PsSlateText,
    outline = PsMutedText,
    outlineVariant = PsNavySurfaceElevated,
    error = PsDangerRed,
    onError = PsIceWhite,
  )

private val GameHubShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

/** Tema Material 3 customizado do Game Hub com estética PlayStation 5. */
@Composable
fun GameHubTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = Ps5DarkColorScheme,
    typography = GameHubTypography,
    shapes = GameHubShapes,
    content = content,
  )
}
