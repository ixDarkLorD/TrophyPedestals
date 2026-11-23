package net.ixdarklord.trophypedestals.client;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ixdarklord.coolcatcanvas.api.client.registry.BlockEntityRendererRegistry;
import net.ixdarklord.coolcatcore.api.client.registry.TooltipComponentRegistry;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ItemTooltipEvents;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.api.TrophyKinds;
import net.ixdarklord.trophypedestals.api.client.PedestalEffect;
import net.ixdarklord.trophypedestals.api.client.PedestalEffects;
import net.ixdarklord.trophypedestals.client.lens.AppraisalLensVisuals;
import net.ixdarklord.trophypedestals.client.lens.LensVisuals;
import net.ixdarklord.trophypedestals.client.lens.SanguineLensVisuals;
import net.ixdarklord.trophypedestals.pedestal.Leaderboards;
import net.ixdarklord.trophypedestals.pedestal.Lens;
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
import net.minecraft.world.entity.EntityTypes;
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
 * A trophy pedestal in the world: its icon turning and bobbing above the cap, lit full bright so it glows like a
 * hologram, with everything else coming from its rank ({@link PedestalRarity}): outlines of light rising out of the
 * glowing well in its cap and fading away by the icon's middle, as a cartoon hologram's scan lines do; a turning flare
 * behind the icon (two, at the higher ranks); a beam of light; and the particles ({@link PedestalParticles}).
 */
public final class PedestalRenderer implements BlockEntityRenderer<PedestalBlockEntity, PedestalRenderer.State> {
    private static final float PANEL_BOTTOM = 1.95F, PANEL_OVER_MOB = 0.15F, PANEL_SCALE = 0.02F;
    private static final int LINE_HEIGHT = 10, PANEL_PAD = 8, PANEL_BORDER = 8;
    private static final int CREST_WIDTH = 12, CREST_HEIGHT = 6;
    private static final float CREST_FOOT = 2.0F, GEM_FOOT = 8.25F, PENDANT_TUCK = 6.5F;
    private static final int PANEL_ORDER = 1, PANEL_ORDER_RANGE = 64;
    public static final int FULL_BRIGHT = 0xF000F0;
    private static final int ALWAYS_REACH = 32;
    private static final double DETAIL_RANGE = 40.0;
    private static final double ITEM_SIDE = 0.2, ITEM_TOP = 1.75;
    private static final PedestalRarity DISPLAY = PedestalRarity.builder(TrophyPedestalsMod.id("display"), 0, 0xFFE6BE, 0xFFF6E6).build();
    private static final float CROWN_RADIUS = 0.62F, CROWN_HALF_HEIGHT = 0.15F, CROWN_HALF_WIDTH = 0.04F, CROWN_SPIN = 0.012F, CROWN_GLOW = 0.1F;
    private static final float HALO_WIDTH = 0.018F, HALO_TILT = 24.0F, HALO_SPIN = 1.2F, HALO_SWEEP = 0.09F;
    private static final int HALO_SEGMENTS = 28, HALO_ALPHA = 220;
    private static final float RISE_TICKS = 12.0F, RISE_DROP = 0.35F;
    /** How much smaller a powered-off pedestal's showpiece rests, and the height it rests at. */
    private static final float REST_SCALE = 0.72F, REST_FLOOR = 0.9F;
    /** A model thinner than this share of its longest side is a flat item, which lies down to rest; anything thicker sits upright. */
    private static final float REST_FLAT_DEPTH = 0.45F;
    public static final float CONE_FOOT = 0.21F, CONE_TOP = 0.36F, CONE_HEIGHT = 0.8F;
    private static final float TROPHY_CONE = 0.65F;
    private static final int ITEM_LIGHT_ORDER = 1;
    private static final Identifier ITEM_LIGHT_SPRITE = TrophyPedestalsMod.id("item/pedestal_light");
    public static final float FRAME_HALF = 0.235F;
    private static final Identifier WELL_GLOW = TrophyPedestalsMod.id("textures/block/pedestal/well_glow.png");
    public static final float LENS_GLOW_RADIUS = 0.22F;
    public static final int LENS_GLOW_SEGMENTS = 16;
    private static final float BREATH_TICKS = 60.0F, BREATH_DIM = 0.72F, GLOW_DIM = 0.55F;
    private static final Identifier FLARE = TrophyPedestalsMod.id("textures/vfx/pedestal_flare.png");
    private static final float ICON_Y = 1.32F, ICON_SCALE = 1.2F, FLARE_GAP = 0.03F;
    private static final float MOB_FLOOR = 1.17F, MOB_REACH = 0.3F, MOB_HEIGHT = 0.85F;
    private static final float[] PODIUM_SIZE = {0.62F, 0.56F}, PODIUM_SIDE = {-0.3F, 0.3F}, PODIUM_DROP = {-0.04F, 0.0F};
    private static final float PODIUM_BACK = 0.06F;
    private static final float UNMEASURED_REACH = 0.25F;
    private static final float ICON_FLOOR = 1.0F;
    private static final float RING_FROM = 0.875F, RING_TO = 1.34F;
    private static final float RING_HALF = 0.25F, RING_LINE = 0.022F;
    private static final float ITEM_RING_INSET = 0.04F;
    private static final int[][] LENS_ROWS = {{3, 6, 9}, {4, 5, 10}, {5, 4, 11}, {6, 3, 12}, {7, 3, 12}, {8, 3, 12}, {9, 3, 12},
            {10, 4, 11}, {11, 5, 10}, {12, 6, 9}};
    public static final float GLASS_TOP = 14.25F / 16.0F + 0.002F;
    private static final float LENS_FADE = 0.6F, LENS_UNLIT = 0.74F, LENS_OFF = 0.38F, LENS_BREATH_TICKS = 48.0F, LENS_BREATH_RADIUS = 0.17F, PALE_PEAK = 1.0F, PALE_OVERLAY = 0.28F;
    private static final float BEAM_HAZE = 0.36F, BEAM_HALF = 0.17F, BEAM_CORE = 0.05F, BEAM_BEHIND_FROM = 1.02F;
    private static final int BEAM_BANDS = 2;
    private static final float BEAM_BAND_TICKS = 40.0F, BEAM_BAND_HALF = 0.22F, BEAM_BAND_WIDTH = 0.09F;

