package net.ixdarklord.trophypedestals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.compat.render.CameraRenderState;
import net.ixdarklord.trophypedestals.compat.render.EntityRenderState;
import net.ixdarklord.trophypedestals.compat.render.EntityRenderStates;
import net.ixdarklord.trophypedestals.compat.render.SubmitNodeCollector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mobs made only to be drawn (on a pedestal, in a slot, on a recipe page) never join a level, so nothing numbers them.
 */
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
        while (!OWN_POSE.clear()) OWN_POSE.popPose();
        OWN_POSE.last().pose().set(poseStack.last().pose());
        OWN_POSE.last().normal().set(poseStack.last().normal());
        try {
            EntityRenderStates.submit(mob, camera, 0.0, 0.0, 0.0, OWN_POSE, collector);
            return true;
        } catch (RuntimeException | LinkageError e) {
            if (mob.entityType != null && FAILED.add(mob.entityType)) {
                TrophyPedestalsMod.LOGGER.warn("Can't draw {} on a trophy pedestal; its icon shows instead", BuiltInRegistries.ENTITY_TYPE.getKey(mob.entityType), e);
            }
            return false;
        }
    }
}
