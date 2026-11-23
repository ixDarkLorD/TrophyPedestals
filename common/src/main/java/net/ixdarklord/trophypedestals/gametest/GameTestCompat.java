package net.ixdarklord.trophypedestals.gametest;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.DataResult;
import io.netty.channel.embedded.EmbeddedChannel;
import net.ixdarklord.trophypedestals.compat.ItemInteractionResult;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.UUID;

/**
 * What the shared game tests use and 1.20.1's GameTestHelper lacks, or gets wrong on Forge (as in Tierwise's 1.20.1
 * port).
 */
final class GameTestCompat {
    private GameTestCompat() {}

    static GameTestAssertException assertionException(GameTestHelper helper, String message) {
        return new GameTestAssertException(message);
    }

    static DataResult<LootItemFunction> lootFunction(JsonElement json) {
        try {
            return DataResult.success(Deserializers.createFunctionSerializer().create().fromJson(json, LootItemFunction.class));
        } catch (RuntimeException e) {
            return DataResult.error(e::toString);
        }
    }

    static ItemInteractionResult useItemOn(BlockState state, ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        player.setItemInHand(hand, stack);
        try {
            InteractionResult result = state.use(level, player, hand, hit);
            return result == InteractionResult.PASS ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                    : result.consumesAction() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
        } finally {
            ItemStack given = player.getItemInHand(hand);
            player.setItemInHand(hand, held);
            if (given != stack && given != held && !given.isEmpty() && !player.getInventory().add(given)) player.drop(given, false);
        }
    }

    static InteractionResult useWithoutItem(BlockState state, Level level, Player player, BlockHitResult hit) {
        ItemStack held = player.getMainHandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        try {
            return state.use(level, player, InteractionHand.MAIN_HAND, hit);
        } finally {
            ItemStack given = player.getMainHandItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            if (!given.isEmpty() && !player.getInventory().add(given)) player.drop(given, false);
        }
    }

    /** A crafting grid holding these items, for asking the recipes what they make (1.20.1 has no CraftingInput). */
    static CraftingContainer craftingGrid(int width, int height, List<ItemStack> items) {
        AbstractContainerMenu menu = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        CraftingContainer grid = new TransientCraftingContainer(menu, width, height);
        for (int slot = 0; slot < items.size(); slot++) grid.setItem(slot, items.get(slot).copy());
        return grid;
    }

    static ServerPlayer mockServerPlayer(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        connection.setProtocol(ConnectionProtocol.PLAY);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player);
        return player;
    }
}
