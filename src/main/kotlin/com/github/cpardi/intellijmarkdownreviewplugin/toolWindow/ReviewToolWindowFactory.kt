package com.github.cpardi.intellijmarkdownreviewplugin.toolWindow

import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.icons.AllIcons

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
