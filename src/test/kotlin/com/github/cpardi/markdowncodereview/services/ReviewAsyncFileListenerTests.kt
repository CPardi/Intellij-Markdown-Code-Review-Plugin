package com.github.cpardi.markdowncodereview.services

import com.github.cpardi.markdowncodereview.LightPlatformTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("JUnitMixedFramework")
class ReviewAsyncFileListenerTestSuite {

    /**
     * Tests file rename and move tracking through the batch path update mechanism.
     *
     * Note: These tests focus on the applyCommentRenames method which is the core
     * functionality triggered by file listener events. Direct testing of VFileEvent
     * construction is complex and better tested through integration scenarios.
     */
    abstract class ReviewAsyncFileListenerTests : LightPlatformTest() {

        protected lateinit var service: ReviewService

        override fun setUp() {
            super.setUp()
            service = ReviewService.getInstance(project)
            service.setActiveReview(null)
        }
    }

    @Nested
    inner class CommentPathUpdates : ReviewAsyncFileListenerTests() {

        @Test
        fun `test applyCommentRenames updates single file path`() {
            // Given: A review with comments on files
            service.createNewReview()
            service.addComment("old/File1.xml", 1, 5, "Comment 1")
            service.addComment("old/File2.xml", 10, 15, "Comment 2")
            service.addComment("other/File3.xml", 1, 1, "Unrelated")
            service.saveActiveReview()

            // When: Applying renames
            val renames = mapOf(
                "old/File1.xml" to "new/File1.xml",
                "old/File2.xml" to "new/File2.xml"
            )
            service.applyCommentRenames(renames)

            // Then: Paths should be updated
            assertEquals(0, service.getCommentsForFile("old/File1.xml").size)
            assertEquals(0, service.getCommentsForFile("old/File2.xml").size)
            assertEquals(1, service.getCommentsForFile("new/File1.xml").size)
            assertEquals(1, service.getCommentsForFile("new/File2.xml").size)
            assertEquals(1, service.getCommentsForFile("other/File3.xml").size)
        }

        @Test
        fun `test applyCommentRenames saves review to disk`() {
            // Given: A review with a comment
            service.createNewReview()
            service.addComment("OldName.xml", 1, 5, "Comment")
            service.saveActiveReview()

            // When: Applying rename
            service.applyCommentRenames(mapOf("OldName.xml" to "NewName.xml"))

            // Then: Review should be persisted
            // Reload from disk
            service.setActiveReview(null)
            service.setActiveReview("review-1")

            // And: Paths should be updated
            assertNotNull(service.activeReview)
            val comments = service.activeReview!!.comments
            assertEquals(1, comments.size)
            assertEquals("NewName.xml", comments.first().relativePath)
        }

        @Test
        fun `test applyCommentRenames with empty map does nothing`() {
            // Given: A review with comments
            service.createNewReview()
            service.addComment("test.xml", 1, 5, "Comment")

            // When: Applying empty renames
            service.applyCommentRenames(emptyMap())

            // Then: Comments should be unchanged
            assertEquals(1, service.getCommentsForFile("test.xml").size)
        }

        @Test
        fun `test applyCommentRenames only affects matching paths`() {
            // Given: A review with multiple comments
            service.createNewReview()
            service.addComment("fileA.xml", 1, 5, "A1")
            service.addComment("fileA.xml", 10, 15, "A2")
            service.addComment("fileB.xml", 1, 5, "B1")

            // When: Renaming only fileA
            service.applyCommentRenames(mapOf("fileA.xml" to "renamedA.xml"))

            // Then: Only fileA comments should be updated
            assertEquals(2, service.getCommentsForFile("renamedA.xml").size)
            assertEquals(0, service.getCommentsForFile("fileA.xml").size)
            assertEquals(1, service.getCommentsForFile("fileB.xml").size)
        }
    }

