package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
  primary = CobaltPrimary,
  onPrimary = PureWhite,
  primaryContainer = IceContainer,
  onPrimaryContainer = DeepSapphire,
  secondary = SkyAccent,
  onSecondary = PureWhite,
  secondaryContainer = IceSurfaceVariant,
  onSecondaryContainer = CobaltPrimary,
  tertiary = RoyalBlue,
  onTertiary = PureWhite,
  background = IceBackground,
  onBackground = DeepNavyInk,
  surface = PureWhite,
  onSurface = DeepNavyInk,
  surfaceVariant = IceSurfaceVariant,
  onSurfaceVariant = SlateInk,
  outline = OutlineBlue,
  outlineVariant = IceContainer,
  error = CrimsonOut,
  onError = PureWhite,
  errorContainer = CrimsonSoft,
  onErrorContainer = CrimsonOut,
)

private val DarkColorScheme = darkColorScheme(
  primary = DarkPrimary,
  onPrimary = DarkSurface,
  primaryContainer = DeepSapphire,
  onPrimaryContainer = IceContainer,
  secondary = DarkSecondary,
  onSecondary = DarkSurface,
  secondaryContainer = DarkSurfaceVariant,
  onSecondaryContainer = CyanGlow,
  background = DarkSurface,
  onBackground = PureWhite,
  surface = DarkSurfaceVariant,
  onSurface = PureWhite,
  surfaceVariant = ColorTokens.DarkCard,
  onSurfaceVariant = IceContainer,
  outline = CobaltPrimary,
  error = CrimsonOut,
  onError = PureWhite,
)

private object ColorTokens {
  val DarkCard = androidx.compose.ui.graphics.Color(0xFF153156)
}

val BoutikShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun BoutikProTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = BoutikShapes,
    content = content,
  )
}
