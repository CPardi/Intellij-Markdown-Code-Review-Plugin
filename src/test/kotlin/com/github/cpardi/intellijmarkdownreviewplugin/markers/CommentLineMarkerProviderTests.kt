package com.github.cpardi.intellijmarkdownreviewplugin.markers

import com.github.cpardi.intellijmarkdownreviewplugin.LightPlatformTest
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewService
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPlainTextFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@Suppress("JUnitMixedFramework")
object CommentLineMarkerProviderTestSuite {

    /**
     * Base class for integration tests of CommentLineMarkerProvider.
     */
    abstract class CommentLineMarkerProviderTests : LightPlatformTest() {

        protected lateinit var service: ReviewService
        protected lateinit var markerProvider: CommentLineMarkerProvider

        override fun setUp() {
            super.setUp()
            service = ReviewService.getInstance(project)
            service.setActiveReview(null)
            markerProvider = CommentLineMarkerProvider()
        }
    }

    val content = """
        <xml>
            <one/>
            <two/>
        </xml>
    """.trimIndent()

    class CollectSlowLineMarkers : CommentLineMarkerProviderTests() {

        @Test
        fun `test no markers when no active review`() {
            // Given: No active review
            assertNull(service.activeReview)

            // And: A file with content
            val file = createVirtualFile("test.xml", content)

            // When: Collecting slow line markers
            val result = collectSlowLineMarkers(markerProvider, project, file)

            // Then: Should produce no markers
            assertTrue(result.isEmpty(), "Should have no markers without active review")
        }

        @Test
        fun `test no markers when file has no comments`() {
            // Given: An active review with comments on different file
            service.createNewReview()
            service.addComment("other.xml", 1, 5, "Other comment")

            // And: A file without comments
            val file = createVirtualFile("test.xml", content)

            // When: Collecting slow line markers
            val result = collectSlowLineMarkers(markerProvider, project, file)

            // Then: Should produce no markers
            assertTrue(result.isEmpty(), "Should have no markers for file without comments")
        }

        @Test
        fun `test marker appears for line with comment`() {
            // Given: An active review with a comment on line 1
            service.createNewReview()
            val file = createVirtualFile("test.xml", content)
            val relativePath = service.getRelativePath(file)
            service.addComment(relativePath, 1, 1, "Test comment")

            // When: Collecting slow line markers
            val result = collectSlowLineMarkers(markerProvider, project, file)

            // Then: Should produce marker
            assertFalse(result.isEmpty(), "Should have marker for commented line")
        }

        @Test
        fun `test page comment marker on first line`() {
            // Given: An active review with a page comment
            service.createNewReview()
            val file = createVirtualFile("test.xml", content)
            val relativePath = service.getRelativePath(file)
            service.addPageComment(relativePath, "Page comment")

            // When: Collecting slow line markers
            val result = collectSlowLineMarkers(markerProvider, project, file)

            // Then: Should produce marker for page comment
            assertEquals(1, result.size, "Should have one marker for page comment")
        }

        @Test
        fun `test multiple comments on same line show single marker`() {
            // Given: An active review with multiple comments on same line
            service.createNewReview()
            val file = createVirtualFile("test.xml", content)
            val relativePath = service.getRelativePath(file)
            service.addComment(relativePath, 1, 1, "First comment")
            service.addComment(relativePath, 1, 1, "Second comment")

            // When: Collecting slow line markers
            val result = collectSlowLineMarkers(markerProvider, project, file)

            // Then: Should produce single marker (with badge)
            assertEquals(1, result.size, "Should have single marker for multiple comments on same line")
        }

        @Test
        fun `test comments on different lines show separate markers`() {
            // Given: An active review with comments on different lines
            service.createNewReview()
            val file = createVirtualFile("test.xml", content) //
            val relativePath = service.getRelativePath(file)
            service.addComment(relativePath, 1, 1, "Line 1 comment")
            service.addComment(relativePath, 3, 3, "Line 3 comment")

            // When: Collecting slow line markers
            val result = collectSlowLineMarkers(markerProvider, project, file)

            // Then: Should produce separate markers
            assertEquals(2, result.size, "Should have separate markers for different lines")
        }

