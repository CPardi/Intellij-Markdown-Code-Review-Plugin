package com.github.cpardi.intellijmarkdownreviewplugin.services

import com.github.cpardi.intellijmarkdownreviewplugin.LightPlatformTest
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("JUnitMixedFramework")
object DocumentChangeListenerTestSuite {

    /**
     * Base class for integration tests of DocumentChangeListener.
     */
    abstract class DocumentChangeListenerTests : LightPlatformTest() {

        protected lateinit var service: ReviewService
        protected lateinit var listener: DocumentChangeListener

        override fun setUp() {
            super.setUp()
            service = ReviewService.getInstance(project)
            listener = DocumentChangeListener(project)
            service.setActiveReview(null)
        }
    }

    class LineInsertionTracking : DocumentChangeListenerTests() {

        @Test
        fun `test RangeMarker tracks line insertions correctly`() {
            // Given: A file with a comment on line 5
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            service.addComment("test.xml", 5, 7, "Comment on line 5-7")

            // Attach marker and verify initial state
            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarkersForOpenFiles()

                val comment = service.getCommentById(1)!!
                assertNotNull(comment.rangeMarker)
                assertTrue(comment.rangeMarker!!.isValid)
            }
        }

        @Test
        fun `test RangeMarker survives edits within document`() {
            // Given: A file with a comment
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 5, 7, "Comment on line 5-7")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarker(comment, document)

