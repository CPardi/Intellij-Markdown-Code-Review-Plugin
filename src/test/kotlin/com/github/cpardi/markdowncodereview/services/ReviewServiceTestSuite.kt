package com.github.cpardi.markdowncodereview.services

import com.github.cpardi.markdowncodereview.BaseTestHelper
import com.github.cpardi.markdowncodereview.LightPlatformTest
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

object ReviewServiceTestSuite {

    /**
     * Base class for integration tests of ReviewService.
     */
    abstract class ReviewServiceTest : LightPlatformTest() {

        protected lateinit var service: ReviewService

        override fun setUp() {
            super.setUp()
            service = ReviewService.getInstance(project)
            service.setActiveReview(null)
        }
    }

    @Nested
    @Suppress("JUnitMixedFramework")
    class GetAvailableReviewNames : ReviewServiceTest() {

        @Test
        fun `test returns empty list when reviews directory does not exist`() {
            // Given: No reviews directory
            // When: Getting available review names
            val names = service.getAvailableReviewNames()

            // Then: Should return empty list
            assertTrue(names.isEmpty(), "Should return empty list when no reviews directory")
        }

        @Test
        fun `test returns empty list when reviews directory is empty`() {
            // Given: An empty reviews directory
            // When: Getting available review names
            val names = service.getAvailableReviewNames()

            // Then: Should return empty list
            assertTrue(names.isEmpty(), "Should return empty list for empty directory")
        }

        @Test
        fun `test returns sorted list of review names`() {
            // Given: Multiple review files
            createVirtualFile("reviews/review-2.md", "")
            createVirtualFile("reviews/review-1.md", "")
            createVirtualFile("reviews/review-3.md", "")

            // When: Getting available review names
            val names = service.getAvailableReviewNames()

            // Then: Should return alphabetically sorted names
            assertEquals(listOf("review-1", "review-2", "review-3"), names)
        }

        @Test
        fun `test filters to only md files`() {
            // Given: Directory with mixed file types
            createVirtualFile("reviews/review-1.md", "")
            createVirtualFile("reviews/notes.txt", "")
            createVirtualFile("reviews/image.png", "")

            // When: Getting available review names
            val names = service.getAvailableReviewNames()

            // Then: Should only include .md files
            assertEquals(listOf("review-1"), names)
        }

        @Test
        fun `test strips md extension from names`() {
            // Given: A review file
            createVirtualFile("reviews/my-review.md", "")

            // When: Getting available review names
            val names = service.getAvailableReviewNames()

            // Then: Should return name without extension
            assertEquals(listOf("my-review"), names)
        }
    }

    @Nested
    @Suppress("JUnitMixedFramework")
    class CreateNewReview : ReviewServiceTest() {

        @Test
        fun `test creates file with generated name in reviews directory`() {
            // Given: A project with no existing reviews
            // When: Creating a new review
            val result = service.createNewReview()

            // Then: Should create a file named review-1
            ServiceTestHelper.assertCreateReviewSuccess(result, "review-1")
            assertFileExists("reviews/review-1.md")
        }

        @Test
        fun `test creates second review with sequential name`() {
            // Given: A project with review-1
            service.createNewReview()

            // When: Creating another review
            val result = service.createNewReview()

            // Then: Should name it review-2
            ServiceTestHelper.assertCreateReviewSuccess(result, "review-2")
            assertFileExists("reviews/review-2.md")
        }

        @Test
        fun `test active review is set after creation`() {
            // Given: No active review
            assertNull(service.activeReview)

            // When: Creating a new review
            service.createNewReview()

            // Then: Active review should be set
            assertNotNull(service.activeReview)
            assertEquals("review-1", service.activeReview!!.name)
        }

        @Test
        fun `test returns Success with correct name`() {
            // Given: A project ready for review creation
            // When: Creating a new review
            val result = service.createNewReview()

            // Then: Should return Success
            assertTrue(result is CreateReviewResult.Success)
            assertEquals("review-1", (result as CreateReviewResult.Success).name)
        }

        @Test
        fun `test creates valid empty markdown file`() {
            // Given: No existing reviews
            // When: Creating a new review
            service.createNewReview()

            // Then: File should exist and be parseable
            assertFileExists("reviews/review-1.md")
            assertNotNull(service.activeReview)
            assertTrue(service.activeReview!!.isEmpty())
        }

