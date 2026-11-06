package net.ixdarklord.trophypedestals.pedestal;
import net.ixdarklord.trophypedestals.compat.ValueInput;
import net.ixdarklord.trophypedestals.compat.ValueOutput;
import net.ixdarklord.trophypedestals.compat.component.Components;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A trophy cabinet half: its two trophies (empty where there's none), on the viewer's left and right. */
public class TrophyCabinetBlockEntity extends BlockEntity {
    public static final int SLOTS = 2;
    private static final String[] KEYS = {"Left", "Right"};
    private static final String TRIM = "Trim", POWERED = "Powered";
    private static final String[] SETTINGS_KEYS = {"LeftSettings", "RightSettings"};
    private final ItemStack[] items = {ItemStack.EMPTY, ItemStack.EMPTY};
    // How each spot's trophy is shown (as a new pedestal until changed; back to that when its trophy comes off), and
    // whether a redstone signal reaches the display (for the settings that answer it).
    private final DisplayedTrophySettings[] settings = {DisplayedTrophySettings.DEFAULT, DisplayedTrophySettings.DEFAULT};
    private boolean powered;
    // The block its trim is made of, in place of its gold (null for gold).
    private @Nullable ResourceLocation trim;
    // On clients: each trophy as a pedestal of its own, not in the world, for its renderer to draw as one; and the trophy
    // each was made for.
    private final @Nullable PedestalBlockEntity[] shown = new PedestalBlockEntity[SLOTS];
    private final ItemStack[] shownFor = {ItemStack.EMPTY, ItemStack.EMPTY};
    private final @Nullable DisplayedTrophySettings[] shownWith = new DisplayedTrophySettings[SLOTS];
    private final boolean[] shownPowered = new boolean[SLOTS];

    public TrophyCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(Pedestals.CABINET_ENTITY.get(), pos, state);
    }

    public ItemStack item(int slot) {
        return items[slot];
    }

    // (A trophy put on or taken off: its spot's settings go back to a new pedestal's.)
    public void setItem(int slot, ItemStack stack) {
        items[slot] = stack;
        settings[slot] = DisplayedTrophySettings.DEFAULT;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /**
     * The trophy on this spot as a pedestal of its own (made from its item, as placing it would make one, standing where
     * this block is but not in the world), for drawing it as it would be drawn on its own pedestal; null if the spot's
     * empty. Made again when the trophy changes.
     */
    public @Nullable PedestalBlockEntity shownPedestal(int slot) {
        ItemStack stack = items[slot];
        if (stack.isEmpty() || level == null) return null;
        if (shown[slot] == null || !ItemStack.matches(shownFor[slot], stack) || shownWith[slot] != settings[slot] || shownPowered[slot] != powered) {
            BlockState state = stack.getItem() instanceof BlockItem item && item.getBlock() instanceof PedestalBlock
                    ? item.getBlock().defaultBlockState() : Pedestals.DEFAULT.get().defaultBlockState();
            PedestalBlockEntity pedestal = new PedestalBlockEntity(worldPosition, state);
            pedestal.setLevel(level);
            Components.applyToBlockEntity(pedestal, stack);
            settings[slot].applyTo(pedestal, powered);
            shown[slot] = pedestal;
            shownFor[slot] = stack.copy();
            shownWith[slot] = settings[slot];
            shownPowered[slot] = powered;
        }
        return shown[slot];
    }

    public DisplayedTrophySettings settings(int slot) {
        return settings[slot];
    }

    public void setSettings(int slot, DisplayedTrophySettings settings) {
        this.settings[slot] = settings;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** Records whether a redstone signal reaches it, sending a change to clients. */
    public void setPowered(boolean powered) {
        if (this.powered == powered) return;
        this.powered = powered;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public @Nullable ResourceLocation trim() {
        return trim;
    }

    public void setTrim(@Nullable ResourceLocation trim) {
        this.trim = trim;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ValueOutput output = ValueOutput.of(tag);
        if (trim != null) output.store(TRIM, ResourceLocation.CODEC, trim);
        if (powered) output.putBoolean(POWERED, true);
        for (int i = 0; i < SLOTS; i++) {
            if (!items[i].isEmpty()) output.store(KEYS[i], ItemStack.CODEC, items[i]);
            if (!settings[i].equals(DisplayedTrophySettings.DEFAULT)) output.store(SETTINGS_KEYS[i], DisplayedTrophySettings.CODEC, settings[i]);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ValueInput input = ValueInput.of(tag);
        for (int i = 0; i < SLOTS; i++) {
            items[i] = input.read(KEYS[i], ItemStack.CODEC).orElse(ItemStack.EMPTY);
            settings[i] = input.read(SETTINGS_KEYS[i], DisplayedTrophySettings.CODEC).orElse(DisplayedTrophySettings.DEFAULT);
        }
        trim = input.read(TRIM, ResourceLocation.CODEC).orElse(null);
        powered = input.getBooleanOr(POWERED, false);
    }

    // Its trophies drop when it's broken.
    /** As it's broken (on 1.20.1 its block's onRemove calls this). */
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        for (ItemStack item : items) {
            if (!item.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}