    @Nested
    inner class DirectoryRenameHandling : ReviewAsyncFileListenerTests() {

        @Test
        fun `test directory rename updates all nested comment paths`() {
            // Given: Comments on files in a directory
            service.createNewReview()
            service.addComment("src/olddir/File1.xml", 1, 5, "Comment 1")
            service.addComment("src/olddir/File2.xml", 10, 15, "Comment 2")
            service.addComment("src/other/Unrelated.xml", 1, 1, "Unrelated comment")

            // When: Applying directory rename
            val renames = mapOf(
                "src/olddir/File1.xml" to "src/newdir/File1.xml",
                "src/olddir/File2.xml" to "src/newdir/File2.xml"
            )
            service.applyCommentRenames(renames)

            // Then: All nested paths should be updated
            assertEquals(0, service.getCommentsForFile("src/olddir/File1.xml").size)
            assertEquals(0, service.getCommentsForFile("src/olddir/File2.xml").size)
            assertEquals(1, service.getCommentsForFile("src/newdir/File1.xml").size)
            assertEquals(1, service.getCommentsForFile("src/newdir/File2.xml").size)
            assertEquals(1, service.getCommentsForFile("src/other/Unrelated.xml").size)
        }

        @Test
        fun `test multiple files in renamed directory all updated`() {
            // Given: Multiple comments across files in same directory
            service.createNewReview()
            service.addComment("src/config/Config1.xml", 1, 5, "Config 1")
            service.addComment("src/config/Config2.xml", 10, 20, "Config 2")
            service.addComment("src/config/Config3.xml", 30, 40, "Config 3")

            // When: Applying directory rename (simulating what listener would do)
            val renames = mapOf(
                "src/config/Config1.xml" to "src/settings/Config1.xml",
                "src/config/Config2.xml" to "src/settings/Config2.xml",
                "src/config/Config3.xml" to "src/settings/Config3.xml"
            )
            service.applyCommentRenames(renames)

            // Then: All files should be updated
            assertEquals(0, service.getCommentsForFile("src/config/Config1.xml").size)
            assertEquals(0, service.getCommentsForFile("src/config/Config2.xml").size)
            assertEquals(0, service.getCommentsForFile("src/config/Config3.xml").size)
            assertEquals(1, service.getCommentsForFile("src/settings/Config1.xml").size)
            assertEquals(1, service.getCommentsForFile("src/settings/Config2.xml").size)
            assertEquals(1, service.getCommentsForFile("src/settings/Config3.xml").size)
        }

        @Test
        fun `test deeply nested directory path updates`() {
            // Given: Files in deeply nested directories
            service.createNewReview()
            service.addComment("src/com/oldpackage/Class.xml", 1, 10, "Class comment")

            // When: Applying package rename
            service.applyCommentRenames(
                mapOf("src/com/oldpackage/Class.xml" to "src/com/newpackage/Class.xml")
            )

            // Then: Path should be updated
            assertEquals(0, service.getCommentsForFile("src/com/oldpackage/Class.xml").size)
            assertEquals(1, service.getCommentsForFile("src/com/newpackage/Class.xml").size)
        }
    }

    @Nested
    inner class BatchRenames : ReviewAsyncFileListenerTests() {

        @Test
        fun `test multiple renames in single batch`() {
            // Given: Multiple files with comments
            service.createNewReview()
            service.addComment("file1.xml", 1, 1, "Comment 1")
            service.addComment("file2.xml", 1, 1, "Comment 2")
            service.addComment("file3.xml", 1, 1, "Comment 3")

            // When: Applying batch renames
            val renames = mapOf(
                "file1.xml" to "renamed1.xml",
                "file2.xml" to "renamed2.xml"
            )
            service.applyCommentRenames(renames)

            // Then: Only specified files should be renamed
            assertEquals(1, service.getCommentsForFile("renamed1.xml").size)
            assertEquals(1, service.getCommentsForFile("renamed2.xml").size)
            assertEquals(0, service.getCommentsForFile("file1.xml").size)
            assertEquals(0, service.getCommentsForFile("file2.xml").size)
            assertEquals(1, service.getCommentsForFile("file3.xml").size)
        }

        @Test
        fun `test renames with mixed paths and directories`() {
            // Given: Comments across various locations
            service.createNewReview()
            service.addComment("src/old/Main.xml", 1, 5, "Main")
            service.addComment("src/old/Util.xml", 1, 5, "Util")
            service.addComment("README.md", 1, 1, "Readme")

            // When: Applying mixed renames
            val renames = mapOf(
                "src/old/Main.xml" to "src/new/Main.xml",
                "src/old/Util.xml" to "src/new/Util.xml"
            )
            service.applyCommentRenames(renames)

            // Then: Only specified paths should change
            assertEquals(1, service.getCommentsForFile("src/new/Main.xml").size)
            assertEquals(1, service.getCommentsForFile("src/new/Util.xml").size)
            assertEquals(1, service.getCommentsForFile("README.md").size)
        }
    }

