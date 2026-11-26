package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.compat.GuiSprites;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Asks a player, as they join a world, whether to turn on the pedestals' coloured lighting, which is off until they do:
 * Turn On does, Not Now leaves it off until they next join, and either with "Don't show this again" ticked stops asking.
 * Drawn in the pedestal screen's navy and gold, on a panel of its own: cut corners, a dotted inner line, diamond studs
 * and a lens set into its top edge, glowing.
 */
public final class LightingPromptScreen extends Screen {
    private static final ResourceLocation PANEL = TrophyPedestalsMod.id("lighting_prompt/panel");
    private static final ResourceLocation LENS = TrophyPedestalsMod.id("lighting_prompt/lens");
    private static final ResourceLocation CHECKBOX = TrophyPedestalsMod.id("lighting_prompt/checkbox");
    private static final ResourceLocation CHECKBOX_HOVER = TrophyPedestalsMod.id("lighting_prompt/checkbox_hover");
    private static final ResourceLocation CHECKBOX_CHECKED = TrophyPedestalsMod.id("lighting_prompt/checkbox_checked");
    private static final ResourceLocation CHECKBOX_CHECKED_HOVER = TrophyPedestalsMod.id("lighting_prompt/checkbox_checked_hover");
    private static final ResourceLocation SEGMENT = TrophyPedestalsMod.id("pedestal/segment");
    private static final ResourceLocation SEGMENT_HOVER = TrophyPedestalsMod.id("pedestal/segment_hover");
    private static final ResourceLocation SEGMENT_SELECTED = TrophyPedestalsMod.id("pedestal/segment_selected");

    private static final int WIDTH = 240, PADDING = 16, LENS_SIZE = 22, LINE = 10, BUTTON_HEIGHT = 18, CHECK = 11;
    private static final int GOLD = 0xFFF2C94C, PALE_GOLD = 0xFFFFE08A, TEXT = 0xFFDCDCE0, LABEL = 0xFF9DA2A7;
    private static final int GROOVE_DARK = 0xFF07090E, GROOVE_LIT = 0xFF2A3344;
    /** The warning's amber, and the gap above it. */
    private static final int WARNING = 0xFFFFB347, WARNING_GAP = 6;
    /** The warm light falling from the lens over the top of the panel, at its strongest and gone. */
    private static final int GLOW_TOP = 0x30FFC870, GLOW_BOTTOM = 0x00FFC870;

    private final Component body = Component.translatable("trophypedestals.lighting_prompt.body");
    private final Component warning = Component.translatable("trophypedestals.lighting_prompt.warning");
    private List<FormattedCharSequence> lines = List.of(), warningLines = List.of();
    private boolean dontShowAgain;
    private int left, top, height;

    public LightingPromptScreen() {
        super(Component.translatable("trophypedestals.lighting_prompt.title"));
    }

    @Override
    protected void init() {
        int textWidth = WIDTH - 2 * PADDING;
        lines = font.split(body, textWidth);
        warningLines = font.split(warning, textWidth);
        height = PADDING + 14 + 8 + lines.size() * LINE + WARNING_GAP + warningLines.size() * LINE + 10 + CHECK + 10 + BUTTON_HEIGHT + PADDING;
        left = (this.width - WIDTH) / 2;
        top = Math.max(LENS_SIZE / 2 + 4, (this.height - height) / 2);
        int checkY = top + height - PADDING - BUTTON_HEIGHT - 10 - CHECK;
        addRenderableWidget(new Checkbox(left + PADDING, checkY, textWidth));
        int half = (textWidth - 6) / 2, buttonY = top + height - PADDING - BUTTON_HEIGHT;
        addRenderableWidget(new PromptButton(left + PADDING, buttonY, half, Component.translatable("trophypedestals.lighting_prompt.enable"), true, this::enable));
        addRenderableWidget(new PromptButton(left + PADDING + half + 6, buttonY, textWidth - half - 6,
                Component.translatable("trophypedestals.lighting_prompt.not_now"), false, this::onClose));
    }

