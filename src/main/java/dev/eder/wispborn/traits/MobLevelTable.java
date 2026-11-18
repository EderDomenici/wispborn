package dev.eder.wispborn.traits;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.eder.wispborn.WispbornMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class MobLevelTable extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "wisp_mob_levels";
    public static final MobLevelTable INSTANCE = new MobLevelTable();

    private static final Gson GSON = new GsonBuilder().create();
    private static final Codec<Map<String, Dynamic<?>>> FILE_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH).fieldOf("entries").codec();

    private volatile Map<ResourceLocation, MobLevelRange> entries = Map.of();

    public MobLevelTable() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> map = new LinkedHashMap<>();
        scanDirectory(resourceManager, DIRECTORY, GSON, map);
        return map;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.entries = parseFiles(files);
        WispbornMod.LOGGER.info("Carregadas {} faixas de nível de mobs em '{}'", this.entries.size(), DIRECTORY);
    }

    public static Map<ResourceLocation, MobLevelRange> parseFiles(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, MobLevelRange> merged = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> fileEntry : files.entrySet()) {
            ResourceLocation fileId = fileEntry.getKey();
            DataResult<Map<String, Dynamic<?>>> parsedFile = FILE_CODEC.parse(JsonOps.INSTANCE, fileEntry.getValue());
            if (parsedFile.isError()) {
                WispbornMod.LOGGER.warn(
                        "Arquivo de faixas de nível inválido '{}': {}",
                        fileId,
                        parsedFile.error().map(DataResult.Error::message).orElse("erro desconhecido")
                );
                continue;
            }

            Map<String, Dynamic<?>> rawEntries = parsedFile.getOrThrow();
            for (Map.Entry<String, Dynamic<?>> entry : rawEntries.entrySet()) {
                String rawKey = entry.getKey();
                DataResult<ResourceLocation> idResult = ResourceLocation.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(rawKey));
                if (idResult.isError()) {
                    WispbornMod.LOGGER.warn(
                            "Entrada de nível inválida em '{}' para chave '{}': {}",
                            fileId,
                            rawKey,
                            idResult.error().map(DataResult.Error::message).orElse("id inválido")
                    );
                    continue;
                }

                DataResult<MobLevelRange> rangeResult = MobLevelRange.CODEC.parse(entry.getValue());
                if (rangeResult.isError()) {
                    WispbornMod.LOGGER.warn(
                            "Entrada de nível inválida em '{}' para chave '{}': {}",
                            fileId,
                            rawKey,
                            rangeResult.error().map(DataResult.Error::message).orElse("faixa inválida")
                    );
                    continue;
                }

                merged.put(idResult.getOrThrow(), rangeResult.getOrThrow());
            }
        }

        return Map.copyOf(merged);
    }

    public Optional<MobLevelRange> rangeFor(EntityType<?> entityType) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return Optional.ofNullable(this.entries.get(id));
    }
}
