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
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * Panel for the Review Output tool window using Kotlin UI DSL patterns.
 * Shows review selector dropdown, add/delete buttons, and the comments list.
 */
class ReviewToolWindowPanel(private val project: Project) : JBPanel<ReviewToolWindowPanel>(BorderLayout()) {

    private val service = ReviewService.getInstance(project)
    private val reviewComboBox = ComboBox<String>()
    private val commentsPanel = JPanel().apply { border = JBUI.Borders.empty(10) }
    private val commentsScrollPane = JScrollPane(commentsPanel)
    private var isUpdatingSelection = false
    private val deleteButton = JButton("Delete")

    init {
        setupTopBar()
        setupCommentsList()
        refreshReviewList()
        refreshCommentsList()

        // Subscribe to change notifications
        project.messageBus.connect().subscribe(
            ReviewService.REVIEW_CHANGE_TOPIC,
            object : ReviewChangeListener {
                override fun onCommentsChanged() {
                    ApplicationManager.getApplication().invokeLater {
                        refreshCommentsList()
                    }
                }

                override fun onReviewChanged() {
                    ApplicationManager.getApplication().invokeLater {
                        refreshReviewList()
                        refreshCommentsList()
                    }
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
                        refreshCommentsList()
                    }
                }
            }
        )
    }

    private fun setupTopBar() {
        val topPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
            border = JBUI.Borders.customLineBottom(JBUI.CurrentTheme.ToolWindow.borderColor())

            // Review selector dropdown
            add(JBLabel("Review:"))
            reviewComboBox.addActionListener { onReviewSelected() }
            add(reviewComboBox)

            // Add button
            val addButton = JButton("Add").apply {
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

    private fun setupCommentsList() {
        commentsPanel.layout = BoxLayout(commentsPanel, BoxLayout.Y_AXIS)
        commentsScrollPane.border = JBUI.Borders.empty()

        // Set scroll speed to match the file editor
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val editorUnitIncrement = editor?.lineHeight ?: 16
        commentsScrollPane.verticalScrollBar.unitIncrement = editorUnitIncrement

        add(commentsScrollPane, BorderLayout.CENTER)
    }

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

    private fun refreshCommentsList() {
        commentsPanel.removeAll()

        val review = service.activeReview
        if (review == null || review.isEmpty()) {
            commentsPanel.add(JBLabel(ReviewBundle.message("noComments")).apply {
                border = JBUI.Borders.empty(10)
            })
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
                commentsPanel.add(createCommentItem(comment, displayPaths[comment.id]!!))
                // Add spacing between bubbles (but not after the last one)
                if (index < sortedComments.lastIndex) {
                    commentsPanel.add(Box.createVerticalStrut(JBUI.scale(8)))
                }
            }
        }

        commentsPanel.revalidate()
        commentsPanel.repaint()
    }

    private fun createCommentItem(comment: Comment, displayPath: String): JPanel {
        return CommentBubblePanel().apply {
            border = JBUI.Borders.empty(10, 12, 10, 12)

            // Header with file path and line range (or "Page" for page comments)
            val headerText = if (comment.isPageComment()) {
                "$displayPath:${comment.getLineRangeText()} [Page]"
            } else {
                "$displayPath:${comment.getLineRangeText()}:${comment.getLineRangeText()}"
            }

            val header = JBLabel(headerText).apply {
                toolTipText = "Click to navigate"
                cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        navigateToComment(comment)
                    }
                })
            }

            // Comment body text field
            val bodyField = JBTextArea(comment.body).apply {
                lineWrap = true
                wrapStyleWord = true
                border = JBUI.Borders.empty(5)
                background = CommentBubblePanel.getBubbleBackground()
            }

            // Wrap in panel with minimum height and border
            val fontMetrics = bodyField.getFontMetrics(bodyField.font)
            val minHeight = fontMetrics.height * 3
            val defaultBorderColor = CommentBubblePanel.getUnfocusedBorderColor()
            val bodyPanel = object : JBPanel<JBPanel<*>>(BorderLayout()) {
                override fun getPreferredSize(): java.awt.Dimension {
                    val size = super.getPreferredSize()
                    return java.awt.Dimension(size.width, maxOf(size.height, minHeight))
                }}.apply {
                border = JBUI.Borders.customLine(defaultBorderColor)
                add(bodyField, BorderLayout.CENTER)
            }

            // Change border on focus
            bodyField.addFocusListener(object : java.awt.event.FocusAdapter() {
                override fun focusGained(e: java.awt.event.FocusEvent?) {
                    bodyPanel.border = JBUI.Borders.customLine(JBUI.CurrentTheme.Focus.focusColor())
                }

                override fun focusLost(e: java.awt.event.FocusEvent?) {
                    bodyPanel.border = JBUI.Borders.customLine(defaultBorderColor)
                }
            })

            // Delete button
            val deleteButton = JButton("Delete").apply {
                toolTipText = "Delete comment"
                addActionListener { onDeleteComment(comment.id) }
                isOpaque = false
            }

            // Button panel
            val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
                add(deleteButton)
                isOpaque = false
            }

            // Layout
            val topPanel = JPanel(BorderLayout()).apply {
                add(header, BorderLayout.WEST)
                add(buttonPanel, BorderLayout.EAST)
            }

            add(topPanel, BorderLayout.NORTH)
            add(bodyPanel, BorderLayout.CENTER)

            // Save body on focus lost
            bodyField.addFocusListener(object : java.awt.event.FocusAdapter() {
                override fun focusLost(e: java.awt.event.FocusEvent?) {
                    val newBody = bodyField.text
                    if (newBody != comment.body && !newBody.contains("---")) {
                        service.editComment(comment.id, newBody)
                    }
                }
            })
        }
    }

    private fun computeDisplayPaths(comments: List<Comment>): Map<Int, String> {
        val builder = UniqueNameBuilder<Comment>("", "/")
        comments.forEach { builder.addPath(it, it.relativePath) }
        return comments.associate { it.id to builder.getShortPath(it) }
    }

    private fun navigateToComment(comment: Comment) {
        val baseDir = com.intellij.openapi.vfs.VirtualFileManager.getInstance()
            .findFileByNioPath(java.nio.file.Path.of(project.basePath ?: return)) ?: return
        val file = com.intellij.openapi.vfs.VfsUtil.findRelativeFile(
            baseDir, *comment.relativePath.split("/").toTypedArray()
        ) ?: return

        val fileEditorManager = FileEditorManager.getInstance(project)
        fileEditorManager.openFile(file, true)

        // Navigate to the specific line for line comments (not for page comments)
        if (!comment.isPageComment()) {
            val editor = fileEditorManager.selectedTextEditor
            if (editor != null) {
                val line = comment.startLine - 1 // 0-based
                if (line >= 0 && line < editor.document.lineCount) {
                    editor.caretModel.moveToLogicalPosition(
                        com.intellij.openapi.editor.LogicalPosition(line, 0)
                    )
                    editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
                }
            }
        }
    }

    private fun onReviewSelected() {
        if (isUpdatingSelection) return
        
        val selectedName = reviewComboBox.selectedItem as? String ?: return
        if (selectedName == (service.activeReview?.name ?: ReviewService.NONE_SENTINEL)) return
        
        service.setActiveReview(selectedName)
        updateDeleteButtonState()
    }

    private fun onCreateNewReview() {
        service.createNewReview().getOrShowError(project) ?: return
        refreshReviewList()
        refreshCommentsList()
    }

    private fun onDeleteReview() {
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
            refreshCommentsList()
        }
    }

    private fun onDeleteComment(commentId: Int) {
        service.deleteComment(commentId)
        refreshCommentsList()
    }

    /**
     * Updates the enabled state of the delete button based on active review.
     * Disables the button when <None> is selected.
     */
    private fun updateDeleteButtonState() {
        deleteButton.isEnabled = service.activeReview != null
    }
}