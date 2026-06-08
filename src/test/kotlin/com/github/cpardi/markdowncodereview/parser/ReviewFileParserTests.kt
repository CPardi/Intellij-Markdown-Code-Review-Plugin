package com.github.cpardi.markdowncodereview.parser

import com.github.cpardi.markdowncodereview.BaseTestHelper
import com.github.cpardi.markdowncodereview.UnitTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests all parsing scenarios including line comments, page comments,
 * preamble/postamble handling, malformed input, and edge cases.
 */
class ReviewFileParserTests : UnitTest() {

    @Nested
    inner class LineCommentParsing {

        @Test
        fun `test parse single line comment with minimal values`() {
            // Given: A single line comment at line 1
            val content = "@[file.kt:1:1]:\nComment body\n---"

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse correctly with line 1
            assertNotNull(reviewFile)
            assertEquals(1, reviewFile!!.size())
            val comment = reviewFile.comments.first()
            assertEquals(1, comment.id)
            assertEquals("file.kt", comment.relativePath)
            assertEquals(1, comment.startLine)
            assertEquals(1, comment.endLine)
            assertEquals("Comment body", comment.body)
        }

        @Test
        fun `test parse single line comment with multi-line range`() {
            // Given: A comment spanning multiple lines
            val content = "@[Main.kt:10:25]:\nMulti-line comment\n---"

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should preserve the range
            assertNotNull(reviewFile)
            val comment = reviewFile!!.comments.first()
            assertEquals(10, comment.startLine)
            assertEquals(25, comment.endLine)
        }

        @Test
        fun `test parse line comment with empty body`() {
            // Given: A comment with no body text
            val content = "@[file.kt:5:10]:\n---"

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should have empty body
            assertNotNull(reviewFile)
            val comment = reviewFile!!.comments.first()
            assertEquals("", comment.body)
        }

        @Test
        fun `test parse line comment with multi-line body`() {
            // Given: A comment with multiple lines in body
            val content = "@[file.kt:1:5]:\nFirst line\nSecond line\nThird line\n---"

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should preserve newlines in body
            assertNotNull(reviewFile)
            val comment = reviewFile!!.comments.first()
            assertEquals("First line\nSecond line\nThird line", comment.body)
        }

        @Test
        fun `test parse line comment with special characters in body`() {
            // Given: A comment with special characters
            val content = "@[file.kt:1:5]:\nUnicode: äöü 日本語 🚀\nCode: `val x = 42`\n---"

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should preserve all characters
            assertNotNull(reviewFile)
            val comment = reviewFile!!.comments.first()
            assertTrue(comment.body.contains("äöü"))
            assertTrue(comment.body.contains("日本語"))
            assertTrue(comment.body.contains("🚀"))
            assertTrue(comment.body.contains("`val x = 42`"))
        }

        @Test
        fun `test parse line comment with path containing spaces`() {
            // Given: A comment with path containing spaces
            val content = "@[src/main file.kt:1:5]:\nComment\n---"

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse path with spaces
            assertNotNull(reviewFile)
            val comment = reviewFile!!.comments.first()
            assertEquals("src/main file.kt", comment.relativePath)
        }

        @Test
        fun `test parse line comment with path containing dots`() {
            // Given: A comment with path containing dots
            val content = "@[src/com/example/Main.kt:1:10]:\nComment\n---"

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse path correctly
            assertNotNull(reviewFile)
            val comment = reviewFile!!.comments.first()
            assertEquals("src/com/example/Main.kt", comment.relativePath)
        }

        @Test
        fun `test parse line comment with path containing hyphens and underscores`() {
            // Given: A comment with path containing hyphens and underscores
            val content = "@[src/my-module/User_service.kt:1:5]:\nComment\n---"

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse path correctly
            assertNotNull(reviewFile)
            val comment = reviewFile!!.comments.first()
            assertEquals("src/my-module/User_service.kt", comment.relativePath)
        }

        @Test
        fun `test sequential ID assignment`() {
            // Given: Multiple comments
            val content = """
                @[file1.kt:1:5]:
                First
                ---
                @[file2.kt:10:15]:
                Second
                ---
                @[file3.kt:20:25]:
                Third
                ---
            """.trimIndent()

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: IDs should be assigned sequentially
            assertNotNull(reviewFile)
            assertEquals(3, reviewFile!!.size())
            assertEquals(1, reviewFile.comments[0].id)
            assertEquals(2, reviewFile.comments[1].id)
            assertEquals(3, reviewFile.comments[2].id)
        }
    }

