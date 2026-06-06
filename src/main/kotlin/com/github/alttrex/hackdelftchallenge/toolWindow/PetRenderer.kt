package com.github.alttrex.hackdelftchallenge.toolWindow

import com.github.alttrex.hackdelftchallenge.state.EvolutionStage
import com.github.alttrex.hackdelftchallenge.state.PetMood
import java.awt.*
import javax.swing.JComponent
import javax.swing.Timer

/**
 * Custom component that draws the duck pet using Graphics2D.
 * Evolution stages: egg → duckling → duck → rubber ducky.
 * Supports different moods and idle animation.
 */
class PetRenderer : JComponent() {

    var stage: EvolutionStage = EvolutionStage.EGG
    var mood: PetMood = PetMood.NEUTRAL
    var equippedHat: String = ""
    var equippedBackground: String = ""

    /** Pulled live on every repaint so the bubble appears/disappears without polling lag. */
    var speechSupplier: () -> String = { "" }

    private var animFrame = 0
    private val animTimer = Timer(500) {
        animFrame = (animFrame + 1) % 4
        repaint()
    }

    init {
        preferredSize = Dimension(200, 200)
        minimumSize = Dimension(120, 120)
        isOpaque = false
        animTimer.start()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        drawBackground(g2)

        val cx = width / 2
        val cy = height / 2
        // Idle bobbing offset
        val bob = when (animFrame) {
            0 -> 0
            1 -> -3
            2 -> 0
            else -> 3
        }

        when (stage) {
            EvolutionStage.EGG -> drawEgg(g2, cx, cy + bob)
            EvolutionStage.BABY -> drawDuckling(g2, cx, cy + bob)
            EvolutionStage.TEEN -> drawDuck(g2, cx, cy + bob)
            EvolutionStage.ADULT -> drawRubberDucky(g2, cx, cy + bob)
        }

        drawHat(g2, cx, cy + bob)
        drawMoodEffect(g2, cx, cy + bob)
        drawSpeechBubble(g2, speechSupplier(), cx, cy + bob)

        g2.dispose()
    }

    // ── Egg ──────────────────────────────────────────────
    private fun drawEgg(g: Graphics2D, cx: Int, cy: Int) {
        val w = 60
        val h = 76
        // Shadow
        g.color = Color(0, 0, 0, 40)
        g.fillOval(cx - w / 2 + 4, cy + h / 2 - 6, w - 8, 12)
        // Shell
        g.color = Color(255, 248, 220) // cream
        g.fillOval(cx - w / 2, cy - h / 2, w, h)
        g.color = Color(210, 180, 140)
        g.stroke = BasicStroke(2.5f)
        g.drawOval(cx - w / 2, cy - h / 2, w, h)
        // Zigzag crack
        g.color = Color(180, 150, 100)
        g.stroke = BasicStroke(2f)
        val crackY = cy - 4
        val pts = intArrayOf(cx - 18, cx - 8, cx, cx + 10, cx + 18)
        val ptsY = intArrayOf(crackY, crackY - 7, crackY + 3, crackY - 5, crackY + 2)
        g.drawPolyline(pts, ptsY, 5)
        // Tiny beak peeking through the crack
        drawBeak(g, cx, cy - 8, 10, 6)
        // Eyes (peeking through crack)
        drawEyes(g, cx, cy - 16, 10, mood)
    }

