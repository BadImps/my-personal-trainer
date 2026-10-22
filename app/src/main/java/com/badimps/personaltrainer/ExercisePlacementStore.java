package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Permanent, member-managed exercise placement. A saved move is used by the
 * Library and future plan choices until the person moves it again or restores
 * its original placement.
 */
public final class ExercisePlacementStore {
    private static final String FILE = "exercise_library_placements";
    private static final String FIELD = "\t";

    public static final class Placement {
        public final String bodyArea;
        public final String equipment;

        Placement(String bodyArea, String equipment) {
            this.bodyArea = bodyArea;
            this.equipment = equipment;
        }
    }

    public static Placement get(Context context, Coach.Exercise exercise) {
        String stored = preferences(context).getString(key(exercise), "");
        int divider = stored.indexOf(FIELD);
        if (divider <= 0 || divider >= stored.length() - 1) return null;
        return new Placement(stored.substring(0, divider), stored.substring(divider + FIELD.length()));
    }

    public static boolean has(Context context, Coach.Exercise exercise) {
        return get(context, exercise) != null;
    }

    public static void save(Context context, Coach.Exercise exercise, String bodyArea, String equipment) {
        preferences(context).edit().putString(key(exercise), clean(bodyArea) + FIELD + clean(equipment)).apply();
    }

    public static void clear(Context context, Coach.Exercise exercise) {
        preferences(context).edit().remove(key(exercise)).apply();
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().apply();
    }

    private static String key(Coach.Exercise exercise) {
        return clean(exercise.name) + "\u001f" + clean(exercise.target);
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\t', ' ').replace('\u001f', ' ');
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private ExercisePlacementStore() { }
}
