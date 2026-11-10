# Changelog
This file is for listing all the changes to this project

## v0.1.0 Release | Nov 10, 2026
### ✨ New Features
- First release, for Minecraft 26.1 to 26.1.2, 26.2 and 26.3 (NeoForge, Fabric), 1.21.1 (NeoForge, Fabric) and 1.20.1 (Forge, Fabric), built on CoolCatLib 8.2.0.
- **Pedestals** in 37 of vanilla's stones and woods, each crafted from six of its material around a gold ingot, and a custom pedestal of any other block through data (six recipes included). Any item put on one turns in a soft cone of light.
- **Five ranks**, common to legendary, each grander than the last: outlines, flares, halos, sparks, a beam, a crown, a text frame of its own and sounds. A lit pedestal lights the room by its rank.
- **Mob trophies:** mobs a player kills now and then drop a pedestal showing them as they looked (variant, colors, gear, name), other mods' mobs included; bosses give epic and legendary ones. With `mob_trophies.player_trophies` on, a defeated player drops their statue in their own skin.
- **Adjust screen** (sneak and use an empty hand): size, height, rotation (yaw, pitch and roll), text, pose and animation, spin, flourishes, sounds, light, crown, redstone and who else may adjust it.
- **Trophy cabinets and shelves** in every wood: cabinets hold four and stack, shelves hang on walls and join, and their trim can be any block. Each trophy on them keeps its own settings.
- **Earned trophies:** a trophy for the first kill of each boss on the server, for milestones (100 km walked, 1,000 mobs killed, 100 fish caught, 100 hours played) and, if the server turns them on, for advancements. Data packs add their own in `data/<namespace>/trophypedestals/feats/`.
- **Collecting:** mob trophies count their slayer's kills and grow a rank; rare gilded trophies on gold pedestals; the Mob Trophies Ledger, listing every mob trophy you've collected and buying back a lost one for experience levels; cabinet sets (Nether, End, Ocean, Bosses, Illagers); and leaderboard pedestals, showing whoever leads a statistic as their statue.
- **Showing them off:** engrave a trophy by renaming it in an anvil, close a glass case over any pedestal, and install an armor stand on a display pedestal to dress it as a mannequin with armour and held items, shown as the armor stand (posed part by part in its adjust screen) or as its owner. Mob trophies make their mob's sound when used, and comparators read a pedestal's rank.
- **Integrations:** REI mob trophy pages, Jade and The One Probe tooltips for pedestals, and an FTB Quests trophy reward (where each has a build for that version).
- **Creative tabs** "Trophy Pedestals" and "Trophy Pedestals: Mobs" (a trophy of every living mob), and the mob trophies in JEI.
- **For packs and mods:** the `trophypedestals:set_trophy` and `set_mob_trophy` loot functions, `/trophypedestals mob_trophy`, a server config (display pedestals can get Display and Text tabs, off by default), a Java API (`TrophyPedestals`, `TrophyKinds`, `PedestalSettings`, `PedestalRarities`) and a `TrophyPedestals` KubeJS binding (NeoForge and Forge).
