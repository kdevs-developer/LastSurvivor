# Last Survivor

A top-down zombie survival shooter for Android. Hold out against endless waves of the undead, take down bosses, collect coins and diamonds, and unlock new weapons and characters.

**Genre:** top-down survival shooter  |  **Platform:** Android (landscape)  |  **Style:** toxic neon menus, dark horror world

---

## How to play

Survive as many waves as you can. Each wave sends more zombies, faster and tougher than the last. Clear every zombie to finish the wave, earn bonus coins, and start the next one. When your health hits zero the run ends, and your coins, kills and best wave are kept.

### Controls

| Control | What it does |
| ------- | ------------ |
| Left stick | Move |
| Right stick | Aim. Push it to the edge to auto-fire |
| Fire button | Hold to shoot |
| Reload | Reload the magazine (it also reloads automatically when empty) |
| Grenade | Throw a grenade in the direction you face |
| Ability button | Use your character's special ability (a ring shows the cooldown) |

---

## Game modes

Play opens the **Mode select** screen with two tabs. Tap a tile, then press Start.

### Campaign

Ten levels, each set in its own **area** with a fixed number of waves and a **boss finale** on the last wave. Clear the last wave to complete the level, earn a level bonus (50 coins x the level number, plus 3 diamonds) and unlock the next one. Later levels start tougher: more zombies, more health, more speed and the heavy zombie types sooner.

| Levels | Area | Waves per level |
| ------ | ---- | --------------- |
| 1 - 2 | Ruined City | 5, 6 |
| 3 - 4 | Toxic Swamp | 6, 7 |
| 5 - 6 | Dust Wasteland | 7, 8 |
| 7 - 8 | Frozen Tundra | 8, 9 |
| 9 - 10 | Haunted Graveyard | 9, 10 |

The Brute and the Brood Mother alternate as the finale boss of each level, getting tougher every level. Campaign waves do not count towards your Infinite best wave. Cleared levels show a tick, and Continue brings back an unfinished level.

### Infinite

Endless waves with a boss every 5th wave, in the area you pick. You play until you die, and your best wave is saved.

### Areas

Areas change the look only: the floor, the map border and the decoration. Gameplay is the same everywhere.

| Area | Look |
| ---- | ---- |
| Ruined City | Cracked grey asphalt with a red border |
| Toxic Swamp | Murky green ground with a toxic green border |
| Dust Wasteland | Sandy brown ground with an orange border |
| Frozen Tundra | Cold blue-grey ground with an icy blue border |
| Haunted Graveyard | Dark purple ground with a violet border |

---

## Zombies

| Zombie | Appears from | Notes |
| ------ | ------------ | ----- |
| Walker | Wave 1 | Slow and weak, the bulk of every wave |
| Runner | Wave 3 | Fast and lean |
| Toxic | Wave 5 | Its hit poisons you (5 HP/s for 4 s) |
| Tank | Wave 7 | Heavily armoured, takes many hits |

Zombies gain health every wave (+10% per wave) and get faster (up to +40%). Wave size grows with each wave, with a cap on how many can be alive at once.

### Boss fights

A boss arrives every 5th wave and pays double coins when the wave is cleared. Bosses alternate, and each new one has 50% more health. Below 35% health a boss becomes enraged: faster, with quicker abilities.

| Boss | Appears | Special attack |
| ---- | ------- | -------------- |
| The Brute | Waves 5, 15, 25... | Stops, shows a red strip where it will charge, then rushes along that line. Step aside |
| Brood Mother | Waves 10, 20, 30... | Summons Walkers around herself every few seconds |

---

## Weapons

| Weapon | Price | Damage | Fire rate | Magazine | Notes |
| ------ | ----- | ------ | --------- | -------- | ----- |
| Pistol | Owned | 20 | 4.5/s | 12 | Accurate starter weapon |
| SMG | 120 coins | 11 | 11/s | 30 | Fast, slight spread |
| Shotgun | 250 coins | 12 x 6 pellets | 1.25/s | 6 | Wide cone, short range |
| Rifle | 400 coins | 38 | 2.9/s | 20 | Long range, pierces up to 3 zombies |

Ammo reserve is unlimited, so you only manage the magazine and the reload.

### Upgrades

Every weapon has four upgrade tracks, five levels each:

- **Damage** (+15% per level)
- **Fire rate** (8% faster per level)
- **Magazine size** (+20% per level)
- **Reload speed** (8% faster per level)

A level costs more the higher you go. Upgrades are bought with coins on the Armory screen.