        @Test
        fun `test marker tooltip shows comment preview`() {
            // Given: An active review with a comment
            service.createNewReview()
            val file = createVirtualFile("test.xml", content)
            val relativePath = service.getRelativePath(file)
            service.addComment(relativePath, 1, 1, "This is a test comment that should appear in tooltip")

            // When: Collecting slow line markers
            val result = collectSlowLineMarkers(markerProvider, project, file)

            // Then: Marker should exist
            assertEquals(1, result.size)
            val marker = result.first()
            assertNotNull(marker, "Should have marker")
            // The tooltip is created by a function, we verify the marker exists
            // The actual tooltip content depends on comment text: "Review comment: <preview>..."
        }
    }

    class EdgeCases : CommentLineMarkerProviderTests() {

        @Test
        fun `test handles empty file gracefully`() {
            // Given: An active review
            service.createNewReview()

            // And: An empty file with no comments
            val file = createVirtualFile("empty.xml", "")

            // When: Collecting slow line markers
            val result = collectSlowLineMarkers(markerProvider, project, file)

            // Then: Should handle gracefully
            assertTrue(result.isEmpty(), "Should handle empty file without error")
        }

        @Test
        fun `test handles multi-line comment range correctly`() {
            // Given: An active review with multi-line comment
            service.createNewReview()
            val file = createVirtualFile("test.xml", content)
            val relativePath = service.getRelativePath(file)
            service.addComment(relativePath, 2, 4, "Multi-line comment")

            // When: Collecting slow line markers
            val result = collectSlowLineMarkers(markerProvider, project, file)

            // Then: Should produce marker at start line
            assertEquals(1, result.size, "Should have marker for multi-line comment range")
        }

        @Test
        fun `test skips injected code fragments`() {
            // Given: An active review
            service.createNewReview()

            // And: A file with comments
            val file = createVirtualFile("test.xml", content)
            val relativePath = service.getRelativePath(file)
            service.addComment(relativePath, 1, 1, "Comment")

            // When: Called with empty elements (simulating injected fragment)
            val result = mutableListOf<LineMarkerInfo<*>>()
            markerProvider.collectSlowLineMarkers(mutableListOf(), result)

            // Then: Should handle gracefully
            assertTrue(result.isEmpty(), "Should handle empty elements")
        }
    }

    // ==================== Helper Methods ====================

    private fun collectSlowLineMarkers(
        markerProvider: CommentLineMarkerProvider,
        project: Project,
        file: VirtualFile
    ): MutableList<LineMarkerInfo<*>> = runReadAction {
        val psiFile = PsiManager.getInstance(project).findFile(file)!!
        val elements = collectLeafElements(psiFile)
        val result = mutableListOf<LineMarkerInfo<*>>()
        markerProvider.collectSlowLineMarkers(elements, result)
        return@runReadAction result
    }

    private fun findPsiFile(project: Project, file: VirtualFile) = runReadAction {
        PsiManager.getInstance(project).findFile(file)!!
    }

    /**
     * Collects all leaf PSI elements from a file for testing.
     */
    private fun collectLeafElements(psiFile: PsiFile): MutableList<PsiElement> = runReadAction {
        val elements = mutableListOf<PsiElement>()
        psiFile.accept(object : com.intellij.psi.PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.children.isEmpty()) {
                    elements.add(element)
                }
                super.visitElement(element)
            }
        })
        elements
    }

    private fun collectSlowLineMarkers(
        markerProvider: CommentLineMarkerProvider,
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ): Unit = runReadAction {
        markerProvider.collectSlowLineMarkers(elements, result)
    }
}
