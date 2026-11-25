package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public final class CameraRenderState {
    public @Nullable Quaternionf orientation;
    public Vec3 pos = Vec3.ZERO;

    public static CameraRenderState of(Camera camera) {
        CameraRenderState state = new CameraRenderState();
        state.orientation = new Quaternionf(camera.rotation());
        state.pos = camera.getPosition();
        return state;
    }

    public static CameraRenderState current() {
        return of(Minecraft.getInstance().gameRenderer.getMainCamera());
    }
}
