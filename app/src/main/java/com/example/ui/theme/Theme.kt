package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val JarvisDarkColorScheme = darkColorScheme(
    primary = ArcCyan,
    onPrimary = AbyssalVoid,
    primaryContainer = CyberSlate,
    onPrimaryContainer = IceBlueText,
    secondary = SapphireCore,
    onSecondary = IceBlueText,
    secondaryContainer = CyberNavy,
    onSecondaryContainer = IceBlueText,
    tertiary = ActiveEmerald,
    onTertiary = AbyssalVoid,
    tertiaryContainer = ActiveEmeraldDark,
    onTertiaryContainer = ActiveEmerald,
    error = EmergencyCrimson,
    onError = IceBlueText,
    errorContainer = EmergencyCrimsonDark,
    onErrorContainer = EmergencyCrimson,
    background = AbyssalVoid,
    onBackground = IceBlueText,
    surface = CyberNavy,
    onSurface = IceBlueText,
    surfaceVariant = CyberSlate,
    onSurfaceVariant = MutedSteelText,
    outline = CyberGlassBorder,
    outlineVariant = ColorTokens.SubtleBorder
)

private object ColorTokens {
    val SubtleBorder = CyberGlassBorder.copy(alpha = 0.45f)
}

val JarvisShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun JarvisVipTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = JarvisTypography,
        shapes = JarvisShapes,
        content = content
    )
}
