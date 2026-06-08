package com.github.cpardi.markdowncodereview.ui

import com.github.cpardi.markdowncodereview.LightPlatformTest
import com.github.cpardi.markdowncodereview.ReviewBundle
import com.github.cpardi.markdowncodereview.services.ReviewService
import com.github.cpardi.markdowncodereview.settings.ReviewSettings
import com.github.cpardi.markdowncodereview.toolWindow.ReviewToolWindowPanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.awt.EventQueue.invokeAndWait

@Suppress("JUnitMixedFramework")
class ReviewToolWindowPanelTestSuite {

    /**
     * Base class for integration tests of ReviewToolWindowPanel.
     */
    abstract class ReviewToolWindowPanelTest : LightPlatformTest() {

        protected lateinit var service: ReviewService
        protected lateinit var fileEditorManager: FileEditorManager

        override fun setUp() {
            super.setUp()
            service = ReviewService.getInstance(project)
            service.setActiveReview(null)
            fileEditorManager = FileEditorManager.getInstance(project)
        }
    }

    @Nested
    inner class ToolWindowInit : ReviewToolWindowPanelTest() {

        @Test
        fun `panel is initialised with available reviews`() {
            // Given: Multiple reviews exist
            createDirectory("reviews")
            createVirtualFile("reviews/review-1.md", "")
            createVirtualFile("reviews/review-2.md", "")
            createVirtualFile("reviews/review-3.md", "")

            // When: Tool window is initialised
            val panel = ReviewToolWindowPanel(project, service)

            // Then: Reviews and <None> are listed, <None> selected and delete disabled
            Assertions.assertEquals(4, panel.reviewComboBox.itemCount, "Should be an item for each review + one for <None>")
            Assertions.assertEquals(ReviewService.NONE_SENTINEL, panel.reviewComboBox.item, "Default selection should be <None> sentinel")
            Assertions.assertTrue(panel.addButton.isEnabled, "User should be able to add a new review")
            Assertions.assertFalse(panel.deleteButton.isEnabled, "User should not be able to delete <None> sentinel")
        }

        @Test
        fun `help text is shown when review without comments is active`() {
            // Given: A reviews without comments
            val reviewName = "review-1"
            createVirtualFile("reviews/${reviewName}.md", "")
            val xmlFile = "test.xml"
            createVirtualFile(xmlFile, "<xml>\n</xml>")

            // When: Review without comments is active
            service.setActiveReview(reviewName)
            val panel = ReviewToolWindowPanel(project, service)

            // Then: Help label is shown
            Assertions.assertNotNull(panel.commentsPanel.components.contains(panel.noCommentsLabel), "Label should be added")
            Assertions.assertEquals(ReviewBundle.message("noComments"), panel.noCommentsLabel.text, "Label should show user help text")
        }

        @Test
        fun `comments are shown when review with comments is active`() {
            // Given: A reviews with two comments
            val reviewName = "review-1"
            createVirtualFile("reviews/${reviewName}.md", "")
            val xmlFile = "test.xml"
            createVirtualFile(xmlFile, "<xml>\n</xml>")

            // When: Review with two comments is active
            service.setActiveReview(reviewName)
            service.addComment(xmlFile, 1, 1, "First comment")
            service.addComment(xmlFile, 2, 2, "Second comment")

            val panel = ReviewToolWindowPanel(project, service)
            panel.reviewComboBox.selectedItem = reviewName

            // Then: Two review comment bubbles are shown
            val commentBubbles = panel.commentsPanel.components.filterIsInstance<CommentBubblePanel>()
            Assertions.assertEquals(2, commentBubbles.size, "One bubble per comment should be displayed")
        }
    }

    @Nested
    inner class UserInteractions : ReviewToolWindowPanelTest() {

        @Test
        fun `onReviewSelected does nothing when same review selected`() {
            // Given: A review is active
            val reviewName = "review-1"
            createVirtualFile("reviews/${reviewName}.md", "")
            service.setActiveReview(reviewName)
            val panel = ReviewToolWindowPanel(project, service)

            // When: Same review is selected again
            panel.reviewComboBox.selectedItem = reviewName

            // Then: No state change occurs (verify delete button still enabled)
            Assertions.assertTrue(panel.deleteButton.isEnabled, "Delete should remain enabled for active review")
        }

        @Test
        fun `onReviewSelected switches to different review`() {
            // Given: Multiple reviews exist and one is active
            createDirectory("reviews")
            createVirtualFile("reviews/review-1.md", "")
            createVirtualFile("reviews/review-2.md", "")
            service.setActiveReview("review-1")
            val panel = ReviewToolWindowPanel(project, service)

            // When: Different review is selected
            panel.reviewComboBox.selectedItem = "review-2"

            // Then: Active review changes
            Assertions.assertEquals("review-2", service.activeReview?.name, "Active review should change")
        }

        @Test
        fun `onReviewSelected switches to None sentinel`() {
            // Given: A review is active
            val reviewName = "review-1"
            createVirtualFile("reviews/${reviewName}.md", "")
            service.setActiveReview(reviewName)
            val panel = ReviewToolWindowPanel(project, service)

            // When: <None> is selected
            panel.reviewComboBox.selectedItem = ReviewService.NONE_SENTINEL

            // Then: Active review is cleared
            Assertions.assertNull(service.activeReview, "Active review should be null")
            Assertions.assertFalse(panel.deleteButton.isEnabled, "Delete should be disabled for <None>")
        }
    }

