# Versions and sources (26.2)

Every version Trophy Pedestals' 26.2 build pins, and where each was checked (2026-11-07). The sources are ported from
`../TrophyPedestals-26.1.2` by `tools/port-from-26.1.2.py`; that folder's `docs/VERSIONS.md` has the 26.1.2 pins.

| What | Version | Where |
|---|---|---|
| Minecraft | 26.2 | shared catalog `net.ixdarklord:sharedcatalogs:26.2-v1` |
| NeoForge | 26.2.0.88 (min 26.2.0.84) | catalog; the minimum is Tierwise 26.2's |
| Fabric Loader / Fabric API | 0.19.5 / 0.161.0+26.2 (min API 0.159.0) | catalog; the minimum is Tierwise 26.2's |
| Gradle / Architectury Loom (no-remap) | 9.4.0 / 1.17.493 | wrapper; catalog |
| Java | 25 | catalog |
| CoolCatLib | `8.2.0+26.2`, min 8.2.0 | from the ModResources Maven (branch `26.2`, 4deb3ab) |
| JEI (optional, compile-only) | 30.48.0.244 (`jei-26.2-common-api`, `-neoforge`, `-fabric`) | https://maven.blamejared.com/mezz/jei/ (as Tierwise 26.2 pins it; the catalog has 30.39.0.232) |
| spark (dev runs, `-Ptest_spark=true`) | 1.10.187 | Modrinth API: `1.10.187-neoforge` lists 26.2 |
| MCP Bridge (dev runs only) | 1.0.0+26.2 | Maven Local |

## CoolCatLib 8.1.0 for 26.2

Maven Local had only `8.0.0+26.2`, and Trophy Pedestals needs two things 26.1.2's 8.1.0 has: 
`SpecialModelRendererRegistry` (the trophy icon and custom pedestal item renderers) and `CreativeTabs.builder(ItemLister)`
(with the access widener line that opens `CreativeModeTab$Output`). Both were copied from `../CoolCatLib-26.1.2` into
`../CoolCatLib-26.2` (not committed; the user agreed) and published with
`./gradlew publishToMavenLocal -Ptestmods=false` (its test mods don't compile on 26.2). That local jar also carries
the 26.2 folder's other uncommitted work (`SceneViewport` changes).

## Left out on 26.2

KubeJS (and Rhino) have no 26.2 build: Modrinth's newest KubeJS is `26.1.2-8.0.8+neoforge`. The KubeJS plugin
(`neoforge/compat/kubejs`, `kubejs.plugins.txt`) isn't ported, and `kubejs` is out of the NeoForge publish
dependencies.

## API differences handled (from the 26.2 Minecraft sources Loom generates)

- Entity type constants moved from `EntityType` to `EntityTypes`.
- `Options.hideGui` is `Gui.hud.isHidden()`; `Minecraft.setScreen` is `Gui.setScreen` (`minecraft.gui.setScreen`).
- `BlockPos.getCenter()` is gone: `Vec3.atCenterOf(pos)`.
- `Items.COPPER_BLOCK` and `Blocks.COPPER_BLOCK` are `WeatheringCopperCollection`s: `.weathering().unaffected()`.

Data and asset formats are the same as 26.1.2's (vanilla's loot tables, recipes, item models and blockstates compared).
