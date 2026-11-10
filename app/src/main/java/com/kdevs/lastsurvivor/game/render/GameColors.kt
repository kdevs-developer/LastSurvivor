package com.kdevs.lastsurvivor.game.render

import android.content.Context
import androidx.annotation.ColorRes
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.entities.CharacterSkin

/** The four colours that change between outfits. */
class SkinPalette(val body: Int, val arm: Int, val pack: Int, val hair: Int)

/**
 * Every colour the SurfaceView renderer needs, resolved once from res/values/colors.xml.
 * The renderer runs outside Compose, so it cannot call colorResource(); this holder is the
 * bridge. Resolve it once (it is not free) and reuse it: never inside render().
 */
class GameColors(context: Context) {

    private val ctx = context

    private fun c(@ColorRes id: Int): Int = ctx.getColor(id)

    // world
    val clear = c(R.color.world_clear)
    val floorBase = c(R.color.world_floor_base)
    val floorSpeck = c(R.color.world_floor_speck)
    val floorLine = c(R.color.world_floor_line)
    val border = c(R.color.world_border)
    val shadow = c(R.color.world_shadow)
    val pauseDim = c(R.color.world_pause_dim)

    // hud
    val hudText = c(R.color.hud_text)
    val hudDebugText = c(R.color.hud_debug_text)

    // player
    val playerWeapon = c(R.color.player_weapon)
    val playerArm = c(R.color.player_arm)
    val playerBody = c(R.color.player_body)
    val playerSkin = c(R.color.player_skin)
    val playerHair = c(R.color.player_hair)
    val aimLaser = c(R.color.player_aim_laser)
    val muzzleFlash = c(R.color.player_muzzle_flash)

    // zombies, indexed by ZombieType.ordinal: WALKER, RUNNER, TANK, TOXIC, BRUTE, MOTHER
    val zombieSkin = intArrayOf(
        c(R.color.zombie_skin_walker), c(R.color.zombie_skin_runner),
        c(R.color.zombie_skin_tank), c(R.color.zombie_skin_toxic),
        c(R.color.zombie_skin_brute), c(R.color.zombie_skin_mother)
    )
    val zombieCloth = intArrayOf(
        c(R.color.zombie_cloth_walker), c(R.color.zombie_cloth_runner),
        c(R.color.zombie_cloth_tank), c(R.color.zombie_cloth_toxic),
        c(R.color.zombie_cloth_brute), c(R.color.zombie_cloth_mother)
    )
    val zombieGlow = intArrayOf(
        0, 0, 0, c(R.color.zombie_glow_toxic),
        c(R.color.zombie_glow_brute), c(R.color.zombie_glow_mother)
    )
    val zombieWound = c(R.color.zombie_wound)
    val zombieEye = c(R.color.zombie_eye)
    val zombieHealthBack = c(R.color.zombie_health_back)
    val zombieHealthFill = c(R.color.zombie_health_fill)

    // damage feedback
    val damageFlash = c(R.color.damage_flash)
    val poisonTint = c(R.color.poison_tint)

    // reload bar
    val reloadBarBack = c(R.color.reload_bar_back)
    val reloadBarFill = c(R.color.reload_bar_fill)

    // power-ups, indexed by PowerUpType.ordinal: MEDKIT, RAPID_FIRE, DOUBLE_DAMAGE, SHIELD, GRENADE
    val powerUp = intArrayOf(
        c(R.color.powerup_medkit), c(R.color.powerup_rapid), c(R.color.powerup_damage),
        c(R.color.powerup_shield), c(R.color.powerup_grenade)
    )
    val powerUpSymbol = c(R.color.powerup_symbol)
    val powerUpRing = c(R.color.powerup_ring)
    val shieldBubble = c(R.color.shield_bubble)
    val damageAura = c(R.color.damage_aura)

    // grenades and explosions
    val grenadeBody = c(R.color.grenade_body)
    val grenadeFlash = c(R.color.grenade_flash)
    val explosionFill = c(R.color.explosion_fill)
    val explosionRing = c(R.color.explosion_ring)

    // bosses
    val bossWarning = c(R.color.boss_warning)
    val bossEnraged = c(R.color.boss_enraged)

    // Phase 9 (colors_effects.xml): particles, stains, sprite details and overlays
    val particleBlood = c(R.color.particle_blood)
    val particleGoo = c(R.color.particle_goo)
    val particleSpark = c(R.color.particle_spark)
    val particleDust = c(R.color.particle_dust)
    val particleSmoke = c(R.color.particle_smoke)
    val particleFire = c(R.color.particle_fire)
    val particleCasing = c(R.color.particle_casing)
    val decalBlood = c(R.color.decal_blood)
    val decalGoo = c(R.color.decal_goo)
    val decalScorch = c(R.color.decal_scorch)
    val decorCrack = c(R.color.decor_crack)
    val decorStain = c(R.color.decor_stain)
    val decorRubble = c(R.color.decor_rubble)
    val decorRubbleLight = c(R.color.decor_rubble_light)
    val decorMark = c(R.color.decor_mark)
    val spriteOutline = c(R.color.sprite_outline)
    val spriteHighlight = c(R.color.sprite_highlight)
    val spriteShade = c(R.color.sprite_shade)
    val playerWeaponAccent = c(R.color.player_weapon_accent)
    val playerWeaponWood = c(R.color.player_weapon_wood)
    val playerPack = c(R.color.player_pack)
    val playerBoot = c(R.color.player_boot)
    val zombieArmor = c(R.color.zombie_armor)
    val zombiePustule = c(R.color.zombie_pustule)
    val zombieSpike = c(R.color.zombie_spike)
    val zombieSac = c(R.color.zombie_sac)
    val hitFlash = c(R.color.hit_flash)
    val muzzleCore = c(R.color.muzzle_core)
    val muzzleGlow = c(R.color.muzzle_glow)
    val explosionCore = c(R.color.explosion_core)
    val vignette = c(R.color.vignette)
    val lowHealthVignette = c(R.color.low_health_vignette)
    val damageNumber = c(R.color.damage_number)
    val damageNumberBoosted = c(R.color.damage_number_boosted)
    val damageNumberKill = c(R.color.damage_number_kill)
    val damageNumberOutline = c(R.color.damage_number_outline)

    /** The colours for [skin]. The default outfit uses the original player colours above. */
    fun skinPalette(skin: CharacterSkin): SkinPalette = when (skin) {
        CharacterSkin.SURVIVOR -> SkinPalette(playerBody, playerArm, playerPack, playerHair)
        CharacterSkin.RANGER -> SkinPalette(c(R.color.skin_ranger_body), c(R.color.skin_ranger_arm), c(R.color.skin_ranger_pack), c(R.color.skin_ranger_hair))
        CharacterSkin.SCAVENGER -> SkinPalette(c(R.color.skin_scavenger_body), c(R.color.skin_scavenger_arm), c(R.color.skin_scavenger_pack), c(R.color.skin_scavenger_hair))
        CharacterSkin.SHADOW -> SkinPalette(c(R.color.skin_shadow_body), c(R.color.skin_shadow_arm), c(R.color.skin_shadow_pack), c(R.color.skin_shadow_hair))
    }

    // shooting
    val bulletTracer = c(R.color.bullet_tracer)
    val bulletCore = c(R.color.bullet_core)
    val impactBlood = c(R.color.impact_blood)
    val impactDust = c(R.color.impact_dust)
}
