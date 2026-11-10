package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.DisplayedTrophySettings;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.TrophyCabinetBlockEntity;
import net.minecraft.core.BlockPos;
import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sent when a player closes the adjust screen of a trophy on a trophy display: the settings for its spot that can be
 * changed there (none that change its size or place, which stay as a new pedestal has them).
 */
public record AdjustDisplayedTrophyPayload(BlockPos pos, int slot, PedestalBlockEntity.TextMode textMode, boolean spins, float yaw,
                                           float pitch, float roll, boolean effects, PedestalBlockEntity.RedstoneMode redstone, boolean sounds, boolean animates,
                                           boolean statue, PedestalBlockEntity.StatuePose statuePose, String animation, boolean crown,
                                           boolean light) implements CustomPacketPayload {
    public static final Type<AdjustDisplayedTrophyPayload> TYPE = new Type<>(TrophyPedestalsMod.id("adjust_displayed_trophy"));
    // How near the display the player must be, in blocks.
    private static final double REACH = 8.0;

    @Override
    public Type<AdjustDisplayedTrophyPayload> type() {
        return TYPE;
    }

    /** Applies it for this player, if they're near the display and there's a trophy on that spot. */
    public void apply(ServerPlayer player) {
        if (slot < 0 || slot >= TrophyCabinetBlockEntity.SLOTS) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > REACH * REACH) return;
        if (!(player.level().getBlockEntity(pos) instanceof TrophyCabinetBlockEntity display) || display.item(slot).isEmpty()) return;
        DisplayedTrophySettings old = display.settings(slot);
        // (Only trophies go on cabinets and shelves: each tab only if the server lets players change a trophy's, Behavior always.)
        boolean shown = TrophyPedestalsConfig.TROPHY_DISPLAY_SETTINGS.get(), text = TrophyPedestalsConfig.TROPHY_TEXT_SETTINGS.get();
        boolean pose = TrophyPedestalsConfig.TROPHY_POSE_SETTINGS.get();
        display.setSettings(slot, new DisplayedTrophySettings(text ? textMode : old.textMode(), spins, shown ? yaw : old.yaw(), shown ? pitch : old.pitch(),
                shown ? roll : old.roll(), effects,
                redstone, sounds, pose ? animates : old.animates(), pose ? statue : old.statue(), pose ? statuePose : old.statuePose(),
                pose ? animation : old.animation(), crown, light));
    }
}