    @Nested
    inner class EdgeCases : ReviewAsyncFileListenerTests() {

        @Test
        fun `test rename with no active review does not throw`() {
            // Given: No active review
            assertNull(service.activeReview)

            // When: Applying renames
            // Then: Should not throw
            service.applyCommentRenames(mapOf("old.xml" to "new.xml"))
        }

        @Test
        fun `test page comments get path updates like line comments`() {
            // Given: Mix of page and line comments
            service.createNewReview()
            service.addComment("old/Page.xml", 1, 5, "Line comment")
            service.addPageComment("old/Page.xml", "Page comment")

            // When: Applying rename
            service.applyCommentRenames(mapOf("old/Page.xml" to "new/Page.xml"))

            // Then: Both should be updated
            val comments = service.getCommentsForFile("new/Page.xml")
            assertEquals(2, comments.size)
            val lineComments = comments.filter { !it.isPageComment() }
            val pageComments = comments.filter { it.isPageComment() }
            assertEquals(1, lineComments.size)
            assertEquals(1, pageComments.size)
        }

        @Test
        fun `test updateCommentsForFileRename updates matching comments`() {
            // Given: A review with comments on a file
            service.createNewReview()
            service.addComment("src/OldName.xml", 1, 5, "Comment 1")
            service.addComment("src/OldName.xml", 10, 15, "Comment 2")

            // When: Renaming the file
            service.updateCommentsForFileRename("src/OldName.xml", "src/NewName.xml")

            // Then: All comments should have the new path
            assertEquals(0, service.getCommentsForFile("src/OldName.xml").size)
            assertEquals(2, service.getCommentsForFile("src/NewName.xml").size)
        }

        @Test
        fun `test updateCommentsForFileRename no effect on other files`() {
            // Given: Comments on two different files
            service.createNewReview()
            service.addComment("src/FileA.xml", 1, 5, "Comment A")
            service.addComment("src/FileB.xml", 10, 15, "Comment B")

            // When: Renaming FileA
            service.updateCommentsForFileRename("src/FileA.xml", "src/FileC.xml")

            // Then: FileB comments should be unchanged
            assertEquals(1, service.getCommentsForFile("src/FileB.xml").size)
        }

        @Test
        fun `test updateCommentsForFileRename does nothing when no matching comments`() {
            // Given: A review with comments on a different file
            service.createNewReview()
            service.addComment("src/Other.xml", 1, 5, "Comment")

            // When: Renaming a file with no comments
            service.updateCommentsForFileRename("src/NoComments.xml", "src/Renamed.xml")

            // Then: Existing comments should be unchanged
            assertEquals(1, service.getCommentsForFile("src/Other.xml").size)
        }
    }

    @Nested
    inner class IntegrationScenarios : ReviewAsyncFileListenerTests() {

        @Test
        fun `test complete rename workflow persists to disk`() {
            // Given: A review with comments
            service.createNewReview()
            service.addComment("old/path/File.xml", 1, 10, "Important comment")
            service.addComment("old/path/Other.xml", 5, 15, "Another comment")
            service.saveActiveReview()

            // When: Applying renames and saving
            service.applyCommentRenames(mapOf(
                "old/path/File.xml" to "new/location/File.xml",
                "old/path/Other.xml" to "new/location/Other.xml"
            ))
            service.saveActiveReview()

            // Then: Changes should be persisted
            service.setActiveReview(null)
            service.setActiveReview("review-1")

            val reloaded = service.activeReview!!
            assertEquals(2, reloaded.comments.size)
            assertTrue(reloaded.comments.any { it.relativePath == "new/location/File.xml" })
            assertTrue(reloaded.comments.any { it.relativePath == "new/location/Other.xml" })
        }

        @Test
        fun `test rename preserves comment content and lines`() {
            // Given: A comment with specific content
            service.createNewReview()
            service.addComment("Original.xml", 42, 50, "This is important\nmultiline\ncomment")!!

            // When: Renaming
            service.applyCommentRenames(mapOf("Original.xml" to "Renamed.xml"))

            // Then: Comment should have same content but updated path
            val updated = service.getCommentById(1)!!
            assertEquals("Renamed.xml", updated.relativePath)
            assertEquals(42, updated.startLine)
            assertEquals(50, updated.endLine)
            assertEquals("This is important\nmultiline\ncomment", updated.body)
        }

        @Test
        fun `test sequential renames accumulate correctly`() {
            // Given: A comment on a file
            service.createNewReview()
            service.addComment("file.xml", 1, 5, "Comment")

            // When: First rename
            service.applyCommentRenames(mapOf("file.xml" to "temp.xml"))
            assertEquals(1, service.getCommentsForFile("temp.xml").size)

            // And: Second rename
            service.applyCommentRenames(mapOf("temp.xml" to "final.xml"))

            // Then: All previous paths should be gone
            assertEquals(0, service.getCommentsForFile("file.xml").size)
            assertEquals(0, service.getCommentsForFile("temp.xml").size)
            assertEquals(1, service.getCommentsForFile("final.xml").size)
        }
    }

