package com.github.alttrex.hackdelftchallenge.toolWindow

import com.github.alttrex.hackdelftchallenge.achievements.Achievement
import com.github.alttrex.hackdelftchallenge.state.PetMood
import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Font
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.Timer
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import kotlin.concurrent.scheduleAtFixedRate

class DevPetPanel : JPanel(BorderLayout()) {

    /** Wired by the tool window factory so the Shop button can switch tabs. */
    var onOpenShop: () -> Unit = {}

    private val petRenderer = PetRenderer()

    private val nameLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.BOLD, 16f)
    }

    private val moodLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(14f)
    }

    private val stageLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
    }

    private val levelLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
    }

    private val xpBar = JProgressBar().apply {
        isStringPainted = true
        string = "XP"
    }

    private val quotaBar = SegmentedQuotaBar()

    private val quotaLegend = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(10f)
    }

    private val coinsLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.BOLD, 13f)
    }

    private val statsLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(11f)
    }

    private val achievementsLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(11f)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        toolTipText = "Click to view all achievements"
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = showAchievementsDialog()
        })
    }

    private val shopButton = JButton("🛒 Open Shop").apply {
        addActionListener { onOpenShop() }
    }

    private val animationLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.ITALIC, 12f)
        foreground = JBColor(0xFF9800.toInt(), 0xFFB74D.toInt())
    }

    // Instant UI update on any state change (e.g. typing a line). Self-removes when
    // the panel is no longer displayed so disposed tool windows don't leak listeners.
    private val changeListener: () -> Unit = {
        SwingUtilities.invokeLater {
            if (isDisplayable) refresh() else PetState.getInstance().removeChangeListener(changeListener)
        }
    }

    init {
        border = JBUI.Borders.empty(10)
        // Bubble text is pulled live by the renderer every repaint (no polling lag).
        petRenderer.speechSupplier = { PetState.getInstance().currentSpeech() }
        buildUI()
        refresh()
        PetState.getInstance().addChangeListener(changeListener)
        startRefreshTimer()
    }

    private fun buildUI() {
        // Top: Pet name + stage
        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(nameLabel)
            add(stageLabel)
        }

        // Center: Pet graphic + mood + animation
        val centerPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10)
            add(petRenderer, BorderLayout.CENTER)
            val labelPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(moodLabel)
                add(animationLabel)
            }
            add(labelPanel, BorderLayout.SOUTH)
        }

        // Stats
        val statsPanel = JPanel(GridLayout(0, 1, 4, 4)).apply {
            border = BorderFactory.createTitledBorder("Stats")
            add(levelLabel)
            add(xpBar)
            add(quotaBar)
            add(quotaLegend)
            add(coinsLabel)
            add(statsLabel)
            add(achievementsLabel)
        }

        val bottomPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(statsPanel)
            add(JPanel().apply { add(shopButton) })
        }

        add(topPanel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)
    }

    private fun refresh() {
        val state = PetState.getInstance()
        state.checkDayReset()
        state.generateCoins()

        val stage = state.getCurrentStage()
        val mood = state.getCurrentMood()

        nameLabel.text = state.petName
        stageLabel.text = "Stage: ${stage.displayName}"
        levelLabel.text = "Level ${state.level}"

        // Update pet renderer
        petRenderer.stage = stage
        petRenderer.mood = mood
        petRenderer.equippedHat = state.equippedHat
        petRenderer.equippedBackground = state.equippedBackground

        moodLabel.text = "Mood: ${mood.displayName}"

        // Animation text
        animationLabel.text = when (mood) {
            PetMood.EATING -> "*nom nom nom* eating your code..."
            PetMood.BUILDING -> "*plop* build artifact created!"
            PetMood.SICK -> "Blegh! Type human code to recover (${state.healingLines}/${PetState.SICK_RECOVERY_LINES})"
            PetMood.HAPPY -> "Happy and generating DevCoins!"
            PetMood.HUNGRY -> "Feed me some code!"
            PetMood.CONCUSSED -> "Dizzy! Type human code to recover (${state.healingLines}/${PetState.SICK_RECOVERY_LINES})"
            PetMood.NEUTRAL -> ""
        }

        xpBar.maximum = state.xpToNextLevel
        xpBar.value = state.xp
        xpBar.string = "XP: ${state.xp} / ${state.xpToNextLevel}"

        val javadoc = state.dailyJavadocLinesWritten
        val tests = state.dailyTestLinesWritten
        val prod = (state.dailyLinesWritten - javadoc - tests).coerceAtLeast(0)

        quotaBar.max = state.dailyQuota
        quotaBar.prodLines = prod
        quotaBar.javadocLines = javadoc
        quotaBar.testLines = tests
        quotaBar.text = "LOC: ${state.dailyLinesWritten} / ${state.dailyQuota}"
        quotaBar.repaint()

        quotaLegend.text = "<html>" +
            "<font color='#4CAF50'>\u25A0</font> Prod $prod &nbsp; " +
            "<font color='#2196F3'>\u25A0</font> Javadoc $javadoc &nbsp; " +
            "<font color='#9C27B0'>\u25A0</font> Tests $tests</html>"

        coinsLabel.text = "DevCoins: ${state.devCoins}"
        statsLabel.text = "<html>Total LOC: ${state.totalLinesWritten} | Builds: ${state.totalSuccessfulBuilds}/${state.totalBuilds}<br>" +
            "📝 Javadoc: ${state.totalJavadocLinesWritten} | 🧪 Tests: ${state.totalTestLinesWritten}</html>"
        achievementsLabel.text = "🏆 Achievements: ${state.unlockedAchievements.size} / ${Achievement.entries.size} (click to view)"
    }

    /** Shows a dialog listing every achievement and whether it has been unlocked. */
    private fun showAchievementsDialog() {
        val unlocked = PetState.getInstance().unlockedAchievements
        val rows = Achievement.entries.joinToString("") { achievement ->
            val done = achievement.id in unlocked
            val icon = if (done) "\u2705" else "\uD83D\uDD12"
            val titleColor = if (done) "#4CAF50" else "#9E9E9E"
            "<tr>" +
                "<td valign='top' style='padding:4px 8px 4px 0;font-size:14px'>$icon</td>" +
                "<td style='padding:4px 0'>" +
                "<b style='color:$titleColor'>${achievement.title}</b><br>" +
                "<span style='color:#9E9E9E'>${achievement.description}</span>" +
                "</td></tr>"
        }
        val unlockedCount = Achievement.entries.count { it.id in unlocked }
        val html = "<html><body style='width:320px'>" +
            "<p><b>Unlocked $unlockedCount / ${Achievement.entries.size}</b></p>" +
            "<table>$rows</table></body></html>"
        JOptionPane.showMessageDialog(
            this,
            JLabel(html),
            "Achievements",
            JOptionPane.PLAIN_MESSAGE,
        )
    }

    private fun startRefreshTimer() {
        Timer("DevPet-UI-Refresh", true).scheduleAtFixedRate(0L, 2000L) {
            SwingUtilities.invokeLater { refresh() }
        }
    }
}
