package com.badimps.personaltrainer;

import android.content.Context;
import java.util.Locale;

/** Resolves the selected trainer's visual for every movement the coach can prescribe. */
public final class ExerciseVisuals {
    private ExerciseVisuals() {}

    public static int forExercise(Context context, String trainer, String exerciseName) {
        String coach = "Male".equals(trainer) ? "leo_" : "mia_";
        return context.getResources().getIdentifier(coach + resourceStem(exerciseName), "drawable", context.getPackageName());
    }

    static String resourceStem(String exerciseName) {
        String slug;
        if ("90/90 Hip Switch".equals(exerciseName)) slug = "ninety_ninety_hip_switch";
        else if ("World's Greatest Stretch".equals(exerciseName)) slug = "worlds_greatest_stretch";
        else if ("Band Pull-apart".equals(exerciseName)) slug = "band_pullapart";
        else if ("Incline Push-up".equals(exerciseName)) slug = "incline_pushup";
        else if ("Pike Push-up".equals(exerciseName)) slug = "pike_pushup";
        else if ("Push-up".equals(exerciseName)) slug = "pushup";
        else if ("Step-up".equals(exerciseName)) slug = "stepup";
        else if ("Backpack Floor Press".equals(exerciseName)) slug = "floor_press";
        else if ("Seated Cable Press".equals(exerciseName)) slug = "machine_chest_press";
        else if ("Assisted Dip".equals(exerciseName)) slug = "bench_dip";
        else if ("Seated Leg Curl".equals(exerciseName)) slug = "romanian_deadlift";
        else if ("Cable Biceps Curl".equals(exerciseName)) slug = "biceps_curl";
        else if ("Cable Triceps Pressdown".equals(exerciseName)) slug = "overhead_triceps_extension";
        else if ("Machine Shoulder Press".equals(exerciseName)) slug = "dumbbell_press";
        else if ("Cable Reverse Fly".equals(exerciseName)) slug = "band_pullapart";
        // New planning focuses reuse a close local coach loop until a dedicated recording is added.
        // This prevents a blank tile or unstable remote GIF on the exercise-selection screen.
        else if ("Incline Reverse Fly".equals(exerciseName) || "Towel Pull-apart".equals(exerciseName)) slug = "band_pullapart";
        else if ("Dumbbell Pullover".equals(exerciseName) || "Front Raise".equals(exerciseName) || "Water Bottle Lateral Raise".equals(exerciseName) || "Cable Lateral Raise".equals(exerciseName)) slug = "dumbbell_press";
        else if ("Lateral Raise".equals(exerciseName)) slug = "dumbbell_press";
        else if ("Dumbbell Dead Bug".equals(exerciseName)) slug = "dead_bug";
        else if ("Suitcase Carry".equals(exerciseName) || "Dumbbell March".equals(exerciseName) || "Treadmill Incline Walk".equals(exerciseName) || "Stationary Bike".equals(exerciseName) || "Elliptical Trainer".equals(exerciseName)) slug = "brisk_march";
        else if ("Dumbbell Squat to Press".equals(exerciseName)) slug = "squat_to_press";
        else if ("Dumbbell Step-up".equals(exerciseName)) slug = "stepup";
        else if ("Backpack Row".equals(exerciseName) || "Machine High Row".equals(exerciseName)) slug = "one_arm_row";
        else if ("Prone Y Raise".equals(exerciseName)) slug = "prone_snow_angel";
        else if ("Plank Shoulder Tap".equals(exerciseName)) slug = "mountain_climber";
        else if ("Assisted Pull-up".equals(exerciseName)) slug = "lat_pulldown";
        else if ("Reverse Pec Deck".equals(exerciseName)) slug = "pec_deck_fly";
        else if ("Cable Face Pull".equals(exerciseName)) slug = "band_pullapart";
        else if ("Cable Pallof Press".equals(exerciseName)) slug = "side_plank";
        else if ("Cable Wood Chop".equals(exerciseName)) slug = "renegade_row";
        else if ("Rotary Torso Machine".equals(exerciseName) || "Captain's Chair Knee Raise".equals(exerciseName)) slug = "dead_bug";
        else if ("Rowing Machine".equals(exerciseName)) slug = "seated_cable_row";
        else slug = exerciseName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        return slug;
    }
}