        @Test
        fun `test handles gap in sequence`() {
            // Given: review-1 and review-3 exist
            createVirtualFile("reviews/review-1.md", "")
            createVirtualFile("reviews/review-3.md", "")

            // When: Creating a new review
            val result = service.createNewReview()

            // Then: Should create review-2 (first available number)
            ServiceTestHelper.assertCreateReviewSuccess(result, "review-2")
        }
    }

    @Nested
    @Suppress("JUnitMixedFramework")
    class DeleteReview : ReviewServiceTest() {

        @Test
        fun `test deletes existing review file from disk`() {
            // Given: A review file
            createVirtualFile("reviews/review-1.md", "")

            // When: Deleting the review
            val result = service.deleteReview("review-1")

            // Then: Should succeed and file should be gone
            assertTrue(result)
            assertFileNotExists("reviews/review-1.md")
        }

        @Test
        fun `test returns false for NONE_SENTINEL`() {
            // Given: The NONE sentinel value
            // When: Trying to delete it
            val result = service.deleteReview(ReviewService.NONE_SENTINEL)

            // Then: Should return false
            assertFalse(result)
        }

        @Test
        fun `test returns false when reviews directory does not exist`() {
            // Given: No reviews directory
            // When: Trying to delete a review
            val result = service.deleteReview("review-1")

            // Then: Should return false
            assertFalse(result)
        }

        @Test
        fun `test clears active review when deleting active review`() {
            // Given: An active review
            service.createNewReview()
            assertNotNull(service.activeReview)
            val name = service.activeReview!!.name

            // When: Deleting the active review
            service.deleteReview(name)

            // Then: Active review should be cleared
            assertNull(service.activeReview)
        }

        @Test
        fun `test active review remains when deleting different review`() {
            // Given: Two reviews, one active
            createVirtualFile("reviews/review-1.md", "")
            createVirtualFile("reviews/review-2.md", "")

            val review = BaseTestHelper.createReviewFile("review-1")
            service.setActiveReview(review)

            // When: Deleting the other review
            service.deleteReview("review-2")

            // Then: Active review should remain
            assertNotNull(service.activeReview)
            assertEquals("review-1", service.activeReview!!.name)
        }

        @Test
        fun `test returns false when file does not exist`() {
            // Given: no existing files
            // When: Trying to delete a non-existent review
            val result = service.deleteReview("nonexistent")

            // Then: Should return false
            assertFalse(result)
        }
    }

    @Nested
    @Suppress("JUnitMixedFramework")
    class SetActiveReview : ReviewServiceTest() {

        @Test
        fun `test null clears active review`() {
            // Given: An active review
            service.createNewReview()
            assertNotNull(service.activeReview)

            // When: Setting active review to null
            service.setActiveReview(null)

            // Then: Should clear active review
            assertNull(service.activeReview)
        }

        @Test
        fun `test NONE_SENTINEL clears active review`() {
            // Given: An active review
            service.createNewReview()
            assertNotNull(service.activeReview)

            // When: Setting active review to NONE_SENTINEL
            service.setActiveReview(ReviewService.NONE_SENTINEL)

            // Then: Should clear active review
            assertNull(service.activeReview)
        }

        @Test
        fun `test loads review from disk and sets active review`() {
            // Given: A review file on disk
            val content = "@[src/Main.kt:1:5]:\nComment body\n---\n"
            createVirtualFile("reviews/test-review.md", content)

            // When: Setting active review
            service.setActiveReview("test-review")

            // Then: Should load and set the review
            assertNotNull(service.activeReview)
            assertEquals("test-review", service.activeReview!!.name)
            assertEquals(1, service.activeReview!!.size())
        }

        @Test
        fun `test handles file not found gracefully`() {
            // Given: No review file with that name
            // When: Setting active review to non-existent file
            service.setActiveReview("nonexistent")

            // Then: Active review should be null
            assertNull(service.activeReview)
        }
    }

    @Nested
    @Suppress("JUnitMixedFramework")
    class SaveActiveReview : ReviewServiceTest() {

        @Test
        fun `test saves active review to disk`() {
            // Given: A review with a comment
            service.createNewReview()
            service.activeReview!!.comments.add(
                BaseTestHelper.createComment(1, "test.kt", 1, 5, "Comment")
            )

            // When: Saving the active review
            val result = service.saveActiveReview()

            // Then: Should succeed
            assertTrue(result)
        }

