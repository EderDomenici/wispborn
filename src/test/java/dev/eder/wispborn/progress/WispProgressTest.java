package dev.eder.wispborn.progress;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WispProgressTest {

    @Test
    void codecRoundTrip() {
        WispProgress original = new WispProgress(450);
        JsonElement encoded = WispProgress.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        assertEquals(original, WispProgress.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void legacySaveWithLevelFieldStillLoads() {
        JsonElement legacy = JsonParser.parseString("{\"total_ep\": 450, \"level\": 2}");
        assertEquals(new WispProgress(450), WispProgress.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow());
    }

    @Test
    void levelFollowsTheCurrentCurve() {
        // O nível vem sempre do EP: trocar a curva no config muda o nível sem precisar ganhar EP.
        int totalEp = 450;
        assertEquals(1, LevelCurve.levelFor(totalEp, List.of(500, 1000)));
        assertEquals(3, LevelCurve.levelFor(totalEp, List.of(100, 400, 1000)));
    }
}
