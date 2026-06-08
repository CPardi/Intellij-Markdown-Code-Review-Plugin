package com.github.cpardi.markdowncodereview.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.messages.Topic

/**
 * Listener interface for settings changes.
 */
interface SettingsChangeListener {
    fun onSettingsChanged()
}

/**
 * Application-level settings for the Review Markdown Generator plugin.
 * Stores the reviews output directory name.
 */
@Service(Service.Level.APP)
@State(
    name = "ReviewSettings",
    storages = [Storage("markdown-code-review.xml")]
)
class ReviewSettings : SimplePersistentStateComponent<ReviewSettings.State>(State()) {

    /**
     * The directory name where review Markdown files are stored.
     */
    var reviewsDir: String
        get() = state.reviewsDir ?: "reviews"
        set(value) {
            val oldValue = state.reviewsDir
            state.reviewsDir = value
            if (oldValue != value) {
                notifySettingsChanged()
            }
        }

    /**
     * State class for persistent storage.
     */
    class State : BaseState() {
        var reviewsDir: String? by string("reviews")
    }

    private fun notifySettingsChanged() {
        ApplicationManager.getApplication().messageBus.syncPublisher(SETTINGS_CHANGED_TOPIC).onSettingsChanged()
    }

    companion object {
        /**
         * Message bus topic for settings changes.
         */
        val SETTINGS_CHANGED_TOPIC: Topic<SettingsChangeListener> = Topic.create(
            "Review Settings Changed",
            SettingsChangeListener::class.java
        )

        /**
         * Gets the singleton instance of the settings.
         */
        fun getInstance(): ReviewSettings =
            ApplicationManager.getApplication().getService(ReviewSettings::class.java)
    }
}
