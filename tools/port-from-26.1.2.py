"""
Brings the sources over from the 26.1.2 project (the source of truth) and ports them to Minecraft 1.21.1.

    python tools/port-from-26.1.2.py [--record] [--force] [path to TrophyPedestals-26.1.2]

Copies the common/, neoforge/ and fabric/ sources and resources, the example data pack (testpack/) and the changelog,
then, file by file:
  1. applies RENAMES (regular expressions, to Java sources) and EDITS (exact text, to any file);
  2. applies the file's patch from tools/patches/ (a unified diff of the hand-ported changes): each hunk must match the
     text exactly once, or the script stops, so a change upstream is noticed instead of half ported;
  3. converts the resources to 1.21.1's formats (convert_resources).
The files under tools/keep/ are written by hand for this version (the compat classes that give 26.1's calls over
1.21.1's, and files whose APIs differ too much to patch): they're copied in as they are, and changes to their 26.1.2
originals are ported to them by hand. Files in SKIP aren't brought over.

After changing ported files by hand (in common/, neoforge/ or fabric/), run with --record: it writes each file's
difference from steps 1 and 3 into its patch, and copies changed hand-written files back to tools/keep/. A plain run
refuses to overwrite files changed since the last run or recording (--force overwrites them). The build files
(gradle.properties, the build.gradle files) and docs/ are this version's own and aren't touched.
"""
import difflib
import hashlib
import json
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ARGS = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
SOURCE = Path(ARGS[0]) if ARGS else ROOT.parent / "TrophyPedestals-26.1.2"
RECORD = "--record" in sys.argv
FORCE = "--force" in sys.argv
MODULES = ["common", "neoforge", "fabric"]
COPIED = ["testpack", "CHANGELOG.md"]
PATCHES = ROOT / "tools/patches"
MANIFEST = ROOT / "tools/patches/.written.json"
JAVA = "src/main/java/net/ixdarklord/trophypedestals"
ASSETS = "common/src/main/resources/assets/trophypedestals"
DATA = "common/src/main/resources/data"

# Written by hand for this version: every file under tools/keep/, at the same path in the project. Among them,
# compat/ValueInput and ValueOutput (26.1's saving calls over a saved tag), compat/ARGB (26.1's color helpers) and
# compat/render/ (26.1's render states and collector over 1.21.1's buffers).
KEEP_ROOT = ROOT / "tools/keep"
KEEP: set[str] = {file.relative_to(KEEP_ROOT).as_posix() for file in KEEP_ROOT.rglob("*") if file.is_file()}

# Not brought over.
SKIP: list[str] = []

