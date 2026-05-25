package com.github.cpardi.intellijmarkdownreviewplugin.ui

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import java.awt.*
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent

/**
 * A panel with rounded corners and a themed background color.
 * Used for displaying comment items as visually distinct "bubbles".
 */
class CommentBubblePanel : JBPanel<CommentBubblePanel> {

    companion object {
        // Corner radius for rounded edges (15px)
        private const val ARC_WIDTH = 15f
        private const val ARC_HEIGHT = 15f

        // Theme-aware background colors
        // Light theme: slightly darker than panel background for subtle contrast
        // Dark theme: lighter than panel background (similar to editor background)
        private val BUBBLE_BACKGROUND = JBColor(
            Color(0xE8E8E8), // Light theme: light gray
            Color(0x3C3F41)  // Dark theme: lifted gray (lighter than Darcula's default)
        )

        // Theme-aware border color for unfocused text editors
        // More visible than tool window border color
        private val UNFOCUSED_BORDER_COLOR = JBColor(
            Color(0xB0B0B0), // Light theme: medium gray
            Color(0x555555)  // Dark theme: lifted gray
        )

        /**
         * Returns the bubble background color for components that need it
         * (e.g., EditorTextField which requires an explicit background).
         */
        fun getBubbleBackground(): JBColor = BUBBLE_BACKGROUND

        /**
         * Returns the unfocused border color for text editors within the bubble.
         */
        fun getUnfocusedBorderColor(): JBColor = UNFOCUSED_BORDER_COLOR
    }

    constructor() : super(BorderLayout())

    init {
        isOpaque = false
    }

    override fun getMaximumSize(): java.awt.Dimension {
        val preferred = preferredSize
        return java.awt.Dimension(Integer.MAX_VALUE, preferred.height)
    }

    override fun addImpl(comp: java.awt.Component?, constraints: Any?, index: Int) {
        // Make JPanel children transparent so bubble background shows through
        if (comp is JComponent) {
            comp.isOpaque = false
        }

        super.addImpl(comp, constraints, index)
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g.create() as Graphics2D
        try {
            // Enable antialiasing for smooth rounded corners
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            // Draw rounded rectangle background
            g2d.color = BUBBLE_BACKGROUND
            g2d.fill(RoundRectangle2D.Float(
                0f, 0f,
                width.toFloat(), height.toFloat(),
                ARC_WIDTH, ARC_HEIGHT
            ))
        } finally {
            g2d.dispose()
        }
        super.paintComponent(g)
    }
}