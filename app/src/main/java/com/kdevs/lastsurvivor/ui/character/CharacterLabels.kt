package com.kdevs.lastsurvivor.ui.character

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.ui.graphics.vector.ImageVector
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.entities.AbilityType
import com.kdevs.lastsurvivor.game.entities.GameCharacter

/** Maps game-side character data to resources (the game package stays free of Android types). */
@StringRes
fun GameCharacter.nameRes(): Int = when (this) {
    GameCharacter.SURVIVOR -> R.string.char_survivor
    GameCharacter.RUNNER -> R.string.char_runner
    GameCharacter.TANK -> R.string.char_tank
    GameCharacter.MEDIC -> R.string.char_medic
    GameCharacter.ENGINEER -> R.string.char_engineer
    GameCharacter.COMMANDO -> R.string.char_commando
}

/** One line on how the character plays. */
@StringRes
fun GameCharacter.roleRes(): Int = when (this) {
    GameCharacter.SURVIVOR -> R.string.char_survivor_role
    GameCharacter.RUNNER -> R.string.char_runner_role
    GameCharacter.TANK -> R.string.char_tank_role
    GameCharacter.MEDIC -> R.string.char_medic_role
    GameCharacter.ENGINEER -> R.string.char_engineer_role
    GameCharacter.COMMANDO -> R.string.char_commando_role
}

/** The jacket colour, used for the character preview. */
@ColorRes
fun GameCharacter.bodyColorRes(): Int = when (this) {
    GameCharacter.SURVIVOR -> R.color.player_body
    GameCharacter.RUNNER -> R.color.char_runner_body
    GameCharacter.TANK -> R.color.char_tank_body
    GameCharacter.MEDIC -> R.color.char_medic_body
    GameCharacter.ENGINEER -> R.color.char_engineer_body
    GameCharacter.COMMANDO -> R.color.char_commando_body
}

/** The sleeve colour, used for the character preview. */
@ColorRes
fun GameCharacter.armColorRes(): Int = when (this) {
    GameCharacter.SURVIVOR -> R.color.player_arm
    GameCharacter.RUNNER -> R.color.char_runner_arm
    GameCharacter.TANK -> R.color.char_tank_arm
    GameCharacter.MEDIC -> R.color.char_medic_arm
    GameCharacter.ENGINEER -> R.color.char_engineer_arm
    GameCharacter.COMMANDO -> R.color.char_commando_arm
}

/** The hair colour, used for the character preview. */
@ColorRes
fun GameCharacter.hairColorRes(): Int = when (this) {
    GameCharacter.SURVIVOR -> R.color.player_hair
    GameCharacter.RUNNER -> R.color.char_runner_hair
    GameCharacter.TANK -> R.color.char_tank_hair
    GameCharacter.MEDIC -> R.color.char_medic_hair
    GameCharacter.ENGINEER -> R.color.char_engineer_hair
    GameCharacter.COMMANDO -> R.color.char_commando_hair
}

@StringRes
fun AbilityType.nameRes(): Int = when (this) {
    AbilityType.ADRENALINE -> R.string.ability_adrenaline
    AbilityType.DASH -> R.string.ability_dash
    AbilityType.SHOCKWAVE -> R.string.ability_shockwave
    AbilityType.HEAL_PULSE -> R.string.ability_heal_pulse
    AbilityType.TURRET -> R.string.ability_turret
    AbilityType.RAGE -> R.string.ability_rage
}

@StringRes
fun AbilityType.descriptionRes(): Int = when (this) {
    AbilityType.ADRENALINE -> R.string.ability_adrenaline_desc
    AbilityType.DASH -> R.string.ability_dash_desc
    AbilityType.SHOCKWAVE -> R.string.ability_shockwave_desc
    AbilityType.HEAL_PULSE -> R.string.ability_heal_pulse_desc
    AbilityType.TURRET -> R.string.ability_turret_desc
    AbilityType.RAGE -> R.string.ability_rage_desc
}

/** The icon on the ability button and the character screen. */
fun AbilityType.icon(): ImageVector = when (this) {
    AbilityType.ADRENALINE -> Icons.Filled.FlashOn
    AbilityType.DASH -> Icons.Filled.FastForward
    AbilityType.SHOCKWAVE -> Icons.Filled.RadioButtonChecked
    AbilityType.HEAL_PULSE -> Icons.Filled.Favorite
    AbilityType.TURRET -> Icons.Filled.Build
    AbilityType.RAGE -> Icons.Filled.LocalFireDepartment
}
