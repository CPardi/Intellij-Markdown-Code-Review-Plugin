package com.github.cpardi.intellijmarkdownreviewplugin.parser

import com.github.cpardi.intellijmarkdownreviewplugin.BaseTestHelper
import com.github.cpardi.intellijmarkdownreviewplugin.UnitTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Smoke tests for ReviewFileParser.
 * These tests validate the test infrastructure and basic parser functionality.
 */
class ReviewFileParserSmokeTest : UnitTest() {

    @Test
    fun `test parse empty review file`() {
        // Given: Empty Markdown content
        val content = ""
        
        // When: Parsing the content
        val reviewFile = ReviewFileParser.parseContent("test-review", content)
        
        // Then: Should return a ReviewFile with no comments
        assertNotNull(reviewFile, "Parser should return a ReviewFile for empty content")
        assertEquals("test-review", reviewFile!!.name, "Review name should match")
        assertTrue(reviewFile.isEmpty(), "Empty content should produce empty review")
        assertEquals(0, reviewFile.size(), "Empty content should have zero comments")
    }

    @Test
    fun `test parse single line comment`() {
        // Given: A single line comment
        val content = "@[src/main.kt:1:5]:\nThis is a test comment\n---"
        
        // When: Parsing the content
        val reviewFile = ReviewFileParser.parseContent("test-review", content)
        
        // Then: Should return a ReviewFile with one comment
        assertNotNull(reviewFile, "Parser should return a ReviewFile")
        assertNotNull(reviewFile?.let { file ->
            assertEquals(1, file.size(), "Should have exactly one comment")
            
            val comment = file.comments.first()
            assertEquals(1, comment.id, "Comment ID should be 1")
            assertEquals("src/main.kt", comment.relativePath, "Path should match")
            assertEquals(1, comment.startLine, "Start line should be 1")
            assertEquals(5, comment.endLine, "End line should be 5")
            assertEquals("This is a test comment", comment.body, "Body should match")
        })
    }

    @Test
    fun `test parse page comment`() {
        // Given: A page comment (no line numbers)
        val content = "@[src/main.kt]:\nThis applies to the whole file\n---"
        
        // When: Parsing the content
        val reviewFile = ReviewFileParser.parseContent("test-review", content)
        
        // Then: Should return a ReviewFile with a page comment
        assertNotNull(reviewFile, "Parser should return a ReviewFile")
        assertNotNull(reviewFile?.let { file ->
            assertEquals(1, file.size(), "Should have exactly one comment")
            
            val comment = file.comments.first()
            assertTrue(comment.isPageComment(), "Should be a page comment")
            assertEquals(0, comment.startLine, "Page comment should have startLine=0")
            assertEquals(0, comment.endLine, "Page comment should have endLine=0")
            assertEquals("This applies to the whole file", comment.body, "Body should match")
        })
    }

    @Test
    fun `test parse multiple comments`() {
        // Given: Multiple comments separated by delimiters
        val content = """
            @[file1.kt:1:10]:
            Comment on file1
            ---
            @[file2.kt:20:30]:
            Comment on file2
            ---
        """.trimIndent()
        
        // When: Parsing the content
        val reviewFile = ReviewFileParser.parseContent("test-review", content)
        
        // Then: Should return a ReviewFile with two comments
        assertNotNull(reviewFile, "Parser should return a ReviewFile")
        assertNotNull(reviewFile?.let { file ->
            assertEquals(2, file.size(), "Should have exactly two comments")
            
            val firstComment = file.comments[0]
            assertEquals("file1.kt", firstComment.relativePath)
            assertEquals(1, firstComment.startLine)
            assertEquals(10, firstComment.endLine)
            
            val secondComment = file.comments[1]
            assertEquals("file2.kt", secondComment.relativePath)
            assertEquals(20, secondComment.startLine)
            assertEquals(30, secondComment.endLine)
        })
    }

    @Test
    fun `test parse with preamble`() {
        // Given: Content with preamble text before first header
        val content = "# Code Review\n\nThis is a review for project X.\n\n@[file.kt:1:5]:\nComment\n---"
        
        // When: Parsing the content
        val reviewFile = ReviewFileParser.parseContent("test-review", content)
        
        // Then: Should preserve preamble
        assertNotNull(reviewFile, "Parser should return a ReviewFile")
        assertNotNull(reviewFile?.let { file ->
            assertEquals("# Code Review\n\nThis is a review for project X.", file.preamble)
            assertEquals(1, file.size(), "Should have exactly one comment")
        })
    }

    @Test
    fun `test parse preserves comment order`() {
        // Given: Multiple comments in specific order
        val comment1 = BaseTestHelper.createComment(1, "a.kt", 1, 5, "First")
        val comment2 = BaseTestHelper.createComment(2, "b.kt", 10, 20, "Second")
        val comment3 = BaseTestHelper.createComment(3, "c.kt", 30, 40, "Third")
        
        val content = BaseTestHelper.buildReviewMarkdown(comment1, comment2, comment3)
        
        // When: Parsing the content
        val reviewFile = ReviewFileParser.parseContent("test-review", content)
        
        // Then: Should preserve order
        assertNotNull(reviewFile, "Parser should return a ReviewFile")
        assertNotNull(reviewFile?.let { file ->
            assertEquals(3, file.size(), "Should have three comments")
            assertEquals(1, file.comments[0].id)
            assertEquals(2, file.comments[1].id)
            assertEquals(3, file.comments[2].id)
        })
    }
}
