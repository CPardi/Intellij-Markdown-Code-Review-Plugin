package com.github.cpardi.intellijmarkdownreviewplugin.ui

import com.github.cpardi.intellijmarkdownreviewplugin.ReviewBundle
import com.github.cpardi.intellijmarkdownreviewplugin.services.Comment
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * Dialog for editing multiple comments on the same line.
 * Shows a scrollable list of comments, each with a text area and delete button.
 * A single Save button commits all changes; Cancel aborts all.
 */
class EditCommentsDialog(
    project: Project,
    comments: List<Comment>
) : DialogWrapper(project) {

    internal val service = ReviewService.getInstance(project)
    internal val commentItems = mutableListOf<CommentItem>()
    internal val deletedIds = mutableSetOf<Int>()
    internal val commentsPanel = JPanel()

    internal data class CommentItem(
        val comment: Comment,
        val textArea: JBTextArea,
        val originalBody: String,
        val panel: JPanel,
        val header: JLabel,
        val delete: JButton
    )

    init {
        // Use "Edit Page Comment" title if all comments are page comments, otherwise "Edit Comment"
        title = if (comments.all { it.isPageComment() }) {
            ReviewBundle.message("editPageComment")
        } else {
            ReviewBundle.message("editComment")
        }
        setOKButtonText(ReviewBundle.message("save"))
        setOKButtonMnemonic('S'.code)
        init()

        // Populate comments in init after dialog is created
        for (comment in comments) {
            commentItems.add(createCommentItem(comment))
        }
    }

    // Control navigation function
//
//    internal fun getTextAreas(): List<JBTextArea> = commentItems.map { item -> item.textArea }
//
//    internal fun getHeaders(): List<String> = commentItems.map { item -> item.header.text }
//
//    internal fun getDeleteButtons(): List<JButton> = commentItems.map { item -> item.delete }

    // User interaction handlers

    internal fun deleteComment(commentId: Int, itemPanel: JPanel) {
        // Delete the comment immediately
        service.deleteComment(commentId)
        deletedIds.add(commentId)

        // Remove from UI
        commentsPanel.remove(itemPanel)
        commentsPanel.revalidate()
        commentsPanel.repaint()

        // Close dialog if all comments are deleted
        if (deletedIds.size == commentItems.size) {
            close(OK_EXIT_CODE)
        }
    }

    internal fun validateComments(): com.intellij.openapi.ui.ValidationInfo? {
        for (item in commentItems) {
            if (item.comment.id !in deletedIds && item.textArea.text.contains("---")) {
                return com.intellij.openapi.ui.ValidationInfo(
                    ReviewBundle.message("delimiterError"),
                    item.textArea
                )
            }
        }
        return null
    }

    internal fun saveChanges() {
        // Apply edits for comments that weren't deleted
        for (item in commentItems) {
            if (item.comment.id !in deletedIds) {
                val newBody = item.textArea.text.trim()
                if (newBody != item.originalBody && !newBody.contains("---")) {
                    service.editComment(item.comment.id, newBody)
                }
            }
        }
    }

    // Overridden functions

    override fun createCenterPanel(): JComponent {
        commentsPanel.layout = BoxLayout(commentsPanel, BoxLayout.Y_AXIS)
        val scrollPane = JScrollPane(commentsPanel).apply {
            preferredSize = Dimension(800, 600)
            border = JBUI.Borders.empty()
        }
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10)
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    override fun getPreferredFocusedComponent(): JComponent? = commentItems.firstOrNull()?.textArea

    override fun doValidate(): com.intellij.openapi.ui.ValidationInfo? = validateComments()

    override fun doOKAction() {
        saveChanges()
        super.doOKAction()
    }

    // Helper functions

    private fun createCommentItem(comment: Comment): CommentItem {
        val textArea = JBTextArea().apply {
            text = comment.body
            rows = 4
            columns = 50
            lineWrap = true
            wrapStyleWord = true
            // Position caret at end of text for easier editing
            caretPosition = text.length
        }

        val fileName = comment.relativePath.substringAfterLast('/')
        val headerLabel = JBLabel(
            if (comment.isPageComment()) {
                ReviewBundle.message("pageCommentLocation", fileName)
            } else
            {
                "$fileName:${comment.getLineRangeText()}"
            }
        )

        val itemPanel = JPanel(BorderLayout(5, 5)).apply {
            border = JBUI.Borders.empty(5)
        }

        val deleteButton = JButton(ReviewBundle.message("delete")).apply {
            toolTipText = ReviewBundle.message("delete")
            addActionListener { deleteComment(comment.id, itemPanel) }
        }

        val headerPanel = JPanel(BorderLayout()).apply {
            add(headerLabel, BorderLayout.WEST)
            add(deleteButton, BorderLayout.EAST)
        }

        itemPanel.apply {
            add(headerPanel, BorderLayout.NORTH)
            add(JScrollPane(textArea), BorderLayout.CENTER)
        }

        commentsPanel.add(itemPanel)

        return CommentItem(comment, textArea, comment.body, itemPanel, headerLabel, deleteButton)
    }

    companion object {
        fun show(project: Project, comments: List<Comment>) {
            EditCommentsDialog(project, comments).show()
        }
    }
}
