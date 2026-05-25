package com.github.cpardi.intellijmarkdownreviewplugin.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.github.cpardi.intellijmarkdownreviewplugin.ReviewBundle
import com.github.cpardi.intellijmarkdownreviewplugin.services.Comment
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

/**
 * Dialog for editing multiple comments on the same line.
 * Shows a scrollable list of comments, each with a text area and delete button.
 * A single Save button commits all changes; Cancel aborts all.
 */
class EditCommentsDialog(
    private val project: Project,
    comments: List<Comment>
) : DialogWrapper(project) {

    private val service = ReviewService.getInstance(project)
    private val commentItems = mutableListOf<CommentItem>()
    private val deletedIds = mutableSetOf<Int>()
    private val commentsPanel = JPanel()

    private data class CommentItem(
        val comment: Comment,
        val textArea: JBTextArea,
        val originalBody: String,
        val panel: JPanel
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

    override fun createCenterPanel(): JComponent {
        commentsPanel.layout = BoxLayout(commentsPanel, BoxLayout.Y_AXIS)

        // Comments are added in init()

        val scrollPane = JScrollPane(commentsPanel).apply {
            preferredSize = Dimension(800, 600)
            border = JBUI.Borders.empty()
        }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10)
            add(scrollPane, BorderLayout.CENTER)
        }
    }

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
            } else {
                "$fileName:${comment.getLineRangeText()}"
            }
        )

        val itemPanel = JPanel(BorderLayout(5, 5)).apply {
            border = JBUI.Borders.empty(5)
        }

        val deleteButton = JButton(ReviewBundle.message("delete")).apply {
            toolTipText = ReviewBundle.message("delete")
            addActionListener {
                // Delete the comment immediately
                service.deleteComment(comment.id)
                deletedIds.add(comment.id)
                
                // Remove from UI
                commentsPanel.remove(itemPanel)
                commentsPanel.revalidate()
                commentsPanel.repaint()
                
                // Close dialog if all comments are deleted
                if (deletedIds.size == commentItems.size) {
                    close(OK_EXIT_CODE)
                }
            }
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

        return CommentItem(comment, textArea, comment.body, itemPanel)
    }
    override fun getPreferredFocusedComponent(): JComponent? {
        return commentItems.firstOrNull()?.textArea
    }

    override fun doValidate(): com.intellij.openapi.ui.ValidationInfo? {
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

    override fun doOKAction() {
        // Apply edits for comments that weren't deleted
        for (item in commentItems) {
            if (item.comment.id !in deletedIds) {
                val newBody = item.textArea.text.trim()
                if (newBody != item.originalBody && !newBody.contains("---")) {
                    service.editComment(item.comment.id, newBody)
                }
            }
        }
        super.doOKAction()
    }

    companion object {
        fun show(project: Project, comments: List<Comment>) {
            EditCommentsDialog(project, comments).show()
        }
    }
}