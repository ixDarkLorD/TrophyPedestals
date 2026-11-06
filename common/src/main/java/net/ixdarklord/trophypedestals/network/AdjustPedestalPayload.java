package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * From a client's adjustment screen: when the pedestal at {@code pos} shows its text, how large its item or mob is, how
 * far it's moved up, whether it turns, how far its text is moved up, its yaw, pitch and roll, whether its flourishes show, how
 * it answers redstone, whether it makes its sounds, whether a mob trophy's mob moves, and whether an achievement trophy shows
 * its unlocker's statue, the pose a statue takes, and the animation its mob or statue plays, how large its text is, whether a legendary one wears its crown (and its crown's size and place), whether its
 * showcase light shows, and who else may adjust it.
 */
public record AdjustPedestalPayload(BlockPos pos, PedestalBlockEntity.TextMode textMode, float scale, float height, boolean spins, float textHeight,
                                    float yaw, float pitch, float roll, boolean effects, PedestalBlockEntity.RedstoneMode redstone, boolean sounds,
                                    boolean animates, boolean statue, PedestalBlockEntity.StatuePose statuePose,
                                    String animation, float textScale, boolean crown, float crownScale, float crownX, float crownY,
                                    boolean light, PedestalBlockEntity.Access access)
        implements CustomPacketPayload {
    public static final Type<AdjustPedestalPayload> TYPE = new Type<>(TrophyPedestalsMod.id("adjust_pedestal"));
    // How near the pedestal the player must be, in blocks.
    private static final double REACH = 8.0;

    @Override
    public Type<AdjustPedestalPayload> type() {
        return TYPE;
    }

    /** Applies it for this player, if they're near the pedestal and allowed to adjust it. */
    public void apply(ServerPlayer player) {
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > REACH * REACH) return;
        if (!(player.level().getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) || !pedestal.canAdjust(player)) return;
        // Each of the screen's tabs only if the server lets players change it (Behavior always).
        if (TrophyPedestalsConfig.PEDESTAL_DISPLAY_SETTINGS.get()) {
            pedestal.setScale(scale);
            pedestal.setHeight(height);
            pedestal.setRotation(yaw, pitch, roll);
            pedestal.setCrownPlacement(crownScale, crownX, crownY);
        }
        if (TrophyPedestalsConfig.PEDESTAL_TEXT_SETTINGS.get()) {
            pedestal.setTextMode(textMode);
            pedestal.setTextHeight(textHeight);
            pedestal.setTextScale(textScale);
        }
        if (TrophyPedestalsConfig.PEDESTAL_POSE_SETTINGS.get()) {
            pedestal.setAnimates(animates);
            pedestal.setStatue(statue);
            pedestal.setStatuePose(statuePose);
            pedestal.setAnimation(animation);
        }
        pedestal.setSpins(spins);
        pedestal.setEffects(effects);
        pedestal.setRedstone(redstone);
        pedestal.setSounds(sounds);
        pedestal.setCrown(crown);
        pedestal.setLight(light);
        // (Who else may adjust it is its owner's (or an operator's) to choose.)
        if (pedestal.canChangeAccess(player) && pedestal.access() != access) pedestal.setAccess(access);
    }
}
