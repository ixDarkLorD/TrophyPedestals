package net.ixdarklord.trophypedestals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.ixdarklord.coolcatcore.api.client.registry.SpecialModelRendererRegistry;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * The custom pedestal item's body, drawn with its material's textures as the placed block is ({@link PedestalBody}): the
 * {@code trophypedestals:custom_pedestal} special item model, which the custom pedestal's item definition uses (with a trophy's
 * icon over it, for a trophy).
 */
public final class CustomPedestalItemRenderer implements SpecialModelRenderer<Identifier> {
    public static final Identifier TYPE = TrophyPedestalsMod.id("custom_pedestal");
    private static final CustomPedestalItemRenderer INSTANCE = new CustomPedestalItemRenderer();

    private CustomPedestalItemRenderer() {}

    public static void init() {
        SpecialModelRendererRegistry.register(TYPE, Unbaked.MAP_CODEC);
    }

    @Override
    public @Nullable Identifier extractArgument(ItemStack stack) {
        return stack.get(Pedestals.PEDESTAL_MATERIAL.get());
    }

    @Override
    public void submit(@Nullable Identifier material, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outlineColor) {
        PedestalBody.submit(material, poseStack, collector, light);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0.0F, 0.0F, 0.0F));
        output.accept(new Vector3f(1.0F, 1.0F, 1.0F));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Identifier> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<Identifier> bake(SpecialModelRenderer.BakingContext context) {
            return INSTANCE;
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