    @Nested
    inner class PageCommentParsing {

        @Test
        fun `test parse page comment with body`() {
            // Given: A page comment
            val content = "@[file.kt]:\nThis applies to the whole file\n---"

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should create page comment
            assertNotNull(reviewFile)
            assertEquals(1, reviewFile!!.size())
            val comment = reviewFile.comments.first()
            assertTrue(comment.isPageComment())
            assertEquals(0, comment.startLine)
            assertEquals(0, comment.endLine)
            assertEquals("This applies to the whole file", comment.body)
        }

        @Test
        fun `test parse page comment with empty body`() {
            // Given: A page comment with no body
            val content = "@[file.kt]:\n---"

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should have empty body
            assertNotNull(reviewFile)
            val comment = reviewFile!!.comments.first()
            assertTrue(comment.isPageComment())
            assertEquals("", comment.body)
        }

        @Test
        fun `test parse page comment verifies startLine and endLine`() {
            // Given: A page comment
            val content = "@[path/to/file.kt]:\nBody\n---"

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should set sentinel values
            assertNotNull(reviewFile)
            val comment = reviewFile!!.comments.first()
            assertEquals(0, comment.startLine)
            assertEquals(0, comment.endLine)
            assertTrue(comment.isPageComment())
        }

        @Test
        fun `test parse page comment with special characters in path`() {
            // Given: A page comment with special chars in path
            val content = "@[src/my-app/Component.js]:\nComment\n---"

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse path correctly
            assertNotNull(reviewFile)
            assertEquals("src/my-app/Component.js", reviewFile!!.comments.first().relativePath)
        }

        @Test
        fun `test page comment is identified before line comment`() {
            // Given: A path that could match both patterns (like "file:5")
            // Note: PAGE_HEADER_REGEX is checked after HEADER_REGEX, so line comment matches first
            val content = "@[file.kt:5:10]:\nLine comment\n---"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should be parsed as line comment (more specific pattern)
            assertNotNull(reviewFile)
            assertFalse(reviewFile!!.comments.first().isPageComment())
        }
    }

    @Nested
    inner class PreamblePostamble {

        @Test
        fun `test parse preserves preamble before first header`() {
            // Given: Content with preamble
            val content = "# Code Review\n\nThis is a review.\n\n@[file.kt:1:5]:\nComment\n---"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Preamble should be preserved
            assertNotNull(reviewFile)
            assertEquals("# Code Review\n\nThis is a review.", reviewFile!!.preamble)
        }

        @Test
        fun `test parse handles empty preamble`() {
            // Given: Content starting with header
            val content = "@[file.kt:1:5]:\nComment\n---"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Preamble should be empty
            assertNotNull(reviewFile)
            assertEquals("", reviewFile!!.preamble)
        }

        @Test
        fun `test parse preserves multi-line preamble`() {
            // Given: Preamble with multiple lines
            val content = "# Review\n\n## Project X\n\n- Item 1\n- Item 2\n\n@[file.kt:1:5]:\nC\n---"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Multi-line preamble preserved
            assertNotNull(reviewFile)
            assertTrue(reviewFile!!.preamble.contains("## Project X"))
            assertTrue(reviewFile.preamble.contains("- Item 1"))
        }

        @Test
        fun `test parse handles preamble with blank lines`() {
            // Given: Preamble with blank lines
            val content = "# Header\n\n\nText after blanks\n\n@[file.kt:1:5]:\nC\n---"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should preserve structure (trimEnd applied)
            assertNotNull(reviewFile)
            assertTrue(reviewFile!!.preamble.contains("# Header"))
        }

        @Test
        fun `test parse handles empty postamble`() {
            // Given: Content ending with delimiter
            val content = "@[file.kt:1:5]:\nComment\n---"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Postamble should be empty
            assertNotNull(reviewFile)
            assertEquals("", reviewFile!!.postamble)
        }
    }

