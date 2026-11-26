package net.ixdarklord.trophypedestals.neoforge.client;

import net.ixdarklord.trophypedestals.client.MobTrophiesTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.CreativeModeTabSearchRegistry;

import java.util.List;

/** Keeps the mob trophies tab's search trees current when its items are rebuilt outside the creative screen. */
public final class MobTrophySearchNeoForge {
    private MobTrophySearchNeoForge() {}

    public static void init() {
        MobTrophiesTab.onRebuilt(tab -> {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection == null) return;
            List<ItemStack> items = List.copyOf(tab.getDisplayItems());
            connection.searchTrees().updateCreativeTooltips(connection.registryAccess(), items, CreativeModeTabSearchRegistry.getNameSearchKey(tab));
            connection.searchTrees().updateCreativeTags(items, CreativeModeTabSearchRegistry.getTagSearchKey(tab));
        });
    }
}
