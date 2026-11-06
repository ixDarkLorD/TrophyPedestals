package net.ixdarklord.trophypedestals.client;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.compat.render.EntityRenderStates;
import net.ixdarklord.trophypedestals.network.AdjustDisplayedTrophyPayload;
import net.ixdarklord.trophypedestals.network.AdjustPedestalPayload;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.Statues;
import net.ixdarklord.trophypedestals.pedestal.TrophyCabinetBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A pedestal's adjustment screen, opened by sneaking and using it with an empty hand: a slate and gold panel with the
 * pedestal and its owner at the top. A trophy's settings are in tabs, one column each, so the screen stays small at every
 * GUI scale: Display (its size on a slider, from half to twenty times and finer at the small end, with quick 1x to 20x
 * choices; its height; its rotation: yaw, pitch and roll), Text (when it shows: when looked at, always or hidden; its height; its
 * size), Behavior (spinning, flourishes and sounds as toggles; how it answers redstone) and, for a mob or statue, Pose
 * (moving or holding still, or icon or statue, with the statue's pose; and the animation it plays on loop, picked from
 * what it can). Each slider has a button putting it back; Reset and Done sit under every tab, and the last tab used
 * opens next time. A display pedestal's (one showing a player's item, or empty) has only its text, redstone and sounds
 * choices, on one page.
 * Changes show on the pedestal at once and are sent to the server as the screen closes, which keeps them only for a
 * player allowed to adjust it. Its sprites are in {@code textures/gui/sprites/pedestal/}.
 */
public final class PedestalScreen extends Screen {
    private static final ResourceLocation PANEL = TrophyPedestalsMod.id("pedestal/panel");
    private static final ResourceLocation SEGMENT = TrophyPedestalsMod.id("pedestal/segment");
    private static final ResourceLocation SEGMENT_HOVER = TrophyPedestalsMod.id("pedestal/segment_hover");
    private static final ResourceLocation SEGMENT_SELECTED = TrophyPedestalsMod.id("pedestal/segment_selected");
    private static final ResourceLocation TRACK = TrophyPedestalsMod.id("pedestal/track");
    private static final ResourceLocation TRACK_FILL = TrophyPedestalsMod.id("pedestal/track_fill");
    private static final ResourceLocation HANDLE = TrophyPedestalsMod.id("pedestal/handle");
    private static final ResourceLocation HANDLE_HOVER = TrophyPedestalsMod.id("pedestal/handle_hover");
    // The mod logo's rarity gems, set into the middle of the panel's top and bottom edges (each in its edge's golds).
    private static final ResourceLocation GEMS_TOP = TrophyPedestalsMod.id("pedestal/gems_top");
    private static final ResourceLocation GEMS = TrophyPedestalsMod.id("pedestal/gems");
    private static final int GEMS_WIDTH = 28, FRAME = 4;
    private static final int MARGIN = 12, RESET = 16;
    // A display pedestal's screen: when its text shows, how it answers redstone, its sounds and its showcase light, in one column.
    private static final int COMPACT_WIDTH = 186, COMPACT_HEIGHT = 230, COMPACT_ROW_2 = 84, COMPACT_ROW_3 = 122, COMPACT_ROW_4 = 160;
    private static final ResourceLocation RESET_ICON = TrophyPedestalsMod.id("pedestal/reset");
    // The "?" after each label, and how wide its tooltip's lines wrap.
    private static final ResourceLocation INFO = TrophyPedestalsMod.id("pedestal/info");
    private static final ResourceLocation INFO_HOVER = TrophyPedestalsMod.id("pedestal/info_hover");
    private static final int INFO_SIZE = 9, TOOLTIP_WIDTH = 180;
    // The mod logo's colors: its golds, its stone grey for labels, and the dark navy inside the panel's gold frame for
    // grooves and lists.
    private static final int GOLD = 0xFFF2C94C, PALE_GOLD = 0xFFFFE08A, LABEL = 0xFF9DA2A7, TEXT = 0xFFDCDCE0;
    private static final float[] PRESETS = {1.0F, 2.0F, 5.0F, 10.0F, 20.0F};
    // The room kept clear on each side of a button's label, inside its gold trim.
    private static final int LABEL_PADDING = 4;
    // The rows' labels, down from the panel's top; each row's controls sit just under its label. A display pedestal's
    // screen starts at ROW_1 too.
    private static final int ROW_1 = 46;
    // A trophy's screen: one column under a row of tabs, each tab's sections down from CONTENT, the next one SECTION
    // further (the display size, with its quick choices under it, takes one more row).
    private static final int TABBED_WIDTH = 248, TABBED_HEIGHT = 246, TAB_ROW = 44, CONTENT = 74, SECTION = 38, PRESET_ROW = 16;
    // The part of a trophy's screen a tab's sections scroll in: under the tabs' groove, over Reset and Done. A tab whose
    // sections run past its bottom scrolls (SCROLL_STEP a wheel notch), with a bar in the panel's right margin.
    private static final int VIEW_TOP = TAB_ROW + 26, VIEW_BOTTOM = TABBED_HEIGHT - 34, SCROLL_STEP = 14;
    // The scroll bar: how wide it is and how far in from the panel's right edge; its thumb's least height. Its groove is
    // cut like the panel's dividers; its thumb is gold, lit along its top and left, shaded along its bottom and right,
    // with grip lines across its middle.
    private static final int BAR_WIDTH = 4, BAR_INSET = 6, MIN_THUMB = 14;
    private static final int GROOVE_DARK = 0xFF07090E, GROOVE = 0xFF0E121A, GROOVE_LIT = 0xFF343B4E, GOLD_SHADE = 0xFFB8872B, GOLD_DEEP = 0xFF9A6A1A;
    private static final int GOLD_FRAME = 0xFFE0AC3C, GOLD_TOP = 0xFFFFF0A8, GOLD_LIT = 0xFFFFC24A, LIST = 0xF00E121A, LIST_HOVER = 0xFF252C3C;

    /** A trophy screen's tabs; the last one used opens next time. */
    private enum Tab {
        DISPLAY, TEXT, POSE, BEHAVIOR;

        Component label() {
            return Component.translatable("trophypedestals.pedestal.screen.tab." + name().toLowerCase(Locale.ROOT));
        }
    }

    private static Tab lastTab = Tab.DISPLAY;
    // The animation list's rows: their height and how many show.
    private static final int LIST_ROW = 13, LIST_ROWS = 7;

