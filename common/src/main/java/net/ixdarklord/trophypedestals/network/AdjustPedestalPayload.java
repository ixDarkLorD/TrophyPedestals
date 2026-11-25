package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record AdjustPedestalPayload(BlockPos pos, PedestalBlockEntity.TextMode textMode, float scale, float height, boolean spins, float textHeight,
                                    float yaw, float pitch, float roll, PedestalBlockEntity.TextMode effects, PedestalBlockEntity.RedstoneMode redstone, boolean sounds,
                                    boolean animates, boolean statue, PedestalBlockEntity.StatuePose statuePose,
                                    String animation, float textScale, boolean crown, float crownScale, float crownX, float crownY,
                                    boolean light, PedestalBlockEntity.Access access)
        implements CustomPacketPayload {
    public static final Type<AdjustPedestalPayload> TYPE = new Type<>(TrophyPedestalsMod.id("adjust_pedestal"));
    private static final double REACH = 8.0;

    @Override
    public Type<AdjustPedestalPayload> type() {
        return TYPE;
    }

    public void apply(ServerPlayer player) {
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > REACH * REACH) return;
        if (!(player.level().getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) || !pedestal.canAdjust(player)) return;
        boolean trophy = pedestal.isTrophy();
        if (TrophyPedestalsConfig.displaySettings(trophy)) {
            pedestal.setScale(scale);
            pedestal.setHeight(height);
            pedestal.setRotation(yaw, pitch, roll);
            pedestal.setCrownPlacement(crownScale, crownX, crownY);
        }
        if (TrophyPedestalsConfig.textModeSettings(trophy)) pedestal.setTextMode(textMode);
        if (TrophyPedestalsConfig.textSettings(trophy)) {
            pedestal.setTextHeight(textHeight);
            pedestal.setTextScale(textScale);
        }
        if (TrophyPedestalsConfig.poseSettings(trophy)) {
            pedestal.setAnimates(animates);
            pedestal.setStatue(statue);
            pedestal.setStatuePose(statuePose);
            pedestal.setAnimation(animation);
        }
        pedestal.setSpins(spins);
        pedestal.setEffectsMode(effects);
        pedestal.setRedstone(redstone);
        pedestal.setSounds(sounds);
        pedestal.setCrown(crown);
        pedestal.setLight(light);
        if (pedestal.canChangeAccess(player) && pedestal.access() != access) pedestal.setAccess(access);
    }
}
