package com.github.cpardi.intellijmarkdownreviewplugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for BaseTestHelper utility methods.
 * Validates that test utilities work correctly before relying on them in other tests.
 */
class BaseTestHelperTest {

    @Test
    fun `test createComment factory method`() {
        // When: Creating a comment using the factory
        val comment = BaseTestHelper.createComment(1, "test.kt", 10, 20, "Test comment")
        
        // Then: Should have all properties set correctly
        assertEquals(1, comment.id, "ID should match")
        assertEquals("test.kt", comment.relativePath, "Path should match")
        assertEquals(10, comment.startLine, "Start line should match")
        assertEquals(20, comment.endLine, "End line should match")
        assertEquals("Test comment", comment.body, "Body should match")
    }

    @Test
    fun `test createPageComment factory method`() {
        // When: Creating a page comment using the factory
        val comment = BaseTestHelper.createPageComment(1, "test.kt", "Page comment")
        
        // Then: Should be a page comment with correct properties
        assertEquals(1, comment.id, "ID should match")
        assertEquals("test.kt", comment.relativePath, "Path should match")
        assertEquals(0, comment.startLine, "Start line should be 0 for page comment")
        assertEquals(0, comment.endLine, "End line should be 0 for page comment")
        assertEquals("Page comment", comment.body, "Body should match")
        assertTrue(comment.isPageComment(), "Should be identified as page comment")
    }

    @Test
    fun `test createReviewFile factory method`() {
        // When: Creating a review file
        val comments = listOf(
            BaseTestHelper.createComment(1, "a.kt", 1, 5, "First"),
            BaseTestHelper.createComment(2, "b.kt", 10, 15, "Second")
        )
        val reviewFile = BaseTestHelper.createReviewFile("test-review", comments)
        
        // Then: Should have correct properties
        assertEquals("test-review", reviewFile.name, "Name should match")
        assertEquals(2, reviewFile.size(), "Should have 2 comments")
        assertFalse(reviewFile.isEmpty(), "Should not be empty")
    }

    @Test
    fun `test createReviewFile empty`() {
        // When: Creating an empty review file
        val reviewFile = BaseTestHelper.createReviewFile("empty-review")
        
        // Then: Should be empty
        assertEquals("empty-review", reviewFile.name, "Name should match")
        assertEquals(0, reviewFile.size(), "Should have 0 comments")
        assertTrue(reviewFile.isEmpty(), "Should be empty")
        assertEquals("", reviewFile.preamble, "Preamble should be empty")
        assertEquals("", reviewFile.postamble, "Postamble should be empty")
    }

