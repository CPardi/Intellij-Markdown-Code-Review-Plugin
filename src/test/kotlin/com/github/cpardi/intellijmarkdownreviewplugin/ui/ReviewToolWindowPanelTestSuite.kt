package com.github.cpardi.intellijmarkdownreviewplugin.ui

import com.github.cpardi.intellijmarkdownreviewplugin.LightPlatformTest
import com.github.cpardi.intellijmarkdownreviewplugin.ReviewBundle
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.github.cpardi.intellijmarkdownreviewplugin.toolWindow.ReviewToolWindowPanel
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit


@Suppress("JUnitMixedFramework")
object ReviewToolWindowPanelTestSuite {

    /**
     * Base class for integration tests of ReviewToolWindowPanel.
     */
    abstract class ReviewToolWindowPanelTests : LightPlatformTest() {

        protected lateinit var service: ReviewService

        override fun setUp() {
            super.setUp()
            service = ReviewService.getInstance(project)
            service.setActiveReview(null)
        }
    }

    class ToolWindowInit : ReviewToolWindowPanelTests() {

        @Test
        fun `Panel initialised with available reviews`() {
            // Given: Multiple reviews exist
            createDirectory("reviews")
            createVirtualFile("reviews/review-1.md", "")
            createVirtualFile("reviews/review-2.md", "")
            createVirtualFile("reviews/review-3.md", "")

            // When: Tool window is initialised
            val panel = ReviewToolWindowPanel(project, service);

            // Then: Reviews and <None> are listed, <None> selected and delete disabled
            assertEquals("Should be an item for each review + one for <None>", 4, panel.reviewComboBox.itemCount)
            assertEquals("Default selection should be <None> sentinel", ReviewService.NONE_SENTINEL, panel.reviewComboBox.item)
            assertTrue("User should be able to add a new review", panel.addButton.isEnabled)
            assertFalse("User should not be able to delete <None> sentinel", panel.deleteButton.isEnabled)
        }

        @Test
        fun `Help text shown when review without comments is active`() {
            // Given: A reviews without comments
            val reviewName = "review-1"
            createVirtualFile("reviews/${reviewName}.md", "")
            val xmlFile = "test.xml"
            createVirtualFile(xmlFile, "<xml>\n</xml>")

            // When: Review without comments is active
            val future = service.setActiveReview(reviewName)
            future.get(5, TimeUnit.SECONDS)
            val panel = ReviewToolWindowPanel(project, service);

            // Then: Help label is shown
            assertNotNull("Label should be added", panel.commentsPanel.components.contains(panel.noCommentsLabel))
            assertEquals("Label should show user help text", ReviewBundle.message("noComments"), panel.noCommentsLabel.text)
        }

        @Test
        fun `Comments shown when review with comments is active`() {
            // Given: A reviews with two comments
            val reviewName = "review-1"
            createVirtualFile("reviews/${reviewName}.md", "")
            val xmlFile = "test.xml"
            createVirtualFile(xmlFile, "<xml>\n</xml>")

            // When: Review with two comments is active
            val future = service.setActiveReview(reviewName)
            future.get(5, TimeUnit.SECONDS)
            service.addComment(xmlFile, 1, 1, "First comment")
            service.addComment(xmlFile, 2, 2, "Second comment")

            val panel = ReviewToolWindowPanel(project, service);
            panel.reviewComboBox.selectedItem = reviewName

            // Then: Two review comment bubbles are shown
            val commentBubbles = panel.commentsPanel.components.filterIsInstance<CommentBubblePanel>()
            assertEquals("One bubble per comment should be displayed", 2, commentBubbles.size)
        }
    }

//    class CommentsDisplay : ReviewToolWindowPanelTests() {
//    }
}
