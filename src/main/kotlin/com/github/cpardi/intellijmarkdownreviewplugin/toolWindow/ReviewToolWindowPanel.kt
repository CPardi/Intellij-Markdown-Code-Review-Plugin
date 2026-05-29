package com.github.cpardi.intellijmarkdownreviewplugin.toolWindow

import com.github.cpardi.intellijmarkdownreviewplugin.ReviewBundle
import com.github.cpardi.intellijmarkdownreviewplugin.services.Comment
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewChangeListener
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.github.cpardi.intellijmarkdownreviewplugin.settings.ReviewSettings
import com.github.cpardi.intellijmarkdownreviewplugin.settings.SettingsChangeListener
import com.github.cpardi.intellijmarkdownreviewplugin.ui.CommentBubblePanel
import com.intellij.filename.UniqueNameBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import java.nio.file.Path
import javax.swing.*

/**
 * Panel for the Review Output tool window using Kotlin UI DSL patterns.
 * Shows review selector dropdown, add/delete buttons, and the comments list.
 */
class ReviewToolWindowPanel(private val project: Project, private val service: ReviewService) : JBPanel<ReviewToolWindowPanel>(BorderLayout()) {

    private var isUpdatingSelection = false
    private val commentPanelMap = mutableMapOf<Int, CommentBubblePanel>()

    private val fileEditorManager get() = FileEditorManager.getInstance(project)

    internal val reviewComboBox = ComboBox<String>()
    internal val addButton = JButton("Add")
    internal val deleteButton = JButton("Delete")
    internal val noCommentsLabel = JBLabel(ReviewBundle.message("noComments")).apply { border = JBUI.Borders.empty(10) }
    internal val commentsPanel = JPanel().apply { border = JBUI.Borders.empty(10) }
    internal val commentsScrollPane = JScrollPane(commentsPanel)

    init {
        fun setupTopBar() {
            val topPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
                border = JBUI.Borders.customLineBottom(JBUI.CurrentTheme.ToolWindow.borderColor())

                // Review selector dropdown
                add(JBLabel("Review:"))
                reviewComboBox.addItemListener { event -> if (event.stateChange == ItemEvent.SELECTED) onReviewSelected() }
                add(reviewComboBox)

                // Add button
                addButton.apply {
                    toolTipText = ReviewBundle.message("createNewReview")
                    addActionListener { onCreateNewReview() }
                }
                add(addButton)

                // Delete button
                deleteButton.apply {
                    toolTipText = ReviewBundle.message("deleteReview")
                    addActionListener { onDeleteReview() }
                }
                add(deleteButton)
            }

            add(topPanel, BorderLayout.NORTH)
        }

        fun setupCommentsList() {
            commentsPanel.layout = BoxLayout(commentsPanel, BoxLayout.Y_AXIS)
            commentsScrollPane.border = JBUI.Borders.empty()

            // Set scroll speed to match the file editor
            val editor = fileEditorManager.selectedTextEditor
            val editorUnitIncrement = editor?.lineHeight ?: 16
            commentsScrollPane.verticalScrollBar.unitIncrement = editorUnitIncrement

            add(commentsScrollPane, BorderLayout.CENTER)
        }

        setupTopBar()
        setupCommentsList()
        refreshReviewList()
        refreshAllComments()

        // Subscribe to change notifications
        project.messageBus.connect().subscribe(
            ReviewService.REVIEW_CHANGE_TOPIC,
            object : ReviewChangeListener {
                override fun onCommentsChanged(commentId: Int?) = ApplicationManager.getApplication().invokeLater { updateSingleComment(commentId) }

                override fun onReviewChanged() = ApplicationManager.getApplication().invokeLater {
                    refreshReviewList()
                    refreshAllComments()
                }
            }
        )