    @Test
    fun `test createReviewFileWithMeta factory method`() {
        // When: Creating a review file with metadata
        val comments = listOf(BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment"))
        val reviewFile = BaseTestHelper.createReviewFileWithMeta(
            name = "test-review",
            comments = comments,
            preamble = "# Header",
            postamble = "## Footer"
        )
        
        // Then: Should preserve metadata
        assertEquals("test-review", reviewFile.name, "Name should match")
        assertEquals(1, reviewFile.size(), "Should have 1 comment")
        assertEquals("# Header", reviewFile.preamble, "Preamble should match")
        assertEquals("## Footer", reviewFile.postamble, "Postamble should match")
    }

    @Test
    fun `test buildReviewMarkdown single comment`() {
        // Given: A single comment
        val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Test comment")
        
        // When: Building Markdown
        val markdown = BaseTestHelper.buildReviewMarkdown(comment)
        
        // Then: Should produce correct format
        val expected = "@[test.kt:1:5]:\nTest comment\n---"
        assertEquals(expected, markdown, "Should produce correct markdown")
    }

    @Test
    fun `test buildReviewMarkdown multiple comments`() {
        // Given: Multiple comments
        val comment1 = BaseTestHelper.createComment(1, "a.kt", 1, 5, "First")
        val comment2 = BaseTestHelper.createComment(2, "b.kt", 10, 15, "Second")
        
        // When: Building Markdown
        val markdown = BaseTestHelper.buildReviewMarkdown(comment1, comment2)
        
        // Then: Should produce both comments with delimiters
        val expected = "@[a.kt:1:5]:\nFirst\n---\n@[b.kt:10:15]:\nSecond\n---"
        assertEquals(expected, markdown, "Should produce correct multi-comment markdown")
    }

    @Test
    fun `test buildReviewMarkdown with preamble`() {
        // Given: A comment with preamble
        val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
        
        // When: Building Markdown with preamble
        val markdown = BaseTestHelper.buildReviewMarkdown(comment, preamble = "# Review\n\nProject X")
        
        // Then: Should prepend preamble
        assertTrue(markdown.startsWith("# Review\n\nProject X\n\n@"), "Should start with preamble")
        assertTrue(markdown.contains("@[test.kt:1:5]:"), "Should contain comment header")
    }

    @Test
    fun `test buildReviewMarkdown with postamble`() {
        // Given: A comment with postamble
        val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
        
        // When: Building Markdown with postamble
        val markdown = BaseTestHelper.buildReviewMarkdown(comment, postamble = "\n## Summary")
        
        // Then: Should append postamble
        assertTrue(markdown.contains("Comment\n---"), "Should contain comment and delimiter")
        assertTrue(markdown.endsWith("\n## Summary"), "Should end with postamble")
    }

    @Test
    fun `test buildReviewMarkdown page comment`() {
        // Given: A page comment
        val comment = BaseTestHelper.createPageComment(1, "test.kt", "Whole file comment")
        
        // When: Building Markdown
        val markdown = BaseTestHelper.buildReviewMarkdown(comment)
        
        // Then: Should produce page comment format
        val expected = "@[test.kt]:\nWhole file comment\n---"
        assertEquals(expected, markdown, "Should produce page comment markdown")
    }

    @Test
    fun `test buildReviewMarkdown empty`() {
        // When: Building Markdown with no comments
        val markdown = BaseTestHelper.buildReviewMarkdown()
        
        // Then: Should produce empty string
        assertEquals("", markdown, "Empty comments should produce empty markdown")
    }

    @Test
    fun `test assertCommentEquals identical comments`() {
        // Given: Two identical comments
        val comment1 = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
        val comment2 = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
        
        // When: Comparing them
        // Then: Should not throw exception
        BaseTestHelper.assertCommentEquals(comment1, comment2, "Identical comments should be equal")
    }

    @Test
    fun `test assertCommentEquals both null`() {
        // When: Comparing two null comments
        // Then: Should not throw exception
        BaseTestHelper.assertCommentEquals(null, null, "Two nulls should be considered equal")
    }

    @Test
    fun `test assertCommentListEquals same order`() {
        // Given: Two identical comment lists
        val list1 = listOf(
            BaseTestHelper.createComment(1, "a.kt", 1, 5, "First"),
            BaseTestHelper.createComment(2, "b.kt", 10, 15, "Second")
        )
        val list2 = listOf(
            BaseTestHelper.createComment(1, "a.kt", 1, 5, "First"),
            BaseTestHelper.createComment(2, "b.kt", 10, 15, "Second")
        )
        
        // When: Comparing them
        // Then: Should not throw exception
        BaseTestHelper.assertCommentListEquals(list1, list2, "Identical lists should be equal")
    }

    @Test
    fun `test assertReviewFileEquals identical files`() {
        // Given: Two identical review files
        val comments = listOf(BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment"))
        val file1 = BaseTestHelper.createReviewFileWithMeta(
            name = "review",
            comments = comments,
            preamble = "# Header",
            postamble = "## Footer"
        )
        val file2 = BaseTestHelper.createReviewFileWithMeta(
            name = "review",
            comments = comments,
            preamble = "# Header",
            postamble = "## Footer"
        )
        
        // When: Comparing them
        // Then: Should not throw exception
        BaseTestHelper.assertReviewFileEquals(file1, file2, "Identical review files should be equal")
    }

    @Test
    fun `test assertReviewFileEquals both null`() {
        // When: Comparing two null review files
        // Then: Should not throw exception
        BaseTestHelper.assertReviewFileEquals(null, null, "Two nulls should be considered equal")
    }
}