package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerTickEvents;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.network.OpenLeaderboardPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Leaderboard pedestals: a display pedestal given a Leaderboard Crest shows whoever on the server leads one statistic
 * (most mob kills, longest played...), as their statue in their own skin, with the board's name, theirs and their count.
 * Everyone who has played counts, online or not, and it updates live.
 *
 * <p>Kept cheap however many pedestals there are: the players who are offline are read from their saved statistics once,
 * as the server starts (and taken from the game as each logs out); once a second, the server looks at the online
 * players' counts for only the boards some loaded pedestal shows, and keeps each board's leader; each pedestal just
 * compares its leader with that, and sends an update only when it changes.
 */
public final class Leaderboards {
    /**
     * What a leaderboard ranks: one of the game's own statistics (its "custom" ones: mob kills, distance walked, time
     * played... and any a mod adds), by id. A few have titles of their own ("Most Mob Kills"); the rest are "Top: <its
     * name>".
     */
    public record Board(ResourceLocation id) implements StringRepresentable {
        public static final Board MOB_KILLS = new Board(Stats.MOB_KILLS);
        // The boards with a title of their own, by the name their title's lang key takes (as boards were once saved).
        private static final Map<ResourceLocation, String> TITLED = Map.of(
                Stats.MOB_KILLS, "mob_kills", Stats.PLAYER_KILLS, "player_kills", Stats.DEATHS, "deaths",
                Stats.WALK_ONE_CM, "distance_walked", Stats.PLAY_TIME, "play_time", Stats.FISH_CAUGHT, "fish_caught",
                Stats.JUMP, "jumps", Stats.DAMAGE_DEALT, "damage_dealt", TrophyPedestalsMod.id("mob_trophies_collected"), "mob_trophies");

        public Stat<ResourceLocation> stat() {
            // (The registry's own id, which it finds its statistic by: an equal one isn't enough.)
            ResourceLocation registered = BuiltInRegistries.CUSTOM_STAT.get(id);
            return Stats.CUSTOM.get(registered != null ? registered : id);
        }

        /** Its title on the pedestal: "Most Mob Kills", or "Top: <the statistic's name>". */
        public Component title() {
            String titled = TITLED.get(id);
            return titled != null ? Component.translatable("trophypedestals.leaderboard." + titled)
                    : Component.translatable("trophypedestals.leaderboard.top", Component.translatable("stat." + id.toLanguageKey()));
        }

        /** A count of it, as the game's statistics screen writes it (a distance in km, a time in hours...). */
        public Component format(int value) {
            return Component.literal(stat().format(value));
        }

        @Override
        public String getSerializedName() {
            return id.toString();
        }

        /** The board saved under this name: a statistic's id, or one of the names boards were once saved by. */
        public static @Nullable Board byName(String name) {
            for (Map.Entry<ResourceLocation, String> titled : TITLED.entrySet()) if (titled.getValue().equals(name)) return new Board(titled.getKey());
            ResourceLocation id = ResourceLocation.tryParse(name);
            return id != null && BuiltInRegistries.CUSTOM_STAT.containsKey(id) ? new Board(id) : null;
        }

        /** Every board: those with titles of their own first, then the rest by id. */
        public static List<Board> all() {
            return BuiltInRegistries.CUSTOM_STAT.keySet().stream()
                    .sorted(Comparator.comparing((ResourceLocation id) -> !TITLED.containsKey(id)).thenComparing(ResourceLocation::toString))
                    .map(Board::new).toList();
        }
    }

