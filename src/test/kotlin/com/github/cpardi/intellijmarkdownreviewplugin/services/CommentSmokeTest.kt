package com.github.cpardi.intellijmarkdownreviewplugin.services

import com.github.cpardi.intellijmarkdownreviewplugin.UnitTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Smoke tests for Comment and ReviewFile models.
 * These tests validate the test infrastructure and basic model functionality.
 */
class CommentSmokeTest : UnitTest() {

    @Test
    fun `test comment is page comment`() {
        // Given: A page comment (startLine=0, endLine=0)
        val pageComment = Comment(
            id = 1,
            relativePath = "test.kt",
            startLine = 0,
            endLine = 0,
            body = "Whole file comment"
        )
        
        // When: Checking if it's a page comment
        val isPage = pageComment.isPageComment()
        
        // Then: Should return true
        assertTrue(isPage, "Comment with startLine=0 and endLine=0 should be a page comment")
    }

    @Test
    fun `test comment is not page comment`() {
        // Given: A line comment (startLine=1, endLine=5)
        val lineComment = Comment(
            id = 1,
            relativePath = "test.kt",
            startLine = 1,
            endLine = 5,
            body = "Line comment"
        )
        
        // When: Checking if it's a page comment
        val isPage = lineComment.isPageComment()
        
        // Then: Should return false
        assertFalse(isPage, "Comment with non-zero lines should not be a page comment")
    }

    @Test
    fun `test comment valid range for page comment`() {
        // Given: A page comment
        val pageComment = Comment(
            id = 1,
            relativePath = "test.kt",
            startLine = 0,
            endLine = 0,
            body = "Page comment"
        )
        
        // When: Validating the range
        val isValid = pageComment.isValidRange()
        
        // Then: Should be valid
        assertTrue(isValid, "Page comment should have valid range")
    }

    @Test
    fun `test comment valid range for single line`() {
        // Given: A single-line comment
        val comment = Comment(
            id = 1,
            relativePath = "test.kt",
            startLine = 5,
            endLine = 5,
            body = "Single line"
        )
        
        // When: Validating the range
        val isValid = comment.isValidRange()
        
        // Then: Should be valid
        assertTrue(isValid, "Single line comment should have valid range")
    }

    @Test
    fun `test comment valid range for multi line`() {
        // Given: A multi-line comment
        val comment = Comment(
            id = 1,
            relativePath = "test.kt",
            startLine = 10,
            endLine = 20,
            body = "Multi line"
        )
        
        // When: Validating the range
        val isValid = comment.isValidRange()
        
        // Then: Should be valid
        assertTrue(isValid, "Multi-line comment with startLine < endLine should have valid range")
    }

    @Test
    fun `test comment invalid range`() {
        // Given: A comment with invalid range (startLine > endLine)
        val comment = Comment(
            id = 1,
            relativePath = "test.kt",
            startLine = 10,
            endLine = 5,
            body = "Invalid range"
        )
        
        // When: Validating the range
        val isValid = comment.isValidRange()
        
        // Then: Should be invalid
        assertFalse(isValid, "Comment with startLine > endLine should have invalid range")
    }

    @Test
    fun `test comment get line range text single line`() {
        // Given: A single-line comment
        val comment = Comment(
            id = 1,
            relativePath = "test.kt",
            startLine = 5,
            endLine = 5,
            body = "Single line"
        )
        
        // When: Getting line range text
        val rangeText = comment.getLineRangeText()
        
        // Then: Should return just the line number
        assertEquals("5", rangeText, "Single line should return just the line number")
    }

    @Test
    fun `test comment get line range text multi line`() {
        // Given: A multi-line comment
        val comment = Comment(
            id = 1,
            relativePath = "test.kt",
            startLine = 10,
            endLine = 20,
            body = "Multi line"
        )
        
        // When: Getting line range text
        val rangeText = comment.getLineRangeText()
        
        // Then: Should return range
        assertEquals("10-20", rangeText, "Multi-line should return line range")
    }