RENAMES = [
    (r"IdentifierArgument", "ResourceLocationArgument"),
    (r"\bIdentifier\b", "ResourceLocation"),
    (r"\.identifier\(\)", ".location()"),
    (r"::identifier\b", "::location"),
    (r"getInventory\(\)\.getSelectedSlot\(\)", "getInventory().selected"),
    (r"import net\.minecraft\.util\.Util;", "import net.minecraft.Util;"),
    # Packed colors: the compat ARGB has 26.1's helpers (1.21.1's FastColor lacks some).
    (r"import net\.minecraft\.util\.ARGB;\n", ""),
    (r"\bEntitySpawnReason\b", "MobSpawnType"),
    (r"\.snapTo\(", ".moveTo("),
    (r"\.sendOverlayMessage\((.*)\);", r".displayClientMessage(\1, true);"),
    (r"helper\.assertionException\(", "new GameTestAssertException("),
    (r"\.getOptionalParameter\(", ".getParamOrNull("),
    # Registry lookups by id: get() here, getValue() on 26.1.
    (r"(BuiltInRegistries\.\w+|registry)\.getValue\(", r"\1.get("),
    (r"\bRenderTypes\.", "RenderType."),
    (r"import net\.minecraft\.client\.renderer\.rendertype\.RenderTypes;\n", ""),
    (r"\bLightCoordsUtil\.FULL_BRIGHT\b", "LightTexture.FULL_BRIGHT"),
    (r"import net\.minecraft\.util\.LightCoordsUtil;\n", ""),

    # ── Saving: 1.21.1 block entities save to a tag; the compat ValueInput and ValueOutput give 26.1's calls over it.
    (r"import net\.minecraft\.world\.level\.storage\.(ValueInput|ValueOutput);\n", ""),
    (r"protected void saveAdditional\(ValueOutput output\) \{\n        super\.saveAdditional\(output\);",
     "protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {\n        super.saveAdditional(tag, registries);\n"
     "        ValueOutput output = ValueOutput.of(tag, registries);"),
    (r"protected void loadAdditional\(ValueInput input\) \{\n        super\.loadAdditional\(input\);",
     "protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {\n        super.loadAdditional(tag, registries);\n"
     "        ValueInput input = ValueInput.of(tag, registries);"),
    (r"public void removeComponentsFromTag\(ValueOutput output\) \{\n        super\.removeComponentsFromTag\(output\);",
     "public void removeComponentsFromTag(CompoundTag tag) {\n        super.removeComponentsFromTag(tag);\n        ValueOutput output = ValueOutput.of(tag);"),
    (r"import net\.minecraft\.core\.component\.DataComponentGetter;\n", ""),
    (r"\bDataComponentGetter components\b", "DataComponentInput components"),
    # A saved look (a tag) read the same way; an entity's name in it is its text's JSON, in a string.
    (r"\blook\.read\(\"CustomName\", ComponentSerialization\.CODEC\)", 'ValueInput.of(look).readName("CustomName")'),
    (r"\blook\.read\(", "ValueInput.of(look).read("),
    (r"look\.putString\(\"CustomName\", (\w+)\.getName\(\)\.getString\(\)\);",
     r'look.putString("CustomName", Component.Serializer.toJson(Component.literal(\1.getName().getString()), RegistryAccess.EMPTY));'),
    # Nothing calls a block entity as it's removed: its block's onRemove does (see EDITS).
    (r"    @Override\n    public void preRemoveSideEffects\(BlockPos pos, BlockState state\) \{\n        super\.preRemoveSideEffects\(pos, state\);\n",
     "    /** As it's broken (on 1.21.1 its block's onRemove calls this). */\n    public void preRemoveSideEffects(BlockPos pos, BlockState state) {\n"),

    # ── Blocks: 1.21.1's neighbour and shape updates, and item use, have their own signatures.
    (r"Block neighbor, @Nullable Orientation orientation, boolean moved\) \{\n        super\.neighborChanged\(state, level, pos, neighbor, orientation, moved\);",
     "Block neighbor, BlockPos neighborPos, boolean moved) {\n        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved);"),
    (r"import net\.minecraft\.world\.level\.redstone\.Orientation;\n", ""),
    (r"protected BlockState updateShape\(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,\s*\n\s*BlockPos neighborPos, BlockState neighbor, RandomSource random\) \{",
     "protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos,\n"
     "                                     BlockPos neighborPos) {"),
    (r"super\.updateShape\(state, level, ticks, pos, direction, neighborPos, neighbor, random\)", "super.updateShape(state, direction, neighbor, level, pos, neighborPos)"),
    (r"import net\.minecraft\.world\.level\.ScheduledTickAccess;\n", ""),
    (r"protected boolean propagatesSkylightDown\(BlockState state\)", "protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos)"),
    (r"protected InteractionResult useItemOn\(", "protected ItemInteractionResult useItemOn("),

    # ── Permissions: a level number.
    (r"Commands\.hasPermission\(Commands\.LEVEL_GAMEMASTERS\)\.test\(player::permissions\)", "player.hasPermissions(Commands.LEVEL_GAMEMASTERS)"),
    (r"\.requires\(Commands\.hasPermission\(Commands\.LEVEL_GAMEMASTERS\)\)", ".requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))"),
    (r"Commands\.hasPermission\(Commands\.LEVEL_GAMEMASTERS\)\.test\((\w+\.createCommandSourceStack\(\))\)", r"\1.hasPermission(Commands.LEVEL_GAMEMASTERS)"),

    # ── Loot functions: registered as LootItemFunctionType, named by getType(); their context keys are LootContextParams.
    (r"public MapCodec<(SetTrophyFunction|SetMobTrophyFunction)> codec\(\) \{\n        return MAP_CODEC;",
     lambda m: f"@SuppressWarnings(\"unchecked\")\n    public LootItemFunctionType<{m.group(1)}> getType() {{\n"
               f"        return (LootItemFunctionType<{m.group(1)}>) Pedestals.{'SET_TROPHY' if m.group(1) == 'SetTrophyFunction' else 'SET_MOB_TROPHY'}.get();"),
    (r"Set<ContextKey<\?>>", "Set<LootContextParam<?>>"),
    (r"import net\.minecraft\.util\.context\.ContextKey;\n", ""),
    (r"DeferredRegister<MapCodec<\? extends LootItemFunction>>", "DeferredRegister<LootItemFunctionType<?>>"),
    (r"RegistryEntry<MapCodec<\? extends LootItemFunction>> (\w+) = LOOT_FUNCTIONS\.register\(\"(\w+)\", \(\) -> (\w+)\.MAP_CODEC\);",
     r'RegistryEntry<LootItemFunctionType<?>> \1 = LOOT_FUNCTIONS.register("\2", () -> new LootItemFunctionType<>(\3.MAP_CODEC));'),

    # ── Registration: no ids in the properties (the registry names them), and block items are named by their block.
    (r"\s*\.setId\(ResourceKey\.create\(Registries\.\w+, TrophyPedestalsMod\.id\([^()]*(?:\([^()]*\))?\)\)\)", ""),
    (r"\.useBlockDescriptionPrefix\(\)", ""),
    # Creative tabs list their items to the tab's output (public here).
    (r"CreativeTabs\.builder\(", "CreativeTabs.builder().displayItems("),
    (r"MobTrophies\.creativeTrophies\(parameters\.enabledFeatures\(\), items\)", "MobTrophies.creativeTrophies(parameters.enabledFeatures(), items::accept)"),

    # ── Entities: made without a reason, and drop items without being told their level.
    (r"\.create\(([^;]*?), MobSpawnType\.[A-Z_]+\)", r".create(\1)"),
    (r"\.spawnAtLocation\(\w+, (\w+)\)", r".spawnAtLocation(\1)"),
    (r"\bSpawnEggItem\.byId\((\w+)\)\.map\(item -> new ItemStack\(item\.value\(\)\)\)", r"Optional.ofNullable(SpawnEggItem.byId(\1)).map(ItemStack::new)"),
    (r"\bstack\.hasNonDefault\(", "stack.has("),
    # A sound event's id.
    (r"(SoundEvents\.\w+(?:\.value\(\))?)\.location\(\)", r"\1.getLocation()"),
    # A config constant's description isn't shown on this CoolCatLib (the method stays, unused).
    (r"        @Override\n        public Component description\(\)", "        public Component description()"),
    # A player's profile, resolved or not.
    (r"ResolvableProfile\.createUnresolved\((\w+)\)", r"new ResolvableProfile(Optional.of(\1), Optional.empty(), new PropertyMap())"),
    (r"ResolvableProfile\.createResolved\(", "new ResolvableProfile("),
    # A player's statue: there's no mannequin before 1.21.9, so a statue is a player figure, drawn by the client.
    (r"EntityType\.MANNEQUIN\b", "EntityType.PLAYER"),
    # Mobs saved to and loaded from a plain tag.
    (r"TagValueOutput output = TagValueOutput\.createWithContext\(ProblemReporter\.DISCARDING, mob\.registryAccess\(\)\);\n\s*mob\.saveWithoutId\(output\);\n\s*CompoundTag look = output\.buildResult\(\);",
     "CompoundTag look = mob.saveWithoutId(new CompoundTag());"),
    (r"\.load\(TagValueInput\.create\(ProblemReporter\.DISCARDING, \w+, (\w+)\)\)", r".load(\1)"),
    (r"\.loadCustomOnly\(TagValueInput\.create\(ProblemReporter\.DISCARDING, ([^,]+), (\w+)\)\)", r".loadCustomOnly(\2, \1)"),
    (r"import net\.minecraft\.world\.level\.storage\.(TagValueInput|TagValueOutput);\n", ""),
    (r"import net\.minecraft\.util\.ProblemReporter;\n", ""),
    (r"\.look\(\)\.getBooleanOr\(\"(\w+)\", false\)", r'.look().getBoolean("\1")'),
    (r"\blook\.keySet\(\)", "look.getAllKeys()"),
    (r"net\.minecraft\.world\.entity\.monster\.zombie\.", "net.minecraft.world.entity.monster."),

    # ── GUI: 1.21.1 draws with GuiGraphics, screens and widgets render(), and input comes as plain numbers.
    (r"\bGuiGraphicsExtractor\b", "GuiGraphics"),
    (r"\bextractRenderState\((GuiGraphics graphics|graphics), ", r"render(\1, "),
    (r"\bextractBackground\(", "renderBackground("),
    (r"protected void extractContents\(GuiGraphics graphics,", "protected void renderWidget(GuiGraphics graphics,"),
    (r"public void extractWidgetRenderState\(GuiGraphics graphics,", "public void renderWidget(GuiGraphics graphics,"),
    (r"\bgraphics\.blitSprite\(RenderPipelines\.GUI_TEXTURED, ", "graphics.blitSprite("),
    (r"import net\.minecraft\.client\.renderer\.RenderPipelines;\n", ""),
    (r"\bgraphics\.text\(", "graphics.drawString("),
    (r"\bgraphics\.item\(", "graphics.renderItem("),
    (r"\bgraphics\.setTooltipForNextFrame\((.*), mouseX, mouseY\);", r"setTooltipForNextRenderPass(\1);"),
    (r"public void onPress\(InputWithModifiers input\)", "public void onPress()"),
    (r"import net\.minecraft\.client\.input\.(InputWithModifiers|KeyEvent|MouseButtonEvent);\n", ""),
    # Tooltip lines.
    (r"public int getHeight\(Font font\)", "public int getHeight()"),
    (r"public void extractImage\(Font font, int x, int y, int width, int height, GuiGraphics graphics\)", "public void renderImage(Font font, int x, int y, GuiGraphics graphics)"),
    (r"import net\.minecraft\.server\.level\.ParticleStatus;\n", ""),

    # ── Drawing: the compat.render classes give 26.1's render states and collector over 1.21.1's buffers (see COMPAT).
    (r"implements BlockEntityRenderer<(\w+), (?:\w+\.)?State>", r"implements BlockEntityRenderer<\1>"),
    (r"\bcontext\.itemModelResolver\(\)", "ItemModelResolver.INSTANCE"),
    (r"\b(?:minecraft|Minecraft\.getInstance\(\))\.getItemModelResolver\(\)", "ItemModelResolver.INSTANCE"),
    (r"\b(?:minecraft|Minecraft\.getInstance\(\))\.getEntityRenderDispatcher\(\)\.(submit|extractEntity)\(",
     lambda m: "EntityRenderStates." + ("submit" if m.group(1) == "submit" else "extract") + "("),
    (r"\b(\w+)\.create\((pedestal\.getLevel\(\)|minecraft\.level)\)", r"EntityRenderStates.create(\1, \2)"),
    (r"\bLivingEntityRenderer<\?, \?, \?>", "LivingEntityRenderer<?, ?>"),
    # A block entity renderer here draws in one call: it fills its state and draws it there (see block_entity_render).
    (r"    @Override\n    public ((?:\w+\.)?State createRenderState\(\)|void extractRenderState\(|void submit\(State state, )", r"    public \1"),
    # A model's parts: only a hierarchical model has a root.
    (r"ModelPart root = renderer\.getModel\(\)\.root\(\);", "if (!(renderer.getModel() instanceof HierarchicalModel<?> model)) return ModelSizes.measure(renderer.getModel(), mob);\n            ModelPart root = model.root();"),
    (r"\bmob instanceof Avatar\b", "mob instanceof Player"),
    (r"import net\.minecraft\.world\.entity\.Avatar;\n", ""),
    # Item textures are in the block atlas.
    (r"Minecraft\.getInstance\(\)\.getAtlasManager\(\)\.getAtlasOrThrow\(AtlasIds\.(ITEMS|BLOCKS)\)",
     "Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS)"),
    (r"import net\.minecraft\.data\.AtlasIds;\n", ""),
    # A texture's pixel (ABGR here; only its alpha, which is where ARGB's is, is read).
    (r"\bimage\.getPixel\(", "image.getPixelRGBA("),
    # The well's lens and an item's light in an entity sheet.
    (r"\bRenderType\.entityTranslucentEmissive\(", "RenderType.entityTranslucentEmissive("),
]

