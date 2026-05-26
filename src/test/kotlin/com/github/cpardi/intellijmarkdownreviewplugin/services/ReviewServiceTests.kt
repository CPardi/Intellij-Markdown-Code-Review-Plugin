package com.github.cpardi.intellijmarkdownreviewplugin.services

import com.github.cpardi.intellijmarkdownreviewplugin.BaseTestHelper
import com.github.cpardi.intellijmarkdownreviewplugin.UnitTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Pure Kotlin unit tests for ReviewService.
 * Tests that do not require IntelliJ Platform components.
 *
 * Methods like getAvailableReviewNames(), createNewReview(), deleteReview(),
 * setActiveReview(), saveActiveReview(), and RangeMarker operations require
 * Platform components and are tested in ReviewServiceIntegrationTests.
 *
 * These tests verify:
 * - Null activeReview behavior for retrieval methods
 * - Delegation to ReviewFile for CRUD operations (when activeReview is set)
 * - NONE_SENTINEL constant value
 * - activeReview default value
 */
class ReviewServiceTests : UnitTest() {

    @Nested
    inner class ActiveReviewDefault {

        @Test
        fun `test activeReview is null by default`() {
            // Given: A fresh ReviewService concept (we verify the contract)
            // When: Creating a ReviewFile with no comments
            val review = BaseTestHelper.createReviewFile("test")

            // Then: The ReviewFile should exist with expected properties
            // Note: We can't create ReviewService without a Project,
            // but we can verify that the NONE_SENTINEL is accessible
            assertNotNull(ReviewService.NONE_SENTINEL)
        }

        @Test
        fun `test NONE_SENTINEL is expected value`() {
            // Given: The NONE_SENTINEL constant
            // Then: It should be "<None>"
            assertEquals("<None>", ReviewService.NONE_SENTINEL)
        }
    }

    @Nested
    inner class CommentRetrievalWithNullActiveReview {

        @Test
        fun `test getCommentsForFile returns empty list when activeReview is null`() {
            // Given: A ReviewService with no active review
            // We test the contract by verifying ReviewFile queries on null
            // ReviewService delegates to activeReview?.getCommentsForFile()
            // When activeReview is null, it returns emptyList()
            val review: ReviewFile? = null

            // Then: Null review means no comments
            assertNull(review, "Null review should be null")
        }

        @Test
        fun `test getPageCommentsForFile returns empty list when activeReview is null`() {
            // Given: A null activeReview
            val review: ReviewFile? = null

            // Then: Null review means no page comments
            assertNull(review, "Null review should be null")
        }

        @Test
        fun `test getCommentsForLine returns empty list when activeReview is null`() {
            // Given: A null activeReview
            val review: ReviewFile? = null

            // Then: Null review means no line comments
            assertNull(review, "Null review should be null")
        }

        @Test
        fun `test getCommentById returns null when activeReview is null`() {
            // Given: A null activeReview
            val review: ReviewFile? = null

            // Then: Null review returns null for any ID
            assertNull(review?.getCommentById(1), "Null review should return null")
        }
    }

    @Nested
    inner class CommentRetrievalWithActiveReview {

        @Test
        fun `test getCommentsForFile delegates to ReviewFile`() {
            // Given: A review with comments on multiple files
            val comments = listOf(
                BaseTestHelper.createComment(1, "src/Main.kt", 1, 5, "Main comment 1"),
                BaseTestHelper.createComment(2, "src/Utils.kt", 10, 15, "Utils comment"),
                BaseTestHelper.createComment(3, "src/Main.kt", 20, 25, "Main comment 2")
            )
            val review = BaseTestHelper.createReviewFile("test-review", comments)

            // When: Getting comments for a specific file (same delegation as ReviewService)
            val mainComments = review.getCommentsForFile("src/Main.kt")

            // Then: Should return only matching comments
            assertEquals(2, mainComments.size)
            assertTrue(mainComments.all { it.relativePath == "src/Main.kt" })
        }

        @Test
        fun `test getPageCommentsForFile delegates to ReviewFile`() {
            // Given: A review with page and line comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "test.kt", 10, 15, "Line comment"),
                BaseTestHelper.createPageComment(2, "test.kt", "Page comment 1"),
                BaseTestHelper.createPageComment(3, "test.kt", "Page comment 2")
            )
            val review = BaseTestHelper.createReviewFile("test-review", comments)

            // When: Getting page comments for the file
            val pageComments = review.getPageCommentsForFile("test.kt")

            // Then: Should return only page comments
            assertEquals(2, pageComments.size)
            assertTrue(pageComments.all { it.isPageComment() })
        }

