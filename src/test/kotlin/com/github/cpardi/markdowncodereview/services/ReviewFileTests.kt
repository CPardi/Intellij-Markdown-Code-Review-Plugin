package com.github.cpardi.markdowncodereview.services

import com.github.cpardi.markdowncodereview.BaseTestHelper
import com.github.cpardi.markdowncodereview.UnitTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests all query methods, mutation operations, ID generation, preamble/postamble handling, and edge cases.
 */
class ReviewFileTests : UnitTest() {

    @Nested
    inner class IsEmpty {

        @Test
        fun `test isEmpty returns true for empty review`() {
            // Given: An empty review file
            val reviewFile = BaseTestHelper.createReviewFile("empty-review")

            // When: Checking if empty
            val isEmpty = reviewFile.isEmpty()

            // Then: Should return true
            assertTrue(isEmpty, "Review with no comments should be empty")
        }

        @Test
        fun `test isEmpty returns false for review with comments`() {
            // Given: A review file with comments
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Checking if empty
            val isEmpty = reviewFile.isEmpty()

            // Then: Should return false
            assertFalse(isEmpty, "Review with comments should not be empty")
        }

        @Test
        fun `test isEmpty returns false for review with only page comments`() {
            // Given: A review file with only page comments
            val pageComment = BaseTestHelper.createPageComment(1, "test.kt", "Page comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(pageComment))

            // When: Checking if empty
            val isEmpty = reviewFile.isEmpty()

            // Then: Should return false (page comments count as comments)
            assertFalse(isEmpty, "Review with page comments should not be empty")
        }
    }

    @Nested
    inner class Size {

        @Test
        fun `test size returns zero for empty review`() {
            // Given: An empty review file
            val reviewFile = BaseTestHelper.createReviewFile("empty")

            // When: Getting size
            val size = reviewFile.size()

            // Then: Should return 0
            assertEquals(0, size, "Empty review should have size 0")
        }

        @Test
        fun `test size returns correct count for single comment`() {
            // Given: A review with one comment
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Getting size
            val size = reviewFile.size()

            // Then: Should return 1
            assertEquals(1, size, "Review with one comment should have size 1")
        }

        @Test
        fun `test size returns correct count for multiple comments`() {
            // Given: A review with multiple comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "a.kt", 1, 5, "First"),
                BaseTestHelper.createComment(2, "b.kt", 10, 15, "Second"),
                BaseTestHelper.createComment(3, "c.kt", 20, 25, "Third")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Getting size
            val size = reviewFile.size()

            // Then: Should return 3
            assertEquals(3, size, "Review with three comments should have size 3")
        }

        @Test
        fun `test size includes page comments`() {
            // Given: A review with both line and page comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "a.kt", 1, 5, "Line comment"),
                BaseTestHelper.createPageComment(2, "a.kt", "Page comment")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Getting size
            val size = reviewFile.size()

            // Then: Should count both types
            assertEquals(2, size, "Size should include both line and page comments")
        }
    }

    @Nested
    inner class GetCommentById {

        @Test
        fun `test getCommentById returns comment when found`() {
            // Given: A review with comments
            val comment1 = BaseTestHelper.createComment(1, "a.kt", 1, 5, "First")
            val comment2 = BaseTestHelper.createComment(2, "b.kt", 10, 15, "Second")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment1, comment2))

            // When: Looking up by ID
            val found = reviewFile.getCommentById(2)

            // Then: Should return the correct comment
            assertNotNull(found, "Should find comment with ID 2")
            assertEquals(2, found!!.id, "Found comment should have ID 2")
            assertEquals("b.kt", found.relativePath, "Found comment should have correct path")
        }

        @Test
        fun `test getCommentById returns null when not found`() {
            // Given: A review with comments
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Looking up non-existent ID
            val found = reviewFile.getCommentById(999)

            // Then: Should return null
            assertNull(found, "Should return null for non-existent ID")
        }