        @Test
        fun `test returns false when no active review`() {
            // Given: No active review
            assertNull(service.activeReview)

            // When: Trying to save
            val result = service.saveActiveReview()

            // Then: Should return false
            assertFalse(result)
        }

        @Test
        fun `test preserves comments after save and reload cycle`() {
            // Given: A review with comments
            service.createNewReview()
            val review = service.activeReview!!
            val commentInMain = BaseTestHelper.createComment(1, "src/Main.kt", 1, 5, "Main comment")
            val commentInUtils = BaseTestHelper.createPageComment(2, "src/Utils.kt", "Utils page comment")
            review.comments.add(commentInMain)
            review.comments.add(commentInUtils)

            // When: Saving and reloading
            service.saveActiveReview()
            service.setActiveReview(review.name)

            // Then: Comments should be preserved
            val reloaded = service.activeReview!!
            assertEquals(2, reloaded.size())
            BaseTestHelper.assertCommentContentsListEquals(
                reloaded.getCommentsForFile("src/Main.kt"),
                listOf(commentInMain)
            )
            BaseTestHelper.assertCommentContentsListEquals(
                reloaded.getCommentsForFile("src/Utils.kt"),
                listOf(commentInUtils)
            )
        }
    }

    @Nested
    @Suppress("JUnitMixedFramework")
    class CommentCRUDWithFiles : ReviewServiceTest() {

        @Test
        fun `test addComment creates comment and persists to file`() {
            // Given: An active review
            service.createNewReview()

            // When: Adding a comment
            val comment = service.addComment("src/Main.kt", 1, 5, "New comment")

            // Then: Comment should be created
            assertNotNull(comment)
            assertEquals("src/Main.kt", comment!!.relativePath)
            assertEquals(1, comment.startLine)
            assertEquals(5, comment.endLine)
            assertEquals("New comment", comment.body)

            // And: Review should have the comment
            assertEquals(1, service.activeReview!!.size())
        }

        @Test
        fun `test addComment returns null when no active review`() {
            // Given: No active review
            assertNull(service.activeReview)

            // When: Adding a comment
            val comment = service.addComment("test.kt", 1, 5, "Comment")

            // Then: Should return null
            assertNull(comment)
        }

        @Test
        fun `test addPageComment creates page comment and persists`() {
            // Given: An active review
            service.createNewReview()

            // When: Adding a page comment
            val comment = service.addPageComment("src/Main.kt", "Page comment")

            // Then: Should create a page comment
            assertNotNull(comment)
            assertTrue(comment!!.isPageComment())
            assertEquals("src/Main.kt", comment.relativePath)
            assertEquals("Page comment", comment.body)
        }

        @Test
        fun `test addComment assigns sequential IDs`() {
            // Given: An active review
            service.createNewReview()

            // When: Adding multiple comments
            val comment1 = service.addComment("test.kt", 1, 5, "First")
            val comment2 = service.addComment("test.kt", 10, 15, "Second")

            // Then: IDs should be sequential
            assertEquals(1, comment1!!.id)
            assertEquals(2, comment2!!.id)
        }

        @Test
        fun `test editComment updates body`() {
            // Given: An active review with a comment
            service.createNewReview()
            val comment = service.addComment("test.kt", 1, 5, "Original")!!

            // When: Editing the comment
            val result = service.editComment(comment.id, "Updated body")

            // Then: Should succeed
            assertTrue(result)
            assertEquals("Updated body", service.getCommentById(comment.id)!!.body)
        }

        @Test
        fun `test editComment returns false when comment not found`() {
            // Given: An active review
            service.createNewReview()

            // When: Editing non-existent comment
            val result = service.editComment(999, "New body")

            // Then: Should return false
            assertFalse(result)
        }

        @Test
        fun `test editCommentRange updates startLine and endLine`() {
            // Given: An active review with a comment
            service.createNewReview()
            val comment = service.addComment("test.kt", 1, 5, "Comment")!!

            // When: Updating the range
            val result = service.editCommentRange(comment.id, 10, 20)

            // Then: Should update lines
            assertTrue(result)
            val updated = service.getCommentById(comment.id)!!
            assertEquals(10, updated.startLine)
            assertEquals(20, updated.endLine)
        }

