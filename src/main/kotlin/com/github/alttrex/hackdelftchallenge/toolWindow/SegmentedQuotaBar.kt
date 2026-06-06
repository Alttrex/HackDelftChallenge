package com.github.alttrex.hackdelftchallenge.toolWindow

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent

/**
 * A daily-quota progress bar split into colored segments showing how much of the
 * current progress came from production code, Javadoc, and test code.
 *
 * The three counts are expected to sum to the total lines written; they are drawn
 * stacked left-to-right against [max] (the daily quota).
 */
class SegmentedQuotaBar : JComponent() {

    var max: Int = 1
    var prodLines: Int = 0
    var javadocLines: Int = 0
    var testLines: Int = 0
    var text: String = ""

    init {
        preferredSize = Dimension(JBUI.scale(200), JBUI.scale(20))
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val w = width
            val h = height
            val arc = JBUI.scale(8)
            val safeMax = max.coerceAtLeast(1)

            // Track / background
            g2.color = TRACK_COLOR
            g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc)

            // Segments, capped so the total never overflows the track.
            val capacity = safeMax
            val prod = prodLines.coerceAtLeast(0)
            val doc = javadocLines.coerceAtLeast(0)
            val test = testLines.coerceAtLeast(0)

            var consumed = 0
            var x = 0
            for ((count, color) in listOf(prod to PROD_COLOR, doc to JAVADOC_COLOR, test to TEST_COLOR)) {
                if (count <= 0) continue
                val drawable = (count).coerceAtMost(capacity - consumed)
                if (drawable <= 0) break
                val segWidth = Math.round(drawable.toDouble() / capacity * (w - 1)).toInt()
                if (segWidth > 0) {
                    g2.color = color
                    g2.fillRoundRect(x, 0, segWidth, h - 1, arc, arc)
                }
                x += segWidth
                consumed += drawable
            }

            // Border
            g2.color = TRACK_BORDER
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc)

            // Centered text
            if (text.isNotEmpty()) {
                g2.color = JBColor.foreground()
                val fm = g2.fontMetrics
                val tx = (w - fm.stringWidth(text)) / 2
                val ty = (h - fm.height) / 2 + fm.ascent
                g2.drawString(text, tx, ty)
            }
        } finally {
            g2.dispose()
        }
    }

    companion object {
        val PROD_COLOR = JBColor(0x4CAF50, 0x66BB6A)      // green
        val JAVADOC_COLOR = JBColor(0x2196F3, 0x42A5F5)   // blue
        val TEST_COLOR = JBColor(0x9C27B0, 0xBA68C8)      // purple
        private val TRACK_COLOR = JBColor(0xE0E0E0, 0x3C3F41)
        private val TRACK_BORDER = JBColor(0xBDBDBD, 0x555555)
    }
}
