package com.vardiya.notlar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.vardiya.notlar.domain.NoteColor

// Amber brand scheme (seed #7C5800): warm paper tones for a notes app.
private val LightColors = lightColorScheme(
    primary = Color(0xFF7C5800),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEA6),
    onPrimaryContainer = Color(0xFF271900),
    secondary = Color(0xFF6C5C3F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5E0BB),
    onSecondaryContainer = Color(0xFF241A04),
    tertiary = Color(0xFF4A6545),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCEBC4),
    onTertiaryContainer = Color(0xFF072107),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F2),
    onBackground = Color(0xFF1F1B13),
    surface = Color(0xFFFFF8F2),
    onSurface = Color(0xFF1F1B13),
    surfaceVariant = Color(0xFFEDE1CF),
    onSurfaceVariant = Color(0xFF4D4639),
    outline = Color(0xFF7F7667),
    outlineVariant = Color(0xFFD0C5B4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF2E8),
    surfaceContainer = Color(0xFFF4ECE2),
    surfaceContainerHigh = Color(0xFFEEE7DD),
    surfaceContainerHighest = Color(0xFFE9E1D7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF7BD48),
    onPrimary = Color(0xFF412D00),
    primaryContainer = Color(0xFF5E4200),
    onPrimaryContainer = Color(0xFFFFDEA6),
    secondary = Color(0xFFD8C4A0),
    onSecondary = Color(0xFF3B2F15),
    secondaryContainer = Color(0xFF52452A),
    onSecondaryContainer = Color(0xFFF5E0BB),
    tertiary = Color(0xFFB0CFA9),
    onTertiary = Color(0xFF1D361A),
    tertiaryContainer = Color(0xFF334D2F),
    onTertiaryContainer = Color(0xFFCCEBC4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF17130B),
    onBackground = Color(0xFFEBE1D4),
    surface = Color(0xFF17130B),
    onSurface = Color(0xFFEBE1D4),
    surfaceVariant = Color(0xFF4D4639),
    onSurfaceVariant = Color(0xFFD0C5B4),
    outline = Color(0xFF999080),
    outlineVariant = Color(0xFF4D4639),
    surfaceContainerLowest = Color(0xFF110E07),
    surfaceContainerLow = Color(0xFF1F1B13),
    surfaceContainer = Color(0xFF231F17),
    surfaceContainerHigh = Color(0xFF2E2921),
    surfaceContainerHighest = Color(0xFF39342B),
)

/** Wallpaper colors (Material You) exist from Android 12. */
val DynamicColorAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private val LocalNotePalette = staticCompositionLocalOf { LightColors }

/**
 * Scheme the note colors are taken from. With dynamic colors the app chrome follows the wallpaper,
 * but Sarı, Kum, Yeşil and Kırmızı keep their amber-scheme tones so a note keeps the color its name says.
 */
val notePalette: ColorScheme
    @Composable @ReadOnlyComposable
    get() = LocalNotePalette.current

@Composable
fun NotlarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val brand = if (darkTheme) DarkColors else LightColors
    val scheme = if (dynamicColor && DynamicColorAvailable) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        brand
    }
    val palette = if (scheme === brand) {
        brand
    } else {
        // The default note color is the plain surface, which should follow the wallpaper too.
        brand.copy(
            surfaceContainerLow = scheme.surfaceContainerLow,
            onSurface = scheme.onSurface,
            outline = scheme.outline,
            outlineVariant = scheme.outlineVariant,
        )
    }
    CompositionLocalProvider(LocalNotePalette provides palette) {
        // Expressive theme: spring-based expressive motion scheme and the emphasized type styles.
        MaterialExpressiveTheme(colorScheme = scheme, content = content)
    }
}

/** Card background for a note color; follows light and dark through the scheme roles. */
fun ColorScheme.container(color: NoteColor): Color = when (color) {
    NoteColor.DEFAULT -> surfaceContainerLow
    NoteColor.YELLOW -> primaryContainer
    NoteColor.SAND -> secondaryContainer
    NoteColor.GREEN -> tertiaryContainer
    NoteColor.RED -> errorContainer
}

/** Text color on [container]. */
fun ColorScheme.onContainer(color: NoteColor): Color = when (color) {
    NoteColor.DEFAULT -> onSurface
    NoteColor.YELLOW -> onPrimaryContainer
    NoteColor.SAND -> onSecondaryContainer
    NoteColor.GREEN -> onTertiaryContainer
    NoteColor.RED -> onErrorContainer
}

val NoteColor.label: String
    get() = when (this) {
        NoteColor.DEFAULT -> "Varsayılan"
        NoteColor.YELLOW -> "Sarı"
        NoteColor.SAND -> "Kum"
        NoteColor.GREEN -> "Yeşil"
        NoteColor.RED -> "Kırmızı"
    }
