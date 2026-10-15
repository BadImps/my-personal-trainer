package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Arrays;

import org.junit.Test;

/** Regression coverage for the selectable Custom, Daily, and Weekly plan catalogue. */
public final class PlanOptionsTest {
    @Test public void everyBodyAreaHasOptionsForEveryEquipmentType() {
        for (String target : Coach.TARGETS) {
            for (String equipment : Coach.EQUIPMENT_TYPES) {
                List<Coach.Exercise> options = Coach.planOptions(target, equipment);
                assertTrue(target + " / " + equipment + " should offer a series of exercises", options.size() >= 3);
                for (Coach.Exercise exercise : options) {
                    assertFalse("Exercise names must be present", exercise.name.trim().isEmpty());
                    assertEquals(target, exercise.target);
                }
            }
        }
    }

    @Test public void prescriptionCopiesTheSelectedExerciseWithoutChangingItsIdentity() {
        Coach.Exercise source = Coach.planOptions("Chest", "Dumbbells").get(0);
        Coach.Exercise prescribed = Coach.prescribe(source, 4, 8, 105);
        assertEquals(source.name, prescribed.name);
        assertEquals(source.target, prescribed.target);
        assertEquals(4, prescribed.sets);
        assertEquals(8, prescribed.reps);
        assertEquals(105, prescribed.restSeconds);
    }

    @Test public void dailyMultiFocusRecommendationIncludesBothAreasAndRespectsDurationShortlist() {
        List<Coach.Exercise> options = Coach.planOptions(Arrays.asList("Chest", "Arms"), "Machine-assisted");
        assertTrue(options.size() >= 6);
        boolean includesChest = false;
        boolean includesArms = false;
        for (Coach.Exercise exercise : options) {
            includesChest |= "Chest".equals(exercise.target);
            includesArms |= "Arms".equals(exercise.target);
        }
        assertTrue(includesChest);
        assertTrue(includesArms);
        assertTrue(Coach.recommendedForDuration(options, 45).size() <= 4);
        assertTrue(Coach.recommendedForDuration(options, 60).size() <= 6);
    }
}
