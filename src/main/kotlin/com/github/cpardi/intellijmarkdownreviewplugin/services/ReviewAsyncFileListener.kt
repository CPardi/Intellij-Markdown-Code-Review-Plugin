package com.github.cpardi.intellijmarkdownreviewplugin.services

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.AsyncFileListener.ChangeApplier
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent

/**
 * Async file listener that tracks file renames and moves to update comment paths.
 *
 * Uses [AsyncFileListener] instead of the deprecated [com.intellij.openapi.vfs.VirtualFileListener].
 * [prepareChange] performs pure computation on a background thread, and [ChangeApplier.afterVfsChange]
 * applies the updates under write lock.
 */
class ReviewAsyncFileListener(private val project: Project) : AsyncFileListener {

    private val LOG = thisLogger()

    override fun prepareChange(events: List<VFileEvent>): ChangeApplier? {
        val baseDir = project.guessProjectDir() ?: return null
        val renames = mutableMapOf<String, String>()

        for (event in events) {
            when (event) {
                is VFilePropertyChangeEvent -> {
                    if (event.propertyName == VirtualFile.PROP_NAME) {
                        collectRenamePaths(event, baseDir, renames)
                    }
                }
                is VFileMoveEvent -> {
                    collectMovePaths(event, baseDir, renames)
                }
            }
        }

        if (renames.isEmpty()) return null

        return object : ChangeApplier {
            override fun afterVfsChange() {
                val service = project.getService(ReviewService::class.java)
                service.applyCommentRenames(renames)
                LOG.info("Applied ${renames.size} path rename(s)")
            }
        }
    }

    private fun collectRenamePaths(
        event: VFilePropertyChangeEvent,
        baseDir: VirtualFile,
        renames: MutableMap<String, String>
    ) {
        val file = event.file
        val oldName = event.oldValue as? String ?: return
        val newName = event.newValue as? String ?: return
        val parent = file.parent ?: return

        val parentPath = VfsUtil.getRelativePath(parent, baseDir) ?: return
        val oldRelativePath = if (parentPath.isEmpty()) oldName else "$parentPath/$oldName"
        val newRelativePath = if (parentPath.isEmpty()) newName else "$parentPath/$newName"

        updatePathsForFileOrDirectory(oldRelativePath, newRelativePath, file.isDirectory, renames)
    }

    private fun collectMovePaths(
        event: VFileMoveEvent,
        baseDir: VirtualFile,
        renames: MutableMap<String, String>
    ) {
        val file = event.file
        val oldParent = event.oldParent
        val newParent = event.newParent

        val oldParentPath = VfsUtil.getRelativePath(oldParent, baseDir) ?: return
        val newParentPath = VfsUtil.getRelativePath(newParent, baseDir) ?: return
        val fileName = file.name

        val oldRelativePath = if (oldParentPath.isEmpty()) fileName else "$oldParentPath/$fileName"
        val newRelativePath = if (newParentPath.isEmpty()) fileName else "$newParentPath/$fileName"

        updatePathsForFileOrDirectory(oldRelativePath, newRelativePath, file.isDirectory, renames)
    }

    private fun collectDirectoryRenamePaths(
        oldDirPath: String,
        newDirPath: String,
        renames: MutableMap<String, String>
    ) {
        val service = project.getService(ReviewService::class.java)
        val review = service.activeReview ?: return

        val prefix = if (oldDirPath.isEmpty()) "" else "$oldDirPath/"
        val newPrefix = if (newDirPath.isEmpty()) "" else "$newDirPath/"

        for (comment in review.comments) {
            if (comment.relativePath.startsWith(prefix)) {
                val suffix = comment.relativePath.removePrefix(prefix)
                val newPath = "$newPrefix$suffix"
                renames[comment.relativePath] = newPath
            }
        }
    }

    private fun updatePathsForFileOrDirectory(
        oldRelativePath: String,
        newRelativePath: String,
        isDirectory: Boolean,
        renames: MutableMap<String, String>
    ) {
        if (isDirectory) {
            collectDirectoryRenamePaths(oldRelativePath, newRelativePath, renames)
        } else {
            renames[oldRelativePath] = newRelativePath
        }
    }
}
