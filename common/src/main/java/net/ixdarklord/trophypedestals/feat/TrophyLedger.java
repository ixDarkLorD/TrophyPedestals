package net.ixdarklord.trophypedestals.feat;

import com.google.common.collect.Iterables;
import net.ixdarklord.coolcatcore.api.attachment.Attachment;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.network.OpenLedgerPayload;
import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The mob trophies each player has collected, for the Mob Trophies Ledger: every mob they've had a trophy of (one dropped for
 * them, or a first kill), and the ones they've had a gilded trophy of. Kept on the player, through death.
 */
public final class TrophyLedger {
    /** The mobs a player has had a trophy of, by id. */
    public static final Attachment<List<ResourceLocation>> COLLECTED = Feats.ATTACHMENTS.builder("ledger", ResourceLocation.CODEC.listOf(), List.<ResourceLocation>of())
            .copyOnDeath()
            .build();
    /** The mobs a player has had a gilded trophy of. */
    public static final Attachment<List<ResourceLocation>> COLLECTED_GILDED = Feats.ATTACHMENTS.builder("ledger_gilded", ResourceLocation.CODEC.listOf(), List.<ResourceLocation>of())
            .copyOnDeath()
            .build();

    private TrophyLedger() {}

    /** Loads the attachments, before {@link Feats} registers them. */
    static void init() {}

    /** Notes a mob trophy in the player's ledger (and, if it's gilded, among their gilded ones). */
    public static void collect(ServerPlayer player, MobTrophyRecord trophy) {
        COLLECTED.update(player, list -> with(list, trophy.entity()));
        if (trophy.gilded()) COLLECTED_GILDED.update(player, list -> with(list, trophy.entity()));
    }

    /** What buying back a trophy of this mob costs, in experience levels: twice as many for a gilded one. */
    public static int buyBackCost(boolean gilded) {
        return TrophyPedestalsConfig.LEDGER_BUY_BACK_LEVELS.get() * (gilded ? 2 : 1);
    }

    /**
     * Buys back a trophy of this mob (a gilded one, if {@code gilded}) for the player, from their ledger: one they've
     * collected, and don't carry already, for {@link #buyBackCost} levels (free in creative). It's theirs, slain by them
     * now, of the rank a new one would drop at. Tells them why not if it can't be bought; true if it was.
     */
    public static boolean buyBack(ServerPlayer player, ResourceLocation mob, boolean gilded) {
        if (!TrophyPedestalsConfig.LEDGER_BUY_BACK.get()) return false;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mob).orElse(null);
        if (type == null || !COLLECTED.get(player).contains(mob) || gilded && !COLLECTED_GILDED.get(player).contains(mob)) return false;
        if (carries(player, mob)) {
            player.displayClientMessage(Component.translatable("trophypedestals.ledger.buy_back.carried", type.getDescription()), true);
            return false;
        }
        int cost = buyBackCost(gilded);
        if (!player.isCreative() && player.experienceLevel < cost) {
            player.displayClientMessage(Component.translatable("trophypedestals.ledger.buy_back.too_few", cost), true);
            return false;
        }
        if (!player.isCreative() && cost > 0) player.giveExperienceLevels(-cost);
        ResourceLocation rank = switch (TrophyPedestalsConfig.MOB_TROPHY_RARITY.get()) {
            case NONE -> PedestalRarities.NONE;
            case STRENGTH -> MobTrophies.rarity(type, player.serverLevel()).id();
            case KILLS -> MobTrophies.byKills(type, player.getStats().getValue(Stats.ENTITY_KILLED, type)).id();
        };
        ItemStack trophy = MobTrophies.trophy(Pedestals.DEFAULT.get(), type, player.getName().getString(), System.currentTimeMillis(), rank);
        if (gilded) trophy = MobTrophies.gild(trophy);
        Component name = trophy.getHoverName();
        Pedestals.give(player, List.of(trophy));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("trophypedestals.ledger.buy_back.bought", name), true);
        return true;
    }

    // Whether the player carries a trophy of this mob already (in their inventory, or a gilded one).
    private static boolean carries(ServerPlayer player, ResourceLocation mob) {
        for (ItemStack stack : Iterables.concat(player.getInventory().items, player.getInventory().armor, player.getInventory().offhand)) {
            MobTrophyRecord record = stack.get(Pedestals.MOB_TROPHY.get());
            if (record != null && record.entity().equals(mob)) return true;
        }
        return false;
    }

    /** Opens the player's ledger on their screen. */
    public static void open(ServerPlayer player) {
        Network.sendToPlayer(player, new OpenLedgerPayload(COLLECTED.get(player), COLLECTED_GILDED.get(player)));
    }

    private static List<ResourceLocation> with(List<ResourceLocation> list, ResourceLocation id) {
        if (list.contains(id)) return list;
        List<ResourceLocation> added = new ArrayList<>(list);
        added.add(id);
        return List.copyOf(added);
    }
}
