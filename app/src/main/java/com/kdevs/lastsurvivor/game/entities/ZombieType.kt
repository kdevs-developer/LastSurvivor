package com.kdevs.lastsurvivor.game.entities

/**
 * Static stats per zombie type. WaveManager scales health and speed by wave at spawn time.
 * Speeds are world units per second; the player walks at 260 and sprints at ~416.
 */
enum class ZombieType(
    val displayName: String,
    val maxHealth: Float,
    val speed: Float,
    val attackDamage: Float,
    val radius: Float,
    val attackCooldown: Float,   // seconds between swings
    val spawnWeight: Int,        // relative chance in the wave spawner
    val poisonDps: Float = 0f,   // applied to the player when a Toxic swing lands
    val poisonSeconds: Float = 0f,
    val unlockWave: Int = 1,     // first wave this type can appear in
    val coinReward: Int = 1,     // coins for a kill
    val dropChance: Float = 0.05f, // chance to drop a power-up on death (bosses always drop)
    val diamondChance: Float = 0f, // chance to drop a diamond on death (bosses drop GameConfig.BOSS_DIAMOND_DROPS)
    val isBoss: Boolean = false, // bosses are spawned by WaveManager on boss waves, never picked randomly
    val abilityInterval: Float = 0f // boss only: seconds between special abilities
) {
    WALKER("Walker", maxHealth = 40f, speed = 70f, attackDamage = 8f, radius = 26f, attackCooldown = 1.2f, spawnWeight = 50, unlockWave = 1, coinReward = 1, dropChance = 0.05f, diamondChance = 0.03f),
    RUNNER("Runner", maxHealth = 70f, speed = 170f, attackDamage = 10f, radius = 24f, attackCooldown = 0.9f, spawnWeight = 25, unlockWave = 3, coinReward = 2, dropChance = 0.07f, diamondChance = 0.04f),
    TANK("Tank", maxHealth = 300f, speed = 55f, attackDamage = 25f, radius = 40f, attackCooldown = 1.8f, spawnWeight = 10, unlockWave = 7, coinReward = 5, dropChance = 0.25f, diamondChance = 0.15f),
    TOXIC("Toxic", maxHealth = 90f, speed = 100f, attackDamage = 6f, radius = 28f, attackCooldown = 1.2f, spawnWeight = 15, poisonDps = 5f, poisonSeconds = 4f, unlockWave = 5, coinReward = 3, dropChance = 0.10f, diamondChance = 0.06f),

    // ---- Phase 8 bosses (appear every GameConfig.BOSS_WAVE_INTERVAL waves, alternating) ----
    /** Huge and slow. Every few seconds it stops, flashes red, then charges in a straight line. */
    BRUTE("Brute", maxHealth = 1200f, speed = 80f, attackDamage = 30f, radius = 70f, attackCooldown = 1.5f, spawnWeight = 0, unlockWave = Int.MAX_VALUE, coinReward = 60, isBoss = true, abilityInterval = 5f),
    /** Walks at the player while summoning a handful of Walkers around itself on a timer. */
    MOTHER("Brood Mother", maxHealth = 900f, speed = 65f, attackDamage = 20f, radius = 62f, attackCooldown = 1.4f, spawnWeight = 0, unlockWave = Int.MAX_VALUE, coinReward = 80, isBoss = true, abilityInterval = 7f)
}
