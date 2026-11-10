# Versions and sources (26.3, experimental)

Every version Trophy Pedestals' 26.3 build pins, and where each was checked (2026-11-07). The sources are ported from
`../TrophyPedestals-26.1.2` by `tools/port-from-26.1.2.py`; that folder's `docs/VERSIONS.md` has the 26.1.2 pins.

| What | Version | Where |
|---|---|---|
| Minecraft | 26.3 | shared catalog `net.ixdarklord:sharedcatalogs:26.3-v1` |
| NeoForge | 26.3.0.42-beta (min 26.3.0.38-beta) | catalog; the minimum is Tierwise 26.3's (and CoolCatLib's) |
| Fabric Loader / Fabric API | 0.19.5 / 0.161.0+26.3 (min API 0.160.6) | catalog; the minimum is Tierwise 26.3's |
| Gradle / Architectury Loom (no-remap) | 9.4.0 / 1.17.493 | wrapper; catalog |
| Java | 25 | catalog |
| CoolCatLib | `8.2.0+26.3`, min 8.2.0 | from the ModResources Maven (branch `26.3`, 16668b4) |
| JEI (optional, compile-only) | 31.17.0.108 (`jei-26.3-common-api`, `-neoforge`, `-fabric`) | https://maven.blamejared.com/mezz/jei/ (as Tierwise 26.3 pins it; the catalog has 31.9.0.56) |
| spark (dev runs, `-Ptest_spark=true`) | 1.10.187 | Modrinth API: `1.10.187-neoforge` lists 26.3 |
| MCP Bridge (dev runs only) | 1.0.0+26.3 | Maven Local |

## CoolCatLib 8.1.0 for 26.3

As on 26.2: `SpecialModelRendererRegistry` and `CreativeTabs.builder(ItemLister)` (with its access widener line) were
copied from `../CoolCatLib-26.1.2` into `../CoolCatLib-26.3` (not committed; the user agreed) and published with
`./gradlew publishToMavenLocal -Ptestmods=false`. That local jar also carries the folder's other uncommitted work.

## Left out on 26.3

KubeJS (and Rhino) have no 26.3 build (newest `26.1.2-8.0.8+neoforge` on Modrinth): the KubeJS plugin isn't ported.

## API differences handled (from the 26.3 Minecraft sources Loom generates)

Everything 26.2 changed (see `../TrophyPedestals-26.2/docs/VERSIONS.md`), and:
- `PoseStack.mulPose` takes only matrices: quaternions go through `rotate`.
- `LivingEntity.drop` and `Inventory.placeItemBackInInventory` take a `Prediction` (`SERVER_ONLY` here).
- `LootContext.getOptionalParameter` is `getOptional`; `LootItemFunctions.ROOT_CODEC` is `DIRECT_CODEC`.
- A conditional loot function takes one condition (`Optional<Holder<LootItemCondition>>`), not a list.
- `ArmedEntityRenderState.attackTime` is `swingAnimation`.
- There's no `AxeItem`: an axe is an item in `#minecraft:axes`; `SoundEvents.AXE_STRIP` is a holder.
- Blocks have no codecs (`simpleCodec`, `codec()` are gone).

**Loot table format.** 26.3's loot tables have one `"condition"` (several go in `minecraft:all_of`'s `"terms"`) and one
`"modifier"` (a list is a sequence) in place of the `"conditions"` and `"functions"` lists, name a function's or
condition's kind with `"type"`, and `minecraft:block_state_property` is `minecraft:match_block` (`"blocks"`,
`"state"`). Checked against vanilla 26.3's own loot tables (`blocks/oak_planks`, `blocks/red_bed`,
`blocks/shulker_box`, `entities/wither_skeleton`). The port script rewrites the mod's and the example pack's loot
tables, and the game tests' loot function JSON, into it. Recipes, tags, item models and blockstates are unchanged.
