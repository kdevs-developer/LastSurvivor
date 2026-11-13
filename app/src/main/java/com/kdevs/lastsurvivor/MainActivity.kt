package com.kdevs.lastsurvivor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kdevs.lastsurvivor.data.PlayerPrefsRepository
import com.kdevs.lastsurvivor.game.audio.MusicManager
import com.kdevs.lastsurvivor.game.audio.SoundManager
import kotlinx.coroutines.launch
import com.kdevs.lastsurvivor.navigation.AppNavigation
import com.kdevs.lastsurvivor.ui.splash.AnimatedSplash
import com.kdevs.lastsurvivor.ui.theme.LastSurvivorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        // Load the sound effects now so the first shot is not silent, and keep the SoundManager's
        // on/off switch in step with the saved Sound setting.
        val sounds = SoundManager.get(this)
        val music = MusicManager.get(this)
        val prefs = PlayerPrefsRepository(this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                prefs.settings.collect { s ->
                    sounds.enabled = s.soundEnabled
                    sounds.volume = s.sfxVolume
                    sounds.vibration = s.vibration
                    music.setEnabled(s.soundEnabled)
                    music.setVolume(s.musicVolume)
                }
            }
        }

        setContent {
            LastSurvivorTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(targetState = showSplash, label = "splash") { splash ->
                        if (splash) AnimatedSplash(onFinished = { showSplash = false }) else AppNavigation()
                    }
                }
            }
        }
    }

    // Pause on onPause, not onStop: when the Recents screen opens (or the app is split off / covered by
    // another window) the activity is paused but onStop can arrive late or never, so the music kept playing.
    override fun onResume() {
        super.onResume()
        MusicManager.get(this).setForeground(true)
    }

    override fun onPause() {
        MusicManager.get(this).setForeground(false)
        super.onPause()
    }

    // Safety net in case the app is stopped without a pause being seen.
    override fun onStop() {
        MusicManager.get(this).setForeground(false)
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}