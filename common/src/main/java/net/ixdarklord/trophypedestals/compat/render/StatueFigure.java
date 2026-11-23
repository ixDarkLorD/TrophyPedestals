package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.authlib.GameProfile;
import net.ixdarklord.trophypedestals.client.PosedFigure;
import net.ixdarklord.trophypedestals.compat.ValueInput;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

/**
 * A player's statue on a pedestal, on 1.21.1 (which has no mannequin): a player figure that exists only to be drawn,
 * wearing the skin of the player its look names ({@code "profile"}), looked up as it's first shown (the default skin
 * until then).
 */
public final class StatueFigure extends RemotePlayer implements PosedFigure {
    private @Nullable GameProfile skinOf;
    boolean fallFlying;
    float swimAmount;
    int fallFlyingTicks;
    @Nullable Direction bed;
    private float @Nullable [] pose;

    private StatueFigure(ClientLevel level) {
        super(level, new GameProfile(Util.NIL_UUID, ""));
    }

    public static StatueFigure create(ClientLevel level) {
        return new StatueFigure(level);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        try {
            super.readAdditionalSaveData(tag);
        } catch (RuntimeException ignored) {
        }
        wear(tag, "ArmorItems", new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD});
        wear(tag, "HandItems", new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
        ValueInput.of(tag).read("profile", ResolvableProfile.CODEC).ifPresent(profile -> {
            skinOf = profile.gameProfile();
            profile.resolve().thenAccept(resolved -> skinOf = resolved.gameProfile());
        });
    }

    @Override
    public PlayerSkin getSkin() {
        return skinOf == null ? super.getSkin() : Minecraft.getInstance().getSkinManager().getInsecureSkin(skinOf);
    }

    private void wear(CompoundTag tag, String key, EquipmentSlot[] slots) {
        if (!tag.contains(key, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < slots.length && i < list.size(); i++) setItemSlot(slots[i], ItemStack.parseOptional(registryAccess(), list.getCompound(i)));
    }

    @Override
    public float @Nullable [] trophypedestals$pose() {
        return pose;
    }

    @Override
    public void trophypedestals$setPose(float @Nullable [] pose) {
        this.pose = pose;
    }

    @Override
    public boolean isFallFlying() {
        return fallFlying;
    }

    @Override
    public int getFallFlyingTicks() {
        return fallFlyingTicks;
    }

    @Override
    public float getSwimAmount(float partialTicks) {
        return swimAmount;
    }

    @Override
    public @Nullable Direction getBedOrientation() {
        return bed;
    }

    @Override
    public boolean isVehicle() {
        return true;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }
}
