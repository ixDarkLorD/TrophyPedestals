package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The animations a mob (or a player's statue) on a pedestal can play on loop, found from the mob itself rather than a
 * fixed list. Its renderer's state and model are looked into once per kind of mob, and each candidate is tried on the
 * model: only those that actually move it are listed. Candidates are:
 * <ul>
 *     <li>walking and running (its walk cycle), and attacking (a swing, for an armed or golem-like mob);</li>
 *     <li>its own keyframe animations, from its render state's {@code AnimationState}s (a warden's roar, a camel's
 *     sitting), looped over their own length;</li>
 *     <li>its own on/off states, from its render state's flags (a fox sitting or sleeping, a zombie's raised arms);</li>
 *     <li>its own poses, from its render state's choices (a parrot's flying and dancing).</li>
 * </ul>
 * Names come from the game's own field names ("isSitting" as "sitting"), shown through {@code trophypedestals.animation.<id>}
 * or made readable.
 */
public final class MobAnimations {
    // How fast the walk cycle goes walking and running (vanilla's limb swing speed, 0 to 1).
    private static final float WALK_SPEED = 0.5F, RUN_SPEED = 1.0F;
    // An attack's swing and the rest after it, in ticks.
    private static final int SWING = 10, SWING_PERIOD = 20;
    // How long one of a mob's own animations is taken to last when its model doesn't say, and the rest between plays of
    // one that doesn't loop by itself, in ticks.
    private static final int UNKNOWN_LENGTH = 60, REST = 10;
    private static final String STATE_SUFFIX = "AnimationState";
    // The render states whose fields aren't a mob's own (where it is, how it's lit, what it wears): their flags and
    // choices aren't offered.
    private static final Set<Class<?>> SHARED_STATES = Set.of(EntityRenderState.class, LivingEntityRenderState.class,
            ArmedEntityRenderState.class, HumanoidRenderState.class, AvatarRenderState.class);
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

    enum Kind { IDLE, WALK, RUN, SWING, GOLEM_SWING, KEYFRAME, FLAG, CHOICE }

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
     * Plays the named animation on what's drawn of the mob, at {@code time} (in ticks): its own keyframe animations other
     * than that one stopped. One it doesn't have plays as idle.
     */
    public static void play(Entity mob, EntityRenderState shown, String id, float time) {
        List<Animation> animations = of(mob);
        Animation chosen = null;
        for (Animation animation : animations) {
            if (animation.id().equals(id)) chosen = animation;
        }
        if (chosen == null) chosen = animations.getFirst();
        try {
            for (Animation animation : animations) {
                if (animation != chosen && animation.kind() == Kind.KEYFRAME && animation.field().get(shown) instanceof AnimationState state) state.stop();
            }
            apply(chosen, shown, time);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // (Looked into once already; one that turns out not to play is left standing.)
        }
    }

