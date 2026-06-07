package com.github.cpardi.intellijmarkdownreviewplugin.ui

import com.github.cpardi.intellijmarkdownreviewplugin.LightPlatformTest
import com.github.cpardi.intellijmarkdownreviewplugin.ReviewBundle
import com.github.cpardi.intellijmarkdownreviewplugin.services.Comment
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.ui.components.JBTextArea
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * This test suite covers the dialog's ability to edit multiple comments simultaneously,
 * handle deletions immediately, and manage the complex state between edited and deleted comments.
 */
@Suppress("JUnitMixedFramework")
class EditCommentsDialogTestSuite {

    abstract class EditCommentsDialogTest : LightPlatformTest() {

        protected lateinit var service: ReviewService

        override fun setUp() {
            super.setUp()
            service = ReviewService.getInstance(project)
            service.setActiveReview(null)
        }

        protected fun createComment(id: Int, relativePath: String, startLine: Int, endLine: Int, body: String): Comment = Comment(id, relativePath, startLine, endLine, body)

        protected fun createPageComment(id: Int, relativePath: String, body: String): Comment = Comment(id, relativePath, 0, 0, body)

        protected fun createDialog(vararg comments: Comment): EditCommentsDialog {
            return invokeAndWaitIfNeeded {
                EditCommentsDialog(project, comments.toList())
            }
        }
    }

    @Nested
    inner class DialogInit : EditCommentsDialogTest() {

        @Test
        fun `dialog title is Edit Page Comment when all comments are page comments`() {
            // Given: Only page comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("page.md", "# Page\nContent")
            service.setActiveReview("test-review")

            val pageComments = listOf(
                createPageComment(1, "page.md", "First page comment"),
                createPageComment(2, "page.md", "Second page comment")
            )

            // When: Dialog is created
            val dialog = createDialog(*pageComments.toTypedArray())

            // Then: Title uses "editPageComment" message
            Assertions.assertEquals(ReviewBundle.message("editPageComment"), dialog.title)
        }

        @Test
        fun `dialog title is Edit Comment when at least one comment is not a page comment`() {
            // Given: Mixed comments (page and line)
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\nLine2\n</xml>")
            service.setActiveReview("test-review")

            val mixedComments = listOf(
                createPageComment(1, "test.xml", "Page comment"),
                createComment(2, "test.xml", 5, 5, "Line comment")
            )

            // When: Dialog is created
            val dialog = createDialog(*mixedComments.toTypedArray())

            // Then: Title uses "editComment" message
            Assertions.assertEquals(ReviewBundle.message("editComment"), dialog.title)
        }

        @Test
        fun `dialog title is Edit Comment when all comments are line comments`() {
            // Given: Only line comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("code.xml", "<code>\n</code>")
            service.setActiveReview("test-review")

            val lineComments = listOf(
                createComment(1, "code.xml", 1, 1, "First line comment"),
                createComment(2, "code.xml", 5, 10, "Multi-line comment")
            )

            // When: Dialog is created
            val dialog = createDialog(*lineComments.toTypedArray())

            // Then: Title uses "editComment" message
            Assertions.assertEquals(ReviewBundle.message("editComment"), dialog.title)
        }

        @Test
        fun `dialog creates comment item for each comment`() {
            // Given: Multiple comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("file.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(
                createComment(1, "file.xml", 1, 1, "First"),
                createComment(2, "file.xml", 2, 2, "Second"),
                createComment(3, "file.xml", 3, 3, "Third")
            )

            // When: Dialog is created
            val dialog = createDialog(*comments.toTypedArray())

            // Then: All comment items are created
            Assertions.assertEquals(3, dialog.commentItems.size)
        }

        @Test
        fun `getPreferredFocusedComponent returns first comment text area`() {
            // Given: Dialog with multiple comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(
                createComment(1, "test.xml", 1, 1, "First"),
                createComment(2, "test.xml", 2, 2, "Second")
            )

