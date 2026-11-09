package com.kdevs.lastsurvivor.game.render

import android.content.Context
import androidx.annotation.ColorRes
import com.kdevs.lastsurvivor.R

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

    // shooting
    val bulletTracer = c(R.color.bullet_tracer)
    val bulletCore = c(R.color.bullet_core)
    val impactBlood = c(R.color.impact_blood)
    val impactDust = c(R.color.impact_dust)
}
