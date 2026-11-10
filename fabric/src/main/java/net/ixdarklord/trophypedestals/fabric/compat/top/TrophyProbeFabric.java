package net.ixdarklord.trophypedestals.fabric.compat.top;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TheOneProbePlugin;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.PedestalInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Adds what a looked-at pedestal holds to The One Probe ({@link PedestalInfo}). TOP finds it through the
 * {@code theoneprobe} entrypoint; nothing else loads this class.
 */
public final class TrophyProbeFabric implements TheOneProbePlugin, IProbeInfoProvider {
    private static final Identifier ID = TrophyPedestalsMod.id("pedestal");

    @Override
    public void register(ITheOneProbe probe) {
        probe.registerProvider(this);
    }

    @Override
    public Identifier getID() {
        return ID;
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level, BlockState state, IProbeHitData data) {
        if (level.getBlockEntity(data.getPos()) instanceof PedestalBlockEntity pedestal) PedestalInfo.lines(pedestal, false).forEach(info::text);
    }
}
