package net.ixdarklord.trophypedestals.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.TrophyKinds;
import net.ixdarklord.trophypedestals.compat.component.Components;
import net.ixdarklord.trophypedestals.compat.render.CameraRenderState;
import net.ixdarklord.trophypedestals.compat.render.EntityRenderState;
import net.ixdarklord.trophypedestals.compat.render.EntityRenderStates;
import net.ixdarklord.trophypedestals.compat.render.ItemModelResolver;
import net.ixdarklord.trophypedestals.compat.render.ItemStackRenderState;
import net.ixdarklord.trophypedestals.compat.render.SpecialModelRenderer;
import net.ixdarklord.trophypedestals.compat.render.SpecialModelRendererRegistry;
import net.ixdarklord.trophypedestals.compat.render.SubmitNodeCollector;
import net.ixdarklord.trophypedestals.pedestal.MobLooks;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.PedestalRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A trophy pedestal item's icon, floating over the pedestal's block model as it does on the placed block: the
 * {@code trophypedestals:trophy_icon} special item model, which the pedestal items use (with their block model) when
 * they carry a record.
 */
public final class TrophyIconRenderer implements SpecialModelRenderer<TrophyIconRenderer.Shown> {
    public static final ResourceLocation TYPE = TrophyPedestalsMod.id("trophy_icon");
    private static final float ICON_Y = 1.35F, ICON_SCALE = 0.95F;
    private static final int FULL_BRIGHT = 0xF000F0;

    private static final long SPIN_MILLIS = 9000L;
    private static final float BOB = 0.05F;
    private static final double BOB_MILLIS = 600.0;

    private final ItemStackRenderState icon = new ItemStackRenderState();
    private static final Map<MobTrophyRecord, Entity> MOBS = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<MobTrophyRecord, Entity> eldest) {
            return size() > MAX_MOBS;
        }
    };
    private static final Set<EntityType<?>> UNDRAWABLE = ConcurrentHashMap.newKeySet();

    /** Lets go of the mobs kept for drawing, which would otherwise keep the world they were made in loaded after leaving it. */
    public static void forgetMobs() {
        MOBS.clear();
    }

    private static final int MAX_MOBS = 512;
    private static final CameraRenderState CAMERA = new CameraRenderState();
    private static final float SLOT_TIME = 0.0F;
    private static final float MOB_SLOT_YAW = 180.0F;
    private final boolean spin;

    private TrophyIconRenderer(boolean spin) {
        this.spin = spin;
    }

    public static void init() {
        SpecialModelRendererRegistry.register(TYPE, Unbaked.MAP_CODEC);
    }

    public record Shown(ItemStack icon, @Nullable ResourceLocation rarity, @Nullable MobTrophyRecord mob) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Shown shown && ItemStack.matches(icon, shown.icon) && Objects.equals(rarity, shown.rarity) && Objects.equals(mob, shown.mob);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Objects.hash(icon.getItem(), icon.getTag()), rarity, mob);
        }
    }

    @Override
    public @Nullable Shown extractArgument(ItemStack stack) {
        PedestalRecord record = Components.get(stack, Pedestals.RECORD.get());
        if (record != null) return new Shown(TrophyKinds.icon(record, true), record.rarity(), null);
        MobTrophyRecord mob = Components.get(stack, Pedestals.MOB_TROPHY.get());
        return mob == null ? null : new Shown(mob.icon(), mob.rarity(), mob);
    }

    @Override
    public void submit(@Nullable Shown trophy, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outlineColor) {
        if (trophy == null || trophy.icon().isEmpty()) return;
        ItemStack shown = trophy.icon();
        PedestalRenderer.submitItemLight(poseStack, collector, trophy.rarity(), spin ? Util.getMillis() / 50.0F : SLOT_TIME, spin, ICON_Y);
        Minecraft minecraft = Minecraft.getInstance();
        if (trophy.mob() != null && submitMob(trophy.mob(), poseStack, collector)) return;
        ItemModelResolver.INSTANCE.updateForTopItem(icon, shown, ItemDisplayContext.GROUND, minecraft.level, null, 0);
        poseStack.pushPose();
        poseStack.translate(0.5F, ICON_Y, 0.5F);
        if (spin) {
            long now = Util.getMillis();
            poseStack.translate(0.0F, (float) Math.sin(now / BOB_MILLIS) * BOB, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(now % SPIN_MILLIS * 360.0F / SPIN_MILLIS));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
        }
        poseStack.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);
        poseStack.translate(0.0F, -0.15F, 0.0F);
        icon.submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, outlineColor);
        poseStack.popPose();
    }

    private boolean submitMob(MobTrophyRecord trophy, PoseStack poseStack, SubmitNodeCollector collector) {
        Minecraft minecraft = Minecraft.getInstance();
        EntityType<?> type = trophy.type();
        if (minecraft.level == null || type == null || UNDRAWABLE.contains(type)) return false;
        try {
            Entity mob = MOBS.get(trophy);
            if (mob == null || mob.level() != minecraft.level) {
                mob = EntityRenderStates.create(type, minecraft.level);
                if (mob == null) return false;
                DrawnMobs.numbered(mob);
                MobLooks.apply(mob, trophy.look(), minecraft.level.registryAccess());
                MOBS.put(trophy, mob);
            }
            long now = Util.getMillis();
            EntityRenderState shown = EntityRenderStates.extract(mob, 0.0F);
            shown.lightCoords = FULL_BRIGHT;
            shown.shadowRadius = 0.0F;
            shown.shadowPieces.clear();
            shown.nameTag = null;
            shown.ageInTicks = spin ? now / 50.0F : 0.0F;
            PedestalRenderer.idle(shown, shown.ageInTicks);
            float scale = spin ? PedestalRenderer.fitScale(mob) : PedestalRenderer.slotScale(mob);
            float turn = PedestalRenderer.modelTurn(shown), raise = PedestalRenderer.raise(mob, shown);
            poseStack.pushPose();
            poseStack.translate(0.5F, PedestalRenderer.mobFloor(), 0.5F);
            if (spin) {
                poseStack.translate(0.0F, (float) Math.sin(now / BOB_MILLIS) * BOB, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(now % SPIN_MILLIS * 360.0F / SPIN_MILLIS));
            } else {
                poseStack.mulPose(Axis.YP.rotationDegrees(MOB_SLOT_YAW + turn));
            }
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0.0F, raise, 0.0F);
            boolean drawn = DrawnMobs.submit(shown, CAMERA, poseStack, collector);
            poseStack.popPose();
            if (!drawn) {
                UNDRAWABLE.add(type);
                MOBS.remove(trophy);
            }
            return drawn;
        } catch (RuntimeException | LinkageError e) {
            UNDRAWABLE.add(type);
            MOBS.remove(trophy);
            TrophyPedestalsMod.LOGGER.warn("Can't draw {} on a trophy pedestal item; its icon shows instead", trophy.entity(), e);
            return false;
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0.0F, 0.0F, 0.0F));
        output.accept(new Vector3f(1.0F, 1.0F, 1.0F));
    }

    public record Unbaked(boolean spin) implements SpecialModelRenderer.Unbaked<Shown> {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("spin", false).forGetter(Unbaked::spin)
        ).apply(instance, Unbaked::new));

        @Override
        public SpecialModelRenderer<Shown> bake(SpecialModelRenderer.BakingContext context) {
            return new TrophyIconRenderer(spin);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
