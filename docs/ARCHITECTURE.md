# Architecture

Laid out like Tierwise and the author's other mods: one folder per Minecraft version (this is 26.1.2; the others come
later), built with CoolCatLib's shared scripts (ModResources `gradle/v4`) and Architectury Loom. CoolCatLib is the only
loader abstraction.

```
common/    all game logic (net.ixdarklord.trophypedestals)
neoforge/  the entrypoint, metadata and the KubeJS plugin (KubeJS is NeoForge only on 26.x)
fabric/    the entrypoint and metadata
testpack/  data the game tests read (not in the jar): an amethyst pedestal recipe and a wither trophy
tools/     generators for the pedestal, cabinet and trim models, textures, recipes and loot (Python, Pillow)
```

Inside `common/`:

- `pedestal/`: blocks, block entities, items, the loot functions, `MobTrophies`, `MobLooks`, `Statues`, and `Pedestals`
  (the registries, data components and creative tabs).
- `client/`: the pedestal, cabinet and item renderers, the light and particles, mob animations and the adjust screen.
- `network/`: the open and adjust payloads for pedestals and displayed trophies.
- `api/`: what other mods use: `TrophyPedestals`, `TrophyKinds` / `TrophyKind`, `PedestalTeams`, `PedestalSettings`,
  `PedestalBlocks`, `PedestalRarities` / `PedestalRarity`, and `client/PedestalEffects`.
- `command/`, `compat/jei/`, `gametest/`, and `TrophyPedestalsConfig` (the server config).

## Trophy kinds

A trophy (`PedestalRecord`) records a `kind` and a `subject`, never another mod's types. The mod that owns the kind
registers a `TrophyKind` with `TrophyKinds`, which names the subject and gives its icon and default rank, on both sides.
That keeps this mod free of any dependency: Tierwise registers `tierwise:tier` when both are installed, and a trophy
whose kind's mod is missing still loads and places, showing its subject's id.
