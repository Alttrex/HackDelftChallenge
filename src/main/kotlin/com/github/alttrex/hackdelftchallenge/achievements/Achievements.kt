package com.github.alttrex.hackdelftchallenge.achievements

import com.github.alttrex.hackdelftchallenge.state.EvolutionStage
import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

/**
 * Catalogue of unlockable achievements. Each one is a predicate over [PetState];
 * [Achievements.check] evaluates them and fires a toast for any newly satisfied one.
 */
enum class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val reward: Int,
    val predicate: (PetState) -> Boolean,
) {
    FIRST_LINE("first_line", "Hello, World!", "Wrote your first line of code.", 10,
        { it.totalLinesWritten >= 1 }),
    CENTURION("centurion", "Centurion", "Wrote 100 lines of code in total.", 50,
        { it.totalLinesWritten >= 100 }),
    QUOTA_MET("quota_met", "Daily Grind", "Met your daily LOC quota.", 25,
        { it.dailyLinesWritten >= it.dailyQuota }),
    DOCUMENTER("documenter", "Well Documented", "Wrote 50 lines of documentation.", 40,
        { it.totalJavadocLinesWritten >= 50 }),
    TESTER("tester", "Test Driven", "Wrote 50 lines of test code.", 60,
        { it.totalTestLinesWritten >= 50 }),
    DUCKLING("evolve_duckling", "It Hatched!", "Your duck evolved into a duckling.", 20,
        { it.getCurrentStage() == EvolutionStage.BABY || laterThanEgg(it) }),
    DUCK("evolve_duck", "Full-Grown Duck", "Your duck reached the Duck stage.", 40,
        { it.getCurrentStage() == EvolutionStage.TEEN || it.getCurrentStage() == EvolutionStage.ADULT }),
    RUBBER_DUCKY("evolve_rubber", "Rubber Ducky", "Reached the final Rubber Ducky form!", 100,
        { it.getCurrentStage() == EvolutionStage.ADULT }),
    BUILD_MASTER("build_master", "Build Master", "Completed 10 successful builds.", 60,
        { it.totalSuccessfulBuilds >= 10 }),
    RICH("rich", "Coin Collector", "Saved up 100 DevCoins.", 30,
        { it.devCoins >= 100 });

    companion object {
        private fun laterThanEgg(s: PetState): Boolean =
            s.getCurrentStage() != EvolutionStage.EGG
    }
}

object Achievements {

    private const val GROUP_ID = "DevPet Notifications"

    /**
     * Evaluate all achievements; for each newly unlocked one, persist it and show a toast.
     * Safe to call frequently (e.g. after typing, building, or scanning).
     */
    fun check(project: Project?) {
        val state = PetState.getInstance()
        state.syncBadgesFromAchievements()
        for (achievement in Achievement.entries) {
            if (achievement.id in state.unlockedAchievements) continue
            if (!achievement.predicate(state)) continue

            state.unlockedAchievements.add(achievement.id)
            Badge.forAchievement(achievement.id)?.let { state.unlockBadge(it.id) }
            state.addCoins(achievement.reward)
            notify(project, achievement)
        }
    }

    private fun notify(project: Project?, achievement: Achievement) {
        val message = "🏆 ${achievement.title} — ${achievement.description} (+${achievement.reward} DevCoins)"
        println("[DevPet] Achievement unlocked: ${achievement.title}")
        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification("Achievement Unlocked!", message, NotificationType.INFORMATION)
                .notify(project)
        } catch (t: Throwable) {
            // Notification group may be unavailable in some headless/test contexts.
            thisLogger().info("DevPet achievement (no toast): $message")
        }
    }
}
