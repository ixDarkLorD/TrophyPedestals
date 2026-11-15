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
 * fixed list.
 */
public final class MobAnimations {
    private static final float WALK_SPEED = 0.5F, RUN_SPEED = 1.0F;
    private static final int SWING = 10, SWING_PERIOD = 20;
    private static final int UNKNOWN_LENGTH = 60, REST = 10;
    private static final String STATE_SUFFIX = "AnimationState";
    private static final Set<Class<?>> SHARED_STATES = Set.of(EntityRenderState.class, LivingEntityRenderState.class,
            ArmedEntityRenderState.class, HumanoidRenderState.class, AvatarRenderState.class);
    private static final float MOVED = 1.0E-3F;

    private static final Map<EntityType<?>, List<Animation>> FOUND = new ConcurrentHashMap<>();

    private MobAnimations() {}

    public record Animation(String id, Kind kind, @Nullable Field field, @Nullable Object value, int period) {
        public Component name() {
            return MobAnimations.name(id);
        }
    }

    enum Kind { IDLE, WALK, RUN, SWING, GOLEM_SWING, KEYFRAME, FLAG, CHOICE }

    public static Component name(String id) {
        StringBuilder readable = new StringBuilder();
        for (String word : id.split("_")) {
            if (word.isEmpty()) continue;
            if (!readable.isEmpty()) readable.append(' ');
            readable.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return Component.translatableWithFallback("trophypedestals.animation." + id, readable.toString());
    }

    public static List<Animation> of(Entity mob) {
        return FOUND.computeIfAbsent(mob.getType(), type -> find(mob));
    }

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
                    armed.attackTime = into < SWING ? into / SWING : 0.0F;
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
            if (ownIdle != null) found.set(0, ownIdle);
            for (Animation candidate : candidates) {
                if (candidate == ownIdle || found.stream().anyMatch(a -> a.id().equals(candidate.id()))) continue;
                if (candidate.kind() == Kind.KEYFRAME || model == null || moves(mob, model, candidate)) found.add(candidate);
            }
        } catch (RuntimeException | LinkageError e) {
            TrophyPedestalsMod.LOGGER.debug("Can't look into {}'s animations", EntityType.getKey(mob.getType()), e);
        }
        return List.copyOf(found);
    }

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

    private static void add(List<Animation> list, Animation animation) {
        if (animation.id().matches("[a-z0-9_]{1,32}")) list.add(animation);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean moves(Entity mob, Model<?> model, Animation candidate) {
        try {
            Model raw = model;
            EntityRenderState still = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(mob, 0.0F);
            raw.setupAnim(still);
            float[] before = pose(model);
            EntityRenderState moving = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(mob, 0.0F);
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

    private static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
