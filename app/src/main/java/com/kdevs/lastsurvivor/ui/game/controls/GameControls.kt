package com.kdevs.lastsurvivor.ui.game.controls

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.engine.InputState

/**
 * All on-screen controls. Sizes derive from the available height, so the layout adapts to
 * phones and tablets. Layout (landscape):
 *
 *  Sprint                                                    Grenade
 *  Move stick                                       Reload   Aim + fire stick
 *
 *  - Sprint is a toggle: tap to switch it on, tap again to switch it off.
 *  - The right stick aims and fires: the weapon shoots automatically while it is dragged.
 *  - Grenade is a tap: it is thrown automatically at the biggest nearby group of zombies.
 *  - Reload is a tap.
 *
 * Only empty areas pass touches through to the game view underneath.
 */
@Composable
fun GameControls(input: InputState, modifier: Modifier = Modifier) {
    val moveColor = colorResource(R.color.control_move)
    val aimColor = colorResource(R.color.control_aim)
    val reloadColor = colorResource(R.color.control_reload)
    val grenadeColor = colorResource(R.color.control_grenade)
    val sprintColor = colorResource(R.color.control_sprint)

    // Controls leave the screen on pause and InputState.reset() clears sprint, so a fresh "off" here always matches.
    var sprintOn by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
    ) {
        val stick = (maxHeight * 0.42f).coerceIn(110.dp, 170.dp)
        val reload = stick * 0.5f
        val small = stick * 0.4f
        val gap = 14.dp

        // ---- Left: movement + sprint toggle ----
        VirtualJoystick(
            diameter = stick,
            accent = moveColor,
            modifier = Modifier.align(Alignment.BottomStart)
        ) { x, y, _ ->
            input.moveX = x
            input.moveY = y
        }

        ActionButton(
            icon = Icons.Filled.Speed,
            description = stringResource(R.string.control_sprint),
            diameter = small,
            color = sprintColor,
            active = sprintOn,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = stick * 0.1f, bottom = stick + gap)
        ) { pressed ->
            if (pressed) {
                sprintOn = !sprintOn
                input.sprintHeld = sprintOn
            }
        }

        // ---- Right: aim and fire stick, reload, grenade ----
        VirtualJoystick(
            diameter = stick,
            accent = aimColor,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) { x, y, active ->
            input.aimX = x
            input.aimY = y
            input.aimStrength = kotlin.math.sqrt(x * x + y * y)
            input.aimActive = active
        }

        ActionButton(
            icon = Icons.Filled.Autorenew,
            description = stringResource(R.string.control_reload),
            diameter = reload,
            color = reloadColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = stick + gap, bottom = stick * 0.2f)
        ) { pressed -> if (pressed) input.requestReload() }

        ActionButton(
            icon = Icons.Filled.Whatshot,
            description = stringResource(R.string.control_grenade),
            diameter = small,
            color = grenadeColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = (stick - small) / 2, bottom = stick + gap)
        ) { pressed -> if (pressed) input.requestGrenade() }
    }
}