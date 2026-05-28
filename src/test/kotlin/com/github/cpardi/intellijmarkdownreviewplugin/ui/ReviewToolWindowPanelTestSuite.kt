package com.github.cpardi.intellijmarkdownreviewplugin.ui

import com.github.cpardi.intellijmarkdownreviewplugin.LightPlatformTest
import com.github.cpardi.intellijmarkdownreviewplugin.ReviewBundle
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.github.cpardi.intellijmarkdownreviewplugin.toolWindow.ReviewToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import org.junit.jupiter.api.Test

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

    class Initialisation : ReviewToolWindowPanelTests() {

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
    }

    class CommentsDisplay : ReviewToolWindowPanelTests() {

        @Test
        fun `Help text shown when selecting review without comments`() {
            // Given: Multiple reviews exist
            createDirectory("reviews")
            val review1Path = "reviews/review-1.xml"
            createVirtualFile(review1Path, "<xml/>")
            val review2Path = "reviews/review-2.xml"
            createVirtualFile(review2Path, "<xml/>")

            service.addComment(review2Path, 1, 1, "Single comment")

            // When: Review without comments is selected
            val panel = ReviewToolWindowPanel(project, service);
            panel.reviewComboBox.selectedItem = review1Path
            val label = UIUtil.findComponentOfType(panel.commentsPanel, JBLabel::class.java)

            // Then: Reviews and <None> are listed, <None> selected and delete disabled
            assertNotNull("Label should be added", label)
            assertEquals("Label should show user help text", ReviewBundle.message("noComments"), label!!.text)
        }

        @Test
        fun `Comments shown when selecting review with comments`() {
            // Given: Multiple reviews exist
            createDirectory("reviews")
            val review1Path = "reviews/review-1.md"
            createVirtualFile(review1Path, "")
            val review2Name = "review-2"
            createVirtualFile("reviews/review-2.md", "")
            val xmlFile = "test.xml"
            createVirtualFile(xmlFile, "<xml>\n</xml>")

            // When: Review with 2 comments is selected
            service.setActiveReview(review2Name)
            service.addComment(xmlFile, 1, 1, "First comment")
            service.addComment(xmlFile, 2, 2, "Second comment")

            val panel = ReviewToolWindowPanel(project, service);

            // Then: Reviews and <None> are listed, <None> selected and delete disabled
            val commentBubbles = panel.commentsPanel.components.filterIsInstance<CommentBubblePanel>()
            assertEquals("One bubble per comment should be displayed", 2, commentBubbles.size)
        }
    }
}
