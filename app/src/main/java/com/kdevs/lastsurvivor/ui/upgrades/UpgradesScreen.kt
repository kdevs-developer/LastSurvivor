package com.kdevs.lastsurvivor.ui.upgrades

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.weapons.UpgradeConfig
import com.kdevs.lastsurvivor.game.weapons.UpgradeTrack
import com.kdevs.lastsurvivor.game.weapons.WeaponStats
import com.kdevs.lastsurvivor.game.weapons.WeaponType
import com.kdevs.lastsurvivor.ui.weapons.descriptionRes
import com.kdevs.lastsurvivor.ui.weapons.labelRes
import com.kdevs.lastsurvivor.ui.weapons.nameRes

/** Content never stretches wider than this, so tablets and TVs get a centred, readable layout. */
private val MaxContentWidth: Dp = 1000.dp

/** Landscape screens shorter than this (after system bars) scroll instead of squeezing the panel. */
private val ShortHeight: Dp = 330.dp

/** Landscape screens shorter than this use tighter paddings and hide the card icons. */
private val CompactHeight: Dp = 400.dp

private class ArmoryActions(
    val onBack: () -> Unit,
    val onSelect: (WeaponType) -> Unit,
    val onStep: (Int) -> Unit,
    val onUnlock: () -> Unit,
    val onEquip: () -> Unit,
    val onUpgrade: (UpgradeTrack) -> Unit
)

/**
 * Armory "Weapon Showcase". Adapts to the space it gets:
 *  - landscape: tabs and coins on top, big weapon panel (fills the height), four upgrade cards below;
 *  - short landscape: same, but the page scrolls and the panel keeps a fixed height;
 *  - portrait: back and coins, tabs, stacked weapon panel, upgrade cards in a 2 x 2 grid (scrolls),
 *    with the Equip / Unlock button pinned to the bottom of the screen.
 */
