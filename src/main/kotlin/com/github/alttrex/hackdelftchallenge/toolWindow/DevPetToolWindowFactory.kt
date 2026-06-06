package com.github.alttrex.hackdelftchallenge.toolWindow

import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class DevPetToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val petPanel = DevPetPanel()
        val content = ContentFactory.getInstance().createContent(petPanel, "Pet", false)
        toolWindow.contentManager.addContent(content)

        val shopPanel = ShopPanel()
        val shopContent = ContentFactory.getInstance().createContent(shopPanel, "Shop", false)
        toolWindow.contentManager.addContent(shopContent)

        val settingsPanel = SettingsPanel()
        val settingsContent = ContentFactory.getInstance().createContent(settingsPanel, "Settings", false)
        toolWindow.contentManager.addContent(settingsContent)
    }

    override fun shouldBeAvailable(project: Project) = true
}
