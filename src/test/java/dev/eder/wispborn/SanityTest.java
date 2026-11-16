package dev.eder.wispborn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanityTest {
    @Test
    void modIdMatchesConstant() {
        assertEquals("wispborn", WispbornMod.MOD_ID);
    }
}
