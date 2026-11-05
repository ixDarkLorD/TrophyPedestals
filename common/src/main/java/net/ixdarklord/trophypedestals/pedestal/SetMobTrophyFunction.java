package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The {@code trophypedestals:set_mob_trophy} loot function: makes a pedestal a mob trophy, so data packs can hand them out as
 * quest rewards, from loot tables, advancement rewards or {@code /loot}, or make a mob always drop one.
 * <ul>
 *   <li>{@code "entity"}: the mob it shows. Without it, the mob the loot is for (in a mob's own loot table: the mob
 *   that died), which also gives it that mob's look; anything else leaves the item as it is.</li>
 *   <li>{@code "look"}: how the mob looks, in its own saved-data format ({@code {"IsBaby": true}}). Without it, the
 *   look of the mob the loot is for, if it's that kind of mob; otherwise as it spawns.</li>
 *   <li>{@code "slayer"}: who slew it. Without it, the player who last hurt the mob the loot is for, or the player the
 *   loot is for (one opening a chest, or earning an advancement).</li>
 *   <li>{@code "rarity"}: its rank ({@code "epic"}, a mod's {@code namespace:path}, or {@code "none"}). Without it, by
 *   the mob's toughness, as mob trophies are ranked by strength.</li>
 * </ul>
 * It's recorded as slain now. Anything but a pedestal is left as it is.
 * <pre>{@code
 * {"function": "trophypedestals:set_mob_trophy", "entity": "minecraft:wither", "rarity": "legendary"}
 * }</pre>
 */
public class SetMobTrophyFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetMobTrophyFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance).and(instance.group(
            Identifier.CODEC.optionalFieldOf("entity").forGetter(function -> function.entity),
            CompoundTag.CODEC.optionalFieldOf("look").forGetter(function -> function.look),
            Codec.STRING.optionalFieldOf("slayer").forGetter(function -> function.slayer),
            PedestalRarities.ID_CODEC.optionalFieldOf("rarity").forGetter(function -> function.rarity)
    )).apply(instance, SetMobTrophyFunction::new));

    private final Optional<Identifier> entity;
    private final Optional<CompoundTag> look;
    private final Optional<String> slayer;
    private final Optional<Identifier> rarity;

    private SetMobTrophyFunction(List<LootItemCondition> predicates, Optional<Identifier> entity, Optional<CompoundTag> look,
                                 Optional<String> slayer, Optional<Identifier> rarity) {
        super(predicates);
        this.entity = entity;
        this.look = look;
        this.slayer = slayer;
        this.rarity = rarity;
    }

    @Override
    public MapCodec<SetMobTrophyFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof PedestalBlock)) return stack;
        Entity subject = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        // The mob the loot is for, if it's one that could have a trophy.
        LivingEntity mob = subject instanceof LivingEntity living && MobTrophies.droppable(living) ? living : null;
        EntityType<?> type;
        if (entity.isPresent()) type = BuiltInRegistries.ENTITY_TYPE.getOptional(entity.get()).orElse(null);
        else type = mob == null ? null : mob.getType();
        if (type == null) return stack;
        // Its look and rank are that mob's only if it's the kind shown.
        LivingEntity measured = mob != null && mob.getType() == type ? mob : null;
        CompoundTag shown = look.orElseGet(() -> measured == null ? new CompoundTag() : MobLooks.capture(measured));
        String who = slayer.orElseGet(() -> {
            Player hurt = context.getOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER);
            if (hurt != null) return hurt.getName().getString();
            return subject instanceof Player player ? player.getName().getString() : "";
        });
        Identifier rank = rarity.orElseGet(() -> (measured != null ? MobTrophies.rarity(measured) : MobTrophies.rarity(type, context.getLevel())).id());
        MobTrophies.record(stack, new MobTrophyRecord(BuiltInRegistries.ENTITY_TYPE.getKey(type), who, System.currentTimeMillis(), rank, shown));
        return stack;
    }
}
