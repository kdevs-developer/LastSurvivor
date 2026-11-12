package com.kdevs.lastsurvivor.ui.mode

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.colorResource
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.engine.GameArea

/**
 * A small top-down scene of [area], used on the mode select screen instead of one shared floor preview:
 * a street with wrecks in the city, trees and poison pools in the swamp, dunes and cacti in the desert,
 * pines and snow in the tundra, tombstones under a moon in the graveyard. [seed] moves the props around,
 * so each campaign level of the same area looks a little different. Everything is drawn from fixed numbers,
 * so it never changes between redraws.
 */
@Composable
fun AreaScenery(area: GameArea, seed: Int, modifier: Modifier = Modifier) {
    val floor = colorResource(area.floorBase)
    val speck = colorResource(area.floorSpeck)
    val line = colorResource(area.floorLine)
    val border = colorResource(area.border)
    val p = SceneryPalette(
        shadow = colorResource(R.color.prop_shadow),
        road = colorResource(R.color.scenery_road),
        lane = colorResource(R.color.scenery_lane),
        carBody = colorResource(R.color.prop_car_body),
        carRoof = colorResource(R.color.prop_car_roof),
        carGlass = colorResource(R.color.prop_car_glass),
        concrete = colorResource(R.color.prop_concrete),
        concreteDark = colorResource(R.color.prop_concrete_dark),
        barrel = colorResource(R.color.prop_barrel),
        barrelBand = colorResource(R.color.prop_barrel_band),
        rain = colorResource(R.color.fx_rain),
        trunk = colorResource(R.color.prop_tree_trunk),
        canopy = colorResource(R.color.prop_tree_canopy),
        canopyLight = colorResource(R.color.prop_tree_canopy_light),
        swampRock = colorResource(R.color.prop_swamp_rock),
        swampRockLight = colorResource(R.color.prop_swamp_rock_light),
        poison = colorResource(R.color.hazard_poison),
        poisonCore = colorResource(R.color.hazard_poison_core),
        bubble = colorResource(R.color.hazard_poison_bubble),
        boulder = colorResource(R.color.prop_boulder),
        boulderLight = colorResource(R.color.prop_boulder_light),
        cactus = colorResource(R.color.prop_cactus),
        cactusLight = colorResource(R.color.prop_cactus_light),
        sand = colorResource(R.color.hazard_sand),
        sandRing = colorResource(R.color.hazard_sand_ring),
        dust = colorResource(R.color.fx_dust),
        ice = colorResource(R.color.prop_ice),
        iceLight = colorResource(R.color.prop_ice_light),
        iceDark = colorResource(R.color.prop_ice_dark),
        pine = colorResource(R.color.prop_pine),
        pineLight = colorResource(R.color.prop_pine_light),
        snowCap = colorResource(R.color.prop_snow_cap),
        snow = colorResource(R.color.hazard_snow),
        snowRing = colorResource(R.color.hazard_snow_ring),
        flake = colorResource(R.color.fx_snow),
        tomb = colorResource(R.color.prop_tomb),
        tombLight = colorResource(R.color.prop_tomb_light),
        tombDark = colorResource(R.color.prop_tomb_dark),
        crypt = colorResource(R.color.prop_crypt),
        cryptRoof = colorResource(R.color.prop_crypt_roof),
        deadTree = colorResource(R.color.prop_dead_tree),
        moon = colorResource(R.color.scenery_moon),
        moonGlow = colorResource(R.color.scenery_moon_glow),
        night = colorResource(R.color.scenery_night),
        fog = colorResource(R.color.scenery_fog)
    )
    Canvas(modifier = modifier) {
        drawRect(floor)
        // faint slab joints and specks, like the real floor
        val cols = 4
        val rows = 3
        for (i in 1 until cols) drawLine(line, Offset(size.width / cols * i, 0f), Offset(size.width / cols * i, size.height), strokeWidth = 1.5f)
        for (j in 1 until rows) drawLine(line, Offset(0f, size.height / rows * j), Offset(size.width, size.height / rows * j), strokeWidth = 1.5f)
        for (n in 0 until 12) {
            val x = ((n * 53 + seed * 7) % 97) / 97f * size.width
            val y = ((n * 29 + seed * 5) % 89) / 89f * size.height
            drawRect(speck, topLeft = Offset(x, y), size = Size(size.width * 0.05f, size.height * 0.035f))
        }
        val rnd = Scatter(seed * 7919 + area.ordinal * 104729 + 13)
        when (area) {
            GameArea.CITY -> city(p, rnd)
            GameArea.SWAMP -> swamp(p, rnd)
            GameArea.DESERT -> desert(p, rnd)
            GameArea.TUNDRA -> tundra(p, rnd)
            GameArea.GRAVEYARD -> graveyard(p, rnd)
        }
        drawRect(border, style = Stroke(width = 5f))
    }
}

