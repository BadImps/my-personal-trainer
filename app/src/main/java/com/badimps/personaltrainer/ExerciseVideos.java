package com.badimps.personaltrainer;

import android.content.Context;

/** Resolves a trainer-specific, local MP4 loop for a movement. */
public final class ExerciseVideos {
    private ExerciseVideos() { }

    public static int forExercise(Context context, String trainer, String exerciseName) {
        String coach = "Male".equals(trainer) ? "leo_" : "mia_";
        return context.getResources().getIdentifier(
                coach + ExerciseVisuals.resourceStem(exerciseName), "raw", context.getPackageName());
    }
}
