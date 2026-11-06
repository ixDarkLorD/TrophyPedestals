package net.ixdarklord.trophypedestals.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.ixdarklord.coolcatcore.api.event.v2.common.CommandEvents;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlock;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/** {@code /trophypedestals mob_trophy}: hands out mob trophies, for functions and operators (game master level). */
public final class TrophyPedestalsCommands {
    private static final DynamicCommandExceptionType NOT_A_PEDESTAL = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.trophypedestals.not_pedestal", String.valueOf(id)));
    private static final DynamicCommandExceptionType UNKNOWN_RARITY = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.trophypedestals.unknown_rarity", String.valueOf(id)));
    private static final DynamicCommandExceptionType NOT_A_MOB = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.trophypedestals.mob_trophy.not_mob", String.valueOf(id)));
    /** The stone and wood pedestals, for other mods' commands that take one too. */
    public static final SuggestionProvider<CommandSourceStack> PEDESTALS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(Pedestals.BLOCKS_BY_STONE.values().stream().map(entry -> BuiltInRegistries.BLOCK.getKey(entry.get())), builder);
    /** Every rarity, for other mods' commands that take one too. */
    public static final SuggestionProvider<CommandSourceStack> RARITIES = (context, builder) ->
            SharedSuggestionProvider.suggestResource(PedestalRarities.all().stream().map(PedestalRarity::id), builder);
    private static final SuggestionProvider<CommandSourceStack> ENTITIES = (context, builder) ->
            SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), builder);

    private TrophyPedestalsCommands() {}

    public static void init() {
        CommandEvents.REGISTER.register((dispatcher, context, selection) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(TrophyPedestalsMod.MOD_ID).requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        root.then(Commands.literal("mob_trophy").then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("entity", ResourceLocationArgument.id()).suggests(ENTITIES).executes(ctx -> mobTrophy(ctx, null, null))
                        .then(Commands.argument("pedestal", ResourceLocationArgument.id()).suggests(PEDESTALS)
                                .executes(ctx -> mobTrophy(ctx, ResourceLocationArgument.getId(ctx, "pedestal"), null))
                                .then(Commands.argument("rarity", ResourceLocationArgument.id()).suggests(RARITIES)
                                        .executes(ctx -> mobTrophy(ctx, ResourceLocationArgument.getId(ctx, "pedestal"), ResourceLocationArgument.getId(ctx, "rarity"))))))));
        dispatcher.register(root);
    }

    // A mob trophy for each target, as if they'd just slain the mob, shown as it spawns, on the default pedestal or the one
    // named, ranked by the mob's toughness or as named.
    private static int mobTrophy(CommandContext<CommandSourceStack> ctx, @Nullable ResourceLocation pedestalId, @Nullable ResourceLocation rarityId)
            throws CommandSyntaxException {
        ResourceLocation entityId = ResourceLocationArgument.getId(ctx, "entity");
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        // Only a living mob that could drop one (not a player, an armor stand or an item).
        if (type == null || !(type.create(ctx.getSource().getLevel()) instanceof LivingEntity mob) || !MobTrophies.droppable(mob)) {
            throw NOT_A_MOB.create(entityId);
        }
        PedestalBlock pedestal = pedestalId == null ? Pedestals.DEFAULT.get() : pedestal(pedestalId);
        ResourceLocation rank = rarityId == null ? MobTrophies.rarity(mob).id() : rarity(rarityId).id();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer player : targets) {
            Pedestals.give(player, List.of(MobTrophies.trophy(pedestal, type, new CompoundTag(), player.getName().getString(), System.currentTimeMillis(), rank)));
        }
        Component name = type.getDescription();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.trophypedestals.mob_trophy.success", name, targets.size()), true);
        return targets.size();
    }

    /** The pedestal block named, or a command error if it isn't one. */
    public static PedestalBlock pedestal(ResourceLocation id) throws CommandSyntaxException {
        PedestalBlock named = Pedestals.pedestal(id);
        if (named == null) throw NOT_A_PEDESTAL.create(id);
        return named;
    }

    /** The rarity named (a bare name, read as minecraft:epic, being one of this mod's), or a command error if there's none. */
    public static PedestalRarity rarity(ResourceLocation id) throws CommandSyntaxException {
        PedestalRarity named = PedestalRarities.find(id);
        if (named == null && id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) named = PedestalRarities.find(TrophyPedestalsMod.id(id.getPath()));
        if (named == null) throw UNKNOWN_RARITY.create(id);
        return named;
    }
}
