package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Persists the user's chosen plan locally so Daily and Weekly plans survive reopening the app. */
public final class PlanStore {
    private static final String FILE = "saved_workout_plan";
    private static final String NAMES_SEPARATOR = "\n";
    private static final String DAYS_SEPARATOR = ",";

    public static final class SavedPlan {
        public final String mode, target, equipment, focusTargets, weeklyTargets, weeklyExerciseNames;
        public final int minutes, weightKg, sets, reps, restSeconds, cooldownSeconds;
        public final String weeklyDays;
        public final int scheduledDay, weekOffset;
        public final List<String> exerciseNames;

        SavedPlan(String mode, String target, String equipment, int minutes, int weightKg, int sets, int reps, int restSeconds, int cooldownSeconds, String weeklyDays, String focusTargets, String weeklyTargets, String weeklyExerciseNames, int scheduledDay, int weekOffset, List<String> exerciseNames) {
            this.mode = mode;
            this.target = target;
            this.equipment = equipment;
            this.minutes = minutes;
            this.weightKg = weightKg;
            this.sets = sets;
            this.reps = reps;
            this.restSeconds = restSeconds;
            this.cooldownSeconds = cooldownSeconds;
            this.weeklyDays = weeklyDays;
            this.focusTargets = focusTargets;
            this.weeklyTargets = weeklyTargets;
            this.weeklyExerciseNames = weeklyExerciseNames;
            this.scheduledDay = scheduledDay;
            this.weekOffset = weekOffset;
            this.exerciseNames = exerciseNames;
        }
    }

    public static void save(Context context, String mode, String target, String equipment, int minutes, int weightKg, int sets, int reps, int restSeconds, boolean[] weeklyDays, List<Coach.Exercise> exercises) {
        save(context, mode, target, equipment, minutes, weightKg, sets, reps, restSeconds, WorkoutTiming.DEFAULT_COOLDOWN_SECONDS, weeklyDays, target, "", "", 2, 1, exercises);
    }

    /** Stores calendar and one/two-focus assignments alongside the existing local-only plan. */
    public static void save(Context context, String mode, String target, String equipment, int minutes, int weightKg, int sets, int reps, int restSeconds, boolean[] weeklyDays, String focusTargets, String weeklyTargets, int scheduledDay, int weekOffset, List<Coach.Exercise> exercises) {
        save(context, mode, target, equipment, minutes, weightKg, sets, reps, restSeconds, WorkoutTiming.DEFAULT_COOLDOWN_SECONDS, weeklyDays, focusTargets, weeklyTargets, "", scheduledDay, weekOffset, exercises);
    }

    /** Persists the separately generated templates for each selected Weekly day. */
    public static void save(Context context, String mode, String target, String equipment, int minutes, int weightKg, int sets, int reps, int restSeconds, boolean[] weeklyDays, String focusTargets, String weeklyTargets, String weeklyExerciseNames, int scheduledDay, int weekOffset, List<Coach.Exercise> exercises) {
        save(context, mode, target, equipment, minutes, weightKg, sets, reps, restSeconds, WorkoutTiming.DEFAULT_COOLDOWN_SECONDS, weeklyDays, focusTargets, weeklyTargets, weeklyExerciseNames, scheduledDay, weekOffset, exercises);
    }

    /** Persists distinct between-set rest and between-exercise cooldown choices. */
    public static void save(Context context, String mode, String target, String equipment, int minutes, int weightKg, int sets, int reps, int restSeconds, int cooldownSeconds, boolean[] weeklyDays, String focusTargets, String weeklyTargets, String weeklyExerciseNames, int scheduledDay, int weekOffset, List<Coach.Exercise> exercises) {
        ArrayList<String> names = new ArrayList<>();
        for (Coach.Exercise exercise : exercises) names.add(exercise.name);
        preferences(context).edit()
                .putBoolean("saved", true)
                .putString("mode", mode)
                .putString("target", target)
                .putString("equipment", equipment)
                .putInt("minutes", minutes)
                .putInt("weight", weightKg)
                .putInt("sets", sets)
                .putInt("reps", reps)
                .putInt("rest", restSeconds)
                .putInt("cooldown", WorkoutTiming.normalizeCooldownSeconds(cooldownSeconds))
                .putString("weekly_days", encodeWeeklyDays(weeklyDays))
                .putString("focus_targets", focusTargets)
                .putString("weekly_targets", weeklyTargets)
                .putString("weekly_exercises", weeklyExerciseNames)
                .putInt("scheduled_day", scheduledDay)
                .putInt("week_offset", weekOffset)
                .putString("names", String.join(NAMES_SEPARATOR, names))
                .apply();
    }

    public static SavedPlan load(Context context) {
        SharedPreferences preferences = preferences(context);
        if (!preferences.getBoolean("saved", false)) return null;
        String storedNames = preferences.getString("names", "");
        List<String> names = storedNames.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(storedNames.split(NAMES_SEPARATOR, -1)));
        return new SavedPlan(
                preferences.getString("mode", Coach.PLAN_MODES[0]),
                preferences.getString("target", "Full Body"),
                preferences.getString("equipment", Coach.EQUIPMENT_TYPES[0]),
                preferences.getInt("minutes", 30),
                preferences.getInt("weight", 20),
                preferences.getInt("sets", 3),
                preferences.getInt("reps", 10),
                preferences.getInt("rest", 90),
                preferences.getInt("cooldown", WorkoutTiming.DEFAULT_COOLDOWN_SECONDS),
                preferences.getString("weekly_days", "1,0,1,0,1,0,0"),
                preferences.getString("focus_targets", preferences.getString("target", "Full Body")),
                preferences.getString("weekly_targets", ""),
                preferences.getString("weekly_exercises", ""),
                preferences.getInt("scheduled_day", 2),
                preferences.getInt("week_offset", 1),
                names);
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static String encodeWeeklyDays(boolean[] days) {
        if (days == null || days.length != 7) return "1,0,1,0,1,0,0";
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < days.length; i++) {
            if (i > 0) output.append(DAYS_SEPARATOR);
            output.append(days[i] ? "1" : "0");
        }
        return output.toString();
    }

    private PlanStore() {}
}
