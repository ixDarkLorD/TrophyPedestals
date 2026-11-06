package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FormattedCharSequence;

/** 1.20.1's stand-in for 26.1's {@code OrderedSubmitNodeCollector} (see {@link SubmitNodeCollector}). */
public interface OrderedSubmitNodeCollector {
    void submitCustomGeometry(PoseStack poseStack, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer);

    void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode mode,
                    int light, int color, int background, int outline);
}
