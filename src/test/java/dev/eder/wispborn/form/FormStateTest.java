package dev.eder.wispborn.form;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormStateTest {

    @Test
    void formTypeSerializesLowercaseNames() {
        assertEquals("wisp", FormType.CODEC.encodeStart(JsonOps.INSTANCE, FormType.WISP).getOrThrow().getAsString());
        assertEquals("host", FormType.CODEC.encodeStart(JsonOps.INSTANCE, FormType.HOST).getOrThrow().getAsString());
    }

    @Test
    void formStateDecodesLegacyHumanAsWisp() {
        assertEquals(FormType.WISP, FormType.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("human")).getOrThrow());

        JsonObject legacyState = new JsonObject();
        legacyState.addProperty("type", "human");
        FormState decoded = FormState.CODEC.parse(JsonOps.INSTANCE, legacyState).getOrThrow();
        assertEquals(FormState.WISP, decoded);

        JsonElement reencoded = FormState.CODEC.encodeStart(JsonOps.INSTANCE, decoded).getOrThrow();
        assertEquals("wisp", reencoded.getAsJsonObject().get("type").getAsString());
    }

    @Test
    void formStateCodecRoundTripWisp() {
        assertRoundTrip(FormState.WISP);
    }

    @Test
    void formStateCodecRoundTripHostCow() {
        FormState hostCow = FormState.host(ResourceLocation.fromNamespaceAndPath("minecraft", "cow"));
        assertRoundTrip(hostCow);
    }

    private static void assertRoundTrip(FormState original) {
        DataResult<JsonElement> encoded = FormState.CODEC.encodeStart(JsonOps.INSTANCE, original);
        assertTrue(encoded.isSuccess(), () -> "Falha ao serializar: " + encoded.error());

        DataResult<FormState> decoded = FormState.CODEC.parse(JsonOps.INSTANCE, encoded.getOrThrow());
        assertTrue(decoded.isSuccess(), () -> "Falha ao desserializar: " + decoded.error());
        assertEquals(original, decoded.getOrThrow());
    }
}
