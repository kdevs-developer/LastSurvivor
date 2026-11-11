package com.kdevs.lastsurvivor.ui.character

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.data.PlayerStats
import com.kdevs.lastsurvivor.game.audio.SoundEffect
import com.kdevs.lastsurvivor.game.entities.AbilityType
import com.kdevs.lastsurvivor.game.entities.GameCharacter
import com.kdevs.lastsurvivor.ui.DiamondIcon
import com.kdevs.lastsurvivor.ui.rememberSoundManager
import java.util.Locale
import kotlin.math.roundToInt

/** Content never stretches wider than this, so tablets and TVs get a centred, readable layout. */
private val MaxContentWidth: Dp = 1000.dp

/** Screens shorter than this (after system bars) use tighter paddings and smaller text. */
private val CompactHeight: Dp = 360.dp

private class CharacterActions(
    val onBack: () -> Unit,
    val onStep: (Int) -> Unit,
    val onSelect: () -> Unit,
    val onUnlock: () -> Unit
)

/** One line of the lifetime statistics list. */
private class StatLine(val labelRes: Int, val value: String, val colorRes: Int)

/**
 * Characters screen, compact and fixed (no scrolling). The game is landscape-only, so everything lives in
 * ONE panel that fills the space under the header:
 *
 *   [<] preview [>]  |  name, role, six spec tiles, ability, Select / Unlock  |  lifetime statistics
 *
 * Arrows flip through the six characters one at a time. Every section uses weights, so the panel
 * shrinks or grows with the screen instead of scrolling.
 */
@Composable
fun CharacterScreen(onBack: () -> Unit, viewModel: CharacterViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sounds = rememberSoundManager()

    val actions = CharacterActions(
        onBack = {
            sounds.play(SoundEffect.UI_CLICK)
            onBack()
        },
        onStep = viewModel::step,
        onSelect = viewModel::select,
        onUnlock = viewModel::unlock
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.survivor_background))
            .safeDrawingPadding()
    ) {
        val compact = maxHeight < CompactHeight
        val pad = if (compact || maxWidth < 400.dp) 8.dp else 14.dp

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxSize()
                    .padding(pad),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Header(actions, state.progress.diamonds)
                CharacterPanel(state, actions, compact, Modifier.weight(1f))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------------------------

@Composable
private fun Header(actions: CharacterActions, diamonds: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val shape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(shape)
                .border(1.dp, colorResource(R.color.neon_border), shape)
                .clickable(onClick = actions.onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = colorResource(R.color.neon_green),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = stringResource(R.string.character_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.neon_title),
            letterSpacing = 3.sp,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        // diamond balance: what the locked characters cost is paid from here
        val pill = RoundedCornerShape(50)
        Row(
            modifier = Modifier
                .clip(pill)
                .background(colorResource(R.color.neon_panel))
                .border(1.dp, colorResource(R.color.neon_border), pill)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(DiamondIcon, contentDescription = null, tint = colorResource(R.color.powerup_diamond), modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.character_diamonds_format, diamonds),
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.neon_title),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// The single panel. Every character uses exactly the same slots; only the values differ:
//   preview + dots | name, role, six spec tiles, ability box, one button | lifetime statistics
// ---------------------------------------------------------------------------------------------

@Composable
private fun CharacterPanel(
    state: CharacterUiState,
    actions: CharacterActions,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colorResource(R.color.neon_panel))
            .border(1.dp, colorResource(R.color.neon_border), shape)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. preview between the arrows, page dots underneath
        Column(
            modifier = Modifier.weight(0.8f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ArrowButton(Icons.Filled.ChevronLeft, R.string.character_previous) { actions.onStep(-1) }
                SurvivorArt(
                    state.viewing,
                    Modifier.weight(1f).fillMaxHeight().padding(4.dp).alpha(if (state.owned) 1f else 0.5f)
                )
                ArrowButton(Icons.Filled.ChevronRight, R.string.character_next) { actions.onStep(1) }
            }
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (entry in GameCharacter.entries) PageDot(selected = entry == state.viewing)
            }
        }

        // 2. the character: name, role, specs, ability, action
        CharacterInfo(state, actions, compact, Modifier.weight(1.45f).fillMaxHeight().padding(horizontal = 6.dp))

        // 3. lifetime statistics as a compact list
        LifetimeStats(state, compact, Modifier.weight(0.95f).fillMaxHeight().padding(start = 2.dp, end = 6.dp))
    }
}

@Composable
private fun ArrowButton(icon: ImageVector, descriptionRes: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            icon,
            contentDescription = stringResource(descriptionRes),
            tint = colorResource(R.color.neon_green_dim),
            modifier = Modifier.size(30.dp)
        )
    }
}