    // ── Duckling ─────────────────────────────────────────
    private fun drawDuckling(g: Graphics2D, cx: Int, cy: Int) {
        val bodyColor = getMoodBodyColor()
        // Shadow
        g.color = Color(0, 0, 0, 40)
        g.fillOval(cx - 22, cy + 30, 44, 10)
        // Tiny tail tuft
        g.color = bodyColor
        val tx = intArrayOf(cx + 22, cx + 34, cx + 22)
        val ty = intArrayOf(cy + 8, cy + 2, cy + 22)
        g.fillPolygon(tx, ty, 3)
        // Body (round fluffy chick)
        g.fillOval(cx - 26, cy - 4, 52, 44)
        // Head
        g.fillOval(cx - 20, cy - 38, 40, 40)
        // Head tuft feather
        g.fillOval(cx - 3, cy - 46, 6, 9)
        // Outlines
        g.color = bodyColor.darker()
        g.stroke = BasicStroke(2f)
        g.drawOval(cx - 26, cy - 4, 52, 44)
        g.drawOval(cx - 20, cy - 38, 40, 40)
        // Wing
        g.color = lighten(bodyColor, 0.18f)
        g.fillOval(cx + 4, cy + 2, 20, 22)
        g.color = bodyColor.darker()
        g.drawOval(cx + 4, cy + 2, 20, 22)
        // Beak
        drawBeak(g, cx, cy - 14, 13, 8)
        // Eyes
        drawEyes(g, cx, cy - 24, 12, mood)
        // Webbed feet
        drawWebFoot(g, cx - 9, cy + 38)
        drawWebFoot(g, cx + 9, cy + 38)
    }

    // ── Duck ─────────────────────────────────────────────
    private fun drawDuck(g: Graphics2D, cx: Int, cy: Int) {
        val bodyColor = getMoodBodyColor()
        // Shadow
        g.color = Color(0, 0, 0, 40)
        g.fillOval(cx - 30, cy + 40, 60, 12)
        // Pointed tail
        g.color = bodyColor
        val tx = intArrayOf(cx + 24, cx + 44, cx + 24)
        val ty = intArrayOf(cy + 2, cy - 8, cy + 16)
        g.fillPolygon(tx, ty, 3)
        // Body (egg-shaped)
        g.fillOval(cx - 30, cy - 2, 60, 48)
        // Head
        g.fillOval(cx - 22, cy - 46, 44, 44)
        // Outlines
        g.color = bodyColor.darker()
        g.stroke = BasicStroke(2f)
        g.drawOval(cx - 30, cy - 2, 60, 48)
        g.drawOval(cx - 22, cy - 46, 44, 44)
        // Belly
        g.color = lighten(bodyColor, 0.3f)
        g.fillOval(cx - 4, cy + 8, 28, 30)
        // Wing
        g.color = lighten(bodyColor, 0.18f)
        g.fillOval(cx - 26, cy + 2, 28, 30)
        g.color = bodyColor.darker()
        g.drawOval(cx - 26, cy + 2, 28, 30)
        // Beak
        drawBeak(g, cx, cy - 20, 19, 11)
        // Eyes
        drawEyes(g, cx, cy - 30, 16, mood)
        // Webbed feet
        drawWebFoot(g, cx - 10, cy + 44)
        drawWebFoot(g, cx + 10, cy + 44)
    }

    // ── Rubber Ducky (final form) ────────────────────────
    private fun drawRubberDucky(g: Graphics2D, cx: Int, cy: Int) {
        val body = Color(255, 211, 61)       // classic rubber-duck yellow
        val bodyDark = Color(228, 165, 28)
        // Water ripple it floats on
        g.color = Color(120, 190, 235, 120)
        g.stroke = BasicStroke(2.5f)
        g.drawArc(cx - 46, cy + 40, 36, 14, 0, -180)
        g.drawArc(cx + 8, cy + 40, 36, 14, 0, -180)
        // Shadow / reflection
        g.color = Color(0, 0, 0, 35)
        g.fillOval(cx - 34, cy + 44, 68, 12)
        // Body
        g.color = body
        g.fillOval(cx - 32, cy + 2, 64, 46)
        // Tail flip
        val tx = intArrayOf(cx + 26, cx + 46, cx + 26)
        val ty = intArrayOf(cy + 4, cy - 10, cy + 22)
        g.fillPolygon(tx, ty, 3)
        // Big head
        g.fillOval(cx - 30, cy - 48, 60, 58)
        // Outlines
        g.color = bodyDark
        g.stroke = BasicStroke(2.5f)
        g.drawOval(cx - 32, cy + 2, 64, 46)
        g.drawOval(cx - 30, cy - 48, 60, 58)
        // Big flat beak
        g.color = Color(246, 150, 32)
        g.fillRoundRect(cx - 18, cy - 24, 36, 15, 9, 9)
        g.color = Color(205, 115, 18)
        g.stroke = BasicStroke(1.5f)
        g.drawLine(cx - 12, cy - 16, cx + 12, cy - 16)
        // Eyes
        drawEyes(g, cx, cy - 34, 16, mood)
        // Glossy rubber highlight on head
        g.color = Color(255, 255, 255, 150)
        g.fillOval(cx + 6, cy - 42, 12, 18)
        // Wing
        g.color = Color(245, 193, 38)
        g.fillOval(cx - 28, cy + 8, 24, 28)
        g.color = bodyDark
        g.stroke = BasicStroke(1.5f)
        g.drawOval(cx - 28, cy + 8, 24, 28)
        // Sparkle
        g.color = Color(255, 255, 255, 210)
        drawStar(g, cx - 20, cy - 38, 4)
    }