    /** Turns the lighting on, and stops asking if the box is ticked (there's nothing left to ask once it's on anyway). */
    private void enable() {
        TrophyPedestalsClientConfig.PEDESTAL_LIGHTING.set(true);
        remember();
        super.onClose();
    }

    /** Not Now, or Escape: leaves the lighting off, and stops asking if the box is ticked. */
    @Override
    public void onClose() {
        remember();
        super.onClose();
    }

    private void remember() {
        if (dontShowAgain) TrophyPedestalsClientConfig.LIGHTING_PROMPT.set(false);
        TrophyPedestalsClientConfig.CONFIG.save();
    }

    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics);
        GuiSprites.blit(graphics, PANEL, left, top, WIDTH, height);
        graphics.fillGradient(left + 8, top + 8, left + WIDTH - 8, top + 40, GLOW_TOP, GLOW_BOTTOM);
        GuiSprites.blit(graphics, LENS, left + (WIDTH - LENS_SIZE) / 2, top - LENS_SIZE / 2, LENS_SIZE, LENS_SIZE);
        int titleY = top + PADDING + 4;
        graphics.drawCenteredString(font, title, left + WIDTH / 2, titleY, GOLD);
        int grooveY = titleY + 13;
        graphics.fill(left + PADDING, grooveY, left + WIDTH - PADDING, grooveY + 1, GROOVE_DARK);
        graphics.fill(left + PADDING, grooveY + 1, left + WIDTH - PADDING, grooveY + 2, GROOVE_LIT);
        int y = grooveY + 8;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, left + (WIDTH - font.width(line)) / 2, y, TEXT);
            y += LINE;
        }
        // that it's still experimental, in amber
        y += WARNING_GAP;
        for (FormattedCharSequence line : warningLines) {
            graphics.drawString(font, line, left + (WIDTH - font.width(line)) / 2, y, WARNING);
            y += LINE;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    /** A button in the pedestal screen's style: Turn On golden, as a chosen option is there, Not Now plain. */
    private final class PromptButton extends AbstractButton {
        private final boolean primary;
        private final Runnable action;

        PromptButton(int x, int y, int width, Component label, boolean primary, Runnable action) {
            super(x, y, width, BUTTON_HEIGHT, label);
            this.primary = primary;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean lit = isHoveredOrFocused();
            ResourceLocation sprite = primary ? SEGMENT_SELECTED : lit ? SEGMENT_HOVER : SEGMENT;
            GuiSprites.blit(graphics, sprite, getX(), getY(), width, height);
            if (primary && lit) graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0x20FFE08A);
            int color = primary ? PALE_GOLD : lit ? 0xFFFFFFFF : TEXT;
            graphics.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + (height - 8) / 2, color);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** "Don't show this again": a gold-framed box, ticked or not, and its label beside it. */
    private final class Checkbox extends AbstractButton {
        Checkbox(int x, int y, int width) {
            super(x, y, width, CHECK, Component.translatable("trophypedestals.lighting_prompt.dont_show"));
        }

        @Override
        public void onPress() {
            dontShowAgain = !dontShowAgain;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean lit = isHoveredOrFocused();
            ResourceLocation sprite = dontShowAgain ? lit ? CHECKBOX_CHECKED_HOVER : CHECKBOX_CHECKED : lit ? CHECKBOX_HOVER : CHECKBOX;
            GuiSprites.blit(graphics, sprite, getX(), getY(), CHECK, CHECK);
            graphics.drawString(font, getMessage(), getX() + CHECK + 5, getY() + (CHECK - 8) / 2, lit ? TEXT : LABEL);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
            output.add(NarratedElementType.HINT, Component.translatable(dontShowAgain ? "options.on" : "options.off"));
        }
    }
}
