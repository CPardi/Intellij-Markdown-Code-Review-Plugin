package com.github.cpardi.intellijmarkdownreviewplugin.actions

import com.github.cpardi.intellijmarkdownreviewplugin.LightPlatformTest
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionEvent.createEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile

abstract class ContextMenuActionTest : LightPlatformTest() {

    protected lateinit var service: ReviewService
    protected val createdEditors = mutableListOf<Editor>()

    override fun setUp() {
        super.setUp()
        service = ReviewService.getInstance(project)
        service.setActiveReview(null)
    }

    /**
     * Creates a mock AnActionEvent with specified context.
     * Note: This helper creates editors that must be released by the caller.
     */
    protected fun createAnActionEvent(hasEditor: Boolean, hasFile: Boolean, virtualFile: VirtualFile? = null): AnActionEvent {
        val dataContext = DataContext { dataId ->
            return@DataContext when (dataId) {
                CommonDataKeys.PROJECT.name -> project
                CommonDataKeys.EDITOR.name -> if (hasEditor && virtualFile != null) {
                    val fileDoc = FileDocumentManager.getInstance().getDocument(virtualFile)
                    if (fileDoc == null) null
                    else {
                        val editor = EditorFactory.getInstance().createEditor(fileDoc, project, virtualFile.fileType, false)
                        createdEditors.add(editor)
                        editor
                    }
                } else null

                CommonDataKeys.VIRTUAL_FILE.name -> if (hasFile) virtualFile else null
                CommonDataKeys.VIRTUAL_FILE_ARRAY.name -> if (hasFile && virtualFile != null) arrayOf(virtualFile) else null
                else -> null
            }
        }

        return createEvent(dataContext, null, "test", ActionUiKind.NONE, null)
    }


    /**
     * Creates a mock AnActionEvent with project view context (file selection).
     */
    protected fun createAnActionEventFromProjectView(virtualFile: VirtualFile? = null): AnActionEvent {
        val dataContext = DataContext { dataId ->
            when (dataId) {
                CommonDataKeys.PROJECT.name -> project
                CommonDataKeys.EDITOR.name -> null // No editor in project view
                CommonDataKeys.VIRTUAL_FILE.name -> virtualFile
                CommonDataKeys.VIRTUAL_FILE_ARRAY.name -> if (virtualFile != null) arrayOf(virtualFile) else null
                else -> null
            }
        }

        return createEvent(dataContext, null, "test", ActionUiKind.NONE, null)
    }

    override fun tearDown() {
        invokeAndWaitIfNeeded {
            createdEditors.forEach { editor ->
                EditorFactory.getInstance().releaseEditor(editor)
            }
        }

        createdEditors.clear()
        super.tearDown()
    }
}
