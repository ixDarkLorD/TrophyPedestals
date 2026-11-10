---
title: Getting started
description: Craft a pedestal, put something on it, and find out where trophies come from.
---

# Getting started

## Craft a pedestal

A pedestal is crafted from six of its material around a gold ingot. There's one for 37 of vanilla's stones and woods:

- **Stones:** stone, cobblestone, mossy cobblestone, cobbled deepslate, deepslate, blackstone, stone bricks, bricks,
  mud bricks, resin bricks, nether bricks, red nether bricks, granite, diorite, andesite, calcite, tuff, basalt, end
  stone, purpur, prismarine, dark prismarine, sandstone, red sandstone and quartz.
- **Woods:** oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, pale oak, bamboo, crimson and warped.

Each one has a gold diamond carved on its fluted column, and a dark glass lens set in a gold bezel in its well. A stone
pedestal is mined with a pickaxe, a wooden one with an axe.

Six more come from blocks that aren't on that list: amethyst block, obsidian, copper block, mossy stone bricks, smooth
stone and polished blackstone bricks, crafted the same way. These are **custom pedestals**, drawn in their block's own
textures and named after it. Any block, a mod's included, can make one; a data pack only has to add the recipe
([Custom pedestals](../reference/index.md)).

## Put something on it

Use a pedestal with an item in hand to put the item on it, and with an empty hand to take it back (unless the server
has turned that off). It turns slowly over the well in a soft cone of warm light, and a named item shows its name over
it while you look at it. That's a **display pedestal**.

A lit pedestal lights the area round it too: a display at light level 10, a trophy from 11 (common) up to 15
(legendary).

## Earn trophies

A **trophy** is a pedestal that records something: what it was, who earned it and when. It shows its icon (or its
mob) turning above it in its rank's light, and its text panel over it while you look at it. Breaking a trophy keeps
its record, so you can move it.

- **Mobs.** A mob you kill now and then drops a trophy of itself: one in a hundred kills by default. It keeps how the
  mob looked: its variant and colors, whether it was a baby, its gear and saddle, and the name it was given. The ender
  dragon and the wither give legendary trophies, the warden and the elder guardian epic ones, and other mods' mobs
  drop them too. How the rest are ranked is the server's choice ([Mob trophies](../reference/mob-trophies.md)).
- **Players.** With the server's `mob_trophies.player_trophies` on (it's off by default), a player killed by another player can
  drop a trophy of their statue, in their own skin.
- **Other mods.** A mod can register its own kind of trophy, and FTB Quests can give one as a quest reward.
- **Packs.** Quests, advancements and loot tables can hand trophies out ([Making trophies](../reference/making-trophies.md)).

## Every trophy has a rank

There are five ranks, and each one is grander than the last:

| Rank | Its light |
|---|---|
| Common | a grey glow, three rising outlines and a faint flare |
| Uncommon | green, faster outlines, a stronger flare, and a chime when it's placed |
| Rare | blue, a halo circling the icon, a spark orbiting it, and a bell |
| Epic | purple and pink, five shimmering outlines, a pulsing double flare and two crossed halos |
| Legendary | gold, a beam of light, a crown of light shards, a gold crown, bursts of light and a fanfare |

The rank shows over the trophy, in its tooltip and in its item's name color, and its text panel wears the rank's own
frame, from a plain grey line to a bevelled gold frame with a winged crest. Every effect is in [Ranks](../reference/ranks.md).

## Make it yours

Sneak and use a pedestal with an empty hand to open its [adjust screen](adjust.md). From there you can set its size
and height, its text, its pose and animation, its light and sounds, and how it answers redstone.
