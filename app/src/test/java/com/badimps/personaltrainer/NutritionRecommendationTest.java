package com.badimps.personaltrainer;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NutritionRecommendationTest {
    @Test public void rotationSuppliesBreakfastAndLunchForEachDay() {
        for (int day = java.util.Calendar.SUNDAY; day <= java.util.Calendar.SATURDAY; day++) {
            NutritionRecommendation.Day recommendation = NutritionRecommendation.forDayOfWeek(day);
            assertTrue(recommendation.breakfastTitle.length() > 0);
            assertTrue(recommendation.lunchTitle.length() > 0);
        }
    }

    @Test public void adjacentDaysDoNotRepeatTheSameBreakfast() {
        assertNotEquals(NutritionRecommendation.forDayOfWeek(java.util.Calendar.MONDAY).breakfastTitle,
                NutritionRecommendation.forDayOfWeek(java.util.Calendar.TUESDAY).breakfastTitle);
    }
}
