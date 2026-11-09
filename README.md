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
| 8 | Power-ups and Boss Fights | Next |
| 9 | Graphics, Animations and Sound Effects | Pending |
| 10 | DataStore, Optimization and Testing | Pending |

## Phase 5: Shooting and Collision Detection

- **Firing:** hold the Fire button, or push the aim stick to its edge (auto-fire). The starter pistol fires about 4.5 bullets per second in the direction the player faces.
- **Bullets:** pooled `Bullet` entities (`game/entities/Bullet.kt`) fly at 1500 units/s, disappear after 900 units, and are drawn as a tracer with a bright head.
- **Collision:** `game/engine/Collision.kt` does a swept segment-vs-circle test over each bullet's path every tick, so fast bullets never pass through a zombie. If several zombies lie on the path, the first one reached is hit.
- **Damage:** a hit deals 20 damage, knocks the zombie back slightly, flashes it and shows its health bar. A Walker takes 2 hits, a Runner 4, a Toxic 5 and a Tank 15.
- **Impacts:** a short blood puff on a zombie, a dust puff where a bullet hits the map edge (`ImpactEffect`, pooled).
- **Stats:** the debug overlay shows kills, shots fired, hits and accuracy.
- **Temporary:** the grenade button is still a debug blast, and reload is still a placeholder. Real versions arrive in Phases 7 and 8.
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
- **Still temporary:** the grenade button is a debug blast until Phase 8.

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

Add new strings to the file of the module they belong to, and new colors under that module's section in `colors.xml`.

## Project layout

```
game/engine    GameLoop, GameWorld, GameConfig, WaveManager, HudState, Collision, Camera, InputState, ObjectPool
game/entities  Entity, Player, Zombie, ZombieType, Bullet, ImpactEffect
game/weapons   WeaponType, WeaponStats (+ UpgradeConfig), WeaponState, WeaponInventory
game/render    GameRenderer, GameColors, PlaceholderSprites
ui/menu        Main menu and placeholder screens
ui/upgrades    UpgradesScreen, UpgradesViewModel
ui/weapons     WeaponLabels (weapon and upgrade names for the UI)
ui/game        GameScreen, GameOverOverlay, GameSessionViewModel, hud (GameHud), controls (joysticks and buttons)
ui/theme       Compose Material 3 theme
data           PlayerPrefsRepository (DataStore)
```
