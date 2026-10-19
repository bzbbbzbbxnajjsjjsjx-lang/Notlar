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

// Deep indigo with a lilac primary, light-blue and lime accents and a coral stop colour: the palette of a status-first
// app (Tor VPN's), so a state is read from colour first. Lime means working, blue means waiting, coral means stop or
// fix. Dynamic color is off on purpose.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFD2A8F7),
    onPrimary = Color(0xFF3A0D66),
    primaryContainer = Color(0xFF5A2D9A),
    onPrimaryContainer = Color(0xFFF0DCFF),
    secondary = Color(0xFFA9CBFF),
    onSecondary = Color(0xFF0A2750),
    secondaryContainer = Color(0xFF34408F),
    onSecondaryContainer = Color(0xFFDCE6FF),
    tertiary = Color(0xFFC9EB6E),
    onTertiary = Color(0xFF243300),
    tertiaryContainer = Color(0xFF3A4D00),
    onTertiaryContainer = Color(0xFFDDF889),
    error = Color(0xFFF2877D),
    onError = Color(0xFF4A0A05),
    errorContainer = Color(0xFFE8857A),
    onErrorContainer = Color(0xFF5C1410),
    background = Color(0xFF1A1455),
    onBackground = Color(0xFFEDE7FF),
    surface = Color(0xFF1A1455),
    onSurface = Color(0xFFEDE7FF),
    surfaceVariant = Color(0xFF2F2780),
    onSurfaceVariant = Color(0xFFC9C2F5),
    outline = Color(0xFF8F86D6),
    outlineVariant = Color(0xFF3D3490),
    surfaceContainerLowest = Color(0xFF120D40),
    surfaceContainerLow = Color(0xFF1E1760),
    surfaceContainer = Color(0xFF241C6B),
    surfaceContainerHigh = Color(0xFF2B2377),
    surfaceContainerHighest = Color(0xFF342B84),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6C3CB0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEBDCFF),
    onPrimaryContainer = Color(0xFF2A0A5C),
    secondary = Color(0xFF3A5BA0),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE6FF),
    onSecondaryContainer = Color(0xFF0A2750),
    tertiary = Color(0xFF4C6700),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCBEA73),
    onTertiaryContainer = Color(0xFF161F00),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD5),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1C1B21),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1C1B21),
    surfaceVariant = Color(0xFFE6E0F3),
    onSurfaceVariant = Color(0xFF48454F),
    outline = Color(0xFF79747F),
    outlineVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F1FF),
    surfaceContainer = Color(0xFFF1EBFB),
    surfaceContainerHigh = Color(0xFFEBE5F5),
    surfaceContainerHighest = Color(0xFFE5DFEF),
)

@Composable
fun SludgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Expressive theme: spring-based motion scheme, emphasized type styles and rounder corners than baseline.
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = Shapes(
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(28.dp),
            large = RoundedCornerShape(36.dp),
            extraLarge = RoundedCornerShape(48.dp),
        ),
        content = content,
    )
}