@Composable
fun UpgradesScreen(onBack: () -> Unit, viewModel: UpgradesViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = ArmoryActions(
        onBack = onBack,
        onSelect = { viewModel.select(it) },
        onStep = { delta ->
            val types = WeaponType.entries
            val index = types.indexOf(state.selected)
            viewModel.select(types[(index + delta + types.size) % types.size])
        },
        onUnlock = { viewModel.unlock(state.selected) },
        onEquip = { viewModel.equip(state.selected) },
        onUpgrade = { track -> viewModel.upgrade(state.selected, track) }
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.armory_background))
            .safeDrawingPadding()
    ) {
        val portrait = maxHeight > maxWidth
        val short = !portrait && maxHeight < ShortHeight
        val compact = !portrait && maxHeight < CompactHeight
        val scrolling = portrait || short
        val pad = if (compact || maxWidth < 400.dp) 10.dp else 16.dp

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (scrolling) {
                Column(modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(pad),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HeaderBar(state, actions, stacked = portrait)
                        ShowcasePanel(
                            state = state,
                            actions = actions,
                            stacked = portrait,
                            modifier = if (portrait) Modifier else Modifier.height(190.dp)
                        )
                        UpgradeCards(state, actions, columns = if (portrait) 2 else 4, compact = compact)
                    }
                    // Portrait: the Equip / Unlock button is pinned to the bottom, never scrolls away.
                    if (portrait) {
                        WeaponAction(
                            state, actions,
                            Modifier.fillMaxWidth().padding(horizontal = pad).padding(top = 6.dp, bottom = 10.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = MaxContentWidth)
                        .fillMaxSize()
                        .padding(pad),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeaderBar(state, actions, stacked = false)
                    ShowcasePanel(state, actions, stacked = false, modifier = Modifier.weight(1f))
                    UpgradeCards(state, actions, columns = 4, compact = compact)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Header: back, weapon tabs, coins
// ---------------------------------------------------------------------------------------------

@Composable
private fun HeaderBar(state: UpgradesUiState, actions: ArmoryActions, stacked: Boolean) {
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BackButton(actions.onBack)
                Spacer(Modifier.weight(1f))
                CoinChip(state.coins)
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WeaponTabs(state, actions)
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BackButton(actions.onBack)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                WeaponTabs(state, actions)
            }
            CoinChip(state.coins)
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .border(1.dp, colorResource(R.color.neon_border), shape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.common_back),
            tint = colorResource(R.color.neon_green),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CoinChip(coins: Int) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(colorResource(R.color.neon_panel))
            .border(1.dp, colorResource(R.color.neon_border), shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = colorResource(R.color.coin_gold), modifier = Modifier.size(20.dp))
        Text(
            stringResource(R.string.upgrades_coins_format, coins),
            color = colorResource(R.color.coin_gold),
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Underline tabs that scroll sideways. The selected weapon is always brought to the middle of the row, so
 * the one being viewed stays centred and the others sit either side of it (also with the arrows, which
 * select the next or previous weapon).
 */
@Composable
private fun WeaponTabs(state: UpgradesUiState, actions: ArmoryActions) {
    val line = colorResource(R.color.neon_border)
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    var viewportPx by remember { mutableIntStateOf(0) }
    // where each tab's centre is inside the row (pixels), filled in as the tabs are laid out
    val centres = remember { mutableStateMapOf<WeaponType, Float>() }
    val selectedCentre = centres[state.selected]

    // With half a viewport of padding on both sides, scrolling to a tab's centre puts it in the middle.
    LaunchedEffect(state.selected, selectedCentre, viewportPx, scroll.maxValue) {
        if (selectedCentre != null && viewportPx > 0) {
            scroll.animateScrollTo(selectedCentre.roundToInt().coerceIn(0, scroll.maxValue))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { viewportPx = it.width }
            .drawBehind {
                drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scroll)
                .padding(horizontal = with(density) { (viewportPx / 2f).toDp() }),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            for (type in WeaponType.entries) {
                WeaponTab(
                    type = type,
                    selected = type == state.selected,
                    owned = state.inventory.owns(type),
                    onClick = { actions.onSelect(type) },
                    modifier = Modifier.onGloballyPositioned { centres[type] = it.positionInParent().x + it.size.width / 2f }
                )
            }
        }
    }
}

@Composable
private fun WeaponTab(type: WeaponType, selected: Boolean, owned: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = colorResource(R.color.neon_green)
    val color by animateColorAsState(
        targetValue = when {
            selected -> accent
            owned -> colorResource(R.color.neon_label)
            else -> colorResource(R.color.armory_locked)
        },
        label = "weaponTabColor"
    )
    Column(
        modifier = modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = stringResource(type.nameRes()).uppercase(),
                color = color,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
            if (!owned) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            }
        }
        Box(
            modifier = Modifier
                .height(3.dp)
                .fillMaxWidth()
                .background(if (selected) accent else colorResource(R.color.transparent))
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Showcase panel
// ---------------------------------------------------------------------------------------------

/**
 * [stacked] = weapon art on top with the arrows beside it, details underneath (portrait).
 * Otherwise art and details sit side by side and the details scroll if they do not fit.
 */
@Composable
private fun ShowcasePanel(
    state: UpgradesUiState,
    actions: ArmoryActions,
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
                ArrowButton(Icons.Filled.ChevronLeft, R.string.armory_previous) { actions.onStep(-1) }
                WeaponArtBox(state, Modifier.weight(1f).height(130.dp))
                ArrowButton(Icons.Filled.ChevronRight, R.string.armory_next) { actions.onStep(1) }
            }
            WeaponDetails(state, Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp))
        }
    } else {
        Row(modifier = panel.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(Icons.Filled.ChevronLeft, R.string.armory_previous) { actions.onStep(-1) }
            WeaponArtBox(state, Modifier.weight(1f).fillMaxHeight())
            Column(
                modifier = Modifier.weight(1.15f).fillMaxHeight().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    WeaponDetails(state, Modifier.verticalScroll(rememberScrollState()))
                }
                // Pinned: stays visible even when the details above scroll.
                WeaponAction(state, actions, Modifier.fillMaxWidth())
            }
            ArrowButton(Icons.Filled.ChevronRight, R.string.armory_next) { actions.onStep(1) }
        }
    }
}

@Composable
private fun WeaponArtBox(state: UpgradesUiState, modifier: Modifier = Modifier) {
    Crossfade(targetState = state.selected, modifier = modifier, label = "weaponArt") { shown ->
        Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
            WeaponArt(type = shown, owned = state.inventory.owns(shown), modifier = Modifier.fillMaxSize())
        }
    }
}