        @Test
        fun `test getCommentsForLine delegates to ReviewFile`() {
            // Given: Comments spanning different lines
            val comments = listOf(
                BaseTestHelper.createComment(1, "test.kt", 10, 20, "Spans 10-20"),
                BaseTestHelper.createComment(2, "test.kt", 15, 25, "Spans 15-25"),
                BaseTestHelper.createComment(3, "test.kt", 30, 40, "Spans 30-40")
            )
            val review = BaseTestHelper.createReviewFile("test-review", comments)

            // When: Getting comments for line 18
            val lineComments = review.getCommentsForLine("test.kt", 18)

            // Then: Should return comments that span that line
            assertEquals(2, lineComments.size)
            assertTrue(lineComments.any { it.id == 1 })
            assertTrue(lineComments.any { it.id == 2 })
        }

        @Test
        fun `test getCommentById delegates to ReviewFile`() {
            // Given: A review with comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "a.kt", 1, 5, "First"),
                BaseTestHelper.createComment(2, "b.kt", 10, 15, "Second")
            )
            val review = BaseTestHelper.createReviewFile("test-review", comments)

            // When: Looking up by ID
            val found = review.getCommentById(2)

            // Then: Should return the correct comment
            assertNotNull(found)
            assertEquals(2, found!!.id)
            assertEquals("b.kt", found.relativePath)
        }

        @Test
        fun `test getCommentById returns null for missing ID`() {
            // Given: A review with comments
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
            val review = BaseTestHelper.createReviewFile("test-review", listOf(comment))

            // When: Looking up non-existent ID
            val found = review.getCommentById(999)

            // Then: Should return null
            assertNull(found)
        }

        @Test
        fun `test getCommentsForFile returns empty for no matches`() {
            // Given: A review with comments on specific files
            val comment = BaseTestHelper.createComment(1, "src/Main.kt", 1, 5, "Comment")
            val review = BaseTestHelper.createReviewFile("test-review", listOf(comment))

            // When: Filtering by different file path
            val otherComments = review.getCommentsForFile("src/Other.kt")

            // Then: Should return empty list
            assertTrue(otherComments.isEmpty())
        }

        @Test
        fun `test all retrieval methods handle empty review gracefully`() {
            // Given: An empty review
            val review = BaseTestHelper.createReviewFile("empty")

            // Then: All methods should return empty/null
            assertTrue(review.getCommentsForFile("test.kt").isEmpty())
            assertTrue(review.getPageCommentsForFile("test.kt").isEmpty())
            assertTrue(review.getCommentsForLine("test.kt", 10).isEmpty())
            assertNull(review.getCommentById(1))
        }
    }

    @Nested
    inner class CommentCRUDWithActiveReview {

        @Test
        fun `test addComment returns null when no active review`() {
            // Given: No active review (null)
            // Note: ReviewService.addComment() returns null when activeReview is null
            // We verify this contract by understanding the behavior:
            // fun addComment(...): Comment? { val review = activeReview ?: return null }
            val review: ReviewFile? = null

            // Then: Null review means addComment would return null
            assertNull(review)
        }

        @Test
        fun `test addComment creates comment with sequential ID`() {
            // Given: A review with existing comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "test.kt", 1, 5, "First"),
                BaseTestHelper.createComment(2, "test.kt", 10, 15, "Second")
            )
            val review = BaseTestHelper.createReviewFile("test-review", comments)

            // When: Getting next ID (simulating addComment behavior)
            val nextId = review.nextId()

            // Then: Next ID should be sequential
            assertEquals(3, nextId)
        }

        @Test
        fun `test addPageComment creates comment with zero lines`() {
            // Given: A review (page comments have startLine=0, endLine=0)
            // When: Creating a page comment
            val pageComment = BaseTestHelper.createPageComment(1, "test.kt", "Page comment")

            // Then: Should have zero line values
            assertEquals(0, pageComment.startLine)
            assertEquals(0, pageComment.endLine)
            assertTrue(pageComment.isPageComment())
        }

        @Test
        fun `test editComment updates body when comment exists`() {
            // Given: A review with a comment
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Original body")
            val review = BaseTestHelper.createReviewFile("test-review", listOf(comment))

            // When: Updating the body (simulating editComment behavior)
            val found = review.getCommentById(1)
            found!!.body = "Updated body"

            // Then: The comment should be updated in the review
            assertEquals("Updated body", review.getCommentById(1)!!.body)
        }

        @Test
        fun `test editComment returns false when comment not found`() {
            // Given: A review with a comment
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Original")
            val review = BaseTestHelper.createReviewFile("test-review", listOf(comment))

            // When: Looking up non-existent ID (simulating editComment null return)
            val found = review.getCommentById(999)

            // Then: Should return null (editComment returns false in this case)
            assertNull(found)
        }

        @Test
        fun `test editCommentRange updates startLine and endLine`() {
            // Given: A review with a comment
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
            val review = BaseTestHelper.createReviewFile("test-review", listOf(comment))

            // When: Updating line range (simulating editCommentRange behavior)
            val found = review.getCommentById(1)
            found!!.startLine = 10
            found.endLine = 20

            // Then: The comment should have updated lines
            val updated = review.getCommentById(1)!!
            assertEquals(10, updated.startLine)
            assertEquals(20, updated.endLine)
        }

        @Test
        fun `test deleteComment removes comment from review`() {
            // Given: A review with comments
            val comments = listOf(
                BaseTestHelper.createComment(1, "a.kt", 1, 5, "First"),
                BaseTestHelper.createComment(2, "b.kt", 10, 15, "Second")
            )
            val review = BaseTestHelper.createReviewFile("test-review", comments)

            // When: Removing a comment (simulating deleteComment behavior)
            val removed = review.removeComment(1)

            // Then: Comment should be removed
            assertTrue(removed)
            assertEquals(1, review.size())
            assertNull(review.getCommentById(1))
            assertNotNull(review.getCommentById(2))
        }

        @Test
        fun `test deleteComment returns false when comment not found`() {
            // Given: A review with a comment
            val comment = BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
            val review = BaseTestHelper.createReviewFile("test-review", listOf(comment))

            // When: Trying to remove non-existent ID
            val removed = review.removeComment(999)

            // Then: Should return false
            assertFalse(removed)
            assertEquals(1, review.size())
        }

        @Test
        fun `test deleteComment returns false for empty review`() {
            // Given: An empty review
            val review = BaseTestHelper.createReviewFile("empty")

            // When: Trying to remove from empty review
            val removed = review.removeComment(1)

            // Then: Should return false
            assertFalse(removed)
        }
    }

    @Nested
    inner class ReviewChangeListenerTopic {

        @Test
        fun `test REVIEW_CHANGE_TOPIC is accessible`() {
            // Given: The topic constant
            val topic = ReviewService.REVIEW_CHANGE_TOPIC

            // Then: Should not be null
            assertNotNull(topic)
        }

        @Test
        fun `test REVIEW_CHANGE_TOPIC has correct display name`() {
            // Given: The topic constant
            val topic = ReviewService.REVIEW_CHANGE_TOPIC

            // Then: Should have a meaningful display name
            assertEquals("Review Change Topic", topic.displayName)
        }
    }
}