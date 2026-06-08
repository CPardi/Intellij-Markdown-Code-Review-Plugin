package com.github.cpardi.markdowncodereview.services

import com.intellij.openapi.editor.RangeMarker

/**
 * Represents a single review comment in the system.
 *
 * @property id Sequential identifier (1, 2, 3...), assigned on load
 * @property relativePath File path relative to project root
 * @property startLine 1-based inclusive start line
 * @property endLine 1-based inclusive end line
 * @property body Comment text, may contain newlines
 * @property rangeMarker Attached to document, null if file not open/deleted
 */
data class Comment(
    val id: Int,
    @Volatile var relativePath: String,
    @Volatile var startLine: Int,
    @Volatile var endLine: Int,
    @Volatile var body: String,
    @Volatile private var _rangeMarker: RangeMarker? = null
) {
    /**
     * Checks if this is a page comment (applies to entire file).
     * Page comments use startLine=0 and endLine=0 as sentinel values.
     * @return true if this is a page comment
     */
    fun isPageComment(): Boolean = startLine == 0 && endLine == 0

    /**
     * Returns a formatted string representation of the line range.
     * @return "startLine-endLine" or "startLine" if single line, empty string for page comments
     */
    fun getLineRangeText(): String {
        if (isPageComment()) return ""
        return if (startLine == endLine) {
            startLine.toString()
        } else {
            "$startLine-$endLine"
        }
    }

    /**
     * Returns a formatted header string for this comment.
     * @return "@[relativePath]:" for page comments, "@[relativePath:startLine:endLine]:" for line comments
     */
    fun toHeader(): String {
        return if (isPageComment()) {
            "@[$relativePath]:"
        } else {
            "@[$relativePath:$startLine:$endLine]:"
        }
    }

    /**
     * Checks if a given line falls within this comment's range.
     * @param line The 1-based line number to check
     * @return true if the line is within the comment's range
     */
    fun containsLine(line: Int): Boolean = line in startLine..endLine

    @get:Synchronized
    @set:Synchronized
    var rangeMarker: RangeMarker?
        get() = _rangeMarker
        set(value) {
            _rangeMarker?.dispose()
            _rangeMarker = value
        }
}
