package com.github.alttrex.hackdelftchallenge.toolWindow

import com.github.alttrex.hackdelftchallenge.state.Cosmetic
import com.github.alttrex.hackdelftchallenge.state.EvolutionStage
import com.github.alttrex.hackdelftchallenge.state.PetMood
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.JComponent

/**
 * A small thumbnail that previews a shop [Cosmetic] by rendering the pet wearing the
 * hat (or sitting on the background) with [PetRenderer], drawn once offscreen at a
 * comfortable size and scaled down so the existing pixel-art reuses cleanly.
 */
class CosmeticIcon(cosmetic: Cosmetic) : JComponent() {

    private val image: BufferedImage

    init {
        preferredSize = Dimension(JBUI.scale(48), JBUI.scale(44))
        minimumSize = preferredSize
        isOpaque = false

        val src = 120
        val renderer = PetRenderer().apply {
            stage = EvolutionStage.ADULT
            mood = PetMood.NEUTRAL
            if (cosmetic.type == "hat") equippedHat = cosmetic.id else equippedBackground = cosmetic.id
            setBounds(0, 0, src, src)
        }
        image = BufferedImage(src, src, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            renderer.paint(g)
        } finally {
            g.dispose()
            renderer.dispose() // stop the animation timer; the thumbnail is static
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val size = minOf(width, height)
            val x = (width - size) / 2
            val y = (height - size) / 2
            g2.drawImage(image, x, y, size, size, null)
        } finally {
            g2.dispose()
        }
    }
}
