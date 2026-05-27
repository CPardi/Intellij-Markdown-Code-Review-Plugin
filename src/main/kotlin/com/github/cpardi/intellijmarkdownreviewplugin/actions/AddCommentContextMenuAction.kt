package com.github.cpardi.intellijmarkdownreviewplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.github.cpardi.intellijmarkdownreviewplugin.ui.NewCommentDialog

/**
 * Action for "Add Review Comment" in the editor context menu.
 * Creates a new comment based on the current selection or line.
 * Auto-creates a review if none is active.
 */
class AddCommentContextMenuAction : AnAction() {

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
        val document = editor.document
        val selectionModel = editor.selectionModel

        val (startLine, endLine) = if (selectionModel.hasSelection()) {
            val startOffset = selectionModel.selectionStart
            val endOffset = selectionModel.selectionEnd
            Pair(
                document.getLineNumber(startOffset) + 1,
                document.getLineNumber(endOffset) + 1
            )
        } else {
            val lineNumber = editor.caretModel.logicalPosition.line + 1
            Pair(lineNumber, lineNumber)
        }

        val relativePath = service.getRelativePath(virtualFile)
        NewCommentDialog.show(project, relativePath, startLine, endLine)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val service = project?.let { ReviewService.getInstance(it) }

        // Always show in context menu when there's a project and editor
        e.presentation.isEnabled = project != null && editor != null && virtualFile != null
    }
}
