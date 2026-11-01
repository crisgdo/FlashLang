package com.cristiano.flashlang.data.importer

/** Utilities shared by JSON import and manual card editing for cloze examples. */
object ClozeText {
    private val gapRegex = Regex("\\[\\s*_{3,}\\s*]|_{3,}|\\[\\.\\.\\.\\]|…")

    /** Returns a normalized example with one `___` gap, or null if no gap can be made. */
    fun normalize(example: String, term: String): String? {
        val trimmedExample = example.trim()
        val trimmedTerm = term.trim()
        if (trimmedExample.isEmpty() || trimmedTerm.isEmpty()) return null

        val gaps = gapRegex.findAll(trimmedExample).toList()
        if (gaps.isNotEmpty()) {
            return buildString {
                var cursor = 0
                gaps.forEachIndexed { index, gap ->
                    append(trimmedExample, cursor, gap.range.first)
                    append(if (index == 0) "___" else trimmedTerm)
                    cursor = gap.range.last + 1
                }
                append(trimmedExample.substring(cursor))
            }
        }

        val termRegex = Regex(
            "(?<![\\p{L}\\p{N}_])${Regex.escape(trimmedTerm)}(?![\\p{L}\\p{N}_])",
            RegexOption.IGNORE_CASE,
        )
        val match = termRegex.find(trimmedExample) ?: return null
        return buildString {
            append(trimmedExample, 0, match.range.first)
            append("___")
            append(trimmedExample.substring(match.range.last + 1))
        }
    }

    fun fillFirstGap(example: String, term: String): String =
        example.replaceFirst("___", term)
}
