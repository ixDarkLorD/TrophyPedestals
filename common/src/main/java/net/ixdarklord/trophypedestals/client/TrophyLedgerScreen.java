package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.feat.TrophyLedger;
import net.ixdarklord.trophypedestals.network.BuyBackTrophyPayload;
import net.ixdarklord.trophypedestals.network.OpenLedgerPayload;
import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The Mob Trophies Ledger: every mob that can be a trophy, in the order of the mob trophies' creative tab, as a grid of their
 * icons (spawn eggs) on the pedestal screen's panel. A mob the reader has collected a trophy of shows in full, its name on
 * hover; one they haven't is a dark silhouette, "???". A gilded one has a gold corner. The header counts them. A search
 * box finds collected mobs by name and any mob by its mod's name (so searching gives away no mob not yet collected), and
 * the button beside it shows one mod's mobs at a time, the header then counting that mod's. The Missing toggle leaves
 * only the mobs not collected yet.
 */
public final class TrophyLedgerScreen extends Screen {
    private static final ResourceLocation PANEL = TrophyPedestalsMod.id("pedestal/panel");
    private static final int WIDTH = 248, MAX_HEIGHT = 236, MARGIN = 12, CELL = 22, COLUMNS = 9, GRID_TOP = 60, EDGE = 4;
    // The search row, under the header: the search box and, at its right, the Missing toggle and the mod button.
    private static final int SEARCH_ROW = 34, MOD_BUTTON = 76, MISSING_BUTTON = 54;
    // How many of a mob's biomes its hint names.
    private static final int HINT_BIOMES = 3;
    private static final ResourceLocation SEGMENT = TrophyPedestalsMod.id("pedestal/segment");
    private static final ResourceLocation SEGMENT_HOVER = TrophyPedestalsMod.id("pedestal/segment_hover");
    private static final ResourceLocation SEGMENT_SELECTED = TrophyPedestalsMod.id("pedestal/segment_selected");
    private static final int TEXT = 0xFFDCDCE0;
    private static final int GOLD = 0xFFF2C94C, LABEL = 0xFF9DA2A7, GROOVE_DARK = 0xFF07090E, GROOVE_LIT = 0xFF343B4E;
    private static final int SLOT = 0xFF0E121A, SLOT_HOVER = 0xFF252C3C, SHADOW = 0xE0101418;
    // The scroll bar, as the adjust screen's: its width, how far in from the panel's right edge, its thumb's least
    // height; a groove cut like the panel's dividers, and a gold thumb lit along its top and left, with grip lines.
    private static final int BAR_WIDTH = 4, BAR_INSET = 8, MIN_THUMB = 14;
    // How much shorter than the list the scroll bar's track is, at each end.
    private static final int BAR_TRIM = 1;
    private static final int GROOVE = 0xFF0E121A, PALE_GOLD = 0xFFFFE08A, GOLD_SHADE = 0xFFB8872B, GOLD_DEEP = 0xFF9A6A1A;
    private static final int GOLD_FRAME = 0xFFE0AC3C, GOLD_TOP = 0xFFFFF0A8, GOLD_LIT = 0xFFFFC24A;

    // Every mob, and its icon; then the ones the search and mod filter leave, as the grid shows them.
    private final List<EntityType<?>> allMobs = new ArrayList<>();
    private final List<ItemStack> allIcons = new ArrayList<>();
    private final List<EntityType<?>> mobs = new ArrayList<>();
    private final List<ItemStack> icons = new ArrayList<>();
    // The mods with mobs in it (by namespace), and the one shown: -1 for all of them.
    private final List<String> mods = new ArrayList<>();
    private int mod = -1;
    // Whether only the mobs not collected yet show.
    private boolean missingOnly;
    private @Nullable EditBox search;
    private final Set<ResourceLocation> collected, gilded;
    private int left, top, height2, scroll, maxScroll;
    // The mob picked to buy back (clicked once: a second click buys it), and whether its gilded trophy; -1 for none.
    private int picked = -1;
    private boolean pickedGilded;
    // While the scroll bar's thumb is held: where on it it was taken, down from its top (-1 while it isn't).
    private int dragOffset = -1;

