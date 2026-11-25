package net.ixdarklord.trophypedestals.compat.component;

import java.util.Map;
import java.util.function.Supplier;

public final class DataComponentRegister {
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

    public void register() {}
}
