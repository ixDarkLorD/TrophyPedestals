---
title: Docs
description: What Trophy Pedestals adds, where to start reading, and what it needs.
---

# Docs

Trophy Pedestals gives a Minecraft world somewhere to keep what its players have done. Put any item on a **display
pedestal** and it turns in a soft cone of light. Slay a mob and now and then it drops a **mob trophy**: a pedestal
showing that mob as it looked, with who slew it and when. Defeat a player (when the server allows it) for their
**statue**. Other mods can give trophies for their own achievements. Line them all up in **trophy cabinets** and **wall shelves**.

## Where to start

<div class="grid cards" markdown>

-   **Just installed it?**

    ---

    Craft a pedestal, put something on it, and find out where trophies come from.

    [Getting started](guide/index.md)

-   **Want it just so?**

    ---

    Size, height, rotation, text, pose, animation, light and redstone, on the adjust screen.

    [The adjust screen](guide/adjust.md)

-   **Running out of floor?**

    ---

    Cabinets that stack and shelves that join, trimmed in any block you like.

    [Cabinets and shelves](guide/cabinets.md)

-   **Making a pack or a mod?**

    ---

    Loot functions, the command, the server config, KubeJS and the Java API.

    [The reference](reference/index.md)

</div>

## What's in it

| | |
|---|---|
| **Pedestals** | 37 of vanilla's stones and woods, each crafted from six of its material around a gold ingot, and a custom pedestal of any other block, a mod's included |
| **Five ranks** | common, uncommon, rare, epic and legendary, each grander than the last: light, outlines, flares, a beam, halos, a crown and sounds |
| **Mob trophies** | mobs, other mods' included, sometimes drop a pedestal showing them as they looked; bosses give epic and legendary ones |
| **Player statues** | with `mob_trophies.player_trophies` on, a defeated player can drop their statue, in their own skin |
| **Cabinets and shelves** | in every wood: cabinets hold four and stack, shelves hang on walls and join, and their trim can be any block |
| **The adjust screen** | sneak and use a pedestal with an empty hand to set how it shows what it holds |
| **For packs and mods** | `trophypedestals:set_trophy` and `set_mob_trophy` loot functions, `/trophypedestals mob_trophy`, a Java API and a KubeJS binding |

In creative, everything is in two tabs: **Trophy Pedestals** (every pedestal, cabinet and shelf) and **Trophy
Pedestals: Mobs** (a trophy of every living mob in the game, mods' included, sorted by rank, then name). With
JEI installed, the mob trophies are listed there too, one per mob.

## Requirements

Trophy Pedestals runs on NeoForge and Fabric for Minecraft 26.1 to 26.1.2; 1.21.1, 1.20.1, 26.2 and 26.3 come later.
It needs [CoolCatLib](https://ixdarklord.github.io/CoolCatLib/) on the server and every client, and Fabric API on
Fabric. JEI and KubeJS (NeoForge) are optional.
