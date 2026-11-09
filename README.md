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
| 6 | Health, Damage and Wave Progression | Next |
| 7 | Weapons and Upgrade System | Pending |
| 8 | Power-ups and Boss Fights | Pending |
| 9 | Graphics, Animations and Sound Effects | Pending |
| 10 | DataStore, Optimization and Testing | Pending |

## Phase 5: Shooting and Collision Detection

- **Firing:** hold the Fire button, or push the aim stick to its edge (auto-fire). The starter pistol fires about 4.5 bullets per second in the direction the player faces.
- **Bullets:** pooled `Bullet` entities (`game/entities/Bullet.kt`) fly at 1500 units/s, disappear after 900 units, and are drawn as a tracer with a bright head.
- **Collision:** `game/engine/Collision.kt` does a swept segment-vs-circle test over each bullet's path every tick, so fast bullets never pass through a zombie. If several zombies lie on the path, the first one reached is hit.
- **Damage:** a hit deals 20 damage, knocks the zombie back slightly, flashes it and shows its health bar. A Walker takes 2 hits, a Runner 4, a Toxic 5 and a Tank 15.
- **Impacts:** a short blood puff on a zombie, a dust puff where a bullet hits the map edge (`ImpactEffect`, pooled).
- **Stats:** the debug overlay shows kills, shots fired, hits and accuracy.
- **Temporary:** the grenade button is still a debug blast, and reload is still a placeholder. Real versions arrive in Phases 7 and 8. Zombie swings still do no damage until Phase 6.
- Tuning values (speed, damage, range, fire rate, knockback) are in `game/engine/GameConfig.kt`.

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
| `strings_game.xml` | Game screen and pause overlay |
| `strings_controls.xml` | On-screen control descriptions |
| `strings_hud.xml` | In-game debug HUD |

Add new strings to the file of the module they belong to, and new colors under that module's section in `colors.xml`.

## Project layout

```
game/engine    GameLoop, GameWorld, GameConfig, Collision, Camera, InputState, ZombieSpawner, ObjectPool
game/entities  Entity, Player, Zombie, ZombieType, Bullet, ImpactEffect
game/render    GameRenderer, GameColors, PlaceholderSprites
ui/menu        Main menu and placeholder screens
ui/game        GameScreen, pause overlay, controls (joysticks and buttons)
ui/theme       Compose Material 3 theme
data           PlayerPrefsRepository (DataStore)
```
