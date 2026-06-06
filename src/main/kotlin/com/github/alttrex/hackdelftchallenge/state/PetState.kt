package com.github.alttrex.hackdelftchallenge.state

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.time.LocalDate

enum class EvolutionStage(val displayName: String, val ascii: String) {
    EGG("Egg", """
        ______
       /      \
      |        |
      |  ~~~~  |
       \______/
    """.trimIndent()),
    BABY("Duckling", """
        _
       (o>
       //)
      v_/_
    """.trimIndent()),
    TEEN("Duck", """
         __
       <(o )___
        ( ._> /
         `---'
    """.trimIndent()),
    ADULT("Rubber Ducky", """
          __
        <(o )___
         ( ._> /  squeak!
          `---'
        ~~~~~~~~~~
    """.trimIndent());

    companion object {
        fun forLevel(level: Int): EvolutionStage = when {
            level < 3 -> EGG
            level < 7 -> BABY
            level < 15 -> TEEN
            else -> ADULT
        }
    }
}

enum class PetMood(val displayName: String, val emoji: String) {
    HAPPY("Happy", "😊"),
    NEUTRAL("Neutral", "😐"),
    HUNGRY("Hungry", "😟"),
    SICK("Sick", "🤢"),
    EATING("Eating", "😋"),
    BUILDING("Building", "🔨"),
    CONCUSSED("Concussed", "😵");
}

data class Cosmetic(
    var id: String = "",
    var name: String = "",
    var cost: Int = 0,
    var type: String = "hat" // hat, background
)

@Service(Service.Level.APP)
@State(name = "DevPetState", storages = [Storage("devpet.xml")])
class PetState : PersistentStateComponent<PetState> {

    var petName: String = "DevPet"
    var level: Int = 1
    var xp: Int = 0
    var xpToNextLevel: Int = 100
    var devCoins: Int = 0

    var dailyQuota: Int = 50 // lines of code goal
    var dailyLinesWritten: Int = 0
    var lastActiveDate: String = LocalDate.now().toString()

    var mood: String = PetMood.NEUTRAL.name
    var stage: String = EvolutionStage.EGG.name

    var totalLinesWritten: Int = 0
    var totalBuilds: Int = 0
    var totalSuccessfulBuilds: Int = 0

    var dailyJavadocLinesWritten: Int = 0
    var totalJavadocLinesWritten: Int = 0
    var dailyTestLinesWritten: Int = 0
    var totalTestLinesWritten: Int = 0

    var ownedCosmetics: MutableList<String> = mutableListOf()
    var equippedHat: String = ""
    var equippedBackground: String = ""

    // Coin generation tracking
    var lastCoinGenerationTime: Long = System.currentTimeMillis()

    /** Genuine human lines typed while sick; once it hits the threshold the pet heals. */
    var healingLines: Int = 0

    // ── Speech bubble (transient compliments/reactions) ──────────────────────
    var speechMessage: String = ""
    var speechUntil: Long = 0

    // ── Achievements ────────────────────────────────────────────────────────
    var unlockedAchievements: MutableList<String> = mutableListOf()

    // ── Change notification (lets the UI update instantly instead of polling) ─
    // Not a bean property (private, no accessor), so it is never serialized.
    private val changeListeners = mutableListOf<() -> Unit>()

    fun addChangeListener(listener: () -> Unit) {
        changeListeners.add(listener)
    }

    fun removeChangeListener(listener: () -> Unit) {
        changeListeners.remove(listener)
    }

    private fun notifyChanged() {
        if (changeListeners.isEmpty()) return
        changeListeners.toList().forEach { runCatching { it() } }
    }

    fun getCurrentStage(): EvolutionStage = try {
        EvolutionStage.valueOf(stage)
    } catch (_: Exception) {
        EvolutionStage.EGG
    }

    fun getCurrentMood(): PetMood = try {
        PetMood.valueOf(mood)
    } catch (_: Exception) {
        PetMood.NEUTRAL
    }

    fun addXp(amount: Int) {
        if (amount <= 0) return
        xp += amount
        while (xp >= xpToNextLevel) {
            xp -= xpToNextLevel
            level++
            xpToNextLevel = (xpToNextLevel * 1.5).toInt()
            stage = EvolutionStage.forLevel(level).name
        }
    }

    fun addLine() {
        checkDayReset()
        dailyLinesWritten++
        totalLinesWritten++
        addXp(2)
        updateMood()
    }

    /** A line of Javadoc documentation — rewarded more than plain code. */
    fun addJavadocLine() {
        checkDayReset()
        dailyLinesWritten++
        totalLinesWritten++
        dailyJavadocLinesWritten++
        totalJavadocLinesWritten++
        addXp(3)
        updateMood()
    }

