package net.ixdarklord.trophypedestals.compat.component;

import net.ixdarklord.trophypedestals.compat.ResolvableProfile;
import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.PedestalRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** 1.20.1's stand-ins for the vanilla components Trophy Pedestals uses, over what 1.20.1 items keep instead. */
public final class DataComponents {
    private static final String ITEM_NAME_KEY = "trophypedestals:item_name";

    private DataComponents() {}

    /**
     * An item's own name (not a custom one, so not in italics), as JSON on its tag. A trophy's is its record's ("Diamond
     * Age Trophy"), worked out again for one that lost it (a broken pedestal's drop carries only its record), as a
     * pedestal item's name is shown from it.
     */
    public static final DataComponentType<Component> ITEM_NAME = new DataComponentType<>(null) {
        @Override
        protected @Nullable Component get(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(ITEM_NAME_KEY, Tag.TAG_STRING)) {
                try {
                    Component name = Component.Serializer.fromJson(tag.getString(ITEM_NAME_KEY));
                    if (name != null) return name;
                } catch (RuntimeException ignored) {
                    // (A name that can't be read is worked out again.)
                }
            }
            return fromRecord(stack);
        }

        @Override
        protected void set(ItemStack stack, @Nullable Component value) {
            if (value == null) remove(stack);
            else stack.getOrCreateTag().putString(ITEM_NAME_KEY, Component.Serializer.toJson(value));
        }

        @Override
        protected void remove(ItemStack stack) {
            if (stack.getTag() != null) stack.getTag().remove(ITEM_NAME_KEY);
        }
    };

    /** A name given with an anvil or a name tag: the item's custom hover name. */
    public static final DataComponentType<Component> CUSTOM_NAME = new DataComponentType<>(null) {
        @Override
        protected @Nullable Component get(ItemStack stack) {
            return stack.hasCustomHoverName() ? stack.getHoverName() : null;
        }

        @Override
        protected void set(ItemStack stack, @Nullable Component value) {
            if (value == null) remove(stack);
            else stack.setHoverName(value);
        }

        @Override
        protected void remove(ItemStack stack) {
            stack.resetHoverName();
        }
    };

    /** A head's player: its {@code SkullOwner}. */
    public static final DataComponentType<ResolvableProfile> PROFILE = new DataComponentType<>(null) {
        @Override
        protected @Nullable ResolvableProfile get(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("SkullOwner", Tag.TAG_COMPOUND)) return null;
            var profile = NbtUtils.readGameProfile(tag.getCompound("SkullOwner"));
            return profile == null ? null : ResolvableProfile.createResolved(profile);
        }

        @Override
        protected void set(ItemStack stack, @Nullable ResolvableProfile value) {
            if (value == null) remove(stack);
            else stack.getOrCreateTag().put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), value.gameProfile()));
        }

        @Override
        protected void remove(ItemStack stack) {
            if (stack.getTag() != null) stack.getTag().remove("SkullOwner");
        }
    };

    // A trophy's name, made from its record as the mod names it when it makes the trophy.
    private static @Nullable Component fromRecord(ItemStack stack) {
        PedestalRecord record = Components.get(stack, Pedestals.RECORD.get());
        MobTrophyRecord mob = record == null ? Components.get(stack, Pedestals.MOB_TROPHY.get()) : null;
        if (record == null && mob == null) return null;
        ItemStack named = stack.copy();
        if (record != null) Pedestals.record(named, record);
        else MobTrophies.record(named, mob);
        CompoundTag tag = named.getTag();
        if (tag == null || !tag.contains(ITEM_NAME_KEY, Tag.TAG_STRING)) return null;
        return Component.Serializer.fromJson(tag.getString(ITEM_NAME_KEY));
    }
}