    /** Who leads a board, and with how much. */
    public record Leader(String name, int value) {
        public static final Codec<Leader> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("Name").forGetter(Leader::name),
                Codec.INT.fieldOf("Value").forGetter(Leader::value)
        ).apply(instance, Leader::new));
    }

    /** How many lead a board: the leader and two runners-up, shown on a leaderboard pedestal's text. */
    public static final int TOP = 3;

    // How often the leaders are worked out, and how often each pedestal looks at its board's, in ticks; and how long a
    // board stays worked out after the last pedestal showing it looked.
    private static final int UPDATE = 20, ACTIVE_FOR = 100;

    // The offline players' counts, by board, and their names; each board's best of them.
    private static final Map<UUID, Map<ResourceLocation, Integer>> OFFLINE = new HashMap<>();
    private static final Map<UUID, String> NAMES = new HashMap<>();
    // (Each board's best offline players, worked out when it's first asked for and kept till someone logs in or out.)
    private static final Map<Board, List<Leader>> OFFLINE_BEST = new HashMap<>();
    // Each board's top players, and when a pedestal last showed it (in server ticks).
    private static final Map<Board, List<Leader>> LEADERS = new HashMap<>();
    private static final Map<Board, Integer> SHOWN_AT = new HashMap<>();
    private static boolean loaded;

    private Leaderboards() {}

    public static void init() {
        ServerTickEvents.END.register(Leaderboards::update);
        // A player logging out: their counts kept as they leave; logging in: counted live instead.
        PlayerEvents.LEAVE.register(player -> {
            if (!loaded) return;
            OFFLINE.put(player.getUUID(), counts(player.getStats()));
            NAMES.put(player.getUUID(), player.getName().getString());
            OFFLINE_BEST.clear();
        });
        PlayerEvents.JOIN.register(player -> {
            if (!loaded || OFFLINE.remove(player.getUUID()) == null) return;
            OFFLINE_BEST.clear();
        });
        ServerLifecycleEvents.STOPPED.register(server -> {
            OFFLINE.clear();
            NAMES.clear();
            OFFLINE_BEST.clear();
            LEADERS.clear();
            SHOWN_AT.clear();
            loaded = false;
        });
    }

    /**
     * A leaderboard pedestal's tick: every {@link #UPDATE} ticks (each on its own beat), it marks its board as shown and
     * takes its leader, if they changed.
     */
    public static void tick(Level level, BlockPos pos, PedestalBlockEntity pedestal) {
        Board board = pedestal.leaderboard();
        if (board == null || (level.getGameTime() + pos.asLong()) % UPDATE != 0 || !(level instanceof ServerLevel server)) return;
        int now = server.getServer().getTickCount();
        boolean fresh = !SHOWN_AT.containsKey(board) || now - SHOWN_AT.get(board) > ACTIVE_FOR;
        SHOWN_AT.put(board, now);
        // (A board no pedestal showed lately is worked out now, rather than waiting for the next update.)
        if (fresh) LEADERS.put(board, top(server.getServer(), board));
        show(pedestal, LEADERS.get(board));
    }

    /** Opens the board picker for this pedestal: every board, and who leads each now. */
    public static void openPicker(ServerPlayer player, BlockPos pos, PedestalBlockEntity pedestal) {
        MinecraftServer server = player.level().getServer();
        List<OpenLeaderboardPayload.Status> boards = new ArrayList<>();
        for (Board board : Board.all()) {
            Leader leader = leader(server, board);
            boards.add(new OpenLeaderboardPayload.Status(board.id(), leader == null ? "" : leader.name(), leader == null ? 0 : leader.value()));
        }
        String current = pedestal.leaderboard() == null ? "" : pedestal.leaderboard().getSerializedName();
        Network.sendToPlayer(player, new OpenLeaderboardPayload(pos, current, boards));
    }

    /** Puts a board's top players on a pedestal (the leader, then the runners-up), if they aren't on it already. */
    public static void show(PedestalBlockEntity pedestal, List<Leader> top) {
        String name = top.isEmpty() ? "" : top.getFirst().name();
        int value = top.isEmpty() ? 0 : top.getFirst().value();
        List<Leader> runnersUp = top.size() > 1 ? List.copyOf(top.subList(1, top.size())) : List.of();
        if (!name.equals(pedestal.leaderName()) || value != pedestal.leaderValue() || !runnersUp.equals(pedestal.runnersUp())) {
            pedestal.setLeader(name, value, runnersUp);
        }
    }

    // Once a second: each shown board's leader, from the online players' counts now and the offline players' best.
    private static void update(MinecraftServer server) {
        int now = server.getTickCount();
        if (now % UPDATE != 0 || SHOWN_AT.isEmpty()) return;
        SHOWN_AT.entrySet().removeIf(entry -> now - entry.getValue() > ACTIVE_FOR);
        for (Board board : SHOWN_AT.keySet()) LEADERS.put(board, top(server, board));
    }

    /** Who leads this board of everyone who has played on the server, or null if no one has any of it. */
    public static @Nullable Leader leader(MinecraftServer server, Board board) {
        List<Leader> top = top(server, board);
        return top.isEmpty() ? null : top.getFirst();
    }

    /**
     * The board's top {@link #TOP} of everyone who has played on the server, most first (those with none of it left
     * out): the online players' counts now, and the offline players' best.
     */
    public static List<Leader> top(MinecraftServer server, Board board) {
        loadOffline(server);
        List<Leader> all = new ArrayList<>(OFFLINE_BEST.computeIfAbsent(board, Leaderboards::bestOffline));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int value = player.getStats().getValue(board.stat());
            if (value > 0) all.add(new Leader(player.getName().getString(), value));
        }
        all.sort(Comparator.comparingInt(Leader::value).reversed());
        return List.copyOf(all.subList(0, Math.min(TOP, all.size())));
    }

    // A player's counts on every board they have any of.
    private static Map<ResourceLocation, Integer> counts(ServerStatsCounter stats) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        for (ResourceLocation id : BuiltInRegistries.CUSTOM_STAT) {
            int value = stats.getValue(Stats.CUSTOM.get(id));
            if (value > 0) counts.put(id, value);
        }
        return counts;
    }

    // A board's best {@link #TOP} of the offline players, most first.
    private static List<Leader> bestOffline(Board board) {
        List<Leader> best = new ArrayList<>();
        for (Map.Entry<UUID, Map<ResourceLocation, Integer>> entry : OFFLINE.entrySet()) {
            int value = entry.getValue().getOrDefault(board.id(), 0);
            String name = NAMES.getOrDefault(entry.getKey(), "");
            if (value > 0 && !name.isEmpty()) best.add(new Leader(name, value));
        }
        best.sort(Comparator.comparingInt(Leader::value).reversed());
        return List.copyOf(best.subList(0, Math.min(TOP, best.size())));
    }

    // The saved statistics of every player who has played, read once (but not of those online, counted live).
    private static void loadOffline(MinecraftServer server) {
        if (loaded) return;
        loaded = true;
        Path folder = server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        if (Files.isDirectory(folder)) {
            try (Stream<Path> files = Files.list(folder)) {
                files.filter(file -> file.getFileName().toString().endsWith(".json")).forEach(file -> {
                    String name = file.getFileName().toString();
                    UUID id;
                    try {
                        id = UUID.fromString(name.substring(0, name.length() - ".json".length()));
                    } catch (IllegalArgumentException e) {
                        return;
                    }
                    if (server.getPlayerList().getPlayer(id) != null) return;
                    String known = Optional.ofNullable(server.getProfileCache()).flatMap(cache -> cache.get(id)).map(GameProfile::getName).orElse("");
                    if (known.isEmpty()) return;
                    OFFLINE.put(id, counts(new ServerStatsCounter(server, file.toFile())));
                    NAMES.put(id, known);
                });
            } catch (IOException e) {
                TrophyPedestalsMod.LOGGER.warn("Couldn't read the players' statistics for the leaderboards", e);
            }
        }
        OFFLINE_BEST.clear();
    }
}
