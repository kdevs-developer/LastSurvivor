package com.kdevs.lastsurvivor.ui.menu

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdevs.lastsurvivor.ui.DiamondIcon
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.audio.SoundEffect
import com.kdevs.lastsurvivor.ui.rememberSoundManager
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val TIP_SECONDS = 6L

/**
 * Main menu, "Toxic neon" style: centred glowing title, one big pulsing Play orb, and four round
 * icon buttons (Continue, Armory, Survivor, Options) over the animated infected-city background.
 */
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
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val checkpoint by viewModel.checkpoint.collectAsStateWithLifecycle()
    val sounds = rememberSoundManager()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    /** Wraps a button action so it also plays the click sound and a light haptic tick. */
    fun clicked(action: () -> Unit): () -> Unit = {
        sounds.play(SoundEffect.UI_CLICK)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action()
    }
    val bloodRed = colorResource(R.color.blood_red)
    val coinGold = colorResource(R.color.coin_gold)
    val toxicGreen = colorResource(R.color.neon_green)
    val toxicGlow = colorResource(R.color.neon_glow)
    val toxicTitle = colorResource(R.color.neon_title)
    val toxicLabel = colorResource(R.color.neon_label)
    val toxicPanel = colorResource(R.color.neon_panel)

    // Back on the main menu asks before leaving the app.
    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = !showExitDialog) { showExitDialog = true }

    // Numbers count up when the menu opens and whenever they change.
    val coinsShown by animateIntAsState(progress.coins, tween(900, easing = FastOutSlowInEasing), label = "coins")
    val diamondsShown by animateIntAsState(progress.diamonds, tween(900, easing = FastOutSlowInEasing), label = "diamonds")
    val killsShown by animateIntAsState(stats.kills, tween(900, easing = FastOutSlowInEasing), label = "kills")

    // The title flickers now and then, like a failing neon sign.
    val flickerTransition = rememberInfiniteTransition(label = "titleFlicker")
    val flicker by flickerTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 7000
                1f at 0
                1f at 4200
                0.45f at 4260
                1f at 4330
                0.7f at 4420
                1f at 4480
                1f at 6100
                0.55f at 6150
                1f at 6230
            }
        ),
        label = "flicker"
    )

    // Survival tips: one at a time, changing every few seconds.
    val tips = stringArrayResource(R.array.menu_tips)
    var tipIndex by remember { mutableIntStateOf(Random.nextInt(tips.size)) }
    LaunchedEffect(tips.size) {
        while (true) {
            delay(TIP_SECONDS * 1000)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    Box(Modifier.fillMaxSize()) {
        ZombieBackground(Modifier.fillMaxSize())

        // Centre column; it scrolls (and stays centred otherwise) if the screen is very short.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Entrance(index = 0, fromY = (-28).dp) {
                    Text(
                        text = stringResource(R.string.menu_title),
                        modifier = Modifier.graphicsLayer { alpha = flicker },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 8.sp,
                            shadow = Shadow(toxicGlow, Offset.Zero, 36f)
                        ),
                        color = toxicTitle
                    )
                }
                Entrance(index = 1, fromY = (-16).dp) {
                    Text(
                        text = stringResource(R.string.menu_tagline),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 5.sp),
                        color = toxicLabel
                    )
                }
                Entrance(index = 2, fromY = 24.dp) {
                    Box(Modifier.padding(vertical = 10.dp)) {
                        PlayOrb(
                            description = stringResource(R.string.menu_play),
                            onClick = clicked(onPlay)
                        )
                    }
                }
                Entrance(index = 3, fromY = 24.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                        OrbButton(
                            icon = Icons.Filled.FastForward,
                            label = checkpoint?.let { stringResource(R.string.menu_label_continue_wave_format, it.wave) }
                                ?: stringResource(R.string.menu_label_continue),
                            sublabel = checkpoint?.let { stringResource(R.string.menu_continue_hp_format, it.health) },
                            enabled = checkpoint != null,
                            highlight = checkpoint != null,
                            onClick = clicked(onContinue)
                        )
                        OrbButton(Icons.Filled.Build, stringResource(R.string.menu_label_armory), onClick = clicked(onUpgrades))
                        OrbButton(Icons.Filled.Person, stringResource(R.string.menu_label_survivor), onClick = clicked(onCharacter))
                        OrbButton(Icons.Filled.Settings, stringResource(R.string.menu_label_options), onClick = clicked(onSettings))
                    }
                }
                Entrance(index = 4, fromY = 16.dp) {
                    Crossfade(
                        targetState = tips[tipIndex.coerceIn(0, tips.lastIndex)],
                        animationSpec = tween(600),
                        label = "tip"
                    ) { tip ->
                        Text(
                            text = tip,
                            modifier = Modifier
                                .widthIn(max = 360.dp)
                                .padding(top = 6.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = toxicLabel.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Top left: coins and diamonds. Top right: best wave and the sound switch, with total kills below the wave.
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .padding(start = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatPill(Icons.Filled.MonetizationOn, stringResource(R.string.menu_coins_format, coinsShown), coinGold)
            StatPill(DiamondIcon, stringResource(R.string.menu_diamonds_format, diamondsShown), colorResource(R.color.powerup_diamond))
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(end = 8.dp, top = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatPill(Icons.Filled.EmojiEvents, stringResource(R.string.menu_wave_format, progress.highestWave), toxicGreen)
                IconButton(
                    onClick = {
                        // play the click first when turning sound on, so it is heard; turning off is silent
                        if (!soundEnabled) sounds.enabled = true
                        sounds.play(SoundEffect.UI_CLICK)
                        viewModel.toggleSound()
                    }
                ) {
                    Icon(
                        if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = stringResource(R.string.sound_toggle_description),
                        tint = toxicTitle
                    )
                }
            }
            if (stats.kills > 0) {
                // end padding = sound button (48 dp) + gap (4 dp), so the kills pill sits right under the wave pill
                Box(modifier = Modifier.padding(end = 52.dp)) {
                    StatPill(Icons.Filled.Whatshot, stringResource(R.string.menu_kills_format, killsShown), colorResource(R.color.menu_kills_orange))
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = toxicPanel,
            titleContentColor = toxicTitle,
            textContentColor = toxicLabel,
            title = { Text(stringResource(R.string.menu_exit_title)) },
            text = { Text(stringResource(R.string.menu_exit_message)) },
            confirmButton = {
                TextButton(onClick = { context.findActivity()?.finish() }) {
                    Text(stringResource(R.string.menu_exit_confirm), color = bloodRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.menu_exit_cancel), color = toxicGreen)
                }
            }
        )
    }
}

/** Slides in and fades in, [index] x 80 ms after the screen opens. */
@Composable
private fun Entrance(
    index: Int,
    modifier: Modifier = Modifier,
    fromX: Dp = 0.dp,
    fromY: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 80L)
        progress.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationX = (1f - progress.value) * fromX.toPx()
            translationY = (1f - progress.value) * fromY.toPx()
        }
    ) { content() }
}