    @Nested
    inner class MultiCommentParsing {

        @Test
        fun `test parse multiple comments separated by delimiters`() {
            // Given: Multiple comments
            val content = """
                @[file1.kt:1:10]:
                Comment 1
                ---
                @[file2.kt:20:30]:
                Comment 2
                ---
            """.trimIndent()

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Both should be parsed
            assertNotNull(reviewFile)
            assertEquals(2, reviewFile!!.size())
            assertEquals("Comment 1", reviewFile.comments[0].body)
            assertEquals("Comment 2", reviewFile.comments[1].body)
        }

        @Test
        fun `test parse handles extra blank lines between comments`() {
            // Given: Comments with blank lines
            val content = """
                @[file1.kt:1:5]:
                Comment 1
                ---


                @[file2.kt:10:15]:
                Comment 2
                ---
            """.trimIndent()

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse both comments
            assertNotNull(reviewFile)
            assertEquals(2, reviewFile!!.size())
        }

        @Test
        fun `test parse consecutive delimiters`() {
            // Given: Multiple delimiters in a row
            val content = "@[file.kt:1:5]:\nComment\n---\n---\n---"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse one comment
            assertNotNull(reviewFile)
            assertEquals(1, reviewFile!!.size())
        }

        @Test
        fun `test parse mixed line and page comments`() {
            // Given: Mixed comments
            val content = """
                @[page.kt]:
                Page comment
                ---
                @[line.kt:1:5]:
                Line comment
                ---
            """.trimIndent()

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Both types should be parsed
            assertNotNull(reviewFile)
            assertEquals(2, reviewFile!!.size())
            assertTrue(reviewFile.comments[0].isPageComment())
            assertFalse(reviewFile.comments[1].isPageComment())
        }

        @Test
        fun `test parse preserves comment order exactly as in file`() {
            // Given: Comments in specific order
            val content = """
                @[a.kt:1:5]:
                First
                ---
                @[b.kt:10:15]:
                Second
                ---
                @[c.kt:20:25]:
                Third
                ---
            """.trimIndent()

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Order should be preserved
            assertNotNull(reviewFile)
            assertEquals("a.kt", reviewFile!!.comments[0].relativePath)
            assertEquals("b.kt", reviewFile.comments[1].relativePath)
            assertEquals("c.kt", reviewFile.comments[2].relativePath)
        }
    }

