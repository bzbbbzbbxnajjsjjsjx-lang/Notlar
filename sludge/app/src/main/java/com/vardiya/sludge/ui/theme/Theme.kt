package com.vardiya.sludge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Burnt-orange fuzz pedal on olive and violet: a desert-rock palette (seed #9A4521), with a dark scheme tuned
// for a dim room.
private val LightColors = lightColorScheme(
    primary = Color(0xFF9A4521),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF360F00),
    secondary = Color(0xFF626043),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E5BE),
    onSecondaryContainer = Color(0xFF1C1D00),
    tertiary = Color(0xFF6A5A8E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEBDDFF),
    onTertiaryContainer = Color(0xFF231047),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A15),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF5DED4),
    onSurfaceVariant = Color(0xFF53443C),
    outline = Color(0xFF85736B),
    outlineVariant = Color(0xFFD8C2B8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EA),
    surfaceContainer = Color(0xFFFCEAE1),
    surfaceContainerHigh = Color(0xFFF6E4DB),
    surfaceContainerHighest = Color(0xFFF0DED5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB693),
    onPrimary = Color(0xFF561F00),
    primaryContainer = Color(0xFF7A3305),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFFCCC9A0),
    onSecondary = Color(0xFF323216),
    secondaryContainer = Color(0xFF49492B),
    onSecondaryContainer = Color(0xFFE8E5BE),
    tertiary = Color(0xFFD2BCFA),
    onTertiary = Color(0xFF38275C),
    tertiaryContainer = Color(0xFF504074),
    onTertiaryContainer = Color(0xFFEBDDFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A120D),
    onBackground = Color(0xFFF0DFD7),
    surface = Color(0xFF1A120D),
    onSurface = Color(0xFFF0DFD7),
    surfaceVariant = Color(0xFF53443C),
    onSurfaceVariant = Color(0xFFD8C2B8),
    outline = Color(0xFFA08D84),
    outlineVariant = Color(0xFF53443C),
    surfaceContainerLowest = Color(0xFF140C08),
    surfaceContainerLow = Color(0xFF221A15),
    surfaceContainer = Color(0xFF271E19),
    surfaceContainerHigh = Color(0xFF322823),
    surfaceContainerHighest = Color(0xFF3D332D),
)

@Composable
fun SludgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Expressive theme: spring-based motion scheme and the emphasized type styles.
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        // Expressive corners: cards and sheets are rounder than the baseline 12dp.
        shapes = Shapes(
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(28.dp),
            large = RoundedCornerShape(36.dp),
            extraLarge = RoundedCornerShape(48.dp),
        ),
        content = content,
    )
}
