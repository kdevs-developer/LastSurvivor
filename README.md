# Last Survivor

Open this folder in Android Studio (Otter 3 Feature Drop or newer; AGP 9 + Gradle 9.1.0, JDK 17), let Gradle sync, run on a device/emulator.

## Progress

| Phase | Name | Status |
|---|---|---|
| 1 | Main Menu and Navigation | Done |
| 2 | Game Engine and Rendering | Done |
| 3 | Player Movement and Joystick Controls | Done |
| 4 | Zombie Spawning and AI | Done |
| 5 | Shooting and Collision Detection | Done |
| 6 | Health, Damage and Wave Progression | Done |
| 7 | Weapons and Upgrade System | Done |
| 8 | Power-ups and Boss Fights | Done |
| 9 | Graphics, Animations and Sound Effects | Done |
| 10 | DataStore, Optimization and Testing | Done |

## Enhancements (screen by screen)

**1. Main menu, "Toxic neon" style** (`ui/menu/`)

- **Layout:** centred glowing title, then one big circular **Play** orb with a pulse that ripples outwards, then four round icon buttons: Continue (shows `WAVE n` and `HP x`, greyed out when nothing is saved), Armory, Survivor, Options. Coin and kill pills sit top left; best wave and the sound switch top right.
- **Palette:** near-black purple with acid green. The menu colours in `colors.xml` (`menu_*`) and the new `colors_menu.xml` (`neon_*`) drive both the Compose UI and the background.
- **Background** (`ZombieBackground.kt`): toxic green moon behind the title, green fog, lit windows (some flicker), bats, green-white lightning with a bolt behind the skyline. All loops are seamless.
- **Motion:** items slide in one after another, the title flickers like a failing neon sign, coins and kills count up, and buttons shrink slightly when pressed with a light haptic tick.
- **Extras:** a rotating survival tip at the bottom (`menu_tips` in `strings_menu.xml`) and an exit prompt when pressing Back.
- **Fonts:** the system font is still used. For the premium look, drop a font such as Orbitron or Rajdhani into `res/font/` and set it as the `fontFamily` in `ui/theme/Theme.kt` typography.
- Not compiled in the build sandbox (no Android SDK): Sync, Build and run, then check the title flicker, the pulse ring and the lightning brightness on your device.

## Phase 10: DataStore, Optimization and Testing

All ten phases are done. This phase adds saving, the two screens that were still placeholders, a performance pass and unit tests.

**Saving** (`data/PlayerPrefsRepository.kt`, one DataStore file)

| Saved | Where it is used |
|---|---|
| Coins, best wave | Main menu, upgrades |
| Unlocked weapons, equipped weapon, upgrade levels | Upgrades screen, each run |
| Options: sound, screen shake, gore level, damage numbers, FPS counter | Settings screen, applied to the engine at the start of a run |
| Outfit | Character screen, player sprite |
| Lifetime statistics: runs, kills, bosses, shots, hits, coins earned, time played | Character screen |
| Checkpoint: wave, health, grenades | Continue button |

- **Continue:** the start of every wave from wave 2 on is saved. **Continue** on the main menu (it shows the wave, and is greyed out when there is nothing saved) starts that wave again with a normal countdown, the saved grenades and at least 50% health (`GameConfig.CHECKPOINT_MIN_HEALTH_FRACTION`). Zombies, power-ups and buffs are not saved. **Play** and **Retry** throw the saved checkpoint away; dying discards it; quitting from the pause menu keeps it.
- **Nothing is lost if the app is killed:** coins and statistics are banked at each wave start, when the app goes to the background, and when the run ends. A "bank" writes only what is new since the previous one (`RunTotals.minus`), so nothing is counted twice, and each bank is one DataStore transaction. This replaces the old rule that backing out of the app mid-run did not save.
- **Saves finish even if the screen is gone:** they run in `persistenceScope` (process-wide), not in the game screen's ViewModel, which is cleared the moment you leave.
- **Damaged data cannot crash the game:** a corrupt DataStore file is replaced by an empty one, a bad checkpoint string is ignored, negative numbers are clamped, upgrade levels are limited to 0..5 and unknown enum names fall back to defaults.
- **Reset progress** (Settings) erases everything above except the options, after a confirmation.

