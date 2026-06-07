package com.github.cpardi.intellijmarkdownreviewplugin.services

import com.github.cpardi.intellijmarkdownreviewplugin.ReviewBundle
import com.github.cpardi.intellijmarkdownreviewplugin.parser.ReviewFileParser
import com.github.cpardi.intellijmarkdownreviewplugin.parser.ReviewFileWriter
import com.github.cpardi.intellijmarkdownreviewplugin.settings.ReviewSettings
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic

/**
 * Listener interface for UI refresh events.
 */
interface ReviewChangeListener {
    fun onCommentsChanged(commentId: Int? = null)
    fun onReviewChanged()
}

/**
 * Main service for managing review files and comments.
 * This is a project-level service that holds the active review state.
 */
@Service(Service.Level.PROJECT)
class ReviewService(private val project: Project) {

    private val LOG = thisLogger()

    /**
     * Current active review, null means `<None>` is selected
     */
    @Volatile
    var activeReview: ReviewFile? = null
        private set

    /**
     * Gets the project base directory.
     */
    private val projectBaseDir: VirtualFile?
        get() = project.guessProjectDir()

    /**
     * Sets the active review by loading from disk.
     * Pass null to select <None>.
     * @param name The review name, or null for <None>
     */
    fun setActiveReview(name: String) {
        if (name == NONE_SENTINEL) setActiveReview(null as ReviewFile?)
        else setActiveReview(loadReview(name))
    }

    /**
     * Sets the active review.
     * Pass null to select <None>.
     * @param review The review, or null for <None>
     */
    fun setActiveReview(review: ReviewFile?) {
        if (review == null) {
            synchronized(this) {
                activeReview = null
            }
            LOG.info("Set active review to <None>")
            notifyReviewChanged()
            refreshOpenEditors()
        } else {
            synchronized(this) {
                activeReview = review
            }
            LOG.info("Set active review to: ${review.name}")
            attachRangeMarkersForOpenFiles()
            notifyReviewChanged()
            refreshOpenEditors()
        }
    }

    /**
     * Loads a review file from disk.
     * @param name The review name (filename without extension)
     * @return The loaded ReviewFile, or null if loading failed
     */
    private fun loadReview(name: String): ReviewFile? {
        val baseDir = projectBaseDir ?: return null
        val reviewsDirName = ReviewSettings.getInstance().reviewsDir
        val reviewsDir = baseDir.findChild(reviewsDirName) ?: return null
        val file = reviewsDir.findChild("$name.md") ?: return null

        return ReviewFileParser.parse(file)
    }

    /**
     * Gets the list of available review names from the reviews directory.
     * @return List of review names (filenames without .md extension)
     */
    fun getAvailableReviewNames(): List<String> {
        val baseDir = projectBaseDir ?: return emptyList()
        val reviewsDirName = ReviewSettings.getInstance().reviewsDir
        val reviewsDir = baseDir.findChild(reviewsDirName) ?: return emptyList()

        return reviewsDir.children
            .filter { it.extension == "md" }
            .map { it.nameWithoutExtension }
            .sorted()
    }

    /**
     * Creates a new review file with a unique name.
     *
     * @return CreateReviewResult.Success with the review name on success,
     *         or CreateReviewResult.Failure with an error message on failure
     */
    fun createNewReview(): CreateReviewResult {
        val baseDir = projectBaseDir
            ?: return CreateReviewResult.Failure(ReviewBundle.message("errorProjectDirNotFound"))

        val existingNames = getAvailableReviewNames().toSet()
        var counter = 1
        var name = "review-$counter"

        while (name in existingNames) {
            counter++
            name = "review-$counter"
        }

        val reviewFile = ReviewFile(name = name)

        val resultFile = WriteAction.computeAndWait<VirtualFile?, Exception> {
            ReviewFileWriter.write(reviewFile, baseDir)
        }

        return if (resultFile != null) {
            reviewFile.virtualFile = resultFile
            activeReview = reviewFile
            LOG.info("Created new review: $name")
            notifyReviewChanged()
            CreateReviewResult.Success(name)
        } else {
            CreateReviewResult.Failure(ReviewBundle.message("errorCreateReview"))
        }
    }

