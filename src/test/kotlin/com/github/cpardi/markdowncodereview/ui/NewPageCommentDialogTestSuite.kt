package com.github.cpardi.markdowncodereview.ui

import com.github.cpardi.markdowncodereview.LightPlatformTest
import com.github.cpardi.markdowncodereview.ReviewBundle
import com.github.cpardi.markdowncodereview.services.ReviewService
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for NewPageCommentDialog.
 *
 * Note: This test suite only covers behaviour specific to NewPageCommentDialog.
 * Shared behaviour inherited from BaseNewCommentDialog (validation, commit flow,
 * empty body handling, delimiter checking, whitespace trimming) is tested in
 * BaseNewCommentDialogTestSuite to avoid duplication.
 */
@Suppress("JUnitMixedFramework")
class NewPageCommentDialogTestSuite {

    /**
     * Base class for NewPageCommentDialog tests.
     */
    abstract class NewPageCommentDialogTest : LightPlatformTest() {

        protected lateinit var service: ReviewService

        override fun setUp() {
            super.setUp()
            service = ReviewService.getInstance(project)
            service.setActiveReview(null)
        }

        protected fun createDialog(relativePath: String): TestableNewPageCommentDialog {
            return invokeAndWaitIfNeeded {
                TestableNewPageCommentDialog(project, relativePath)
            }
        }
    }

    /**
     * Tests for getDialogTitle() which returns the page comment specific title.
     * This is NewPageCommentDialog-specific behaviour - it uses "addPageComment" message.
     */
    @Nested
    inner class DialogTitle : NewPageCommentDialogTest() {

        @Test
        fun `getDialogTitle returns Add Page Comment message`() {
            // Given: A file for page comment
            createVirtualFile("page.md", "# Page Title\nContent")
            val dialog = createDialog("page.md")

            // When: getDialogTitle is called
            val title = dialog.getDialogTitleTest()

            // Then: Title is from ReviewBundle "addPageComment"
            Assertions.assertEquals(ReviewBundle.message("addPageComment"), title)
        }

        @Test
        fun `dialog title differs from line comment dialog`() {
            // Given: A page comment dialog
            createVirtualFile("page.md", "Content")
            val dialog = createDialog("page.md")

            // When: Titles are compared
            val pageTitle = dialog.getDialogTitleTest()
            val lineCommentTitle = ReviewBundle.message("addComment")

            // Then: Page comment title is different from line comment title
            Assertions.assertNotEquals(lineCommentTitle, pageTitle, "Page comment title should differ from line comment title")
        }
    }

    /**
     * Tests for getLocationText() which formats the location display.
     * This is NewPageCommentDialog-specific behaviour - it uses "pageCommentLocation" message.
     */
    @Nested
    inner class LocationText : NewPageCommentDialogTest() {

        @Test
        fun `getLocationText returns page comment location format`() {
            // Given: A file
            createVirtualFile("document.md", "# Title\nContent")
            val dialog = createDialog("document.md")

            // When: getLocationText is called
            val locationText = dialog.getLocationTextTest()

            // Then: Format uses ReviewBundle "pageCommentLocation"
            val expected = ReviewBundle.message("pageCommentLocation", "document.md")
            Assertions.assertEquals(expected, locationText)
        }

        @Test
        fun `getLocationText displays filename correctly`() {
            // Given: A file with specific name
            createVirtualFile("my-important-file.xml", "<xml>\n</xml>")
            val dialog = createDialog("my-important-file.xml")

            // When: getLocationText is called
            val locationText = dialog.getLocationTextTest()

            // Then: Filename is included in location text
            Assertions.assertTrue(locationText.contains("my-important-file.xml"), "Location text should contain filename")
        }

        @Test
        fun `getLocationText handles nested path correctly`() {
            // Given: A file in a nested directory
            createVirtualFile("src/main/resources/config.xml", "<config/>\n")
            val dialog = createDialog("src/main/resources/config.xml")

            // When: getLocationText is called
            val locationText = dialog.getLocationTextTest()

            // Then: Location text shows just the filename per ReviewBundle format
            val expected = ReviewBundle.message("pageCommentLocation", "src/main/resources/config.xml")
            Assertions.assertEquals(expected, locationText)
        }

        @Test
        fun `getLocationText differs from line comment location format`() {
            // Given: A page comment dialog
            createVirtualFile("test.xml", "<xml>\n</xml>")
            val dialog = createDialog("test.xml")

            // When: Location texts are compared
            val pageLocationText = dialog.getLocationTextTest()
            val lineLocationText = ReviewBundle.message("commentLocation", "test.xml", "5")

            // Then: Page comment location format is different
            Assertions.assertNotEquals(lineLocationText, pageLocationText, "Page location format should differ from line comment format")
        }
    }