# 26.1 classes that compat classes stand in for here: their imports become the compat class's (in order).
COMPAT = {
    "net.ixdarklord.coolcatcore.api.client.registry.SpecialModelRendererRegistry": "net.ixdarklord.trophypedestals.compat.render.SpecialModelRendererRegistry",
    "net.minecraft.client.renderer.special.SpecialModelRenderer": "net.ixdarklord.trophypedestals.compat.render.SpecialModelRenderer",
    "net.minecraft.client.renderer.SubmitNodeCollector": "net.ixdarklord.trophypedestals.compat.render.SubmitNodeCollector",
    "net.minecraft.client.renderer.OrderedSubmitNodeCollector": "net.ixdarklord.trophypedestals.compat.render.OrderedSubmitNodeCollector",
    "net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState": "net.ixdarklord.trophypedestals.compat.render.BlockEntityRenderState",
    "net.minecraft.client.renderer.feature.ModelFeatureRenderer": "net.ixdarklord.trophypedestals.compat.render.ModelFeatureRenderer",
    "net.minecraft.client.renderer.item.ItemModelResolver": "net.ixdarklord.trophypedestals.compat.render.ItemModelResolver",
    "net.minecraft.client.renderer.item.ItemStackRenderState": "net.ixdarklord.trophypedestals.compat.render.ItemStackRenderState",
    "net.minecraft.client.renderer.state.level.CameraRenderState": "net.ixdarklord.trophypedestals.compat.render.CameraRenderState",
    **{f"net.minecraft.client.renderer.entity.state.{name}": f"net.ixdarklord.trophypedestals.compat.render.{name}"
       for name in ["EntityRenderState", "LivingEntityRenderState", "ArmedEntityRenderState", "HumanoidRenderState",
                    "AvatarRenderState", "EnderDragonRenderState", "WitherRenderState"]},
}

