package com.github.cpardi.markdowncodereview

import com.github.cpardi.markdowncodereview.services.Comment
import com.github.cpardi.markdowncodereview.services.ReviewFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Common test utilities for the Review Markdown Generator test suite.
 * Provides factory methods, assertion helpers, and test data builders.
 */
object BaseTestHelper {

    // ==================== Comment Factory Methods ====================

    /**
     * Creates a line comment with the specified properties.
     *
     * @param id The comment ID
     * @param path The relative file path
     * @param startLine The start line (1-based)
     * @param endLine The end line (1-based)
     * @param body The comment body text
     * @return A new Comment instance
     */
    fun createComment(
        id: Int,
        path: String,
        startLine: Int,
        endLine: Int,
        body: String
    ): Comment {
        return Comment(
            id = id,
            relativePath = path,
            startLine = startLine,
            endLine = endLine,
            body = body
        )
    }

    /**
     * Creates a page comment (applies to entire file) with the specified properties.
     *
     * @param id The comment ID
     * @param path The relative file path
     * @param body The comment body text
     * @return A new Comment instance with startLine=0 and endLine=0
     */
    fun createPageComment(
        id: Int,
        path: String,
        body: String
    ): Comment {
        return Comment(
            id = id,
            relativePath = path,
            startLine = 0,
            endLine = 0,
            body = body
        )
    }

    // ==================== ReviewFile Factory Methods ====================

    /**
     * Creates a ReviewFile with the specified name and comments.
     *
     * @param name The review name (without .md extension)
     * @param comments The list of comments
     * @return A new ReviewFile instance
     */
    fun createReviewFile(
        name: String,
        comments: List<Comment> = emptyList()
    ): ReviewFile {
        return ReviewFile(
            name = name,
            virtualFile = null,
            comments = CopyOnWriteArrayList(comments),
            preamble = "",
            postamble = ""
        )
    }

    /**
     * Creates a ReviewFile with preamble and postamble.
     *
     * @param name The review name
     * @param comments The list of comments
     * @param preamble Text before first header
     * @param postamble Text after last delimiter
     * @return A new ReviewFile instance
     */
    fun createReviewFileWithMeta(
        name: String,
        comments: List<Comment> = emptyList(),
        preamble: String,
        postamble: String
    ): ReviewFile {
        return ReviewFile(
            name = name,
            virtualFile = null,
            comments = CopyOnWriteArrayList(comments),
            preamble = preamble,
            postamble = postamble
        )
    }

    // ==================== Markdown Content Builders ====================

    /**
     * Builds a Markdown review file content string from comments.
     *
     * @param comments Variable number of comments to include
     * @param preamble Optional text before first header
     * @param postamble Optional text after last delimiter
     * @return A formatted Markdown string
     */
    fun buildReviewMarkdown(
        vararg comments: Comment,
        preamble: String = "",
        postamble: String = ""
    ): String {
        val output = StringBuilder()

        if (preamble.isNotEmpty()) {
            output.append(preamble)
            if (comments.isNotEmpty()) {
                output.append("\n\n")
            }
        }

        for ((index, comment) in comments.withIndex()) {
            output.append(comment.toHeader())
            output.append("\n")
            if (comment.body.isNotEmpty()) {
                output.append(comment.body)
                output.append("\n")
            }
            output.append("---")
            if (index < comments.size - 1) {
                output.append("\n")
            }
        }

        if (postamble.isNotEmpty()) {
            output.append("\n")
            output.append(postamble)
        }

        return output.toString()
    }

    // ==================== Assertion Helpers ====================

    /**
     * Asserts that two comments are equal in all their properties.
     * Provides detailed failure message showing which property differs.
     *
     * @param expected The expected comment
     * @param actual The actual comment
     * @param message Optional custom failure message prefix
     */
    fun assertCommentContentEquals(
        expected: Comment?,
        actual: Comment?,
        message: String = ""
    ) {
        val prefix = if (message.isNotEmpty()) "$message: " else ""

        if (expected == null && actual == null) return

        if (expected == null) {
            fail { "${prefix}Expected null, but was: $actual" }
        }

        if (actual == null) {
            fail { "${prefix}Expected: $expected, but was null" }
        }

        assertEquals(expected.relativePath, actual.relativePath, "${prefix}Comment relativePath mismatch")
        assertEquals(expected.startLine, actual.startLine, "${prefix}Comment startLine mismatch")
        assertEquals(expected.endLine, actual.endLine, "${prefix}Comment endLine mismatch")
        assertEquals(expected.body, actual.body, "${prefix}Comment body mismatch")
    }

    /**
     * Asserts that two lists of comments are equal in order and content.
     *
     * @param expected The expected list of comments
     * @param actual The actual list of comments
     * @param message Optional custom failure message prefix
     */
    fun assertCommentContentsListEquals(
        expected: List<Comment>,
        actual: List<Comment>,
        message: String = ""
    ) {
        val prefix = if (message.isNotEmpty()) "$message: " else ""

        assertEquals(expected.size, actual.size, "${prefix}Comment list size mismatch")

        for (i in expected.indices) {
            assertCommentContentEquals(expected[i], actual[i], "${prefix}Comment at index $i")
        }
    }

    /**
     * Asserts that two ReviewFile objects are equal in all their properties.
     * Does not compare virtualFile references.
     *
     * @param expected The expected review file
     * @param actual The actual review file
     * @param message Optional custom failure message prefix
     */
    fun assertReviewFileEquals(
        expected: ReviewFile?,
        actual: ReviewFile?,
        message: String = ""
    ) {
        val prefix = if (message.isNotEmpty()) "$message: " else ""

        if (expected == null && actual == null) return

        if (expected == null) {
            fail { "${prefix}Expected null, but was: $actual" }
        }

        if (actual == null) {
            fail { "${prefix}Expected: $expected, but was null" }
        }

        assertEquals(expected.name, actual.name, "${prefix}ReviewFile name mismatch")
        assertEquals(expected.preamble, actual.preamble, "${prefix}ReviewFile preamble mismatch")
        assertEquals(expected.postamble, actual.postamble, "${prefix}ReviewFile postamble mismatch")
        assertCommentContentsListEquals(expected.comments.toList(), actual.comments.toList(), "${prefix}ReviewFile comments")
    }

    // ==================== Test Resource Loading ====================

    /**
     * Loads a test resource file from the testData directory as a string.
     *
     * @param path Relative path from src/test/testData/
     * @return The file content as a string
     * @throws IllegalArgumentException if the resource cannot be found
     */
    fun loadTestResource(path: String): String {
        val classLoader = BaseTestHelper::class.java.classLoader
        val resourcePath = if (path.startsWith("testData/")) path else "testData/$path"

        val inputStream = classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Test resource not found: $resourcePath")

        return inputStream.bufferedReader().use { it.readText() }
    }

    /**
     * Gets the path to a test resource file.
     *
     * @param path Relative path from src/test/testData/
     * @return The absolute path to the resource
     * @throws IllegalArgumentException if the resource cannot be found
     */
    fun getTestResourcePath(path: String): String {
        val classLoader = BaseTestHelper::class.java.classLoader
        val resourcePath = if (path.startsWith("testData/")) path else "testData/$path"

        val resourceUrl = classLoader.getResource(resourcePath)
            ?: throw IllegalArgumentException("Test resource not found: $resourcePath")

        return resourceUrl.path
    }
}
