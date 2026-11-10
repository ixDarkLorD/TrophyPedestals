package net.ixdarklord.trophypedestals.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The mannequin poses a player saved from the adjust screen's Pose tab, kept on their own computer (in
 * {@code config/trophypedestals/poses.json}) so they can use them on any pedestal, in any world or server.
 */
public final class SavedPoses {
    /** The longest a saved pose's name can be, and the most poses kept. */
    public static final int MAX_NAME = 24, MAX_POSES = 32;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<Pose> poses;

    /** A saved pose: its name, and each of the armor stand's parts' x, y and z (as {@link PedestalBlockEntity#STAND_PARTS}). */
    public record Pose(String name, List<Float> angles) {}

    // How it's written: {"poses": [{"name": ..., "angles": [...]}, ...]}.
    private record Saved(List<Pose> poses) {}

    private SavedPoses() {}

    /** The saved poses, oldest first. */
    public static List<Pose> all() {
        if (poses == null) poses = load();
        return List.copyOf(poses);
    }

    /** Saves this pose under this name, replacing one of the same name; false when there's no room for another. */
    public static boolean save(String name, List<Float> angles) {
        all();
        String trimmed = name.strip();
        int same = indexOf(trimmed);
        if (same < 0 && poses.size() >= MAX_POSES) return false;
        Pose pose = new Pose(trimmed, List.copyOf(angles));
        if (same >= 0) poses.set(same, pose);
        else poses.add(pose);
        write();
        return true;
    }

    public static void delete(String name) {
        all();
        int index = indexOf(name);
        if (index < 0) return;
        poses.remove(index);
        write();
    }

    private static int indexOf(String name) {
        for (int i = 0; i < poses.size(); i++) if (poses.get(i).name().equals(name)) return i;
        return -1;
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(TrophyPedestalsMod.MOD_ID).resolve("poses.json");
    }

    private static List<Pose> load() {
        List<Pose> loaded = new ArrayList<>();
        Path file = file();
        if (!Files.isRegularFile(file)) return loaded;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Saved saved = GSON.fromJson(reader, Saved.class);
            if (saved == null || saved.poses() == null) return loaded;
            // (Only whole poses with names: one edited by hand into something else is left out.)
            for (Pose pose : saved.poses()) {
                if (pose != null && pose.name() != null && !pose.name().isBlank() && pose.angles() != null
                        && pose.angles().size() == PedestalBlockEntity.DEFAULT_STAND_POSE.size() && !pose.angles().contains(null)) {
                    loaded.add(new Pose(pose.name(), List.copyOf(pose.angles())));
                }
            }
        } catch (IOException | JsonParseException e) {
            TrophyPedestalsMod.LOGGER.warn("Couldn't read the saved mannequin poses from {}", file, e);
        }
        return loaded;
    }

    private static void write() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(new Saved(poses), writer);
            }
        } catch (IOException e) {
            TrophyPedestalsMod.LOGGER.warn("Couldn't save the mannequin poses to {}", file, e);
        }
    }
}
