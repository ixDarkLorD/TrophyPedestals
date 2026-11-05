package net.ixdarklord.trophypedestals.api;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Where a pedestal's Team access finds teams: scoreboard teams ({@code /team}) by default. A mod with teams of its own
 * (Tierwise, which follows FTB Teams) can set another source.
 */
public final class PedestalTeams {
    /** Where teams come from: each answers with a key that's the same for every member of a team, or null for none. */
    public interface Source {
        /** The team the player is in, or null. */
        @Nullable String teamOf(ServerPlayer player);

        /** The team of a player who may be offline (a pedestal's owner), or null. By default only known while online. */
        default @Nullable String teamOf(MinecraftServer server, UUID player) {
            ServerPlayer online = server.getPlayerList().getPlayer(player);
            return online == null ? null : teamOf(online);
        }
    }

    /** Scoreboard teams. Only known for players who are online. */
    public static final Source VANILLA = player -> {
        PlayerTeam team = player.getTeam();
        return team == null ? null : "vanilla:" + team.getName();
    };

    private static volatile Source source = VANILLA;

    private PedestalTeams() {}

    /** Uses this source for teams from now on. */
    public static void setSource(Source teams) {
        source = teams;
    }

    /** Whether the player is on the same team as the player {@code other} (who may be offline). */
    public static boolean sameTeam(ServerPlayer player, UUID other) {
        String team = source.teamOf(player);
        if (team == null) return false;
        String theirs = source.teamOf(player.level().getServer(), other);
        return team.equals(theirs);
    }
}