    // ── Helpers ──────────────────────────────────────────
    private fun drawEyes(g: Graphics2D, cx: Int, cy: Int, gap: Int, mood: PetMood) {
        val lx = cx - gap / 2
        val rx = cx + gap / 2
        when (mood) {
            PetMood.HAPPY -> {
                // Happy arc eyes
                g.color = Color.BLACK
                g.stroke = BasicStroke(2f)
                g.drawArc(lx - 4, cy - 2, 8, 6, 0, 180)
                g.drawArc(rx - 4, cy - 2, 8, 6, 0, 180)
            }
            PetMood.SICK -> {
                // X eyes
                g.color = Color(120, 180, 60)
                g.stroke = BasicStroke(2f)
                g.drawLine(lx - 3, cy - 3, lx + 3, cy + 3)
                g.drawLine(lx + 3, cy - 3, lx - 3, cy + 3)
                g.drawLine(rx - 3, cy - 3, rx + 3, cy + 3)
                g.drawLine(rx + 3, cy - 3, rx - 3, cy + 3)
            }
            PetMood.EATING -> {
                // Squinted happy eyes
                g.color = Color.BLACK
                g.stroke = BasicStroke(2.5f)
                g.drawLine(lx - 4, cy, lx + 4, cy)
                g.drawLine(rx - 4, cy, rx + 4, cy)
            }
            PetMood.HUNGRY -> {
                // Sad eyes
                g.color = Color.BLACK
                g.fillOval(lx - 3, cy - 3, 6, 6)
                g.fillOval(rx - 3, cy - 3, 6, 6)
                // Tear drop
                g.color = Color(100, 180, 255)
                g.fillOval(rx + 4, cy + 2, 4, 6)
            }
            PetMood.CONCUSSED -> {
                // Dizzy spiral eyes
                g.color = Color.BLACK
                g.stroke = BasicStroke(1.5f)
                g.drawArc(lx - 4, cy - 4, 8, 8, 90, 270)
                g.drawArc(lx - 2, cy - 2, 4, 4, -90, 270)
                g.drawArc(rx - 4, cy - 4, 8, 8, 90, 270)
                g.drawArc(rx - 2, cy - 2, 4, 4, -90, 270)
            }
            else -> {
                // Normal dot eyes
                g.color = Color.BLACK
                g.fillOval(lx - 3, cy - 3, 7, 7)
                g.fillOval(rx - 3, cy - 3, 7, 7)
                // Pupils (white highlight)
                g.color = Color.WHITE
                g.fillOval(lx - 1, cy - 2, 3, 3)
                g.fillOval(rx - 1, cy - 2, 3, 3)
            }
        }
    }

