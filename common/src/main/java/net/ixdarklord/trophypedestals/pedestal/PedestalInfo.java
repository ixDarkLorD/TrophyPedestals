package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.TrophyKinds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * What a looked-at pedestal tells Jade and The One Probe: its trophy (its name, rank, who earned it, a mob trophy's
 * kills), its leaderboard's leader, a mannequin, an item on display, and its glass case.
 */
public final class PedestalInfo {
    private PedestalInfo() {}

    public static List<Component> lines(PedestalBlockEntity pedestal, boolean client) {
        List<Component> lines = new ArrayList<>();
        Component engraved = pedestal.components().get(DataComponents.CUSTOM_NAME);
        PedestalRecord record = pedestal.record();
        MobTrophyRecord mob = pedestal.mobTrophy();
        if (record != null) {
            lines.add(engraved != null ? engraved : TrophyKinds.name(record, client));
            lines.add(PedestalRarities.get(record.rarity()).displayName().copy().withColor(PedestalRarities.get(record.rarity()).color()));
            lines.add(record.unlockedBy().copy().withStyle(ChatFormatting.GRAY));
        } else if (mob != null) {
            lines.add(engraved != null ? engraved : mob.displayName());
            if (!PedestalRarities.isNone(mob.rarity())) {
                lines.add(PedestalRarities.get(mob.rarity()).displayName().copy().withColor(PedestalRarities.get(mob.rarity()).color()));
            }
            if (mob.slain()) lines.add(mob.slainLine().copy().withStyle(ChatFormatting.GRAY));
        } else if (pedestal.leaderboard() != null) {
            lines.add(pedestal.leaderboard().title());
            lines.add(pedestal.leaderName().isEmpty() ? Component.translatable("trophypedestals.leaderboard.no_leader").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("trophypedestals.info.leader", pedestal.leaderName(), pedestal.leaderboard().format(pedestal.leaderValue())).withStyle(ChatFormatting.GRAY));
            for (int place = 0; place < pedestal.runnersUp().size(); place++) {
                Leaderboards.Leader runnerUp = pedestal.runnersUp().get(place);
                lines.add(Component.translatable("trophypedestals.leaderboard.place." + (place + 2), runnerUp.name(),
                        pedestal.leaderboard().format(runnerUp.value())).withStyle(ChatFormatting.DARK_GRAY));
            }
        } else if (pedestal.mannequin()) {
            lines.add(Component.translatable("trophypedestals.info.mannequin"));
        } else if (!pedestal.display().isEmpty()) {
            lines.add(Component.translatable("trophypedestals.info.showing", pedestal.display().getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        if (pedestal.glass()) lines.add(Component.translatable("trophypedestals.info.glass", pedestal.glassItem().getHoverName()).withStyle(ChatFormatting.GRAY));
        return lines;
    }
}
