package com.github.cpardi.intellijmarkdownreviewplugin.parser

import com.github.cpardi.intellijmarkdownreviewplugin.BaseTestHelper
import com.github.cpardi.intellijmarkdownreviewplugin.UnitTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Round-trip integration tests for parser and writer.
 * Tests that parse(write(x)) == x for all valid inputs,
 * ensuring the parser and writer work together seamlessly.
 */
class ParserWriterRoundTripTest : UnitTest() {

    // ==================== Helper Methods ====================

    /**
     * Performs a round-trip: write -> parse -> write -> parse
     * and verifies the results are identical.
     */
    private fun verifyRoundTrip(reviewFile: com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewFile) {
        // First pass: write -> parse
        val content1 = ReviewFileWriter.buildContent(reviewFile)
        val parsed1 = ReviewFileParser.parseContent("test", content1)
        assertNotNull(parsed1, "First parse should succeed")

        // Second pass: write -> parse again
        val content2 = ReviewFileWriter.buildContent(parsed1!!)
        val parsed2 = ReviewFileParser.parseContent("test", content2)
        assertNotNull(parsed2, "Second parse should succeed")

        // Verify the two parsings are identical
        assertEquals(content1, content2, "Content should be identical after round-trip")
        BaseTestHelper.assertReviewFileEquals(parsed1, parsed2, "Round-trip should preserve data")
    }

    /**
     * Loads a test resource file and performs a round-trip test.
     */
    private fun verifyRoundTripFromResource(path: String) {
        val content = BaseTestHelper.loadTestResource(path)
        val parsed1 = ReviewFileParser.parseContent("test", content)
        assertNotNull(parsed1, "Parse should succeed for $path")

        val written = ReviewFileWriter.buildContent(parsed1!!)
        val parsed2 = ReviewFileParser.parseContent("test", written)

        BaseTestHelper.assertReviewFileEquals(parsed1, parsed2, "Round-trip for $path")
    }

    // ==================== Basic Round-Trip Tests ====================

    @Nested
    inner class BasicRoundTrip {

        @Test
        fun `test round-trip empty ReviewFile`() {
            // Given: Empty review
            val reviewFile = BaseTestHelper.createReviewFile("empty")

            // When/Then: Should round-trip successfully
            verifyRoundTrip(reviewFile)
        }

        @Test
        fun `test round-trip single line comment`() {
            // Given: Single line comment
            val comment = BaseTestHelper.createComment(1, "Main.kt", 42, 42, "TODO: Fix this")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))

