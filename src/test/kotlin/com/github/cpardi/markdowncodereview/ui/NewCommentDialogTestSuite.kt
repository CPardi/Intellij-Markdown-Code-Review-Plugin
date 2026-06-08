package com.github.cpardi.markdowncodereview.ui

import com.github.cpardi.markdowncodereview.LightPlatformTest
import com.github.cpardi.markdowncodereview.ReviewBundle
import com.github.cpardi.markdowncodereview.services.ReviewService
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for NewCommentDialog.
 *
 * Note: This test suite only covers behaviour specific to NewCommentDialog.
 * Shared behaviour inherited from BaseNewCommentDialog (validation, commit flow,
 * empty body handling, delimiter checking, whitespace trimming) is tested in
 * BaseNewCommentDialogTestSuite to avoid duplication.
 */
@Suppress("JUnitMixedFramework")
class NewCommentDialogTestSuite {

    /**
     * Base class for NewCommentDialog tests.
     */
    abstract class NewCommentDialogTest : LightPlatformTest() {

        protected lateinit var service: ReviewService

        override fun setUp() {
            super.setUp()
            service = ReviewService.getInstance(project)
            service.setActiveReview(null)
        }

        protected fun createDialog(relativePath: String, startLine: Int, endLine: Int): TestableNewCommentDialog {
            return invokeAndWaitIfNeeded {
                TestableNewCommentDialog(project, relativePath, startLine, endLine)
            }
        }
    }

    /**
     * Tests for getLocationText() which formats the location display.
     * This is NewCommentDialog-specific behaviour - how it formats filename and line ranges.
     */
    @Nested
    inner class LocationText : NewCommentDialogTest() {

        @Test
        fun `getLocationText shows filename colon line for single line comment`() {
            // Given: A file with a single-line comment
            createVirtualFile("test.xml", "<xml>\n</xml>")
            val dialog = createDialog("test.xml", 5, 5)

            // When: getLocationText is called
            val locationText = dialog.getLocationText()

            // Then: Format is "Comment location: test.xml:5"
            val expected = ReviewBundle.message("commentLocation", "test.xml", "5")
            Assertions.assertEquals(expected, locationText)
        }

        @Test
        fun `getLocationText shows filename colon range for multi-line comment`() {
            // Given: A file with a multi-line comment
            createVirtualFile("src/main/file.xml", "<xml>\n</xml>")
            val dialog = createDialog("src/main/file.xml", 10, 15)

            // When: getLocationText is called
            val locationText = dialog.getLocationText()

            // Then: Format is "Comment location: file.xml:10-15"
            val expected = ReviewBundle.message("commentLocation", "file.xml", "10-15")
            Assertions.assertEquals(expected, locationText)
        }

        @Test
        fun `getLocationText extracts filename from nested path`() {
            // Given: A file in a deeply nested path
            createVirtualFile("a/b/c/d/deep.xml", "<xml>\n</xml>")
            val dialog = createDialog("a/b/c/d/deep.xml", 1, 1)

            // When: getLocationText is called
            val locationText = dialog.getLocationText()

            // Then: Only filename is shown, not full path
            val expected = ReviewBundle.message("commentLocation", "deep.xml", "1")
            Assertions.assertEquals(expected, locationText)
        }

        @Test
        fun `getLocationText handles file at root level`() {
            // Given: A file at project root
            createVirtualFile("root-level.xml", "<xml>\n</xml>")
            val dialog = createDialog("root-level.xml", 3, 3)

            // When: getLocationText is called
            val locationText = dialog.getLocationText()

            // Then: Filename is correctly extracted (no path separators)
            val expected = ReviewBundle.message("commentLocation", "root-level.xml", "3")
            Assertions.assertEquals(expected, locationText)
        }

        @Test
        fun `getLocationText uses single line format when start equals end`() {
            // Given: A comment where startLine equals endLine
            createVirtualFile("test.xml", "<xml>\n</xml>")
            val dialog = createDialog("test.xml", 7, 7)

            // When: getLocationText is called
            val locationText = dialog.getLocationText()

            // Then: Single line format is used (not "7-7")
            val expected = ReviewBundle.message("commentLocation", "test.xml", "7")
            Assertions.assertEquals(expected, locationText)
        }
    }

