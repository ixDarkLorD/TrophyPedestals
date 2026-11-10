# Versions and sources (1.20.1)

Every version Trophy Pedestals' 1.20.1 build pins, and where each was checked (2026-11-07). The sources are ported from
`../TrophyPedestals-26.1.2` by `tools/port-from-26.1.2.py` (common/ and fabric/; forge/ is this version's own); that
folder's `docs/VERSIONS.md` has the 26.1.2 pins.

| What | Version | Where |
|---|---|---|
| Minecraft | 1.20.1 | shared catalog `net.ixdarklord:sharedcatalogs:1.20.1-v9` |
| Forge | 1.20.1-47.4.10 | catalog (as `../Tierwise-26.1.2/docs/VERSIONS.md` records it) |
| Fabric Loader / Fabric API | 0.17.2 / 0.92.6+1.20.1 (min API 0.92.6) | catalog; the minimum is Tierwise 1.20.1's |
| Gradle / Architectury Loom (remap) | 9.4.0 / 1.17.493 | wrapper; catalog |
| Java | 17 | catalog |
| CoolCatLib | `8.2.0+1.20.1`, min 8.2.0 | from the ModResources Maven (branch `1.20.1`, df2dddc: `LivingEvents.DEATH`, `BlockEntityRendererRegistry`, `BlockRenderLayers`) |
| JEI (optional, compile-only) | 15.20.0.112 (`jei-1.20.1-common-api`) | catalog (`libs.jei`), resolved by the build |
| MCP Bridge (dev runs only) | 1.0.0+1.20.1 | Maven Local |

CoolCatLib has **no NeoForge build for 1.20.1**: Forge and Fabric only (Tierwise's PLAN D2).

## Not here (yet)

- **KubeJS**: 26.1.2's plugin is for KubeJS 8 on NeoForge; 1.20.1's KubeJS (6, `2001.6.5-build.26+forge` / `+fabric` on
  Modrinth) has another plugin API. Porting it means a compile-only dependency on it (asked).
- **spark** dev runs: not wired up for 1.20.1 (Modrinth has `1.10.53-forge` / `1.10.53-fabric` for 1.20.1).

## What's different here (and why)

Everything 1.21.1 changes (`../TrophyPedestals-1.21.1/docs/VERSIONS.md`: no pale oak or resin bricks, statues as a
player figure, mob animations, item definitions read by `compat/render/ItemDefinitions`, block entities drawn by their
models), and:

**Block entity renderers:** `BlockEntityRendererRegistry` was backported to `../CoolCatLib-1.20.1` as to 1.21 (Forge's
`EntityRenderersEvent.RegisterRenderers`, Fabric API's `BlockEntityRendererRegistry`; not committed) and published as
`8.2.0+1.20.1`.

**Block render layers and drawn once a frame:** as on 1.21.1 (`BlockRenderLayers` backported to
`../CoolCatLib-1.20.1` too, not committed; `compat/render/OncePerFrame`).

**No item components.** `compat/component/` stands in for 26.1's data components over items' tags. A trophy's records
are kept in the item's `BlockEntityTag`, under the keys its block entity saves them by (`Record`, `MobTrophy`,
`Material`, `Trim`), so the game hands them to a placed pedestal itself, and the loot tables copy them back
(`minecraft:copy_nbt`, turned from 26.1's `copy_components`). A trophy's name (26.1's `item_name`) is kept on its tag as
JSON, or worked out from its record again for a dropped trophy, and `PedestalItem` shows it. Heads' profiles are their
`SkullOwner`, and `compat/ResolvableProfile` stands in for 26.1's.

**Tuff:** polished tuff and tuff bricks came in 1.20.3, so the tuff pedestal is made of, and drawn with, plain tuff.

**Recipes with data:** a 26.1 shaped recipe whose result has components becomes `trophypedestals:crafting_shaped`
(`compat/ShapedNbtRecipe`, `"result": {"item", "count", "nbt"}`), as vanilla 1.20.1's shaped recipes make no item with
data (Forge's can, Fabric's can't). The example pack's amethyst pedestal is one.

**GUI sprites:** 1.20.1 has no GUI sprite atlas; `compat/GuiSprites` draws a sprite from its texture, nine-sliced as its
`.mcmeta` says (edges and middle stretched rather than tiled).

## API differences handled (from the 1.20.1 Minecraft sources Loom generates)

On top of 1.21.1's:
- Block entities save and load a tag (`saveAdditional(CompoundTag)`, `load(CompoundTag)`, no registries); their
  component methods are taken out (the BlockEntityTag carries them). Block methods are public, `use()` is one method
  (the port makes it call the item use, then the empty-hand use; `compat/ItemInteractionResult` stands in for 1.20.5+'s),
  `playerWillDestroy` returns nothing, and blocks have no codecs.
- Loot functions are read by JSON serializers: `compat/LootCodecs` wraps a function's codec as one and reads its
  `"conditions"` with the game's own condition reader. Loot contexts have no random sequence (`create(null)`).
- `ResourceLocation`s are made with `new`; stream codecs and `CustomPacketPayload` come from CoolCatLib's backports
  (`ByteBufCodecs.RESOURCE_LOCATION`, `BLOCK_POS`); `FriendlyByteBuf` for `RegistryFriendlyByteBuf`.
- Codecs' `getOrThrow(false, error -> {})`; Java 17 lists have no `getFirst()`/`getLast()`; a text's color is part of
  its style; `Component.Serializer` reads and writes names without registries; `ExtraCodecs.COMPONENT` for
  `ComponentSerialization.CODEC`.
- Drawing: 1.21's vertex calls over 1.20.1's buffers (`compat/render/VertexConsumer`, writing each vertex's parts in
  order with `endVertex()`); `RenderType.lightning()` for the dragon rays; a statue's skin from the skin manager
  (`getSkinTextureLocation`, `getModelName`).
- GUI: screens draw their own background (`render()` draws it first), scrolling has one amount, widgets have no
  `getBottom()`; a player's reach is the game mode's pick range.
- JEI 15's one-method `IIngredientSubtypeInterpreter`; this CoolCatLib's tooltip event has no tooltip context.
- Data: 1.20.1's folder names (`loot_tables`, `recipes`, `tags/blocks`, `tags/items`, `tags/entity_types`); recipe
  results as `{"item", "count"}`.
- Game tests: Tierwise 1.20.1's mock player on an embedded channel (`gametest/GameTestCompat`), blocks used through
  `use()` with the item in hand, loot functions read from JSON with the game's own reader, and the example recipe read
  by `RecipeManager.fromJson`.
