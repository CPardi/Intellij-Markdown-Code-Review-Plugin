package com.github.cpardi.markdowncodereview.parser

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.github.cpardi.markdowncodereview.services.Comment
import com.github.cpardi.markdowncodereview.settings.ReviewSettings
import com.github.cpardi.markdowncodereview.services.ReviewFile

/**
 * Writer for review markdown files.
 * Writes `ReviewFile` objects to disk as `<reviewsDir>/<name>.md`.
 */
object ReviewFileWriter {

    private val LOG = thisLogger()
    const val DELIMITER = "---"

    /**
     * Gets the configured reviews directory name from settings.
     *
     * @return The reviews directory name
     */
    fun getReviewsDirName(): String {
        return ReviewSettings.getInstance().reviewsDir
    }

    /**
     * Writes a review file to disk.
     * Creates the reviews directory if it doesn't exist.
     *
     * @param reviewFile The review file to write
     * @param projectBaseDir The project base directory
     * @return The VirtualFile that was written, or null on failure
     */
    fun write(reviewFile: ReviewFile, projectBaseDir: VirtualFile): VirtualFile? {
        return write(reviewFile, projectBaseDir, getReviewsDirName())
    }

    /**
     * Writes a review file to disk.
     * Creates the reviews directory if it doesn't exist.
     *
     * @param reviewFile The review file to write
     * @param projectBaseDir The project base directory
     * @param reviewsDirName The reviews directory name
     * @return The VirtualFile that was written, or null on failure
     */
    fun write(reviewFile: ReviewFile, projectBaseDir: VirtualFile, reviewsDirName: String): VirtualFile? {
        return try {
            // Validate comments don't contain delimiter
            for (comment in reviewFile.comments) {
                if (comment.body.contains(DELIMITER)) {
                    LOG.error("Comment body contains delimiter '---' which would corrupt the file")
                    throw IllegalArgumentException("Comment body cannot contain '---' delimiter")
                }
            }

            // Ensure reviews directory exists
            val reviewsDir = getOrCreateReviewsDir(projectBaseDir, reviewsDirName)

            // Get or create the file
            val fileName = "${reviewFile.name}.md"
            val file = reviewsDir.findChild(fileName) ?: reviewsDir.createChildData(this, fileName)

            // Write content
            val content = buildContent(reviewFile)
            VfsUtil.saveText(file, content)

            reviewFile.virtualFile = file
            LOG.info("Wrote review file: ${file.path}")
            file
        } catch (e: Exception) {
            LOG.error("Failed to write review file: ${reviewFile.name}", e)
            null
        }
    }

    /**
     * Gets or creates the reviews directory in the project.
     *
     * @param projectBaseDir The project base directory
     * @param reviewsDirName The reviews directory name
     * @return The reviews VirtualFile directory
     */
    fun getOrCreateReviewsDir(projectBaseDir: VirtualFile, reviewsDirName: String = getReviewsDirName()): VirtualFile {
        return projectBaseDir.findChild(reviewsDirName) ?: projectBaseDir.createChildDirectory(this, reviewsDirName)
    }

    /**
     * Builds the markdown content for a review file.
     *
     * @param reviewFile The review file to convert to string
     * @return The formatted markdown content
     */
    fun buildContent(reviewFile: ReviewFile): String {
        val output = StringBuilder()

        // Write preamble
        if (reviewFile.preamble.isNotEmpty()) {
            output.append(reviewFile.preamble)
            if (reviewFile.comments.isNotEmpty()) {
                output.append("\n\n")
            }
        }

        // Sort comments: page comments first, then by path, then by line
        val sortedComments = reviewFile.comments.sortedWith(
            compareBy(
                { if (it.isPageComment()) 0 else 1 },
                { it.relativePath },
                { it.startLine }
            )
        )

        // Write each comment
        for ((index, comment) in sortedComments.withIndex()) {
            output.append(formatComment(comment))
            if (index < sortedComments.size - 1) {
                output.append("\n")
            }
        }

        // Write postamble
        if (reviewFile.postamble.isNotEmpty()) {
            output.append("\n")
            output.append(reviewFile.postamble)
        }

        return output.toString()
    }

    /**
     * Formats a single comment as markdown.
     *
     * @param comment The comment to format
     * @return The formatted comment string
     */
    fun formatComment(comment: Comment): String {
        val sb = StringBuilder()
        sb.append(comment.toHeader())
        sb.append("\n")
        if (comment.body.isNotEmpty()) {
            sb.append(comment.body)
            sb.append("\n")
        }
        sb.append(DELIMITER)
        return sb.toString()
    }

    /**
     * Deletes a review file from disk.
     *
     * @param reviewFile The review file to delete
     * @return true if deletion succeeded, false otherwise
     */
    fun delete(reviewFile: ReviewFile): Boolean {
        val file = reviewFile.virtualFile ?: return false
        return try {
            file.delete(this)
            LOG.info("Deleted review file: ${reviewFile.name}")
            true
        } catch (e: Exception) {
            LOG.error("Failed to delete review file: ${reviewFile.name}", e)
            false
        }
    }
}
