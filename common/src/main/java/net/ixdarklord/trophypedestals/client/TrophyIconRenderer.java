package net.ixdarklord.trophypedestals.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.TrophyKinds;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
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
 * {@code trophypedestals:trophy_icon} special item model, which the pedestal items use (with their block model) when they carry
 * a record. In inventory slots it stands still, turned to face the viewer; elsewhere (in hand, on the ground, in an item
 * frame) it stands upright and turns, as over the placed block. The item model picks which with {@code "spin"}.
 */
public final class TrophyIconRenderer implements SpecialModelRenderer<TrophyIconRenderer.Shown> {
    public static final ResourceLocation TYPE = TrophyPedestalsMod.id("trophy_icon");
    // Where the icon floats over the block (its middle) and how big it is, in the block's units; lit full bright.
    private static final float ICON_Y = 1.35F, ICON_SCALE = 0.95F;
    private static final int FULL_BRIGHT = 0xF000F0;

    // How long the icon takes to turn once where it spins, in milliseconds (as over the placed block: 2 degrees a tick).
    private static final long SPIN_MILLIS = 9000L;
    // Its float: how high it rises and sinks, and how slowly (as over the placed block: a sine over 12 ticks a radian).
    private static final float BOB = 0.05F;
    private static final double BOB_MILLIS = 600.0;

