package net.ixdarklord.trophypedestals.compat.ftb;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import de.marhali.json5.Json5Object;
import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
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
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * An FTB Quests reward that gives a trophy made as it's claimed: titled as the pack maker writes it, showing the item
 * they name, of the rank they pick, recorded as earned by the player who claims it, now. Pack makers pick it as
 * "Trophy" in the quest editor. Only loaded when FTB Quests is.
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
    public boolean claim(ServerPlayer player, boolean notify) {
        Identifier iconId = Identifier.tryParse(icon);
        Identifier rankId = PedestalRarities.ID_CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(rank)).result()
                .orElse(PedestalRarities.RARE.id());
        Component name = title.isBlank() ? getQuest().getTitle() : Component.literal(title);
        ItemStack trophy = new ItemStack(Pedestals.DEFAULT.get());
        Pedestals.record(trophy, new PedestalRecord(Feats.FEAT, TrophyPedestalsMod.id("quest/" + getQuest().getCodeString()), PedestalRecord.Unlocker.PLAYER,
                player.getName().getString(), System.currentTimeMillis(), rankId, Optional.of(name), Optional.ofNullable(iconId)));
        Pedestals.give(player, List.of(trophy));
        return true;
    }

    @Override
    public void writeData(Json5Object json, HolderLookup.Provider provider) {
        super.writeData(json, provider);
        json.addProperty("title", title);
        json.addProperty("icon", icon);
        json.addProperty("rank", rank);
    }

    @Override
    public void readData(Json5Object json, HolderLookup.Provider provider) {
        super.readData(json, provider);
        title = json.has("title") ? json.get("title").getAsString() : "";
        icon = json.has("icon") ? json.get("icon").getAsString() : "minecraft:gold_ingot";
        rank = json.has("rank") ? json.get("rank").getAsString() : "rare";
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(title);
        buffer.writeUtf(icon);
        buffer.writeUtf(rank);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        title = buffer.readUtf();
        icon = buffer.readUtf();
        rank = buffer.readUtf();
    }

    @Override
    public void fillConfigGroup(EditableConfigGroup config) {
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