/** Name, description, stats and the locked hint. The Equip / Unlock button is separate ([WeaponAction]). */
@Composable
private fun WeaponDetails(state: UpgradesUiState, modifier: Modifier = Modifier) {
    val type = state.selected
    val inventory = state.inventory
    val owned = inventory.owns(type)
    val equipped = type == inventory.equipped
    val stats = inventory.statsFor(type)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(type.nameRes()),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.neon_title)
            )
            if (equipped) {
                Text(
                    stringResource(R.string.upgrades_equipped),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.armory_on_accent),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colorResource(R.color.neon_green))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Text(
            text = stringResource(type.descriptionRes()),
            style = MaterialTheme.typography.bodySmall,
            color = colorResource(R.color.neon_label),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(UpgradeTrack.DAMAGE, stats, Modifier.weight(1f))
            StatItem(UpgradeTrack.FIRE_RATE, stats, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(UpgradeTrack.MAGAZINE, stats, Modifier.weight(1f))
            StatItem(UpgradeTrack.RELOAD, stats, Modifier.weight(1f))
        }
        if (!owned) {
            Text(
                stringResource(R.string.upgrades_locked_hint),
                style = MaterialTheme.typography.labelSmall,
                color = colorResource(R.color.armory_locked)
            )
        }
    }
}

