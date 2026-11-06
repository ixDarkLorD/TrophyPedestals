package net.ixdarklord.trophypedestals.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.client.DrawnMobs;
import net.ixdarklord.trophypedestals.compat.component.Components;
import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The mob trophies: each mob, drawn standing still, and the trophy a player who slays it can
 * find, with its rank and the chance of it dropping ({@code mob_trophy_chance}). Its spawn egg finds the page too.
 */
public final class MobTrophyCategory implements IRecipeCategory<MobTrophyCategory.Drop> {
    public static final RecipeType<Drop> TYPE = new RecipeType<>(TrophyPedestalsMod.id("mob_trophy"), Drop.class);
    private static final int WIDTH = 120, HEIGHT = 48, SLOT = 18, LABEL = 0xFF404040;
    // The mob's box on the left (its feet on its floor); the arrow, the trophy's slot and the two lines of text right of it.
    private static final int MOB_WIDTH = 34, MOB_FIT = 38, TEXT_X = MOB_WIDTH + 4, ROW_Y = 6, RANK_Y = 28, CHANCE_Y = 38;
    // Where the mob looks, from its box's middle and top: to its right (towards its trophy), level with its head.
    private static final float LOOK_X = 24.0F, LOOK_Y = 12.0F;
    // One of each mob, made on the client to be drawn (null for one that can't be made outside a world).
    private static final Map<EntityType<?>, Optional<LivingEntity>> MOBS = new HashMap<>();

    private final IGuiHelper gui;
    private final IDrawable icon;

    public MobTrophyCategory(IGuiHelper gui) {
        this.gui = gui;
        this.icon = gui.createDrawableItemStack(new ItemStack(Pedestals.DEFAULT.get()));
    }

    /** A mob, its spawn egg (empty for none) and its trophy. */
    public record Drop(EntityType<?> type, ItemStack egg, ItemStack trophy, PedestalRarity rarity) {}

    /** One of each mob that drops a trophy, as the creative tab lists them (by rank, then by name). */
    public static List<Drop> drops() {
        List<Drop> drops = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        MobTrophies.creativeTrophies(level == null ? FeatureFlags.DEFAULT_FLAGS : level.enabledFeatures(), trophy -> {
            MobTrophyRecord record = Components.get(trophy, Pedestals.MOB_TROPHY.get());
            EntityType<?> type = record == null ? null : record.type();
            if (type == null) return;
            ItemStack egg = Optional.ofNullable(SpawnEggItem.byId(type)).map(ItemStack::new).orElse(ItemStack.EMPTY);
            drops.add(new Drop(type, egg, trophy, PedestalRarities.get(record.rarity())));
        });
        return drops;
    }

    @Override
    public RecipeType<Drop> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.trophypedestals.mob_trophy");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Drop drop, IFocusGroup focuses) {
        // (The mob is drawn, not a slot: its egg is there to find it by.)
        if (!drop.egg().isEmpty()) builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(drop.egg());
        builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - SLOT + 1, ROW_Y + 1).addItemStack(drop.trophy());
    }

    // The mob (its egg if it can't be drawn), the arrow to its trophy's slot, and under them its rank (in its color) and
    // the chance of it dropping.
    @Override
    public void draw(Drop drop, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        LivingEntity mob = mob(drop.type());
        if (mob != null) {
            // Scaled to fit its box, the larger of its height and width, so a dragon fits as a chicken does.
            int scale = Math.max(2, (int) (MOB_FIT / Math.max(mob.getBbHeight(), mob.getBbWidth() * 1.4F)));
            drawMob(graphics, mob, scale);
        } else if (!drop.egg().isEmpty()) {
            graphics.renderItem(drop.egg(), (MOB_WIDTH - 16) / 2, (HEIGHT - 16) / 2);
        }
        gui.getRecipeArrow().draw(graphics, TEXT_X + (WIDTH - SLOT - TEXT_X - 22) / 2, ROW_Y + 1);
        gui.getSlotDrawable().draw(graphics, WIDTH - SLOT, ROW_Y);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, drop.rarity().displayName(), TEXT_X, RANK_Y, 0xFF000000 | drop.rarity().color(), false);
        Component chance = Component.translatable("jei.trophypedestals.mob_trophy.chance", percent(TrophyPedestalsConfig.MOB_TROPHY_CHANCE.get()));
        graphics.drawString(font, chance, TEXT_X, CHANCE_Y, LABEL, false);
    }

    // The mob in its box, standing still, turned a little towards its trophy (as if looking at a point LOOK_X right of
    // its middle and LOOK_Y down from the top). (The game draws it at the screen's own coordinates, not the page's: the
    // page's place on the screen is added.)
    private static void drawMob(GuiGraphics graphics, LivingEntity mob, int scale) {
        // (Here it's drawn where the page is, from its feet, turned by where it looks from its head.)
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, MOB_WIDTH / 2, HEIGHT - 2, scale, -LOOK_X, HEIGHT / 2.0F - LOOK_Y, mob);
    }

    // The mob of this kind to draw, made once (in the world the client is in), or null for one that won't be made there.
    private static @Nullable LivingEntity mob(EntityType<?> type) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return null;
        Optional<LivingEntity> mob = MOBS.get(type);
        if (mob == null || mob.isPresent() && mob.get().level() != level) {
            Entity made;
            try {
                made = type.create(level);
            } catch (RuntimeException | LinkageError e) {
                made = null;
            }
            mob = Optional.ofNullable(made instanceof LivingEntity living ? DrawnMobs.numbered(living) : null);
            MOBS.put(type, mob);
        }
        return mob.orElse(null);
    }

    // The chance as a percentage, without trailing zeros (1%, 0.5%).
    private static String percent(double chance) {
        String text = String.format(Locale.ROOT, "%.2f", chance * 100.0);
        text = text.contains(".") ? text.replaceAll("0+$", "").replaceAll("\\.$", "") : text;
        return text + "%";
    }
}
