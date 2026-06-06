package com.github.alttrex.hackdelftchallenge.listeners

import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.editor.event.DocumentEvent

/**
 * Tracks human-typed code and filters out large pastes (suspected AI-generated code).
 * Implements anti-AI heuristic: if a document change inserts more than [PASTE_THRESHOLD]
 * characters at once, it's treated as a paste/AI-generated block and gives 0 XP (or makes pet sick).
 */
class CodeTracker : BulkAwareDocumentListener {

    companion object {
        // Max characters in a single insert to be considered "human typed"
        private const val PASTE_THRESHOLD = 80
        // Min new lines in a paste to trigger "sick" reaction
        private const val SICK_LINE_THRESHOLD = 5
    }

    override fun documentChangedNonBulk(event: DocumentEvent) {
        val newFragment = event.newFragment.toString()
        val insertedLength = newFragment.length

        // Ignore deletions or empty changes
        if (insertedLength == 0) return

        val state = PetState.getInstance()
        state.checkDayReset()

        // Anti-AI / Anti-paste heuristic
        if (insertedLength > PASTE_THRESHOLD) {
            val pastedLines = newFragment.count { it == '\n' }
            thisLogger().info("DevPet: Large paste detected ($insertedLength chars, $pastedLines lines) — no XP awarded")
            if (pastedLines >= SICK_LINE_THRESHOLD) {
                state.makeSick()
            }
            return
        }

        // Count new lines added by human typing
        val newLines = newFragment.count { it == '\n' }
        if (newLines > 0) {
            repeat(newLines) {
                state.addLine()
            }
            thisLogger().info("DevPet: Human typed $newLines new line(s). Daily: ${state.dailyLinesWritten}/${state.dailyQuota}")
        }
    }
}
