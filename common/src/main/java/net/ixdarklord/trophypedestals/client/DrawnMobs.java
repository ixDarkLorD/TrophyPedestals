package net.ixdarklord.trophypedestals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.joml.Matrix4f;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class DrawnMobs {
    private static final AtomicInteger NEXT = new AtomicInteger(-1_000_000);
    private static final PoseStack OWN_POSE = new PoseStack();
    private static final Set<EntityType<?>> FAILED = ConcurrentHashMap.newKeySet();

    private DrawnMobs() {}

    public static <T extends Entity> T numbered(T mob) {
        mob.setId(NEXT.decrementAndGet());
        return mob;
    }

    public static boolean submit(EntityRenderState mob, CameraRenderState camera, PoseStack poseStack, SubmitNodeCollector collector) {
        return submit(mob, camera, poseStack, collector, false, 0.0F);
    }

    public static boolean submit(EntityRenderState mob, CameraRenderState camera, PoseStack poseStack, SubmitNodeCollector collector, boolean aboveFloor, float floorY) {
        while (!OWN_POSE.isEmpty()) OWN_POSE.popPose();
        OWN_POSE.last().set(poseStack.last());
        try {
            FloorClip clip = aboveFloor ? new FloorClip(new Matrix4f(OWN_POSE.last().pose()).translate(0.0F, floorY, 0.0F)) : null;
            Minecraft.getInstance().getEntityRenderDispatcher().submit(mob, camera, 0.0, 0.0, 0.0, OWN_POSE, clip != null ? clip.collector(collector) : collector);
            return true;
        } catch (RuntimeException | LinkageError e) {
            if (mob.entityType != null && FAILED.add(mob.entityType)) {
                TrophyPedestalsMod.LOGGER.warn("Can't draw {} on a trophy pedestal; its icon shows instead", BuiltInRegistries.ENTITY_TYPE.getKey(mob.entityType), e);
            }
            return false;
        }
    }

}
