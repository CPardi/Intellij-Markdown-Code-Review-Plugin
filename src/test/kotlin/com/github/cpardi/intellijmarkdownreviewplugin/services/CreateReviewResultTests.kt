package com.github.cpardi.intellijmarkdownreviewplugin.services

import com.github.cpardi.intellijmarkdownreviewplugin.UnitTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Comprehensive unit tests for CreateReviewResult sealed class.
 * Tests Success/Failure variants, property access, and exhaustiveness checking.
 *
 * Note: Tests for getOrShowError() method are not included because:
 * - The method calls Messages.showErrorDialog() which requires UI thread and IntelliJ Platform
 * - Testing UI dialogs requires integration tests with LightPlatformTest (Phase 5)
 * - These tests will be added in the integration test phase
 */
class CreateReviewResultTests : UnitTest() {

    // ==================== Success Variant Tests ====================

    @Nested
    inner class SuccessVariant {

        @Test
        fun `test Success creation with name`() {
            // Given: A success result
            val result = CreateReviewResult.Success("my-review")

            // Then: Should have correct name property
            assertEquals("my-review", result.name, "Success should have correct name")
        }

        @Test
        fun `test Success with empty name`() {
            // Given: A success result with empty name
            val result = CreateReviewResult.Success("")

            // Then: Should allow empty name (though not recommended in practice)
            assertEquals("", result.name, "Should allow empty name")
        }

        @Test
        fun `test Success with special characters in name`() {
            // Given: A success result with special characters
            val result = CreateReviewResult.Success("review-2024_01@branch")

            // Then: Should preserve special characters
            assertEquals("review-2024_01@branch", result.name, "Should preserve special characters")
        }

        @Test
        fun `test Success data class properties`() {
            // Given: Two success results with same name
            val result1 = CreateReviewResult.Success("review")
            val result2 = CreateReviewResult.Success("review")

            // Then: Should be equal (data class equality)
            assertEquals(result1, result2, "Success instances with same name should be equal")
        }

        @Test
        fun `test Success copy creates independent instance`() {
            // Given: A success result
            val original = CreateReviewResult.Success("original")

            // When: Copying with different name
            val copy = original.copy(name = "copy")

            // Then: Should be independent
            assertEquals("original", original.name, "Original should be unchanged")
            assertEquals("copy", copy.name, "Copy should have new name")
        }

        @Test
        fun `test Success component1 equals name`() {
            // Given: A success result
            val result = CreateReviewResult.Success("my-review")

            // When: Destructuring
            val (name) = result

            // Then: Component should equal name
            assertEquals(result.name, name, "Destructured name should match property")
        }
    }

    // ==================== Failure Variant Tests ====================

    @Nested
    inner class FailureVariant {

        @Test
        fun `test Failure creation with message`() {
            // Given: A failure result
            val result = CreateReviewResult.Failure("File already exists")

            // Then: Should have correct message property
            assertEquals("File already exists", result.message, "Failure should have correct message")
        }

        @Test
        fun `test Failure with empty message`() {
            // Given: A failure result with empty message
            val result = CreateReviewResult.Failure("")

            // Then: Should allow empty message
            assertEquals("", result.message, "Should allow empty message")
        }

        @Test
        fun `test Failure with detailed error message`() {
            // Given: A failure result with detailed message
            val result = CreateReviewResult.Failure(
                "Failed to create review: Directory 'reviews' does not exist " +
                "and cannot be created (permission denied)"
            )

            // Then: Should preserve full message
            assertTrue(
                result.message.contains("Directory 'reviews'"),
                "Should preserve detailed error message"
            )
        }

        @Test
        fun `test Failure data class properties`() {
            // Given: Two failure results with same message
            val result1 = CreateReviewResult.Failure("Error")
            val result2 = CreateReviewResult.Failure("Error")

            // Then: Should be equal (data class equality)
            assertEquals(result1, result2, "Failure instances with same message should be equal")
        }

        @Test
        fun `test Failure copy creates independent instance`() {
            // Given: A failure result
            val original = CreateReviewResult.Failure("Original error")

            // When: Copying with different message
            val copy = original.copy(message = "New error")

            // Then: Should be independent
            assertEquals("Original error", original.message, "Original should be unchanged")
            assertEquals("New error", copy.message, "Copy should have new message")
        }

        @Test
        fun `test Failure component1 equals message`() {
            // Given: A failure result
            val result = CreateReviewResult.Failure("Error occurred")

            // When: Destructuring
            val (message) = result

            // Then: Component should equal message
            assertEquals(result.message, message, "Destructured message should match property")
        }
    }

    // ==================== Sealed Class Exhaustiveness Tests ====================

