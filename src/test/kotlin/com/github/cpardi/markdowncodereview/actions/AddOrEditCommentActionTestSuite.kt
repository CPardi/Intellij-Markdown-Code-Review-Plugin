package com.github.cpardi.markdowncodereview.actions

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Integration tests for AddOrEditCommentAction.
 * Tests action visibility, enablement, and conditional behavior.
 */
@Suppress("JUnitMixedFramework")
class AddOrEditCommentActionTestSuite {

    @Nested
    inner class ActionUpdate : ContextMenuActionTest() {

        @Test
        fun `action disabled when no project`() {
            // Given: An action event without a project
            val action = AddOrEditCommentAction()
            val event = createAnActionEvent(hasEditor = true, hasFile = true)

            // When: Updating action presentation
            action.update(event)

            // Then: Action should be disabled
            assertFalse(event.presentation.isEnabled, "Action should be disabled without project")
        }

        @Test
        fun `action disabled when no editor`() {
            // Given: An action event without an editor
            val action = AddOrEditCommentAction()
            val event = createAnActionEvent(hasEditor = false, hasFile = true)

            // When: Updating action presentation
            action.update(event)

            // Then: Action should be disabled
            assertFalse(event.presentation.isEnabled, "Action should be disabled without editor")
        }

        @Test
        fun `action disabled when no virtual file`() {
            // Given: An action event without a virtual file
            val action = AddOrEditCommentAction()
            val event = createAnActionEvent(hasEditor = true, hasFile = false)

            // When: Updating action presentation
            action.update(event)

            // Then: Action should be disabled
            assertFalse(event.presentation.isEnabled, "Action should be disabled without virtual file")
        }

        @Test
        fun `action enabled with all required context`() {
            // Given: A proper action context
            val action = AddOrEditCommentAction()
            val file = createVirtualFile("test.kt", "fun main() {}")
            val event = createAnActionEvent(hasEditor = true, hasFile = true, virtualFile = file)

            // When: Updating action presentation
            invokeAndWaitIfNeeded {
                action.update(event)
            }

            // Then: Action should be enabled
            assertTrue(event.presentation.isEnabled, "Action should be enabled with full context")
        }
    }

    @Nested
    inner class ActionExecution : ContextMenuActionTest() {

        @Test
        fun `action auto-creates review when none exists`() {
            // Given: No active review
            Assertions.assertNull(service.activeReview)

            // And: A file with content
            val file = createVirtualFile("test.kt", "fun main() {}")

            // And: An action event
            val action = AddOrEditCommentAction()
            val event = createAnActionEvent(hasEditor = true, hasFile = true, virtualFile = file)

            // When: Performing the action
            invokeAndWaitIfNeeded {
                action.actionPerformed(event)
            }

            // Then: A new review should be created
            assertNotNull(service.activeReview, "Review should be auto-created")
        }

        @Test
        fun `action uses existing review when active`() {
            // Given: An existing active review
            service.createNewReview()
            val reviewName = service.activeReview!!.name

            // And: A file with content
            val file = createVirtualFile("test.kt", "fun main() {}")

            // And: An action event
            val action = AddOrEditCommentAction()
            val event = createAnActionEvent(hasEditor = true, hasFile = true, virtualFile = file)

            // When: Performing the action
            invokeAndWaitIfNeeded {
                action.actionPerformed(event)
            }

            // Then: Same review should still be active
            assertTrue(service.activeReview!!.name == reviewName, "Same review should remain active")
        }
    }
}
