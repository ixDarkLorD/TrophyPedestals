package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.TrophyKinds;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;
import java.util.Set;

/**
 * The {@code trophypedestals:set_trophy} loot function: makes a pedestal a trophy recording an achievement (its
 * {@code "kind"}, a mod's {@code TrophyKind}, and its {@code "subject"}), so data packs can
 * hand trophies out from loot tables, advancement rewards or {@code /loot}. It records the unlock as of now, by
 * {@code "unlocker"} ({@code player}, the default, {@code team} or {@code server}) named {@code "name"}, or, without a
 * name, the entity the loot is for, with the kind's own rarity for it or the one named in {@code "rarity"}. Anything but a
 * pedestal is left as it is.
 * <pre>{@code
 * {"function": "trophypedestals:set_trophy", "kind": "tierwise:tier", "subject": "mypack:iron_age", "rarity": "epic"}
 * }</pre>
 */
public class SetTrophyFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetTrophyFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance).and(instance.group(
            Identifier.CODEC.fieldOf("kind").forGetter(function -> function.kind),
            Identifier.CODEC.fieldOf("subject").forGetter(function -> function.subject),
            PedestalRecord.Unlocker.CODEC.optionalFieldOf("unlocker", PedestalRecord.Unlocker.PLAYER).forGetter(function -> function.unlocker),
            Codec.STRING.optionalFieldOf("name").forGetter(function -> function.name),
            PedestalRarities.ID_CODEC.optionalFieldOf("rarity").forGetter(function -> function.rarity)
    )).apply(instance, SetTrophyFunction::new));

    private final Identifier kind;
    private final Identifier subject;
    private final PedestalRecord.Unlocker unlocker;
    private final Optional<String> name;
    private final Optional<Identifier> rarity;

    private SetTrophyFunction(Optional<Holder<LootItemCondition>> condition, Identifier kind, Identifier subject, PedestalRecord.Unlocker unlocker, Optional<String> name,
                              Optional<Identifier> rarity) {
        super(condition);
        this.kind = kind;
        this.subject = subject;
        this.unlocker = unlocker;
        this.name = name;
        this.rarity = rarity;
    }

    @Override
    public MapCodec<SetTrophyFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof PedestalBlock)) return stack;
        String who = name.orElseGet(() -> {
            Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
            return entity == null ? "" : entity.getName().getString();
        });
        Identifier rank = rarity.orElseGet(() -> TrophyKinds.rarity(kind, subject).id());
        Pedestals.record(stack, new PedestalRecord(kind, subject, unlocker, who, System.currentTimeMillis(), rank));
        return stack;
    }
}
