package com.github.cpardi.markdowncodereview.settings

import com.github.cpardi.markdowncodereview.ReviewBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Application-level settings configurable for the Review Markdown Generator plugin.
 * Provides UI for configuring the reviews output directory name.
 */
class ReviewSettingsConfigurable : Configurable {

    private var panel: DialogPanel? = null
    private val settings: ReviewSettings = ReviewSettings.getInstance()

    override fun getDisplayName(): String = ReviewBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        panel = panel {
            row(ReviewBundle.message("settings.reviewsDir")) {
                textField()
                    .align(AlignX.FILL)
                    .bindText(settings::reviewsDir)
                    .validationOnApply {
                        val text = it.text.trim()
                        val errorMsg = validateDirectoryName(text)
                        if (errorMsg != null) {
                            error(errorMsg)
                        } else {
                            null
                        }
                    }
            }.rowComment(ReviewBundle.message("settings.reviewsDir.description"))
        }
        return panel!!
    }

    override fun isModified(): Boolean {
        return panel?.isModified() ?: false
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        panel?.apply()
    }

    override fun reset() {
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }

    /**
     * Validates the directory name and returns an error message if invalid.
     * @param name the directory name to validate
     * @return error message if invalid, null if valid
     */
    private fun validateDirectoryName(name: String): String? {
        if (name.isEmpty()) {
            return "Directory name cannot be empty"
        }

        // Check for path separators
        if (name.contains("/") || name.contains("\\")) {
            return "Directory name cannot contain path separators (/ or \\)"
        }

        // Check for invalid characters (allow alphanumeric, hyphens, underscores)
        if (!name.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            return "Directory name can only contain letters, numbers, hyphens, and underscores"
        }

        return null
    }
}
