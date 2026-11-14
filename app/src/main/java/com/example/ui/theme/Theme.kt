package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LinguaBridgeDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = TextPrimary,
    secondary = EmeraldReady,
    onSecondary = OnEmeraldReady,
    secondaryContainer = EmeraldReadyContainer,
    onSecondaryContainer = EmeraldReady,
    tertiary = AmberNotice,
    onTertiary = OnAmberNotice,
    tertiaryContainer = AmberNoticeContainer,
    onTertiaryContainer = AmberNotice,
    error = CoralActive,
    onError = OnCoralActive,
    errorContainer = CoralActiveContainer,
    onErrorContainer = CoralActive,
    background = NavyVoid,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavyCardElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = NavyVoid,
    surfaceContainerLow = NavySurface,
    surfaceContainer = NavyCard,
    surfaceContainerHigh = NavyCardElevated,
    surfaceContainerHighest = NavyCardHighest,
    outline = NavyBorder,
    outlineVariant = NavyCardHighest
)

val LinguaBridgeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LinguaBridgeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LinguaBridgeDarkColorScheme,
        typography = LinguaBridgeTypography,
        shapes = LinguaBridgeShapes,
        content = content
    )
}
