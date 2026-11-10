package net.ixdarklord.trophypedestals.client;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ixdarklord.coolcatcore.api.client.registry.BlockEntityRendererRegistry;
import net.ixdarklord.coolcatcore.api.client.registry.TooltipComponentRegistry;
import net.ixdarklord.coolcatcore.api.event.v2.client.ItemTooltipEvents;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.api.TrophyKinds;
import net.ixdarklord.trophypedestals.api.client.PedestalEffect;
import net.ixdarklord.trophypedestals.api.client.PedestalEffects;
import net.ixdarklord.trophypedestals.pedestal.MobLooks;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlock;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.PedestalRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.ixdarklord.trophypedestals.pedestal.Statues;
import net.ixdarklord.trophypedestals.pedestal.TrophyTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A trophy pedestal in the world: its icon turning and bobbing above the cap, lit full bright so it glows like
 * a hologram, with everything else coming from its rank ({@link PedestalRarity}): outlines of light rising out of the
 * glowing well in its cap and fading away by the icon's middle, as a cartoon hologram's scan lines do; a turning flare
 * behind the icon (two, at the higher ranks); a beam of light; and the particles ({@link PedestalParticles}). Over it,
 * floating as name tags do, its rank, what it records in its color, who achieved it and when, while the player looks at
 * it (or always, if it's set to). An empty pedestal draws only its model, which the chunk draws; one showing a player's
 * item is a showcase, turning it slowly in a soft cone of warm light over the well, with its name over it if it was
 * named. A trophy's item tooltip says what it shows.
 * <p>
 * Light on the frame: all of a pedestal's additive light (the well's glow, the outlines and the beam) goes in one batch
 * and both flares in another; the outlines and flares stop past {@link #DETAIL_RANGE} blocks (farther for a display shown
 * larger or higher), leaving the icon and the
 * well (and a legendary beam, which is seen from far); nothing is allocated for what a rank doesn't have.
 */
public final class PedestalRenderer implements BlockEntityRenderer<PedestalBlockEntity, PedestalRenderer.State> {
    // The text panel: the height its bottom floats at, its scale (a name tag's), its lines' height, its padding inside
    // the frame, and its frame's border (a third of the 48 pixel frame texture, drawn at half size).
    // (Over a mob or a large item it floats a little above its top instead, if that's higher.)
    private static final float PANEL_BOTTOM = 1.95F, PANEL_OVER_MOB = 0.15F, PANEL_SCALE = 0.02F;
    private static final int LINE_HEIGHT = 10, PANEL_PAD = 8, PANEL_BORDER = 8;
    // The crest over an epic or legendary frame's top edge: 24 by 12 pixels, drawn at half size.
    private static final int CREST_WIDTH = 12, CREST_HEIGHT = 6;
    // Where a crest's foot is, below the panel's top edge: the legendary crown stands on the frame's band (its outer line
    // 1.5 units in), and the epic gem is set on it, its middle on the band's (2.75 in), half over the frame.
    private static final float CREST_FOOT = 2.0F, GEM_FOOT = 8.25F, PENDANT_TUCK = 6.5F;
    // The render passes panels go in: after the pedestal's light (pass 0), one pair of passes per 1/16 block of distance
    // within the range, the farthest first.
    private static final int PANEL_ORDER = 1, PANEL_ORDER_RANGE = 64;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int ALWAYS_REACH = 32;
    // How near the camera must be for the outlines and flares (the beam shows as far as the pedestal does).
    private static final double DETAIL_RANGE = 40.0;
    // The space the floating item takes over the block, which counts as looking at the pedestal: its inset from the
    // block's sides and its top above the block's floor.
    private static final double ITEM_SIDE = 0.2, ITEM_TOP = 1.75;
    // A pedestal showing a player's item is a showcase, not a trophy: a warm museum light, the item turning slowly in a
    // soft cone of light out of the well over a gently pulsing frame, and none of a trophy's outlines, flare or sparks.
    private static final PedestalRarity DISPLAY = PedestalRarity.builder(TrophyPedestalsMod.id("display"), 0, 0xFFE6BE, 0xFFF6E6).build();
    // The crown: its radius, its shards' half height and half width, and how fast it turns and its glow runs round
    // (radians a tick).
    private static final float CROWN_RADIUS = 0.62F, CROWN_HALF_HEIGHT = 0.15F, CROWN_HALF_WIDTH = 0.04F, CROWN_SPIN = 0.012F, CROWN_GLOW = 0.1F;
    // The halos: their half width (their radius is the rank's), how far they lean (degrees), how fast they turn and their arcs sweep
    // (degrees and radians a tick), how many pieces each is drawn in, and their brightest.
    private static final float HALO_WIDTH = 0.018F, HALO_TILT = 24.0F, HALO_SPIN = 1.2F, HALO_SWEEP = 0.09F;
    private static final int HALO_SEGMENTS = 28, HALO_ALPHA = 220;
    // A showcase's light rising or sinking: how long it takes (ticks), and how far below its place its item starts.
    private static final float RISE_TICKS = 12.0F, RISE_DROP = 0.35F;
    // The showcase's cone: its half width at the well and at its top, and its height.
    private static final float CONE_FOOT = 0.21F, CONE_TOP = 0.36F, CONE_HEIGHT = 0.8F;
    // A trophy's cone, a share of a showcase's height: lower, under its own light.
    private static final float TROPHY_CONE = 0.65F;
    // The pass a pedestal item's showcase light is drawn in: after the item's model.
    private static final int ITEM_LIGHT_ORDER = 1;
    // A plain white sprite in the item atlas, which an item's light is drawn with.
    private static final Identifier ITEM_LIGHT_SPRITE = TrophyPedestalsMod.id("item/pedestal_light");
    // Its frame's half width: a little inside the well's walls (half 0.25).
    private static final float FRAME_HALF = 0.235F;
    // The well's lens lit up: its bezel and glass in shades of white to take the rank's color, the rest cut away.
    private static final Identifier WELL_GLOW = TrophyPedestalsMod.id("textures/block/pedestal/well_glow.png");
    // The glow over it: out to a little past the lens's bezel (6 of the well's 16 pixels, each 1/32 of a block), in
    // a fan of this many slices.
    private static final float LENS_GLOW_RADIUS = 0.22F;
    private static final int LENS_GLOW_SEGMENTS = 16;
    // The lens's breathing: one breath in this many ticks, the glass dimming to this share of full at its lowest and
    // its glow to this.
    private static final float BREATH_TICKS = 60.0F, BREATH_DIM = 0.72F, GLOW_DIM = 0.55F;
    // The flare around the icon: the cinematic's rays with their middle faded out (the icon, edge on, wouldn't hide it),
    // unlit so it keeps its color. It faces the camera centered behind the icon's middle, bobbing and turning with it;
    // the gap keeps it just clear of the icon's turning edges.
    private static final Identifier FLARE = TrophyPedestalsMod.id("textures/vfx/pedestal_flare.png");
    private static final float ICON_Y = 1.32F, ICON_SCALE = 1.2F, FLARE_GAP = 0.03F;
    // A mob trophy's mob: where its feet float (a little over the cap), and the most room it takes before it's shrunk to fit:
    // how far out from its middle it may reach as it turns (inside the hologram's outlines and its light), and how tall.
    private static final float MOB_FLOOR = 1.17F, MOB_REACH = 0.3F, MOB_HEIGHT = 0.85F;
    // How far a mob whose model can't be measured (one drawn without a model of parts, as the ender dragon is) is taken
    // to reach, against its hitbox's width: half of it, as its wings spread about that far, then halved again so it may
    // take twice the usual room (its wingtips over the pedestal's edges). Taking its hitbox's corner drew the dragon tiny.
    private static final float UNMEASURED_REACH = 0.25F;
    // The lowest a large item's bottom may be: just over the cap.
    private static final float ICON_FLOOR = 1.0F;
    // The rising outlines rise out of the well in the cap (its floor 14/16 up, inside the rim) and are gone by the icon's
    // middle (1.32 above the block); as wide as the well. Their line's thickness.
    private static final float RING_FROM = 0.875F, RING_TO = 1.34F;
    private static final float RING_HALF = 0.25F, RING_LINE = 0.022F;
    // An item's outlines stand inside its cone of light, this far in from its faces: drawn in the item sheet, which writes
    // depth, an outline crossing the cone's faces had pieces of each hiding the other.
    private static final float ITEM_RING_INSET = 0.04F;
    // The lens's glass, as the pedestal's model has it (its own voxels, tools/generate_pedestals.py's LENS_GLASS): each
    // row of the well's 16 pixels across, {row, first, last}, its top this high over the block's bottom; the glow is drawn
    // on exactly those faces, a hair over them.
    private static final int[][] LENS_ROWS = {{3, 6, 9}, {4, 5, 10}, {5, 4, 11}, {6, 3, 12}, {7, 3, 12}, {8, 3, 12}, {9, 3, 12},
            {10, 4, 11}, {11, 5, 10}, {12, 6, 9}};
    private static final float GLASS_TOP = 14.25F / 16.0F + 0.002F;
    // How much of the rise the lit glass takes to fade fully in (and, turning off, out).
    private static final float LENS_FADE = 0.6F;
    // The beam: its haze's, glow's and core's half widths; the height from which it stands behind the icon (just under
    // it); its bands of light: how many, how long each takes to run up it (ticks), their half height and half width.
    private static final float BEAM_HAZE = 0.36F, BEAM_HALF = 0.17F, BEAM_CORE = 0.05F, BEAM_BEHIND_FROM = 1.02F;
    private static final int BEAM_BANDS = 2;
    private static final float BEAM_BAND_TICKS = 40.0F, BEAM_BAND_HALF = 0.22F, BEAM_BAND_WIDTH = 0.09F;

