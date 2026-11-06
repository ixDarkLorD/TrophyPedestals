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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * A placed pedestal's record ({@link PedestalRecord} for an achievement trophy, {@link MobTrophyRecord} for a mob's), or none
 * for a crafted one. It comes from the item's component when placed and goes back onto it when broken (the loot table
 * copies it), and is sent to clients for the renderer. A crafted pedestal can hold one item for display instead,
 * dropped when it's broken. Its text shows while looked at, always, or never ({@link TextMode}).
 */
public class PedestalBlockEntity extends BlockEntity {
    private static final String RECORD = "Record";
    private static final String DISPLAY = "Display";
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
    /** The animation a mob or statue plays unless one is chosen: its own, as it stands. */
    public static final String IDLE = "idle";
    private static final String REFUSAL = "Refusal";
    // How long after it's placed (ticks) a client first hearing of it plays its turning on: long enough for the news to
    // arrive, short enough that one coming into view later doesn't.
    private static final long JUST_PLACED = 40L;
    private static final String TEXT_HEIGHT = "TextHeight";
    private static final String TEXT_SCALE = "TextScale";
    private static final String NO_CROWN = "NoCrown";
    private static final String ACCESS = "Access";
    private static final String CROWN_SCALE = "CrownScale";
    private static final String CROWN_X = "CrownX";
    private static final String CROWN_Y = "CrownY";
    /** How small and large a legendary trophy's crown can be made, and how far it can be moved each way (in blocks at the usual display size). */
    public static final float MIN_CROWN_SCALE = 0.5F, MAX_CROWN_SCALE = 2.0F, MAX_CROWN_OFFSET = 0.5F;
    private static final String NO_LIGHT = "NoLight";
    private static final String YAW = "Yaw", PITCH = "Pitch", ROLL = "Roll";
    // (What Yaw was saved as before pitch and roll.)
    private static final String OLD_FACING = "Facing";
    /** How far what it shows can be pitched or rolled each way, in degrees. */
    public static final float MAX_TILT = 180.0F;
    private static final String NO_EFFECTS = "NoEffects";
    private static final String MUTED = "Muted";
    private static final String REDSTONE = "Redstone";
    private static final String POWERED = "Powered";
    /** How far down and up its text can be moved from its usual place, in blocks. */
    public static final float MIN_TEXT_HEIGHT = -8.0F, MAX_TEXT_HEIGHT = 8.0F;
    /** How small and how large its text and frame can be shown. */
    public static final float MIN_TEXT_SCALE = 0.5F, MAX_TEXT_SCALE = 3.0F;
    /** How far down and up its item or mob can be moved, in blocks. */
    public static final float MIN_HEIGHT = -0.5F, MAX_HEIGHT = 10.0F;
    /** How small and how large its item or mob can be shown. */
    public static final float MIN_SCALE = 0.5F, MAX_SCALE = 20.0F;
    // What older saves wrote before there were three modes.
    private static final String TEXT_ALWAYS = "TextAlways";
    private @Nullable PedestalRecord record;
    private @Nullable MobTrophyRecord mobTrophy;
    // A custom pedestal's material: the block it's drawn with.
    private @Nullable ResourceLocation material;
    private ItemStack display = ItemStack.EMPTY;
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
    // Whether a mod has locked its settings (api/PedestalSettings), and what a player trying to change them is told (none,
    // to refuse silently).
    private boolean locked;
    private @Nullable Component refusal;
    private float textHeight;
    private float textScale = 1.0F;
    private boolean crown = true;
    private Access access = Access.PRIVATE;
    private float crownScale = 1.0F, crownX, crownY;
    private boolean light = true;
    private float yaw, pitch, roll;
    private boolean effects = true;
    private boolean sounds = true;
    private RedstoneMode redstone = RedstoneMode.IGNORE;
    private boolean powered;
    // On clients: an item shown in place of a trophy's icon (the cinematic's pack-chosen block), never saved.
    private ItemStack iconOverride = ItemStack.EMPTY;
    // On clients: whether it's drawn in a scene of its own (a SceneViewport), never in the world.
    private boolean scene;
    // On clients: the mob a mob trophy shows, made once to be drawn.
    private @Nullable Entity shownMob;
    // When a player placed it (game time), so the clients that see it placed play its turning on.
    private long placedAt = Long.MIN_VALUE / 2;
    // On clients: when it last turned on or off (game time), so its light can rise and lower; whether it was on then;
    // and whether it has had its data yet, so a pedestal coming into view doesn't play it.
    private long displayChangedAt = Long.MIN_VALUE / 2;
    private boolean wasOn, synced;

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(Pedestals.BLOCK_ENTITY.get(), pos, state);
    }

    public @Nullable PedestalRecord record() {
        return record;
    }

    /** The mob a mob trophy shows, or null for anything else. */
    public @Nullable MobTrophyRecord mobTrophy() {
        return mobTrophy;
    }

    /** On clients, an item shown in place of its trophy's icon, or empty for the icon. */
    public ItemStack iconOverride() {
        return iconOverride;
    }

    /**
     * Makes this a trophy for drawing only, never saved or sent: for a pedestal in a scene of its own (the cinematic
     * reveal's altar), showing {@code icon} if it isn't empty in place of its icon.
     */
    public void showTrophy(PedestalRecord record, ItemStack icon) {
        this.record = record;
        this.iconOverride = icon;
        this.scene = true;
    }

    /**
     * On clients, whether it's drawn in a scene of its own ({@link #showTrophy}) rather than in the world: its light and
     * flares are then drawn with it, in the scene, rather than as world effects.
     */
    public boolean inScene() {
        return scene;
    }

    /** A custom pedestal's material, the block whose textures it's drawn with; null for any other pedestal, or one with none. */
    public @Nullable ResourceLocation material() {
        return material;
    }

    /** On clients, the mob made to draw a mob trophy with, or null if none is made yet. */
    public @Nullable Entity shownMob() {
        return shownMob;
    }

    public void setShownMob(@Nullable Entity mob) {
        shownMob = mob;
    }

    /** Whether it's a trophy of either kind. */
    public boolean isTrophy() {
        return record != null || mobTrophy != null;
    }

    /** Its trophy's rarity id, or null if it isn't a trophy. */
    public @Nullable ResourceLocation rarity() {
        return record != null ? record.rarity() : mobTrophy != null ? mobTrophy.rarity() : null;
    }

    /** The item a crafted pedestal shows, or empty. */
    public ItemStack display() {
        return display;
    }

    /** Puts an item on display (empty to take it off), saving and sending the change to clients. */
    public void setDisplay(ItemStack stack) {
        display = stack;
        setChanged();
        refresh();
    }

    /** Lights the block as it should be (on, and as strongly as its trophy's rank says), if it isn't already. */
    public void syncLight() {
        BlockState state = getBlockState();
        if (state.getValue(PedestalBlock.LIT) != on() || state.getValue(PedestalBlock.RANK) != PedestalBlock.rank(rarity())) refresh();
    }

    // Sends a change to clients, lighting the block up while it's on (as strongly as its trophy's rank says) and putting
    // its light out while it's off (on the server; a client's preview only redraws).
    private void refresh() {
        if (level == null) return;
        BlockState state = getBlockState();
        boolean lit = on();
        int rank = PedestalBlock.rank(rarity());
        if (!level.isClientSide() && state.hasProperty(PedestalBlock.LIT)
                && (state.getValue(PedestalBlock.LIT) != lit || state.getValue(PedestalBlock.RANK) != rank)) {
            level.setBlock(worldPosition, state.setValue(PedestalBlock.LIT, lit).setValue(PedestalBlock.RANK, rank), Block.UPDATE_ALL);
        } else {
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    /**
     * Whether it's on: shown (as its redstone setting and signal decide), and a trophy or holding an item. It lights up
     * as it turns on, and its light sinks as it turns off.
     */
    public boolean on() {
        return displayed() && (isTrophy() || !display.isEmpty());
    }

    /** Records that it was just placed, so the clients that see it placed play its turning on. */
    public void markPlaced() {
        if (level == null) return;
        placedAt = level.getGameTime();
        setChanged();
        refresh();
    }

    /** On clients, the game time it last turned on or off (long ago if it hasn't since it came into view). */
    public long displayChangedAt() {
        return displayChangedAt;
    }

    /** The player who placed it, or null for one placed before pedestals had owners (or by a machine). */
    public @Nullable UUID owner() {
        return owner;
    }

    /** Its owner's name when they placed it, or empty. */
    public String ownerName() {
        return ownerName;
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getName().getString();
        setChanged();
    }

    /** Whether a mod has locked its settings so no player can change them, its owner and operators included. */
    public boolean locked() {
        return locked;
    }

    /** What a player trying to change locked settings is told, or null to refuse them silently. */
    public @Nullable Component refusal() {
        return refusal;
    }

    /** Locks its settings against every player, telling one who tries {@code refusal} (null: refusing silently). */
    public void lock(@Nullable Component refusal) {
        this.locked = true;
        this.refusal = refusal;
        setChanged();
    }

    /** Lets players with access change its settings again. */
    public void unlock() {
        this.locked = false;
        this.refusal = null;
        setChanged();
    }

    /**
     * Whether this player can adjust it (its text and display size): no one while a mod has locked it; otherwise anyone,
     * if the server's config says so or it has no owner; else its owner and operators, and others as its owner allows
     * ({@link #access()}: everyone, the owner's team, or no one else).
     */
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

    /** Whether this player can choose who else may adjust it: its owner (anyone, with no owner) and operators, unless a mod has locked it. */
    public boolean canChangeAccess(Player player) {
        return !locked && (owner == null || owner.equals(player.getUUID()) || isOperator(player));
    }

    private static boolean isOperator(Player player) {
        return player.hasPermissions(Commands.LEVEL_GAMEMASTERS);
    }

    // Whether the player is on its owner's team (scoreboard teams, or another mod's: PedestalTeams).
    private boolean sameTeam(ServerPlayer player) {
        return owner != null && PedestalTeams.sameTeam(player, owner);
    }

    /** Who besides its owner and operators may adjust it (only its owner by default). */
    public Access access() {
        return access;
    }

    public void setAccess(Access access) {
        this.access = access;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** How much larger or smaller than usual its item or mob is shown, from {@link #MIN_SCALE} to {@link #MAX_SCALE}. */
    public float scale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** How far its item or mob is moved up (or down) from its usual place, in blocks. */
    public float height() {
        return height;
    }

    public void setHeight(float height) {
        this.height = Mth.clamp(height, MIN_HEIGHT, MAX_HEIGHT);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** How far its text is moved up (or down) from its usual place over what it shows, in blocks. */
    public float textHeight() {
        return textHeight;
    }

    public void setTextHeight(float height) {
        textHeight = Mth.clamp(height, MIN_TEXT_HEIGHT, MAX_TEXT_HEIGHT);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** Whether a legendary trophy wears its crown over what it shows (on by default). */
    public boolean crown() {
        return crown;
    }

    public void setCrown(boolean crown) {
        this.crown = crown;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** How large a legendary trophy's crown is, against its usual size. */
    public float crownScale() {
        return crownScale;
    }

    /**
     * How far a legendary trophy's crown is moved forward (towards where what it shows faces; back if negative), and up
     * (down if negative), in blocks at the usual display size: to sit it on a head that isn't over the middle.
     */
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

    /** Whether its showcase light shows: the lit lens, the cone of light rising out of the well, and its frame (on by default). */
    public boolean light() {
        return light;
    }

    public void setLight(boolean light) {
        this.light = light;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** How large its text and the frame behind it are shown, from half to three times the usual. */
    public float textScale() {
        return textScale;
    }

    public void setTextScale(float textScale) {
        this.textScale = Mth.clamp(textScale, MIN_TEXT_SCALE, MAX_TEXT_SCALE);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** How it answers a redstone signal: not at all (the default), showing its display only while powered, or hiding it then. */
    public RedstoneMode redstone() {
        return redstone;
    }

    public void setRedstone(RedstoneMode mode) {
        redstone = mode;
        setChanged();
        refresh();
    }

    /** Whether a redstone signal reaches it, as last seen by the server. */
    public boolean powered() {
        return powered;
    }

    /** Records whether a redstone signal reaches it, sending a change to clients. */
    public void setPowered(boolean powered) {
        if (this.powered == powered) return;
        this.powered = powered;
        setChanged();
        refresh();
    }

    /** Whether its display (its item or mob, its effects and its text) shows, as its redstone setting and signal decide. */
    public boolean displayed() {
        return switch (redstone) {
            case IGNORE -> true;
            case SHOW -> powered;
            case HIDE -> !powered;
        };
    }

    /** Whether it makes its own sounds (the default): a trophy's ambient sound and bursts, a display's item going on and off. */
    public boolean sounds() {
        return sounds;
    }

    public void setSounds(boolean sounds) {
        this.sounds = sounds;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Whether its flourishes show (the default): its rank's beam, halos, crown, flares, particles and any added effects.
     * Without them it still shows its showcase light and outlines, its item or mob and its text.
     */
    public boolean effects() {
        return effects;
    }

    public void setEffects(boolean effects) {
        this.effects = effects;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** The way what it shows faces, turned about its upright axis (where a spinning one starts from), in degrees from 0 to 360 (the same way as 0). */
    public float yaw() {
        return yaw;
    }

    public void setYaw(float degrees) {
        yaw = Mth.clamp(degrees, 0.0F, 360.0F);
        changedRotation();
    }

    /** How far what it shows is tipped forward (or, below 0, back), in degrees from -180 to 180. */
    public float pitch() {
        return pitch;
    }

    public void setPitch(float degrees) {
        pitch = Mth.clamp(degrees, -MAX_TILT, MAX_TILT);
        changedRotation();
    }

    /** How far what it shows is rolled to its side about the way it faces, in degrees from -180 to 180. */
    public float roll() {
        return roll;
    }

    public void setRoll(float degrees) {
        roll = Mth.clamp(degrees, -MAX_TILT, MAX_TILT);
        changedRotation();
    }

    /** Sets its yaw, pitch and roll together, sending one update. */
    public void setRotation(float yaw, float pitch, float roll) {
        this.yaw = Mth.clamp(yaw, 0.0F, 360.0F);
        this.pitch = Mth.clamp(pitch, -MAX_TILT, MAX_TILT);
        this.roll = Mth.clamp(roll, -MAX_TILT, MAX_TILT);
        changedRotation();
    }

    private void changedRotation() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** Whether its item or mob turns (the default), or stands still facing one way. */
    public boolean spins() {
        return spins;
    }

    public void setSpins(boolean spins) {
        this.spins = spins;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Whether a mob trophy's mob moves as it would (the default: a blaze's rods turning, an allay's wings beating), or
     * holds one pose, for a mob whose moving looks restless on a pedestal.
     */
    public boolean animates() {
        return animates;
    }

    public void setAnimates(boolean animates) {
        this.animates = animates;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Whether an achievement trophy earned by a player shows that player's statue, in their own skin, in place of the tier's
     * icon; off by default.
     */
    public boolean statue() {
        return statue;
    }

    public void setStatue(boolean statue) {
        this.statue = statue;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** The pose a statue it shows takes (an achievement trophy's statue, or a player trophy's): standing, by default. */
    public StatuePose statuePose() {
        return statuePose;
    }

    public void setStatuePose(StatuePose pose) {
        this.statuePose = pose;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /**
     * The animation its mob or statue plays on loop while it {@link #animates()}: {@link #IDLE} (the default), {@code walk},
     * {@code run}, {@code attack}, or one of the mob's own (as named by its renderer's state, like a warden's {@code roar});
     * one the mob doesn't have plays as idle.
     */
    public String animation() {
        return animation;
    }

    public void setAnimation(String animation) {
        this.animation = animationId(animation);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    // A safe animation name (lower-case letters, digits and underscores, at most 32), or idle.
    private static String animationId(@Nullable String animation) {
        return animation != null && animation.matches("[a-z0-9_]{1,32}") ? animation : IDLE;
    }

    /** Whether what it shows is a statue (an achievement trophy set to show its earner's, or a player trophy), which can be posed. */
    public boolean showsStatue() {
        return statue && canShowStatue() || mobTrophy != null && mobTrophy.type() == Statues.FIGURE;
    }

    /** Whether it can show a statue: an achievement trophy earned by one player. */
    public boolean canShowStatue() {
        return record != null && record.unlocker() == PedestalRecord.Unlocker.PLAYER && !record.name().isEmpty();
    }

    /** When its text shows: while a player looks at it (the default), always, or never. */
    public TextMode textMode() {
        return textMode;
    }

    public void setTextMode(TextMode mode) {
        textMode = mode;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** As it's broken (on 1.21.1 its block's onRemove calls this). */
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !display.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, display);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = ValueOutput.of(tag, registries);
        if (record != null) output.store(RECORD, PedestalRecord.CODEC, record);
        if (mobTrophy != null) output.store(MOB_TROPHY, MobTrophyRecord.CODEC, mobTrophy);
        if (material != null) output.store(MATERIAL, ResourceLocation.CODEC, material);
        if (!display.isEmpty()) output.store(DISPLAY, ItemStack.CODEC, display);
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
        if (!effects) output.putBoolean(NO_EFFECTS, true);
        if (!sounds) output.putBoolean(MUTED, true);
        if (redstone != RedstoneMode.IGNORE) output.store(REDSTONE, RedstoneMode.CODEC, redstone);
        if (powered) output.putBoolean(POWERED, true);
        if (placedAt > Long.MIN_VALUE / 2) output.putLong(PLACED_AT, placedAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
        record = input.read(RECORD, PedestalRecord.CODEC).orElse(null);
        MobTrophyRecord shown = mobTrophy;
        mobTrophy = input.read(MOB_TROPHY, MobTrophyRecord.CODEC).orElse(null);
        // A different mob, or a different look: made again when next drawn.
        if (!Objects.equals(shown, mobTrophy)) shownMob = null;
        material = input.read(MATERIAL, ResourceLocation.CODEC).orElse(null);
        display = input.read(DISPLAY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
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
        // (One saved before pitch and roll keeps the way it faced as its yaw.)
        yaw = Mth.clamp(input.getFloatOr(YAW, input.getFloatOr(OLD_FACING, 0.0F)), 0.0F, 360.0F);
        pitch = Mth.clamp(input.getFloatOr(PITCH, 0.0F), -MAX_TILT, MAX_TILT);
        roll = Mth.clamp(input.getFloatOr(ROLL, 0.0F), -MAX_TILT, MAX_TILT);
        effects = !input.getBooleanOr(NO_EFFECTS, false);
        sounds = !input.getBooleanOr(MUTED, false);
        redstone = input.read(REDSTONE, RedstoneMode.CODEC).orElse(RedstoneMode.IGNORE);
        powered = input.getBooleanOr(POWERED, false);
        textMode = input.read(TEXT_MODE, TextMode.CODEC).orElse(input.getBooleanOr(TEXT_ALWAYS, false) ? TextMode.ALWAYS : TextMode.LOOKING);
        placedAt = input.getLongOr(PLACED_AT, Long.MIN_VALUE / 2);
        boolean on = on();
        if (level == null) {
            // Read with its chunk, before it's in a level: as it was, nothing to play.
            wasOn = on;
            synced = true;
        } else if (level.isClientSide()) {
            // Turned on or off since its last word; or first heard of just after it was placed, already on.
            boolean placedNow = !synced && on && level.getGameTime() - placedAt < JUST_PLACED;
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
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (record != null) components.set(Pedestals.RECORD.get(), record);
        if (mobTrophy != null) components.set(Pedestals.MOB_TROPHY.get(), mobTrophy);
        if (material != null) components.set(Pedestals.PEDESTAL_MATERIAL.get(), material);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        ValueOutput output = ValueOutput.of(tag);
        output.discard(RECORD);
        output.discard(MOB_TROPHY);
        output.discard(MATERIAL);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    /** How a pedestal answers a redstone signal. */
    /** Who besides its owner and operators may adjust a pedestal. */
    public enum Access implements StringRepresentable {
        /** Only its owner. */
        PRIVATE,
        /** Its owner's team too. */
        TEAM,
        /** Everyone. */
        EVERYONE;

        static final Codec<Access> CODEC = StringRepresentable.fromEnum(Access::values);

        /** The next one, as the screen's button cycles them. */
        public Access next() {
            return values()[(ordinal() + 1) % values().length];
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum RedstoneMode implements StringRepresentable {
        /** Not at all. */
        IGNORE,
        /** Its display shows only while powered. */
        SHOW,
        /** Its display hides while powered. */
        HIDE;

        static final Codec<RedstoneMode> CODEC = StringRepresentable.fromEnum(RedstoneMode::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** The poses a statue on a pedestal can take, as vanilla's mannequin can. */
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

        /** The figure's own pose. */
        public Pose pose() {
            return pose;
        }

        /** Whether the figure lies along the ground (no head on top to wear a crown). */
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

    /** When a pedestal's text shows. */
    public enum TextMode implements StringRepresentable {
        /** While a player looks at it, within their reach. */
        LOOKING,
        /** All the time, within name tags' usual distance. */
        ALWAYS,
        /** Never. */
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
