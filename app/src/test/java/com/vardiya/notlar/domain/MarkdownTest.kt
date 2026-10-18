package com.vardiya.notlar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTest {

    @Test
    fun boldWrapsTheSelectionAndUnwrapsItAgain() {
        val wrapped = Markdown.bold(TextEdit("merhaba dünya", 8, 13))
        assertEquals(TextEdit("merhaba **dünya**", 10, 15), wrapped)
        assertTrue(Markdown.isBold(wrapped))
        assertEquals(TextEdit("merhaba dünya", 8, 13), Markdown.bold(wrapped))
    }

    @Test
    fun withNothingSelectedTheCursorLandsBetweenTheMarks() {
        assertEquals(TextEdit("ab****cd", 4, 4), Markdown.bold(TextEdit("abcd", 2, 2)))
        val italic = Markdown.italic(TextEdit("", 0, 0))
        assertEquals(TextEdit("**", 1, 1), italic)
    }

    @Test
    fun headingAndBulletToggleOnTheLine() {
        val heading = Markdown.heading(TextEdit("bir\niki", 5, 5))
        assertEquals("bir\n# iki", heading.text)
        assertTrue(Markdown.isHeading(heading))
        assertEquals("bir\niki", Markdown.heading(heading).text)
        // A bullet replaces a heading rather than stacking on it.
        assertEquals("bir\n- iki", Markdown.bullet(heading).text)
    }

    @Test
    fun bulletsApplyToEveryLineOfTheSelection() {
        val bullets = Markdown.bullet(TextEdit("a\nb\nc", 0, 5))
        assertEquals("- a\n- b\n- c", bullets.text)
        assertTrue(Markdown.isBullet(bullets))
        assertEquals("a\nb\nc", Markdown.bullet(bullets.copy(start = 0, end = bullets.text.length)).text)
    }

    @Test
    fun stripRemovesMarksButKeepsBullets() {
        assertEquals("Başlık\n• madde\nkalın ve italik", Markdown.strip("# Başlık\n- madde\n**kalın** ve *italik*"))
        assertFalse(Markdown.strip("2 * 3").isEmpty())
    }

    @Test
    fun italicInsideBoldAddsAStarInsteadOfBreakingTheBold() {
        val both = Markdown.italic(TextEdit("**kalın**", 2, 7))
        assertEquals(TextEdit("***kalın***", 3, 8), both)
        assertTrue(Markdown.isBold(both) && Markdown.isItalic(both))
        assertEquals(TextEdit("**kalın**", 2, 7), Markdown.italic(both))
    }
}
