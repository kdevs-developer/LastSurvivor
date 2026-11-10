package com.kdevs.lastsurvivor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kdevs.lastsurvivor.game.audio.SoundManager

/** The app-wide [SoundManager], for button click sounds in Compose screens. */
@Composable
fun rememberSoundManager(): SoundManager {
    val appContext = LocalContext.current.applicationContext
    return remember { SoundManager.get(appContext) }
}
