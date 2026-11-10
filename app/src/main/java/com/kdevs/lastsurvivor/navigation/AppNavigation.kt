package com.kdevs.lastsurvivor.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kdevs.lastsurvivor.ui.character.CharacterScreen
import com.kdevs.lastsurvivor.ui.game.GameScreen
import com.kdevs.lastsurvivor.ui.menu.MainMenuScreen
import com.kdevs.lastsurvivor.ui.settings.SettingsScreen
import com.kdevs.lastsurvivor.ui.upgrades.UpgradesScreen

object Routes {
    const val MENU = "menu"
    const val CONTINUE_ARG = "continue"
    const val GAME = "game/{$CONTINUE_ARG}"
    /** The game route; [continueRun] restores the saved checkpoint instead of starting at wave 1. */
    fun game(continueRun: Boolean) = "game/$continueRun"
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
                onPlay = { nav.navigate(Routes.game(continueRun = false)) },
                onContinue = { nav.navigate(Routes.game(continueRun = true)) },
                onUpgrades = { nav.navigate(Routes.UPGRADES) },
                onCharacter = { nav.navigate(Routes.CHARACTER) },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.GAME,
            arguments = listOf(navArgument(Routes.CONTINUE_ARG) { type = NavType.BoolType })
        ) { entry ->
            GameScreen(continueRun = entry.arguments?.getBoolean(Routes.CONTINUE_ARG) ?: false, onExit = back)
        }
        composable(Routes.UPGRADES) { UpgradesScreen(onBack = back) }
        composable(Routes.CHARACTER) { CharacterScreen(onBack = back) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = back) }
    }
}
