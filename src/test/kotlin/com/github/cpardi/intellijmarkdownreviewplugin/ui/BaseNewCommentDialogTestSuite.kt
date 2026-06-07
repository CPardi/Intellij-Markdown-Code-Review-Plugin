package com.github.cpardi.intellijmarkdownreviewplugin.ui

import com.github.cpardi.intellijmarkdownreviewplugin.LightPlatformTest
import com.github.cpardi.intellijmarkdownreviewplugin.ReviewBundle
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextArea
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.awt.BorderLayout
import java.util.stream.Stream
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane

@Suppress("JUnitMixedFramework")
class BaseNewCommentDialogTestSuite {

    /**
     * Concrete implementation of BaseNewCommentDialog for testing.
     * Allows verification of all shared functionality that subclasses inherit.
     * Marked as open to allow anonymous subclasses in tests.
     */
    private open class TestableNewCommentDialog(
        project: Project,
        private val locationText: String = "test.txt:1"
    ) : BaseNewCommentDialog(project) {

        var createCalled = false
        var lastBody: String? = null

        override fun getDialogTitle(): String = "Test Dialog"

        override fun getLocationText(): String = locationText

        override fun doCreate(body: String) {
            createCalled = true
            lastBody = body
        }

        // Expose protected textArea for testing
        fun getTextAreaTest(): JBTextArea = textArea

        // Expose protected createCenterPanel for testing
        fun createCenterPanelTest(): javax.swing.JComponent = createCenterPanel()

        // Expose protected doValidate for testing
        fun doValidateTest(): com.intellij.openapi.ui.ValidationInfo? = doValidate()

        // Expose protected doOKAction for testing
        fun doOKActionTest() = doOKAction()

        // Expose protected initDialog for testing
        fun initDialogTest() = initDialog()
    }

    /**
     * Base class for integration tests of BaseNewCommentDialog.
     */
    abstract class BaseNewCommentDialogTest : LightPlatformTest() {

        protected lateinit var service: ReviewService

        override fun setUp() {
            super.setUp()
            service = ReviewService.getInstance(project)
            service.setActiveReview(null)
        }
    }

    @Nested
    inner class DialogInit : BaseNewCommentDialogTest() {

        @Test
        fun `createCenterPanel creates correct UI structure`() {
            // Given: Dialog with default location text
            createVirtualFile("reviews/test-review.md", "")
            service.setActiveReview("test-review")
            val dialog = invokeAndWaitIfNeeded {
                TestableNewCommentDialog(project, "test.xml:5").also { it.initDialogTest() }
            }

            // When: createCenterPanel is called
            val centerPanel = dialog.createCenterPanelTest()

            // Then: Panel has BorderLayout with label and scroll pane
            Assertions.assertTrue(centerPanel is JPanel, "Center panel should be a JPanel")
            centerPanel as JPanel
            val borderLayout = centerPanel.layout as BorderLayout
            Assertions.assertNotNull(borderLayout, "Panel should use BorderLayout")

            // Verify label exists at NORTH
            val northComponent = borderLayout.getLayoutComponent(BorderLayout.NORTH)
            Assertions.assertTrue(northComponent is JLabel, "NORTH component should be a JLabel")
            Assertions.assertEquals("test.xml:5", (northComponent as JLabel).text, "Label should show location text")

            // Verify scroll pane exists at CENTER
            val centerComponent = borderLayout.getLayoutComponent(BorderLayout.CENTER)
            Assertions.assertTrue(centerComponent is JScrollPane, "CENTER component should be a JScrollPane")
        }

        @Test
        fun `dialog title defaults to Add Comment`() {
            // Given: Dialog without custom title override
            createVirtualFile("reviews/test-review.md", "")
            service.setActiveReview("test-review")

            // Create a test dialog that uses the default title from BaseNewCommentDialog
            class DefaultTitleDialog(
                project: Project,
                private val location: String = "test.txt:1"
            ) : BaseNewCommentDialog(project) {

                var createCalled = false
                var lastBody: String? = null

                // Don't override getDialogTitle - use the default
                override fun getLocationText(): String = location

                override fun doCreate(body: String) {
                    createCalled = true
                    lastBody = body
                }

                fun initDialogTest() = initDialog()
            }

            // When: Dialog is initialised
            val dialog = invokeAndWaitIfNeeded {
                DefaultTitleDialog(project).also { it.initDialogTest() }
            }

            // Then: Title defaults to "Add Comment" from ReviewBundle
            Assertions.assertEquals(ReviewBundle.message("addComment"), dialog.title, "Dialog title should default to 'Add Comment'")
        }

        @Test
        fun `text area has correct properties`() {
            // Given: Dialog is initialised
            createVirtualFile("reviews/test-review.md", "")
            service.setActiveReview("test-review")
            val dialog = invokeAndWaitIfNeeded {
                TestableNewCommentDialog(project).also { it.initDialogTest() }
            }

            // When: Text area is accessed
            dialog.createCenterPanelTest() // Ensure UI is created
            val textArea = dialog.getTextAreaTest()

            // Then: Text area properties are correct
            Assertions.assertTrue(textArea.lineWrap, "Text area should have line wrap enabled")
            Assertions.assertTrue(textArea.wrapStyleWord, "Text area should wrap at word boundaries")
            Assertions.assertEquals(ReviewBundle.message("enterComment"), textArea.emptyText.text, "Empty text should be set")
        }
    }

