# Changelog
This file is for listing all the changes to this project

## v0.2.0 (unreleased)
### ✨ New Features
- **Drop chance overrides** (`mob_trophies.chance_overrides`): set another chance for a mob, an entity type tag or a whole mod, as `"<mob id, #tag or @mod id> = chance"`. JEI and REI show each mob's own chance.
- **Four new trophy sets:** Undead, Arthropods, Farm Animals and Wildlife, for a cabinet of four of their mobs.
- **Leaderboard podium:** a leaderboard pedestal shows second and third place under the leader, in its text (and Jade and The One Probe), and as smaller statues beside the leader's.
- **"Most Mob Trophies" leaderboard:** a new statistic, `trophypedestals:mob_trophies_collected`, kept equal to each player's ledger, is a board like any other.
- **Collection milestones:** Collector (10 different mobs), Curator (25), Archivist (50), Grand Curator (100) and Gilded Gallery (5 gilded), with a `collection` trigger for data pack trophies.
- **Mod sets:** four different mobs of one mod make that mod's set ("Aether II Set") with no data pack needed.
- **Rare trophy announcements** (`mob_trophies.announce_rare`, on by default): the server is told in chat, with a gold trophy cup, when someone gets a legendary or gilded mob trophy.
- **Owned cabinets and shelves:** whoever places one owns it; its Cabinet Settings (sneak with an empty hand on it) set who else may use it (Private, Team, Everyone) and when its set name shows (Shown, On Look, Hidden).
- **Aim at each trophy:** each cabinet and shelf spot is aimed at by itself, through the open front only, so a trophy's text and settings no longer react through the rim, sides or back.
- **Cleaner outlines:** cabinets, shelves and pedestals outline as clean shapes that follow their silhouettes.
- **Ledger hints:** hovering a mob not collected yet shows where it spawns ("Found in: Plains, Forest and 3 more").
- **`/trophypedestals trophy`:** gives a titled trophy, with the icon, rank and pedestal named.
- **Trophy events:** `TrophyPedestalsEvents.mobTrophyDrop` (change or cancel a mob trophy about to drop) and `TrophyPedestalsEvents.collected` (a trophy recorded in a player's ledger) for KubeJS server scripts, and the same as `TrophyEvents` in the Java API for mods on either loader.
- **Client settings** (`config/trophypedestals-client`): each player can turn off trophies' flourishes, ambient sounds and moving mobs, and set how far away pedestals' displays are drawn, for a calmer or lighter trophy room.
- **FTB Quests "Mob Trophy" task:** done once the player has collected a trophy of a named mob, or of a number of different mobs; it can ask for gilded ones. Trophies collected before the quest was added count too.
- **Items in cabinets and shelves:** any item that isn't a block can go on a cabinet's or shelf's spot, standing there by itself beside the trophies (a block in hand still sets the trim).
- **Ledger search and mod filter:** the Mob Trophies Ledger has a search box (collected mobs by name, any mob by its mod's name, so it gives away nothing not yet collected), a button showing one mod's mobs at a time (its count then that mod's), and a Missing toggle showing only the mobs not collected yet.
- **Boss trophies once** (`mob_trophies.boss_trophies_once`, off by default): a boss drops its trophy only for a player who hasn't collected it yet.

### 🔧 Changes
- Display pedestals now always open the tabbed adjust screen. A tab the server turns off (a display pedestal's Display tab by default, or a trophy's tab turned off in `trophy_settings`) shows greyed out and can't be opened; hovering it says the server turned it off and that an operator can turn it on.
- Nothing can be placed in the space on top of a pedestal (by a player, a dispenser or another mod's machine): it's the room for what the pedestal shows. Glass or stained glass placed there closes a glass case over the pedestal instead, as using it on the pedestal does.
- The adjust screens' corner studs sit a pixel in from the frame, and the header's pedestal is drawn larger and sharp.
- The Leaderboard Crest's board picker has a scroll bar, like the adjust screen's and the ledger's; all three sit a pixel further in from the frame and a little shorter.
- The README explains how to contribute a translation.

### 🐛 Fixes
- An item under a glass case stays at its usual size instead of growing to fill the case, and ignores its display size until the glass comes off.
- Trophy cabinet items no longer show broken, gappy or speckled icons (and dropped or held cabinets no longer come apart) in large modpacks: the item model's wooden faces now carry their own texture coordinates instead of reading past the planks' texture into its neighbours in the atlas.

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