    // Each of Trophy Pedestals's ranks' text frames, and the crests over the grandest.
    private static final Map<Identifier, Identifier> FRAMES = Map.of(
            PedestalRarities.COMMON.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/common.png"),
            PedestalRarities.UNCOMMON.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/uncommon.png"),
            PedestalRarities.RARE.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/rare.png"),
            PedestalRarities.EPIC.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/epic.png"),
            PedestalRarities.LEGENDARY.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/legendary.png"));
    private static final Map<Identifier, Identifier> CRESTS = Map.of(
            PedestalRarities.EPIC.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/epic_crest.png"),
            PedestalRarities.LEGENDARY.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/legendary_crown.png"));
    // The crown worn over the grandest trophies' display: a square crown of voxels, the frame's crown art on each of its
    // four sides built out of little blocks, floating just over the top of what it shows and turning with it.
    private static final Map<Identifier, Identifier> TOPPERS = Map.of(
            PedestalRarities.LEGENDARY.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/legendary_crown.png"));
    // Half its width, and where its base sits from the display's top (right on it, worn as a crown is), in blocks at the
    // usual display size.
    private static final float TOPPER_HALF = 0.11F, TOPPER_GAP = 0.0F;
    // Where the text floats over a trophy wearing a crown, from the crown's own top (a little under it: the panel's frame
    // has room of its own below the text).
    private static final float TOPPER_CLEARANCE = 0.03F;
    // Which of each crown art's pixels are solid, read once from its texture (null for one that can't be read).
    private static final Map<Identifier, boolean[][]> TOPPER_PIXELS = new ConcurrentHashMap<>();
    // The voxels' shading, as unshaded drawing has none of its own: their outer faces, inner faces, tops, sides and undersides.
    private static final float SHADE_OUTER = 0.92F, SHADE_INNER = 0.62F, SHADE_TOP = 1.0F, SHADE_SIDE = 0.76F, SHADE_UNDER = 0.5F;
    // The ornaments hanging from the grandest frames' bottom edge, tucked a little behind it.
    private static final Map<Identifier, Identifier> PENDANTS = Map.of(
            PedestalRarities.LEGENDARY.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/legendary_crest.png"));
    // How far behind its text the panel sits, in its own units.
    private static final float PANEL_DEPTH = -0.05F;

    // How far off a custom pedestal's body is drawn (as far as most players see blocks), and the rest of a pedestal (as
    // far as block entities usually are).
    private static final int BODY_DISTANCE = 256, DISPLAY_DISTANCE = 64;

    // Mobs that couldn't be made or drawn outside a world (some mods'), shown by their icon instead.
    private static final Set<EntityType<?>> UNDRAWABLE = ConcurrentHashMap.newKeySet();
    // Each kind of mob's model at rest: how far it reaches out from its middle as it turns, and how tall it is, in blocks.
    // Measured once, since a long mob (a sheep, a horse) or a leggy one (a spider) reaches well past its hitbox.
    private static final Map<EntityType<?>, float[]> MODEL_SIZES = new ConcurrentHashMap<>();
    // A model whose hitbox is between these times its height, or its width, is taken as drawn at its own size (a ghast,
    // whose tentacles hang far under its hitbox but whose body is as wide as it); one off in both (a giant, a baby, a
    // large slime), as scaled to its hitbox's height.
    private static final float OWN_SIZE_LOW = 0.7F, OWN_SIZE_HIGH = 1.6F;
    // The size the game draws a player-shaped figure (a player, a mannequin) at, against its model.
    private static final float AVATAR_SCALE = 0.9375F;
    // How far a statue lying along the ground is raised off the pedestal, in its own units (a chest-down glider's or a
    // sleeper's half thickness).
    private static final float LIE_LIFT = 0.3F;

    private final ItemModelResolver items;

    // (Also made by the trophy displays, to draw the trophies on them as on their own pedestals.)
    PedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
    }

    public static void init() {
        PedestalLight.init(FLARE);
        BlockEntityRendererRegistry.register(Pedestals.BLOCK_ENTITY, PedestalRenderer::new);
        BlockEntityRendererRegistry.register(Pedestals.CABINET_ENTITY, TrophyCabinetRenderer::new);
        TooltipComponentRegistry.register(TrophyTooltip.class, TrophyTooltipLine::new);
        ItemTooltipEvents.APPEND.register((stack, context, player, flag, lines) -> {
            // (Its rank is a badge after its name: see TrophyTooltipLine.)
            PedestalRecord record = stack.get(Pedestals.RECORD.get());
            if (record != null) {
                lines.add(record.unlockedBy().copy().withStyle(ChatFormatting.GRAY));
                lines.add(record.date().copy().withStyle(ChatFormatting.DARK_GRAY));
            }
            MobTrophyRecord mob = stack.get(Pedestals.MOB_TROPHY.get());
            if (mob != null && mob.slain()) {
                lines.add(mob.slainLine().copy().withStyle(ChatFormatting.GRAY));
                lines.add(mob.date().copy().withStyle(ChatFormatting.DARK_GRAY));
            }
        });
    }

    /**
     * Whether the player is looking at this block within their reach (the crosshair's target), or at the item floating
     * over it, unless something nearer is in the way.
     */
    static boolean lookedAt(BlockPos pos, float scale, float lift) {
        Minecraft minecraft = Minecraft.getInstance();
        HitResult target = minecraft.hitResult;
        if (target instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) return true;
        Entity camera = minecraft.getCameraEntity();
        if (camera == null || minecraft.player == null) return false;
        Vec3 eye = camera.getEyePosition();
        double reach = target == null || target.getType() == HitResult.Type.MISS ? minecraft.player.blockInteractionRange() : eye.distanceTo(target.getLocation());
        double side = 0.5 - (0.5 - ITEM_SIDE) * scale, height = (ITEM_TOP - 1.0) * scale;
        AABB item = new AABB(pos.getX() + side, pos.getY() + 1.0, pos.getZ() + side, pos.getX() + 1.0 - side, pos.getY() + 1.0 + Math.max(0.0, lift) + height, pos.getZ() + 1.0 - side);
        return item.clip(eye, eye.add(camera.getViewVector(1.0F).scale(reach))).isPresent();
    }

    // A mob trophy's mob, made once for the pedestal and drawn full bright, without a shadow or name tag, shrunk to fit
    // over it (never grown); null if it can't be (a removed mod's mob, or a mod's that won't make or draw outside a
    // world, which is remembered and logged once), when its icon shows instead.
    // (A player's statue is one too: a mannequin in their skin.)
    private static @Nullable EntityRenderState extractMob(PedestalBlockEntity pedestal, @Nullable EntityType<?> type, Supplier<CompoundTag> look,
                                                          State state, float partialTicks, float time) {
        if (type == null || UNDRAWABLE.contains(type)) return null;
        try {
            return makeMob(pedestal, type, look, state, partialTicks, time);
        } catch (RuntimeException | LinkageError e) {
            UNDRAWABLE.add(type);
            pedestal.setShownMob(null);
            TrophyPedestalsMod.LOGGER.warn("Can't draw {} on a trophy pedestal; its icon shows instead", BuiltInRegistries.ENTITY_TYPE.getKey(type), e);
            return null;
        }
    }

    private static @Nullable EntityRenderState makeMob(PedestalBlockEntity pedestal, EntityType<?> type, Supplier<CompoundTag> look,
                                                       State state, float partialTicks, float time) {
        Entity mob = pedestal.shownMob();
        if (mob == null || mob.getType() != type) {
            mob = type.create(pedestal.getLevel(), EntitySpawnReason.LOAD);
            if (mob == null) return null;
            DrawnMobs.numbered(mob);
            MobLooks.apply(mob, look.get(), pedestal.getLevel().registryAccess());
            mob.setPos(Vec3.atBottomCenterOf(pedestal.getBlockPos()).add(0.0, MOB_FLOOR, 0.0));
            mob.setYRot(0.0F);
            if (mob instanceof LivingEntity living) {
                living.setYBodyRot(0.0F);
                living.setYHeadRot(0.0F);
            }
            pedestal.setShownMob(mob);
        }
        // (A mannequin's armor stand takes its pose as it's set, as the adjust screen changes it; under glass it stands as
        // an armor stand does, its pose kept for when the glass comes off.)
        if (mob instanceof ArmorStand stand && pedestal.mannequin()) pose(stand, pedestal.glass() ? PedestalBlockEntity.DEFAULT_STAND_POSE : pedestal.standPose());
        // (A mod's mob may say it has no size: taken as a small one.)
        float tall = Math.max(0.1F, mob.getBbHeight());
        state.mobReach = Math.max(0.05F, reach(mob, tall));
        state.mobLift = lift(mob);
        // Its size: shrunk to fit over the pedestal, then the display size (its usual size, under glass).
        state.mobScale = Math.min(1.0F, Math.min(MOB_REACH / state.mobReach, MOB_HEIGHT / tall)) * (pedestal.glass() ? 1.0F : pedestal.scale());
        // (Its whole height, from what hangs under its feet to its top, for it to fit inside its light or its case.)
        float drawnTall = state.mobLift + drawnHeight(mob, tall);
        // (Under glass: fitted inside the case once it's posed, below.)
        if (!pedestal.glass()) {
            // (A mob or statue, on a trophy or a leaderboard, stands as on a trophy: raised only as far as its light's
            // trophy cone needs, and only once it's grown past its usual size; at that size or smaller it's kept narrow
            // enough already, and stands on the cap. Everything over the mob follows the lift.)
            float[] fitted = fitToCone(state, true, state.mobScale, state.mobReach, drawnTall / 2.0F, MOB_FLOOR);
            state.mobScale = fitted[0];
            if (pedestal.scale() > 1.0F) state.lift += fitted[1] - MOB_FLOOR;
        }
        EntityRenderState shown = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(mob, partialTicks);
        shown.lightCoords = FULL_BRIGHT;
        shown.shadowRadius = 0.0F;
        shown.shadowPieces.clear();
        shown.nameTag = null;
        // One that doesn't animate holds the pose it has at its first moment.
        shown.ageInTicks = pedestal.animates() ? time : 0.0F;
        idle(shown, shown.ageInTicks);
        if (pedestal.showsStatue()) pose(shown, pedestal.statuePose());
        if (shown instanceof SquidRenderState squid) tumble(squid, pedestal.getBlockPos(), shown.ageInTicks);
        // Playing its chosen animation on loop (a frozen one holds still whichever is chosen).
        if (pedestal.animates()) MobAnimations.play(mob, shown, pedestal.animation(), time);
        if (pedestal.glass()) {
            // Under glass: fitted inside the case, standing where it does, by the most it has reached as it's posed and
            // animated (its limbs, and anything in its hands: a mannequin's raised sword stays inside too), raised or
            // lowered as the game draws it. It stands still there, facing its yaw, so it's fitted by its footprint that
            // way round (the case is square); one tipped (by its pitch or roll, or a squid's tumble) is fitted as a ball
            // round its middle, so no way it's turned takes it out of the glass.
            boolean tipped = pedestal.pitch() != 0.0F || pedestal.roll() != 0.0F || shown instanceof SquidRenderState;
            // (A mannequin, standing as an armor stand does under glass, is sized by its figure alone: what it holds isn't
            // shrunk for.)
            float[] posed = pedestal.mannequin() ? null : posedSize(mob, shown, pedestal.yaw() + modelTurn(shown));
            float reach = state.mobReach, high = drawnTall;
            if (posed != null) {
                reach = tipped ? Math.max(reach, posed[0]) : posed[1];
                high = Math.max(high, state.mobLift + posed[2]);
            }
            float[] most = pedestal.fitGlass(reach, Math.max(0.1F, high - sink(shown)));
            // (As large as the case lets it be, up to its own size: the case is its whole room.)
            float fit = fitInCase(1.0F, most[0], most[1], GLASS_FLOOR, tipped);
            // Its display size makes it smaller, never larger than that.
            state.mobScale = fit * Math.min(1.0F, pedestal.scale());
            // Centred in the case, top to bottom.
            state.lift = caseMiddle(most[1] * state.mobScale) - MOB_FLOOR;
        }
        return shown;
    }

    // A reused pose stack and corner for measuring a posed mob, and the turn it's measured at (on the render thread only).
    private static final PoseStack POSED_POSE = new PoseStack();
    private static final Vector3f POSED_CORNER = new Vector3f();
    private static float posedCos = 1.0F, posedSin = 0.0F;

    /**
     * How a mob is drawn now, posed and animated, in its own blocks: how far it reaches out from its upright axis, how
     * far out it reaches along the case's sides turned {@code yaw} degrees,
     * and how high its top is over its feet ({reach, footprint, height}); measured from its model as set up for this frame
     * and anything in its hands (held as the game holds it). Null for one that can't be measured that way.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static float @Nullable [] posedSize(Entity mob, EntityRenderState shown, float yaw) {
        if (!(shown instanceof LivingEntityRenderState living)
                || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(mob) instanceof LivingEntityRenderer<?, ?, ?> renderer)) return null;
        float[] rest = MODEL_SIZES.get(mob.getType());
        if (rest == null) return null;
        try {
            EntityModel model = renderer.getModel();
            model.setupAnim(living);
            posedCos = Mth.cos(yaw * Mth.DEG_TO_RAD);
            posedSin = Mth.sin(yaw * Mth.DEG_TO_RAD);
            // {reach, its lowest point, its top, its footprint}: upside down, as its model is, its feet at MODEL_FEET.
            float[] bounds = {0.0F, -Float.MAX_VALUE, Float.MAX_VALUE, 0.0F};
            model.root().visit(POSED_POSE, (pose, path, index, cube) -> {
                for (int i = 0; i < 8; i++) {
                    POSED_CORNER.set((i & 1) == 0 ? cube.minX : cube.maxX, (i & 2) == 0 ? cube.minY : cube.maxY, (i & 4) == 0 ? cube.minZ : cube.maxZ)
                            .div(16.0F).mulPosition(pose.pose());
                    include(bounds, POSED_CORNER);
                }
            });
            if (model instanceof ArmedModel armed && living instanceof ArmedEntityRenderState arms) {
                for (HumanoidArm arm : HumanoidArm.values()) {
                    ItemStackRenderState item = arm == HumanoidArm.RIGHT ? arms.rightHandItemState : arms.leftHandItemState;
                    if (item.isEmpty()) continue;
                    POSED_POSE.pushPose();
                    // (As the game's held item layer places it in the hand.)
                    armed.translateToHand(arms, arm, POSED_POSE);
                    POSED_POSE.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    POSED_POSE.mulPose(Axis.YP.rotationDegrees(180.0F));
                    POSED_POSE.translate((arm == HumanoidArm.LEFT ? -1.0F : 1.0F) / 16.0F, 2.0F / 16.0F, -10.0F / 16.0F);
                    // (By its model's own points, not its box: a sword drawn along its box's diagonal reaches only as far as
                    // its tip.)
                    Matrix4f hand = POSED_POSE.last().pose();
                    item.visitExtents(point -> include(bounds, POSED_CORNER.set(point).mulPosition(hand)));
                    POSED_POSE.popPose();
                }
            }
            if (bounds[2] == Float.MAX_VALUE) return null;
            float scale = drawScale(mob, rest) * (mob instanceof Avatar ? AVATAR_SCALE : 1.0F);
            // (Armour sits a little outside the model: a pixel's allowance all round.)
            return new float[] {(bounds[0] + ARMOR_ALLOWANCE) * scale, (bounds[3] + ARMOR_ALLOWANCE) * scale, (MODEL_FEET - bounds[2] + ARMOR_ALLOWANCE) * scale};
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static final float ARMOR_ALLOWANCE = 1.0F / 16.0F;

    private static void include(float[] bounds, Vector3f corner) {
        bounds[0] = Math.max(bounds[0], (float) Math.hypot(corner.x, corner.z));
        // (Turned as the game draws its model: about its upright axis, its z mirrored.)
        float x = corner.x, z = corner.z, c = posedCos, s = posedSin;
        bounds[3] = Math.max(bounds[3], Math.max(Math.abs(x * c - z * s), Math.abs(x * s + z * c)));
        bounds[1] = Math.max(bounds[1], corner.y);
        bounds[2] = Math.min(bounds[2], corner.y);
    }

    // A squid tipped and turned its own way on each pedestal (as one swims, never quite upright), swaying gently while it
    // moves. Picked from the pedestal's place, so it keeps its tilt.
    private static void tumble(SquidRenderState squid, BlockPos pos, float age) {
        long seed = pos.asLong() * 0x9E3779B97F4A7C15L;
        float tip = ((seed >>> 40 & 0xFFFF) / 65535.0F * 2.0F - 1.0F) * SQUID_TIP;
        float turn = (seed >>> 20 & 0xFFFF) / 65535.0F * 360.0F;
        squid.xBodyRot = tip + Mth.sin(age / 25.0F) * SQUID_SWAY;
        squid.zBodyRot = turn;
    }

    // How far a squid on a pedestal is tipped at most, either way, and how far it sways while moving, in degrees.
    private static final float SQUID_TIP = 40.0F, SQUID_SWAY = 6.0F;

    // How much of a statue's standing height its pose leaves on top: a crouch is lower, a lying figure barely above the
    // pedestal.
    private static float statueTop(PedestalBlockEntity pedestal) {
        if (!pedestal.showsStatue()) return 1.0F;
        return switch (pedestal.statuePose()) {
            case STANDING -> 1.0F;
            case CROUCHING -> 0.83F;
            default -> 0.35F;
        };
    }

    // A statue's pose, set on what's drawn (it never ticks, so a swimmer's lean and a glider's flight are set outright).
    private static void pose(EntityRenderState shown, PedestalBlockEntity.StatuePose pose) {
        if (!(shown instanceof LivingEntityRenderState living)) return;
        living.pose = pose.pose();
        if (pose == PedestalBlockEntity.StatuePose.SLEEPING) living.bedOrientation = Direction.SOUTH;
        if (shown instanceof HumanoidRenderState humanoid) {
            // (The renderer bends a crouch by its own flag, not by the pose.)
            humanoid.isCrouching = pose == PedestalBlockEntity.StatuePose.CROUCHING;
            humanoid.isVisuallySwimming = pose == PedestalBlockEntity.StatuePose.SWIMMING;
            humanoid.swimAmount = humanoid.isVisuallySwimming ? 1.0F : 0.0F;
            humanoid.isFallFlying = pose == PedestalBlockEntity.StatuePose.GLIDING;
            if (humanoid.isFallFlying && shown instanceof AvatarRenderState avatar) avatar.fallFlyingTimeInTicks = 20.0F;
        }
    }

    // How far a mob reaches out from its middle as it turns: its model's farthest corner from the upright axis, scaled
    // to its hitbox if it's drawn larger or smaller than its own size; for a mob drawn without a model of parts (or one
    // that can't be measured), from its hitbox.
    /**
     * How much a mob is shrunk to fit over a pedestal at the usual display size (never grown), and where its feet float
     * over the block's bottom: for a mob trophy item, drawn as the placed trophy is.
     */
    static float fitScale(Entity mob) {
        float tall = Math.max(0.1F, mob.getBbHeight());
        return Math.min(1.0F, Math.min(MOB_REACH / Math.max(0.05F, reach(mob, tall)), MOB_HEIGHT / tall));
    }

    /**
     * How much a mob is shrunk standing over a trophy item in an inventory slot: as over the placed trophy, but no more
     * than lets it fit between the pedestal's top and the slot's top edge (SLOT_ROOM, in the item model's blocks, under
     * its inventory view: tipped back 30 degrees, so the mob's depth shows as half as much height). Measured by its model
     * or its hitbox, whichever is larger, so a mob whose renderer draws it larger than its model still fits.
     */
    static float slotScale(Entity mob) {
        float tall = Math.max(0.1F, mob.getBbHeight());
        float height = Math.max(drawnHeight(mob, tall), tall);
        float depth = Math.max(reach(mob, tall), mob.getBbWidth() * 0.5F);
        return Math.min(fitScale(mob), SLOT_ROOM / (height + depth * 0.5F));
    }

    private static final float SLOT_ROOM = 0.66F;

    /** Whether a trophy of this rank wears a crown over what it shows (so its screen offers to hide it). */
    static boolean hasCrown(@Nullable Identifier rarity) {
        return rarity != null && TOPPERS.containsKey(rarity);
    }

    // A trophy's mob never ticks, so what moves by its own clock rather than its age is moved here: the ender dragon's
    // wings, beating once a second as it hovers.
    static void idle(EntityRenderState shown, float ageInTicks) {
        if (shown instanceof EnderDragonRenderState dragon) dragon.flapTime = ageInTicks * DRAGON_FLAPS_PER_TICK;
    }

    private static final float DRAGON_FLAPS_PER_TICK = 0.05F;

    // How far a mob's model is turned from facing the way its yaw says (degrees): the ender dragon's is drawn facing
    // backwards, so it's turned round to face the way others do.
    static float modelTurn(EntityRenderState shown) {
        return shown instanceof EnderDragonRenderState ? 180.0F : 0.0F;
    }

    // How far a mob is lowered onto the well, in its own units (so at any size): the wither's model hovers well over its
    // feet, so it's brought down nearer the well.
    static float sink(EntityRenderState shown) {
        if (shown instanceof WitherRenderState) return WITHER_SINK;
        // The game draws a squid (a glow squid too) lower than its model, its tentacles further under its feet: raised by as
        // much (half as much for a baby).
        if (shown instanceof SquidRenderState squid) return squid.isBaby ? -SQUID_DROP / 2.0F : -SQUID_DROP;
        return 0.0F;
    }

    private static final float SQUID_DROP = 0.7F;

    private static final float WITHER_SINK = 0.5F;

    /**
     * How far a mob's model reaches below its feet, in its own blocks (a squid's or a ghast's tentacles, hanging under
     * the bottom of its hitbox), for it to be lifted by so it stands on the pedestal rather than in it. None for a mob
     * whose model ends at its feet, or with no model of parts.
     */
    static float lift(Entity mob) {
        float[] model = MODEL_SIZES.computeIfAbsent(mob.getType(), type -> measure(mob));
        if (model == null || model[2] <= 0.0F) return 0.0F;
        return model[2] * drawScale(mob, model);
    }

    // How much larger than its model a mob is drawn: 1, at its own size, unless its hitbox is far off its model's height
    // and its width both, when it's taken as scaled to its hitbox's height.
    private static float drawScale(Entity mob, float[] model) {
        float tall = Math.max(0.1F, mob.getBbHeight()) / model[1];
        float wide = Math.max(0.1F, mob.getBbWidth()) / model[3];
        boolean ownHeight = tall > OWN_SIZE_LOW && tall < OWN_SIZE_HIGH, ownWidth = wide > OWN_SIZE_LOW && wide < OWN_SIZE_HIGH;
        return ownHeight || ownWidth ? 1.0F : tall;
    }

    // Where a model's feet are, in its own (upside-down) units: the game draws it flipped and moved down by this.
    private static final float MODEL_FEET = 1.501F;

    static float mobFloor() {
        return MOB_FLOOR;
    }

    // How tall a mob is drawn: its model's height, scaled as its reach is, and as the game draws a player-shaped figure
    // (at 15/16 its model's size); its hitbox's for one without a model of parts.
    private static float drawnHeight(Entity mob, float tall) {
        float[] model = MODEL_SIZES.computeIfAbsent(mob.getType(), type -> measure(mob));
        if (model == null) return tall;
        float height = model[1] * drawScale(mob, model);
        return mob instanceof Avatar ? height * AVATAR_SCALE : height;
    }

    private static float reach(Entity mob, float tall) {
        float[] model = MODEL_SIZES.computeIfAbsent(mob.getType(), type -> measure(mob));
        if (model == null) return Math.max(0.1F, mob.getBbWidth()) * UNMEASURED_REACH;
        return model[0] * drawScale(mob, model);
    }

    // A mob's model at rest, as {reach, height, below its feet, width} in blocks, or null if it has no model of parts.
    private static float @Nullable [] measure(Entity mob) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(mob) instanceof LivingEntityRenderer<?, ?, ?> renderer)) return null;
        try {
            ModelPart root = renderer.getModel().root();
            root.getAllParts().forEach(ModelPart::resetPose);
            float[] bounds = {0.0F, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE};
            Vector3f corner = new Vector3f();
            root.visit(new PoseStack(), (pose, path, index, cube) -> {
                for (int i = 0; i < 8; i++) {
                    corner.set((i & 1) == 0 ? cube.minX : cube.maxX, (i & 2) == 0 ? cube.minY : cube.maxY, (i & 4) == 0 ? cube.minZ : cube.maxZ)
                            .div(16.0F).mulPosition(pose.pose());
                    bounds[0] = Math.max(bounds[0], (float) Math.hypot(corner.x, corner.z));
                    bounds[1] = Math.min(bounds[1], corner.y);
                    bounds[2] = Math.max(bounds[2], corner.y);
                    bounds[3] = Math.min(bounds[3], corner.x);
                    bounds[4] = Math.max(bounds[4], corner.x);
                    bounds[5] = Math.min(bounds[5], corner.z);
                    bounds[6] = Math.max(bounds[6], corner.z);
                }
            });
            float height = bounds[2] - bounds[1];
            // (Its model is upside down: its lowest point is its largest y, its feet at MODEL_FEET.)
            float width = Math.max(0.01F, Math.max(bounds[4] - bounds[3], bounds[6] - bounds[5]));
            return height > 0.01F && bounds[0] > 0.01F ? new float[] {bounds[0], height, Math.max(0.0F, bounds[2] - MODEL_FEET), width} : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    // A mob's icon: its spawn egg, or a spawner.
    private static ItemStack spawnEgg(MobTrophyRecord trophy) {
        return trophy.icon();
    }

    // A mannequin's look wearing its pedestal's outfit (in the mob's own saved-data format: its equipment by slot), in the
    // skin of the player who placed the pedestal (a plain mannequin's for one placed by no one).
    private static CompoundTag outfitLook(PedestalBlockEntity pedestal) {
        CompoundTag look;
        if (pedestal.ownerName().isEmpty()) {
            look = new CompoundTag();
            look.putBoolean("hide_description", true);
            look.putBoolean("immovable", true);
        } else {
            look = Statues.look(pedestal.ownerName());
        }
        return dress(look, pedestal);
    }

    // A mannequin's armor stand wearing its pedestal's outfit: with arms, and no base plate (the pedestal is its base).
    private static CompoundTag standLook(PedestalBlockEntity pedestal) {
        CompoundTag look = new CompoundTag();
        look.putBoolean("ShowArms", true);
        look.putBoolean("NoBasePlate", true);
        return dress(look, pedestal);
    }

    // The look given its pedestal's outfit (in the mob's own saved-data format: its equipment by slot).
    private static CompoundTag dress(CompoundTag look, PedestalBlockEntity pedestal) {
        if (pedestal.getLevel() == null) return look;
        CompoundTag equipment = new CompoundTag();
        var ops = pedestal.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (EquipmentSlot slot : PedestalBlockEntity.OUTFIT_SLOTS) {
            ItemStack worn = pedestal.outfit(slot);
            if (!worn.isEmpty()) ItemStack.CODEC.encodeStart(ops, worn).result().ifPresent(tag -> equipment.put(slot.getSerializedName(), tag));
        }
        look.put("equipment", equipment);
        return look;
    }

    // Puts an armor stand in this pose: each part's x, y and z, in PedestalBlockEntity.STAND_PARTS' order.
    private static void pose(ArmorStand stand, List<Float> pose) {
        stand.setHeadPose(rotations(pose, 0));
        stand.setBodyPose(rotations(pose, 1));
        stand.setLeftArmPose(rotations(pose, 2));
        stand.setRightArmPose(rotations(pose, 3));
        stand.setLeftLegPose(rotations(pose, 4));
        stand.setRightLegPose(rotations(pose, 5));
    }

    private static Rotations rotations(List<Float> pose, int part) {
        return new Rotations(pose.get(part * 3), pose.get(part * 3 + 1), pose.get(part * 3 + 2));
    }

    /**
     * The name engraved on a trophy, if it has one: renamed in an anvil, its item's custom name, which it keeps placed
     * and broken. Plain, as the trophy's own name is shown (its frame gives it its look).
     */
    static @Nullable Component engraved(PedestalBlockEntity pedestal) {
        Component name = pedestal.components().get(DataComponents.CUSTOM_NAME);
        return name == null || name.getString().isBlank() ? null : Component.literal(name.getString());
    }

    static Component name(PedestalRecord record) {
        return TrophyKinds.name(record, true).copy().withColor(TrophyKinds.color(record, true));
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState icon = new ItemStackRenderState();
        boolean shown, trophy, detailed, effects, showcaseLight;
        // How far its light has risen, 0 (down, dormant) to 1, as it turns on or off.
        float rise = 1.0F;
        PedestalRarity rarity = DISPLAY;
        @Nullable PedestalEffect effect;
        // A mob trophy's mob, and how much it's shrunk to fit over the pedestal.
        @Nullable EntityRenderState mob;
        // A lying statue's fit and placing: how much more it's shrunk, and how far it's moved up and back, in its own units.
        float lieScale = 1.0F, lieY, lieZ;
        // Whether it's a custom pedestal, whose body is drawn here, and the block it's made of.
        boolean custom;
        @Nullable Identifier material;
        // The crown over its display, if its rank wears one, and the top of what it shows, which the crown floats over.
        @Nullable Identifier topper;
        // Its crown's size, and how far it's moved forward and up (pedestal settings).
        float topperScale = 1.0F, topperX, topperY;
        // (and the height it grows and shrinks about as it turns on and off: its pivot)
        float displayTop, displayPivot;
        // Whether a glass case closes over it, and how high it reaches.
        boolean glass;
        float glassTop;
        // Its stained glass's colour, as a tint (white for clear glass).
        int glassTint = 0xFFFFFFFF;
        // (and how far it reaches out from its middle as it turns, at its own size)
        float mobScale, mobReach;
        // How far its mob is lifted, in the mob's own blocks, so a model hanging below its feet stands on the pedestal.
        float mobLift;
        // Its item's size (the usual one times the pedestal's display scale), and how far its text rises to clear a
        // larger one.
        // The pedestal's display scale, which its effects grow with; its item's size; the height its item turns about
        // (raised for a large item so it stays over the cap); and the height its text panel floats at.
        float size = 1.0F, iconScale = ICON_SCALE, iconY = ICON_Y, panelBottom;
        // How large its text and frame are shown (the pedestal's text size).
        float textScale = 1.0F;
        // How far the pedestal's display height moves what it shows up (or down).
        float lift;
        // Its text's lines (rank, name, who, when), any of which can be missing, and its frame (with its tint, white
        // but for a mod's rank, which takes the common frame in its color) and crest.
        final @Nullable Component[] lines = new Component[4];
        // Where its light and flares are drawn in the world (they're world effects, not drawn with the pose): its
        // block's corner, and how many blocks its units are (a placed pedestal's own, at full size; a trophy display
        // moves and shrinks them with the trophy).
        double originX, originY, originZ;
        float worldScale = 1.0F;
        // Drawn in a scene of its own (the cinematic's altar): its light and flares go with it rather than to the world.
        boolean scene;
        // Whether its rank's beam of light rises from it (not inside a trophy cabinet, whose top it would go through).
        boolean beam = true;
        Identifier frame = FRAMES.get(PedestalRarities.COMMON.id());
        @Nullable Identifier crest, pendant;
        int frameTint = 0xFFFFFFFF;
        // Its yaw (with its spin) about the upright axis, then its pitch and roll about its middle, in degrees.
        float turn, pitch, roll, spin, bob, time, middle, flareX, flareY, flareZ, flareScale, faceX, faceZ, beamX, beamZ;
        double distanceSq;
    }

    // How many times farther than usual a pedestal's display is drawn (and in detail): as many times as it's shown larger,
    // and farther again by as many usual sizes as it's raised.
    private static double reach(PedestalBlockEntity pedestal) {
        return Math.max(1.0, pedestal.scale()) + Math.max(0.0, pedestal.height()) / 2.0;
    }

    private static double sq(double value) {
        return value * value;
    }

    // A large item or mob reaches well past its block, so it's drawn even when the block itself is out of view.
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    // A custom pedestal's body is drawn here rather than with the world's blocks, so it's drawn as far off as blocks are;
    // anything else of a pedestal only as far as block entities usually are (see extractRenderState).
    @Override
    public int getViewDistance() {
        return BODY_DISTANCE;
    }

    /**
     * The space it draws in, for NeoForge, which skips a block entity whose box is out of view: the block, widened and
     * raised as far as its display scale and height take its item or mob, its beam and its text. Declared with the
     * erased type so it overrides NeoForge's {@code IBlockEntityRendererExtension} default from common code; on Fabric
     * nothing calls it, and {@link #shouldRenderOffScreen()} is enough.
     */
    public AABB getRenderBoundingBox(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        if (!(blockEntity instanceof PedestalBlockEntity pedestal)) return new AABB(pos);
        float size = pedestal.scale();
        double wide = 1.0 + size, high = 4.0 + Math.max(0.0F, pedestal.height()) + Math.max(0.0F, pedestal.textHeight()) + 4.0 * size;
        return new AABB(pos.getX() + 0.5 - wide, pos.getY() + Math.min(0.0F, pedestal.height()), pos.getZ() + 0.5 - wide,
                pos.getX() + 0.5 + wide, pos.getY() + high, pos.getZ() + 0.5 + wide);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PedestalBlockEntity pedestal, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(pedestal, state, breakProgress);
        state.originX = pedestal.getBlockPos().getX();
        state.originY = pedestal.getBlockPos().getY();
        state.originZ = pedestal.getBlockPos().getZ();
        state.worldScale = 1.0F;
        state.scene = pedestal.inScene();
        state.beam = true;
        // A custom pedestal's body is drawn here, with its material's textures (its block model is empty).
        state.custom = pedestal.getBlockState().is(Pedestals.CUSTOM.get());
        state.material = pedestal.material();
        // A display shown larger or higher is seen from farther off, so it's drawn, in detail, from as much farther.
        double farther = reach(pedestal);
        if (cameraPosition.distanceToSqr(Vec3.atCenterOf(pedestal.getBlockPos())) > sq(Math.min(BODY_DISTANCE, DISPLAY_DISTANCE * farther))) {
            state.shown = false;
            return;
        }
        PedestalRecord record = pedestal.record();
        MobTrophyRecord mobTrophy = pedestal.mobTrophy();
        state.trophy = pedestal.isTrophy();
        boolean on = pedestal.on();
        state.rise = on ? 1.0F : 0.0F;
        if (pedestal.getLevel() != null) {
            // Its light rises out of the well as it turns on (a trophy placed or powered, an item going on), and sinks
            // back as it turns off (unpowered, or the item coming off).
            float since = Mth.clamp((pedestal.getLevel().getGameTime() - pedestal.displayChangedAt() + partialTicks) / RISE_TICKS, 0.0F, 1.0F);
            float eased = since * since * (3.0F - 2.0F * since);
            state.rise = on ? eased : 1.0F - eased;
        }
        state.shown = pedestal.getLevel() != null && (on || state.rise > 0.0F);
        if (!state.shown) return;
        boolean ranked = state.trophy && !PedestalRarities.isNone(pedestal.rarity());
        state.rarity = ranked ? PedestalRarities.get(pedestal.rarity()) : DISPLAY;
        state.effects = pedestal.effects();
        state.showcaseLight = pedestal.light();
        state.effect = ranked ? PedestalEffects.of(state.rarity) : null;
        float time = pedestal.getLevel().getGameTime() + partialTicks;
        BlockPos pos = pedestal.getBlockPos();
        // (Under glass, at its usual size and place, to fit the case: its own settings are kept for when the glass comes off.)
        state.size = pedestal.glass() ? 1.0F : pedestal.scale();
        state.iconScale = ICON_SCALE * state.size;
        state.lift = pedestal.glass() ? 0.0F : pedestal.height();
        state.iconY = ICON_Y + state.lift;
        // The top of what it shows, for the text panel to float over.
        float top;
        // How far out from its middle the shown thing reaches as it turns, for the flare and the beam to stand behind.
        double reach;
        // A mob trophy's mob, or the statue of the player who earned an achievement trophy if it's set to show it.
        if (mobTrophy != null) state.mob = extractMob(pedestal, mobTrophy.type(), mobTrophy::look, state, partialTicks, time);
        else if (pedestal.statue() && pedestal.canShowStatue()) state.mob = extractMob(pedestal, Statues.FIGURE, () -> Statues.look(record.name()), state, partialTicks, time);
        // A mannequin pedestal: its armor stand wearing its outfit, posed; or a player in its owner's skin wearing it.
        else if (pedestal.mannequin()) state.mob = pedestal.playerModel()
                ? extractMob(pedestal, Statues.FIGURE, () -> outfitLook(pedestal), state, partialTicks, time)
                : extractMob(pedestal, EntityType.ARMOR_STAND, () -> standLook(pedestal), state, partialTicks, time);
        // A leaderboard: its leader's statue.
        else if (pedestal.leaderboard() != null && !pedestal.leaderName().isEmpty())
            state.mob = extractMob(pedestal, Statues.FIGURE, () -> Statues.look(pedestal.leaderName()), state, partialTicks, time);
        else state.mob = null;
        if (state.mob != null) {
            items.updateForTopItem(state.icon, ItemStack.EMPTY, ItemDisplayContext.GROUND, pedestal.getLevel(), null, 0);
            Entity mob = pedestal.shownMob();
            state.middle = MOB_FLOOR + (state.mobLift + Math.max(0.1F, mob.getBbHeight()) / 2.0F) * state.mobScale - ICON_Y;
            state.iconY = ICON_Y + state.lift;
            reach = state.mobReach * state.mobScale + FLARE_GAP;
            // A statue lying along the ground (swimming, gliding, sleeping) lies across the pedestal's middle, shrunk to fit
            // the hologram's width as a standing one fits its height.
            state.lieScale = 1.0F;
            state.lieY = 0.0F;
            state.lieZ = 0.0F;
            if (pedestal.showsStatue() && pedestal.statuePose().lying()) {
                float length = drawnHeight(mob, Math.max(0.1F, mob.getBbHeight()));
                state.lieScale = Math.min(1.0F, 2.0F * MOB_REACH / (length * state.mobScale / pedestal.scale()));
                // The game lays a glider forward from its feet, chest down; a swimmer the same, then a block back and a little
                // up; a sleeper along its bed from near its head. Each is moved back to lie on the middle, off the floor.
                switch (pedestal.statuePose()) {
                    case GLIDING -> {
                        state.lieZ = -length / 2.0F;
                        state.lieY = LIE_LIFT;
                    }
                    case SWIMMING -> state.lieZ = 1.0F - length / 2.0F;
                    default -> {
                        state.lieZ = (mob.getEyeHeight() - 0.1F) - length / 2.0F;
                        state.lieY = LIE_LIFT;
                    }
                }
            }
            // (Its model's top, which a crown sits on: a player's head reaches past its hitbox.)
            top = MOB_FLOOR + state.lift + (state.mobLift + drawnHeight(mob, Math.max(0.1F, mob.getBbHeight())) * statueTop(pedestal)) * state.mobScale;
        } else {
            // A trophy's icon, a mob's spawn egg if the mob can't be made, or the showcased item.
            ItemStack shown = record != null ? (!pedestal.iconOverride().isEmpty() ? pedestal.iconOverride() : TrophyKinds.icon(record, true))
                    : mobTrophy != null ? spawnEgg(mobTrophy) : pedestal.display();
            items.updateForTopItem(state.icon, shown, ItemDisplayContext.GROUND, pedestal.getLevel(), null, (int) pos.asLong());
            // The icon's middle above its pivot: the ground transform lifts items and blocks by different amounts.
            AABB box = state.icon.getModelBoundingBox();
            // Grown from its bottom: at any size its bottom stays where it is at its usual size (kept over the cap), so a
            // larger one rises as it grows rather than reaching down into the pedestal.
            float bottom = pedestal.glass() ? GLASS_FLOOR : Math.max(ICON_Y + (float) box.minY * ICON_SCALE, ICON_FLOOR);
            // Raised into its showcase light (and shrunk only if it can't fit there): see fitToCone.
            float reachPerScale = (float) Math.hypot(Math.max(-box.minX, box.maxX), Math.max(-box.minZ, box.maxZ));
            if (pedestal.glass()) {
                // Under glass: fitted inside the case, as wide as it reaches when it turns and as tall as it is.
                // (As large as the case lets it be, up to GLASS_ITEM_GROW times its usual size, standing on the cap.)
                state.iconScale = fitInCase(ICON_SCALE * GLASS_ITEM_GROW, reachPerScale, (float) (box.maxY - box.minY), bottom, pedestal.pitch() != 0.0F || pedestal.roll() != 0.0F)
                        * Math.min(1.0F, pedestal.scale());
            } else {
                float[] fitted = fitToCone(state, state.rarity != DISPLAY, ICON_SCALE * state.size, reachPerScale, (float) (box.maxY - box.minY) / 2.0F, bottom);
                state.iconScale = fitted[0];
                bottom = fitted[1];
            }
            // (Under glass, centred in the case, top to bottom.)
            if (pedestal.glass()) bottom = caseMiddle((float) (box.maxY - box.minY) * state.iconScale);
            state.iconY = bottom - (float) box.minY * state.iconScale + state.lift;
            state.middle = (float) (box.minY + box.maxY) / 2.0F * state.iconScale;
            reach = Math.hypot(Math.max(-box.minX, box.maxX), Math.max(-box.minZ, box.maxZ)) * state.iconScale + FLARE_GAP;
            top = state.iconY + (float) box.maxY * state.iconScale;
        }
        // A trophy turns and floats like a hologram; a showcased item turns slowly and floats less; one set still doesn't
        // turn (its flare still does).
        state.spin = time * (state.trophy ? 2.0F : 1.0F) % 360.0F;
        // Turned to the pedestal's yaw, then spinning from there if it spins; tipped by its pitch and roll as it turns.
        // (A mob or mannequin under glass stands still, facing its yaw: it's fitted to the case that way round.)
        boolean still = pedestal.glass() && state.mob != null;
        state.turn = (pedestal.yaw() + (pedestal.spins() && !still ? state.spin : 0.0F)) % 360.0F;
        state.pitch = pedestal.pitch();
        state.roll = pedestal.roll();
        // (Under glass, floating within the gaps left over and under it.)
        state.bob = Mth.sin(time / 12.0F) * (state.trophy ? 0.05F : 0.025F);
        state.time = time;
        // Moved by the pedestal's own text height, for a display too large for the usual place to suit.
        state.panelBottom = Math.max(PANEL_BOTTOM, top + PANEL_OVER_MOB) + pedestal.textHeight();
        state.textScale = pedestal.textScale();
        state.displayTop = top;
        state.glass = pedestal.glass();
        state.glassTop = CAP_TOP + PedestalBlock.CASE_HEIGHT;
        state.glassTint = pedestal.glassColor() == null ? 0xFFFFFFFF : ARGB.opaque(pedestal.glassColor().getTextureDiffuseColor());
        state.displayPivot = state.mob != null ? MOB_FLOOR + state.lift : state.iconY;
        // (No crown on a statue lying along the ground: it has no head on top.)
        boolean lying = state.mob != null && pedestal.showsStatue() && pedestal.statuePose().lying();
        // (Unless the pedestal is set not to wear it.)
        state.topper = ranked && !lying && pedestal.crown() ? TOPPERS.get(state.rarity.id()) : null;
        // Over a crown (its art half as tall as it's wide), the text floats clear of it.
        state.topperScale = pedestal.crownScale();
        state.topperX = pedestal.crownX();
        state.topperY = pedestal.crownY();
        if (state.topper != null) {
            state.panelBottom += (TOPPER_GAP + TOPPER_HALF * state.topperScale + Math.max(0.0F, state.topperY)) * state.size + TOPPER_CLEARANCE;
        }
        state.distanceSq = cameraPosition.distanceToSqr(Vec3.atCenterOf(pos));
        state.detailed = state.distanceSq < sq(DETAIL_RANGE * farther);
        if (state.detailed && state.rarity.flareSize() > 0.0F) {
            // The flare sits straight behind the icon's middle along the line from the camera, just past the farthest
            // its turning edges reach (so it never cuts through the icon and glows over it), and scaled up as much as it
            // was pushed back, so it looks the same size and stays centered on the icon from every side.
            double toX = pos.getX() + 0.5 - cameraPosition.x, toY = pos.getY() + state.iconY + state.middle + state.bob - cameraPosition.y;
            double toZ = pos.getZ() + 0.5 - cameraPosition.z;
            double far = Math.max(Math.sqrt(toX * toX + toY * toY + toZ * toZ), 0.5);
            state.flareX = (float) (toX * reach / far);
            state.flareY = (float) (toY * reach / far);
            state.flareZ = (float) (toZ * reach / far);
            state.flareScale = (float) ((far + reach) / far);
        }
        if (state.rarity.beamHeight() > 0.0F || state.rarity.crown() > 0) {
            // The beam and the crown's shards turn about its upright axis to face the camera: its width runs across the line to it. From just
            // under the icon up, it stands straight behind it, past the farthest its turning edges reach, so the icon
            // hides it instead of it glowing over the icon.
            double toX = pos.getX() + 0.5 - cameraPosition.x, toZ = pos.getZ() + 0.5 - cameraPosition.z;
            double across = Math.max(Math.sqrt(toX * toX + toZ * toZ), 1.0E-4);
            state.faceX = (float) (-toZ / across);
            state.faceZ = (float) (toX / across);
            state.beamX = (float) (toX / across * reach);
            state.beamZ = (float) (toZ / across * reach);
        }
        Arrays.fill(state.lines, null);
        Identifier frame = FRAMES.get(state.rarity.id());
        state.frame = frame != null ? frame : FRAMES.get(PedestalRarities.COMMON.id());
        state.frameTint = frame != null || !state.trophy ? 0xFFFFFFFF : ARGB.opaque(state.rarity.color());
        state.crest = CRESTS.get(state.rarity.id());
        state.pendant = PENDANTS.get(state.rarity.id());
        // Its text while the player looks at it, always if it's set to (within name tags' usual distance), or never.
        boolean text = switch (pedestal.textMode()) {
            case LOOKING -> lookedAt(pos, pedestal.scale(), pedestal.height());
            case ALWAYS -> state.distanceSq < sq(ALWAYS_REACH * farther);
            case HIDDEN -> false;
        };
        if (text) {
            if (record != null) {
                state.lines[0] = state.rarity.displayName();
                state.lines[1] = engraved(pedestal) != null ? engraved(pedestal).copy().withColor(TrophyKinds.color(record, true)) : name(record);
                state.lines[2] = record.unlockedBy();
                state.lines[3] = record.date().copy().withStyle(ChatFormatting.GRAY);
            } else if (mobTrophy != null) {
                state.lines[0] = ranked ? state.rarity.displayName() : null;
                state.lines[1] = engraved(pedestal) != null ? engraved(pedestal) : mobTrophy.displayName();
                state.lines[2] = mobTrophy.slain() ? mobTrophy.slainLine() : null;
                state.lines[3] = mobTrophy.slain() ? mobTrophy.date().copy().withStyle(ChatFormatting.GRAY) : null;
            } else if (pedestal.leaderboard() != null) {
                // A leaderboard: its board, in gold, then its leader and their count (or that no one leads it yet).
                state.lines[1] = pedestal.leaderboard().title().copy().withColor(0xFFC24A);
                state.lines[2] = pedestal.leaderName().isEmpty() ? Component.translatable("trophypedestals.leaderboard.no_leader")
                        : Component.literal(pedestal.leaderName());
                state.lines[3] = pedestal.leaderName().isEmpty() ? null : pedestal.leaderboard().format(pedestal.leaderValue()).copy().withStyle(ChatFormatting.GRAY);
            } else {
                // A displayed item that was named shows its name, on the line just over it.
                state.lines[3] = pedestal.display().get(DataComponents.CUSTOM_NAME);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.custom) PedestalBody.submit(state.material, poseStack, collector, state.lightCoords);
        if (!state.shown) return;
        poseStack.pushPose();
        // What it shows rises out of the well, growing from nothing, as it turns on, and sinks back into it, shrinking away
        // to nothing, as it turns off.
        float scale = state.iconScale * state.rise;
        poseStack.translate(0.5F, state.iconY + state.bob - (1.0F - state.rise) * RISE_DROP, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.turn));
        tilt(state, poseStack, state.middle * state.rise);
        poseStack.scale(scale, scale, scale);
        state.icon.submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        if (state.mob != null) {
            // The mob stands over the well, turning and floating as an icon does.
            poseStack.pushPose();
            float mobScale = state.mobScale * state.rise * state.lieScale;
            poseStack.translate(0.5F, MOB_FLOOR + state.lift + state.bob - (1.0F - state.rise) * RISE_DROP, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.turn));
            // (About its middle: from its feet, as far up as the icon's middle is over the floor.)
            tilt(state, poseStack, (ICON_Y + state.middle - MOB_FLOOR) * state.rise);
            poseStack.mulPose(Axis.YP.rotationDegrees(modelTurn(state.mob)));
            poseStack.scale(mobScale, mobScale, mobScale);
            poseStack.translate(0.0F, state.mobLift - sink(state.mob), 0.0F);
            // (A lying statue, moved to lie on the middle: in its own units, so at any size.)
            poseStack.translate(0.0F, state.lieY, state.lieZ);
            Minecraft.getInstance().getEntityRenderDispatcher().submit(state.mob, camera, 0.0, 0.0, 0.0, poseStack, collector);
            poseStack.popPose();
        }
        submitWell(state, poseStack, collector);
        if (state.glass) submitGlass(state, poseStack, collector);
        if (state.effects && state.detailed && state.rarity.flareSize() > 0.0F) submitFlares(state, poseStack, collector, camera);
        if (state.effects && state.detailed && state.topper != null) submitTopper(state, poseStack, collector);
        // A mod's effect and the text show once it's fully on.
        if (state.rise < 1.0F) return;
        if (state.effects && state.effect != null) state.effect.submit(poseStack, collector, camera, state.rarity, state.time);
        submitText(state, poseStack, collector, camera);
    }

    // A glass case standing on the cap, as wide as it: its four sides and its lid, each one pane of clear glass with a
    // silver rim, the lid tinted by its light's color; just over what it shows (at its usual size, under glass). Drawn
    // once each: the render type shows both sides of a face, so the far panes show through the near ones, and writes no
    // depth, so the light inside (drawn after it, as a world effect) still shows through it.
    private static void submitGlass(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        float lo = 0.5F - GLASS_CASE_HALF, hi = 0.5F + GLASS_CASE_HALF, bottom = CAP_TOP, top = state.glassTop;
        int light = state.lightCoords;
        // Clear glass: its lid tinted by its light; stained glass: all of it in its colour.
        int sides = state.glassTint;
        int lid = sides != 0xFFFFFFFF ? sides : ARGB.opaque(ARGB.srgbLerp(0.25F, 0xFFFFFFFF, ARGB.opaque(state.rarity.color())));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(GLASS_TEXTURE), (pose, consumer) -> {
            // The sides, each facing out.
            pane(consumer, pose, lo, lo, hi, lo, bottom, top, light, sides, 0, -1);
            pane(consumer, pose, hi, lo, hi, hi, bottom, top, light, sides, 1, 0);
            pane(consumer, pose, hi, hi, lo, hi, bottom, top, light, sides, 0, 1);
            pane(consumer, pose, lo, hi, lo, lo, bottom, top, light, sides, -1, 0);
            // The lid.
            glassVertex(consumer, pose, lo, top, lo, 0, 0, lid, light, 0, 1, 0);
            glassVertex(consumer, pose, lo, top, hi, 0, 1, lid, light, 0, 1, 0);
            glassVertex(consumer, pose, hi, top, hi, 1, 1, lid, light, 0, 1, 0);
            glassVertex(consumer, pose, hi, top, lo, 1, 0, lid, light, 0, 1, 0);
        });
        // While the pedestal is being broken, its cracks show on the whole case too (the case's upper part, a block of its
        // own, takes the game's cracks on its clear model while it's the one being broken).
        if (state.breakProgress != null) {
            int stage = Mth.clamp(state.breakProgress.progress(), 0, ModelBakery.DESTROY_TYPES.size() - 1);
            collector.submitCustomGeometry(poseStack, ModelBakery.DESTROY_TYPES.get(stage), (pose, consumer) -> {
                crackPane(consumer, pose, lo, lo, hi, lo, bottom, top, light, 0, -1);
                crackPane(consumer, pose, hi, lo, hi, hi, bottom, top, light, 1, 0);
                crackPane(consumer, pose, hi, hi, lo, hi, bottom, top, light, 0, 1);
                crackPane(consumer, pose, lo, hi, lo, lo, bottom, top, light, -1, 0);
                crackVertex(consumer, pose, lo, top, lo, 0, 0, light, 0, 1, 0);
                crackVertex(consumer, pose, lo, top, hi, 0, 1, light, 0, 1, 0);
                crackVertex(consumer, pose, hi, top, hi, 1, 1, light, 0, 1, 0);
                crackVertex(consumer, pose, hi, top, lo, 1, 0, light, 0, 1, 0);
            });
        }
    }

