package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.feat.TrophyLedger;
import net.ixdarklord.trophypedestals.network.BuyBackTrophyPayload;
import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The Mob Trophies Ledger: every mob that can be a trophy, in the order of the mob trophies' creative tab, as a grid of their
 * icons (spawn eggs) on the pedestal screen's panel. A mob the reader has collected a trophy of shows in full, its name on
 * hover; one they haven't is a dark silhouette, "???". A gilded one has a gold corner. The header counts them.
 */
public final class TrophyLedgerScreen extends Screen {
    private static final Identifier PANEL = TrophyPedestalsMod.id("pedestal/panel");
    private static final int WIDTH = 248, MAX_HEIGHT = 236, MARGIN = 12, CELL = 22, COLUMNS = 9, GRID_TOP = 44, EDGE = 4;
    private static final int GOLD = 0xFFF2C94C, LABEL = 0xFF9DA2A7, GROOVE_DARK = 0xFF07090E, GROOVE_LIT = 0xFF343B4E;
    private static final int SLOT = 0xFF0E121A, SLOT_HOVER = 0xFF252C3C, SHADOW = 0xE0101418;
    // The scroll bar, as the adjust screen's: its width, how far in from the panel's right edge, its thumb's least
    // height; a groove cut like the panel's dividers, and a gold thumb lit along its top and left, with grip lines.
    private static final int BAR_WIDTH = 4, BAR_INSET = 7, MIN_THUMB = 14;
    private static final int GROOVE = 0xFF0E121A, PALE_GOLD = 0xFFFFE08A, GOLD_SHADE = 0xFFB8872B, GOLD_DEEP = 0xFF9A6A1A;
    private static final int GOLD_FRAME = 0xFFE0AC3C, GOLD_TOP = 0xFFFFF0A8, GOLD_LIT = 0xFFFFC24A;

    private final List<EntityType<?>> mobs = new ArrayList<>();
    private final List<ItemStack> icons = new ArrayList<>();
    private final Set<Identifier> collected, gilded;
    private int left, top, height2, scroll, maxScroll;
    // The mob picked to buy back (clicked once: a second click buys it), and whether its gilded trophy; -1 for none.
    private int picked = -1;
    private boolean pickedGilded;
    // While the scroll bar's thumb is held: where on it it was taken, down from its top (-1 while it isn't).
    private int dragOffset = -1;

    private TrophyLedgerScreen(List<Identifier> collected, List<Identifier> gilded) {
        super(Component.translatable("item.trophypedestals.trophy_ledger"));
        this.collected = Set.copyOf(collected);
        this.gilded = Set.copyOf(gilded);
        Minecraft minecraft = Minecraft.getInstance();
        for (EntityType<?> type : MobTrophies.collectable(minecraft.level != null ? minecraft.level.enabledFeatures() : FeatureFlags.DEFAULT_FLAGS)) {
            mobs.add(type);
            icons.add(new MobTrophyRecord(BuiltInRegistries.ENTITY_TYPE.getKey(type), "", 0L, PedestalRarities.NONE).icon());
        }
    }

    /** Opens the ledger with what the server says the reader has collected. */
    public static void open(List<Identifier> collected, List<Identifier> gilded) {
        Minecraft.getInstance().setScreen(new TrophyLedgerScreen(collected, gilded));
    }

    @Override
    protected void init() {
        height2 = Math.min(MAX_HEIGHT, height - 2 * EDGE);
        left = Math.max(EDGE, (width - WIDTH) / 2);
        top = Math.max(EDGE, (height - height2) / 2);
        int rows = (mobs.size() + COLUMNS - 1) / COLUMNS;
        maxScroll = Math.max(0, rows * CELL - (height2 - GRID_TOP - MARGIN));
        scroll = Mth.clamp(scroll, 0, maxScroll);
    }

