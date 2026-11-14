package net.ixdarklord.trophypedestals.forge.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.player.PlayerEventJS;
import net.ixdarklord.trophypedestals.api.TrophyEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Trophy Pedestals' events for KubeJS server scripts, from {@link TrophyEvents} (KubeJS 6's event classes on 1.20.1):
 * <pre>{@code
 * TrophyPedestalsEvents.mobTrophyDrop(event => {
 *     if (event.mob.type == 'minecraft:pig') event.cancel()
 * })
 * TrophyPedestalsEvents.collected(event => {
 *     if (event.first) event.player.tell(`New trophy: ${event.mobType}`)
 * })
 * }</pre>
 */
public final class TrophyPedestalsKubeEvents {
    public static final EventGroup GROUP = EventGroup.of("TrophyPedestalsEvents");
    /** A mob trophy is about to drop for a player: change it with {@code event.trophy = ...}, or {@code event.cancel()}. */
    public static final EventHandler MOB_TROPHY_DROP = GROUP.server("mobTrophyDrop", () -> MobTrophyDropKubeEvent.class).hasResult();
    /** A mob trophy was recorded in a player's ledger. */
    public static final EventHandler COLLECTED = GROUP.server("collected", () -> CollectedKubeEvent.class);

    private TrophyPedestalsKubeEvents() {}

    /** Forwards the mod's events to scripts, while any script listens. */
    static void init() {
        TrophyEvents.MOB_TROPHY_DROP.register(drop -> {
            if (!MOB_TROPHY_DROP.hasListeners()) return;
            MobTrophyDropKubeEvent event = new MobTrophyDropKubeEvent(drop);
            if (MOB_TROPHY_DROP.post(event).interruptFalse()) drop.cancel();
        });
        TrophyEvents.COLLECTED.register((player, mob, gilded, first) -> {
            if (COLLECTED.hasListeners()) COLLECTED.post(new CollectedKubeEvent(player, mob, gilded, first));
        });
    }

    public static final class MobTrophyDropKubeEvent extends PlayerEventJS {
        private final TrophyEvents.Drop drop;

        MobTrophyDropKubeEvent(TrophyEvents.Drop drop) {
            this.drop = drop;
        }

        @Override
        public ServerPlayer getEntity() {
            return drop.slayer();
        }

        /** The mob killed (or the player, for a statue). */
        public LivingEntity getMob() {
            return drop.mob();
        }

        public ItemStack getTrophy() {
            return drop.trophy();
        }

        public void setTrophy(ItemStack trophy) {
            drop.setTrophy(trophy);
        }
    }

    public static final class CollectedKubeEvent extends PlayerEventJS {
        private final ServerPlayer player;
        private final EntityType<?> mob;
        private final boolean gilded, first;

        CollectedKubeEvent(ServerPlayer player, EntityType<?> mob, boolean gilded, boolean first) {
            this.player = player;
            this.mob = mob;
            this.gilded = gilded;
            this.first = first;
        }

        @Override
        public ServerPlayer getEntity() {
            return player;
        }

        public EntityType<?> getMobType() {
            return mob;
        }

        public boolean isGilded() {
            return gilded;
        }

        /** Whether it's the first trophy of this mob the player has had (the first gilded one, for a gilded one). */
        public boolean isFirst() {
            return first;
        }
    }
}