    @Nested
    inner class Validation : BaseNewCommentDialogTest() {

        @Test
        fun `doValidate returns null for text without delimiter`() {
            // Given: Dialog with valid comment text
            createVirtualFile("reviews/test-review.md", "")
            service.setActiveReview("test-review")
            val dialog = invokeAndWaitIfNeeded {
                TestableNewCommentDialog(project).also { it.initDialogTest() }
            }
            dialog.createCenterPanelTest()
            dialog.getTextAreaTest().text = "This is a valid comment"

            // When: Validation is performed
            val validationInfo = dialog.doValidateTest()

            // Then: No validation error
            Assertions.assertNull(validationInfo, "Validation should pass for valid text")
        }

        @ParameterizedTest(name = "\"{0}\" fails")
        @ValueSource(strings = [
            "This comment has --- delimiter",
            "--- starts with delimiter",
            "ends with delimiter ---",
            "--- first --- second ---",
        ])
        fun `doValidate returns error for text containing delimiter`(inputText: String) {
            // Given: Dialog with text containing delimiter
            createVirtualFile("reviews/test-review.md", "")
            service.setActiveReview("test-review")
            val dialog = invokeAndWaitIfNeeded {
                TestableNewCommentDialog(project).also { it.initDialogTest() }
            }
            dialog.createCenterPanelTest()
            dialog.getTextAreaTest().text = inputText

            // When: Validation is performed
            val validationInfo = dialog.doValidateTest()

            // Then: Validation error is returned
            Assertions.assertNotNull(validationInfo, "Validation should have failed")
            Assertions.assertEquals(ReviewBundle.message("delimiterError"), validationInfo?.message, "Error message should be delimiterError")
            Assertions.assertSame(dialog.getTextAreaTest(), validationInfo?.component, "Error should reference text area")
        }
    }

    companion object CommitFlowTestCases {
        @JvmStatic
        fun invalidInputTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of("empty text", ""),
            Arguments.of("whitespace only", "   \n\t   "),
            Arguments.of("text with delimiter", "Invalid --- comment")
        )

        @JvmStatic
        fun validBodyTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of("trimmed body", "  Valid comment with surrounding whitespace  \n", "Valid comment with surrounding whitespace"),
            Arguments.of("exact body", "Exact comment", "Exact comment"),
            Arguments.of("multiline body", "Line 1\nLine 2\nLine 3", "Line 1\nLine 2\nLine 3")
        )
    }

    @Nested
    inner class CommitFlow : BaseNewCommentDialogTest() {

        @ParameterizedTest(name = "doOKAction does not create comment for: {0}")
        @MethodSource("com.github.cpardi.intellijmarkdownreviewplugin.ui.BaseNewCommentDialogTestSuite#invalidInputTestCases")
        fun `doOKAction does not create comment for invalid input`(scenario: String, invalidText: String) {
            // Given: Dialog with invalid input
            createVirtualFile("reviews/test-review.md", "")
            service.setActiveReview("test-review")
            val dialog = invokeAndWaitIfNeeded {
                TestableNewCommentDialog(project).also { it.initDialogTest() }
            }
            invokeAndWaitIfNeeded {
                dialog.createCenterPanelTest()
                dialog.getTextAreaTest().text = invalidText

                // When: OK action is triggered
                dialog.doOKActionTest()
            }

            // Then: doCreate is not called
            Assertions.assertFalse(dialog.createCalled, "doCreate should not be called for $scenario")
        }

        @ParameterizedTest(name = "doOKAction calls doCreate with {0}")
        @MethodSource("com.github.cpardi.intellijmarkdownreviewplugin.ui.BaseNewCommentDialogTestSuite#validBodyTestCases")
        fun `doOKAction calls doCreate with correct body`(description: String, inputText: String, expectedBody: String) {
            // Given: Dialog with valid text
            createVirtualFile("reviews/test-review.md", "")
            service.setActiveReview("test-review")
            val dialog = invokeAndWaitIfNeeded {
                TestableNewCommentDialog(project).also { it.initDialogTest() }
            }

            invokeAndWaitIfNeeded {
                dialog.createCenterPanelTest()
                dialog.getTextAreaTest().text = inputText

                // When: OK action is triggered
                dialog.doOKActionTest()
            }

            // Then: doCreate is called with expected body
            Assertions.assertTrue(dialog.createCalled, "doCreate should be called for ${description}")
            Assertions.assertEquals(expectedBody, dialog.lastBody, "Body should match expected for ${description}")
        }

        @Test
        fun `getLocationText returns correct location`() {
            // Given: Dialog with custom location text
            createVirtualFile("reviews/test-review.md", "")
            service.setActiveReview("test-review")
            val expectedLocation = "myfile.xml:42"
            val dialog = invokeAndWaitIfNeeded {
                TestableNewCommentDialog(project, expectedLocation).also { it.initDialogTest() }
            }

            // When: Location text is retrieved via createCenterPanel
            val centerPanel = dialog.createCenterPanelTest()
            val borderLayout = (centerPanel as JPanel).layout as BorderLayout
            val label = borderLayout.getLayoutComponent(BorderLayout.NORTH) as JLabel

            // Then: Location text matches
            Assertions.assertEquals(expectedLocation, label.text, "Location text should be passed to UI correctly")
        }
    }
}