    @Test
    fun `test comment get line range text page comment`() {
        // Given: A page comment
        val comment = Comment(
            id = 1,
            relativePath = "test.kt",
            startLine = 0,
            endLine = 0,
            body = "Page comment"
        )
        
        // When: Getting line range text
        val rangeText = comment.getLineRangeText()
        
        // Then: Should return empty string
        assertEquals("", rangeText, "Page comment should return empty string for line range")
    }

    @Test
    fun `test comment to header line comment`() {
        // Given: A line comment
        val comment = Comment(
            id = 1,
            relativePath = "src/main.kt",
            startLine = 10,
            endLine = 15,
            body = "Test"
        )
        
        // When: Converting to header
        val header = comment.toHeader()
        
        // Then: Should produce correct header format
        assertEquals("@[src/main.kt:10:15]:", header, "Should produce correct line comment header")
    }

    @Test
    fun `test comment to header page comment`() {
        // Given: A page comment
        val comment = Comment(
            id = 1,
            relativePath = "src/main.kt",
            startLine = 0,
            endLine = 0,
            body = "Test"
        )
        
        // When: Converting to header
        val header = comment.toHeader()
        
        // Then: Should produce page comment header format
        assertEquals("@[src/main.kt]:", header, "Should produce correct page comment header")
    }

    @Test
    fun `test comment contains line`() {
        // Given: A comment spanning lines 10-20
        val comment = Comment(
            id = 1,
            relativePath = "test.kt",
            startLine = 10,
            endLine = 20,
            body = "Test"
        )
        
        // When: Checking if lines are contained
        val contains5 = comment.containsLine(5)
        val contains10 = comment.containsLine(10)
        val contains15 = comment.containsLine(15)
        val contains20 = comment.containsLine(20)
        val contains25 = comment.containsLine(25)
        
        // Then: Should correctly identify contained lines
        assertFalse(contains5, "Line 5 should not be contained")
        assertTrue(contains10, "Line 10 (start) should be contained")
        assertTrue(contains15, "Line 15 (middle) should be contained")
        assertTrue(contains20, "Line 20 (end) should be contained")
        assertFalse(contains25, "Line 25 should not be contained")
    }

    @Test
    fun `test comment overlaps same file`() {
        // Given: Two comments on the same file with overlapping ranges
        val comment1 = Comment(
            id = 1,
            relativePath = "test.kt",
            startLine = 10,
            endLine = 20,
            body = "First"
        )
        val comment2 = Comment(
            id = 2,
            relativePath = "test.kt",
            startLine = 15,
            endLine = 25,
            body = "Second"
        )
        
        // When: Checking overlap
        val overlaps = comment1.overlaps(comment2)
        
        // Then: Should report overlap
        assertTrue(overlaps, "Overlapping comments on same file should return true")
    }

    @Test
    fun `test comment overlaps different files`() {
        // Given: Two comments on different files
        val comment1 = Comment(
            id = 1,
            relativePath = "file1.kt",
            startLine = 10,
            endLine = 20,
            body = "First"
        )
        val comment2 = Comment(
            id = 2,
            relativePath = "file2.kt",
            startLine = 15,
            endLine = 25,
            body = "Second"
        )
        
        // When: Checking overlap
        val overlaps = comment1.overlaps(comment2)
        
        // Then: Should not report overlap
        assertFalse(overlaps, "Comments on different files should not overlap")
    }

    @Test
    fun `test comment overlaps page comment`() {
        // Given: A page comment and a line comment on same file
        val pageComment = Comment(
            id = 1,
            relativePath = "test.kt",
            startLine = 0,
            endLine = 0,
            body = "Page comment"
        )
        val lineComment = Comment(
            id = 2,
            relativePath = "test.kt",
            startLine = 10,
            endLine = 20,
            body = "Line comment"
        )
        
        // When: Checking overlap in both directions
        val pageOverlapsLine = pageComment.overlaps(lineComment)
        val lineOverlapsPage = lineComment.overlaps(pageComment)
        
        // Then: Both should report overlap (page comments overlap all comments on same file)
        assertTrue(pageOverlapsLine, "Page comment should overlap line comment on same file")
        assertTrue(lineOverlapsPage, "Line comment should overlap page comment on same file")
    }
}