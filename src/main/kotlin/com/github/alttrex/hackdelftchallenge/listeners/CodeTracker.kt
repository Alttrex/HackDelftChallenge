package com.github.alttrex.hackdelftchallenge.listeners

import com.github.alttrex.hackdelftchallenge.achievements.Achievements
import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager

/**
 * Tracks code written in the editor and awards XP.
 *
 * XP rules implemented here:
 *  - Only non-empty, non-comment lines award XP.
 *  - Lines in src/test get a multiplier (reward writing tests).
 */
class CodeTracker : BulkAwareDocumentListener {

    companion object {
        // XP multiplier for lines written under a test source root
        private const val TEST_MULTIPLIER = 2

        // Compliments the pet says when it sees you writing tests.
        private val TEST_COMPLIMENTS = listOf(
            "Ooh, writing tests? You're the best!",
            "Tests are my favorite snack. Keep going!",
            "Quack yeah - that's some solid testing!",
            "Future-you will thank you for this.",
            "Green checks incoming. I'm so proud!",
            "A tested duck is a happy duck.",
            "This is how legends ship code.",
            "So responsible! Bugs fear you now.",
        )
    }

    override fun documentChangedNonBulk(event: DocumentEvent) {
        val newFragment = event.newFragment.toString()
        val insertedLength = newFragment.length

        // Ignore deletions or empty changes
        if (insertedLength == 0) return

        val state = PetState.getInstance()
        state.checkDayReset()

        val isTest = isTestFile(event)
        val multiplier = if (isTest) TEST_MULTIPLIER else 1
        val countedLines = countCodeLines(event, newFragment)
        if (countedLines > 0) {
            repeat(countedLines) { state.addCodeLine(multiplier) }
            val tag = if (multiplier > 1) " (test x$multiplier)" else ""
            log("✍️ $countedLines line(s) of code written$tag. Daily: ${state.dailyLinesWritten}/${state.dailyQuota}")
            // Compliment the developer for writing tests.
            if (isTest) state.say(TEST_COMPLIMENTS.random())
            Achievements.check(null)
        }
    }

    /**
     * Counts only non-empty, non-comment lines. When the user presses Enter we inspect
     * the line that was just completed in the document; for small multi-line inserts we
     * inspect each line of the fragment.
     */
    private fun countCodeLines(event: DocumentEvent, fragment: String): Int {
        val newlineCount = fragment.count { it == '\n' }
        if (newlineCount == 0) return 0

        // Common case: user typed content then pressed Enter (fragment is just "\n").
        if (fragment == "\n") {
            return try {
                val doc = event.document
                val lineIdx = doc.getLineNumber(event.offset)
                val lineStart = doc.getLineStartOffset(lineIdx)
                val completed = doc.getText(com.intellij.openapi.util.TextRange(lineStart, event.offset))
                if (isCountableCode(completed)) 1 else 0
            } catch (t: Throwable) {
                1 // fall back to counting the line
            }
        }

        // Small multi-line insert: count the countable lines in the fragment.
        return fragment.split('\n').count { isCountableCode(it) }
    }

    private fun isCountableCode(line: String): Boolean {
        val t = line.trim()
        if (t.isEmpty()) return false
        return !(t.startsWith("//") || t.startsWith("#") || t.startsWith("*") ||
                t.startsWith("/*") || t.startsWith("<!--"))
    }

    private fun isTestFile(event: DocumentEvent): Boolean {
        val file = FileDocumentManager.getInstance().getFile(event.document) ?: return false
        val path = file.path.replace('\\', '/').lowercase()
        if (path.contains("/test/") || path.contains("/tests/")) return true
        // Also recognize common test file naming conventions.
        val name = file.nameWithoutExtension
        return name.endsWith("Test", ignoreCase = true) ||
                name.endsWith("Tests", ignoreCase = true) ||
                name.endsWith("Spec", ignoreCase = true) ||
                name.startsWith("Test", ignoreCase = true)
    }

    /** Logs to both the run console (stdout) and the IDE log. */
    private fun log(message: String) {
        println("[DevPet] $message")
        thisLogger().info("DevPet: $message")
    }
}
