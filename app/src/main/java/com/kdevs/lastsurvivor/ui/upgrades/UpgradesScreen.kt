package com.kdevs.lastsurvivor.ui.upgrades

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

/** Weapon list on the left; stats, unlock/equip and upgrades of the selected weapon on the right. */
@Composable
fun UpgradesScreen(onBack: () -> Unit, viewModel: UpgradesViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- Left: title, coins, weapon cards, back ----
        Column(
            modifier = Modifier.weight(0.38f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.upgrades_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            CoinChip(state.coins)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (type in WeaponType.entries) {
                    WeaponCard(
                        type = type,
                        selected = type == state.selected,
                        owned = state.inventory.owns(type),
                        equipped = type == state.inventory.equipped,
                        onClick = { viewModel.select(type) }
                    )
                }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.common_back))
            }
        }

        // ---- Right: selected weapon ----
        WeaponDetail(
            type = state.selected,
            state = state,
            onUnlock = { viewModel.unlock(state.selected) },
            onEquip = { viewModel.equip(state.selected) },
            onUpgrade = { track -> viewModel.upgrade(state.selected, track) },
            modifier = Modifier.weight(0.62f).fillMaxHeight()
        )
    }
}

@Composable
private fun CoinChip(coins: Int) {
    Surface(shape = RoundedCornerShape(50), color = colorResource(R.color.menu_chip_container)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = colorResource(R.color.coin_gold), modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.upgrades_coins_format, coins), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WeaponCard(type: WeaponType, selected: Boolean, owned: Boolean, equipped: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val background = if (selected) colorResource(R.color.upgrade_card_selected) else colorResource(R.color.upgrade_card)
    val borderColor = if (selected) colorResource(R.color.blood_red) else colorResource(R.color.upgrade_pip_off)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, borderColor), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(type.nameRes()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (owned) MaterialTheme.colorScheme.onBackground else colorResource(R.color.upgrade_locked)
        )
        when {
            equipped -> Text(stringResource(R.string.upgrades_equipped), color = colorResource(R.color.upgrade_equipped), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            owned -> Text(stringResource(R.string.upgrades_owned), color = colorResource(R.color.upgrade_text_secondary), style = MaterialTheme.typography.labelMedium)
            else -> Text(stringResource(R.string.upgrades_locked_format, type.price), color = colorResource(R.color.coin_gold), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WeaponDetail(
    type: WeaponType,
    state: UpgradesUiState,
    onUnlock: () -> Unit,
    onEquip: () -> Unit,
    onUpgrade: (UpgradeTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val inventory = state.inventory
    val owned = inventory.owns(type)
    val equipped = type == inventory.equipped
    val stats = inventory.statsFor(type)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colorResource(R.color.upgrade_card))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(type.nameRes()), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            when {
                !owned -> Button(onClick = onUnlock, enabled = state.coins >= type.price) {
                    Text(stringResource(R.string.upgrades_unlock_format, type.price), fontWeight = FontWeight.Bold)
                }
                equipped -> Button(onClick = {}, enabled = false) { Text(stringResource(R.string.upgrades_equipped), fontWeight = FontWeight.Bold) }
                else -> Button(onClick = onEquip) { Text(stringResource(R.string.upgrades_equip), fontWeight = FontWeight.Bold) }
            }
        }
        Text(
            text = stringResource(type.descriptionRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(R.color.upgrade_text_secondary)
        )
        if (!owned) {
            Text(
                text = stringResource(R.string.upgrades_locked_hint),
                style = MaterialTheme.typography.labelLarge,
                color = colorResource(R.color.upgrade_locked)
            )
        }

        for (track in UpgradeTrack.entries) {
            val level = inventory.level(type, track)
            val maxed = level >= UpgradeConfig.MAX_LEVEL
            val cost = UpgradeConfig.cost(type, level)
            // Stats with one more level in this track, to preview the change.
            val next: WeaponStats? = if (maxed) null else
                WeaponStats.of(type, inventory.levelsOf(type) + (track to level + 1))
            UpgradeRow(
                track = track,
                level = level,
                current = stats,
                next = next,
                cost = cost,
                owned = owned,
                canAfford = state.coins >= cost,
                onUpgrade = { onUpgrade(track) }
            )
        }
    }
}

@Composable
private fun UpgradeRow(
    track: UpgradeTrack,
    level: Int,
    current: WeaponStats,
    next: WeaponStats?,
    cost: Int,
    owned: Boolean,
    canAfford: Boolean,
    onUpgrade: () -> Unit
) {
    val currentText = statText(track, current)
    val valueText = if (next != null) {
        stringResource(R.string.upgrades_stat_change_format, currentText, statText(track, next))
    } else currentText

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(track.labelRes()), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(valueText, style = MaterialTheme.typography.bodySmall, color = colorResource(R.color.upgrade_text_secondary))
            Spacer(Modifier.size(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                for (i in 0 until UpgradeConfig.MAX_LEVEL) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colorResource(if (i < level) R.color.upgrade_pip_on else R.color.upgrade_pip_off))
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.upgrades_level_format, level, UpgradeConfig.MAX_LEVEL),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorResource(R.color.upgrade_text_secondary)
                )
            }
        }
        if (owned) {
            if (next == null) {
                Text(stringResource(R.string.upgrades_max), color = colorResource(R.color.upgrade_pip_on), fontWeight = FontWeight.Black)
            } else {
                Button(onClick = onUpgrade, enabled = canAfford) {
                    Text(stringResource(R.string.upgrades_buy_format, cost), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun statText(track: UpgradeTrack, stats: WeaponStats): String = when (track) {
    UpgradeTrack.DAMAGE -> stringResource(R.string.stat_damage_format, stats.damage)
    UpgradeTrack.FIRE_RATE -> stringResource(R.string.stat_fire_rate_format, stats.shotsPerSecond)
    UpgradeTrack.MAGAZINE -> stringResource(R.string.stat_magazine_format, stats.magazine)
    UpgradeTrack.RELOAD -> stringResource(R.string.stat_reload_format, stats.reloadSeconds)
}
