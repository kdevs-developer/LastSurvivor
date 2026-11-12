package com.kdevs.lastsurvivor.game.engine

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.kdevs.lastsurvivor.R

/**
 * A map theme. Gameplay is identical everywhere: only the floor, the map border and the floor
 * decoration are redrawn in the area's colours (colors_areas.xml; the Ruined City keeps the original
 * look). [accent] is the highlight used on the mode select screen.
 */
enum class GameArea(
    @StringRes val nameRes: Int,
    @ColorRes val floorBase: Int,
    @ColorRes val floorSpeck: Int,
    @ColorRes val floorLine: Int,
    @ColorRes val border: Int,
    @ColorRes val decorCrack: Int,
    @ColorRes val decorStain: Int,
    @ColorRes val decorRubble: Int,
    @ColorRes val decorRubbleLight: Int,
    @ColorRes val decorMark: Int,
    @ColorRes val accent: Int
) {
    CITY(
        R.string.area_city,
        R.color.world_floor_base, R.color.world_floor_speck, R.color.world_floor_line, R.color.world_border,
        R.color.decor_crack, R.color.decor_stain, R.color.decor_rubble, R.color.decor_rubble_light, R.color.decor_mark,
        R.color.area_city_accent
    ),
    SWAMP(
        R.string.area_swamp,
        R.color.area_swamp_floor_base, R.color.area_swamp_floor_speck, R.color.area_swamp_floor_line, R.color.area_swamp_border,
        R.color.area_swamp_decor_crack, R.color.area_swamp_decor_stain, R.color.area_swamp_decor_rubble,
        R.color.area_swamp_decor_rubble_light, R.color.area_swamp_decor_mark, R.color.area_swamp_accent
    ),
    DESERT(
        R.string.area_desert,
        R.color.area_desert_floor_base, R.color.area_desert_floor_speck, R.color.area_desert_floor_line, R.color.area_desert_border,
        R.color.area_desert_decor_crack, R.color.area_desert_decor_stain, R.color.area_desert_decor_rubble,
        R.color.area_desert_decor_rubble_light, R.color.area_desert_decor_mark, R.color.area_desert_accent
    ),
    TUNDRA(
        R.string.area_tundra,
        R.color.area_tundra_floor_base, R.color.area_tundra_floor_speck, R.color.area_tundra_floor_line, R.color.area_tundra_border,
        R.color.area_tundra_decor_crack, R.color.area_tundra_decor_stain, R.color.area_tundra_decor_rubble,
        R.color.area_tundra_decor_rubble_light, R.color.area_tundra_decor_mark, R.color.area_tundra_accent
    ),
    GRAVEYARD(
        R.string.area_graveyard,
        R.color.area_graveyard_floor_base, R.color.area_graveyard_floor_speck, R.color.area_graveyard_floor_line, R.color.area_graveyard_border,
        R.color.area_graveyard_decor_crack, R.color.area_graveyard_decor_stain, R.color.area_graveyard_decor_rubble,
        R.color.area_graveyard_decor_rubble_light, R.color.area_graveyard_decor_mark, R.color.area_graveyard_accent
    )
}