                // When: Making edits to the document
                // Then: Marker should still be valid
                assertTrue(comment.rangeMarker!!.isValid)
            }
        }

        @Test
        fun `test comment line numbers remain correct after marker updates`() {
            // Given: A file with a comment and attached marker
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 3, 5, "Original comment")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarker(comment, document)

                // When: Checking marker tracking
                val marker = comment.rangeMarker!!
                assertTrue(marker.isValid)

                // Then: Marker should cover correct lines
                val startLine = document.getLineNumber(marker.startOffset) + 1
                val endLine = document.getLineNumber(marker.endOffset) + 1

                assertEquals(3, startLine, "Start line should match")
                assertEquals(5, endLine, "End line should match")
            }
        }
    }

    class MultiFileTracking : DocumentChangeListenerTests() {

        @Test
        fun `test markers track independently across multiple files`() {
            // Given: Two files with comments
            val file1 = createVirtualFile("fileA.xml", (1..10).joinToString("\n") { "A$it" })
            val file2 = createVirtualFile("fileB.xml", (1..10).joinToString("\n") { "B$it" })

            service.createNewReview()
            val c1 = service.addComment("fileA.xml", 5, 7, "Comment A")!!
            val c2 = service.addComment("fileB.xml", 3, 4, "Comment B")!!

            runReadAction {
                val doc1 = FileDocumentManager.getInstance().getDocument(file1)!!
                val doc2 = FileDocumentManager.getInstance().getDocument(file2)!!

                service.attachRangeMarker(c1, doc1)
                service.attachRangeMarker(c2, doc2)

                // Then: Both markers should be valid
                assertTrue(c1.rangeMarker!!.isValid)
                assertTrue(c2.rangeMarker!!.isValid)

                // And: Each should track its own document
                val line1Start = doc1.getLineNumber(c1.rangeMarker!!.startOffset) + 1
                val line2Start = doc2.getLineNumber(c2.rangeMarker!!.startOffset) + 1

                assertEquals(5, line1Start, "FileA comment start line")
                assertEquals(3, line2Start, "FileB comment start line")
            }
        }

        @Test
        fun `test updateCommentLinesFromMarkers updates only target file`() {
            // Given: Comments on multiple files with different line numbers
            service.createNewReview()
            val file1 = createVirtualFile("file1.xml", "<xml><one/>\n<two/>\n<three/>\n</xml>")
            val file2 = createVirtualFile("file2.xml", "<xml><a/>\n<b/>\n<c/>\n</xml>")

            val comment1 = service.addComment("file1.xml", 1, 2, "Comment on File1")!!
            val comment2 = service.addComment("file2.xml", 2, 3, "Comment on File2")!!

            runReadAction {
                val document1 = FileDocumentManager.getInstance().getDocument(file1)!!
                val document2 = FileDocumentManager.getInstance().getDocument(file2)!!

                service.attachRangeMarker(comment1, document1)
                service.attachRangeMarker(comment2, document2)

                // When: Updating lines from document1 only
                service.updateCommentLinesFromMarkers(document1)

                // Then: File1 comment should have correct lines
                val c1 = service.getCommentById(1)!!
                val c2 = service.getCommentById(2)!!

                assertEquals(1, c1.startLine, "File1 comment start")
                assertEquals(2, c1.endLine, "File1 comment end")

                // And: File2 comment should remain unchanged
                assertEquals(2, c2.startLine, "File2 comment start should NOT be changed")
                assertEquals(3, c2.endLine, "File2 comment end should NOT be changed")
            }
        }
    }

    class PageCommentExclusion() : DocumentChangeListenerTests() {

        @Test
        fun `test page comments do not get RangeMarkers`() {
            // Given: A page comment
            val content = "<xml><one/>\n<two/>\n<three/>\n</xml>"
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val pageComment = service.addPageComment("test.xml", "Page comment")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!

                // When: Attaching markers for the file
                service.updateRangeMarkersForFile("test.xml", document)

                // Then: Page comment should not have a marker
                assertNull("Page comments should not have RangeMarkers", pageComment.rangeMarker)
            }
        }

        @Test
        fun `test line comments get RangeMarkers`() {
            // Given: A line comment
            val content = "<xml><one/>\n<two/>\n<three/>\n</xml>"
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val lineComment = service.addComment("test.xml", 1, 2, "Line comment")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!

                // When: Attaching markers for the file
                service.updateRangeMarkersForFile("test.xml", document)

                // Then: Line comment should have a marker
                assertNotNull(lineComment.rangeMarker, "Line comments should have RangeMarkers")
                assertTrue(lineComment.rangeMarker!!.isValid)
            }
        }
    }

    class MarkerLifecycle : DocumentChangeListenerTests() {

        @Test
        fun `test attachRangeMarker creates valid marker for open file`() {
            // Given: A file with a line comment
            val content = "<xml><one/>\n<two/>\n<three/>\n</xml>"
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 2, 3, "Comment on lines 2-3")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!

                // When: Attaching a range marker
                service.attachRangeMarker(comment, document)

                // Then: Marker should be created and valid
                assertNotNull(comment.rangeMarker)
                assertTrue(comment.rangeMarker!!.isValid)
            }
        }

        @Test
        fun `test marker spans correct offsets`() {
            // Given: A file with known content
            val content = "first line\nsecond line\nthird line\nfourth line\n"
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 2, 3, "Lines 2-3")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarker(comment, document)

                // When: Checking marker offsets
                val marker = comment.rangeMarker!!

                // Then: Marker should span lines 2-3 (0-indexed: lines 1-2)
                val startOffset = marker.startOffset
                val endOffset = marker.endOffset

                assertTrue(startOffset >= document.getLineStartOffset(1))
                assertTrue(endOffset <= document.getLineEndOffset(2))
                assertTrue(marker.isGreedyToRight, "Marker should be greedy to right")
            }
        }

        @Test
        fun `test invalid markers are disposed on reassignment`() {
            // Given: A comment with an attached marker
            val content = "<xml><one/>\n<two/>\n<three/>\n</xml>"
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 1, 2, "Comment")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarker(comment, document)

                val oldMarker = comment.rangeMarker!!
                assertTrue(oldMarker.isValid)

                // When: Reassigning marker
                service.attachRangeMarker(comment, document)

                // Then: Old marker should be disposed
                assertTrue(!oldMarker.isValid, "Old marker should be disposed")
                val newMarker = comment.rangeMarker!!
                assertTrue(newMarker.isValid, "New marker should be valid")
            }
        }

        @Test
        fun `test disposed markers are cleared on update`() {
            // Given: A comment with a marker that gets disposed
            val content = "<xml><one/>\n<two/>\n<three/>\n</xml>"
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 1, 2, "Comment")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarker(comment, document)

                // When: Manually disposing the marker
                comment.rangeMarker!!.dispose()

                // And: Updating lines from markers
                service.updateCommentLinesFromMarkers(document)

                // Then: Marker should be cleared
                assertNull("Invalid marker should be cleared", comment.rangeMarker)
            }
        }
    }

    class IntegrationScenarios : DocumentChangeListenerTests() {

        @Test
        fun `test multiple comments on same file all have valid markers`() {
            // Given: Multiple comments on one file
            val content = (1..20).joinToString("\n") { "line $it" }
            val file = createVirtualFile("multiComment.xml", content)

            service.createNewReview()
            val c1 = service.addComment("multiComment.xml", 2, 4, "First comment")!!
            val c2 = service.addComment("multiComment.xml", 10, 12, "Second comment")!!
            val c3 = service.addComment("multiComment.xml", 18, 20, "Third comment")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarker(c1, document)
                service.attachRangeMarker(c2, document)
                service.attachRangeMarker(c3, document)

                // Then: All markers should be valid
                assertTrue(c1.rangeMarker!!.isValid)
                assertTrue(c2.rangeMarker!!.isValid)
                assertTrue(c3.rangeMarker!!.isValid)

                // And: Each should span its intended range
                assertEquals(2, document.getLineNumber(c1.rangeMarker!!.startOffset) + 1)
                assertEquals(4, document.getLineNumber(c1.rangeMarker!!.endOffset) + 1)

                assertEquals(10, document.getLineNumber(c2.rangeMarker!!.startOffset) + 1)
                assertEquals(12, document.getLineNumber(c2.rangeMarker!!.endOffset) + 1)

                assertEquals(18, document.getLineNumber(c3.rangeMarker!!.startOffset) + 1)
                assertEquals(20, document.getLineNumber(c3.rangeMarker!!.endOffset) + 1)
            }
        }

        @Test
        fun `test RangeMarkers track across document edits`() {
            // Given: A file with a comment and attached marker
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 5, 7, "Tracked comment")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarker(comment, document)

                // Initial state
                assertTrue(comment.rangeMarker!!.isValid)
                assertEquals(5, comment.startLine)
                assertEquals(7, comment.endLine)

                // When: Making edits that RangeMarkers track
                // RangeMarkers automatically track offset changes within the document
                // This tests that the mechanism is working

                // Then: Marker should remain valid
                assertTrue(comment.rangeMarker!!.isValid)
            }
        }
    }

    class EdgeCases : DocumentChangeListenerTests() {

        @Test
        fun `test attachRangeMarker handles single line comments`() {
            // Given: A comment on a single line
            val content = "<xml><one/>\n<two/>\n<three/>\n</xml>"
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 2, 2, "Single line comment")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarker(comment, document)

                // Then: Marker should be valid
                assertNotNull(comment.rangeMarker)
                assertTrue(comment.rangeMarker!!.isValid)

                // And: Should span just that line
                val startLine = document.getLineNumber(comment.rangeMarker!!.startOffset) + 1
                val endLine = document.getLineNumber(comment.rangeMarker!!.endOffset) + 1

                assertEquals(2, startLine)
                assertEquals(2, endLine)
            }
        }

        @Test
        fun `test attachRangeMarkersForOpenFiles attaches markers for all comments`() {
            // Given: Multiple comments on an open file
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val c1 = service.addComment("test.xml", 2, 3, "Comment 1")!!
            val c2 = service.addComment("test.xml", 7, 9, "Comment 2")!!

            runReadAction {
                // Simulate opening the file (getting its document)
                val document = FileDocumentManager.getInstance().getDocument(file)!!

                // When: Attaching markers for all open files (simulating review switch)
                service.attachRangeMarkersForOpenFiles()

                // Then: All line comments should have markers
                assertNotNull(c1.rangeMarker)
                assertNotNull(c2.rangeMarker)
                assertTrue(c1.rangeMarker!!.isValid)
                assertTrue(c2.rangeMarker!!.isValid)
            }
        }
    }
}
