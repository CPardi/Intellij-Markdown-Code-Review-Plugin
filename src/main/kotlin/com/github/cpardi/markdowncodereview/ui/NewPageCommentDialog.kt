package com.github.cpardi.markdowncodereview.ui

import com.github.cpardi.markdowncodereview.ReviewBundle
import com.intellij.openapi.project.Project

/**
 * Dialog for creating a page comment (comment for entire file).
 * Extends BaseNewCommentDialog to provide file-only location text and page comment creation.
 */
class NewPageCommentDialog(
    project: Project,
    private val relativePath: String
) : BaseNewCommentDialog(project) {

    init {
        initDialog()
    }

    override fun getDialogTitle(): String {
        return ReviewBundle.message("addPageComment")
    }

    override fun getLocationText(): String {
        return ReviewBundle.message("pageCommentLocation", relativePath)
    }

    override fun doCreate(body: String) {
        service.addPageComment(relativePath, body)
    }

    companion object {
        fun show(project: Project, relativePath: String) {
            NewPageCommentDialog(project, relativePath).show()
        }
    }
}