# Methods whose bodies take renames of their own: (the method's signature, as a regex up to its "{"; the renames).
METHOD_RENAMES = [
    (r"protected ItemInteractionResult useItemOn\([^)]*\) \{", [
        (r"InteractionResult\.TRY_WITH_EMPTY_HAND", "ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION"),
        (r"(?<!Item)InteractionResult\.SUCCESS", "ItemInteractionResult.SUCCESS"),
        (r"(?<!Item)InteractionResult\.PASS\b", "ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION"),
        (r"(?<!Item)InteractionResult\.FAIL\b", "ItemInteractionResult.FAIL"),
    ]),
    # Mouse and key input: the event's parts are the method's arguments.
    (r"public boolean mouseClicked\(MouseButtonEvent event, boolean doubleClick\) \{", [
        (r"MouseButtonEvent event, boolean doubleClick", "double mouseX, double mouseY, int button"),
        (r"super\.mouseClicked\(event, doubleClick\)", "super.mouseClicked(mouseX, mouseY, button)"),
    ] + [(r"\bevent\.x\(\)", "mouseX"), (r"\bevent\.y\(\)", "mouseY"), (r"\bevent\.button\(\)", "button")]),
    (r"public boolean mouseDragged\(MouseButtonEvent event, double dragX, double dragY\) \{", [
        (r"MouseButtonEvent event, double dragX", "double mouseX, double mouseY, int button, double dragX"),
        (r"super\.mouseDragged\(event, dragX, dragY\)", "super.mouseDragged(mouseX, mouseY, button, dragX, dragY)"),
        (r"\bevent\.x\(\)", "mouseX"), (r"\bevent\.y\(\)", "mouseY"), (r"\bevent\.button\(\)", "button"),
    ]),
    (r"public boolean mouseReleased\(MouseButtonEvent event\) \{", [
        (r"MouseButtonEvent event\)", "double mouseX, double mouseY, int button)"),
        (r"super\.mouseReleased\(event\)", "super.mouseReleased(mouseX, mouseY, button)"),
        (r"\bevent\.x\(\)", "mouseX"), (r"\bevent\.y\(\)", "mouseY"), (r"\bevent\.button\(\)", "button"),
    ]),
    (r"public boolean keyPressed\(KeyEvent event\) \{", [
        (r"KeyEvent event\)", "int keyCode, int scanCode, int modifiers)"),
        (r"super\.keyPressed\(event\)", "super.keyPressed(keyCode, scanCode, modifiers)"),
        (r"\bevent\.key\(\)", "keyCode"),
    ]),
]

# Classes the renames may start using, imported where they do: (a regex the text then matches, the class).
IMPORTS = [
    (r"\bPedestalOverlays\.", "net.ixdarklord.trophypedestals.compat.render.PedestalOverlays"),
    (r"\bOncePerFrame\.", "net.ixdarklord.trophypedestals.compat.render.OncePerFrame"),
    (r"\bARGB\.", "net.ixdarklord.trophypedestals.compat.ARGB"),
    (r"\bValueOutput\.of\(", "net.ixdarklord.trophypedestals.compat.ValueOutput"),
    (r"\bValueInput\.of\(", "net.ixdarklord.trophypedestals.compat.ValueInput"),
    (r"\bLevelAccessor level\b", "net.minecraft.world.level.LevelAccessor"),
    (r"\bBlockGetter level\b", "net.minecraft.world.level.BlockGetter"),
    (r"\bItemInteractionResult\b", "net.minecraft.world.ItemInteractionResult"),
    (r"\bLootItemFunctionType<", "net.minecraft.world.level.storage.loot.functions.LootItemFunctionType"),
    (r"\bLootContextParam<", "net.minecraft.world.level.storage.loot.parameters.LootContextParam"),
    (r"\bOptional\.", "java.util.Optional"),
    (r"\bnew PropertyMap\(\)", "com.mojang.authlib.properties.PropertyMap"),
    (r"\bHolderLookup\.Provider registries\b", "net.minecraft.core.HolderLookup"),
    (r"\bCompoundTag tag\b", "net.minecraft.nbt.CompoundTag"),
    (r"\bUidContext context\b", "mezz.jei.api.ingredients.subtypes.UidContext"),
    (r"@Nullable", "org.jetbrains.annotations.Nullable"),
    (r"\bnew GameTestAssertException\(", "net.minecraft.gametest.framework.GameTestAssertException"),
    (r"\bGuiGraphics\b", "net.minecraft.client.gui.GuiGraphics"),
    (r"\bItemModelResolver\.INSTANCE\b", "net.ixdarklord.trophypedestals.compat.render.ItemModelResolver"),
    (r"\bEntityRenderStates\.", "net.ixdarklord.trophypedestals.compat.render.EntityRenderStates"),
    (r"\bTextureAtlas\.LOCATION_BLOCKS\b", "net.minecraft.client.renderer.texture.TextureAtlas"),
    (r"\binstanceof Player\b", "net.minecraft.world.entity.player.Player"),
    (r"\bHierarchicalModel<", "net.minecraft.client.model.HierarchicalModel"),
    (r"\bModelSizes\.", "net.ixdarklord.trophypedestals.compat.render.ModelSizes"),
    (r"\bSheets\.", "net.minecraft.client.renderer.Sheets"),
    (r"\bMultiBufferSource buffers\b", "net.minecraft.client.renderer.MultiBufferSource"),
    (r"\bBlockEntity blockEntity\b", "net.minecraft.world.level.block.entity.BlockEntity"),
    (r"\bCameraRenderState\.current\(\)", "net.ixdarklord.trophypedestals.compat.render.CameraRenderState"),
    (r"\bnew SubmitNodeCollector\(", "net.ixdarklord.trophypedestals.compat.render.SubmitNodeCollector"),
    (r"\bVec3 camera = ", "net.minecraft.world.phys.Vec3"),
    (r"\bMinecraft\.getInstance\(\)", "net.minecraft.client.Minecraft"),
    (r"\bPoseStack poseStack\b", "com.mojang.blaze3d.vertex.PoseStack"),
    (r"\bRenderType\.", "net.minecraft.client.renderer.RenderType"),
    (r"\bComponent\.Serializer\.toJson\(", "net.minecraft.network.chat.Component"),
    (r"\bRegistryAccess\.EMPTY\b", "net.minecraft.core.RegistryAccess"),
    (r"\bRenderShape\.MODEL\b", "net.minecraft.world.level.block.RenderShape"),
    (r"\bLightTexture\.FULL_BRIGHT\b", "net.minecraft.client.renderer.LightTexture"),
    (r"\bParticleStatus\b", "net.minecraft.client.ParticleStatus"),
]


def model_shape(text: str) -> str:
    """A block with a block entity is drawn by its model: before 1.21.2 such a block draws nothing unless it says so."""
    declared = re.search(r"public (?:abstract )?class \w+ extends BaseEntityBlock \{\n", text)
    if declared is None or "getRenderShape(" in text:
        return text
    at = text.find("\n    @Override\n", declared.end())
    if at < 0:
        return text
    method = ("\n    // Drawn by its model (a block with a block entity draws nothing unless it says so).\n"
              "    @Override\n"
              "    protected RenderShape getRenderShape(BlockState state) {\n"
              "        return RenderShape.MODEL;\n"
              "    }\n")
    return text[:at] + method + text[at:]


