package com.github.cpardi.intellijmarkdownreviewplugin.markers

import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewChangeListener
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import java.awt.Color

/**
 * Applies background highlights to comment ranges in the editor.
 * Line comments get a light yellow background on their range.
 * Page comments get a lighter cream background on the entire file.
 * Listens for file open events and comment changes to apply/remove highlights.
 */
class CommentRangeHighlighter(private val project: Project) {

    private val LOG = thisLogger()

    /** Light yellow background color for line comment highlights */
    private val LINE_HIGHLIGHT_COLOR = JBColor(Color(255, 255, 224), Color(75, 75, 40))

    /** Very light cream background for page comments (lighter than line comments) with 50% transparency */
    private val PAGE_HIGHLIGHT_COLOR = JBColor(Color(255, 255, 240, 128), Color(60, 60, 35, 128))

    /** Track current highlighters so we can remove them */
    private val activeHighlighters = mutableMapOf<String, MutableList<RangeHighlighter>>()

    init {
        // Listen for file selection changes
        project.messageBus.connect(project).subscribe<FileEditorManagerListener>(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    applyHighlights(file)
                }

                override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                    removeHighlights(file)
                }

                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val newFile = event.newFile ?: return
                    applyHighlights(newFile)
                }
            }
        )

        // Listen for comment changes
        project.messageBus.connect(project).subscribe<ReviewChangeListener>(
            ReviewService.REVIEW_CHANGE_TOPIC,
            object : ReviewChangeListener {
                override fun onCommentsChanged(commentId: Int?) {
                    ApplicationManager.getApplication().invokeLater {
                        refreshAllHighlights()
                    }
                }

                override fun onReviewChanged() {
                    ApplicationManager.getApplication().invokeLater {
                        refreshAllHighlights()
                    }
                }
            }
        )
    }

    /**
     * Applies highlights to a specific file based on current comments.
     */
    fun applyHighlights(file: VirtualFile) {
        val service = ReviewService.getInstance(project)
        val relativePath = service.getRelativePath(file)
        val comments = service.getCommentsForFile(relativePath)

        val document = runReadAction { FileDocumentManager.getInstance().getDocument(file) } ?: return

        // Attach RangeMarkers for comments in this file
        service.updateRangeMarkersForFile(relativePath, document)

        // Remove existing highlights first
        removeHighlights(file)

        // Apply new highlights
        val highlighters = mutableListOf<RangeHighlighter>()
        val markupModel = com.intellij.openapi.editor.EditorFactory.getInstance()
            .getEditors(document, project)
            .firstOrNull()?.markupModel ?: return

        // Separate page comments from line comments
        val pageComments = comments.filter { it.isPageComment() }
        val lineComments = comments.filter { !it.isPageComment() }

        // Apply page comment highlights first (lower layer)
        for (comment in pageComments) {
            try {
                // Highlight entire file for page comments
                val startOffset = 0
                val endOffset = document.textLength

                val textAttributes = TextAttributes()
                textAttributes.backgroundColor = PAGE_HIGHLIGHT_COLOR

                val highlighter = markupModel.addRangeHighlighter(
                    startOffset,
                    endOffset,
                    HighlighterLayer.SELECTION - 2, // Lower layer than line comments
                    textAttributes,
                    HighlighterTargetArea.LINES_IN_RANGE
                )

                highlighters.add(highlighter)
            } catch (e: Exception) {
                LOG.warn("Failed to highlight page comment ${comment.id}: ${e.message}")
            }
        }

        // Apply line comment highlights on top
        for (comment in lineComments) {
            try {
                val startOffset = document.getLineStartOffset(comment.startLine - 1)
                val endOffset = document.getLineEndOffset(comment.endLine - 1)

                val textAttributes = TextAttributes()
                textAttributes.backgroundColor = LINE_HIGHLIGHT_COLOR

                val highlighter = markupModel.addRangeHighlighter(
                    startOffset,
                    endOffset,
                    HighlighterLayer.SELECTION - 1,
                    textAttributes,
                    HighlighterTargetArea.LINES_IN_RANGE
                )

                highlighters.add(highlighter)
            } catch (e: Exception) {
                LOG.warn("Failed to highlight comment ${comment.id}: ${e.message}")
            }
        }

        activeHighlighters[relativePath] = highlighters
    }

    /**
     * Removes highlights for a specific file.
     */
    private fun removeHighlights(file: VirtualFile) {
        val service = ReviewService.getInstance(project)
        val relativePath = service.getRelativePath(file)

        val highlighters = activeHighlighters.remove(relativePath) ?: return
        for (highlighter in highlighters) {
            try {
                highlighter.dispose()
            } catch (e: Exception) {
                // Highlighter may already be disposed
            }
        }
    }

    /**
     * Refreshes all highlights by removing and reapplying them.
     */
    private fun refreshAllHighlights() {
        val fileEditorManager = FileEditorManager.getInstance(project)
        for (file in fileEditorManager.openFiles) {
            applyHighlights(file)
        }
    }
}
