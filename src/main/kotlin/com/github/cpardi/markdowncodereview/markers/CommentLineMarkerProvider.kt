package com.github.cpardi.markdowncodereview.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.github.cpardi.markdowncodereview.services.Comment
import com.github.cpardi.markdowncodereview.services.ReviewService
import com.github.cpardi.markdowncodereview.ui.EditCommentsDialog
import com.github.cpardi.markdowncodereview.util.BadgeIconFactory
import com.intellij.psi.PsiCodeFragment

/**
 * Provides gutter icons for lines with comments.
 * Shows a bubble icon on lines that have review comments.
 * For page comments, shows an icon on line 1.
 * If multiple comments exist on the same line, shows a badge with the count.
 */
class CommentLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return null
    }

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.isEmpty()) return

        val element = elements.firstOrNull() ?: return
        val project = element.project
        val psiFile = element.containingFile ?: return
        val virtualFile = psiFile.virtualFile ?: return

        // Only process the base language file, not injected fragments
        if (psiFile is PsiCodeFragment) return
        if (psiFile.language != psiFile.viewProvider.baseLanguage) return
        if (!virtualFile.isInLocalFileSystem) return

        val service = ReviewService.getInstance(project)
        val relativePath = service.getRelativePath(virtualFile)

        val comments = service.getCommentsForFile(relativePath)
        if (comments.isEmpty()) return

        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return

        // Separate page comments from line comments
        val pageComments = comments.filter { it.isPageComment() }
        val lineComments = comments.filter { !it.isPageComment() }

        // Add page comment marker on line 1
        if (pageComments.isNotEmpty()) {
            val anchorElement = elements.firstOrNull { it.containingFile.viewProvider.baseLanguage == it.language }
            if(anchorElement != null) {
                val tooltip = if (pageComments.size == 1) {
                    "Page comment: ${pageComments.first().body.take(50)}..."
                } else {
                    "${pageComments.size} page comments"
                }

                val marker = LineMarkerInfo(
                    anchorElement,
                    anchorElement.textRange,
                    AllIcons.FileTypes.Text, // Different icon for page comments
                    { tooltip },
                    { _, _ -> showPageCommentEditDialog(project, pageComments) },
                    GutterIconRenderer.Alignment.RIGHT,
                    { tooltip }
                )
                result.add(marker)
            }
        }

        // Group line comments by their start line
        val lineCommentsByLine = lineComments.groupBy { it.startLine }

        for ((line, lineCommentList) in lineCommentsByLine) {
            val anchorElement = findElementAtLine(elements, document, line) ?: continue

            val tooltip = if (lineCommentList.size == 1) {
                "Review comment: ${lineCommentList.first().body.take(50)}..."
            } else {
                "${lineCommentList.size} review comments"
            }

            val marker = LineMarkerInfo(
                anchorElement,
                anchorElement.textRange,
                getIconForComments(lineCommentList),
                { tooltip },
                { _, _ -> showEditDialog(project, lineCommentList) },
                GutterIconRenderer.Alignment.RIGHT,
                { tooltip }
            )

            result.add(marker)
        }
    }

    /**
     * Finds a PSI element on the specified line from the provided elements list.
     * Prefers non-whitespace elements for more stable anchor positioning.
     *
     * @param elements The list of leaf PSI elements from collectSlowLineMarkers
     * @param document The document being processed
     * @param line The 1-based line number
     * @return A PSI element on the target line, or null if not found
     */
    private fun findElementAtLine(elements: MutableList<out PsiElement>, document: Document, line: Int): PsiElement? {
        val lineIndex = line - 1 // Convert to 0-based
        if (lineIndex < 0 || lineIndex >= document.lineCount) return null

        val lineStartOffset = document.getLineStartOffset(lineIndex)
        val lineEndOffset = document.getLineEndOffset(lineIndex)

        // Find first non-whitespace element on the target line
        // This avoids issues where whitespace tokens can span line boundaries
        val nonWhitespaceElement = elements.firstOrNull { element ->
            val offset = element.textOffset
            offset in lineStartOffset..lineEndOffset && element.text.isNotBlank()
        }

        if (nonWhitespaceElement != null) {
            return nonWhitespaceElement
        }

        // Fallback: find any element on the line (including whitespace)
        return elements.firstOrNull { element ->
            element.textOffset in lineStartOffset..lineEndOffset
        }
    }

    private fun getIconForComments(comments: List<Comment>): javax.swing.Icon {
        return if (comments.size > 1) {
            BadgeIconFactory.getBadgeIcon(comments.size)
        } else {
            AllIcons.General.Balloon
        }
    }

    private fun showEditDialog(project: Project, comments: List<Comment>) {
        EditCommentsDialog.show(project, comments)
    }

    private fun showPageCommentEditDialog(project: Project, comments: List<Comment>) {
        EditCommentsDialog.show(project, comments)
    }
}
