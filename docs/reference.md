# Trophy Pedestals reference

## Pedestals

A **trophy pedestal** records an achievement: one of the mod's own feats, an advancement, or anything else another mod
registers a kind of trophy for ([Trophy kinds](#trophy-kinds-for-mods)). Placed, it shows the achievement's icon turning above it,
glowing like a hologram, with its rank's light round it ([Ranks](#ranks)); floating over it are its rank, what it
records in its color, who earned it (a player, their team, or "the whole server") and when, in the reader's own date format. The text sits on a panel in its rank's frame, and with the particles shows while a player looks at the pedestal within their reach. Sneaking and using a pedestal with an empty hand opens its adjustment screen, a trophy's in tabs (Display: size, height, yaw, pitch and roll; Text: when it shows, its height, its size; Behavior: spinning, flourishes, sounds, a legendary trophy's crown, the showcase light and redstone; Pose, for a mob or statue: its pose and animation): its text shows when looked at, always (within 32 blocks) or never, its item or mob can be shown from half to twenty times its usual size (its light, outlines, flare, beam, halos, crown and particles grow with it), moved from half a block down to ten blocks up and turned to face any way; it can stand still instead of spinning; a mob trophy's mob can hold one pose instead of moving (Mob Pose: Frozen, which stops a blaze's rods and an allay's wings); an achievement trophy earned by a player can show that player's statue, in their own skin, in place of its icon (Display: Statue), standing, crouching, swimming, gliding or sleeping (the button beside it; a player trophy's is beside Mob Pose); a mob or statue can play one animation on loop, picked from a list found from the mob itself (Animation: walking, running and attacking where its model moves for them, its own animations like a warden's roar or a camel sitting down, its own states like a fox sitting or sleeping, and its own poses like a parrot's; choosing one sets a frozen mob moving); its text can be shown from half to three times its usual size, frame and all (Text Size); its flourishes (its rank's beam, halos, crowns, flares and particles) can be turned off, leaving only its showcase light and outlines; its showcase light (the lit lens, the cone of light rising out of the well and its frame) can be turned off too, on a trophy or a display pedestal (Showcase Light); its sounds can be turned off; its text can be moved up or down; and it can answer redstone, showing its display only while powered or hiding it then. Each section's name has a ? beside it that explains the option while hovered, and so does each Behavior button. Each slider has a button putting it back, and Reset puts everything back. Mods can set a pedestal's settings and lock them with `net.ixdarklord.trophypedestals.api.PedestalSettings` (`set`, `lock`, `force`, `unlock`; `PedestalBlocks.pedestalAt` finds the pedestal at a position): a locked pedestal's screen opens for no one, its owner and operators included, and a player who tries is told the mod's message in their action bar ("This pedestal's settings are locked" unless it gives its own), or is refused silently. Who may adjust it is the server's `pedestals.access` setting: `OWNER` (the default: the player who placed it, and operators; anyone, for a pedestal placed before pedestals had owners) or `ANYONE`. The item's tooltip says the same, and breaking the pedestal keeps its record.

Pedestals come in vanilla's stones and woods (`trophypedestals:<material>_pedestal`): stone, cobblestone, mossy cobblestone,
cobbled deepslate, deepslate, blackstone, stone bricks, bricks, mud bricks, resin bricks, nether bricks, red nether
bricks, granite, diorite, andesite, calcite, tuff, basalt, end stone, purpur, prismarine, dark prismarine, sandstone, red
sandstone and quartz, and oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, pale oak, bamboo, crimson and
warped. An empty one is crafted from six of its material around a gold ingot (`data/trophypedestals/recipe/<material>_pedestal.json`,
which a data pack can replace); a stone one is mined with a pickaxe, a wooden one with an axe.

Any other block, a mod's stones and woods included, makes a **custom pedestal** (`trophypedestals:custom_pedestal`): one block
whose material is data, the `trophypedestals:pedestal_material` component naming the block it's made of. It's drawn with that
block's own textures (its top, bottom and sides, tinted as the block is), with the same gold carving and well as every
pedestal, and named after it ("Mossy Stone Bricks Pedestal"; give the item a `minecraft:item_name` for another name). A
data pack hands them out with a recipe, a loot table or `/give`, and breaking one keeps its material. The mod adds none of
its own; the example data pack (`testpack/`) has six, crafted like the others: amethyst block, obsidian, copper block,
mossy stone bricks, smooth stone and polished blackstone bricks (`data/trophypedestals_example/recipe/<block>_pedestal.json`).
A recipe of your own looks like this:

```json
{
  "type": "minecraft:crafting_shaped",
  "key": {"G": "minecraft:gold_ingot", "S": "minecraft:amethyst_block"},
  "pattern": ["SGS", " S ", "SSS"],
  "result": {"id": "trophypedestals:custom_pedestal", "components": {"trophypedestals:pedestal_material": "minecraft:amethyst_block"}}
}
```

One with no material, or a block no longer in the game, is drawn as stone. Either tool mines it. It can be a trophy too
(`set_trophy` on one in a loot table, below), and works as any pedestal does. A crafted pedestal displays any item put on it (use it with the item in hand; use it with an empty hand to take it back), as a showcase: the item turns slowly in a soft cone of warm light rising out of the well, over a gently glowing frame; a named item shows its name over it while you look at it. The rising outlines, flare, beam and particles are a trophy's only. An empty pedestal's well shows a dark glass lens raised in a gold bezel, which fades up to the rank's color once it holds something (and back down as it turns off). Every pedestal's column is fluted.

### Trophy cabinets and shelves

A **trophy cabinet** (two blocks wide, half a block deep, holding four trophies; cabinets stacked on one another join)
and a **trophy shelf** (on the side of a solid block, holding two; shelves side by side join) come in every wood, plain,
with no trim. Use a block on one to trim it with that block: its rails, posts, corner caps and brackets are drawn in
that block's textures. A gold ingot (or a gold block) gives the gold trim, and an axe takes the trim off again. The
trim is the `trophypedestals:trim_material` component (`minecraft:gold_block` for gold), kept when it's broken; a
trimmed one's item shows its trim. Use a trophy on a spot to put it there, and an empty hand to take it back.

### Ranks

Every trophy has a rank, its **rarity**, which sets how grand it looks and sounds. A trophy's kind picks its rank (a mod's kind can read it
from its own data; common if it names none); the loot function and the API can name another. Each rank has its own colors, and its effects grow with it:

