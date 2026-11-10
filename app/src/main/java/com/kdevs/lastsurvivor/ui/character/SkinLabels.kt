package com.kdevs.lastsurvivor.ui.character

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.entities.CharacterSkin

/** Maps game-side outfit data to resources (the game package stays free of Android types). */
@StringRes
fun CharacterSkin.nameRes(): Int = when (this) {
    CharacterSkin.SURVIVOR -> R.string.skin_survivor
    CharacterSkin.RANGER -> R.string.skin_ranger
    CharacterSkin.SCAVENGER -> R.string.skin_scavenger
    CharacterSkin.SHADOW -> R.string.skin_shadow
}

/** The jacket colour, used for the preview swatch. */
@ColorRes
fun CharacterSkin.bodyColorRes(): Int = when (this) {
    CharacterSkin.SURVIVOR -> R.color.player_body
    CharacterSkin.RANGER -> R.color.skin_ranger_body
    CharacterSkin.SCAVENGER -> R.color.skin_scavenger_body
    CharacterSkin.SHADOW -> R.color.skin_shadow_body
}

/** The sleeve colour, used for the preview swatch ring. */
@ColorRes
fun CharacterSkin.armColorRes(): Int = when (this) {
    CharacterSkin.SURVIVOR -> R.color.player_arm
    CharacterSkin.RANGER -> R.color.skin_ranger_arm
    CharacterSkin.SCAVENGER -> R.color.skin_scavenger_arm
    CharacterSkin.SHADOW -> R.color.skin_shadow_arm
}
