# Versions and sources (1.21.1)

Every version Trophy Pedestals' 1.21.1 build pins, and where each was checked (2026-11-07). The sources are ported from
`../TrophyPedestals-26.1.2` by `tools/port-from-26.1.2.py`; that folder's `docs/VERSIONS.md` has the 26.1.2 pins.

| What | Version | Where |
|---|---|---|
| Minecraft | 1.21.1 | shared catalog `net.ixdarklord:sharedcatalogs:1.21.1-v10` |
| NeoForge | 21.1.200 (min 21.1.200) | catalog (as `../Tierwise-26.1.2/docs/VERSIONS.md` records it) |
| Fabric Loader / Fabric API | 0.17.2 / 0.116.5+1.21.1 (min API 0.116.5) | catalog; the minimum is Tierwise 1.21.1's |
| Gradle / Architectury Loom (remap) | 9.4.0 / 1.17.493 | wrapper; catalog |
| Java | 21 | catalog |
| CoolCatLib | `8.2.0+1.21.1`, min 8.2.0 | from the ModResources Maven (branch `1.21-1.21.1`, f95ae3b: `LivingEvents.DEATH`, `BlockEntityRendererRegistry`, `BlockRenderLayers`) |
| JEI (optional, compile-only) | 19.22.1.316 (`jei-1.21.1-common-api`) | catalog (`libs.jei`), resolved by the build |
| KubeJS + Rhino (optional, NeoForge, compile-only) | `2101.7.2-build.381` / `2101.2.8-build.91+Rhino-1.21` | Modrinth API (`kubejs`, `rhino` versions for 1.21.1); the plugin compiles against them unchanged |
| spark (dev runs, `-Ptest_spark=true`) | `1.10.124-neoforge-1.21.1` | Modrinth API (newest NeoForge 1.21.1 file) |
| MCP Bridge (dev runs only) | 1.0.0+1.21.1 | Maven Local |

## What's different here (and why)

**Left out:** the pale oak (1.21.2) and resin bricks (1.21.4) pedestals, trophy cabinet and shelf, with their models,
recipes, loot, names and tag entries (asked about; the port script's `MISSING_MATERIALS`).

**Player statues:** there's no mannequin before 1.21.9, so a statue's record names `minecraft:player`, and the client
draws it as a player figure made only to be drawn (`compat/render/StatueFigure`), wearing the skin of the player its
look names, in the same five poses.

**Mob animations** (`client/MobAnimations`, written for 1.21.1): walking, running and attacking (each listed only if it
moves the mob's model) and the mob's own keyframe animations, found by their `AnimationState` field names, which only
NeoForge's runtime (Mojang names) has: under Fabric's intermediary names no keyframe animations are found. 26.1's
on/off states and poses, read from render-state fields, have no counterpart.

**Item models:** 1.21.1 has no item definitions, so every pedestal, cabinet and shelf item is a code-drawn model
(`builtin/entity`) drawn by `compat/render/ItemDefinitions`, which reads 26.1's `assets/trophypedestals/items/*.json`
(the kinds those use: model, special, composite, has_component condition, display_context select) and bakes the models
they name as CoolCatLib extra models. A cabinet's own item model is moved to `<wood>_trophy_cabinet_trimmed`.
In inventory slots every pedestal item is lit as a block (26.1's trophy models are lit from the front).

**Block entity renderers:** CoolCatLib for 1.21 had no `BlockEntityRendererRegistry` (and vanilla's
`BlockEntityRenderers.register` is private), so it was backported to `../CoolCatLib-1.21` (not committed; the user
chose it): the same `register(Supplier<type>, provider)` as 26.x, taking 1.21's `BlockEntityRendererProvider<T>`; NeoForge
and Forge queue it for their `EntityRenderersEvent.RegisterRenderers`, Fabric registers it with Fabric API's
`BlockEntityRendererRegistry`. Published as `8.2.0+1.21.1` with `./gradlew publishToMavenLocal -Ptestmods=false`.

**Block render layers:** a pedestal's flutes and well imprint are partly see-through; 26.1 draws those faces
translucent by itself, but here a block's model is drawn in one layer, solid unless registered. CoolCatLib for 1.21
had no way to register one, so `BlockRenderLayers` was added to `../CoolCatLib-1.21` (not committed; the user chose
it): Forge and NeoForge set it during client setup (`ItemBlockRenderTypes.setRenderLayer`), Fabric with Fabric API's
`BlockRenderLayerMap`. Every pedestal is registered translucent (cabinets and shelves are opaque).

**Drawn once a frame:** a block entity whose renderer draws off-screen is listed both with its chunk and with the
level's global ones, and both lists are drawn every frame, so a pedestal was drawn twice and its light (added to
the scene) was twice as bright as on 26.1. `compat/render/OncePerFrame` skips the second draw in a frame.

**Config:** a config constant's description (`EnumType.Displayable.description`) isn't shown: CoolCatLib for 1.21.1
doesn't have it.

## API differences handled (from the 1.21.1 Minecraft sources Loom generates)

Through the port script's renames and edits, and compat classes that give 26.1's calls over 1.21.1's
(`compat/`, in `tools/keep/`):
- Saving: `ValueInput`/`ValueOutput` over a block entity's saved tag (`saveAdditional`/`loadAdditional` with a
  registry provider); a removed block's drops come from its block's `onRemove`. An entity's saved name is JSON in a
  string (`ValueInput.readName`).
- `Identifier` is `ResourceLocation`; `ARGB` (compat) for `FastColor`; `EntitySpawnReason` is `MobSpawnType`, and
  `EntityType.create` takes no reason; `spawnAtLocation` takes no level.
- Blocks: `useItemOn` returns `ItemInteractionResult`; 1.21.1's `neighborChanged`, `updateShape` and
  `propagatesSkylightDown` signatures; block codecs as 1.21.1 has them. A block with a block entity draws nothing unless
  it says so (`getRenderShape` is `MODEL` here).
- Loot functions: registered as `LootItemFunctionType`, named by `getType()`; context keys are `LootContextParam`s.
- Registration: no ids in properties, block items named by their block, creative tabs list items through
  `displayItems` (its output is public here).
- Rendering: 26.1's render states, `SubmitNodeCollector`, `ItemStackRenderState`/`ItemModelResolver`,
  `CameraRenderState` and special model renderers as compat classes over `MultiBufferSource`, the item and entity
  renderers (`compat/render/`). Block entity renderers draw in one `render()` call that fills and draws their state.
- GUI: `GuiGraphics` for `GuiGraphicsExtractor`; screens and widgets `render`/`renderWidget`; mouse and key input as
  numbers; tooltips with `setTooltipForNextRenderPass`; the animation list over the rest through the pose's depth.
- JEI 19's two-method `ISubtypeInterpreter`.
- Recipes name an ingredient as `{"item": id}` / `{"tag": id}`; NeoForge 21.1's version range for one Minecraft
  version is `[1.21.1]`.
