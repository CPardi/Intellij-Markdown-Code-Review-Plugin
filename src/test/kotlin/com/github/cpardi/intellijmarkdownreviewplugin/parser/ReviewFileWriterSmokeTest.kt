package com.github.cpardi.intellijmarkdownreviewplugin.parser

import com.github.cpardi.intellijmarkdownreviewplugin.BaseTestHelper
import com.github.cpardi.intellijmarkdownreviewplugin.UnitTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Smoke tests for ReviewFileWriter.
 * These tests validate the test infrastructure and basic writer functionality.
 */
class ReviewFileWriterSmokeTest : UnitTest() {

    @Test
    fun `test write empty review file`() {
        // Given: An empty ReviewFile
        val reviewFile = BaseTestHelper.createReviewFile("test-review")
        
        // When: Building content
        val content = ReviewFileWriter.buildContent(reviewFile)
        
        // Then: Should produce empty string (no content)
        assertEquals("", content, "Empty review should produce empty content")
    }

    @Test
    fun `test write single line comment`() {
        // Given: A ReviewFile with a single line comment
        val comment = BaseTestHelper.createComment(1, "src/main.kt", 1, 5, "This is a test comment")
        val reviewFile = BaseTestHelper.createReviewFile("test-review", listOf(comment))
        
        // When: Building content
        val content = ReviewFileWriter.buildContent(reviewFile)
        
        // Then: Should produce properly formatted Markdown
        val expected = "@[src/main.kt:1:5]:\nThis is a test comment\n---"
        assertEquals(expected, content, "Should produce correctly formatted markdown")
    }

    @Test
    fun `test write page comment`() {
        // Given: A ReviewFile with a page comment
        val comment = BaseTestHelper.createPageComment(1, "src/main.kt", "Applies to whole file")
        val reviewFile = BaseTestHelper.createReviewFile("test-review", listOf(comment))
        
        // When: Building content
        val content = ReviewFileWriter.buildContent(reviewFile)
        
        // Then: Should produce page comment format
        val expected = "@[src/main.kt]:\nApplies to whole file\n---"
        assertEquals(expected, content, "Should produce page comment format")
    }

    @Test
    fun `test write multiple comments`() {
        // Given: A ReviewFile with multiple comments
        val comment1 = BaseTestHelper.createComment(1, "file1.kt", 1, 10, "First comment")
        val comment2 = BaseTestHelper.createComment(2, "file2.kt", 20, 30, "Second comment")
        val reviewFile = BaseTestHelper.createReviewFile("test-review", listOf(comment1, comment2))
        
        // When: Building content
        val content = ReviewFileWriter.buildContent(reviewFile)
        
        // Then: Should produce both comments separated by newline
        val expected = """
            @[file1.kt:1:10]:
            First comment
            ---
            @[file2.kt:20:30]:
            Second comment
            ---
        """.trimIndent()
        assertEquals(expected, content, "Should produce correctly formatted multi-comment markdown")
    }

    @Test
    fun `test write with preamble`() {
        // Given: A ReviewFile with preamble
        val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Comment")
        val reviewFile = BaseTestHelper.createReviewFileWithMeta(
            name = "test-review",
            comments = listOf(comment),
            preamble = "# Code Review\n\nProject X Review",
            postamble = ""
        )
        
        // When: Building content
        val content = ReviewFileWriter.buildContent(reviewFile)
        
        // Then: Should prepend preamble
        val expected = "# Code Review\n\nProject X Review\n\n@[file.kt:1:5]:\nComment\n---"
        assertEquals(expected, content, "Should include preamble before comments")
    }

    @Test
    fun `test write with postamble`() {
        // Given: A ReviewFile with postamble
        val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Comment")
        val reviewFile = BaseTestHelper.createReviewFileWithMeta(
            name = "test-review",
            comments = listOf(comment),
            preamble = "",
            postamble = "\n## Summary\n\nReview completed."
        )
        
        // When: Building content
        val content = ReviewFileWriter.buildContent(reviewFile)
        
        // Then: Should append postamble
        val expected = "@[file.kt:1:5]:\nComment\n---\n\n## Summary\n\nReview completed."
        assertEquals(expected, content, "Should include postamble after comments")
    }

    @Test
    fun `test write sorts comments`() {
        // Given: Comments out of order (page comments should come first)
        val lineComment = BaseTestHelper.createComment(1, "b.kt", 5, 10, "Line comment")
        val pageComment = BaseTestHelper.createPageComment(2, "a.kt", "Page comment")
        val otherLineComment = BaseTestHelper.createComment(3, "a.kt", 1, 2, "Another line")
        
        val reviewFile = BaseTestHelper.createReviewFile(
            "test-review",
            listOf(lineComment, pageComment, otherLineComment)
        )
        
        // When: Building content
        val content = ReviewFileWriter.buildContent(reviewFile)
        
        // Then: Comments should be sorted (page first, then by path, then by line)
        assertTrue(content.contains("@[a.kt]:"), "Page comment should come first")
        val pageCommentIndex = content.indexOf("@[a.kt]:")
        val lineCommentIndex = content.indexOf("@[b.kt:5:10]:")
        assertTrue(pageCommentIndex < lineCommentIndex, "Page comment should appear before line comments")
    }

    @Test
    fun `test format comment`() {
        // Given: A comment
        val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Test body")
        
        // When: Formatting the comment
        val formatted = ReviewFileWriter.formatComment(comment)
        
        // Then: Should produce correct format
        val expected = "@[test.kt:1:5]:\nTest body\n---"
        assertEquals(expected, formatted, "Should format comment correctly")
    }

    @Test
    fun `test delimiter constant`() {
        // Given: The delimiter constant
        val delimiter = ReviewFileWriter.DELIMITER
        
        // Then: Should be three dashes
        assertEquals("---", delimiter, "Delimiter should be three dashes")
    }
}