**Settings screen** (`ui/settings/`): switches for sound, screen shake, damage numbers and "Show FPS and debug info", and Off / Low / Normal for blood and gore. These replaced the `SCREEN_SHAKE_ENABLED`, `GORE_AMOUNT`, `SHOW_DAMAGE_NUMBERS` and `SHOW_DEBUG_OVERLAY` constants in `GameConfig`; the defaults are the same as before (`game/engine/GameSettings.kt`).

**Character screen** (`ui/character/`): pick one of four outfits (Survivor, Ranger, Scavenger, Shadow) and read your lifetime statistics. Outfits recolour the jacket, sleeves, backpack and hair of the generated sprite (`SkinPalette`, colours in `colors_character.xml`). If you drop in real `player_*.png` art (see `assets/sprites/README.txt`) it is used as it is and the outfit choice has no effect on it.

**Optimization**
- A paused game redraws 15 times a second instead of 60 (`GameConfig.PAUSED_FPS`), which saves battery and heat while the pause menu is open.
- The renderer was already allocation-free per frame; the new outfit code allocates only when the outfit changes. Settings are one immutable object swapped in a single write, so the game thread never reads half-changed options.
- Zombie separation is O(n^2) but is capped at 40 alive (plus dying ones), so it stays cheap. If you raise `WAVE_MAX_ALIVE_CAP` a lot, a spatial grid is the next step.
- `proguard-rules.pro` documents why release builds (minify + shrink) need no keep rules and adds line numbers to crash traces.

**Testing** (`app/src/test`, run with `./gradlew testDebugUnitTest`)

| Test class | Covers |
|---|---|
| `GameWorldTest` | The real simulation, headless: wave countdown, Continue (wave, health floor, clamped values, boss wave), restart, kills/coins/shots/time in the run totals, time stops at death, shake and gore settings |
| `PlayerStatsTest` | Stats arithmetic, saturation, accuracy, play time, `RunTotals.minus` never negative |
| `RunCheckpointTest` | Checkpoint encode/decode and rejection of damaged text |
| `CharacterSkinTest` | Saved outfit names, fallback |
| `CollisionTest` | Swept bullet-vs-circle hits, misses, fast bullets, ordering |
| `WeaponTest` | Upgrade maths and costs, magazine and reload, object pool |
| `PlayerPrefsRepositoryTest` | A real DataStore in a temporary file: banking, checkpoint lifecycle, settings, outfit, clamping, purchases, reset |

- **Verification status:** all tests except `PlayerPrefsRepositoryTest` were compiled and run (38 tests, all passing) against the pure-Kotlin parts of the game in the build sandbox. The Compose screens, the renderer, DataStore and `PlayerPrefsRepositoryTest` need the Android SDK, which was not available there, so they were checked by reading only. Run **Sync, then Build, then `testDebugUnitTest`** first, and fix any compile error in those files (most likely a missing import or a changed API) before playing.
- **Still to judge on a device:** the Ranger, Scavenger and Shadow colours, the Settings and Character layouts on your screen sizes, and that Continue feels right (wave restarts with a countdown).

## Phase 9: Graphics, Animations and Sound Effects

**Graphics** (`game/render/`, `game/effects/`)

