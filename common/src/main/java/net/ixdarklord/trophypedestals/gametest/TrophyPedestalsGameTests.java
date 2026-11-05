package net.ixdarklord.trophypedestals.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.ixdarklord.coolcatcore.api.gametest.GameTest;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalBlocks;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.api.PedestalSettings;
import net.ixdarklord.trophypedestals.api.TrophyKind;
import net.ixdarklord.trophypedestals.api.TrophyKinds;
import net.ixdarklord.trophypedestals.api.TrophyPedestals;
import net.ixdarklord.trophypedestals.network.AdjustPedestalPayload;
import net.ixdarklord.trophypedestals.pedestal.MobLooks;
import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlock;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.PedestalRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.ixdarklord.trophypedestals.pedestal.Statues;
import net.ixdarklord.trophypedestals.pedestal.TrophyCabinetBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.TrophyDisplayBlock;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Trophy Pedestals in a running game: pedestals, trophies of a test kind of achievement (registered here, as another
 * mod would), mob trophies, statues and the settings mods can lock. Each test makes a fresh player and goes through the
 * real blocks, loot functions, commands and payloads. Data the tests read from a pack is in {@code testpack/}.
 */
public final class TrophyPedestalsGameTests {
    /** The test kind's achievement; its trophies are rare. */
    private static final Identifier SUBJECT = TrophyPedestalsMod.id("test_achievement");

    private static ServerPlayer player(GameTestHelper helper) {
        return helper.makeMockServerPlayerInLevel();
    }

    // The test kind, registered as a mod would from its constructor.
    private static Identifier kind() {
        Identifier id = TrophyPedestalsMod.id("test_kind");
        if (TrophyKinds.get(id) == null) TrophyKinds.register(id, new TrophyKind() {
            @Override
            public Component name(Identifier subject, boolean client) {
                return Component.literal("Test Achievement");
            }

            @Override
            public ItemStack icon(Identifier subject, boolean client) {
                return new ItemStack(Items.DIAMOND);
            }

            @Override
            public @Nullable PedestalRarity rarity(Identifier subject) {
                return subject.equals(SUBJECT) ? PedestalRarities.RARE : null;
            }
        });
        return id;
    }

