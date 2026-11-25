package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public final class MobLooks {
    public static final int MAX_BYTES = 16 * 1024;

    private static final Set<String> NOT_LOOK = Set.of(
            "Air", "CustomNameVisible", "Fire", "Glowing", "HasVisualFire", "Invulnerable", "Motion", "NoGravity",
            "OnGround", "Passengers", "PortalCooldown", "Pos", "Rotation", "Silent", "Tags", "TicksFrozen", "UUID",
            "data", "fall_distance",
            "AbsorptionAmount", "Brain", "DeathTime", "FallFlying", "Health", "HurtByTimestamp", "HurtTime",
            "active_effects", "attributes", "current_explosion_impact_pos", "current_impulse_context_reset_grace_time",
            "last_hurt_by_mob", "last_hurt_by_player", "last_hurt_by_player_memory_time", "locator_bar_icon",
            "ticks_since_last_hurt_by_mob",
            "CanPickUpLoot", "DeathLootTable", "DeathLootTableSeed", "NoAI", "PersistenceRequired", "drop_chances",
            "home_pos", "home_radius", "AgeLocked", "ForcedAge", "InLove", "LoveCause", "Owner", "Bred",
            "EatingHaystack", "Temper", "Inventory", "AngerTime", "AngryAt", "anger_end_time", "angry_at",
            "AssignProfessionWhenSpawned", "FoodLevel", "Gossips", "LastGossipDecay", "LastRestock", "Offers",
            "RestocksToday", "Xp");

    private MobLooks() {}

    public static CompoundTag capture(LivingEntity mob) {
        try {
            CompoundTag look = mob.saveWithoutId(new CompoundTag());
            NOT_LOOK.forEach(look::remove);
            if (look.sizeInBytes() <= MAX_BYTES) return look;
            TrophyPedestalsMod.LOGGER.debug("{}'s look is too big for its trophy ({} bytes); it shows as it spawns", mob.getType(), look.sizeInBytes());
        } catch (RuntimeException e) {
            TrophyPedestalsMod.LOGGER.debug("Couldn't keep {}'s look for its trophy", mob.getType(), e);
        }
        return new CompoundTag();
    }

    public static void apply(Entity mob, CompoundTag look, HolderLookup.Provider registries) {
        if (look.isEmpty()) return;
        try {
            CompoundTag shown = look.copy();
            NOT_LOOK.forEach(shown::remove);
            mob.load(shown);
        } catch (RuntimeException e) {
            TrophyPedestalsMod.LOGGER.debug("Couldn't show {}'s look on its trophy", mob.getType(), e);
        }
    }
}