def block_entity_render(text: str) -> str:
    """Gives a block entity renderer 1.21.1's render(): its state filled (as 26.1 extracts it) and drawn (as 26.1 submits it)."""
    renderer = re.search(r"implements BlockEntityRenderer<(\w+)>", text)
    if renderer is not None:
        # (Asked about the block entity drawn.)
        text = text.replace("public boolean shouldRenderOffScreen()", f"public boolean shouldRenderOffScreen({renderer.group(1)} blockEntity)")
    create = re.search(r"    public (?:\w+\.)?State createRenderState\(\) \{\n        return new State\(\);\n    \}\n", text)
    if renderer is None or create is None:
        return text
    method = (
        "\n    // Drawn in one call here: its state filled, as it's extracted on 26.1, then drawn, as it's submitted.\n"
        "    @Override\n"
        f"    public void render({renderer.group(1)} blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {{\n"
        # (One that draws off-screen is listed twice here, so drawn twice a frame: see compat/render/OncePerFrame.)
        + ("        if (!OncePerFrame.first(blockEntity, partialTicks)) return;\n" if "shouldRenderOffScreen(" in text else "") +
        "        State state = createRenderState();\n"
        "        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();\n"
        "        extractRenderState(blockEntity, state, partialTicks, camera, null);\n"
        "        state.lightCoords = light;\n"
        "        submit(state, poseStack, new SubmitNodeCollector(buffers), CameraRenderState.current());\n"
        # (A pedestal's see-through overlays aren't in its block model here: see compat/render/PedestalOverlays.)
        + ("        PedestalOverlays.draw(blockEntity.getBlockState(), poseStack, buffers, light);\n" if renderer.group(1) == "PedestalBlockEntity" else "") +
        "    }\n")
    return text[:create.end()] + method + text[create.end():]

# Imports a rename may leave unused: dropped where nothing else names the class.
UNUSED = [
    "net.minecraft.world.level.LevelReader",
    "net.minecraft.util.RandomSource",
    "net.minecraft.resources.ResourceKey",
    "net.minecraft.core.registries.Registries",
    "com.mojang.serialization.MapCodec",
    "net.minecraft.world.level.storage.loot.functions.LootItemFunction",
    "net.minecraft.world.InteractionResult",
    "net.minecraft.world.item.ItemStack",
    "net.minecraft.network.chat.ComponentSerialization",
]

