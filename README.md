# Trophy Pedestals

Pedestals to show off what you've done. Put any item on a **display pedestal** in a soft cone of light; collect **mob
trophies**, pedestals showing a mob as it looked when you slew it; defeat a player for their **statue**; and line them
up in **trophy cabinets** and **wall shelves**. Other mods give trophies for their own achievements:
[Tierwise](https://modrinth.com/mod/tierwise) gives one for every tier unlocked.

Built on [CoolCatLib](https://ixdarklord.github.io/CoolCatLib/), which must be installed on the server and every client.

| Minecraft | Loaders | Branch |
|---|---|---|
| 26.1 – 26.1.2 | NeoForge, Fabric | `26.1.2` (main line) |

1.21.1, 1.20.1, 26.2 and 26.3 come later.

## What's in it

- **Pedestals** in 37 vanilla stones and woods, each crafted from six of its material around a gold ingot, and a
  custom pedestal of any block (a mod's included) through data.
- **Five ranks**, common to legendary, each grander than the last: light, outlines, flares, a beam, halos, a crown and
  sounds.
- **Mob trophies:** mobs now and then drop a pedestal showing them as they looked, posed or animated; bosses give epic
  and legendary ones, and other mods' mobs drop them too. With `player_trophies` on, a defeated player can drop their
  statue in their own skin.
- **Trophy cabinets and shelves** in every wood: cabinets hold four and stack, shelves hang on walls and join; their
  gold trim can be any block.
- An **adjust screen** (sneak and use an empty hand): size, height, facing, text, pose and animation, spin,
  flourishes, sounds, light, crown and redstone.
- For packs and mods: the `trophypedestals:set_trophy` and `trophypedestals:set_mob_trophy` loot functions,
  `/trophypedestals mob_trophy`, the `TrophyPedestals`, `TrophyKinds`, `PedestalSettings` and `PedestalRarities` APIs,
  and a `TrophyPedestals` KubeJS binding (NeoForge).

See the [reference](docs/reference.md) for every field, setting and API.

## Building

```bash
./gradlew build                        # NeoForge and Fabric jars
./gradlew publishToMavenLocal          # for Tierwise, which compiles against it
./gradlew :neoforge:runGameTestServer  # game tests (also :fabric:runGameTestServer)
```

CoolCatLib 8.1.0 isn't released yet, so it's built from source into Maven Local first. Every pinned version is in
[docs/VERSIONS.md](docs/VERSIONS.md), and the layout in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## License

To be decided by the author (the build currently says MPL-2.0 as a placeholder).
