package com.kdevs.lastsurvivor.game.entities

/**
 * Static stats per zombie type. Wave scaling (Phase 6) multiplies these at spawn time.
 * Speeds are world units per second; the player walks at 260 and sprints at ~416.
 */
enum class ZombieType(
    val displayName: String,
    val maxHealth: Float,
    val speed: Float,
    val attackDamage: Float,
    val radius: Float,
    val attackCooldown: Float,   // seconds between swings
    val spawnWeight: Int,        // relative chance in the temporary spawner
    val poisonDps: Float = 0f,   // applied by Phase 6 when a Toxic swing lands
    val poisonSeconds: Float = 0f
) {
    WALKER("Walker", maxHealth = 40f, speed = 70f, attackDamage = 8f, radius = 26f, attackCooldown = 1.2f, spawnWeight = 50),
    RUNNER("Runner", maxHealth = 70f, speed = 170f, attackDamage = 10f, radius = 24f, attackCooldown = 0.9f, spawnWeight = 25),
    TANK("Tank", maxHealth = 300f, speed = 55f, attackDamage = 25f, radius = 40f, attackCooldown = 1.8f, spawnWeight = 10),
    TOXIC("Toxic", maxHealth = 90f, speed = 100f, attackDamage = 6f, radius = 28f, attackCooldown = 1.2f, spawnWeight = 15, poisonDps = 5f, poisonSeconds = 4f)
}
