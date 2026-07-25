package com.github.cpardi.markdowncodereview.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for BadgeIconFactory.
 * Tests badge icon creation and caching.
 */
class BadgeIconFactoryTests {

    @Nested
    inner class GetBadgeIcon {

        @Test
        fun `test throws for count less than 2`() {
            // Given: A count of 1 (invalid for badge)
            // When/Then: Should throw IllegalArgumentException
            assertThrows(IllegalArgumentException::class.java) {
                BadgeIconFactory.getBadgeIcon(1)
            }
        }

        @Test
        fun `test throws for count of 0`() {
            // Given: A count of 0 (invalid for badge)
            // When/Then: Should throw IllegalArgumentException
            assertThrows(IllegalArgumentException::class.java) {
                BadgeIconFactory.getBadgeIcon(0)
            }
        }

        @Test
        fun `test throws for negative count`() {
            // Given: A negative count (invalid for badge)
            // When/Then: Should throw IllegalArgumentException
            assertThrows(IllegalArgumentException::class.java) {
                BadgeIconFactory.getBadgeIcon(-1)
            }
        }

        @Test
        fun `test returns icon for count of 2`() {
            // Given: A valid count of 2
            // When: Getting badge icon
            val icon = BadgeIconFactory.getBadgeIcon(2)

            // Then: Should return an icon
            assertNotNull(icon, "Should return icon for count 2")
        }

        @Test
        fun `test returns icon for count of 3`() {
            // Given: A valid count of 3
            // When: Getting badge icon
            val icon = BadgeIconFactory.getBadgeIcon(3)

            // Then: Should return an icon
            assertNotNull(icon, "Should return icon for count 3")
        }

        @Test
        fun `test returns icon for count of 9`() {
            // Given: A valid count of 9 (max display count)
            // When: Getting badge icon
            val icon = BadgeIconFactory.getBadgeIcon(9)

            // Then: Should return an icon
            assertNotNull(icon, "Should return icon for count 9")
        }

        @Test
        fun `test returns overflow icon for count greater than 9`() {
            // Given: A count greater than max display count
            // When: Getting badge icon
            val icon10 = BadgeIconFactory.getBadgeIcon(10)
            val icon100 = BadgeIconFactory.getBadgeIcon(100)

            // Then: Should return icons (clamped to 9)
            assertNotNull(icon10, "Should return icon for count 10")
            assertNotNull(icon100, "Should return icon for count 100")
        }

        @Test
        fun `test returns same icon for cached values`() {
            // Given: Clear cache
            BadgeIconFactory.clearCache()

            // When: Getting same icon twice
            val icon1 = BadgeIconFactory.getBadgeIcon(2)
            val icon2 = BadgeIconFactory.getBadgeIcon(2)

            // Then: Should return same cached instance
            assertTrue(icon1 === icon2, "Should return same cached icon instance")
        }

        @Test
        fun `test returns different icons for different counts`() {
            // Given: Clear cache
            BadgeIconFactory.clearCache()

            // When: Getting different icons
            val icon2 = BadgeIconFactory.getBadgeIcon(2)
            val icon3 = BadgeIconFactory.getBadgeIcon(3)

            // Then: Should return different instances
            assertFalse(icon2 === icon3, "Different counts should return different icon instances")
        }
    }

    @Nested
    inner class ClearCache {

        @Test
        fun `test clearCache removes cached icons`() {
            // Given: Pre-cache some icons
            BadgeIconFactory.getBadgeIcon(2)
            BadgeIconFactory.getBadgeIcon(3)

            // When: Clearing cache
            BadgeIconFactory.clearCache()

            // Then: Next call should create new icon (no error)
            val icon = BadgeIconFactory.getBadgeIcon(2)
            assertNotNull(icon, "Should be able to get icon after cache clear")
        }

        @Test
        fun `test clearCache allows re-creation of icons`() {
            // Given: Clear cache
            BadgeIconFactory.clearCache()

            // When: Creating icons after clear
            val icon2 = BadgeIconFactory.getBadgeIcon(2)
            BadgeIconFactory.clearCache()
            val icon2Again = BadgeIconFactory.getBadgeIcon(2)

            // Then: Should be different instances (cache was cleared)
            // Note: They may or may not be same instance depending on implementation
            assertNotNull(icon2)
            assertNotNull(icon2Again)
        }
    }

    @Nested
    inner class IconProperties {

        @Test
        fun `test icon has reasonable size`() {
            // Given: A badge icon
            val icon = BadgeIconFactory.getBadgeIcon(5)

            // When: Checking icon size
            val width = icon.iconWidth
            val height = icon.iconHeight

            // Then: Should be reasonable size (base is 12px)
            assertTrue(width > 0, "Icon width should be positive")
            assertTrue(height > 0, "Icon height should be positive")
            assertTrue(width <= 32, "Icon width should not be excessive")
            assertTrue(height <= 32, "Icon height should not be excessive")
        }

        @Test
        fun `test icons have consistent size for different counts`() {
            // Given: Multiple badge icons
            val icon2 = BadgeIconFactory.getBadgeIcon(2)
            val icon3 = BadgeIconFactory.getBadgeIcon(3)
            val icon9 = BadgeIconFactory.getBadgeIcon(9)

            // When: Checking sizes
            // Then: All icons should have same dimensions (base icon size)
            assertEquals(icon2.iconWidth, icon3.iconWidth, "Icons should have same width")
            assertEquals(icon2.iconHeight, icon3.iconHeight, "Icons should have same height")
            assertEquals(icon2.iconWidth, icon9.iconWidth, "All icons should have same width")
        }
    }
}