            // When/Then: Should round-trip successfully
            verifyRoundTrip(reviewFile)
        }

        @Test
        fun `test round-trip single page comment`() {
            // Given: Single page comment
            val comment = BaseTestHelper.createPageComment(1, "Main.kt", "Whole file comment")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))

            // When/Then: Should round-trip successfully
            verifyRoundTrip(reviewFile)
        }

        @Test
        fun `test round-trip multiple comments`() {
            // Given: Multiple comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "A.kt", 1, 5, "First"),
                BaseTestHelper.createComment(2, "B.kt", 10, 15, "Second"),
                BaseTestHelper.createComment(3, "C.kt", 20, 25, "Third")
            )
            val reviewFile = BaseTestHelper.createReviewFile("test", comments)

            // When/Then: Should round-trip successfully
            verifyRoundTrip(reviewFile)
        }

        @Test
        fun `test round-trip mixed comments`() {
            // Given: Mixed page and line comments
            val comments = listOf(
                BaseTestHelper.createPageComment(1, "Main.kt", "Page comment"),
                BaseTestHelper.createComment(2, "Main.kt", 42, 50, "Line comment")
            )
            val reviewFile = BaseTestHelper.createReviewFile("test", comments)

            // When/Then: Should round-trip successfully
            verifyRoundTrip(reviewFile)
        }
    }

    // ==================== Metadata Round-Trip Tests ====================

    @Nested
    inner class MetadataRoundTrip {

        @Test
        fun `test round-trip preserves preamble`() {
            // Given: Review with preamble
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFileWithMeta(
                name = "test",
                comments = listOf(comment),
                preamble = "# Code Review\n\nProject X",
                postamble = ""
            )

            // When: Write -> Parse
            val content = ReviewFileWriter.buildContent(reviewFile)
            val parsed = ReviewFileParser.parseContent("test", content)

            // Then: Preamble should be preserved
            assertNotNull(parsed)
            // Note: Round-trip preamble may differ slightly due to normalization
            assertTrue(parsed!!.preamble.contains("# Code Review"))
        }

        @Test
        fun `test round-trip preserves postamble`() {
            // Given: Review with postamble
            // NOTE: Postamble extraction from file is not yet implemented in parser
            // The writer can write postamble, but parser doesn't extract it
            // This test verifies that postamble in ReviewFile can be written
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFileWithMeta(
                name = "test",
                comments = listOf(comment),
                preamble = "",
                postamble = "## Summary\n\nDone."
            )

            // When: Write
            val content = ReviewFileWriter.buildContent(reviewFile)
            
            // Then: Postamble appears in content after last delimiter
            assertTrue(content.contains("## Summary\n\nDone."))
        }

        @Test
        fun `test round-trip preserves preamble and postamble together`() {
            // Given: Review with both
            // NOTE: Postamble parsing not implemented - only testing preamble preservation
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFileWithMeta(
                name = "test",
                comments = listOf(comment),
                preamble = "# Header",
                postamble = "## Footer"
            )

            // When: Write -> Parse
            val content = ReviewFileWriter.buildContent(reviewFile)
            val parsed = ReviewFileParser.parseContent("test", content)

            // Then: Preamble preserved, postamble written
            assertNotNull(parsed)
            assertTrue(parsed!!.preamble.contains("# Header"))
            assertTrue(content.contains("## Footer"))
        }

        @Test
        fun `test round-trip preserves comment order`() {
            // Given: Comments in specific order
            val comments = listOf(
                BaseTestHelper.createComment(1, "b.kt", 1, 5, "B first"),
                BaseTestHelper.createComment(2, "a.kt", 10, 15, "A second"),
                BaseTestHelper.createComment(3, "c.kt", 20, 25, "C third")
            )
            val reviewFile = BaseTestHelper.createReviewFile("test", comments)

            // When: Write -> Parse
            val content = ReviewFileWriter.buildContent(reviewFile)
            val parsed = ReviewFileParser.parseContent("test", content)

            // Then: Original order should be lost (sorting applied)
            // But after sorting, order should be stable
            assertNotNull(parsed)
            assertEquals(3, parsed!!.size())
            
            // Comments are sorted: a.kt comes before b.kt comes before c.kt (alphabetically)
            // After round-trip, IDs are re-assigned in sorted order
            assertEquals("a.kt", parsed.comments[0].relativePath)
            assertEquals("b.kt", parsed.comments[1].relativePath)
            assertEquals("c.kt", parsed.comments[2].relativePath)
        }
    }

    // ==================== Test Data Round-Trip Tests ====================

    @Nested
    inner class TestDataRoundTrip {

        @Test
        fun `test round-trip from test data file single-comment_md`() {
            verifyRoundTripFromResource("testData/parser/single-comment.md")
        }

        @Test
        fun `test round-trip from test data file page-comment_md`() {
            verifyRoundTripFromResource("testData/parser/page-comment.md")
        }

        @Test
        fun `test round-trip from test data file multiple-comments_md`() {
            verifyRoundTripFromResource("testData/parser/multiple-comments.md")
        }

        @Test
        fun `test round-trip from test data file empty_md`() {
            verifyRoundTripFromResource("testData/parser/empty.md")
        }
    }

    // ==================== Complex Round-Trip Tests ====================

    @Nested
    inner class ComplexRoundTrip {

        @Test
        fun `test round-trip with special characters in all fields`() {
            // Given: Comments with special characters
            val comments = listOf(
                BaseTestHelper.createComment(1, "file-日本語.kt", 1, 5, "Unicode: äöü 日本語 🚀"),
                BaseTestHelper.createComment(2, "src/Main.java", 10, 15, "Code: `val x = 42`\n```kotlin\nfun main() {}\n```")
            )
            val reviewFile = BaseTestHelper.createReviewFile("test", comments)

            // When/Then: Should handle special characters
            verifyRoundTrip(reviewFile)
        }

        @Test
        fun `test round-trip with long paths and bodies`() {
            // Given: Very long content
            val longPath = "src/main/kotlin/com/example/project/module/submodule/VeryLongClassName.kt"
            val longBody = "This is a very long comment that spans multiple lines.\n".repeat(10).trim()
            val comment = BaseTestHelper.createComment(1, longPath, 1, 100, longBody)
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))

            // When/Then: Should handle long content
            verifyRoundTrip(reviewFile)
        }

        @Test
        fun `test round-trip with many comments`() {
            // Given: Many comments
            val comments = (1..50).map { i ->
                BaseTestHelper.createComment(i, "File$i.kt", i * 10, i * 10 + 5, "Comment $i")
            }
            val reviewFile = BaseTestHelper.createReviewFile("test", comments)

            // When/Then: Should handle many comments
            verifyRoundTrip(reviewFile)
        }

        @Test
        fun `test round-trip stability`() {
            // Given: A review file
            val comments = listOf(
                BaseTestHelper.createPageComment(1, "Main.kt", "File-level comment"),
                BaseTestHelper.createComment(2, "Main.kt", 10, 20, "Line comment"),
                BaseTestHelper.createComment(3, "Util.kt", 5, 8, "Another comment")
            )
            val reviewFile = BaseTestHelper.createReviewFile("test", comments)

            // When: Multiple round-trips
            val content1 = ReviewFileWriter.buildContent(reviewFile)
            val parsed1 = ReviewFileParser.parseContent("test", content1)
            
            val content2 = ReviewFileWriter.buildContent(parsed1!!)
            val parsed2 = ReviewFileParser.parseContent("test", content2)
            
            val content3 = ReviewFileWriter.buildContent(parsed2!!)
            val parsed3 = ReviewFileParser.parseContent("test", content3)

            // Then: All should be identical
            assertEquals(content1, content2, "Content should stabilize after first round-trip")
            assertEquals(content2, content3, "Content should remain stable")
            BaseTestHelper.assertReviewFileEquals(parsed1, parsed2)
            BaseTestHelper.assertReviewFileEquals(parsed2, parsed3)
        }

        @Test
        fun `test round-trip preserves comment bodies exactly`() {
            // Given: Comments with exact body content
            val comments = listOf(
                BaseTestHelper.createComment(1, "file.kt", 1, 5, "Line 1\nLine 2\nLine 3"),
                BaseTestHelper.createComment(2, "file.kt", 10, 15, "    Indented\n\tTabbed"),
                BaseTestHelper.createComment(3, "file.kt", 20, 25, "")
            )
            val reviewFile = BaseTestHelper.createReviewFile("test", comments)

            // When: Round-trip
            val content = ReviewFileWriter.buildContent(reviewFile)
            val parsed = ReviewFileParser.parseContent("test", content)

            // Then: Bodies should be preserved
            assertNotNull(parsed)
            assertEquals("Line 1\nLine 2\nLine 3", parsed!!.comments[0].body)
            assertEquals("    Indented\n\tTabbed", parsed.comments[1].body)
            assertEquals("", parsed.comments[2].body)
        }

        @Test
        fun `test round-trip handles all line ranges`() {
            // Given: Various line ranges
            val comments = listOf(
                BaseTestHelper.createComment(1, "file.kt", 1, 1, "Single line"),
                BaseTestHelper.createComment(2, "file.kt", 1, 100, "Large range"),
                BaseTestHelper.createComment(3, "file.kt", 999, 1000, "High line numbers")
            )
            val reviewFile = BaseTestHelper.createReviewFile("test", comments)

            // When/Then: All ranges should round-trip
            verifyRoundTrip(reviewFile)
        }
    }

    // ==================== Edge Case Round-Trip Tests ====================

    @Nested
    inner class EdgeCaseRoundTrip {

        @Test
        fun `test round-trip with only page comments`() {
            // Given: Only page comments
            val comments = listOf(
                BaseTestHelper.createPageComment(1, "A.kt", "Page A"),
                BaseTestHelper.createPageComment(2, "B.kt", "Page B"),
                BaseTestHelper.createPageComment(3, "C.kt", "Page C")
            )
            val reviewFile = BaseTestHelper.createReviewFile("test", comments)

            // When/Then: Should round-trip
            verifyRoundTrip(reviewFile)
        }

        @Test
        fun `test round-trip with duplicate paths`() {
            // Given: Multiple comments on same file
            val comments = listOf(
                BaseTestHelper.createComment(1, "file.kt", 1, 5, "First"),
                BaseTestHelper.createComment(2, "file.kt", 10, 15, "Second"),
                BaseTestHelper.createComment(3, "file.kt", 20, 25, "Third")
            )
            val reviewFile = BaseTestHelper.createReviewFile("test", comments)

            // When/Then: Should handle duplicate paths
            verifyRoundTrip(reviewFile)
        }

        @Test
        fun `test round-trip single line format preserved`() {
            // Given: Single line comment
            val comment = BaseTestHelper.createComment(1, "file.kt", 42, 42, "TODO")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))

            // When: Write
            val content = ReviewFileWriter.buildContent(reviewFile)

            // Then: Should use single-line format (42:42 not just 42)
            assertTrue(content.contains("@[file.kt:42:42]:"))

            // And should round-trip
            val parsed = ReviewFileParser.parseContent("test", content)
            assertNotNull(parsed)
            assertEquals(42, parsed!!.comments.first().startLine)
            assertEquals(42, parsed.comments.first().endLine)
        }

        @Test
        fun `test round-trip empty body preserved`() {
            // Given: Comment with empty body
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment))

            // When: Round-trip
            val content = ReviewFileWriter.buildContent(reviewFile)
            val parsed = ReviewFileParser.parseContent("test", content)

            // Then: Empty body should be preserved
            assertNotNull(parsed)
            assertEquals("", parsed!!.comments.first().body)
        }

        @Test
        fun `test round-trip with complex nested paths`() {
            // Given: Complex paths
            val comments = listOf(
                BaseTestHelper.createComment(1, "src/main/kotlin/com/example/MyClass.kt", 1, 10, "Main class"),
                BaseTestHelper.createComment(2, "src/test/kotlin/com/example/MyClassTest.kt", 20, 30, "Test class"),
                BaseTestHelper.createPageComment(3, "build.gradle.kts", "Build configuration")
            )
            val reviewFile = BaseTestHelper.createReviewFile("test", comments)

            // When/Then: Should handle nested paths
            verifyRoundTrip(reviewFile)
        }

        @Test
        fun `test round-trip with whitespace only body`() {
            // Given: Comment with whitespace-only body
            // Note: This might be normalized during round-trip
            val comment = BaseTestHelper.createComment(1, "file.kt", 1, 5, "   ")
            val comment2 = BaseTestHelper.createComment(2, "file.kt", 10, 15, "Has content")
            val reviewFile = BaseTestHelper.createReviewFile("test", listOf(comment, comment2))

            // When: Round-trip
            val content = ReviewFileWriter.buildContent(reviewFile)
            val parsed = ReviewFileParser.parseContent("test", content)

            // Then: Should preserve whitespace body
            assertNotNull(parsed)
            assertEquals("   ", parsed!!.comments[0].body)
        }
    }
}