    // One side of the case's cracks, as its pane is drawn (the cracks' render type is a block's: no overlay).
    private static void crackPane(VertexConsumer consumer, PoseStack.Pose pose, float x0, float z0, float x1, float z1, float bottom, float top,
                                  int light, int nx, int nz) {
        crackVertex(consumer, pose, x0, bottom, z0, 0, 1, light, nx, 0, nz);
        crackVertex(consumer, pose, x1, bottom, z1, 1, 1, light, nx, 0, nz);
        crackVertex(consumer, pose, x1, top, z1, 1, 0, light, nx, 0, nz);
        crackVertex(consumer, pose, x0, top, z0, 0, 0, light, nx, 0, nz);
    }

    private static void crackVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light, int nx, int ny, int nz) {
        consumer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setLight(light).setNormal(pose, nx, ny, nz);
    }

    // One side of the case, from one corner to the next, as one pane facing out this way.
    private static void pane(VertexConsumer consumer, PoseStack.Pose pose, float x0, float z0, float x1, float z1, float bottom, float top,
                             int light, int argb, int nx, int nz) {
        glassVertex(consumer, pose, x0, bottom, z0, 0, 1, argb, light, nx, 0, nz);
        glassVertex(consumer, pose, x1, bottom, z1, 1, 1, argb, light, nx, 0, nz);
        glassVertex(consumer, pose, x1, top, z1, 1, 0, argb, light, nx, 0, nz);
        glassVertex(consumer, pose, x0, top, z0, 0, 0, argb, light, nx, 0, nz);
    }

    private static void glassVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb, int light,
                                    int nx, int ny, int nz) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    // The glass case: its texture, its half width (the cap's), and where it stands (the cap's top); its height is the
    // block's (PedestalBlock.CASE_HEIGHT), its shape taking it in.
    private static final Identifier GLASS_TEXTURE = TrophyPedestalsMod.id("textures/misc/vitrine.png");
    private static final float GLASS_CASE_HALF = 6.0F / 16.0F, CAP_TOP = 14.0F / 16.0F;
    // How far inside the glass what it shows is kept, all round; the gap left over and under it, so it floats in the case
    // (bobbing within it); where it stands (that gap over the cap); and how much larger than usual an item may be drawn.
    private static final float GLASS_CASE_MARGIN = 0.04F, GLASS_GAP = 0.14F, GLASS_FLOOR = CAP_TOP + GLASS_GAP, GLASS_ITEM_GROW = 2.0F;

    // Tips what it shows by its pitch (about its own side to side) and then its roll (about the way it faces), round its
    // middle, this far over where it's drawn from.
    private static void tilt(State state, PoseStack poseStack, float middle) {
        if (state.pitch == 0.0F && state.roll == 0.0F) return;
        poseStack.translate(0.0F, middle, 0.0F);
        if (state.pitch != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
        if (state.roll != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(state.roll));
        poseStack.translate(0.0F, -middle, 0.0F);
    }

    // The crown over its display: a square crown of voxels, each solid pixel of the crown art a little block on each of
    // its four sides, the sides one voxel thick; floating just over the top of what it shows, bobbing and turning with it,
    // full bright and shaded by hand; it grows and shrinks, rises and sinks, with what it shows as the trophy turns on and off.
    private static void submitTopper(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        // Worn on what it shows: shrinking and sinking with it as it turns off, and growing and rising with it as it turns on.
        float follow = state.rise, grow = follow * state.size;
        if (state.rise <= 0.0F) return;
        boolean[][] solid = TOPPER_PIXELS.computeIfAbsent(state.topper, PedestalRenderer::readPixels);
        if (solid == null) return;
        int wide = solid.length, tall = solid[0].length;
        // The art's outermost columns (its outline down each end) are left out, so where two sides meet the crown's gold
        // runs on round the corner; each side starts with its corner, ending a voxel short of the next one, whose own
        // start takes the corner there.
        int first = wide > 2 ? 1 : 0, last = wide > 2 ? wide - 2 : wide - 1, columns = last - first + 1;
        poseStack.pushPose();
        float top = state.displayPivot + (state.displayTop - state.displayPivot) * follow;
        poseStack.translate(0.5F, top + TOPPER_GAP * grow + state.bob - (1.0F - state.rise) * RISE_DROP, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.turn));
        poseStack.scale(grow, grow, grow);
        // Moved forward (the way what it shows faces as it turns) and up, then sized, as the pedestal is set.
        poseStack.translate(0.0F, state.topperY, state.topperX);
        poseStack.scale(state.topperScale, state.topperScale, state.topperScale);
        float side = 2.0F * TOPPER_HALF, voxel = side / (columns + 1), height = voxel * tall, length = voxel * columns;
        float u0 = first / (float) wide, u1 = (last + 1) / (float) wide;
        float[] xs = {-TOPPER_HALF, TOPPER_HALF, TOPPER_HALF, -TOPPER_HALF}, zs = {-TOPPER_HALF, -TOPPER_HALF, TOPPER_HALF, TOPPER_HALF};
        collector.submitCustomGeometry(poseStack, RenderTypes.text(state.topper), (pose, consumer) -> {
            for (int i = 0; i < 4; i++) {
                float x0 = xs[i], z0 = zs[i], x1 = xs[(i + 1) % 4], z1 = zs[(i + 1) % 4];
                // Along the side, and in towards the middle by one voxel.
                float dx = (x1 - x0) / side, dz = (z1 - z0) / side;
                float inX = -dz * voxel, inZ = dx * voxel;
                if ((x0 + x1) * inX + (z0 + z1) * inZ > 0.0F) {
                    inX = -inX;
                    inZ = -inZ;
                }
                // Its outer and inner faces: the art, its see-through pixels cut away.
                crownFace(consumer, pose, x0, z0, dx, dz, length, height, u0, u1, SHADE_OUTER);
                crownFace(consumer, pose, x0 + inX, z0 + inZ, dx, dz, length, height, u0, u1, SHADE_INNER);
                // Each solid pixel's edges where it meets a see-through one (or the art's top and bottom): one voxel deep,
                // in its own color. Its start, at the corner, is the outside there, on the previous side's face; its end
                // is hidden in the next side's corner.
                for (int px = first; px <= last; px++) {
                    for (int py = 0; py < tall; py++) {
                        if (!solid[px][py]) continue;
                        float a0 = (px - first) * voxel, a1 = a0 + voxel, y0 = (tall - 1 - py) * voxel, y1 = y0 + voxel;
                        float u = (px + 0.5F) / wide, v = (py + 0.5F) / tall;
                        if (py == 0 || !solid[px][py - 1]) crownEdge(consumer, pose, x0, z0, dx, dz, inX, inZ, a0, y1, a1, y1, u, v, SHADE_TOP);
                        if (py == tall - 1 || !solid[px][py + 1]) crownEdge(consumer, pose, x0, z0, dx, dz, inX, inZ, a0, y0, a1, y0, u, v, SHADE_UNDER);
                        if (px == first) crownEdge(consumer, pose, x0, z0, dx, dz, inX, inZ, a0, y0, a0, y1, u, v, SHADE_OUTER);
                        else if (!solid[px - 1][py]) crownEdge(consumer, pose, x0, z0, dx, dz, inX, inZ, a0, y0, a0, y1, u, v, SHADE_SIDE);
                        if (px < last && !solid[px + 1][py]) crownEdge(consumer, pose, x0, z0, dx, dz, inX, inZ, a1, y0, a1, y1, u, v, SHADE_SIDE);
                    }
                }
            }
        });
        poseStack.popPose();
    }

    // Which pixels of a crown art are solid, as [x][y]; null (and logged once) if it can't be read.
    private static boolean @Nullable [][] readPixels(Identifier texture) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(texture).open(); NativeImage image = NativeImage.read(in)) {
            boolean[][] solid = new boolean[image.getWidth()][image.getHeight()];
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) solid[x][y] = ARGB.alpha(image.getPixel(x, y)) > 127;
            }
            return solid;
        } catch (IOException | RuntimeException e) {
            TrophyPedestalsMod.LOGGER.warn("Can't read the trophy crown {}; it isn't drawn", texture, e);
            return null;
        }
    }

    // A face of a crown side, this long, the art from u0 to u1 across it, seen from both sides.
    private static void crownFace(VertexConsumer consumer, PoseStack.Pose pose, float x0, float z0, float dx, float dz, float length, float height,
                                  float u0, float u1, float shade) {
        int color = ARGB.colorFromFloat(1.0F, shade, shade, shade);
        float bx = x0 + dx * length, bz = z0 + dz * length;
        crownQuad(consumer, pose, x0, 0.0F, z0, u0, 1.0F, bx, 0.0F, bz, u1, 1.0F, bx, height, bz, u1, 0.0F, x0, height, z0, u0, 0.0F, color);
    }

    // The edge of one pixel along a crown side, between (a0, y0) and (a1, y1) on its outer face and the same one voxel in.
    private static void crownEdge(VertexConsumer consumer, PoseStack.Pose pose, float x0, float z0, float dx, float dz, float inX, float inZ,
                                  float a0, float y0, float a1, float y1, float u, float v, float shade) {
        int color = ARGB.colorFromFloat(1.0F, shade, shade, shade);
        float ax = x0 + dx * a0, az = z0 + dz * a0, bx = x0 + dx * a1, bz = z0 + dz * a1;
        crownQuad(consumer, pose, ax, y0, az, u, v, bx, y1, bz, u, v, bx + inX, y1, bz + inZ, u, v, ax + inX, y0, az + inZ, u, v, color);
    }

    // A quad, wound both ways so it's seen from either side.
    private static void crownQuad(VertexConsumer consumer, PoseStack.Pose pose, float x1, float y1, float z1, float u1, float v1,
                                  float x2, float y2, float z2, float u2, float v2, float x3, float y3, float z3, float u3, float v3,
                                  float x4, float y4, float z4, float u4, float v4, int color) {
        crownCorner(consumer, pose, x1, y1, z1, u1, v1, color);
        crownCorner(consumer, pose, x2, y2, z2, u2, v2, color);
        crownCorner(consumer, pose, x3, y3, z3, u3, v3, color);
        crownCorner(consumer, pose, x4, y4, z4, u4, v4, color);
        crownCorner(consumer, pose, x4, y4, z4, u4, v4, color);
        crownCorner(consumer, pose, x3, y3, z3, u3, v3, color);
        crownCorner(consumer, pose, x2, y2, z2, u2, v2, color);
        crownCorner(consumer, pose, x1, y1, z1, u1, v1, color);
    }

    private static void crownCorner(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color) {
        consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setLight(FULL_BRIGHT);
    }

    // The text: its lines (rank, name, who and when) on a panel facing the camera, in its rank's frame, over its
    // pedestal; raised to clear an item or mob shown larger than usual. The frame goes in a later pass than the
    // pedestal's light and the text in a later one still, so the text is drawn over the beam and the flare.
    private static void submitText(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        submitTextAt(state, poseStack, collector, camera, 0.5F, state.panelBottom, 0.5F);
    }

    /**
     * Draws its text frame (if it has lines) with its bottom middle here, in the pose's units, facing the camera, at its
     * text scale. (A trophy display places its trophies' frames itself.)
     */
    static void submitTextAt(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, float atX, float atY, float atZ) {
        submitTextAt(state, poseStack, collector, camera, atX, atY, atZ, 0.0F);
    }

    /**
     * As above, pushed {@code back} blocks farther from the camera (or nearer, for a negative one), and drawn as one that
     * far: so a trophy's frame on a cabinet, brought nearer, is drawn over its cabinet's set banner.
     */
    static void submitTextAt(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, float atX, float atY, float atZ,
                             float back) {
        Component[] lines = state.lines;
        int count = 0;
        for (Component line : lines) if (line != null) count++;
        if (count == 0) return;
        Font font = Minecraft.getInstance().font;
        int widest = 0;
        for (Component line : lines) if (line != null) widest = Math.max(widest, font.width(line));
        float halfWidth = widest / 2.0F + PANEL_PAD, height = count * LINE_HEIGHT + 2 * PANEL_PAD - 2;
        poseStack.pushPose();
        poseStack.translate(atX, atY, atZ);
        // (Along the line from the eye to it, which only moves as the camera does, not as it turns: the pose is the
        // world's, from the camera.)
        if (back != 0.0F) {
            Vector3f away = poseStack.last().pose().getTranslation(new Vector3f());
            float far = away.length();
            if (far > 1.0E-3F) poseStack.translate(away.x / far * back, away.y / far * back, away.z / far * back);
        }
        poseStack.mulPose(camera.orientation);
        float panelScale = PANEL_SCALE * state.textScale;
        poseStack.scale(panelScale, -panelScale, panelScale);
        float x0 = -halfWidth, x1 = halfWidth, y0 = -height, y1 = 0.0F;
        int tint = state.frameTint;
        // Panels drawn back to front, the farthest first, so a nearer one's see-through middle shows the one behind it
        // rather than cutting it out; each panel's text right after it.
        int order = PANEL_ORDER + 2 * (int) Math.max(0.0, PANEL_ORDER_RANGE - Math.sqrt(state.distanceSq) - back);
        collector.order(order).submitCustomGeometry(poseStack, RenderTypes.text(state.frame), (pose, consumer) -> {
            float[] xs = {x0, x0 + PANEL_BORDER, x1 - PANEL_BORDER, x1};
            float[] ys = {y0, y0 + PANEL_BORDER, y1 - PANEL_BORDER, y1};
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    panelQuad(consumer, pose, xs[column], ys[row], xs[column + 1], ys[row + 1], column / 3.0F, row / 3.0F, (column + 1) / 3.0F, (row + 1) / 3.0F, tint);
                }
            }
        });
        if (state.crest != null) {
            float foot = y0 + (state.rarity.id().equals(PedestalRarities.EPIC.id()) ? GEM_FOOT : CREST_FOOT);
            collector.order(order).submitCustomGeometry(poseStack, RenderTypes.text(state.crest), (pose, consumer) ->
                    // A little in front of the frame (between it and the text), so where they overlap it's on top.
                    panelQuad(consumer, pose, -CREST_WIDTH, foot - 2 * CREST_HEIGHT, CREST_WIDTH, foot, 0.0F, 0.0F, 1.0F, 1.0F, 0xFFFFFFFF, PANEL_DEPTH / 2.0F));
        }
        if (state.pendant != null) {
            // Hanging from the bottom edge, upside down, its top tucked behind the frame's band.
            float hang = y1 - PENDANT_TUCK;
            collector.order(order).submitCustomGeometry(poseStack, RenderTypes.text(state.pendant), (pose, consumer) ->
                    panelQuad(consumer, pose, -CREST_WIDTH, hang, CREST_WIDTH, hang + 2 * CREST_HEIGHT, 0.0F, 1.0F, 1.0F, 0.0F, 0xFFFFFFFF, 2.0F * PANEL_DEPTH));
        }
        OrderedSubmitNodeCollector text = collector.order(order + 1);
        float y = y0 + PANEL_PAD;
        for (Component line : lines) {
            if (line == null) continue;
            text.submitText(poseStack, -font.width(line) / 2.0F, y, line.getVisualOrderText(), false, Font.DisplayMode.NORMAL, FULL_BRIGHT, 0xFFFFFFFF, 0, 0);
            y += LINE_HEIGHT;
        }
        poseStack.popPose();
    }

    // A quad of the text panel, a little behind its text (towards the far side of the panel, so the text isn't hidden).
    private static void panelQuad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1,
                                  float u0, float v0, float u1, float v1, int argb) {
        panelQuad(consumer, pose, x0, y0, x1, y1, u0, v0, u1, v1, argb, PANEL_DEPTH);
    }

    // The same at another depth (a pendant sits farther back, behind the frame).
    private static void panelQuad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1,
                                  float u0, float v0, float u1, float v1, int argb, float depth) {
        consumer.addVertex(pose, x0, y0, depth).setColor(argb).setUv(u0, v0).setLight(FULL_BRIGHT);
        consumer.addVertex(pose, x0, y1, depth).setColor(argb).setUv(u0, v1).setLight(FULL_BRIGHT);
        consumer.addVertex(pose, x1, y1, depth).setColor(argb).setUv(u1, v1).setLight(FULL_BRIGHT);
        consumer.addVertex(pose, x1, y0, depth).setColor(argb).setUv(u1, v0).setLight(FULL_BRIGHT);
    }

    // The well's lens lit up in the rank's color, full bright; then, in one batch of additive light that
    // writes no depth (the dragon rays' render type; lightning's writes it, so even faded outlines would cut holes in
    // the flare behind them), a soft glow over it, the outlines and the beam.
    private static void submitWell(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        PedestalRarity rarity = state.rarity;
        // The outlines' square, round the well.
        float lo = 0.5F - RING_HALF, hi = 0.5F + RING_HALF;
        // The lens lights up while the pedestal is on (a trophy, or a showcase with something on it, as the rise brings
        // it up): its glass (its own voxels in the model) glows in the rank's color, on a face a hair over each of its
        // rows, see-through so the dark glass under it shows less and less as it fades in.
        float rise = state.rise;
        // It breathes, brightening and dimming slowly, its glow swelling with it (offset by its place, so pedestals side
        // by side don't breathe together).
        float breath = 0.5F + 0.5F * Mth.sin((state.time + (state.blockPos.hashCode() & 0xFF)) * Mth.TWO_PI / BREATH_TICKS);
        // Lit on the glass's own faces, fading in and out with the rise (eased), so turning on and off is a smooth
        // brightening of the glass itself.
        float fade = Mth.clamp(rise / LENS_FADE, 0.0F, 1.0F);
        fade = fade * fade * (3.0F - 2.0F * fade);
        int tint = ARGB.color((int) (255 * fade), ARGB.scaleRGB(rarity.color(), BREATH_DIM + (1.0F - BREATH_DIM) * breath));
        // (None of the lens, its glow, or the cone and frame with the showcase light turned off.)
        if (state.showcaseLight && fade > 0.01F) {
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(WELL_GLOW), (pose, consumer) -> litGlass(consumer, pose, tint));
        }
        // With its flourishes off, only the showcase light and its outlines stay: no beam, halos or crown (nor flares,
        // custom effects or particles).
        boolean rings = state.detailed && rarity.rings() > 0, beam = state.effects && state.beam && rarity.beamHeight() > 0.0F;
        int glow = ARGB.color((int) ((40 + 8 * Math.min(rarity.rank(), 5)) * rise * (GLOW_DIM + (1.0F - GLOW_DIM) * breath)), rarity.color());
        if (state.scene) {
            // In a scene of its own there are no world effects: added to the scene with it, as they would add it.
            collector.submitCustomGeometry(poseStack, RenderTypes.dragonRays(),
                    (pose, consumer) -> light(state, PedestalLight.additive(consumer, pose), lo, hi, rings, beam, glow));
            return;
        }
        // As world effects, drawn after the rest of the level (the clouds included, which drawn with the block
        // entities would be painted over it).
        light(state, PedestalLight.world(state.originX, state.originY, state.originZ, state.worldScale), lo, hi, rings, beam, glow);
    }

    // Its light: the lens's glow, the outlines, the showcase's cone and frame, the beam, the halos and the crown.
    private static void light(State state, PedestalLight.Glow light, float lo, float hi, boolean rings, boolean beam, int glow) {
        PedestalRarity rarity = state.rarity;
        // (Over the lens's raised glass.)
        float up = GLASS_TOP + 0.002F;
        if (state.showcaseLight) light.lens(up, LENS_GLOW_RADIUS, glow, LENS_GLOW_SEGMENTS);
        if (rings) submitRings(state, light, lo, hi);
        // The showcase's cone of light and frame, for a trophy too, in its rank's colors.
        if (state.showcaseLight) showcase(light, up, rarity, state.size, state.rise, state.time);
        // (Under glass, no beam rising through its lid, nor halos wider than the case.)
        if (beam && !state.glass) submitBeam(state, light, up);
        if (state.effects && state.detailed && rarity.halos() > 0 && !state.glass) submitHalos(state, light);
        if (state.effects && state.detailed && rarity.crown() > 0) submitCrown(state, light);
    }

    // The halos: thin rings of light round the icon's middle, each leaning over (the second the other way, a quarter turn
    // round, so they cross like a gyroscope's), turning slowly, with a bright arc sweeping round each (the other way
    // round the second). Wide enough to clear the icon's turning edges, so it hides their far side.
    private static void submitHalos(State state, PedestalLight.Glow glow) {
        PedestalRarity rarity = state.rarity;
        float y = state.iconY + state.middle + state.bob;
        for (int halo = 0; halo < rarity.halos(); halo++) {
            boolean odd = halo % 2 == 1;
            float tilt = (odd ? -HALO_TILT : HALO_TILT) * Mth.DEG_TO_RAD;
            float spin = state.time * HALO_SPIN * Mth.DEG_TO_RAD + halo * Mth.HALF_PI;
            float sweep = state.time * HALO_SWEEP * (odd ? -1.0F : 1.0F);
            int color = odd ? rarity.accent() : rarity.color();
            float tiltCos = Mth.cos(tilt), tiltSin = Mth.sin(tilt), spinCos = Mth.cos(spin), spinSin = Mth.sin(spin);
            for (int i = 0; i < HALO_SEGMENTS; i++) {
                float a0 = i * Mth.TWO_PI / HALO_SEGMENTS, a1 = (i + 1) * Mth.TWO_PI / HALO_SEGMENTS;
                int c0 = ARGB.color((int) (haloAlpha(a0, sweep) * state.rise), color), c1 = ARGB.color((int) (haloAlpha(a1, sweep) * state.rise), color);
                float cos0 = Mth.cos(a0), sin0 = Mth.sin(a0), cos1 = Mth.cos(a1), sin1 = Mth.sin(a1);
                float radius = rarity.haloRadius() * state.size;
                float in = radius - HALO_WIDTH * state.size, out = radius + HALO_WIDTH * state.size;
                // Each corner laid round the ring, leant over about the x axis, then turned about the upright one.
                float ax = cos0 * in, az = sin0 * in, bx = cos0 * out, bz = sin0 * out;
                float cx = cos1 * out, cz = sin1 * out, dx = cos1 * in, dz = sin1 * in;
                glow.quad(
                        0.5F + ax * spinCos + az * tiltCos * spinSin, y - az * tiltSin, 0.5F - ax * spinSin + az * tiltCos * spinCos,
                        0.5F + bx * spinCos + bz * tiltCos * spinSin, y - bz * tiltSin, 0.5F - bx * spinSin + bz * tiltCos * spinCos,
                        0.5F + cx * spinCos + cz * tiltCos * spinSin, y - cz * tiltSin, 0.5F - cx * spinSin + cz * tiltCos * spinCos,
                        0.5F + dx * spinCos + dz * tiltCos * spinSin, y - dz * tiltSin, 0.5F - dx * spinSin + dz * tiltCos * spinCos,
                        c0, c0, c1, c1);
            }
        }
    }

    // The crown: shards of light, long diamonds facing the camera, standing in a ring round the icon, alternating the
    // rank's two colors. The ring turns slowly, the shards rise and fall in a wave, and a glow runs round them, each
    // shard brightening as it passes; a faint wider glow round each.
    private static void submitCrown(State state, PedestalLight.Glow glow) {
        PedestalRarity rarity = state.rarity;
        int shards = rarity.crown();
        float middle = state.iconY + state.middle + state.bob - 0.06F * state.size;
        float sweep = state.time * CROWN_GLOW;
        for (int i = 0; i < shards; i++) {
            float angle = i * Mth.TWO_PI / shards + state.time * CROWN_SPIN;
            float x = 0.5F + Mth.cos(angle) * CROWN_RADIUS * state.size, z = 0.5F + Mth.sin(angle) * CROWN_RADIUS * state.size;
            float y = middle + Mth.sin(state.time * 0.08F + i * Mth.TWO_PI * 2.0F / shards) * 0.05F * state.size;
            float near = (Mth.cos(i * Mth.TWO_PI / shards - sweep) + 1.0F) / 2.0F;
            int color = i % 2 == 0 ? rarity.color() : rarity.accent();
            int tip = ARGB.color((int) ((110 + 145 * near * near) * state.rise), color), core = ARGB.color((int) ((70 + 120 * near * near) * state.rise), color);
            int soft = ARGB.color((int) ((30 + 50 * near) * state.rise), color);
            shard(state, glow, x, y, z, CROWN_HALF_HEIGHT * 1.5F * state.size, CROWN_HALF_WIDTH * 3.0F * state.size, soft, soft);
            shard(state, glow, x, y, z, CROWN_HALF_HEIGHT * state.size, CROWN_HALF_WIDTH * state.size, tip, core);
        }
    }

    // One shard: a long diamond facing the camera about its upright axis, its tips in one color and its sides another.
    private static void shard(State state, PedestalLight.Glow glow, float x, float y, float z, float halfHeight, float halfWidth,
                              int tip, int side) {
        float sx = state.faceX * halfWidth, sz = state.faceZ * halfWidth;
        glow.quad(x, y + halfHeight, z, x + sx, y, z + sz, x, y - halfHeight, z, x - sx, y, z - sz, tip, side, tip, side);
    }

    // A halo's strength at this angle round it: faint, rising to bright where its sweeping arc is.
    private static int haloAlpha(float angle, float sweep) {
        float near = (Mth.cos(angle - sweep) + 1.0F) / 2.0F;
        return (int) (HALO_ALPHA * (0.25F + 0.75F * near * near * near));
    }

    /**
     * Fits an item or mob into its showcase light, as {scale, bottom}: no wider as it turns than the cone is at its
     * middle. The cone widens as it rises, so one too wide where it stands is raised up into it, to where it's wide
     * enough; a display's is also kept under the cone's top, and shrunk (from {@code wanted}) only if it can't fit at all.
     * A trophy's cone is lower, under its icon, which floats over it: a trophy (and a mob or statue anywhere) is only
     * raised, by that cone. {@code reach} (how far it
     * reaches as it turns) and {@code half} (half its height) are per unit of scale; {@code bottom} is where it stands.
     */
    private static float[] fitToCone(State state, boolean trophy, float wanted, float reach, float half, float bottom) {
        float floor = GLASS_TOP + 0.002F;
        float height = CONE_HEIGHT * (trophy ? TROPHY_CONE : 1.0F) * state.size;
        // How much wider the cone gets per block up from its foot, as half widths.
        float slope = (CONE_TOP * state.size - CONE_FOOT) / height;
        float scale = wanted;
        if (!trophy) {
            // Under the cone's top, standing where it does; and wide enough at its middle, raised as far as the top allows.
            if (half > 0.0F) scale = Math.min(scale, (floor + height - bottom) / (2.0F * half));
            scale = Math.min(scale, (height + CONE_FOOT / slope) / (reach / slope + half));
            scale = Math.max(scale, 0.05F * wanted);
        }
        // The lowest its middle may be for the cone to be as wide there as it reaches: its bottom raised to put it there.
        float raised = floor + (reach * scale - CONE_FOOT) / slope - half * scale;
        return new float[] {scale, Math.max(bottom, raised)};
    }

    /**
     * The largest an item or mob may be drawn at (at most {@code wanted}) to fit inside a glass case standing from
     * {@code bottom}: as wide as it reaches when it turns, and as tall as it is ({@code reach} and {@code tall} per unit of
     * scale), clear of the glass by a margin. One that's tipped is fitted as the ball round its middle that it can't
     * reach out of however it's turned.
     */
    private static float fitInCase(float wanted, float reach, float tall, float bottom, boolean tipped) {
        // (Leaving the gap over it too, and room for its bob.)
        float room = CAP_TOP + PedestalBlock.CASE_HEIGHT - GLASS_GAP - bottom;
        if (tipped) {
            float ball = (float) Math.hypot(Math.max(0.0F, reach), Math.max(0.0F, tall) / 2.0F);
            if (ball <= 0.0F) return wanted;
            return Math.max(0.01F, Math.min(wanted, Math.min((GLASS_CASE_HALF - GLASS_CASE_MARGIN) / ball, room / (tall / 2.0F + ball))));
        }
        float wide = reach <= 0.0F ? wanted : (GLASS_CASE_HALF - GLASS_CASE_MARGIN) / reach;
        float high = tall <= 0.0F ? wanted : room / tall;
        return Math.max(0.01F, Math.min(wanted, Math.min(wide, high)));
    }

    // Where something this tall stands to be centred in a glass case, top to bottom.
    private static float caseMiddle(float tall) {
        return CAP_TOP + (PedestalBlock.CASE_HEIGHT - tall) / 2.0F;
    }

    // How high the showcase's cone reaches: its height grows with the display scale.
    private static float coneTop(float floor, PedestalRarity rarity, float size, float rise) {
        return floor + CONE_HEIGHT * (rarity != DISPLAY ? TROPHY_CONE : 1.0F) * size * rise;
    }

    // The showcase's light: a cone widening up out of the well and fading out by the item, and a thin frame round the
    // well's edge, glowing up and down slowly.
    private static void showcase(PedestalLight.Glow glow, float floor, PedestalRarity rarity, float size, float rise, float time) {
        int color = rarity.color();
        int foot = ARGB.color((int) (48 * rise), color), clear = ARGB.color(0, color);
        // Its height and the width of its top grow with the display scale; its foot stays the well's.
        float top = coneTop(floor, rarity, size, rise);
        float f0 = 0.5F - CONE_FOOT, f1 = 0.5F + CONE_FOOT, t0 = 0.5F - CONE_TOP * size, t1 = 0.5F + CONE_TOP * size;
        glow.quad(f0, floor, f0, f1, floor, f0, t1, top, t0, t0, top, t0, foot, foot, clear, clear);
        glow.quad(f1, floor, f0, f1, floor, f1, t1, top, t1, t1, top, t0, foot, foot, clear, clear);
        glow.quad(f1, floor, f1, f0, floor, f1, t0, top, t1, t1, top, t1, foot, foot, clear, clear);
        glow.quad(f0, floor, f1, f0, floor, f0, t0, top, t0, t0, top, t1, foot, foot, clear, clear);
        int frame = ARGB.color((int) ((90 + 50 * Mth.sin(time / 16.0F)) * rise), rarity.accent());
        // Just inside the well's walls, so it doesn't fight with their faces.
        float y = floor + 0.004F, lineTop = y + RING_LINE, lo = 0.5F - FRAME_HALF, hi = 0.5F + FRAME_HALF;
        glow.quad(lo, y, lo, hi, y, lo, hi, lineTop, lo, lo, lineTop, lo, frame, frame, frame, frame);
        glow.quad(hi, y, lo, hi, y, hi, hi, lineTop, hi, hi, lineTop, lo, frame, frame, frame, frame);
        glow.quad(hi, y, hi, lo, y, hi, lo, lineTop, hi, hi, lineTop, hi, frame, frame, frame, frame);
        glow.quad(lo, y, hi, lo, y, lo, lo, lineTop, lo, lo, lineTop, hi, frame, frame, frame, frame);
    }

    /**
     * A pedestal item's showcase light, as the placed pedestal shows it while on (in slots and in hand, for a trophy):
     * its lit, breathing lens in its rank's color (a display's warm light for none), the glow over it, and the cone of
     * light and frame rising out of the well; with {@code rings}, its rank's rising outlines too (in hand, not in a slot,
     * where they'd crowd the icon). In the block's units, over its model.
     */
    public static void submitItemLight(PoseStack poseStack, SubmitNodeCollector collector, @Nullable Identifier rarityId, float time,
                                       boolean rings, float iconMiddle) {
        PedestalRarity rarity = rarityId == null || PedestalRarities.isNone(rarityId) ? DISPLAY : PedestalRarities.get(rarityId);
        float breath = 0.5F + 0.5F * Mth.sin(time * Mth.TWO_PI / BREATH_TICKS);
        int tint = ARGB.opaque(ARGB.scaleRGB(rarity.color(), BREATH_DIM + (1.0F - BREATH_DIM) * breath));
        // After the item's own model, so the light rises out of its well rather than sitting behind it.
        OrderedSubmitNodeCollector later = collector.order(ITEM_LIGHT_ORDER);
        later.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(WELL_GLOW), (pose, consumer) -> litGlass(consumer, pose, tint));
        // (In the translucent item sheet, drawn after the item's own model and icon: see PedestalLight.itemSheet.)
        TextureAtlasSprite white = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).getSprite(ITEM_LIGHT_SPRITE);
        later.submitCustomGeometry(poseStack, Sheets.translucentItemSheet(), (pose, consumer) -> {
            int glow = ARGB.color((int) ((40 + 8 * Math.min(rarity.rank(), 5)) * (GLOW_DIM + (1.0F - GLOW_DIM) * breath)), rarity.color());
            // (Over the lens's raised glass.)
            float up = GLASS_TOP + 0.002F;
            boolean withRings = rings && rarity.rings() > 0;
            // (With the outlines, its faces are cut finer, so they sort true against the outlines' (see ItemSheet); in a
            // slot, where rings is off, not at all.)
            PedestalLight.Detail detail = withRings ? PedestalLight.Detail.FINE : rings ? PedestalLight.Detail.NORMAL : PedestalLight.Detail.WHOLE;
            PedestalLight.Glow light = PedestalLight.itemSheet(consumer, pose, white, detail);
            light.lens(up, LENS_GLOW_RADIUS, glow, LENS_GLOW_SEGMENTS);
            showcase(light, up, rarity, 1.0F, 1.0F, time);
            if (withRings) itemRings(light, rarity, time, up, coneTop(up, rarity, 1.0F, 1.0F), Math.max(RING_TO, iconMiddle));
        });
    }

    // Each outline rises from the cap towards the icon's middle, coming in quickly and fading away over most of its
    // rise; with shimmer they take the rank's two colors in turn.
    private static void submitRings(State state, PedestalLight.Glow glow, float lo, float hi) {
        rings(glow, state.rarity, state.time, lo, hi, RING_FROM, Math.max(RING_TO, state.iconY + state.middle), state.size, state.rise);
    }

    // An item's outlines, rising from its lens to this height, each inside the cone of light rising from floor to top
    // (as wide as the cone where the outline is, less the inset), so none crosses its faces.
    private static void itemRings(PedestalLight.Glow glow, PedestalRarity rarity, float time, float floor, float top, float to) {
        int count = rarity.rings();
        float from = floor + 0.02F;
        for (int i = 0; i < count; i++) {
            float rise = ((time / rarity.ringTicks()) + i / (float) count) % 1.0F;
            int argb = ringColor(rarity, i, rise, 1.0F);
            if (argb == 0) continue;
            float y = Mth.lerp(rise, from, to);
            float along = Mth.clamp((y - floor) / (top - floor), 0.0F, 1.0F);
            float half = Mth.lerp(along, CONE_FOOT, CONE_TOP) - ITEM_RING_INSET;
            ringBox(glow, 0.5F - half, 0.5F + half, 0.5F - half, 0.5F + half, y, y + RING_LINE, argb);
        }
    }

    // An outline's color at this point in its rise (0 when it doesn't show): coming in quickly and fading away over
    // most of its rise; with shimmer, every other one in the rank's second color.
    private static int ringColor(PedestalRarity rarity, int i, float rise, float shown) {
        float alpha = Math.min(1.0F, rise / 0.12F) * (1.0F - Mth.clamp((rise - 0.35F) / 0.65F, 0.0F, 1.0F));
        if (alpha <= 0.01F) return 0;
        int color = rarity.shimmer() && i % 2 == 1 ? rarity.accent() : rarity.color();
        return ARGB.color((int) (200 * alpha * alpha * shown), color);
    }

    // An outline's four sides, from x0 to x1 and z0 to z1, from y to top.
    private static void ringBox(PedestalLight.Glow glow, float x0, float x1, float z0, float z1, float y, float top, int argb) {
        glow.quad(x0, y, z0, x1, y, z0, x1, top, z0, x0, top, z0, argb, argb, argb, argb);
        glow.quad(x1, y, z0, x1, y, z1, x1, top, z1, x1, top, z0, argb, argb, argb, argb);
        glow.quad(x1, y, z1, x0, y, z1, x0, top, z1, x1, top, z1, argb, argb, argb, argb);
        glow.quad(x0, y, z1, x0, y, z0, x0, top, z0, x0, top, z1, argb, argb, argb, argb);
    }

    // The outlines, rising from one height to another, widening (or narrowing) to this display scale as they rise.
    private static void rings(PedestalLight.Glow glow, PedestalRarity rarity, float time, float lo, float hi, float from, float to, float size, float shown) {
        int count = rarity.rings();
        for (int i = 0; i < count; i++) {
            float rise = ((time / rarity.ringTicks()) + i / (float) count) % 1.0F;
            int argb = ringColor(rarity, i, rise, shown);
            if (argb == 0) continue;
            // Up to the middle of what it shows, however large.
            float y = Mth.lerp(rise, from, to), top = y + RING_LINE;
            // Out of the well at its own size, widening (or narrowing) to the display scale as it rises.
            // (Round the middle of lo to hi across x, the block's middle across z.)
            float half = Mth.lerp(rise, (hi - lo) / 2.0F, (hi - lo) / 2.0F * size), middle = (lo + hi) / 2.0F;
            ringBox(glow, middle - half, middle + half, 0.5F - half, 0.5F + half, y, top, argb);
        }
    }

    // The beam: three layers (a wide haze, a glow and a white-hot core), each brightest down its middle and fading to
    // nothing at its edges, rising out of the well, brightest just over it and fading out towards the top, breathing a
    // little; with bands of light running up it. It faces the camera about its upright axis, and from just under the
    // icon up it stands behind it (its foot bending back to meet it).
    private static void submitBeam(State state, PedestalLight.Glow glow, float from) {
        PedestalRarity rarity = state.rarity;
        float height = rarity.beamHeight() * state.size;
        float breath = 0.85F + 0.15F * Mth.sin(state.time / 9.0F);
        float[] at = {from, BEAM_BEHIND_FROM, from + 0.45F, from + height * 0.5F, from + height};
        float[] strength = {0.55F, 1.0F, 0.9F, 0.45F, 0.0F};
        float wide = (float) Math.sqrt(state.size);
        beamLayer(state, glow, at, strength, BEAM_HAZE * wide, rarity.color(), 55 * breath);
        beamLayer(state, glow, at, strength, BEAM_HALF * wide, rarity.color(), 110 * breath);
        beamLayer(state, glow, at, strength, BEAM_CORE * wide, rarity.accent(), 230 * breath);
        // The bands: two short brightenings of the core running up the beam, fading in and out on the way.
        for (int i = 0; i < BEAM_BANDS; i++) {
            float rise = (state.time / BEAM_BAND_TICKS + i / (float) BEAM_BANDS) % 1.0F;
            float middle = BEAM_BEHIND_FROM + rise * (from + height * 0.75F - BEAM_BEHIND_FROM);
            int peak = ARGB.color((int) (170 * Mth.sin(rise * Mth.PI) * state.rise), rarity.accent());
            int clear = ARGB.color(0, rarity.accent());
            float lo = middle - BEAM_BAND_HALF, hi = middle + BEAM_BAND_HALF;
            beamQuad(state, glow, lo, middle, BEAM_BAND_WIDTH, clear, peak, clear, clear);
            beamQuad(state, glow, middle, hi, BEAM_BAND_WIDTH, peak, clear, clear, clear);
        }
    }

    // One layer of the beam, in strips between these heights, each half fading from its middle out to its edge.
    private static void beamLayer(State state, PedestalLight.Glow glow, float[] at, float[] strength, float half, int color, float alpha) {
        int clear = ARGB.color(0, color);
        alpha *= state.rise;
        for (int i = 0; i < at.length - 1; i++) {
            int below = ARGB.color((int) (alpha * strength[i]), color), above = ARGB.color((int) (alpha * strength[i + 1]), color);
            beamQuad(state, glow, at[i], at[i + 1], half, below, above, clear, clear);
        }
    }

    // A strip of the beam from height y0 to y1, as two halves meeting down its middle: the middle takes mid0 at the
    // bottom and mid1 at the top, the edges edge0 and edge1. Its middle stands behind the icon from just under it up.
    private static void beamQuad(State state, PedestalLight.Glow glow, float y0, float y1, float half,
                                 int mid0, int mid1, int edge0, int edge1) {
        float back0 = y0 >= BEAM_BEHIND_FROM ? 1.0F : 0.0F, back1 = y1 >= BEAM_BEHIND_FROM ? 1.0F : 0.0F;
        float x0 = 0.5F + state.beamX * back0, z0 = 0.5F + state.beamZ * back0;
        float x1 = 0.5F + state.beamX * back1, z1 = 0.5F + state.beamZ * back1;
        float sx = state.faceX * half, sz = state.faceZ * half;
        glow.quad(x0 - sx, y0, z0 - sz, x0, y0, z0, x1, y1, z1, x1 - sx, y1, z1 - sz, edge0, mid0, mid1, edge1);
        glow.quad(x0, y0, z0, x0 + sx, y0, z0 + sz, x1 + sx, y1, z1 + sz, x1, y1, z1, mid0, edge0, edge1, mid1);
    }

    // The flare, and at the higher ranks a smaller second one in the accent color turning the other way, in one batch:
    // both are turned here, corner by corner, in the plane facing the camera.
    private static void submitFlares(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        PedestalRarity rarity = state.rarity;
        float half = rarity.flareSize() / 2.0F * state.size * (1.0F + rarity.flarePulse() * Mth.sin(state.time / 7.0F));
        int color = rarity.shimmer() ? ARGB.srgbLerp((Mth.sin(state.time / 10.0F) + 1.0F) / 2.0F, rarity.color(), rarity.accent()) : rarity.color();
        int argb = ARGB.color((int) (255 * rarity.flareAlpha() * state.rise), color);
        // Turning as the icon does, its rays sweeping the way the icon's front face moves.
        float angle = -state.spin * rarity.flareSpin() * Mth.DEG_TO_RAD;
        boolean second = rarity.secondFlare();
        float secondHalf = half * 0.7F, secondAngle = state.spin * rarity.flareSpin() * 0.7F * Mth.DEG_TO_RAD;
        int secondArgb = ARGB.color((int) (200 * rarity.flareAlpha() * state.rise), rarity.accent());
        if (state.scene && camera.orientation != null) {
            // In a scene of its own, with it: each a quad facing the scene's camera, round the icon's middle.
            float fx = 0.5F + state.flareX, fy = state.iconY + state.middle + state.bob + state.flareY, fz = 0.5F + state.flareZ;
            Quaternionf facing = new Quaternionf(camera.orientation);
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(FLARE), (pose, consumer) -> {
                sceneFlare(consumer, pose, facing, fx, fy, fz, half * state.flareScale, angle, argb);
                if (second) sceneFlare(consumer, pose, facing, fx, fy, fz, secondHalf * state.flareScale, secondAngle, secondArgb);
            });
            return;
        }
        // Facing the camera, as world effects (after the clouds).
        float s = state.worldScale;
        double x = state.originX + (0.5 + state.flareX) * s, y = state.originY + (state.iconY + state.middle + state.bob + state.flareY) * s;
        double z = state.originZ + (0.5 + state.flareZ) * s;
        PedestalLight.flare(x, y, z, half * state.flareScale * s, angle, argb);
        if (second) PedestalLight.flare(x, y, z, secondHalf * state.flareScale * s, secondAngle, secondArgb);
    }

    // A flare in a scene: a square of this half width round (x, y, z), turned by `roll` in the plane facing the camera.
    private static void sceneFlare(VertexConsumer consumer, PoseStack.Pose pose, Quaternionf facing, float x, float y, float z, float half, float roll,
                                   int argb) {
        if (ARGB.alpha(argb) == 0) return;
        float cos = Mth.cos(roll) * half, sin = Mth.sin(roll) * half;
        // The corners, from the top left round, then turned to face the camera.
        float[][] corners = {{-cos - sin, -sin + cos}, {-cos + sin, -sin - cos}, {cos + sin, sin - cos}, {cos - sin, sin + cos}};
        float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        Vector3f corner = new Vector3f();
        for (int i = 0; i < 4; i++) {
            facing.transform(corner.set(corners[i][0], corners[i][1], 0.0F));
            consumer.addVertex(pose, x + corner.x, y + corner.y, z + corner.z).setColor(argb).setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0, 1, 0);
        }
    }

    // The lit glass: each of the glass's rows a face over the model's own, showing its pixels of the glow texture.
    private static void litGlass(VertexConsumer consumer, PoseStack.Pose pose, int argb) {
        for (int[] row : LENS_ROWS) {
            float z0 = (4.0F + row[0] * 0.5F) / 16.0F, z1 = z0 + 0.5F / 16.0F;
            float x0 = (4.0F + row[1] * 0.5F) / 16.0F, x1 = (4.0F + (row[2] + 1) * 0.5F) / 16.0F;
            float u0 = row[1] / 16.0F, u1 = (row[2] + 1) / 16.0F, v0 = row[0] / 16.0F, v1 = (row[0] + 1) / 16.0F;
            lens(consumer, pose, x0, GLASS_TOP, z0, u0, v0, argb);
            lens(consumer, pose, x0, GLASS_TOP, z1, u0, v1, argb);
            lens(consumer, pose, x1, GLASS_TOP, z1, u1, v1, argb);
            lens(consumer, pose, x1, GLASS_TOP, z0, u1, v0, argb);
        }
    }

    private static void lens(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }
}