    @Nested
    inner class SealedClassExhaustiveness {

        @Test
        fun `test when expression covers all variants`() {
            // Given: Both variants
            val success = CreateReviewResult.Success("review")
            val failure = CreateReviewResult.Failure("Error")

            // When: Handling with when expression
            val successName = handleResult(success)
            val failureMessage = handleResult(failure)

            // Then: Should handle all variants correctly
            assertEquals("Success: review", successName, "Should handle Success variant")
            assertEquals("Failure: Error", failureMessage, "Should handle Failure variant")
        }

        @Test
        fun `test sealed class inheritance`() {
            // Given: Both variants
            val success: CreateReviewResult = CreateReviewResult.Success("review")
            val failure: CreateReviewResult = CreateReviewResult.Failure("Error")

            // Then: Both should be instances of sealed class
            assertTrue(success is CreateReviewResult, "Success should be CreateReviewResult")
            assertTrue(failure is CreateReviewResult, "Failure should be CreateReviewResult")
        }

        @Test
        fun `test is check for Success`() {
            // Given: A success result
            val result: CreateReviewResult = CreateReviewResult.Success("review")

            // When: Checking type
            val isSuccess = result is CreateReviewResult.Success

            // Then: Should be true
            assertTrue(isSuccess, "Success should be instance of Success variant")
        }

        @Test
        fun `test is check for Failure`() {
            // Given: A failure result
            val result: CreateReviewResult = CreateReviewResult.Failure("Error")

            // When: Checking type
            val isFailure = result is CreateReviewResult.Failure

            // Then: Should be true
            assertTrue(isFailure, "Failure should be instance of Failure variant")
        }

        @Test
        fun `test smart cast after is check`() {
            // Given: A result of unknown type
            val result: CreateReviewResult = CreateReviewResult.Success("review")

            // When: Checking type and accessing property
            val output = if (result is CreateReviewResult.Success) {
                // Smart cast allows direct access to name
                "Name: ${result.name}"
            } else {
                "Not a success"
            }

            // Then: Should work with smart cast
            assertEquals("Name: review", output, "Smart cast should allow property access")
        }

        @Test
        fun `test exhaustive when expression with else not needed`() {
            // Given: A result
            val result: CreateReviewResult = CreateReviewResult.Success("review")

            // When: Using exhaustive when (compiler enforces coverage)
            val output = when (result) {
                is CreateReviewResult.Success -> "Created: ${result.name}"
                is CreateReviewResult.Failure -> "Failed: ${result.message}"
                // No else needed - compiler knows all cases are covered
            }

            // Then: Should handle the case correctly
            assertEquals("Created: review", output, "When expression should handle all variants")
        }
    }

    // ==================== Edge Cases Tests ====================

    @Nested
    inner class EdgeCases {

        @Test
        fun `test Success and Failure are not equal`() {
            // Given: Success and Failure with same string content
            val success = CreateReviewResult.Success("error")
            val failure = CreateReviewResult.Failure("error")

            // Then: Should not be equal (different types)
            // Note: data class equality checks type first
            assertTrue(success != failure, "Success should not equal Failure with same string")
        }

        @Test
        fun `test toString for Success`() {
            // Given: A success result
            val result = CreateReviewResult.Success("review-1")

            // When: Converting to string
            val string = result.toString()

            // Then: Should contain relevant information
            assertTrue(string.contains("Success"), "toString should contain Success")
            assertTrue(string.contains("review-1"), "toString should contain name")
        }

        @Test
        fun `test toString for Failure`() {
            // Given: A failure result
            val result = CreateReviewResult.Failure("File not found")

            // When: Converting to string
            val string = result.toString()

            // Then: Should contain relevant information
            assertTrue(string.contains("Failure"), "toString should contain Failure")
            assertTrue(string.contains("File not found"), "toString should contain message")
        }

        @Test
        fun `test hashCode consistency for Success`() {
            // Given: Two equal success results
            val result1 = CreateReviewResult.Success("review")
            val result2 = CreateReviewResult.Success("review")

            // Then: Hash codes should be equal
            assertEquals(result1.hashCode(), result2.hashCode(), "Equal Success instances should have same hashCode")
        }

        @Test
        fun `test hashCode consistency for Failure`() {
            // Given: Two equal failure results
            val result1 = CreateReviewResult.Failure("Error")
            val result2 = CreateReviewResult.Failure("Error")

            // Then: Hash codes should be equal
            assertEquals(result1.hashCode(), result2.hashCode(), "Equal Failure instances should have same hashCode")
        }

        @Test
        fun `test Success with unicode name`() {
            // Given: A success result with unicode characters
            val result = CreateReviewResult.Success("review-日本語-🎉")

            // Then: Should preserve unicode
            assertEquals("review-日本語-🎉", result.name, "Should handle unicode in name")
        }

        @Test
        fun `test Failure with unicode message`() {
            // Given: A failure result with unicode
            val result = CreateReviewResult.Failure("Error: 文件名无效 🚫")

            // Then: Should preserve unicode
            assertEquals("Error: 文件名无效 🚫", result.message, "Should handle unicode in message")
        }

        @Test
        fun `test Success with very long name`() {
            // Given: A success result with long name
            val longName = "a".repeat(1000)
            val result = CreateReviewResult.Success(longName)

            // Then: Should handle long names
            assertEquals(1000, result.name.length, "Should handle long names")
        }

        @Test
        fun `test Failure with multiline message`() {
            // Given: A failure result with multiline message
            val multilineMessage = """
                Multiple errors occurred:
                - Directory not found
                - Permission denied
                - Disk full
            """.trimIndent()
            val result = CreateReviewResult.Failure(multilineMessage)

            // Then: Should preserve newlines
            assertTrue(result.message.contains("\n"), "Should preserve newlines in message")
            assertTrue(result.message.contains("Directory not found"), "Should preserve all lines")
        }
    }

    // ==================== Helper Functions ====================

    /**
     * Helper function to demonstrate exhaustive when handling.
     * Returns a formatted string for either variant.
     */
    private fun handleResult(result: CreateReviewResult): String {
        return when (result) {
            is CreateReviewResult.Success -> "Success: ${result.name}"
            is CreateReviewResult.Failure -> "Failure: ${result.message}"
        }
    }
}