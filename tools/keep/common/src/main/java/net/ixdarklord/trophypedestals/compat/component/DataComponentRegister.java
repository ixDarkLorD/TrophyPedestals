package net.ixdarklord.trophypedestals.compat.component;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 1.20.1's stand-in for registering 26.1's data components (there's no registry for them): each is placed on items'
 * tags as it's made. Trophy Pedestals' own go in the item's {@code BlockEntityTag}, under the keys its block entities
 * save them by, so the game itself hands them to a placed pedestal and loot tables copy them back
 * ({@code minecraft:copy_nbt}).
 */
public final class DataComponentRegister {
    // A component's name, and its key in the block entity's saved data.
    private static final Map<String, String> BLOCK_ENTITY_KEYS = Map.of(
            "pedestal", "Record",
            "mob_trophy", "MobTrophy",
            "pedestal_material", "Material",
            "trim_material", "Trim");
    private final String namespace;

    private DataComponentRegister(String namespace) {
        this.namespace = namespace;
    }

    public static DataComponentRegister create(String namespace) {
        return new DataComponentRegister(namespace);
    }

    public <T> Supplier<DataComponentType<T>> register(String name, Supplier<DataComponentType<T>> factory) {
        DataComponentType<T> type = factory.get();
        String key = BLOCK_ENTITY_KEYS.get(name);
        if (key != null) type.at("BlockEntityTag", key);
        else type.at(namespace + ":" + name);
        Components.register(namespace + ":" + name, type);
        return () -> type;
    }

    /** (Nothing to register with the game.) */
    public void register() {}
}
