package com.github.alttrex.hackdelftchallenge.listeners

import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

/**
 * Tracks code changes and accumulates human-typed lines for XP.
 *
 * On each flush interval, accumulated code snippets are sent to the OpenAI API
 * to estimate what percentage is AI-generated. Multiple quality scores (AI usage,
 * cleanliness, test coverage) are combined into an overall health score. The pet
 * gets sick when the overall health score falls below [sickThresholdPercent].
 *
 * Document changes are accumulated and processed on a configurable timer interval
 * (see [trackingIntervalSeconds]) instead of reacting to every single keystroke.
 */
class CodeTracker : BulkAwareDocumentListener {

    companion object {
        @Volatile
        var instance: CodeTracker? = null
            private set

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

    var project: Project? = null

    /** How often (in seconds) accumulated document changes are processed. */
    var trackingIntervalSeconds: Long = 30

    /**
     * OpenAI API key. Read from the `DEVPET_OPENAI_API_KEY` environment variable.
     * If not set, AI detection is skipped (all code counts as human-written).
     */
    var openAiApiKey: String = System.getenv("DEVPET_OPENAI_API_KEY") ?: ""

    /** OpenAI model to use for AI detection. */
    var openAiModel: String = "gpt-4o-mini"

    // ── Overall health threshold ────────────────────────────────────────

    /**
     * When the overall health score (0–100) drops below this percentage,
     * the pet gets sick. The health score is computed from multiple factors
     * (AI usage, cleanliness, test coverage, etc.).
     */
    var sickThresholdPercent: Double = 50.0

    // ── Per-factor weights (must sum to 1.0) ────────────────────────────

    /** Weight of the AI-usage score in the overall health calculation. */
    var aiScoreWeight: Double = 0.4

    /** Weight of the cleanliness score in the overall health calculation. */
    var cleanlinessScoreWeight: Double = 0.3

    /** Weight of the test-coverage score in the overall health calculation. */
    var testScoreWeight: Double = 0.3

    // ── AI detection running totals ─────────────────────────────────────

    private var totalCharsInProject: Long = 0
    private var aiCharsInProject: Long = 0

    // Accumulated snippets and line counts between flushes
    private val pendingSnippets = mutableListOf<String>()
    private var pendingCodeLines = 0
    private var pendingDocLines = 0
    private var pendingTestLines = 0
    private val lock = Any()

    private var timer: Timer? = null
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    data class MethodCount(
        val sourceMethodCount: Int,
        val testMethodCount: Int,
    ) {
        val total get() = sourceMethodCount + testMethodCount
    }

    fun countMethods(): MethodCount {
        val proj = project ?: return MethodCount(0, 0)

        return ReadAction.compute<MethodCount, Exception> {
            var sourceMethods = 0
            var testMethods = 0
            val fileIndex = ProjectFileIndex.getInstance(proj)
            val psiManager = PsiManager.getInstance(proj)

            fileIndex.iterateContent { virtualFile ->
                val psiFile = psiManager.findFile(virtualFile)
                if (psiFile != null) {
                    val isTest = fileIndex.isInTestSourceContent(virtualFile)

                    // Count Java methods
                    val javaMethods = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethod::class.java).size
                    // Count Kotlin functions
                    val ktFunctions = PsiTreeUtil.findChildrenOfType(psiFile, KtNamedFunction::class.java).size
                    val count = javaMethods + ktFunctions

                    if (isTest) testMethods += count
                    else sourceMethods += count
                }
                true // continue iterating
            }

            MethodCount(sourceMethods, testMethods)
        }
    }

    /** Starts the periodic flush timer. Call once after registration. */
    fun start() {
        instance = this
        timer?.cancel()
        timer = fixedRateTimer("CodeTracker-flush", daemon = true, period = trackingIntervalSeconds * 1000) {
            flush()
        }
    }

    /** Stops the periodic flush timer and processes any remaining events. */
    fun stop() {
        timer?.cancel()
        timer = null
        flush()
    }

    override fun documentChangedNonBulk(event: DocumentEvent) {
        val newFragment = event.newFragment.toString()
        if (newFragment.isEmpty()) return

        val insertedLines = newFragment.count { it == '\n' }
        val completedLine = if (insertedLines > 0) lineTextAtOffset(event) else ""
        val rewardableLine = insertedLines > 0 && completedLine.isNotBlank()
        val isTestLine = rewardableLine && isTestFile(event)
        val isDocLine = rewardableLine && !isTestLine && isDocLine(completedLine)

        synchronized(lock) {
            totalCharsInProject += newFragment.length
            pendingSnippets.add(newFragment)

            if (rewardableLine) {
                when {
                    isTestLine -> pendingTestLines += insertedLines
                    isDocLine -> pendingDocLines += insertedLines
                    else -> pendingCodeLines += insertedLines
                }
            }
        }
    }

    /** Returns the current AI code percentage (0–100) in the project. */
    fun getAiPercent(): Double {
        synchronized(lock) {
            if (totalCharsInProject == 0L) return 0.0
            return (aiCharsInProject.toDouble() / totalCharsInProject) * 100
        }
    }

    /**
     * Returns the AI health score (0–100). 100 = no AI code, 0 = all AI code.
     * This is the inverse of [getAiPercent].
     */
    fun getAiScore(): Double = 100.0 - getAiPercent()