        @Test
        fun `test getCommentById returns null for empty review`() {
            // Given: An empty review
            val reviewFile = BaseTestHelper.createReviewFile("empty")

            // When: Looking up any ID
            val found = reviewFile.getCommentById(1)

            // Then: Should return null
            assertNull(found, "Empty review should return null for any ID")
        }

        @Test
        fun `test getCommentById finds page comment`() {
            // Given: A review with a page comment
            val pageComment = BaseTestHelper.createPageComment(5, "test.kt", "Page comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(pageComment))

            // When: Looking up the page comment ID
            val found = reviewFile.getCommentById(5)

            // Then: Should find the page comment
            assertNotNull(found, "Should find page comment")
            assertTrue(found!!.isPageComment(), "Found comment should be a page comment")
        }
    }

    @Nested
    inner class GetCommentsForFile {

        @Test
        fun `test getCommentsForFile returns matching comments`() {
            // Given: A review with comments on multiple files
            val comments = listOf(
                BaseTestHelper.createComment(1, "src/Main.kt", 1, 5, "Main comment 1"),
                BaseTestHelper.createComment(2, "src/Utils.kt", 10, 15, "Utils comment"),
                BaseTestHelper.createComment(3, "src/Main.kt", 20, 25, "Main comment 2")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Filtering by file path
            val mainComments = reviewFile.getCommentsForFile("src/Main.kt")

            // Then: Should return only matching comments
            assertEquals(2, mainComments.size, "Should find 2 comments for Main.kt")
            assertTrue(mainComments.all { it.relativePath == "src/Main.kt" }, "All results should match path")
        }

        @Test
        fun `test getCommentsForFile returns empty list for no matches`() {
            // Given: A review with comments on specific files
            val comment = BaseTestHelper.createComment(1, "src/Main.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Filtering by different file path
            val otherComments = reviewFile.getCommentsForFile("src/Other.kt")

            // Then: Should return empty list
            assertTrue(otherComments.isEmpty(), "Should return empty list for non-matching path")
        }

        @Test
        fun `test getCommentsForFile returns empty list for empty review`() {
            // Given: An empty review
            val reviewFile = BaseTestHelper.createReviewFile("empty")

            // When: Filtering by any path
            val comments = reviewFile.getCommentsForFile("test.kt")

            // Then: Should return empty list
            assertTrue(comments.isEmpty(), "Empty review should return empty list")
        }

        @Test
        fun `test getCommentsForFile includes both line and page comments`() {
            // Given: A review with both line and page comments on same file
            val comments = listOf(
                BaseTestHelper.createComment(1, "test.kt", 10, 15, "Line comment"),
                BaseTestHelper.createPageComment(2, "test.kt", "Page comment")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Filtering by file path
            val fileComments = reviewFile.getCommentsForFile("test.kt")

            // Then: Should include both types
            assertEquals(2, fileComments.size, "Should include both line and page comments")
        }

        @Test
        fun `test getCommentsForFile preserves order`() {
            // Given: Multiple comments on same file
            val comments = listOf(
                BaseTestHelper.createComment(1, "test.kt", 1, 5, "First"),
                BaseTestHelper.createComment(2, "test.kt", 10, 15, "Second"),
                BaseTestHelper.createComment(3, "test.kt", 20, 25, "Third")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Getting comments for file
            val fileComments = reviewFile.getCommentsForFile("test.kt")

            // Then: Should preserve insertion order
            assertEquals(listOf(1, 2, 3), fileComments.map { it.id }, "Should preserve comment order")
        }
    }

    @Nested
    inner class GetPageCommentsForFile {

        @Test
        fun `test getPageCommentsForFile returns only page comments`() {
            // Given: A review with both line and page comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "test.kt", 10, 15, "Line comment"),
                BaseTestHelper.createPageComment(2, "test.kt", "Page comment 1"),
                BaseTestHelper.createPageComment(3, "test.kt", "Page comment 2")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Getting page comments
            val pageComments = reviewFile.getPageCommentsForFile("test.kt")

            // Then: Should return only page comments
            assertEquals(2, pageComments.size, "Should return only page comments")
            assertTrue(pageComments.all { it.isPageComment() }, "All results should be page comments")
        }

        @Test
        fun `test getPageCommentsForFile does not return line comments`() {
            // Given: A review with only line comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "test.kt", 1, 5, "Line 1"),
                BaseTestHelper.createComment(2, "test.kt", 10, 15, "Line 2")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Getting page comments
            val pageComments = reviewFile.getPageCommentsForFile("test.kt")

            // Then: Should return empty list
            assertTrue(pageComments.isEmpty(), "Should not return line comments")
        }

        @Test
        fun `test getPageCommentsForFile filters by path`() {
            // Given: Page comments on different files
            val comments = listOf(
                BaseTestHelper.createPageComment(1, "Main.kt", "Main page"),
                BaseTestHelper.createPageComment(2, "Utils.kt", "Utils page")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Filtering by specific path
            val mainPageComments = reviewFile.getPageCommentsForFile("Main.kt")

            // Then: Should return only matching page comments
            assertEquals(1, mainPageComments.size, "Should find one page comment for Main.kt")
            assertEquals("Main.kt", mainPageComments.first().relativePath, "Should match path filter")
        }

        @Test
        fun `test getPageCommentsForFile returns empty for empty review`() {
            // Given: An empty review
            val reviewFile = BaseTestHelper.createReviewFile("empty")

            // When: Getting page comments
            val pageComments = reviewFile.getPageCommentsForFile("test.kt")

            // Then: Should return empty list
            assertTrue(pageComments.isEmpty(), "Empty review should return empty list")
        }
    }

    @Nested
    inner class GetCommentsForLine {

        @Test
        fun `test getCommentsForLine returns comments spanning line`() {
            // Given: Comments spanning different lines
            val comments = listOf(
                BaseTestHelper.createComment(1, "test.kt", 10, 20, "Spans 10-20"),
                BaseTestHelper.createComment(2, "test.kt", 15, 25, "Spans 15-25"),
                BaseTestHelper.createComment(3, "test.kt", 30, 40, "Spans 30-40")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Getting comments for line 18
            val lineComments = reviewFile.getCommentsForLine("test.kt", 18)

            // Then: Should return comments that span line 18
            assertEquals(2, lineComments.size, "Should find comments spanning line 18")
            assertTrue(lineComments.any { it.id == 1 }, "Should include comment 1 (10-20)")
            assertTrue(lineComments.any { it.id == 2 }, "Should include comment 2 (15-25)")
        }

        @Test
        fun `test getCommentsForLine at start boundary`() {
            // Given: A comment starting at line 10
            val comment = BaseTestHelper.createComment(1, "test.kt", 10, 20, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Getting comments for start line
            val lineComments = reviewFile.getCommentsForLine("test.kt", 10)

            // Then: Should include the comment (inclusive start)
            assertEquals(1, lineComments.size, "Should find comment at start boundary")
        }

        @Test
        fun `test getCommentsForLine at end boundary`() {
            // Given: A comment ending at line 20
            val comment = BaseTestHelper.createComment(1, "test.kt", 10, 20, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Getting comments for end line
            val lineComments = reviewFile.getCommentsForLine("test.kt", 20)

            // Then: Should include the comment (inclusive end)
            assertEquals(1, lineComments.size, "Should find comment at end boundary")
        }

        @Test
        fun `test getCommentsForLine before range`() {
            // Given: A comment spanning lines 10-20
            val comment = BaseTestHelper.createComment(1, "test.kt", 10, 20, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Getting comments for line before range
            val lineComments = reviewFile.getCommentsForLine("test.kt", 5)

            // Then: Should return empty list
            assertTrue(lineComments.isEmpty(), "Should not find comment before range")
        }

        @Test
        fun `test getCommentsForLine after range`() {
            // Given: A comment spanning lines 10-20
            val comment = BaseTestHelper.createComment(1, "test.kt", 10, 20, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Getting comments for line after range
            val lineComments = reviewFile.getCommentsForLine("test.kt", 25)

            // Then: Should return empty list
            assertTrue(lineComments.isEmpty(), "Should not find comment after range")
        }

        @Test
        fun `test getCommentsForLine excludes page comments`() {
            // Given: A page comment and a line comment on same file
            val comments = listOf(
                BaseTestHelper.createPageComment(1, "test.kt", "Page comment"),
                BaseTestHelper.createComment(2, "test.kt", 10, 15, "Line comment")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Getting comments for any line
            val lineComments = reviewFile.getCommentsForLine("test.kt", 12)

            // Then: Should not include page comments
            assertEquals(1, lineComments.size, "Should find only line comments")
            assertEquals(2, lineComments.first().id, "Should only include line comment")
        }

        @Test
        fun `test getCommentsForLine filters by path`() {
            // Given: Comments on different files
            val comments = listOf(
                BaseTestHelper.createComment(1, "Main.kt", 10, 20, "Main"),
                BaseTestHelper.createComment(2, "Utils.kt", 10, 20, "Utils")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Getting comments for specific file
            val mainComments = reviewFile.getCommentsForLine("Main.kt", 15)

            // Then: Should only return comments for that file
            assertEquals(1, mainComments.size, "Should filter by path")
            assertEquals("Main.kt", mainComments.first().relativePath, "Should match path filter")
        }

        @Test
        fun `test getCommentsForLine single line comment`() {
            // Given: A single-line comment (line 10 to 10)
            val comment = BaseTestHelper.createComment(1, "test.kt", 10, 10, "Single line")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Getting comments for that exact line
            val lineComments = reviewFile.getCommentsForLine("test.kt", 10)

            // Then: Should find the comment
            assertEquals(1, lineComments.size, "Should find single-line comment")

            // And: Should not find it on adjacent lines
            assertTrue(reviewFile.getCommentsForLine("test.kt", 9).isEmpty(), "Should not find on line 9")
            assertTrue(reviewFile.getCommentsForLine("test.kt", 11).isEmpty(), "Should not find on line 11")
        }

        @Test
        fun `test getCommentsForLine returns empty for empty review`() {
            // Given: An empty review
            val reviewFile = BaseTestHelper.createReviewFile("empty")

            // When: Getting comments for any line
            val lineComments = reviewFile.getCommentsForLine("test.kt", 10)

            // Then: Should return empty list
            assertTrue(lineComments.isEmpty(), "Empty review should return empty list")
        }
    }

    @Nested
    inner class RemoveComment {

        @Test
        fun `test removeComment returns true when found`() {
            // Given: A review with comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "a.kt", 1, 5, "First"),
                BaseTestHelper.createComment(2, "b.kt", 10, 15, "Second")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Removing existing comment
            val removed = reviewFile.removeComment(1)

            // Then: Should return true
            assertTrue(removed, "Should return true when comment removed")
            assertEquals(1, reviewFile.size(), "Size should decrease by 1")
            assertNull(reviewFile.getCommentById(1), "Comment should no longer exist")
        }

        @Test
        fun `test removeComment returns false when not found`() {
            // Given: A review with comments
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Removing non-existent ID
            val removed = reviewFile.removeComment(999)

            // Then: Should return false
            assertFalse(removed, "Should return false when ID not found")
            assertEquals(1, reviewFile.size(), "Size should remain unchanged")
        }

        @Test
        fun `test removeComment returns false for empty review`() {
            // Given: An empty review
            val reviewFile = BaseTestHelper.createReviewFile("empty")

            // When: Trying to remove from empty review
            val removed = reviewFile.removeComment(1)

            // Then: Should return false
            assertFalse(removed, "Should return false for empty review")
        }

        @Test
        fun `test removeComment preserves other comments`() {
            // Given: Multiple comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "a.kt", 1, 5, "First"),
                BaseTestHelper.createComment(2, "b.kt", 10, 15, "Second"),
                BaseTestHelper.createComment(3, "c.kt", 20, 25, "Third")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Removing middle comment
            reviewFile.removeComment(2)

            // Then: Other comments should remain intact
            assertNotNull(reviewFile.getCommentById(1), "Comment 1 should remain")
            assertNotNull(reviewFile.getCommentById(3), "Comment 3 should remain")
            assertEquals(2, reviewFile.size(), "Should have 2 comments remaining")
        }

        @Test
        fun `test removeComment can remove page comment`() {
            // Given: A review with a page comment
            val pageComment = BaseTestHelper.createPageComment(1, "test.kt", "Page comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(pageComment))

            // When: Removing the page comment
            val removed = reviewFile.removeComment(1)

            // Then: Should succeed
            assertTrue(removed, "Should be able to remove page comments")
            assertTrue(reviewFile.isEmpty(), "Review should be empty after removal")
        }

        @Test
        fun `test removeComment can empty review completely`() {
            // Given: A review with one comment
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Only comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Removing the only comment
            reviewFile.removeComment(1)

            // Then: Review should be empty
            assertTrue(reviewFile.isEmpty(), "Review should be empty after removing only comment")
        }
    }

    @Nested
    inner class NextId {

        @Test
        fun `test nextId returns 1 for empty review`() {
            // Given: An empty review
            val reviewFile = BaseTestHelper.createReviewFile("empty")

            // When: Getting next ID
            val nextId = reviewFile.nextId()

            // Then: Should return 1 (first ID)
            assertEquals(1, nextId, "Empty review should start with ID 1")
        }

        @Test
        fun `test nextId returns max plus one for single comment`() {
            // Given: A review with one comment (ID 5)
            val comment = BaseTestHelper.createComment(5, "test.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Getting next ID
            val nextId = reviewFile.nextId()

            // Then: Should return 6
            assertEquals(6, nextId, "Should return max ID + 1")
        }

        @Test
        fun `test nextId returns max plus one for multiple comments`() {
            // Given: A review with multiple comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "a.kt", 1, 5, "First"),
                BaseTestHelper.createComment(5, "b.kt", 10, 15, "Second"),
                BaseTestHelper.createComment(3, "c.kt", 20, 25, "Third")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Getting next ID
            val nextId = reviewFile.nextId()

            // Then: Should return max + 1 (5 + 1 = 6)
            assertEquals(6, nextId, "Should return max ID + 1 regardless of order")
        }

        @Test
        fun `test nextId is sequential after removal`() {
            // Given: A review with comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "a.kt", 1, 5, "First"),
                BaseTestHelper.createComment(2, "b.kt", 10, 15, "Second"),
                BaseTestHelper.createComment(3, "c.kt", 20, 25, "Third")
            )
            val reviewFile = BaseTestHelper.createReviewFile("review", comments)

            // When: Removing middle comment and getting next ID
            reviewFile.removeComment(2)
            val nextId = reviewFile.nextId()

            // Then: Should return max of remaining + 1
            assertEquals(4, nextId, "Should return max remaining ID + 1")
        }

        @Test
        fun `test nextId returns 1 after removing all comments`() {
            // Given: A review with comments
            val comment = BaseTestHelper.createComment(10, "test.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Removing all comments
            reviewFile.removeComment(10)
            val nextId = reviewFile.nextId()

            // Then: Should return 1
            assertEquals(1, nextId, "Should return 1 when review becomes empty")
        }

        @Test
        fun `test nextId does not increment state`() {
            // Given: A review with comments
            val comment = BaseTestHelper.createComment(5, "test.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFile("review", listOf(comment))

            // When: Calling nextId multiple times
            val first = reviewFile.nextId()
            val second = reviewFile.nextId()
            val third = reviewFile.nextId()

            // Then: Should return same value each time
            assertEquals(6, first, "First call should return 6")
            assertEquals(6, second, "Second call should also return 6")
            assertEquals(6, third, "Third call should also return 6")
        }
    }

    @Nested
    inner class PreambleAndPostamble {

        @Test
        fun `test review file with preamble preserves metadata`() {
            // Given: A review file with preamble
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFileWithMeta(
                name = "review",
                comments = listOf(comment),
                preamble = "# Code Review\n\nProject: MyProject",
                postamble = ""
            )

            // When: Accessing preamble
            val preamble = reviewFile.preamble

            // Then: Should preserve preamble text
            assertEquals("# Code Review\n\nProject: MyProject", preamble, "Should preserve preamble")
        }

        @Test
        fun `test review file with postamble preserves metadata`() {
            // Given: A review file with postamble
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFileWithMeta(
                name = "review",
                comments = listOf(comment),
                preamble = "",
                postamble = "\n## Summary\nReviewed by: Alice"
            )

            // When: Accessing postamble
            val postamble = reviewFile.postamble

            // Then: Should preserve postamble text
            assertEquals("\n## Summary\nReviewed by: Alice", postamble, "Should preserve postamble")
        }

        @Test
        fun `test review file with both preamble and postamble`() {
            // Given: A review file with both metadata sections
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
            val reviewFile = BaseTestHelper.createReviewFileWithMeta(
                name = "review",
                comments = listOf(comment),
                preamble = "# Header",
                postamble = "## Footer"
            )

            // Then: Both should be preserved
            assertEquals("# Header", reviewFile.preamble, "Should preserve preamble")
            assertEquals("## Footer", reviewFile.postamble, "Should preserve postamble")
        }

        @Test
        fun `test review file defaults to empty preamble and postamble`() {
            // Given: A review file created without metadata
            val reviewFile = BaseTestHelper.createReviewFile("review")

            // Then: Both should default to empty strings
            assertEquals("", reviewFile.preamble, "Default preamble should be empty")
            assertEquals("", reviewFile.postamble, "Default postamble should be empty")
        }

        @Test
        fun `test preamble and postamble are mutable`() {
            // Given: A review file
            val reviewFile = BaseTestHelper.createReviewFile("review")

            // When: Modifying preamble and postamble
            reviewFile.preamble = "New header"
            reviewFile.postamble = "New footer"

            // Then: Should reflect changes
            assertEquals("New header", reviewFile.preamble, "Preamble should be mutable")
            assertEquals("New footer", reviewFile.postamble, "Postamble should be mutable")
        }
    }

    @Nested
    inner class DataClassProperties {

        @Test
        fun `test review file name property`() {
            // Given: A review file
            val reviewFile = BaseTestHelper.createReviewFile("my-review")

            // Then: Name should match
            assertEquals("my-review", reviewFile.name, "Name should match constructor value")
        }

        @Test
        fun `test virtual file defaults to null`() {
            // Given: A review file created via helper
            val reviewFile = BaseTestHelper.createReviewFile("review")

            // Then: VirtualFile should be null
            assertNull(reviewFile.virtualFile, "VirtualFile should default to null")
        }

        @Test
        fun `test comments are initialized as CopyOnWriteArrayList`() {
            // Given: A review file
            val reviewFile = BaseTestHelper.createReviewFile("review")

            // Then: Comments should be CopyOnWriteArrayList
            assertTrue(
                reviewFile.comments is java.util.concurrent.CopyOnWriteArrayList,
                "Comments should be CopyOnWriteArrayList for thread safety"
            )
        }

        @Test
        fun `test review file equality based on properties`() {
            // Given: Two review files with same properties
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
            val file1 = BaseTestHelper.createReviewFile("review", listOf(comment))
            val file2 = BaseTestHelper.createReviewFile("review", listOf(comment))

            // Then: Should be equal (data class)
            assertEquals(file1, file2, "ReviewFiles with same properties should be equal")
        }

        @Test
        fun `test review file copy creates independent instance`() {
            // Given: A review file
            val original = BaseTestHelper.createReviewFile("original")

            // When: Copying with different name
            val copy = original.copy(name = "copy")

            // Then: Should be independent
            assertEquals("original", original.name, "Original should be unchanged")
            assertEquals("copy", copy.name, "Copy should have new name")
        }
    }
}
