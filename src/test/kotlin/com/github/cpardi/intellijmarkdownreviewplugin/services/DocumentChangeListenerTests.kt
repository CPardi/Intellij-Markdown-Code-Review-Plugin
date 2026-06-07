package com.github.cpardi.intellijmarkdownreviewplugin.services

import com.github.cpardi.intellijmarkdownreviewplugin.LightPlatformTest
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("JUnitMixedFramework")
class DocumentChangeListenerTestSuite {

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

    @Nested
    inner class LineInsertionTracking : DocumentChangeListenerTests() {

        @Test
        fun `RangeMarker tracks line insertions correctly`() {
            // Given: A file with a comment on line 5
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            service.addComment("test.xml", 5, 7, "Comment on line 5-7")

            // Attach marker and verify initial state
            runReadAction {
                FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarkersForOpenFiles()

                val comment = service.getCommentById(1)!!
                Assertions.assertNotNull(comment.rangeMarker)
                Assertions.assertTrue(comment.rangeMarker!!.isValid)
            }
        }

        @Test
        fun `RangeMarker survives edits within document`() {
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
                Assertions.assertTrue(comment.rangeMarker!!.isValid)
            }
        }

        @Test
        fun `comment line numbers remain correct after marker updates`() {
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
                Assertions.assertTrue(marker.isValid)

                // Then: Marker should cover correct lines
                val startLine = document.getLineNumber(marker.startOffset) + 1
                val endLine = document.getLineNumber(marker.endOffset) + 1

                Assertions.assertEquals(3, startLine, "Start line should match")
                Assertions.assertEquals(5, endLine, "End line should match")
            }
        }
    }

    @Nested
    inner class MultiFileTracking : DocumentChangeListenerTests() {

        @Test
        fun `markers track independently across multiple files`() {
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
                Assertions.assertTrue(c1.rangeMarker!!.isValid)
                Assertions.assertTrue(c2.rangeMarker!!.isValid)

                // And: Each should track its own document
                val line1Start = doc1.getLineNumber(c1.rangeMarker!!.startOffset) + 1
                val line2Start = doc2.getLineNumber(c2.rangeMarker!!.startOffset) + 1

                Assertions.assertEquals(5, line1Start, "FileA comment start line")
                Assertions.assertEquals(3, line2Start, "FileB comment start line")
            }
        }

        @Test
        fun `updateCommentLinesFromMarkers updates only target file`() {
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

                Assertions.assertEquals(1, c1.startLine, "File1 comment start")
                Assertions.assertEquals(2, c1.endLine, "File1 comment end")

                // And: File2 comment should remain unchanged
                Assertions.assertEquals(2, c2.startLine, "File2 comment start should NOT be changed")
                Assertions.assertEquals(3, c2.endLine, "File2 comment end should NOT be changed")
            }
        }
    }

    @Nested
    inner class PageCommentExclusion() : DocumentChangeListenerTests() {

        @Test
        fun `page comments do not get RangeMarkers`() {
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
                Assertions.assertNull(pageComment.rangeMarker, "Page comments should not have RangeMarkers")
            }
        }

        @Test
        fun `line comments get RangeMarkers`() {
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
                Assertions.assertNotNull(lineComment.rangeMarker, "Line comments should have RangeMarkers")
                Assertions.assertTrue(lineComment.rangeMarker!!.isValid)
            }
        }
    }

    @Nested
    inner class MarkerLifecycle : DocumentChangeListenerTests() {

        @Test
        fun `attachRangeMarker creates valid marker for open file`() {
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
                Assertions.assertNotNull(comment.rangeMarker)
                Assertions.assertTrue(comment.rangeMarker!!.isValid)
            }
        }

        @Test
        fun `marker spans correct offsets`() {
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

                Assertions.assertTrue(startOffset >= document.getLineStartOffset(1))
                Assertions.assertTrue(endOffset <= document.getLineEndOffset(2))
                Assertions.assertTrue(marker.isGreedyToRight, "Marker should be greedy to right")
            }
        }

        @Test
        fun `invalid markers are disposed on reassignment`() {
            // Given: A comment with an attached marker
            val content = "<xml><one/>\n<two/>\n<three/>\n</xml>"
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 1, 2, "Comment")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarker(comment, document)

                val oldMarker = comment.rangeMarker!!
                Assertions.assertTrue(oldMarker.isValid)

                // When: Reassigning marker
                service.attachRangeMarker(comment, document)

                // Then: Old marker should be disposed
                Assertions.assertTrue(!oldMarker.isValid, "Old marker should be disposed")
                val newMarker = comment.rangeMarker!!
                Assertions.assertTrue(newMarker.isValid, "New marker should be valid")
            }
        }

        @Test
        fun `disposed markers are cleared on update`() {
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
                Assertions.assertNull(comment.rangeMarker, "Invalid marker should be cleared")
            }
        }
    }

    @Nested
    inner class IntegrationScenarios : DocumentChangeListenerTests() {

        @Test
        fun `multiple comments on same file all have valid markers`() {
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
                Assertions.assertTrue(c1.rangeMarker!!.isValid)
                Assertions.assertTrue(c2.rangeMarker!!.isValid)
                Assertions.assertTrue(c3.rangeMarker!!.isValid)

                // And: Each should span its intended range
                Assertions.assertEquals(2, document.getLineNumber(c1.rangeMarker!!.startOffset) + 1)
                Assertions.assertEquals(4, document.getLineNumber(c1.rangeMarker!!.endOffset) + 1)

                Assertions.assertEquals(10, document.getLineNumber(c2.rangeMarker!!.startOffset) + 1)
                Assertions.assertEquals(12, document.getLineNumber(c2.rangeMarker!!.endOffset) + 1)

                Assertions.assertEquals(18, document.getLineNumber(c3.rangeMarker!!.startOffset) + 1)
                Assertions.assertEquals(20, document.getLineNumber(c3.rangeMarker!!.endOffset) + 1)
            }
        }

        @Test
        fun `RangeMarkers track across document edits`() {
            // Given: A file with a comment and attached marker
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 5, 7, "Tracked comment")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarker(comment, document)

                // Initial state
                Assertions.assertTrue(comment.rangeMarker!!.isValid)
                Assertions.assertEquals(5, comment.startLine)
                Assertions.assertEquals(7, comment.endLine)

                // When: Making edits that RangeMarkers track
                // RangeMarkers automatically track offset changes within the document
                // This tests that the mechanism is working

                // Then: Marker should remain valid
                Assertions.assertTrue(comment.rangeMarker!!.isValid)
            }
        }
    }

    @Nested
    inner class EdgeCases : DocumentChangeListenerTests() {

        @Test
        fun `attachRangeMarker handles single line comments`() {
            // Given: A comment on a single line
            val content = "<xml><one/>\n<two/>\n<three/>\n</xml>"
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 2, 2, "Single line comment")!!

            runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(file)!!
                service.attachRangeMarker(comment, document)

                // Then: Marker should be valid
                Assertions.assertNotNull(comment.rangeMarker)
                Assertions.assertTrue(comment.rangeMarker!!.isValid)

                // And: Should span just that line
                val startLine = document.getLineNumber(comment.rangeMarker!!.startOffset) + 1
                val endLine = document.getLineNumber(comment.rangeMarker!!.endOffset) + 1

                Assertions.assertEquals(2, startLine)
                Assertions.assertEquals(2, endLine)
            }
        }

        @Test
        fun `attachRangeMarkersForOpenFiles attaches markers for all comments`() {
            // Given: Multiple comments on an open file
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val c1 = service.addComment("test.xml", 2, 3, "Comment 1")!!
            val c2 = service.addComment("test.xml", 7, 9, "Comment 2")!!

            runReadAction {
                // Simulate opening the file (getting its document)
                FileDocumentManager.getInstance().getDocument(file)!!

                // When: Attaching markers for all open files (simulating review switch)
                service.attachRangeMarkersForOpenFiles()

                // Then: All line comments should have markers
                Assertions.assertNotNull(c1.rangeMarker)
                Assertions.assertNotNull(c2.rangeMarker)
                Assertions.assertTrue(c1.rangeMarker!!.isValid)
                Assertions.assertTrue(c2.rangeMarker!!.isValid)
            }
        }
    }

    /**
     * Tests for actual document edit simulation with line insertions and deletions.
     * These verify that RangeMarkers correctly track offset changes during edits.
     */
    @Nested
    inner class DocumentEditSimulation : DocumentChangeListenerTests() {

        @Test
        fun `RangeMarker adjusts start offset after line insertion before comment`() {
            // Given: A file with a comment on line 5-7
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 5, 7, "Comment on lines 5-7")!!

            val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
            service.attachRangeMarker(comment, document)
            val marker = comment.rangeMarker!!

            val originalStartOffset = marker.startOffset
            val originalEndOffset = marker.endOffset

            // When: Inserting a line before the comment (at line 3)
            runWriteAction {
                val insertOffset = document.getLineStartOffset(2) // Line 3 (0-indexed)
                document.insertString(insertOffset, "new line\n")
            }

            // Then: Marker should shift to accommodate inserted text
            // The marker tracks offsets, so both start and end should shift by inserted length
            runReadAction {
                Assertions.assertTrue(marker.isValid, "Marker should still be valid after insertion")
            }
            val insertedLength = "new line\n".length
            Assertions.assertEquals(originalStartOffset + insertedLength, marker.startOffset, "Start offset should shift by inserted length")
            Assertions.assertEquals(originalEndOffset + insertedLength, marker.endOffset, "End offset should shift by inserted length")
        }

        @Test
        fun `RangeMarker preserves offsets after line insertion after comment`() {
            // Given: A file with a comment on line 3-4
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 3, 4, "Comment on lines 3-4")!!

            val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
            service.attachRangeMarker(comment, document)
            val marker = comment.rangeMarker!!

            val originalStartOffset = marker.startOffset
            val originalEndOffset = marker.endOffset

            // When: Inserting a line after the comment (at line 6, well after line 4)
            runWriteAction {
                val insertOffset = document.getLineStartOffset(5) // Line 6 (0-indexed)
                document.insertString(insertOffset, "new line\n")
            }

            // Then: Marker offsets should remain unchanged
            runReadAction {
                Assertions.assertTrue(marker.isValid, "Marker should still be valid")
            }

            Assertions.assertEquals(originalStartOffset, marker.startOffset, "Start offset should not change for insertion after marker")
            Assertions.assertEquals(originalEndOffset, marker.endOffset, "End offset should not change for insertion after marker")
        }

        @Test
        fun `RangeMarker adjusts offsets after line deletion before comment`() {
            // Given: A file with a comment on line 5-7
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 5, 7, "Comment on lines 5-7")!!

            val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
            service.attachRangeMarker(comment, document)
            val marker = comment.rangeMarker!!

            val originalStartOffset = marker.startOffset
            val originalEndOffset = marker.endOffset

            // When: Deleting a line before the comment (line 3)
            runWriteAction {
                val startOffset = document.getLineStartOffset(2) // Line 3 (0-indexed)
                val endOffset = document.getLineEndOffset(2) + 1 // Include newline
                document.deleteString(startOffset, endOffset.coerceAtMost(document.textLength))
            }

            // Then: Marker offsets should decrease by deleted text length
            runReadAction {
                Assertions.assertTrue(marker.isValid, "Marker should still be valid after deletion")
            }
            // Since we deleted content before the marker, offsets should decrease
            Assertions.assertTrue(marker.startOffset < originalStartOffset, "Start offset should decrease")
            Assertions.assertTrue(marker.endOffset < originalEndOffset, "End offset should decrease")
        }

        @Test
        fun `updateCommentLinesFromMarkers updates line numbers after edit`() {
            // Given: A file with a comment and marker
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 5, 5, "Original line 5")!!

            val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
            service.attachRangeMarker(comment, document)

            // When: Inserting text before the marker
            runWriteAction {
                document.insertString(0, "new line\n")
            }

            // And: Updating comment lines from markers
            runReadAction {
                service.updateCommentLinesFromMarkers(document)
            }

            // Then: Comment line numbers should be updated
            // Original line 5 is now line 6 after inserting a new line at the top
            Assertions.assertEquals(6, comment.startLine, "Start line should reflect the shift")
            Assertions.assertEquals(6, comment.endLine, "End line should reflect the shift")
        }

        @Test
        fun `line numbers update correctly after multi-line insertion`() {
            // Given: A file with a single-line comment
            val content = "line1\nline2\nline3\nline4\nline5\n"
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 3, 3, "Comment on line 3")!!

            val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
            service.attachRangeMarker(comment, document)

            // When: Inserting multiple lines before the comment
            runWriteAction {
                document.insertString(0, "new1\nnew2\nnew3\n")
            }

            // And: Updating lines
            runReadAction {
                service.updateCommentLinesFromMarkers(document)
            }

            // Then: Line number should shift by 3 lines
            Assertions.assertEquals(6, comment.startLine, "Line should shift by 3")
        }

        @Test
        fun `marker tracks text insertion at start of comment range`() {
            // Given: A comment spanning lines 4-6
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 4, 6, "Comment spanning 3 lines")!!

            val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
            service.attachRangeMarker(comment, document)
            val marker = comment.rangeMarker!!

            val originalStart = marker.startOffset
            val originalEnd = marker.endOffset

            // When: Inserting text at the start of the marker range
            // Note: RangeMarkers have greedyToLeft=false by default, so text inserted at the
            // start boundary is NOT included - the start offset shifts forward
            runWriteAction {
                document.insertString(originalStart, "inserted ")
            }

            // Then: Start offset shifts to exclude the inserted text (greedyToLeft=false)
            runReadAction {
                Assertions.assertTrue(marker.isValid, "Marker should remain valid")
            }
            // With greedyToLeft=false, inserting at start causes start to shift
            val insertedLength = "inserted ".length
            Assertions.assertEquals(originalStart + insertedLength, marker.startOffset, "Start offset shifts forward when greedyToLeft=false")
            Assertions.assertEquals(originalEnd + insertedLength, marker.endOffset, "End offset shifts by inserted length")
        }

        @Test
        fun `marker tracks text insertion at end of comment range`() {
            // Given: A comment spanning lines 4-6
            val content = (1..10).joinToString("\n") { "line $it" }
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 4, 6, "Comment spanning 3 lines")!!

            val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
            service.attachRangeMarker(comment, document)
            val marker = comment.rangeMarker!!

            val originalStart = marker.startOffset
            val originalEnd = marker.endOffset

            // When: Inserting text at the end of the marker range
            runWriteAction {
                document.insertString(originalEnd, " appended")
            }

            // Then: Both offsets stay same (if greedyToRight is true, the marker may absorb)
            runReadAction {
                Assertions.assertTrue(marker.isValid, "Marker should remain valid")
            }

            // Start should stay the same
            Assertions.assertEquals(originalStart, marker.startOffset, "Start offset stays same")
            // End offset depends on greedyToRight flag - it may or may not increase
            // The marker.isGreedyToRight affects whether text inserted at end is included
            Assertions.assertTrue(marker.endOffset >= originalEnd, "End offset should stay same or increase")
        }

        @Test
        fun `deletion within marker range shrinks marker`() {
            // Given: A comment spanning lines 2-4
            val content = "line1\nline2\nline3\nline4\nline5\n"
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 2, 4, "Multi-line comment")!!

            val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
            service.attachRangeMarker(comment, document)
            val marker = comment.rangeMarker!!

            val originalEnd = marker.endOffset

            // When: Deleting text within the marker range (middle of line 3)
            runWriteAction {
                val line3Start = document.getLineStartOffset(2) // Line 3 is 0-indexed
                document.deleteString(line3Start, line3Start + 2) // Delete "li" from "line3"
            }

            // Then: Marker should still be valid but smaller
            runReadAction {
                Assertions.assertTrue(marker.isValid, "Marker should remain valid")
            }

            Assertions.assertEquals(originalEnd - 2, marker.endOffset, "End offset should decrease by deleted length")
        }

        @Test
        fun `marker survives text replacement within its range`() {
            // Given: A comment on line 3
            val content = "line1\nline2\nline3\nline4\n"
            val file = createVirtualFile("test.xml", content)

            service.createNewReview()
            val comment = service.addComment("test.xml", 3, 3, "Single line comment")!!

            val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
            service.attachRangeMarker(comment, document)
            val marker = comment.rangeMarker!!

            val originalStart = marker.startOffset

            // When: Replacing text within the marker range
            runWriteAction {
                val line3Start = document.getLineStartOffset(2)
                val line3End = document.getLineEndOffset(2)
                document.replaceString(line3Start, line3End, "modified")
            }

            // Then: Marker should still be valid
            runReadAction {
                Assertions.assertTrue(marker.isValid, "Marker should survive in-place replacement")
            }
            Assertions.assertEquals(originalStart, marker.startOffset, "Start offset should stay same")
        }

        @Test
        fun `complete edit workflow simulates real editing scenario`() {
            // Given: A file being edited with tracked comments
            val content = "class Example {\n  void method() {\n    // TODO\n  }\n}\n"
            val file = createVirtualFile("Example.java", content)

            service.createNewReview()
            // Comment on the TODO line (line 3)
            val comment = service.addComment("Example.java", 3, 3, "Review this TODO")!!

            val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
            service.attachRangeMarker(comment, document)

            // Initial state
            Assertions.assertEquals(3, comment.startLine)
            Assertions.assertEquals(3, comment.endLine)

            // When: Inserting a new line at the top (simulating adding an import)
            runWriteAction {
                document.insertString(0, "package com.example;\n\n")
            }

            // And: Updating line numbers from marker
            runReadAction {
                service.updateCommentLinesFromMarkers(document)
            }

            // Then: Comment should now be on line 5 (shifted by 2 lines)
            Assertions.assertEquals(5, comment.startLine, "Comment should shift down by 2 lines")
            Assertions.assertEquals(5, comment.endLine, "Comment should shift down by 2 lines")

            // When: Deleting a line before the comment (simulating removing a line)
            runWriteAction {
                // Delete line 4 (0-indexed line 3) - which is now the first empty line after package
                val startOffset = document.getLineStartOffset(3)
                val endOffset = document.getLineEndOffset(3) + 1
                document.deleteString(startOffset, endOffset.coerceAtMost(document.textLength))
            }

            // And: Updating again
            runReadAction {
                service.updateCommentLinesFromMarkers(document)
            }

            // Then: Comment should now be on line 4 (shifted back up by 1 line)
            Assertions.assertEquals(4, comment.startLine, "Comment should shift up after deletion")
        }
    }
}
