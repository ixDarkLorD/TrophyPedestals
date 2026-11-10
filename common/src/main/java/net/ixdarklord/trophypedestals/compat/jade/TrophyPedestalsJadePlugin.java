package net.ixdarklord.trophypedestals.compat.jade;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlock;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.PedestalInfo;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Adds what a looked-at pedestal holds to Jade's tooltip: its trophy, rank and who earned it, a mob trophy's kills, a
 * leaderboard's leader, and its glass case ({@link PedestalInfo}). Jade finds it by its annotation on NeoForge and
 * through the {@code jade} entrypoint on Fabric; nothing else loads this class.
 */
@WailaPlugin(TrophyPedestalsMod.MOD_ID)
public final class TrophyPedestalsJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Pedestal.INSTANCE, PedestalBlock.class);
    }

    private enum Pedestal implements IBlockComponentProvider {
        INSTANCE;

        private static final Identifier UID = TrophyPedestalsMod.id("pedestal");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof PedestalBlockEntity pedestal) PedestalInfo.lines(pedestal, true).forEach(tooltip::add);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