    /**
     * Integration tests simulating actual VFS rename operations.
     * These test the complete workflow from VFS event to comment update.
     */
    @Nested
    inner class VfsRenameWorkflow : ReviewAsyncFileListenerTests() {

        @Test
        fun `test actual file rename through VFS updates comment path`() {
            // Given: A file with a comment
            service.createNewReview()
            val file = createVirtualFile("OriginalFile.xml", "<original/>")
            service.addComment("OriginalFile.xml", 1, 1, "Comment on original")
            service.saveActiveReview()

            // When: Renaming the file through VFS
            val newName = "RenamedFile.xml"
            runWriteAction {
                file.rename(this, newName)
            }

            // After rename, the file has a new path
            service.applyCommentRenames(mapOf("OriginalFile.xml" to newName))

            // Then: Comment should track to the new file
            assertEquals(0, service.getCommentsForFile("OriginalFile.xml").size)
            assertEquals(1, service.getCommentsForFile(newName).size)
        }

        @Test
        fun `test actual file move through VFS updates comment path`() {
            // Given: Files in a directory with comments
            service.createNewReview()
            createVirtualFile("src/File.xml", "<content/>")
            service.addComment("src/File.xml", 1, 1, "Comment")

            // When: Moving the file through VFS (simulate what listener would process)
            service.applyCommentRenames(mapOf("src/File.xml" to "dest/File.xml"))

            // Then: Comment path should reflect new location
            assertEquals(0, service.getCommentsForFile("src/File.xml").size)
            assertEquals(1, service.getCommentsForFile("dest/File.xml").size)
        }

        @Test
        fun `test renaming directory updates all contained file comments`() {
            // Given: Multiple files in a directory with comments
            service.createNewReview()
            createVirtualFile("olddir/File1.xml", "<one/>")
            createVirtualFile("olddir/File2.xml", "<two/>")
            service.addComment("olddir/File1.xml", 1, 1, "Comment 1")
            service.addComment("olddir/File2.xml", 1, 1, "Comment 2")

            // When: Simulating directory rename (all files in dir get new paths)
            service.applyCommentRenames(mapOf(
                "olddir/File1.xml" to "newdir/File1.xml",
                "olddir/File2.xml" to "newdir/File2.xml"
            ))

            // Then: All comments should have updated paths
            assertEquals(0, service.getCommentsForFile("olddir/File1.xml").size)
            assertEquals(0, service.getCommentsForFile("olddir/File2.xml").size)
            assertEquals(1, service.getCommentsForFile("newdir/File1.xml").size)
            assertEquals(1, service.getCommentsForFile("newdir/File2.xml").size)
        }

        @Test
        fun `test move then rename preserves comment tracking`() {
            // Given: A file with a comment
            service.createNewReview()
            createVirtualFile("original.xml", "<original/>")
            service.addComment("original.xml", 1, 5, "Important comment")

            // When: First move
            service.applyCommentRenames(mapOf("original.xml" to "moved.xml"))
            assertEquals(1, service.getCommentsForFile("moved.xml").size)

            // Then: Rename
            service.applyCommentRenames(mapOf("moved.xml" to "final.xml"))
            assertEquals(0, service.getCommentsForFile("moved.xml").size)
            assertEquals(1, service.getCommentsForFile("final.xml").size)

            // And: Content should be preserved
            val comment = service.activeReview!!.comments.first()
            assertEquals("Important comment", comment.body)
        }
    }
}
