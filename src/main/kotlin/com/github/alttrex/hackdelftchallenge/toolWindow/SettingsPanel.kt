package com.github.alttrex.hackdelftchallenge.toolWindow

import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class SettingsPanel : JPanel(BorderLayout()) {

    init {
        border = JBUI.Borders.empty(12)
        add(buildContent(), BorderLayout.NORTH)
    }

    private fun buildContent(): JPanel {
        val state = PetState.getInstance()

        val nameField = JBTextField(state.petName, 18)
        val quotaSpinner = JSpinner(SpinnerNumberModel(state.dailyQuota, 10, 1000, 10))

        return panel {
            group("Pet") {
                row("Name:") {
                    cell(nameField).align(AlignX.FILL)
                }
                row("Daily LOC quota:") {
                    cell(quotaSpinner)
                }
                row {
                    button("Save Settings") {
                        state.petName = nameField.text.ifBlank { "DevPet" }
                        state.dailyQuota = quotaSpinner.value as Int
                    }
                }
            }

            group("Debug Actions") {
                row {
                    button("Reset Pet") {
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
                    button("+100 Coins") {
                        state.addCoins(100)
                    }
                }.rowComment("Hackathon/debug helpers — reset all progress or top up coins.")
            }
        }
    }
}