        @Test
        fun `test editCommentRange returns false when comment not found`() {
            // Given: An active review
            service.createNewReview()

            // When: Editing range of non-existent comment
            val result = service.editCommentRange(999, 1, 5)

            // Then: Should return false
            assertFalse(result)
        }

        @Test
        fun `test deleteComment removes comment from review`() {
            // Given: An active review with comments
            service.createNewReview()
            val comment1 = service.addComment("test.kt", 1, 5, "First")!!
            service.addComment("test.kt", 10, 15, "Second")

            // When: Deleting first comment
            val result = service.deleteComment(comment1.id)

            // Then: Should succeed and remove the comment
            assertTrue(result)
            assertEquals(1, service.activeReview!!.size())
            assertNull(service.getCommentById(comment1.id))
        }

        @Test
        fun `test deleteComment returns false when no active review`() {
            // Given: No active review
            assertNull(service.activeReview)

            // When: Trying to delete
            val result = service.deleteComment(1)

            // Then: Should return false
            assertFalse(result)
        }

        @Test
        fun `test deleteComment returns false when comment not found`() {
            // Given: An active review
            service.createNewReview()

            // When: Deleting non-existent comment
            val result = service.deleteComment(999)

            // Then: Should return false
            assertFalse(result)
        }
    }

    @Nested
    @Suppress("JUnitMixedFramework")
    class CommentRetrievalMethods : ReviewServiceTest() {

        @Test
        fun `test getCommentsForFile returns empty when no active review`() {
            // Given: No active review
            assertNull(service.activeReview)

            // When: Getting comments for a file
            val comments = service.getCommentsForFile("test.kt")

            // Then: Should return empty list
            assertTrue(comments.isEmpty())
        }

        @Test
        fun `test getCommentsForFile delegates to ReviewFile`() {
            // Given: An active review with comments on multiple files
            service.createNewReview()
            service.addComment("src/Main.kt", 1, 5, "Main comment 1")
            service.addComment("src/Utils.kt", 10, 15, "Utils comment")
            service.addComment("src/Main.kt", 20, 25, "Main comment 2")

            // When: Getting comments for Main.kt
            val mainComments = service.getCommentsForFile("src/Main.kt")

            // Then: Should return only matching comments
            assertEquals(2, mainComments.size)
            assertTrue(mainComments.all { it.relativePath == "src/Main.kt" })
        }

        @Test
        fun `test getPageCommentsForFile returns only page comments`() {
            // Given: An active review with both comment types
            service.createNewReview()
            service.addComment("test.kt", 1, 5, "Line comment")
            service.addPageComment("test.kt", "Page comment")

            // When: Getting page comments
            val pageComments = service.getPageCommentsForFile("test.kt")

            // Then: Should only return page comments
            assertEquals(1, pageComments.size)
            assertTrue(pageComments.first().isPageComment())
        }

        @Test
        fun `test getCommentsForLine returns comments spanning the line`() {
            // Given: Comments spanning different lines
            service.createNewReview()
            service.addComment("test.kt", 10, 20, "Spans 10-20")
            service.addComment("test.kt", 15, 25, "Spans 15-25")
            service.addComment("test.kt", 30, 40, "Spans 30-40")

            // When: Getting comments for line 18
            val lineComments = service.getCommentsForLine("test.kt", 18)

            // Then: Should return spanning comments
            assertEquals(2, lineComments.size)
        }

        @Test
        fun `test getCommentById returns null when not found`() {
            // Given: An active review
            service.createNewReview()
            service.addComment("test.kt", 1, 5, "Comment")

            // When: Looking up non-existent ID
            val found = service.getCommentById(999)

            // Then: Should return null
            assertNull(found)
        }

        @Test
        fun `test getCommentById returns correct comment by ID`() {
            // Given: An active review with comments
            service.createNewReview()
            service.addComment("test.kt", 1, 5, "First")
            service.addComment("test.kt", 10, 15, "Second")

            // When: Looking up by ID
            val found = service.getCommentById(2)

            // Then: Should return correct comment
            assertNotNull(found)
            assertEquals(2, found!!.id)
            assertEquals("Second", found.body)
        }