    /** Front-facing rounded duck bill. Lower bill droops a little when hungry/sick. */
    private fun drawBeak(g: Graphics2D, cx: Int, cy: Int, w: Int, h: Int) {
        g.color = Color(245, 165, 45)
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, h, h)
        g.color = Color(210, 130, 30)
        g.stroke = BasicStroke(1.5f)
        val lipDrop = if (mood == PetMood.HUNGRY || mood == PetMood.SICK) 2 else 0
        g.drawLine(cx - w / 2 + 2, cy + lipDrop, cx + w / 2 - 2, cy + lipDrop)
    }

    /** Little orange webbed foot. */
    private fun drawWebFoot(g: Graphics2D, x: Int, y: Int) {
        g.color = Color(240, 150, 40)
        val xp = intArrayOf(x - 7, x + 7, x)
        val yp = intArrayOf(y + 6, y + 6, y - 3)
        g.fillPolygon(xp, yp, 3)
    }

    private fun drawStar(g: Graphics2D, cx: Int, cy: Int, r: Int) {
        val pts = 5
        val xp = IntArray(pts * 2)
        val yp = IntArray(pts * 2)
        for (i in 0 until pts * 2) {
            val angle = Math.PI / 2 + i * Math.PI / pts
            val radius = if (i % 2 == 0) r else r / 2
            xp[i] = (cx + radius * Math.cos(angle)).toInt()
            yp[i] = (cy - radius * Math.sin(angle)).toInt()
        }
        g.fillPolygon(xp, yp, pts * 2)
    }

    // ── Backgrounds (cosmetic, from the shop) ────────────────────────────────
    private fun drawBackground(g: Graphics2D) {
        val w = width
        val h = height
        when (equippedBackground) {
            "bg_space" -> drawSpaceBackground(g, w, h)
            "bg_forest" -> drawForestBackground(g, w, h)
            "bg_ocean" -> drawOceanBackground(g, w, h)
            else -> {} // none → transparent
        }
    }

    private fun drawSpaceBackground(g: Graphics2D, w: Int, h: Int) {
        g.paint = GradientPaint(0f, 0f, Color(18, 14, 38), 0f, h.toFloat(), Color(40, 24, 66))
        g.fillRect(0, 0, w, h)
        // Deterministic starfield (seeded so stars don't jump each repaint), with twinkle.
        val rnd = java.util.Random(7L)
        for (i in 0 until 50) {
            val x = rnd.nextInt(w.coerceAtLeast(1))
            val y = rnd.nextInt(h.coerceAtLeast(1))
            val size = 1 + rnd.nextInt(2)
            val twinkle = (animFrame + i) % 5 == 0
            g.color = if (twinkle) Color(255, 255, 210) else Color(210, 210, 245)
            g.fillOval(x, y, size, size)
        }
        // Planet in the corner
        g.color = Color(120, 140, 210)
        g.fillOval(w - 52, 12, 36, 36)
        g.color = Color(95, 115, 185)
        g.fillOval(w - 30, 20, 9, 9)
        g.color = Color(150, 165, 225)
        g.fillOval(w - 46, 30, 7, 7)
    }

    private fun drawForestBackground(g: Graphics2D, w: Int, h: Int) {
        // Sky
        g.paint = GradientPaint(0f, 0f, Color(176, 220, 238), 0f, h.toFloat(), Color(206, 235, 214))
        g.fillRect(0, 0, w, h)
        // Sun
        g.color = Color(255, 236, 150)
        g.fillOval(14, 14, 26, 26)
        // Rolling hills
        val groundY = h * 2 / 3
        g.color = Color(120, 175, 95)
        g.fillOval(-30, groundY - 24, w / 2 + 40, 70)
        g.color = Color(104, 160, 84)
        g.fillOval(w / 2 - 20, groundY - 34, w / 2 + 50, 80)
        // Ground
        g.color = Color(110, 168, 88)
        g.fillRect(0, groundY, w, h - groundY)
        // A few simple pine trees
        drawPine(g, w / 6, groundY)
        drawPine(g, w - w / 6, groundY - 6)
    }

    private fun drawPine(g: Graphics2D, baseX: Int, baseY: Int) {
        g.color = Color(110, 80, 50)
        g.fillRect(baseX - 2, baseY - 8, 4, 12)
        g.color = Color(60, 120, 70)
        for (layer in 0 until 3) {
            val ly = baseY - 8 - layer * 8
            val half = 12 - layer * 3
            val xp = intArrayOf(baseX - half, baseX + half, baseX)
            val yp = intArrayOf(ly, ly, ly - 12)
            g.fillPolygon(xp, yp, 3)
        }
    }

    private fun drawOceanBackground(g: Graphics2D, w: Int, h: Int) {
        val seaY = h / 2
        // Sky
        g.paint = GradientPaint(0f, 0f, Color(186, 226, 245), 0f, seaY.toFloat(), Color(224, 244, 252))
        g.fillRect(0, 0, w, seaY)
        // Sun
        g.color = Color(255, 226, 130)
        g.fillOval(w - 48, 16, 28, 28)
        // Sea
        g.paint = GradientPaint(0f, seaY.toFloat(), Color(70, 150, 205), 0f, h.toFloat(), Color(40, 110, 170))
        g.fillRect(0, seaY, w, h - seaY)
        // Animated wave crests
        g.color = Color(255, 255, 255, 120)
        g.stroke = BasicStroke(2f)
        val shift = animFrame * 6
        var row = seaY + 12
        while (row < h) {
            var x = -20 + (shift % 40)
            while (x < w) {
                g.drawArc(x, row, 20, 8, 0, 180)
                x += 40
            }
            row += 18
        }
    }

    // ── Speech bubble ────────────────────────────────────────────────────────
    private fun drawSpeechBubble(g: Graphics2D, text: String, cx: Int, cy: Int) {
        if (text.isBlank() || width < 60) return

        g.font = Font("SansSerif", Font.PLAIN, 11)
        val fm = g.fontMetrics
        val maxBubbleWidth = (width - 12).coerceAtLeast(60)
        val lines = wrapText(text, fm, maxBubbleWidth - 16)
        val lineH = fm.height
        val textW = lines.maxOf { fm.stringWidth(it) }
        val padX = 8
        val padY = 6
        val bw = (textW + padX * 2).coerceAtMost(maxBubbleWidth)
        val bh = lineH * lines.size + padY * 2

        // Anchor the bubble just above the pet's head so the tail emerges from the pet.
        val headTop = cy + when (stage) {
            EvolutionStage.EGG -> -38
            EvolutionStage.BABY -> -42
            EvolutionStage.TEEN -> -50
            EvolutionStage.ADULT -> -54
        }
        val tipX = cx
        val tailTipY = headTop + 6 // dip slightly into the head
        var by = headTop - 8 - bh
        if (by < 2) by = 2
        val bubbleBottom = by + bh
        val tailBottom = maxOf(tailTipY, bubbleBottom + 6)

        var bx = cx - bw / 2
        bx = bx.coerceIn(2, (width - bw - 2).coerceAtLeast(2))

        // Bubble body
        g.color = Color(255, 255, 255, 240)
        g.fillRoundRect(bx, by, bw, bh, 12, 12)
        // Tail pointing down into the pet's head
        g.fillPolygon(
            intArrayOf(tipX - 6, tipX + 6, tipX),
            intArrayOf(bubbleBottom - 1, bubbleBottom - 1, tailBottom),
            3,
        )
        // Border
        g.color = Color(120, 120, 120)
        g.stroke = BasicStroke(1.5f)
        g.drawRoundRect(bx, by, bw, bh, 12, 12)
        g.drawLine(tipX - 6, bubbleBottom, tipX, tailBottom)
        g.drawLine(tipX, tailBottom, tipX + 6, bubbleBottom)

        // Text
        g.color = Color(40, 40, 40)
        var ty = by + padY + fm.ascent
        for (line in lines) {
            val lx = bx + (bw - fm.stringWidth(line)) / 2
            g.drawString(line, lx, ty)
            ty += lineH
        }
    }

    private fun wrapText(text: String, fm: FontMetrics, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (current.isEmpty() || fm.stringWidth(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    private fun drawHat(g: Graphics2D, cx: Int, cy: Int) {
        val headTop = when (stage) {
            EvolutionStage.EGG -> cy - 38
            EvolutionStage.BABY -> cy - 38
            EvolutionStage.TEEN -> cy - 46
            EvolutionStage.ADULT -> cy - 48
        }
        when (equippedHat) {
            "top_hat" -> {
                g.color = Color(30, 30, 30)
                g.fillRect(cx - 20, headTop - 4, 40, 8)
                g.fillRect(cx - 14, headTop - 28, 28, 28)
                g.color = Color(180, 50, 50)
                g.fillRect(cx - 14, headTop - 8, 28, 4)
            }
            "crown" -> {
                g.color = Color(255, 215, 0)
                val bx = cx - 16
                val by = headTop - 18
                g.fillRect(bx, by + 10, 32, 10)
                val crownX = intArrayOf(bx, bx + 6, bx + 10, bx + 16, bx + 22, bx + 26, bx + 32)
                val crownY = intArrayOf(by + 10, by, by + 8, by - 2, by + 8, by, by + 10)
                g.fillPolygon(crownX, crownY, 7)
                // Gems
                g.color = Color(220, 20, 60)
                g.fillOval(cx - 3, by + 12, 6, 6)
                g.color = Color(0, 100, 255)
                g.fillOval(cx - 12, by + 12, 5, 5)
                g.fillOval(cx + 8, by + 12, 5, 5)
            }
            "party_hat" -> {
                g.color = Color(255, 100, 150)
                val px = intArrayOf(cx - 14, cx, cx + 14)
                val py = intArrayOf(headTop + 2, headTop - 26, headTop + 2)
                g.fillPolygon(px, py, 3)
                // Stripes
                g.color = Color(100, 200, 255)
                g.stroke = BasicStroke(2f)
                g.drawLine(cx - 8, headTop - 4, cx + 8, headTop - 4)
                g.drawLine(cx - 4, headTop - 14, cx + 4, headTop - 14)
                // Pom pom
                g.color = Color(255, 255, 100)
                g.fillOval(cx - 4, headTop - 30, 8, 8)
            }
            "wizard_hat" -> {
                g.color = Color(70, 50, 140)
                val wx = intArrayOf(cx - 18, cx, cx + 18)
                val wy = intArrayOf(headTop + 2, headTop - 34, headTop + 2)
                g.fillPolygon(wx, wy, 3)
                // Brim
                g.fillArc(cx - 22, headTop - 4, 44, 12, 0, 180)
                // Stars
                g.color = Color(255, 255, 100)
                drawStar(g, cx - 6, headTop - 10, 4)
                drawStar(g, cx + 5, headTop - 20, 3)
                // Tip star
                g.color = Color(255, 215, 0)
                drawStar(g, cx, headTop - 34, 5)
            }
        }
    }

    private fun drawMoodEffect(g: Graphics2D, cx: Int, cy: Int) {
        when (mood) {
            PetMood.EATING -> {
                // Food particles
                g.color = Color(255, 200, 80, 180)
                val offsets = arrayOf(-20 to -10, 22 to -5, -15 to 5, 18 to 8)
                for ((ox, oy) in offsets) {
                    val shift = if ((animFrame + offsets.indexOf(ox to oy)) % 2 == 0) -2 else 2
                    g.fillOval(cx + ox + shift, cy + oy, 5, 5)
                }
            }
            PetMood.BUILDING -> {
                // Sparkle effect
                g.color = Color(255, 180, 0, 200)
                val sparklePositions = arrayOf(-30 to -20, 28 to -15, -25 to 15, 30 to 10)
                for ((i, pos) in sparklePositions.withIndex()) {
                    if ((animFrame + i) % 3 != 0) {
                        drawStar(g, cx + pos.first, cy + pos.second, 4)
                    }
                }
                // "Pooped" build artifact dropping below the pet
                val dropY = cy + 36 + animFrame * 4
                g.color = Color(120, 90, 60)
                g.fillRoundRect(cx - 8, dropY, 16, 12, 4, 4)
                g.color = Color(160, 120, 80)
                g.fillRoundRect(cx - 8, dropY, 16, 4, 4, 4)
                g.color = Color(255, 215, 0, 220)
                drawStar(g, cx + 12, dropY - 2, 3)
            }
            PetMood.CONCUSSED -> {
                // Spinning stars circling the head (dizzy)
                g.color = Color(255, 215, 0, 220)
                val radius = 34
                for (i in 0 until 3) {
                    val angle = Math.toRadians((animFrame * 30 + i * 120).toDouble())
                    val sx = cx + (radius * Math.cos(angle)).toInt()
                    val sy = cy - 38 + (radius / 2 * Math.sin(angle)).toInt()
                    drawStar(g, sx, sy, 4)
                }
            }
            PetMood.SICK -> {
                // Green swirls
                g.color = Color(120, 200, 60, 150)
                g.stroke = BasicStroke(2f)
                val swirlOffset = animFrame * 3
                g.drawArc(cx + 24, cy - 20 + swirlOffset, 12, 12, 0, 270)
                g.drawArc(cx - 36, cy - 10 + swirlOffset, 10, 10, 0, -270)
            }
            PetMood.HAPPY -> {
                // Floating hearts / sparkles
                g.color = Color(255, 100, 150, 180)
                if (animFrame % 2 == 0) {
                    drawHeart(g, cx - 30, cy - 30, 8)
                }
                if (animFrame % 2 == 1) {
                    drawHeart(g, cx + 26, cy - 25, 6)
                }
            }
            PetMood.HUNGRY -> {
                // Thought bubble "..."
                g.color = Color(200, 200, 200, 180)
                g.fillOval(cx + 26, cy - 10, 6, 6)
                g.fillOval(cx + 34, cy - 18, 8, 8)
                g.fillRoundRect(cx + 38, cy - 36, 30, 18, 10, 10)
                g.color = Color(80, 80, 80)
                g.font = Font("SansSerif", Font.BOLD, 12)
                g.drawString("...", cx + 43, cy - 22)
            }
            else -> {}
        }
    }

    private fun drawHeart(g: Graphics2D, x: Int, y: Int, size: Int) {
        val s = size / 2
        g.fillOval(x, y, s + 1, s + 1)
        g.fillOval(x + s, y, s + 1, s + 1)
        val tx = intArrayOf(x, x + size, x + s)
        val ty = intArrayOf(y + s / 2, y + s / 2, y + size)
        g.fillPolygon(tx, ty, 3)
    }

    // Duck stays in a yellow family; mood shifts the tone for at-a-glance feedback.
    private fun getMoodBodyColor(): Color = when (mood) {
        PetMood.HAPPY -> Color(255, 224, 70)     // bright happy yellow
        PetMood.NEUTRAL -> Color(255, 209, 64)   // golden yellow
        PetMood.HUNGRY -> Color(240, 226, 150)   // pale, washed-out yellow
        PetMood.SICK -> Color(214, 222, 120)     // sickly greenish yellow
        PetMood.EATING -> Color(255, 184, 80)    // warm orange-yellow
        PetMood.BUILDING -> Color(255, 200, 120) // light amber
        PetMood.CONCUSSED -> Color(200, 205, 150) // washed-out, woozy
    }

    private fun lighten(c: Color, factor: Float): Color {
        val r = (c.red + (255 - c.red) * factor).toInt().coerceIn(0, 255)
        val g = (c.green + (255 - c.green) * factor).toInt().coerceIn(0, 255)
        val b = (c.blue + (255 - c.blue) * factor).toInt().coerceIn(0, 255)
        return Color(r, g, b)
    }

    fun dispose() {
        animTimer.stop()
    }
}
