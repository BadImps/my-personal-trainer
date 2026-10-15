package com.badimps.personaltrainer;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Regression coverage for the exercise-library images that previously disappeared on screen. */
@RunWith(RobolectricTestRunner.class)
public final class ExerciseVisualsTest {
    @Test public void everyLibraryExerciseHasMiaAndLeoDemonstrations() {
        Context context = RuntimeEnvironment.getApplication();
        for (String group : Coach.TARGETS) {
            for (Coach.Exercise exercise : Coach.library().get(group)) {
                assertTrue("Missing Coach Mia visual for " + exercise.name,
                        ExerciseVisuals.forExercise(context, "Female", exercise.name) != 0);
                assertTrue("Missing Coach Leo visual for " + exercise.name,
                        ExerciseVisuals.forExercise(context, "Male", exercise.name) != 0);
            }
        }
    }
}
