package com.github.alttrex.hackdelftchallenge.toolWindow

import com.github.alttrex.hackdelftchallenge.listeners.CodeTracker
import com.github.alttrex.hackdelftchallenge.state.EvolutionStage
import com.github.alttrex.hackdelftchallenge.state.PetMood
import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.*

/**
 * Development / debug panel that allows quick manipulation of pet state.
 * Guarded by [com.github.alttrex.hackdelftchallenge.DevPetConfig.DEV_MODE].
 */
class DevPanel : JPanel(BorderLayout()) {

    init {
        border = JBUI.Borders.empty(10)
        buildUI()
    }

    private fun buildUI() {
        val grid = JPanel(GridLayout(0, 1, 4, 6))
        grid.border = BorderFactory.createTitledBorder("🛠 Dev Controls")

        // ── Stage ────────────────────────────────────────
        val stageCombo = JComboBox(EvolutionStage.entries.toTypedArray())
        stageCombo.selectedItem = PetState.getInstance().getCurrentStage()
        stageCombo.addActionListener {
            val selected = stageCombo.selectedItem as EvolutionStage
            PetState.getInstance().stage = selected.name
        }
        grid.add(labeledRow("Stage:", stageCombo))

        // ── Mood ─────────────────────────────────────────
        val moodCombo = JComboBox(PetMood.entries.toTypedArray())
        moodCombo.selectedItem = PetState.getInstance().getCurrentMood()
        moodCombo.addActionListener {
            val selected = moodCombo.selectedItem as PetMood
            PetState.getInstance().mood = selected.name
        }
        grid.add(labeledRow("Mood:", moodCombo))

        // ── Level ────────────────────────────────────────
        val levelSpinner = JSpinner(SpinnerNumberModel(PetState.getInstance().level, 1, 999, 1))
        levelSpinner.addChangeListener {
            PetState.getInstance().level = levelSpinner.value as Int
        }
        grid.add(labeledRow("Level:", levelSpinner))

        // ── XP ───────────────────────────────────────────
        val xpSpinner = JSpinner(SpinnerNumberModel(PetState.getInstance().xp, 0, 100_000, 10))
        xpSpinner.addChangeListener {
            PetState.getInstance().xp = xpSpinner.value as Int
        }
        grid.add(labeledRow("XP:", xpSpinner))

        // ── XP to next level ─────────────────────────────
        val xpNextSpinner = JSpinner(SpinnerNumberModel(PetState.getInstance().xpToNextLevel, 1, 100_000, 10))
        xpNextSpinner.addChangeListener {
            PetState.getInstance().xpToNextLevel = xpNextSpinner.value as Int
        }
        grid.add(labeledRow("XP to Next:", xpNextSpinner))

        // ── DevCoins ─────────────────────────────────────
        val coinsSpinner = JSpinner(SpinnerNumberModel(PetState.getInstance().devCoins, 0, 999_999, 10))
        coinsSpinner.addChangeListener {
            PetState.getInstance().devCoins = coinsSpinner.value as Int
        }
        grid.add(labeledRow("DevCoins:", coinsSpinner))

        // ── Daily Lines Written ──────────────────────────
        val dailyLinesSpinner = JSpinner(SpinnerNumberModel(PetState.getInstance().dailyLinesWritten, 0, 100_000, 5))
        dailyLinesSpinner.addChangeListener {
            PetState.getInstance().dailyLinesWritten = dailyLinesSpinner.value as Int
        }
        grid.add(labeledRow("Daily LOC:", dailyLinesSpinner))

        // ── Daily Quota ──────────────────────────────────
        val quotaSpinner = JSpinner(SpinnerNumberModel(PetState.getInstance().dailyQuota, 1, 10_000, 5))
        quotaSpinner.addChangeListener {
            PetState.getInstance().dailyQuota = quotaSpinner.value as Int
        }
        grid.add(labeledRow("Daily Quota:", quotaSpinner))

        // ── Total Lines Written ──────────────────────────
        val totalLinesSpinner = JSpinner(SpinnerNumberModel(PetState.getInstance().totalLinesWritten, 0, 999_999, 10))
        totalLinesSpinner.addChangeListener {
            PetState.getInstance().totalLinesWritten = totalLinesSpinner.value as Int
        }
        grid.add(labeledRow("Total LOC:", totalLinesSpinner))

        // ── Total Builds ─────────────────────────────────
        val buildsSpinner = JSpinner(SpinnerNumberModel(PetState.getInstance().totalBuilds, 0, 999_999, 1))
        buildsSpinner.addChangeListener {
            PetState.getInstance().totalBuilds = buildsSpinner.value as Int
        }
        grid.add(labeledRow("Total Builds:", buildsSpinner))

        // ── Total Successful Builds ──────────────────────
        val successBuildsSpinner = JSpinner(SpinnerNumberModel(PetState.getInstance().totalSuccessfulBuilds, 0, 999_999, 1))
        successBuildsSpinner.addChangeListener {
            PetState.getInstance().totalSuccessfulBuilds = successBuildsSpinner.value as Int
        }
        grid.add(labeledRow("Success Builds:", successBuildsSpinner))

        // ── Equipped Hat ─────────────────────────────────
        val hatOptions = arrayOf("", "top_hat", "crown", "party_hat", "wizard_hat")
        val hatCombo = JComboBox(hatOptions)
        hatCombo.selectedItem = PetState.getInstance().equippedHat
        hatCombo.addActionListener {
            PetState.getInstance().equippedHat = hatCombo.selectedItem as String
        }
        grid.add(labeledRow("Hat:", hatCombo))

        // ── Quick Actions ────────────────────────────────
        val actionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))

        val addXpBtn = JButton("+100 XP")
        addXpBtn.addActionListener { PetState.getInstance().addXp(100) }
        actionsPanel.add(addXpBtn)

        val addCoinsBtn = JButton("+100 Coins")
        addCoinsBtn.addActionListener { PetState.getInstance().devCoins += 100 }
        actionsPanel.add(addCoinsBtn)

        val levelUpBtn = JButton("Level Up")
        levelUpBtn.addActionListener {
            val state = PetState.getInstance()
            state.addXp(state.xpToNextLevel - state.xp)
        }
        actionsPanel.add(levelUpBtn)

        grid.add(actionsPanel)

        // ── Percentages ───────────────────────────────
        val aiLabel = JBLabel("AI: —")
        val testLabel = JBLabel("Tests: —")
        val cleanLabel = JBLabel("Clean: —")
        val healthLabel = JBLabel("Health: —")
        val percentPanel = JPanel(GridLayout(0, 1, 0, 2))
        percentPanel.add(aiLabel)
        percentPanel.add(testLabel)
        percentPanel.add(cleanLabel)
        percentPanel.add(healthLabel)
        grid.add(percentPanel)

        // ── Reset Button ─────────────────────────────────
        val resetPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        val resetBtn = JButton("Reset All State")
        resetBtn.addActionListener {
            val confirm = JOptionPane.showConfirmDialog(
                this,
                "Reset all pet state to defaults?",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION
            )
            if (confirm == JOptionPane.YES_OPTION) {
                val state = PetState.getInstance()
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
                state.mood = PetMood.NEUTRAL.name
                state.stage = EvolutionStage.EGG.name
                state.equippedHat = ""
                state.equippedBackground = ""
                state.ownedCosmetics.clear()
                state.unlockedAchievements.clear()
                // Sync spinners
                refreshControls(
                    stageCombo, moodCombo, levelSpinner, xpSpinner, xpNextSpinner,
                    coinsSpinner, dailyLinesSpinner, quotaSpinner, totalLinesSpinner,
                    buildsSpinner, successBuildsSpinner, hatCombo
                )
            }
        }
        resetPanel.add(resetBtn)
        grid.add(resetPanel)

        // Wrap in scroll pane for small tool windows
        val scrollPane = JScrollPane(grid).apply {
            border = null
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }
        add(scrollPane, BorderLayout.CENTER)

        // Periodic sync from state → controls
        val syncTimer = Timer(2000) {
            SwingUtilities.invokeLater {
                refreshControls(
                    stageCombo, moodCombo, levelSpinner, xpSpinner, xpNextSpinner,
                    coinsSpinner, dailyLinesSpinner, quotaSpinner, totalLinesSpinner,
                    buildsSpinner, successBuildsSpinner, hatCombo
                )
                val tracker = CodeTracker.instance
                if (tracker != null) {
                    // Scores iterate the project + parse PSI, which is a slow operation
                    // and must not run on the EDT. Compute off-thread, then update labels.
                    ApplicationManager.getApplication().executeOnPooledThread {
                        val ai = 100.0 - tracker.getAiScore()
                        val tests = tracker.getTestScore()
                        val clean = tracker.getCleanlinessScore()
                        val health = tracker.getOverallHealthScore()
                        SwingUtilities.invokeLater {
                            aiLabel.text = "AI: %.1f%%".format(ai)
                            testLabel.text = "Tests: %.1f%%".format(tests)
                            cleanLabel.text = "Clean: %.1f%%".format(clean)
                            healthLabel.text = "Health: %.1f%%".format(health)
                        }
                    }
                }
            }
        }
        syncTimer.isRepeats = true
        syncTimer.start()
    }

    private fun refreshControls(
        stageCombo: JComboBox<EvolutionStage>,
        moodCombo: JComboBox<PetMood>,
        levelSpinner: JSpinner,
        xpSpinner: JSpinner,
        xpNextSpinner: JSpinner,
        coinsSpinner: JSpinner,
        dailyLinesSpinner: JSpinner,
        quotaSpinner: JSpinner,
        totalLinesSpinner: JSpinner,
        buildsSpinner: JSpinner,
        successBuildsSpinner: JSpinner,
        hatCombo: JComboBox<String>,
    ) {
        val state = PetState.getInstance()
        stageCombo.selectedItem = state.getCurrentStage()
        moodCombo.selectedItem = state.getCurrentMood()
        levelSpinner.value = state.level
        xpSpinner.value = state.xp
        xpNextSpinner.value = state.xpToNextLevel
        coinsSpinner.value = state.devCoins
        dailyLinesSpinner.value = state.dailyLinesWritten
        quotaSpinner.value = state.dailyQuota
        totalLinesSpinner.value = state.totalLinesWritten
        buildsSpinner.value = state.totalBuilds
        successBuildsSpinner.value = state.totalSuccessfulBuilds
        hatCombo.selectedItem = state.equippedHat
    }

    private fun labeledRow(label: String, component: JComponent): JPanel {
        return JPanel(BorderLayout(8, 0)).apply {
            add(JBLabel(label).apply { preferredSize = java.awt.Dimension(110, 24) }, BorderLayout.WEST)
            add(component, BorderLayout.CENTER)
        }
    }
}
