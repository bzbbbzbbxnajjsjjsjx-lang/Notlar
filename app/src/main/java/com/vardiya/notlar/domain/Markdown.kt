package com.vardiya.notlar.domain

/** A piece of text with a selection, as the editor field holds it; [start] to [end] is the selection. */
data class TextEdit(val text: String, val start: Int, val end: Int)

/** The few Markdown marks the editor writes and understands: bold, italic, headings and bullets. */
object Markdown {

    private val boldOrItalic = Regex("""\*\*(.+?)\*\*|\*(.+?)\*""")

    /** Wraps the selection in [BOLD]; with nothing selected it puts the cursor between two marks. */
    fun bold(edit: TextEdit): TextEdit = wrap(edit, BOLD)

    fun italic(edit: TextEdit): TextEdit = wrap(edit, ITALIC)

    /** Turns the line at the cursor into a heading, or back into a plain line. */
    fun heading(edit: TextEdit): TextEdit = togglePrefix(edit, HEADING)

    /** Turns the lines in the selection into bullets, or back into plain lines. */
    fun bullet(edit: TextEdit): TextEdit = togglePrefix(edit, BULLET)

    /** True when the selection sits inside a pair of [BOLD] marks. */
    fun isBold(edit: TextEdit): Boolean = surrounded(edit, BOLD)

    fun isItalic(edit: TextEdit): Boolean = italicAround(edit)

    /** True when the line at the cursor is a heading. */
    fun isHeading(edit: TextEdit): Boolean = lineAt(edit.text, edit.start).startsWith(HEADING)

    fun isBullet(edit: TextEdit): Boolean = lineAt(edit.text, edit.start).startsWith(BULLET)

    /** [text] without the marks, for plain text exports and shares; bullets stay as "• ". */
    fun strip(text: String): String = text.lines().joinToString("\n") { line ->
        val body = when {
            line.startsWith(HEADING) -> line.removePrefix(HEADING)
            line.startsWith(BULLET) -> "• " + line.removePrefix(BULLET)
            else -> line
        }
        boldOrItalic.replace(body) { it.groupValues[1].ifEmpty { it.groupValues[2] } }
    }
}

private const val BOLD = "**"
private const val ITALIC = "*"
private const val HEADING = "# "
private const val BULLET = "- "
private const val BOLD_ITALIC = "***"

private fun wrap(edit: TextEdit, mark: String): TextEdit {
    val (text, start, end) = edit
    val from = minOf(start, end)
    val to = maxOf(start, end)
    // Already wrapped: take the marks off again.
    val wrapped = if (mark == ITALIC) italicAround(edit) else surrounded(edit, mark)
    if (wrapped) {
        return TextEdit(text.removeRange(to, to + mark.length).removeRange(from - mark.length, from), from - mark.length, to - mark.length)
    }
    return TextEdit(text.substring(0, from) + mark + text.substring(from, to) + mark + text.substring(to), from + mark.length, to + mark.length)
}

private fun surrounded(edit: TextEdit, mark: String): Boolean {
    val from = minOf(edit.start, edit.end)
    val to = maxOf(edit.start, edit.end)
    return from >= mark.length && edit.text.startsWith(mark, from - mark.length) && edit.text.startsWith(mark, to)
}

/** True when the selection is italic; inside bold marks that takes a third star on each side. */
private fun italicAround(edit: TextEdit): Boolean {
    val boldItalic = surrounded(edit, BOLD_ITALIC)
    return if (surrounded(edit, BOLD)) boldItalic else surrounded(edit, ITALIC)
}

private fun togglePrefix(edit: TextEdit, prefix: String): TextEdit {
    val (text, start, end) = edit
    val from = minOf(start, end).coerceIn(0, text.length)
    val to = maxOf(start, end).coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', from - 1) + 1
    val lineEnd = text.indexOf('\n', to).let { if (it < 0) text.length else it }
    val lines = text.substring(lineStart, lineEnd).split('\n')
    val remove = lines.all { it.startsWith(prefix) }
    val changed = lines.joinToString("\n") { if (remove) it.removePrefix(prefix) else prefix + it.removePrefix(HEADING).removePrefix(BULLET) }
    val delta = changed.length - (lineEnd - lineStart)
    val firstDelta = if (remove) -prefix.length else prefix.length - lines.first().let { l ->
        if (l.startsWith(HEADING)) HEADING.length else if (l.startsWith(BULLET)) BULLET.length else 0
    }
    return TextEdit(
        text.substring(0, lineStart) + changed + text.substring(lineEnd),
        (start + firstDelta).coerceAtLeast(lineStart),
        (end + delta).coerceAtLeast(lineStart),
    )
}

private fun lineAt(text: String, index: Int): String {
    val at = index.coerceIn(0, text.length)
    val begin = text.lastIndexOf('\n', at - 1) + 1
    val finish = text.indexOf('\n', at).let { if (it < 0) text.length else it }
    return text.substring(begin, finish)
}
