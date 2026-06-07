package com.github.alttrex.hackdelftchallenge.listeners

import com.github.alttrex.hackdelftchallenge.achievements.Achievements
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
import com.puppycrawl.tools.checkstyle.Checker
import com.puppycrawl.tools.checkstyle.ConfigurationLoader
import com.puppycrawl.tools.checkstyle.PropertiesExpander
import com.puppycrawl.tools.checkstyle.api.AuditEvent
import com.puppycrawl.tools.checkstyle.api.AuditListener
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.xml.sax.InputSource
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Properties
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

    // ── Cleanliness (Checkstyle) tuning ─────────────────────────────────

    /** Number of Checkstyle violations that are forgiven before the score drops. */
    var cleanlinessFreeErrors: Int = 5

    /** Points removed from the cleanliness score for each violation beyond the free allowance. */
    var cleanlinessPenaltyPerError: Double = 2.0

    /** Checkstyle is comparatively expensive, so its result is cached for this long. */
    private val cleanlinessCacheMs: Long = 15_000L
    @Volatile
    private var cachedCleanliness: Double = 100.0
    @Volatile
    private var cachedCleanlinessAt: Long = 0

    // ── AI detection running totals ─────────────────────────────────────

    private var totalCharsInProject: Long = 0
    private var aiCharsInProject: Long = 0

    // Accumulated code snippets between flushes (used only for batched AI detection).
    private val pendingSnippets = mutableListOf<String>()
    private val lock = Any()

    // Dedup guard: a single Enter can fire several newline-bearing events (the "\n"
    // insert plus auto-indent/reformat). Remember the last completed line so we don't
    // count it more than once in quick succession.
    private var lastCountedLineKey: String? = null
    private var lastCountedAt: Long = 0
    private val countDedupWindowMs = 600L

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

        // Snippets are batched for AI detection (done on the flush timer).
        synchronized(lock) {
            totalCharsInProject += newFragment.length
            pendingSnippets.add(newFragment)
        }

        val isNewLine = newFragment.trimEnd{it == ' ' || it == '\t'}.endsWith("\n")
        if (!isNewLine) return

        val document = event.document
        val firstLine = document.getLineNumber(event.offset)
        val lastLine = document.getLineNumber(event.offset + newFragment.length)
        if (lastLine <= firstLine) return

        // Lines [firstLine, lastLine) were each terminated by an inserted newline and are
        // therefore "completed". The trailing partial line (lastLine) is still being edited.
        var completedLines = 0
        var firstCompletedText: String? = null
        for (lineNumber in firstLine until lastLine) {
            val start = document.getLineStartOffset(lineNumber)
            val end = document.getLineEndOffset(lineNumber)
            val text = document.getText(com.intellij.openapi.util.TextRange(start, end))
            if (text.isNotBlank()) {
                completedLines++    
                if (firstCompletedText == null) firstCompletedText = text
            }
        }
        if (completedLines == 0 || firstCompletedText == null) return

        // Guard against the same completion being rewarded twice when a single Enter fires
        // multiple newline-bearing events (e.g. reformat-on-enter).
        val lineKey = "${System.identityHashCode(document)}#$firstLine#$firstCompletedText"
        val now = System.currentTimeMillis()
        if (lineKey == lastCountedLineKey && now - lastCountedAt < countDedupWindowMs) return
        lastCountedLineKey = lineKey
        lastCountedAt = now

        // Award XP/lines immediately so the UI updates the moment a line is completed,
        // rather than waiting for the periodic flush.
        val state = PetState.getInstance()
        state.checkDayReset()
        when {
            isTestFile(event) -> {
                repeat(completedLines) { state.addTestLine() }
                state.say(TEST_COMPLIMENTS.random())
                log("🧪 $completedLines test line(s) written. Daily: ${state.dailyLinesWritten}/${state.dailyQuota}")
            }
            isDocLine(firstCompletedText) -> {
                repeat(completedLines) { state.addJavadocLine() }
                log("📝 $completedLines documentation line(s) written. Daily: ${state.dailyLinesWritten}/${state.dailyQuota}")
            }
            else -> {
                repeat(completedLines) { state.addCodeLine() }
                log("✍️ $completedLines line(s) of code written. Daily: ${state.dailyLinesWritten}/${state.dailyQuota}")
            }
        }
        Achievements.check(project)
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
     * Returns the code cleanliness score (0–100), derived from Checkstyle violations
     * in the project's Java files. The first [cleanlinessFreeErrors] violations are
     * forgiven; beyond that, each violation removes [cleanlinessPenaltyPerError] points.
     * 100 = clean (or no Java files), 0 = very messy.
     *
     * The Checkstyle run is comparatively expensive, so results are cached for
     * [cleanlinessCacheMs]. Must be called off the EDT (it reads files from disk).
     */
    fun getCleanlinessScore(): Double {
        val now = System.currentTimeMillis()
        if (now - cachedCleanlinessAt < cleanlinessCacheMs) return cachedCleanliness

        val score = computeCheckstyleCleanliness()
        cachedCleanliness = score
        cachedCleanlinessAt = now
        return score
    }

    /** Runs Checkstyle over the project's Java files and converts violations into a 0–100 score. */
    private fun computeCheckstyleCleanliness(): Double {
        val proj = project ?: return 100.0

        val javaFiles = ReadAction.compute<List<File>, Exception> {
            val result = mutableListOf<File>()
            ProjectFileIndex.getInstance(proj).iterateContent { vf ->
                if (!vf.isDirectory && vf.isInLocalFileSystem && vf.extension.equals("java", ignoreCase = true)) {
                    val file = File(vf.path)
                    if (file.isFile) result.add(file)
                }
                true
            }
            result
        }
        if (javaFiles.isEmpty()) return 100.0

        return try {
            val violations = runCheckstyle(javaFiles)
            val excess = (violations - cleanlinessFreeErrors).coerceAtLeast(0)
            val score = (100.0 - excess * cleanlinessPenaltyPerError).coerceIn(0.0, 100.0)
            log("🧹 Checkstyle: $violations violation(s) across ${javaFiles.size} Java file(s) " +
                "(first $cleanlinessFreeErrors forgiven) → cleanliness ${String.format("%.1f", score)}%")
            score
        } catch (t: Throwable) {
            thisLogger().warn("DevPet: Checkstyle run failed; defaulting cleanliness to 100", t)
            100.0
        }
    }

    /** Executes Checkstyle with the bundled config and returns the total violation count. */
    private fun runCheckstyle(files: List<File>): Int {
        val configStream = javaClass.getResourceAsStream("/checkstyle/devpet_checks.xml")
            ?: error("Bundled Checkstyle config not found on classpath")

        val config = configStream.use { stream ->
            ConfigurationLoader.loadConfiguration(
                InputSource(stream),
                PropertiesExpander(Properties()),
                ConfigurationLoader.IgnoredModulesOptions.EXECUTE,
            )
        }

        var violations = 0
        val listener = object : AuditListener {
            override fun auditStarted(event: AuditEvent?) {}
            override fun auditFinished(event: AuditEvent?) {}
            override fun fileStarted(event: AuditEvent?) {}
            override fun fileFinished(event: AuditEvent?) {}
            override fun addError(event: AuditEvent?) { violations++ }
            override fun addException(event: AuditEvent?, throwable: Throwable?) {}
        }

        val checker = Checker()
        try {
            checker.setModuleClassLoader(Checker::class.java.classLoader)
            checker.configure(config)
            checker.addListener(listener)
            checker.process(files)
        } finally {
            checker.destroy()
        }
        return violations
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

    /**
     * Periodic batched work (called by the timer): runs AI detection on the accumulated
     * snippets and updates the pet's health/sickness. Line/XP awarding is handled
     * immediately in [documentChangedNonBulk], not here.
     */
    private fun flush() {
        val snippets: List<String>
        synchronized(lock) {
            snippets = pendingSnippets.toList()
            pendingSnippets.clear()
        }

        if (snippets.isEmpty()) return

        // Run AI detection on accumulated snippets
        val combinedCode = snippets.joinToString("\n")
        val aiPercent = checkAiPercentage(combinedCode)
        val aiCharsInBatch = (combinedCode.length * aiPercent / 100).toLong()

        synchronized(lock) {
            aiCharsInProject += aiCharsInBatch
        }

        if (aiCharsInBatch > 0) {
            log("🤖 AI detection: ${String.format("%.1f", aiPercent)}% of batch (${aiCharsInBatch}/${combinedCode.length} chars) flagged as AI-generated.")
        }

        // Check if overall health score is below threshold
        val healthScore = getOverallHealthScore()
        if (healthScore < sickThresholdPercent) {
            PetState.getInstance().makeSick()
            log("🤢 Health score ${String.format("%.1f", healthScore)}% is below threshold ($sickThresholdPercent%) — pet is sick! " +
                "[AI: ${String.format("%.1f", getAiScore())}, Clean: ${String.format("%.1f", getCleanlinessScore())}, Tests: ${String.format("%.1f", getTestScore())}]")
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
