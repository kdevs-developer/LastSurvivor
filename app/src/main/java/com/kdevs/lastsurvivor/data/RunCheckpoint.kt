package com.kdevs.lastsurvivor.data

import com.kdevs.lastsurvivor.game.engine.RunSetup

/**
 * The state the Continue button restores: the start of a wave, and what is being played (a campaign
 * level or an endless area). Saved every time a new wave begins counting down. Stored as one string so
 * it is written in a single DataStore transaction. Old "v1" checkpoints (no setup) load as endless in the Ruined City.
 */
data class RunCheckpoint(
    val wave: Int,
    val health: Int,
    val grenades: Int,
    val setup: RunSetup = RunSetup.DEFAULT
) {

    fun encode(): String = "$V2|$wave|$health|$grenades|${setup.encode()}"

    companion object {
        private const val V1 = "v1"
        private const val V2 = "v2"

        /** Returns null for anything that is not a valid checkpoint (missing, unknown format, damaged). */
        fun decode(text: String?): RunCheckpoint? {
            if (text.isNullOrEmpty()) return null
            val parts = text.split('|')
            val setup = when {
                parts[0] == V1 && parts.size == 4 -> RunSetup.DEFAULT
                parts[0] == V2 && parts.size == 5 -> RunSetup.decode(parts[4]) ?: return null
                else -> return null
            }
            val wave = parts[1].toIntOrNull() ?: return null
            val health = parts[2].toIntOrNull() ?: return null
            val grenades = parts[3].toIntOrNull() ?: return null
            if (wave < 1 || health < 1 || grenades < 0) return null
            return RunCheckpoint(wave, health, grenades, setup)
        }
    }
}
