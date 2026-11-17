package dev.eder.wispborn.form;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WispAnimationsTest {

    @Test
    void wispAnimationJsonContainsExpectedClipsAndLoopSettings() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/assets/wispborn/animations/entity/wisp.animation.json")) {
            assertNotNull(stream, "wisp.animation.json deve estar disponível no classpath");

            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject animations = root.getAsJsonObject("animations");
            assertNotNull(animations, "Objeto 'animations' deve existir no JSON");

            assertTrue(animations.has(WispAnimations.IDLE), "Deve conter " + WispAnimations.IDLE);
            assertTrue(animations.has(WispAnimations.WALK), "Deve conter " + WispAnimations.WALK);
            assertTrue(animations.has(WispAnimations.POSSESS), "Deve conter " + WispAnimations.POSSESS);

            assertTrue(isLooped(animations.getAsJsonObject(WispAnimations.IDLE)), "IDLE deve ter loop: true");
            assertTrue(isLooped(animations.getAsJsonObject(WispAnimations.WALK)), "WALK deve ter loop: true");
            assertFalse(isLooped(animations.getAsJsonObject(WispAnimations.POSSESS)), "POSSESS não deve ter loop");
        }
    }

    private static boolean isLooped(JsonObject clip) {
        JsonElement loop = clip.get("loop");
        return loop != null && loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean() && loop.getAsBoolean();
    }
}
