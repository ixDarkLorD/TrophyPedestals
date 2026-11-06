package net.ixdarklord.trophypedestals.neoforge.compat.kubejs;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalBlocks;
import net.ixdarklord.trophypedestals.api.PedestalSettings;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * What server scripts see as {@code TrophyPedestals}: a pedestal's settings, read, changed or locked against players,
 * as {@code PedestalSettings} does for Java mods.
 * <pre>{@code
 * TrophyPedestals.forcePedestal(level, [10, 64, -3], { scale: 2, spins: false, animation: 'walk' }, 'This trophy is part of the museum')
 * TrophyPedestals.lockPedestal(level, [10, 64, -3], null)   // refuse silently
 * }</pre>
 */
public final class TrophyPedestalsBinding {
    public static final TrophyPedestalsBinding INSTANCE = new TrophyPedestalsBinding();

    private TrophyPedestalsBinding() {}

    /** Whether there's a pedestal at the position. */
    public boolean isPedestal(Level level, BlockPos pos) {
        return PedestalBlocks.pedestalAt(level, pos).isPresent();
    }

    /**
     * A pedestal's settings, by name: textMode, scale, height, yaw, pitch, roll, spins, animates, effects, sounds, textHeight, textScale, crown,
     * redstone, statue, statuePose, animation (and locked); empty if there's no pedestal.
     */
    public Map<String, Object> pedestalSettings(Level level, BlockPos pos) {
        Optional<PedestalBlockEntity> pedestal = PedestalBlocks.pedestalAt(level, pos);
        if (pedestal.isEmpty()) return Map.of();
        PedestalSettings.Settings settings = PedestalSettings.get(pedestal.get());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("textMode", settings.textMode().getSerializedName());
        map.put("scale", settings.scale());
        map.put("height", settings.height());
        map.put("yaw", settings.yaw());
        map.put("pitch", settings.pitch());
        map.put("roll", settings.roll());
        map.put("spins", settings.spins());
        map.put("animates", settings.animates());
        map.put("effects", settings.effects());
        map.put("sounds", settings.sounds());
        map.put("textHeight", settings.textHeight());
        map.put("textScale", settings.textScale());
        map.put("crown", settings.crown());
        map.put("light", settings.light());
        map.put("redstone", settings.redstone().getSerializedName());
        map.put("statue", settings.statue());
        map.put("statuePose", settings.statuePose().getSerializedName());
        map.put("animation", settings.animation());
        map.put("locked", PedestalSettings.isLocked(pedestal.get()));
        return map;
    }

    /** Changes the named settings (any of {@link #pedestalSettings}'s), leaving the rest. False if there's no pedestal. */
    public boolean setPedestal(Level level, BlockPos pos, Map<String, Object> settings) {
        Optional<PedestalBlockEntity> pedestal = PedestalBlocks.pedestalAt(level, pos);
        pedestal.ifPresent(found -> PedestalSettings.set(found, apply(PedestalSettings.get(found).toBuilder(), settings).build()));
        return pedestal.isPresent();
    }

    /**
     * Changes the named settings and locks them, so no player can open the pedestal's screen; one who tries is told
     * {@code refusal}, or with null is refused silently. False if there's no pedestal.
     */
    public boolean forcePedestal(Level level, BlockPos pos, Map<String, Object> settings, @Nullable Component refusal) {
        Optional<PedestalBlockEntity> pedestal = PedestalBlocks.pedestalAt(level, pos);
        pedestal.ifPresent(found -> PedestalSettings.force(found, apply(PedestalSettings.get(found).toBuilder(), settings).build(), refusal));
        return pedestal.isPresent();
    }

    /** Locks a pedestal's settings as they are, with Trophy Pedestals' own refusal message. */
    public boolean lockPedestal(Level level, BlockPos pos) {
        return lockPedestal(level, pos, PedestalSettings.DEFAULT_REFUSAL);
    }

    /** Locks a pedestal's settings as they are, telling a player who tries {@code refusal} (null: silently). */
    public boolean lockPedestal(Level level, BlockPos pos, @Nullable Component refusal) {
        Optional<PedestalBlockEntity> pedestal = PedestalBlocks.pedestalAt(level, pos);
        pedestal.ifPresent(found -> PedestalSettings.lock(found, refusal));
        return pedestal.isPresent();
    }

    /** Lets players with access change a pedestal's settings again. */
    public boolean unlockPedestal(Level level, BlockPos pos) {
        Optional<PedestalBlockEntity> pedestal = PedestalBlocks.pedestalAt(level, pos);
        pedestal.ifPresent(PedestalSettings::unlock);
        return pedestal.isPresent();
    }

    // Each named setting a script gave onto the builder; names it doesn't know are logged and skipped.
    private static PedestalSettings.Builder apply(PedestalSettings.Builder builder, Map<String, Object> settings) {
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            Object value = entry.getValue();
            switch (entry.getKey()) {
                case "textMode" -> builder.textMode(PedestalBlockEntity.TextMode.valueOf(upper(value)));
                case "scale" -> builder.scale(number(value));
                case "height" -> builder.height(number(value));
                case "yaw" -> builder.yaw(number(value));
                case "pitch" -> builder.pitch(number(value));
                case "roll" -> builder.roll(number(value));
                case "textHeight" -> builder.textHeight(number(value));
                case "textScale" -> builder.textScale(number(value));
                case "crown" -> builder.crown(bool(value));
                case "light" -> builder.light(bool(value));
                case "spins" -> builder.spins(bool(value));
                case "animates" -> builder.animates(bool(value));
                case "effects" -> builder.effects(bool(value));
                case "sounds" -> builder.sounds(bool(value));
                case "statue" -> builder.statue(bool(value));
                case "redstone" -> builder.redstone(PedestalBlockEntity.RedstoneMode.valueOf(upper(value)));
                case "statuePose" -> builder.statuePose(PedestalBlockEntity.StatuePose.valueOf(upper(value)));
                case "animation" -> builder.animation(String.valueOf(value));
                default -> TrophyPedestalsMod.LOGGER.warn("Unknown pedestal setting '{}' from a script; skipped", entry.getKey());
            }
        }
        return builder;
    }

    private static String upper(Object value) {
        return String.valueOf(value).toUpperCase(Locale.ROOT);
    }

    private static float number(Object value) {
        return value instanceof Number number ? number.floatValue() : Float.parseFloat(String.valueOf(value));
    }

    private static boolean bool(Object value) {
        return value instanceof Boolean flag ? flag : Boolean.parseBoolean(String.valueOf(value));
    }
}
