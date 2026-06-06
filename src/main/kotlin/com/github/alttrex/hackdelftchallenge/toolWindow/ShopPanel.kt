package com.github.alttrex.hackdelftchallenge.toolWindow

import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

class ShopPanel : JPanel(BorderLayout()) {

    private val balanceLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(java.awt.Font.BOLD, 14f)
    }

    private val itemsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    // Live preview of the pet wearing a selected/equipped hat.
    private val previewRenderer = PetRenderer().apply {
        preferredSize = java.awt.Dimension(160, 140)
    }

    private val previewLabel = JBLabel("Preview").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(java.awt.Font.ITALIC, 11f)
    }

    // When the user clicks "Preview", stop snapping the preview back to equipped cosmetics.
    private var previewOverride = false

    init {
        border = JBUI.Borders.empty(10)
        buildUI()
        refresh()
        startRefreshTimer()
    }

    private fun buildUI() {
        val topPanel = JPanel(BorderLayout()).apply {
            add(balanceLabel, BorderLayout.NORTH)
            val previewPanel = JPanel(BorderLayout()).apply {
                add(previewRenderer, BorderLayout.CENTER)
                add(previewLabel, BorderLayout.SOUTH)
            }
            add(previewPanel, BorderLayout.CENTER)
        }
        add(topPanel, BorderLayout.NORTH)
        add(JBScrollPane(itemsPanel), BorderLayout.CENTER)
    }

    fun refresh() {
        val state = PetState.getInstance()
        balanceLabel.text = "🪙 DevCoins: ${state.devCoins}"

        // Keep the preview pet's stage/mood live; cosmetics follow the real pet
        // unless the user is actively previewing an item.
        previewRenderer.stage = state.getCurrentStage()
        previewRenderer.mood = state.getCurrentMood()
        if (!previewOverride) {
            previewRenderer.equippedHat = state.equippedHat
            previewRenderer.equippedBackground = state.equippedBackground
            previewLabel.text = "Preview"
        }

        itemsPanel.removeAll()

        for (item in PetState.SHOP_ITEMS) {
            val owned = item.id in state.ownedCosmetics
            val equipped = (item.type == "hat" && state.equippedHat == item.id) ||
                    (item.type == "background" && state.equippedBackground == item.id)

            val row = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                    JBUI.Borders.empty(6)
                )

                add(JBLabel("${item.name} (${item.type})"))

                // Cosmetics can be previewed on the pet without buying/equipping.
                add(JButton("Preview").apply {
                    addActionListener {
                        previewOverride = true
                        if (item.type == "hat") previewRenderer.equippedHat = item.id
                        else previewRenderer.equippedBackground = item.id
                        previewLabel.text = "Preview: ${item.name} (not equipped)"
                    }
                })

                if (owned) {
                    if (equipped) {
                        add(JBLabel("  ✅ Equipped"))
                        add(JButton("Unequip").apply {
                            addActionListener {
                                if (item.type == "hat") state.equippedHat = ""
                                else state.equippedBackground = ""
                                previewOverride = false
                                refresh()
                            }
                        })
                    } else {
                        add(JBLabel("  Owned"))
                        add(JButton("Equip").apply {
                            addActionListener {
                                if (item.type == "hat") state.equippedHat = item.id
                                else state.equippedBackground = item.id
                                previewOverride = false
                                refresh()
                            }
                        })
                    }
                } else {
                    add(JBLabel("  💰 ${item.cost}"))
                    add(JButton("Buy").apply {
                        isEnabled = state.devCoins >= item.cost
                        addActionListener {
                            if (state.devCoins >= item.cost) {
                                state.devCoins -= item.cost
                                state.ownedCosmetics.add(item.id)
                                refresh()
                            } else {
                                JOptionPane.showMessageDialog(
                                    this@ShopPanel,
                                    "Not enough DevCoins!",
                                    "Shop",
                                    JOptionPane.WARNING_MESSAGE
                                )
                            }
                        }
                    })
                }
            }
            itemsPanel.add(row)
        }

        itemsPanel.revalidate()
        itemsPanel.repaint()
    }

    private fun startRefreshTimer() {
        Timer("DevPet-Shop-Refresh", true).scheduleAtFixedRate(0L, 2000L) {
            SwingUtilities.invokeLater { refresh() }
        }
    }
}