    private static final Map<Identifier, Identifier> FRAMES = Map.of(
            PedestalRarities.COMMON.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/common.png"),
            PedestalRarities.UNCOMMON.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/uncommon.png"),
            PedestalRarities.RARE.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/rare.png"),
            PedestalRarities.EPIC.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/epic.png"),
            PedestalRarities.LEGENDARY.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/legendary.png"));
    private static final Map<Identifier, Identifier> CRESTS = Map.of(
            PedestalRarities.EPIC.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/epic_crest.png"),
            PedestalRarities.LEGENDARY.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/legendary_crown.png"));
    private static final Map<Identifier, Identifier> TOPPERS = Map.of(
            PedestalRarities.LEGENDARY.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/legendary_crown.png"));
    private static final float TOPPER_HALF = 0.11F, TOPPER_GAP = 0.0F;
    private static final float TOPPER_CLEARANCE = 0.03F;
    private static final Map<Identifier, boolean[][]> TOPPER_PIXELS = new ConcurrentHashMap<>();
    private static final float SHADE_OUTER = 0.92F, SHADE_INNER = 0.62F, SHADE_TOP = 1.0F, SHADE_SIDE = 0.76F, SHADE_UNDER = 0.5F;
    private static final Map<Identifier, Identifier> PENDANTS = Map.of(
            PedestalRarities.LEGENDARY.id(), TrophyPedestalsMod.id("textures/gui/trophy_frame/legendary_crest.png"));
    private static final float PANEL_DEPTH = -0.05F;

    private static final int BODY_DISTANCE = 256;

    private static final Set<EntityType<?>> UNDRAWABLE = ConcurrentHashMap.newKeySet();
    private static final Map<EntityType<?>, float[]> MODEL_SIZES = new ConcurrentHashMap<>();
    private static final float OWN_SIZE_LOW = 0.7F, OWN_SIZE_HIGH = 1.6F;
    private static final float AVATAR_SCALE = 0.9375F;
    private static final float LIE_LIFT = 0.3F;

    private final ItemModelResolver items;

    PedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
    }

    public static void init() {
        PedestalLight.init(FLARE);
        BlockEntityRendererRegistry.register(Pedestals.BLOCK_ENTITY, PedestalRenderer::new);
        BlockEntityRendererRegistry.register(Pedestals.CABINET_ENTITY, TrophyCabinetRenderer::new);
        TooltipComponentRegistry.register(TrophyTooltip.class, TrophyTooltipLine::new);
        // the mobs drawn on trophy items each hold on to their world, so they go when it does
        ClientPlayerEvents.LEAVE.register(player -> TrophyIconRenderer.forgetMobs());
        ItemTooltipEvents.APPEND.register((stack, context, player, flag, lines) -> {
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
     * Whether a pedestal's flourishes show for this player now: as its owner set them to show (always, or while looked
     * at) and the player's own {@code flourishes} option allows. Its showcase light and outlines show either way.
     */
    public static boolean flourishes(PedestalBlockEntity pedestal) {
        if (!TrophyPedestalsClientConfig.FLOURISHES.get()) return false;
        return switch (pedestal.effectsMode()) {
            case ALWAYS -> true;
            case LOOKING -> lookedAt(pedestal.getBlockPos(), pedestal.glass() ? 1.0F : pedestal.scale(), pedestal.glass() ? 0.0F : pedestal.height());
            case HIDDEN -> false;
        };
    }

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

    private static void extractPodium(PedestalBlockEntity pedestal, State state, float partialTicks) {
        Arrays.fill(state.podium, null);
        if (pedestal.leaderboard() == null || state.mob == null || pedestal.glass()) return;
        List<Leaderboards.Leader> runnersUp = pedestal.runnersUp();
        for (int place = 0; place < Math.min(2, runnersUp.size()); place++) {
            String name = runnersUp.get(place).name();
            Entity statue = pedestal.runnerUpStatue(place, name);
            if (statue == null) {
                statue = Statues.FIGURE.create(pedestal.getLevel(), EntitySpawnReason.LOAD);
                if (statue == null) continue;
                DrawnMobs.numbered(statue);
                MobLooks.apply(statue, Statues.look(name), pedestal.getLevel().registryAccess());
                statue.setPos(Vec3.atBottomCenterOf(pedestal.getBlockPos()).add(0.0, MOB_FLOOR, 0.0));
                statue.setYRot(0.0F);
                if (statue instanceof LivingEntity living) {
                    living.setYBodyRot(0.0F);
                    living.setYHeadRot(0.0F);
                }
                pedestal.setRunnerUpStatue(place, name, statue);
            }
            EntityRenderState shown = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(statue, partialTicks);
            shown.lightCoords = FULL_BRIGHT;
            shown.shadowRadius = 0.0F;
            shown.shadowPieces.clear();
            shown.nameTag = null;
            shown.ageInTicks = 0.0F;
            idle(shown, 0.0F);
            state.podium[place] = shown;
            state.podiumScale[place] = state.mobScale * PODIUM_SIZE[place];
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
        if (mob instanceof ArmorStand stand && pedestal.mannequin()) pose(stand, pedestal.glass() ? PedestalBlockEntity.DEFAULT_STAND_POSE : pedestal.standPose());
        float tall = Math.max(0.1F, mob.getBbHeight());
        state.mobReach = Math.max(0.05F, reach(mob, tall));
        state.mobLift = lift(mob);
        state.mobScale = Math.min(1.0F, Math.min(MOB_REACH / state.mobReach, MOB_HEIGHT / tall)) * (pedestal.glass() ? 1.0F : pedestal.scale());
        float drawnTall = state.mobLift + drawnHeight(mob, tall);
        if (!pedestal.glass()) {
            float[] fitted = fitToCone(state, true, state.mobScale, state.mobReach, drawnTall / 2.0F, MOB_FLOOR);
            state.mobScale = fitted[0];
            if (pedestal.scale() > 1.0F) state.lift += fitted[1] - MOB_FLOOR;
        }
        EntityRenderState shown = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(mob, partialTicks);
        shown.lightCoords = FULL_BRIGHT;
        shown.shadowRadius = 0.0F;
        shown.shadowPieces.clear();
        shown.nameTag = null;
        if (shown instanceof HumanoidRenderState) ((PosedFigure) shown).trophypedestals$setPose(figurePose(pedestal));
        boolean moving = pedestal.on() && pedestal.animates() && TrophyPedestalsClientConfig.MOVING_MOBS.get();
        shown.ageInTicks = moving ? time : 0.0F;
        idle(shown, shown.ageInTicks);
        if (pedestal.showsStatue()) pose(shown, pedestal.statuePose());
        if (shown instanceof SquidRenderState squid) tumble(squid, pedestal.getBlockPos(), shown.ageInTicks);
        if (moving) MobAnimations.play(mob, shown, pedestal.animation(), time);
        if (pedestal.glass()) {
            boolean tipped = pedestal.pitch() != 0.0F || pedestal.roll() != 0.0F || shown instanceof SquidRenderState;
            float[] posed = pedestal.mannequin() ? null : posedSize(mob, shown, pedestal.yaw() + modelTurn(shown));
            float reach = state.mobReach, high = drawnTall;
            if (posed != null) {
                reach = tipped ? Math.max(reach, posed[0]) : posed[1];
                high = Math.max(high, state.mobLift + posed[2]);
            }
            float[] most = pedestal.fitGlass(reach, Math.max(0.1F, high - sink(shown)));
            float fit = fitInCase(1.0F, most[0], most[1], GLASS_FLOOR, tipped);
            state.mobScale = fit * Math.min(1.0F, pedestal.scale());
            state.lift = caseMiddle(most[1] * state.mobScale) - MOB_FLOOR;
        }
        return shown;
    }

    private static final PoseStack POSED_POSE = new PoseStack();
    private static final Vector3f POSED_CORNER = new Vector3f();
    private static float posedCos = 1.0F, posedSin = 0.0F;

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
                    armed.translateToHand(arms, arm, POSED_POSE);
                    POSED_POSE.rotate(Axis.XP.rotationDegrees(-90.0F));
                    POSED_POSE.rotate(Axis.YP.rotationDegrees(180.0F));
                    POSED_POSE.translate((arm == HumanoidArm.LEFT ? -1.0F : 1.0F) / 16.0F, 2.0F / 16.0F, -10.0F / 16.0F);
                    Matrix4f hand = POSED_POSE.last().pose();
                    item.visitExtents(point -> include(bounds, POSED_CORNER.set(point).mulPosition(hand)));
                    POSED_POSE.popPose();
                }
            }
            if (bounds[2] == Float.MAX_VALUE) return null;
            float scale = drawScale(mob, rest) * (mob instanceof Avatar ? AVATAR_SCALE : 1.0F);
            return new float[] {(bounds[0] + ARMOR_ALLOWANCE) * scale, (bounds[3] + ARMOR_ALLOWANCE) * scale, (MODEL_FEET - bounds[2] + ARMOR_ALLOWANCE) * scale};
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static final float ARMOR_ALLOWANCE = 1.0F / 16.0F;

    private static void include(float[] bounds, Vector3f corner) {
        bounds[0] = Math.max(bounds[0], (float) Math.hypot(corner.x, corner.z));
        float x = corner.x, z = corner.z, c = posedCos, s = posedSin;
        bounds[3] = Math.max(bounds[3], Math.max(Math.abs(x * c - z * s), Math.abs(x * s + z * c)));
        bounds[1] = Math.max(bounds[1], corner.y);
        bounds[2] = Math.min(bounds[2], corner.y);
    }

    private static void tumble(SquidRenderState squid, BlockPos pos, float age) {
        long seed = pos.asLong() * 0x9E3779B97F4A7C15L;
        float tip = ((seed >>> 40 & 0xFFFF) / 65535.0F * 2.0F - 1.0F) * SQUID_TIP;
        float turn = (seed >>> 20 & 0xFFFF) / 65535.0F * 360.0F;
        squid.xBodyRot = tip + Mth.sin(age / 25.0F) * SQUID_SWAY;
        squid.zBodyRot = turn;
    }

    private static final float SQUID_TIP = 40.0F, SQUID_SWAY = 6.0F;

    private static float statueTop(PedestalBlockEntity pedestal) {
        if (!pedestal.showsStatue()) return 1.0F;
        return switch (pedestal.statuePose()) {
            case STANDING -> 1.0F;
            case CROUCHING -> 0.83F;
            default -> 0.35F;
        };
    }

    private static void pose(EntityRenderState shown, PedestalBlockEntity.StatuePose pose) {
        if (!(shown instanceof LivingEntityRenderState living)) return;
        living.pose = pose.pose();
        if (pose == PedestalBlockEntity.StatuePose.SLEEPING) living.bedOrientation = Direction.SOUTH;
        if (shown instanceof HumanoidRenderState humanoid) {
            humanoid.isCrouching = pose == PedestalBlockEntity.StatuePose.CROUCHING;
            humanoid.isVisuallySwimming = pose == PedestalBlockEntity.StatuePose.SWIMMING;
            humanoid.swimAmount = humanoid.isVisuallySwimming ? 1.0F : 0.0F;
            humanoid.isFallFlying = pose == PedestalBlockEntity.StatuePose.GLIDING;
            if (humanoid.isFallFlying && shown instanceof AvatarRenderState avatar) avatar.fallFlyingTimeInTicks = 20.0F;
        }
    }

    static float fitScale(Entity mob) {
        float tall = Math.max(0.1F, mob.getBbHeight());
        return Math.min(1.0F, Math.min(MOB_REACH / Math.max(0.05F, reach(mob, tall)), MOB_HEIGHT / tall));
    }

    static float slotScale(Entity mob) {
        float tall = Math.max(0.1F, mob.getBbHeight());
        float height = Math.max(drawnHeight(mob, tall), tall);
        float depth = Math.max(reach(mob, tall), mob.getBbWidth() * 0.5F);
        return Math.min(fitScale(mob), SLOT_ROOM / (height + depth * 0.5F));
    }

    private static final float SLOT_ROOM = 0.66F;

    static boolean hasCrown(@Nullable Identifier rarity) {
        return rarity != null && TOPPERS.containsKey(rarity);
    }

    static void idle(EntityRenderState shown, float ageInTicks) {
        if (shown instanceof EnderDragonRenderState dragon) dragon.flapTime = ageInTicks * DRAGON_FLAPS_PER_TICK;
    }

    private static final float DRAGON_FLAPS_PER_TICK = 0.05F;

    static float modelTurn(EntityRenderState shown) {
        return shown instanceof EnderDragonRenderState ? 180.0F : 0.0F;
    }

    static float sink(EntityRenderState shown) {
        if (shown instanceof WitherRenderState) return WITHER_SINK;
        if (shown instanceof SquidRenderState squid) return squid.isBaby ? -SQUID_DROP / 2.0F : -SQUID_DROP;
        return 0.0F;
    }

    private static final float SQUID_DROP = 0.7F;

    private static final float WITHER_SINK = 0.5F;

    static float lift(Entity mob) {
        float[] model = MODEL_SIZES.computeIfAbsent(mob.getType(), type -> measure(mob));
        if (model == null || model[2] <= 0.0F) return 0.0F;
        return model[2] * drawScale(mob, model);
    }

    private static float drawScale(Entity mob, float[] model) {
        float tall = Math.max(0.1F, mob.getBbHeight()) / model[1];
        float wide = Math.max(0.1F, mob.getBbWidth()) / model[3];
        boolean ownHeight = tall > OWN_SIZE_LOW && tall < OWN_SIZE_HIGH, ownWidth = wide > OWN_SIZE_LOW && wide < OWN_SIZE_HIGH;
        return ownHeight || ownWidth ? 1.0F : tall;
    }

    private static final float MODEL_FEET = 1.501F;

    static float mobFloor() {
        return MOB_FLOOR;
    }

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
            float width = Math.max(0.01F, Math.max(bounds[4] - bounds[3], bounds[6] - bounds[5]));
            return height > 0.01F && bounds[0] > 0.01F ? new float[] {bounds[0], height, Math.max(0.0F, bounds[2] - MODEL_FEET), width} : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static ItemStack spawnEgg(MobTrophyRecord trophy) {
        return trophy.icon();
    }

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

    private static CompoundTag standLook(PedestalBlockEntity pedestal) {
        CompoundTag look = new CompoundTag();
        look.putBoolean("ShowArms", true);
        look.putBoolean("NoBasePlate", true);
        return dress(look, pedestal);
    }

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

    static @Nullable Component engraved(PedestalBlockEntity pedestal) {
        Component name = pedestal.components().get(DataComponents.CUSTOM_NAME);
        return name == null || name.getString().isBlank() ? null : Component.literal(name.getString());
    }

    static Component name(PedestalRecord record) {
        return TrophyKinds.name(record, true).copy().withColor(TrophyKinds.color(record, true));
    }

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState icon = new ItemStackRenderState();
        public boolean shown, trophy, detailed, effects, showcaseLight;
        public @Nullable Lens lens;
        public boolean lensEffects;
        /** A display pedestal with no lens fitted: its bezel stands empty. */
        public boolean bareWell;
        /** How far the empty socket's glow has eased in, 0 to 1: it glows while the When Powered option has the pedestal showing. */
        public float socketGlow, socketTime;
        /** How far its When Powered option has it on, 0 to 1, easing as that changes. */
        public float powerFade = 1.0F;
        /** The lens whose glass sits in the bezel; null for the plain dark glass of a trophy, leaderboard or mannequin. */
        public @Nullable Lens glassLens;
        public int lensTint = 0xFFFFFFFF;
        /** How far the lens's effects have faded in, 0 to 1: they ease in and out rather than pop, as when looked at. */
        public float lensFade;
        /** How far the pedestal's display has risen, kept as it is while the lens's effects fade. */
        public float shownRise;
        /** The rarity of the item shown, as the Appraisal Lens reads it: 0 common to 3 epic. */
        public int itemRarity;
        public float backX, backZ;
        public float rise = 1.0F;
        public boolean rests, restsFlat;
        public float restScale, restY, restMobScale, restDepth, restBottom;
        public PedestalRarity rarity = DISPLAY;
        public @Nullable PedestalEffect effect;
        public @Nullable EntityRenderState mob;
        public final @Nullable EntityRenderState[] podium = new EntityRenderState[2];
        public final float[] podiumScale = new float[2];
        public float lieScale = 1.0F, lieY, lieZ;
        public boolean custom;
        public @Nullable Identifier material;
        public @Nullable Identifier topper;
        public float topperScale = 1.0F, topperX, topperY;
        public float displayTop, displayPivot;
        public boolean glass;
        public float glassTop;
        public int glassTint = 0xFFFFFFFF;
        public float mobScale, mobReach;
        public float mobLift;
        public float size = 1.0F, iconScale = ICON_SCALE, iconY = ICON_Y, panelBottom;
        public float textScale = 1.0F;
        public float lift;
        public final @Nullable Component[] lines = new Component[4];
        public double originX, originY, originZ;
        public float worldScale = 1.0F;
        public boolean scene;
        public boolean beam = true;
        public Identifier frame = FRAMES.get(PedestalRarities.COMMON.id());
        public @Nullable Identifier crest, pendant;
        public int frameTint = 0xFFFFFFFF;
        public float turn, pitch, roll, spin, bob, time, middle, flareX, flareY, flareZ, flareScale, faceX, faceZ, beamX, beamZ;
        public double distanceSq;
    }

    /**
     * What a lens needs to know of the display it's showing, kept so that its effects can fade out as they were once the
     * display has been taken: the same rarity, and around an item of the same size and height. Replaced as soon as something
     * else is shown, and dropped once the fade has finished.
     */
    private static final class LastDisplay {
        private PedestalRarity light = DISPLAY;
        private int itemRarity;
        private float iconScale, iconY, middle, top;
        private double reach;

        private void remember(State state, float top, double reach) {
            this.light = state.rarity;
            this.itemRarity = state.itemRarity;
            this.iconScale = state.iconScale;
            this.iconY = state.iconY;
            this.middle = state.middle;
            this.top = top;
            this.reach = reach;
        }
    }

    /** What the renderer keeps for one pedestal between frames, held by the pedestal itself so that it goes when the pedestal does. */
    private static final class Memory {
        private float @Nullable [] lensFade, socketFade;
        private @Nullable LastDisplay last;
        private @Nullable List<Float> figureFrom;
        private float @Nullable [] figurePose;
    }

    /** The pose a player-model mannequin holds, or null to leave it its own stance: when it's in the default pose, or under glass. */
    private static float @Nullable [] figurePose(PedestalBlockEntity pedestal) {
        if (!pedestal.mannequin() || !pedestal.playerModel() || pedestal.glass()) return null;
        List<Float> pose = pedestal.standPose();
        if (pose.equals(PedestalBlockEntity.DEFAULT_STAND_POSE)) return null;
        Memory memory = memory(pedestal);
        if (memory.figureFrom != pose) {
            memory.figureFrom = pose;
            memory.figurePose = new float[pose.size()];
            for (int i = 0; i < pose.size(); i++) memory.figurePose[i] = pose.get(i);
        }
        return memory.figurePose;
    }

    private static Memory memory(PedestalBlockEntity pedestal) {
        if (pedestal.renderMemory() instanceof Memory memory) return memory;
        Memory memory = new Memory();
        pedestal.setRenderMemory(memory);
        return memory;
    }

    /** The display a pedestal's effects are still fading out from, or null if it's showing one or has finished fading. */
    private static @Nullable LastDisplay heldDisplay(PedestalBlockEntity pedestal, float rise) {
        if (pedestal.hasShowpiece()) return null;
        Memory memory = memory(pedestal);
        if (rise <= 0.0F) memory.last = null;
        return memory.last;
    }

    private static double reach(PedestalBlockEntity pedestal) {
        return Math.max(1.0, pedestal.scale()) + Math.max(0.0, pedestal.height()) / 2.0;
    }

    private static double sq(double value) {
        return value * value;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return BODY_DISTANCE;
    }

    public AABB getRenderBoundingBox(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        if (!(blockEntity instanceof PedestalBlockEntity pedestal)) return new AABB(pos);
        float size = pedestal.scale();
        double wide = 1.0 + 1.5 * size, high = 4.0 + Math.max(0.0F, pedestal.height()) + Math.max(0.0F, pedestal.textHeight()) + 4.0 * size;
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
        state.custom = pedestal.getBlockState().is(Pedestals.CUSTOM.get());
        state.material = pedestal.material();
        state.glassLens = LensLooks.lens(pedestal);
        state.bareWell = state.glassLens == null && !pedestal.isTrophy() && pedestal.leaderboard() == null;
        state.powerFade = pedestal.getLevel() != null
                ? socketFade(pedestal, pedestal.displayed(), pedestal.getLevel().getGameTime() + partialTicks) : 1.0F;
        state.socketGlow = state.bareWell ? state.powerFade : 0.0F;
        state.socketTime = pedestal.getLevel() != null ? pedestal.getLevel().getGameTime() + partialTicks : 0.0F;
        state.lensTint = state.glassLens == Lens.CLEAR ? ARGB.opaque(Lens.dyed(pedestal.lens())) : 0xFFFFFFFF;
        double farther = reach(pedestal);
        if (cameraPosition.distanceToSqr(Vec3.atCenterOf(pedestal.getBlockPos())) > sq(Math.min(BODY_DISTANCE, TrophyPedestalsClientConfig.DISPLAY_DISTANCE.get() * farther))) {
            state.shown = false;
            return;
        }
        PedestalRecord record = pedestal.record();
        MobTrophyRecord mobTrophy = pedestal.mobTrophy();
        state.trophy = pedestal.isTrophy();
        boolean on = pedestal.on();
        state.rise = on ? 1.0F : 0.0F;
        if (pedestal.getLevel() != null) {
            float since = Mth.clamp((pedestal.getLevel().getGameTime() - pedestal.displayChangedAt() + partialTicks) / RISE_TICKS, 0.0F, 1.0F);
            float eased = since * since * (3.0F - 2.0F * since);
            state.rise = on ? eased : 1.0F - eased;
        }
        LastDisplay held = heldDisplay(pedestal, state.rise);
        state.shown = pedestal.getLevel() != null && (on || state.rise > 0.0F || pedestal.hasShowpiece());
        if (!state.shown) return;
        boolean ranked = state.trophy && !PedestalRarities.isNone(pedestal.rarity());
        state.rarity = ranked ? PedestalRarities.get(pedestal.rarity()) : DISPLAY;
        state.lens = state.trophy ? null : LensLooks.lens(pedestal);
        state.lensEffects = state.lens != null && LensLooks.effects();
        if (state.lens != null) {
            PedestalRarity light = LensLooks.light(state.lens, pedestal, pedestal.getLevel().getGameTime() + partialTicks);
            if (light != null) state.rarity = light;
            if (held != null && state.lens == Lens.APPRAISAL) state.rarity = held.light;
        }
        state.effects = flourishes(pedestal);
        state.showcaseLight = pedestal.light();
        state.effect = ranked ? PedestalEffects.of(state.rarity) : null;
        float time = pedestal.getLevel().getGameTime() + partialTicks;
        state.lensFade = state.lensEffects ? lensFade(pedestal, state.effects, time) : 0.0F;
        BlockPos pos = pedestal.getBlockPos();
        state.size = pedestal.glass() ? 1.0F : pedestal.scale();
        state.iconScale = ICON_SCALE * state.size;
        state.lift = pedestal.glass() ? 0.0F : pedestal.height();
        state.iconY = ICON_Y + state.lift;
        float top;
        double reach;
        if (mobTrophy != null) state.mob = extractMob(pedestal, mobTrophy.type(), mobTrophy::look, state, partialTicks, time);
        else if (pedestal.statue() && pedestal.canShowStatue()) state.mob = extractMob(pedestal, Statues.FIGURE, () -> Statues.look(record.name()), state, partialTicks, time);
        else if (pedestal.mannequin()) state.mob = pedestal.playerModel()
                ? extractMob(pedestal, Statues.FIGURE, () -> outfitLook(pedestal), state, partialTicks, time)
                : extractMob(pedestal, EntityTypes.ARMOR_STAND, () -> standLook(pedestal), state, partialTicks, time);
        else if (pedestal.leaderboard() != null && !pedestal.leaderName().isEmpty())
            state.mob = extractMob(pedestal, Statues.FIGURE, () -> Statues.look(pedestal.leaderName()), state, partialTicks, time);
        else state.mob = null;
        extractPodium(pedestal, state, partialTicks);
        if (state.mob != null) {
            state.itemRarity = LensLooks.tier(LensLooks.appraised(pedestal));
            items.updateForTopItem(state.icon, ItemStack.EMPTY, ItemDisplayContext.GROUND, pedestal.getLevel(), null, 0);
            Entity mob = pedestal.shownMob();
            state.middle = MOB_FLOOR + (state.mobLift + Math.max(0.1F, mob.getBbHeight()) / 2.0F) * state.mobScale - ICON_Y;
            state.iconY = ICON_Y + state.lift;
            reach = state.mobReach * state.mobScale + FLARE_GAP;
            state.lieScale = 1.0F;
            state.lieY = 0.0F;
            state.lieZ = 0.0F;
            if (pedestal.showsStatue() && pedestal.statuePose().lying()) {
                float length = drawnHeight(mob, Math.max(0.1F, mob.getBbHeight()));
                state.lieScale = Math.min(1.0F, 2.0F * MOB_REACH / (length * state.mobScale / pedestal.scale()));
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
            top = MOB_FLOOR + state.lift + (state.mobLift + drawnHeight(mob, Math.max(0.1F, mob.getBbHeight())) * statueTop(pedestal)) * state.mobScale;
        } else {
            ItemStack shown = record != null ? (!pedestal.iconOverride().isEmpty() ? pedestal.iconOverride() : TrophyKinds.icon(record, true))
                    : mobTrophy != null ? spawnEgg(mobTrophy) : pedestal.display();
            state.itemRarity = LensLooks.tier(shown);
            if (state.lensEffects && state.lens == Lens.APPRAISAL && state.itemRarity == 3 && !shown.isEmpty()) {
                shown = shown.copy();
                shown.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            }
            items.updateForTopItem(state.icon, shown, ItemDisplayContext.GROUND, pedestal.getLevel(), null, (int) pos.asLong());
            AABB box = state.icon.getModelBoundingBox();
            state.restDepth = (float) (box.maxZ - box.minZ) / 2.0F;
            state.restBottom = (float) box.minY;
            state.restsFlat = box.maxZ - box.minZ < REST_FLAT_DEPTH * Math.max(box.maxX - box.minX, box.maxY - box.minY);
            float bottom = pedestal.glass() ? GLASS_FLOOR : Math.max(ICON_Y + (float) box.minY * ICON_SCALE, ICON_FLOOR);
            float reachPerScale = (float) Math.hypot(Math.max(-box.minX, box.maxX), Math.max(-box.minZ, box.maxZ));
            if (pedestal.glass()) {
                state.iconScale = fitInCase(ICON_SCALE, reachPerScale, (float) (box.maxY - box.minY), bottom, pedestal.pitch() != 0.0F || pedestal.roll() != 0.0F);
            } else {
                float[] fitted = fitToCone(state, !displayLight(state.rarity), ICON_SCALE * state.size, reachPerScale, (float) (box.maxY - box.minY) / 2.0F, bottom);
                state.iconScale = fitted[0] * (state.lensEffects && state.lens == Lens.HOLOGRAM ? HOLOGRAM_SCALE : 1.0F);
                bottom = fitted[1];
            }
            if (pedestal.glass()) bottom = caseMiddle((float) (box.maxY - box.minY) * state.iconScale);
            state.iconY = bottom - (float) box.minY * state.iconScale + state.lift;
            state.middle = (float) (box.minY + box.maxY) / 2.0F * state.iconScale;
            reach = Math.hypot(Math.max(-box.minX, box.maxX), Math.max(-box.minZ, box.maxZ)) * state.iconScale + FLARE_GAP;
            top = state.iconY + (float) box.maxY * state.iconScale;
        }
        if (held != null) {
            state.itemRarity = held.itemRarity;
            state.iconScale = held.iconScale;
            state.iconY = held.iconY;
            state.middle = held.middle;
            top = held.top;
            reach = held.reach;
        } else if (state.lens != null && pedestal.hasShowpiece()) {
            Memory memory = memory(pedestal);
            if (memory.last == null) memory.last = new LastDisplay();
            memory.last.remember(state, top, reach);
        }
        state.spin = time * (state.trophy ? 2.0F : 1.0F) % 360.0F;
        boolean still = pedestal.glass() && state.mob != null;
        state.turn = (pedestal.yaw() + (pedestal.spins() && !still ? state.spin : 0.0F)) % 360.0F;
        state.pitch = pedestal.pitch();
        state.roll = pedestal.roll();
        state.bob = Mth.sin(time / 12.0F) * (state.trophy ? 0.05F : 0.025F);
        if (state.lensEffects && !pedestal.glass()) {
            if (state.lens == Lens.TIDE) state.bob = Mth.sin(time / 16.0F) * TIDE_BOB * state.size;
            if (state.lens == Lens.ENDER) {
                state.bob = ENDER_FLOAT * state.size + Mth.sin(time / 20.0F) * 0.04F * state.size;
                // a mannequin floats but stays upright, so that its pose still reads
                if (!pedestal.mannequin()) {
                    state.pitch += Mth.sin(time / 23.0F) * ENDER_TUMBLE;
                    state.roll += Mth.cos(time / 31.0F) * ENDER_TUMBLE * 0.8F;
                }
            }
        }
        state.rests = pedestal.hasShowpiece();
        if (state.rests) {
            // off, it settles on to the pedestal, a flat item lying on the lens and a block sitting on it: a little smaller, tipped no longer and quite still
            state.restScale = Math.min(state.iconScale, ICON_SCALE) * REST_SCALE;
            state.restY = state.restsFlat ? REST_FLOOR + Math.max(0.02F, state.restDepth * state.restScale) - state.middle / state.iconScale * state.restScale
                    : REST_FLOOR + 0.01F - state.restBottom * state.restScale;
            state.restMobScale = state.mobScale / Math.max(1.0F, state.size) * REST_SCALE;
            state.turn = Mth.rotLerp(state.rise, pedestal.yaw() + pedestal.restTurn(), state.turn);
            state.pitch *= state.rise;
            state.roll *= state.rise;
            state.bob *= state.rise;
            if (!on && state.rise <= 0.0F) {
                state.effects = false;
                state.effect = null;
            }
        }
        state.time = time;
        state.panelBottom = Math.max(PANEL_BOTTOM, top + PANEL_OVER_MOB) + pedestal.textHeight();
        state.textScale = pedestal.textScale();
        state.displayTop = top;
        state.glass = pedestal.glass();
        state.glassTop = CAP_TOP + PedestalBlock.CASE_HEIGHT;
        state.glassTint = pedestal.glassColor() == null ? 0xFFFFFFFF : ARGB.opaque(pedestal.glassColor().getTextureDiffuseColor());
        state.displayPivot = state.mob != null ? MOB_FLOOR + state.lift : state.iconY;
        boolean lying = state.mob != null && pedestal.showsStatue() && pedestal.statuePose().lying();
        state.topper = ranked && !lying && pedestal.crown() ? TOPPERS.get(state.rarity.id()) : null;
        state.topperScale = pedestal.crownScale();
        state.topperX = pedestal.crownX();
        state.topperY = pedestal.crownY();
        if (state.topper != null) {
            state.panelBottom += (TOPPER_GAP + TOPPER_HALF * state.topperScale + Math.max(0.0F, state.topperY)) * state.size + TOPPER_CLEARANCE;
        }
        state.distanceSq = cameraPosition.distanceToSqr(Vec3.atCenterOf(pos));
        state.detailed = state.distanceSq < sq(DETAIL_RANGE * farther);
        if (state.detailed && state.rarity.flareSize() > 0.0F) {
            double toX = pos.getX() + 0.5 - cameraPosition.x, toY = pos.getY() + state.iconY + state.middle + state.bob - cameraPosition.y;
            double toZ = pos.getZ() + 0.5 - cameraPosition.z;
            double far = Math.max(Math.sqrt(toX * toX + toY * toY + toZ * toZ), 0.5);
            state.flareX = (float) (toX * reach / far);
            state.flareY = (float) (toY * reach / far);
            state.flareZ = (float) (toZ * reach / far);
            state.flareScale = (float) ((far + reach) / far);
        }
        if (state.rarity.beamHeight() > 0.0F || state.rarity.crown() > 0 || state.lensEffects) {
            double toX = pos.getX() + 0.5 - cameraPosition.x, toZ = pos.getZ() + 0.5 - cameraPosition.z;
            double across = Math.max(Math.sqrt(toX * toX + toZ * toZ), 1.0E-4);
            state.faceX = (float) (-toZ / across);
            state.backX = (float) (toX / across);
            state.backZ = (float) (toZ / across);
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
                Leaderboards.Board board = pedestal.leaderboard();
                state.lines[0] = board.title().copy().withColor(0xFFC24A);
                state.lines[1] = pedestal.leaderName().isEmpty() ? Component.translatable("trophypedestals.leaderboard.no_leader")
                        : Component.translatable("trophypedestals.info.leader", pedestal.leaderName(), board.format(pedestal.leaderValue()));
                List<Leaderboards.Leader> runnersUp = pedestal.runnersUp();
                for (int place = 0; place < 2; place++) {
                    state.lines[2 + place] = place >= runnersUp.size() ? null : Component.translatable("trophypedestals.leaderboard.place." + (place + 2),
                            runnersUp.get(place).name(), board.format(runnersUp.get(place).value())).withStyle(ChatFormatting.GRAY);
                }
            } else {
                state.lines[3] = pedestal.display().get(DataComponents.CUSTOM_NAME);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.custom) PedestalBody.submit(state.material, poseStack, collector, state.lightCoords);
        submitLensGlass(state, poseStack, collector);
        if (!state.shown) return;
        AppraisalLensVisuals.submitVelvet(state, poseStack, collector);
        SanguineLensVisuals.submitMoonDisc(state, poseStack, collector);
        poseStack.pushPose();
        float scale = state.rests ? Mth.lerp(state.rise, state.restScale, state.iconScale) : state.iconScale * state.rise;
        poseStack.translate(0.5F, state.rests ? Mth.lerp(state.rise, state.restY, state.iconY + state.bob) : state.iconY + state.bob - (1.0F - state.rise) * RISE_DROP, 0.5F);
        poseStack.rotate(Axis.YP.rotationDegrees(state.turn));
        tilt(state, poseStack, state.middle * state.rise);
        if (state.rests && state.restsFlat && state.rise < 1.0F) {
            float center = state.middle / state.iconScale * scale;
            poseStack.translate(0.0F, center, 0.0F);
            poseStack.rotate(Axis.XP.rotationDegrees(-90.0F * (1.0F - state.rise)));
            poseStack.translate(0.0F, -center, 0.0F);
        }
        poseStack.scale(scale, scale, scale);
        state.icon.submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        if (state.mob != null) {
            poseStack.pushPose();
            float mobScale = (state.rests ? Mth.lerp(state.rise, state.restMobScale, state.mobScale) : state.mobScale * state.rise) * state.lieScale;
            float mobY = state.rests ? Mth.lerp(state.rise, REST_FLOOR, MOB_FLOOR + state.lift + state.bob) : MOB_FLOOR + state.lift + state.bob - (1.0F - state.rise) * RISE_DROP;
            poseStack.translate(0.5F, mobY, 0.5F);
            poseStack.rotate(Axis.YP.rotationDegrees(state.turn));
            tilt(state, poseStack, (ICON_Y + state.middle - MOB_FLOOR) * state.rise);
            poseStack.rotate(Axis.YP.rotationDegrees(modelTurn(state.mob)));
            poseStack.scale(mobScale, mobScale, mobScale);
            poseStack.translate(0.0F, state.mobLift - sink(state.mob), 0.0F);
            poseStack.translate(0.0F, state.lieY, state.lieZ);
            if (!DrawnMobs.submit(state.mob, camera, poseStack, collector) && state.mob.entityType != null) UNDRAWABLE.add(state.mob.entityType);
            poseStack.popPose();
            for (int place = 0; place < state.podium.length; place++) {
                EntityRenderState runnerUp = state.podium[place];
                if (runnerUp == null) continue;
                poseStack.pushPose();
                float shrink = state.rests ? Mth.lerp(state.rise, REST_SCALE, 1.0F) : state.rise, runnerScale = state.podiumScale[place] * shrink;
                poseStack.translate(0.5F, mobY, 0.5F);
                poseStack.rotate(Axis.YP.rotationDegrees(state.turn));
                poseStack.translate(PODIUM_SIDE[place] * shrink, -PODIUM_DROP[place] * (state.rests ? state.rise : 1.0F), -PODIUM_BACK * shrink);
                poseStack.rotate(Axis.YP.rotationDegrees(modelTurn(runnerUp)));
                poseStack.scale(runnerScale, runnerScale, runnerScale);
                poseStack.translate(0.0F, -sink(runnerUp), 0.0F);
                DrawnMobs.submit(runnerUp, camera, poseStack, collector);
                poseStack.popPose();
            }
        }
        submitWell(state, poseStack, collector);
        if (state.glass) submitGlass(state, poseStack, collector);
        if (state.effects && state.detailed && state.rarity.flareSize() > 0.0F) submitFlares(state, poseStack, collector, camera);
        if (state.effects && state.detailed && state.topper != null) submitTopper(state, poseStack, collector);
        if (state.rise < 1.0F) return;
        if (state.effects && state.effect != null) state.effect.submit(poseStack, collector, camera, state.rarity, state.time);
        submitText(state, poseStack, collector, camera);
    }

    private static void submitGlass(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        float lo = 0.5F - GLASS_CASE_HALF, hi = 0.5F + GLASS_CASE_HALF, bottom = CAP_TOP, top = state.glassTop;
        int light = state.lightCoords;
        int sides = state.glassTint;
        int lid = sides != 0xFFFFFFFF ? sides : ARGB.opaque(ARGB.srgbLerp(0.25F, 0xFFFFFFFF, ARGB.opaque(state.rarity.color())));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(GLASS_TEXTURE), (pose, consumer) -> {
            pane(consumer, pose, lo, lo, hi, lo, bottom, top, light, sides, 0, -1);
            pane(consumer, pose, hi, lo, hi, hi, bottom, top, light, sides, 1, 0);
            pane(consumer, pose, hi, hi, lo, hi, bottom, top, light, sides, 0, 1);
            pane(consumer, pose, lo, hi, lo, lo, bottom, top, light, sides, -1, 0);
            glassVertex(consumer, pose, lo, top, lo, 0, 0, lid, light, 0, 1, 0);
            glassVertex(consumer, pose, lo, top, hi, 0, 1, lid, light, 0, 1, 0);
            glassVertex(consumer, pose, hi, top, hi, 1, 1, lid, light, 0, 1, 0);
            glassVertex(consumer, pose, hi, top, lo, 1, 0, lid, light, 0, 1, 0);
        });
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

    private static final Identifier GLASS_TEXTURE = TrophyPedestalsMod.id("textures/misc/vitrine.png");
    private static final float GLASS_CASE_HALF = 6.0F / 16.0F, CAP_TOP = 14.0F / 16.0F;
    private static final float GLASS_CASE_MARGIN = 0.04F, GLASS_GAP = 0.14F, GLASS_FLOOR = CAP_TOP + GLASS_GAP;

    private static void tilt(State state, PoseStack poseStack, float middle) {
        if (state.pitch == 0.0F && state.roll == 0.0F) return;
        poseStack.translate(0.0F, middle, 0.0F);
        if (state.pitch != 0.0F) poseStack.rotate(Axis.XP.rotationDegrees(state.pitch));
        if (state.roll != 0.0F) poseStack.rotate(Axis.ZP.rotationDegrees(state.roll));
        poseStack.translate(0.0F, -middle, 0.0F);
    }

    private static void submitTopper(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        float follow = state.rise, grow = follow * state.size;
        if (state.rise <= 0.0F) return;
        boolean[][] solid = TOPPER_PIXELS.computeIfAbsent(state.topper, PedestalRenderer::readPixels);
        if (solid == null) return;
        int wide = solid.length, tall = solid[0].length;
        int first = wide > 2 ? 1 : 0, last = wide > 2 ? wide - 2 : wide - 1, columns = last - first + 1;
        poseStack.pushPose();
        float top = state.displayPivot + (state.displayTop - state.displayPivot) * follow;
        poseStack.translate(0.5F, top + TOPPER_GAP * grow + state.bob - (1.0F - state.rise) * RISE_DROP, 0.5F);
        poseStack.rotate(Axis.YP.rotationDegrees(state.turn));
        poseStack.scale(grow, grow, grow);
        poseStack.translate(0.0F, state.topperY, state.topperX);
        poseStack.scale(state.topperScale, state.topperScale, state.topperScale);
        float side = 2.0F * TOPPER_HALF, voxel = side / (columns + 1), height = voxel * tall, length = voxel * columns;
        float u0 = first / (float) wide, u1 = (last + 1) / (float) wide;
        float[] xs = {-TOPPER_HALF, TOPPER_HALF, TOPPER_HALF, -TOPPER_HALF}, zs = {-TOPPER_HALF, -TOPPER_HALF, TOPPER_HALF, TOPPER_HALF};
        collector.submitCustomGeometry(poseStack, RenderTypes.text(state.topper), (pose, consumer) -> {
            for (int i = 0; i < 4; i++) {
                float x0 = xs[i], z0 = zs[i], x1 = xs[(i + 1) % 4], z1 = zs[(i + 1) % 4];
                float dx = (x1 - x0) / side, dz = (z1 - z0) / side;
                float inX = -dz * voxel, inZ = dx * voxel;
                if ((x0 + x1) * inX + (z0 + z1) * inZ > 0.0F) {
                    inX = -inX;
                    inZ = -inZ;
                }
                crownFace(consumer, pose, x0, z0, dx, dz, length, height, u0, u1, SHADE_OUTER);
                crownFace(consumer, pose, x0 + inX, z0 + inZ, dx, dz, length, height, u0, u1, SHADE_INNER);
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

    private static void crownFace(VertexConsumer consumer, PoseStack.Pose pose, float x0, float z0, float dx, float dz, float length, float height,
                                  float u0, float u1, float shade) {
        int color = ARGB.colorFromFloat(1.0F, shade, shade, shade);
        float bx = x0 + dx * length, bz = z0 + dz * length;
        crownQuad(consumer, pose, x0, 0.0F, z0, u0, 1.0F, bx, 0.0F, bz, u1, 1.0F, bx, height, bz, u1, 0.0F, x0, height, z0, u0, 0.0F, color);
    }

    private static void crownEdge(VertexConsumer consumer, PoseStack.Pose pose, float x0, float z0, float dx, float dz, float inX, float inZ,
                                  float a0, float y0, float a1, float y1, float u, float v, float shade) {
        int color = ARGB.colorFromFloat(1.0F, shade, shade, shade);
        float ax = x0 + dx * a0, az = z0 + dz * a0, bx = x0 + dx * a1, bz = z0 + dz * a1;
        crownQuad(consumer, pose, ax, y0, az, u, v, bx, y1, bz, u, v, bx + inX, y1, bz + inZ, u, v, ax + inX, y0, az + inZ, u, v, color);
    }

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

    private static void submitText(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        submitTextAt(state, poseStack, collector, camera, 0.5F, state.panelBottom, 0.5F);
    }

    static void submitTextAt(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, float atX, float atY, float atZ) {
        submitTextAt(state, poseStack, collector, camera, atX, atY, atZ, 0.0F);
    }

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
        if (back != 0.0F) {
            Vector3f away = poseStack.last().pose().getTranslation(new Vector3f());
            float far = away.length();
            if (far > 1.0E-3F) poseStack.translate(away.x / far * back, away.y / far * back, away.z / far * back);
        }
        poseStack.rotate(camera.orientation);
        float panelScale = PANEL_SCALE * state.textScale;
        poseStack.scale(panelScale, -panelScale, panelScale);
        float x0 = -halfWidth, x1 = halfWidth, y0 = -height, y1 = 0.0F;
        int tint = state.frameTint;
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
                    panelQuad(consumer, pose, -CREST_WIDTH, foot - 2 * CREST_HEIGHT, CREST_WIDTH, foot, 0.0F, 0.0F, 1.0F, 1.0F, 0xFFFFFFFF, PANEL_DEPTH / 2.0F));
        }
        if (state.pendant != null) {
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

    private static void panelQuad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1,
                                  float u0, float v0, float u1, float v1, int argb) {
        panelQuad(consumer, pose, x0, y0, x1, y1, u0, v0, u1, v1, argb, PANEL_DEPTH);
    }

    private static void panelQuad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1,
                                  float u0, float v0, float u1, float v1, int argb, float depth) {
        consumer.addVertex(pose, x0, y0, depth).setColor(argb).setUv(u0, v0).setLight(FULL_BRIGHT);
        consumer.addVertex(pose, x0, y1, depth).setColor(argb).setUv(u0, v1).setLight(FULL_BRIGHT);
        consumer.addVertex(pose, x1, y1, depth).setColor(argb).setUv(u1, v1).setLight(FULL_BRIGHT);
        consumer.addVertex(pose, x1, y0, depth).setColor(argb).setUv(u1, v0).setLight(FULL_BRIGHT);
    }

    private static void submitWell(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        PedestalRarity rarity = state.rarity;
        float lo = 0.5F - RING_HALF, hi = 0.5F + RING_HALF;
        float rise = state.rise;
        float breath = 0.5F + 0.5F * Mth.sin((state.time + (state.blockPos.hashCode() & 0xFF)) * Mth.TWO_PI / BREATH_TICKS);
        float fade = Mth.clamp(rise / LENS_FADE, 0.0F, 1.0F);
        fade = fade * fade * (3.0F - 2.0F * fade);
        int tint = ARGB.color((int) (255 * fade), ARGB.scaleRGB(rarity.color(), BREATH_DIM + (1.0F - BREATH_DIM) * breath));
        if (state.showcaseLight && fade > 0.01F && state.glassLens == null && !state.bareWell) {
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(WELL_GLOW), (pose, consumer) -> litGlass(consumer, pose, tint));
        }
        boolean rings = state.detailed && rarity.rings() > 0, beam = state.effects && state.beam && rarity.beamHeight() > 0.0F;
        int glow = ARGB.color((int) ((40 + 8 * Math.min(rarity.rank(), 5)) * rise * (GLOW_DIM + (1.0F - GLOW_DIM) * breath)), rarity.color());
        if (state.scene) {
            collector.submitCustomGeometry(poseStack, RenderTypes.dragonRays(),
                    (pose, consumer) -> light(state, PedestalLight.additive(consumer, pose), lo, hi, rings, beam, glow));
            return;
        }
        light(state, PedestalLight.world(state.originX, state.originY, state.originZ, state.worldScale), lo, hi, rings, beam, glow);
    }

    private static void light(State state, PedestalLight.Glow light, float lo, float hi, boolean rings, boolean beam, int glow) {
        PedestalRarity rarity = state.rarity;
        float up = GLASS_TOP + 0.002F;
        float overlay = paleGlass(state.glassLens) ? PALE_OVERLAY : 1.0F;
        if (state.showcaseLight && !state.bareWell) {
            light.lens(up, LENS_GLOW_RADIUS, ARGB.color((int) (ARGB.alpha(glow) * overlay), glow), LENS_GLOW_SEGMENTS);
        }
        if (state.showcaseLight && state.glassLens != null && state.rise > 0.0F) {
            int lit = ARGB.srgbLerp(0.35F, ARGB.opaque(rarity.color()), 0xFFFFFFFF);
            light.lens(up + 0.001F, LENS_BREATH_RADIUS, ARGB.color((int) ((16.0F + 104.0F * lensBreath(state)) * overlay * state.rise), lit), LENS_GLOW_SEGMENTS);
        }
        if (rings) submitRings(state, light, lo, hi);
        if (state.showcaseLight) showcase(light, up, rarity, state.size, state.rise, state.time);
        if (beam && !state.glass) submitBeam(state, light, up);
        if (state.effects && state.detailed && rarity.halos() > 0 && !state.glass) submitHalos(state, light);
        if (state.effects && state.detailed && rarity.crown() > 0) submitCrown(state, light);
        if (state.detailed && state.lensEffects && state.lensFade > 0.0F) submitLens(state, light, up);
    }

    private static boolean displayLight(PedestalRarity rarity) {
        return rarity == DISPLAY || LensLooks.isLight(rarity);
    }

    private static final float RUNE_SPIN = 0.02F;
    private static final float HOLOGRAM_SCALE = 1.25F;
    private static final float TIDE_BOB = 0.07F, ENDER_FLOAT = 0.14F, ENDER_TUMBLE = 16.0F;
    /** What the lens fitted to a display pedestal draws around its item. */
    private static final float LENS_FADE_TICKS = 8.0F, SOCKET_FADE_TICKS = 4.0F;
    private static final Identifier PLAIN_GLASS = TrophyPedestalsMod.id("textures/block/pedestal/lens.png");
    private static final Identifier BEZEL = TrophyPedestalsMod.id("textures/block/pedestal/bezel.png");
    private static final float SOCKET_GLOW_Y = 14.08F / 16.0F;
    private static final float BEZEL_FLOOR = 14.0F / 16.0F, BEZEL_TOP = 14.5F / 16.0F;
    /** The bezel's pixels, a run a row: its row, first column and last. */
    private static final int[][] BEZEL_RUNS = {{2, 6, 9}, {3, 4, 5}, {3, 10, 11}, {4, 3, 4}, {4, 11, 12}, {5, 3, 3}, {5, 12, 12}, {6, 2, 2}, {6, 13, 13}, {7, 2, 2}, {7, 13, 13}, {8, 2, 2}, {8, 13, 13}, {9, 2, 2}, {9, 13, 13}, {10, 3, 3}, {10, 12, 12}, {11, 3, 4}, {11, 11, 12}, {12, 4, 5}, {12, 10, 11}, {13, 6, 9}};

    /** Eases a pedestal's lens effects toward shown or hidden, over {@link #LENS_FADE_TICKS}, from where they were last frame. */
    private static float lensFade(PedestalBlockEntity pedestal, boolean shown, float time) {
        Memory memory = memory(pedestal);
        if (memory.lensFade == null) memory.lensFade = new float[] {shown ? 1.0F : 0.0F, time};
        return fade(memory.lensFade, shown, time, LENS_FADE_TICKS);
    }

    private static float socketFade(PedestalBlockEntity pedestal, boolean shown, float time) {
        Memory memory = memory(pedestal);
        if (memory.socketFade == null) memory.socketFade = new float[] {shown ? 1.0F : 0.0F, time};
        return fade(memory.socketFade, shown, time, SOCKET_FADE_TICKS);
    }

    private static float fade(float[] fade, boolean shown, float time, float ticks) {
        float step = Mth.clamp(time - fade[1], 0.0F, ticks) / ticks;
        fade[1] = time;
        fade[0] = Mth.clamp(fade[0] + (shown ? step : -step), 0.0F, 1.0F);
        return fade[0] * fade[0] * fade[0] * (fade[0] * (fade[0] * 6.0F - 15.0F) + 10.0F);
    }

    private static void submitLens(State state, PedestalLight.Glow glow, float up) {
        // Everything a lens draws fades by how far its display has risen; fading its effects too is fading that.
        float rise = state.rise;
        state.shownRise = rise;
        state.rise = rise * state.lensFade;
        try {
            LensVisuals.submit(state, glow, up);
        } finally {
            state.rise = rise;
        }
    }

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

    private static void shard(State state, PedestalLight.Glow glow, float x, float y, float z, float halfHeight, float halfWidth,
                              int tip, int side) {
        float sx = state.faceX * halfWidth, sz = state.faceZ * halfWidth;
        glow.quad(x, y + halfHeight, z, x + sx, y, z + sz, x, y - halfHeight, z, x - sx, y, z - sz, tip, side, tip, side);
    }

    private static int haloAlpha(float angle, float sweep) {
        float near = (Mth.cos(angle - sweep) + 1.0F) / 2.0F;
        return (int) (HALO_ALPHA * (0.25F + 0.75F * near * near * near));
    }

    private static float[] fitToCone(State state, boolean trophy, float wanted, float reach, float half, float bottom) {
        float floor = GLASS_TOP + 0.002F;
        float height = CONE_HEIGHT * (trophy ? TROPHY_CONE : 1.0F) * state.size;
        float slope = (CONE_TOP * state.size - CONE_FOOT) / height;
        float scale = wanted;
        if (!trophy) {
            if (half > 0.0F) scale = Math.min(scale, (floor + height - bottom) / (2.0F * half));
            scale = Math.min(scale, (height + CONE_FOOT / slope) / (reach / slope + half));
            scale = Math.max(scale, 0.05F * wanted);
        }
        float raised = floor + (reach * scale - CONE_FOOT) / slope - half * scale;
        return new float[] {scale, Math.max(bottom, raised)};
    }

    private static float fitInCase(float wanted, float reach, float tall, float bottom, boolean tipped) {
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

    private static float caseMiddle(float tall) {
        return CAP_TOP + (PedestalBlock.CASE_HEIGHT - tall) / 2.0F;
    }

    public static float coneTop(float floor, PedestalRarity rarity, float size, float rise) {
        return floor + CONE_HEIGHT * (displayLight(rarity) ? 1.0F : TROPHY_CONE) * size * rise;
    }

    private static void showcase(PedestalLight.Glow glow, float floor, PedestalRarity rarity, float size, float rise, float time) {
        int color = rarity.color();
        int foot = ARGB.color((int) (48 * rise), color), clear = ARGB.color(0, color);
        float top = coneTop(floor, rarity, size, rise);
        float f0 = 0.5F - CONE_FOOT, f1 = 0.5F + CONE_FOOT, t0 = 0.5F - CONE_TOP * size, t1 = 0.5F + CONE_TOP * size;
        glow.quad(f0, floor, f0, f1, floor, f0, t1, top, t0, t0, top, t0, foot, foot, clear, clear);
        glow.quad(f1, floor, f0, f1, floor, f1, t1, top, t1, t1, top, t0, foot, foot, clear, clear);
        glow.quad(f1, floor, f1, f0, floor, f1, t0, top, t1, t1, top, t1, foot, foot, clear, clear);
        glow.quad(f0, floor, f1, f0, floor, f0, t0, top, t0, t0, top, t1, foot, foot, clear, clear);
        int frame = ARGB.color((int) ((90 + 50 * Mth.sin(time / 16.0F)) * rise), rarity.accent());
        float y = floor + 0.004F, lineTop = y + RING_LINE, lo = 0.5F - FRAME_HALF, hi = 0.5F + FRAME_HALF;
        glow.quad(lo, y, lo, hi, y, lo, hi, lineTop, lo, lo, lineTop, lo, frame, frame, frame, frame);
        glow.quad(hi, y, lo, hi, y, hi, hi, lineTop, hi, hi, lineTop, lo, frame, frame, frame, frame);
        glow.quad(hi, y, hi, lo, y, hi, lo, lineTop, hi, hi, lineTop, hi, frame, frame, frame, frame);
        glow.quad(lo, y, hi, lo, y, lo, lo, lineTop, lo, lo, lineTop, hi, frame, frame, frame, frame);
    }

    public static void submitItemLight(PoseStack poseStack, SubmitNodeCollector collector, @Nullable Identifier rarityId, float time,
                                       boolean rings, float iconMiddle) {
        PedestalRarity rarity = rarityId == null || PedestalRarities.isNone(rarityId) ? DISPLAY : PedestalRarities.get(rarityId);
        float breath = 0.5F + 0.5F * Mth.sin(time * Mth.TWO_PI / BREATH_TICKS);
        int tint = ARGB.opaque(ARGB.scaleRGB(rarity.color(), BREATH_DIM + (1.0F - BREATH_DIM) * breath));
        OrderedSubmitNodeCollector later = collector.order(ITEM_LIGHT_ORDER);
        collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(PLAIN_GLASS), (pose, consumer) -> glass(consumer, pose, 0xFFFFFFFF, FULL_BRIGHT));
        collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(BEZEL), (pose, consumer) -> bezel(consumer, pose, FULL_BRIGHT));
        later.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(WELL_GLOW), (pose, consumer) -> litGlass(consumer, pose, tint));
        TextureAtlasSprite white = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).getSprite(ITEM_LIGHT_SPRITE);
        later.submitCustomGeometry(poseStack, Sheets.translucentItemSheet(), (pose, consumer) -> {
            int glow = ARGB.color((int) ((40 + 8 * Math.min(rarity.rank(), 5)) * (GLOW_DIM + (1.0F - GLOW_DIM) * breath)), rarity.color());
            float up = GLASS_TOP + 0.002F;
            boolean withRings = rings && rarity.rings() > 0;
            PedestalLight.Detail detail = withRings ? PedestalLight.Detail.FINE : rings ? PedestalLight.Detail.NORMAL : PedestalLight.Detail.WHOLE;
            PedestalLight.Glow light = PedestalLight.itemSheet(consumer, pose, white, detail);
            light.lens(up, LENS_GLOW_RADIUS, glow, LENS_GLOW_SEGMENTS);
            showcase(light, up, rarity, 1.0F, 1.0F, time);
            if (withRings) itemRings(light, rarity, time, up, coneTop(up, rarity, 1.0F, 1.0F), Math.max(RING_TO, iconMiddle));
        });
    }