        @Test
        fun `test all retrieval methods handle empty review gracefully`() {
            // Given: An active empty review
            service.createNewReview()

            // Then: All methods should return empty/null
            assertTrue(service.getCommentsForFile("test.kt").isEmpty())
            assertTrue(service.getPageCommentsForFile("test.kt").isEmpty())
            assertTrue(service.getCommentsForLine("test.kt", 1).isEmpty())
            assertNull(service.getCommentById(1))
        }
    }

    @Nested
    @Suppress("JUnitMixedFramework")
    class PathUtilities : ReviewServiceTest() {

        @Test
        fun `test getRelativePath returns relative path for nested file`() {
            // Given: A file in a nested directory
            val file = createVirtualFile("src/Main.kt", "fun main() {}")

            // When: Getting relative path
            val relativePath = service.getRelativePath(file)

            // Then: Should return relative path
            assertEquals("src/Main.kt", relativePath)
        }

        @Test
        fun `test getRelativePath handles files at project root`() {
            // Given: A file at the project root
            val file = createVirtualFile("README.md", "# Project")

            // When: Getting relative path
            val relativePath = service.getRelativePath(file)

            // Then: Should return just the filename
            assertEquals("README.md", relativePath)
        }

        @Test
        fun `test getRelativePath handles deeply nested paths`() {
            // Given: A deeply nested file
            val file = createVirtualFile("src/main/kotlin/com/example/App.kt", "class App")

            // When: Getting relative path
            val relativePath = service.getRelativePath(file)

            // Then: Should return full relative path
            assertEquals("src/main/kotlin/com/example/App.kt", relativePath)
        }
    }

    @Nested
    @Suppress("JUnitMixedFramework")
    class FileRenameHandling : ReviewServiceTest() {

        @Test
        fun `test updateCommentsForFileRename updates all comments for a file`() {
            // Given: A review with comments on a file
            service.createNewReview()
            service.addComment("src/OldName.kt", 1, 5, "Comment 1")
            service.addComment("src/OldName.kt", 10, 15, "Comment 2")

            // When: Renaming the file
            service.updateCommentsForFileRename("src/OldName.kt", "src/NewName.kt")

            // Then: All comments should have the new path
            assertEquals(0, service.getCommentsForFile("src/OldName.kt").size)
            assertEquals(2, service.getCommentsForFile("src/NewName.kt").size)
        }

        @Test
        fun `test updateCommentsForFileRename no effect on other files`() {
            // Given: Comments on two different files
            service.createNewReview()
            service.addComment("src/FileA.kt", 1, 5, "Comment A")
            service.addComment("src/FileB.kt", 10, 15, "Comment B")

            // When: Renaming FileA
            service.updateCommentsForFileRename("src/FileA.kt", "src/FileC.kt")

            // Then: FileB comments should be unchanged
            assertEquals(1, service.getCommentsForFile("src/FileB.kt").size)
        }

        @Test
        fun `test applyCommentRenames updates multiple paths`() {
            // Given: A review with comments on multiple files
            service.createNewReview()
            service.addComment("src/OldA.kt", 1, 5, "Comment A")
            service.addComment("src/OldB.kt", 10, 15, "Comment B")
            service.addComment("src/Unchanged.kt", 20, 25, "Comment C")

            // When: Applying batch renames
            val renames = mapOf(
                "src/OldA.kt" to "src/NewA.kt",
                "src/OldB.kt" to "src/NewB.kt"
            )
            service.applyCommentRenames(renames)

            // Then: Only renamed paths should be updated
            assertEquals(1, service.getCommentsForFile("src/NewA.kt").size)
            assertEquals(1, service.getCommentsForFile("src/NewB.kt").size)
            assertEquals(1, service.getCommentsForFile("src/Unchanged.kt").size)
            assertEquals(0, service.getCommentsForFile("src/OldA.kt").size)
            assertEquals(0, service.getCommentsForFile("src/OldB.kt").size)
        }

        @Test
        fun `test applyCommentRenames does nothing when no active review`() {
            // Given: No active review
            assertNull(service.activeReview)

            // When: Applying renames
            val renames = mapOf("old" to "new")
            service.applyCommentRenames(renames)

            // Then: Should not throw
        }

