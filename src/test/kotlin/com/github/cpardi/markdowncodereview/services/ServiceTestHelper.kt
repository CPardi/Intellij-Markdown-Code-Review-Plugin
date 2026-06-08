package com.github.cpardi.markdowncodereview.services

import com.github.cpardi.markdowncodereview.BaseTestHelper
import com.github.cpardi.markdowncodereview.parser.ReviewFileParser
import com.github.cpardi.markdowncodereview.settings.SettingsChangeListener
import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Test utilities specific to service layer testing.
 * Provides factory methods, assertion helpers, and listener trackers.
 */
object ServiceTestHelper {

    // ==================== CreateReviewResult Assertions ====================

    /**
     * Asserts that the result is a Success with the expected name.
     */
    fun assertCreateReviewSuccess(result: CreateReviewResult, expectedName: String) {
        assertTrue(result is CreateReviewResult.Success, "Expected Success, got $result")
        assertEquals(expectedName, (result as CreateReviewResult.Success).name)
    }

    /**
     * Asserts that the result is a Failure.
     */
    fun assertCreateReviewFailure(result: CreateReviewResult) {
        assertTrue(result is CreateReviewResult.Failure, "Expected Failure, got $result")
    }

    // ==================== Integration Test Assertions ====================

    /**
     * Asserts that a review file exists on disk with the expected content.
     * For use in LightPlatformTest integration tests.
     */
    fun assertReviewFileExists(
        baseDir: VirtualFile,
        reviewsDir: String,
        name: String,
        expectedComments: List<Comment>
    ) {
        val reviewsDirectory = baseDir.findChild(reviewsDir)
            ?: throw AssertionError("Reviews directory not found: $reviewsDir")
        val file = reviewsDirectory.findChild("$name.md")
            ?: throw AssertionError("Review file not found: $name.md")

        val parsed = ReviewFileParser.parse(file)
            ?: throw AssertionError("Failed to parse review file: $name.md")

        BaseTestHelper.assertCommentContentsListEquals(
            expectedComments,
            parsed.comments.toList(),
            "Review file content mismatch"
        )
    }

    // ==================== Message Bus Listener Tracking ====================

    /**
     * Creates a ReviewChangeListener that tracks whether it was notified.
     */
    fun createReviewChangeListener(): Pair<ReviewChangeListener, ReviewChangeTracker> {
        val tracker = ReviewChangeTracker()
        val listener = object : ReviewChangeListener {
            override fun onCommentsChanged(commentId: Int?) {
                tracker.commentsChanged.set(true)
                tracker.lastCommentId.set(commentId)
            }

            override fun onReviewChanged() {
                tracker.reviewChanged.set(true)
            }
        }
        return listener to tracker
    }

    /**
     * Creates a SettingsChangeListener that tracks whether it was notified.
     */
    fun createSettingsChangeListener(): Pair<SettingsChangeListener, AtomicBoolean> {
        val wasNotified = AtomicBoolean(false)
        val listener = object : SettingsChangeListener {
            override fun onSettingsChanged() {
                wasNotified.set(true)
            }
        }
        return listener to wasNotified
    }

    /**
     * Tracks which change events were received.
     */
    class ReviewChangeTracker {
        val commentsChanged = AtomicBoolean(false)
        val reviewChanged = AtomicBoolean(false)
        val lastCommentId = AtomicReference<Int?>(null)

        fun reset() {
            commentsChanged.set(false)
            reviewChanged.set(false)
            lastCommentId.set(null)
        }
    }
}