/** Fixed order of slots, the same for all six characters. The ability box absorbs any spare height. */
@Composable
private fun CharacterInfo(state: CharacterUiState, actions: CharacterActions, compact: Boolean, modifier: Modifier = Modifier) {
    val character = state.viewing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)) {
        Column {
            Text(
                stringResource(character.nameRes()),
                style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.neon_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stringResource(character.roleRes()),
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(R.color.neon_label),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        SpecGrid(character)
        AbilityBox(character.ability, Modifier.weight(1f))
        CharacterActionButton(state, actions)
    }
}

// ---- Specs: always the same six tiles in the same order -------------------------------------

/** [good] > 0 is an advantage (green), < 0 a drawback (red), 0 means unchanged. */
private class SpecLine(val labelRes: Int, val value: String, val good: Int)

@Composable
private fun percentText(percent: Int): String =
    if (percent == 0) stringResource(R.string.char_spec_none) else stringResource(R.string.char_spec_percent_format, percent)

@Composable
private fun SpecGrid(c: GameCharacter) {
    fun pct(multiplier: Float): Int = ((multiplier - 1f) * 100f).roundToInt()
    val health = pct(c.healthMult)
    val speed = pct(c.speedMult)
    val damageTaken = pct(c.damageTakenMult)
    val reload = pct(c.reloadTimeMult)
    val magazine = pct(c.magazineMult)
    val regen = if (c.regenPerSecond > 0f) {
        stringResource(R.string.char_spec_regen_format, String.format(Locale.getDefault(), "%.1f", c.regenPerSecond))
    } else {
        stringResource(R.string.char_spec_none)
    }

    val specs = listOf(
        SpecLine(R.string.char_spec_health, percentText(health), Integer.signum(health)),
        SpecLine(R.string.char_spec_speed, percentText(speed), Integer.signum(speed)),
        SpecLine(R.string.char_spec_damage_taken, percentText(damageTaken), -Integer.signum(damageTaken)), // less is better
        SpecLine(R.string.char_spec_regen, regen, if (c.regenPerSecond > 0f) 1 else 0),
        SpecLine(R.string.char_spec_reload, percentText(reload), -Integer.signum(reload)), // shorter is better
        SpecLine(R.string.char_spec_magazine, percentText(magazine), Integer.signum(magazine))
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in specs.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (spec in row) SpecTile(spec, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpecTile(spec: SpecLine, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val valueColor = when {
        spec.good > 0 -> colorResource(R.color.neon_green)
        spec.good < 0 -> MaterialTheme.colorScheme.error
        else -> colorResource(R.color.neon_label)
    }
    Row(
        modifier = modifier
            .height(26.dp)
            .clip(shape)
            .background(colorResource(R.color.survivor_background))
            .border(1.dp, colorResource(R.color.neon_border), shape)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            stringResource(spec.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = colorResource(R.color.neon_label),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            spec.value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1
        )
    }
}

// ---- Ability: icon, name, cooldown, description in one box ----------------------------------

@Composable
private fun AbilityBox(ability: AbilityType, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colorResource(R.color.survivor_background))
            .border(1.dp, colorResource(R.color.neon_border), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            ability.icon(),
            contentDescription = null,
            tint = colorResource(R.color.control_ability),
            modifier = Modifier.size(26.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(ability.nameRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.neon_title),
                    maxLines = 1
                )
                Text(
                    stringResource(R.string.character_cooldown_format, ability.cooldownSeconds.roundToInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorResource(R.color.neon_green_dim),
                    maxLines = 1
                )
            }
            Text(
                stringResource(ability.descriptionRes()),
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(R.color.neon_label),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---- Action: ONE full-width, fixed-height button; only its state changes ---------------------

@Composable
private fun CharacterActionButton(state: CharacterUiState, actions: CharacterActions) {
    val shape = RoundedCornerShape(10.dp)
    val slot = Modifier.fillMaxWidth().height(38.dp)
    when {
        // already chosen: same slot, outlined and not clickable
        state.selected -> Box(
            modifier = slot.clip(shape).border(1.5.dp, colorResource(R.color.neon_green), shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.character_selected).uppercase(),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colorResource(R.color.neon_green)
            )
        }
        state.owned -> Button(
            onClick = actions.onSelect,
            shape = shape,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            modifier = slot,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.neon_green),
                contentColor = colorResource(R.color.survivor_on_accent)
            )
        ) {
            Text(stringResource(R.string.character_select).uppercase(), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        // locked: always the diamond + one line; the text says what is missing when it is too expensive
        else -> Button(
            onClick = actions.onUnlock,
            shape = shape,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            modifier = slot,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(if (state.canAfford) R.color.powerup_diamond else R.color.neon_border),
                contentColor = colorResource(if (state.canAfford) R.color.survivor_on_accent else R.color.neon_label)
            )
        ) {
            Icon(DiamondIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (state.canAfford) {
                    stringResource(R.string.character_unlock_format, state.viewing.diamondPrice)
                } else {
                    stringResource(R.string.character_need_more_format, state.viewing.diamondPrice - state.progress.diamonds)
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PageDot(selected: Boolean) {
    val width by animateDpAsState(if (selected) 22.dp else 6.dp, label = "dotWidth")
    val color by animateColorAsState(
        colorResource(if (selected) R.color.neon_green else R.color.survivor_dot_off),
        label = "dotColor"
    )
    Box(modifier = Modifier.size(width = width, height = 6.dp).clip(CircleShape).background(color))
}

// ---------------------------------------------------------------------------------------------
// Lifetime statistics: one compact list inside the panel (label left, value right)
// ---------------------------------------------------------------------------------------------

@Composable
private fun LifetimeStats(state: CharacterUiState, compact: Boolean, modifier: Modifier = Modifier) {
    val stats = state.stats
    val lines = listOf(
        StatLine(R.string.stat_highest_wave, number(state.progress.highestWave), R.color.neon_green),
        StatLine(R.string.stat_kills, number(stats.kills), R.color.neon_green),
        StatLine(R.string.stat_runs, number(stats.runs), R.color.neon_title),
        StatLine(R.string.stat_bosses, number(stats.bossKills), R.color.neon_title),
        StatLine(R.string.stat_accuracy, stringResource(R.string.stat_accuracy_format, stats.accuracyPercent), R.color.neon_title),
        StatLine(R.string.stat_coins_earned, number(stats.coinsEarned), R.color.coin_gold),
        StatLine(R.string.stat_time, playTime(stats), R.color.neon_title)
    )
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colorResource(R.color.survivor_background))
            .border(1.dp, colorResource(R.color.neon_border), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        for (line in lines) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    stringResource(line.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorResource(R.color.neon_label),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    line.value,
                    style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(line.colorRes),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun number(value: Int): String = stringResource(R.string.stat_number_format, value)

@Composable
private fun playTime(stats: PlayerStats): String = when {
    stats.playHours > 0 -> stringResource(R.string.stat_time_hours_format, stats.playHours, stats.playMinutes)
    stats.playMinutes > 0 -> stringResource(R.string.stat_time_minutes_format, stats.playMinutes)
    else -> stringResource(R.string.stat_time_none)
}

// ---------------------------------------------------------------------------------------------
// Character preview: the in-game top-down player, drawn on a 120 x 120 unit grid facing right,
// scaled to fit whatever box it gets and centred in it. Outfit colours animate when the character changes.
// ---------------------------------------------------------------------------------------------

@Composable
private fun SurvivorArt(character: GameCharacter, modifier: Modifier = Modifier) {
    val body by animateColorAsState(colorResource(character.bodyColorRes()), label = "body")
    val arm by animateColorAsState(colorResource(character.armColorRes()), label = "arm")
    val hair by animateColorAsState(colorResource(character.hairColorRes()), label = "hair")
    val skinTone = colorResource(R.color.player_skin)
    val weapon = colorResource(R.color.player_weapon)
    val ring = colorResource(R.color.neon_border)
    val shadow = colorResource(R.color.survivor_ground_shadow)

    Canvas(modifier = modifier) {
        val s = minOf(size.width, size.height) / 120f
        val ox = (size.width - 120f * s) / 2f
        val oy = (size.height - 120f * s) / 2f

        fun at(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
        fun sz(w: Float, h: Float) = Size(w * s, h * s)

        // dashed ground ring and soft shadow
        drawCircle(
            color = ring,
            radius = 54f * s,
            center = at(60f, 60f),
            style = Stroke(width = 1.5f * s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * s, 5f * s)))
        )
        drawOval(shadow, at(32f, 95f), sz(52f, 10f))

        // arms and weapon reach to the right
        drawRoundRect(arm, at(58f, 42f), sz(34f, 11f), CornerRadius(5f * s))
        drawRoundRect(arm, at(58f, 67f), sz(30f, 11f), CornerRadius(5f * s))
        drawRoundRect(weapon, at(80f, 54f), sz(34f, 10f), CornerRadius(2f * s))

        // jacket, head and hair
        drawOval(body, at(35f, 30f), sz(40f, 60f))
        drawCircle(skinTone, 13f * s, at(58f, 60f))
        drawOval(hair, at(44f, 47f), sz(18f, 26f))
    }
}