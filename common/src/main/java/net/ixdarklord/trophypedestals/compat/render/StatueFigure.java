package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.authlib.GameProfile;
import net.ixdarklord.trophypedestals.compat.ValueInput;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

/**
 * A player's statue on a pedestal, on 1.21.1 (which has no mannequin): a player figure that exists only to be drawn,
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
        ValueInput.of(tag).read("profile", ResolvableProfile.CODEC).ifPresent(profile -> {
            skinOf = profile.gameProfile();
            profile.resolve().thenAccept(resolved -> skinOf = resolved.gameProfile());
        });
    }

    @Override
    public PlayerSkin getSkin() {
        return skinOf == null ? super.getSkin() : Minecraft.getInstance().getSkinManager().getInsecureSkin(skinOf);
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
