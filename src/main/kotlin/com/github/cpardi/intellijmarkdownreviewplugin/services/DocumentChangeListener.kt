package com.github.cpardi.intellijmarkdownreviewplugin.services

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project

/**
 * Listens to document changes and updates comment line positions.
 * Does NOT write to disk on every change - only on explicit save/add/edit/delete.
 */
class DocumentChangeListener(private val project: Project) : BulkAwareDocumentListener {

    private val LOG = thisLogger()

    override fun documentChangedNonBulk(event: DocumentEvent) {
        val service = project.getService(ReviewService::class.java)
        val document = event.document
        
        // Get the file for this document
        val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
        
        // Update comment line numbers from RangeMarkers
        service.updateCommentLinesFromMarkers(document)
    }
}