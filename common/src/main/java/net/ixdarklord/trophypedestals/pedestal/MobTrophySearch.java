package net.ixdarklord.trophypedestals.pedestal;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * The mob trophies tab's search bar. NeoForge turns on its builder's own search bar (set from the loader module); Fabric has
 * none, so a screen mixin shows the box on this tab and filters it with {@link #filter}.
 */
public final class MobTrophySearch {
    /** The search box's width; it sits at the right end of the title row, beside the tab's title. */
    public static final int SEARCH_WIDTH = 71;
    /** How wide the tab's title may be drawn: from x 8 to just before the box, which starts at x 98. */
    public static final int TITLE_WIDTH = 86;
    /** The scale the title is drawn at, so the box has room; a longer title (another language) is shrunk to fit. */
    public static final float TITLE_SCALE = 0.75F;
    /**
     * The search tab's background with a shorter text box, so the title fits to its left. Fabric's 1.20.1 screen only reads
     * a file name under minecraft's creative inventory textures, so it lives there with the mod's name in it.
     */
    private static final String BACKGROUND = "trophypedestals_mob_trophies.png";
    private static UnaryOperator<CreativeModeTab.Builder> searchBar = UnaryOperator.identity();

    private MobTrophySearch() {}

    /** Sets how the loader gives a tab its search bar. Call it before the tabs are registered. */
    public static void searchBar(UnaryOperator<CreativeModeTab.Builder> searchBar) {
        MobTrophySearch.searchBar = searchBar;
    }

    /** The vanilla title is hidden: the screen mixin draws it smaller beside the box. */
    static CreativeModeTab.Builder withSearchBar(CreativeModeTab.Builder builder) {
        return searchBar.apply(builder.backgroundSuffix(BACKGROUND).hideTitle());
    }

    public static boolean searchable(CreativeModeTab tab) {
        return tab == Pedestals.MOB_TROPHIES_TAB.get();
    }

    /** The trophies whose name (for example "Zombie Trophy") holds {@code text}, ignoring case; all of them when it's blank. */
    public static List<ItemStack> filter(Collection<ItemStack> items, String text) {
        String query = text.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return List.copyOf(items);
        return items.stream().filter(stack -> stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)).toList();
    }
}
