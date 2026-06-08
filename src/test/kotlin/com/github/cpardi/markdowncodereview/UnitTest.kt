package com.github.cpardi.markdowncodereview

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Base class for pure Kotlin unit tests that do not require IntelliJ Platform dependencies.
 * Use this for testing:
 * - Parser logic (ReviewFileParser)
 * - Writer logic (ReviewFileWriter)
 * - Data models (Comment, ReviewFile)
 * - Utility classes
 *
 * For tests that require IntelliJ Platform services or VirtualFiles, use [LightPlatformTest] instead.
 *
 * This class provides:
 * - Standard @BeforeEach and @AfterEach hooks
 * - Clear documentation on test isolation
 * - Base functionality for unit tests
 */
abstract class UnitTest {

    /**
     * Called before each test method.
     * Override this to set up test fixtures.
     */
    @BeforeEach
    open fun setUp() {
        // Default implementation does nothing
        // Sub-classes can override to provide setup logic
    }

    /**
     * Called after each test method.
     * Override this to clean up test fixtures.
     */
    @AfterEach
    open fun tearDown() {
        // Default implementation does nothing
        // Sub-classes can override to provide cleanup logic
    }
}