    @GameTest
    public static void trophiesRecordAnyModsAchievements(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        // A mod's kind names its achievements, gives their icon and their rank.
        ItemStack made = TrophyPedestals.create(kind(), SUBJECT, PedestalRecord.Unlocker.TEAM, "Builders");
        PedestalRecord record = TrophyPedestals.record(made);
        helper.assertTrue(record != null && record.kind().equals(kind()) && record.subject().equals(SUBJECT) && record.unlocker() == PedestalRecord.Unlocker.TEAM
                && record.name().equals("Builders") && made.is(Pedestals.DEFAULT.get().asItem()), "the API makes a trophy on the default pedestal: " + record);
        helper.assertTrue(TrophyPedestals.rarity(made) == PedestalRarities.RARE, "of the kind's rank for it");
        helper.assertTrue(TrophyKinds.icon(record, false).is(Items.DIAMOND) && TrophyKinds.name(record, false).getString().equals("Test Achievement"),
                "showing the kind's icon and name");
        helper.assertTrue(made.get(DataComponents.ITEM_NAME).getContents() instanceof TranslatableContents name
                && name.getKey().equals("trophypedestals.pedestal.trophy_name"), "and named after it: " + made.get(DataComponents.ITEM_NAME));
        helper.assertTrue(TrophyPedestals.record(new ItemStack(Pedestals.DEFAULT.get())) == null, "an empty pedestal records nothing");
        // A kind no mod registers (its mod was removed) still reads, showing its subject's id and no icon.
        PedestalRecord orphan = new PedestalRecord(Identifier.fromNamespaceAndPath("gone", "kind"), SUBJECT, PedestalRecord.Unlocker.PLAYER, "x", 0L, PedestalRarities.COMMON.id());
        helper.assertTrue(TrophyKinds.name(orphan, false).getString().equals(SUBJECT.toString()) && TrophyKinds.icon(orphan, false).isEmpty()
                && TrophyKinds.rarity(orphan.kind(), SUBJECT) == PedestalRarities.COMMON, "an unknown kind shows its id, no icon, and is common");

        // The loot function, as a data pack writes it, recording the entity the loot is for.
        LootParams params = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.CHEST);
        LootContext context = new LootContext.Builder(params).create(Optional.empty());
        LootItemFunction function = LootItemFunctions.ROOT_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"function\": \"trophypedestals:set_trophy\", \"kind\": \"" + kind() + "\", \"subject\": \"" + SUBJECT + "\"}")).getOrThrow();
        PedestalRecord fromLoot = TrophyPedestals.record(function.apply(new ItemStack(Pedestals.BLOCKS_BY_STONE.get(Pedestals.Material.QUARTZ).get()), context));
        helper.assertTrue(fromLoot != null && fromLoot.subject().equals(SUBJECT) && fromLoot.unlocker() == PedestalRecord.Unlocker.PLAYER
                && fromLoot.name().equals(player.getName().getString()) && fromLoot.rarity().equals(PedestalRarities.RARE.id()),
                "set_trophy makes a pedestal a trophy, of the kind's rank: " + fromLoot);
        helper.assertTrue(TrophyPedestals.record(function.apply(new ItemStack(Items.STONE), context)) == null, "and leaves other items alone");
        LootItemFunction legendary = LootItemFunctions.ROOT_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"function\": \"trophypedestals:set_trophy\", \"kind\": \"" + kind() + "\", \"subject\": \"" + SUBJECT + "\", \"rarity\": \"legendary\"}")).getOrThrow();
        helper.assertTrue(TrophyPedestals.rarity(legendary.apply(new ItemStack(Pedestals.DEFAULT.get()), context)) == PedestalRarities.LEGENDARY,
                "set_trophy names a rank by its bare name");
        ItemStack epic = TrophyPedestals.create(Pedestals.DEFAULT.get(), kind(), SUBJECT, PedestalRecord.Unlocker.PLAYER, "x", 0L, PedestalRarities.EPIC);
        helper.assertTrue(TrophyPedestals.rarity(epic) == PedestalRarities.EPIC, "the API names a rank");

        // Mods add ranks; the ranks list in order; an unknown or missing rank shows as common.
        Identifier mythicId = TrophyPedestalsMod.id("test_mythic");
        PedestalRarity mythic = PedestalRarities.find(mythicId);
        if (mythic == null) mythic = PedestalRarities.register(PedestalRarity.builder(mythicId, 6, 0xFF3355, 0xFFFFFF).rings(8, 20.0F).beam(5.0F).build());
        helper.assertTrue(TrophyPedestals.rarity(TrophyPedestals.create(Pedestals.DEFAULT.get(), kind(), SUBJECT, PedestalRecord.Unlocker.PLAYER, "x", 0L, mythic)) == mythic,
                "a mod's rank works as this mod's do");
        List<PedestalRarity> all = PedestalRarities.all();
        helper.assertTrue(all.getFirst() == PedestalRarities.COMMON && all.getLast() == mythic, "ranks list from lowest to highest");
        helper.assertTrue(PedestalRarities.get(Identifier.fromNamespaceAndPath("gone", "rank")) == PedestalRarities.COMMON, "an unknown rank is common");
        PedestalRecord old = PedestalRecord.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"kind\": \"" + kind() + "\", \"subject\": \"" + SUBJECT + "\", \"unlocker\": \"player\", \"unlocked_at\": 0}")).getOrThrow();
        helper.assertTrue(old.rarity().equals(PedestalRarities.COMMON.id()), "a trophy from before ranks is common");
        helper.succeed();
    }

    @GameTest
    public static void craftedPedestalsDisplayAnItem(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, Pedestals.DEFAULT.get().defaultBlockState());
        BlockState state = helper.getLevel().getBlockState(pos);
        PedestalBlockEntity pedestal = (PedestalBlockEntity) helper.getLevel().getBlockEntity(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        // In survival, so it's used up as a player's would be (creative keeps it).
        player.setGameMode(GameType.SURVIVAL);
        ItemStack held = new ItemStack(Items.DIAMOND, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        state.useItemOn(held, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(pedestal.display().is(Items.DIAMOND) && pedestal.display().getCount() == 1 && held.getCount() == 2,
                "an item in hand goes on display, one of it");
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(PedestalBlock.LIT), "and the pedestal lights up");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        state.useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(pedestal.display().isEmpty() && player.getInventory().countItem(Items.DIAMOND) >= 1, "and comes back off with an empty hand");
        helper.assertFalse(helper.getLevel().getBlockState(pos).getValue(PedestalBlock.LIT), "going dark again");
        helper.assertTrue(pedestal.textMode() == PedestalBlockEntity.TextMode.LOOKING && pedestal.scale() == 1.0F, "its text shows when looked at by default, at the usual size");
        // The adjustment screen's changes, as the server takes them: anyone may adjust a pedestal with no owner.
        player.setPos(pos.getCenter());
        new AdjustPedestalPayload(pos, PedestalBlockEntity.TextMode.HIDDEN, 1.5F, 1.0F, false, -2.0F, 90.0F, false, PedestalBlockEntity.RedstoneMode.SHOW, false, false, false, PedestalBlockEntity.StatuePose.STANDING, PedestalBlockEntity.IDLE, 1.0F, true, 1.0F, 0.0F, 0.0F, true, PedestalBlockEntity.Access.PRIVATE).apply(player);
        helper.assertTrue(pedestal.textMode() == PedestalBlockEntity.TextMode.HIDDEN && pedestal.scale() == 1.5F && pedestal.height() == 1.0F
                && !pedestal.spins() && pedestal.textHeight() == -2.0F && pedestal.facing() == 90.0F && !pedestal.effects()
                && pedestal.redstone() == PedestalBlockEntity.RedstoneMode.SHOW && !pedestal.displayed() && !pedestal.sounds() && !pedestal.animates(),
                "the screen hides its text, resizes it, raises it, stills it, freezes it, lowers its text and turns it");
        // A redstone signal shows it.
        helper.getLevel().setBlockAndUpdate(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.assertTrue(pedestal.powered() && pedestal.displayed(), "a redstone signal shows a pedestal set to show while powered");
        helper.getLevel().setBlockAndUpdate(pos.north(), Blocks.AIR.defaultBlockState());
        new AdjustPedestalPayload(pos, PedestalBlockEntity.TextMode.ALWAYS, 99.0F, 99.0F, true, 0.0F, 0.0F, true, PedestalBlockEntity.RedstoneMode.IGNORE, true, true, false, PedestalBlockEntity.StatuePose.STANDING, PedestalBlockEntity.IDLE, 1.0F, true, 1.0F, 0.0F, 0.0F, true, PedestalBlockEntity.Access.PRIVATE).apply(player);
        helper.assertTrue(pedestal.textMode() == PedestalBlockEntity.TextMode.ALWAYS && pedestal.scale() == PedestalBlockEntity.MAX_SCALE
                && pedestal.height() == PedestalBlockEntity.MAX_HEIGHT, "within its limits");
        // Owned by someone else, only they (and operators) may, unless the server lets anyone.
        ServerPlayer owner = player(helper);
        pedestal.setOwner(owner);
        boolean op = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
        helper.assertTrue(pedestal.canAdjust(owner) && pedestal.canAdjust(player) == (op || owner.getUUID().equals(player.getUUID())), "its owner may adjust it");
        TrophyPedestalsConfig.PEDESTAL_ACCESS.set(TrophyPedestalsConfig.PedestalAccess.ANYONE);
        try {
            helper.assertTrue(pedestal.canAdjust(player), "anyone may when the server says so");
        } finally {
            TrophyPedestalsConfig.PEDESTAL_ACCESS.reset();
        }
        player.setShiftKeyDown(false);
        state = helper.getLevel().getBlockState(pos);

        pedestal.applyComponentsFromItemStack(TrophyPedestals.create(kind(), SUBJECT, PedestalRecord.Unlocker.PLAYER, "x"));
        ItemStack more = new ItemStack(Items.DIAMOND);
        state.useItemOn(more, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(pedestal.display().isEmpty() && more.getCount() == 1, "a trophy takes no item");
        helper.succeed();
    }

    @GameTest
    public static void mobsDropTrophiesNowAndThen(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
        DamageSource byPlayer = helper.getLevel().damageSources().playerAttack(player);
        RandomSource random = RandomSource.create(1L);
        TrophyPedestalsConfig.MOB_TROPHY_CHANCE.set(1.0);
        TrophyPedestalsConfig.MOB_TROPHY_RARITY.set(TrophyPedestalsConfig.MobTrophyRarity.STRENGTH);
        try {
            ItemStack trophy = MobTrophies.roll(zombie, byPlayer, random);
            MobTrophyRecord record = trophy == null ? null : trophy.get(Pedestals.MOB_TROPHY.get());
            helper.assertTrue(record != null && record.type() == EntityType.ZOMBIE && record.slayer().equals(player.getName().getString())
                    && record.rarity().equals(PedestalRarities.UNCOMMON.id()), "a mob a player kills can drop its trophy: " + record);
            helper.assertTrue(MobTrophies.roll(zombie, helper.getLevel().damageSources().generic(), random) == null, "but not one that dies otherwise");
            helper.assertTrue(MobTrophies.droppable(zombie) && !MobTrophies.droppable(helper.spawn(EntityType.ARMOR_STAND, 2, 1, 1)) && !MobTrophies.droppable(player),
                    "any living mob counts, not an armor stand or a player");
            helper.assertTrue(record.icon().getItem() instanceof SpawnEggItem, "a mob's icon is its spawn egg");
            helper.assertTrue(MobTrophies.tagged(EntityType.WITHER) == PedestalRarities.LEGENDARY && MobTrophies.tagged(EntityType.WARDEN) == PedestalRarities.EPIC,
                    "bosses give grander trophies");
            helper.assertTrue(MobTrophies.rarity(zombie) == PedestalRarities.UNCOMMON, "a zombie's is uncommon by its strength: " + MobTrophies.strength(zombie));
            TrophyPedestalsConfig.MOB_TROPHY_RARITY.set(TrophyPedestalsConfig.MobTrophyRarity.NONE);
            ItemStack plain = MobTrophies.roll(zombie, byPlayer, random);
            helper.assertTrue(plain != null && PedestalRarities.isNone(plain.get(Pedestals.MOB_TROPHY.get()).rarity()), "and none when the server says so");
            // By kills: the more zombies the player has killed, the grander a zombie's trophy.
            TrophyPedestalsConfig.MOB_TROPHY_RARITY.set(TrophyPedestalsConfig.MobTrophyRarity.KILLS);
            player.getStats().setValue(player, Stats.ENTITY_KILLED.get(EntityType.ZOMBIE), 30);
            ItemStack veteran = MobTrophies.roll(zombie, byPlayer, random);
            helper.assertTrue(veteran != null && veteran.get(Pedestals.MOB_TROPHY.get()).rarity().equals(PedestalRarities.RARE.id()),
                    "thirty zombies killed make a zombie's trophy rare");
            player.getStats().setValue(player, Stats.ENTITY_KILLED.get(EntityType.ZOMBIE), 0);
            ItemStack first = MobTrophies.roll(zombie, byPlayer, random);
            helper.assertTrue(first != null && first.get(Pedestals.MOB_TROPHY.get()).rarity().equals(PedestalRarities.COMMON.id()),
                    "and a first one common");
            helper.assertTrue(MobTrophies.byKills(EntityType.ZOMBIE, 100) == PedestalRarities.LEGENDARY && MobTrophies.byKills(EntityType.ZOMBIE, 9) == PedestalRarities.COMMON,
                    "a hundred kills make it legendary");
            // Bosses need fewer, by their tags; a mob named by id wins over its tags.
            helper.assertTrue(MobTrophies.byKills(EntityType.WITHER, 5) == PedestalRarities.LEGENDARY && MobTrophies.byKills(EntityType.WARDEN, 5) == PedestalRarities.RARE,
                    "five withers make a legendary trophy, five wardens a rare one");
            TrophyPedestalsConfig.KILLS_OVERRIDES.set(List.of("not an entry", "#trophypedestals:legendary_mob_trophies = 1, 2, 3, 5", "minecraft:wither = 10, 20, 30, 40"));
            helper.assertTrue(MobTrophies.byKills(EntityType.WITHER, 5) == PedestalRarities.COMMON && MobTrophies.byKills(EntityType.ENDER_DRAGON, 5) == PedestalRarities.LEGENDARY,
                    "the wither's own entry wins over its tag's; the dragon keeps the tag's; an unreadable entry is skipped");
            TrophyPedestalsConfig.MOB_TROPHY_RARITY.set(TrophyPedestalsConfig.MobTrophyRarity.STRENGTH);
            TrophyPedestalsConfig.MOB_TROPHIES.set(false);
            helper.assertTrue(MobTrophies.roll(zombie, byPlayer, random) == null, "and none while they're turned off");
            TrophyPedestalsConfig.MOB_TROPHIES.reset();
            TrophyPedestalsConfig.MOB_TROPHY_CHANCE.set(0.0);
            helper.assertTrue(MobTrophies.roll(zombie, byPlayer, random) == null, "or at no chance");
        } finally {
            TrophyPedestalsConfig.MOB_TROPHIES.reset();
            TrophyPedestalsConfig.MOB_TROPHY_CHANCE.reset();
            TrophyPedestalsConfig.MOB_TROPHY_RARITY.reset();
            TrophyPedestalsConfig.KILLS_OVERRIDES.reset();
        }
        // Placed, the pedestal keeps it and lights up as a trophy.
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        ItemStack trophy = MobTrophies.trophy(Pedestals.DEFAULT.get(), EntityType.ZOMBIE, "x", 0L, PedestalRarities.RARE);
        helper.getLevel().setBlockAndUpdate(pos, Pedestals.DEFAULT.get().defaultBlockState());
        PedestalBlockEntity pedestal = (PedestalBlockEntity) helper.getLevel().getBlockEntity(pos);
        pedestal.applyComponentsFromItemStack(trophy);
        helper.assertTrue(pedestal.isTrophy() && pedestal.mobTrophy() != null && PedestalRarities.RARE.id().equals(pedestal.rarity()), "the placed pedestal keeps its mob");
        // Set to show only while powered, it turns off (its light going out) until a signal turns it back on.
        helper.getLevel().setBlockAndUpdate(pos, helper.getLevel().getBlockState(pos).setValue(PedestalBlock.LIT, true));
        pedestal.setRedstone(PedestalBlockEntity.RedstoneMode.SHOW);
        helper.assertTrue(!pedestal.on() && !helper.getLevel().getBlockState(pos).getValue(PedestalBlock.LIT), "an unpowered trophy set to show while powered is off and dark");
        helper.getLevel().setBlockAndUpdate(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.assertTrue(pedestal.on() && helper.getLevel().getBlockState(pos).getValue(PedestalBlock.LIT), "and a signal turns it on, lit");
        helper.assertTrue(PedestalBlock.light(helper.getLevel().getBlockState(pos)) == 13,
                "as brightly as a rare trophy's rank says (13 of 15): " + PedestalBlock.light(helper.getLevel().getBlockState(pos)));
        helper.getLevel().setBlockAndUpdate(pos.north(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(!pedestal.on() && !helper.getLevel().getBlockState(pos).getValue(PedestalBlock.LIT), "and off again without it");
        helper.succeed();
    }

    @GameTest
    public static void mobTrophiesKeepTheMobsLook(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
        zombie.setBaby(true);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        zombie.setHealth(1.0F);
        zombie.setCustomName(Component.literal("Bob"));
        CompoundTag look = MobLooks.capture(zombie);
        helper.assertTrue(!look.isEmpty() && !look.contains("Pos") && !look.contains("UUID") && !look.contains("Health") && !look.contains("Brain"),
                "a trophy keeps how its mob looked, not where it was or how it was: " + look.keySet());
        Zombie shown = EntityType.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.LOAD);
        helper.assertTrue(shown != null, "a zombie can be made");
        MobLooks.apply(shown, look, helper.getLevel().registryAccess());
        helper.assertTrue(shown.isBaby() && shown.getItemBySlot(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET)
                && shown.getHealth() == shown.getMaxHealth(), "and the mob made to show it looks the same, at full health");
        // Given by a pack, a look can't move the mob or change its health.
        CompoundTag odd = look.copy();
        odd.putFloat("Health", 0.0F);
        odd.putBoolean("NoAI", false);
        Zombie packed = EntityType.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.LOAD);
        MobLooks.apply(packed, odd, helper.getLevel().registryAccess());
        helper.assertTrue(packed.getHealth() == packed.getMaxHealth(), "a look given by a pack can't kill the mob shown");
        MobTrophyRecord record = new MobTrophyRecord(Identifier.withDefaultNamespace("zombie"), "x", 0L, PedestalRarities.COMMON.id(), look);
        MobTrophyRecord read = MobTrophyRecord.CODEC.parse(NbtOps.INSTANCE,
                MobTrophyRecord.CODEC.encodeStart(NbtOps.INSTANCE, record).getOrThrow()).getOrThrow();
        helper.assertTrue(read.equals(record) && new MobTrophyRecord(record.entity(), "x", 0L, record.rarity()).look().isEmpty(),
                "the look is saved with the record, and an old record has none");
        helper.assertTrue(record.mobName().getString().equals("Bob") && new MobTrophyRecord(record.entity(), "x", 0L, record.rarity()).mobName().getString().equals("Zombie"),
                "a named mob's trophy goes by its name: " + record.mobName().getString());
        helper.succeed();
    }

    @GameTest
    public static void packsCommandsAndModsGiveMobTrophies(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        String name = player.getName().getString();
        // In a mob's own loot table: the mob that died, as it looked, slain by the player who last hurt it.
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
        zombie.setBaby(true);
        LootItemFunction fromMob = LootItemFunctions.ROOT_CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"function\": \"trophypedestals:set_mob_trophy\"}")).getOrThrow();
        LootParams deathParams = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.THIS_ENTITY, zombie).withParameter(LootContextParams.DAMAGE_SOURCE, helper.getLevel().damageSources().playerAttack(player))
                .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).create(LootContextParamSets.ENTITY);
        ItemStack dropped = fromMob.apply(new ItemStack(Pedestals.DEFAULT.get()), new LootContext.Builder(deathParams).create(Optional.empty()));
        MobTrophyRecord fromDeath = TrophyPedestals.mobRecord(dropped);
        helper.assertTrue(fromDeath != null && fromDeath.type() == EntityType.ZOMBIE && fromDeath.slayer().equals(name)
                && fromDeath.look().getBooleanOr("IsBaby", false) && fromDeath.rarity().equals(PedestalRarities.UNCOMMON.id()),
                "set_mob_trophy in a mob's loot table makes its trophy, as it looked: " + fromDeath);
        // From a chest or an advancement: the mob named, of the rank named, slain by the player the loot is for.
        LootItemFunction named = LootItemFunctions.ROOT_CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"function\": \"trophypedestals:set_mob_trophy\", \"entity\": \"minecraft:wither\", \"rarity\": \"epic\"}")).getOrThrow();
        LootParams chestParams = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.CHEST);
        LootContext chest = new LootContext.Builder(chestParams).create(Optional.empty());
        MobTrophyRecord fromChest = TrophyPedestals.mobRecord(named.apply(new ItemStack(Pedestals.BLOCKS_BY_STONE.get(Pedestals.Material.QUARTZ).get()), chest));
        helper.assertTrue(fromChest != null && fromChest.type() == EntityType.WITHER && fromChest.slayer().equals(name) && fromChest.look().isEmpty()
                && fromChest.rarity().equals(PedestalRarities.EPIC.id()), "and from a chest, the mob it names: " + fromChest);
        helper.assertTrue(TrophyPedestals.mobRecord(fromMob.apply(new ItemStack(Pedestals.DEFAULT.get()), chest)) == null,
                "naming no mob where the loot isn't for one leaves the pedestal empty");
        helper.assertTrue(TrophyPedestals.mobRecord(named.apply(new ItemStack(Items.STONE), chest)) == null, "and other items are left alone");
        // The command, ranked by the mob's toughness unless it names a rank; never for something that isn't a mob.
        int before = countMobTrophies(player);
        helper.getLevel().getServer().getCommands().performPrefixedCommand(helper.getLevel().getServer().createCommandSourceStack().withEntity(player).withSuppressedOutput(),
                "trophypedestals mob_trophy @s minecraft:wither");
        helper.getLevel().getServer().getCommands().performPrefixedCommand(helper.getLevel().getServer().createCommandSourceStack().withEntity(player).withSuppressedOutput(),
                "trophypedestals mob_trophy @s minecraft:creeper trophypedestals:quartz_pedestal rare");
        helper.getLevel().getServer().getCommands().performPrefixedCommand(helper.getLevel().getServer().createCommandSourceStack().withEntity(player).withSuppressedOutput(),
                "trophypedestals mob_trophy @s minecraft:arrow");
        helper.assertTrue(countMobTrophies(player) == before + 2, "/trophypedestals mob_trophy gives them, and not for an arrow: " + (countMobTrophies(player) - before));
        boolean witherLegendary = false, creeperRare = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            MobTrophyRecord record = TrophyPedestals.mobRecord(stack);
            if (record == null) continue;
            witherLegendary |= record.type() == EntityType.WITHER && record.rarity().equals(PedestalRarities.LEGENDARY.id());
            creeperRare |= record.type() == EntityType.CREEPER && record.rarity().equals(PedestalRarities.RARE.id()) && stack.is(Pedestals.BLOCKS_BY_STONE.get(Pedestals.Material.QUARTZ).get().asItem());
        }
        helper.assertTrue(witherLegendary && creeperRare, "a wither's is legendary by its toughness, and a named rank and pedestal are kept");
        // The API, from a living mob.
        ItemStack made = TrophyPedestals.createMob(zombie, "Alex");
        MobTrophyRecord fromApi = TrophyPedestals.mobRecord(made);
        helper.assertTrue(fromApi != null && fromApi.slayer().equals("Alex") && fromApi.look().getBooleanOr("IsBaby", false)
                && TrophyPedestals.rarity(made) == PedestalRarities.UNCOMMON, "the API makes one from a mob, as it looks: " + fromApi);
        helper.succeed();
    }

    private static int countMobTrophies(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).has(Pedestals.MOB_TROPHY.get())) count++;
        }
        return count;
    }

    @GameTest
    public static void customPedestalsTakeTheirMaterialFromData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // The item, named after its material, and with none just "Pedestal".
        ItemStack amethyst = new ItemStack(Pedestals.CUSTOM_ITEM.get());
        helper.assertTrue(amethyst.getHoverName().getContents() instanceof TranslatableContents plain
                && plain.getKey().equals("block.trophypedestals.custom_pedestal"), "a custom pedestal with no material is just a pedestal: " + amethyst.getHoverName());
        amethyst.set(Pedestals.PEDESTAL_MATERIAL.get(), BuiltInRegistries.BLOCK.getKey(Blocks.AMETHYST_BLOCK));
        helper.assertTrue(amethyst.getHoverName().getContents() instanceof TranslatableContents named
                && named.getKey().equals("block.trophypedestals.custom_pedestal.named"), "and one of amethyst is named after it: " + amethyst.getHoverName());
        // Placed, it keeps its material, and breaking it gives it back.
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, Pedestals.CUSTOM.get().defaultBlockState());
        PedestalBlockEntity pedestal = (PedestalBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(pedestal != null, "a custom pedestal has a pedestal's block entity");
        pedestal.applyComponentsFromItemStack(amethyst);
        helper.assertTrue(BuiltInRegistries.BLOCK.getKey(Blocks.AMETHYST_BLOCK).equals(pedestal.material()), "placed, it keeps its material");
        List<ItemStack> drops = Block.getDrops(level.getBlockState(pos), level, pos, pedestal);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(Pedestals.CUSTOM_ITEM.get())
                && BuiltInRegistries.BLOCK.getKey(Blocks.AMETHYST_BLOCK).equals(drops.getFirst().get(Pedestals.PEDESTAL_MATERIAL.get())),
                "and broken, it drops a pedestal of the same material: " + drops);
        // A trophy can be one too, and keeps its own name.
        ItemStack trophy = MobTrophies.trophy(Pedestals.CUSTOM.get(), EntityType.ZOMBIE, "x", 0L, PedestalRarities.RARE);
        trophy.set(Pedestals.PEDESTAL_MATERIAL.get(), BuiltInRegistries.BLOCK.getKey(Blocks.AMETHYST_BLOCK));
        helper.assertTrue(trophy.getHoverName().getContents() instanceof TranslatableContents name
                && name.getKey().equals("trophypedestals.pedestal.trophy_name"), "a trophy on one keeps its trophy's name");
        // The example pack's recipe (read from the pack, which isn't loaded as a data pack) makes an amethyst one. Only
        // this mod's own test runs point to the pack; another mod's runs these tests without it.
        String pack = System.getProperty("trophypedestals.testpack");
        if (pack == null) {
            helper.succeed();
            return;
        }
        try {
            JsonObject recipe = JsonParser.parseString(Files.readString(Path.of(pack,
                    "data", "trophypedestals_example", "recipe", "amethyst_pedestal.json"))).getAsJsonObject();
            RegistryOps<JsonElement> ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            helper.assertTrue(Recipe.CODEC.parse(ops, recipe).isSuccess(), "the example pack's amethyst pedestal recipe reads");
            ItemStack made = ItemStack.CODEC.parse(ops, recipe.get("result")).getOrThrow();
            helper.assertTrue(made.is(Pedestals.CUSTOM_ITEM.get()) && BuiltInRegistries.BLOCK.getKey(Blocks.AMETHYST_BLOCK).equals(made.get(Pedestals.PEDESTAL_MATERIAL.get())),
                    "and makes an amethyst pedestal: " + made);
        } catch (IOException e) {
            throw helper.assertionException("the example pack's amethyst pedestal recipe can be read: " + e);
        }
        helper.succeed();
    }

    @GameTest
    public static void playersGetStatues(GameTestHelper helper) {
        ServerPlayer victim = player(helper), killer = player(helper);
        RandomSource random = RandomSource.create(1L);
        DamageSource byKiller = helper.getLevel().damageSources().playerAttack(killer);
        // A player another player kills drops no trophy unless the server turns player trophies on.
        TrophyPedestalsConfig.MOB_TROPHY_CHANCE.set(1.0);
        try {
            helper.assertTrue(MobTrophies.roll(victim, byKiller, random) == null, "player trophies are off by default");
            TrophyPedestalsConfig.PLAYER_TROPHIES.set(true);
            ItemStack trophy = MobTrophies.roll(victim, byKiller, random);
            MobTrophyRecord record = trophy == null ? null : TrophyPedestals.mobRecord(trophy);
            helper.assertTrue(record != null && record.type() == Statues.FIGURE && record.look().contains("profile")
                    && record.slayer().equals(killer.getName().getString()) && record.mobName().getString().equals(victim.getName().getString()),
                    "turned on, it's the player's statue, slain by their killer and named after them: " + record);
            helper.assertTrue(record.icon().is(Items.PLAYER_HEAD), "its icon is their head");
            helper.assertTrue(MobTrophies.roll(victim, helper.getLevel().damageSources().playerAttack(victim), random) == null, "but not for their own death");
        } finally {
            TrophyPedestalsConfig.PLAYER_TROPHIES.reset();
            TrophyPedestalsConfig.MOB_TROPHY_CHANCE.reset();
        }
        // An achievement trophy earned by a player can show their statue; one unlocked by a team can't.
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, Pedestals.DEFAULT.get().defaultBlockState());
        PedestalBlockEntity pedestal = (PedestalBlockEntity) helper.getLevel().getBlockEntity(pos);
        pedestal.applyComponentsFromItemStack(TrophyPedestals.create(kind(), SUBJECT, PedestalRecord.Unlocker.PLAYER, victim.getName().getString()));
        helper.assertTrue(pedestal.canShowStatue() && !pedestal.statue(), "a player's achievement trophy can show their statue, and shows its icon by default");
        victim.setPos(pos.getCenter());
        new AdjustPedestalPayload(pos, PedestalBlockEntity.TextMode.LOOKING, 1.0F, 0.0F, true, 0.0F, 0.0F, true, PedestalBlockEntity.RedstoneMode.IGNORE, true, true, true, PedestalBlockEntity.StatuePose.STANDING, PedestalBlockEntity.IDLE, 1.0F, true, 1.0F, 0.0F, 0.0F, true, PedestalBlockEntity.Access.PRIVATE).apply(victim);
        helper.assertTrue(pedestal.statue() && pedestal.showsStatue(), "the screen sets it to show the statue");
        PedestalSettings.set(pedestal, PedestalSettings.get(pedestal).toBuilder().statuePose(PedestalBlockEntity.StatuePose.SLEEPING).build());
        PedestalBlockEntity read = (PedestalBlockEntity) BlockEntity.loadStatic(pos, pedestal.getBlockState(),
                pedestal.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(read != null && read.statuePose() == PedestalBlockEntity.StatuePose.SLEEPING && read.statuePose().lying(),
                "a statue can be posed, and keeps its pose");
        // It can play an animation on loop, kept with it; a name that isn't one plays as idle.
        PedestalSettings.set(pedestal, PedestalSettings.get(pedestal).toBuilder().animation("walk").build());
        read = (PedestalBlockEntity) BlockEntity.loadStatic(pos, pedestal.getBlockState(),
                pedestal.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(read != null && read.animation().equals("walk"), "a statue keeps the animation it plays");
        // A legendary one's crown can be hidden, and stays hidden.
        helper.assertTrue(pedestal.crown(), "a pedestal wears its crown by default");
        PedestalSettings.set(pedestal, PedestalSettings.get(pedestal).toBuilder().crown(false).build());
        read = (PedestalBlockEntity) BlockEntity.loadStatic(pos, pedestal.getBlockState(),
                pedestal.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(read != null && !read.crown(), "a hidden crown stays hidden");
        // Its showcase light can be turned off, and stays off.
        helper.assertTrue(pedestal.light(), "a pedestal's showcase light is on by default");
        PedestalSettings.set(pedestal, PedestalSettings.get(pedestal).toBuilder().light(false).build());
        read = (PedestalBlockEntity) BlockEntity.loadStatic(pos, pedestal.getBlockState(),
                pedestal.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(read != null && !read.light(), "a showcase light turned off stays off");
        pedestal.setAnimation("../Not An Animation");
        helper.assertTrue(pedestal.animation().equals(PedestalBlockEntity.IDLE), "an animation name that isn't one plays as idle");
        pedestal.applyComponentsFromItemStack(TrophyPedestals.create(kind(), SUBJECT, PedestalRecord.Unlocker.TEAM, "Builders"));
        helper.assertFalse(pedestal.canShowStatue(), "a team's can't");
        helper.succeed();
    }

    @GameTest
    public static void modsCanLockPedestalSettings(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, Pedestals.DEFAULT.get().defaultBlockState());
        PedestalBlockEntity pedestal = (PedestalBlockEntity) helper.getLevel().getBlockEntity(pos);
        player.setPos(pos.getCenter());
        helper.assertTrue(PedestalBlocks.pedestalAt(helper.getLevel(), pos).orElse(null) == pedestal
                && PedestalBlocks.pedestalAt(helper.getLevel(), pos.above()).isEmpty(), "a mod finds the pedestal at a position, and none where there's none");
        helper.assertTrue(pedestal.canAdjust(player) && !PedestalSettings.isLocked(pedestal), "a pedestal with no owner starts open to anyone");
        // Forced: set and locked, with a message for anyone who tries.
        Component refusal = Component.literal("Hands off");
        PedestalSettings.force(pedestal, PedestalSettings.Settings.builder().scale(2.0F).spins(false).facing(90.0F).build(), refusal);
        helper.assertTrue(pedestal.scale() == 2.0F && !pedestal.spins() && pedestal.facing() == 90.0F && PedestalSettings.isLocked(pedestal)
                && !pedestal.canAdjust(player) && PedestalSettings.refusal(pedestal).orElseThrow().getString().equals("Hands off"),
                "a mod sets its settings and locks them, with its message");
        new AdjustPedestalPayload(pos, PedestalBlockEntity.TextMode.ALWAYS, 5.0F, 1.0F, true, 0.0F, 0.0F, true, PedestalBlockEntity.RedstoneMode.IGNORE, true, true, false, PedestalBlockEntity.StatuePose.STANDING, PedestalBlockEntity.IDLE, 1.0F, true, 1.0F, 0.0F, 0.0F, true, PedestalBlockEntity.Access.PRIVATE).apply(player);
        helper.assertTrue(pedestal.scale() == 2.0F && !pedestal.spins() && pedestal.textMode() == PedestalBlockEntity.TextMode.LOOKING,
                "and a player's screen can't change them");
        // Saved and read back, still locked with its message.
        PedestalBlockEntity read = (PedestalBlockEntity) BlockEntity.loadStatic(pos, pedestal.getBlockState(),
                pedestal.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(read != null && read.locked() && read.refusal() != null && read.refusal().getString().equals("Hands off") && read.scale() == 2.0F,
                "the lock and its message are saved");
        // A silent lock has no message; unlocked, players with access can change it again.
        PedestalSettings.lock(pedestal);
        helper.assertTrue(PedestalSettings.refusal(pedestal).orElseThrow().getContents() instanceof TranslatableContents locked
                && locked.getKey().equals("trophypedestals.pedestal.locked"), "a lock with no message of the mod's own uses Trophy Pedestals'");
        PedestalSettings.lock(pedestal, null);
        helper.assertTrue(PedestalSettings.isLocked(pedestal) && PedestalSettings.refusal(pedestal).isEmpty(), "a lock can refuse silently");
        PedestalSettings.unlock(pedestal);
        helper.assertTrue(pedestal.canAdjust(player) && PedestalSettings.get(pedestal).scale() == 2.0F, "and unlocked, it's open again, keeping its settings");
        helper.succeed();
    }

    @GameTest
    public static void displaysComePlainAndTakeTrims(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos.south(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Pedestals.SHELVES.get(Pedestals.Material.OAK).get().defaultBlockState().setValue(TrophyDisplayBlock.FACING, Direction.NORTH));
        TrophyCabinetBlockEntity shelf = (TrophyCabinetBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(shelf != null && shelf.trim() == null && !level.getBlockState(pos).getValue(TrophyDisplayBlock.GOLD_TRIM), "a shelf comes with no trim");
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        level.getBlockState(pos).useItemOn(new ItemStack(Items.GOLD_INGOT), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(TrophyDisplayBlock.GOLD.equals(shelf.trim()) && level.getBlockState(pos).getValue(TrophyDisplayBlock.GOLD_TRIM), "a gold ingot trims it with gold");
        level.getBlockState(pos).useItemOn(new ItemStack(Items.COPPER_BLOCK), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(BuiltInRegistries.BLOCK.getKey(Blocks.COPPER_BLOCK).equals(shelf.trim()) && !level.getBlockState(pos).getValue(TrophyDisplayBlock.GOLD_TRIM),
                "a block trims it with that block");
        List<ItemStack> drops = Block.getDrops(level.getBlockState(pos), level, pos, shelf);
        helper.assertTrue(drops.size() == 1 && BuiltInRegistries.BLOCK.getKey(Blocks.COPPER_BLOCK).equals(drops.getFirst().get(Pedestals.TRIM_MATERIAL.get())),
                "broken, it keeps its trim: " + drops);
        level.getBlockState(pos).useItemOn(new ItemStack(Items.IRON_AXE), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(shelf.trim() == null && !level.getBlockState(pos).getValue(TrophyDisplayBlock.GOLD_TRIM), "and an axe takes it off");
        helper.succeed();
    }
}
