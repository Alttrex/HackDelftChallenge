package com.github.alttrex.hackdelftchallenge.toolWindow

import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class SettingsPanel : JPanel() {

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(10)
        buildUI()
    }

    private fun buildUI() {
        val state = PetState.getInstance()

        // Pet name
        val nameField = JBTextField(state.petName, 15)
        val nameRow = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JBLabel("Pet Name: "))
            add(nameField)
        }
        add(nameRow)

        // Daily quota
        val quotaSpinner = JSpinner(SpinnerNumberModel(state.dailyQuota, 10, 1000, 10))
        val quotaRow = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JBLabel("Daily LOC Quota: "))
            add(quotaSpinner)
        }
        add(quotaRow)

        // Save button
        val saveRow = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton("Save Settings").apply {
                addActionListener {
                    state.petName = nameField.text.ifBlank { "DevPet" }
                    state.dailyQuota = quotaSpinner.value as Int
                }
            })
        }
        add(saveRow)

        // Reset button (debug/hackathon convenience)
        val resetRow = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton("🔄 Reset Pet (Debug)").apply {
                addActionListener {
                    state.level = 1
                    state.xp = 0
                    state.xpToNextLevel = 100
                    state.devCoins = 0
                    state.dailyLinesWritten = 0
                    state.totalLinesWritten = 0
                    state.totalBuilds = 0
                    state.totalSuccessfulBuilds = 0
                    state.dailyJavadocLinesWritten = 0
                    state.totalJavadocLinesWritten = 0
                    state.dailyTestLinesWritten = 0
                    state.totalTestLinesWritten = 0
                    state.ownedCosmetics.clear()
                    state.unlockedAchievements.clear()
                    state.equippedHat = ""
                    state.equippedBackground = ""
                    state.stage = "EGG"
                    state.mood = "NEUTRAL"
                }
            })

            add(JButton("💰 +100 Coins (Debug)").apply {
                addActionListener {
                    state.devCoins += 100
                }
            })
        }
        add(resetRow)
    }
}
