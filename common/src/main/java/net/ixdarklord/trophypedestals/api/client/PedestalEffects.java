package net.ixdarklord.trophypedestals.api.client;

import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extra trophy pedestal effects, one for each rarity that has one; call {@link #register} from client setup. A rarity
 * registered with {@code PedestalRarities.register} already gets every effect its properties ask for; this is for
 * anything else.
 */
public final class PedestalEffects {
    private static final Map<Identifier, PedestalEffect> EFFECTS = new ConcurrentHashMap<>();

    private PedestalEffects() {}

    /**
     * Gives trophies of this rarity an extra effect.
     *
     * @throws IllegalStateException if the rarity already has one
     */
    public static void register(Identifier rarity, PedestalEffect effect) {
        if (EFFECTS.putIfAbsent(rarity, effect) != null) throw new IllegalStateException("Pedestal rarity " + rarity + " already has an effect");
    }

    /** The rarity's extra effect, or null if it has none. */
    public static @Nullable PedestalEffect of(PedestalRarity rarity) {
        return EFFECTS.get(rarity.id());
    }
}
