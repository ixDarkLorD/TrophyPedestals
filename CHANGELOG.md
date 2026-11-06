# Changelog
This file is for listing all the changes to this project

## v0.1.0 Release | Unreleased
### ✨ New Features
- First release, for Minecraft 26.1 to 26.1.2 (NeoForge, Fabric), built on CoolCatLib. Split out of Tierwise as a mod of its own; it runs alone, and Tierwise, when installed, gives a trophy for every tier unlocked.
- **Pedestals** in 37 of vanilla's stones and woods, each crafted from six of its material around a gold ingot, and a custom pedestal of any other block through data (six recipes included). Any item put on one turns in a soft cone of light.
- **Five ranks**, common to legendary, each grander than the last: outlines, flares, halos, sparks, a beam, a crown, a text frame of its own and sounds. A lit pedestal lights the room by its rank.
- **Mob trophies:** mobs a player kills now and then drop a pedestal showing them as they looked (variant, colors, gear, name), other mods' mobs included; bosses give epic and legendary ones. With `player_trophies` on, a defeated player drops their statue in their own skin.
- **Adjust screen** (sneak and use an empty hand): size, height, rotation (yaw, pitch and roll), text, pose and animation, spin, flourishes, sounds, light, crown, redstone and who else may adjust it.
- **Trophy cabinets and shelves** in every wood: cabinets hold four and stack, shelves hang on walls and join, and their trim can be any block. Each trophy on them keeps its own settings.
- **Creative tabs** "Trophy Pedestals" and "Trophy Pedestals: Mobs" (a trophy of every living mob), and the mob trophies in JEI.
- **For packs and mods:** the `trophypedestals:set_trophy` and `set_mob_trophy` loot functions, `/trophypedestals mob_trophy`, a server config, a Java API (`TrophyPedestals`, `TrophyKinds`, `PedestalSettings`, `PedestalRarities`) and a `TrophyPedestals` KubeJS binding (NeoForge).
