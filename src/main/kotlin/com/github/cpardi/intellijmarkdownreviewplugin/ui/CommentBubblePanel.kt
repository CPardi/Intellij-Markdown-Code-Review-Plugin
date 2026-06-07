package com.github.cpardi.intellijmarkdownreviewplugin.ui

import com.intellij.ide.UiActivity
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * A panel with rounded corners and a themed background color.
 * Used for displaying comment items as visually distinct "bubbles".
 */
class CommentBubblePanel : JBPanel<CommentBubblePanel>(BorderLayout()) {

    private val headerLabel = JBLabel()
    private var deleteButton = JButton("Delete")
    private val bodyField = JBTextArea()

    init {
        isOpaque = false

        headerLabel.apply {
            toolTipText = "Click to navigate"
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    onHeaderClick()
                }
            })
        }

        // Delete button
        deleteButton.apply {
            toolTipText = "Delete comment"
            addActionListener { onDelete }
            background = UNFOCUSED_BORDER_COLOR
            isOpaque = false
        }

        // Button panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            add(deleteButton)
            isOpaque = false
        }

        // Layout
        val topPanel = JPanel(BorderLayout()).apply {
            add(headerLabel, BorderLayout.WEST)
            add(buttonPanel, BorderLayout.EAST)
        }

        add(topPanel, BorderLayout.NORTH)

        // Comment body text field
        bodyField.apply {
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty(5)
        }

        // Wrap in panel with minimum height and border
        val fontMetrics = bodyField.getFontMetrics(bodyField.font)
        val minHeight = fontMetrics.height * 3
        val defaultBorderColor = CommentBubblePanel.getUnfocusedBorderColor()
        val bodyPanel = object : JBPanel<JBPanel<*>>(BorderLayout()) {
            override fun getPreferredSize(): Dimension {
                val size = super.getPreferredSize()
                return Dimension(size.width, maxOf(size.height, minHeight))
            }
        }.apply {
            border = JBUI.Borders.customLine(defaultBorderColor)
            add(bodyField, BorderLayout.CENTER)
        }

        // Change border on focus
        bodyField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                bodyPanel.border = JBUI.Borders.customLine(JBUI.CurrentTheme.Focus.focusColor())
            }

            override fun focusLost(e: FocusEvent?) {
                bodyPanel.border = JBUI.Borders.customLine(defaultBorderColor)
            }
        })


        // Save body on focus lost
        bodyField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                onBodyFocusLost(e)
            }
        })

        add(bodyPanel, BorderLayout.CENTER)
    }

    var onHeaderClick: () -> Unit = {}
    var onDelete: (commentId: Int) -> Unit = {}
    var onBodyFocusLost: (e: FocusEvent?) -> Unit = {}

    var headerText: String = ""
        get() { return headerLabel.text }
        set(value) {
            field = value
            headerLabel.text = value
        }

    var bodyText: String = ""
        get() { return bodyField.text }
        set(value) {
            field = value
            bodyField.text = value
        }

    override fun getMaximumSize(): Dimension {
        val preferred = preferredSize
        return Dimension(Integer.MAX_VALUE, preferred.height)
    }

    override fun addImpl(comp: Component?, constraints: Any?, index: Int) {
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
}
