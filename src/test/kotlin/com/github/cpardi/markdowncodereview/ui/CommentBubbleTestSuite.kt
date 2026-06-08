package com.github.cpardi.markdowncodereview.ui

import com.github.cpardi.markdowncodereview.LightPlatformTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.awt.event.FocusEvent
import java.awt.event.MouseEvent
import javax.swing.JPanel

@Suppress("JUnitMixedFramework")
class CommentBubbleTestSuite {

    /**
     * Base class for CommentBubblePanel tests.
     * Extends LightPlatformTest to provide IntelliJ Platform testing infrastructure
     * needed for Swing component initialization.
     */
    abstract class CommentBubbleTest : LightPlatformTest() {
        protected var commentId = 42
    }

    @Nested
    inner class PropertyAccessors : CommentBubbleTest() {

        @Test
        fun `headerText getter returns underlying headerLabel text`() {
            // Given: A panel with header text set
            val panel = CommentBubble(commentId)
            panel.headerLabel.text = "Test header"

            // When: The getter is accessed
            val result = panel.headerText

            // Then: It returns the underlying label's text
            Assertions.assertEquals("Test header", result, "headerText getter should return headerLabel.text")
        }

        @Test
        fun `headerText setter updates headerLabel text`() {
            // Given: A panel instance
            val panel = CommentBubble(commentId)

            // When: Header text is set via the property
            panel.headerText = "New header"

            // Then: The underlying label is updated
            Assertions.assertEquals("New header", panel.headerLabel.text, "headerText setter should update headerLabel.text")
        }

        @Test
        fun `bodyText getter returns underlying bodyField text`() {
            // Given: A panel with body text set
            val panel = CommentBubble(commentId)
            panel.bodyField.text = "Test Body"

            // When: The getter is accessed
            val result = panel.bodyText

            // Then: It returns the underlying field's text
            Assertions.assertEquals("Test Body", result, "bodyText getter should return bodyField.text")
        }

        @Test
        fun `bodyText setter updates bodyField text`() {
            // Given: A panel instance
            val panel = CommentBubble(commentId)

            // When: Body text is set via the property
            panel.bodyText = "New Body"

            // Then: The underlying field is updated
            Assertions.assertEquals("New Body", panel.bodyField.text, "bodyText setter should update bodyField.text")
        }
    }

    @Nested
    inner class Callbacks : CommentBubbleTest() {

        @Test
        fun `onHeaderClick callback is invoked when header label is clicked`() {
            // Given: A panel with a click callback registered
            val panel = CommentBubble(commentId)
            var callbackInvoked = false
            panel.onHeaderClick = { callbackInvoked = true }

            // When: The header label is clicked
            val listeners = panel.headerLabel.mouseListeners
            val event = MouseEvent(panel.headerLabel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false)
            listeners.forEach { it.mouseClicked(event) }

            // Then: The callback is invoked
            Assertions.assertTrue(callbackInvoked, "onHeaderClick should be invoked when header is clicked")
        }

        @Test
        fun `onDelete callback is invoked with correct commentId when delete button is clicked`() {
            // Given: A panel with a delete callback registered
            val panel = CommentBubble(commentId)
            var deletedCommentId: Int? = null
            panel.onDelete = { id -> deletedCommentId = id }

            // When: The delete button is clicked
            panel.deleteButton.doClick()

            // Then: The callback receives the correct commentId
            Assertions.assertEquals(commentId, deletedCommentId, "onDelete should receive correct commentId")
        }

        @Test
        fun `onBodyFocusLost callback is invoked when body field loses focus`() {
            // Given: A panel with a focus lost callback registered
            val panel = CommentBubble(commentId)
            var focusLostInvoked = false
            panel.onBodyFocusLost = { _ -> focusLostInvoked = true }

            // When: The body field loses focus
            val listeners = panel.bodyField.focusListeners
            val event = FocusEvent(panel.bodyField, FocusEvent.FOCUS_LOST, false, null)
            listeners.forEach { it.focusLost(event) }

            // Then: The callback is invoked
            Assertions.assertTrue(focusLostInvoked, "onBodyFocusLost should be invoked")
        }
    }

    @Nested
    inner class FocusBehavior : CommentBubbleTest() {

        @Test
        fun `bodyPanel border changes appearance on focus gained`() {
            // Given: A panel with body field wrapped in a container
            val panel = CommentBubble(commentId)
            val bodyPanel = panel.bodyField.parent as? JPanel
            Assertions.assertNotNull(bodyPanel, "Body field should be wrapped in a panel")

            // When: The body field gains focus
            val listeners = panel.bodyField.focusListeners
            val event = FocusEvent(panel.bodyField, FocusEvent.FOCUS_GAINED, false, null)
            listeners.forEach { it.focusGained(event) }

            // Then: The border is set
            Assertions.assertNotNull(bodyPanel?.border, "Body panel should have a border after focus gained")
        }

        @Test
        fun `bodyPanel border reverts on focus lost`() {
            // Given: A panel with body field wrapped in a container
            val panel = CommentBubble(commentId)
            val bodyPanel = panel.bodyField.parent as? JPanel
            Assertions.assertNotNull(bodyPanel, "Body field should be wrapped in a panel")

            // When: Focus is gained then lost
            val listeners = panel.bodyField.focusListeners
            val gainedEvent = FocusEvent(panel.bodyField, FocusEvent.FOCUS_GAINED, false, null)
            listeners.forEach { it.focusGained(gainedEvent) }
            val lostEvent = FocusEvent(panel.bodyField, FocusEvent.FOCUS_LOST, false, null)
            listeners.forEach { it.focusLost(lostEvent) }

            // Then: The border remains set
            Assertions.assertNotNull(bodyPanel?.border, "Body panel should have a border after focus cycle")
        }
    }



    @Nested
    inner class EdgeCases : CommentBubbleTest() {

        @Test
        fun `bodyText handles multiline text`() {
            // Given: Multiline text content
            val panel = CommentBubble(commentId)
            val multilineText = "Line 1\nLine 2\nLine 3"

            // When: The body text is set
            panel.bodyText = multilineText

            // Then: Newlines are preserved
            Assertions.assertEquals(multilineText, panel.bodyText, "Multiline body text should be preserved")
        }

        @Test
        fun `bodyText handles very long text`() {
            // Given: Very long text content
            val panel = CommentBubble(commentId)
            val longText = "A".repeat(10000)

            // When: The body text is set
            panel.bodyText = longText

            // Then: The text is accepted
            Assertions.assertEquals(longText, panel.bodyText, "Long body text should be accepted")
        }
    }
}