    private int gridLeft() {
        return left + (WIDTH - COLUMNS * CELL) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, left, top, WIDTH, height2);
        long count = mobs.stream().filter(type -> collected.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type))).count();
        int percent = mobs.isEmpty() ? 0 : (int) (count * 100 / mobs.size());
        graphics.text(font, title, left + MARGIN, top + 12, GOLD);
        graphics.text(font, Component.translatable("trophypedestals.ledger.count", count, mobs.size(), percent), left + MARGIN, top + 23, LABEL);
        graphics.fill(left + MARGIN, top + 36, left + WIDTH - MARGIN, top + 37, GROOVE_DARK);
        graphics.fill(left + MARGIN, top + 37, left + WIDTH - MARGIN, top + 38, GROOVE_LIT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int x0 = gridLeft(), y0 = top + GRID_TOP, bottom = top + height2 - MARGIN;
        graphics.enableScissor(left, y0, left + WIDTH, bottom);
        List<Component> hoveredLines = null;
        for (int i = 0; i < mobs.size(); i++) {
            int x = x0 + i % COLUMNS * CELL, y = y0 + i / COLUMNS * CELL - scroll;
            if (y + CELL < y0 || y > bottom) continue;
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mobs.get(i));
            boolean has = collected.contains(id);
            boolean over = mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2 && mouseY >= y0 && mouseY < bottom;
            graphics.fill(x, y, x + CELL - 2, y + CELL - 2, over ? SLOT_HOVER : SLOT);
            graphics.item(icons.get(i), x + 2, y + 2);
            // Not collected: a dark silhouette.
            if (!has) graphics.fill(x + 2, y + 2, x + 18, y + 18, SHADOW);
            // Gilded: a gold corner.
            if (gilded.contains(id)) {
                graphics.fill(x + CELL - 6, y, x + CELL - 2, y + 1, GOLD);
                graphics.fill(x + CELL - 3, y, x + CELL - 2, y + 4, GOLD);
            }
            // Picked to buy back: a gold outline.
            if (i == picked) {
                graphics.fill(x - 1, y - 1, x + CELL - 1, y, GOLD);
                graphics.fill(x - 1, y + CELL - 2, x + CELL - 1, y + CELL - 1, GOLD);
                graphics.fill(x - 1, y, x, y + CELL - 2, GOLD);
                graphics.fill(x + CELL - 2, y, x + CELL - 1, y + CELL - 2, GOLD);
            }
            if (over) {
                hoveredLines = new ArrayList<>();
                hoveredLines.add(has ? (gilded.contains(id) ? Component.translatable("trophypedestals.pedestal.gilded", mobs.get(i).getDescription())
                        : mobs.get(i).getDescription()) : Component.translatable("trophypedestals.ledger.unknown"));
                if (has && buyBack()) buyBackLines(hoveredLines, i, gilded.contains(id));
            }
        }
        graphics.disableScissor();
        if (maxScroll > 0) scrollBar(graphics, mouseX, mouseY);
        if (hoveredLines != null) graphics.setTooltipForNextFrame(font, hoveredLines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
    }

    // The grid's top and bottom, where the scroll bar runs.
    private int viewTop() {
        return top + GRID_TOP;
    }

    private int viewBottom() {
        return top + height2 - MARGIN;
    }

    private int barX() {
        return left + WIDTH - BAR_INSET - BAR_WIDTH;
    }

    private int thumbHeight() {
        int track = viewBottom() - viewTop() - 2;
        return Math.max(MIN_THUMB, track * track / (track + maxScroll));
    }

    private int thumbY() {
        return viewTop() + 1 + (viewBottom() - viewTop() - 2 - thumbHeight()) * scroll / Math.max(1, maxScroll);
    }

    private boolean overBar(double mouseX, double mouseY) {
        return maxScroll > 0 && mouseX >= barX() - 1 && mouseX < barX() + BAR_WIDTH + 1 && mouseY >= viewTop() && mouseY < viewBottom();
    }

    // Scrolls so the thumb's top is where the mouse is, less where on the thumb it was taken.
    private void dragThumb(double mouseY) {
        int travel = viewBottom() - viewTop() - 2 - thumbHeight();
        scroll = travel <= 0 ? 0 : Mth.clamp((int) Math.round((mouseY - dragOffset - (viewTop() + 1)) * maxScroll / travel), 0, maxScroll);
    }

    // The groove (a dark rim, lit along its bottom and right) and the gold thumb in it, brighter while hovered or held.
    private void scrollBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = barX(), y0 = viewTop(), y1 = viewBottom();
        graphics.fill(x - 1, y0 - 1, x + BAR_WIDTH + 1, y1 + 1, GROOVE_LIT);
        graphics.fill(x - 1, y0 - 1, x + BAR_WIDTH, y1, GROOVE_DARK);
        graphics.fill(x, y0, x + BAR_WIDTH, y1, GROOVE);
        int ty = thumbY(), th = thumbHeight();
        boolean lit = dragOffset >= 0 || (mouseX >= x && mouseX < x + BAR_WIDTH && mouseY >= ty && mouseY < ty + th);
        graphics.fill(x, ty, x + BAR_WIDTH, ty + th, GOLD_DEEP);
        graphics.fill(x, ty, x + BAR_WIDTH - 1, ty + th - 1, lit ? GOLD_TOP : PALE_GOLD);
        graphics.fill(x + 1, ty + 1, x + BAR_WIDTH - 1, ty + th - 1, lit ? GOLD_LIT : GOLD_FRAME);
        int mid = ty + th / 2;
        for (int line = -2; line <= 2; line += 2) graphics.fill(x + 1, mid + line, x + BAR_WIDTH - 1, mid + line + 1, GOLD_SHADE);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragOffset >= 0) {
            dragThumb(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragOffset >= 0 && event.button() == 0) {
            dragOffset = -1;
            return true;
        }
        return super.mouseReleased(event);
    }

    // Whether the server lets trophies be bought back.
    private static boolean buyBack() {
        return TrophyPedestalsConfig.LEDGER_BUY_BACK.get();
    }

    // How to buy it back, and what it costs (red while the reader has too few levels); once picked, the click that buys it.
    private void buyBackLines(List<Component> lines, int index, boolean hasGilded) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean creative = minecraft.player != null && minecraft.player.isCreative();
        int levels = minecraft.player == null ? 0 : minecraft.player.experienceLevel;
        boolean gild = hasGilded && minecraft.hasShiftDown();
        int cost = TrophyLedger.buyBackCost(gild);
        boolean afford = creative || levels >= cost;
        lines.add(Component.translatable(index == picked && pickedGilded == gild ? "trophypedestals.ledger.buy_back.confirm" : "trophypedestals.ledger.buy_back.click",
                Component.translatable("trophypedestals.ledger.buy_back.cost", cost).withStyle(afford ? ChatFormatting.GREEN : ChatFormatting.RED))
                .withStyle(ChatFormatting.GRAY));
        if (hasGilded) lines.add(Component.translatable("trophypedestals.ledger.buy_back.gilded_hint", TrophyLedger.buyBackCost(true)).withStyle(ChatFormatting.DARK_GRAY));
    }

    // A collected mob clicked: picked to buy back, and bought on a second click (sneak-click for its gilded trophy).
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // The scroll bar: its thumb taken where it's clicked, or (clicked above or below it) moved there by its middle.
        if (event.button() == 0 && overBar(event.x(), event.y())) {
            int ty = thumbY(), th = thumbHeight();
            dragOffset = event.y() >= ty && event.y() < ty + th ? (int) event.y() - ty : th / 2;
            dragThumb(event.y());
            return true;
        }
        if (event.button() == 0 && buyBack()) {
            int x0 = gridLeft(), y0 = top + GRID_TOP, bottom = top + height2 - MARGIN;
            double mouseX = event.x(), mouseY = event.y();
            if (mouseX >= x0 && mouseX < x0 + COLUMNS * CELL && mouseY >= y0 && mouseY < bottom) {
                int column = (int) ((mouseX - x0) / CELL), row = (int) ((mouseY - y0 + scroll) / CELL), index = row * COLUMNS + column;
                if (index >= 0 && index < mobs.size()) {
                    Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mobs.get(index));
                    if (!collected.contains(id)) return true;
                    boolean gild = gilded.contains(id) && event.hasShiftDown();
                    if (index == picked && pickedGilded == gild) {
                        Network.sendToServer(new BuyBackTrophyPayload(id, gild));
                        picked = -1;
                    } else {
                        picked = index;
                        pickedGilded = gild;
                    }
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) (scrollY * CELL), 0, maxScroll);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
