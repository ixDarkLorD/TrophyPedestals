package net.ixdarklord.trophypedestals.neoforge.compat.top;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.PedestalInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

import java.util.function.Function;

/**
 * Adds what a looked-at pedestal holds to The One Probe ({@link PedestalInfo}). TOP takes its providers through
 * NeoForge's inter-mod messages, which CoolCatLib doesn't wrap; only loaded when TOP is installed.
 */
public final class TrophyProbeNeoForge implements IProbeInfoProvider {
    private static final Identifier ID = TrophyPedestalsMod.id("pedestal");

    public static void register(IEventBus modBus) {
        modBus.addListener(InterModEnqueueEvent.class, event -> InterModComms.sendTo("theoneprobe", "getTheOneProbe",
                () -> (Function<ITheOneProbe, Void>) probe -> {
                    probe.registerProvider(new TrophyProbeNeoForge());
                    return null;
                }));
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
