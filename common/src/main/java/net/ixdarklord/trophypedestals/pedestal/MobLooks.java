package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.Set;

/**
 * How a mob trophy keeps the look of the mob it shows: the mob's saved data when it was slain, less everything about
 * where it was, what it was doing and what it knew, so what's left is how it looked (its variant, color, size, whether
 * it was a baby, its gear and saddle, its name, a sheep's wool, a villager's profession), mods' mobs' own look
 * included.
 */
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
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, mob.registryAccess());
            mob.saveWithoutId(output);
            CompoundTag look = output.buildResult();
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
            mob.load(TagValueInput.create(ProblemReporter.DISCARDING, registries, shown));
        } catch (RuntimeException e) {
            TrophyPedestalsMod.LOGGER.debug("Couldn't show {}'s look on its trophy", mob.getType(), e);
        }
    }
}
