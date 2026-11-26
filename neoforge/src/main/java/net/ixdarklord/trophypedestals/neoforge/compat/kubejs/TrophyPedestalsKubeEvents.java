package net.ixdarklord.trophypedestals.neoforge.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.player.KubePlayerEvent;
import net.ixdarklord.trophypedestals.api.TrophyEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public final class TrophyPedestalsKubeEvents {
    public static final EventGroup GROUP = EventGroup.of("TrophyPedestalsEvents");
    public static final EventHandler MOB_TROPHY_DROP = GROUP.server("mobTrophyDrop", () -> MobTrophyDropKubeEvent.class).hasResult();
    public static final EventHandler COLLECTED = GROUP.server("collected", () -> CollectedKubeEvent.class);
    public static final EventHandler MOB_DISPLAY_SIZES = GROUP.client("mobDisplaySizes", () -> MobDisplaySizesKubeEvent.class);

    private TrophyPedestalsKubeEvents() {}

    static void init() {
        TrophyEvents.MOB_TROPHY_DROP.register(drop -> {
            if (!MOB_TROPHY_DROP.hasListeners()) return;
            MobTrophyDropKubeEvent event = new MobTrophyDropKubeEvent(drop);
            if (MOB_TROPHY_DROP.post(event).interruptFalse()) drop.cancel();
        });
        TrophyEvents.COLLECTED.register((player, mob, gilded, first) -> {
            if (COLLECTED.hasListeners()) COLLECTED.post(new CollectedKubeEvent(player, mob, gilded, first));
        });
        TrophyEvents.MOB_DISPLAY_SIZES.register(sizes -> {
            if (MOB_DISPLAY_SIZES.hasListeners()) MOB_DISPLAY_SIZES.post(new MobDisplaySizesKubeEvent(sizes));
        });
    }

    public static final class MobTrophyDropKubeEvent implements KubePlayerEvent {
        private final TrophyEvents.Drop drop;

        MobTrophyDropKubeEvent(TrophyEvents.Drop drop) {
            this.drop = drop;
        }

        @Override
        public ServerPlayer getEntity() {
            return drop.slayer();
        }

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

    public static final class CollectedKubeEvent implements KubePlayerEvent {
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

        public boolean isFirst() {
            return first;
        }
    }

    public static final class MobDisplaySizesKubeEvent implements KubeEvent {
        private final TrophyEvents.DisplaySizes sizes;

        MobDisplaySizesKubeEvent(TrophyEvents.DisplaySizes sizes) {
            this.sizes = sizes;
        }

        public void set(String mob, Map<String, Object> display) {
            Identifier id = Identifier.tryParse(mob);
            EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if (type == null) return;
            sizes.set(type, number(display, "scale", 1.0F), number(display, "size", 0.0F), number(display, "offset", 0.0F));
        }

        private static float number(Map<String, Object> display, String key, float fallback) {
            return display != null && display.get(key) instanceof Number value ? value.floatValue() : fallback;
        }
    }
}
