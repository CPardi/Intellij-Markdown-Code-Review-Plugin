package com.github.cpardi.intellijmarkdownreviewplugin.parser

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.VirtualFile
import com.github.cpardi.intellijmarkdownreviewplugin.services.Comment
import com.github.cpardi.intellijmarkdownreviewplugin.services.ReviewFile
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Parser for review markdown files.
 * Parses `reviews/<name>.md` files into `ReviewFile` objects.
 */
object ReviewFileParser {

    private val LOG = thisLogger()
    
    // Header pattern for line comments: @[<relative-path>:<start-line>:<end-line>]:
    private val HEADER_REGEX = Regex("""^@\[(.+):(\d+):(\d+)\]:""")
    
    // Header pattern for page comments: @[<relative-path>]:
    private val PAGE_HEADER_REGEX = Regex("""^@\[(.+)\]:""")
    
    /**
     * Parses a review file from a VirtualFile.
     *
     * @param virtualFile The virtual file to parse
     * @return A ReviewFile object, or null if parsing fails
     */
    fun parse(virtualFile: VirtualFile): ReviewFile? {
        return try {
            val content = virtualFile.inputStream.bufferedReader().use { it.readText() }
            val name = virtualFile.nameWithoutExtension
            parseContent(name, content, virtualFile)
        } catch (e: Exception) {
            LOG.error("Failed to parse review file: ${virtualFile.path}", e)
            null
        }
    }

    /**
     * Parses review content from a string.
     *
     * @param name The review name (filename without extension)
     * @param content The file content
     * @param virtualFile Optional virtual file reference
     * @return A ReviewFile object, or null if parsing fails
     */
    fun parseContent(name: String, content: String, virtualFile: VirtualFile? = null): ReviewFile? {
        return try {
            val lines = content.lines()
            val comments = CopyOnWriteArrayList<Comment>()
            val preamble = StringBuilder()
            val postamble = StringBuilder()
            
            var currentLineIndex = 0
            var foundFirstHeader = false
            
            // Extract preamble (text before first header)
            while (currentLineIndex < lines.size) {
                val line = lines[currentLineIndex]
                if (HEADER_REGEX.matches(line) || PAGE_HEADER_REGEX.matches(line)) {
                    foundFirstHeader = true
                    break
                }
                if (preamble.isNotEmpty()) {
                    preamble.append("\n")
                }
                preamble.append(line)
                currentLineIndex++
            }
            
            // Parse comments
            var nextId = 1
            while (currentLineIndex < lines.size) {
                val line = lines[currentLineIndex]
                
                // Try line comment header first (more specific)
                val lineHeaderMatch = HEADER_REGEX.matchEntire(line)
                if (lineHeaderMatch != null) {
                    val (relativePath, startLine, endLine) = lineHeaderMatch.destructured
                    val comment = parseLineComment(nextId, relativePath, startLine.toInt(), endLine.toInt(), lines, currentLineIndex + 1)
                    comments.add(comment.first)
                    currentLineIndex = comment.second
                    nextId++
                    continue
                }
                
                // Try page comment header
                val pageHeaderMatch = PAGE_HEADER_REGEX.matchEntire(line)
                if (pageHeaderMatch != null) {
                    val (relativePath) = pageHeaderMatch.destructured
                    val comment = parsePageComment(nextId, relativePath, lines, currentLineIndex + 1)
                    comments.add(comment.first)
                    currentLineIndex = comment.second
                    nextId++
                    continue
                }
                
                if (line == "---") {
                    // Skip standalone delimiters
                    currentLineIndex++
                } else {
                    // Unknown content after first header - skip
                    currentLineIndex++
                }
            }
            
            val reviewFile = ReviewFile(
                name = name,
                virtualFile = virtualFile,
                comments = comments,
                preamble = preamble.toString(),
                postamble = postamble.toString()
            )
            
            LOG.info("Parsed ${comments.size} comments from review: $name")
            reviewFile
        } catch (e: Exception) {
            LOG.error("Failed to parse review content: $name", e)
            null
        }
    }

    /**
     * Parses the body content of a comment (shared logic for both line and page comments).
     *
     * @param lines All lines in the file
     * @param startIndex The index of the first body line (after header)
     * @return A pair of the body lines and the next line index to process
     */
    private fun parseCommentBody(
        lines: List<String>,
        startIndex: Int
    ): Pair<List<String>, Int> {
        val bodyLines = mutableListOf<String>()
        var currentIndex = startIndex
        
        while (currentIndex < lines.size) {
            val line = lines[currentIndex]
            
            // Stop at delimiter
            if (line == "---") {
                currentIndex++ // Consume the delimiter
                break
            }
            
            // Stop at next header (means previous comment had no delimiter - malformed but handle gracefully)
            if (HEADER_REGEX.matches(line) || PAGE_HEADER_REGEX.matches(line)) {
                break
            }
            
            bodyLines.add(line)
            currentIndex++
        }
        
        return Pair(bodyLines, currentIndex)
    }
    
    /**
     * Parses a line comment starting from the body.
     *
     * @param id The sequential ID to assign to this comment
     * @param relativePath The file path from the header
     * @param startLine The start line from the header
     * @param endLine The end line from the header
     * @param lines All lines in the file
     * @param startIndex The index of the first body line (after header)
     * @return A pair of the Comment and the next line index to process
     */
    private fun parseLineComment(
        id: Int,
        relativePath: String,
        startLine: Int,
        endLine: Int,
        lines: List<String>,
        startIndex: Int
    ): Pair<Comment, Int> {
        val (bodyLines, nextIndex) = parseCommentBody(lines, startIndex)
        val body = bodyLines.joinToString("\n")
        return Pair(
            Comment(
                id = id,
                relativePath = relativePath,
                startLine = startLine,
                endLine = endLine,
                body = body
            ),
            nextIndex
        )
    }
    
    /**
     * Parses a page comment starting from the body.
     *
     * @param id The sequential ID to assign to this comment
     * @param relativePath The file path from the header
     * @param lines All lines in the file
     * @param startIndex The index of the first body line (after header)
     * @return A pair of the Comment and the next line index to process
     */
    private fun parsePageComment(
        id: Int,
        relativePath: String,
        lines: List<String>,
        startIndex: Int
    ): Pair<Comment, Int> {
        val (bodyLines, nextIndex) = parseCommentBody(lines, startIndex)
        val body = bodyLines.joinToString("\n")
        return Pair(
            Comment(
                id = id,
                relativePath = relativePath,
                startLine = 0,
                endLine = 0,
                body = body
            ),
            nextIndex
        )
    }
}