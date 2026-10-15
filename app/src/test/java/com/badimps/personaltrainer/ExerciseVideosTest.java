package com.badimps.personaltrainer;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Prevents a plan option from silently falling back to the old GIF path. */
@RunWith(RobolectricTestRunner.class)
public final class ExerciseVideosTest {
    @Test public void everyPrescribedMovementHasLocalVideoForBothTrainers() {
        Context context = RuntimeEnvironment.getApplication();
        for (String target : Coach.TARGETS) {
            for (String equipment : Coach.EQUIPMENT_TYPES) {
                for (Coach.Exercise exercise : Coach.planOptions(target, equipment)) {
                    assertTrue("Missing Coach Mia MP4 for " + exercise.name,
                            ExerciseVideos.forExercise(context, "Female", exercise.name) != 0);
                    assertTrue("Missing Coach Leo MP4 for " + exercise.name,
                            ExerciseVideos.forExercise(context, "Male", exercise.name) != 0);
                }
            }
        }
    }
}