EDITS: dict[str, list[tuple[str, str]]] = {
    "common/" + JAVA + "/client/PedestalBody.java": [
        # (Over the material, in the see-through sheet the level draws after the cutout one: the plain see-through
        # type is drawn first here, before the column under the flutes, which then hides it.)
        ("collector.submitCustomGeometry(poseStack, RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (pose, consumer) -> {\n            for (Face face : faces) if (face.texture.overlay())",
         "collector.submitCustomGeometry(poseStack, Sheets.translucentCullBlockSheet(), (pose, consumer) -> {\n            for (Face face : faces) if (face.texture.overlay())"),
    ],
    # A pedestal's lens and bezel have clear pixels: 26.1 draws those faces cutout by itself; here a block's model is drawn
    # in the one layer registered for it (solid unless told). (Its see-through overlays: see compat/render/PedestalOverlays.)
    "common/" + JAVA + "/client/PedestalRenderer.java": [
        ("import net.ixdarklord.coolcatcore.api.client.registry.BlockEntityRendererRegistry;\n",
         "import net.ixdarklord.coolcatcore.api.client.registry.BlockEntityRendererRegistry;\n"
         "import net.ixdarklord.coolcatcore.api.client.registry.BlockRenderLayers;\n"),
        ("        BlockEntityRendererRegistry.register(Pedestals.CABINET_ENTITY, TrophyCabinetRenderer::new);\n",
         "        BlockEntityRendererRegistry.register(Pedestals.CABINET_ENTITY, TrophyCabinetRenderer::new);\n"
         "        Pedestals.BLOCKS_BY_STONE.values().forEach(block -> BlockRenderLayers.register(block, RenderType.cutout()));\n"
         "        BlockRenderLayers.register(Pedestals.CUSTOM, RenderType.cutout());\n"),
    ],
    # No resin bricks or pale oak here (see MISSING_MATERIALS).
    "common/" + JAVA + "/pedestal/Pedestals.java": [
        ("        RESIN_BRICKS(MapColor.TERRACOTTA_ORANGE, SoundType.RESIN_BRICKS, false),\n", ""),
        ("        PALE_OAK(MapColor.QUARTZ, SoundType.WOOD, true),\n", ""),
    ],
    # The display comes off as it's broken (1.21.1 block entities hear nothing of it).
    "common/" + JAVA + "/pedestal/PedestalBlock.java": [
        ("    @Override\n    protected VoxelShape getShape(",
         "    @Override\n"
         "    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {\n"
         "        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) pedestal.preRemoveSideEffects(pos, state);\n"
         "        super.onRemove(state, level, pos, newState, moved);\n"
         "    }\n\n"
         "    @Override\n    protected VoxelShape getShape("),
    ],
    "common/" + JAVA + "/pedestal/TrophyDisplayBlock.java": [
        ("    // Its trophies' settings that answer redstone know whether a signal reaches it.\n",
         "    // Its trophies drop as it's broken (1.21.1 block entities hear nothing of it).\n"
         "    @Override\n"
         "    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {\n"
         "        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TrophyCabinetBlockEntity display) display.preRemoveSideEffects(pos, state);\n"
         "        super.onRemove(state, level, pos, newState, moved);\n"
         "    }\n\n"
         "    // Its trophies' settings that answer redstone know whether a signal reaches it.\n"),
    ],
    # A single Minecraft version: NeoForge 21.1's version ranges can't have identical boundaries ([1.21.1,1.21.1]).
    "neoforge/src/main/resources/META-INF/neoforge.mods.toml": [
        ('versionRange = "[${project.supported_min_mc_version},${max_mc_version}]"', 'versionRange = "[${project.supported_min_mc_version}]"'),
    ],
    # 26.1's item definitions, read and drawn here (see compat/render/ItemDefinitions).
    "common/" + JAVA + "/TrophyPedestalsClient.java": [
        ("        CustomPedestalItemRenderer.init();\n", "        CustomPedestalItemRenderer.init();\n        ItemDefinitions.init();\n"),
        ("import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;\n",
         "import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;\nimport net.ixdarklord.trophypedestals.compat.render.ItemDefinitions;\n"),
    ],
    # The animation list goes over everything else: drawn nearer the viewer (there are no strata).
    "common/" + JAVA + "/client/PedestalScreen.java": [
        ("        graphics.nextStratum();\n", "        graphics.pose().pushPose();\n        graphics.pose().translate(0.0F, 0.0F, 400.0F);\n"),
        ("            graphics.fill(x + width - 3, thumbY, x + width - 1, thumbY + thumb, PALE_GOLD);\n        }\n    }\n",
         "            graphics.fill(x + width - 3, thumbY, x + width - 1, thumbY + thumb, PALE_GOLD);\n        }\n        graphics.pose().popPose();\n    }\n"),
    ],
    # JEI 19's subtype interpreters and catalysts.
    "common/" + JAVA + "/compat/jei/TrophyPedestalsJeiPlugin.java": [
        ('    private static ISubtypeInterpreter<ItemStack> interpreter(Function<ItemStack, @Nullable String> subtype) {\n        return (stack, context) -> subtype.apply(stack);\n    }',
         '    private static ISubtypeInterpreter<ItemStack> interpreter(Function<ItemStack, @Nullable String> subtype) {\n        // (JEI 19\'s has two methods: the second is its old string form.)\n        return new ISubtypeInterpreter<>() {\n            @Override\n            public @Nullable Object getSubtypeData(ItemStack stack, UidContext context) {\n                return subtype.apply(stack);\n            }\n\n            @Override\n            @SuppressWarnings("removal")\n            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {\n                String data = subtype.apply(stack);\n                return data == null ? "" : data;\n            }\n        };\n    }'),
        ('        registration.addCraftingStation(MobTrophyCategory.TYPE, Pedestals.DEFAULT.get());\n        registration.addCraftingStation(TrimCategory.TYPE, Items.GOLD_INGOT);',
         '        registration.addRecipeCatalyst(Pedestals.DEFAULT.get(), MobTrophyCategory.TYPE);\n        registration.addRecipeCatalyst(Items.GOLD_INGOT, TrimCategory.TYPE);'),
        ('import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;',
         'import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;\nimport mezz.jei.api.ingredients.subtypes.UidContext;'),
    ],
    # JEI 19's recipe types and slots; the mob drawn as this version draws one in a GUI.
    "common/" + JAVA + "/compat/jei/MobTrophyCategory.java": [
        ('import mezz.jei.api.recipe.types.IRecipeType;',
         'import mezz.jei.api.recipe.RecipeType;'),
        ('public static final IRecipeType<',
         'public static final RecipeType<'),
        ('IRecipeType.create(',
         'new RecipeType<>('),
        ('    public IRecipeType<',
         '    public RecipeType<'),
        ('builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).add(drop.egg());',
         'builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(drop.egg());'),
        ('.add(drop.trophy());',
         '.addItemStack(drop.trophy());'),
        ('        Vector2f at = graphics.pose().transformPosition(0.0F, 0.0F, new Vector2f());\n        int x = Math.round(at.x()), y = Math.round(at.y());\n        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x, y, x + MOB_WIDTH, y + HEIGHT, scale, 0.0625F,\n                x + MOB_WIDTH / 2.0F + LOOK_X, y + LOOK_Y, mob);',
         "        // (Here it's drawn where the page is.)\n        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, 0, 0, MOB_WIDTH, HEIGHT, scale, 0.0625F, MOB_WIDTH / 2.0F + LOOK_X, LOOK_Y, mob);"),
        ('import org.joml.Vector2f;\n',
         ''),
    ],
    # JEI 19's recipe types and slots.
    "common/" + JAVA + "/compat/jei/TrimCategory.java": [
        ('import mezz.jei.api.recipe.types.IRecipeType;',
         'import mezz.jei.api.recipe.RecipeType;'),
        ('public static final IRecipeType<',
         'public static final RecipeType<'),
        ('IRecipeType.create(',
         'new RecipeType<>('),
        ('    public IRecipeType<',
         '    public RecipeType<'),
        ('.add(trim.display());',
         '.addItemStack(trim.display());'),
    ],
}


def in_methods(text: str) -> str:
    """Applies METHOD_RENAMES inside each matching method (from its signature to its closing brace)."""
    for signature, renames in METHOD_RENAMES:
        out, at = [], 0
        for match in re.finditer(signature, text):
            depth, end = 0, match.end() - 1
            for end in range(match.end() - 1, len(text)):
                depth += {"{": 1, "}": -1}.get(text[end], 0)
                if depth == 0:
                    break
            body = text[match.start():end + 1]
            for pattern, replacement in renames:
                body = re.sub(pattern, replacement, body)
            out.append(text[at:match.start()] + body)
            at = end + 1
        text = "".join(out) + text[at:]
    return text


def add_import(text: str, name: str) -> str:
    """Imports this class, in order among the other imports, unless it's imported or in the file's own package."""
    line = "import " + name + ";\n"
    package = re.search(r"^package ([\w.]+);", text, flags=re.M)
    if line in text or (package and name.rsplit(".", 1)[0] == package.group(1)):
        return text
    imports = list(re.finditer(r"^import (?!static )([\w.]+);\n", text, flags=re.M))
    if not imports:
        return re.sub(r"(^package [\w.]+;\n\n)", lambda m: m.group(1) + line + "\n", text, count=1, flags=re.M)
    after = next((m for m in imports if m.group(1) > name), None)
    at = after.start() if after is not None else imports[-1].end()
    return text[:at] + line + text[at:]


def fix_imports(text: str) -> str:
    for old, new in COMPAT.items():
        line = "import " + old + ";\n"
        if line in text:
            text = add_import(text.replace(line, ""), new)
    for used, name in IMPORTS:
        body = re.sub(r"^import .*\n", "", text, flags=re.M)
        if re.search(used, body):
            text = add_import(text, name)
    for name in UNUSED:
        line = "import " + name + ";\n"
        simple = name.rsplit(".", 1)[1]
        if line in text and re.search(r"\b" + simple + r"\b", text.replace(line, "")) is None:
            text = text.replace(line, "")
    return text


# ── Resources ────────────────────────────────────────────────────────────────────────────────────────────────────────

