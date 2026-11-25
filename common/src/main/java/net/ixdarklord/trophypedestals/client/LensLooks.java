package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.coolcatcanvas.api.client.vfx.Curve;
import net.ixdarklord.coolcatcanvas.api.client.vfx.ParticleSpec;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldParticles;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.pedestal.Lens;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LensLooks {
    private static final Identifier MOTE = TrophyPedestalsMod.id("textures/vfx/halo.png");
    private static final Identifier BUBBLE = TrophyPedestalsMod.id("textures/vfx/bubble.png");
    private static final Identifier SPARKLE = TrophyPedestalsMod.id("textures/vfx/sparkle.png");
    private static final double REACH = 32.0, LENS_TOP = 14.25 / 16.0, CAP_EDGE = 0.31, CAP_RIM = 0.96, SPARK_SPREAD = 0.19, PORTAL_LIFT = 0.15;
    private static final Identifier TWINKLE = TrophyPedestalsMod.id("textures/vfx/twinkle.png");
    private static final ParticleSpec GLINT = ParticleSpec.builder(VfxMaterial.luminous(TWINKLE).withSmoothing(false))
            .lifetime(14, 26).size(Curve.of(0.0F, 0.06F, 0.0F)).opacity(Curve.constant(0.95F))
            .color(0xFFFFFF, 0xFFE6BE).spin(Curve.constant(0.03F)).randomRoll().drag(0.9F).build();
    private static final Map<Integer, ParticleSpec> SPARKLES = new ConcurrentHashMap<>();
    private static final ParticleSpec PIXELS = ParticleSpec.builder(VfxMaterial.luminous(MOTE).withSmoothing(false))
            .lifetime(18, 30).size(Curve.of(0.018F, 0.014F, 0.0F)).opacity(Curve.FADE.scaled(0.85F))
            .color(0xE6FFFF, 0x3FE8FF).build();
    private static final Map<Integer, PedestalRarity> LIGHTS = new ConcurrentHashMap<>();
    private static final String LIGHT_PREFIX = "lens_";
    private static final int MAX_CACHED = 256;
    private static final int PRISM_STEPS = 90, EMBER_STEPS = 8;
    static final float PRISM_TICKS = 160.0F;
    private static final Identifier SNOWFLAKE = TrophyPedestalsMod.id("textures/vfx/snowflake.png");
    private static final ParticleSpec SPARK = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.STREAK))
            .shape(ParticleSpec.Shape.SPARK).stretch(2.2F).lifetime(10, 20).size(Curve.of(0.014F, 0.011F, 0.0F))
            .opacity(Curve.FADE).color(0xFFFFE6, 0xFF7A1A).gravity(0.0035F).drag(0.97F).build();
    private static final ParticleSpec EMBER_SMOKE = ParticleSpec.builder(VfxMaterial.alpha(VfxMaterial.SMOKE))
            .lifetime(60, 95).size(Curve.of(0.07F, 0.2F, 0.34F)).opacity(Curve.of(0.0F, 0.36F, 0.0F))
            .color(0x5E5650, 0x2A2826).spin(Curve.constant(0.018F)).randomRoll().gravity(-0.0004F).drag(0.965F).build();
    private static final ParticleSpec ASH = ParticleSpec.builder(VfxMaterial.alpha(TrophyPedestalsMod.id("textures/vfx/ash.png")).withSmoothing(false))
            .lifetime(50, 85).size(Curve.of(0.012F, 0.026F, 0.018F)).opacity(Curve.of(0.0F, 0.95F, 0.0F))
            .color(0xFF9A50, 0x6E6A66).spin(Curve.constant(0.05F)).randomRoll().gravity(0.0007F).drag(0.97F).build();
    private static final ParticleSpec SNOW = ParticleSpec.builder(VfxMaterial.luminous(SNOWFLAKE).withSmoothing(false))
            .lifetime(70, 110).size(Curve.of(0.026F, 0.04F, 0.03F)).opacity(Curve.FADE.scaled(0.95F))
            .color(0xFFFFFF, 0xCDEEFF).spin(Curve.constant(0.035F)).randomRoll().gravity(0.0003F).drag(0.985F).build();
    private static final ParticleSpec FROST_MIST = ParticleSpec.builder(VfxMaterial.alpha(TrophyPedestalsMod.id("textures/vfx/cold_wisp.png")))
            .lifetime(40, 62).size(Curve.of(0.06F, 0.16F, 0.27F)).opacity(Curve.of(0.0F, 0.58F, 0.0F))
            .color(0xFFFFFF, 0xA8DCF8).spin(Curve.constant(0.022F)).randomRoll().gravity(0.001F).drag(0.965F).build();
    private static final ParticleSpec DUST_MOTE = ParticleSpec.builder(VfxMaterial.luminous(MOTE).withSmoothing(false))
            .lifetime(40, 70).size(Curve.of(0.0F, 0.02F, 0.008F)).opacity(Curve.of(0.0F, 0.8F, 0.0F))
            .color(0xF4EEDF, 0xB8B0A0).gravity(-0.00005F).drag(0.98F).build();
    private static final int BLOOD_RISE_TICKS = 42;
    private static final float BLOOD_RISE_GRAVITY = -0.0004F, BLOOD_RISE_DRAG = 0.96F;
    private static final Map<Integer, ParticleSpec> BLOOD_DROPS = new ConcurrentHashMap<>();
    private static final ParticleSpec RAVEN_FEATHER = ParticleSpec.builder(VfxMaterial.alpha(TrophyPedestalsMod.id("textures/vfx/raven_feather.png")).withSmoothing(false))
            .lifetime(45, 75).size(Curve.constant(0.05F)).opacity(Curve.of(0.0F, 1.0F, 0.0F))
            .color(0xFFFFFF, 0xC8C8D0).spin(Curve.constant(0.07F)).randomRoll().gravity(0.0006F).drag(0.96F).build();
    private static final ParticleSpec GOLD_FALL = ParticleSpec.builder(VfxMaterial.luminous(TrophyPedestalsMod.id("textures/vfx/twinkle.png")).withSmoothing(false))
            .lifetime(34, 56).size(Curve.of(0.0F, 0.05F, 0.012F)).opacity(Curve.constant(0.95F))
            .color(0xFFF4C0, 0xFFB83A).spin(Curve.constant(0.07F)).randomRoll().gravity(0.0005F).drag(0.97F).build();
    private static final ParticleSpec ICE_DUST = ParticleSpec.builder(VfxMaterial.luminous(TrophyPedestalsMod.id("textures/vfx/twinkle.png")).withSmoothing(false))
            .lifetime(22, 40).size(Curve.of(0.0F, 0.038F, 0.0F)).opacity(Curve.constant(0.95F))
            .color(0xFFFFFF, 0x9FE0FF).spin(Curve.constant(0.05F)).randomRoll().gravity(0.0007F).drag(0.96F).build();
    private static final ParticleSpec BUBBLES = ParticleSpec.builder(VfxMaterial.luminous(BUBBLE).withSmoothing(false))
            .lifetime(40, 60).size(Curve.of(0.025F, 0.04F, 0.05F)).opacity(Curve.FADE.scaled(0.8F))
            .color(0xE6FFFB, 0x6FE3D8).drag(0.98F).build();

    private LensLooks() {}

    private static ParticleSpec bloodDrop(boolean bead, double size) {
        int step = Math.max(1, (int) Math.round(size * 10.0));
        return BLOOD_DROPS.computeIfAbsent(step * 2 + (bead ? 1 : 0), key -> {
            float scale = step / 10.0F;
            Curve sizes = bead ? Curve.of(0.012F, 0.016F, 0.005F) : Curve.of(0.022F, 0.028F, 0.008F);
            return ParticleSpec.builder(VfxMaterial.alpha(TrophyPedestalsMod.id(bead ? "textures/vfx/blood_bead.png" : "textures/vfx/blood_drop.png")).withSmoothing(false))
                    .lifetime(bead ? BLOOD_RISE_TICKS - 8 : BLOOD_RISE_TICKS, BLOOD_RISE_TICKS).size(sizes.scaled(scale))
                    .opacity(new Curve(0.95F, 0.95F, 0.0F, Easing.LINEAR, Easing.QUAD_IN))
                    .color(0xFFFFFF, 0x8A5A60).gravity(BLOOD_RISE_GRAVITY * scale).drag(BLOOD_RISE_DRAG).build();
        });
    }

    public static @Nullable Lens lens(PedestalBlockEntity pedestal) {
        if (pedestal.isTrophy() || pedestal.leaderboard() != null) return null;
        return Lens.of(pedestal.lens());
    }

    public static boolean effects() {
        return TrophyPedestalsClientConfig.LENS_EFFECTS.get();
    }

    public static @Nullable PedestalRarity light(Lens lens, PedestalBlockEntity pedestal, float time) {
        return switch (lens) {
            case CLEAR -> Lens.isDyed(pedestal.lens()) ? light(Lens.dyed(pedestal.lens())) : null;
            case PRISM -> light(prism(time, 0.0F));
            case APPRAISAL -> light(rarityColor(appraised(pedestal)));
            case EMBER -> {
                int step = (int) (flicker(time, pedestal.getBlockPos()) * EMBER_STEPS);
                yield light(ARGB.scaleRGB(lens.color, 0.72F + 0.28F * step / EMBER_STEPS) & 0xFFFFFF);
            }
            default -> light(lens.color);
        };
    }

    static final int[] RARITY_LIGHT = {0xFFFFFF, 0xFFFF55, 0x55FFFF, 0xFF55FF};

    static int rarityColor(ItemStack shown) {
        if (shown.isEmpty()) return RARITY_LIGHT[0];
        int ordinal = shown.getRarity().ordinal();
        if (ordinal < RARITY_LIGHT.length) return RARITY_LIGHT[ordinal];
        TextColor styled = shown.getStyledHoverName().getStyle().getColor();
        if (styled != null) return styled.getValue() & 0xFFFFFF;
        Integer named = shown.getRarity().color().getColor();
        return named != null ? named & 0xFFFFFF : RARITY_LIGHT[3];
    }

    public static ItemStack appraised(PedestalBlockEntity pedestal) {
        if (!pedestal.mannequin()) return pedestal.display();
        ItemStack rarest = ItemStack.EMPTY;
        for (EquipmentSlot slot : PedestalBlockEntity.OUTFIT_SLOTS) {
            ItemStack worn = pedestal.outfit(slot);
            if (!worn.isEmpty() && (rarest.isEmpty() || worn.getRarity().ordinal() > rarest.getRarity().ordinal())) rarest = worn;
        }
        return rarest;
    }

    public static int tier(ItemStack shown) {
        return shown.isEmpty() ? 0 : Math.min(shown.getRarity().ordinal(), 3);
    }

    public static int prism(float time, float offset) {
        int step = (int) ((((time / PRISM_TICKS + offset) % 1.0F) + 1.0F) % 1.0F * PRISM_STEPS);
        return Mth.hsvToRgb(step / (float) PRISM_STEPS, 0.6F, 1.0F) & 0xFFFFFF;
    }

    public static float flicker(float time, BlockPos pos) {
        float t = time + (pos.hashCode() & 0xFF);
        // Two mismatched sines summed for an irregular flicker.
        return Mth.clamp(0.55F + 0.25F * Mth.sin(t * 0.9F) + 0.2F * Mth.sin(t * 2.3F + 1.3F), 0.0F, 0.999F);
    }

    private static PedestalRarity light(int color) {
        if (LIGHTS.size() > MAX_CACHED) LIGHTS.clear();
        return LIGHTS.computeIfAbsent(color, rgb -> PedestalRarity.builder(TrophyPedestalsMod.id(LIGHT_PREFIX + Integer.toHexString(rgb)), 0, rgb,
                ARGB.srgbLerp(0.55F, ARGB.opaque(rgb), 0xFFFFFFFF) & 0xFFFFFF).build());
    }

    public static boolean isLight(PedestalRarity rarity) {
        return rarity.id().getNamespace().equals(TrophyPedestalsMod.MOD_ID) && rarity.id().getPath().startsWith(LIGHT_PREFIX);
    }

    public static void tick(Level level, BlockPos pos, PedestalBlockEntity pedestal) {
        Lens lens = lens(pedestal);
        if (lens == null || !effects() || pedestal.glass() || !pedestal.on() || !PedestalRenderer.flourishes(pedestal)) return;
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        if (status == ParticleStatus.MINIMAL) return;
        float rate = status == ParticleStatus.ALL ? 1.0F : 0.5F;
        double size = pedestal.scale(), height = pedestal.height();
        float around = rate * (float) Mth.clamp(size, 1.0, 4.0);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.95 + height, z = pos.getZ() + 0.5;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.distanceToSqr(x, y, z) > (REACH + 2.0 * size) * (REACH + 2.0 * size)) return;
        float time = level.getGameTime();
        RandomSource random = level.getRandom();
        double a = random.nextDouble() * Math.PI * 2.0, cos = Math.cos(a), sin = Math.sin(a);
        switch (lens) {
            case CLEAR -> {
                if (random.nextFloat() < 0.3F * around) {
                    double up = random.nextDouble(), r = random.nextDouble() * (0.18 + 0.14 * up) * size;
                    WorldParticles.spawn(sized(Lens.isDyed(pedestal.lens()) ? glint(Lens.dyed(pedestal.lens())) : GLINT, size), x + cos * r, y + (0.05 + up * 0.7) * size, z + sin * r, 0.0, 0.002 * size, 0.0);
                }
            }
            case SANGUINE -> {
                if (random.nextFloat() < 0.36F * rate) {
                    double r = 0.2 + random.nextDouble() * 0.04, px = x + cos * r, py = pos.getY() + LENS_TOP, pz = z + sin * r;
                    double out = (0.001 + random.nextDouble() * 0.002) * size, up = (0.018 + random.nextDouble() * 0.01) * size;
                    WorldParticles.spawn(bloodDrop(random.nextFloat() < 0.6F, size), px, py, pz, cos * out, up, sin * out);
                    if (random.nextFloat() < 0.35F) WorldParticles.spawn(bloodDrop(true, size), px - sin * 0.02 * size, py - 0.03 * size, pz + cos * 0.02 * size, cos * out, up * 0.85, sin * out);
                }
                if (random.nextFloat() < 0.05F * around) {
                    double r = (0.2 + random.nextDouble() * 0.25) * size;
                    WorldParticles.spawn(sized(RAVEN_FEATHER, size), x + cos * r, y + (0.75 + random.nextDouble() * 0.3) * size, z + sin * r, -sin * 0.006 * size, -0.004 * size, cos * 0.006 * size);
                }
            }
            case PRISM -> {
                if (random.nextFloat() < 0.35F * around) {
                    double r = (0.25 + random.nextDouble() * 0.15) * size;
                    WorldParticles.spawn(sized(sparkle(prism(time, random.nextFloat())), size), x + cos * r, y + (0.2 + random.nextDouble() * 0.5) * size, z + sin * r,
                            -sin * 0.01 * size, 0.004 * size, cos * 0.01 * size);
                }
            }
            case HOLOGRAM -> {
                if (random.nextFloat() < 0.4F * around) {
                    double sx = (random.nextDouble() - 0.5) * 0.4 * size, sz = (random.nextDouble() - 0.5) * 0.4 * size;
                    WorldParticles.spawn(sized(PIXELS, size), x + sx, pos.getY() + LENS_TOP + 0.08, z + sz, 0.0, (0.02 + random.nextDouble() * 0.015) * size, 0.0);
                }
            }
            case TIDE -> {
                if (random.nextFloat() < 0.3F * around) {
                    double r = (0.05 + random.nextDouble() * 0.17) * size;
                    WorldParticles.spawn(sized(BUBBLES, size), x + cos * r, pos.getY() + LENS_TOP + 0.06, z + sin * r, 0.0, (0.012 + random.nextDouble() * 0.008) * size, 0.0);
                }
            }
            case APPRAISAL -> {
                int tier = tier(appraised(pedestal));
                int color = rarityColor(appraised(pedestal));
                if (tier == 3 && random.nextFloat() < 0.4F * around) {
                    double r = random.nextDouble() * 0.46 * size;
                    WorldParticles.spawn(sized(GOLD_FALL, size), x + cos * r, y + (0.85 + random.nextDouble() * 0.4) * size, z + sin * r,
                            (random.nextDouble() - 0.5) * 0.006 * size, -0.005 * size, (random.nextDouble() - 0.5) * 0.006 * size);
                }
                if (tier == 3 && random.nextFloat() < 0.42F * around) {
                    double up = random.nextDouble(), r = random.nextDouble() * (0.18 + 0.14 * up) * size;
                    WorldParticles.spawn(sized(glint(color), size), x + cos * r, y + (0.05 + up * 0.7) * size, z + sin * r, 0.0, 0.003 * size, 0.0);
                }
                if (tier == 3 && random.nextFloat() < 0.35F * around) {
                    double r = (0.22 + random.nextDouble() * 0.12) * size;
                    WorldParticles.spawn(sized(sparkle(color), size), x + cos * r, y + (0.1 + random.nextDouble() * 0.4) * size, z + sin * r,
                            -sin * 0.006 * size, 0.008 * size, cos * 0.006 * size);
                }
                // Square root of a uniform value gives an even spread over a widening disc.
                double up = random.nextDouble(), spot = Math.sqrt(random.nextDouble()) * (0.16 + 0.2 * up) * size;
                double px = x + cos * spot, pz = z + sin * spot;
                if (tier == 0 && random.nextFloat() < 0.4F * around) {
                    WorldParticles.spawn(sized(DUST_MOTE, size), px, y + (0.05 + up * 0.8) * size, pz, -sin * 0.003 * size, 0.004 * size, cos * 0.003 * size);
                }
            }
            case EMBER -> {
                double lensTop = pos.getY() + LENS_TOP + 0.02;
                for (int spark = 0; spark < 2; spark++) {
                    if (random.nextFloat() >= 0.4F * rate) continue;
                    double b = random.nextDouble() * Math.PI * 2.0, from = Math.sqrt(random.nextDouble()) * SPARK_SPREAD;
                    double bx = Math.cos(b), bz = Math.sin(b), lean = (0.006 + random.nextDouble() * 0.026) * size;
                    double wobble = random.nextDouble() * Math.PI * 2.0, drift = random.nextDouble() * 0.012 * size;
                    WorldParticles.spawn(sized(SPARK, size), x + bx * from, lensTop, z + bz * from, bx * lean + Math.cos(wobble) * drift,
                            (0.055 + random.nextDouble() * 0.07) * size, bz * lean + Math.sin(wobble) * drift);
                }
                if (random.nextFloat() < 0.16F * rate) {
                    double from = Math.sqrt(random.nextDouble()) * SPARK_SPREAD;
                    WorldParticles.spawn(sized(EMBER_SMOKE, size), x + cos * from, lensTop + 0.06, z + sin * from,
                            cos * 0.004 * size, (0.016 + random.nextDouble() * 0.012) * size, sin * 0.004 * size);
                }
                if (random.nextFloat() < 0.14F * rate) {
                    double from = Math.sqrt(random.nextDouble()) * SPARK_SPREAD;
                    WorldParticles.spawn(sized(ASH, size), x + cos * from, lensTop + 0.04, z + sin * from,
                            cos * (0.004 + random.nextDouble() * 0.012) * size, (0.03 + random.nextDouble() * 0.024) * size, sin * (0.004 + random.nextDouble() * 0.012) * size);
                }
            }
            case FROST -> {
                if (random.nextFloat() < 0.3F * around) {
                    double r = random.nextDouble() * 0.45 * size;
                    WorldParticles.spawn(sized(SNOW, size), x + cos * r, y + (0.9 + random.nextDouble() * 0.3) * size, z + sin * r,
                            (random.nextDouble() - 0.5) * 0.008 * size, -0.006 * size, (random.nextDouble() - 0.5) * 0.008 * size);
                }
                if (random.nextFloat() < 0.45F * rate) {
                    double dust = random.nextDouble() * Math.PI * 2.0, reach = 0.36 + random.nextDouble() * 0.16;
                    WorldParticles.spawn(ICE_DUST, pos.getX() + 0.5 + Math.cos(dust) * reach, pos.getY() + 0.25 + random.nextDouble() * 0.68,
                            pos.getZ() + 0.5 + Math.sin(dust) * reach, Math.cos(dust) * 0.003 + draughtDust(time), -0.004, Math.sin(dust) * 0.003);
                }
                double draught = Math.sin(time * 0.031) * 0.004, across = Math.cos(time * 0.023) * 0.004;
                for (int puff = 0; puff < 2; puff++) {
                    if (random.nextFloat() >= 0.17F * rate) continue;
                    int side = random.nextInt(4);
                    double along = (random.nextDouble() - 0.5) * 0.66, outX = side == 0 ? 1.0 : side == 1 ? -1.0 : 0.0, outZ = side == 2 ? 1.0 : side == 3 ? -1.0 : 0.0;
                    double px = pos.getX() + 0.5 + outX * CAP_EDGE + (outX == 0.0 ? along : 0.0), pz = pos.getZ() + 0.5 + outZ * CAP_EDGE + (outZ == 0.0 ? along : 0.0);
                    double push = 0.016 + random.nextDouble() * 0.012;
                    WorldParticles.spawn(FROST_MIST, px, pos.getY() + CAP_RIM + random.nextDouble() * 0.04, pz,
                            outX * push + draught, 0.001, outZ * push + across);
                }
            }
            case ENDER -> {
                if (random.nextFloat() < 0.5F * around) {
                    double r = (0.9 + random.nextDouble() * 0.5) * size;
                    level.addParticle(ParticleTypes.ENCHANT, x, y + 0.6 * size, z, cos * r, (random.nextDouble() * 0.7 - 0.1) * size, sin * r);
                }
                double lensY = pos.getY() + LENS_TOP + PORTAL_LIFT;
                for (int i = 0; i < (rate < 1.0F ? 1 : 2); i++) {
                    double b = random.nextDouble() * Math.PI * 2.0, r = (0.55 + random.nextDouble() * 0.45) * size;
                    level.addParticle(ParticleTypes.PORTAL, x, lensY, z, Math.cos(b) * r, (-0.8 + random.nextDouble() * 0.35) * size, Math.sin(b) * r);
                }
            }
        }
    }

    private static final Map<Integer, ParticleSpec> GLINTS = new ConcurrentHashMap<>();

    private static ParticleSpec glint(int color) {
        if (GLINTS.size() > MAX_CACHED) GLINTS.clear();
        return GLINTS.computeIfAbsent(color, rgb -> ParticleSpec.builder(VfxMaterial.luminous(TWINKLE).withSmoothing(false))
                .lifetime(14, 26).size(Curve.of(0.0F, 0.06F, 0.0F)).opacity(Curve.constant(0.95F))
                .color(0xFFFFFF, rgb).spin(Curve.constant(0.03F)).randomRoll().drag(0.9F).build());
    }

    private record SizedSpec(ParticleSpec spec, int step) {}

    private static final Map<SizedSpec, ParticleSpec> SIZED = new ConcurrentHashMap<>();

    private static ParticleSpec sized(ParticleSpec spec, double size) {
        int step = Math.max(1, (int) Math.round(size * 4.0));
        if (step == 4) return spec;
        if (SIZED.size() > 4 * MAX_CACHED) SIZED.clear();
        return SIZED.computeIfAbsent(new SizedSpec(spec, step), key -> {
            float scale = step / 4.0F;
            ParticleSpec.Builder builder = ParticleSpec.builder(spec.material()).shape(spec.shape()).lifetime(spec.minLifetime(), spec.maxLifetime())
                    .size(spec.size().scaled(scale)).opacity(spec.opacity()).color(spec.startColor(), spec.endColor()).colorEasing(spec.colorEasing())
                    .spin(spec.spin()).gravity(spec.gravity() * scale).drag(spec.drag()).stretch(spec.stretch());
            return (spec.randomRoll() ? builder.randomRoll() : builder).build();
        });
    }

    private static double draughtDust(float time) {
        return Math.sin(time * 0.031) * 0.003;
    }

    private static ParticleSpec sparkle(int color) {
        if (SPARKLES.size() > MAX_CACHED) SPARKLES.clear();
        return SPARKLES.computeIfAbsent(color, rgb -> ParticleSpec.builder(VfxMaterial.luminous(SPARKLE).withSmoothing(false))
                .lifetime(20, 34).size(Curve.of(0.0F, 0.05F, 0.0F)).opacity(Curve.FADE.scaled(0.9F))
                .color(0xFFFFFF, rgb).drag(0.96F).build());
    }
}
