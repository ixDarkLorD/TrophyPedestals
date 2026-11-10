package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.network.OpenLeaderboardPayload;
import net.ixdarklord.trophypedestals.network.SetLeaderboardPayload;
import net.ixdarklord.trophypedestals.pedestal.Leaderboards;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The board picker a Leaderboard Crest opens: every leaderboard (one for each of the game's statistics), who leads each
 * now and with how much, filtered by a search bar (by a board's title or its leader). Clicking one shows it on the
 * pedestal. The board the pedestal shows now is in gold.
 */
public final class LeaderboardScreen extends Screen {
    private static final ResourceLocation PANEL = TrophyPedestalsMod.id("pedestal/panel");
    private static final int WIDTH = 260, MAX_HEIGHT = 236, MARGIN = 12, ROW = 20, LIST_TOP = 60, EDGE = 4;
    private static final int GOLD = 0xFFF2C94C, LABEL = 0xFF9DA2A7, TEXT = 0xFFDCDCE0, VALUE = 0xFFFFE08A;
    private static final int GROOVE_DARK = 0xFF07090E, GROOVE_LIT = 0xFF343B4E, SLOT = 0xFF0E121A, SLOT_HOVER = 0xFF252C3C;

    private record Row(Leaderboards.Board board, Component title, String leader, int value) {}

    private final BlockPos pos;
    private final String current;
    private final List<Row> rows = new ArrayList<>();
    private final List<Row> shown = new ArrayList<>();
    private EditBox search;
    private int left, top, height2, scroll, maxScroll;

    private LeaderboardScreen(OpenLeaderboardPayload payload) {
        super(Component.translatable("trophypedestals.leaderboard.picker"));
        this.pos = payload.pos();
        this.current = payload.current();
        for (OpenLeaderboardPayload.Status status : payload.boards()) {
            Leaderboards.Board board = new Leaderboards.Board(status.board());
            rows.add(new Row(board, board.title(), status.leader(), status.value()));
        }
    }

    /** Opens the picker the server sent. */
    public static void open(OpenLeaderboardPayload payload) {
        Minecraft.getInstance().setScreen(new LeaderboardScreen(payload));
    }

    @Override
    protected void init() {
        height2 = Math.min(MAX_HEIGHT, height - 2 * EDGE);
        left = Math.max(EDGE, (width - WIDTH) / 2);
        top = Math.max(EDGE, (height - height2) / 2);
        String query = search == null ? "" : search.getValue();
        search = addRenderableWidget(new EditBox(font, left + MARGIN, top + 34, WIDTH - 2 * MARGIN, 16, Component.translatable("trophypedestals.leaderboard.search")));
        search.setHint(Component.translatable("trophypedestals.leaderboard.search").withColor(LABEL));
        search.setValue(query);
        search.setResponder(text -> filter());
        setInitialFocus(search);
        filter();
    }

    // The boards whose title or leader holds what's searched for.
    private void filter() {
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (Row row : rows) {
            if (query.isEmpty() || row.title().getString().toLowerCase(Locale.ROOT).contains(query) || row.leader().toLowerCase(Locale.ROOT).contains(query)) {
                shown.add(row);
            }
        }
        maxScroll = Math.max(0, shown.size() * ROW - listHeight());
        scroll = Mth.clamp(scroll, 0, maxScroll);
    }

    private int listHeight() {
        return height2 - LIST_TOP - MARGIN;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(PANEL, left, top, WIDTH, height2);
        graphics.drawString(font, title, left + MARGIN, top + 12, GOLD);
        graphics.drawString(font, Component.translatable("trophypedestals.leaderboard.count", shown.size(), rows.size()), left + MARGIN, top + 22, LABEL);
        graphics.fill(left + MARGIN, top + LIST_TOP - 5, left + WIDTH - MARGIN, top + LIST_TOP - 4, GROOVE_DARK);
        graphics.fill(left + MARGIN, top + LIST_TOP - 4, left + WIDTH - MARGIN, top + LIST_TOP - 3, GROOVE_LIT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x0 = left + MARGIN, x1 = left + WIDTH - MARGIN, y0 = top + LIST_TOP, bottom = y0 + listHeight();
        graphics.enableScissor(left, y0, left + WIDTH, bottom);
        for (int i = 0; i < shown.size(); i++) {
            int y = y0 + i * ROW - scroll;
            if (y + ROW < y0 || y > bottom) continue;
            Row row = shown.get(i);
            boolean over = mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + ROW - 2 && mouseY >= y0 && mouseY < bottom;
            graphics.fill(x0, y, x1, y + ROW - 2, over ? SLOT_HOVER : SLOT);
            boolean chosen = row.board().getSerializedName().equals(current);
            Component leader = row.leader().isEmpty() ? Component.translatable("trophypedestals.leaderboard.no_leader")
                    : Component.translatable("trophypedestals.info.leader", row.leader(), row.board().format(row.value()));
            int leaderX = x1 - 4 - font.width(leader);
            // Its name, cut with "..." where it would run into its leader (all of it shows while hovered).
            int room = leaderX - GAP - (x0 + 4);
            boolean cut = font.width(row.title()) > room;
            graphics.drawString(font, cut ? fit(row.title(), room) : row.title().getVisualOrderText(), x0 + 4, y + 5, chosen ? GOLD : TEXT);
            if (cut && over) setTooltipForNextRenderPass(row.title());
            graphics.drawString(font, leader, leaderX, y + 5, row.leader().isEmpty() ? LABEL : VALUE);
        }
        graphics.disableScissor();
        if (shown.isEmpty()) graphics.drawCenteredString(font, Component.translatable("trophypedestals.leaderboard.none_found"), left + WIDTH / 2, y0 + 8, LABEL);
    }

    // The gap kept between a board's name and its leader.
    private static final int GAP = 8;

    // The text cut to fit this many pixels, ending in "...".
    private FormattedCharSequence fit(Component text, int room) {
        FormattedText cut = font.substrByWidth(text, Math.max(0, room - font.width(CommonComponents.ELLIPSIS)));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, CommonComponents.ELLIPSIS));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x0 = left + MARGIN, x1 = left + WIDTH - MARGIN, y0 = top + LIST_TOP, bottom = y0 + listHeight();
        if (mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < bottom) {
            int index = (int) ((mouseY - y0 + scroll) / ROW);
            if (index >= 0 && index < shown.size()) {
                Network.sendToServer(new SetLeaderboardPayload(pos, shown.get(index).board().id()));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) (scrollY * ROW), 0, maxScroll);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
