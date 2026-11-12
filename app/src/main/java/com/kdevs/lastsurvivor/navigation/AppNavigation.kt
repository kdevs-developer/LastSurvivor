package com.kdevs.lastsurvivor.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kdevs.lastsurvivor.game.audio.MusicManager
import com.kdevs.lastsurvivor.game.audio.MusicTrack
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kdevs.lastsurvivor.game.engine.RunSetup
import com.kdevs.lastsurvivor.ui.character.CharacterScreen
import com.kdevs.lastsurvivor.ui.game.GameScreen
import com.kdevs.lastsurvivor.ui.menu.MainMenuScreen
import com.kdevs.lastsurvivor.ui.mode.ModeSelectScreen
import com.kdevs.lastsurvivor.ui.settings.SettingsScreen
import com.kdevs.lastsurvivor.ui.upgrades.UpgradesScreen

object Routes {
    const val MENU = "menu"
    /** Play leads here: pick Campaign (a level) or Infinite (an area). */
    const val MODE = "mode"
    const val SETUP_ARG = "setup"
    const val CONTINUE_ARG = "continue"
    const val GAME = "game/{$SETUP_ARG}/{$CONTINUE_ARG}"
    /**
     * The game route. [setup] is the level or area to play; [continueRun] restores the saved checkpoint
     * (which carries its own level or area) instead of starting at wave 1.
     */
    fun game(setup: RunSetup, continueRun: Boolean) = "game/${setup.encode()}/$continueRun"
    const val UPGRADES = "upgrades"
    const val CHARACTER = "character"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }

    // Menu music on every screen except the game, where GameScreen plays the track of the area being played.
    // Asking for the track that is already playing does nothing, so moving between menus does not restart it.
    val context = LocalContext.current
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(route) {
        if (route != null && route != Routes.GAME) MusicManager.get(context).play(MusicTrack.MENU)
    }

    NavHost(navController = nav, startDestination = Routes.MENU) {
        composable(Routes.MENU) {
            MainMenuScreen(
                onPlay = { nav.navigate(Routes.MODE) },
                onContinue = { nav.navigate(Routes.game(RunSetup.DEFAULT, continueRun = true)) },
                onUpgrades = { nav.navigate(Routes.UPGRADES) },
                onCharacter = { nav.navigate(Routes.CHARACTER) },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.MODE) {
            ModeSelectScreen(
                onBack = back,
                onStart = { setup -> nav.navigate(Routes.game(setup, continueRun = false)) }
            )
        }
        composable(
            route = Routes.GAME,
            arguments = listOf(
                navArgument(Routes.SETUP_ARG) { type = NavType.StringType },
                navArgument(Routes.CONTINUE_ARG) { type = NavType.BoolType }
            )
        ) { entry ->
            GameScreen(
                setup = RunSetup.decode(entry.arguments?.getString(Routes.SETUP_ARG)) ?: RunSetup.DEFAULT,
                continueRun = entry.arguments?.getBoolean(Routes.CONTINUE_ARG) ?: false,
                onExit = back
            )
        }
        composable(Routes.UPGRADES) { UpgradesScreen(onBack = back) }
        composable(Routes.CHARACTER) { CharacterScreen(onBack = back) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = back) }
    }
}