    private static void apply(Animation animation, EntityRenderState shown, float time) throws ReflectiveOperationException {
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
                    armed.swingAnimation = into < SWING ? into / SWING : 0.0F;
                }
            }
            case GOLEM_SWING -> animation.field().setFloat(shown, Math.max(0.0F, SWING - time % SWING_PERIOD));
            case KEYFRAME -> {
                if (animation.field().get(shown) instanceof AnimationState state) {
                    int now = (int) Math.floor(time);
                    state.start(now - now % animation.period());
                }
            }
            case FLAG -> animation.field().setBoolean(shown, true);
            case CHOICE -> animation.field().set(shown, animation.value());
        }
    }

    // --- Finding them ---

    private static List<Animation> find(Entity mob) {
        List<Animation> found = new ArrayList<>();
        found.add(new Animation(PedestalBlockEntity.IDLE, Kind.IDLE, null, null, 1));
        try {
            EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(mob);
            Model<?> model = renderer instanceof LivingEntityRenderer<?, ?, ?> living ? living.getModel() : null;
            List<Animation> candidates = candidates(mob, model);
            Animation ownIdle = null;
            for (Animation candidate : candidates) {
                if (candidate.kind() == Kind.KEYFRAME && candidate.id().equals(PedestalBlockEntity.IDLE)) ownIdle = candidate;
            }
            // A mob's own idle animation plays as Idle.
            if (ownIdle != null) found.set(0, ownIdle);
            for (Animation candidate : candidates) {
                if (candidate == ownIdle || found.stream().anyMatch(a -> a.id().equals(candidate.id()))) continue;
                // Keyframe animations are always the mob's own; the rest are listed only if they move its model.
                if (candidate.kind() == Kind.KEYFRAME || model == null || moves(mob, model, candidate)) found.add(candidate);
            }
        } catch (RuntimeException | LinkageError e) {
            TrophyPedestalsMod.LOGGER.debug("Can't look into {}'s animations", EntityType.getKey(mob.getType()), e);
        }
        return List.copyOf(found);
    }

    // Everything the mob might play, before trying them on its model.
    private static List<Animation> candidates(Entity mob, @Nullable Model<?> model) {
        EntityRenderState shown = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(mob, 0.0F);
        List<Animation> walking = new ArrayList<>(), keyframes = new ArrayList<>(), flags = new ArrayList<>(), choices = new ArrayList<>();
        Field golemSwing = null;
        for (Class<?> type = shown.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            boolean own = !SHARED_STATES.contains(type);
            for (Field field : type.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) continue;
                String name = field.getName();
                if (field.getType() == AnimationState.class && name.endsWith(STATE_SUFFIX) && name.length() > STATE_SUFFIX.length()) {
                    String base = name.substring(0, name.length() - STATE_SUFFIX.length());
                    add(keyframes, new Animation(snake(base), Kind.KEYFRAME, field, null, period(model, base)));
                } else if (field.getType() == float.class && name.equals("attackTicksRemaining")) {
                    golemSwing = field;
                } else if (own && !Modifier.isFinal(modifiers) && field.getType() == boolean.class) {
                    add(flags, new Animation(snake(name.startsWith("is") && name.length() > 2 ? name.substring(2) : name), Kind.FLAG, field, true, 1));
                } else if (own && !Modifier.isFinal(modifiers) && field.getType().isEnum()) {
                    for (Object constant : field.getType().getEnumConstants()) {
                        add(choices, new Animation(snake(name) + "_" + ((Enum<?>) constant).name().toLowerCase(Locale.ROOT), Kind.CHOICE, field, constant, 1));
                    }
                }
            }
        }
        if (shown instanceof LivingEntityRenderState) {
            walking.add(new Animation("walk", Kind.WALK, null, null, 1));
            walking.add(new Animation("run", Kind.RUN, null, null, 1));
        }
        if (shown instanceof ArmedEntityRenderState) walking.add(new Animation("attack", Kind.SWING, null, null, SWING_PERIOD));
        else if (golemSwing != null) walking.add(new Animation("attack", Kind.GOLEM_SWING, golemSwing, null, SWING_PERIOD));
        List<Animation> all = new ArrayList<>(walking);
        all.addAll(flags);
        all.addAll(choices);
        all.addAll(keyframes);
        return all;
    }

    // Ids must be short and plain (they're saved on the pedestal).
    private static void add(List<Animation> list, Animation animation) {
        if (animation.id().matches("[a-z0-9_]{1,32}")) list.add(animation);
    }

    // Whether the candidate moves the mob's model away from how it stands: tried once on a fresh copy of what's drawn.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean moves(Entity mob, Model<?> model, Animation candidate) {
        try {
            Model raw = model;
            EntityRenderState still = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(mob, 0.0F);
            raw.setupAnim(still);
            float[] before = pose(model);
            EntityRenderState moving = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(mob, 0.0F);
            // A moment into it, where a swing or a step has moved something.
            moving.ageInTicks = SWING / 2.0F;
            apply(candidate, moving, SWING / 2.0F);
            raw.setupAnim(moving);
            float[] after = pose(model);
            for (int i = 0; i < before.length; i++) {
                if (Math.abs(before[i] - after[i]) > MOVED) return true;
            }
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }

    // Every part's place, turn, size and visibility, one after another.
    private static float[] pose(Model<?> model) {
        List<ModelPart> parts = model.allParts();
        float[] pose = new float[parts.size() * 10];
        int i = 0;
        for (ModelPart part : parts) {
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

    // How long one of the mob's own animations takes before it plays again, in ticks: its model's keyframe animation of
    // the same name ("roar" for "roarAnimationState"), with a rest after one that doesn't loop by itself.
    private static int period(@Nullable Model<?> model, String base) {
        if (model == null) return UNKNOWN_LENGTH;
        try {
            for (Class<?> type = model.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (field.getType() != KeyframeAnimation.class || !field.getName().equals(base + "Animation")) continue;
                    field.setAccessible(true);
                    Object animation = field.get(model);
                    if (animation == null) return UNKNOWN_LENGTH;
                    Field definition = KeyframeAnimation.class.getDeclaredField("definition");
                    definition.setAccessible(true);
                    AnimationDefinition read = (AnimationDefinition) definition.get(animation);
                    int length = Math.max(1, Math.round(read.lengthInSeconds() * 20.0F));
                    return read.looping() ? length : length + REST;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            return UNKNOWN_LENGTH;
        }
        return UNKNOWN_LENGTH;
    }

    // "sitPose" as "sit_pose".
    private static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