/** The big circular Play button: a glowing ring with a pulse that ripples outwards. */
@Composable
private fun PlayOrb(description: String, onClick: () -> Unit) {
    val green = colorResource(R.color.neon_green)
    val glow = colorResource(R.color.neon_glow)
    val panel = colorResource(R.color.neon_panel)
    val text = colorResource(R.color.neon_text)

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, tween(90), label = "orbPress")
    val pulseTransition = rememberInfiniteTransition(label = "orbPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .size(84.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                val r = size.minDimension / 2f
                drawCircle(
                    Brush.radialGradient(listOf(glow.copy(alpha = 0.35f), Color.Transparent), radius = r * 1.8f),
                    radius = r * 1.8f
                )
                drawCircle(
                    color = green.copy(alpha = 0.5f * (1f - pulse)),
                    radius = r * (1f + 0.45f * pulse),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            .clip(CircleShape)
            .background(panel)
            .border(2.dp, green, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = description,
            tint = text,
            modifier = Modifier.size(46.dp)
        )
    }
}

/** A round icon button with a small label under it (and an optional second line). */
@Composable
private fun OrbButton(
    icon: ImageVector,
    label: String,
    sublabel: String? = null,
    enabled: Boolean = true,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    val green = colorResource(R.color.neon_green)
    val dim = colorResource(R.color.neon_green_dim)
    val panel = colorResource(R.color.neon_panel)
    val text = colorResource(R.color.neon_text)
    val labelColor = colorResource(R.color.neon_label)
    val fade = if (enabled) 1f else 0.4f

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(90), label = "orbButtonPress")

    Column(
        modifier = Modifier
            .widthIn(min = 68.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(panel)
                .border(1.5.dp, (if (highlight) green else dim).copy(alpha = fade), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = text.copy(alpha = fade), modifier = Modifier.size(22.dp))
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            color = labelColor.copy(alpha = fade)
        )
        if (sublabel != null) {
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StatPill(icon: ImageVector, label: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = colorResource(R.color.neon_panel),
        border = BorderStroke(1.dp, colorResource(R.color.neon_border))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(label, fontWeight = FontWeight.Bold, color = colorResource(R.color.neon_text), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}