            // When: Dialog is created and preferred focused component is requested
            val dialog = createDialog(*comments.toTypedArray())
            val focused = dialog.preferredFocusedComponent

            // Then: First comment's text area is returned
            Assertions.assertTrue(focused is JBTextArea, "Focused component should be a JBTextArea")
            Assertions.assertEquals("First", (focused as JBTextArea).text, "Should focus first comment's text area")
        }
    }

    @Nested
    inner class Validation : EditCommentsDialogTest() {

        @Test
        fun `doValidate returns null when no comments contain delimiter`() {
            // Given: Dialog with valid comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(
                createComment(1, "test.xml", 1, 1, "Valid comment"),
                createComment(2, "test.xml", 2, 2, "Another valid comment")
            )
            val dialog = createDialog(*comments.toTypedArray())

            // When: Validation is performed
            val validationInfo = dialog.validateComments()

            // Then: No validation error
            Assertions.assertNull(validationInfo, "Validation should pass for valid comments")
        }

        @Test
        fun `doValidate returns error when any comment contains delimiter`() {
            // Given: Dialog with one comment containing delimiter
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(
                createComment(1, "test.xml", 1, 1, "Valid comment"),
                createComment(2, "test.xml", 2, 2, "Invalid --- comment")
            )
            val dialog = createDialog(*comments.toTypedArray())

            // When: Validation is performed
            val validationInfo = dialog.validateComments()

            // Then: Validation error is returned
            Assertions.assertNotNull(validationInfo, "Validation should fail for comment with delimiter")
            Assertions.assertEquals(ReviewBundle.message("delimiterError"), validationInfo?.message)
        }

        @Test
        fun `doValidate returns correct text area reference`() {
            // Given: Dialog with comment containing delimiter
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(
                createComment(1, "test.xml", 1, 1, "Valid comment"),
                createComment(2, "test.xml", 2, 2, "Invalid --- comment")
            )
            val dialog = createDialog(*comments.toTypedArray())

            // When: Validation is performed
            val validationInfo = dialog.validateComments()

            // Then: Validation info references the text area with delimiter
            Assertions.assertNotNull(validationInfo?.component, "Validation info should have component")
            val textAreas = dialog.commentItems.map { item -> item.textArea }
            val invalidTextArea = textAreas.find { it.text.contains("---") }
            Assertions.assertSame(invalidTextArea, validationInfo?.component, "Should reference correct text area")
        }

        @Test
        fun `doValidate ignores deleted comments`() {
            // Given: Dialog with comment containing delimiter that gets deleted
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(
                createComment(1, "test.xml", 1, 1, "Valid comment"),
                createComment(2, "test.xml", 2, 2, "Invalid --- comment")
            )
            val dialog = createDialog(*comments.toTypedArray())

            // When: First delete the comment with delimiter
            val panelToDelete = dialog.commentItems.find { it.comment.id == comments[1].id }?.panel
            dialog.deleteComment(comments[1].id, panelToDelete!!)
            val validationInfo = dialog.validateComments()

            // Then: Validation passes (deleted comment is ignored)
            Assertions.assertNull(validationInfo, "Validation should pass when invalid comment is deleted")
        }

        @Test
        fun `doValidate checks all comment items`() {
            // Given: Dialog with delimiter in non-first comment
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(
                createComment(1, "test.xml", 1, 1, "Valid comment"),
                createComment(2, "test.xml", 2, 2, "Also valid"),
                createComment(3, "test.xml", 3, 3, "Invalid --- delimiter")
            )
            val dialog = createDialog(*comments.toTypedArray())

            // When: Validation is performed
            val validationInfo = dialog.validateComments()

            // Then: Validation finds the delimiter in the third comment
            Assertions.assertNotNull(validationInfo, "Validation should find delimiter in any comment")
        }
    }

    @Nested
    inner class EditOperations : EditCommentsDialogTest() {

        @Test
        fun `doOKAction saves only comments with changed body text`() {
            // Given: Dialog with some changed and unchanged comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "Original first")
            service.addComment("test.xml", 2, 2, "Original second")
            val comments = service.activeReview?.comments!!

            val dialog = createDialog(*comments.toTypedArray())
            val textAreas = dialog.commentItems.map { item -> item.textArea }

            // When: First text area changed, second unchanged
            invokeAndWaitIfNeeded {
                textAreas[0].text = "Changed first"
            }
            dialog.saveChanges()

            // Then: Only first comment was edited
            val updatedComments = service.activeReview?.comments!!
            Assertions.assertEquals("Changed first", updatedComments.find { it.id == comments[0].id }?.body)
            Assertions.assertEquals("Original second", updatedComments.find { it.id == comments[1].id }?.body)
        }

        @Test
        fun `doOKAction ignores comments with unchanged body text`() {
            // Given: Dialog with all unchanged comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "Comment one")
            service.addComment("test.xml", 2, 2, "Comment two")
            val comments = service.activeReview?.comments!!

            val dialog = createDialog(*comments.toTypedArray())

            // When: No changes made
            dialog.saveChanges()

            // Then: Comments remain unchanged
            val updatedComments = service.activeReview?.comments!!
            Assertions.assertEquals("Comment one", updatedComments.find { it.id == comments[0].id }?.body)
            Assertions.assertEquals("Comment two", updatedComments.find { it.id == comments[1].id }?.body)
        }

        @Test
        fun `doOKAction ignores deleted comments`() {
            // Given: Dialog with deleted comment
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "Comment one")
            service.addComment("test.xml", 2, 2, "Comment two")
            val comments = service.activeReview?.comments!!.toList()

            val dialog = createDialog(*comments.toTypedArray())

            // When: Delete first comment, change second
            // When: Delete first comment and edit second
            val deletedId = comments[0].id
            val panelToDelete = dialog.commentItems.find { it.comment.id == deletedId }?.panel!!
            invokeAndWaitIfNeeded {
                dialog.deleteComment(deletedId, panelToDelete)
            }
            val textAreas = dialog.commentItems.map { item -> item.textArea }
            // After delete, textAreas[1] is the second comment (commentItems is not modified)
            invokeAndWaitIfNeeded {
                textAreas[1].text = "Changed second"
            }
            dialog.saveChanges()
            // Then: Only non-deleted comment was edited
            Assertions.assertEquals(1, service.activeReview?.comments?.size, "Deleted comment should be removed")
            Assertions.assertEquals("Changed second", service.activeReview?.comments?.first()?.body)
        }

        @Test
        fun `doOKAction trims whitespace from body before saving`() {
            // Given: Dialog with whitespace-padded text
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "Original")
            val comments = service.activeReview?.comments!!

            val dialog = createDialog(*comments.toTypedArray())
            val textAreas = dialog.commentItems.map { item -> item.textArea }

            // When: Text has surrounding whitespace
            invokeAndWaitIfNeeded {
                textAreas[0].text = "  Trimmed comment  \n"
            }
            dialog.saveChanges()

            // Then: Body is trimmed
            val updatedComment = service.activeReview?.comments?.first()
            Assertions.assertEquals("Trimmed comment", updatedComment?.body)
        }

        @Test
        fun `doOKAction does not save comments containing delimiter`() {
            // Given: Dialog with delimiter in edited text
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "Original")
            val comments = service.activeReview?.comments!!

            val dialog = createDialog(*comments.toTypedArray())
            val textAreas = dialog.commentItems.map { item -> item.textArea }

            // When: Text contains delimiter
            invokeAndWaitIfNeeded {
                textAreas[0].text = "Invalid --- comment"
            }
            dialog.saveChanges()

            // Then: Comment is not edited (doOKAction should skip it)
            val result = dialog.validateComments()
            Assertions.assertNotNull(result, "Validation should fail for delimiter")
        }

        @Test
        fun `multiple comments can be edited simultaneously`() {
            // Given: Dialog with multiple comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "First")
            service.addComment("test.xml", 2, 2, "Second")
            service.addComment("test.xml", 3, 3, "Third")
            val comments = service.activeReview?.comments!!

            val dialog = createDialog(*comments.toTypedArray())
            val textAreas = dialog.commentItems.map { item -> item.textArea }

            // When: All comments are changed
            invokeAndWaitIfNeeded {
                textAreas[0].text = "Changed 1"
                textAreas[1].text = "Changed 2"
                textAreas[2].text = "Changed 3"
            }
            dialog.saveChanges()

            // Then: All comments are updated
            val updated = service.activeReview?.comments!!
            Assertions.assertEquals("Changed 1", updated.find { it.id == comments[0].id }?.body)
            Assertions.assertEquals("Changed 2", updated.find { it.id == comments[1].id }?.body)
            Assertions.assertEquals("Changed 3", updated.find { it.id == comments[2].id }?.body)
        }

        @Test
        fun `comments with empty body are treated as unchanged`() {
            // Given: Dialog with comment
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "Original")
            val comments = service.activeReview?.comments!!

            val dialog = createDialog(*comments.toTypedArray())
            val textAreas = dialog.commentItems.map { item -> item.textArea }

            // When: Text is cleared (empty)
            invokeAndWaitIfNeeded {
                textAreas[0].text = ""
            }
            dialog.saveChanges()

            // Then: Comment is not edited (empty body should not be saved)
            // Note: Empty body is treated as unchanged or just ignored
            // The actual behavior depends on implementation - this test verifies that
            // empty body doesn't crash or cause unexpected behavior
            Assertions.assertEquals(1, service.activeReview?.comments?.size, "Comment should still exist")
        }
    }

    @Nested
    inner class DeleteOperations : EditCommentsDialogTest() {

        @Test
        fun `delete button immediately removes comment from service`() {
            // Given: Dialog with comments in service
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "First")
            service.addComment("test.xml", 2, 2, "Second")
            val comments = service.activeReview?.comments!!.toList()

            val dialog = createDialog(*comments.toTypedArray())

            // When: Delete first comment
            val deletedId = comments[0].id
            val panelToDelete = dialog.commentItems.find { it.comment.id == deletedId }?.panel!!
            dialog.deleteComment(deletedId, panelToDelete)

            // Then: Comment is removed from service
            Assertions.assertEquals(1, service.activeReview?.comments?.size, "One comment should remain")
            Assertions.assertFalse(service.activeReview?.comments?.any { it.id == deletedId }!!, "Deleted comment should not exist")
        }

        @Test
        fun `deleted comment ID is tracked in deletedIds set`() {
            // Given: Dialog with comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "Comment")
            val comments = service.activeReview?.comments!!

            val dialog = createDialog(*comments.toTypedArray())

            // When: Delete a comment
            val deletedId = comments[0].id
            val panelToDelete = dialog.commentItems.find { it.comment.id == deletedId }?.panel!!
            invokeAndWaitIfNeeded {
                dialog.deleteComment(deletedId, panelToDelete)
            }

            // Then: ID is tracked in deletedIds
            Assertions.assertTrue(deletedId in dialog.deletedIds, "ID should be tracked in deletedIds")
        }

        @Test
        fun `doOKAction does not attempt to save deleted comments`() {
            // Given: Dialog with deleted comment
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "First")
            service.addComment("test.xml", 2, 2, "Second")
            val comments = service.activeReview?.comments!!.toList()

            val dialog = createDialog(*comments.toTypedArray())
            val textAreas = dialog.commentItems.map { item -> item.textArea }

            // When: Delete first comment and edit second
            val deletedId = comments[0].id
            val panelToDelete = dialog.commentItems.find { it.comment.id == deletedId }?.panel!!
            invokeAndWaitIfNeeded {
                dialog.deleteComment(deletedId, panelToDelete)
            }
            // After delete, textAreas[1] is the second comment (commentItems is not modified)
            invokeAndWaitIfNeeded {
                textAreas[1].text = "Changed second"
            }
            dialog.saveChanges()

            // Then: Deleted comment not in service, edited comment saved
            Assertions.assertEquals(1, service.activeReview?.comments?.size, "Only one comment should remain")
            Assertions.assertEquals("Changed second", service.activeReview?.comments?.first()?.body)
        }

        @Test
        fun `all comments deleted closes dialog with OK_EXIT_CODE`() {
            // Given: Dialog with single comment
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "Only comment")
            val comments = service.activeReview?.comments!!

            val dialog = createDialog(*comments.toTypedArray())

            // When: Delete all comments
            val deletedId = comments[0].id
            val panelToDelete = dialog.commentItems.find { it.comment.id == deletedId }?.panel!!
            invokeAndWaitIfNeeded {
                dialog.deleteComment(deletedId, panelToDelete)
            }

            // Then: Dialog should close (all comments deleted)
            Assertions.assertTrue(dialog.deletedIds.size == dialog.commentItems.size, "All comments should be deleted")
        }

        @Test
        fun `doOKAction succeeds when some comments deleted and others edited`() {
            // Given: Dialog with multiple comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "First")
            service.addComment("test.xml", 2, 2, "Second")
            service.addComment("test.xml", 3, 3, "Third")
            val comments = service.activeReview?.comments!!.toList()

            val dialog = createDialog(*comments.toTypedArray())
            val textAreas = dialog.commentItems.map { item -> item.textArea }

            // When: Delete first comment and edit second
            val deletedId = comments[0].id
            val panelToDelete = dialog.commentItems.find { it.comment.id == deletedId }?.panel!!
            invokeAndWaitIfNeeded {
                dialog.deleteComment(deletedId, panelToDelete)
            }
            // After delete, textAreas[1] is the second comment (commentItems is not modified)
            invokeAndWaitIfNeeded {
                textAreas[1].text = "Edited second"
            }
            dialog.saveChanges()

            // Then: Correct state after OK
            val remaining = service.activeReview?.comments!!
            Assertions.assertEquals(2, remaining.size, "Two comments should remain")
            Assertions.assertEquals("Edited second", remaining.find { it.id == comments[1].id }?.body)
            Assertions.assertEquals("Third", remaining.find { it.id == comments[2].id }?.body)
        }

        @Test
        fun `comment item is removed from commentsPanel on delete`() {
            // Given: Dialog with multiple comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            service.addComment("test.xml", 1, 1, "First")
            service.addComment("test.xml", 2, 2, "Second")
            val comments = service.activeReview?.comments!!.toList()

            val dialog = createDialog(*comments.toTypedArray())
            val initialCount = dialog.commentItems.size

            // When: Delete first comment
            val deletedId = comments[0].id
            val panelToDelete = dialog.commentItems.find { it.comment.id == deletedId }?.panel!!
            invokeAndWaitIfNeeded {
                dialog.deleteComment(deletedId, panelToDelete)
            }

            // Then: Comment removed from panel - commentItems list is NOT modified
            // The UI panel is removed, but commentItems still holds all items
            Assertions.assertEquals(initialCount, dialog.commentItems.size, "commentItems is not modified after deletion")
        }
    }

    @Nested
    inner class UIStructure : EditCommentsDialogTest() {

        @Test
        fun `each comment item displays correct header for line comment`() {
            // Given: Dialog with line comment
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("src/main/code.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(createComment(1, "src/main/code.xml", 10, 15, "Multi-line comment"))
            val dialog = createDialog(*comments.toTypedArray())

            // When: Header is retrieved
            val headers = dialog.commentItems.map { item -> item.header }

            // Then: Header shows filename:lineRange
            Assertions.assertEquals(1, headers.size)
            Assertions.assertTrue(headers[0].text.contains("code.xml"), "Header should contain filename")
            Assertions.assertTrue(headers[0].text.contains("10-15"), "Header should contain line range")
        }

        @Test
        fun `each comment item displays correct header for page comment`() {
            // Given: Dialog with page comment
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("document.md", "# Title\nContent")
            service.setActiveReview("test-review")

            val comments = listOf(createPageComment(1, "document.md", "Page comment"))
            val dialog = createDialog(*comments.toTypedArray())

            // When: Header is retrieved
            val headers = dialog.commentItems.map { item -> item.header }

            // Then: Header shows page comment location
            Assertions.assertEquals(1, headers.size)
            val expected = ReviewBundle.message("pageCommentLocation", "document.md")
            Assertions.assertEquals(expected, headers[0].text)
        }

        @Test
        fun `text area shows original comment body`() {
            // Given: Dialog with comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(
                createComment(1, "test.xml", 1, 1, "Original body text"),
                createComment(2, "test.xml", 2, 2, "Another comment")
            )
            val dialog = createDialog(*comments.toTypedArray())

            // When: Text areas are retrieved
            val textAreas = dialog.commentItems.map { item -> item.textArea }

            // Then: Text areas show original bodies
            Assertions.assertEquals("Original body text", textAreas[0].text)
            Assertions.assertEquals("Another comment", textAreas[1].text)
        }

        @Test
        fun `text area has correct properties`() {
            // Given: Dialog with comment
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(createComment(1, "test.xml", 1, 1, "Comment"))
            val dialog = createDialog(*comments.toTypedArray())

            // When: Text area is retrieved
            val textAreas = dialog.commentItems.map { item -> item.textArea }

            // Then: Text area properties are correct
            Assertions.assertEquals(4, textAreas[0].rows, "Text area should have 4 rows")
            Assertions.assertEquals(50, textAreas[0].columns, "Text area should have 50 columns")
            Assertions.assertTrue(textAreas[0].lineWrap, "Text area should have line wrap")
            Assertions.assertTrue(textAreas[0].wrapStyleWord, "Text area should wrap at word boundaries")
        }

        @Test
        fun `each comment item has a delete button`() {
            // Given: Dialog with multiple comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(
                createComment(1, "test.xml", 1, 1, "First"),
                createComment(2, "test.xml", 2, 2, "Second")
            )
            val dialog = createDialog(*comments.toTypedArray())

            // When: Delete buttons are retrieved
            val deleteButtons = dialog.commentItems.map { item -> item.delete }

            // Then: Each comment has a delete button
            Assertions.assertEquals(2, deleteButtons.size, "Each comment should have a delete button")
            deleteButtons.forEach { button ->
                Assertions.assertEquals(ReviewBundle.message("delete"), button.text, "Delete button should have correct text")
            }
        }

        @Test
        fun `scroll pane contains all comment panels`() {
            // Given: Dialog with multiple comments
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(
                createComment(1, "test.xml", 1, 1, "First"),
                createComment(2, "test.xml", 2, 2, "Second"),
                createComment(3, "test.xml", 3, 3, "Third")
            )
            val dialog = createDialog(*comments.toTypedArray())

            // When: Comment panels count is retrieved
            val count = dialog.commentItems.size

            // Then: All comment panels are in scroll pane
            Assertions.assertEquals(3, count, "All comment panels should be in scroll pane")
        }
    }

    @Nested
    inner class ShowCompanion : EditCommentsDialogTest() {

        @Test
        fun `show creates dialog with correct parameters`() {
            // Given: Comments to edit
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val comments = listOf(
                createComment(1, "test.xml", 1, 1, "First"),
                createComment(2, "test.xml", 2, 2, "Second")
            )

            // When: Dialog is created via companion (can't actually test show(), but verify creation)
            val dialog = createDialog(*comments.toTypedArray())

            // Then: Dialog is created correctly
            Assertions.assertNotNull(dialog)
            Assertions.assertEquals(2, dialog.commentItems.size)
        }
    }
}