/** The Equip / Unlock / Equipped button for the selected weapon, always placed outside any scroll area. */
@Composable
private fun WeaponAction(state: UpgradesUiState, actions: ArmoryActions, modifier: Modifier = Modifier) {
    val type = state.selected
    val owned = state.inventory.owns(type)
    EquipButton(
        owned = owned,
        equipped = type == state.inventory.equipped,
        price = type.price,
        canAffordUnlock = state.coins >= type.price,
        onUnlock = actions.onUnlock,
        onEquip = actions.onEquip,
        modifier = modifier
    )
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

@Composable
private fun StatItem(track: UpgradeTrack, stats: WeaponStats, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            stringResource(track.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            color = colorResource(R.color.neon_label),
            maxLines = 1
        )
        Text(
            statText(track, stats),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.neon_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Unlock (gold) when not owned, Equip (neon) when owned, a disabled outlined Equipped when active. */
@Composable
private fun EquipButton(
    owned: Boolean,
    equipped: Boolean,
    price: Int,
    canAffordUnlock: Boolean,
    onUnlock: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = colorResource(R.color.neon_green)
    val onAccent = colorResource(R.color.armory_on_accent)
    val buttonModifier = modifier.height(44.dp)
    when {
        !owned -> Button(
            onClick = onUnlock,
            enabled = canAffordUnlock,
            modifier = buttonModifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.coin_gold),
                contentColor = onAccent,
                disabledContainerColor = colorResource(R.color.armory_locked_border),
                disabledContentColor = colorResource(R.color.armory_locked)
            )
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("  " + stringResource(R.string.upgrades_unlock_format, price), fontWeight = FontWeight.Bold, maxLines = 1)
        }

        equipped -> OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = buttonModifier,
            border = BorderStroke(1.dp, accent),
            colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = accent)
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("  " + stringResource(R.string.upgrades_equipped), fontWeight = FontWeight.Bold, maxLines = 1)
        }

        else -> Button(
            onClick = onEquip,
            modifier = buttonModifier,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent)
        ) {
            Text(stringResource(R.string.upgrades_equip), fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Upgrade cards
// ---------------------------------------------------------------------------------------------

/** The four track cards in rows of [columns] (4 in landscape, 2 in portrait). */
@Composable
private fun UpgradeCards(state: UpgradesUiState, actions: ArmoryActions, columns: Int, compact: Boolean) {
    val inventory = state.inventory
    val type = state.selected
    val owned = inventory.owns(type)
    val stats = inventory.statsFor(type)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rowTracks in UpgradeTrack.entries.chunked(columns)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (track in rowTracks) {
                    val level = inventory.level(type, track)
                    val maxed = level >= UpgradeConfig.MAX_LEVEL
                    val cost = UpgradeConfig.cost(type, level)
                    val next: WeaponStats? = if (maxed) null else
                        WeaponStats.of(type, inventory.levelsOf(type) + (track to level + 1))
                    UpgradeCard(
                        track = track,
                        level = level,
                        current = stats,
                        next = next,
                        cost = cost,
                        owned = owned,
                        canAfford = state.coins >= cost,
                        compact = compact,
                        onUpgrade = { actions.onUpgrade(track) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    track: UpgradeTrack,
    level: Int,
    current: WeaponStats,
    next: WeaponStats?,
    cost: Int,
    owned: Boolean,
    canAfford: Boolean,
    compact: Boolean,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val currentText = statText(track, current)
    val valueText = if (next != null) {
        stringResource(R.string.upgrades_stat_change_format, currentText, statText(track, next))
    } else currentText
    val buttonHeight = if (compact) 32.dp else 36.dp

    Column(
        modifier = modifier
            .alpha(if (owned) 1f else 0.5f)
            .clip(shape)
            .background(colorResource(R.color.neon_panel))
            .border(1.dp, colorResource(if (next == null) R.color.neon_green_dim else R.color.neon_border), shape)
            .padding(horizontal = 8.dp, vertical = if (compact) 6.dp else 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)
    ) {
        if (!compact) {
            Icon(track.icon(), contentDescription = null, tint = colorResource(R.color.neon_green), modifier = Modifier.size(20.dp))
        }
        Text(
            stringResource(track.labelRes()),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.neon_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until UpgradeConfig.MAX_LEVEL) {
                Pip(on = i < level)
            }
        }
        Text(
            valueText,
            style = MaterialTheme.typography.labelSmall,
            color = colorResource(R.color.neon_label),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (next == null) {
            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                border = BorderStroke(1.dp, colorResource(R.color.neon_green)),
                colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = colorResource(R.color.neon_green))
            ) {
                Text(stringResource(R.string.upgrades_max), fontWeight = FontWeight.Bold, maxLines = 1)
            }
        } else {
            Button(
                onClick = onUpgrade,
                enabled = owned && canAfford,
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.neon_green),
                    contentColor = colorResource(R.color.armory_on_accent),
                    disabledContainerColor = colorResource(R.color.armory_locked_border),
                    disabledContentColor = colorResource(R.color.armory_locked)
                )
            ) {
                Text(
                    stringResource(R.string.upgrades_buy_format, cost),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun Pip(on: Boolean) {
    val color by animateColorAsState(
        targetValue = colorResource(if (on) R.color.upgrade_pip_on else R.color.armory_pip_off),
        label = "pip"
    )
    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
}

private fun UpgradeTrack.icon(): ImageVector = when (this) {
    UpgradeTrack.DAMAGE -> Icons.Filled.Bolt
    UpgradeTrack.FIRE_RATE -> Icons.Filled.LocalFireDepartment
    UpgradeTrack.MAGAZINE -> Icons.Filled.Layers
    UpgradeTrack.RELOAD -> Icons.Filled.Autorenew
}

@Composable
private fun statText(track: UpgradeTrack, stats: WeaponStats): String = when (track) {
    UpgradeTrack.DAMAGE -> stringResource(R.string.stat_damage_format, stats.damage)
    UpgradeTrack.FIRE_RATE -> stringResource(R.string.stat_fire_rate_format, stats.shotsPerSecond)
    UpgradeTrack.MAGAZINE -> stringResource(R.string.stat_magazine_format, stats.magazine)
    UpgradeTrack.RELOAD -> stringResource(R.string.stat_reload_format, stats.reloadSeconds)
}

// ---------------------------------------------------------------------------------------------
// Weapon silhouettes: drawn on a 120 x 62 unit grid (muzzle pointing right), scaled to fit
// whatever box they get and centred in it, so they never stretch or overflow.
// ---------------------------------------------------------------------------------------------

@Composable
private fun WeaponArt(type: WeaponType, owned: Boolean, modifier: Modifier = Modifier) {
    val body = colorResource(if (owned) R.color.neon_border else R.color.armory_weapon_locked)
    val accent = colorResource(if (owned) R.color.neon_green else R.color.armory_locked)

    Canvas(modifier = modifier) {
        val s = minOf(size.width / 120f, size.height / 62f)
        val ox = (size.width - 120f * s) / 2f
        val oy = (size.height - 62f * s) / 2f

        fun rect(x: Float, y: Float, w: Float, h: Float, color: Color = body) =
            drawRect(color, Offset(ox + x * s, oy + y * s), Size(w * s, h * s))

        fun poly(vararg p: Float) {
            val path = Path().apply {
                moveTo(ox + p[0] * s, oy + p[1] * s)
                var i = 2
                while (i < p.size) {
                    lineTo(ox + p[i] * s, oy + p[i + 1] * s)
                    i += 2
                }
                close()
            }
            drawPath(path, body)
        }

        when (type) {
            WeaponType.PISTOL -> {
                rect(8f, 12f, 92f, 15f)
                rect(100f, 16f, 12f, 7f)
                rect(14f, 27f, 46f, 8f)
                poly(16f, 35f, 44f, 35f, 36f, 58f, 20f, 58f)
                rect(8f, 12f, 92f, 3f, accent)
                val guard = Path().apply {
                    moveTo(ox + 46f * s, oy + 35f * s)
                    lineTo(ox + 46f * s, oy + 43f * s)
                    quadraticTo(ox + 46f * s, oy + 47f * s, ox + 50f * s, oy + 47f * s)
                    lineTo(ox + 58f * s, oy + 47f * s)
                }
                drawPath(guard, accent, style = Stroke(width = 2f * s))
            }

            WeaponType.SMG -> {
                rect(2f, 14f, 8f, 8f)
                rect(10f, 12f, 84f, 15f)
                rect(94f, 16f, 18f, 6f)
                rect(34f, 27f, 10f, 26f)
                poly(60f, 27f, 72f, 27f, 68f, 50f, 62f, 50f)
                rect(10f, 12f, 84f, 3f, accent)
            }

            WeaponType.SHOTGUN -> {
                rect(30f, 14f, 86f, 5f)
                rect(30f, 19f, 34f, 9f)
                rect(64f, 19f, 28f, 7f)
                poly(4f, 17f, 30f, 17f, 30f, 30f, 14f, 38f, 4f, 30f)
                poly(36f, 28f, 46f, 28f, 42f, 46f, 34f, 46f)
                rect(30f, 14f, 86f, 2f, accent)
            }

            WeaponType.RIFLE -> {
                poly(2f, 16f, 22f, 16f, 22f, 30f, 2f, 34f)
                rect(22f, 14f, 60f, 12f)
                rect(82f, 17f, 32f, 5f)
                rect(36f, 8f, 26f, 5f)
                rect(40f, 13f, 4f, 2f)
                rect(52f, 26f, 8f, 22f)
                poly(30f, 26f, 40f, 26f, 36f, 46f, 30f, 46f)
                rect(22f, 14f, 60f, 3f, accent)
            }

            WeaponType.REVOLVER -> {
                rect(30f, 14f, 80f, 9f)
                rect(104f, 17f, 10f, 4f)
                rect(26f, 12f, 26f, 18f)
                poly(8f, 20f, 26f, 18f, 30f, 30f, 22f, 56f, 8f, 56f, 12f, 34f)
                rect(30f, 14f, 80f, 2f, accent)
                rect(36f, 18f, 8f, 4f, accent)
            }

            WeaponType.ASSAULT -> {
                poly(2f, 14f, 22f, 16f, 22f, 32f, 4f, 36f)
                rect(22f, 12f, 52f, 14f)
                rect(70f, 15f, 34f, 9f)
                rect(104f, 17f, 12f, 5f)
                poly(46f, 26f, 56f, 26f, 62f, 54f, 52f, 54f)
                poly(30f, 26f, 40f, 26f, 36f, 44f, 30f, 44f)
                rect(22f, 12f, 52f, 3f, accent)
                rect(70f, 15f, 34f, 3f, accent)
            }

            WeaponType.AUTO_SHOTGUN -> {
                rect(34f, 14f, 82f, 6f)
                rect(30f, 20f, 40f, 12f)
                rect(70f, 20f, 36f, 8f)
                poly(4f, 17f, 30f, 17f, 30f, 32f, 14f, 40f, 4f, 32f)
                poly(42f, 32f, 54f, 32f, 56f, 52f, 40f, 52f)
                rect(34f, 14f, 82f, 2f, accent)
                rect(48f, 22f, 18f, 4f, accent)
            }

            WeaponType.SNIPER -> {
                poly(2f, 18f, 24f, 18f, 24f, 32f, 4f, 36f)
                rect(24f, 16f, 44f, 10f)
                rect(68f, 19f, 50f, 4f)
                rect(30f, 6f, 30f, 7f)
                rect(34f, 13f, 4f, 3f)
                rect(52f, 13f, 4f, 3f)
                poly(40f, 26f, 48f, 26f, 44f, 44f, 38f, 44f)
                rect(24f, 16f, 44f, 3f, accent)
                rect(30f, 6f, 30f, 2f, accent)
            }

            WeaponType.MINIGUN -> {
                rect(18f, 8f, 34f, 22f)
                rect(52f, 11f, 60f, 4f)
                rect(52f, 17f, 60f, 4f)
                rect(52f, 23f, 60f, 4f)
                rect(106f, 9f, 8f, 20f)
                poly(2f, 14f, 18f, 14f, 18f, 30f, 2f, 30f)
                poly(26f, 30f, 38f, 30f, 36f, 52f, 28f, 52f)
                rect(18f, 8f, 34f, 3f, accent)
                rect(52f, 11f, 60f, 2f, accent)
            }
        }
    }
}