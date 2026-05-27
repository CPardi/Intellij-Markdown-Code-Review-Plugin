package com.github.cpardi.intellijmarkdownreviewplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import java.io.IOException

/**
 * Base class for IntelliJ Platform integration tests.
 * Extends BasePlatformTestCase which is the recommended base class for
 * IntelliJ Platform 2025.x (newer replacement for LightPlatformCodeInsightFixtureTestCase).
 *
 * Uses JUnit 5 lifecycle annotations (@BeforeEach, @AfterEach) to bridge with
 * BasePlatformTestCase's JUnit 3-style setUp()/tearDown() methods.
 *
 * Provides test fixture setup/teardown and helper methods for working with
 * VirtualFiles and the IntelliJ Platform SDK.
 *
 * Use this base class for tests that require:
 * - A real Project instance
 * - VirtualFile creation and manipulation
 * - Document and PSI access
 * - IntelliJ Platform services
 *
 * For pure Kotlin unit tests (parser, writer, models), use [UnitTest] instead.
 */
@Suppress("JUnitMixedFramework")
abstract class LightPlatformTest : BasePlatformTestCase() {

    /**
     * JUnit 5 lifecycle bridge - calls BasePlatformTestCase's setUp().
     * This bridges JUnit 5's @BeforeEach with JUnit 3-style setup.
     */
    @BeforeEach
    fun setUpJunit5() {
        setUp()
    }

    /**
     * JUnit 5 lifecycle bridge - calls BasePlatformTestCase's tearDown().
     * This bridges JUnit 5's @AfterEach with JUnit 3-style teardown.
     */
    @AfterEach
    fun tearDownJunit5() {
        tearDown()
    }

    /**
     * Creates a virtual file in the test project with the specified content.
     *
     * @param relativePath The path relative to the project root
     * @param content The file content
     * @return The created VirtualFile
     * @throws IOException if file creation fails
     */
    @Throws(IOException::class)
    protected fun createVirtualFile(relativePath: String, content: String): VirtualFile {
        val file = myFixture.addFileToProject(relativePath, content)
        return file.virtualFile
    }

    /**
     * Creates a directory structure in the test project.
     *
     * @param relativePath The directory path relative to the project root
     * @return The created VirtualFile representing the directory
     * @throws IOException if directory creation fails
     */
    @Throws(IOException::class)
    protected fun createDirectory(relativePath: String): VirtualFile {
        val basePath = project.basePath ?: throw IllegalStateException("Project has no base directory")
        val projectDir = VirtualFileManager.getInstance().findFileByNioPath(java.nio.file.Path.of(basePath))
            ?: throw IllegalStateException("Project directory not found: $basePath")

        return runWriteAction<VirtualFile> {
            VfsUtil.createDirectories(projectDir.path + "/" + relativePath)
        }
    }

    /**
     * Creates a temporary file in the test project's temporary directory.
     * The file will be automatically cleaned up after the test.
     *
     * @param fileName The name of the file
     * @param content The file content
     * @return The created VirtualFile
     */
    protected fun createTempFile(fileName: String, content: String): VirtualFile {
        return createVirtualFile("temp/$fileName", content)
    }

    // ==================== Write Action Helpers ====================

    /**
     * Executes a write action and returns its result.
     * Use this when modifying files or the IntelliJ Platform model.
     *
     * @param action The action to execute
     * @return The result of the action
     */
    protected fun <T> runWriteAction(action: () -> T): T {
        return WriteCommandAction.writeCommandAction(project).compute<T, Exception> {
            action()
        }
    }

    /**
     * Executes a write action without returning a result.
     * Use this when modifying files or the IntelliJ Platform model.
     *
     * @param action The action to execute
     */
    protected fun runWriteAction(action: () -> Unit) {
        WriteCommandAction.writeCommandAction(project).run<Exception> {
            action()
        }
    }

    // ==================== Assertion Helpers ====================

    /**
     * Asserts that a file exists in the project at the specified path.
     *
     * @param relativePath The path relative to the project root
     */
    protected fun assertFileExists(relativePath: String) {
        val projectDir = project.guessProjectDir()
        assertNotNull(projectDir, "Project directory not found: $projectDir")

        val file = VfsUtil.findRelativeFile(projectDir!!, *relativePath.split("/").toTypedArray())
        assertNotNull(file, "File should exist at path: $relativePath")
    }

    /**
     * Asserts that a file does not exist in the project at the specified path.
     *
     * @param relativePath The path relative to the project root
     */
    protected fun assertFileNotExists(relativePath: String) {
        val basePath = project.basePath
        assertNotNull(basePath, "Project has no base directory")
        val projectDir = VirtualFileManager.getInstance().findFileByNioPath(java.nio.file.Path.of(basePath!!))
        assertNotNull(projectDir, "Project directory not found: $basePath")

        val file = VfsUtil.findRelativeFile(projectDir!!, *relativePath.split("/").toTypedArray())
        assertNull(file, "File should not exist at path: $relativePath")
    }

    /**
     * Asserts that a file has the expected content.
     *
     * @param relativePath The path relative to the project root
     * @param expectedContent The expected file content
     */
    protected fun assertFileContent(relativePath: String, expectedContent: String) {
        val basePath = project.basePath
        assertNotNull(basePath, "Project has no base directory")
        val projectDir = VirtualFileManager.getInstance().findFileByNioPath(java.nio.file.Path.of(basePath!!))
        assertNotNull(projectDir, "Project directory not found: $basePath")

        val file = VfsUtil.findRelativeFile(projectDir!!, *relativePath.split("/").toTypedArray())
        assertNotNull(file, "File should exist at path: $relativePath")

        val actualContent = VfsUtil.loadText(file!!)
        assertEquals(expectedContent, actualContent, "File content mismatch")
    }

    // ==================== Utility Methods ====================

    /**
     * Gets the base directory of the test project.
     */
    protected val projectBaseDir: VirtualFile
        get() {
            val basePath = project.basePath ?: throw IllegalStateException("Project has no base directory")
            return VirtualFileManager.getInstance().findFileByNioPath(java.nio.file.Path.of(basePath))
                ?: throw IllegalStateException("Project directory not found: $basePath")
        }

    /**
     * Refreshes the virtual file system to pick up external changes.
     */
    protected fun refreshVFS() {
        ApplicationManager.getApplication().runReadAction {
            projectBaseDir.refresh(false, true)
        }
    }
}
