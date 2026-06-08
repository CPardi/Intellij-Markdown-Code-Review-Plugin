package com.github.cpardi.markdowncodereview.ui

import com.github.cpardi.markdowncodereview.ReviewBundle
import com.intellij.openapi.project.Project

/**
 * Dialog for creating a new comment.
 * Extends BaseNewCommentDialog to provide line-based location text and comment creation.
 */
class NewCommentDialog(
    project: Project,
    private val relativePath: String,
    private val startLine: Int,
    private val endLine: Int
) : BaseNewCommentDialog(project) {

    init {
        initDialog()
    }

    override fun getLocationText(): String {
        val fileName = relativePath.substringAfterLast('/')
        return if (startLine == endLine) {
            ReviewBundle.message("commentLocation", fileName, startLine.toString())
        } else {
            ReviewBundle.message("commentLocation", fileName, "$startLine-$endLine")
        }
    }

    override fun doCreate(body: String) {
        service.addComment(relativePath, startLine, endLine, body)
    }

    companion object {
        fun show(
            project: Project,
            relativePath: String,
            startLine: Int,
            endLine: Int
        ) {
            NewCommentDialog(project, relativePath, startLine, endLine).show()
        }
    }
}
