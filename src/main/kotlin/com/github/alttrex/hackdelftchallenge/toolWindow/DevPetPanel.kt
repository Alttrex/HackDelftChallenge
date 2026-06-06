package com.github.alttrex.hackdelftchallenge.toolWindow

import com.github.alttrex.hackdelftchallenge.state.PetMood
import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import java.util.Timer
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import kotlin.concurrent.scheduleAtFixedRate

class DevPetPanel : JPanel(BorderLayout()) {

    private val petAsciiLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = Font("Monospaced", Font.PLAIN, 12)
    }

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

    private val quotaBar = JProgressBar().apply {
        isStringPainted = true
        string = "Daily Quota"
        foreground = JBColor(0x4CAF50, 0x66BB6A)
    }

    private val coinsLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.BOLD, 13f)
    }

    private val statsLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(11f)
    }

    private val animationLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.ITALIC, 12f)
        foreground = JBColor(0xFF9800.toInt(), 0xFFB74D.toInt())
    }

    init {
        border = JBUI.Borders.empty(10)
        buildUI()
        refresh()
        startRefreshTimer()
    }

    private fun buildUI() {
        // Top: Pet name + stage
        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(nameLabel)
            add(stageLabel)
        }

        // Center: Pet ASCII art + mood + animation
        val centerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(10)
            add(petAsciiLabel)
            add(moodLabel)
            add(animationLabel)
        }

        // Bottom: Stats
        val statsPanel = JPanel(GridLayout(0, 1, 4, 4)).apply {
            border = BorderFactory.createTitledBorder("Stats")
            add(levelLabel)
            add(xpBar)
            add(quotaBar)
            add(coinsLabel)
            add(statsLabel)
        }

        add(topPanel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)
        add(statsPanel, BorderLayout.SOUTH)
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

        // Pet ASCII art with mood variation
        val petText = buildPetDisplay(stage.ascii, mood)
        petAsciiLabel.text = "<html><pre style='text-align:center'>$petText</pre></html>"

        moodLabel.text = "Mood: ${mood.emoji} ${mood.displayName}"

        // Animation text
        animationLabel.text = when (mood) {
            PetMood.EATING -> "🍔 *nom nom nom* eating your code..."
            PetMood.BUILDING -> "💩 *plop* build artifact created!"
            PetMood.SICK -> "🤢 Blegh! That code tasted artificial..."
            PetMood.HAPPY -> "✨ Happy and generating DevCoins!"
            PetMood.HUNGRY -> "😟 Feed me some code!"
            PetMood.NEUTRAL -> ""
        }

        xpBar.maximum = state.xpToNextLevel
        xpBar.value = state.xp
        xpBar.string = "XP: ${state.xp} / ${state.xpToNextLevel}"

        quotaBar.maximum = state.dailyQuota
        quotaBar.value = state.dailyLinesWritten.coerceAtMost(state.dailyQuota)
        quotaBar.string = "LOC: ${state.dailyLinesWritten} / ${state.dailyQuota}"

        coinsLabel.text = "🪙 DevCoins: ${state.devCoins}"
        statsLabel.text = "Total LOC: ${state.totalLinesWritten} | Builds: ${state.totalSuccessfulBuilds}/${state.totalBuilds}"
    }

    private fun buildPetDisplay(ascii: String, mood: PetMood): String {
        val equipped = PetState.getInstance().equippedHat
        val hatArt = when (equipped) {
            "top_hat" -> "   ___\n  |   |\n  |___|"
            "crown" -> "  👑"
            "party_hat" -> "    ▲\n   / \\"
            "wizard_hat" -> "    ★\n   /|\\\n  / | \\"
            else -> ""
        }
        return if (hatArt.isNotEmpty()) "$hatArt\n$ascii" else ascii
    }

    private fun startRefreshTimer() {
        Timer("DevPet-UI-Refresh", true).scheduleAtFixedRate(0L, 2000L) {
            SwingUtilities.invokeLater { refresh() }
        }
    }
}