private class SceneryPalette(
    val shadow: Color, val road: Color, val lane: Color, val carBody: Color, val carRoof: Color, val carGlass: Color,
    val concrete: Color, val concreteDark: Color, val barrel: Color, val barrelBand: Color, val rain: Color,
    val trunk: Color, val canopy: Color, val canopyLight: Color, val swampRock: Color, val swampRockLight: Color,
    val poison: Color, val poisonCore: Color, val bubble: Color,
    val boulder: Color, val boulderLight: Color, val cactus: Color, val cactusLight: Color,
    val sand: Color, val sandRing: Color, val dust: Color,
    val ice: Color, val iceLight: Color, val iceDark: Color, val pine: Color, val pineLight: Color,
    val snowCap: Color, val snow: Color, val snowRing: Color, val flake: Color,
    val tomb: Color, val tombLight: Color, val tombDark: Color, val crypt: Color, val cryptRoof: Color,
    val deadTree: Color, val moon: Color, val moonGlow: Color, val night: Color, val fog: Color
)

/** Tiny seeded random numbers (0..1), so a scene is the same on every redraw. */
private class Scatter(private var state: Int) {
    fun next(): Float {
        state = state * 1103515245 + 12345
        return ((state ushr 16) and 0x7FFF) / 32767f
    }
}

// ---------------------------------------------------------------------------------------------
// The five scenes. Sizes are fractions of the shorter side so they scale to any tile.
// ---------------------------------------------------------------------------------------------

private fun DrawScope.city(p: SceneryPalette, r: Scatter) {
    val u = size.minDimension
    // two horizontal roads with dashed lane lines
    val roadY = floatArrayOf(size.height * (0.28f + r.next() * 0.08f), size.height * (0.70f + r.next() * 0.08f))
    for (y in roadY) {
        drawRect(p.road, Offset(0f, y - u * 0.13f), Size(size.width, u * 0.26f))
        var x = 0f
        while (x < size.width) {
            drawRect(p.lane, Offset(x, y - u * 0.008f), Size(u * 0.09f, u * 0.016f))
            x += u * 0.17f
        }
    }
    // wrecked cars on the roads
    for (n in 0 until 3) {
        val y = roadY[n % 2] + (r.next() - 0.5f) * u * 0.1f
        car(p, Offset(size.width * (0.12f + 0.3f * n + r.next() * 0.1f), y), u, r.next() * 0.5f - 0.25f)
    }
    // a roadblock of barriers
    val bx = size.width * (0.35f + r.next() * 0.3f)
    val by = (roadY[0] + roadY[1]) / 2f
    for (k in 0 until 4) {
        val x = bx + (k - 1.5f) * u * 0.14f
        drawRoundRect(p.shadow, Offset(x - u * 0.06f, by - u * 0.03f + u * 0.012f), Size(u * 0.12f, u * 0.06f), CornerRadius(u * 0.012f))
        drawRoundRect(p.concrete, Offset(x - u * 0.06f, by - u * 0.03f), Size(u * 0.12f, u * 0.06f), CornerRadius(u * 0.012f))
        drawRect(p.concreteDark, Offset(x - u * 0.06f, by + u * 0.012f), Size(u * 0.12f, u * 0.018f))
    }
    // explosive barrels
    val ex = size.width * (0.7f + r.next() * 0.2f)
    val ey = size.height * (0.4f + r.next() * 0.2f)
    for (k in 0 until 3) barrel(p, Offset(ex + (k - 1) * u * 0.11f, ey + (k % 2) * u * 0.05f), u * 0.045f)
    // rain
    for (n in 0 until 22) {
        val x = r.next() * size.width
        val y = r.next() * size.height
        drawLine(p.rain, Offset(x, y), Offset(x - u * 0.03f, y + u * 0.08f), strokeWidth = 1.5f)
    }
}

private fun DrawScope.car(p: SceneryPalette, c: Offset, u: Float, tilt: Float) {
    val w = u * 0.22f
    val h = u * 0.11f
    rotate(tilt * 57.3f, c) {
        drawRoundRect(p.shadow, Offset(c.x - w / 2 + 3f, c.y - h / 2 + 4f), Size(w, h), CornerRadius(h * 0.3f))
        drawRoundRect(p.carBody, Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(h * 0.3f))
        drawRoundRect(p.carRoof, Offset(c.x - w * 0.22f, c.y - h * 0.34f), Size(w * 0.44f, h * 0.68f), CornerRadius(h * 0.15f))
        drawRect(p.carGlass, Offset(c.x + w * 0.2f, c.y - h * 0.3f), Size(w * 0.1f, h * 0.6f))
        drawRect(p.carGlass, Offset(c.x - w * 0.3f, c.y - h * 0.3f), Size(w * 0.08f, h * 0.6f))
    }
}