        @Test
        fun `test updateCommentsForFileRename does nothing when no matching comments`() {
            // Given: A review with comments on a different file
            service.createNewReview()
            service.addComment("src/Other.kt", 1, 5, "Comment")

            // When: Renaming a file with no comments
            service.updateCommentsForFileRename("src/NoComments.kt", "src/Renamed.kt")

            // Then: Existing comments should be unchanged
            assertEquals(1, service.getCommentsForFile("src/Other.kt").size)
        }
    }

    @Nested
    @Suppress("JUnitMixedFramework")
    class RangeMarkerManagement : ReviewServiceTest() {

        @Test
        fun `test attachRangeMarker creates valid marker`() = runBlocking {
            // Given: A document with content and a comment
            val file = createVirtualFile("src/Main.kt", "line1\nline2\nline3\nline4\nline5\n")
            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                val comment = BaseTestHelper.createComment(1, "src/Main.kt", 1, 3, "Comment")

                // When: Attaching a range marker
                service.attachRangeMarker(comment, document)

                // Then: Marker should be created
                assertNotNull(comment.rangeMarker)
                assertTrue(comment.rangeMarker!!.isValid)
            }
        }

        @Test
        fun `test marker spans correct line range`() = runBlocking {
            // Given: A document with 10 lines
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("src/Main.kt", content)

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                val comment = BaseTestHelper.createComment(1, "src/Main.kt", 2, 4, "Comment")

                // When: Attaching a range marker
                service.attachRangeMarker(comment, document)

                // Then: Marker should span lines 2-4
                assertNotNull(comment.rangeMarker)
                val marker = comment.rangeMarker!!
                val startLine = document.getLineNumber(marker.startOffset) + 1
                val endLine = document.getLineNumber(marker.endOffset) + 1
                assertEquals(2, startLine)
                assertEquals(4, endLine)
            }
        }

        @Test
        fun `test marker isGreedyToRight is true`() = runBlocking {
            // Given: A document with content
            val file = createVirtualFile("src/Main.kt", "line1\nline2\n")
            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                val comment = BaseTestHelper.createComment(1, "src/Main.kt", 1, 2, "Comment")

                // When: Attaching a range marker
                service.attachRangeMarker(comment, document)

                // Then: Marker should be greedy to right
                assertTrue(comment.rangeMarker!!.isGreedyToRight)
            }
        }

        @Test
        fun `test page comments are skipped in updateRangeMarkersForFile`() = runBlocking {
            // Given: A review with page comments
            service.createNewReview()
            service.addPageComment("src/Main.kt", "Page comment")

            val file = createVirtualFile("src/Main.kt", "content\n")
            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!

                // When: Updating range markers for the file
                service.updateRangeMarkersForFile("src/Main.kt", document)

                // Then: Page comments should not have markers
                val pageComment = service.activeReview!!.getCommentById(1)!!
                assertNull(pageComment.rangeMarker)
            }
        }

        @Test
        fun `test updateCommentLinesFromMarkers updates line numbers`() = runBlocking {
            // Given: A document with a range marker attached
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("src/Main.kt", content)
            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!

                val review = BaseTestHelper.createReviewFile("test-review")
                val comment = BaseTestHelper.createComment(1, "src/Main.kt", 2, 4, "Comment")
                review.comments.add(comment)
                service.setActiveReview(review)

                service.attachRangeMarker(comment, document)

                // When: Updating comment lines from markers
                service.updateCommentLinesFromMarkers(document)

                // Then: Lines should match marker positions
                val updated = service.getCommentById(1)!!
                assertEquals(2, updated.startLine)
                assertEquals(4, updated.endLine)
            }
        }

        @Test
        fun `test invalid markers are cleared`() = runBlocking {
            // Given: A review with an attached marker
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("src/Main.kt", content)
            readAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!

                val review = BaseTestHelper.createReviewFile("test-review")
                val comment = BaseTestHelper.createComment(1, "src/Main.kt", 2, 4, "Comment")
                review.comments.add(comment)
                service.setActiveReview(review)

                service.attachRangeMarker(comment, document)

                // When: Disposing the marker (simulating invalidation)
                comment.rangeMarker!!.dispose()

                // And: Updating lines from markers
                service.updateCommentLinesFromMarkers(document)

                // Then: Marker should be cleared
                assertNull(comment.rangeMarker)
            }
        }
    }
}
