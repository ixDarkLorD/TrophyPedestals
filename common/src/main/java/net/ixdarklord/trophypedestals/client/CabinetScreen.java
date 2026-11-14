package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.network.AdjustCabinetPayload;
import net.ixdarklord.trophypedestals.network.OpenCabinetPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A trophy cabinet's or shelf's settings, for its owner (opened with the Trophy Ledger, or sneaking on an empty spot):
 * who else may use it (Private, Team, Everyone) and when the name of the set its trophies make shows over it (Shown, On
 * Look, Hidden). Sent to the server as it closes. Drawn as the pedestal's adjust screen is.
 */
public final class CabinetScreen extends Screen {
    private static final Identifier PANEL = TrophyPedestalsMod.id("pedestal/panel");
    private static final Identifier SEGMENT = TrophyPedestalsMod.id("pedestal/segment");
    private static final Identifier SEGMENT_HOVER = TrophyPedestalsMod.id("pedestal/segment_hover");
    private static final Identifier SEGMENT_SELECTED = TrophyPedestalsMod.id("pedestal/segment_selected");
    private static final Identifier INFO = TrophyPedestalsMod.id("pedestal/info");
    private static final Identifier INFO_HOVER = TrophyPedestalsMod.id("pedestal/info_hover");
    // Its size; the margin inside its frame; where its two sections' labels are, down from its top; how wide tooltips wrap.
    private static final int WIDTH = 220, HEIGHT = 150, MARGIN = 12, ROW_1 = 46, ROW_2 = 84, INFO_SIZE = 9, TOOLTIP_WIDTH = 180, EDGE = 4;
    private static final int GOLD = 0xFFF2C94C, PALE_GOLD = 0xFFFFE08A, LABEL = 0xFF9DA2A7, TEXT = 0xFFDCDCE0;
    private static final int GROOVE_DARK = 0xFF07090E, GROOVE_LIT = 0xFF343B4E;
    private static final List<String> ACCESS = List.of("private", "team", "everyone"), SET_NAMES = List.of("shown", "on_look", "hidden");

    private final BlockPos pos;
    private String access, setName;
    private int left, top;

    private CabinetScreen(OpenCabinetPayload payload) {
        super(Component.translatable("trophypedestals.cabinet.screen.title"));
        this.pos = payload.pos();
        this.access = payload.access();
        this.setName = payload.setName();
    }

    public static void open(OpenCabinetPayload payload) {
        Minecraft.getInstance().gui.setScreen(new CabinetScreen(payload));
    }

    @Override
    protected void init() {
        left = Math.max(EDGE, (width - WIDTH) / 2);
        top = Math.max(EDGE, (height - HEIGHT) / 2);
        int x = left + MARGIN, column = WIDTH - 2 * MARGIN, third = (column - 4) / 3;
        // Who else may use it, then when its set name shows: three choices each, across the column.
        for (int i = 0; i < 3; i++) {
            String each = ACCESS.get(i);
            addRenderableWidget(new Choice(x + i * (third + 2), top + ROW_1 + 12, i == 2 ? column - 2 * (third + 2) : third,
                    Component.translatable("trophypedestals.pedestal.screen.access." + each), () -> access.equals(each), () -> access = each));
            String name = SET_NAMES.get(i);
            addRenderableWidget(new Choice(x + i * (third + 2), top + ROW_2 + 12, i == 2 ? column - 2 * (third + 2) : third,
                    Component.translatable("trophypedestals.cabinet.screen.set_name." + name), () -> setName.equals(name), () -> setName = name));
        }
        addRenderableWidget(new Choice(x, top + HEIGHT - 28, column, CommonComponents.GUI_DONE, () -> false, this::onClose));
    }

    @Override
    public void removed() {
        Network.sendToServer(new AdjustCabinetPayload(pos, access, setName));
    }

    // The panel, its title over a groove, and its two sections' labels, each with a "?" explaining it while hovered.
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, left, top, WIDTH, HEIGHT);
        graphics.text(font, title, left + MARGIN, top + 14, GOLD);
        graphics.fill(left + MARGIN, top + 32, left + WIDTH - MARGIN, top + 33, GROOVE_DARK);
        graphics.fill(left + MARGIN, top + 33, left + WIDTH - MARGIN, top + 34, GROOVE_LIT);
        label(graphics, "trophypedestals.cabinet.screen.access", ROW_1, mouseX, mouseY);
        label(graphics, "trophypedestals.cabinet.screen.set_name", ROW_2, mouseX, mouseY);
    }

    private void label(GuiGraphicsExtractor graphics, String key, int row, int mouseX, int mouseY) {
        Component text = Component.translatable(key).withStyle(ChatFormatting.BOLD);
        int x = left + MARGIN, y = top + row;
        graphics.text(font, text, x, y, LABEL);
        int iconX = x + font.width(text) + 3, iconY = y - 1;
        boolean hovered = mouseX >= iconX && mouseX < iconX + INFO_SIZE && mouseY >= iconY && mouseY < iconY + INFO_SIZE;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? INFO_HOVER : INFO, iconX, iconY, INFO_SIZE, INFO_SIZE);
        if (hovered) graphics.setTooltipForNextFrame(font.split(Component.translatable(key + ".tooltip"), TOOLTIP_WIDTH), mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** A choice: a slate button, gold-trimmed while chosen. */
    private final class Choice extends AbstractButton {
        private final BooleanSupplier selected;
        private final Runnable action;

        Choice(int x, int y, int width, Component label, BooleanSupplier selected, Runnable action) {
            super(x, y, width, 18, label);
            this.selected = selected;
            this.action = action;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            action.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            boolean chosen = selected.getAsBoolean();
            Identifier sprite = chosen ? SEGMENT_SELECTED : isHoveredOrFocused() ? SEGMENT_HOVER : SEGMENT;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), width, height);
            int color = chosen ? PALE_GOLD : isHoveredOrFocused() ? 0xFFFFFFFF : TEXT;
            graphics.text(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + (height - 8) / 2, color);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
