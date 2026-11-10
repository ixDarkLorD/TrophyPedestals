package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.ixdarklord.trophypedestals.compat.ResolvableProfile;
import net.ixdarklord.trophypedestals.compat.ValueInput;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A player's statue on a pedestal, on 1.20.1 (which has no mannequin): a player figure that exists only to be drawn,
 * wearing the skin of the player its look names ({@code "profile"}), looked up as it's first shown (the default skin
 * until then). Its poses are set from outside ({@link EntityRenderStates#submit}), as a figure that never ticks can't take
 * them by itself.
 */
public final class StatueFigure extends RemotePlayer {
    private @Nullable GameProfile skinOf;
    boolean fallFlying;
    float swimAmount;
    int fallFlyingTicks;
    @Nullable Direction bed;

    private StatueFigure(ClientLevel level) {
        super(level, new GameProfile(Util.NIL_UUID, ""));
    }

    public static StatueFigure create(ClientLevel level) {
        return new StatueFigure(level);
    }

    // Reads the player whose statue it is from its look, with the rest of the look.
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        try {
            super.readAdditionalSaveData(tag);
        } catch (RuntimeException ignored) {
            // (A look is a mannequin's or a statue's, not a player's save: what a player's save has is left as it is.)
        }
        // What it wears and holds, as a mannequin's look gives them (a player's own save keeps them in its inventory).
        wear(tag, "ArmorItems", new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD});
        wear(tag, "HandItems", new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
        ValueInput.of(tag).read("profile", ResolvableProfile.CODEC).ifPresent(profile -> {
            skinOf = profile.gameProfile();
            profile.resolve().thenAccept(resolved -> skinOf = resolved.gameProfile());
        });
    }

    @Override
    public ResourceLocation getSkinTextureLocation() {
        return skinOf == null ? super.getSkinTextureLocation() : Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(skinOf);
    }

    // Slim arms for a slim skin.
    @Override
    public String getModelName() {
        if (skinOf == null) return super.getModelName();
        MinecraftProfileTexture skin = Minecraft.getInstance().getSkinManager().getInsecureSkinInformation(skinOf).get(MinecraftProfileTexture.Type.SKIN);
        String model = skin == null ? null : skin.getMetadata("model");
        return model == null ? "default" : model;
    }

    private void wear(CompoundTag tag, String key, EquipmentSlot[] slots) {
        if (!tag.contains(key, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < slots.length && i < list.size(); i++) setItemSlot(slots[i], ItemStack.of(list.getCompound(i)));
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

    // Drawn without its name over it: the game shows a player's name unless they're carrying someone.
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
