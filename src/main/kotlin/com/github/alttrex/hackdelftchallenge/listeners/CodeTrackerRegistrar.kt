package com.github.alttrex.hackdelftchallenge.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer

/**
 * Registers the [CodeTracker] document listener on project startup.
 */
class CodeTrackerRegistrar : ProjectActivity {

    override suspend fun execute(project: Project) {
        val tracker = CodeTracker()
        tracker.project = project
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(tracker, project)
        tracker.start()

        // Stop the timer when the project is disposed
        Disposer.register(project as Disposable) {
            tracker.stop()
        }
    }
}
