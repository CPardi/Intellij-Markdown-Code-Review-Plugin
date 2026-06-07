package com.github.cpardi.intellijmarkdownreviewplugin.actions

import com.github.cpardi.intellijmarkdownreviewplugin.LightPlatformTest
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionEvent.createEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Integration tests for AddPageCommentAction.
 * Tests action visibility, enablement, and execution.
 */
@Suppress("JUnitMixedFramework")
class AddPageCommentActionTestSuite {

    @Nested
    inner class ActionUpdate : ContextMenuActionTest() {

        @Test
        fun `test action disabled when no project`() {
            // Given: An action event without a project
            val action = AddPageCommentAction()
            val event = createAnActionEvent(hasEditor = false, hasFile = true)

            // When: Updating action presentation
            action.update(event)

            // Then: Action should be disabled
            assertFalse(event.presentation.isEnabled, "Action should be disabled without project")
        }

        @Test
        fun `test action disabled when no virtual file`() {
            // Given: An action event without a virtual file
            val action = AddPageCommentAction()
            val event = createAnActionEvent(hasEditor = false, hasFile = false)

            // When: Updating action presentation
            action.update(event)

            // Then: Action should be disabled
            assertFalse(event.presentation.isEnabled, "Action should be disabled without virtual file")
        }

        @Test
        fun `test action enabled with project and virtual file from editor`() {
            // Given: A proper action context from editor
            val action = AddPageCommentAction()
            val file = createVirtualFile("test.kt", "fun main() {}")
            val event = createAnActionEvent(hasEditor = true, hasFile = true, virtualFile = file)

            // When: Updating action presentation
            invokeAndWaitIfNeeded {
                action.update(event)
            }

            // Then: Action should be enabled
            assertTrue(event.presentation.isEnabled, "Action should be enabled with project and file from editor")
        }

        @Test
        fun `test action enabled with project and virtual file from project_view`() {
            // Given: A proper action context from project view
            val action = AddPageCommentAction()
            val file = createVirtualFile("test.kt", "fun main() {}")
            val event = createAnActionEventFromProjectView(virtualFile = file)

            // When: Updating action presentation
            action.update(event)

            // Then: Action should be enabled
            assertTrue(event.presentation.isEnabled, "Action should be enabled with project and file from project view")
        }
    }

    @Nested
    inner class ActionExecution : ContextMenuActionTest() {

        @Test
        fun `test action auto-creates review when none exists`() {
            // Given: No active review
            Assertions.assertNull(service.activeReview)

            // And: A file with content
            val file = createVirtualFile("test.kt", "fun main() {}")

            // And: An action event
            val action = AddPageCommentAction()
            val event = createAnActionEvent(hasEditor = true, hasFile = true, virtualFile = file)

            // When: Performing the action
            invokeAndWaitIfNeeded {
                action.actionPerformed(event)
            }

            // Then: A new review should be created
            assertNotNull(service.activeReview, "Review should be auto-created")
        }

        @Test
        fun `test action uses existing review when active`() {
            // Given: An existing active review
            service.createNewReview()
            val reviewName = service.activeReview!!.name

            // And: A file with content
            val file = createVirtualFile("test.kt", "fun main() {}")

            // And: An action event
            val action = AddPageCommentAction()
            val event = createAnActionEvent(hasEditor = true, hasFile = true, virtualFile = file)

            // When: Performing the action
            invokeAndWaitIfNeeded {
                action.actionPerformed(event)
            }

            // Then: Same review should still be active
            assertTrue(service.activeReview!!.name == reviewName, "Same review should remain active")
        }

        @Test
        fun `test action gets virtual file from editor context`() {
            // Given: A file and editor context
            val file = createVirtualFile("test.kt", "fun main() {}")

            // And: An action event with editor context
            val action = AddPageCommentAction()
            val event = createAnActionEvent(hasEditor = true, hasFile = true, virtualFile = file)

            // When: Performing the action
            invokeAndWaitIfNeeded {
                action.actionPerformed(event)
            }

            // Then: Should succeed without error (getVirtualFile returns editor's file)
            // Verify no exception is thrown
        }

        @Test
        fun `test action gets virtual file from project_view context`() {
            // Given: A file and project view context
            val file = createVirtualFile("test.kt", "fun main() {}")

            // And: An action event with project view context
            val action = AddPageCommentAction()
            val event = createAnActionEventFromProjectView(virtualFile = file)

            // When: Performing the action
            invokeAndWaitIfNeeded {
                action.actionPerformed(event)
            }

            // Then: Should succeed without error (getVirtualFile returns from selection)
            // Verify no exception is thrown
        }
    }
}
