package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/** 1.21.1's stand-in for 26.1's {@code CameraRenderState}: where the camera is and which way it faces. */
public final class CameraRenderState {
    public @Nullable Quaternionf orientation;
    public Vec3 pos = Vec3.ZERO;

    /** The game's camera as it is this frame. */
    public static CameraRenderState of(Camera camera) {
        CameraRenderState state = new CameraRenderState();
        state.orientation = new Quaternionf(camera.rotation());
        state.pos = camera.getPosition();
        return state;
    }

    /** The game's own camera this frame. */
    public static CameraRenderState current() {
        return of(Minecraft.getInstance().gameRenderer.getMainCamera());
    }
}
