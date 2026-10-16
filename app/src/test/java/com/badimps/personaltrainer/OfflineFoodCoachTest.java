package com.badimps.personaltrainer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class OfflineFoodCoachTest {
    @Test public void createsAnOfflineEstimateForARecognisedBalancedMeal() {
        OfflineFoodCoach.Result result = OfflineFoodCoach.analyse("chicken, rice, and vegetables", "Build muscle", "Chest", 45);
        assertTrue(result.hasEstimate);
        assertTrue(result.calories.contains("kcal"));
        assertTrue(result.summary.contains("protein"));
        assertTrue(result.summary.contains("produce"));
        assertTrue(result.planFit.contains("protein"));
    }

    @Test public void doesNotPretendToEstimateAnUnknownMeal() {
        OfflineFoodCoach.Result result = OfflineFoodCoach.analyse("my family recipe", "Move & feel good", "Full Body", 45);
        assertFalse(result.hasEstimate);
        assertTrue(result.calories.contains("No local estimate"));
        assertTrue(result.healthNote.contains("ingredients"));
    }

    @Test public void preparedFoodsReceiveANeutralFlexiblePlanMessage() {
        OfflineFoodCoach.Result result = OfflineFoodCoach.analyse("burger and fries", "Reduce body fat", "Legs", 60);
        assertTrue(result.hasEstimate);
        assertTrue(result.healthNote.contains("flexible plan"));
        assertTrue(result.planFit.contains("not a pass/fail food"));
    }
}
