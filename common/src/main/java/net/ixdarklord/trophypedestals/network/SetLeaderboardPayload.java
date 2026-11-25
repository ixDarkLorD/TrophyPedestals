package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.Leaderboards;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public record SetLeaderboardPayload(BlockPos pos, ResourceLocation board) implements CustomPacketPayload {
    public static final Type<SetLeaderboardPayload> TYPE = new Type<>(TrophyPedestalsMod.id("set_leaderboard"));
    private static final double REACH = 8.0;

    @Override
    public Type<SetLeaderboardPayload> type() {
        return TYPE;
    }

    public void apply(ServerPlayer player) {
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > REACH * REACH) return;
        if (!(player.level().getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) || pedestal.isTrophy() || !pedestal.display().isEmpty()
                || pedestal.mannequin() || pedestal.glass()) return;
        Leaderboards.Board chosen = Leaderboards.Board.byName(board.toString());
        if (chosen == null) return;
        if (pedestal.leaderboard() == null) {
            InteractionHand hand = player.getMainHandItem().is(Pedestals.LEADERBOARD_CREST.get()) ? InteractionHand.MAIN_HAND
                    : player.getOffhandItem().is(Pedestals.LEADERBOARD_CREST.get()) ? InteractionHand.OFF_HAND : null;
            if (hand == null) return;
            ItemStack crest = player.getItemInHand(hand);
            if (!player.getAbilities().instabuild) crest.shrink(1);
        } else if (!pedestal.canAdjust(player)) {
            return;
        }
        pedestal.setLeaderboard(chosen);
        Leaderboards.show(pedestal, Leaderboards.top(player.level().getServer(), chosen));
        player.displayClientMessage(Component.translatable("trophypedestals.leaderboard.set", chosen.title()), true);
        player.level().playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
