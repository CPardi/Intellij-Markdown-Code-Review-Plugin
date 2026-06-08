package com.github.cpardi.markdowncodereview.util

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.LayeredIcon
import com.intellij.util.ui.ImageUtil
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.ImageIcon

/**
 * Factory for creating icons with count badges overlayed.
 * Used for displaying multiple comments on the same line with a count indicator.
 */
object BadgeIconFactory {

    private const val BADGE_SIZE = 9.0f
    private const val MAX_DISPLAY_COUNT = 9
    private const val BASE_ICON_SIZE = 12

    // Cache for badge icons by count (2-9, overflow)
    private val iconCache = mutableMapOf<Int, Icon>()

    // Theme-aware colors for badge background
    private val BADGE_BACKGROUND = JBColor(Color(0x4D4D4D), Color(0x6C707E))

    // Theme-aware colors for badge text
    private val BADGE_TEXT = JBColor(Color.WHITE, Color(0x2B2D30))

    /**
     * Gets or creates a badge icon for the given comment count.
     * Uses caching to avoid recreating icons for common counts.
     *
     * @param count The number of comments (must be > 1)
     * @return An icon with a badge showing the count
     */
    fun getBadgeIcon(count: Int): Icon {
        require(count > 1) { "Badge icon is only for counts > 1" }

        val displayCount = minOf(count, MAX_DISPLAY_COUNT)
        return iconCache.getOrPut(displayCount) {
            createBadgeIcon(displayCount)
        }
    }

    private fun createBadgeIcon(count: Int): Icon {
        val baseIcon = AllIcons.General.Balloon
        val iconSize = baseIcon.iconWidth

        // Create a combined image with the badge in the top-right corner
        // We draw at 1:1 scale since ImageUtil.createImage handles HiDPI internally
        val image = ImageUtil.createImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        try {
            // Enable anti-aliasing for smooth edges
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            // Draw the base icon
            baseIcon.paintIcon(null, g2d, 0, 0)

            // Draw badge background in top-right corner
            g2d.color = BADGE_BACKGROUND
            val badgeX = iconSize - BADGE_SIZE
            val badgeY = 0.0f
            val circle = Ellipse2D.Float(badgeX, badgeY, BADGE_SIZE, BADGE_SIZE)
            g2d.fill(circle)

            // Draw count text centered in the badge
            g2d.color = BADGE_TEXT
            g2d.font = Font("SansSerif", Font.BOLD, 7)

            val text = count.toString()
            val metrics = g2d.fontMetrics
            val textWidth = metrics.stringWidth(text)
            val textHeight = metrics.ascent

            // Center text in the badge circle
            val textX = badgeX + (BADGE_SIZE - textWidth) / 2.1f
            val textY = badgeY + (BADGE_SIZE - textHeight) / 2.2f + metrics.ascent

            g2d.drawString(text, textX, textY)
        } finally {
            g2d.dispose()
        }

        // Create an icon from the image
        val badgeImageIcon = ImageIcon(image)
        // Use LayeredIcon.create to compose properly for HiDPI
        return LayeredIcon.create(baseIcon, badgeImageIcon)
    }

    /**
     * Clears the icon cache. Useful for testing or theme changes.
     */
    fun clearCache() {
        iconCache.clear()
    }
}
