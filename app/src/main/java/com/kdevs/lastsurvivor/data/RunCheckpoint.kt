package com.kdevs.lastsurvivor.data

/**
 * The state the Continue button restores: the start of a wave. Saved every time a new wave begins
 * counting down. Stored as one string so it is written in a single DataStore transaction.
 */
data class RunCheckpoint(val wave: Int, val health: Int, val grenades: Int) {

    fun encode(): String = "$VERSION|$wave|$health|$grenades"

    companion object {
        private const val VERSION = "v1"

        /** Returns null for anything that is not a valid checkpoint (missing, old format, damaged). */
        fun decode(text: String?): RunCheckpoint? {
            if (text.isNullOrEmpty()) return null
            val parts = text.split('|')
            if (parts.size != 4 || parts[0] != VERSION) return null
            val wave = parts[1].toIntOrNull() ?: return null
            val health = parts[2].toIntOrNull() ?: return null
            val grenades = parts[3].toIntOrNull() ?: return null
            if (wave < 1 || health < 1 || grenades < 0) return null
            return RunCheckpoint(wave, health, grenades)
        }
    }
}