    /** The lens fitted in a pedestal's item, in its bezel, as it looks on an unlit pedestal. */
    public static void submitItemLens(PoseStack poseStack, SubmitNodeCollector collector, Lens lens, int tint, int light) {
        int shown = ARGB.opaque(ARGB.scaleRGB(tint, LENS_UNLIT));
        collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(lens.glass()), (pose, consumer) -> glass(consumer, pose, shown, FULL_BRIGHT));
        collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(BEZEL), (pose, consumer) -> bezel(consumer, pose, light));
    }

    private static void submitRings(State state, PedestalLight.Glow glow, float lo, float hi) {
        rings(glow, state.rarity, state.time, lo, hi, RING_FROM, Math.max(RING_TO, state.iconY + state.middle), state.size, state.rise);
    }

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

    private static int ringColor(PedestalRarity rarity, int i, float rise, float shown) {
        float alpha = Math.min(1.0F, rise / 0.12F) * (1.0F - Mth.clamp((rise - 0.35F) / 0.65F, 0.0F, 1.0F));
        if (alpha <= 0.01F) return 0;
        int color = rarity.shimmer() && i % 2 == 1 ? rarity.accent() : rarity.color();
        return ARGB.color((int) (200 * alpha * alpha * shown), color);
    }

    private static void ringBox(PedestalLight.Glow glow, float x0, float x1, float z0, float z1, float y, float top, int argb) {
        glow.quad(x0, y, z0, x1, y, z0, x1, top, z0, x0, top, z0, argb, argb, argb, argb);
        glow.quad(x1, y, z0, x1, y, z1, x1, top, z1, x1, top, z0, argb, argb, argb, argb);
        glow.quad(x1, y, z1, x0, y, z1, x0, top, z1, x1, top, z1, argb, argb, argb, argb);
        glow.quad(x0, y, z1, x0, y, z0, x0, top, z0, x0, top, z1, argb, argb, argb, argb);
    }

    private static void rings(PedestalLight.Glow glow, PedestalRarity rarity, float time, float lo, float hi, float from, float to, float size, float shown) {
        int count = rarity.rings();
        for (int i = 0; i < count; i++) {
            float rise = ((time / rarity.ringTicks()) + i / (float) count) % 1.0F;
            int argb = ringColor(rarity, i, rise, shown);
            if (argb == 0) continue;
            float y = Mth.lerp(rise, from, to), top = y + RING_LINE;
            float half = Mth.lerp(rise, (hi - lo) / 2.0F, (hi - lo) / 2.0F * size), middle = (lo + hi) / 2.0F;
            ringBox(glow, middle - half, middle + half, 0.5F - half, 0.5F + half, y, top, argb);
        }
    }

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

    private static void beamLayer(State state, PedestalLight.Glow glow, float[] at, float[] strength, float half, int color, float alpha) {
        int clear = ARGB.color(0, color);
        alpha *= state.rise;
        for (int i = 0; i < at.length - 1; i++) {
            int below = ARGB.color((int) (alpha * strength[i]), color), above = ARGB.color((int) (alpha * strength[i + 1]), color);
            beamQuad(state, glow, at[i], at[i + 1], half, below, above, clear, clear);
        }
    }

    private static void beamQuad(State state, PedestalLight.Glow glow, float y0, float y1, float half,
                                 int mid0, int mid1, int edge0, int edge1) {
        float back0 = y0 >= BEAM_BEHIND_FROM ? 1.0F : 0.0F, back1 = y1 >= BEAM_BEHIND_FROM ? 1.0F : 0.0F;
        float x0 = 0.5F + state.beamX * back0, z0 = 0.5F + state.beamZ * back0;
        float x1 = 0.5F + state.beamX * back1, z1 = 0.5F + state.beamZ * back1;
        float sx = state.faceX * half, sz = state.faceZ * half;
        glow.quad(x0 - sx, y0, z0 - sz, x0, y0, z0, x1, y1, z1, x1 - sx, y1, z1 - sz, edge0, mid0, mid1, edge1);
        glow.quad(x0, y0, z0, x0 + sx, y0, z0 + sz, x1 + sx, y1, z1 + sz, x1, y1, z1, mid0, edge0, edge1, mid1);
    }

    private static void submitFlares(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        PedestalRarity rarity = state.rarity;
        float half = rarity.flareSize() / 2.0F * state.size * (1.0F + rarity.flarePulse() * Mth.sin(state.time / 7.0F));
        int color = rarity.shimmer() ? ARGB.srgbLerp((Mth.sin(state.time / 10.0F) + 1.0F) / 2.0F, rarity.color(), rarity.accent()) : rarity.color();
        int argb = ARGB.color((int) (255 * rarity.flareAlpha() * state.rise), color);
        float angle = -state.spin * rarity.flareSpin() * Mth.DEG_TO_RAD;
        boolean second = rarity.secondFlare();
        float secondHalf = half * 0.7F, secondAngle = state.spin * rarity.flareSpin() * 0.7F * Mth.DEG_TO_RAD;
        int secondArgb = ARGB.color((int) (200 * rarity.flareAlpha() * state.rise), rarity.accent());
        if (state.scene && camera.orientation != null) {
            float fx = 0.5F + state.flareX, fy = state.iconY + state.middle + state.bob + state.flareY, fz = 0.5F + state.flareZ;
            Quaternionf facing = new Quaternionf(camera.orientation);
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(FLARE), (pose, consumer) -> {
                sceneFlare(consumer, pose, facing, fx, fy, fz, half * state.flareScale, angle, argb);
                if (second) sceneFlare(consumer, pose, facing, fx, fy, fz, secondHalf * state.flareScale, secondAngle, secondArgb);
            });
            return;
        }
        float s = state.worldScale;
        double x = state.originX + (0.5 + state.flareX) * s, y = state.originY + (state.iconY + state.middle + state.bob + state.flareY) * s;
        double z = state.originZ + (0.5 + state.flareZ) * s;
        PedestalLight.flare(x, y, z, half * state.flareScale * s, angle, argb);
        if (second) PedestalLight.flare(x, y, z, secondHalf * state.flareScale * s, secondAngle, secondArgb);
    }

    private static void sceneFlare(VertexConsumer consumer, PoseStack.Pose pose, Quaternionf facing, float x, float y, float z, float half, float roll,
                                   int argb) {
        if (ARGB.alpha(argb) == 0) return;
        float cos = Mth.cos(roll) * half, sin = Mth.sin(roll) * half;
        float[][] corners = {{-cos - sin, -sin + cos}, {-cos + sin, -sin - cos}, {cos + sin, sin - cos}, {cos - sin, sin + cos}};
        float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        Vector3f corner = new Vector3f();
        for (int i = 0; i < 4; i++) {
            facing.transform(corner.set(corners[i][0], corners[i][1], 0.0F));
            consumer.addVertex(pose, x + corner.x, y + corner.y, z + corner.z).setColor(argb).setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0, 1, 0);
        }
    }

    /**
     * The glass in the pedestal's bezel: none on a display pedestal with no lens, the fitted lens's own look (dimmed until
     * its showcase light is on), or the plain dark glass of a trophy, leaderboard or mannequin.
     */
    private static void submitLensGlass(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.bareWell) {
            submitSocketGlow(state);
            return;
        }
        Identifier texture = state.glassLens == null ? PLAIN_GLASS : state.glassLens.glass();
        boolean lit = state.glassLens != null && state.shown && state.showcaseLight;
        float glow = lit ? state.rise : 0.0F;
        float peak = paleGlass(state.glassLens) ? PALE_PEAK : 1.0F;
        float idle = LENS_OFF + (LENS_UNLIT - LENS_OFF) * state.powerFade;
        float bright = idle + (peak - idle) * glow * (lit ? lensBreath(state) : 0.0F);
        int tint = state.glassLens == null ? 0xFFFFFFFF : ARGB.opaque(ARGB.scaleRGB(state.lensTint, bright));
        int light = state.glassLens != null ? FULL_BRIGHT : state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(texture), (pose, consumer) -> glass(consumer, pose, tint, light));
        collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(BEZEL), (pose, consumer) -> bezel(consumer, pose, state.lightCoords));
    }

    /** The gold bezel a lens sits in, one box a run of its pixels: the left half of its texture is its top, the right its sides. */
    private static void bezel(VertexConsumer consumer, PoseStack.Pose pose, int light) {
        for (int[] run : BEZEL_RUNS) {
            float x0 = (4.0F + run[1] * 0.5F) / 16.0F, x1 = (4.0F + (run[2] + 1) * 0.5F) / 16.0F;
            float z0 = (4.0F + run[0] * 0.5F) / 16.0F, z1 = z0 + 0.5F / 16.0F;
            float u0 = run[1] / 32.0F, u1 = (run[2] + 1) / 32.0F, v0 = run[0] / 32.0F, v1 = (run[0] + 1) / 32.0F;
            int top = 0xFFFFFFFF, across = ARGB.colorFromFloat(1.0F, 0.8F, 0.8F, 0.8F), along = ARGB.colorFromFloat(1.0F, 0.6F, 0.6F, 0.6F);
            lens(consumer, pose, x0, BEZEL_TOP, z0, u0, v0, top, light);
            lens(consumer, pose, x0, BEZEL_TOP, z1, u0, v1, top, light);
            lens(consumer, pose, x1, BEZEL_TOP, z1, u1, v1, top, light);
            lens(consumer, pose, x1, BEZEL_TOP, z0, u1, v0, top, light);
            float su0 = u0 + 0.5F, su1 = u1 + 0.5F;
            lens(consumer, pose, x1, BEZEL_TOP, z0, su1, v0, across, light);
            lens(consumer, pose, x1, BEZEL_FLOOR, z0, su1, v1, across, light);
            lens(consumer, pose, x0, BEZEL_FLOOR, z0, su0, v1, across, light);
            lens(consumer, pose, x0, BEZEL_TOP, z0, su0, v0, across, light);
            lens(consumer, pose, x0, BEZEL_TOP, z1, su0, v0, across, light);
            lens(consumer, pose, x0, BEZEL_FLOOR, z1, su0, v1, across, light);
            lens(consumer, pose, x1, BEZEL_FLOOR, z1, su1, v1, across, light);
            lens(consumer, pose, x1, BEZEL_TOP, z1, su1, v0, across, light);
            float left = su0, leftEnd = su0 + 1.0F / 32.0F, right = su1 - 1.0F / 32.0F;
            lens(consumer, pose, x0, BEZEL_TOP, z0, left, v0, along, light);
            lens(consumer, pose, x0, BEZEL_FLOOR, z0, left, v1, along, light);
            lens(consumer, pose, x0, BEZEL_FLOOR, z1, leftEnd, v1, along, light);
            lens(consumer, pose, x0, BEZEL_TOP, z1, leftEnd, v0, along, light);
            lens(consumer, pose, x1, BEZEL_TOP, z1, su1, v0, along, light);
            lens(consumer, pose, x1, BEZEL_FLOOR, z1, su1, v1, along, light);
            lens(consumer, pose, x1, BEZEL_FLOOR, z0, right, v1, along, light);
            lens(consumer, pose, x1, BEZEL_TOP, z0, right, v0, along, light);
        }
    }

    /** Whether a lens's glass is nearly white already, so light added over it would wash its detail out. */
    private static boolean paleGlass(@Nullable Lens lens) {
        return lens == Lens.CLEAR || lens == Lens.FROST;
    }

    /** A fitted lens's glass breathing while it's lit, 0 to 1, each pedestal in its own time. */
    private static float lensBreath(State state) {
        return 0.5F + 0.5F * Mth.sin((state.time + (state.blockPos.hashCode() & 0xFF)) * Mth.TWO_PI / LENS_BREATH_TICKS);
    }

    /** The empty socket's soft glow while the When Powered option has the pedestal showing, breathing gently and easing in and out. */
    private static void submitSocketGlow(State state) {
        if (state.socketGlow <= 0.0F) return;
        float breath = 0.82F + 0.18F * Mth.sin(state.socketTime * Mth.TWO_PI / BREATH_TICKS);
        // Added to what's under it, so it dims evenly all the way out; a cut-out entity layer would drop its faint pixels early.
        float strength = 1.0F + (breath - 1.0F) * state.socketGlow;
        int argb = ARGB.color(Math.round(255.0F * strength * state.socketGlow), 0xFFFFFF);
        PedestalLight.world(state.originX, state.originY, state.originZ, state.worldScale)
                .flat(PedestalLight.Sprite.SOCKET_GLOW, SOCKET_GLOW_Y, 0.25F, 0.0F, argb);
        PedestalLight.world(state.originX, state.originY, state.originZ, state.worldScale)
                .flat(PedestalLight.Sprite.SOCKET_GLOW, SOCKET_GLOW_Y + 0.002F, 0.25F, 0.0F, ARGB.color(Math.round(120.0F * strength * state.socketGlow), 0xFFFFFF));
    }

    private static void glass(VertexConsumer consumer, PoseStack.Pose pose, int argb, int light) {
        lensRows(consumer, pose, GLASS_TOP - 0.001F, argb, light);
    }

    private static void litGlass(VertexConsumer consumer, PoseStack.Pose pose, int argb) {
        lensRows(consumer, pose, GLASS_TOP, argb, FULL_BRIGHT);
    }

    private static void lensRows(VertexConsumer consumer, PoseStack.Pose pose, float y, int argb, int light) {
        for (int[] row : LENS_ROWS) {
            float z0 = (4.0F + row[0] * 0.5F) / 16.0F, z1 = z0 + 0.5F / 16.0F;
            float x0 = (4.0F + row[1] * 0.5F) / 16.0F, x1 = (4.0F + (row[2] + 1) * 0.5F) / 16.0F;
            float u0 = row[1] / 16.0F, u1 = (row[2] + 1) / 16.0F, v0 = row[0] / 16.0F, v1 = (row[0] + 1) / 16.0F;
            lens(consumer, pose, x0, y, z0, u0, v0, argb, light);
            lens(consumer, pose, x0, y, z1, u0, v1, argb, light);
            lens(consumer, pose, x1, y, z1, u1, v1, argb, light);
            lens(consumer, pose, x1, y, z0, u1, v0, argb, light);
        }
    }

    public static void lens(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb, int light) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
    }
}
