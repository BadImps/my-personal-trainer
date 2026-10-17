package com.badimps.personaltrainer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Prevents a plan option from becoming blank or showing a mismatched recorded movement. */
@RunWith(RobolectricTestRunner.class)
public final class ExerciseVideosTest {
    @Test public void everyPrescribedMovementHasExactVideoOrNativeMotionGuideForBothTrainers() {
        Context context = RuntimeEnvironment.getApplication();
        for (String target : Coach.TARGETS) {
            for (String equipment : Coach.EQUIPMENT_TYPES) {
                for (Coach.Exercise exercise : Coach.planOptions(target, equipment)) {
                    assertTrue("Missing Coach Mia presentation for " + exercise.name,
                            ExerciseVideos.forExercise(context, "Female", exercise.name) != 0 || ExerciseVideos.hasMotionGuide(exercise.name));
                    assertTrue("Missing Coach Leo presentation for " + exercise.name,
                            ExerciseVideos.forExercise(context, "Male", exercise.name) != 0 || ExerciseVideos.hasMotionGuide(exercise.name));
                }
            }
        }
    }

    @Test public void machineAssistedChestOffersTheFourPreviewedMovementsWithSmoothMedia() {
        Context context = RuntimeEnvironment.getApplication();
        String[] expected = {"Machine Chest Press", "Seated Cable Press", "Pec Deck Fly", "Assisted Dip"};
        java.util.List<Coach.Exercise> exercises = Coach.planOptions("Chest", "Machine-assisted");
        assertEquals(expected.length, exercises.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], exercises.get(i).name);
            assertTrue(ExerciseVideos.forExercise(context, "Female", exercises.get(i).name) != 0 || ExerciseVideos.hasMotionGuide(exercises.get(i).name));
            assertTrue(ExerciseVideos.forExercise(context, "Male", exercises.get(i).name) != 0 || ExerciseVideos.hasMotionGuide(exercises.get(i).name));
        }
    }

    @Test public void trainingAreasDoNotTreatMachinesAsABodyArea() {
        for (String target : Coach.TARGETS) assertTrue(!"Machines".equals(target));
    }

    @Test public void newMachineExercisesUseTheirOwnCoachMediaInsteadOfAnAlias() {
        Context context = RuntimeEnvironment.getApplication();
        assertEquals("pec_deck_fly", ExerciseVisuals.resourceStem("Pec Deck Fly"));
        assertEquals("leg_extension", ExerciseVisuals.resourceStem("Leg Extension"));
        for (String trainer : new String[]{"Female", "Male"}) {
            assertTrue(ExerciseVisuals.forExercise(context, trainer, "Pec Deck Fly") != 0);
            assertTrue(ExerciseVideos.forExercise(context, trainer, "Pec Deck Fly") != 0);
            assertTrue(ExerciseVisuals.forExercise(context, trainer, "Leg Extension") != 0);
            assertTrue(ExerciseVideos.forExercise(context, trainer, "Leg Extension") != 0);
        }
    }
}
