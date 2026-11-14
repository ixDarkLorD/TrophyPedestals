package net.ixdarklord.trophypedestals.compat.ftb;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.AbstractBooleanTask;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.feat.TrophyLedger;
import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An FTB Quests task done once the player has collected mob trophies, as their Mob Trophies Ledger records them: one of a
 * mob the pack maker names, or a number of different mobs' (any mob, with no mob named). It can ask for gilded ones
 * only. Pack makers pick it as "Mob Trophy" in the quest editor. Checked on login and every few ticks, so trophies
 * collected before the quest was added count too. Only loaded when FTB Quests is. (1.20.1's FTB Quests saves a task to a
 * tag, sends it in a plain buffer and edits it in a config group: the same fields as 26.1.2's.)
 */
public final class MobTrophyTask extends AbstractBooleanTask {
    private static TaskType type;

    private String mob = "";
    private int count = 1;
    private boolean gilded;

    public MobTrophyTask(long id, Quest quest) {
        super(id, quest);
    }

    public static void register() {
        type = TaskTypes.register(TrophyPedestalsMod.id("mob_trophy"), MobTrophyTask::new, () -> Icon.getIcon("trophypedestals:item/trophy_ledger"));
    }

    @Override
    public TaskType getType() {
        return type;
    }

    // The mob named, or null for any mob.
    private @Nullable ResourceLocation mobId() {
        return mob.isBlank() ? null : ResourceLocation.tryParse(mob.trim());
    }

    @Override
    public boolean canSubmit(TeamData team, ServerPlayer player) {
        List<ResourceLocation> collected = gilded ? TrophyLedger.COLLECTED_GILDED.get(player) : TrophyLedger.COLLECTED.get(player);
        ResourceLocation id = mobId();
        return id != null ? collected.contains(id) : collected.size() >= Math.max(1, count);
    }

    // As an advancement task is: a few times a second, and on login.
    @Override
    public int autoSubmitOnPlayerTick() {
        return 5;
    }

    @Override
    public boolean checkOnLogin() {
        return true;
    }

    @Override
    public void writeData(CompoundTag nbt) {
        super.writeData(nbt);
        nbt.putString("mob", mob);
        nbt.putInt("count", count);
        nbt.putBoolean("gilded", gilded);
    }

    @Override
    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        mob = nbt.contains("mob") ? nbt.getString("mob") : "";
        count = nbt.contains("count") ? Math.max(1, nbt.getInt("count")) : 1;
        gilded = nbt.getBoolean("gilded");
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(mob);
        buffer.writeVarInt(count);
        buffer.writeBoolean(gilded);
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        mob = buffer.readUtf();
        count = buffer.readVarInt();
        gilded = buffer.readBoolean();
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addString("mob", mob, value -> mob = value, "");
        config.addInt("count", count, value -> count = value, 1, 1, 10_000);
        config.addBool("gilded", gilded, value -> gilded = value, false);
    }

    // "Gilded Mob Trophy: Wither", or "Mob Trophies: 20".
    @Override
    public Component getAltTitle() {
        ResourceLocation id = mobId();
        EntityType<?> entity = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (entity != null) {
            return Component.translatable(gilded ? "trophypedestals.ftbquests.task.mob_trophy.gilded" : "trophypedestals.ftbquests.task.mob_trophy", entity.getDescription());
        }
        return Component.translatable(gilded ? "trophypedestals.ftbquests.task.mob_trophies.gilded" : "trophypedestals.ftbquests.task.mob_trophies", Math.max(1, count));
    }

    // The mob's trophy, as the creative tab shows it (gilded, if the task asks for that); the ledger for any mob.
    @Override
    public Icon getAltIcon() {
        ResourceLocation id = mobId();
        EntityType<?> entity = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (entity == null) return super.getAltIcon();
        PedestalRarity rarity = MobTrophies.rarity(entity);
        ItemStack trophy = MobTrophies.trophy(Pedestals.DEFAULT.get(), entity, "", 0L, rarity != null ? rarity : PedestalRarities.COMMON);
        return ItemIcon.getItemIcon(gilded ? MobTrophies.gild(trophy) : trophy);
    }
}