    /**
     * Returns the code cleanliness score (0–100).
     *
     * **Stub** — replace this with your own logic (e.g. linting, formatting checks).
     * 100 = perfectly clean, 0 = very messy.
     */
    fun getCleanlinessScore(): Double {
        // TODO: Implement cleanliness calculation
        return 100.0
    }

    /**
     * Returns the test coverage score (0–100).
     *
     * **Stub** — replace this with your own logic (e.g. ratio of test files to source files).
     * 100 = fully tested, 0 = no tests.
     */
    fun getTestScore(): Double {
        val count = countMethods()
        if (count.sourceMethodCount == 0) return 100.0

        val ratio = count.testMethodCount.toDouble() / count.sourceMethodCount.toDouble()

        return (ratio * 100.0).coerceIn(0.0, 100.0)
    }

    /**
     * Computes the overall health score (0–100) as a weighted average of
     * AI score, cleanliness score, and test score.
     * The pet gets sick when this drops below [sickThresholdPercent].
     */
    fun getOverallHealthScore(): Double {
        val ai = getAiScore() * aiScoreWeight
        val cleanliness = getCleanlinessScore() * cleanlinessScoreWeight
        val tests = getTestScore() * testScoreWeight
        return ai + cleanliness + tests
    }

    /**
     * Calls the OpenAI API to estimate what percentage (0–100) of [codeSnippet]
     * is AI-generated. Returns 0.0 if the API key is blank or the call fails.
     */
    fun checkAiPercentage(codeSnippet: String): Double {
        if (openAiApiKey.isBlank() || codeSnippet.isBlank()) return 0.0

        try {
            val escapedCode = escapeJson(codeSnippet)
            val requestBody = """
                {
                    "model": "$openAiModel",
                    "messages": [
                        {
                            "role": "system",
                            "content": "You are a code analysis tool. Given a code snippet, estimate what percentage (0-100) of it appears to be AI-generated vs human-written. Respond with ONLY a single number, nothing else."
                        },
                        {
                            "role": "user",
                            "content": "$escapedCode"
                        }
                    ],
                    "max_tokens": 10,
                    "temperature": 0
                }
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                log("⚠️ OpenAI API returned status ${response.statusCode()}: ${response.body().take(200)}")
                return 0.0
            }

            // Extract the content field from the JSON response
            val contentMatch = Regex(""""content"\s*:\s*"([^"]*?)"""").find(response.body())
            val content = contentMatch?.groupValues?.get(1)?.trim() ?: return 0.0
            val percent = content.toDoubleOrNull() ?: return 0.0
            return percent.coerceIn(0.0, 100.0)
        } catch (e: Exception) {
            log("⚠️ OpenAI API call failed: ${e.message}")
            return 0.0
        }
    }

    /** Flushes accumulated changes to [PetState]. Called by the timer. */
    private fun flush() {
        val snippets: List<String>
        val codeLines: Int
        val docLines: Int
        val testLines: Int
        synchronized(lock) {
            snippets = pendingSnippets.toList()
            codeLines = pendingCodeLines
            docLines = pendingDocLines
            testLines = pendingTestLines
            pendingSnippets.clear()
            pendingCodeLines = 0
            pendingDocLines = 0
            pendingTestLines = 0
        }

        if (snippets.isEmpty() && codeLines == 0 && docLines == 0 && testLines == 0) return

        // Run AI detection on accumulated snippets
        if (snippets.isNotEmpty()) {
            val combinedCode = snippets.joinToString("\n")
            val aiPercent = checkAiPercentage(combinedCode)
            val aiCharsInBatch = (combinedCode.length * aiPercent / 100).toLong()

            synchronized(lock) {
                aiCharsInProject += aiCharsInBatch
            }

            if (aiCharsInBatch > 0) {
                log("🤖 AI detection: ${String.format("%.1f", aiPercent)}% of batch (${aiCharsInBatch}/${combinedCode.length} chars) flagged as AI-generated.")
            }
        }

        val state = PetState.getInstance()
        state.checkDayReset()

        // Check if overall health score is below threshold
        val healthScore = getOverallHealthScore()
        if (healthScore < sickThresholdPercent) {
            state.makeSick()
            log("🤢 Health score ${String.format("%.1f", healthScore)}% is below threshold ($sickThresholdPercent%) — pet is sick! " +
                "[AI: ${String.format("%.1f", getAiScore())}, Clean: ${String.format("%.1f", getCleanlinessScore())}, Tests: ${String.format("%.1f", getTestScore())}]")
        }

        if (codeLines > 0) {
            repeat(codeLines) { state.addLine() }
            log("✍️ $codeLines line(s) of code written. Daily: ${state.dailyLinesWritten}/${state.dailyQuota}")
        }

        if (docLines > 0) {
            repeat(docLines) { state.addJavadocLine() }
            log("📝 $docLines documentation line(s) written. Daily: ${state.dailyLinesWritten}/${state.dailyQuota}")
        }

        if (testLines > 0) {
            repeat(testLines) { state.addTestLine() }
            log("🧪 $testLines test line(s) written. Daily: ${state.dailyLinesWritten}/${state.dailyQuota}")
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

    /** Escapes a string for safe inclusion in a JSON string value. */
    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /** Logs to both the run console (stdout) and the IDE log. */
    private fun log(message: String) {
        println("[DevPet] $message")
        thisLogger().info("DevPet: $message")
    }
}
