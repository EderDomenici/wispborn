package dev.eder.wispborn.host;

import dev.eder.wispborn.progress.EpMath;
import dev.eder.wispborn.progress.LevelCurve;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsumeRulesTest {

    @Test
    void checkValidatesHostStateAndFloor() {
        assertEquals(ConsumeResult.NOT_HOST, ConsumeRules.check(false, true, 1.0F, 10.0F, 0.2D));
        assertEquals(ConsumeResult.NOT_HOST, ConsumeRules.check(true, false, 1.0F, 10.0F, 0.2D));
        assertEquals(ConsumeResult.NOT_READY, ConsumeRules.check(true, true, 2.01F, 10.0F, 0.2D));
        assertEquals(ConsumeResult.NOT_READY, ConsumeRules.check(true, true, 8.0F, 10.0F, 0.2D));
        assertEquals(ConsumeResult.OK, ConsumeRules.check(true, true, 2.0F, 10.0F, 0.2D));
        assertEquals(ConsumeResult.OK, ConsumeRules.check(true, true, 0.5F, 10.0F, 0.2D));
    }

    @Test
    void consumeEpRoundsWithLevelAndFactor() {
        assertEquals(60, ConsumeRules.consumeEp(2, 30, 1.0D));
        assertEquals(68, ConsumeRules.consumeEp(9, 30, 0.25D));
        assertEquals(0, ConsumeRules.consumeEp(9, 30, 0.0D));
    }

    @Test
    void foundationLoopRhythmMatchesDesign() {
        List<Integer> thresholds = List.of(
                300, 800, 1600, 2800, 4500, 6800, 9800, 13600, 18300,
                24000, 30800, 38800, 48100, 58800, 71000, 84800, 100300, 117600, 136800
        );

        // Galinha Nv 1 com wisp Nv 1, sem atributos: 40 pulsos x 3 + 30 = 150 EP
        double chickenFactor = EpMath.levelFactor(1, 1, 2, 0.25D);
        int chickenTotalEp = 40 * DrainMath.epPerPulse(1, 3, chickenFactor)
                + ConsumeRules.consumeEp(1, 30, chickenFactor);
        assertEquals(150, chickenTotalEp);
        assertEquals(2, LevelCurve.levelFor(chickenTotalEp * 2, thresholds));

        // Vaca Nv 2 com wisp Nv 2: 40 x 6 + 60 = 300 EP
        double cowFactor = EpMath.levelFactor(2, 2, 2, 0.25D);
        int cowTotalEp = 40 * DrainMath.epPerPulse(2, 3, cowFactor)
                + ConsumeRules.consumeEp(2, 30, cowFactor);
        assertEquals(300, cowTotalEp);
    }
}
