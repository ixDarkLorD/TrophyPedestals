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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.phys.BlockHitResult;

import java.util.UUID;

/** What the shared game tests use and 1.20.1's GameTestHelper lacks, or gets wrong on Forge (as in Tierwise's 1.20.1 port). */
final class GameTestCompat {
    private GameTestCompat() {}

    /** A failure to throw, like {@code GameTestHelper#assertionException}. */
    static GameTestAssertException assertionException(GameTestHelper helper, String message) {
        return new GameTestAssertException(message);
    }

    /** A loot function read from its JSON, as a data pack writes it (26.1's tests read it with a codec). */
    static DataResult<LootItemFunction> lootFunction(JsonElement json) {
        try {
            return DataResult.success(Deserializers.createFunctionSerializer().create().fromJson(json, LootItemFunction.class));
        } catch (RuntimeException e) {
            return DataResult.error(e::toString);
        }
    }

    /**
     * Uses this item on the block, as 26.1's {@code BlockState#useItemOn}: 1.20.1 uses a block with what the player holds,
     * so the item is held for it (the same stack, so what the use takes from it shows).
     */
    static ItemInteractionResult useItemOn(BlockState state, ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        player.setItemInHand(hand, stack);
        try {
            InteractionResult result = state.use(level, player, hand, hit);
            return result == InteractionResult.PASS ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                    : result.consumesAction() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
        } finally {
            player.setItemInHand(hand, held);
        }
    }

    /**
     * Uses the block with an empty hand, as 26.1's {@code BlockState#useWithoutItem}. What the use puts in the emptied
     * hand (an item taken back off a pedestal) is kept, in the inventory.
     */
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

    /**
     * A mock player like {@code GameTestHelper#makeMockServerPlayerInLevel}, but on a connection with an embedded
     * channel, as later versions make it. 1.20.1's mock connection has no channel, and Forge's {@code placeNewPlayer}
     * sends its registry packets through it and fails; Fabric doesn't, but the same mock works on both.
     */
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