    private final BlockPos pos;
    private final ItemStack icon;
    private final Component owner;
    private PedestalBlockEntity.TextMode textMode;
    private float scale, lift, textLift, textScale, yaw, pitch, roll, crownScale, crownX, crownY;
    private boolean spins, effects, sounds, animates, statue, crown, light;
    // Where the mouse is this frame, for the labels' "?" tooltips.
    private int mouseX, mouseY;
    private PedestalBlockEntity.StatuePose statuePose;
    private String animation;
    private PedestalBlockEntity.RedstoneMode redstone;
    // Who else may adjust it, and whether this player may change that (its owner, or an operator).
    private PedestalBlockEntity.Access access;
    private final boolean accessible;
    private SizeSlider slider;
    private HeightSlider heightSlider;
    private TextHeightSlider textSlider;
    private TextSizeSlider textSizeSlider;
    private final List<AngleSlider> rotationSliders = new ArrayList<>();
    private final List<ValueSlider> crownSliders = new ArrayList<>();
    private int left, top;
    // Whether it's a trophy (every option), or a display pedestal (text and redstone only).
    private final boolean trophy;
    // Whether it's a mob trophy, whose mob can be set to hold still.
    private final boolean mob;
    // Whether it's an achievement trophy earned by a player, which can show their statue.
    private final boolean statueable;
    // Whether it's a player trophy, whose statue can be posed.
    private final boolean playerTrophy;
    // Whether its rank wears a crown, which can be hidden.
    private final boolean crowned;
    // What its mob or statue can play (none for one that shows neither); the animation list's button, whether the list
    // is open, and how far it's scrolled.
    private final List<MobAnimations.Animation> animations;
    private @Nullable Choice animationButton;
    private boolean listOpen;
    private int listScroll;
    // For a trophy on a trophy display: its spot (-1 for a pedestal of its own), and the stand-in its changes are shown
    // on. Its size and place stay as a new pedestal has them, so the settings that change them aren't offered.
    private final int slot;
    private final @Nullable PedestalBlockEntity standIn;
    // The showing tab's widgets, which scroll (each with where it sits unscrolled), while init is adding them; how far
    // the tab is scrolled, and how far it can be.
    private final Map<AbstractWidget, Integer> scrolled = new LinkedHashMap<>();
    private boolean addingScrolled;
    private int scroll, maxScroll;
    // While the scroll bar's thumb is held: where on it it was taken, down from its top (-1 while it isn't).
    private int dragOffset = -1;

    private PedestalScreen(BlockPos pos, PedestalBlockEntity pedestal) {
        this(pos, pedestal, -1);
    }

    private PedestalScreen(BlockPos pos, PedestalBlockEntity pedestal, int slot) {
        super(Component.translatable("trophypedestals.pedestal.screen.title"));
        this.pos = pos;
        this.slot = slot;
        this.standIn = slot >= 0 ? pedestal : null;
        this.icon = new ItemStack(pedestal.getBlockState().getBlock());
        this.owner = pedestal.ownerName().isEmpty() ? Component.translatable("trophypedestals.pedestal.screen.no_owner")
                : Component.translatable("trophypedestals.pedestal.screen.owner", pedestal.ownerName());
        this.textMode = pedestal.textMode();
        this.scale = pedestal.scale();
        this.lift = pedestal.height();
        this.spins = pedestal.spins();
        this.animates = pedestal.animates();
        this.statue = pedestal.statue();
        this.statuePose = pedestal.statuePose();
        this.textLift = pedestal.textHeight();
        this.textScale = pedestal.textScale();
        this.crown = pedestal.crown();
        this.crownScale = pedestal.crownScale();
        this.crownX = pedestal.crownX();
        this.crownY = pedestal.crownY();
        this.light = pedestal.light();
        this.crowned = PedestalRenderer.hasCrown(pedestal.rarity());
        this.yaw = pedestal.yaw();
        this.pitch = pedestal.pitch();
        this.roll = pedestal.roll();
        this.effects = pedestal.effects();
        this.sounds = pedestal.sounds();
        this.redstone = pedestal.redstone();
        this.access = pedestal.access();
        Minecraft minecraft = Minecraft.getInstance();
        this.accessible = slot < 0 && minecraft.player != null && pedestal.canChangeAccess(minecraft.player);
        this.trophy = pedestal.isTrophy();
        this.mob = pedestal.mobTrophy() != null;
        this.statueable = pedestal.canShowStatue();
        this.playerTrophy = pedestal.mobTrophy() != null && pedestal.mobTrophy().type() == Statues.FIGURE;
        this.animation = pedestal.animation();
        this.animations = animationsOf(pedestal);
    }

    // What the pedestal's mob or statue can play: the one it shows, or one made to look into (a statue not shown yet).
    private static List<MobAnimations.Animation> animationsOf(PedestalBlockEntity pedestal) {
        EntityType<?> type = pedestal.mobTrophy() != null ? pedestal.mobTrophy().type() : pedestal.canShowStatue() ? Statues.FIGURE : null;
        if (type == null || pedestal.getLevel() == null) return List.of();
        Entity mob = pedestal.shownMob();
        try {
            if (mob == null || mob.getType() != type) mob = EntityRenderStates.create(type, pedestal.getLevel());
        } catch (RuntimeException | LinkageError e) {
            mob = null;
        }
        return mob == null ? List.of() : MobAnimations.of(mob);
    }

