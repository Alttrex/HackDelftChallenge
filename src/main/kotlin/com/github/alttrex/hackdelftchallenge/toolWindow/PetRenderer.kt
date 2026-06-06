package com.github.alttrex.hackdelftchallenge.toolWindow

import com.github.alttrex.hackdelftchallenge.state.EvolutionStage
import com.github.alttrex.hackdelftchallenge.state.PetMood
import java.awt.*
import javax.swing.JComponent
import javax.swing.Timer

/**
 * Custom component that draws pixel-art style pets using Graphics2D.
 * Supports different evolution stages, moods, and idle animation.
 */
class PetRenderer : JComponent() {

    var stage: EvolutionStage = EvolutionStage.EGG
    var mood: PetMood = PetMood.NEUTRAL
    var equippedHat: String = ""

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
            EvolutionStage.BABY -> drawBaby(g2, cx, cy + bob)
            EvolutionStage.TEEN -> drawTeen(g2, cx, cy + bob)
            EvolutionStage.ADULT -> drawAdult(g2, cx, cy + bob)
        }

        drawHat(g2, cx, cy + bob)
        drawMoodEffect(g2, cx, cy + bob)

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
        // Eyes (peeking through crack)
        drawEyes(g, cx, cy - 14, 10, mood)
    }

    // ── Baby ─────────────────────────────────────────────
    private fun drawBaby(g: Graphics2D, cx: Int, cy: Int) {
        val bodyColor = getMoodBodyColor()
        // Shadow
        g.color = Color(0, 0, 0, 40)
        g.fillOval(cx - 22, cy + 28, 44, 10)
        // Body (round blob)
        g.color = bodyColor
        g.fillOval(cx - 28, cy - 24, 56, 56)
        g.color = bodyColor.darker()
        g.stroke = BasicStroke(2f)
        g.drawOval(cx - 28, cy - 24, 56, 56)
        // Belly
        g.color = lighten(bodyColor, 0.3f)
        g.fillOval(cx - 14, cy - 4, 28, 24)
        // Eyes
        drawEyes(g, cx, cy - 12, 14, mood)
        // Mouth
        drawMouth(g, cx, cy + 4, 8)
        // Tiny feet
        g.color = bodyColor.darker()
        g.fillOval(cx - 18, cy + 26, 12, 8)
        g.fillOval(cx + 6, cy + 26, 12, 8)
    }

    // ── Teen ─────────────────────────────────────────────
    private fun drawTeen(g: Graphics2D, cx: Int, cy: Int) {
        val bodyColor = getMoodBodyColor()
        // Shadow
        g.color = Color(0, 0, 0, 40)
        g.fillOval(cx - 26, cy + 38, 52, 12)
        // Ears
        g.color = bodyColor
        drawEar(g, cx - 24, cy - 44, -1)
        drawEar(g, cx + 14, cy - 44, 1)
        // Head
        g.color = bodyColor
        g.fillRoundRect(cx - 30, cy - 36, 60, 50, 30, 30)
        // Body
        g.fillRoundRect(cx - 24, cy + 8, 48, 30, 20, 20)
        // Outlines
        g.color = bodyColor.darker()
        g.stroke = BasicStroke(2f)
        g.drawRoundRect(cx - 30, cy - 36, 60, 50, 30, 30)
        g.drawRoundRect(cx - 24, cy + 8, 48, 30, 20, 20)
        // Belly
        g.color = lighten(bodyColor, 0.3f)
        g.fillOval(cx - 12, cy + 12, 24, 20)
        // Eyes
        drawEyes(g, cx, cy - 18, 16, mood)
        // Mouth
        drawMouth(g, cx, cy - 2, 10)
        // Feet
        g.color = bodyColor.darker()
        g.fillRoundRect(cx - 20, cy + 34, 14, 10, 6, 6)
        g.fillRoundRect(cx + 6, cy + 34, 14, 10, 6, 6)
        // Tail
        g.stroke = BasicStroke(3f)
        g.drawArc(cx + 20, cy + 14, 20, 20, 0, -160)
    }

    // ── Adult ────────────────────────────────────────────
    private fun drawAdult(g: Graphics2D, cx: Int, cy: Int) {
        val bodyColor = getMoodBodyColor()
        // Shadow
        g.color = Color(0, 0, 0, 40)
        g.fillOval(cx - 30, cy + 48, 60, 14)
        // Ears
        g.color = bodyColor
        drawEar(g, cx - 30, cy - 56, -1)
        drawEar(g, cx + 18, cy - 56, 1)
        // Head
        g.color = bodyColor
        g.fillRoundRect(cx - 36, cy - 46, 72, 58, 36, 36)
        // Body
        g.fillRoundRect(cx - 30, cy + 6, 60, 42, 24, 24)
        // Outlines
        g.color = bodyColor.darker()
        g.stroke = BasicStroke(2.5f)
        g.drawRoundRect(cx - 36, cy - 46, 72, 58, 36, 36)
        g.drawRoundRect(cx - 30, cy + 6, 60, 42, 24, 24)
        // Belly
        g.color = lighten(bodyColor, 0.3f)
        g.fillOval(cx - 16, cy + 12, 32, 28)
        // Eyes
        drawEyes(g, cx, cy - 24, 18, mood)
        // Mouth
        drawMouth(g, cx, cy - 6, 12)
        // Arms
        g.color = bodyColor
        g.stroke = BasicStroke(4f)
        g.drawArc(cx - 38, cy + 4, 16, 24, 40, 180)
        g.drawArc(cx + 22, cy + 4, 16, 24, -40, -180)
        // Feet
        g.color = bodyColor.darker()
        g.fillRoundRect(cx - 24, cy + 42, 16, 12, 8, 8)
        g.fillRoundRect(cx + 8, cy + 42, 16, 12, 8, 8)
        // Tail
        g.stroke = BasicStroke(4f)
        g.drawArc(cx + 26, cy + 18, 24, 24, 0, -180)
        // Star sparkle on forehead
        g.color = Color(255, 215, 0)
        drawStar(g, cx, cy - 42, 6)
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

    private fun drawMouth(g: Graphics2D, cx: Int, cy: Int, width: Int) {
        g.stroke = BasicStroke(1.5f)
        when (mood) {
            PetMood.HAPPY, PetMood.EATING -> {
                g.color = Color.BLACK
                g.drawArc(cx - width / 2, cy - 2, width, width / 2, 180, 180)
            }
            PetMood.SICK -> {
                g.color = Color(120, 180, 60)
                g.drawArc(cx - width / 2, cy + 2, width, width / 2, 0, 180)
            }
            PetMood.HUNGRY -> {
                g.color = Color.BLACK
                g.drawOval(cx - 3, cy, 6, 8)
            }
            else -> {
                g.color = Color.BLACK
                g.drawLine(cx - width / 3, cy + 2, cx + width / 3, cy + 2)
            }
        }
    }

    private fun drawEar(g: Graphics2D, x: Int, y: Int, dir: Int) {
        val xp = intArrayOf(x, x + 6 * dir, x + 12)
        val yp = intArrayOf(y + 20, y, y + 20)
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

    private fun drawHat(g: Graphics2D, cx: Int, cy: Int) {
        val headTop = when (stage) {
            EvolutionStage.EGG -> cy - 38
            EvolutionStage.BABY -> cy - 24
            EvolutionStage.TEEN -> cy - 36
            EvolutionStage.ADULT -> cy - 46
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

    private fun getMoodBodyColor(): Color = when (mood) {
        PetMood.HAPPY -> Color(120, 210, 150)   // green
        PetMood.NEUTRAL -> Color(150, 190, 230)  // blue
        PetMood.HUNGRY -> Color(230, 200, 140)   // yellowish
        PetMood.SICK -> Color(180, 210, 140)      // pale green
        PetMood.EATING -> Color(250, 180, 120)    // orange
        PetMood.BUILDING -> Color(180, 150, 220)  // purple
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
