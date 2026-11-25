package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.Codec;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.api.PedestalTeams;
import net.ixdarklord.trophypedestals.compat.ValueInput;
import net.ixdarklord.trophypedestals.compat.ValueOutput;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PedestalBlockEntity extends BlockEntity {
    private static final String RECORD = "Record";
    private static final String GLASS = "Glass", GLASS_COLOR = "GlassColor";
    private static final String OUTFIT = "Outfit";
    private static final String ARMOR_STAND = "ArmorStand", PLAYER_MODEL = "PlayerModel", STAND_POSE = "StandPose";
    public static final List<String> STAND_PARTS = List.of("head", "body", "left_arm", "right_arm", "left_leg", "right_leg");
    public static final List<Float> DEFAULT_STAND_POSE = List.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -10.0F, 0.0F, -10.0F,
            -15.0F, 0.0F, 10.0F, -1.0F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F);
    public static final List<EquipmentSlot> OUTFIT_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);
    private static final String LEADERBOARD = "Leaderboard", LEADER_NAME = "LeaderName", LEADER_VALUE = "LeaderValue", RUNNERS_UP = "RunnersUp";
    private static final String DISPLAY = "Display";
    private static final String LENS = "Lens";
    private static final String NO_LENS = "NoLens";
    private static final String MOB_TROPHY = "MobTrophy";
    private static final String TEXT_MODE = "TextMode";
    private static final String OWNER = "Owner";
    private static final String OWNER_NAME = "OwnerName";
    private static final String SCALE = "Scale";
    private static final String HEIGHT = "Height";
    private static final String STILL = "Still";
    private static final String FROZEN = "Frozen";
    private static final String PLACED_AT = "PlacedAt";
    private static final String MATERIAL = "Material";
    private static final String STATUE = "Statue";
    private static final String LOCKED = "Locked";
    private static final String STATUE_POSE = "StatuePose";
    private static final String ANIMATION = "Animation";
    public static final String IDLE = "idle";
    private static final String REFUSAL = "Refusal";
    private static final long JUST_PLACED = 40L;
    private static final String TEXT_HEIGHT = "TextHeight";
    private static final String TEXT_SCALE = "TextScale";
    private static final String NO_CROWN = "NoCrown";
    private static final String ACCESS = "Access";
    private static final String CROWN_SCALE = "CrownScale";
    private static final String CROWN_X = "CrownX";
    private static final String CROWN_Y = "CrownY";
    public static final float MIN_CROWN_SCALE = 0.5F, MAX_CROWN_SCALE = 2.0F, MAX_CROWN_OFFSET = 0.5F;
    private static final String NO_LIGHT = "NoLight";
    private static final String YAW = "Yaw", PITCH = "Pitch", ROLL = "Roll";
    private static final String OLD_FACING = "Facing";
    public static final float MAX_TILT = 180.0F;
    private static final String NO_EFFECTS = "NoEffects";
    private static final String EFFECTS_MODE = "EffectsMode";
    private static final String MUTED = "Muted";
    private static final String REDSTONE = "Redstone";
    private static final String POWERED = "Powered";
    public static final float MIN_TEXT_HEIGHT = -8.0F, MAX_TEXT_HEIGHT = 8.0F;
    public static final float MIN_TEXT_SCALE = 0.5F, MAX_TEXT_SCALE = 3.0F;
    public static final float MIN_HEIGHT = -0.5F, MAX_HEIGHT = 10.0F;
    public static final float MIN_SCALE = 0.5F, MAX_SCALE = 20.0F;
    private static final String TEXT_ALWAYS = "TextAlways";
    private @Nullable PedestalRecord record;
    private @Nullable MobTrophyRecord mobTrophy;
    private final Map<EquipmentSlot, ItemStack> outfit = new EnumMap<>(EquipmentSlot.class);
    private boolean armorStand, playerModel;
    private List<Float> standPose = DEFAULT_STAND_POSE;
    private long lastSound = Long.MIN_VALUE / 2;
    private boolean glass;
    private @Nullable DyeColor glassColor;
    private Leaderboards.@Nullable Board leaderboard;
    private String leaderName = "";
    private int leaderValue;
    private List<Leaderboards.Leader> runnersUp = List.of();
    private @Nullable ResourceLocation material;
    private ItemStack display = ItemStack.EMPTY;
    private ItemStack lens = ItemStack.EMPTY;
    private TextMode textMode = TextMode.LOOKING;
    private @Nullable UUID owner;
    private String ownerName = "";
    private float scale = 1.0F;
    private float height;
    private boolean spins = true;
    private boolean animates = true;
    private boolean statue;
    private StatuePose statuePose = StatuePose.STANDING;
    private String animation = IDLE;
    private boolean locked;
    private @Nullable Component refusal;
    private float textHeight;
    private float textScale = 1.0F;
    private boolean crown = true;
    private Access access = Access.PRIVATE;
    private float crownScale = 1.0F, crownX, crownY;
    private boolean light = true;
    private float yaw, pitch, roll;
    private TextMode effectsMode = TextMode.ALWAYS;
    private boolean sounds = true;
    private RedstoneMode redstone = RedstoneMode.IGNORE;
    private boolean powered;
    private ItemStack iconOverride = ItemStack.EMPTY;
    private boolean scene;
    private @Nullable Entity shownMob;
    private @Nullable Object renderMemory;
    private float glassReach, glassHeight;
    private long placedAt = Long.MIN_VALUE / 2;
    private long displayChangedAt = Long.MIN_VALUE / 2;
    private boolean wasOn, synced;

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(Pedestals.BLOCK_ENTITY.get(), pos, state);
    }

    public @Nullable PedestalRecord record() {
        return record;
    }

    public @Nullable MobTrophyRecord mobTrophy() {
        return mobTrophy;
    }

    public ItemStack iconOverride() {
        return iconOverride;
    }

    public void showTrophy(PedestalRecord record, ItemStack icon) {
        this.record = record;
        this.iconOverride = icon;
        this.scene = true;
    }

    public boolean inScene() {
        return scene;
    }

    public @Nullable ResourceLocation material() {
        return material;
    }

    public @Nullable Object renderMemory() {
        return renderMemory;
    }

    public void setRenderMemory(Object memory) {
        renderMemory = memory;
    }

    public @Nullable Entity shownMob() {
        return shownMob;
    }

    private final @Nullable Entity[] runnerUpStatues = new Entity[2];
    private final String[] runnerUpStatueNames = {"", ""};

    public @Nullable Entity runnerUpStatue(int place, String name) {
        return name.equals(runnerUpStatueNames[place]) ? runnerUpStatues[place] : null;
    }

    public void setRunnerUpStatue(int place, String name, @Nullable Entity statue) {
        runnerUpStatueNames[place] = name;
        runnerUpStatues[place] = statue;
    }

    public void setShownMob(@Nullable Entity mob) {
        clearGlassFit();
        shownMob = mob;
    }

    public void updateMobTrophy(MobTrophyRecord updated) {
        if (mobTrophy == null || level == null) return;
        mobTrophy = updated;
        ItemStack named = new ItemStack(getBlockState().getBlock());
        MobTrophies.record(named, updated);
        setComponents(DataComponentMap.builder().addAll(components()).set(DataComponents.ITEM_NAME, named.get(DataComponents.ITEM_NAME)).build());
        BlockState state = getBlockState();
        int rank = PedestalBlock.rank(updated.rarity());
        if (state.hasProperty(PedestalBlock.RANK) && state.getValue(PedestalBlock.RANK) != rank) level.setBlock(worldPosition, state.setValue(PedestalBlock.RANK, rank), Block.UPDATE_ALL);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    private boolean sameOutfit(Map<EquipmentSlot, ItemStack> other) {
        if (other.size() != outfit.size()) return false;
        for (Map.Entry<EquipmentSlot, ItemStack> entry : other.entrySet()) {
            if (!ItemStack.matches(entry.getValue(), outfit(entry.getKey()))) return false;
        }
        return true;
    }

    public boolean mannequin() {
        return armorStand;
    }

    public void setArmorStand(boolean installed) {
        armorStand = installed;
        if (!installed) {
            playerModel = false;
            standPose = DEFAULT_STAND_POSE;
        }
        setChanged();
        refresh();
    }

    public boolean playerModel() {
        return playerModel;
    }

    public List<Float> standPose() {
        return standPose;
    }

    public void setMannequinLook(boolean playerModel, List<Float> pose) {
        clearGlassFit();
        this.playerModel = playerModel;
        if (pose.size() == DEFAULT_STAND_POSE.size()) this.standPose = pose.stream().map(angle -> Mth.wrapDegrees(angle)).toList();
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public ItemStack outfit(EquipmentSlot slot) {
        return outfit.getOrDefault(slot, ItemStack.EMPTY);
    }

    public ItemStack setOutfit(EquipmentSlot slot, ItemStack stack) {
        ItemStack old = stack.isEmpty() ? outfit.remove(slot) : outfit.put(slot, stack);
        setChanged();
        refresh();
        return old == null ? ItemStack.EMPTY : old;
    }

    public float[] fitGlass(float reach, float height) {
        glassReach = Math.max(glassReach, reach);
        glassHeight = Math.max(glassHeight, height);
        return new float[] {glassReach, glassHeight};
    }

    private void clearGlassFit() {
        glassReach = 0.0F;
        glassHeight = 0.0F;
    }

    public long lastSound() {
        return lastSound;
    }

    public void markSound(long time) {
        lastSound = time;
    }

    public boolean glass() {
        return glass;
    }

    public @Nullable DyeColor glassColor() {
        return glassColor;
    }

    public ItemStack glassItem() {
        return glassItem(glassColor);
    }

    public static ItemStack glassItem(@Nullable DyeColor color) {
        return color == null ? new ItemStack(Items.GLASS)
                : new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getSerializedName() + "_stained_glass")));
    }

    public void setGlass(boolean glass, @Nullable DyeColor color) {
        this.glassColor = glass ? color : null;
        setGlass(glass);
    }

    public void setGlass(boolean glass) {
        this.glass = glass;
        setChanged();
        BlockState state = getBlockState();
        if (level != null && !level.isClientSide() && state.hasProperty(PedestalBlock.CASED) && state.getValue(PedestalBlock.CASED) != glass) {
            BlockPos above = worldPosition.above();
            if (!glass && level.getBlockState(above).is(Pedestals.VITRINE_TOP.get())) level.removeBlock(above, false);
            level.setBlock(worldPosition, state.setValue(PedestalBlock.CASED, glass), Block.UPDATE_ALL);
            if (glass && level.getBlockState(above).canBeReplaced()) level.setBlock(above, Pedestals.VITRINE_TOP.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public Leaderboards.@Nullable Board leaderboard() {
        return leaderboard;
    }

    public String leaderName() {
        return leaderName;
    }

    public int leaderValue() {
        return leaderValue;
    }

    public void setLeaderboard(Leaderboards.@Nullable Board board) {
        leaderboard = board;
        leaderName = "";
        leaderValue = 0;
        runnersUp = List.of();
        setChanged();
        refresh();
    }

    public List<Leaderboards.Leader> runnersUp() {
        return runnersUp;
    }

    public void setLeader(String name, int value) {
        setLeader(name, value, List.of());
    }

    public void setLeader(String name, int value, List<Leaderboards.Leader> runnersUp) {
        leaderName = name;
        leaderValue = value;
        this.runnersUp = List.copyOf(runnersUp);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public boolean isTrophy() {
        return record != null || mobTrophy != null;
    }

    public @Nullable ResourceLocation rarity() {
        return record != null ? record.rarity() : mobTrophy != null ? mobTrophy.rarity() : null;
    }

    public ItemStack display() {
        return display;
    }

    public void setDisplay(ItemStack stack) {
        display = stack;
        setChanged();
        refresh();
    }

    public ItemStack lens() {
        return lens;
    }

    public ItemStack setLens(ItemStack stack) {
        ItemStack old = lens;
        lens = stack;
        setChanged();
        refresh();
        return old;
    }

    public void syncLight() {
        BlockState state = getBlockState();
        if (state.getValue(PedestalBlock.LIT) != on() || state.getValue(PedestalBlock.RANK) != PedestalBlock.rank(rarity())
                || state.getValue(PedestalBlock.POWERED) != displayed()
                || state.getValue(PedestalBlock.SOCKET) != emptySocket()) refresh();
    }

    public boolean emptySocket() {
        return lens.isEmpty() && !isTrophy() && leaderboard == null;
    }

    private void refresh() {
        if (level == null) return;
        BlockState state = getBlockState();
        boolean lit = on();
        int rank = PedestalBlock.rank(rarity());
        if (!level.isClientSide() && state.hasProperty(PedestalBlock.LIT)
                && (state.getValue(PedestalBlock.LIT) != lit || state.getValue(PedestalBlock.RANK) != rank || state.getValue(PedestalBlock.POWERED) != displayed()
                || state.getValue(PedestalBlock.SOCKET) != emptySocket())) {
            level.setBlock(worldPosition, state.setValue(PedestalBlock.LIT, lit).setValue(PedestalBlock.RANK, rank).setValue(PedestalBlock.POWERED, displayed())
                    .setValue(PedestalBlock.SOCKET, emptySocket()), Block.UPDATE_ALL);
        } else {
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    public boolean on() {
        return displayed() && hasShowpiece();
    }

    public boolean hasShowpiece() {
        return isTrophy() || !display.isEmpty() || leaderboard != null || armorStand;
    }

    public float restTurn() {
        BlockState state = getBlockState();
        return state.hasProperty(PedestalBlock.FACING) ? (360.0F - state.getValue(PedestalBlock.FACING).toYRot()) % 360.0F : 0.0F;
    }

    public void markPlaced() {
        if (level == null) return;
        placedAt = level.getGameTime();
        setChanged();
        refresh();
    }

    public long displayChangedAt() {
        return displayChangedAt;
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getName().getString();
        setChanged();
    }

    public boolean locked() {
        return locked;
    }

    public @Nullable Component refusal() {
        return refusal;
    }

    public void lock(@Nullable Component refusal) {
        this.locked = true;
        this.refusal = refusal;
        setChanged();
    }

    public void unlock() {
        this.locked = false;
        this.refusal = null;
        setChanged();
    }

    public boolean canAdjust(Player player) {
        if (locked) return false;
        if (TrophyPedestalsConfig.PEDESTAL_ACCESS.get() == TrophyPedestalsConfig.PedestalAccess.ANYONE || owner == null || owner.equals(player.getUUID())) return true;
        if (isOperator(player)) return true;
        return switch (access) {
            case EVERYONE -> true;
            case TEAM -> player instanceof ServerPlayer server && sameTeam(server);
            case PRIVATE -> false;
        };
    }

    public boolean canChangeAccess(Player player) {
        return !locked && (owner == null || owner.equals(player.getUUID()) || isOperator(player));
    }

    private static boolean isOperator(Player player) {
        return player.hasPermissions(Commands.LEVEL_GAMEMASTERS);
    }

    private boolean sameTeam(ServerPlayer player) {
        return owner != null && PedestalTeams.sameTeam(player, owner);
    }

    public Access access() {
        return access;
    }

    public void setAccess(Access access) {
        this.access = access;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public float scale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public float height() {
        return height;
    }

    public void setHeight(float height) {
        this.height = Mth.clamp(height, MIN_HEIGHT, MAX_HEIGHT);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public float textHeight() {
        return textHeight;
    }

    public void setTextHeight(float height) {
        textHeight = Mth.clamp(height, MIN_TEXT_HEIGHT, MAX_TEXT_HEIGHT);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public boolean crown() {
        return crown;
    }

    public void setCrown(boolean crown) {
        this.crown = crown;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public float crownScale() {
        return crownScale;
    }

    public float crownX() {
        return crownX;
    }

    public float crownY() {
        return crownY;
    }

    public void setCrownPlacement(float scale, float x, float y) {
        this.crownScale = Mth.clamp(scale, MIN_CROWN_SCALE, MAX_CROWN_SCALE);
        this.crownX = Mth.clamp(x, -MAX_CROWN_OFFSET, MAX_CROWN_OFFSET);
        this.crownY = Mth.clamp(y, -MAX_CROWN_OFFSET, MAX_CROWN_OFFSET);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public boolean light() {
        return light;
    }

    public void setLight(boolean light) {
        this.light = light;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public float textScale() {
        return textScale;
    }

    public void setTextScale(float textScale) {
        this.textScale = Mth.clamp(textScale, MIN_TEXT_SCALE, MAX_TEXT_SCALE);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public RedstoneMode redstone() {
        return redstone;
    }

    public void setRedstone(RedstoneMode mode) {
        redstone = mode;
        setChanged();
        refresh();
    }

    public boolean powered() {
        return powered;
    }

    public void setPowered(boolean powered) {
        if (this.powered == powered) return;
        this.powered = powered;
        setChanged();
        refresh();
    }

    public boolean displayed() {
        return switch (redstone) {
            case IGNORE -> true;
            case SHOW -> powered;
            case HIDE -> !powered;
        };
    }

    public boolean sounds() {
        return sounds;
    }

    public void setSounds(boolean sounds) {
        this.sounds = sounds;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public boolean effects() {
        return effectsMode != TextMode.HIDDEN;
    }

    public void setEffects(boolean effects) {
        setEffectsMode(!effects ? TextMode.HIDDEN : effectsMode == TextMode.HIDDEN ? TextMode.ALWAYS : effectsMode);
    }

    public TextMode effectsMode() {
        return effectsMode;
    }

    public void setEffectsMode(TextMode mode) {
        this.effectsMode = mode;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public float yaw() {
        return yaw;
    }

    public void setYaw(float degrees) {
        yaw = Mth.clamp(degrees, 0.0F, 360.0F);
        changedRotation();
    }

    public float pitch() {
        return pitch;
    }

    public void setPitch(float degrees) {
        pitch = Mth.clamp(degrees, -MAX_TILT, MAX_TILT);
        changedRotation();
    }

    public float roll() {
        return roll;
    }

    public void setRoll(float degrees) {
        roll = Mth.clamp(degrees, -MAX_TILT, MAX_TILT);
        changedRotation();
    }

    public void setRotation(float yaw, float pitch, float roll) {
        clearGlassFit();
        this.yaw = Mth.clamp(yaw, 0.0F, 360.0F);
        this.pitch = Mth.clamp(pitch, -MAX_TILT, MAX_TILT);
        this.roll = Mth.clamp(roll, -MAX_TILT, MAX_TILT);
        changedRotation();
    }

    private void changedRotation() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public boolean spins() {
        return spins;
    }

    public void setSpins(boolean spins) {
        this.spins = spins;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public boolean animates() {
        return animates;
    }

    public void setAnimates(boolean animates) {
        this.animates = animates;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public boolean statue() {
        return statue;
    }

    public void setStatue(boolean statue) {
        this.statue = statue;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public StatuePose statuePose() {
        return statuePose;
    }

    public void setStatuePose(StatuePose pose) {
        clearGlassFit();
        this.statuePose = pose;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public String animation() {
        return animation;
    }

    public void setAnimation(String animation) {
        clearGlassFit();
        this.animation = animationId(animation);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    private static String animationId(@Nullable String animation) {
        return animation != null && animation.matches("[a-z0-9_]{1,32}") ? animation : IDLE;
    }

    public boolean showsStatue() {
        return statue && canShowStatue() || mobTrophy != null && mobTrophy.type() == Statues.FIGURE;
    }

    public boolean canShowStatue() {
        return record != null && record.unlocker() == PedestalRecord.Unlocker.PLAYER && !record.name().isEmpty();
    }

    public TextMode textMode() {
        return textMode;
    }

    public void setTextMode(TextMode mode) {
        textMode = mode;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !display.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, display);
        if (level != null) outfit.values().forEach(worn -> Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, worn));
        if (level != null && armorStand) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, new ItemStack(Items.ARMOR_STAND));
        if (level != null && glass) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, glassItem());
        if (level != null && leaderboard != null) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, new ItemStack(Pedestals.LEADERBOARD_CREST.get()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = ValueOutput.of(tag, registries);
        if (glass) output.putBoolean(GLASS, true);
        if (glass && glassColor != null) output.putString(GLASS_COLOR, glassColor.getSerializedName());
        outfit.forEach((slot, worn) -> output.store(OUTFIT + slot.getSerializedName(), ItemStack.CODEC, worn));
        if (armorStand) {
            output.putBoolean(ARMOR_STAND, true);
            if (playerModel) output.putBoolean(PLAYER_MODEL, true);
            if (!standPose.equals(DEFAULT_STAND_POSE)) output.store(STAND_POSE, Codec.FLOAT.listOf(), standPose);
        }
        if (leaderboard != null) {
            output.putString(LEADERBOARD, leaderboard.getSerializedName());
            output.putString(LEADER_NAME, leaderName);
            output.putInt(LEADER_VALUE, leaderValue);
            if (!runnersUp.isEmpty()) output.store(RUNNERS_UP, Leaderboards.Leader.CODEC.listOf(), runnersUp);
        }
        if (record != null) output.store(RECORD, PedestalRecord.CODEC, record);
        if (mobTrophy != null) output.store(MOB_TROPHY, MobTrophyRecord.CODEC, mobTrophy);
        if (material != null) output.store(MATERIAL, ResourceLocation.CODEC, material);
        if (!display.isEmpty()) output.store(DISPLAY, ItemStack.CODEC, display);
        if (!lens.isEmpty()) output.store(LENS, ItemStack.CODEC, lens);
        else output.putBoolean(NO_LENS, true);
        if (textMode != TextMode.LOOKING) output.store(TEXT_MODE, TextMode.CODEC, textMode);
        if (owner != null) {
            output.store(OWNER, UUIDUtil.CODEC, owner);
            output.putString(OWNER_NAME, ownerName);
        }
        if (scale != 1.0F) output.putFloat(SCALE, scale);
        if (height != 0.0F) output.putFloat(HEIGHT, height);
        if (!spins) output.putBoolean(STILL, true);
        if (!animates) output.putBoolean(FROZEN, true);
        if (statue) output.putBoolean(STATUE, true);
        if (statuePose != StatuePose.STANDING) output.store(STATUE_POSE, StatuePose.CODEC, statuePose);
        if (!animation.equals(IDLE)) output.putString(ANIMATION, animation);
        if (locked) {
            output.putBoolean(LOCKED, true);
            if (refusal != null) output.store(REFUSAL, ComponentSerialization.CODEC, refusal);
        }
        if (textHeight != 0.0F) output.putFloat(TEXT_HEIGHT, textHeight);
        if (textScale != 1.0F) output.putFloat(TEXT_SCALE, textScale);
        if (!crown) output.putBoolean(NO_CROWN, true);
        if (access != Access.PRIVATE) output.store(ACCESS, Access.CODEC, access);
        if (crownScale != 1.0F) output.putFloat(CROWN_SCALE, crownScale);
        if (crownX != 0.0F) output.putFloat(CROWN_X, crownX);
        if (crownY != 0.0F) output.putFloat(CROWN_Y, crownY);
        if (!light) output.putBoolean(NO_LIGHT, true);
        if (yaw != 0.0F) output.putFloat(YAW, yaw);
        if (pitch != 0.0F) output.putFloat(PITCH, pitch);
        if (roll != 0.0F) output.putFloat(ROLL, roll);
        if (effectsMode != TextMode.ALWAYS) output.store(EFFECTS_MODE, TextMode.CODEC, effectsMode);
        if (!sounds) output.putBoolean(MUTED, true);
        if (redstone != RedstoneMode.IGNORE) output.store(REDSTONE, RedstoneMode.CODEC, redstone);
        if (powered) output.putBoolean(POWERED, true);
        if (placedAt > Long.MIN_VALUE / 2) output.putLong(PLACED_AT, placedAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
        glass = input.getBooleanOr(GLASS, false);
        glassColor = input.getString(GLASS_COLOR).map(name -> DyeColor.byName(name, null)).orElse(null);
        Map<EquipmentSlot, ItemStack> wearing = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : OUTFIT_SLOTS) input.read(OUTFIT + slot.getSerializedName(), ItemStack.CODEC).ifPresent(worn -> wearing.put(slot, worn));
        if (!sameOutfit(wearing)) shownMob = null;
        outfit.clear();
        outfit.putAll(wearing);
        boolean dressedBefore = !wearing.isEmpty() && !input.getBooleanOr(ARMOR_STAND, false);
        armorStand = input.getBooleanOr(ARMOR_STAND, false) || dressedBefore;
        playerModel = input.getBooleanOr(PLAYER_MODEL, false) || dressedBefore;
        List<Float> pose = input.read(STAND_POSE, Codec.FLOAT.listOf()).filter(angles -> angles.size() == DEFAULT_STAND_POSE.size()).orElse(DEFAULT_STAND_POSE);
        if (!pose.equals(standPose)) clearGlassFit();
        standPose = pose;
        leaderboard = input.getString(LEADERBOARD).map(Leaderboards.Board::byName).orElse(null);
        String leader = input.getStringOr(LEADER_NAME, "");
        if (!leader.equals(leaderName)) shownMob = null;
        leaderName = leader;
        leaderValue = input.getIntOr(LEADER_VALUE, 0);
        runnersUp = input.read(RUNNERS_UP, Leaderboards.Leader.CODEC.listOf()).orElse(List.of());
        record = input.read(RECORD, PedestalRecord.CODEC).orElse(null);
        MobTrophyRecord shown = mobTrophy;
        mobTrophy = input.read(MOB_TROPHY, MobTrophyRecord.CODEC).orElse(null);
        if (!Objects.equals(shown, mobTrophy)) shownMob = null;
        material = input.read(MATERIAL, ResourceLocation.CODEC).orElse(null);
        display = input.read(DISPLAY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        lens = input.read(LENS, ItemStack.CODEC).orElseGet(() -> input.getBooleanOr(NO_LENS, false) || isTrophy() ? ItemStack.EMPTY : clearLens());
        owner = input.read(OWNER, UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr(OWNER_NAME, "");
        scale = Mth.clamp(input.getFloatOr(SCALE, 1.0F), MIN_SCALE, MAX_SCALE);
        height = Mth.clamp(input.getFloatOr(HEIGHT, 0.0F), MIN_HEIGHT, MAX_HEIGHT);
        spins = !input.getBooleanOr(STILL, false);
        animates = !input.getBooleanOr(FROZEN, false);
        statue = input.getBooleanOr(STATUE, false);
        statuePose = input.read(STATUE_POSE, StatuePose.CODEC).orElse(StatuePose.STANDING);
        animation = animationId(input.getStringOr(ANIMATION, IDLE));
        locked = input.getBooleanOr(LOCKED, false);
        refusal = locked ? input.read(REFUSAL, ComponentSerialization.CODEC).orElse(null) : null;
        textHeight = Mth.clamp(input.getFloatOr(TEXT_HEIGHT, 0.0F), MIN_TEXT_HEIGHT, MAX_TEXT_HEIGHT);
        textScale = Mth.clamp(input.getFloatOr(TEXT_SCALE, 1.0F), MIN_TEXT_SCALE, MAX_TEXT_SCALE);
        crown = !input.getBooleanOr(NO_CROWN, false);
        access = input.read(ACCESS, Access.CODEC).orElse(Access.PRIVATE);
        crownScale = Mth.clamp(input.getFloatOr(CROWN_SCALE, 1.0F), MIN_CROWN_SCALE, MAX_CROWN_SCALE);
        crownX = Mth.clamp(input.getFloatOr(CROWN_X, 0.0F), -MAX_CROWN_OFFSET, MAX_CROWN_OFFSET);
        crownY = Mth.clamp(input.getFloatOr(CROWN_Y, 0.0F), -MAX_CROWN_OFFSET, MAX_CROWN_OFFSET);
        light = !input.getBooleanOr(NO_LIGHT, false);
        float turned = Mth.clamp(input.getFloatOr(YAW, input.getFloatOr(OLD_FACING, 0.0F)), 0.0F, 360.0F);
        if (turned != yaw) clearGlassFit();
        yaw = turned;
        pitch = Mth.clamp(input.getFloatOr(PITCH, 0.0F), -MAX_TILT, MAX_TILT);
        roll = Mth.clamp(input.getFloatOr(ROLL, 0.0F), -MAX_TILT, MAX_TILT);
        effectsMode = input.read(EFFECTS_MODE, TextMode.CODEC).orElse(input.getBooleanOr(NO_EFFECTS, false) ? TextMode.HIDDEN : TextMode.ALWAYS);
        sounds = !input.getBooleanOr(MUTED, false);
        redstone = input.read(REDSTONE, RedstoneMode.CODEC).orElse(RedstoneMode.IGNORE);
        powered = input.getBooleanOr(POWERED, false);
        textMode = input.read(TEXT_MODE, TextMode.CODEC).orElse(input.getBooleanOr(TEXT_ALWAYS, false) ? TextMode.ALWAYS : TextMode.LOOKING);
        placedAt = input.getLongOr(PLACED_AT, Long.MIN_VALUE / 2);
        boolean on = on();
        if (level == null) {
            wasOn = on;
            synced = true;
        } else if (level.isClientSide()) {
            boolean placedNow = !synced && on && level.getGameTime() - placedAt < JUST_PLACED && level.getGameTime() - displayChangedAt >= JUST_PLACED;
            if (synced ? on != wasOn : placedNow) displayChangedAt = level.getGameTime();
            wasOn = on;
            synced = true;
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        record = components.get(Pedestals.RECORD.get());
        mobTrophy = components.get(Pedestals.MOB_TROPHY.get());
        material = components.get(Pedestals.PEDESTAL_MATERIAL.get());
        ItemContainerContents fitted = components.get(Pedestals.LENS.get());
        lens = fitted != null ? fitted.copyOne() : isTrophy() ? ItemStack.EMPTY : clearLens();
        if (level != null && level.isClientSide() && !synced && on()) displayChangedAt = level.getGameTime();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (record != null) components.set(Pedestals.RECORD.get(), record);
        if (mobTrophy != null) components.set(Pedestals.MOB_TROPHY.get(), mobTrophy);
        if (material != null) components.set(Pedestals.PEDESTAL_MATERIAL.get(), material);
        if (!isTrophy()) components.set(Pedestals.LENS.get(), Pedestals.lens(lens));
    }

    private static ItemStack clearLens() {
        return new ItemStack(Pedestals.LENSES.get(Lens.CLEAR).get());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        ValueOutput output = ValueOutput.of(tag);
        output.discard(RECORD);
        output.discard(MOB_TROPHY);
        output.discard(MATERIAL);
        output.discard(LENS);
        output.discard(NO_LENS);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public enum Access implements StringRepresentable {
        PRIVATE,
        TEAM,
        EVERYONE;

        static final Codec<Access> CODEC = StringRepresentable.fromEnum(Access::values);

        public Access next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public static @Nullable Access byName(String name) {
            for (Access access : values()) if (access.getSerializedName().equals(name)) return access;
            return null;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum RedstoneMode implements StringRepresentable {
        IGNORE,
        SHOW,
        HIDE;

        static final Codec<RedstoneMode> CODEC = StringRepresentable.fromEnum(RedstoneMode::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum StatuePose implements StringRepresentable {
        STANDING(Pose.STANDING),
        CROUCHING(Pose.CROUCHING),
        SWIMMING(Pose.SWIMMING),
        GLIDING(Pose.FALL_FLYING),
        SLEEPING(Pose.SLEEPING);

        public static final Codec<StatuePose> CODEC = StringRepresentable.fromEnum(StatuePose::values);
        private final Pose pose;

        StatuePose(Pose pose) {
            this.pose = pose;
        }

        public Pose pose() {
            return pose;
        }

        public boolean lying() {
            return this == SWIMMING || this == GLIDING || this == SLEEPING;
        }

        public StatuePose next() {
            return values()[(ordinal() + 1) % values().length];
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum TextMode implements StringRepresentable {
        LOOKING,
        ALWAYS,
        HIDDEN;

        static final Codec<TextMode> CODEC = StringRepresentable.fromEnum(TextMode::values);

        public TextMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