private fun DrawScope.barrel(p: SceneryPalette, c: Offset, rad: Float) {
    drawCircle(p.shadow, rad, c + Offset(2f, 3f))
    drawCircle(p.barrel, rad, c)
    drawCircle(p.barrelBand, rad * 0.72f, c, style = Stroke(width = rad * 0.22f))
}

private fun DrawScope.swamp(p: SceneryPalette, r: Scatter) {
    val u = size.minDimension
    for (n in 0 until 3) {
        val c = Offset(size.width * (0.15f + r.next() * 0.7f), size.height * (0.2f + r.next() * 0.6f))
        val rad = u * (0.16f + r.next() * 0.08f)
        drawCircle(p.poison, rad, c)
        drawCircle(p.poisonCore, rad * 0.6f, c)
        for (b in 0 until 3) drawCircle(p.bubble, u * 0.014f, c + Offset((r.next() - 0.5f) * rad, (r.next() - 0.5f) * rad))
    }
    for (n in 0 until 3) {
        val c = Offset(size.width * (0.1f + r.next() * 0.8f), size.height * (0.15f + r.next() * 0.7f))
        drawCircle(p.swampRock, u * 0.05f, c)
        drawCircle(p.swampRockLight, u * 0.028f, c + Offset(-u * 0.012f, -u * 0.012f))
    }
    for (n in 0 until 7) tree(p, Offset(size.width * (0.08f + r.next() * 0.84f), size.height * (0.12f + r.next() * 0.76f)), u * (0.075f + r.next() * 0.03f))
}

private fun DrawScope.tree(p: SceneryPalette, c: Offset, rad: Float) {
    drawCircle(p.shadow, rad, c + Offset(3f, 4f))
    drawCircle(p.canopy, rad, c)
    drawCircle(p.canopyLight, rad * 0.62f, c + Offset(-rad * 0.18f, -rad * 0.18f))
    drawCircle(p.trunk, rad * 0.16f, c)
}

private fun DrawScope.desert(p: SceneryPalette, r: Scatter) {
    val u = size.minDimension
    // dune ridges
    for (n in 0 until 3) {
        val y = size.height * (0.2f + n * 0.3f + r.next() * 0.08f)
        val path = Path().apply {
            moveTo(0f, y)
            quadraticTo(size.width * 0.25f, y - u * 0.12f, size.width * 0.5f, y)
            quadraticTo(size.width * 0.75f, y + u * 0.12f, size.width, y)
        }
        drawPath(path, p.sandRing, style = Stroke(width = 3f))
    }
    // quicksand
    for (n in 0 until 2) {
        val c = Offset(size.width * (0.2f + r.next() * 0.6f), size.height * (0.25f + r.next() * 0.5f))
        val rad = u * (0.16f + r.next() * 0.06f)
        drawCircle(p.sand, rad, c)
        drawCircle(p.sandRing, rad, c, style = Stroke(width = 3f))
        drawCircle(p.sandRing, rad * 0.55f, c, style = Stroke(width = 2f))
    }
    for (n in 0 until 3) {
        val c = Offset(size.width * (0.1f + r.next() * 0.8f), size.height * (0.15f + r.next() * 0.7f))
        val rad = u * (0.07f + r.next() * 0.04f)
        drawCircle(p.shadow, rad, c + Offset(3f, 4f))
        drawCircle(p.boulder, rad, c)
        drawCircle(p.boulderLight, rad * 0.55f, c + Offset(-rad * 0.25f, -rad * 0.25f))
    }
    for (n in 0 until 6) {
        val c = Offset(size.width * (0.08f + r.next() * 0.84f), size.height * (0.12f + r.next() * 0.76f))
        val rad = u * 0.03f
        drawCircle(p.cactus, rad, c)
        drawCircle(p.cactusLight, rad * 0.5f, c)
        drawLine(p.cactus, c, c + Offset(rad * 1.9f, -rad * 0.4f), strokeWidth = rad * 0.7f)
        drawLine(p.cactus, c, c + Offset(-rad * 1.7f, rad * 0.5f), strokeWidth = rad * 0.7f)
    }
    for (n in 0 until 14) {
        val x = r.next() * size.width
        val y = r.next() * size.height
        drawLine(p.dust, Offset(x, y), Offset(x + u * 0.1f, y + u * 0.012f), strokeWidth = 2f)
    }
}