    /**
     * Deletes the specified review file from disk.
     * @param name The review name to delete
     * @return true if deletion succeeded
     */
    fun deleteReview(name: String): Boolean {
        if (name == NONE_SENTINEL) return false

        val baseDir = projectBaseDir ?: return false
        val reviewsDirName = ReviewSettings.getInstance().reviewsDir
        val reviewsDir = baseDir.findChild(reviewsDirName) ?: return false
        val file = reviewsDir.findChild("$name.md") ?: return false
        val parentDir = file.parent

        return try {
            WriteAction.computeAndWait<Void, Exception> {
                file.delete(this)
                parentDir.refresh(false, false)
                null
            }
            if (activeReview?.name == name) {
                activeReview = null
            }
            LOG.info("Deleted review: $name")
            notifyReviewChanged()
            true
        } catch (e: Exception) {
            LOG.error("Failed to delete review: $name", e)
            false
        }
    }

    /**
     * Saves the active review to disk.
     * @return true if save succeeded
     */
    fun saveActiveReview(): Boolean {
        val review = activeReview ?: return false
        val baseDir = projectBaseDir ?: return false

        return WriteAction.computeAndWait<Boolean, Exception> {
            val result = ReviewFileWriter.write(review, baseDir)
            result != null
        }
    }

    // ==================== Comment CRUD Operations ====================

    /**
     * Creates a new comment and adds it to the active review.
     * @param relativePath The relative file path
     * @param startLine The 1-based start line
     * @param endLine The 1-based end line
     * @param body The comment body
     * @return The created comment, or null if no active review
     */
    fun addComment(relativePath: String, startLine: Int, endLine: Int, body: String): Comment? {
        val review = activeReview ?: return null

        val comment = Comment(
            id = review.nextId(),
            relativePath = relativePath,
            startLine = startLine,
            endLine = endLine,
            body = body
        )

        review.comments.add(comment)

        // Try to attach a RangeMarker if the file is open
        attachRangeMarker(comment)

        saveActiveReview()
        notifyCommentsChanged()

        LOG.info("Added comment ${comment.id} to review ${review.name}")
        return comment
    }

    /**
     * Creates a new page comment and adds it to the active review.
     * Page comments apply to an entire file rather than specific lines.
     * @param relativePath The relative file path
     * @param body The comment body
     * @return The created comment, or null if no active review
     */
    fun addPageComment(relativePath: String, body: String): Comment? {
        val review = activeReview ?: return null

        val comment = Comment(
            id = review.nextId(),
            relativePath = relativePath,
            startLine = 0,
            endLine = 0,
            body = body
        )

        review.comments.add(comment)

        // Page comments don't need RangeMarkers

        saveActiveReview()
        notifyCommentsChanged()

        LOG.info("Added page comment ${comment.id} to review ${review.name}")
        return comment
    }

    /**
     * Updates the body of an existing comment.
     * @param id The comment ID
     * @param newBody The new comment body
     * @return true if the comment was updated
     */
    fun editComment(id: Int, newBody: String): Boolean {
        val comment = getCommentById(id) ?: return false

        comment.body = newBody
        saveActiveReview()
        notifyCommentsChanged(id)

        LOG.info("Updated comment $id")
        return true
    }

    /**
     * Updates the line range of an existing comment.
     * @param id The comment ID
     * @param newStartLine The new start line
     * @param newEndLine The new end line
     * @return true if the comment was updated
     */
    fun editCommentRange(id: Int, newStartLine: Int, newEndLine: Int): Boolean {
        val comment = getCommentById(id) ?: return false

        comment.startLine = newStartLine
        comment.endLine = newEndLine

        comment.rangeMarker = null
        attachRangeMarker(comment)

        saveActiveReview()
        notifyCommentsChanged()

        LOG.info("Updated comment $id range to $newStartLine-$newEndLine")
        return true
    }