    // Where each mob not collected yet spawns: its hint on hover.
    private final Map<ResourceLocation, List<ResourceLocation>> habitats = new HashMap<>();

    private TrophyLedgerScreen(List<ResourceLocation> collected, List<ResourceLocation> gilded, List<OpenLedgerPayload.Habitat> habitats) {
        super(Component.translatable("item.trophypedestals.trophy_ledger"));
        this.collected = Set.copyOf(collected);
        this.gilded = Set.copyOf(gilded);
        for (OpenLedgerPayload.Habitat habitat : habitats) this.habitats.put(habitat.mob(), habitat.biomes());
        Minecraft minecraft = Minecraft.getInstance();
        for (EntityType<?> type : MobTrophies.collectable(minecraft.level != null ? minecraft.level.enabledFeatures() : FeatureFlags.DEFAULT_FLAGS)) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            allMobs.add(type);
            allIcons.add(new MobTrophyRecord(id, "", 0L, PedestalRarities.NONE).icon());
            if (!mods.contains(id.getNamespace())) mods.add(id.getNamespace());
        }
        // (Minecraft first, then the others by name.)
        mods.sort(Comparator.comparing((String namespace) -> !namespace.equals(ResourceLocation.DEFAULT_NAMESPACE))
                .thenComparing(TrophyLedgerScreen::modName, String.CASE_INSENSITIVE_ORDER));
    }

    // A mod's name, as its loader knows it, or its namespace.
    private static String modName(String namespace) {
        return namespace.equals(ResourceLocation.DEFAULT_NAMESPACE) ? "Minecraft" : Platform.getModName(namespace).orElse(namespace);
    }

    /** Opens the ledger with what the server says the reader has collected. */
    public static void open(List<ResourceLocation> collected, List<ResourceLocation> gilded, List<OpenLedgerPayload.Habitat> habitats) {
        Minecraft.getInstance().setScreen(new TrophyLedgerScreen(collected, gilded, habitats));
    }

    @Override
    protected void init() {
        height2 = Math.min(MAX_HEIGHT, height - 2 * EDGE);
        left = Math.max(EDGE, (width - WIDTH) / 2);
        top = Math.max(EDGE, (height - height2) / 2);
        String query = search == null ? "" : search.getValue();
        int row = top + SEARCH_ROW, wide = WIDTH - 2 * MARGIN;
        search = addRenderableWidget(new EditBox(font, left + MARGIN, row, wide - MOD_BUTTON - MISSING_BUTTON - 8, 16, Component.translatable("trophypedestals.ledger.search")));
        search.setHint(Component.translatable("trophypedestals.ledger.search").withColor(LABEL));
        search.setValue(query);
        search.setResponder(text -> filter());
        addRenderableWidget(new MissingButton(left + WIDTH - MARGIN - MOD_BUTTON - 4 - MISSING_BUTTON, row - 1, MISSING_BUTTON, 18));
        addRenderableWidget(new ModButton(left + WIDTH - MARGIN - MOD_BUTTON, row - 1, MOD_BUTTON, 18));
        filter();
    }

    // The mobs left by the search and the mod shown. A mob is found by its name only once it's collected (else it's
    // "???"), and by its mod's name always.
    private void filter() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        mobs.clear();
        icons.clear();
        for (int i = 0; i < allMobs.size(); i++) {
            EntityType<?> type = allMobs.get(i);
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (mod >= 0 && !id.getNamespace().equals(mods.get(mod))) continue;
            if (missingOnly && collected.contains(id)) continue;
            if (!query.isEmpty() && !modName(id.getNamespace()).toLowerCase(Locale.ROOT).contains(query)
                    && !(collected.contains(id) && type.getDescription().getString().toLowerCase(Locale.ROOT).contains(query))) continue;
            mobs.add(type);
            icons.add(allIcons.get(i));
        }
        picked = -1;
        int rows = (mobs.size() + COLUMNS - 1) / COLUMNS;
        maxScroll = Math.max(0, rows * CELL - (height2 - GRID_TOP - MARGIN));
        scroll = Mth.clamp(scroll, 0, maxScroll);
    }

    private int gridLeft() {
        return left + (WIDTH - COLUMNS * CELL) / 2;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(PANEL, left, top, WIDTH, height2);
        // The count: of every mob, or of the mod shown's.
        List<EntityType<?>> counted = mod < 0 ? allMobs
                : allMobs.stream().filter(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(mods.get(mod))).toList();
        long count = counted.stream().filter(type -> collected.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type))).count();
        int percent = counted.isEmpty() ? 0 : (int) (count * 100 / counted.size());
        graphics.drawString(font, title, left + MARGIN, top + 12, GOLD);
        graphics.drawString(font, Component.translatable("trophypedestals.ledger.count", count, counted.size(), percent), left + MARGIN, top + 23, LABEL);
        graphics.fill(left + MARGIN, top + GRID_TOP - 6, left + WIDTH - MARGIN, top + GRID_TOP - 5, GROOVE_DARK);
        graphics.fill(left + MARGIN, top + GRID_TOP - 5, left + WIDTH - MARGIN, top + GRID_TOP - 4, GROOVE_LIT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x0 = gridLeft(), y0 = top + GRID_TOP, bottom = top + height2 - MARGIN;
        graphics.enableScissor(left, y0, left + WIDTH, bottom);
        List<Component> hoveredLines = null;
        for (int i = 0; i < mobs.size(); i++) {
            int x = x0 + i % COLUMNS * CELL, y = y0 + i / COLUMNS * CELL - scroll;
            if (y + CELL < y0 || y > bottom) continue;
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mobs.get(i));
            boolean has = collected.contains(id);
            boolean over = mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2 && mouseY >= y0 && mouseY < bottom;
            graphics.fill(x, y, x + CELL - 2, y + CELL - 2, over ? SLOT_HOVER : SLOT);
            graphics.renderItem(icons.get(i), x + 2, y + 2);
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
                if (!has) habitatLines(hoveredLines, id);
            }
        }
        graphics.disableScissor();
        if (maxScroll > 0) scrollBar(graphics, mouseX, mouseY);
        if (hoveredLines != null) setTooltipForNextRenderPass(hoveredLines.stream().map(Component::getVisualOrderText).toList());
    }

    // Where a mob not collected yet spawns, as a hint: "Found in: Plains, Forest, Taiga and 4 more" (up to HINT_BIOMES
    // named), or that it isn't found spawning by itself.
    private void habitatLines(List<Component> lines, ResourceLocation mob) {
        List<ResourceLocation> biomes = habitats.getOrDefault(mob, List.of());
        if (biomes.isEmpty()) {
            lines.add(Component.translatable("trophypedestals.ledger.habitat.none").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        MutableComponent named = Component.empty();
        for (int i = 0; i < Math.min(HINT_BIOMES, biomes.size()); i++) {
            if (i > 0) named.append(", ");
            named.append(Component.translatable(biomes.get(i).toLanguageKey("biome")));
        }
        Component found = biomes.size() > HINT_BIOMES
                ? Component.translatable("trophypedestals.ledger.habitat.more", named, biomes.size() - HINT_BIOMES) : named;
        lines.add(Component.translatable("trophypedestals.ledger.habitat", found).withStyle(ChatFormatting.GRAY));
    }

    // The grid's top and bottom, where the scroll bar runs.
    private int viewTop() {
        return top + GRID_TOP + BAR_TRIM;
    }

    private int viewBottom() {
        return top + height2 - MARGIN - BAR_TRIM;
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
    private void scrollBar(GuiGraphics graphics, int mouseX, int mouseY) {
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragOffset >= 0) {
            dragThumb(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragOffset >= 0 && button == 0) {
            dragOffset = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
        boolean gild = hasGilded && Screen.hasShiftDown();
        int cost = TrophyLedger.buyBackCost(gild);
        boolean afford = creative || levels >= cost;
        lines.add(Component.translatable(index == picked && pickedGilded == gild ? "trophypedestals.ledger.buy_back.confirm" : "trophypedestals.ledger.buy_back.click",
                Component.translatable("trophypedestals.ledger.buy_back.cost", cost).withStyle(afford ? ChatFormatting.GREEN : ChatFormatting.RED))
                .withStyle(ChatFormatting.GRAY));
        if (hasGilded) lines.add(Component.translatable("trophypedestals.ledger.buy_back.gilded_hint", TrophyLedger.buyBackCost(true)).withStyle(ChatFormatting.DARK_GRAY));
    }

    // A collected mob clicked: picked to buy back, and bought on a second click (sneak-click for its gilded trophy).
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // The scroll bar: its thumb taken where it's clicked, or (clicked above or below it) moved there by its middle.
        if (button == 0 && overBar(mouseX, mouseY)) {
            int ty = thumbY(), th = thumbHeight();
            dragOffset = mouseY >= ty && mouseY < ty + th ? (int) mouseY - ty : th / 2;
            dragThumb(mouseY);
            return true;
        }
        if (button == 0 && buyBack()) {
            int x0 = gridLeft(), y0 = top + GRID_TOP, bottom = top + height2 - MARGIN;
            if (mouseX >= x0 && mouseX < x0 + COLUMNS * CELL && mouseY >= y0 && mouseY < bottom) {
                int column = (int) ((mouseX - x0) / CELL), row = (int) ((mouseY - y0 + scroll) / CELL), index = row * COLUMNS + column;
                if (index >= 0 && index < mobs.size()) {
                    ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mobs.get(index));
                    if (!collected.contains(id)) return true;
                    boolean gild = gilded.contains(id) && Screen.hasShiftDown();
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
        return super.mouseClicked(mouseX, mouseY, button);
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

    /** Whether only the mobs not collected yet show: gold while on. */
    private final class MissingButton extends AbstractButton {
        MissingButton(int x, int y, int width, int height) {
            super(x, y, width, height, Component.translatable("trophypedestals.ledger.missing"));
            setTooltip(Tooltip.create(Component.translatable("trophypedestals.ledger.missing.tooltip")));
        }

        @Override
        public void onPress() {
            missingOnly = !missingOnly;
            scroll = 0;
            filter();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation sprite = missingOnly ? SEGMENT_SELECTED : isHoveredOrFocused() ? SEGMENT_HOVER : SEGMENT;
            graphics.blitSprite(sprite, getX(), getY(), width, height);
            int color = missingOnly ? PALE_GOLD : isHoveredOrFocused() ? 0xFFFFFFFF : TEXT;
            graphics.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + (height - 8) / 2, color);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** Which mod's mobs show: all, then each mod in turn, with a click (a right click goes back one). */
    private final class ModButton extends AbstractButton {
        ModButton(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
            setTooltip(Tooltip.create(Component.translatable("trophypedestals.ledger.mod.tooltip")));
        }

        private Component label() {
            return mod < 0 ? Component.translatable("trophypedestals.ledger.mod.all") : Component.literal(modName(mods.get(mod)));
        }

        @Override
        public void onPress() {
            step(1);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1 && isMouseOver(mouseX, mouseY)) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                step(-1);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // From all (-1) through each mod, and round again.
        private void step(int by) {
            mod = Math.floorMod(mod + 1 + by, mods.size() + 1) - 1;
            scroll = 0;
            filter();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blitSprite(isHoveredOrFocused() ? SEGMENT_HOVER : SEGMENT, getX(), getY(), width, height);
            FormattedCharSequence text = Language.getInstance().getVisualOrder(font.substrByWidth(label(), width - 8));
            graphics.drawString(font, text, getX() + (width - font.width(text)) / 2, getY() + (height - 8) / 2, isHoveredOrFocused() ? 0xFFFFFFFF : TEXT);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, label());
        }
    }
}
