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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
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

/** Landscape screens shorter than this (after system bars) scroll instead of squeezing the panel. */
private val ShortHeight: Dp = 330.dp

/** Landscape screens shorter than this use tighter paddings and smaller stat numbers. */
private val CompactHeight: Dp = 400.dp

private class CharacterActions(
    val onBack: () -> Unit,
    val onStep: (Int) -> Unit,
    val onSelect: () -> Unit,
    val onUnlock: () -> Unit
)

/** One tile of the statistics grid; [span] is how many grid columns it takes. */
private class StatTile(
    val labelRes: Int,
    val value: String,
    val highlight: Boolean = false,
    val gold: Boolean = false,
    val span: Int = 1
)

/**
 * Characters screen: a showcase of one character at a time (arrows flip through the six), with its stats,
 * its ability and a Select / Unlock button, and the lifetime statistics as tiles. Locked characters are
 * bought with diamonds. Adapts to the space it gets:
 *  - landscape: header, showcase panel (fills the height), stats in two rows of four columns;
 *  - short landscape: the page scrolls and the panel keeps a fixed height;
 *  - portrait: the character sits above its details and the stats form a two-column grid (scrolls).
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
        val portrait = maxHeight > maxWidth
        val short = !portrait && maxHeight < ShortHeight
        val compact = !portrait && maxHeight < CompactHeight
        val scrolling = portrait || short
        val pad = if (compact || maxWidth < 400.dp) 10.dp else 16.dp
        val columns = if (portrait) 2 else 4

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (scrolling) {
                Column(
                    modifier = Modifier
                        .widthIn(max = MaxContentWidth)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(pad),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Header(actions, state.progress.diamonds)
                    ShowcasePanel(
                        state = state,
                        actions = actions,
                        stacked = portrait,
                        modifier = if (portrait) Modifier else Modifier.height(170.dp)
                    )
                    StatsGrid(state, columns = columns, compact = compact)
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = MaxContentWidth)
                        .fillMaxSize()
                        .padding(pad),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Header(actions, state.progress.diamonds)
                    ShowcasePanel(state, actions, stacked = false, modifier = Modifier.weight(1f))
                    StatsGrid(state, columns = columns, compact = compact)
                }
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
                .size(40.dp)
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
                .padding(horizontal = 12.dp, vertical = 6.dp),
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
// Showcase panel
// ---------------------------------------------------------------------------------------------

/**
 * [stacked] = the character on top with the arrows beside it, details underneath (portrait).
 * Otherwise the character and details sit side by side between the arrows.
 */
@Composable
private fun ShowcasePanel(
    state: CharacterUiState,
    actions: CharacterActions,
    stacked: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val panel = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(colorResource(R.color.neon_panel))
        .border(1.dp, colorResource(R.color.neon_border), shape)

    if (stacked) {
        Column(modifier = panel.padding(horizontal = 6.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArrowButton(Icons.Filled.ChevronLeft, R.string.character_previous) { actions.onStep(-1) }
                SurvivorArt(state.viewing, Modifier.weight(1f).height(150.dp).padding(8.dp).alpha(if (state.owned) 1f else 0.5f))
                ArrowButton(Icons.Filled.ChevronRight, R.string.character_next) { actions.onStep(1) }
            }
            CharacterInfo(state, actions, Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), centered = true)
        }
    } else {
        Row(modifier = panel.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(Icons.Filled.ChevronLeft, R.string.character_previous) { actions.onStep(-1) }
            SurvivorArt(state.viewing, Modifier.weight(1f).fillMaxHeight().padding(10.dp).alpha(if (state.owned) 1f else 0.5f))
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                CharacterInfo(state, actions, Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp), centered = false)
            }
            ArrowButton(Icons.Filled.ChevronRight, R.string.character_next) { actions.onStep(1) }
        }
    }
}

@Composable
private fun ArrowButton(icon: ImageVector, descriptionRes: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(
            icon,
            contentDescription = stringResource(descriptionRes),
            tint = colorResource(R.color.neon_green_dim),
            modifier = Modifier.size(32.dp)
        )
    }
}

/** Name, role, stat changes, the ability, page dots and the Select / Unlock button. */
@Composable
private fun CharacterInfo(state: CharacterUiState, actions: CharacterActions, modifier: Modifier = Modifier, centered: Boolean) {
    val character = state.viewing
    val all = GameCharacter.entries
    val horizontal = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    Column(
        modifier = modifier,
        horizontalAlignment = horizontal,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            stringResource(character.nameRes()),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.neon_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            stringResource(character.roleRes()),
            style = MaterialTheme.typography.bodySmall,
            color = colorResource(R.color.neon_label),
            textAlign = textAlign
        )
        Text(
            traitsText(character),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.neon_green_dim),
            textAlign = textAlign
        )
        AbilityRow(character.ability)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            for (entry in all) {
                PageDot(selected = entry == character)
            }
        }
        Text(
            stringResource(R.string.character_index_format, all.indexOf(character) + 1, all.size),
            style = MaterialTheme.typography.labelSmall,
            color = colorResource(R.color.neon_label)
        )
        CharacterActionButton(state, actions)
    }
}