| Rank | Colors | Looks | Sounds |
|---|---|---|---|
| `common` | grey | three rising outlines, a faint flare, a two-armed spiral of light and a rare glint | none of its own |
| `uncommon` | green | faster outlines, a stronger flare and more glints | a chime when placed |
| `rare` | blue | four outlines, a fuller flare, more glints, a halo circling the icon and a spark orbiting it | a bell when placed, and a chime now and then |
| `epic` | purple and pink | five shimmering outlines, a pulsing double flare, two crossed halos, a three-armed spiral, two sparks circling the icon | an enchanting sound when placed, and amethyst resonating now and then |
| `legendary` | gold | a beam of light, six shimmering outlines, a large pulsing double flare, a turning crown of light shards round the icon, a gold crown floating over it, three circling sparks and bursts of light | a fanfare when placed, and a beacon's hum |

A lit pedestal lights the area round it by its trophy's rank: a display (or a trophy with no rank) at light level 10, then 11 for common up to a legendary trophy's full 15. The particles show while a player looks at the trophy, and the ambient sounds play within 12 blocks. Past 40 blocks a
trophy draws only its icon and its well (and a legendary beam), and past 64 nothing over its pedestal; a display shown
larger or higher is drawn, in full, from as many times farther (up to 256 blocks), and the particles follow the player's particle
setting (half as many on Decreased, none on Minimal).

Mods add ranks with `net.ixdarklord.trophypedestals.api.PedestalRarities.register`, from common setup so servers and clients
both know them: `PedestalRarity.builder(id, rank, color, accent)` sets each effect (`rings`, `flare`, `flarePulse`,
`secondFlare`, `spiral`, `glints`, `orbiters`, `halos`, `crown`, `beam`, `shimmer`, `burst`, `placeSound`, `ambientSound`), and the name
goes under the `trophypedestals.rarity.<namespace>.<path>` lang key. For anything more, `api/client/PedestalEffects.register`
gives a rank an effect of its own that draws each frame and ticks while the trophy is looked at. A trophy whose rank
is no longer there shows as common.