        // Subscribe to settings changes to refresh review list when directory changes
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            ReviewSettings.SETTINGS_CHANGED_TOPIC,
            object : SettingsChangeListener {
                override fun onSettingsChanged() {
                    ApplicationManager.getApplication().invokeLater {
                        service.setActiveReview(null)
                        refreshReviewList()
                        refreshAllComments()
                    }
                }
            }
        )
    }

    // User interaction handlers

    internal fun navigateToComment(comment: Comment) {
        val baseDir = VirtualFileManager.getInstance()
            .findFileByNioPath(Path.of(project.basePath ?: return)) ?: return

        val file = VfsUtil.findRelativeFile(
            baseDir, *comment.relativePath.split("/").toTypedArray()
        ) ?: return

        fileEditorManager.openFile(file, true)

        // Navigate to the specific line for line comments (not for page comments)
        if (!comment.isPageComment()) {
            val editor = fileEditorManager.selectedTextEditor
            if (editor != null) {
                val line = comment.startLine - 1 // 0-based
                if (line >= 0 && line < editor.document.lineCount) {
                    editor.caretModel.moveToLogicalPosition(LogicalPosition(line, 0))
                    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
                }
            }
        }
    }

    internal fun onReviewSelected() {
        if (isUpdatingSelection) return

        val selectedName = reviewComboBox.selectedItem as? String ?: return
        if (selectedName == (service.activeReview?.name ?: ReviewService.NONE_SENTINEL)) return

        service.setActiveReview(selectedName)
        updateDeleteButtonState()
    }

    internal fun onCreateNewReview() {
        service.createNewReview().getOrShowError(project) ?: return
        refreshReviewList()
        refreshAllComments()
    }

    internal fun onDeleteReview() {
        val review = service.activeReview ?: return
        val result = Messages.showYesNoDialog(
            project,
            ReviewBundle.message("deleteConfirmation", review.name),
            ReviewBundle.message("confirm"),
            Messages.getQuestionIcon()
        )
        if (result == Messages.YES) {
            service.deleteReview(review.name)
            // Explicitly clear active review to ensure UI state is consistent
            service.setActiveReview(null)
            refreshReviewList()
            refreshAllComments()
        }
    }

    internal fun onDeleteComment(commentId: Int) {
        service.deleteComment(commentId)
        refreshAllComments()
    }

    // Helper functions

    private fun refreshReviewList() {
        isUpdatingSelection = true
        try {
            reviewComboBox.removeAllItems()

            // Add <None> at top
            reviewComboBox.addItem(ReviewService.NONE_SENTINEL)

            // Add all available reviews
            val names = service.getAvailableReviewNames()
            for (name in names) {
                reviewComboBox.addItem(name)
            }

            // Select the active review
            val activeName = service.activeReview?.name ?: ReviewService.NONE_SENTINEL
            reviewComboBox.selectedItem = activeName

            // Update delete button state based on active review
            updateDeleteButtonState()
        } finally {
            isUpdatingSelection = false
        }
    }


    private fun refreshAllComments() {
        fun createCommentItem(comment: Comment, displayPath: String): CommentBubblePanel {
            return CommentBubblePanel().apply {
                border = JBUI.Borders.empty(10, 12, 10, 12)

                // Header
                headerText = if (comment.isPageComment()) {
                    "$displayPath:${comment.getLineRangeText()} [Page]"
                } else {
                    "$displayPath:${comment.getLineRangeText()}:${comment.getLineRangeText()}"
                }
                onHeaderClick = { navigateToComment(comment) }

                // Delete Button
                onDelete = { commentId -> onDeleteComment(commentId) }

                // Body text
                bodyText = comment.body
                onBodyFocusLost = { e ->
                    val newBody = bodyText
                    if (newBody != comment.body && !newBody.contains("---")) {
                        service.editComment(comment.id, newBody)
                    }
                }
            }
        }

        commentPanelMap.clear()
        commentsPanel.removeAll()

        val review = service.activeReview
        if (review == null || review.isEmpty()) {
            commentsPanel.add(noCommentsLabel)
        } else {
            // Sort comments: page comments first, then by path, then by line
            val sortedComments = review.comments.sortedWith(
                compareBy(
                    { if (it.isPageComment()) 0 else 1 },
                    { it.relativePath },
                    { it.startLine }
                )
            )

            val displayPaths = computeDisplayPaths(sortedComments)
            for ((index, comment) in sortedComments.withIndex()) {
                val panel = createCommentItem(comment, displayPaths[comment.id]!!)
                commentPanelMap[comment.id] = panel
                commentsPanel.add(panel)
                // Add spacing between bubbles (but not after the last one)
                if (index < sortedComments.lastIndex) {
                    commentsPanel.add(Box.createVerticalStrut(JBUI.scale(8)))
                }
            }
        }

        commentsPanel.revalidate()
        commentsPanel.repaint()
    }

    private fun updateSingleComment(commentId: Int?) {
        if(commentId == null) return refreshAllComments()
        val comment = service.getCommentById(commentId) ?: return refreshAllComments()
        val panel = commentPanelMap[commentId] ?: return refreshAllComments()
        panel.bodyText = comment.body
    }

    private fun computeDisplayPaths(comments: List<Comment>): Map<Int, String> {
        val builder = UniqueNameBuilder<Comment>("", "/")
        comments.forEach { builder.addPath(it, it.relativePath) }
        return comments.associate { it.id to builder.getShortPath(it) }
    }

    private fun updateDeleteButtonState() {
        deleteButton.isEnabled = service.activeReview != null
    }
}
