package com.vardiya.notlar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.vardiya.notlar.R

/**
 * One look of Google Sans Flex, the Material 3 Expressive brand face: how wide (wdth), how rounded
 * (ROND) and for what size (opsz) the letters are drawn.
 */
private class Face(val width: Float, val round: Float, val opticalSize: Float)

// The plain face for reading, and the wide, fully rounded one Expressive uses for big titles.
private val Text = Face(width = 100f, round = 0f, opticalSize = 18f)
private val Display = Face(width = 118f, round = 100f, opticalSize = 48f)
private val Title = Face(width = 108f, round = 100f, opticalSize = 24f)

// Weights the type scale uses; the emphasized roles step up to bold and black.
private val Weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)

/**
 * Google Sans Flex as one variable font (Latin subset, OFL; the axes limited to what the app
 * uses). Each weight of a [face] is an instance of the same file.
 */
private fun family(face: Face): FontFamily = FontFamily(
    Weights.map { weight ->
        Font(
            R.font.google_sans_flex,
            weight = weight,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight.weight),
                FontVariation.width(face.width),
                FontVariation.Setting("ROND", face.round),
                FontVariation.Setting("opsz", face.opticalSize),
            ),
        )
    },
)

private val TextFamily = family(Text)
private val DisplayFamily = family(Display)
private val TitleFamily = family(Title)

private fun TextStyle.display() = copy(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold)
private fun TextStyle.headline() = copy(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold)
private fun TextStyle.title() = copy(fontFamily = TitleFamily, fontWeight = FontWeight.Bold)

/**
 * The Expressive type scale in Google Sans Flex. Reading text stays plain; the emphasized display
 * and headline roles are wide, rounded and heavy, the emphasized titles rounded and bold, so the
 * big titles of the app carry the Expressive character.
 */
internal val NotlarTypography: Typography = Typography(fontFamily = TextFamily).let { base ->
    base.copy(
        displayLargeEmphasized = base.displayLargeEmphasized.display(),
        displayMediumEmphasized = base.displayMediumEmphasized.display(),
        displaySmallEmphasized = base.displaySmallEmphasized.display(),
        headlineLargeEmphasized = base.headlineLargeEmphasized.headline(),
        headlineMediumEmphasized = base.headlineMediumEmphasized.headline(),
        headlineSmallEmphasized = base.headlineSmallEmphasized.headline(),
        titleLargeEmphasized = base.titleLargeEmphasized.title(),
        titleMediumEmphasized = base.titleMediumEmphasized.title(),
        titleSmallEmphasized = base.titleSmallEmphasized.title(),
    )
}
