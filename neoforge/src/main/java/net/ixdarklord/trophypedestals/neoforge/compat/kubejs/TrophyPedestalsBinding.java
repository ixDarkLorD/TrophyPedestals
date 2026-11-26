package net.ixdarklord.trophypedestals.neoforge.compat.kubejs;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalBlocks;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.api.PedestalSettings;
import net.ixdarklord.trophypedestals.api.TrophyPedestals;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlock;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TrophyPedestalsBinding {
    public static final TrophyPedestalsBinding INSTANCE = new TrophyPedestalsBinding();

    private TrophyPedestalsBinding() {}

    public boolean isPedestal(Level level, BlockPos pos) {
        return PedestalBlocks.pedestalAt(level, pos).isPresent();
    }

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
        map.put("effectsMode", settings.effectsMode().getSerializedName());
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

    public boolean setPedestal(Level level, BlockPos pos, Map<String, Object> settings) {
        Optional<PedestalBlockEntity> pedestal = PedestalBlocks.pedestalAt(level, pos);
        pedestal.ifPresent(found -> PedestalSettings.set(found, apply(PedestalSettings.get(found).toBuilder(), settings).build()));
        return pedestal.isPresent();
    }

    public boolean forcePedestal(Level level, BlockPos pos, Map<String, Object> settings, @Nullable Component refusal) {
        Optional<PedestalBlockEntity> pedestal = PedestalBlocks.pedestalAt(level, pos);
        pedestal.ifPresent(found -> PedestalSettings.force(found, apply(PedestalSettings.get(found).toBuilder(), settings).build(), refusal));
        return pedestal.isPresent();
    }

    public boolean lockPedestal(Level level, BlockPos pos) {
        return lockPedestal(level, pos, PedestalSettings.DEFAULT_REFUSAL);
    }

    public boolean lockPedestal(Level level, BlockPos pos, @Nullable Component refusal) {
        Optional<PedestalBlockEntity> pedestal = PedestalBlocks.pedestalAt(level, pos);
        pedestal.ifPresent(found -> PedestalSettings.lock(found, refusal));
        return pedestal.isPresent();
    }

    public boolean unlockPedestal(Level level, BlockPos pos) {
        Optional<PedestalBlockEntity> pedestal = PedestalBlocks.pedestalAt(level, pos);
        pedestal.ifPresent(PedestalSettings::unlock);
        return pedestal.isPresent();
    }

    /**
     * A mob trophy of this mob ({@code "minecraft:zombie"}), as its pedestal item, to give, drop or put in a loot table:
     * of its usual rarity unless one is named ({@code "trophypedestals:epic"}, or just {@code "epic"}), on the default
     * pedestal unless one is named ({@code "trophypedestals:deepslate_pedestal"}), and slain by nobody unless a name is
     * given. Empty if the mob can't be a trophy, or the rarity or pedestal isn't known. Works in startup, client and
     * server scripts.
     */
    public ItemStack mobTrophy(String entity) {
        return mobTrophy(entity, null, null, null);
    }

    public ItemStack mobTrophy(String entity, @Nullable String rarity) {
        return mobTrophy(entity, rarity, null, null);
    }

    public ItemStack mobTrophy(String entity, @Nullable String rarity, @Nullable String pedestal) {
        return mobTrophy(entity, rarity, pedestal, null);
    }

    public ItemStack mobTrophy(String entity, @Nullable String rarity, @Nullable String pedestal, @Nullable String slayer) {
        Identifier entityId = Identifier.tryParse(entity);
        EntityType<?> type = entityId == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (type == null || !TrophyPedestals.canBeTrophy(type)) {
            TrophyPedestalsMod.LOGGER.warn("A script asked for a mob trophy of '{}', which can't be one", entity);
            return ItemStack.EMPTY;
        }
        Identifier pedestalId = pedestal == null ? null : Identifier.tryParse(pedestal);
        PedestalBlock block = pedestal == null ? TrophyPedestals.defaultPedestal() : pedestalId == null ? null : TrophyPedestals.pedestal(pedestalId);
        if (block == null) {
            TrophyPedestalsMod.LOGGER.warn("A script asked for a mob trophy on '{}', which isn't a pedestal", pedestal);
            return ItemStack.EMPTY;
        }
        Identifier rarityId = rarity == null ? null : PedestalRarities.parse(rarity);
        PedestalRarity rank = rarity == null ? TrophyPedestals.mobRarity(type) : rarityId == null ? null : PedestalRarities.find(rarityId);
        if (rank == null) {
            TrophyPedestalsMod.LOGGER.warn("A script asked for a mob trophy of rarity '{}', which isn't one", rarity);
            return ItemStack.EMPTY;
        }
        return TrophyPedestals.createMob(block, type, new CompoundTag(), slayer == null ? "" : slayer, System.currentTimeMillis(), rank);
    }

    /**
     * Adds a trophy (from {@link #mobTrophy}, or any trophy item) to the mob trophies creative tab, after the mobs' own,
     * and so to JEI. Call it from a startup script, so both the game and the server have it; the same trophy is only
     * added once.
     *
     * @return whether it was added
     */
    public boolean addMobTrophy(ItemStack trophy) {
        return TrophyPedestals.addToCreativeTab(trophy);
    }

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
                case "effectsMode" -> builder.effectsMode(PedestalBlockEntity.TextMode.valueOf(upper(value)));
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
