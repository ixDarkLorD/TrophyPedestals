package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.compat.render.ArmedEntityRenderState;
import net.ixdarklord.trophypedestals.compat.render.EntityRenderState;
import net.ixdarklord.trophypedestals.compat.render.LivingEntityRenderState;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The animations a mob (or a player's statue) on a pedestal can play on loop, found from the mob itself rather than a
 * fixed list (1.20.1's version: a mob's animations are kept on the mob, not on a render state). Candidates are:
 * <ul>
 *     <li>walking and running (its walk cycle), and attacking (its swing), each listed only if it moves the mob's model;</li>
 *     <li>its own keyframe animations, from its {@code AnimationState}s (a warden's roar, a camel's sitting), looped.
 *     They're found by their names ("roarAnimationState"), which only a game running with Mojang's names has (NeoForge);
 *     under Fabric's names none are found.</li>
 * </ul>
 * Names are shown through {@code trophypedestals.animation.<id>} or made readable. (26.1's on/off states and poses, read
 * from its render states' fields, have no counterpart here.)
 */
public final class MobAnimations {
    // How fast the walk cycle goes walking and running (vanilla's limb swing speed, 0 to 1).
    private static final float WALK_SPEED = 0.5F, RUN_SPEED = 1.0F;
    // An attack's swing and the rest after it, in ticks.
    private static final int SWING = 10, SWING_PERIOD = 20;
    // How long one of a mob's own animations is taken to last (its model doesn't say here), in ticks.
    private static final int UNKNOWN_LENGTH = 60;
    private static final String STATE_SUFFIX = "AnimationState";
    // How far a part must move or turn for a candidate to count as moving the model.
    private static final float MOVED = 1.0E-3F;

    private static final Map<EntityType<?>, List<Animation>> FOUND = new ConcurrentHashMap<>();

    private MobAnimations() {}

    /** One a mob can play: its name, how it's played, and what it sets. */
    public record Animation(String id, Kind kind, @Nullable Field field, @Nullable Object value, int period) {
        /** Its name for players: {@code trophypedestals.animation.<id>}, or the name made readable ("sit_pose" as "Sit Pose"). */
        public Component name() {
            return MobAnimations.name(id);
        }
    }

    enum Kind { IDLE, WALK, RUN, SWING, KEYFRAME }

    /** A name for players: {@code trophypedestals.animation.<id>}, or the name made readable. */
    public static Component name(String id) {
        StringBuilder readable = new StringBuilder();
        for (String word : id.split("_")) {
            if (word.isEmpty()) continue;
            if (!readable.isEmpty()) readable.append(' ');
            readable.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return Component.translatableWithFallback("trophypedestals.animation." + id, readable.toString());
    }

    /** What this mob can play, idle first; just idle for one that can't be looked into. */
    public static List<Animation> of(Entity mob) {
        return FOUND.computeIfAbsent(mob.getType(), type -> find(mob));
    }

    /**
     * Plays the named animation on the mob as it's drawn, at {@code time} (in ticks): its own keyframe animations other
     * than that one stopped. One it doesn't have plays as idle.
     */
    public static void play(Entity mob, EntityRenderState shown, String id, float time) {
        List<Animation> animations = of(mob);
        Animation chosen = null;
        for (Animation animation : animations) {
            if (animation.id().equals(id)) chosen = animation;
        }
        if (chosen == null) chosen = animations.get(0);
        try {
            for (Animation animation : animations) {
                if (animation != chosen && animation.kind() == Kind.KEYFRAME && animation.field().get(mob) instanceof AnimationState state) state.stop();
            }
            apply(chosen, mob, shown, time);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // (Looked into once already; one that turns out not to play is left standing.)
        }
    }

    private static void apply(Animation animation, Entity mob, EntityRenderState shown, float time) throws ReflectiveOperationException {
        switch (animation.kind()) {
            case IDLE -> {}
            case WALK, RUN -> {
                if (shown instanceof LivingEntityRenderState living) {
                    float speed = animation.kind() == Kind.WALK ? WALK_SPEED : RUN_SPEED;
                    living.walkAnimationSpeed = speed;
                    living.walkAnimationPos = time * speed;
                }
            }
            case SWING -> {
                if (shown instanceof ArmedEntityRenderState armed) {
                    float into = time % SWING_PERIOD;
                    armed.attackTime = into < SWING ? into / SWING : 0.0F;
                }
            }
            case KEYFRAME -> {
                if (animation.field().get(mob) instanceof AnimationState state) {
                    int now = (int) Math.floor(time);
                    state.start(now - now % animation.period());
                }
            }
        }
    }

    // --- Finding them ---

    private static List<Animation> find(Entity mob) {
        List<Animation> found = new ArrayList<>();
        found.add(new Animation(PedestalBlockEntity.IDLE, Kind.IDLE, null, null, 1));
        try {
            EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(mob);
            EntityModel<?> model = renderer instanceof LivingEntityRenderer<?, ?> living ? living.getModel() : null;
            List<Animation> candidates = candidates(mob);
            Animation ownIdle = null;
            for (Animation candidate : candidates) {
                if (candidate.kind() == Kind.KEYFRAME && candidate.id().equals(PedestalBlockEntity.IDLE)) ownIdle = candidate;
            }
            // A mob's own idle animation plays as Idle.
            if (ownIdle != null) found.set(0, ownIdle);
            for (Animation candidate : candidates) {
                if (candidate == ownIdle || found.stream().anyMatch(a -> a.id().equals(candidate.id()))) continue;
                // Keyframe animations are always the mob's own; the rest are listed only if they move its model.
                if (candidate.kind() == Kind.KEYFRAME || model != null && mob instanceof LivingEntity living && moves(living, model, candidate)) found.add(candidate);
            }
        } catch (RuntimeException | LinkageError e) {
            TrophyPedestalsMod.LOGGER.debug("Can't look into {}'s animations", EntityType.getKey(mob.getType()), e);
        }
        return List.copyOf(found);
    }

    // Everything the mob might play, before trying them on its model.
    private static List<Animation> candidates(Entity mob) {
        List<Animation> all = new ArrayList<>();
        if (mob instanceof LivingEntity) {
            all.add(new Animation("walk", Kind.WALK, null, null, 1));
            all.add(new Animation("run", Kind.RUN, null, null, 1));
            all.add(new Animation("attack", Kind.SWING, null, null, SWING_PERIOD));
        }
        for (Class<?> type = mob.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                String name = field.getName();
                if (Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers) || field.getType() != AnimationState.class) continue;
                if (!name.endsWith(STATE_SUFFIX) || name.length() <= STATE_SUFFIX.length()) continue;
                add(all, new Animation(snake(name.substring(0, name.length() - STATE_SUFFIX.length())), Kind.KEYFRAME, field, null, UNKNOWN_LENGTH));
            }
        }
        return all;
    }

    // Ids must be short and plain (they're saved on the pedestal).
    private static void add(List<Animation> list, Animation animation) {
        if (animation.id().matches("[a-z0-9_]{1,32}")) list.add(animation);
    }

    // Whether the candidate moves the mob's model away from how it stands: its model posed once standing, once a moment
    // into the candidate (where a swing or a step has moved something).
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean moves(LivingEntity mob, EntityModel<?> model, Animation candidate) {
        try {
            EntityModel raw = model;
            float age = SWING / 2.0F;
            raw.attackTime = 0.0F;
            raw.setupAnim(mob, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
            float[] before = pose(model);
            float limb = 0.0F, amount = 0.0F;
            switch (candidate.kind()) {
                case WALK, RUN -> {
                    amount = candidate.kind() == Kind.WALK ? WALK_SPEED : RUN_SPEED;
                    limb = age * amount;
                }
                case SWING -> raw.attackTime = 0.5F;
                default -> {}
            }
            raw.setupAnim(mob, limb, amount, age, 0.0F, 0.0F);
            float[] after = pose(model);
            raw.attackTime = 0.0F;
            for (int i = 0; i < before.length; i++) {
                if (Math.abs(before[i] - after[i]) > MOVED) return true;
            }
            return false;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    // Every part's place, turn, size and visibility, one after another: the parts the model keeps (found by their type,
    // whatever their names), with those under them.
    private static float[] pose(EntityModel<?> model) {
        Map<ModelPart, Boolean> parts = new IdentityHashMap<>();
        for (Class<?> type = model.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType() != ModelPart.class) continue;
                try {
                    field.setAccessible(true);
                    if (field.get(model) instanceof ModelPart part) part.getAllParts().forEach(found -> parts.put(found, true));
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // (A part that can't be read isn't compared.)
                }
            }
        }
        Set<ModelPart> all = parts.keySet();
        float[] pose = new float[all.size() * 10];
        int i = 0;
        for (ModelPart part : all) {
            pose[i++] = part.x;
            pose[i++] = part.y;
            pose[i++] = part.z;
            pose[i++] = part.xRot;
            pose[i++] = part.yRot;
            pose[i++] = part.zRot;
            pose[i++] = part.xScale;
            pose[i++] = part.yScale;
            pose[i++] = part.zScale;
            pose[i++] = part.visible ? 1.0F : 0.0F;
        }
        return pose;
    }

    // "sitPose" as "sit_pose".
    private static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
