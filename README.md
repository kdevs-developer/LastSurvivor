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

After the last boss falls the level does not end at once: a "Collect the diamonds!" hint appears and you can walk around and pick up the boss's diamonds. The level completes when they are all collected (or have expired).

The Brute and the Brood Mother alternate as the finale boss of each level, getting tougher every level. Campaign waves do not count towards your Infinite best wave. Cleared levels show a tick, and Continue brings back an unfinished level.

### Infinite

Endless waves with a boss every 5th wave, in the area you pick. You play until you die, and your best wave is saved.

An area opens in Infinite once the Campaign has taken you there: the Swamp at level 3, the Wasteland at level 5, the Tundra at level 7 and the Graveyard at level 9 (the Ruined City is always open). Locked areas show a padlock and the level you need.

On the Mode select screen every area and every campaign level has its own little scene (a street with wrecks, swamp trees and poison, dunes and cacti, pines and snow, tombstones under a moon), so they no longer look alike.

### Areas

Each area is a different map, not just a different colour. It has its own obstacles and hazards, its own weather and its own visibility. Obstacles stop you and the zombies, and they stop most bullets too, so you can use them as cover. Zombies are not stupid about it: when a prop is in their way they pick a side and walk round it instead of pushing against it, so hiding behind a single car only buys you a few seconds. Every campaign level (and every Infinite area) lays its props out differently.

| Area | Obstacles and hazards | Weather and visibility |
| ---- | --------------------- | ---------------------- |
| Ruined City | Wrecked cars and concrete roadblocks for cover. **Explosive barrels:** shoot one and it blows up like a grenade, setting off the barrels next to it | Rain |
| Toxic Swamp | Dense forest and rocks, so you have to weave between trees. **Poison pools:** standing in one keeps you poisoned | Drifting spores, green haze, dim edges |
| Dust Wasteland | Boulders and cacti on open ground. **Quicksand:** slows you and the zombies by half | Dust storm that gusts |
| Frozen Tundra | Ice rocks and pines. **Deep snow drifts:** slow you and the zombies by half | Snowfall and a blizzard haze that gusts |
| Haunted Graveyard | Rows of tombstones with aisles, crypts and dead trees | **Near darkness:** you only see around you by a flickering light, plus fog and lightning flashes |

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
| Revolver | 180 coins | 55 | 1.8/s | 6 | Hits hard, pierces 2 zombies |
| Assault Rifle | 550 coins | 22 | 8.3/s | 30 | Fast and accurate, pierces 2 zombies |
| Auto Shotgun | 700 coins | 10 x 5 pellets | 2.5/s | 10 | Semi-automatic, short range |
| Sniper | 900 coins | 120 | 0.9/s | 5 | Extreme range, pierces up to 5 zombies |
| Minigun | 1200 coins | 9 | 18/s | 80 | Huge magazine, slow 3 s reload |

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

Ten playable characters, each with different stats, one **active** ability (a button in the game) and one **passive** skill (always on). Survivor is free; the rest are bought with **diamonds**.

| Character | Price | Stats | Active ability (cooldown) | Passive skill |
| --------- | ----- | ----- | ------------------------- | ------------- |
| Survivor | Free | Balanced | Adrenaline: +50% speed for 5 s and 15 health (20 s) | Last Stand: once per run a deadly hit leaves 35% health and 2.5 s of safety |
| Runner | 50 diamonds | Health -25%, speed +25% | Dash: 0.2 s burst, no damage taken (6 s) | Nimble: 15% of zombie swings miss |
| Tank | 100 diamonds | Health +50%, speed -15%, damage taken -20% | Shockwave: 45 damage, knock-back and 1.5 s stun around you (14 s) | Spiked Armor: zombies that hit you take 14 damage back |
| Medic | 150 diamonds | Regenerates 0.8 HP/s | Heal Pulse: 45 health over 3 s, cures poison (18 s) | Triage: every kill restores 2 health |
| Scavenger | 175 diamonds | Health -10%, speed +10%, magazine +10% | Jackpot: drops 3 random power-ups around you (30 s) | Scavenger: +35% coins from kills, power-ups last 30% longer |
| Engineer | 200 diamonds | Health -10%, reload time -10% | Turret: auto-fires for 10 s (25 s) | Overclock: abilities recharge 25% faster |
| Marksman | 250 diamonds | Health -15%, reload time -15% | Deadeye: 6 s of double damage that pierces 2 extra zombies (22 s) | Sharpshooter: 20% of shots crit for 2.5x damage |
| Commando | 300 diamonds | Reload time -30%, magazine +30% | Rage: rapid fire and double damage for 6 s (25 s) | Veteran: all weapons deal 15% more damage |
| Cryo | 350 diamonds | Health +10%, speed +5%, damage taken -10% | Freeze: stuns every zombie in a wide area for 3 s (20 s) | Frostbite: 12% of bullets freeze a zombie for 1 s |
| Demolitionist | 400 diamonds | Health +15%, speed -5%, damage taken -10%, magazine -10% | Airstrike: 5 explosions on the zombies around you (28 s) | Blast Expert: grenades, barrels and airstrikes deal 50% more damage |

All the numbers are in `GameConfig` (the ability and passive sections).

The Characters screen shows one character at a time in a single compact panel with the same layout for everyone: use the arrows to browse, and only the values change. Each character shows the same six spec tiles (Health, Speed, Damage taken, Regen, Reload time, Magazine; green is an advantage, red a drawback), the active ability and passive skill, a Select or Unlock button, and your lifetime statistics, all without scrolling.

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

Toxic neon menus with a moonlit zombie skyline, fog, lightning and flickering windows. In game you get muzzle flashes, blood, shell casings, explosions, camera shake, damage numbers and a red glow when your health is low. There are 31 sound effects, positional so that sounds pan and fade with distance. Heavier zombies sound deeper. Reloading is two sounds: the magazine dropping out, then the new one seating and the slide racked. There is quiet ambient music that changes with the scene: one calm, warm, bell-free loop for the menus and a different one for each area (rain in the city, deep bubbles in the swamp, wind and plucked notes in the desert, glassy bells in the tundra, a far-off bell toll in the graveyard). The loops are WAV files played from memory with AudioTrack loop points, so they repeat with no gap or click. Music follows the Sound switch, fades in, and pauses when the app goes to the background; the volume is `GameConfig.MUSIC_VOLUME`. The run opens with its own start sound and every following wave rotates through three different ones, and the graveyard's lightning is followed by a rolling thunder. Music is generated by `tools/generate_music.py` and the effects by `tools/generate_sfx.py`. The aiming laser line was removed.

---

## Run it

Open the folder in Android Studio (Otter 3 Feature Drop or newer, AGP 9, Gradle 9.1.0, JDK 17), let Gradle sync, then run on a device or emulator.