# Materials whose blocks came after 1.21.1 (pale oak in 1.21.2, resin bricks in 1.21.4): their pedestals, cabinets and
# shelves are left out here, with their models, recipes, loot and names.
MISSING_MATERIALS = ["pale_oak", "resin_bricks"]


def missing_material(name: str) -> bool:
    return any(name.startswith(material + "_") for material in MISSING_MATERIALS)


def ingredient(value):
    """A recipe ingredient in 1.21.1's form: an item or tag object (a list of them for several)."""
    if isinstance(value, str):
        return {"tag": value[1:]} if value.startswith("#") else {"item": value}
    if isinstance(value, list):
        return [ingredient(item) for item in value]
    return value


def convert_resources():
    """1.21.1's resource formats, and the materials it lacks left out."""
    root = ROOT_FOR_RESOURCES
    placed_pedestal_models(root)
    for folder in [root / ASSETS, root / DATA]:
        for file in list(folder.rglob("*")):
            if file.is_file() and missing_material(file.name):
                file.unlink()
    # Their names, and their entries in tags.
    lang = root / ASSETS / "lang/en_us.json"
    if lang.exists():
        names = json.loads(lang.read_text(encoding="utf-8"))
        names = {key: value for key, value in names.items() if not any(f".{material}_" in key for material in MISSING_MATERIALS)}
        lang.write_text(json.dumps(names, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    # Each item with a 26.1 item definition is drawn by code (compat/render/ItemDefinitions reads the definition): its
    # item model is builtin/entity. An item model of the same name the definition uses is moved aside for it.
    models = root / ASSETS / "models/item"
    for definition in (root / ASSETS / "items").glob("*.json"):
        item = definition.stem
        own = models / (item + ".json")
        if own.exists():
            own.rename(models / (item + "_trimmed.json"))
            text = definition.read_text(encoding="utf-8").replace(f'"trophypedestals:item/{item}"', f'"trophypedestals:item/{item}_trimmed"')
            definition.write_text(text, encoding="utf-8", newline="\n")
        models.mkdir(parents=True, exist_ok=True)
        own.write_text(json.dumps({"parent": "minecraft:builtin/entity", "gui_light": "side"}, indent=2) + "\n", encoding="utf-8", newline="\n")
    # Recipes name an ingredient as {"item": id} or {"tag": id} (1.21.2's bare ids and "#tags" came later).
    for folder in [root / DATA, root / "testpack/data"]:
        for file in folder.glob("*/recipe/**/*.json"):
            recipe = json.loads(file.read_text(encoding="utf-8"))
            if "key" in recipe:
                recipe["key"] = {symbol: ingredient(value) for symbol, value in recipe["key"].items()}
            if "ingredients" in recipe:
                recipe["ingredients"] = [ingredient(value) for value in recipe["ingredients"]]
            file.write_text(json.dumps(recipe, indent=2) + "\n", encoding="utf-8", newline="\n")
    for tag in (root / DATA).glob("*/tags/**/*.json"):
        data = json.loads(tag.read_text(encoding="utf-8"))
        values = data.get("values", [])
        kept = [value for value in values if not (isinstance(value, str) and missing_material(value.split(":")[-1]))]
        if kept != values:
            data["values"] = kept
            tag.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


# A placed pedestal's see-through overlays (its flutes and well imprint) are drawn by its renderer here
# (compat/render/PedestalOverlays), as 26.1 draws only those faces translucent: its blockstate's model is a copy without
# them (*_placed), drawn cutout.
OVERLAY_TEXTURES = {"#column_flutes", "#well_imprint"}


def placed_pedestal_models(root: Path):
    block_models = root / ASSETS / "models/block"
    for template in list(block_models.glob("pedestal_template*.json")):
        model = json.loads(template.read_text(encoding="utf-8"))
        elements = []
        for element in model.get("elements", []):
            faces = {side: face for side, face in element["faces"].items() if face["texture"] not in OVERLAY_TEXTURES}
            if faces:
                elements.append({**element, "faces": faces})
        model["elements"] = elements
        (block_models / (template.stem + "_placed.json")).write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8", newline="\n")
    for state in (root / ASSETS / "blockstates").glob("*_pedestal.json"):
        name = state.stem
        own = block_models / (name + ".json")
        if not own.exists():
            continue
        model = json.loads(own.read_text(encoding="utf-8"))
        if not model.get("parent", "").startswith("trophypedestals:block/pedestal_template"):
            continue
        model["parent"] += "_placed"
        (block_models / (name + "_placed.json")).write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8", newline="\n")
        text = state.read_text(encoding="utf-8").replace(f'"trophypedestals:block/{name}"', f'"trophypedestals:block/{name}_placed"')
        state.write_text(text, encoding="utf-8", newline="\n")


# ── The port ─────────────────────────────────────────────────────────────────────────────────────────────────────────

def renamed(relative: str, text: str) -> str:
    """A source file's text after steps 1 (renames and edits)."""
    if relative.endswith(".java"):
        for pattern, replacement in RENAMES:
            text = re.sub(pattern, replacement, text)
        text = in_methods(text)
        text = block_entity_render(text)
        text = model_shape(text)
    for original, replacement in EDITS.get(relative, []):
        if original not in text:
            sys.exit(f"{relative}: expected text not found, port it by hand:\n{original}")
        text = text.replace(original, replacement)
    if relative.endswith(".java"):
        text = fix_imports(text)
    return text


def hunks(diff: str):
    """The (old, new) text of each hunk of a unified diff."""
    old, new, inside = [], [], False
    for line in diff.splitlines(keepends=True):
        if line.startswith("@@"):
            if inside:
                yield "".join(old), "".join(new)
            old, new, inside = [], [], True
        elif not inside or line.startswith(("---", "+++")):
            continue
        elif line.startswith("\\"):
            # "\ No newline at end of file": the line before it has none.
            target = old if last == "-" else new if last == "+" else None
            for lines in ([old, new] if last == " " else [target]):
                lines[-1] = lines[-1].rstrip("\n")
        else:
            last = line[0]
            if line[0] in " -":
                old.append(line[1:])
            if line[0] in " +":
                new.append(line[1:])
    if inside:
        yield "".join(old), "".join(new)


def patch(relative: str, text: str) -> str:
    """Step 2: the file's patch applied, each hunk matching exactly once."""
    file = PATCHES / (relative + ".diff")
    if not file.exists():
        return text
    for number, (old, new) in enumerate(hunks(file.read_text(encoding="utf-8")), 1):
        found = text.count(old)
        if found != 1:
            sys.exit(f"{relative}: hunk {number} of its patch matches {found} times, port it by hand:\n{old}")
        text = text.replace(old, new, 1)
    return text


def make_patch(relative: str, before: str, after: str) -> str:
    """A diff from before to after whose every hunk matches before once (more context until it does)."""
    for context in range(3, 40):
        diff = "".join(difflib.unified_diff(before.splitlines(keepends=True), after.splitlines(keepends=True),
                                            "a/" + relative, "b/" + relative, n=context))
        if all(before.count(old) == 1 for old, _ in hunks(diff)):
            if patch_text(before, diff) == after:
                return diff
    sys.exit(f"{relative}: can't make a patch whose hunks match once; add it to KEEP")


def patch_text(text: str, diff: str) -> str:
    for old, new in hunks(diff):
        text = text.replace(old, new, 1)
    return text


def sources():
    """Every ported file: its path relative to ROOT, and its file in SOURCE."""
    for module in MODULES:
        for file in sorted((SOURCE / module / "src").rglob("*")):
            if file.is_file():
                relative = file.relative_to(SOURCE).as_posix()
                if not any(relative == skip or relative.startswith(skip + "/") for skip in SKIP):
                    yield relative, file


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def is_text(relative: str) -> bool:
    return relative.endswith((".java", ".json", ".toml", ".txt", ".mcmeta", ".md", ".properties", ".accesswidener"))


def expected_tree(copied: bool = False) -> dict[str, bytes]:
    """What steps 1 and 3 make of the sources (no patches), as relative path -> bytes, built in a scratch copy; with
    copied, the COPIED files and folders too."""
    scratch = ROOT / "build/port-scratch"
    if scratch.exists():
        shutil.rmtree(scratch)
    tree: dict[str, bytes] = {}
    for relative, file in sources():
        target = scratch / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        if is_text(relative):
            target.write_text(renamed(relative, file.read_text(encoding="utf-8")), encoding="utf-8", newline="\n")
        else:
            shutil.copyfile(file, target)
    if copied:
        for name in COPIED:
            source = SOURCE / name
            if source.is_dir():
                shutil.copytree(source, scratch / name)
            else:
                shutil.copy2(source, scratch / name)
    global ROOT_FOR_RESOURCES
    ROOT_FOR_RESOURCES = scratch
    convert_resources()
    ROOT_FOR_RESOURCES = ROOT
    for file in scratch.rglob("*"):
        if file.is_file():
            tree[file.relative_to(scratch).as_posix()] = file.read_bytes()
    shutil.rmtree(scratch)
    return tree


ROOT_FOR_RESOURCES = ROOT


def module_files() -> set[str]:
    return {file.relative_to(ROOT).as_posix() for module in MODULES for file in (ROOT / module / "src").rglob("*") if file.is_file()}


def keep(relative: str):
    """Copies a hand-written file of the project into tools/keep/."""
    target = KEEP_ROOT / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ROOT / relative, target)
    KEEP.add(relative)


def record():
    tree = expected_tree()
    current = module_files()
    written = 0
    # Hand-written files: those changed here since the last port go back to tools/keep/, and new ones (in no 26.1.2
    # file's place) join them. (One changed in tools/keep/ itself is left for the next port to bring in.)
    saved = json.loads(MANIFEST.read_text(encoding="utf-8")) if MANIFEST.exists() else {}
    for relative in sorted(current):
        if relative in KEEP:
            if saved.get(relative) is not None and saved[relative] != digest((ROOT / relative).read_bytes()):
                keep(relative)
        elif relative not in tree:
            print(f"Written by hand for this version (kept in tools/keep): {relative}")
            keep(relative)
    if PATCHES.exists():
        for old in PATCHES.rglob("*.diff"):
            old.unlink()
    for relative in sorted(current - KEEP):
        now = (ROOT / relative).read_bytes()
        if now == tree[relative]:
            continue
        if not is_text(relative):
            sys.exit(f"{relative} differs from 26.1.2's and isn't text: add it to KEEP")
        diff = make_patch(relative, tree[relative].decode("utf-8"), now.decode("utf-8"))
        out = PATCHES / (relative + ".diff")
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(diff, encoding="utf-8", newline="\n")
        written += 1
    missing = sorted(set(tree) - current - KEEP)
    if missing:
        print("Left out here (add them to SKIP, or they come back on the next port):\n  " + "\n  ".join(missing))
    save_manifest()
    print(f"Recorded {written} patches")


def save_manifest():
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps({path: digest((ROOT / path).read_bytes()) for path in sorted(module_files())}, indent=0),
                        encoding="utf-8", newline="\n")