    /** A line of test code — rewarded the most to encourage testing. */
    fun addTestLine() {
        checkDayReset()
        dailyLinesWritten++
        totalLinesWritten++
        dailyTestLinesWritten++
        totalTestLinesWritten++
        addXp(5)
        updateMood()
    }

    fun recordBuild(success: Boolean) {
        totalBuilds++
        if (success) {
            totalSuccessfulBuilds++
            addXp(25)
            addCoins(10) // reward a successful build
        }
        notifyChanged()
    }

    fun addCoins(amount: Int) {
        if (amount > 0) {
            devCoins += amount
            notifyChanged()
        }
    }

    /** Make the pet "say" something for [durationMs]; shown as a speech bubble in the UI. */
    fun say(message: String, durationMs: Long = 4500L) {
        speechMessage = message
        speechUntil = System.currentTimeMillis() + durationMs
        notifyChanged()
    }

    /** The currently active speech text, or empty if nothing is being said right now. */
    fun currentSpeech(): String =
        if (System.currentTimeMillis() < speechUntil) speechMessage else ""

    /**
     * Awards XP/lines for a single line of genuine code.`
     * [testMultiplier] should be >1 for files under src/test to reward test writing.
     */
    fun addCodeLine(testMultiplier: Int = 1) {
        checkDayReset()
        dailyLinesWritten++
        totalLinesWritten++
        addXp(2 * testMultiplier)

        // Nurse the pet back to health: writing genuine code heals sickness.
        if (getCurrentMood() == PetMood.SICK) {
            healingLines += testMultiplier
            if (healingLines >= SICK_RECOVERY_LINES) {
                healingLines = 0
                mood = PetMood.NEUTRAL.name // recovered; updateMood() refines below
                println("[DevPet] 💚 Nursed back to health!")
            }
        }

        updateMood()
    }

    fun generateCoins(): Int {
        checkDayReset()
        if (dailyLinesWritten < dailyQuota) return 0
        val now = System.currentTimeMillis()
        val elapsed = now - lastCoinGenerationTime
        // Generate 1 coin per minute when quota is met and pet is happy
        val coins = (elapsed / 60_000).toInt()
        if (coins > 0) {
            devCoins += coins
            lastCoinGenerationTime = now
        }
        return coins
    }

    fun checkDayReset() {
        val today = LocalDate.now().toString()
        if (lastActiveDate != today) {
            dailyLinesWritten = 0
            dailyJavadocLinesWritten = 0
            dailyTestLinesWritten = 0
            lastActiveDate = today
            lastCoinGenerationTime = System.currentTimeMillis()
        }
    }

    private fun updateMood() {
        val current = getCurrentMood()

        // Sickness/concussion is NOT cleared by mood recompute; it only lifts once the
        // pet has been nursed with enough genuine human lines (see addCodeLine).
        mood = when {
            current == PetMood.SICK -> PetMood.SICK.name
            current == PetMood.CONCUSSED -> PetMood.CONCUSSED.name
            current == PetMood.EATING -> PetMood.EATING.name
            current == PetMood.BUILDING -> PetMood.BUILDING.name
            dailyLinesWritten >= dailyQuota -> PetMood.HAPPY.name
            dailyLinesWritten >= dailyQuota / 2 -> PetMood.NEUTRAL.name
            else -> PetMood.HUNGRY.name
        }
        notifyChanged()
    }

    fun makeSick() {
        mood = PetMood.SICK.name
        healingLines = 0
        notifyChanged()
    }

    fun setEating() {
        mood = PetMood.EATING.name
        notifyChanged()
    }

    fun setBuilding() {
        mood = PetMood.BUILDING.name
        notifyChanged()
    }

    fun resetMoodFromAnimation() {
        mood = when {
            dailyLinesWritten >= dailyQuota -> PetMood.HAPPY.name
            dailyLinesWritten >= dailyQuota / 2 -> PetMood.NEUTRAL.name
            else -> PetMood.HUNGRY.name
        }
        notifyChanged()
    }

    override fun getState(): PetState = this

    override fun loadState(state: PetState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        // Number of lines needed to nurse a sick pet back to health.
        const val SICK_RECOVERY_LINES = 10

        fun getInstance(): PetState =
            ApplicationManager.getApplication().getService(PetState::class.java)

        val SHOP_ITEMS = listOf(
            Cosmetic("top_hat", "Top Hat", 50, "hat"),
            Cosmetic("crown", "Crown", 100, "hat"),
            Cosmetic("party_hat", "Party Hat", 30, "hat"),
            Cosmetic("wizard_hat", "Wizard Hat", 75, "hat"),
            Cosmetic("bg_space", "Space Background", 80, "background"),
            Cosmetic("bg_forest", "Forest Background", 60, "background"),
            Cosmetic("bg_ocean", "Ocean Background", 70, "background"),
        )
    }
}