    /**
     * Deletes a comment from the active review.
     * @param id The comment ID to delete
     * @return true if the comment was deleted
     */
    fun deleteComment(id: Int): Boolean {
        val review = activeReview ?: return false

        val removed = review.removeComment(id)
        if (removed) {
            saveActiveReview()
            notifyCommentsChanged()
            LOG.info("Deleted comment $id")
        }
        return removed
    }

    /**
     * Gets all comments for a specific file path.
     * Includes both page comments and line comments.
     * @param relativePath The relative file path
     * @return List of comments for the file
     */
    fun getCommentsForFile(relativePath: String): List<Comment> =
        activeReview?.getCommentsForFile(relativePath) ?: emptyList()

    /**
     * Gets page comments for a specific file path.
     * @param relativePath The relative file path
     * @return List of page comments for the file
     */
    fun getPageCommentsForFile(relativePath: String): List<Comment> =
        activeReview?.getPageCommentsForFile(relativePath) ?: emptyList()

    /**
     * Gets all comments that span a specific line in a file.
     * Note: Page comments are excluded from results.
     *
     * @param relativePath The relative file path
     * @param line The 1-based line number
     * @return List of comments containing the line
     */
    fun getCommentsForLine(relativePath: String, line: Int): List<Comment> =
        activeReview?.getCommentsForLine(relativePath, line) ?: emptyList()

    /**
     * Gets a comment by its ID from the active review.
     *
     * @param id The comment ID
     * @return The comment, or null if not found
     */
    fun getCommentById(id: Int): Comment? = activeReview?.getCommentById(id)

    // ==================== RangeMarker Management ====================

    /**
     * Attaches a RangeMarker to a comment for the specified document.
     * @param comment The comment to attach the marker to
     * @param document The document to attach to
     */
    fun attachRangeMarker(comment: Comment, document: Document) {
        try {
            val startOffset = document.getLineStartOffset(comment.startLine - 1)
            val endOffset = document.getLineEndOffset(comment.endLine - 1)

            val marker = document.createRangeMarker(startOffset, endOffset, true)
            marker.isGreedyToRight = true

            comment.rangeMarker = marker
        } catch (e: Exception) {
            LOG.warn("Failed to create RangeMarker for comment ${comment.id}: ${e.message}")
        }
    }

    /**
     * Attaches a RangeMarker to a comment by finding the document.
     * @param comment The comment to attach the marker to
     */
    fun attachRangeMarker(comment: Comment) {
        runReadAction {
            val baseDir = projectBaseDir ?: return@runReadAction
            val file = VfsUtil.findRelativeFile(baseDir, *comment.relativePath.split("/").toTypedArray()) ?: return@runReadAction
            if (!file.isValid) return@runReadAction

            val document = FileDocumentManager.getInstance().getDocument(file) ?: return@runReadAction
            attachRangeMarker(comment, document)
        }
    }

    /**
     * Updates RangeMarkers for all comments on a specific file.
     * Called when a file is opened or the active review changes.
     * Note: Page comments (startLine=0, endLine=0) are skipped as they don't track specific lines.
     *
     * @param relativePath The relative file path
     * @param document The document
     */
    fun updateRangeMarkersForFile(relativePath: String, document: Document) {
        val comments = getCommentsForFile(relativePath)
        for (comment in comments) {
            // Skip page comments - they don't need RangeMarkers
            if (comment.isPageComment()) continue

            if (comment.rangeMarker == null || !comment.rangeMarker!!.isValid) {
                attachRangeMarker(comment, document)
            }
        }
    }

    /**
     * Updates comment line numbers from their RangeMarkers.
     * Called on document changes to track edited lines.
     *
     * @param document The document that changed
     */
    fun updateCommentLinesFromMarkers(document: Document) {
        runReadAction {
            val file = FileDocumentManager.getInstance().getFile(document) ?: return@runReadAction
            if (!file.isValid) return@runReadAction

            val relativePath = getRelativePath(file)
            val comments = getCommentsForFile(relativePath)

            for (comment in comments) {
                val marker = comment.rangeMarker
                if (marker != null && marker.isValid) {
                    try {
                        comment.startLine = document.getLineNumber(marker.startOffset) + 1
                        comment.endLine = document.getLineNumber(marker.endOffset) + 1
                    } catch (e: Exception) {
                        LOG.warn("Failed to update lines from RangeMarker: ${e.message}")
                    }
                } else if (marker != null && !marker.isValid) {
                    // Marker became invalid, clear it
                    comment.rangeMarker = null
                }
            }
        }
    }

