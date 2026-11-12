package com.kdevs.lastsurvivor.ui.weapons

import androidx.annotation.StringRes
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.weapons.UpgradeTrack
import com.kdevs.lastsurvivor.game.weapons.WeaponType

/** Maps game-side weapon data to string resources (the game package stays free of Android types). */
@StringRes
fun WeaponType.nameRes(): Int = when (this) {
    WeaponType.PISTOL -> R.string.weapon_pistol
    WeaponType.SMG -> R.string.weapon_smg
    WeaponType.SHOTGUN -> R.string.weapon_shotgun
    WeaponType.RIFLE -> R.string.weapon_rifle
    WeaponType.REVOLVER -> R.string.weapon_revolver
    WeaponType.ASSAULT -> R.string.weapon_assault
    WeaponType.AUTO_SHOTGUN -> R.string.weapon_auto_shotgun
    WeaponType.SNIPER -> R.string.weapon_sniper
    WeaponType.MINIGUN -> R.string.weapon_minigun
}

@StringRes
fun WeaponType.descriptionRes(): Int = when (this) {
    WeaponType.PISTOL -> R.string.weapon_pistol_desc
    WeaponType.SMG -> R.string.weapon_smg_desc
    WeaponType.SHOTGUN -> R.string.weapon_shotgun_desc
    WeaponType.RIFLE -> R.string.weapon_rifle_desc
    WeaponType.REVOLVER -> R.string.weapon_revolver_desc
    WeaponType.ASSAULT -> R.string.weapon_assault_desc
    WeaponType.AUTO_SHOTGUN -> R.string.weapon_auto_shotgun_desc
    WeaponType.SNIPER -> R.string.weapon_sniper_desc
    WeaponType.MINIGUN -> R.string.weapon_minigun_desc
}

@StringRes
fun UpgradeTrack.labelRes(): Int = when (this) {
    UpgradeTrack.DAMAGE -> R.string.upgrade_damage
    UpgradeTrack.FIRE_RATE -> R.string.upgrade_fire_rate
    UpgradeTrack.MAGAZINE -> R.string.upgrade_magazine
    UpgradeTrack.RELOAD -> R.string.upgrade_reload
}
