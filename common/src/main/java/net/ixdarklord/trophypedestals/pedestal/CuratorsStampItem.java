package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Copies one pedestal's settings and lens on to others: sneak-use a pedestal to copy it, use another to paste, use the
 * air to choose whether it pastes both, the settings or the lens, and sneak-use the air to wipe it. The choice is locked
 * while the stamp holds a copy, so it can't be changed by mistake. Pasting follows the rules of the adjustment screen. A survival player pays for the lens with a matching one from their inventory, and gets
 * back the one it replaces.
 */
public class CuratorsStampItem extends Item {
    private static final String COPIED = "PedestalStamp", MODE = "StampMode";

    /** What the stamp pastes. */
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
        CustomData data = stamp.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(COPIED);
    }

    public static Mode mode(ItemStack stamp) {
        CustomData data = stamp.get(DataComponents.CUSTOM_DATA);
        return data == null ? Mode.BOTH : named(Mode.class, data.copyTag().getStringOr(MODE, ""), Mode.BOTH);
    }

    public static void setMode(ItemStack stamp, Mode mode) {
        CustomData data = stamp.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = data == null ? new CompoundTag() : data.copyTag();
        root.putString(MODE, mode.name());
        stamp.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        // picks the badge its item model shows in inventory slots
        if (mode == Mode.BOTH) stamp.remove(DataComponents.CUSTOM_MODEL_DATA);
        else stamp.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(mode.name().toLowerCase(Locale.ROOT)), List.of()));
    }

    /** Stores a pedestal's settings, and its lens if it has one, on the stamp. */
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
        CompoundTag root = new CompoundTag();
        root.putString(MODE, mode(stamp).name());
        root.put(COPIED, tag);
        stamp.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        if (pedestal.lens().isEmpty()) stamp.remove(Pedestals.LENS.get());
        else stamp.set(Pedestals.LENS.get(), ItemContainerContents.fromItems(List.of(pedestal.lens().copyWithCount(1))));
    }

    private static ItemStack copiedLens(ItemStack stamp) {
        ItemContainerContents lens = stamp.get(Pedestals.LENS.get());
        return lens == null ? ItemStack.EMPTY : lens.copyOne();
    }

    private static <E extends Enum<E>> E named(Class<E> type, String name, E fallback) {
        for (E value : type.getEnumConstants()) {
            if (value.name().equals(name)) return value;
        }
        return fallback;
    }

    /** Applies what's on the stamp to a pedestal, for a player using it on one. */
    public InteractionResult paste(ItemStack stamp, Level level, Player player, PedestalBlockEntity pedestal) {
        if (!holdsCopy(stamp)) {
            if (player instanceof ServerPlayer server) server.sendOverlayMessage(Component.translatable("trophypedestals.stamp.empty"));
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ServerPlayer server = (ServerPlayer) player;
        if (pedestal.locked() || !pedestal.canAdjust(server)) {
            if (!pedestal.locked()) server.sendOverlayMessage(Component.translatable("trophypedestals.pedestal.not_yours", pedestal.ownerName()));
            else if (pedestal.refusal() != null) server.sendOverlayMessage(pedestal.refusal());
            level.playSound(null, pedestal.getBlockPos(), SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5F, 1.0F);
            return InteractionResult.FAIL;
        }
        Mode mode = mode(stamp);
        ItemStack lens = mode == Mode.SETTINGS ? ItemStack.EMPTY : copiedLens(stamp);
        boolean takesLens = !lens.isEmpty() && !pedestal.glass() && !pedestal.isTrophy() && pedestal.leaderboard() == null && !pedestal.mannequin();
        if (mode == Mode.LENS && !takesLens) {
            server.sendOverlayMessage(Component.translatable(lens.isEmpty() ? "trophypedestals.stamp.no_lens_copied" : "trophypedestals.stamp.lens_unfit"));
            return InteractionResult.FAIL;
        }
        if (mode != Mode.LENS) applySettings(stamp.get(DataComponents.CUSTOM_DATA).copyTag().getCompoundOrEmpty(COPIED), pedestal);
        boolean missing = takesLens && !ItemStack.isSameItemSameComponents(pedestal.lens(), lens) && !fitLens(player, pedestal, lens);
        if (missing) {
            server.sendOverlayMessage(Component.translatable(mode == Mode.LENS ? "trophypedestals.stamp.lens_only_missing" : "trophypedestals.stamp.lens_missing", lens.getHoverName()));
            if (mode == Mode.LENS) return InteractionResult.FAIL;
        } else {
            server.sendOverlayMessage(Component.translatable(mode == Mode.LENS ? "trophypedestals.stamp.pasted_lens"
                    : takesLens ? "trophypedestals.stamp.pasted_both" : "trophypedestals.stamp.pasted"));
        }
        level.playSound(null, pedestal.getBlockPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.2F);
        return InteractionResult.SUCCESS;
    }

    /** Applies the settings the server lets players change, as the adjustment screen does. */
    private static void applySettings(CompoundTag tag, PedestalBlockEntity pedestal) {
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

    /** Fits the lens, from the player's inventory unless they're in creative, and hands back the one it replaces. */
    private static boolean fitLens(Player player, PedestalBlockEntity pedestal, ItemStack lens) {
        if (player.hasInfiniteMaterials()) {
            pedestal.setLens(lens.copyWithCount(1));
            return true;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack held = inventory.getItem(slot);
            if (!ItemStack.isSameItemSameComponents(held, lens)) continue;
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
            server.sendOverlayMessage(Component.translatable("trophypedestals.stamp.copied"));
            context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stamp = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) {
            if (holdsCopy(stamp)) {
                if (player instanceof ServerPlayer server) server.sendOverlayMessage(Component.translatable("trophypedestals.stamp.mode_locked", mode(stamp).label()));
                return InteractionResult.FAIL;
            }
            if (player instanceof ServerPlayer server) {
                Mode next = mode(stamp).next();
                setMode(stamp, next);
                server.sendOverlayMessage(Component.translatable("trophypedestals.stamp.mode", next.label()));
                level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.35F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        if (!holdsCopy(stamp)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server) {
            Mode mode = mode(stamp);
            stamp.remove(DataComponents.CUSTOM_DATA);
            stamp.remove(Pedestals.LENS.get());
            setMode(stamp, mode);
            server.sendOverlayMessage(Component.translatable("trophypedestals.stamp.cleared"));
            level.playSound(null, player.blockPosition(), SoundEvents.AXE_WAX_OFF, SoundSource.PLAYERS, 0.8F, 1.25F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return holdsCopy(stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        if (holdsCopy(stack)) {
            ItemStack lens = copiedLens(stack);
            builder.accept(Component.translatable("trophypedestals.stamp.holds", lens.isEmpty() ? Component.translatable("trophypedestals.stamp.no_lens") : lens.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        builder.accept(Component.translatable("trophypedestals.stamp.mode", mode(stack).label()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("trophypedestals.stamp.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
