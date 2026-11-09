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
    val coinReward: Int = 1      // coins for a kill
) {
    WALKER("Walker", maxHealth = 40f, speed = 70f, attackDamage = 8f, radius = 26f, attackCooldown = 1.2f, spawnWeight = 50, unlockWave = 1, coinReward = 1),
    RUNNER("Runner", maxHealth = 70f, speed = 170f, attackDamage = 10f, radius = 24f, attackCooldown = 0.9f, spawnWeight = 25, unlockWave = 3, coinReward = 2),
    TANK("Tank", maxHealth = 300f, speed = 55f, attackDamage = 25f, radius = 40f, attackCooldown = 1.8f, spawnWeight = 10, unlockWave = 7, coinReward = 5),
    TOXIC("Toxic", maxHealth = 90f, speed = 100f, attackDamage = 6f, radius = 28f, attackCooldown = 1.2f, spawnWeight = 15, poisonDps = 5f, poisonSeconds = 4f, unlockWave = 5, coinReward = 3)
}
