package com.kdevs.lastsurvivor.ui.mode

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.audio.SoundEffect
import com.kdevs.lastsurvivor.game.engine.Campaign
import com.kdevs.lastsurvivor.game.engine.CampaignLevel
import com.kdevs.lastsurvivor.game.engine.GameArea
import com.kdevs.lastsurvivor.game.engine.RunSetup
import com.kdevs.lastsurvivor.ui.rememberSoundManager

private const val TAB_CAMPAIGN = 0
private const val TAB_INFINITE = 1
private const val LEVELS_PER_ROW = 5

/**
 * Where Play leads. Two tabs in one fixed (non-scrolling) landscape layout:
 *  - Campaign: ten levels, each on its own area with a fixed number of waves and a boss finale. A level
 *    unlocks when the one before it is cleared.
 *  - Infinite: endless waves; pick any of the five areas.
 * Tap a tile to select it, then START.
 */
@Composable
fun ModeSelectScreen(
    onBack: () -> Unit,
    onStart: (RunSetup) -> Unit,
    viewModel: ModeSelectViewModel = viewModel()
) {
    val cleared by viewModel.cleared.collectAsStateWithLifecycle()
    val sounds = rememberSoundManager()
    val unlocked = (cleared + 1).coerceAtMost(Campaign.count)

    var tab by rememberSaveable { mutableIntStateOf(TAB_CAMPAIGN) }
    var pickedLevel by rememberSaveable { mutableIntStateOf(0) }      // 0 = the next level to play
    var pickedArea by rememberSaveable { mutableIntStateOf(0) }       // GameArea ordinal
    val level = if (pickedLevel in 1..Campaign.count) pickedLevel else unlocked
    val area = GameArea.entries[pickedArea.coerceIn(0, GameArea.entries.lastIndex)]

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.survivor_background))
            .safeDrawingPadding()
    ) {
        val pad = if (maxHeight < 360.dp || maxWidth < 400.dp) 8.dp else 14.dp

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = 1000.dp).fillMaxSize().padding(pad),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Header(onBack = { sounds.play(SoundEffect.UI_CLICK); onBack() })

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ModeTab(stringResource(R.string.mode_campaign), tab == TAB_CAMPAIGN, Modifier.weight(1f)) {
                        sounds.play(SoundEffect.UI_CLICK); tab = TAB_CAMPAIGN
                    }
                    ModeTab(stringResource(R.string.mode_infinite), tab == TAB_INFINITE, Modifier.weight(1f)) {
                        sounds.play(SoundEffect.UI_CLICK); tab = TAB_INFINITE
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (tab == TAB_CAMPAIGN) {
                        LevelGrid(selected = level, unlocked = unlocked, cleared = cleared) {
                            sounds.play(SoundEffect.UI_CLICK); pickedLevel = it
                        }
                    } else {
                        AreaRow(selected = area) {
                            sounds.play(SoundEffect.UI_CLICK); pickedArea = it.ordinal
                        }
                    }
                }

                // info about the selection + the one Start button
                val locked = tab == TAB_CAMPAIGN && level > unlocked
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = when {
                            tab == TAB_INFINITE -> stringResource(R.string.mode_infinite_info_format, stringResource(area.nameRes))
                            locked -> stringResource(R.string.mode_level_locked_format, level, level - 1)
                            else -> {
                                val data = Campaign.level(level)
                                stringResource(R.string.mode_level_info_format, level, stringResource(data.area.nameRes), data.waves)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.neon_label),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            sounds.play(SoundEffect.UI_CLICK)
                            onStart(if (tab == TAB_CAMPAIGN) RunSetup.campaign(level) else RunSetup.infinite(area))
                        },
                        enabled = !locked,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.widthIn(min = 200.dp).height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.neon_green),
                            contentColor = colorResource(R.color.survivor_on_accent)
                        )
                    ) {
                        Text(
                            text = if (tab == TAB_CAMPAIGN) stringResource(R.string.mode_start_level_format, level)
                            else stringResource(R.string.mode_start_infinite),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val shape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .size(36.dp)
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
        Text(
            stringResource(R.string.mode_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.neon_title),
            letterSpacing = 3.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) colorResource(R.color.neon_green) else colorResource(R.color.neon_panel))
            .border(1.dp, colorResource(R.color.neon_border), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label.uppercase(),
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = if (selected) colorResource(R.color.survivor_on_accent) else colorResource(R.color.neon_title)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Campaign: 2 rows x 5 level tiles, each drawn in its area's colours
// ---------------------------------------------------------------------------------------------

@Composable
private fun LevelGrid(selected: Int, unlocked: Int, cleared: Int, onPick: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in Campaign.levels.chunked(LEVELS_PER_ROW)) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (level in row) {
                    LevelTile(
                        level = level,
                        selected = level.number == selected,
                        locked = level.number > unlocked,
                        done = level.number <= cleared,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onPick(level.number) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelTile(
    level: CampaignLevel,
    selected: Boolean,
    locked: Boolean,
    done: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val area = level.area
    val accent = colorResource(area.accent)
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .alpha(if (locked) 0.5f else 1f)
            .clip(shape)
            .background(colorResource(area.floorBase))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) accent else colorResource(area.border).copy(alpha = 0.6f),
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.mode_level_format, level.number),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = accent
            )
            Text(
                stringResource(area.nameRes),
                style = MaterialTheme.typography.labelSmall,
                color = colorResource(R.color.neon_label),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        if (locked || done) {
            Icon(
                if (locked) Icons.Filled.Lock else Icons.Filled.Check,
                contentDescription = stringResource(if (locked) R.string.mode_level_locked else R.string.mode_level_cleared),
                tint = if (locked) colorResource(R.color.neon_label) else accent,
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Infinite: one card per area with a little preview of its floor
// ---------------------------------------------------------------------------------------------

@Composable
private fun AreaRow(selected: GameArea, onPick: (GameArea) -> Unit) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (area in GameArea.entries) {
            AreaCard(area, area == selected, Modifier.weight(1f).fillMaxHeight()) { onPick(area) }
        }
    }
}

@Composable
private fun AreaCard(area: GameArea, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = colorResource(area.accent)
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colorResource(R.color.neon_panel))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) accent else colorResource(R.color.neon_border),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AreaPreview(
            floor = colorResource(area.floorBase),
            speck = colorResource(area.floorSpeck),
            line = colorResource(area.floorLine),
            rubble = colorResource(area.decorRubbleLight),
            border = colorResource(area.border),
            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp))
        )
        Text(
            stringResource(area.nameRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) accent else colorResource(R.color.neon_title),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A tiny stand-in for the real floor: base colour, slab joints, specks, a few rubble stones and the map border. */
@Composable
private fun AreaPreview(floor: Color, speck: Color, line: Color, rubble: Color, border: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(floor)
        val cols = 4
        val rows = 3
        val cw = size.width / cols
        val ch = size.height / rows
        for (i in 1 until cols) drawLine(line, Offset(cw * i, 0f), Offset(cw * i, size.height), strokeWidth = 2f)
        for (j in 1 until rows) drawLine(line, Offset(0f, ch * j), Offset(size.width, ch * j), strokeWidth = 2f)
        // fixed pseudo-random specks, so the preview does not change between redraws
        for (n in 0 until 14) {
            val x = ((n * 53) % 97) / 97f * size.width
            val y = ((n * 29) % 89) / 89f * size.height
            drawRect(speck, topLeft = Offset(x, y), size = Size(size.width * 0.07f, size.height * 0.05f))
        }
        for (n in 0 until 5) {
            val x = ((n * 41 + 17) % 83) / 83f * size.width
            val y = ((n * 67 + 11) % 79) / 79f * size.height
            drawCircle(rubble, radius = size.minDimension * 0.045f, center = Offset(x, y))
        }
        drawRect(border, style = Stroke(width = 5f))
    }
}
