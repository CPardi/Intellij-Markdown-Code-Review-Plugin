package com.github.cpardi.intellijmarkdownreviewplugin.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.github.cpardi.intellijmarkdownreviewplugin.ReviewBundle

/**
 * Represents the result of creating a new review.
 */
sealed class CreateReviewResult {
    /**
     * Review was created successfully.
     * @param name The name of the created review
     */
    data class Success(val name: String) : CreateReviewResult()
    
    /**
     * Review creation failed.
     * @param message The error message describing the failure
     */
    data class Failure(val message: String) : CreateReviewResult()
    
    /**
     * Returns the review name if successful, or shows an error dialog and returns null on failure.
     * 
     * @param project The project context for showing the error dialog
     * @param title The dialog title, defaults to addComment message
     * @return The review name on success, null on failure
     */
    fun getOrShowError(project: Project, title: String = ReviewBundle.message("addComment")): String? {
        return when (this) {
            is Success -> name
            is Failure -> {
                Messages.showErrorDialog(project, message, title)
                null
            }
        }
    }
}