/** What the character does better or worse than Survivor, as one line such as "Health -25% - Speed +25%". */
@Composable
private fun traitsText(c: GameCharacter): String {
    fun pct(multiplier: Float): Int = ((multiplier - 1f) * 100f).roundToInt()
    val parts = mutableListOf<String>()
    if (c.healthMult != 1f) parts += stringResource(R.string.char_trait_health, pct(c.healthMult))
    if (c.speedMult != 1f) parts += stringResource(R.string.char_trait_speed, pct(c.speedMult))
    if (c.damageTakenMult != 1f) parts += stringResource(R.string.char_trait_damage_taken, pct(c.damageTakenMult))
    if (c.regenPerSecond > 0f) parts += stringResource(R.string.char_trait_regen, String.format(Locale.getDefault(), "%.1f", c.regenPerSecond))
    if (c.reloadTimeMult != 1f) parts += stringResource(R.string.char_trait_reload, pct(c.reloadTimeMult))
    if (c.magazineMult != 1f) parts += stringResource(R.string.char_trait_magazine, pct(c.magazineMult))
    return if (parts.isEmpty()) stringResource(R.string.char_trait_balanced) else parts.joinToString("  \u2022  ")
}

@Composable
private fun AbilityRow(ability: AbilityType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            ability.icon(),
            contentDescription = null,
            tint = colorResource(R.color.control_ability),
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                stringResource(ability.nameRes()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.neon_title)
            )
            Text(
                stringResource(ability.descriptionRes()),
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(R.color.neon_label)
            )
            Text(
                stringResource(R.string.character_cooldown_format, ability.cooldownSeconds.roundToInt()),
                style = MaterialTheme.typography.labelSmall,
                color = colorResource(R.color.neon_green_dim)
            )
        }
    }
}

/** SELECTED badge for the chosen character, Select for an owned one, Unlock (with the diamond price) for a locked one. */
@Composable
private fun CharacterActionButton(state: CharacterUiState, actions: CharacterActions) {
    val shape = RoundedCornerShape(10.dp)
    when {
        state.selected -> Text(
            stringResource(R.string.character_selected).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.survivor_on_accent),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(colorResource(R.color.neon_green))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
        state.owned -> Button(
            onClick = actions.onSelect,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.neon_green),
                contentColor = colorResource(R.color.survivor_on_accent)
            )
        ) {
            Text(stringResource(R.string.character_select).uppercase(), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        else -> {
            Button(
                onClick = actions.onUnlock,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(if (state.canAfford) R.color.powerup_diamond else R.color.neon_border),
                    contentColor = colorResource(if (state.canAfford) R.color.survivor_on_accent else R.color.neon_label)
                )
            ) {
                Icon(DiamondIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.character_unlock_format, state.viewing.diamondPrice),
                    fontWeight = FontWeight.Bold
                )
            }
            if (!state.canAfford) {
                Text(
                    stringResource(R.string.character_need_more_format, state.viewing.diamondPrice - state.progress.diamonds),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorResource(R.color.neon_label)
                )
            }
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
// Statistics grid
// ---------------------------------------------------------------------------------------------

@Composable
private fun StatsGrid(state: CharacterUiState, columns: Int, compact: Boolean) {
    val stats = state.stats
    val tiles = listOf(
        StatTile(R.string.stat_highest_wave, number(state.progress.highestWave), highlight = true),
        StatTile(R.string.stat_kills, number(stats.kills), highlight = true),
        StatTile(R.string.stat_runs, number(stats.runs)),
        StatTile(R.string.stat_bosses, number(stats.bossKills)),
        StatTile(R.string.stat_accuracy, stringResource(R.string.stat_accuracy_format, stats.accuracyPercent)),
        StatTile(R.string.stat_coins_earned, number(stats.coinsEarned), gold = true),
        StatTile(R.string.stat_time, playTime(stats), span = 2)
    )

    // Greedy rows: tiles are added to a row until its columns are used up.
    val rows = mutableListOf<List<StatTile>>()
    var current = mutableListOf<StatTile>()
    var used = 0
    for (tile in tiles) {
        val span = tile.span.coerceAtMost(columns)
        if (used + span > columns) {
            rows += current
            current = mutableListOf()
            used = 0
        }
        current += tile
        used += span
    }
    if (current.isNotEmpty()) rows += current

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (tile in row) {
                    StatTileCard(tile, compact, Modifier.weight(tile.span.coerceAtMost(columns).toFloat()))
                }
            }
        }
    }
}

@Composable
private fun StatTileCard(tile: StatTile, compact: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val valueColor = when {
        tile.highlight -> colorResource(R.color.neon_green)
        tile.gold -> colorResource(R.color.coin_gold)
        else -> colorResource(R.color.neon_title)
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(colorResource(R.color.neon_panel))
            .border(1.dp, colorResource(if (tile.highlight) R.color.neon_green_dim else R.color.neon_border), shape)
            .padding(horizontal = 8.dp, vertical = if (compact) 6.dp else 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(tile.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = colorResource(R.color.neon_label),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            tile.value,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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