### Mob trophies

A mob a player kills now and then drops a **mob trophy**: a pedestal showing the mob turning over it, with its name,
who slew it and when. The server's config turns them on or off (`mob_trophies.enabled`, on by default) and sets the chance
(`mob_trophies.chance`, 0.01, one in a hundred; 1 is every kill). Only kills by a player count. Their rank is the
server's `mob_trophies.rank`: `NONE` (the default) gives them none, shown as a display pedestal is (its warm light,
no rank line); `STRENGTH` ranks them by how tough the mob is; `KILLS` ranks them by how many of that mob the player
has killed (the game's own kill statistic): from `mob_trophies.kills.uncommon` (10 by default) uncommon, `rare` (25)
rare, `epic` (50) epic and `legendary` (100) legendary, and common before that. `mob_trophies.kills.overrides` sets
other counts for some mobs or groups, each `"<mob id or #entity type tag> = uncommon, rare, epic, legendary"`; a mob
named by its id wins over one of its tags, and the first matching entry is used. By default bosses need fewer:
`"#trophypedestals:legendary_mob_trophies = 1, 2, 3, 5"` and `"#trophypedestals:epic_mob_trophies = 2, 5, 10, 20"`. By strength, a mob in the
`trophypedestals:legendary_mob_trophies` entity type tag (the ender dragon and the wither) gives a legendary trophy, in
`trophypedestals:epic_mob_trophies` (the warden and the elder guardian) an epic one, and any other one by its health plus four
times its attack: under 25 common, under 50 uncommon, under 120 rare, under 250 epic, and from there legendary. Data
packs can add to either tag. Other mods' mobs drop trophies as vanilla's do: any living thing but a player or an armor
stand counts, unless it's in the `trophypedestals:no_mob_trophy` entity type tag (the giant, by default). A mob with no spawn egg shows a spawner in the
inventory, and one whose mod can't draw it outside a world shows its icon on the pedestal instead (logged once). In the inventory and in hand, a mob trophy shows the mob's spawn egg over the pedestal. Mods
make one with `pedestal/MobTrophies.trophy`.

With the server's `mob_trophies.player_trophies` on (off by default), a player another player kills drops a **player trophy** at
the same chance: their statue (a mannequin in their own skin) turning over the pedestal, named after them and slain by
their killer, ranked as mob trophies are. Its icon is their head.

A mob trophy keeps how its mob looked when it was slain: its variant and colors (a red sheep, a tabby cat, a villager's
profession and biome), whether it was a baby, its gear and saddle, and the name it was given, which the trophy then goes
by ("jeb_ Trophy", and a jeb_ sheep's wool keeps cycling). Mods' mobs keep their own looks the same way. What it doesn't
keep is where the mob was, its health, effects, memories, trades or owner; a look over 16 KB is dropped, and that
trophy shows the mob as it spawns. The look is the `look` field of the item's `trophypedestals:mob_trophy` component, in the
mob's own saved-data format, so a command can give one:

```mcfunction
/give @p trophypedestals:deepslate_pedestal[trophypedestals:mob_trophy={entity:"minecraft:zombie",slayer:"Steve",slain_at:0L,rarity:"rare",look:{IsBaby:1b,equipment:{head:{id:"minecraft:golden_helmet"}}}}]
```

A placed mob trophy **grows**: every ten seconds or so it counts how many of its mob its slayer has killed (the game's
own kill statistic, while they're online) and shows it, "Slain by Steve (152 slain)". With the server's
`mob_trophies.rank` set to `KILLS`, a ranked one goes up a rank as the count reaches the next of the `kills` counts,
never down (`mob_trophies.growing` turns this off).

Now and then a mob trophy that drops is **gilded** (`mob_trophies.gilded_chance`, 0.02, one in fifty): the same trophy on
a gold pedestal (a custom pedestal of gold blocks), named "Gilded Zombie Trophy".

### Collecting

The **Mob Trophies Ledger** (a book and a gold nugget) lists every mob that can be a trophy, in the order of the mob trophies'
creative tab. Each mob its reader has had a trophy of (one dropped for them, or a first kill) shows in full, its name on
hover; the rest are dark silhouettes, "???". A gilded one has a gold corner, and the top counts them: "Collected 23 of
87 (26%)".

**Buying back a lost trophy.** A trophy lost (burned, despawned, broken by someone else) can be bought back from the
Ledger: click a collected mob to pick it (a gold outline, and its cost on hover), then click it again to buy a new trophy
of it for 5 experience levels (`ledger.levels`; free in creative). Sneak and click for its gilded one, if you had one,
for twice as many. It's yours, slain by you now, at the rank a new one would drop at. One you carry already can't be
bought again, so it can't be stocked up on.

A trophy cabinet whose four spots hold trophies of four different mobs from one **set** shows the set's name over it in a
gold frame. The sets are the Nether, the End, the Ocean, the Bosses and the Illagers. Each is an entity type tag under
`trophypedestals:sets/` (`trophypedestals:sets/nether`), so data packs can change them or add their own, named by the
lang key `trophypedestals.set.<name>`.

A **Leaderboard Crest** (a gold ingot and paper) used on an empty display pedestal makes it a **leaderboard**: it shows
whoever on the server leads one statistic, as their statue in their own skin, with the board's name, theirs and their
count. Using the crest opens a picker of every board: each of the game's statistics (mob kills, deaths, distance
walked, time played... and any a mod adds), with who leads it now and by how much, and a search bar that finds a board
by its name or its leader. Picking one shows it; using the crest on a leaderboard opens the picker again to change it.
Most Mob Kills, Most Player Kills, Most Deaths, Farthest Walked, Longest Played, Most Fish Caught, Most Jumps and Most
Damage Dealt come first, with titles of their own; the rest read "Top: <the statistic>". Everyone who has played counts, online or
not, and it updates live, within a second. It stays light on the server: saved statistics are read once as it starts,
the online players' counts are checked once a second for only the boards a loaded pedestal shows, and a pedestal is
sent an update only when its leader or their count changes. An empty hand takes the crest back, and breaking the
pedestal drops it.

### Showing them off

**Engraving.** Rename a trophy in an anvil and the name is engraved on it: its text frame shows that name, in the
trophy's own colour, in place of what it records. It keeps the name placed and broken.

**Glass cases.** Use glass, or any of the sixteen stained glasses, on a pedestal that shows something (a trophy, an
item, a mannequin or a leaderboard) to close a case of it over it: clear glass with a silver rim (a clear case's lid
tinted by its light), or stained glass in its colour. Every case is the same size, standing on the cap and as wide as
it, the pedestal and its case within two blocks (the space over the pedestal must be free), and solid, so no one walks
into it. Its upper part is clicked as the pedestal, and a middle click on it gives the pedestal; breaking it lifts the
glass off. What it shows floats centred in the case, a gap over and under it, as large as that room lets it be (a mob or mannequin standing still, facing its yaw), with nothing reaching out of
the glass: a mob or mannequin is fitted by how it's posed and animated, and by what it holds, and anything tipped is fitted
however it turns. Its display size can make it smaller in the case, never larger than the case lets it be (its height
setting is kept for when the glass comes off). There's no beam or halos under
glass. Closing a case sounds like putting an item in an item frame, and lifting one like taking it out. Nothing goes on it or comes off it until the glass is lifted:
sneaking and using an empty hand on the glass lifts it (the same glass comes back), for those who may adjust the pedestal; on the pedestal under it, that opens its adjust screen as usual. Breaking the pedestal drops
the glass.

**Sounds and redstone.** Use a mob trophy with an empty hand and it makes its mob's sound (its ambient sound, or its
hurt sound for a mob with none), at most once a second, softer under glass; a trophy's Sounds setting turns it off. A
comparator reads a pedestal: its rank, 1 (none, or common) to 5 (legendary), while it shows something, and 0 while it's
empty or hidden by redstone.

**Mannequins.** Use an armor stand on an empty display pedestal to install it, making it a mannequin pedestal. Armour
then goes to its own slot, swapping with what was there, and anything else goes in its hand (a shield, or what's worn in
an off hand, in its off hand). Its adjust screen (sneak and use an empty hand) gets two more tabs:

- **Model:** show it as the armor stand, or as a player in the skin of the player who placed the pedestal. Either
  wears the outfit.
- **Pose:** pick one of five presets (Standing, Walking, Waving, Pointing and Sitting), or turn each of the armor stand's
  parts (head, body, arms and legs) about x, y and z, in steps of 5 degrees, each with a reset to the armor stand's own
  pose. Under **Your Poses**, name the pose and save it, then pick it again on any mannequin or delete it. Saved poses
  are kept on your own computer (`config/trophypedestals/poses.json`), so they follow you into every world and server.
  The tab shows only while the model is the armor stand and there's no glass case over it: under glass the armor
  stand stands in its own pose, at its full size, and its pose comes back when the glass comes off.

An empty hand takes the whole outfit back, then the armor stand. Breaking the pedestal drops both. Without an armor
stand, armour goes on display like any other item.

### Earned trophies

Players also earn trophies for what they do, given straight to them (or dropped at their feet if their inventory is
full) and announced in chat (`earned_trophies.announce`).

- **Advancements.** Off by default. With the server's `earned_trophies.advancements` on, an advancement gives a
  trophy titled and shown as it is: `CHALLENGES` (the purple-framed ones), `GOALS` (goals and challenges) or `ALL`
  (`NONE` is the default). Tasks are uncommon, goals rare and challenges epic. Each is given once.
- **First kills.** The first player on the server to kill the Ender Dragon or the Wither gets a legendary trophy of it,
  "First Wither Slain", and the first to kill the Warden or an Elder Guardian an epic one. No one after them does.
- **Milestones.** Walking 100 km (Wanderer, rare), killing 1,000 mobs (Monster Hunter, rare), catching 100 fish
  (Angler, uncommon) and playing for 100 hours (Veteran, epic).

All but the advancements' are data, in `data/trophypedestals/trophypedestals/feats/`, and packs add their own the same
way (`earned_trophies.feats` turns them all off). Each file is one trophy:

```json
{
  "trigger": {"type": "stat", "stat": "minecraft:custom", "value": "minecraft:walk_one_cm", "at_least": 10000000},
  "title": "Wanderer",
  "icon": "minecraft:leather_boots",
  "rank": "rare",
  "pedestal": "trophypedestals:gold_pedestal",
  "once": true
}
```

| Field | Default | Meaning |
|---|---|---|
| `trigger` | (needed) | what earns it (below) |
| `title` | the lang key `trophypedestals.feat.<namespace>.<path>` | its name, a text component |
| `icon` | a gold ingot | the item turning over it |
| `rank` | `common` | its rank |
| `pedestal` | deepslate | the pedestal it's on |
| `once` | `true` | once per player, or every time it's earned |

| Trigger `type` | Fields | Earned when |
|---|---|---|
| `advancement` | `advancement` | a player earns that advancement (its title and icon are the advancement's unless given; one already earned counts when they next join) |
| `stat` | `stat` (default `minecraft:custom`), `value`, `at_least` | a player's statistic reaches `at_least` (checked every 5 seconds) |
| `first_kill` | `entity` (an id or `#tag`), `scope` (`server`, the default, or `player`) | a player kills that mob first, of everyone or for themselves; the trophy shows the mob |

A file that can't be read is logged with its pack and path, and skipped.

### Making trophies

A data pack can hand out trophies with the `trophypedestals:set_trophy` loot function, in a loot table, an
advancement reward or `/loot`. It makes a pedestal item a trophy recording `subject`, an achievement of `kind` (a kind a
mod registers, such as a quest mod's `examplemod:quest` with the quest as its subject), recorded as earned now; other
items are left as they are.

```json
{
  "type": "minecraft:item",
  "name": "trophypedestals:cobblestone_pedestal",
  "functions": [{"function": "trophypedestals:set_trophy", "kind": "examplemod:quest", "subject": "mypack:first_quest", "rarity": "epic"}]
}
```

| Field | Means |
|---|---|
| `kind` | the kind of achievement (required) |
| `subject` | what it records, as the kind names it (required) |
| `unlocker` | `player` (the default), `team` or `server` |
| `name` | the player's or team's name; without it, the entity the loot is for (the player opening a chest, for one) |
| `rarity` | its rank (`epic`, or a mod's `namespace:path`); without it, the kind's own for it (common if none) |

A trophy of a kind no installed mod registers (its mod was removed) still places and keeps its record, showing its
subject's id and no icon.

Mob trophies are handed out the same way, as quest rewards or a sure drop from a boss, with the
`trophypedestals:set_mob_trophy` loot function. In a mob's own loot table it needs nothing: it shows the mob that died, as it
looked, slain by the player who last hurt it. Anywhere else (a chest, an advancement reward, `/loot`, a quest) it
names the mob:

```json
{
  "type": "minecraft:item",
  "name": "trophypedestals:blackstone_pedestal",
  "functions": [{"function": "trophypedestals:set_mob_trophy", "entity": "minecraft:wither", "rarity": "legendary"}]
}
```

| Field | Means |
|---|---|
| `entity` | the mob it shows; without it, the mob the loot is for (in a mob's loot table, the one that died), and anything else is left as it is |
| `look` | how the mob looks, in its own saved-data format, as `/summon` takes it (`{"IsBaby": true}`); without it, the look of the mob the loot is for if it's that kind of mob, otherwise as it spawns |
| `slayer` | who slew it; without it, the player who last hurt the mob the loot is for, or the player the loot is for |
| `rarity` | its rank (`epic`, a mod's `namespace:path`, or `none`); without it, by the mob's toughness, as `mob_trophies.rank` `STRENGTH` ranks them |

It's recorded as slain now, and items other than pedestals are left as they are. From a function or command block,
`/trophypedestals mob_trophy <players> <entity> [pedestal] [rarity]` gives each player one, slain by them, ranked by the
mob's toughness unless a rank is named. Mods use `TrophyPedestals.createMob` (from a living mob, look included, or a
kind of mob and a look) and `mobRecord`; `rarity` reads either kind of trophy's rank.

### Trophy kinds (for mods)

A mod gives trophies for its own achievements by registering a kind, from its mod constructor, on both sides:

```java
TrophyKinds.register(Identifier.fromNamespaceAndPath("mymod", "quest"), new TrophyKind() {
    public Component name(Identifier subject, boolean client) { return Quests.name(subject); }
    public ItemStack icon(Identifier subject, boolean client) { return Quests.icon(subject); }
    public @Nullable PedestalRarity rarity(Identifier subject) { return PedestalRarities.EPIC; } // optional
});
ItemStack trophy = TrophyPedestals.create(kind, questId, PedestalRecord.Unlocker.PLAYER, player.getName().getString());
TrophyPedestals.give(player, List.of(trophy));
```

`name` and `icon` are asked on the client every frame a trophy is drawn (`client` true) and on the server when the item
is made (`client` false), so keep them cheap; `color` (white by default) colors the name on the text frame.
`TrophyPedestals` also has `create(pedestal, kind, subject, unlocker, name, at, rarity)`, `defaultPedestal()`,
`pedestal(id)`, `record(stack)`, `mobRecord(stack)`, `rarity(stack)` and `createMob(...)`; all in
`net.ixdarklord.trophypedestals.api`.

A pedestal's **Team** access asks `PedestalTeams` whether two players share a team: scoreboard teams by default. A mod
with teams of its own sets another source with `PedestalTeams.setSource`, so its teams count.

## Commands

`/trophypedestals mob_trophy <players> <entity> [pedestal] [rarity]` (operators) gives each player a mob trophy, slain
by them, on a deepslate pedestal or the one named, ranked by the mob's toughness or as named.

## Config

The server config (`config/trophypedestals-server`, per world) has three groups.

**`[mob_trophies]`**: trophies that mobs drop.

| Option | Default | Meaning |
|---|---|---|
| `enabled` | `true` | whether mobs a player kills can drop a trophy |
| `chance` | `0.01` | the chance, 0 to 1 |
| `player_trophies` | `false` | whether a player another player kills can drop a statue trophy |
| `gilded_chance` | `0.02` | the chance a mob trophy that drops is gilded, 0 to 1 |
| `growing` | `true` | whether a placed mob trophy counts its slayer's kills, and grows a rank with `KILLS` |
| `rank` | `NONE` | `NONE`, `STRENGTH` or `KILLS` (see [Mob trophies](#mob-trophies)) |
| `kills.uncommon` ... `kills.legendary` | 10, 25, 50, 100 | kills for each rank, with `KILLS` |
| `kills.overrides` | bosses need fewer | other counts for some mobs or tags |

**`[earned_trophies]`**: trophies players earn (see [Earned trophies](#earned-trophies)).

| Option | Default | Meaning |
|---|---|---|
| `advancements` | `NONE` | which advancements give a trophy: `NONE`, `CHALLENGES`, `GOALS` or `ALL` |
| `feats` | `true` | whether data packs' trophies are given (the mod's own first kills and milestones included) |
| `announce` | `true` | whether a player is told in chat when they earn one |

**`[ledger]`**: buying back mob trophies from the Mob Trophies Ledger (see [Collecting](#collecting)).

| Option | Default | Meaning |
|---|---|---|
| `buy_back` | `true` | whether a player can buy back a mob trophy they've collected |
| `levels` | `5` | the experience levels one costs (a gilded one twice as many); 0 makes it free |

**`[pedestals]`**: who may adjust pedestals, and what they can change.

| Option | Default | Meaning |
|---|---|---|
| `access` | `OWNER` | who may adjust a pedestal: its owner (and whoever it allows) and operators, or `ANYONE` |
| `take_items` | `true` | whether an item can be taken off a display pedestal with an empty hand |
| `trophy_settings.display`, `.text`, `.pose` | `true` | whether a trophy's screen has those tabs (a tab turned off is hidden, and the server ignores its settings) |
| `display_pedestal_settings.display` | `false` | whether a display pedestal's screen has a Display tab: its item's size, height and rotation |
| `display_pedestal_settings.text` | `false` | whether a display pedestal's screen has its text's height and size, on a Text tab |

With both `display_pedestal_settings` off, a display pedestal keeps its one-page screen (when its text shows, redstone,
sounds and its showcase light). With either on, its screen has tabs too: Display (if on), Text, and Behavior (spin,
sounds, showcase light and redstone).

## Other mods

- **JEI and REI:** a "Mob Trophies" page for each mob that can be a trophy (its egg, the trophy it drops, its rank and
  the chance), found from the deepslate pedestal.
- **Jade and The One Probe:** what a looked-at pedestal holds: its trophy, rank and who earned it (a mob trophy's
  kills too), an engraved name, a leaderboard's leader, a mannequin, an item on display, and its glass case. Before 26.1,
  The One Probe is on NeoForge and Forge only (it had no Fabric build).
- **FTB Quests:** a "Trophy" reward, made as it's claimed: titled as the pack maker writes it (the quest's title if
  left blank), showing the item they name, of the rank they pick, recorded as earned by the player who claims it. From
  1.20.1 to 26.1.2 (FTB Quests has no build for 26.2 or 26.3 yet).
- **EMI:** not yet.

## KubeJS (NeoForge and Forge, up to 26.1.2)

Server scripts get a `TrophyPedestals` binding:

| Call | Does |
|---|---|
| `TrophyPedestals.isPedestal(level, pos)` | whether there's a pedestal there |
| `TrophyPedestals.pedestalSettings(level, pos)`, `setPedestal(level, pos, {...})` | a pedestal's settings, by name: `textMode`, `scale`, `height`, `yaw`, `pitch`, `roll`, `spins`, `animates`, `effects`, `sounds`, `textHeight`, `textScale`, `crown`, `light`, `redstone`, `statue`, `statuePose`, `animation` (and `locked`) |
| `TrophyPedestals.forcePedestal(level, pos, {...}, message)`, `lockPedestal(level, pos[, message])`, `unlockPedestal(level, pos)` | set and lock its settings against every player, with a refusal message (or `null`, silently) |
