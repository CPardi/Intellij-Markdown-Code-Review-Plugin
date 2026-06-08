package com.github.cpardi.markdowncodereview.actions

import com.github.cpardi.markdowncodereview.ReviewBundle
import com.github.cpardi.markdowncodereview.services.ReviewService
import com.github.cpardi.markdowncodereview.ui.NewPageCommentDialog
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile

/**
 * Action for "Add Page Comment" in the editor context menu or Project View context menu.
 * Creates a page comment for the entire file.
 * Auto-creates a review if none is active.
 */
class AddPageCommentAction : AnAction(
    ReviewBundle.message("addPageComment"),
    ReviewBundle.message("addPageComment"),
    AllIcons.FileTypes.Text
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = getVirtualFile(e) ?: return
        val service = ReviewService.getInstance(project)

        // Auto-create a review if none is active
        if (service.activeReview == null) {
            service.createNewReview().getOrShowError(project) ?: return
        }

        val relativePath = service.getRelativePath(virtualFile)
        NewPageCommentDialog.show(project, relativePath)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val virtualFile = getVirtualFile(e)

        e.presentation.isEnabled = project != null && virtualFile != null
    }

    private fun getVirtualFile(e: AnActionEvent): VirtualFile? {
        // Try to get from editor first
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null) {
            return e.getData(CommonDataKeys.VIRTUAL_FILE)
        }

        // Fall back to Project View selection
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        return if (files != null && files.size == 1) files[0] else null
    }
}
