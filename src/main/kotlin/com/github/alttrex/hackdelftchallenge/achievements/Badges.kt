package com.github.alttrex.hackdelftchallenge.achievements

/**
 * Equippable badges earned by unlocking specific [Achievement]s.
 * When equipped, the badge emoji is shown next to the pet's name.
 */
enum class Badge(
    val id: String,
    val title: String,
    val emoji: String,
    val achievementId: String,
) {
    CENTURION("centurion", "Centurion", "\u2694\uFE0F", Achievement.CENTURION.id),
    BUILD_MASTER("build_master", "Build Master", "\uD83D\uDD28", Achievement.BUILD_MASTER.id),
    RUBBER_DUCKY("rubber_ducky", "Rubber Ducky", "\uD83E\uDD86", Achievement.RUBBER_DUCKY.id);

    companion object {
        fun fromId(id: String): Badge? = entries.find { it.id == id }

        fun forAchievement(achievementId: String): Badge? =
            entries.find { it.achievementId == achievementId }
    }
}
