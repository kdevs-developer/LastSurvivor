package com.kdevs.lastsurvivor.ui.character

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.ui.graphics.vector.ImageVector
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.entities.AbilityType
import com.kdevs.lastsurvivor.game.entities.GameCharacter
import com.kdevs.lastsurvivor.game.entities.PassiveType

/** Maps game-side character data to resources (the game package stays free of Android types). */
@StringRes
fun GameCharacter.nameRes(): Int = when (this) {
    GameCharacter.SURVIVOR -> R.string.char_survivor
    GameCharacter.RUNNER -> R.string.char_runner
    GameCharacter.TANK -> R.string.char_tank
    GameCharacter.MEDIC -> R.string.char_medic
    GameCharacter.ENGINEER -> R.string.char_engineer
    GameCharacter.COMMANDO -> R.string.char_commando
    GameCharacter.SCAVENGER -> R.string.char_scavenger
    GameCharacter.MARKSMAN -> R.string.char_marksman
    GameCharacter.CRYO -> R.string.char_cryo
    GameCharacter.DEMOLITIONIST -> R.string.char_demolitionist
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
    GameCharacter.SCAVENGER -> R.string.char_scavenger_role
    GameCharacter.MARKSMAN -> R.string.char_marksman_role
    GameCharacter.CRYO -> R.string.char_cryo_role
    GameCharacter.DEMOLITIONIST -> R.string.char_demolitionist_role
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
    GameCharacter.SCAVENGER -> R.color.char_scavenger_body
    GameCharacter.MARKSMAN -> R.color.char_marksman_body
    GameCharacter.CRYO -> R.color.char_cryo_body
    GameCharacter.DEMOLITIONIST -> R.color.char_demolitionist_body
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
    GameCharacter.SCAVENGER -> R.color.char_scavenger_arm
    GameCharacter.MARKSMAN -> R.color.char_marksman_arm
    GameCharacter.CRYO -> R.color.char_cryo_arm
    GameCharacter.DEMOLITIONIST -> R.color.char_demolitionist_arm
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
    GameCharacter.SCAVENGER -> R.color.char_scavenger_hair
    GameCharacter.MARKSMAN -> R.color.char_marksman_hair
    GameCharacter.CRYO -> R.color.char_cryo_hair
    GameCharacter.DEMOLITIONIST -> R.color.char_demolitionist_hair
}

@StringRes
fun AbilityType.nameRes(): Int = when (this) {
    AbilityType.ADRENALINE -> R.string.ability_adrenaline
    AbilityType.DASH -> R.string.ability_dash
    AbilityType.SHOCKWAVE -> R.string.ability_shockwave
    AbilityType.HEAL_PULSE -> R.string.ability_heal_pulse
    AbilityType.TURRET -> R.string.ability_turret
    AbilityType.RAGE -> R.string.ability_rage
    AbilityType.DEADEYE -> R.string.ability_deadeye
    AbilityType.FREEZE -> R.string.ability_freeze
    AbilityType.AIRSTRIKE -> R.string.ability_airstrike
    AbilityType.JACKPOT -> R.string.ability_jackpot
}

@StringRes
fun AbilityType.descriptionRes(): Int = when (this) {
    AbilityType.ADRENALINE -> R.string.ability_adrenaline_desc
    AbilityType.DASH -> R.string.ability_dash_desc
    AbilityType.SHOCKWAVE -> R.string.ability_shockwave_desc
    AbilityType.HEAL_PULSE -> R.string.ability_heal_pulse_desc
    AbilityType.TURRET -> R.string.ability_turret_desc
    AbilityType.RAGE -> R.string.ability_rage_desc
    AbilityType.DEADEYE -> R.string.ability_deadeye_desc
    AbilityType.FREEZE -> R.string.ability_freeze_desc
    AbilityType.AIRSTRIKE -> R.string.ability_airstrike_desc
    AbilityType.JACKPOT -> R.string.ability_jackpot_desc
}

/** The icon on the ability button and the character screen. */
fun AbilityType.icon(): ImageVector = when (this) {
    AbilityType.ADRENALINE -> Icons.Filled.FlashOn
    AbilityType.DASH -> Icons.Filled.FastForward
    AbilityType.SHOCKWAVE -> Icons.Filled.RadioButtonChecked
    AbilityType.HEAL_PULSE -> Icons.Filled.Favorite
    AbilityType.TURRET -> Icons.Filled.Build
    AbilityType.RAGE -> Icons.Filled.LocalFireDepartment
    AbilityType.DEADEYE -> Icons.Filled.GpsFixed
    AbilityType.FREEZE -> Icons.Filled.AcUnit
    AbilityType.AIRSTRIKE -> Icons.Filled.RocketLaunch
    AbilityType.JACKPOT -> Icons.Filled.Casino
}

@StringRes
fun PassiveType.nameRes(): Int = when (this) {
    PassiveType.LAST_STAND -> R.string.passive_last_stand
    PassiveType.NIMBLE -> R.string.passive_nimble
    PassiveType.THORNS -> R.string.passive_thorns
    PassiveType.TRIAGE -> R.string.passive_triage
    PassiveType.OVERCLOCK -> R.string.passive_overclock
    PassiveType.VETERAN -> R.string.passive_veteran
    PassiveType.SCAVENGER -> R.string.passive_scavenger
    PassiveType.SHARPSHOOTER -> R.string.passive_sharpshooter
    PassiveType.FROSTBITE -> R.string.passive_frostbite
    PassiveType.BLAST_EXPERT -> R.string.passive_blast_expert
}

@StringRes
fun PassiveType.descriptionRes(): Int = when (this) {
    PassiveType.LAST_STAND -> R.string.passive_last_stand_desc
    PassiveType.NIMBLE -> R.string.passive_nimble_desc
    PassiveType.THORNS -> R.string.passive_thorns_desc
    PassiveType.TRIAGE -> R.string.passive_triage_desc
    PassiveType.OVERCLOCK -> R.string.passive_overclock_desc
    PassiveType.VETERAN -> R.string.passive_veteran_desc
    PassiveType.SCAVENGER -> R.string.passive_scavenger_desc
    PassiveType.SHARPSHOOTER -> R.string.passive_sharpshooter_desc
    PassiveType.FROSTBITE -> R.string.passive_frostbite_desc
    PassiveType.BLAST_EXPERT -> R.string.passive_blast_expert_desc
}
