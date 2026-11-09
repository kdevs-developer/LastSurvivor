package com.kdevs.lastsurvivor.game.engine

/**
 * Minimal allocation-free pool. Used from Phase 5 for bullets and effects.
 * Single-threaded: only touch it from the game thread.
 */
class ObjectPool<T : Any>(private val factory: () -> T, initialSize: Int = 0) {
    private val free = ArrayDeque<T>()

    init { repeat(initialSize) { free.addLast(factory()) } }

    fun obtain(): T = if (free.isEmpty()) factory() else free.removeLast()

    fun release(item: T) { free.addLast(item) }
}
