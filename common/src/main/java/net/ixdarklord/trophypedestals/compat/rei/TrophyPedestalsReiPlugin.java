package net.ixdarklord.trophypedestals.compat.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * REI's "Mob Trophies" pages, as JEI's: each mob that can be a trophy (its spawn egg), the trophy a player who slays it
 * can drop, its rank and the chance. Fabric loads it through the {@code rei_client} entrypoint; on NeoForge, an annotated
 * subclass in the neoforge module does.
 */
public class TrophyPedestalsReiPlugin implements REIClientPlugin {
    public static final CategoryIdentifier<Drop> MOB_TROPHIES = CategoryIdentifier.of(TrophyPedestalsMod.id("mob_trophies"));

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new Category());
        registry.addWorkstations(MOB_TROPHIES, EntryStack.of(VanillaEntryTypes.ITEM, new ItemStack(Pedestals.DEFAULT.get())));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        Minecraft minecraft = Minecraft.getInstance();
        for (EntityType<?> type : MobTrophies.collectable(minecraft.level != null ? minecraft.level.enabledFeatures() : FeatureFlags.DEFAULT_FLAGS)) {
            PedestalRarity rarity = MobTrophies.rarity(type);
            if (rarity == null) continue;
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            ItemStack egg = new MobTrophyRecord(id, "", 0L, rarity.id()).icon();
            ItemStack trophy = MobTrophies.trophy(Pedestals.DEFAULT.get(), type, "", 0L, rarity);
            registry.add(new Drop(egg, trophy, rarity));
        }
    }

    /** A mob's page: its egg, the trophy it drops, and that trophy's rank. */
    public record Drop(ItemStack egg, ItemStack trophy, PedestalRarity rarity) implements Display {
        @Override
        public List<EntryIngredient> getInputEntries() {
            return List.of(EntryIngredient.of(EntryStack.of(VanillaEntryTypes.ITEM, egg)));
        }

        @Override
        public List<EntryIngredient> getOutputEntries() {
            return List.of(EntryIngredient.of(EntryStack.of(VanillaEntryTypes.ITEM, trophy)));
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return MOB_TROPHIES;
        }

        @Override
        public Optional<ResourceLocation> getDisplayLocation() {
            return Optional.empty();
        }
    }

    private static final class Category implements DisplayCategory<Drop> {
        @Override
        public CategoryIdentifier<? extends Drop> getCategoryIdentifier() {
            return MOB_TROPHIES;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.trophypedestals.mob_trophy");
        }

        @Override
        public Renderer getIcon() {
            return EntryStack.of(VanillaEntryTypes.ITEM, new ItemStack(Pedestals.DEFAULT.get()));
        }

        @Override
        public int getDisplayHeight() {
            return 48;
        }

        @Override
        public List<Widget> setupDisplay(Drop drop, Rectangle bounds) {
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            int x = bounds.x + 10, y = bounds.y + 8;
            widgets.add(Widgets.createSlot(new Point(x, y)).entries(drop.getInputEntries().getFirst()).markInput());
            widgets.add(Widgets.createArrow(new Point(x + 30, y)));
            widgets.add(Widgets.createSlot(new Point(x + 64, y)).entries(drop.getOutputEntries().getFirst()).markOutput());
            widgets.add(Widgets.createLabel(new Point(x, y + 22), drop.rarity().displayName().copy().withColor(drop.rarity().color())).leftAligned().noShadow());
            double chance = TrophyPedestalsConfig.MOB_TROPHY_CHANCE.get() * 100.0;
            widgets.add(Widgets.createLabel(new Point(x + 70, y + 22), Component.translatable("jei.trophypedestals.mob_trophy.chance",
                    String.format(Locale.ROOT, chance < 1.0 ? "%.2f" : "%.0f", chance))).leftAligned().noShadow().color(0xFF404040));
            return widgets;
        }
    }
}
