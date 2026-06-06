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

class ShopPanel : JPanel(BorderLayout()) {

    private val balanceLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(java.awt.Font.BOLD, 14f)
    }

    private val itemsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    init {
        border = JBUI.Borders.empty(10)
        buildUI()
        refresh()
    }

    private fun buildUI() {
        add(balanceLabel, BorderLayout.NORTH)
        add(JBScrollPane(itemsPanel), BorderLayout.CENTER)
    }

    fun refresh() {
        val state = PetState.getInstance()
        balanceLabel.text = "🪙 DevCoins: ${state.devCoins}"

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

                if (owned) {
                    if (equipped) {
                        add(JBLabel("  ✅ Equipped"))
                        add(JButton("Unequip").apply {
                            addActionListener {
                                if (item.type == "hat") state.equippedHat = ""
                                else state.equippedBackground = ""
                                refresh()
                            }
                        })
                    } else {
                        add(JBLabel("  Owned"))
                        add(JButton("Equip").apply {
                            addActionListener {
                                if (item.type == "hat") state.equippedHat = item.id
                                else state.equippedBackground = item.id
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
}
