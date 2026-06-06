package com.github.alttrex.hackdelftchallenge.listeners

import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager

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

        // Comment / documentation line prefixes across common IntelliJ-supported languages.
        // Covers C-family (// /* * /** ///), scripting (#), SQL/Haskell/Lua (--),
        // markup (<!--), Python/Kotlin docstrings (""" '''), and Lisp/asm (;).
        private val DOC_PREFIXES = listOf(
            "/**", "/*", "*/", "*", "///", "//",
            "#", "--", "<!--", "-->", "\"\"\"", "'''", ";;", ";"
        )

        // Directory fragments that indicate a test source location.
        private val TEST_DIR_MARKERS = listOf(
            "/test/", "/tests/", "/__tests__/", "/spec/", "/specs/",
            "src/test", "/testing/", "/it/"
        )
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
            log("🤖 AI code detected — large paste of $insertedLength chars / $pastedLines line(s). No XP awarded.")
            if (pastedLines >= SICK_LINE_THRESHOLD) {
                state.makeSick()
            }
            return
        }

        // Count new lines added by human typing
        val newLines = newFragment.count { it == '\n' }
        if (newLines <= 0) return

        // Don't reward empty / whitespace-only lines (e.g. just pressing Enter).
        val completedLine = lineTextAtOffset(event)
        if (completedLine.isBlank()) return

        // Test files take precedence; otherwise comment/doc lines count as documentation.
        val isTestFile = isTestFile(event)
        val isDoc = !isTestFile && isDocLine(completedLine)

        repeat(newLines) {
            when {
                isTestFile -> {
                    state.addTestLine()
                    log("🧪 Test line written. Test daily: ${state.dailyTestLinesWritten} | Daily: ${state.dailyLinesWritten}/${state.dailyQuota}")
                }
                isDoc -> {
                    state.addJavadocLine()
                    log("📝 Doc line written. Javadoc daily: ${state.dailyJavadocLinesWritten} | Daily: ${state.dailyLinesWritten}/${state.dailyQuota}")
                }
                else -> {
                    state.addLine()
                    log("✍️ Line of code written. Daily: ${state.dailyLinesWritten}/${state.dailyQuota}")
                }
            }
        }
    }

    /** Resolves the [DocumentEvent]'s backing file path, or null if unavailable. */
    private fun filePath(event: DocumentEvent): String? =
        FileDocumentManager.getInstance().getFile(event.document)?.path

    /**
     * Language-agnostic test detection: a file is considered a test if it lives under a
     * known test directory or its name follows a common test/spec naming convention
     * (e.g. FooTest.kt, foo_test.go, foo.spec.ts, TestFoo.py, FooSpec.scala).
     */
    private fun isTestFile(event: DocumentEvent): Boolean {
        val path = filePath(event)?.replace('\\', '/')?.lowercase() ?: return false
        if (TEST_DIR_MARKERS.any { path.contains(it) }) return true

        val fileName = path.substringAfterLast('/')
        val baseName = fileName.substringBeforeLast('.', fileName)
        return baseName.endsWith("test") ||
            baseName.endsWith("tests") ||
            baseName.endsWith("spec") ||
            baseName.endsWith("_test") ||
            baseName.endsWith("_spec") ||
            baseName.startsWith("test_") ||
            baseName.startsWith("test") && baseName.length > 4 ||
            fileName.contains(".test.") ||
            fileName.contains(".spec.")
    }

    /** Returns the full text of the line at the change offset. */
    private fun lineTextAtOffset(event: DocumentEvent): String {
        val document = event.document
        val lineNumber = document.getLineNumber(event.offset)
        val start = document.getLineStartOffset(lineNumber)
        val end = document.getLineEndOffset(lineNumber)
        return document.getText(com.intellij.openapi.util.TextRange(start, end))
    }

    /** True if the line is a comment / documentation line in any common language. */
    private fun isDocLine(lineText: String): Boolean {
        val trimmed = lineText.trim()
        if (trimmed.isEmpty()) return false
        return DOC_PREFIXES.any { trimmed.startsWith(it) }
    }

    /** Logs to both the run console (stdout) and the IDE log. */
    private fun log(message: String) {
        println("[DevPet] $message")
        thisLogger().info("DevPet: $message")
    }
}
