package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.compat.ItemInteractionResult;
import net.ixdarklord.trophypedestals.compat.ValueInput;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class CuratorsStampItem extends Item {
    private static final String COPIED = "PedestalStamp", MODE = "StampMode", LENS = "StampLens";
    public static final String BADGE = "trophypedestals:model_data";

    public enum Mode {
        BOTH, SETTINGS, LENS;

        Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        Component label() {
            return Component.translatable("trophypedestals.stamp.mode." + name().toLowerCase(Locale.ROOT));
        }
    }

    public CuratorsStampItem(Properties properties) {
        super(properties);
    }

    public static boolean holdsCopy(ItemStack stamp) {
        return stamp.getTag() != null && stamp.getTag().contains(COPIED);
    }

    public static Mode mode(ItemStack stamp) {
        return stamp.getTag() == null ? Mode.BOTH : named(Mode.class, stamp.getTag().getString(MODE), Mode.BOTH);
    }

    public static void setMode(ItemStack stamp, Mode mode) {
        CompoundTag root = stamp.getOrCreateTag();
        root.putString(MODE, mode.name());
        if (mode == Mode.BOTH) root.remove(BADGE);
        else root.putString(BADGE, mode.name().toLowerCase(Locale.ROOT));
    }

    public static void copy(ItemStack stamp, PedestalBlockEntity pedestal) {
        CompoundTag tag = new CompoundTag();
        tag.putString("TextMode", pedestal.textMode().name());
        tag.putFloat("Scale", pedestal.scale());
        tag.putFloat("Height", pedestal.height());
        tag.putFloat("Yaw", pedestal.yaw());
        tag.putFloat("Pitch", pedestal.pitch());
        tag.putFloat("Roll", pedestal.roll());
        tag.putBoolean("Spins", pedestal.spins());
        tag.putBoolean("Animates", pedestal.animates());
        tag.putBoolean("Sounds", pedestal.sounds());
        tag.putFloat("TextHeight", pedestal.textHeight());
        tag.putFloat("TextScale", pedestal.textScale());
        tag.putString("Redstone", pedestal.redstone().name());
        tag.putBoolean("Statue", pedestal.statue());
        tag.putString("StatuePose", pedestal.statuePose().name());
        tag.putString("Animation", pedestal.animation());
        tag.putBoolean("Crown", pedestal.crown());
        tag.putFloat("CrownScale", pedestal.crownScale());
        tag.putFloat("CrownX", pedestal.crownX());
        tag.putFloat("CrownY", pedestal.crownY());
        tag.putBoolean("Light", pedestal.light());
        tag.putString("EffectsMode", pedestal.effectsMode().name());
        CompoundTag root = stamp.getOrCreateTag();
        root.putString(MODE, mode(stamp).name());
        root.put(COPIED, tag);
        if (pedestal.lens().isEmpty()) root.remove(LENS);
        else root.put(LENS, pedestal.lens().copyWithCount(1).save(new CompoundTag()));
    }

    private static ItemStack copiedLens(ItemStack stamp) {
        CompoundTag root = stamp.getTag();
        return root == null || !root.contains(LENS) ? ItemStack.EMPTY : ItemStack.of(root.getCompound(LENS));
    }

    private static <E extends Enum<E>> E named(Class<E> type, String name, E fallback) {
        for (E value : type.getEnumConstants()) {
            if (value.name().equals(name)) return value;
        }
        return fallback;
    }

    public ItemInteractionResult paste(ItemStack stamp, Level level, Player player, PedestalBlockEntity pedestal) {
        if (!holdsCopy(stamp)) {
            if (player instanceof ServerPlayer server) server.displayClientMessage(Component.translatable("trophypedestals.stamp.empty"), true);
            return ItemInteractionResult.FAIL;
        }
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        ServerPlayer server = (ServerPlayer) player;
        if (pedestal.locked() || !pedestal.canAdjust(server)) {
            if (!pedestal.locked()) server.displayClientMessage(Component.translatable("trophypedestals.pedestal.not_yours", pedestal.ownerName()), true);
            else if (pedestal.refusal() != null) server.displayClientMessage(pedestal.refusal(), true);
            level.playSound(null, pedestal.getBlockPos(), SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5F, 1.0F);
            return ItemInteractionResult.FAIL;
        }
        Mode mode = mode(stamp);
        ItemStack lens = mode == Mode.SETTINGS ? ItemStack.EMPTY : copiedLens(stamp);
        boolean takesLens = !lens.isEmpty() && !pedestal.glass() && !pedestal.isTrophy() && pedestal.leaderboard() == null;
        if (mode == Mode.LENS && !takesLens) {
            server.displayClientMessage(Component.translatable(lens.isEmpty() ? "trophypedestals.stamp.no_lens_copied" : "trophypedestals.stamp.lens_unfit"), true);
            return ItemInteractionResult.FAIL;
        }
        if (mode != Mode.LENS) applySettings(stamp.getTag().getCompound(COPIED), pedestal);
        boolean missing = takesLens && !ItemStack.isSameItemSameTags(pedestal.lens(), lens) && !fitLens(player, pedestal, lens);
        if (missing) {
            server.displayClientMessage(Component.translatable(mode == Mode.LENS ? "trophypedestals.stamp.lens_only_missing" : "trophypedestals.stamp.lens_missing", lens.getHoverName()), true);
            if (mode == Mode.LENS) return ItemInteractionResult.FAIL;
        } else {
            server.displayClientMessage(Component.translatable(mode == Mode.LENS ? "trophypedestals.stamp.pasted_lens"
                    : takesLens ? "trophypedestals.stamp.pasted_both" : "trophypedestals.stamp.pasted"), true);
        }
        level.playSound(null, pedestal.getBlockPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.2F);
        return ItemInteractionResult.SUCCESS;
    }

    private static void applySettings(CompoundTag saved, PedestalBlockEntity pedestal) {
        ValueInput tag = ValueInput.of(saved);
        boolean trophy = pedestal.isTrophy();
        if (TrophyPedestalsConfig.displaySettings(trophy)) {
            pedestal.setScale(tag.getFloatOr("Scale", pedestal.scale()));
            pedestal.setHeight(tag.getFloatOr("Height", pedestal.height()));
            pedestal.setRotation(tag.getFloatOr("Yaw", pedestal.yaw()), tag.getFloatOr("Pitch", pedestal.pitch()), tag.getFloatOr("Roll", pedestal.roll()));
            pedestal.setCrownPlacement(tag.getFloatOr("CrownScale", pedestal.crownScale()), tag.getFloatOr("CrownX", pedestal.crownX()), tag.getFloatOr("CrownY", pedestal.crownY()));
        }
        if (TrophyPedestalsConfig.textModeSettings(trophy)) {
            pedestal.setTextMode(named(PedestalBlockEntity.TextMode.class, tag.getStringOr("TextMode", ""), pedestal.textMode()));
        }
        if (TrophyPedestalsConfig.textSettings(trophy)) {
            pedestal.setTextHeight(tag.getFloatOr("TextHeight", pedestal.textHeight()));
            pedestal.setTextScale(tag.getFloatOr("TextScale", pedestal.textScale()));
        }
        if (TrophyPedestalsConfig.poseSettings(trophy)) {
            pedestal.setAnimates(tag.getBooleanOr("Animates", pedestal.animates()));
            pedestal.setStatue(tag.getBooleanOr("Statue", pedestal.statue()));
            pedestal.setStatuePose(named(PedestalBlockEntity.StatuePose.class, tag.getStringOr("StatuePose", ""), pedestal.statuePose()));
            pedestal.setAnimation(tag.getStringOr("Animation", pedestal.animation()));
        }
        pedestal.setSpins(tag.getBooleanOr("Spins", pedestal.spins()));
        pedestal.setEffectsMode(named(PedestalBlockEntity.TextMode.class, tag.getStringOr("EffectsMode", ""), pedestal.effectsMode()));
        pedestal.setRedstone(named(PedestalBlockEntity.RedstoneMode.class, tag.getStringOr("Redstone", ""), pedestal.redstone()));
        pedestal.setSounds(tag.getBooleanOr("Sounds", pedestal.sounds()));
        pedestal.setCrown(tag.getBooleanOr("Crown", pedestal.crown()));
        pedestal.setLight(tag.getBooleanOr("Light", pedestal.light()));
    }

    private static boolean fitLens(Player player, PedestalBlockEntity pedestal, ItemStack lens) {
        if (player.getAbilities().instabuild) {
            pedestal.setLens(lens.copyWithCount(1));
            return true;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack held = inventory.getItem(slot);
            if (!ItemStack.isSameItemSameTags(held, lens)) continue;
            held.shrink(1);
            ItemStack old = pedestal.setLens(lens.copyWithCount(1));
            if (!old.isEmpty() && !inventory.add(old)) player.drop(old, false);
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive() || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PedestalBlockEntity pedestal)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            copy(context.getItemInHand(), pedestal);
            server.displayClientMessage(Component.translatable("trophypedestals.stamp.copied"), true);
            context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stamp = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) {
            if (holdsCopy(stamp)) {
                if (player instanceof ServerPlayer server) server.displayClientMessage(Component.translatable("trophypedestals.stamp.mode_locked", mode(stamp).label()), true);
                return InteractionResultHolder.fail(stamp);
            }
            if (player instanceof ServerPlayer server) {
                Mode next = mode(stamp).next();
                setMode(stamp, next);
                server.displayClientMessage(Component.translatable("trophypedestals.stamp.mode", next.label()), true);
                level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.35F, 1.2F);
            }
            return InteractionResultHolder.sidedSuccess(stamp, level.isClientSide());
        }
        if (!holdsCopy(stamp)) return InteractionResultHolder.pass(stamp);
        if (player instanceof ServerPlayer server) {
            Mode mode = mode(stamp);
            stamp.removeTagKey(COPIED);
            stamp.removeTagKey(LENS);
            setMode(stamp, mode);
            server.displayClientMessage(Component.translatable("trophypedestals.stamp.cleared"), true);
            level.playSound(null, player.blockPosition(), SoundEvents.AXE_WAX_OFF, SoundSource.PLAYERS, 0.8F, 1.25F);
        }
        return InteractionResultHolder.sidedSuccess(stamp, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return holdsCopy(stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        Consumer<Component> builder = lines::add;
        if (holdsCopy(stack)) {
            ItemStack lens = copiedLens(stack);
            builder.accept(Component.translatable("trophypedestals.stamp.holds", lens.isEmpty() ? Component.translatable("trophypedestals.stamp.no_lens") : lens.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        builder.accept(Component.translatable("trophypedestals.stamp.mode", mode(stack).label()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("trophypedestals.stamp.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
