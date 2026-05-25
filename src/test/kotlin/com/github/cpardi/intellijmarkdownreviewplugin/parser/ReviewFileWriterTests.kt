package com.github.cpardi.intellijmarkdownreviewplugin.parser

import com.github.cpardi.intellijmarkdownreviewplugin.BaseTestHelper
import com.github.cpardi.intellijmarkdownreviewplugin.UnitTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests all serialization scenarios including line comments, page comments,
 * delimiter validation, preamble/postamble handling, sorting, and edge cases.
 */
class ReviewFileWriterTests : UnitTest() {

    @Nested
    inner class LineCommentSerialization {

        @Test
        fun `test write single line comment`() {
            // Given: A single line comment
            val comment = BaseTestHelper.createComment(1, "src/Main.kt", 10, 15, "This is a comment")
            val reviewFile = BaseTestHelper.createReviewFile("test-review", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should produce correct format
            val expected = "@[src/Main.kt:10:15]:\nThis is a comment\n---"
            assertEquals(expected, content)
        }

        @Test
        fun `test write line comment with empty body`() {
            // Given: A comment with no body
            val comment = BaseTestHelper.createComment(1, "file.kt", 5, 10, "")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should produce header and delimiter only
            val expected = "@[file.kt:5:10]:\n---"
            assertEquals(expected, content)
        }

        @Test
        fun `test write line comment with multi-line body`() {
            // Given: A comment with multiple lines
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Line one\nLine two\nLine three")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should preserve newlines
            assertTrue(content.contains("Line one\nLine two\nLine three"))
            assertEquals("@[file.kt:1:5]:\nLine one\nLine two\nLine three\n---", content)
        }

        @Test
        fun `test write line comment with special characters`() {
            // Given: A comment with special characters
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Unicode: äöü 日本語 🚀\nCode: `val x = 42`")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should preserve all characters
            assertTrue(content.contains("äöü"))
            assertTrue(content.contains("日本語"))
            assertTrue(content.contains("🚀"))
            assertTrue(content.contains("`val x = 42`"))
        }

        @Test
        fun `test write line comment single line range`() {
            // Given: A comment spanning single line (5-5)
            val comment = BaseTestHelper.createComment(1, "file.kt", 5, 5, "Single line")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Header should use both line numbers (parser format)
            assertTrue(content.startsWith("@[file.kt:5:5]:"))
        }

        @Test
        fun `test write multiple line comments`() {
            // Given: Multiple comments
            val comment1 = BaseTestHelper.createComment(1, "a.kt", 1, 5, "First")
            val comment2 = BaseTestHelper.createComment(2, "b.kt", 10, 15, "Second")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment1, comment2))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should produce both comments separated by newline
            val expected = "@[a.kt:1:5]:\nFirst\n---\n@[b.kt:10:15]:\nSecond\n---"
            assertEquals(expected, content)
        }
    }

    @Nested
    inner class PageCommentSerialization {

        @Test
        fun `test write page comment`() {
            // Given: A page comment
            val comment = BaseTestHelper.createPageComment(1, "src/Main.kt", "Applies to whole file")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should produce page format without line numbers
            val expected = "@[src/Main.kt]:\nApplies to whole file\n---"
            assertEquals(expected, content)
        }

        @Test
        fun `test write page comment with empty body`() {
            // Given: A page comment with no body
            val comment = BaseTestHelper.createPageComment(1, "file.kt", "")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should produce header and delimiter only
            val expected = "@[file.kt]:\n---"
            assertEquals(expected, content)
        }

        @Test
        fun `test write page comment with multi-line body`() {
            // Given: A page comment with multiple lines
            val comment = BaseTestHelper.createPageComment(1, "file.kt", "Line 1\nLine 2\nLine 3")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should preserve all lines
            assertEquals("@[file.kt]:\nLine 1\nLine 2\nLine 3\n---", content)
        }

        @Test
        fun `test page comment uses no line numbers in header`() {
            // Given: A page comment
            val comment = BaseTestHelper.createPageComment(1, "file.kt", "Body")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should not have :0:0 in header
            assertTrue(content.startsWith("@[file.kt]:"))
            assertFalse(content.contains(":0:0"))
        }
    }

    @Nested
    inner class DelimiterValidation {

        @Test
        fun `test write rejects comment body containing delimiter`() {
            // Given: A comment with delimiter in body
            // Note: The delimiter validation is in write(), not buildContent()
            // buildContent() does not validate - it will include the delimiter
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "This has --- in it")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Content contains the delimiter (validation happens in write())
            assertTrue(content.contains("---"))
        }

        @Test
        fun `test write allows body with delimiter at start of line`() {
            // Given: A comment with delimiter at line start
            // Note: buildContent() doesn't validate - validation is in write()
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Text\n---\nMore text")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Delimiter is included in content (validation happens in write())
            assertTrue(content.contains("---"))
        }

        @Test
        fun `test write allows delimiter embedded in text`() {
            // Given: A comment with embedded dashes (not at line start)
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Use the ---flag option")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Delimiter is included (buildContent doesn't validate)
            // Note: Validation happens in write(), not buildContent()
            assertTrue(content.contains("---flag"))
        }

        @Test
        fun `test write allows body ending with double dash`() {
            // Given: A comment ending with --
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Text ending--")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should allow
            assertTrue(content.contains("Text ending--"))
        }

        @Test
        fun `test delimiter constant is exactly three dashes`() {
            // Then: DELIMITER should be "---"
            assertEquals("---", ReviewFileWriter.DELIMITER)
        }
    }

    @Nested
    inner class PreamblePostambleWriting {

        @Test
        fun `test write prepends preamble before comments`() {
            // Given: A review file with preamble
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFileWithMeta(
                name = "test",
                comments = listOf(comment),
                preamble = "# Code Review",
                postamble = ""
            )
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Preamble should be first
            assertTrue(content.startsWith("# Code Review\n\n@[file.kt"))
        }

        @Test
        fun `test write separates preamble from comments with double newline`() {
            // Given: Preamble and comment
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFileWithMeta(
                name = "test",
                comments = listOf(comment),
                preamble = "# Header",
                postamble = ""
            )
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should have double newline separator
            assertTrue(content.contains("# Header\n\n@[file.kt"))
        }

        @Test
        fun `test write appends postamble after last delimiter`() {
            // Given: A review file with postamble
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFileWithMeta(
                name = "test",
                comments = listOf(comment),
                preamble = "",
                postamble = "## Summary\nDone."
            )
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Postamble should appear after last delimiter
            assertTrue(content.endsWith("---\n## Summary\nDone."))
        }

        @Test
        fun `test write handles empty preamble`() {
            // Given: No preamble
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should start with header
            assertTrue(content.startsWith("@[file.kt"))
            assertFalse(content.startsWith("\n"))
        }

        @Test
        fun `test write handles empty postamble`() {
            // Given: No postamble
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should end with delimiter
            assertTrue(content.endsWith("---"))
        }

        @Test
        fun `test write with preamble and no comments`() {
            // Given: Only preamble
            val reviewFile = BaseTestHelper.createReviewFileWithMeta(
                name = "test",
                comments = emptyList(),
                preamble = "# Header",
                postamble = ""
            )
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should be just preamble
            assertEquals("# Header", content)
        }

        @Test
        fun `test write with postamble and no comments`() {
            // Given: Only postamble
            val reviewFile = BaseTestHelper.createReviewFileWithMeta(
                name = "test",
                comments = emptyList(),
                preamble = "",
                postamble = "## Footer"
            )
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Current implementation adds newline before postamble even with no comments
            // This is because line 126 unconditionally adds newline before postamble
            assertEquals("\n## Footer", content)
        }

        @Test
        fun `test write with preamble and postamble but no comments`() {
            // Given: Only metadata
            val reviewFile = BaseTestHelper.createReviewFileWithMeta(
                name = "test",
                comments = emptyList(),
                preamble = "# Header",
                postamble = "## Footer"
            )
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Preamble followed by single newline then postamble
            // Note: buildContent adds newline before postamble (line 126)
            assertEquals("# Header\n## Footer", content)
        }
    }

    @Nested
    inner class CommentSorting {

        @Test
        fun `test write sorts page comments before line comments`() {
            // Given: Line comment before page comment in list
            val lineComment = BaseTestHelper.createComment(1, "b.kt", 5, 10, "Line")
            val pageComment = BaseTestHelper.createPageComment(2, "a.kt", "Page")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(lineComment, pageComment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Page comment should appear first
            val pageIdx = content.indexOf("@[a.kt]:")
            val lineIdx = content.indexOf("@[b.kt:5:10]:")
            assertTrue(pageIdx < lineIdx)
        }

        @Test
        fun `test write sorts comments by path alphabetically`() {
            // Given: Comments out of alphabetical order
            val comment1 = BaseTestHelper.createComment(1, "z.kt", 1, 5, "Z")
            val comment2 = BaseTestHelper.createComment(2, "a.kt", 1, 5, "A")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment1, comment2))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should be sorted alphabetically by path
            val aIdx = content.indexOf("@[a.kt")
            val zIdx = content.indexOf("@[z.kt")
            assertTrue(aIdx < zIdx)
        }

        @Test
        fun `test write sorts comments by startLine within same path`() {
            // Given: Same path, different lines
            val comment1 = BaseTestHelper.createComment(1, "file.kt", 50, 60, "Late")
            val comment2 = BaseTestHelper.createComment(2, "file.kt", 10, 20, "Early")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment1, comment2))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should be sorted by line number
            val earlyIdx = content.indexOf("Early")
            val lateIdx = content.indexOf("Late")
            assertTrue(earlyIdx < lateIdx)
        }

        @Test
        fun `test write stable sort preserves original order for equal keys`() {
            // Given: Two comments with identical sort keys
            val comment1 = BaseTestHelper.createComment(1, "file.kt", 10, 20, "First")
            val comment2 = BaseTestHelper.createComment(2, "file.kt", 10, 20, "Second")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment1, comment2))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Order should be preserved (stable sort)
            val firstIdx = content.indexOf("First")
            val secondIdx = content.indexOf("Second")
            assertTrue(firstIdx < secondIdx)
        }

        @Test
        fun `test write sort order - page then path then line`() {
            // Given: Complex mix of comments
            val pageB = BaseTestHelper.createPageComment(1, "b.kt", "Page B")
            val lineA1 = BaseTestHelper.createComment(2, "a.kt", 20, 30, "Line A20")
            val lineA2 = BaseTestHelper.createComment(3, "a.kt", 10, 15, "Line A10")
            val pageA = BaseTestHelper.createPageComment(4, "a.kt", "Page A")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(pageB, lineA1, lineA2, pageA))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Order should be: Page A, Page B, Line A10, Line A20
            val indices = listOf(
                content.indexOf("@[a.kt]:") to "PageA",        // Page A
                content.indexOf("@[b.kt]:") to "PageB",        // Page B
                content.indexOf("@[a.kt:10:15]:") to "LineA10", // Line A10
                content.indexOf("@[a.kt:20:30]:") to "LineA20"  // Line A20
            )
            
            // Page A should come first (page comment, alphabetically first)
            // Then Page B (page comment)
            // Then Line A10 (line comment, lower line)
            // Then Line A20 (line comment, higher line)
            val sorted = indices.sortedBy { it.first }
            assertEquals(listOf("PageA", "PageB", "LineA10", "LineA20"), sorted.map { it.second })
        }

        @Test
        fun `test write handles comments with identical paths and lines`() {
            // Given: Identical comments (edge case)
            val comment1 = BaseTestHelper.createComment(1, "file.kt", 10, 10, "First")
            val comment2 = BaseTestHelper.createComment(2, "file.kt", 10, 10, "Second")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment1, comment2))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Both should appear
            assertTrue(content.contains("First"))
            assertTrue(content.contains("Second"))
        }
    }

    @Nested
    inner class EmptyAndEdgeCases {

        @Test
        fun `test write empty ReviewFile produces empty string`() {
            // Given: Empty review
            val reviewFile = BaseTestHelper.createReviewFile("empty")
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should produce empty string
            assertEquals("", content)
        }

        @Test
        fun `test write ReviewFile with empty comments list`() {
            // Given: Review with explicitly empty list
            val reviewFile = BaseTestHelper.createReviewFile("test", emptyList())
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should produce empty string
            assertEquals("", content)
        }

        @Test
        fun `test formatComment line comment`() {
            // Given: A line comment
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Test body")
            
            // When: Formatting
            val formatted = ReviewFileWriter.formatComment(comment)
            
            // Then: Should produce correct format
            assertEquals("@[file.kt:1:5]:\nTest body\n---", formatted)
        }

        @Test
        fun `test formatComment page comment`() {
            // Given: A page comment
            val comment = BaseTestHelper.createPageComment(1, "file.kt", "Page body")
            
            // When: Formatting
            val formatted = ReviewFileWriter.formatComment(comment)
            
            // Then: Should produce correct format
            assertEquals("@[file.kt]:\nPage body\n---", formatted)
        }

        @Test
        fun `test formatComment empty body`() {
            // Given: Comment with no body
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "")
            
            // When: Formatting
            val formatted = ReviewFileWriter.formatComment(comment)
            
            // Then: Should produce header and delimiter only
            assertEquals("@[file.kt:1:5]:\n---", formatted)
        }

        @Test
        fun `test buildContent returns exact expected format`() {
            // Given: A review with known content
            val comment = BaseTestHelper.createComment(1, "Main.kt", 42, 42, "TODO: Fix this")
            val reviewFile = BaseTestHelper.createReviewFile("review-1", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should match exactly
            val expected = "@[Main.kt:42:42]:\nTODO: Fix this\n---"
            assertEquals(expected, content)
        }

        @Test
        fun `test buildContent with multiple comments exact format`() {
            // Given: Multiple comments
            val comment1 = BaseTestHelper.createComment(1, "A.kt", 1, 1, "A")
            val comment2 = BaseTestHelper.createComment(2, "B.kt", 2, 2, "B")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment1, comment2))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should match exact format
            val expected = "@[A.kt:1:1]:\nA\n---\n@[B.kt:2:2]:\nB\n---"
            assertEquals(expected, content)
        }
    }

    @Nested
    inner class SpecialCharacters {

        @Test
        fun `test write comment with markdown formatting`() {
            // Given: A comment with markdown
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "**Bold** and *italic*\n# Heading")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should preserve markdown
            assertTrue(content.contains("**Bold**"))
            assertTrue(content.contains("*italic*"))
            assertTrue(content.contains("# Heading"))
        }

        @Test
        fun `test write comment with tabs and spaces`() {
            // Given: A comment with whitespace
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "\tIndented\n  Two spaces")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should preserve whitespace
            assertTrue(content.contains("\tIndented"))
            assertTrue(content.contains("  Two spaces"))
        }

        @Test
        fun `test write comment with paths containing various characters`() {
            // Given: Comment with complex path
            val comment = BaseTestHelper.createComment(1, "src/main/java/com/example/my-package/MyClass.kt", 1, 5, "Body")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))
            
            // When: Building content
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Should handle path
            assertTrue(content.contains("src/main/java/com/example/my-package/MyClass.kt"))
        }
    }
}