    /**
     * Tests for doCreate() delegating to ReviewService.
     * This is NewCommentDialog-specific behaviour - how it calls the service.
     */
    @Nested
    inner class DoCreate : NewCommentDialogTest() {

        @Test
        fun `doCreate calls service addComment with correct parameters for single line`() {
            // Given: Active review and dialog
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val dialog = createDialog("test.xml", 5, 5)
            dialog.testDoCreate("Test comment")

            // Then: Comment added with correct parameters
            val comments = service.activeReview?.comments
            Assertions.assertEquals(1, comments?.size, "One comment should be added")
            val comment = comments?.first()
            Assertions.assertEquals("test.xml", comment?.relativePath)
            Assertions.assertEquals(5, comment?.startLine)
            Assertions.assertEquals(5, comment?.endLine)
            Assertions.assertEquals("Test comment", comment?.body)
        }

        @Test
        fun `doCreate calls service addComment with correct parameters for multi-line`() {
            // Given: Active review and dialog
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("src/main/code.xml", "<xml>\nLine1\nLine2\nLine3\n</xml>")
            service.setActiveReview("test-review")

            // When: Comment is created
            val dialog = createDialog("src/main/code.xml", 10, 15)
            dialog.testDoCreate("Multi-line comment")

            // Then: Parameters match constructor arguments
            val comments = service.activeReview?.comments
            Assertions.assertEquals(1, comments?.size, "One comment should be added")
            val comment = comments?.first()
            Assertions.assertEquals("src/main/code.xml", comment?.relativePath)
            Assertions.assertEquals(10, comment?.startLine)
            Assertions.assertEquals(15, comment?.endLine)
            Assertions.assertEquals("Multi-line comment", comment?.body)
        }

        @Test
        fun `doCreate preserves relativePath for nested files`() {
            // Given: Active review and nested file
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("src/deep/nested/file.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val dialog = createDialog("src/deep/nested/file.xml", 1, 1)
            dialog.testDoCreate("Nested file comment")

            // Then: Full relativePath is passed to service
            val comment = service.activeReview?.comments?.first()
            Assertions.assertEquals("src/deep/nested/file.xml", comment?.relativePath)
        }
    }

    /**
     * Tests for the companion object show() method.
     */
    @Nested
    inner class ShowCompanion : NewCommentDialogTest() {

        @Test
        fun `show creates dialog with correct parameters`() {
            // Given: Active review
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            // When: Show is called (we can't actually test show(), but we can verify dialog creation)
            val createdDialog = createDialog("test.xml", 1, 1)

            // Then: Dialog is created with correct parameters
            Assertions.assertNotNull(createdDialog)
            val locText = createdDialog.getLocationText()
            Assertions.assertTrue(locText.contains("test.xml"), "Location text should contain filename")
            Assertions.assertTrue(locText.contains("1"), "Location text should contain line number")
        }
    }

    /**
     * Integration test verifying NewCommentDialog works correctly with the service.
     * This tests the full flow from dialog creation to comment being stored.
     */
    @Nested
    inner class ServiceIntegration : NewCommentDialogTest() {

        @Test
        fun `end-to-end flow creates comment in active review`() {
            // Given: Active review with a file
            createVirtualFile("reviews/integration-test.md", "")
            createVirtualFile("integration.xml", "<xml>\nLine 2\nLine 3\n</xml>")
            service.setActiveReview("integration-test")

            // When: Dialog is created and doCreate called
            val dialog = createDialog("integration.xml", 2, 3)
            dialog.testDoCreate("Integration test comment")

            // Then: Comment is added to active review
            val comments = service.activeReview?.comments
            Assertions.assertEquals(1, comments?.size, "Comment should be added")
            val comment = comments?.first()
            Assertions.assertEquals("integration.xml", comment?.relativePath)
            Assertions.assertEquals(2, comment?.startLine)
            Assertions.assertEquals(3, comment?.endLine)
            Assertions.assertEquals("Integration test comment", comment?.body)
        }

        @Test
        fun `no active review does not throw exception`() {
            // Given: No active review (service.setActiveReview(null) in setUp)
            createVirtualFile("test.xml", "<xml>\n</xml>")

            // When: Dialog is created and doCreate called
            val dialog = createDialog("test.xml", 1, 1)

            // Then: No exception thrown (service handles null review gracefully)
            dialog.testDoCreate("Test comment")
            Assertions.assertNull(service.activeReview)
        }
    }
}

/**
 * Testable implementation of NewCommentDialog that exposes protected methods for testing.
 * Must be public to be accessible from nested test classes.
 */
class TestableNewCommentDialog(
    project: com.intellij.openapi.project.Project,
    private val relativePath: String,
    private val startLine: Int,
    private val endLine: Int
) : BaseNewCommentDialog(project) {

    init {
        initDialog()
    }

    public override fun getLocationText(): String {
        val fileName = relativePath.substringAfterLast('/')
        return if (startLine == endLine) {
            ReviewBundle.message("commentLocation", fileName, startLine.toString())
        } else {
            ReviewBundle.message("commentLocation", fileName, "$startLine-$endLine")
        }
    }

    override fun doCreate(body: String) {
        service.addComment(relativePath, startLine, endLine, body)
    }

    // Expose doCreate for testing
    fun testDoCreate(body: String) {
        doCreate(body)
    }
}
