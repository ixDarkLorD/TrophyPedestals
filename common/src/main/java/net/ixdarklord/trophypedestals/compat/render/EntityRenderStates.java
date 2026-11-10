package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.1's stand-in for 26.1's extracting and drawing of a mob through render states (see {@link EntityRenderState}): a
 * mob made only to be drawn, its state taken, and the state put back on the mob as it's drawn.
 */
public final class EntityRenderStates {
    private EntityRenderStates() {}

    /** A mob of this type made only to be drawn (a player's statue figure for a player), or null if it can't be made. */
    public static @Nullable Entity create(EntityType<?> type, @Nullable Level level) {
        if (level == null) return null;
        if (type == EntityType.PLAYER) return level instanceof ClientLevel client ? StatueFigure.create(client) : null;
        return type.create(level);
    }

    /** The mob as it's to be drawn this frame. */
    public static EntityRenderState extract(Entity entity, float partialTicks) {
        EntityRenderState state;
        if (entity instanceof Player) state = new AvatarRenderState(entity);
        else if (entity instanceof EnderDragon) state = new EnderDragonRenderState(entity);
        else if (entity instanceof WitherBoss) state = new WitherRenderState(entity);
        else if (entity instanceof Squid) state = new SquidRenderState(entity);
        else if (entity instanceof Monster) state = new HumanoidRenderState(entity);
        else if (entity instanceof LivingEntity) state = new ArmedEntityRenderState(entity);
        else state = new EntityRenderState(entity);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.shadowRadius = 1.0F;
        state.lightCoords = 0xF000F0;
        return state;
    }

    /** Draws the mob as its state says, at this place in the pose. */
    public static void submit(EntityRenderState state, CameraRenderState camera, double x, double y, double z, PoseStack poseStack,
                              SubmitNodeCollector collector) {
        Entity entity = state.entity;
        int ticks = Mth.floor(state.ageInTicks);
        float partial = state.ageInTicks - ticks;
        entity.tickCount = ticks;
        apply(state);
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(state.shadowRadius > 0.0F && state.shadowPieces.isEmpty());
        try {
            dispatcher.render(entity, x, y, z, 0.0F, partial, poseStack, collector.buffers(), state.lightCoords);
        } finally {
            dispatcher.setRenderShadow(true);
        }
    }

    // What's set on the state, put on the mob.
    public static void apply(EntityRenderState state) {
        if (state instanceof EnderDragonRenderState dragon && dragon.flapTime >= 0.0F && state.entity instanceof EnderDragon entity) {
            entity.oFlapTime = entity.flapTime = dragon.flapTime;
        }
        if (!(state instanceof LivingEntityRenderState living) || !(state.entity instanceof LivingEntity entity)) return;
        if (state instanceof SquidRenderState squid && entity instanceof Squid swimmer) {
            if (!Float.isNaN(squid.xBodyRot)) swimmer.xBodyRotO = swimmer.xBodyRot = squid.xBodyRot;
            if (!Float.isNaN(squid.zBodyRot)) swimmer.zBodyRotO = swimmer.zBodyRot = squid.zBodyRot;
        }
        if (living.pose != null) entity.setPose(living.pose);
        if (living.walkAnimationSpeed >= 0.0F) walk(entity.walkAnimation, living.walkAnimationPos, living.walkAnimationSpeed);
        if (state instanceof ArmedEntityRenderState armed && armed.attackTime >= 0.0F) entity.oAttackAnim = entity.attackAnim = armed.attackTime;
        if (state instanceof HumanoidRenderState humanoid) {
            if (humanoid.isCrouching) {
                entity.setPose(Pose.CROUCHING);
                entity.setShiftKeyDown(true);
            } else {
                entity.setShiftKeyDown(false);
            }
            if (humanoid.isVisuallySwimming) entity.setPose(Pose.SWIMMING);
            if (entity instanceof StatueFigure figure) {
                figure.swimAmount = humanoid.swimAmount;
                figure.fallFlying = humanoid.isFallFlying;
                figure.fallFlyingTicks = state instanceof AvatarRenderState avatar ? (int) avatar.fallFlyingTimeInTicks : 0;
                figure.bed = living.bedOrientation;
            }
        }
    }

    // Sets a walk cycle to this place and speed: its place only moves on as it's updated, so it's moved on to just short
    // of the place, then updated at the speed twice (so its last two speeds are the speed, as drawing reads them).
    private static void walk(WalkAnimationState walk, float position, float speed) {
        walk.update(position - walk.position() - 2.0F * speed, 1.0F);
        walk.update(speed, 1.0F);
        walk.update(speed, 1.0F);
    }
}
