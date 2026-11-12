package com.kdevs.lastsurvivor.game.engine

enum class GameMode { CAMPAIGN, INFINITE }

/**
 * One campaign level: an [area], a fixed number of [waves] and a boss on the last wave. Later levels
 * start tougher: zombie count, health, speed and types follow the wave number plus
 * [difficultyOffset], while the HUD still counts 1..[waves].
 */
class CampaignLevel(val number: Int, val area: GameArea, val waves: Int) {
    val difficultyOffset: Int get() = (number - 1) * GameConfig.CAMPAIGN_OFFSET_PER_LEVEL
    val bonusCoins: Int get() = GameConfig.LEVEL_COMPLETE_COINS_PER_LEVEL * number
    val bonusDiamonds: Int get() = GameConfig.LEVEL_COMPLETE_DIAMONDS
}

object Campaign {
    val levels: List<CampaignLevel> = listOf(
        CampaignLevel(1, GameArea.CITY, 5),
        CampaignLevel(2, GameArea.CITY, 6),
        CampaignLevel(3, GameArea.SWAMP, 6),
        CampaignLevel(4, GameArea.SWAMP, 7),
        CampaignLevel(5, GameArea.DESERT, 7),
        CampaignLevel(6, GameArea.DESERT, 8),
        CampaignLevel(7, GameArea.TUNDRA, 8),
        CampaignLevel(8, GameArea.TUNDRA, 9),
        CampaignLevel(9, GameArea.GRAVEYARD, 9),
        CampaignLevel(10, GameArea.GRAVEYARD, 10)
    )

    val count: Int get() = levels.size

    /** The level with this number; out-of-range numbers are clamped to 1..[count]. */
    fun level(number: Int): CampaignLevel = levels[(number - 1).coerceIn(0, levels.lastIndex)]
}

/**
 * What a run is played on: a campaign level (its area is fixed by the level) or the infinite mode in a
 * chosen area. Stored in the Continue checkpoint and passed to the game screen as a short text ([encode]).
 */
data class RunSetup(val mode: GameMode, val level: Int, val infiniteArea: GameArea) {

    val isCampaign: Boolean get() = mode == GameMode.CAMPAIGN

    /** The campaign level being played, or null in the infinite mode. */
    val levelData: CampaignLevel? get() = if (isCampaign) Campaign.level(level) else null

    val area: GameArea get() = levelData?.area ?: infiniteArea

    /** Waves in the level; 0 means endless. */
    val totalWaves: Int get() = levelData?.waves ?: 0

    /** Makes every campaign level (and every infinite area) a different arrangement of props. */
    val layoutSeed: Int get() = if (isCampaign) 1000 + level * 37 else 7 + infiniteArea.ordinal * 101

    /** "C3" for campaign level 3, "I-SWAMP" for infinite in the swamp. No "|" and no "/", so it fits in a route and a checkpoint. */
    fun encode(): String = if (isCampaign) "C$level" else "I-${infiniteArea.name}"

    companion object {
        val DEFAULT = RunSetup(GameMode.INFINITE, 1, GameArea.CITY)

        fun campaign(level: Int) = RunSetup(GameMode.CAMPAIGN, level.coerceIn(1, Campaign.count), GameArea.CITY)

        fun infinite(area: GameArea) = RunSetup(GameMode.INFINITE, 1, area)

        /** Null for anything that is not a valid setup. */
        fun decode(text: String?): RunSetup? {
            if (text.isNullOrEmpty()) return null
            return when (text[0]) {
                'C' -> text.substring(1).toIntOrNull()?.takeIf { it in 1..Campaign.count }?.let { campaign(it) }
                'I' -> GameArea.entries.firstOrNull { "I-${it.name}" == text }?.let { infinite(it) }
                else -> null
            }
        }
    }
}
