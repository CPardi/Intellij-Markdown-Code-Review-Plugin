package com.github.cpardi.intellijmarkdownreviewplugin.actions

import com.github.cpardi.intellijmarkdownreviewplugin.LightPlatformTest
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionEvent.createEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Integration tests for AddCommentContextMenuAction.
 * Tests action visibility, enablement, and execution with IntelliJ Platform components.
 */
@Suppress("JUnitMixedFramework")
abstract class AddCommentContextMenuActionTests : LightPlatformTest() {

    private lateinit var service: ReviewService

    override fun setUp() {
        super.setUp()
        service = ReviewService.getInstance(project)
        service.setActiveReview(null)
    }

    @Nested
    inner class ActionUpdate : AddCommentContextMenuActionTests() {

        @Test
        fun `test action disabled when no project`() {
            // Given: An action event without a project
            val action = AddCommentContextMenuAction()
            val event = createAnActionEvent(project = null, hasEditor = true, hasFile = true)

            // When: Updating action presentation
            action.update(event)

            // Then: Action should be disabled
            assertFalse(event.presentation.isEnabled, "Action should be disabled without project")
        }

        @Test
        fun `test action disabled when no editor`() {
            // Given: An action event without an editor
            val action = AddCommentContextMenuAction()
            val event = createAnActionEvent(project = project, hasEditor = false, hasFile = true)

            // When: Updating action presentation
            action.update(event)

            // Then: Action should be disabled
            assertFalse(event.presentation.isEnabled, "Action should be disabled without editor")
        }

        @Test
        fun `test action disabled when no virtual file`() {
            // Given: An action event without a virtual file
            val action = AddCommentContextMenuAction()
            val event = createAnActionEvent(project = project, hasEditor = true, hasFile = false)

            // When: Updating action presentation
            action.update(event)

            // Then: Action should be disabled
            assertFalse(event.presentation.isEnabled, "Action should be disabled without virtual file")
        }

        @Test
        fun `test action enabled with all required context`() {
            // Given: A proper action context
            val action = AddCommentContextMenuAction()
            val file = createVirtualFile("test.kt", "fun main() {}")
            val event = createAnActionEvent(project = project, hasEditor = true, hasFile = true, virtualFile = file)

            // When: Updating action presentation
            action.update(event)

            // Then: Action should be enabled
            assertTrue(event.presentation.isEnabled, "Action should be enabled with full context")
        }
    }

    @Nested
    inner class ActionExecution : AddCommentContextMenuActionTests() {

        @Test
        fun `test action auto-creates review when none exists`() {
            // Given: No active review
            assertNull(service.activeReview)

            // And: A file with content
            val file = createVirtualFile("test.kt", "fun main() {}")

            // And: An action event with proper context
            val action = AddCommentContextMenuAction()
            val event = createAnActionEvent(
                project = project,
                hasEditor = true,
                hasFile = true,
                virtualFile = file,
                selectionStart = 0,
                selectionEnd = 0,
                caretLine = 0
            )

            // When: Performing the action
            action.actionPerformed(event)

            // Then: A new review should be created
            // Note: We can't verify dialog was shown, but we can verify review was created
            assertNotNull(service.activeReview, "Review should be auto-created")
        }

        @Test
        fun `test action uses existing review when active`() {
            // Given: An existing active review
            val result = service.createNewReview()
            assertNotNull(result)
            val reviewName = service.activeReview!!.name

            // And: A file with content
            val file = createVirtualFile("test.kt", "fun main() {}")

            // And: An action event
            val action = AddCommentContextMenuAction()
            val event = createAnActionEvent(
                project = project,
                hasEditor = true,
                hasFile = true,
                virtualFile = file,
                selectionStart = 0,
                selectionEnd = 0,
                caretLine = 0
            )

            // When: Performing the action
            action.actionPerformed(event)

            // Then: Same review should still be active
            assertEquals(reviewName, service.activeReview!!.name, "Same review should remain active")
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a mock AnActionEvent with specified context.
     */
    private fun createAnActionEvent(
        project: com.intellij.openapi.project.Project?,
        hasEditor: Boolean,
        hasFile: Boolean,
        virtualFile: com.intellij.openapi.vfs.VirtualFile? = null,
        selectionStart: Int = 0,
        selectionEnd: Int = 0,
        caretLine: Int = 0
    ): AnActionEvent {
        val dataContext = com.intellij.openapi.actionSystem.DataContext { dataId ->
            when (dataId) {
                CommonDataKeys.PROJECT.name -> project
                CommonDataKeys.EDITOR.name -> if (hasEditor && virtualFile != null) {
                    val fileDoc = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(virtualFile)
                    if (fileDoc != null) {
                        com.intellij.openapi.editor.EditorFactory.getInstance().createEditor(fileDoc, project, virtualFile.fileType, false)
                    } else null
                } else null
                CommonDataKeys.VIRTUAL_FILE.name -> if (hasFile) virtualFile else null
                CommonDataKeys.VIRTUAL_FILE_ARRAY.name -> if (hasFile && virtualFile != null) arrayOf(virtualFile) else null
                else -> null
            }
        }


        return createEvent(dataContext, null, "test", ActionUiKind.NONE, null);
    }
}
