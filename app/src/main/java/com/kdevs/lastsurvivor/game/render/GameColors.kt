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

    // zombies, indexed by ZombieType.ordinal: WALKER, RUNNER, TANK, TOXIC
    val zombieSkin = intArrayOf(
        c(R.color.zombie_skin_walker), c(R.color.zombie_skin_runner),
        c(R.color.zombie_skin_tank), c(R.color.zombie_skin_toxic)
    )
    val zombieCloth = intArrayOf(
        c(R.color.zombie_cloth_walker), c(R.color.zombie_cloth_runner),
        c(R.color.zombie_cloth_tank), c(R.color.zombie_cloth_toxic)
    )
    val zombieGlow = intArrayOf(0, 0, 0, c(R.color.zombie_glow_toxic))
    val zombieWound = c(R.color.zombie_wound)
    val zombieEye = c(R.color.zombie_eye)
    val zombieHealthBack = c(R.color.zombie_health_back)
    val zombieHealthFill = c(R.color.zombie_health_fill)

    // shooting
    val bulletTracer = c(R.color.bullet_tracer)
    val bulletCore = c(R.color.bullet_core)
    val impactBlood = c(R.color.impact_blood)
    val impactDust = c(R.color.impact_dust)
}