    /** Opens the screen for the pedestal at {@code pos}, if it's still there. */
    public static void open(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) {
            minecraft.setScreen(new PedestalScreen(pos, pedestal));
        }
    }

    /** Opens the adjust screen of the trophy on this spot of the trophy display here. */
    public static void openDisplayed(BlockPos pos, int slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof TrophyCabinetBlockEntity display) {
            PedestalBlockEntity pedestal = display.shownPedestal(slot);
            if (pedestal != null) minecraft.setScreen(new PedestalScreen(pos, pedestal, slot));
        }
    }

    // Whether its size and place are fixed (a trophy on a trophy display).
    private boolean fixedSize() {
        return slot >= 0;
    }

    private int panelWidth() {
        return trophy ? TABBED_WIDTH : COMPACT_WIDTH;
    }

    // (A trophy's is the same height on every tab; a tab with more than fits scrolls.)
    private int panelHeight() {
        return trophy ? TABBED_HEIGHT : COMPACT_HEIGHT;
    }

    private int tabbedHeight() {
        return TABBED_HEIGHT;
    }

    // Whether it has the Pose tab: a mob's or statue's pose, and the animation it plays.
    private boolean posed() {
        return mob || statueable || !animations.isEmpty();
    }

    // The tabs it shows, Behavior last: Pose only for a mob or statue, and Display, Text and Pose only as the server lets
    // players change them (Behavior always).
    private List<Tab> tabs() {
        List<Tab> tabs = new ArrayList<>();
        if (TrophyPedestalsConfig.PEDESTAL_DISPLAY_SETTINGS.get()) tabs.add(Tab.DISPLAY);
        if (TrophyPedestalsConfig.PEDESTAL_TEXT_SETTINGS.get()) tabs.add(Tab.TEXT);
        if (posed() && TrophyPedestalsConfig.PEDESTAL_POSE_SETTINGS.get()) tabs.add(Tab.POSE);
        tabs.add(Tab.BEHAVIOR);
        return tabs;
    }

    // The tab showing: the last one used, if this pedestal has it, else its first.
    private Tab tab() {
        List<Tab> tabs = tabs();
        return tabs.contains(lastTab) ? lastTab : tabs.getFirst();
    }

    // Where a tab's n-th section's label is, down from the panel's top (the display size's quick choices push later
    // sections on the Display tab down a row).
    private static int section(int n, boolean afterPresets) {
        return CONTENT + n * SECTION + (afterPresets ? PRESET_ROW : 0);
    }

    @Override
    protected void init() {
        left = (width - panelWidth()) / 2;
        top = (height - panelHeight()) / 2;
        // Who else may adjust it, at the header's right, for its owner: cycling Private, Team, Everyone.
        if (accessible) {
            int wide = 0;
            for (PedestalBlockEntity.Access each : PedestalBlockEntity.Access.values()) wide = Math.max(wide, font.width(accessLabel(each)));
            wide += 2 * LABEL_PADDING;
            Choice button = addRenderableWidget(new Choice(left + panelWidth() - MARGIN - wide, top + 12, wide, 18, accessLabel(access), () -> false, () -> {}));
            button.action = () -> {
                access = access.next();
                button.setMessage(accessLabel(access));
            };
            tip(button, "trophypedestals.pedestal.screen.access");
        }
        if (!trophy) {
            initCompact();
            return;
        }
        int column = TABBED_WIDTH - 2 * MARGIN, x = left + MARGIN;
        // The tabs, across the top: the chosen one gold.
        List<Tab> tabs = tabs();
        Component[] tabLabels = new Component[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) tabLabels[i] = tabs.get(i).label();
        int[][] spans = split(x, column, tabLabels);
        for (int i = 0; i < tabs.size(); i++) {
            Tab each = tabs.get(i);
            addRenderableWidget(new Choice(spans[i][0], top + TAB_ROW, spans[i][1], 18, tabLabels[i], () -> tab() == each, () -> {
                lastTab = each;
                scroll = 0;
                rebuildWidgets();
            }));
        }
        this.slider = null;
        textSlider = null;
        textSizeSlider = null;
        heightSlider = null;
        rotationSliders.clear();
        crownSliders.clear();
        animationButton = null;
        listOpen = false;
        scrolled.clear();
        addingScrolled = true;
        switch (tab()) {
            case DISPLAY -> initDisplay(x, column);
            case TEXT -> initText(x, column);
            case BEHAVIOR -> initBehavior(x, column);
            case POSE -> initPose(x, column);
        }
        addingScrolled = false;
        int bottom = top + VIEW_TOP;
        for (AbstractWidget widget : scrolled.keySet()) bottom = Math.max(bottom, widget.getBottom());
        maxScroll = Math.max(0, bottom + 6 - (top + VIEW_BOTTOM));
        applyScroll();
        // Reset puts everything back as a new pedestal has it (on every tab); Done closes.
        int half = (column - 4) / 2;
        addRenderableWidget(new Choice(x, top + tabbedHeight() - 28, half, 18, Component.translatable("trophypedestals.pedestal.screen.reset"),
                () -> false, this::reset));
        addRenderableWidget(new Choice(x + half + 4, top + tabbedHeight() - 28, column - half - 4, 18, CommonComponents.GUI_DONE, () -> false, this::onClose));
    }

    // While a tab's sections are being added, they go in the scrolling part instead: listened to, but drawn (clipped to
    // that part) by extractRenderState.
    @Override
    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
        if (!addingScrolled || !(widget instanceof AbstractWidget scrolls)) return super.addRenderableWidget(widget);
        scrolled.put(scrolls, scrolls.getY());
        return addWidget(widget);
    }

    // Moves the tab's widgets up by how far it's scrolled; any wholly out of the scrolling part don't take the mouse.
    private void applyScroll() {
        scroll = Mth.clamp(scroll, 0, maxScroll);
        scrolled.forEach((widget, y) -> {
            widget.setY(y - scroll);
            widget.visible = widget.getBottom() > top + VIEW_TOP && widget.getY() < top + VIEW_BOTTOM;
        });
    }

    private int barX() {
        return left + panelWidth() - BAR_INSET - BAR_WIDTH;
    }

    private int thumbHeight() {
        int track = VIEW_BOTTOM - VIEW_TOP - 2;
        return Math.max(MIN_THUMB, track * track / (track + maxScroll));
    }

    private int thumbY() {
        return top + VIEW_TOP + 1 + (VIEW_BOTTOM - VIEW_TOP - 2 - thumbHeight()) * scroll / maxScroll;
    }

    private boolean overBar(double mouseX, double mouseY) {
        return maxScroll > 0 && mouseX >= barX() - 1 && mouseX < barX() + BAR_WIDTH + 1 && inView(mouseY);
    }

    // Scrolls so the thumb's top is at this height (the mouse's, less where on the thumb it was taken).
    private void dragThumb(double mouseY) {
        int travel = VIEW_BOTTOM - VIEW_TOP - 2 - thumbHeight();
        scroll = travel <= 0 ? 0 : (int) Math.round((mouseY - dragOffset - (top + VIEW_TOP + 1)) * maxScroll / travel);
        applyScroll();
    }

    // The groove (a dark rim, lit along its bottom and right like the dividers) and the thumb in it: gold with a bevel,
    // brighter while hovered or held, and three grip lines across its middle.
    private void scrollBar(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = barX(), y0 = top + VIEW_TOP, y1 = top + VIEW_BOTTOM;
        graphics.fill(x - 1, y0 - 1, x + BAR_WIDTH + 1, y1 + 1, GROOVE_LIT);
        graphics.fill(x - 1, y0 - 1, x + BAR_WIDTH, y1, GROOVE_DARK);
        graphics.fill(x, y0, x + BAR_WIDTH, y1, GROOVE);
        int ty = thumbY(), th = thumbHeight();
        boolean lit = dragOffset >= 0 || (mouseX >= x && mouseX < x + BAR_WIDTH && mouseY >= ty && mouseY < ty + th);
        graphics.fill(x, ty, x + BAR_WIDTH, ty + th, GOLD_DEEP);
        graphics.fill(x, ty, x + BAR_WIDTH - 1, ty + th - 1, lit ? GOLD_TOP : PALE_GOLD);
        graphics.fill(x + 1, ty + 1, x + BAR_WIDTH - 1, ty + th - 1, lit ? GOLD_LIT : GOLD_FRAME);
        int mid = ty + th / 2;
        for (int line = -2; line <= 2; line += 2) {
            graphics.fill(x + 1, mid + line, x + BAR_WIDTH - 1, mid + line + 1, GOLD_SHADE);
        }
    }

    private boolean inView(double mouseY) {
        return mouseY >= top + VIEW_TOP && mouseY < top + VIEW_BOTTOM;
    }

    // Display: its size (with the quick choices), its height, its rotation (only its rotation, for a trophy on a display).
    private void initDisplay(int x, int column) {
        int slider = column - RESET - 4;
        if (fixedSize()) {
            initRotation(x, column, slider, section(0, false));
            return;
        }
        this.slider = addRenderableWidget(new SizeSlider(x, top + section(0, false) + 14, slider));
        addRenderableWidget(new ResetButton(x + column - RESET, top + section(0, false) + 12, () -> {
            scale = 1.0F;
            this.slider.sync();
            preview();
        }));
        int chip = (column - 3 * (PRESETS.length - 1)) / PRESETS.length;
        for (int i = 0; i < PRESETS.length; i++) {
            float preset = PRESETS[i];
            addRenderableWidget(new Choice(x + i * (chip + 3), top + section(0, false) + 32, chip, 16, Component.literal(size(preset)),
                    () -> Math.abs(scale - preset) < 0.001F, () -> {
                        scale = preset;
                        this.slider.sync();
                        preview();
                    }));
        }
        heightSlider = addRenderableWidget(new HeightSlider(x, top + section(1, true) + 14, slider));
        addRenderableWidget(new ResetButton(x + column - RESET, top + section(1, true) + 12, () -> {
            lift = 0.0F;
            heightSlider.sync();
            preview();
        }));
        initRotation(x, column, slider, section(2, true));
        // A legendary trophy's crown: its size, and how far forward and up it's moved, side by side (each a third of the
        // row), with one reset for all three.
        if (crowned) {
            int row = top + section(3, true) + 14, gap = 6, third = (slider - 2 * gap) / 3;
            crownSliders.add(addRenderableWidget(new ValueSlider(x, row, third, PedestalBlockEntity.MIN_CROWN_SCALE,
                    PedestalBlockEntity.MAX_CROWN_SCALE, () -> crownScale, value -> crownScale = value, "trophypedestals.pedestal.screen.crown_size_narration", true)));
            crownSliders.add(addRenderableWidget(new ValueSlider(x + third + gap, row, third, -PedestalBlockEntity.MAX_CROWN_OFFSET,
                    PedestalBlockEntity.MAX_CROWN_OFFSET, () -> crownX, value -> crownX = value, "trophypedestals.pedestal.screen.crown_x_narration", false)));
            crownSliders.add(addRenderableWidget(new ValueSlider(x + 2 * (third + gap), row, slider - 2 * (third + gap), -PedestalBlockEntity.MAX_CROWN_OFFSET,
                    PedestalBlockEntity.MAX_CROWN_OFFSET, () -> crownY, value -> crownY = value, "trophypedestals.pedestal.screen.crown_y_narration", false)));
            addRenderableWidget(new ResetButton(x + column - RESET, top + section(3, true) + 12, () -> {
                crownScale = 1.0F;
                crownX = 0.0F;
                crownY = 0.0F;
                crownSliders.forEach(ValueSlider::sync);
                preview();
            }));
        }
    }

    // Its rotation, in the section whose label is at this height: yaw, pitch and roll side by side (each a third of the
    // row, as the crown's are), with one reset for all three. Yaw goes round from 0 to 360 (where a spinning one starts
    // from); pitch and roll tip it either way, up to half a turn.
    private void initRotation(int x, int column, int slider, int label) {
        int row = top + label + 14, gap = 6, third = (slider - 2 * gap) / 3;
        rotationSliders.add(addRenderableWidget(new AngleSlider(x, row, third, 0.0F, 360.0F,
                () -> yaw, value -> yaw = value, "trophypedestals.pedestal.screen.yaw_narration")));
        rotationSliders.add(addRenderableWidget(new AngleSlider(x + third + gap, row, third, -PedestalBlockEntity.MAX_TILT,
                PedestalBlockEntity.MAX_TILT, () -> pitch, value -> pitch = value, "trophypedestals.pedestal.screen.pitch_narration")));
        rotationSliders.add(addRenderableWidget(new AngleSlider(x + 2 * (third + gap), row, slider - 2 * (third + gap), -PedestalBlockEntity.MAX_TILT,
                PedestalBlockEntity.MAX_TILT, () -> roll, value -> roll = value, "trophypedestals.pedestal.screen.roll_narration")));
        addRenderableWidget(new ResetButton(x + column - RESET, top + label + 12, () -> {
            yaw = 0.0F;
            pitch = 0.0F;
            roll = 0.0F;
            rotationSliders.forEach(AngleSlider::sync);
            preview();
        }));
    }

    // Text: when it shows, how high it floats, how large it is.
    private void initText(int x, int column) {
        int slider = column - RESET - 4;
        PedestalBlockEntity.TextMode[] modes = PedestalBlockEntity.TextMode.values();
        Component[] modeLabels = new Component[modes.length];
        for (int i = 0; i < modes.length; i++) modeLabels[i] = Component.translatable("trophypedestals.pedestal.screen.text." + modes[i].getSerializedName());
        int[][] spans = split(x, column, modeLabels);
        for (int i = 0; i < modes.length; i++) {
            PedestalBlockEntity.TextMode mode = modes[i];
            addRenderableWidget(new Choice(spans[i][0], top + section(0, false) + 12, spans[i][1], 18, modeLabels[i], () -> textMode == mode, () -> {
                textMode = mode;
                preview();
            }));
        }
        // (For a trophy on a display, only when it shows.)
        if (fixedSize()) return;
        textSlider = addRenderableWidget(new TextHeightSlider(x, top + section(1, false) + 14, slider));
        addRenderableWidget(new ResetButton(x + column - RESET, top + section(1, false) + 12, () -> {
            textLift = 0.0F;
            textSlider.sync();
            preview();
        }));
        textSizeSlider = addRenderableWidget(new TextSizeSlider(x, top + section(2, false) + 14, slider));
        addRenderableWidget(new ResetButton(x + column - RESET, top + section(2, false) + 12, () -> {
            textScale = 1.0F;
            textSizeSlider.sync();
            preview();
        }));
    }

    // Behavior: spinning, flourishes and sounds (each a toggle, gold while on), and how it answers redstone.
    private void initBehavior(int x, int column) {
        Component spin = Component.translatable("trophypedestals.pedestal.screen.toggle.spin");
        Component flourishes = Component.translatable("trophypedestals.pedestal.screen.toggle.flourishes");
        Component soundsLabel = Component.translatable("trophypedestals.pedestal.screen.toggle.sounds");
        Component crownLabel = Component.translatable("trophypedestals.pedestal.screen.toggle.crown");
        int[][] spans = crowned ? split(x, column, spin, flourishes, soundsLabel, crownLabel) : split(x, column, spin, flourishes, soundsLabel);
        tip(addRenderableWidget(new Choice(spans[0][0], top + section(0, false) + 12, spans[0][1], 18, spin, () -> spins, () -> {
            spins = !spins;
            preview();
        })), "trophypedestals.pedestal.screen.toggle.spin");
        tip(addRenderableWidget(new Choice(spans[1][0], top + section(0, false) + 12, spans[1][1], 18, flourishes, () -> effects, () -> {
            effects = !effects;
            preview();
        })), "trophypedestals.pedestal.screen.toggle.flourishes");
        tip(addRenderableWidget(new Choice(spans[2][0], top + section(0, false) + 12, spans[2][1], 18, soundsLabel, () -> sounds, () -> {
            sounds = !sounds;
            preview();
        })), "trophypedestals.pedestal.screen.toggle.sounds");
        // A legendary trophy's crown over what it shows.
        if (crowned) {
            tip(addRenderableWidget(new Choice(spans[3][0], top + section(0, false) + 12, spans[3][1], 18, crownLabel, () -> crown, () -> {
                crown = !crown;
                preview();
            })), "trophypedestals.pedestal.screen.toggle.crown");
        }
        lightChoices(x, column, top + section(1, false) + 12);
        PedestalBlockEntity.RedstoneMode[] redstoneModes = PedestalBlockEntity.RedstoneMode.values();
        Component[] redstoneLabels = new Component[redstoneModes.length];
        for (int i = 0; i < redstoneModes.length; i++) redstoneLabels[i] = Component.translatable("trophypedestals.pedestal.screen.redstone." + redstoneModes[i].getSerializedName());
        spans = split(x, column, redstoneLabels);
        for (int i = 0; i < redstoneModes.length; i++) {
            PedestalBlockEntity.RedstoneMode mode = redstoneModes[i];
            addRenderableWidget(new Choice(spans[i][0], top + section(2, false) + 12, spans[i][1], 18, redstoneLabels[i], () -> redstone == mode, () -> {
                redstone = mode;
                preview();
            }));
        }
    }

    // Pose: a mob trophy's mob moving or holding still (and a player trophy's pose), or an achievement trophy showing its icon
    // or its unlocker's statue (and the statue's pose); then the animation it plays.
    private void initPose(int x, int column) {
        int row = top + section(0, false) + 12;
        if (mob) {
            Component animated = Component.translatable("trophypedestals.pedestal.screen.pose.animated");
            Component frozen = Component.translatable("trophypedestals.pedestal.screen.pose.frozen");
            int[][] spans = playerTrophy ? split(x, column, animated, frozen, widestPose()) : split(x, column, animated, frozen);
            addRenderableWidget(new Choice(spans[0][0], row, spans[0][1], 18, animated, () -> animates, () -> {
                animates = true;
                preview();
            }));
            addRenderableWidget(new Choice(spans[1][0], row, spans[1][1], 18, frozen, () -> !animates, () -> {
                animates = false;
                preview();
            }));
            if (playerTrophy) addPoseButton(spans[2], row);
        }
        if (statueable) {
            Component icon = Component.translatable("trophypedestals.pedestal.screen.display.icon");
            Component figure = Component.translatable("trophypedestals.pedestal.screen.display.statue");
            int[][] spans = split(x, column, icon, figure, widestPose());
            addRenderableWidget(new Choice(spans[0][0], row, spans[0][1], 18, icon, () -> !statue, () -> {
                statue = false;
                preview();
            }));
            addRenderableWidget(new Choice(spans[1][0], row, spans[1][1], 18, figure, () -> statue, () -> {
                statue = true;
                preview();
            }));
            addPoseButton(spans[2], row);
        }
        // The animation its mob or statue plays: a button opening the list of what it can.
        if (!animations.isEmpty()) {
            animationButton = addRenderableWidget(new Choice(x, top + section(1, false) + 12, column, 18, animationLabel(), () -> false, () -> {
                listOpen = !listOpen;
                listScroll = Mth.clamp(indexOf(animation) - LIST_ROWS / 2, 0, Math.max(0, animations.size() - LIST_ROWS));
            }));
        }
    }

    // A display pedestal's: its text and redstone choices, then Reset and Done.
    private void initCompact() {
        int column = COMPACT_WIDTH - 2 * MARGIN, x = left + MARGIN;
        int[][] spans;
        // (When its text shows only if the server lets players change Text settings.)
        if (TrophyPedestalsConfig.PEDESTAL_TEXT_SETTINGS.get()) {
            PedestalBlockEntity.TextMode[] modes = PedestalBlockEntity.TextMode.values();
            Component[] modeLabels = new Component[modes.length];
            for (int i = 0; i < modes.length; i++) modeLabels[i] = Component.translatable("trophypedestals.pedestal.screen.text." + modes[i].getSerializedName());
            spans = split(x, column, modeLabels);
            for (int i = 0; i < modes.length; i++) {
                PedestalBlockEntity.TextMode mode = modes[i];
                addRenderableWidget(new Choice(spans[i][0], top + ROW_1 + 12, spans[i][1], 18, modeLabels[i], () -> textMode == mode, () -> {
                    textMode = mode;
                    preview();
                }));
            }
        }
        PedestalBlockEntity.RedstoneMode[] redstoneModes = PedestalBlockEntity.RedstoneMode.values();
        Component[] redstoneLabels = new Component[redstoneModes.length];
        for (int i = 0; i < redstoneModes.length; i++) redstoneLabels[i] = Component.translatable("trophypedestals.pedestal.screen.redstone." + redstoneModes[i].getSerializedName());
        spans = split(x, column, redstoneLabels);
        for (int i = 0; i < redstoneModes.length; i++) {
            PedestalBlockEntity.RedstoneMode mode = redstoneModes[i];
            addRenderableWidget(new Choice(spans[i][0], top + COMPACT_ROW_2 + 12, spans[i][1], 18, redstoneLabels[i], () -> redstone == mode, () -> {
                redstone = mode;
                preview();
            }));
        }
        int half = (column - 2) / 2;
        addRenderableWidget(new Choice(x, top + COMPACT_ROW_3 + 12, half, 18, Component.translatable("trophypedestals.pedestal.screen.on"),
                () -> sounds, () -> {
                    sounds = true;
                    preview();
                }));
        addRenderableWidget(new Choice(x + half + 2, top + COMPACT_ROW_3 + 12, half, 18, Component.translatable("trophypedestals.pedestal.screen.off"),
                () -> !sounds, () -> {
                    sounds = false;
                    preview();
                }));
        lightChoices(x, column, top + COMPACT_ROW_4 + 12);
        half = (column - 4) / 2;
        addRenderableWidget(new Choice(x, top + COMPACT_HEIGHT - 28, half, 18, Component.translatable("trophypedestals.pedestal.screen.reset"), () -> false, () -> {
            if (TrophyPedestalsConfig.PEDESTAL_TEXT_SETTINGS.get()) textMode = PedestalBlockEntity.TextMode.LOOKING;
            redstone = PedestalBlockEntity.RedstoneMode.IGNORE;
            sounds = true;
            light = true;
            preview();
        }));
        addRenderableWidget(new Choice(x + half + 4, top + COMPACT_HEIGHT - 28, half, 18, CommonComponents.GUI_DONE, () -> false, this::onClose));
    }

    // The showcase light's On and Off, side by side across the column.
    private void lightChoices(int x, int column, int y) {
        int half = (column - 2) / 2;
        addRenderableWidget(new Choice(x, y, half, 18, Component.translatable("trophypedestals.pedestal.screen.on"), () -> light, () -> {
            light = true;
            preview();
        }));
        addRenderableWidget(new Choice(x + half + 2, y, half, 18, Component.translatable("trophypedestals.pedestal.screen.off"), () -> !light, () -> {
            light = false;
            preview();
        }));
    }

    // A button's tooltip: what its option does, from the lang key's ".tooltip".
    private static void tip(AbstractWidget widget, String key) {
        widget.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip")));
    }

    /**
     * A row of buttons across {@code width} from {@code x}, 2 apart, as each one's {x, width}: equal widths when every label
     * fits in its share; otherwise each as wide as its label (with room either side), the space left shared out evenly,
     * so the row still fills the width and no label is cut or shrunk. The last button takes any leftover pixel, so its
     * right edge lines up with the column's.
     */
    private int[][] split(int x, int width, Component... labels) {
        int count = labels.length, room = width - 2 * (count - 1);
        int[] wide = new int[count];
        int needed = 0;
        boolean even = true;
        for (int i = 0; i < count; i++) {
            wide[i] = font.width(labels[i]) + 2 * LABEL_PADDING;
            needed += wide[i];
            if (wide[i] > room / count) even = false;
        }
        int spare = Math.max(0, room - needed);
        int[][] spans = new int[count][2];
        int at = x;
        for (int i = 0; i < count; i++) {
            int w = even ? room / count : wide[i] + spare / count;
            if (i == count - 1 && (even || spare > 0)) w = x + width - at;
            spans[i][0] = at;
            spans[i][1] = w;
            at += w + 2;
        }
        return spans;
    }

    // The statue's pose: one button showing the pose, going on to the next with each press.
    private void addPoseButton(int[] span, int y) {
        Choice[] button = new Choice[1];
        button[0] = addRenderableWidget(new Choice(span[0], y, span[1], 18, poseName(statuePose), () -> false, () -> {
            statuePose = statuePose.next();
            button[0].setMessage(poseName(statuePose));
            preview();
        }));
    }

    private static Component poseName(PedestalBlockEntity.StatuePose pose) {
        return Component.translatable("trophypedestals.pedestal.screen.statue_pose." + pose.getSerializedName());
    }

    // The widest pose's name, so the pose button fits whichever it shows.
    private Component widestPose() {
        Component widest = poseName(PedestalBlockEntity.StatuePose.STANDING);
        for (PedestalBlockEntity.StatuePose pose : PedestalBlockEntity.StatuePose.values()) {
            if (font.width(poseName(pose)) > font.width(widest)) widest = poseName(pose);
        }
        return widest;
    }

    // The animation button's label: the chosen animation's name, and an arrow for the list.
    private Component animationLabel() {
        return MobAnimations.name(animation).copy().append(" \u25BC");
    }

    private int indexOf(String id) {
        for (int i = 0; i < animations.size(); i++) {
            if (animations.get(i).id().equals(id)) return i;
        }
        return 0;
    }

    private void chooseAnimation(String id) {
        animation = id;
        // Choosing one sets the mob moving, if it was frozen.
        animates = true;
        listOpen = false;
        if (animationButton != null) animationButton.setMessage(animationLabel());
        preview();
    }

    // Where the open list is: under its button, or over it if it would run off the screen; {x, y, width, rows}.
    private int[] listBounds() {
        int rows = Math.min(LIST_ROWS, animations.size());
        int height = rows * LIST_ROW + 2;
        int x = animationButton.getX(), y = animationButton.getY() + animationButton.getHeight() + 1;
        if (y + height > this.height) y = animationButton.getY() - 1 - height;
        return new int[]{x, y, animationButton.getWidth(), rows};
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // The tab's widgets, clipped to the scrolling part (and not lit by a mouse outside it), and its scroll bar.
        if (!scrolled.isEmpty()) {
            boolean in = inView(mouseY);
            graphics.enableScissor(left, top + VIEW_TOP, left + panelWidth(), top + VIEW_BOTTOM);
            for (AbstractWidget widget : scrolled.keySet()) {
                if (widget.visible) widget.render(graphics, in ? mouseX : -1, in ? mouseY : -1, partialTick);
            }
            graphics.disableScissor();
            if (maxScroll > 0) scrollBar(graphics, mouseX, mouseY);
        }
        if (!listOpen || animationButton == null) return;
        // Over everything else: a dark navy box with a gold edge, the chosen animation in pale gold, the one under the mouse lit.
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        int[] list = listBounds();
        int x = list[0], y = list[1], width = list[2], rows = list[3];
        graphics.fill(x - 1, y - 1, x + width + 1, y + rows * LIST_ROW + 3, GOLD);
        graphics.fill(x, y, x + width, y + rows * LIST_ROW + 2, LIST);
        for (int row = 0; row < rows; row++) {
            int index = listScroll + row;
            MobAnimations.Animation entry = animations.get(index);
            int rowY = y + 1 + row * LIST_ROW;
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + LIST_ROW;
            if (hovered) graphics.fill(x + 1, rowY, x + width - 1, rowY + LIST_ROW, LIST_HOVER);
            int color = entry.id().equals(animation) ? PALE_GOLD : hovered ? 0xFFFFFFFF : TEXT;
            graphics.drawString(font, entry.name(), x + 2 + LABEL_PADDING, rowY + (LIST_ROW - 8) / 2, color);
        }
        // A scroll bar when there are more than fit.
        if (animations.size() > rows) {
            int track = rows * LIST_ROW, thumb = Math.max(8, track * rows / animations.size());
            int thumbY = y + 1 + (track - thumb) * listScroll / (animations.size() - rows);
            graphics.fill(x + width - 3, thumbY, x + width - 1, thumbY + thumb, PALE_GOLD);
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listOpen && animationButton != null) {
            int[] list = listBounds();
            double x = mouseX, y = mouseY;
            if (x >= list[0] && x < list[0] + list[2] && y >= list[1] + 1 && y < list[1] + 1 + list[3] * LIST_ROW) {
                chooseAnimation(animations.get(listScroll + (int) ((y - list[1] - 1) / LIST_ROW)).id());
            } else {
                // A click anywhere else (its button included) only closes the list.
                listOpen = false;
            }
            return true;
        }
        // The scroll bar: its thumb is taken where it's clicked; the groove above or below it moves the thumb's middle there.
        if (button == 0 && overBar(mouseX, mouseY)) {
            int ty = thumbY(), th = thumbHeight();
            dragOffset = mouseY >= ty && mouseY < ty + th ? (int) mouseY - ty : th / 2;
            dragThumb(mouseY);
            return true;
        }
        // A click above or below the scrolling part doesn't reach the tab's widgets clipped there.
        if (!scrolled.isEmpty() && !inView(mouseY)) {
            List<AbstractWidget> shown = scrolled.keySet().stream().filter(widget -> widget.visible).toList();
            shown.forEach(widget -> widget.visible = false);
            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            shown.forEach(widget -> widget.visible = true);
            return handled;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (listOpen && animations.size() > LIST_ROWS) {
            listScroll = Mth.clamp(listScroll - (int) Math.signum(scrollY), 0, animations.size() - LIST_ROWS);
            return true;
        }
        if (!listOpen && maxScroll > 0 && scrollY != 0.0 && mouseX >= left && mouseX < left + panelWidth() && mouseY >= top && mouseY < top + panelHeight()) {
            scroll -= (int) Math.signum(scrollY) * SCROLL_STEP;
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Escape closes an open list before the screen.
        if (listOpen && keyCode == 256) {
            listOpen = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // (Only the settings the server lets players change.)
    private void reset() {
        if (TrophyPedestalsConfig.PEDESTAL_DISPLAY_SETTINGS.get()) {
            scale = 1.0F;
            lift = 0.0F;
            yaw = 0.0F;
            pitch = 0.0F;
            roll = 0.0F;
            crownScale = 1.0F;
            crownX = 0.0F;
            crownY = 0.0F;
            crownSliders.forEach(ValueSlider::sync);
        }
        if (TrophyPedestalsConfig.PEDESTAL_TEXT_SETTINGS.get()) {
            textMode = PedestalBlockEntity.TextMode.LOOKING;
            textLift = 0.0F;
            textScale = 1.0F;
        }
        if (TrophyPedestalsConfig.PEDESTAL_POSE_SETTINGS.get()) {
            animates = true;
            statue = false;
            statuePose = PedestalBlockEntity.StatuePose.STANDING;
            animation = PedestalBlockEntity.IDLE;
            if (animationButton != null) animationButton.setMessage(animationLabel());
        }
        spins = true;
        effects = true;
        sounds = true;
        redstone = PedestalBlockEntity.RedstoneMode.IGNORE;
        crown = true;
        light = true;
        // (Only the showing tab's sliders exist.)
        if (textSlider != null) textSlider.sync();
        if (textSizeSlider != null) textSizeSlider.sync();
        rotationSliders.forEach(AngleSlider::sync);
        if (slider != null) slider.sync();
        if (heightSlider != null) heightSlider.sync();
        preview();
    }

    // Shows the change on the pedestal right away; the server's word comes back when the screen closes.
    private void preview() {
        PedestalBlockEntity pedestal = standIn != null ? standIn
                : minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof PedestalBlockEntity placed ? placed : null;
        if (pedestal != null) {
            pedestal.setTextMode(textMode);
            pedestal.setScale(scale);
            pedestal.setHeight(lift);
            pedestal.setSpins(spins);
            pedestal.setTextHeight(textLift);
            pedestal.setTextScale(textScale);
            pedestal.setCrown(crown);
            pedestal.setCrownPlacement(crownScale, crownX, crownY);
            pedestal.setLight(light);
            pedestal.setRotation(yaw, pitch, roll);
            pedestal.setEffects(effects);
            pedestal.setRedstone(redstone);
            pedestal.setSounds(sounds);
            pedestal.setAnimates(animates);
            pedestal.setStatue(statue);
            pedestal.setStatuePose(statuePose);
            pedestal.setAnimation(animation);
        }
    }

    @Override
    public void removed() {
        if (fixedSize()) {
            Network.sendToServer(new AdjustDisplayedTrophyPayload(pos, slot, textMode, spins, yaw, pitch, roll, effects, redstone, sounds, animates, statue, statuePose, animation, crown, light));
            return;
        }
        Network.sendToServer(new AdjustPedestalPayload(pos, textMode, scale, lift, spins, textLift, yaw, pitch, roll, effects, redstone, sounds, animates, statue, statuePose, animation, textScale, crown, crownScale, crownX, crownY, light, access));
    }

    // The panel, its header and its section labels (with their values, right-aligned before their reset buttons), under
    // the widgets.
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        graphics.blitSprite(PANEL, left, top, panelWidth(), panelHeight());
        graphics.blitSprite(GEMS_TOP, left + (panelWidth() - GEMS_WIDTH) / 2, top, GEMS_WIDTH, FRAME);
        graphics.blitSprite(GEMS, left + (panelWidth() - GEMS_WIDTH) / 2, top + panelHeight() - FRAME, GEMS_WIDTH, FRAME);
        // The header: the pedestal, the title in gold and its owner under it, over a groove.
        graphics.renderItem(icon, left + MARGIN + 2, top + 12);
        graphics.drawString(font, title, left + MARGIN + 24, top + 12, GOLD);
        graphics.drawString(font, owner, left + MARGIN + 24, top + 23, LABEL);
        graphics.fill(left + MARGIN, top + 36, left + panelWidth() - MARGIN, top + 37, GROOVE_DARK);
        graphics.fill(left + MARGIN, top + 37, left + panelWidth() - MARGIN, top + 38, GROOVE_LIT);
        if (!trophy) {
            if (TrophyPedestalsConfig.PEDESTAL_TEXT_SETTINGS.get()) label(graphics, "trophypedestals.pedestal.screen.text", left + MARGIN, ROW_1, null, 0);
            label(graphics, "trophypedestals.pedestal.screen.redstone", left + MARGIN, COMPACT_ROW_2, null, 0);
            label(graphics, "trophypedestals.pedestal.screen.sounds", left + MARGIN, COMPACT_ROW_3, null, 0);
            label(graphics, "trophypedestals.pedestal.screen.light", left + MARGIN, COMPACT_ROW_4, null, 0);
            return;
        }
        int column = TABBED_WIDTH - 2 * MARGIN, x = left + MARGIN, valueRight = column - RESET - 4;
        // A groove under the tabs, then the showing tab's section labels.
        graphics.fill(x, top + TAB_ROW + 22, x + column, top + TAB_ROW + 23, GROOVE_DARK);
        graphics.fill(x, top + TAB_ROW + 23, x + column, top + TAB_ROW + 24, GROOVE_LIT);
        graphics.enableScissor(left, top + VIEW_TOP, left + panelWidth(), top + VIEW_BOTTOM);
        tabLabels(graphics, x, valueRight);
        graphics.disableScissor();
    }

    private void tabLabels(GuiGraphics graphics, int x, int valueRight) {
        switch (tab()) {
            case DISPLAY -> {
                if (fixedSize()) {
                    rotationLabel(graphics, x, valueRight, section(0, false));
                    return;
                }
                label(graphics, "trophypedestals.pedestal.screen.size", x, section(0, false), Component.literal(size(scale)), valueRight);
                label(graphics, "trophypedestals.pedestal.screen.height", x, section(1, true),
                        Component.translatable("trophypedestals.pedestal.screen.height_value", height(lift)), valueRight);
                rotationLabel(graphics, x, valueRight, section(2, true));
                if (crowned) {
                    label(graphics, "trophypedestals.pedestal.screen.crown", x, section(3, true),
                            Component.translatable("trophypedestals.pedestal.screen.crown_value", size(crownScale), height(crownX), height(crownY)), valueRight);
                }
            }
            case TEXT -> {
                label(graphics, "trophypedestals.pedestal.screen.text", x, section(0, false), null, 0);
                if (fixedSize()) return;
                label(graphics, "trophypedestals.pedestal.screen.text_height", x, section(1, false),
                        Component.translatable("trophypedestals.pedestal.screen.height_value", height(textLift)), valueRight);
                label(graphics, "trophypedestals.pedestal.screen.text_size", x, section(2, false), Component.literal(size(textScale)), valueRight);
            }
            case BEHAVIOR -> {
                label(graphics, "trophypedestals.pedestal.screen.toggles", x, section(0, false), null, 0);
                label(graphics, "trophypedestals.pedestal.screen.light", x, section(1, false), null, 0);
                label(graphics, "trophypedestals.pedestal.screen.redstone", x, section(2, false), null, 0);
            }
            case POSE -> {
                if (mob) label(graphics, "trophypedestals.pedestal.screen.pose", x, section(0, false), null, 0);
                if (statueable) label(graphics, "trophypedestals.pedestal.screen.display", x, section(0, false), null, 0);
                if (!animations.isEmpty()) label(graphics, "trophypedestals.pedestal.screen.animation", x, section(1, false), null, 0);
            }
        }
    }

    // "Rotation", with its yaw, pitch and roll in whole degrees.
    private void rotationLabel(GuiGraphics graphics, int x, int valueRight, int row) {
        label(graphics, "trophypedestals.pedestal.screen.rotation", x, row, Component.translatable("trophypedestals.pedestal.screen.rotation_value",
                Math.round(yaw), Math.round(pitch), Math.round(roll)), valueRight);
    }

    // A section's label, a "?" after it that shows what the option does while hovered (from the key's ".tooltip"), and
    // its value right-aligned at {@code valueRight} from the label's left.
    private void label(GuiGraphics graphics, String key, int x, int row, @Nullable Component value, int valueRight) {
        Component text = Component.translatable(key).withStyle(ChatFormatting.BOLD);
        // (A trophy's labels scroll with their tab.)
        if (trophy) row -= scroll;
        graphics.drawString(font, text, x, top + row, LABEL);
        int iconX = x + font.width(text) + 3, iconY = top + row - 1;
        boolean hovered = (!trophy || inView(mouseY)) && mouseX >= iconX && mouseX < iconX + INFO_SIZE && mouseY >= iconY && mouseY < iconY + INFO_SIZE;
        graphics.blitSprite(hovered ? INFO_HOVER : INFO, iconX, iconY, INFO_SIZE, INFO_SIZE);
        if (hovered) setTooltipForNextRenderPass(font.split(Component.translatable(key + ".tooltip"), TOOLTIP_WIDTH));
        if (value != null) graphics.drawString(font, value, x + valueRight - font.width(value), top + row, PALE_GOLD);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // "2.5x": a size with no trailing zeros.
    private static String size(float scale) {
        String text = String.format(Locale.ROOT, "%.2f", scale).replaceAll("0+$", "").replaceAll("\\.$", "");
        return text + "×";
    }

    // "+1.25": a height with its sign and no trailing zeros.
    private static String height(float height) {
        String text = String.format(Locale.ROOT, "%.2f", Math.abs(height)).replaceAll("0+$", "").replaceAll("\\.$", "");
        return (height > 0.0F ? "+" : height < 0.0F ? "-" : "") + text;
    }

    // A size from the slider's place, half to twenty times, finer at the small end: in steps of 0.05 up to 2, 0.1 up to
    // 5, 0.25 up to 10, then 0.5.
    private static float sizeAt(double value) {
        double min = PedestalBlockEntity.MIN_SCALE, max = PedestalBlockEntity.MAX_SCALE;
        float raw = (float) (min * Math.pow(max / min, value));
        float step = raw < 2.0F ? 0.05F : raw < 5.0F ? 0.1F : raw < 10.0F ? 0.25F : 0.5F;
        return Mth.clamp(Math.round(raw / step) * step, PedestalBlockEntity.MIN_SCALE, PedestalBlockEntity.MAX_SCALE);
    }

    private static double valueOf(float scale) {
        double min = PedestalBlockEntity.MIN_SCALE, max = PedestalBlockEntity.MAX_SCALE;
        return Math.log(scale / min) / Math.log(max / min);
    }

    private static Component accessLabel(PedestalBlockEntity.Access access) {
        return Component.translatable("trophypedestals.pedestal.screen.access." + access.getSerializedName());
    }

    /** A choice: a slate button, gold-trimmed while chosen. */
    private final class Choice extends AbstractButton {
        private final BooleanSupplier selected;
        private Runnable action;

        Choice(int x, int y, int width, int height, Component label, BooleanSupplier selected, Runnable action) {
            super(x, y, width, height, label);
            this.selected = selected;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean chosen = selected.getAsBoolean();
            ResourceLocation sprite = chosen ? SEGMENT_SELECTED : isHoveredOrFocused() ? SEGMENT_HOVER : SEGMENT;
            graphics.blitSprite(sprite, getX(), getY(), width, height);
            int color = chosen ? PALE_GOLD : isHoveredOrFocused() ? 0xFFFFFFFF : TEXT;
            graphics.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + (height - 8) / 2, color);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** A small square button that puts the slider beside it back to its usual value, marked with a turning arrow. */
    private final class ResetButton extends AbstractButton {
        private final Runnable action;

        ResetButton(int x, int y, Runnable action) {
            super(x, y, RESET, RESET, Component.translatable("trophypedestals.pedestal.screen.reset_value"));
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blitSprite(isHoveredOrFocused() ? SEGMENT_HOVER : SEGMENT, getX(), getY(), width, height);
            graphics.blitSprite(RESET_ICON, getX() + 3, getY() + 3, 10, 10);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** A slider drawn as a groove filling with gold up to a gem knob. */
    private abstract static class GemSlider extends AbstractSliderButton {
        GemSlider(int x, int y, int width, double value) {
            super(x, y, width, 13, Component.empty(), value);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int trackY = getY() + (height - 6) / 2;
            graphics.blitSprite(TRACK, getX(), trackY, width, 6);
            int knob = getX() + (int) Math.round(value * (width - 9));
            // Filled up to the knob's middle, and all the way at the top of the range.
            int filled = value >= 1.0 ? width : knob + 5 - getX();
            if (filled >= 4) graphics.blitSprite(TRACK_FILL, getX(), trackY, filled, 6);
            graphics.blitSprite(isHoveredOrFocused() ? HANDLE_HOVER : HANDLE, knob, getY(), 9, 13);
        }
    }

    /** The display height, in steps of 0.05 blocks. */
    private final class HeightSlider extends GemSlider {
        HeightSlider(int x, int y, int width) {
            super(x, y, width, (lift - PedestalBlockEntity.MIN_HEIGHT) / (PedestalBlockEntity.MAX_HEIGHT - PedestalBlockEntity.MIN_HEIGHT));
            updateMessage();
        }

        void sync() {
            value = (lift - PedestalBlockEntity.MIN_HEIGHT) / (PedestalBlockEntity.MAX_HEIGHT - PedestalBlockEntity.MIN_HEIGHT);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("trophypedestals.pedestal.screen.height_narration", height(lift)));
        }

        @Override
        protected void applyValue() {
            float raw = Mth.lerp((float) value, PedestalBlockEntity.MIN_HEIGHT, PedestalBlockEntity.MAX_HEIGHT);
            lift = Math.round(raw * 20.0F) / 20.0F;
            preview();
        }
    }

    /**
     * An angle from {@code min} to {@code max} degrees in steps of 15, read and set through the screen's own fields (its
     * yaw, pitch or roll). Yaw goes up to 360 at the end (the same way as 0, but where the slider says it is).
     */
    private final class AngleSlider extends GemSlider {
        private final float min, max;
        private final Supplier<Float> getter;
        private final Consumer<Float> setter;
        private final String narration;

        AngleSlider(int x, int y, int width, float min, float max, Supplier<Float> getter, Consumer<Float> setter, String narration) {
            super(x, y, width, (getter.get() - min) / (max - min));
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
            this.narration = narration;
            updateMessage();
        }

        void sync() {
            value = (getter.get() - min) / (max - min);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(narration, Math.round(getter.get())));
        }

        @Override
        protected void applyValue() {
            float raw = Mth.lerp((float) value, min, max);
            setter.accept(Mth.clamp(Math.round(raw / 15.0F) * 15.0F, min, max));
            preview();
        }
    }

    /** The text's height, in steps of 0.05 blocks. */
    private final class TextHeightSlider extends GemSlider {
        TextHeightSlider(int x, int y, int width) {
            super(x, y, width, (textLift - PedestalBlockEntity.MIN_TEXT_HEIGHT) / (PedestalBlockEntity.MAX_TEXT_HEIGHT - PedestalBlockEntity.MIN_TEXT_HEIGHT));
            updateMessage();
        }

        void sync() {
            value = (textLift - PedestalBlockEntity.MIN_TEXT_HEIGHT) / (PedestalBlockEntity.MAX_TEXT_HEIGHT - PedestalBlockEntity.MIN_TEXT_HEIGHT);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("trophypedestals.pedestal.screen.text_height_narration", height(textLift)));
        }

        @Override
        protected void applyValue() {
            float raw = Mth.lerp((float) value, PedestalBlockEntity.MIN_TEXT_HEIGHT, PedestalBlockEntity.MAX_TEXT_HEIGHT);
            textLift = Math.round(raw * 20.0F) / 20.0F;
            preview();
        }
    }

    /** The text's size, half to three times, in steps of 0.05. */
    private final class TextSizeSlider extends GemSlider {
        TextSizeSlider(int x, int y, int width) {
            super(x, y, width, (textScale - PedestalBlockEntity.MIN_TEXT_SCALE) / (PedestalBlockEntity.MAX_TEXT_SCALE - PedestalBlockEntity.MIN_TEXT_SCALE));
            updateMessage();
        }

        void sync() {
            value = (textScale - PedestalBlockEntity.MIN_TEXT_SCALE) / (PedestalBlockEntity.MAX_TEXT_SCALE - PedestalBlockEntity.MIN_TEXT_SCALE);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("trophypedestals.pedestal.screen.text_size_narration", size(textScale)));
        }

        @Override
        protected void applyValue() {
            float raw = Mth.lerp((float) value, PedestalBlockEntity.MIN_TEXT_SCALE, PedestalBlockEntity.MAX_TEXT_SCALE);
            textScale = Math.round(raw * 20.0F) / 20.0F;
            preview();
        }
    }

    /**
     * A value from {@code min} to {@code max} in steps of 0.05, read and set through the screen's own fields (a crown's
     * size or offsets); its narration key takes the value as a size ("1.5×") or a signed distance ("+0.25").
     */
    private final class ValueSlider extends GemSlider {
        private final float min, max;
        private final Supplier<Float> getter;
        private final Consumer<Float> setter;
        private final String narration;
        private final boolean isSize;

        ValueSlider(int x, int y, int width, float min, float max, Supplier<Float> getter, Consumer<Float> setter, String narration, boolean isSize) {
            super(x, y, width, (getter.get() - min) / (max - min));
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
            this.narration = narration;
            this.isSize = isSize;
            updateMessage();
        }

        void sync() {
            value = (getter.get() - min) / (max - min);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float now = getter.get();
            setMessage(Component.translatable(narration, isSize ? size(now) : height(now)));
        }

        @Override
        protected void applyValue() {
            setter.accept(Math.round(Mth.lerp((float) value, min, max) * 20.0F) / 20.0F);
            preview();
        }
    }

    /** The display size: a groove filling with gold up to a gem knob. */
    private final class SizeSlider extends GemSlider {
        SizeSlider(int x, int y, int width) {
            super(x, y, width, valueOf(scale));
            updateMessage();
        }

        void sync() {
            value = valueOf(scale);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("trophypedestals.pedestal.screen.size_narration", size(scale)));
        }

        @Override
        protected void applyValue() {
            scale = sizeAt(value);
            preview();
        }
    }
}
