package net.ixdarklord.trophypedestals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.ixdarklord.coolcatcanvas.api.client.registry.ItemRendererRegistry;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.Lens;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public final class PedestalLensItemRenderer implements SpecialModelRenderer<PedestalLensItemRenderer.Fitted> {
    public static final Identifier TYPE = TrophyPedestalsMod.id("pedestal_lens");
    private static final PedestalLensItemRenderer INSTANCE = new PedestalLensItemRenderer();
    private static final Fitted CLEAR = new Fitted(Lens.CLEAR, 0xFFFFFFFF);

    private PedestalLensItemRenderer() {}

    public static void init() {
        ItemRendererRegistry.register(TYPE, Unbaked.MAP_CODEC);
    }

    public record Fitted(Lens lens, int tint) {}

    @Override
    public @Nullable Fitted extractArgument(ItemStack stack) {
        ItemContainerContents contents = stack.get(Pedestals.LENS.get());
        if (contents == null) return CLEAR;
        ItemStack fitted = contents.copyOne();
        Lens lens = Lens.of(fitted);
        if (lens == null) return null;
        return new Fitted(lens, lens == Lens.CLEAR ? ARGB.opaque(Lens.dyed(fitted)) : 0xFFFFFFFF);
    }

    @Override
    public void submit(@Nullable Fitted fitted, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outlineColor) {
        if (fitted != null) PedestalRenderer.submitItemLens(poseStack, collector, fitted.lens(), fitted.tint(), light);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0.0F, 0.0F, 0.0F));
        output.accept(new Vector3f(1.0F, 1.0F, 1.0F));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Fitted> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<Fitted> bake(SpecialModelRenderer.BakingContext context) {
            return INSTANCE;
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
