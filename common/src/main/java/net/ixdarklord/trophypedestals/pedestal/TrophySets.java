package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.BlockPos;
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
 * Sets of mob trophies: a trophy cabinet whose four spots all hold trophies of different mobs from one set (Nether,
 * End, Ocean, Bosses, Illagers, Undead, Arthropods, Farm Animals, Wildlife) shows the set's name over it in a gold
 * frame.
 */
public final class TrophySets {
    private TrophySets() {}

    public static @Nullable TagKey<EntityType<?>> complete(Level level, TrophyCabinetBlockEntity cabinet) {
        List<EntityType<?>> mobs = mobs(level, cabinet);
        if (mobs == null) return null;
        Optional<TagKey<EntityType<?>>> found = BuiltInRegistries.ENTITY_TYPE.getTagNames()
                .filter(TrophySets::isSet)
                .filter(set -> mobs.stream().allMatch(type -> type.builtInRegistryHolder().is(set)))
                .findFirst();
        return found.orElse(null);
    }

    public static @Nullable Component completedName(Level level, TrophyCabinetBlockEntity cabinet) {
        TagKey<EntityType<?>> set = complete(level, cabinet);
        if (set != null) return name(set);
        String mod = modSet(mobs(level, cabinet));
        return mod == null ? null : Component.translatable("trophypedestals.set.mod", Platform.getModName(mod).orElse(mod));
    }

    public static @Nullable String modSet(@Nullable List<EntityType<?>> mobs) {
        if (mobs == null || mobs.isEmpty()) return null;
        String mod = BuiltInRegistries.ENTITY_TYPE.getKey(mobs.getFirst()).getNamespace();
        if (mod.equals(ResourceLocation.DEFAULT_NAMESPACE)) return null;
        return mobs.stream().allMatch(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(mod)) ? mod : null;
    }

    private static @Nullable List<EntityType<?>> mobs(Level level, TrophyCabinetBlockEntity cabinet) {
        BlockPos other = TrophyCabinetBlock.partner(cabinet.getBlockPos(), cabinet.getBlockState());
        if (!(level.getBlockEntity(other) instanceof TrophyCabinetBlockEntity partner)) return null;
        List<EntityType<?>> mobs = new ArrayList<>();
        for (TrophyCabinetBlockEntity half : List.of(cabinet, partner)) {
            for (int i = 0; i < TrophyCabinetBlockEntity.SLOTS; i++) {
                ItemStack item = half.item(i);
                MobTrophyRecord record = item.get(Pedestals.MOB_TROPHY.get());
                EntityType<?> type = record == null ? null : record.type();
                if (type == null) return null;
                mobs.add(type);
            }
        }
        Set<EntityType<?>> distinct = new HashSet<>(mobs);
        return distinct.size() == mobs.size() ? mobs : null;
    }

    public static boolean isSet(TagKey<EntityType<?>> tag) {
        ResourceLocation id = tag.location();
        return id.getNamespace().equals(TrophyPedestalsMod.MOD_ID) && id.getPath().startsWith("sets/");
    }

    public static Component name(TagKey<EntityType<?>> set) {
        String name = set.location().getPath().substring("sets/".length());
        return Component.translatableWithFallback("trophypedestals.set." + name.replace('/', '.'), name + " set");
    }
}
