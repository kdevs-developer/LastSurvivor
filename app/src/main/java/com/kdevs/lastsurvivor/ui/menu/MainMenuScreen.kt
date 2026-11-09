package com.kdevs.lastsurvivor.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdevs.lastsurvivor.R

@Composable
fun MainMenuScreen(
    onPlay: () -> Unit,
    onContinue: () -> Unit,
    onUpgrades: () -> Unit,
    onCharacter: () -> Unit,
    onSettings: () -> Unit,
    viewModel: MainMenuViewModel = viewModel()
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val bloodRed = colorResource(R.color.blood_red)
    val coinGold = colorResource(R.color.coin_gold)

    Box(Modifier.fillMaxSize()) {
        ZombieBackground(Modifier.fillMaxSize())

        Row(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left: title + stats
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(R.string.menu_title),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        lineHeight = 52.sp,
                        shadow = Shadow(bloodRed, Offset.Zero, 28f)
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.menu_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip(Icons.Filled.MonetizationOn, stringResource(R.string.menu_coins_format, progress.coins), coinGold)
                    StatChip(Icons.Filled.EmojiEvents, stringResource(R.string.menu_wave_format, progress.highestWave), bloodRed)
                }
            }

            // Right: buttons (scrolls if the screen is very short)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuButton(stringResource(R.string.menu_play), Icons.Filled.PlayArrow, primary = true, onClick = onPlay)
                MenuButton(
                    stringResource(R.string.menu_continue), Icons.Filled.FastForward,
                    onClick = onContinue
                )
                MenuButton(stringResource(R.string.menu_weapon_upgrades), Icons.Filled.Build, onClick = onUpgrades)
                MenuButton(stringResource(R.string.menu_character), Icons.Filled.Person, onClick = onCharacter)
                MenuButton(stringResource(R.string.menu_settings), Icons.Filled.Settings, onClick = onSettings)
            }
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    icon: ImageVector,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
    val content: @Composable () -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(10.dp))
        Text(text, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    }
    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blood_red))
        ) { content() }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = colorResource(R.color.menu_button_container),
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) { content() }
    }
}

@Composable
private fun StatChip(icon: ImageVector, label: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = colorResource(R.color.menu_chip_container)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}
