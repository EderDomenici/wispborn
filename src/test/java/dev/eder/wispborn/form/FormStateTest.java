package dev.eder.wispborn.form;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormStateTest {

    @Test
    void formTypeSerializesLowercaseNames() {
        assertEquals("human", FormType.CODEC.encodeStart(JsonOps.INSTANCE, FormType.HUMAN).getOrThrow().getAsString());
        assertEquals("wisp", FormType.CODEC.encodeStart(JsonOps.INSTANCE, FormType.WISP).getOrThrow().getAsString());
        assertEquals("host", FormType.CODEC.encodeStart(JsonOps.INSTANCE, FormType.HOST).getOrThrow().getAsString());
    }

    @Test
    void formStateCodecRoundTripHuman() {
        assertRoundTrip(FormState.HUMAN);
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
