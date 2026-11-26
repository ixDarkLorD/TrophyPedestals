package net.ixdarklord.trophypedestals.mixin.client;

import net.ixdarklord.trophypedestals.pedestal.MobTrophySearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the mob trophies tab's title smaller, so it fits beside the tab's search box. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Shadow private static CreativeModeTab selectedTab;

    @Inject(method = "renderLabels", at = @At("TAIL"))
    private void trophypedestals$drawTitle(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!MobTrophySearch.searchable(selectedTab)) return;
        Font font = Minecraft.getInstance().font;
        float scale = Math.min(MobTrophySearch.TITLE_SCALE, (float) MobTrophySearch.TITLE_WIDTH / Math.max(1, font.width(selectedTab.getDisplayName())));
        graphics.pose().pushPose();
        graphics.pose().translate(8.0F, 6.0F + (font.lineHeight - 1) * (1.0F - scale) / 2.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, selectedTab.getDisplayName(), 0, 0, 0x404040, false);
        graphics.pose().popPose();
    }
}
