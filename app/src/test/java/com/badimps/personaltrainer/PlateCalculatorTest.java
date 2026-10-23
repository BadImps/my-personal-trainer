package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PlateCalculatorTest {
    @Test public void calculatesSymmetricalTwentyKilogramBar() {
        PlateCalculator.Result result = PlateCalculator.calculate(100);
        assertTrue(result.exact());
        assertEquals(100, result.achievableKg);
        assertTrue(result.platesPerSide.size() > 0);
    }

    @Test public void roundsDownToAnAvailableSymmetricalLoad() {
        PlateCalculator.Result result = PlateCalculator.calculate(63, 20, new int[]{20, 10, 5, 2});
        assertFalse(result.exact());
        assertEquals(60, result.achievableKg);
    }
}