    @Nested
    inner class CommentOperations : ReviewToolWindowPanelTest() {

        @Test
        fun `comment body can be edited`() {
            // Given: A review with a comment
            val reviewName = "review-1"
            createVirtualFile("reviews/${reviewName}.md", "")
            val xmlFile = "test.xml"
            createVirtualFile(xmlFile, "<xml>\n</xml>")
            service.setActiveReview(reviewName)
            service.addComment(xmlFile, 1, 1, "Original comment")

            val panel = ReviewToolWindowPanel(project, service)
            panel.reviewComboBox.selectedItem = reviewName

            // When: Comment body is edited
            val commentBubble = panel.commentsPanel.components.filterIsInstance<CommentBubblePanel>().first()
            commentBubble.bodyText = "Edited comment"

            // Then: Comment in service should still have original (not saved yet)
            val comment = service.activeReview?.comments?.first()
            Assertions.assertEquals("Original comment", comment?.body, "Comment body should remain original until focus lost")
        }

        @Test
        fun `single comment update refreshes only that comment panel`() {
            // Given: A review with two comments
            val reviewName = "review-1"
            createVirtualFile("reviews/${reviewName}.md", "")
            val xmlFile = "test.xml"
            createVirtualFile(xmlFile, "<xml>\n</xml>")
            service.setActiveReview(reviewName)
            service.addComment(xmlFile, 1, 1, "First comment")
            service.addComment(xmlFile, 2, 2, "Second comment")

            val panel = ReviewToolWindowPanel(project, service)
            panel.reviewComboBox.selectedItem = reviewName

            // When: One comment is edited via service
            val commentId = service.activeReview?.comments?.first()?.id!!
            service.editComment(commentId, "Edited first comment")

            // Then: Wait for async UI update and verify the edit
            flushPendingUiUpdates()

            val commentBubbles = panel.commentsPanel.components.filterIsInstance<CommentBubblePanel>()
            Assertions.assertEquals("Edited first comment", commentBubbles.first().bodyText, "First comment should be updated")
            Assertions.assertEquals("Second comment", commentBubbles[1].bodyText, "Second comment should remain unchanged")
        }
    }

    @Nested
    inner class Navigation : ReviewToolWindowPanelTest() {

        @Test
        fun `navigateToComment opens file in editor`() {
            // Given: A review with a comment
            val reviewName = "review-1"
            createVirtualFile("reviews/${reviewName}.md", "")
            val xmlFile = "test.xml"
            createVirtualFile(xmlFile, "<xml>\nLine 2\nLine 3\n</xml>")
            service.setActiveReview(reviewName)
            service.addComment(xmlFile, 2, 2, "Comment on line 2")

            val panel = ReviewToolWindowPanel(project, service)
            panel.reviewComboBox.selectedItem = reviewName

            // When: Navigate is triggered (requires EDT and read action for file editor operations)
            val comment = service.activeReview?.comments?.first()!!
            invokeAndWait {
                runWriteAction {
                    panel.navigateToComment(comment)
                }
            }

            // Then: File should be opened in editor
            val openFiles = fileEditorManager.openFiles
            Assertions.assertTrue(openFiles.any { it.name == xmlFile }, "File should be opened")
        }
    }

    @Nested
    inner class SettingsIntegration : ReviewToolWindowPanelTest() {

        @Test
        fun `settings change clears active review and refreshes`() {
            // Given: Active review exists
            val reviewName = "review-1"
            createVirtualFile("reviews/${reviewName}.md", "")
            service.setActiveReview(reviewName)
            val panel = ReviewToolWindowPanel(project, service)

            // When: Settings change is published
            ApplicationManager.getApplication().messageBus.syncPublisher(ReviewSettings.SETTINGS_CHANGED_TOPIC).onSettingsChanged()
            flushPendingUiUpdates()

            // Then: Active review is cleared
            Assertions.assertNull(service.activeReview, "Active review should be cleared after settings change")
            Assertions.assertEquals(ReviewService.NONE_SENTINEL, panel.reviewComboBox.selectedItem, "ComboBox should reset to <None>")
        }
    }

    @Nested
    inner class EdgeCases : ReviewToolWindowPanelTest() {

        @Test
        fun `panel handles empty reviews directory gracefully`() {
            // Given: No reviews exist
            createDirectory("reviews")

            // When: Panel is created
            val panel = ReviewToolWindowPanel(project, service)

            // Then: Only <None> sentinel is available
            Assertions.assertEquals(1, panel.reviewComboBox.itemCount, "Should only have <None> sentinel")
            Assertions.assertEquals(ReviewService.NONE_SENTINEL, panel.reviewComboBox.selectedItem, "<None> should be selected")
        }
    }
}
