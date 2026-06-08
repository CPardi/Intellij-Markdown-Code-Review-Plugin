package com.github.cpardi.markdowncodereview.startup

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.github.cpardi.markdowncodereview.markers.CommentRangeHighlighter
import com.github.cpardi.markdowncodereview.services.DocumentChangeListener
import com.github.cpardi.markdowncodereview.services.ReviewAsyncFileListener
import com.github.cpardi.markdowncodereview.services.ReviewService

/**
 * Startup activity that initializes the review system.
 * Registers all necessary listeners and initializes the service.
 */
class ReviewStartupActivity : ProjectActivity {

    private val LOG = thisLogger()

    override suspend fun execute(project: Project) {
        LOG.info("Initializing Review Markdown Generator for project: ${project.name}")

        // Initialize the service (this creates the instance)
        val service = ReviewService.getInstance(project)

        // Register document change listener
        val editorFactory = EditorFactory.getInstance()
        val documentChangeListener = DocumentChangeListener(project)
        editorFactory.eventMulticaster.addDocumentListener(documentChangeListener, project)

        // Register async file listener for rename/move tracking
        val virtualFileManager = VirtualFileManager.getInstance()
        val asyncFileListener = ReviewAsyncFileListener(project)
        virtualFileManager.addAsyncFileListenerBackgroundable(asyncFileListener, project)

        // Initialize range highlighter for comment background highlights
        CommentRangeHighlighter(project)

        LOG.info("Review Markdown Generator initialized successfully")
    }
}
