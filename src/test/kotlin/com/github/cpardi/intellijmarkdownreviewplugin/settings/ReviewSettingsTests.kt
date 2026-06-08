package com.github.cpardi.intellijmarkdownreviewplugin.settings

import com.github.cpardi.intellijmarkdownreviewplugin.LightPlatformTest
import com.github.cpardi.intellijmarkdownreviewplugin.services.ServiceTestHelper
import com.intellij.openapi.application.ApplicationManager
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("JUnitMixedFramework")
class ReviewSettingsTestSuite {

    /**
     * Tests default values, setter/getter behavior, change notifications, and state management.
     */
    abstract class ReviewSettingsTests : LightPlatformTest() {

        protected lateinit var settings: ReviewSettings

        override fun setUp() {
            super.setUp()
            settings = ReviewSettings.getInstance()
            // Reset to default before each test
            settings.reviewsDir = "reviews"
        }
    }

    @Nested
    inner class DefaultValues : ReviewSettingsTests() {

        @Test
        fun `test reviewsDir default is reviews`() {
            // Given: Fresh settings reset to default
            settings.reviewsDir = "reviews"

            // When: Getting reviewsDir
            val dir = settings.reviewsDir

            // Then: Should return "reviews"
            assertEquals("reviews", dir)
        }

        @Test
        fun `test state initialization creates valid State object`() {
            // Given: Settings instance
            // When: Accessing state
            val state = settings.state

            // Then: State should not be null
            assertNotNull(state)
        }

        @Test
        fun `test state has reviewsDir property`() {
            // Given: Settings with default
            settings.reviewsDir = "reviews"

            // When: Accessing state property
            val stateDir = settings.state.reviewsDir

            // Then: Should match the set value
            assertEquals("reviews", stateDir)
        }
    }

    @Nested
    inner class SetterGetter : ReviewSettingsTests() {

        @Test
        fun `test setter updates state value`() {
            // Given: Settings with default
            // When: Setting a new value
            settings.reviewsDir = "custom-reviews"

            // Then: State should reflect the change
            assertEquals("custom-reviews", settings.state.reviewsDir)
        }

        @Test
        fun `test getter retrieves updated value`() {
            // Given: Settings with a custom value
            settings.reviewsDir = "my-reviews"

            // When: Getting the value
            val dir = settings.reviewsDir

            // Then: Should return the updated value
            assertEquals("my-reviews", dir)
        }

        @Test
        fun `test empty string reverts to default`() {
            // Given: Settings
            // When: Setting empty string
            settings.reviewsDir = ""

            // Then: Should accept empty string
            assertEquals("reviews", settings.reviewsDir)
        }

        @Test
        fun `test special characters are allowed in directory name`() {
            // Given: Settings
            // When: Setting special characters
            settings.reviewsDir = "my-reviews_2024"

            // Then: Should preserve special characters
            assertEquals("my-reviews_2024", settings.reviewsDir)
        }

        @Test
        fun `test can set back to default`() {
            // Given: Settings with custom value
            settings.reviewsDir = "custom"

            // When: Setting back to default
            settings.reviewsDir = "reviews"

            // Then: Should be the default
            assertEquals("reviews", settings.reviewsDir)
        }
    }

    @Nested
    inner class SettingsChangeNotification : ReviewSettingsTests() {

        @Test
        fun `test listener receives notification when value changes`() {
            // Given: A subscribed listener
            val (listener, wasNotified) = ServiceTestHelper.createSettingsChangeListener()
            val connection = ApplicationManager.getApplication().messageBus.connect()
            connection.subscribe(ReviewSettings.SETTINGS_CHANGED_TOPIC, listener)
            wasNotified.set(false)

            // When: Changing the value
            settings.reviewsDir = "new-dir"

            // Then: Listener should be notified
            assertTrue(wasNotified.get(), "Listener should be notified of change")
            connection.disconnect()
        }

        @Test
        fun `test listener does not receive notification for same value`() {
            // Given: Settings at "reviews" default
            settings.reviewsDir = "reviews"
            val (listener, wasNotified) = ServiceTestHelper.createSettingsChangeListener()
            val connection = ApplicationManager.getApplication().messageBus.connect()
            connection.subscribe(ReviewSettings.SETTINGS_CHANGED_TOPIC, listener)
            wasNotified.set(false)

            // When: Setting the same value
            settings.reviewsDir = "reviews"

            // Then: Listener should NOT be notified
            assertFalse(wasNotified.get(), "Listener should not be notified for same value")
            connection.disconnect()
        }

        @Test
        fun `test notification fires for reviews to custom`() {
            // Given: Settings at default
            settings.reviewsDir = "reviews"
            val (listener, wasNotified) = ServiceTestHelper.createSettingsChangeListener()
            val connection = ApplicationManager.getApplication().messageBus.connect()
            connection.subscribe(ReviewSettings.SETTINGS_CHANGED_TOPIC, listener)
            wasNotified.set(false)

            // When: Changing to custom directory
            settings.reviewsDir = "custom-dir"

            // Then: Notification should fire
            assertTrue(wasNotified.get())
            connection.disconnect()
        }

        @Test
        fun `test notification fires for custom to custom2`() {
            // Given: Settings at custom value
            settings.reviewsDir = "custom1"
            val (listener, wasNotified) = ServiceTestHelper.createSettingsChangeListener()
            val connection = ApplicationManager.getApplication().messageBus.connect()
            connection.subscribe(ReviewSettings.SETTINGS_CHANGED_TOPIC, listener)
            wasNotified.set(false)

            // When: Changing to different custom value
            settings.reviewsDir = "custom2"

            // Then: Notification should fire
            assertTrue(wasNotified.get())
            connection.disconnect()
        }

        @Test
        fun `test SETTINGS_CHANGED_TOPIC has correct display name`() {
            // Given: The topic constant
            val topic = ReviewSettings.SETTINGS_CHANGED_TOPIC

            // Then: Should have meaningful display name
            assertEquals("Review Settings Changed", topic.displayName)
        }
    }

    @Nested
    inner class StateManagement : ReviewSettingsTests() {

        @Test
        fun `test State default reviewsDir is reviews`() {
            // Given: A fresh state
            val state = ReviewSettings.State()

            // Then: reviewsDir should default to "reviews"
            assertEquals("reviews", state.reviewsDir)
        }

        @Test
        fun `test State reviewsDir can be set`() {
            // Given: A state instance
            val state = ReviewSettings.State()

            // When: Setting reviewsDir
            state.reviewsDir = "custom"

            // Then: Should reflect the change
            assertEquals("custom", state.reviewsDir)
        }
    }
}
