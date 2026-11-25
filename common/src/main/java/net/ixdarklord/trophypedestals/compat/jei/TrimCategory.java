package net.ixdarklord.trophypedestals.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.ixdarklord.trophypedestals.pedestal.TrophyDisplayBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class TrimCategory implements IRecipeCategory<TrimCategory.Trim> {
    public static final RecipeType<Trim> TYPE = new RecipeType<>(TrophyPedestalsMod.id("trim"), Trim.class);
    private static final int WIDTH = 120, HEIGHT = 34, SLOT = 18, TEXT_Y = 24, LABEL = 0xFF404040;
    private static final List<Item> MATERIALS = List.of(Items.GOLD_INGOT, Items.STONE, Items.COBBLESTONE, Items.DEEPSLATE_BRICKS,
            Items.POLISHED_BLACKSTONE, Items.QUARTZ_BLOCK, Items.SMOOTH_SANDSTONE, Items.BRICKS, Items.PRISMARINE, Items.PURPUR_BLOCK,
            Items.END_STONE_BRICKS, Items.COPPER_BLOCK, Items.EMERALD_BLOCK, Items.DIAMOND_BLOCK, Items.AMETHYST_BLOCK, Items.OAK_LOG);

    private final IGuiHelper gui;
    private final IDrawable icon;

    public TrimCategory(IGuiHelper gui) {
        this.gui = gui;
        ItemStack shelf = new ItemStack(Pedestals.SHELVES.values().iterator().next().get());
        shelf.set(Pedestals.TRIM_MATERIAL.get(), TrophyDisplayBlock.GOLD);
        this.icon = gui.createDrawableItemStack(shelf);
    }

    public record Trim(ItemStack display, List<ItemStack> materials, List<ItemStack> trimmed) {}

    public static List<Trim> trims() {
        List<Trim> trims = new ArrayList<>();
        for (var item : Pedestals.DISPLAY_ITEMS) {
            ItemStack display = new ItemStack(item.get());
            List<ItemStack> materials = new ArrayList<>(), trimmed = new ArrayList<>();
            for (Item material : MATERIALS) {
                ResourceLocation trim = material == Items.GOLD_INGOT ? TrophyDisplayBlock.GOLD
                        : material instanceof BlockItem block && TrophyDisplayBlock.canTrim(block.getBlock()) ? BuiltInRegistries.BLOCK.getKey(block.getBlock()) : null;
                if (trim == null) continue;
                ItemStack result = display.copy();
                result.set(Pedestals.TRIM_MATERIAL.get(), trim);
                materials.add(new ItemStack(material));
                trimmed.add(result);
            }
            trims.add(new Trim(display, materials, trimmed));
        }
        return trims;
    }

    @Override
    public RecipeType<Trim> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.trophypedestals.trim");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Trim trim, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).addItemStack(trim.display());
        IRecipeSlotBuilder material = builder.addSlot(RecipeIngredientRole.INPUT, MATERIAL_X + 1, 1).addItemStacks(trim.materials());
        IRecipeSlotBuilder result = builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - SLOT + 1, 1).addItemStacks(trim.trimmed());
        builder.createFocusLink(material, result);
    }

    private static final int PLUS_X = SLOT + 3, MATERIAL_X = PLUS_X + 13 + 3, ARROW_X = MATERIAL_X + SLOT + (WIDTH - SLOT - MATERIAL_X - SLOT - 22) / 2;

    @Override
    public void draw(Trim trim, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        gui.getSlotDrawable().draw(graphics, 0, 0);
        gui.getRecipePlusSign().draw(graphics, PLUS_X, 3);
        gui.getSlotDrawable().draw(graphics, MATERIAL_X, 0);
        gui.getRecipeArrow().draw(graphics, ARROW_X, 1);
        gui.getSlotDrawable().draw(graphics, WIDTH - SLOT, 0);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("jei.trophypedestals.trim.remove"), 0, TEXT_Y, LABEL, false);
    }
}