    /**
     * Attaches RangeMarkers for all comments in files that are currently open in editors.
     * Should be called when the active review changes to ensure line tracking works.
     */
    fun attachRangeMarkersForOpenFiles() {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val documentManager = FileDocumentManager.getInstance()

        runReadAction {
            for (file in fileEditorManager.openFiles) {
                if (!file.isValid) continue

                val relativePath = getRelativePath(file)
                val comments = getCommentsForFile(relativePath)
                if (comments.isEmpty()) continue

                val document = documentManager.getDocument(file) ?: continue
                for (comment in comments) {
                    if (comment.rangeMarker == null || !comment.rangeMarker!!.isValid) {
                        attachRangeMarker(comment, document)
                    }
                }
            }
        }
    }

    /**
     * Refreshes all open editors to update gutter icons and highlights.
     * Forces the daemon analyzer to re-run on all open files.
     */
    fun refreshOpenEditors() {
        ApplicationManager.getApplication().invokeLater {
            val analyzer = DaemonCodeAnalyzer.getInstance(project)
            analyzer.restart("Selected review was modified")
        }
    }

    /**
     * Updates the relative path for all comments on a file that was renamed or moved.
     *
     * @param oldPath The old relative path
     * @param newPath The new relative path
     */
    fun updateCommentsForFileRename(oldPath: String, newPath: String) {
        val comments = getCommentsForFile(oldPath)
        if (comments.isEmpty()) return

        for (comment in comments) {
            comment.relativePath = newPath
        }

        saveActiveReview()
        notifyCommentsChanged()
        LOG.info("Updated ${comments.size} comments from $oldPath to $newPath")
    }

    /**
     * Applies a batch of path renames to comments in the active review.
     * Used by [ReviewAsyncFileListener] to process multiple renames/moves efficiently.
     *
     * @param renames Map of old relative path to new relative path
     */
    fun applyCommentRenames(renames: Map<String, String>) {
        val review = activeReview ?: return
        var updatedCount = 0

        for (comment in review.comments) {
            val newPath = renames[comment.relativePath]
            if (newPath != null) {
                comment.relativePath = newPath
                updatedCount++
            }
        }

        if (updatedCount > 0) {
            saveActiveReview()
            notifyCommentsChanged()
            LOG.info("Applied $updatedCount comment path rename(s)")
        }
    }

    // ==================== Path Utilities ====================

    /**
     * Gets the relative path of a file from the project root.
     * @param file The virtual file
     * @return The relative path string
     */
    fun getRelativePath(file: VirtualFile): String {
        val baseDir = projectBaseDir ?: return file.path
        return VfsUtil.getRelativePath(file, baseDir) ?: file.path
    }

    private fun notifyCommentsChanged(commentId: Int? = null) {
        project.messageBus.syncPublisher(REVIEW_CHANGE_TOPIC).onCommentsChanged(commentId)
    }

    private fun notifyReviewChanged() {
        project.messageBus.syncPublisher(REVIEW_CHANGE_TOPIC).onReviewChanged()
    }

    companion object {
        /**
         * Sentinel value for "no review selected"
         */
        const val NONE_SENTINEL = "<None>"

        /**
         * Message bus topic for UI updates
         */
        val REVIEW_CHANGE_TOPIC: Topic<ReviewChangeListener> = Topic.create(
            "Review Change Topic",
            ReviewChangeListener::class.java
        )

        /**
         * Gets the service instance for a project.
         */
        fun getInstance(project: Project): ReviewService =
            project.getService(ReviewService::class.java)
    }
}
