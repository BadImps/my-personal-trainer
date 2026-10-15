package com.badimps.personaltrainer;

import android.content.Context;
import java.util.Locale;

/** Resolves the selected trainer's visual for every movement the coach can prescribe. */
public final class ExerciseVisuals {
    private ExerciseVisuals() {}

    public static int forExercise(Context context, String trainer, String exerciseName) {
        String slug;
        if ("90/90 Hip Switch".equals(exerciseName)) slug = "ninety_ninety_hip_switch";
        else if ("World's Greatest Stretch".equals(exerciseName)) slug = "worlds_greatest_stretch";
        else if ("Band Pull-apart".equals(exerciseName)) slug = "band_pullapart";
        else if ("Incline Push-up".equals(exerciseName)) slug = "incline_pushup";
        else if ("Push-up".equals(exerciseName)) slug = "pushup";
        else if ("Step-up".equals(exerciseName)) slug = "stepup";
        else slug = exerciseName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        String coach = "Male".equals(trainer) ? "leo_" : "mia_";
        return context.getResources().getIdentifier(coach + slug, "drawable", context.getPackageName());
    }
}