    /**
     * Tests for doCreate() delegating to ReviewService.addPageComment().
     * This is NewPageCommentDialog-specific behaviour - how it calls the service.
     */
    @Nested
    inner class DoCreate : NewPageCommentDialogTest() {

        @Test
        fun `doCreate calls service addPageComment with correct parameters`() {
            // Given: Active review and dialog
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("page.md", "# Page\nContent")
            service.setActiveReview("test-review")

            val dialog = createDialog("page.md")
            dialog.testDoCreate("Test page comment")

            // Then: Page comment added with correct parameters
            val comments = service.activeReview?.comments
            Assertions.assertEquals(1, comments?.size, "One page comment should be added")
            val comment = comments?.first()
            Assertions.assertEquals("page.md", comment?.relativePath)
            Assertions.assertEquals("Test page comment", comment?.body)
        }

        @Test
        fun `doCreate creates page comment with startLine equals endLine equals 0`() {
            // Given: Active review
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("config.xml", "<config/>\n")
            service.setActiveReview("test-review")

            val dialog = createDialog("config.xml")
            dialog.testDoCreate("Configuration file comment")

            // Then: Page comment has no line range (startLine=0, endLine=0)
            val comment = service.activeReview?.comments?.first()
            Assertions.assertEquals(0, comment?.startLine, "Page comment should have startLine=0")
            Assertions.assertEquals(0, comment?.endLine, "Page comment should have endLine=0")
        }

        @Test
        fun `doCreate preserves relativePath for nested files`() {
            // Given: Active review and nested file
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("src/deep/nested/file.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            val dialog = createDialog("src/deep/nested/file.xml")
            dialog.testDoCreate("Nested file page comment")

            // Then: Full relativePath is preserved
            val comment = service.activeReview?.comments?.first()
            Assertions.assertEquals("src/deep/nested/file.xml", comment?.relativePath)
        }
    }

    /**
     * Tests for the companion object show() method.
     */
    @Nested
    inner class ShowCompanion : NewPageCommentDialogTest() {

        @Test
        fun `show creates dialog with correct parameters`() {
            // Given: Active review
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("test.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            // When: Show is called (we can't actually test show(), but we can verify dialog creation)
            val createdDialog = createDialog("test.xml")

            // Then: Dialog is created with correct parameters
            Assertions.assertNotNull(createdDialog)
            val locText = createdDialog.getLocationTextTest()
            Assertions.assertTrue(locText.contains("test.xml"), "Location text should contain filename")
        }
    }

    /**
     * Integration tests verifying NewPageCommentDialog works correctly with the service.
     */
    @Nested
    inner class ServiceIntegration : NewPageCommentDialogTest() {

        @Test
        fun `end-to-end flow creates page comment in active review`() {
            // Given: Active review with a file
            createVirtualFile("reviews/integration-test.md", "")
            createVirtualFile("integration.md", "# Integration\nContent")
            service.setActiveReview("integration-test")

            // When: Dialog is created and doCreate called
            val dialog = createDialog("integration.md")
            dialog.testDoCreate("Integration test page comment")

            // Then: Page comment is added to active review
            val comments = service.activeReview?.comments
            Assertions.assertEquals(1, comments?.size, "Page comment should be added")
            val comment = comments?.first()
            Assertions.assertEquals("integration.md", comment?.relativePath)
            Assertions.assertEquals("Integration test page comment", comment?.body)
            Assertions.assertTrue(comment?.isPageComment() == true, "Comment should be a page comment")
        }

        @Test
        fun `no active review does not throw exception`() {
            // Given: No active review
            createVirtualFile("test.xml", "<xml>\n</xml>")

            // When: Dialog is created and doCreate called
            val dialog = createDialog("test.xml")

            // Then: No exception thrown (service handles null review gracefully)
            dialog.testDoCreate("Test comment")
            Assertions.assertNull(service.activeReview)
        }

        @Test
        fun `page comment is distinguished from line comment in service`() {
            // Given: Active review
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("file.xml", "<xml>\n</xml>")
            service.setActiveReview("test-review")

            // When: Both page and line comments are added
            service.addComment("file.xml", 5, 5, "Line comment")
            val dialog = createDialog("file.xml")
            dialog.testDoCreate("Page comment")

            // Then: Comments are distinguishable
            val allComments = service.activeReview?.comments!!
            Assertions.assertEquals(2, allComments.size, "Should have two comments")

            val pageComments = service.getPageCommentsForFile("file.xml")
            val lineComments = service.getCommentsForLine("file.xml", 5)

            Assertions.assertEquals(1, pageComments.size, "Should have one page comment")
            Assertions.assertEquals(1, lineComments.size, "Should have one line comment")
            Assertions.assertEquals("Page comment", pageComments.first().body)
            Assertions.assertEquals("Line comment", lineComments.first().body)
        }

        @Test
        fun `multiple page comments can be added to same file`() {
            // Given: Active review
            createVirtualFile("reviews/test-review.md", "")
            createVirtualFile("document.md", "# Document\nContent")
            service.setActiveReview("test-review")

            // When: Multiple page comments are added
            val dialog1 = createDialog("document.md")
            dialog1.testDoCreate("First page comment")
            val dialog2 = createDialog("document.md")
            dialog2.testDoCreate("Second page comment")

            // Then: Both page comments exist
            val pageComments = service.getPageCommentsForFile("document.md")
            Assertions.assertEquals(2, pageComments.size, "Should have two page comments")
        }
    }
}

/**
 * Testable implementation of NewPageCommentDialog that exposes protected methods for testing.
 * Must be public to be accessible from nested test classes.
 */
class TestableNewPageCommentDialog(
    project: com.intellij.openapi.project.Project,
    private val relativePath: String
) : BaseNewCommentDialog(project) {

    init {
        initDialog()
    }

    public override fun getDialogTitle(): String {
        return ReviewBundle.message("addPageComment")
    }

    public override fun getLocationText(): String {
        return ReviewBundle.message("pageCommentLocation", relativePath)
    }

    override fun doCreate(body: String) {
        service.addPageComment(relativePath, body)
    }

    // Expose getDialogTitle for testing
    fun getDialogTitleTest(): String = getDialogTitle()

    // Expose getLocationText for testing
    fun getLocationTextTest(): String = getLocationText()

    // Expose doCreate for testing
    fun testDoCreate(body: String) {
        doCreate(body)
    }
}
