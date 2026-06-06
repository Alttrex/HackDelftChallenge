package com.github.alttrex.hackdelftchallenge.listeners

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Registers the [CodeTracker] document listener on project startup.
 */
class CodeTrackerRegistrar : ProjectActivity {

    override suspend fun execute(project: Project) {
        val tracker = CodeTracker()
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(tracker, project)
    }
}
