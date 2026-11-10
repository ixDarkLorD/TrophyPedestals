package net.ixdarklord.trophypedestals.feat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads {@code data/<namespace>/trophypedestals/feats/*.json} on world load and {@code /reload}, and hands them to
 * {@link Feats}. A file that can't be read is logged with its pack and path, and skipped.
 */
public final class FeatLoader extends SimplePreparableReloadListener<Map<Identifier, Feat>> {
    public static final Identifier ID = TrophyPedestalsMod.id("feats");
    private static final FileToIdConverter FEATS = FileToIdConverter.json("trophypedestals/feats");

    @Override
    protected Map<Identifier, Feat> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, Feat> feats = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : FEATS.listMatchingResources(resourceManager).entrySet()) {
            Identifier id = FEATS.fileToId(entry.getKey());
            String source = entry.getValue().sourcePackId() + ": data/" + entry.getKey().getNamespace() + "/" + entry.getKey().getPath();
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                DataResult<Feat> feat = Feat.CODEC.parse(JsonOps.INSTANCE, json);
                feat.error().ifPresent(problem -> TrophyPedestalsMod.LOGGER.warn("Skipping the trophy {}: {}", source, problem.message()));
                feat.result().ifPresent(read -> feats.put(id, read));
            } catch (Exception e) {
                TrophyPedestalsMod.LOGGER.warn("Skipping the trophy {}: {}", source, e.getMessage());
            }
        }
        return feats;
    }

    @Override
    protected void apply(Map<Identifier, Feat> feats, ResourceManager resourceManager, ProfilerFiller profiler) {
        Feats.load(feats);
    }
}
