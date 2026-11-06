package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.TrophyKinds;
import net.ixdarklord.trophypedestals.compat.LootCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
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
    public static final MapCodec<SetTrophyFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> LootCodecs.commonFields(instance).and(instance.group(
            ResourceLocation.CODEC.fieldOf("kind").forGetter(function -> function.kind),
            ResourceLocation.CODEC.fieldOf("subject").forGetter(function -> function.subject),
            PedestalRecord.Unlocker.CODEC.optionalFieldOf("unlocker", PedestalRecord.Unlocker.PLAYER).forGetter(function -> function.unlocker),
            Codec.STRING.optionalFieldOf("name").forGetter(function -> function.name),
            PedestalRarities.ID_CODEC.optionalFieldOf("rarity").forGetter(function -> function.rarity)
    )).apply(instance, SetTrophyFunction::new));

    private final ResourceLocation kind;
    private final ResourceLocation subject;
    private final PedestalRecord.Unlocker unlocker;
    private final Optional<String> name;
    private final Optional<ResourceLocation> rarity;

    private SetTrophyFunction(List<LootItemCondition> predicates, ResourceLocation kind, ResourceLocation subject, PedestalRecord.Unlocker unlocker, Optional<String> name,
                              Optional<ResourceLocation> rarity) {
        super(predicates.toArray(new LootItemCondition[0]));
        this.kind = kind;
        this.subject = subject;
        this.unlocker = unlocker;
        this.name = name;
        this.rarity = rarity;
    }

    @Override
    public LootItemFunctionType getType() {
        return Pedestals.SET_TROPHY.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof PedestalBlock)) return stack;
        String who = name.orElseGet(() -> {
            Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
            return entity == null ? "" : entity.getName().getString();
        });
        ResourceLocation rank = rarity.orElseGet(() -> TrophyKinds.rarity(kind, subject).id());
        Pedestals.record(stack, new PedestalRecord(kind, subject, unlocker, who, System.currentTimeMillis(), rank));
        return stack;
    }
}
