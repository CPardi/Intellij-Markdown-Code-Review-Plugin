package com.github.cpardi.markdowncodereview.actions

import com.github.cpardi.markdowncodereview.ReviewBundle
import com.github.cpardi.markdowncodereview.services.ReviewService
import com.github.cpardi.markdowncodereview.ui.EditCommentsDialog
import com.github.cpardi.markdowncodereview.ui.NewCommentDialog
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/**
 * Action that adds or edits comments based on context.
 *
 * Behavior:
 * - If no active review: Auto-create a new review
 * - If active review and selection/line has no comments: Show NewCommentDialog
 * - If active review and selection/line has existing comments: Show EditCommentsDialog
 */
class AddOrEditCommentAction : AnAction(
    ReviewBundle.message("addOrEditComment"),
    ReviewBundle.message("addOrEditComment"),
    AllIcons.General.Balloon
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val service = ReviewService.getInstance(project)

        // Auto-create a review if none is active
        if (service.activeReview == null) {
            service.createNewReview().getOrShowError(project) ?: return
        }

        // Get the selection or current line
        val selectionModel = editor.selectionModel
        val document = editor.document

        val (startLine, endLine) = if (selectionModel.hasSelection()) {
            // Use selection range
            val startOffset = selectionModel.selectionStart
            val endOffset = selectionModel.selectionEnd
            Pair(
                document.getLineNumber(startOffset) + 1,
                document.getLineNumber(endOffset) + 1
            )
        } else {
            // Use current line
            val lineNumber = editor.caretModel.logicalPosition.line + 1
            Pair(lineNumber, lineNumber)
        }

        val relativePath = service.getRelativePath(virtualFile)
        val commentsOnLine = service.getCommentsForLine(relativePath, startLine)
            .filter { it.containsLine(endLine) || (startLine == endLine && it.containsLine(startLine)) }

        if (commentsOnLine.isEmpty()) {
            // Create new comment
            NewCommentDialog.show(project, relativePath, startLine, endLine)
        } else {
            // Edit existing comment(s)
            EditCommentsDialog.show(project, commentsOnLine)
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

        // Only enable if there's a project and editor
        e.presentation.isEnabled = project != null && editor != null && virtualFile != null
    }
}
