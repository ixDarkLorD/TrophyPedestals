package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.compat.component.Components;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Sets of mob trophies: a trophy cabinet whose four spots all hold trophies of different mobs from one set (Nether, End,
 * Ocean, Bosses, Illagers) shows the set's name over it in a gold frame. A set is an entity type tag under
 * {@code trophypedestals:sets/}, so data packs can add their own ({@code data/<ns>/tags/entity_type/...} under that path
 * in the {@code trophypedestals} namespace) or change these.
 */
public final class TrophySets {
    private TrophySets() {}

    /** The set a whole cabinet (this half and its partner) makes, or null if it makes none. */
    public static @Nullable TagKey<EntityType<?>> complete(Level level, TrophyCabinetBlockEntity cabinet) {
        BlockPos other = TrophyCabinetBlock.partner(cabinet.getBlockPos(), cabinet.getBlockState());
        if (!(level.getBlockEntity(other) instanceof TrophyCabinetBlockEntity partner)) return null;
        List<EntityType<?>> mobs = new ArrayList<>();
        for (TrophyCabinetBlockEntity half : List.of(cabinet, partner)) {
            for (int i = 0; i < TrophyCabinetBlockEntity.SLOTS; i++) {
                ItemStack item = half.item(i);
                MobTrophyRecord record = Components.get(item, Pedestals.MOB_TROPHY.get());
                EntityType<?> type = record == null ? null : record.type();
                if (type == null) return null;
                mobs.add(type);
            }
        }
        Set<EntityType<?>> distinct = new HashSet<>(mobs);
        if (distinct.size() != mobs.size()) return null;
        Optional<TagKey<EntityType<?>>> found = BuiltInRegistries.ENTITY_TYPE.getTagNames()
                .filter(TrophySets::isSet)
                .filter(set -> mobs.stream().allMatch(type -> type.builtInRegistryHolder().is(set)))
                .findFirst();
        return found.orElse(null);
    }

    /** Whether this tag is a set: {@code trophypedestals:sets/...}. */
    public static boolean isSet(TagKey<EntityType<?>> tag) {
        ResourceLocation id = tag.location();
        return id.getNamespace().equals(TrophyPedestalsMod.MOD_ID) && id.getPath().startsWith("sets/");
    }

    /** A set's name: the lang key {@code trophypedestals.set.<name>}, or its name if it has none ("Nether Set"). */
    public static Component name(TagKey<EntityType<?>> set) {
        String name = set.location().getPath().substring("sets/".length());
        return Component.translatableWithFallback("trophypedestals.set." + name.replace('/', '.'), name + " set");
    }
}
