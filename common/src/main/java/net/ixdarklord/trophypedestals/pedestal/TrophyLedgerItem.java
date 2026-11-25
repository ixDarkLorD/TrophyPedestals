package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.feat.TrophyLedger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class TrophyLedgerItem extends Item {
    public TrophyLedgerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) TrophyLedger.open(server);
        return InteractionResult.SUCCESS;
    }
}