private fun DrawScope.tundra(p: SceneryPalette, r: Scatter) {
    val u = size.minDimension
    for (n in 0 until 3) {
        val c = Offset(size.width * (0.15f + r.next() * 0.7f), size.height * (0.2f + r.next() * 0.6f))
        val rad = u * (0.17f + r.next() * 0.07f)
        drawCircle(p.snow, rad, c)
        drawCircle(p.snowRing, rad, c, style = Stroke(width = 3f))
    }
    for (n in 0 until 3) {
        val c = Offset(size.width * (0.1f + r.next() * 0.8f), size.height * (0.15f + r.next() * 0.7f))
        val rad = u * (0.06f + r.next() * 0.03f)
        val rock = Path().apply {
            moveTo(c.x - rad, c.y + rad * 0.3f); lineTo(c.x - rad * 0.4f, c.y - rad); lineTo(c.x + rad * 0.6f, c.y - rad * 0.7f)
            lineTo(c.x + rad, c.y + rad * 0.4f); lineTo(c.x, c.y + rad); close()
        }
        drawPath(rock, p.ice)
        drawPath(Path().apply {
            moveTo(c.x - rad * 0.4f, c.y - rad); lineTo(c.x + rad * 0.6f, c.y - rad * 0.7f); lineTo(c.x, c.y); close()
        }, p.iceLight)
        drawLine(p.iceDark, Offset(c.x, c.y + rad), Offset(c.x + rad, c.y + rad * 0.4f), strokeWidth = 3f)
    }
    for (n in 0 until 6) {
        val c = Offset(size.width * (0.08f + r.next() * 0.84f), size.height * (0.12f + r.next() * 0.76f))
        val rad = u * 0.06f
        drawCircle(p.shadow, rad, c + Offset(3f, 4f))
        drawCircle(p.pine, rad, c)
        drawCircle(p.pineLight, rad * 0.65f, c)
        drawCircle(p.snowCap, rad * 0.3f, c + Offset(-rad * 0.1f, -rad * 0.1f))
    }
    for (n in 0 until 26) drawCircle(p.flake, 1.8f, Offset(r.next() * size.width, r.next() * size.height))
}

private fun DrawScope.graveyard(p: SceneryPalette, r: Scatter) {
    val u = size.minDimension
    // rows of tombstones with aisles
    for (row in 0 until 3) {
        val y = size.height * (0.2f + row * 0.28f)
        for (col in 0 until 6) {
            if (r.next() < 0.3f) continue
            val x = size.width * (0.1f + col * 0.15f) + (r.next() - 0.5f) * u * 0.02f
            val w = u * 0.05f
            val h = u * 0.065f
            drawRoundRect(p.shadow, Offset(x - w / 2 + 2f, y - h / 2 + 3f), Size(w, h), CornerRadius(w * 0.45f))
            drawRoundRect(p.tomb, Offset(x - w / 2, y - h / 2), Size(w, h), CornerRadius(w * 0.45f))
            drawRect(p.tombLight, Offset(x - w * 0.06f, y - h * 0.3f), Size(w * 0.12f, h * 0.5f))
            drawRect(p.tombLight, Offset(x - w * 0.22f, y - h * 0.15f), Size(w * 0.44f, h * 0.1f))
        }
    }
    // a crypt
    val cx = size.width * (0.55f + r.next() * 0.3f)
    val cy = size.height * (0.3f + r.next() * 0.4f)
    drawRect(p.shadow, Offset(cx - u * 0.11f + 3f, cy - u * 0.09f + 4f), Size(u * 0.22f, u * 0.18f))
    drawRect(p.crypt, Offset(cx - u * 0.11f, cy - u * 0.09f), Size(u * 0.22f, u * 0.18f))
    drawRect(p.cryptRoof, Offset(cx - u * 0.11f, cy - u * 0.09f), Size(u * 0.22f, u * 0.05f))
    drawRect(p.tombDark, Offset(cx - u * 0.03f, cy + u * 0.02f), Size(u * 0.06f, u * 0.07f))
    // dead trees
    for (n in 0 until 2) {
        val c = Offset(size.width * (0.1f + r.next() * 0.8f), size.height * (0.15f + r.next() * 0.7f))
        for (b in 0 until 5) {
            val a = b * 1.2566f + r.next()
            drawLine(p.deadTree, c, c + Offset(kotlin.math.cos(a) * u * 0.07f, kotlin.math.sin(a) * u * 0.07f), strokeWidth = 3f)
        }
        drawCircle(p.deadTree, u * 0.014f, c)
    }
    // darkness, moon glow and fog
    drawRect(p.night)
    drawCircle(p.moonGlow, u * 0.4f, Offset(size.width * 0.85f, size.height * 0.15f))
    drawCircle(p.moon, u * 0.06f, Offset(size.width * 0.85f, size.height * 0.15f))
    drawOval(p.fog, Offset(-size.width * 0.1f, size.height * 0.62f), Size(size.width * 0.8f, size.height * 0.25f))
    drawOval(p.fog, Offset(size.width * 0.4f, size.height * 0.15f), Size(size.width * 0.7f, size.height * 0.2f))
}
