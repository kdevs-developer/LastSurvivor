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
| 9 | Graphics, Animations and Sound Effects | Next |
| 10 | DataStore, Optimization and Testing | Pending |

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
- Zombie sprites for the bosses are still the generated placeholders (bigger, with a coloured aura). Real art arrives in Phase 9.

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
- **Saving:** on death or Quit to Menu the run's coins are added and the highest wave updated through `PlayerPrefsRepository` (shown on the main menu). Backing out of the app mid-run does not save; Phase 10 adds that.
- **Debug overlay:** FPS and shot stats are hidden by default. Set `SHOW_DEBUG_OVERLAY = true` in `GameConfig.kt` to show them.
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
| `strings_navigation.xml` | Placeholder screens |
| `strings_game.xml` | Game screen, pause and game over overlays |
| `strings_controls.xml` | On-screen control descriptions |
| `strings_hud.xml` | In-game HUD and debug overlay |
| `strings_upgrades.xml` | Weapons and the upgrade screen |
| `strings_powerups.xml` | Power-ups, grenades and boss fights (HUD banner, boss names) |

Add new strings to the file of the module they belong to, and new colors under that module's section in `colors.xml`. Phase 8 colors live in `colors_powerups.xml`.

## Project layout

```
game/engine    GameLoop, GameWorld, GameConfig, WaveManager, HudState, Collision, Camera, InputState, ObjectPool
game/entities  Entity, Player, Zombie, ZombieType, Bullet, ImpactEffect, PowerUp, PowerUpType, Grenade, ExplosionEffect
game/weapons   WeaponType, WeaponStats (+ UpgradeConfig), WeaponState, WeaponInventory
game/render    GameRenderer, GameColors, PlaceholderSprites
ui/menu        Main menu and placeholder screens
ui/upgrades    UpgradesScreen, UpgradesViewModel
ui/weapons     WeaponLabels (weapon and upgrade names for the UI)
ui/game        GameScreen, GameOverOverlay, GameSessionViewModel, hud (GameHud), controls (joysticks and buttons)
ui/theme       Compose Material 3 theme
data           PlayerPrefsRepository (DataStore)
```