    @Nested
    inner class MalformedInputHandling {

        @Test
        fun `test parse handles missing delimiter at end`() {
            // Given: Comment without ending delimiter
            val content = "@[file.kt:1:5]:\nComment without delimiter"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should still parse (graceful degradation)
            assertNotNull(reviewFile)
            assertEquals(1, reviewFile!!.size())
            assertEquals("Comment without delimiter", reviewFile.comments.first().body)
        }

        @Test
        fun `test parse handles header without body or delimiter`() {
            // Given: Two headers without delimiters
            val content = "@[file1.kt:1:5]:\n@[file2.kt:10:15]:\nBody\n---"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse both (second header stops first body)
            assertNotNull(reviewFile)
            assertEquals(2, reviewFile!!.size())
        }

        @Test
        fun `test parse handles invalid line numbers`() {
            // Given: Comment with unusual line numbers
            val content = "@[file.kt:0:5]:\nComment\n---"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse (parser doesn't validate semantics)
            assertNotNull(reviewFile)
            assertEquals(0, reviewFile!!.comments.first().startLine)
        }

        @Test
        fun `test parse skips unknown content after first header`() {
            // Given: Content with unknown lines
            val content = "@[file.kt:1:5]:\nComment\n---\nUnknown line\nAnother line"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse comment, skip unknown content
            assertNotNull(reviewFile)
            assertEquals(1, reviewFile!!.size())
        }

        @Test
        fun `test parse returns result on badly formed content`() {
            // Given: Content that will not match any header pattern
            val content = "Random text\nNo headers here"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should return ReviewFile with preamble
            assertNotNull(reviewFile)
            assertEquals(0, reviewFile!!.size())
            assertTrue(reviewFile.preamble.isNotEmpty())
        }

        @Test
        fun `test parse handles Windows line endings (CRLF)`() {
            // Given: Content with CRLF
            val content = "@[file.kt:1:5]:\r\nComment\r\n---"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should handle normalized line endings
            assertNotNull(reviewFile)
            assertEquals(1, reviewFile!!.size())
        }

        @Test
        fun `test parse handles content with only delimiters`() {
            // Given: Only delimiters
            val content = "---\n---\n---"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should return empty
            assertNotNull(reviewFile)
            assertEquals(0, reviewFile!!.size())
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `test parse empty file returns empty ReviewFile`() {
            // Given: Empty content
            val content = ""

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("empty", content)

            // Then: Should be empty
            assertNotNull(reviewFile)
            assertEquals(0, reviewFile!!.size())
            assertEquals("", reviewFile.preamble)
            assertEquals("", reviewFile.postamble)
        }

        @Test
        fun `test parse file with only whitespace`() {
            // Given: Whitespace only
            val content = "   \n\n   \n  "

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should be empty after trim
            assertNotNull(reviewFile)
            assertEquals(0, reviewFile!!.size())
        }

        @Test
        fun `test parse handles very large file`() {
            // Given: Many comments
            val sb = StringBuilder()
            for (i in 1..100) {
                sb.append("@[file$i.kt:$i:${i + 10}]:\nComment $i\n---\n")
            }
            val content = sb.toString().trimEnd()

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: All comments should be parsed
            assertNotNull(reviewFile)
            assertEquals(100, reviewFile!!.size())
        }

        @Test
        fun `test parse handles very long single line body`() {
            // Given: Very long body
            val longBody = "x".repeat(15000)
            val content = "@[file.kt:1:5]:\n$longBody\n---"

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should handle long content
            assertNotNull(reviewFile)
            assertEquals(longBody, reviewFile!!.comments.first().body)
        }

        @Test
        fun `test parse handles body with code blocks`() {
            // Given: Comment with code
            val content = """
                @[file.kt:1:10]:
                Here is some code:
                ```kotlin
                fun main() {
                    println("Hello")
                }
                ```
                ---
            """.trimIndent()

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should preserve code block
            assertNotNull(reviewFile)
            assertTrue(reviewFile!!.comments.first().body.contains("```kotlin"))
            assertTrue(reviewFile.comments.first().body.contains("println"))
        }

        @Test
        fun `test parse file using BaseTestHelper`() {
            // Given: Content built by helper
            val comment1 = BaseTestHelper.createComment(1, "a.kt", 1, 5, "First")
            val comment2 = BaseTestHelper.createComment(2, "b.kt", 10, 20, "Second")
            val content = BaseTestHelper.buildReviewMarkdown(comment1, comment2)

            // When: Parsing
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse correctly
            assertNotNull(reviewFile)
            assertEquals(2, reviewFile!!.size())
        }
    }

    @Nested
    inner class TestDataFiles {

        @Test
        fun `test malformed-no-delimiter handles missing delimiter gracefully`() {
            // Given: Test data file with comment missing end delimiter
            val content = BaseTestHelper.loadTestResource("parser/malformed-no-delimiter.md")

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse comment even without delimiter
            assertNotNull(reviewFile)
            assertEquals(1, reviewFile!!.size())
            val comment = reviewFile.comments.first()
            assertEquals("file.kt", comment.relativePath)
            assertEquals(10, comment.startLine)
            assertEquals(20, comment.endLine)
            assertEquals("Comment without delimiter at end", comment.body)
        }

        @Test
        fun `test malformed-header skips invalid headers`() {
            // Given: Test data file with malformed headers
            // Line 1: "Not a valid header format" - preamble
            // Line 2: "@[missing-bracket:1:5]:" - valid header
            // Line 3: "Comment" - body
            // Line 4: "---" - delimiter
            // Line 5-7: postamble (not currently parsed by ReviewFileParser)
            val content = BaseTestHelper.loadTestResource("parser/malformed-header.md")

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse valid comment and treat first line as preamble
            assertNotNull(reviewFile)
            assertEquals(1, reviewFile!!.size())
            assertEquals("Not a valid header format", reviewFile.preamble.trim())
            val comment = reviewFile.comments.first()
            assertEquals("missing-bracket", comment.relativePath)
            assertEquals("Comment", comment.body)
            // Note: postamble is not currently extracted by the parser
            assertEquals("", reviewFile.postamble)
        }

        @Test
        fun `test blank-lines-between handles blank lines and preamble`() {
            // Given: Test data file with blank lines between comments
            val content = BaseTestHelper.loadTestResource("parser/blank-lines-between.md")

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse preamble and both comments
            assertNotNull(reviewFile)
            assertEquals("# Code Review", reviewFile!!.preamble.trim())
            assertEquals(2, reviewFile.size())
            assertEquals("file1.kt", reviewFile.comments[0].relativePath)
            assertEquals("file2.kt", reviewFile.comments[1].relativePath)
        }

        @Test
        fun `test special-chars preserves Unicode and emoji`() {
            // Given: Test data file with special characters
            val content = BaseTestHelper.loadTestResource("parser/special-chars.md")

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should preserve all special characters
            assertNotNull(reviewFile)
            assertEquals(1, reviewFile!!.size())
            val comment = reviewFile.comments.first()
            assertTrue(comment.body.contains("äöü"))
            assertTrue(comment.body.contains("日本語"))
            assertTrue(comment.body.contains("🚀"))
            assertTrue(comment.body.contains("😀"))
            assertTrue(comment.body.contains("`val x = 42`"))
        }

        @Test
        fun `test mixed-comments handles page and line comments`() {
            // Given: Test data file with mixed comment types
            val content = BaseTestHelper.loadTestResource("parser/mixed-comments.md")

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse all four comments correctly
            assertNotNull(reviewFile)
            assertEquals(4, reviewFile!!.size())

            // First comment: page comment
            assertTrue(reviewFile.comments[0].isPageComment())
            assertEquals("page.kt", reviewFile.comments[0].relativePath)
            assertEquals("This is a page comment", reviewFile.comments[0].body)

            // Second comment: line comment
            assertFalse(reviewFile.comments[1].isPageComment())
            assertEquals("line.kt", reviewFile.comments[1].relativePath)
            assertEquals(1, reviewFile.comments[1].startLine)
            assertEquals(10, reviewFile.comments[1].endLine)

            // Third comment: page comment
            assertTrue(reviewFile.comments[2].isPageComment())
            assertEquals("another.kt", reviewFile.comments[2].relativePath)

            // Fourth comment: line comment
            assertFalse(reviewFile.comments[3].isPageComment())
            assertEquals("more.kt", reviewFile.comments[3].relativePath)
        }

        @Test
        fun `test empty-body handles empty comment bodies`() {
            // Given: Test data file with empty comment bodies
            val content = BaseTestHelper.loadTestResource("parser/empty-body.md")

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse both comments with empty bodies
            assertNotNull(reviewFile)
            assertEquals(2, reviewFile!!.size())
            assertEquals("", reviewFile.comments[0].body)
            assertEquals("", reviewFile.comments[1].body)
        }

        @Test
        fun `test postamble handles postamble text`() {
            // Given: Test data file with postamble
            // Note: The parser currently does not extract postamble
            // Postamble support exists in the data model but not in the parser yet
            val content = BaseTestHelper.loadTestResource("parser/postamble.md")

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse the comment correctly
            assertNotNull(reviewFile)
            assertEquals(1, reviewFile!!.size())
            assertEquals("Comment content", reviewFile.comments.first().body)
            // Note: postamble extraction is not currently implemented in ReviewFileParser
            assertEquals("", reviewFile.postamble)
        }

        @Test
        fun `test long-body handles long comment bodies`() {
            // Given: Test data file with long comment body
            val content = BaseTestHelper.loadTestResource("parser/long-body.md")

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse entire long body correctly
            assertNotNull(reviewFile)
            assertEquals(1, reviewFile!!.size())
            val comment = reviewFile.comments.first()
            assertTrue(comment.body.contains("very long comment body"))
            assertTrue(comment.body.contains("Lorem ipsum dolor sit amet"))
            assertTrue(comment.body.contains("More text continues here"))
        }

        @Test
        fun `test many-comments handles 50 comments correctly`() {
            // Given: Test data file with 50 comments
            val content = BaseTestHelper.loadTestResource("parser/many-comments.md")

            // When: Parsing the content
            val reviewFile = ReviewFileParser.parseContent("test", content)

            // Then: Should parse all 50 comments with correct sequential IDs
            assertNotNull(reviewFile)
            assertEquals(50, reviewFile!!.size())

            for (i in 0 until 50) {
                val expectedId = i + 1
                val expectedFile = "file${(i + 1).toString().padStart(2, '0')}.kt"
                val expectedStartLine = i * 5 + 1
                val expectedEndLine = i * 5 + 5
                val expectedBody = "Comment ${i + 1}"

                assertEquals(expectedId, reviewFile.comments[i].id)
                assertEquals(expectedFile, reviewFile.comments[i].relativePath)
                assertEquals(expectedStartLine, reviewFile.comments[i].startLine)
                assertEquals(expectedEndLine, reviewFile.comments[i].endLine)
                assertEquals(expectedBody, reviewFile.comments[i].body)
            }
        }
    }
}
