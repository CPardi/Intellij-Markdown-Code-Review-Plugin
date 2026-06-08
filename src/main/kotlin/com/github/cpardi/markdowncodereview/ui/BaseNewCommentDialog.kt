package com.github.cpardi.markdowncodereview.ui

import com.github.cpardi.markdowncodereview.ReviewBundle
import com.github.cpardi.markdowncodereview.services.ReviewService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane

/**
 * Base class for new comment dialogs.
 * Provides common functionality for creating new comments.
 */
abstract class BaseNewCommentDialog(
    project: Project,
) : DialogWrapper(project) {

    protected val textArea = JBTextArea()
    protected val service = ReviewService.getInstance(project)

    init {
        setOKButtonText(ReviewBundle.message("save"))
        setOKButtonMnemonic('S'.code)
    }

    /**
     * Returns the title for this dialog.
     * Subclasses can override to provide custom titles.
     */
    protected open fun getDialogTitle(): String = ReviewBundle.message("addComment")

    /**
     * Initializes the dialog with the proper title.
     * Must be called by subclasses in their init block after setting the title.
     */
    protected fun initDialog() {
        title = getDialogTitle()
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout(10, 10)).apply {
            border = JBUI.Borders.empty(10)

            // Location label - subclasses provide the text
            add(JLabel(getLocationText()), BorderLayout.NORTH)

            // Comment text area
            textArea.apply { rows = 10; columns = 50; lineWrap = true; wrapStyleWord = true; emptyText.text = ReviewBundle.message("enterComment") }
            add(JScrollPane(textArea), BorderLayout.CENTER)

            // Set preferred size
            preferredSize = Dimension(800, 300)
        }
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return textArea
    }

    override fun createSouthPanel(): JComponent {
        val panel = super.createSouthPanel()
        return panel
    }

    override fun doValidate(): com.intellij.openapi.ui.ValidationInfo? {
        val text = textArea.text
        if (text.contains("---")) {
            return com.intellij.openapi.ui.ValidationInfo(
                ReviewBundle.message("delimiterError"),
                textArea
            )
        }
        return null
    }

    override fun doOKAction() {
        val body = textArea.text.trim()
        if (body.isNotEmpty() && !body.contains("---")) {
            doCreate(body)
        }

        super.doOKAction()
    }

    /**
     * Provides the location text to display in the dialog.
     */
    protected abstract fun getLocationText(): String

    /**
     * Creates the comment in the service.
     */
    protected abstract fun doCreate(body: String)
}
