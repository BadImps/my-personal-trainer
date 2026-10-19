package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

/** Device-local calorie preference. It is a user-selected planning value, not a prescription. */
public final class CalorieTargetStore {
    public static final String LOW = "Lower movement";
    public static final String TYPICAL = "Typical movement";
    public static final String ACTIVE = "Higher movement";
    private static final String FILE = "calorie_target";
    private static final String KEY_TARGET = "target_kcal";
    private static final String KEY_MOVEMENT = "movement";

    public static int target(Context context, int fallback) {
        return preferences(context).getInt(KEY_TARGET, fallback);
    }

    public static String movement(Context context) {
        return preferences(context).getString(KEY_MOVEMENT, TYPICAL);
    }

    public static void save(Context context, int targetKcal, String movement) {
        if (targetKcal < 1000 || targetKcal > 6000) {
            throw new IllegalArgumentException("Choose a target between 1,000 and 6,000 kcal");
        }
        if (!LOW.equals(movement) && !TYPICAL.equals(movement) && !ACTIVE.equals(movement)) {
            throw new IllegalArgumentException("Choose a daily movement level");
        }
        preferences(context).edit().putInt(KEY_TARGET, targetKcal).putString(KEY_MOVEMENT, movement).apply();
    }

    public static void clear(Context context) { preferences(context).edit().clear().apply(); }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private CalorieTargetStore() {}
}
