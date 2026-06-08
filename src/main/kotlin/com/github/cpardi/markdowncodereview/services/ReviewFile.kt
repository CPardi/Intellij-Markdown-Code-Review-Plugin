package com.github.cpardi.markdowncodereview.services

import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Represents a review file containing multiple comments.
 *
 * @property name Filename without `.md` extension (e.g., "review-1")
 * @property virtualFile Handle to disk file, null if not yet created
 * @property comments Ordered list of comments
 * @property preamble Text before first header, preserved verbatim
 * @property postamble Text after last `---`, preserved verbatim
 */
data class ReviewFile(
    val name: String,
    var virtualFile: VirtualFile? = null,
    val comments: CopyOnWriteArrayList<Comment> = CopyOnWriteArrayList(),
    var preamble: String = "",
    var postamble: String = ""
) {
    /**
     * Returns true if this review has no comments.
     */
    fun isEmpty(): Boolean = comments.isEmpty()

    /**
     * Returns the number of comments in this review.
     */
    fun size(): Int = comments.size

    /**
     * Gets a comment by its ID.
     * @param id The comment ID to search for
     * @return The comment with the given ID, or null if not found
     */
    fun getCommentById(id: Int): Comment? = comments.find { it.id == id }

    /**
     * Gets all comments for a specific file path.
     * @param relativePath The relative file path to filter by
     * @return List of comments for the given file
     */
    fun getCommentsForFile(relativePath: String): List<Comment> =
        comments.filter { it.relativePath == relativePath }

    /**
     * Gets page comments for a specific file path.
     * @param relativePath The relative file path to filter by
     * @return List of page comments for the given file
     */
    fun getPageCommentsForFile(relativePath: String): List<Comment> =
        comments.filter { it.relativePath == relativePath && it.isPageComment() }

    /**
     * Gets all comments that span a specific line in a file.
     * Note: Page comments are not included in results as they don't span specific lines.
     * @param relativePath The relative file path
     * @param line The 1-based line number
     * @return List of comments that include the given line
     */
    fun getCommentsForLine(relativePath: String, line: Int): List<Comment> =
        comments.filter { it.relativePath == relativePath && !it.isPageComment() && it.containsLine(line) }

    /**
     * Removes a comment by its ID.
     * @param id The comment ID to remove
     * @return true if a comment was removed, false if not found
     */
    fun removeComment(id: Int): Boolean = comments.removeIf { it.id == id }

    /**
     * Gets the next available ID for a new comment.
     * @return The next sequential ID
     */
    fun nextId(): Int = (comments.maxOfOrNull { it.id } ?: 0) + 1
}
