package com.kdevs.lastsurvivor.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kdevs.lastsurvivor.ui.game.GameScreen
import com.kdevs.lastsurvivor.ui.menu.MainMenuScreen
import com.kdevs.lastsurvivor.ui.menu.PlaceholderScreen

object Routes {
    const val MENU = "menu"
    const val GAME = "game"
    const val UPGRADES = "upgrades"
    const val CHARACTER = "character"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(navController = nav, startDestination = Routes.MENU) {
        composable(Routes.MENU) {
            MainMenuScreen(
                onPlay = { nav.navigate(Routes.GAME) },
                onContinue = { nav.navigate(Routes.GAME) }, // real save restore arrives in Phase 10
                onUpgrades = { nav.navigate(Routes.UPGRADES) },
                onCharacter = { nav.navigate(Routes.CHARACTER) },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.GAME) { GameScreen(onExit = back) }
        composable(Routes.UPGRADES) { PlaceholderScreen("Weapon Upgrades", "Arrives in Phase 7.", back) }
        composable(Routes.CHARACTER) { PlaceholderScreen("Character Selection", "Arrives in Phase 9.", back) }
        composable(Routes.SETTINGS) { PlaceholderScreen("Settings", "Audio sliders arrive in Phase 9, persistence in Phase 10.", back) }
    }
}
