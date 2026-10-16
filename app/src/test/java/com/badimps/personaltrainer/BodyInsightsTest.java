package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class BodyInsightsTest {
    @Test public void calculatesAdultScreeningReferenceFromHeightWithoutCallingItAPersonalTarget() {
        assertEquals(22.7f, BodyInsights.bmi(180f, 73.5f, "kg"), .1f);
        assertEquals(60.0f, BodyInsights.referenceWeightLowKg(180f), .1f);
        assertEquals(80.7f, BodyInsights.referenceWeightHighKg(180f), .1f);
        assertTrue(BodyInsights.referenceRange(180f, "kg").contains("59.9–80.7 kg"));
    }

    @Test public void keepsTeenProfilesOutOfAdultReference() {
        assertFalse(BodyInsights.usesAdultReference(19));
        assertTrue(BodyInsights.usesAdultReference(20));
    }

    @Test public void showsPaceBasedDistanceRatherThanAnAgeBasedPerformanceClaim() {
        assertTrue(BodyInsights.paceDistance(3.0f).contains("3.0 mi in 1 hour"));
        assertTrue(BodyInsights.paceDistance(3.0f).contains("6.0 mi in 2 hours"));
    }
}
