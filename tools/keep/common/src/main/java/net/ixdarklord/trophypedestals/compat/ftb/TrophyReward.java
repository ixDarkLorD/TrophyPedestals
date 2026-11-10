package net.ixdarklord.trophypedestals.compat.ftb;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.feat.Feats;
import net.ixdarklord.trophypedestals.pedestal.PedestalRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * An FTB Quests reward that gives a trophy made as it's claimed: titled as the pack maker writes it, showing the item
 * they name, of the rank they pick, recorded as earned by the player who claims it, now. Pack makers pick it as
 * "Trophy" in the quest editor. Only loaded when FTB Quests is. (1.20.1's FTB Quests saves a reward to a tag, sends it
 * in a plain buffer and edits it in a config group: the same fields as 26.1.2's.)
 */
public final class TrophyReward extends Reward {
    private static RewardType type;

    private String title = "";
    private String icon = "minecraft:gold_ingot";
    private String rank = "rare";

    public TrophyReward(long id, Quest quest) {
        super(id, quest);
    }

    public static void register() {
        type = RewardTypes.register(TrophyPedestalsMod.id("trophy"), TrophyReward::new, () -> Icon.getIcon("trophypedestals:item/trophy_ledger"));
    }

    @Override
    public RewardType getType() {
        return type;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        ResourceLocation iconId = ResourceLocation.tryParse(icon);
        ResourceLocation rankId = PedestalRarities.ID_CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(rank)).result().orElse(PedestalRarities.RARE.id());
        Component name = title.isBlank() ? getQuest().getTitle() : Component.literal(title);
        ItemStack trophy = new ItemStack(Pedestals.DEFAULT.get());
        Pedestals.record(trophy, new PedestalRecord(Feats.FEAT, TrophyPedestalsMod.id("quest/" + getQuest().getCodeString()), PedestalRecord.Unlocker.PLAYER,
                player.getName().getString(), System.currentTimeMillis(), rankId, Optional.of(name), Optional.ofNullable(iconId)));
        Pedestals.give(player, List.of(trophy));
    }

    @Override
    public void writeData(CompoundTag nbt) {
        super.writeData(nbt);
        nbt.putString("title", title);
        nbt.putString("icon", icon);
        nbt.putString("rank", rank);
    }

    @Override
    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        title = nbt.contains("title") ? nbt.getString("title") : "";
        icon = nbt.contains("icon") ? nbt.getString("icon") : "minecraft:gold_ingot";
        rank = nbt.contains("rank") ? nbt.getString("rank") : "rare";
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(title);
        buffer.writeUtf(icon);
        buffer.writeUtf(rank);
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        title = buffer.readUtf();
        icon = buffer.readUtf();
        rank = buffer.readUtf();
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addString("title", title, value -> title = value, "");
        config.addString("icon", icon, value -> icon = value, "minecraft:gold_ingot");
        config.addString("rank", rank, value -> rank = value, "rare");
    }

    @Override
    public Component getAltTitle() {
        return Component.translatable("trophypedestals.ftbquests.reward.trophy", title.isBlank() ? getQuest().getTitle() : Component.literal(title));
    }
}