def check_unrecorded():
    if FORCE or not MANIFEST.exists():
        return
    saved = json.loads(MANIFEST.read_text(encoding="utf-8"))
    files = module_files()
    changed = [path for path in sorted(files) if path in saved and saved[path] != digest((ROOT / path).read_bytes())]
    changed += [path for path in sorted(files - KEEP) if path not in saved]
    if changed:
        sys.exit("Changed since the last port or recording (run with --record to keep them, or --force to drop them):\n  "
                 + "\n  ".join(changed[:30]))


def clear(folder: Path):
    """Deletes every file under a folder, then the folders left empty (one in use, as a shell's, stays)."""
    if not folder.exists():
        return
    for file in folder.rglob("*"):
        if file.is_file():
            file.unlink()
    for directory in sorted((d for d in folder.rglob("*") if d.is_dir()), key=lambda d: len(d.parts), reverse=True):
        try:
            directory.rmdir()
        except OSError:
            pass


def port():
    check_unrecorded()
    # Everything is made first (renamed, converted, patched, and the hand-written files), so a patch that no longer
    # applies stops the port before anything is changed.
    tree = expected_tree(copied=True)
    for relative in sorted(tree):
        if relative not in KEEP and (PATCHES / (relative + ".diff")).exists():
            tree[relative] = patch(relative, tree[relative].decode("utf-8")).encode("utf-8")
    for relative in KEEP:
        tree[relative] = (KEEP_ROOT / relative).read_bytes()
    for folder in [module + "/src" for module in MODULES] + [name for name in COPIED if (SOURCE / name).is_dir()]:
        clear(ROOT / folder)
    for relative, content in tree.items():
        (ROOT / relative).parent.mkdir(parents=True, exist_ok=True)
        (ROOT / relative).write_bytes(content)
    save_manifest()
    print(f"Ported from {SOURCE}")


if __name__ == "__main__":
    record() if RECORD else port()
