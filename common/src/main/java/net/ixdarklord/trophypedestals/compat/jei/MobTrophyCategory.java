package net.ixdarklord.trophypedestals.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.client.DrawnMobs;
import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MobTrophyCategory implements IRecipeCategory<MobTrophyCategory.Drop> {
    public static final IRecipeType<Drop> TYPE = IRecipeType.create(TrophyPedestalsMod.id("mob_trophy"), Drop.class);
    private static final int WIDTH = 120, HEIGHT = 48, SLOT = 18, LABEL = 0xFF404040;
    private static final int MOB_WIDTH = 34, MOB_FIT = 38, TEXT_X = MOB_WIDTH + 4, ROW_Y = 6, RANK_Y = 28, CHANCE_Y = 38;
    private static final float VIEW_PITCH = 30.0F * Mth.DEG_TO_RAD, VIEW_BODY = 225.0F, MOB_MARGIN = 0.9F;
    private static final int COMMON_TEXT = 0xFF4E5560;
    private static final Map<EntityType<?>, Optional<LivingEntity>> MOBS = new HashMap<>();

    private final IGuiHelper gui;
    private final IDrawable icon;

    public MobTrophyCategory(IGuiHelper gui) {
        this.gui = gui;
        this.icon = gui.createDrawableItemStack(new ItemStack(Pedestals.DEFAULT.get()));
    }

    public record Drop(EntityType<?> type, ItemStack egg, ItemStack trophy, PedestalRarity rarity) {}

    public static List<Drop> drops() {
        List<Drop> drops = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        MobTrophies.creativeTrophies(level == null ? FeatureFlags.DEFAULT_FLAGS : level.enabledFeatures(), trophy -> {
            MobTrophyRecord record = trophy.get(Pedestals.MOB_TROPHY.get());
            EntityType<?> type = record == null ? null : record.type();
            if (type == null) return;
            ItemStack egg = SpawnEggItem.byId(type).map(item -> new ItemStack(item.value())).orElse(ItemStack.EMPTY);
            drops.add(new Drop(type, egg, trophy, PedestalRarities.get(record.rarity())));
        });
        return drops;
    }

    @Override
    public IRecipeType<Drop> getRecipeType() {
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
        if (!drop.egg().isEmpty()) builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).add(drop.egg());
        builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - SLOT + 1, ROW_Y + 1).add(drop.trophy());
    }

    @Override
    public void draw(Drop drop, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        LivingEntity mob = mob(drop.type());
        if (mob != null) {
            drawMob(graphics, mob, isometricScale(mob));
        } else if (!drop.egg().isEmpty()) {
            graphics.item(drop.egg(), (MOB_WIDTH - 16) / 2, (HEIGHT - 16) / 2);
        }
        gui.getRecipeArrow().draw(graphics, TEXT_X + (WIDTH - SLOT - TEXT_X - 22) / 2, ROW_Y + 1);
        gui.getSlotDrawable().draw(graphics, WIDTH - SLOT, ROW_Y);
        Font font = Minecraft.getInstance().font;
        int rankColor = drop.rarity() == PedestalRarities.COMMON ? COMMON_TEXT : 0xFF000000 | drop.rarity().color();
        graphics.text(font, drop.rarity().displayName(), TEXT_X, RANK_Y, rankColor, false);
        Component chance = Component.translatable("jei.trophypedestals.mob_trophy.chance", MobTrophies.chanceText(drop.type()));
        graphics.text(font, chance, TEXT_X, CHANCE_Y, LABEL, false);
    }

    private static int isometricScale(LivingEntity mob) {
        float across = mob.getBbWidth() * Mth.SQRT_OF_TWO;
        float wide = MOB_WIDTH / Math.max(across, 0.1F);
        float tall = MOB_FIT / Math.max(mob.getBbHeight() * Mth.cos(VIEW_PITCH) + across * Mth.sin(VIEW_PITCH), 0.1F);
        return Math.max(2, (int) (Math.min(wide, tall) * MOB_MARGIN));
    }

    private static void drawMob(GuiGraphicsExtractor graphics, LivingEntity mob, int scale) {
        Vector2f at = graphics.pose().transformPosition(0.0F, 0.0F, new Vector2f());
        int x = Math.round(at.x()), y = Math.round(at.y());
        EntityRenderState state = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(mob).createRenderState(mob, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = VIEW_BODY;
            living.yRot = 0.0F;
            living.xRot = 0.0F;
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1.0F;
        }
        Quaternionf tip = new Quaternionf().rotateX(VIEW_PITCH);
        Quaternionf rotation = new Quaternionf().rotateZ(Mth.PI).mul(tip);
        graphics.entity(state, scale, new Vector3f(0.0F, state.boundingBoxHeight / 2.0F, 0.0F), rotation, tip, x, y, x + MOB_WIDTH, y + HEIGHT);
    }

    private static @Nullable LivingEntity mob(EntityType<?> type) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return null;
        Optional<LivingEntity> mob = MOBS.get(type);
        if (mob == null || mob.isPresent() && mob.get().level() != level) {
            Entity made;
            try {
                made = type.create(level, EntitySpawnReason.LOAD);
            } catch (RuntimeException | LinkageError e) {
                made = null;
            }
            mob = Optional.ofNullable(made instanceof LivingEntity living ? DrawnMobs.numbered(living) : null);
            MOBS.put(type, mob);
        }
        return mob.orElse(null);
    }
}
