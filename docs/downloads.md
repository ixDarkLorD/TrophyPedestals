---
title: Downloads
description: Where to get Trophy Pedestals, and what it needs.
---

# Downloads

Trophy Pedestals is on [Modrinth](https://modrinth.com/mod/trophypedestals) and
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/trophy-pedestals). It's needed on the server and every
client.

## Versions

| Minecraft | Loaders | Source branch |
|---|---|---|
| 26.3 | NeoForge, Fabric | [`26.3`](https://github.com/ixDarkLorD/TrophyPedestals/tree/26.3) |
| 26.2 | NeoForge, Fabric | [`26.2`](https://github.com/ixDarkLorD/TrophyPedestals/tree/26.2) |
| 26.1 – 26.1.2 | NeoForge, Fabric | [`26.1.2`](https://github.com/ixDarkLorD/TrophyPedestals/tree/26.1.2) (main line) |
| 1.21.1 | NeoForge, Fabric | [`1.21-1.21.1`](https://github.com/ixDarkLorD/TrophyPedestals/tree/1.21-1.21.1) |
| 1.20.1 | Forge, Fabric | [`1.20.1`](https://github.com/ixDarkLorD/TrophyPedestals/tree/1.20.1) |

Every version has the same features. On 1.21.1 and 1.20.1 the pale oak and resin brick pedestals are left out (those
blocks aren't in the game yet), and statues are drawn as a player figure in the player's skin.

## What it needs

- [CoolCatLib](https://ixdarklord.github.io/CoolCatLib/) 8.2.0 or newer, on the server and every client.
- On Fabric, Fabric API.

Everything else is optional, and turns on by itself when it's installed:

| Mod | What it adds | Where |
|---|---|---|
| JEI, REI | a page for every mob trophy | every version |
| Jade | what a looked-at pedestal holds | every version |
| The One Probe | the same | NeoForge and Forge; Fabric from 26.1 |
| FTB Quests | a trophy reward | 1.20.1 to 26.1.2 |
| KubeJS | a `TrophyPedestals` binding for server scripts | NeoForge and Forge, 1.20.1 to 26.1.2 |

## For mod developers

Trophy Pedestals is on the [ModResources Maven](https://ixdarklord.github.io/ModResources/), free to use with no token:

```groovy
repositories {
    maven {
        name = "ModResources"
        url = "https://ixdarklord.github.io/ModResources/maven/"
    }
}

dependencies {
    compileOnly "net.ixdarklord.trophypedestals:trophypedestals-common:0.1.0+26.1.2"       // in the common module
    implementation "net.ixdarklord.trophypedestals:trophypedestals-fabric:0.1.0+26.1.2"    // in each loader's module
}
```

Artifacts are `net.ixdarklord.trophypedestals:trophypedestals-<module>:<version>+<minecraft>`, the module being
`common`, `fabric`, `neoforge` or `forge`. On 1.21.1 and older, use `modImplementation` for the loader jars. The
[For mods](docs/reference/for-mods.md) page has the API.

## Building from source

Check out the branch for your Minecraft version, then:

```bash
./gradlew build                        # the jars, in each loader's build/libs
./gradlew :neoforge:runGameTestServer  # game tests (also :fabric: / :forge:)
```