    private final ItemStackRenderState icon = new ItemStackRenderState();
    // The mobs made to draw mob trophy items with, the most recently drawn kept (enough for a creative tab of every mob,
    // even with mods': with fewer, a full tab made each mob again every frame); the mobs that
    // can't be drawn outside a world; and a camera for drawing them, which only a name tag would look at.
    private static final Map<MobTrophyRecord, Entity> MOBS = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<MobTrophyRecord, Entity> eldest) {
            return size() > MAX_MOBS;
        }
    };
    private static final Set<EntityType<?>> UNDRAWABLE = ConcurrentHashMap.newKeySet();
    private static final int MAX_MOBS = 512;
    private static final CameraRenderState CAMERA = new CameraRenderState();
    // The moment a slot's showcase light is drawn at: a slot's trophy is drawn once and kept (see Shown), so nothing in it
    // moves, as its mob doesn't.
    private static final float SLOT_TIME = 0.0F;
    // How a mob is turned in a slot.
    private static final float MOB_SLOT_YAW = 180.0F;
    private final boolean spin;

    private TrophyIconRenderer(boolean spin) {
        this.spin = spin;
    }

    public static void init() {
        SpecialModelRendererRegistry.register(TYPE, Unbaked.MAP_CODEC);
    }

    /**
     * What a trophy item shows over its pedestal: its icon (for a mob trophy, the mob's egg, shown only if the mob can't
     * be drawn), its rank (whose color its showcase light takes), and a mob trophy's record (whose mob is drawn).
     */
    public record Shown(ItemStack icon, @Nullable ResourceLocation rarity, @Nullable MobTrophyRecord mob) {
        // Equal when they show the same (a new icon stack is made for each look), so the inventory draws a slot's trophy
        // once and keeps it, instead of drawing every trophy in sight (a mob each, for mob trophies) every frame.
        @Override
        public boolean equals(Object other) {
            return other instanceof Shown shown && ItemStack.matches(icon, shown.icon) && Objects.equals(rarity, shown.rarity) && Objects.equals(mob, shown.mob);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ItemStack.hashItemAndComponents(icon), rarity, mob);
        }
    }

    @Override
    public @Nullable Shown extractArgument(ItemStack stack) {
        PedestalRecord record = stack.get(Pedestals.RECORD.get());
        if (record != null) return new Shown(TrophyKinds.icon(record, true), record.rarity(), null);
        // A mob trophy's mob itself, or its spawn egg (or a spawner, for a mob with none) if it can't be drawn.
        MobTrophyRecord mob = stack.get(Pedestals.MOB_TROPHY.get());
        return mob == null ? null : new Shown(mob.icon(), mob.rarity(), mob);
    }

    @Override
    public void submit(@Nullable Shown trophy, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outlineColor) {
        if (trophy == null || trophy.icon().isEmpty()) return;
        ItemStack shown = trophy.icon();
        // Its showcase light rising out of the well, as on the placed trophy.
        // (Its rising outlines too where it turns, in hand: not in a slot.)
        PedestalRenderer.submitItemLight(poseStack, collector, trophy.rarity(), spin ? Util.getMillis() / 50.0F : SLOT_TIME, spin, ICON_Y);
        Minecraft minecraft = Minecraft.getInstance();
        if (trophy.mob() != null && submitMob(trophy.mob(), poseStack, collector)) return;
        ItemModelResolver.INSTANCE.updateForTopItem(icon, shown, ItemDisplayContext.GROUND, minecraft.level, null, 0);
        poseStack.pushPose();
        poseStack.translate(0.5F, ICON_Y, 0.5F);
        if (spin) {
            // Floating up and down as it turns, as over the placed block.
            long now = Util.getMillis();
            // (The clock is wrapped first: in milliseconds it's too large for a float to keep.)
            poseStack.translate(0.0F, (float) Math.sin(now / BOB_MILLIS) * BOB, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(now % SPIN_MILLIS * 360.0F / SPIN_MILLIS));
        } else {
            // Turned to face the viewer in the inventory's view of the block, which turns it 225 degrees and tips it back 30
            // (edge on it would vanish). The trophy model lights itself from the front there ("gui_light": "front"), so
            // the icon's face takes the inventory's light rather than being shaded as a block's side.
            poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
        }
        poseStack.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);
        poseStack.translate(0.0F, -0.15F, 0.0F);
        icon.submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, outlineColor);
        poseStack.popPose();
    }

    // A mob trophy's mob, as over the placed trophy: as it looked, standing over the well, shrunk to fit, full bright and
    // turning (or turned to face the viewer in slots). False if it can't be drawn (no world yet, or a mod's mob that
    // won't make or draw outside one), when its icon shows instead.
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
            // Still in a slot (its pose at its first moment); moving in hand.
            shown.ageInTicks = spin ? now / 50.0F : 0.0F;
            PedestalRenderer.idle(shown, shown.ageInTicks);
            // (In a slot, small enough to stay inside it.)
            float scale = spin ? PedestalRenderer.fitScale(mob) : PedestalRenderer.slotScale(mob);
            poseStack.pushPose();
            poseStack.translate(0.5F, PedestalRenderer.mobFloor(), 0.5F);
            if (spin) {
                poseStack.translate(0.0F, (float) Math.sin(now / BOB_MILLIS) * BOB, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(now % SPIN_MILLIS * 360.0F / SPIN_MILLIS));
            } else {
                // Turned three quarters towards the viewer, looking out to the lower right, in the inventory's view of the
                // block (which turns it 225 degrees; a mob faces south unturned).
                poseStack.mulPose(Axis.YP.rotationDegrees(MOB_SLOT_YAW + PedestalRenderer.modelTurn(shown)));
            }
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0.0F, -PedestalRenderer.sink(shown), 0.0F);
            EntityRenderStates.submit(shown, CAMERA, 0.0, 0.0, 0.0, poseStack, collector);
            poseStack.popPose();
            return true;
        } catch (RuntimeException | LinkageError e) {
            UNDRAWABLE.add(type);
            MOBS.remove(trophy);
            TrophyPedestalsMod.LOGGER.warn("Can't draw {} on a trophy pedestal item; its icon shows instead", trophy.entity(), e);
            return false;
        }
    }

    // Within the block, so inventory slots don't treat it as oversized: the trophy model's own view in them is shrunk to
    // leave room for the icon over it.
    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0.0F, 0.0F, 0.0F));
        output.accept(new Vector3f(1.0F, 1.0F, 1.0F));
    }

    /** @param spin whether the icon turns (in hand and the like) or stands still facing the viewer (in inventory slots) */
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
