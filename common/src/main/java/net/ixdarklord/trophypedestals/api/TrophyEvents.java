package net.ixdarklord.trophypedestals.api;

import net.ixdarklord.coolcatcore.api.event.v2.core.EventInvoker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Trophy Pedestals' events, for mods (on either loader) and, on Forge, KubeJS scripts ({@code TrophyPedestalsEvents}).
 * The first two fire on the server, {@link #MOB_DISPLAY_SIZES} on the client.
 * <pre>{@code
 * TrophyEvents.MOB_TROPHY_DROP.register(drop -> {
 *     if (drop.mob().getType() == EntityType.PIG) drop.cancel();
 * });
 * }</pre>
 */
public final class TrophyEvents {
    /**
     * A mob trophy is about to drop for the player who killed the mob (or, with player trophies on, a player's statue):
     * a listener can change the trophy, or cancel the drop (it's then not recorded in their ledger either).
     */
    public static final EventInvoker<MobTrophyDrop> MOB_TROPHY_DROP = EventInvoker.create(MobTrophyDrop.class);
    /**
     * A mob trophy was recorded in a player's Mob Trophies Ledger: dropped for them, or a first kill's. {@code first} is
     * whether it's the first of that mob they've had (or, for a gilded one, the first gilded one).
     */
    public static final EventInvoker<Collected> COLLECTED = EventInvoker.create(Collected.class);

    /**
     * Mob display sizes are loading on the client, at their first use and after every resource reload, once resource
     * packs' {@code trophypedestals/mob_displays} files are read: a listener can set a mob's, which wins over the packs'.
     */
    public static final EventInvoker<MobDisplaySizes> MOB_DISPLAY_SIZES = EventInvoker.create(MobDisplaySizes.class);

    private TrophyEvents() {}

    @FunctionalInterface
    public interface MobTrophyDrop {
        void onDrop(Drop drop);
    }

    @FunctionalInterface
    public interface Collected {
        void onCollected(ServerPlayer player, EntityType<?> mob, boolean gilded, boolean first);
    }

    /** A mob trophy about to drop: who it's for, the mob it shows, and the trophy, which a listener can replace. */
    public static final class Drop {
        private final ServerPlayer slayer;
        private final LivingEntity mob;
        private ItemStack trophy;
        private boolean cancelled;

        public Drop(ServerPlayer slayer, LivingEntity mob, ItemStack trophy) {
            this.slayer = slayer;
            this.mob = mob;
            this.trophy = trophy;
        }

        /** The player who killed the mob. */
        public ServerPlayer slayer() {
            return slayer;
        }

        /** The mob killed (or the player, for a statue). */
        public LivingEntity mob() {
            return mob;
        }

        /** The trophy that drops. */
        public ItemStack trophy() {
            return trophy;
        }

        /** Drops this instead (empty to drop nothing). */
        public void setTrophy(ItemStack trophy) {
            this.trophy = trophy;
        }

        /** Drops nothing, and records nothing in the player's ledger. */
        public void cancel() {
            this.cancelled = true;
        }

        public boolean cancelled() {
            return cancelled || trophy.isEmpty();
        }
    }

    @FunctionalInterface
    public interface MobDisplaySizes {
        void onLoad(DisplaySizes sizes);
    }

    /** The mob display sizes being loaded. */
    @FunctionalInterface
    public interface DisplaySizes {
        /**
         * Sets how big a mob shows on an open pedestal and on its trophy item: {@code scale} multiplies the size it would
         * have, {@code size} (when above 0) is a fixed scale used instead, and {@code offset} raises it on a pedestal, in
         * blocks. {@code set(mob, 1, 0, 0)} gives it back its own size.
         */
        void set(EntityType<?> mob, float scale, float size, float offset);
    }
}
