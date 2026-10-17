package com.badimps.personaltrainer;

import android.content.Context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Resolves a trainer-specific, local MP4 loop for a movement. */
public final class ExerciseVideos {
    /** Only these have a movement-matched filmed loop. Related clips are never substituted silently. */
    private static final Set<String> DEDICATED_LOOPS = new HashSet<>(Arrays.asList(
            "90/90 Hip Switch", "Band Pull-apart", "Bench Dip", "Biceps Curl", "Bird Dog", "Bodyweight Squat",
            "Brisk March", "Burpee", "Calf Raise", "Cat Cow", "Chest Fly", "Dead Bug", "Dumbbell Press",
            "Floor Press", "Glute Bridge", "Goblet Squat", "Hammer Curl", "Incline Push-up", "Isometric Towel Curl",
            "Jumping Jack", "Lat Pulldown", "Leg Extension", "Leg Press", "Machine Chest Press", "Mountain Climber",
            "One-arm Row", "Overhead Triceps Extension", "Pec Deck Fly", "Pike Push-up", "Prone Snow Angel", "Push-up",
            "Renegade Row", "Reverse Lunge", "Romanian Deadlift", "Seated Cable Row", "Shoulder Wall Slide",
            "Side Plank", "Single-leg Hip Hinge", "Squat to Press", "Step-up", "Superman Hold", "Walking Lunge",
            "Wall Sit", "World's Greatest Stretch"));

    private ExerciseVideos() { }

    public static int forExercise(Context context, String trainer, String exerciseName) {
        if (!DEDICATED_LOOPS.contains(exerciseName)) return 0;
        String coach = "Male".equals(trainer) ? "leo_" : "mia_";
        return context.getResources().getIdentifier(
                coach + ExerciseVisuals.resourceStem(exerciseName), "raw", context.getPackageName());
    }

    /** Every named library movement is covered by the native illustrated motion guide when no exact MP4 exists. */
    public static boolean hasMotionGuide(String exerciseName) {
        return exerciseName != null && !exerciseName.trim().isEmpty();
    }
}
