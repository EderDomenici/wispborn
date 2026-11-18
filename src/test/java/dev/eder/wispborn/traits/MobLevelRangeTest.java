package dev.eder.wispborn.traits;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobLevelRangeTest {

    @Test
    void decodesValidRangeAndRejectsInvalidBounds() {
        JsonElement valid = JsonParser.parseString("{\"min\":4,\"max\":8}");
        DataResult<MobLevelRange> decoded = MobLevelRange.CODEC.parse(JsonOps.INSTANCE, valid);
        assertTrue(decoded.isSuccess());
        assertEquals(new MobLevelRange(4, 8), decoded.getOrThrow());

        JsonElement inverted = JsonParser.parseString("{\"min\":8,\"max\":4}");
        assertTrue(MobLevelRange.CODEC.parse(JsonOps.INSTANCE, inverted).isError());

        JsonElement zeroMin = JsonParser.parseString("{\"min\":0,\"max\":2}");
        assertTrue(MobLevelRange.CODEC.parse(JsonOps.INSTANCE, zeroMin).isError());
    }

    @Test
    void mobLevelTableMergesFilesAndSkipsInvalidEntries() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/data/wispborn/wisp_mob_levels/vanilla.json")) {
            assertNotNull(stream);
            JsonElement vanillaJson = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            JsonElement overrideJson = JsonParser.parseString("""
                    {
                      "entries": {
                        "minecraft:chicken": { "min": 5, "max": 7 },
                        "minecraft:zombie": { "min": 10, "max": 2 }
                      }
                    }
                    """);

            Map<ResourceLocation, JsonElement> files = new LinkedHashMap<>();
            files.put(ResourceLocation.fromNamespaceAndPath("wispborn", "vanilla"), vanillaJson);
            files.put(ResourceLocation.fromNamespaceAndPath("wispborn", "override"), overrideJson);

            Map<ResourceLocation, MobLevelRange> parsed = MobLevelTable.parseFiles(files);
            assertEquals(77, parsed.size());
            assertEquals(
                    new MobLevelRange(5, 7),
                    parsed.get(ResourceLocation.fromNamespaceAndPath("minecraft", "chicken"))
            );
            assertEquals(
                    new MobLevelRange(4, 8),
                    parsed.get(ResourceLocation.fromNamespaceAndPath("minecraft", "zombie"))
            );
        }
    }
}
