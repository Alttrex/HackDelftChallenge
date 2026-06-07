package com.github.alttrex.hackdelftchallenge.toolWindow

import com.github.alttrex.hackdelftchallenge.state.Cosmetic
import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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
        font = font.deriveFont(Font.BOLD, 14f)
    }

    // Two-column grid of compact item cards.
    private val itemsPanel = JPanel(GridLayout(0, 2, 6, 6))

    // Live preview of the pet wearing a selected/equipped cosmetic.
    private val previewRenderer = PetRenderer().apply {
        preferredSize = Dimension(JBUI.scale(130), JBUI.scale(110))
    }

    private val previewLabel = JBLabel("Preview").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.ITALIC, 11f)
    }

    // When the user clicks an item to preview, stop snapping back to equipped cosmetics.
    private var previewOverride = false

    // Icons are expensive to render, so build each one once and reuse it across refreshes.
    private val iconCache = HashMap<String, CosmeticIcon>()

    init {
        border = JBUI.Borders.empty(8)
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

        // Keep the grid top-aligned (don't stretch rows to fill height).
        val gridWrapper = JPanel(BorderLayout()).apply { add(itemsPanel, BorderLayout.NORTH) }
        add(JBScrollPane(gridWrapper).apply { border = null }, BorderLayout.CENTER)
    }

    fun refresh() {
        val state = PetState.getInstance()
        balanceLabel.text = "🪙 DevCoins: ${state.devCoins}"

        previewRenderer.stage = state.getCurrentStage()
        previewRenderer.mood = state.getCurrentMood()
        if (!previewOverride) {
            previewRenderer.equippedHat = state.equippedHat
            previewRenderer.equippedBackground = state.equippedBackground
            previewLabel.text = "Preview"
        }

        itemsPanel.removeAll()
        for (item in PetState.SHOP_ITEMS) {
            itemsPanel.add(cardFor(item, state))
        }
        itemsPanel.revalidate()
        itemsPanel.repaint()
    }

    private fun cardFor(item: Cosmetic, state: PetState): JPanel {
        val owned = item.id in state.ownedCosmetics
        val equipped = (item.type == "hat" && state.equippedHat == item.id) ||
                (item.type == "background" && state.equippedBackground == item.id)

        // Click the thumbnail to preview the cosmetic on the live pet (cached; built once).
        val icon = iconCache.getOrPut(item.id) {
            CosmeticIcon(item).apply {
                toolTipText = "Click to preview ${item.name}"
                cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        previewOverride = true
                        if (item.type == "hat") previewRenderer.equippedHat = item.id
                        else previewRenderer.equippedBackground = item.id
                        previewLabel.text = "Preview: ${item.name}"
                    }
                })
            }
        }

        val nameLabel = JBLabel(item.name).apply { font = font.deriveFont(Font.BOLD, 11f) }

        val statusLabel = JBLabel(
            when {
                equipped -> "✅ Equipped"
                owned -> "Owned"
                else -> "💰 ${item.cost}"
            }
        ).apply {
            font = font.deriveFont(10f)
            if (!owned) foreground = if (state.devCoins >= item.cost)
                JBColor(0x2E7D32, 0x66BB6A) else JBColor.GRAY
        }

        val actionButton = JButton().apply {
            margin = JBUI.insets(1, 6)
            font = font.deriveFont(10f)
            when {
                equipped -> {
                    text = "Unequip"
                    addActionListener {
                        if (item.type == "hat") state.equippedHat = "" else state.equippedBackground = ""
                        previewOverride = false
                        refresh()
                    }
                }
                owned -> {
                    text = "Equip"
                    addActionListener {
                        if (item.type == "hat") state.equippedHat = item.id
                        else state.equippedBackground = item.id
                        previewOverride = false
                        refresh()
                    }
                }
                else -> {
                    text = "Buy"
                    isEnabled = state.devCoins >= item.cost
                    addActionListener {
                        if (state.devCoins >= item.cost) {
                            state.devCoins -= item.cost
                            state.ownedCosmetics.add(item.id)
                            refresh()
                        } else {
                            JOptionPane.showMessageDialog(
                                this@ShopPanel, "Not enough DevCoins!", "Shop", JOptionPane.WARNING_MESSAGE
                            )
                        }
                    }
                }
            }
        }

        val info = JPanel().apply {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(nameLabel)
            add(statusLabel)
            add(actionButton)
        }

        return JPanel(BorderLayout(6, 0)).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border()),
                JBUI.Borders.empty(4)
            )
            add(icon, BorderLayout.WEST)
            add(info, BorderLayout.CENTER)
        }
    }

    private fun startRefreshTimer() {
        Timer("DevPet-Shop-Refresh", true).scheduleAtFixedRate(0L, 2000L) {
            SwingUtilities.invokeLater { refresh() }
        }
    }
}
