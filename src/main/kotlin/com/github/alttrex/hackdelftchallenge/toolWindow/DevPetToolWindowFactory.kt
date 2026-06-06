package com.github.alttrex.hackdelftchallenge.toolWindow

import com.github.alttrex.hackdelftchallenge.DevPetConfig
import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class DevPetToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val factory = ContentFactory.getInstance()

        val petPanel = DevPetPanel()
        val content = factory.createContent(petPanel, "Pet", false)
        toolWindow.contentManager.addContent(content)

        val shopPanel = ShopPanel()
        val shopContent = factory.createContent(shopPanel, "Shop", false)
        toolWindow.contentManager.addContent(shopContent)

        // Let the Pet panel's "Open Shop" button switch to the Shop tab.
        petPanel.onOpenShop = { toolWindow.contentManager.setSelectedContent(shopContent) }

        val settingsPanel = SettingsPanel()
        val settingsContent = factory.createContent(settingsPanel, "Settings", false)
        toolWindow.contentManager.addContent(settingsContent)

        if (DevPetConfig.DEV_MODE) {
            val devPanel = DevPanel()
            val devContent = factory.createContent(devPanel, "Dev", false)
            toolWindow.contentManager.addContent(devContent)
        }
    }

    override fun shouldBeAvailable(project: Project) = true
}