- **Sprites:** the player now holds the equipped weapon (pistol, SMG, shotgun and rifle each look different) and has a walk cycle. Every zombie type has a 4-frame walk cycle plus an attack frame and its own features: Runner is lean and leans forward, Tank has armour plates, Toxic has glowing pustules, the Brute has shoulder spikes and horns, the Brood Mother has a swollen abdomen with sacs and tentacles. Zombies flash white when hit, and zombies spawned mid-fight (the Mother's minions) grow and fade in.
- **Real art is optional:** `SpriteAtlas` looks for a PNG in `app/src/main/assets/sprites/` first and falls back to the generated art in `PlaceholderSprites`. Names and sizes are in `assets/sprites/README.txt`. Replace sprites one file at a time with no code change.
- **Effects:** pooled, allocation-free systems in `game/effects/`. `ParticleSystem` (blood, toxic goo, sparks, dust, smoke, fire, brass casings, pickup sparkles, max 480), `DecalField` (blood pools, goo and scorch marks that fade after 28 s, max 96) and `FloatingTexts` (damage numbers; hits in quick succession on one zombie are summed; the killing blow is bigger and gold, Double Damage hits are orange).
- **Combat feel:** muzzle flash with flame, glow and white core sized per weapon, a shell casing and a puff of smoke per shot, blood spray on hits, a bigger burst and floor stain on kills, a hot flash, fire, smoke and scorch mark on grenade blasts, sparks when the shield blocks a hit.
- **Camera shake:** when hurt, shotgun blasts, explosions, boss charge and roar, boss death and game over. Event shake fades with distance from the player. `GameWorld` exposes `shakeX/shakeY`; the renderer adds them to the camera.
- **Atmosphere:** dark vignette around the screen, a pulsing red edge glow below 30% health, and 220 fixed floor decorations (cracks, stains, rubble, road marks).
- **HUD animations (Compose):** the health bar glides with a pale trail showing recent damage and the heart beats at low health, the wave banner scales and fades in and out, the countdown number pops each second, kills/coins/grenade chips pop when they change, the boss bar glides, and the game over screen fades in with the title dropping in. All are read in the draw phase, so the HUD still does not recompose per frame.

**Sound** (`game/audio/`)

- **31 sound effects** in `res/raw/` as `sfx_<name>.wav`: four weapon shots, reload start and finish, hit, zombie death, two ambient groans, swing, player hurt, poison, shield, low-health heartbeat, pickups, grenade throw and explosion, boss roar / windup / charge / summon / death, wave start and clear, game over, and menu click / purchase / denied.
- **They are synthesised, not recorded.** `tools/generate_sfx.py` (needs only numpy) creates every file, deterministically: `python tools/generate_sfx.py`. To use real recordings, drop files with the same names into `res/raw/` (`.wav` or `.ogg`; if you use another extension, update the matching `R.raw` line in `SoundManager.rawId`).
- **How it works:** `SoundEffect` is one table of every effect (volume, minimum gap between plays, pitch variation, priority). `GameAudio` is the interface the engine talks to, so `GameWorld` stays free of Android types. `SoundManager` plays them through one `SoundPool` (14 streams) and rate-limits repeats so a shotgun blast or SMG burst cannot stack into noise.
- **3D feel:** sounds in the world get quieter with distance (silent beyond 1500 units), pan left/right by where they are relative to the player, and big zombies sound deeper than small ones. Ambient groans come from nearby zombies every 2.5 to 6 s (more often with more zombies), and a heartbeat plays below 30% health, faster as health drops.
- **Sound switch:** a speaker icon on the main menu, a Sound: On/Off button on the pause screen and a switch on the Settings screen. The choice is saved in DataStore (`sound_enabled`, default on). There is no music yet.
- **Tuning:** all numbers are in `GameConfig.kt` (Phase 9 sections: shake, particles, damage numbers, overlays, sound) and `SoundEffect.kt`. Shake, blood and damage numbers are switched on the Settings screen (Phase 10).
- **Not verified in the build sandbox:** the `game/` package was compile-checked, but Gradle could not run there, so the Compose screens were checked by reading and the art and sounds have not been seen or heard on a device. Run the game and judge the sprites, the mix and the shake strength; the config values above are the first things to adjust.

## Phase 8: Power-ups and Boss Fights

- **Power-up drops:** zombies drop a pickup when they die (Walker 5%, Runner 7%, Toxic 10%, Tank 25%, set by `ZombieType.dropChance`). Walk over one to collect it. It stays 14 s and blinks for the last 4. Below 40% health, medkits are twice as likely. Types and weights are in `game/entities/PowerUpType.kt`.

| Power-up | Effect |
|---|---|
| Medkit (green cross) | +40 HP and cures poison |
| Rapid Fire (yellow >>) | Half the time between shots for 8 s |
| Double Damage (red crosshair) | Bullets deal 2x damage for 8 s |
| Shield (blue) | No damage at all (including poison ticks) for 6 s |
| Grenades (brown) | +2 grenades (max 5) |

  Active buffs show as chips under the kills/coins row, with seconds left; the shield draws a bubble around the player and Double Damage a red aura. Buffs do not stack: picking one up again refreshes it. All numbers are in `GameConfig.kt` (Phase 8 sections).
- **Real grenades:** the Grenade button throws one in the facing direction (you start each run with 2, max 5, count shown in the HUD). It slows to a stop over a 1.4 s fuse (about 490 units), then explodes: 140 damage at the centre falling to 40% at the 230 radius, plus knockback. It hurts zombies and bosses, never the player. The temporary debug blast is gone.
- **Boss waves:** every 5th wave (5, 10, 15...) has a boss. It spawns at once with half the usual escort, the countdown banner says BOSS INCOMING, and clearing the wave pays double coins. A boss always drops a random power-up and a grenade pack. Bosses alternate and each later one has +50% health. Below 35% health a boss enrages (+30% speed, abilities 40% more often, orange aura and health bar). Boss tuning: `ZombieType` (BRUTE, MOTHER) and `GameConfig.BOSS_*`.

| Boss | Health | Special |
|---|---|---|
| The Brute (wave 5, 15...) | 1200 | Every 5 s it stops and a red strip shows where it will charge. After 0.8 s it charges that line at 5x speed for 0.7 s and hits for 1.5x damage. Step aside. |
| Brood Mother (wave 10, 20...) | 900 | Every 7 s it summons 3 Walkers around itself (not above 45 zombies alive) |

- **Boss HUD:** name and health bar under the wave number while a boss is alive (`ui/game/hud/GameHud.kt`).
- **Saving:** nothing new is stored. Power-ups and grenades are per run.
- Boss sprites are generated (bigger, with their own features and a coloured aura). Phase 9 made art swappable with PNGs.

## Phase 5: Shooting and Collision Detection

- **Firing:** hold the Fire button, or push the aim stick to its edge (auto-fire). The starter pistol fires about 4.5 bullets per second in the direction the player faces.
- **Bullets:** pooled `Bullet` entities (`game/entities/Bullet.kt`) fly at 1500 units/s, disappear after 900 units, and are drawn as a tracer with a bright head.
- **Collision:** `game/engine/Collision.kt` does a swept segment-vs-circle test over each bullet's path every tick, so fast bullets never pass through a zombie. If several zombies lie on the path, the first one reached is hit.
- **Damage:** a hit deals 20 damage, knocks the zombie back slightly, flashes it and shows its health bar. A Walker takes 2 hits, a Runner 4, a Toxic 5 and a Tank 15.
- **Impacts:** a short blood puff on a zombie, a dust puff where a bullet hits the map edge (`ImpactEffect`, pooled).
- **Stats:** the debug overlay shows kills, shots fired, hits and accuracy.
- Reload and grenades were placeholders here; real versions arrived in Phases 7 and 8.
- Per-weapon tuning moved to `game/weapons/WeaponType.kt` in Phase 7; knockback and bullet size are in `game/engine/GameConfig.kt`.

## Phase 7: Weapons and Upgrade System

- **Four weapons** (`game/weapons/WeaponType.kt`, all stats in one table):

| Weapon | Price | Damage | Fire rate | Magazine | Notes |
|---|---|---|---|---|---|
| Pistol | owned | 20 | 4.5/s | 12 | Accurate starter |
| SMG | 120 | 11 | 11/s | 30 | Slight spread |
| Shotgun | 250 | 12 x 6 pellets | 1.25/s | 6 | 26 degree cone, short range |
| Rifle | 400 | 38 | 2.9/s | 20 | Long range, pierces 3 zombies |

- **Ammo and reload:** each weapon has a magazine and a reload time (reserve ammo is unlimited). The Reload button works now, and the magazine reloads automatically when it empties. A bar over the player shows reload progress, and the HUD shows the weapon name and `ammo / magazine`.
- **Piercing:** a bullet remembers which zombies it hit (`Bullet.hasHit`), so the rifle passes through up to 3 without hitting one twice.
- **Upgrades:** four tracks per weapon, 5 levels each: damage (+15% per level), fire rate (-8% time between shots per level), magazine (+20% per level) and reload speed (-8% time per level). A level costs `upgradeBaseCost x (level + 1)` coins. Numbers are in `game/weapons/WeaponStats.kt` (`UpgradeConfig`).
- **Weapon Upgrades screen** (`ui/upgrades/`), opened from the main menu: weapon list on the left with unlock/equip state, details on the right with a stat preview (`current -> next`), level pips and upgrade buttons. Buttons are disabled when you can't afford them.
- **Saving:** unlocked weapons, the equipped weapon and all upgrade levels are stored in DataStore (`PlayerPrefsRepository`). Purchases check and spend coins inside one DataStore transaction, so coins can't go negative. The game equips your chosen weapon, with its upgrades, at the start of each run.

## Phase 6: Health, Damage and Wave Progression

- **Player health:** 100 HP. A zombie swing deals its type's damage, then the player is invulnerable for 0.35 s so a crowd cannot melt them in one tick. A red screen flash shows each hit.
- **Poison:** a Toxic hit also poisons the player (5 HP/s for 4 s). Poison does not stack, bypasses the short invulnerability, and tints the screen green.
- **Waves:** `game/engine/WaveManager.kt` replaces the temporary spawner. Each wave runs: countdown (3 s before wave 1, 5 s after) -> zombies spawn just off screen -> clear them all -> coin bonus -> next wave.
  - Wave size is 5 + 3 x wave (max 80), spawn rate speeds up each wave, and the number alive at once is capped (15 + 2 per wave, max 40).
  - Zombies get +10% health per wave and up to +40% speed.
  - Types unlock by wave: Walker 1, Runner 3, Toxic 5, Tank 7 (`ZombieType.unlockWave`).
- **Coins:** kills pay by type (Walker 1, Runner 2, Toxic 3, Tank 5), plus 5 x wave for clearing a wave.
- **HUD (Compose):** `ui/game/hud/GameHud.kt` shows the health bar, poison warning, kills, coins, the current wave, zombies left and a wave banner during countdowns. It reads a `HudState` that the game thread publishes only when a value changes.
- **Game over:** at 0 HP the simulation stops and `GameOverOverlay` shows wave reached, kills and coins, with Retry and Quit to Menu. Retry resets the world (`GameWorld.requestRestart()`).
- **Saving:** on death or Quit to Menu the run's coins are added and the highest wave updated through `PlayerPrefsRepository` (shown on the main menu). Phase 10 also banks progress when a wave starts and when the app goes to the background.
- **Debug overlay:** FPS and shot stats are hidden by default. Turn on "Show FPS and debug info" on the Settings screen.
- All wave and health tuning is in `GameConfig.kt`.

## Resources (colors, strings, theme)

Nothing visual or textual is hard-coded in Kotlin any more.

- **Colors:** `res/values/colors.xml`, grouped by module (brand, menu, controls, world, HUD, player, zombies, shooting). Compose reads them with `colorResource()`. The SurfaceView renderer reads them through `game/render/GameColors.kt`.
- **Theme:** `res/values/themes.xml` is the window theme. `ui/theme/Theme.kt` builds the Material 3 color scheme from the same `colors.xml` entries, so both stay in sync.
- **Strings, one file per module:**

| File | Module |
|---|---|
| `strings.xml` | App name and shared strings |
| `strings_menu.xml` | Main menu |
| `strings_settings.xml` | Settings screen |
| `strings_character.xml` | Character screen: outfits and statistics |
| `strings_game.xml` | Game screen, pause and game over overlays |
| `strings_controls.xml` | On-screen control descriptions |
| `strings_hud.xml` | In-game HUD and debug overlay |
| `strings_upgrades.xml` | Weapons and the upgrade screen |
| `strings_powerups.xml` | Power-ups, grenades and boss fights (HUD banner, boss names) |
| `strings_audio.xml` | Sound switch |

Add new strings to the file of the module they belong to, and new colors under that module's section in `colors.xml`. Phase 8 colors live in `colors_powerups.xml`, Phase 9 colors (particles, stains, sprite details, overlays, damage numbers) in `colors_effects.xml`, Phase 10 colors (outfits, character screen) in `colors_character.xml`.

## Project layout

```
game/engine    GameLoop, GameWorld, GameConfig, GameSettings, RunTotals, WaveManager, HudState, Collision, Camera, InputState, ObjectPool
game/entities  Entity, Player, Zombie, ZombieType, CharacterSkin, Bullet, ImpactEffect, PowerUp, PowerUpType, Grenade, ExplosionEffect
game/weapons   WeaponType, WeaponStats (+ UpgradeConfig), WeaponState, WeaponInventory
game/render    GameRenderer, GameColors, SpriteAtlas, PlaceholderSprites
game/effects   ParticleSystem, DecalField, FloatingTexts
game/audio     SoundEffect, GameAudio, SoundManager
tools          generate_sfx.py (makes res/raw/*.wav)
ui/menu        Main menu
ui/settings    SettingsScreen, SettingsViewModel
ui/character   CharacterScreen, CharacterViewModel, SkinLabels
ui/upgrades    UpgradesScreen, UpgradesViewModel
ui/weapons     WeaponLabels (weapon and upgrade names for the UI)
ui/game        GameScreen, GameOverOverlay, GameSessionViewModel, hud (GameHud), controls (joysticks and buttons)
ui/theme       Compose Material 3 theme
ui             Sounds.kt (rememberSoundManager for UI click sounds)
data           PlayerPrefsRepository (DataStore: progress, weapons, settings, outfit, statistics, checkpoint), PlayerStats, RunCheckpoint
app/src/test   Unit tests (see Phase 10)
```
