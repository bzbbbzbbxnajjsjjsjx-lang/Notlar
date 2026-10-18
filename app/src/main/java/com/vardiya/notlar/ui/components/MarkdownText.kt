package com.vardiya.notlar.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit

private val inline = Regex("""\*\*(.+?)\*\*|\*(.+?)\*""")

/**
 * [text] with its Markdown marks (**bold**, *italic*, "# " headings, "- " bullets) drawn instead
 * of shown; [headingSize] is the size a heading line is set in.
 */
fun renderMarkdown(text: String, headingSize: TextUnit): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { index, line ->
        if (index > 0) append('\n')
        when {
            line.startsWith("# ") -> withStyle(SpanStyle(fontSize = headingSize, fontWeight = FontWeight.Bold)) {
                appendInline(line.removePrefix("# "))
            }
            line.startsWith("- ") -> {
                append("•  ")
                appendInline(line.removePrefix("- "))
            }
            else -> appendInline(line)
        }
    }
}

private fun AnnotatedString.Builder.appendInline(line: String) {
    var last = 0
    inline.findAll(line).forEach { match ->
        append(line.substring(last, match.range.first))
        val bold = match.groupValues[1]
        if (bold.isNotEmpty()) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
        } else {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(match.groupValues[2]) }
        }
        last = match.range.last + 1
    }
    append(line.substring(last))
}
