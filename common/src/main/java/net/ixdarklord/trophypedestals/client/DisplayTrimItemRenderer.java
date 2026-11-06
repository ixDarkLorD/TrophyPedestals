package net.ixdarklord.trophypedestals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.compat.render.SpecialModelRenderer;
import net.ixdarklord.trophypedestals.compat.render.SpecialModelRendererRegistry;
import net.ixdarklord.trophypedestals.compat.render.SubmitNodeCollector;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * A trimmed trophy cabinet's or shelf's trim on its item, drawn in its material as the placed block's is
 * ({@link PedestalBody#submitTrim}): the {@code trophypedestals:display_trim} special item model, over the bare model in the
 * display's item definition. Its {@code model} is the model file whose gold faces it draws.
 */
public final class DisplayTrimItemRenderer implements SpecialModelRenderer<ResourceLocation> {
    public static final ResourceLocation TYPE = TrophyPedestalsMod.id("display_trim");
    private final ResourceLocation model;

    private DisplayTrimItemRenderer(ResourceLocation model) {
        this.model = model;
    }

    public static void init() {
        SpecialModelRendererRegistry.register(TYPE, Unbaked.MAP_CODEC);
    }

    @Override
    public @Nullable ResourceLocation extractArgument(ItemStack stack) {
        return stack.get(Pedestals.TRIM_MATERIAL.get());
    }

    @Override
    public void submit(@Nullable ResourceLocation material, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outlineColor) {
        if (material != null) PedestalBody.submitTrim(model, material, poseStack, collector, light);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0.0F, 0.0F, 0.0F));
        output.accept(new Vector3f(1.0F, 1.0F, 1.0F));
    }

    /** @param model the model file whose gold faces are the trim, as {@code trophypedestals:models/block/trophy_shelf.json} */
    public record Unbaked(ResourceLocation model) implements SpecialModelRenderer.Unbaked<ResourceLocation> {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("model").forGetter(Unbaked::model)
        ).apply(instance, Unbaked::new));

        @Override
        public SpecialModelRenderer<ResourceLocation> bake(SpecialModelRenderer.BakingContext context) {
            return new DisplayTrimItemRenderer(model);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