---

## Characters

Six playable characters, each with different stats and one active ability. Survivor is free; the rest are bought with **diamonds**.

| Character | Price | Stats | Ability (cooldown) |
| --------- | ----- | ----- | ------------------ |
| Survivor | Free | Balanced | Adrenaline: +50% speed for 4 s (20 s) |
| Runner | 50 diamonds | Health -25%, speed +25% | Dash: 0.2 s burst, no damage taken (6 s) |
| Tank | 100 diamonds | Health +50%, speed -15%, damage taken -20% | Shockwave: 45 damage, knock-back and 1.5 s stun around you (14 s) |
| Medic | 150 diamonds | Regenerates 0.8 HP/s | Heal Pulse: 45 health over 3 s, cures poison (18 s) |
| Engineer | 200 diamonds | Health -10% | Turret: auto-fires for 10 s (25 s) |
| Commando | 300 diamonds | Reload time -30%, magazine +30% | Rage: rapid fire and double damage for 6 s (25 s) |

The Characters screen shows one character at a time in a single compact panel with the same layout for everyone: use the arrows to browse, and only the values change. Each character shows the same six spec tiles (Health, Speed, Damage taken, Regen, Reload time, Magazine; green is an advantage, red a drawback), the ability, a Select or Unlock button, and your lifetime statistics, all without scrolling.

---

## Power-ups

Zombies can drop a power-up when they die (stronger zombies drop more often). Walk over it to collect it. It stays on the ground for 14 seconds and blinks before it vanishes.

| Power-up | Effect |
| -------- | ------ |
| Medkit | +40 HP and cures poison (twice as likely to drop when your health is low) |
| Rapid Fire | Half the time between shots for 8 s |
| Double Damage | Bullets deal 2x damage for 8 s |
| Shield | No damage at all for 6 s |
| Grenades | +2 grenades (max 5) |

Active buffs show as timers under your kills and coins. Picking up the same buff again refreshes it.

### Grenades

You start every run with 2 grenades (max 5). A grenade slides to a stop, then explodes after a 1.4 s fuse. It deals heavy damage that falls off with distance, and knocks zombies back. It never hurts you.

---

## Coins, diamonds and progress

- **Level bonus (Campaign):** completing a level pays 50 coins x the level number and 3 diamonds.
- **Coins** come from kills (Walker 1, Runner 2, Toxic 3, Tank 5) and from clearing waves (5 x wave number). Spend them on weapons and upgrades.
- **Diamonds** are rare drops from zombies (Walker 3%, Runner 4%, Toxic 6%, Tank 15%), and bosses always drop 5. Spend them on new characters.
- **Health:** you have 100 HP. After a zombie hit you are briefly invulnerable, so a crowd cannot kill you in a single instant.
- **Continue:** the start of every wave from wave 2 is saved. Continue on the main menu restarts that wave, in the same level or area, with your saved grenades and at least 50% health. Dying clears the save.
- **Everything is saved automatically:** coins, diamonds, kills, best wave, unlocked weapons and characters, upgrades, settings and lifetime statistics.

### Lifetime statistics

The Characters screen tracks best wave, zombies killed, runs played, bosses defeated, accuracy, coins earned and total time survived.

---

## Screens

- **Mode select:** Campaign (ten levels on different areas) or Infinite (pick an area), then Start.
- **Main menu:** a big Play button (opens Mode select), then Continue (shows the saved wave and health), Armory, Characters and Options. Coins and diamonds are shown top left; best wave, total kills and the sound switch top right. A rotating survival tip sits at the bottom.
- **Armory:** buy, equip and upgrade weapons.
- **Characters:** browse, unlock and select characters, and view your lifetime statistics.
- **Options:** sound, screen shake, damage numbers, FPS counter, blood and gore level (Off / Low / Normal), and Reset progress.
- **In game:** health bar, wave and zombies left, kills, coins, grenades, active buffs, boss health bar, weapon and ammo, and the pause menu.

## Look and sound

Toxic neon menus with a moonlit zombie skyline, fog, lightning and flickering windows. In game you get muzzle flashes, blood, shell casings, explosions, camera shake, damage numbers and a red glow when your health is low. There are 31 sound effects, positional so that sounds pan and fade with distance. Heavier zombies sound deeper.

---

## Run it

Open the folder in Android Studio (Otter 3 Feature Drop or newer, AGP 9, Gradle 9.1.0, JDK 17), let Gradle sync, then run on a device or emulator.
