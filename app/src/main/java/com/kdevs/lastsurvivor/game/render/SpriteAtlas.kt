package com.kdevs.lastsurvivor.game.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kdevs.lastsurvivor.game.effects.DecalField
import com.kdevs.lastsurvivor.game.entities.GameCharacter
import com.kdevs.lastsurvivor.game.entities.ZombieType
import com.kdevs.lastsurvivor.game.weapons.WeaponType
import java.io.IOException

/**
 * Every bitmap the renderer draws, built once at start-up. For each sprite it first looks for a
 * PNG in assets/sprites/ (see the README there for names and sizes) and falls back to the
 * generated art in [PlaceholderSprites]. So real artwork can be dropped in one file at a time,
 * with no code change.
 */
class SpriteAtlas(private val context: Context, private val colors: GameColors) {

    companion object {
        const val PLAYER_FRAMES = 3   // 0 standing, 1 and 2 walking steps
        const val ZOMBIE_FRAMES = 5   // 0..3 walk cycle, 4 attack
        const val PLAYER_SIZE = 96
        const val FLOOR_SIZE = 256
        const val DECAL_SIZE = 96
        const val DECOR_SIZE = 128
    }

    var floorTile: Bitmap = load("floor.png") ?: PlaceholderSprites.floorTile(FLOOR_SIZE, colors)
        private set

    /** The outfit the current [playerSprites] are drawn in. */
    var skin: GameCharacter = GameCharacter.SURVIVOR
        private set

    /** playerSprites[weapon.ordinal][frame], file player_<weapon>_<frame>.png (a PNG ignores the chosen outfit). */
    var playerSprites: Array<Array<Bitmap>> = buildPlayerSprites(GameCharacter.SURVIVOR)
        private set

    /** Redraws the player sprites in another outfit. Called on the game thread, only when the choice changes. */
    fun applySkin(newSkin: GameCharacter) {
        if (newSkin == skin) return
        playerSprites = buildPlayerSprites(newSkin)
        skin = newSkin
    }

    private fun buildPlayerSprites(forSkin: GameCharacter): Array<Array<Bitmap>> {
        val palette = colors.skinPalette(forSkin)
        return Array(WeaponType.entries.size) { w ->
            val type = WeaponType.entries[w]
            Array(PLAYER_FRAMES) { f ->
                load("player_${type.name.lowercase()}_$f.png") ?: PlaceholderSprites.player(PLAYER_SIZE, colors, palette, type, f)
            }
        }
    }

    /** zombieSprites[type.ordinal][frame], file zombie_<type>_<frame>.png */
    val zombieSprites: Array<Array<Bitmap>> = Array(ZombieType.entries.size) { t ->
        val type = ZombieType.entries[t]
        val size = (type.radius * 3.2f).toInt()
        Array(ZOMBIE_FRAMES) { f ->
            load("zombie_${type.name.lowercase()}_$f.png") ?: PlaceholderSprites.zombie(size, type, f, colors)
        }
    }

    val bloodDecals: Array<Bitmap> = Array(DecalField.BLOOD_VARIANTS) { v ->
        load("decal_blood_$v.png") ?: PlaceholderSprites.splatter(DECAL_SIZE, colors.decalBlood, 100 + v)
    }
    val gooDecals: Array<Bitmap> = Array(DecalField.GOO_VARIANTS) { v ->
        load("decal_goo_$v.png") ?: PlaceholderSprites.splatter(DECAL_SIZE, colors.decalGoo, 200 + v)
    }
    val scorchDecal: Bitmap = load("decal_scorch.png") ?: PlaceholderSprites.scorch(DECAL_SIZE, colors.decalScorch)

    var decor: Array<Bitmap> = buildDecor()
        private set

    private fun buildDecor(): Array<Bitmap> = Array(PlaceholderSprites.DECOR_KINDS) { k ->
        load("decor_$k.png") ?: PlaceholderSprites.decor(k, DECOR_SIZE, colors, 300 + k)
    }

    /** Redraws the generated floor and decoration after [GameColors.applyArea] changed the palette. PNG overrides stay as they are. */
    fun rebuildArea() {
        floorTile = load("floor.png") ?: PlaceholderSprites.floorTile(FLOOR_SIZE, colors)
        decor = buildDecor()
    }

    /** The decal bitmap for a [DecalField] entry. */
    fun decal(kind: Int, variant: Int): Bitmap = when (kind) {
        DecalField.BLOOD -> bloodDecals[variant % bloodDecals.size]
        DecalField.GOO -> gooDecals[variant % gooDecals.size]
        else -> scorchDecal
    }

    private fun load(name: String): Bitmap? = try {
        context.assets.open("sprites/$name").use { BitmapFactory.decodeStream(it) }
    } catch (e: IOException) {
        null
    }
}
