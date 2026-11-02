package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Keeps the unfinished portion of an ended workout available as a same-day custom session. */
public final class DeferredWorkoutStore {
    private static final String FILE = "deferred_workouts";
    private static final String FIELD = "\t";
    private static final String ROW = "\n";

    public static List<Coach.Exercise> onDate(Context context, Calendar date) {
        ArrayList<Coach.Exercise> result = new ArrayList<>();
        String raw = preferences(context).getString(WeeklyScheduleStore.key(date), "");
        if (raw.isEmpty()) return result;
        for (String row : raw.split(ROW, -1)) {
            String[] values = row.split(FIELD, -1);
            if (values.length != 5) continue;
            try {
                result.add(new Coach.Exercise(values[0], values[1], Integer.parseInt(values[2]),
                        Integer.parseInt(values[3]), Integer.parseInt(values[4]), "Finish with control.",
                        "Remaining exercise from an earlier workout."));
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    public static void save(Context context, Calendar date, List<Coach.Exercise> exercises) {
        ArrayList<String> rows = new ArrayList<>();
        if (exercises != null) for (Coach.Exercise exercise : exercises) {
            if (exercise == null) continue;
            rows.add(clean(exercise.name) + FIELD + clean(exercise.target) + FIELD + Math.max(1, exercise.sets)
                    + FIELD + Math.max(1, exercise.reps) + FIELD + Math.max(0, exercise.restSeconds));
        }
        SharedPreferences.Editor edit = preferences(context).edit();
        if (rows.isEmpty()) edit.remove(WeeklyScheduleStore.key(date));
        else edit.putString(WeeklyScheduleStore.key(date), String.join(ROW, rows));
        edit.apply();
    }

    public static void removeOnDate(Context context, Calendar date) {
        preferences(context).edit().remove(WeeklyScheduleStore.key(date)).apply();
    }

    private static String clean(String value) { return value == null ? "" : value.replace('\t', ' ').replace('\n', ' '); }
    private static SharedPreferences preferences(Context context) { return context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    private DeferredWorkoutStore() { }
}
