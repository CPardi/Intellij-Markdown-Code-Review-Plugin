package com.github.cpardi.markdowncodereview.toolWindow

import com.github.cpardi.markdowncodereview.services.ReviewService
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for the Review Output tool window.
 */
class ReviewToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ReviewToolWindowPanel(project, ReviewService.getInstance(project))
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowInspection)
    }
}
