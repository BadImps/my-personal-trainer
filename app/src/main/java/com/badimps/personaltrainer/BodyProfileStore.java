package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/** Stores the optional, device-local body profile used to add context without diagnosing health. */
public final class BodyProfileStore {
    private static final String FILE = "body_profile";

    public static boolean configured(Context context) {
        return preferences(context).getBoolean("configured", false);
    }

    public static void save(Context context, int age, float heightCm) {
        if (age < 18 || age > 100) throw new IllegalArgumentException("Age must be between 18 and 100");
        if (heightCm < 100f || heightCm > 250f) throw new IllegalArgumentException("Height must be between 100 and 250 cm");
        preferences(context).edit()
                .putBoolean("configured", true)
                .putInt("age", age)
                .putFloat("height_cm", heightCm)
                .apply();
    }

    public static int age(Context context) {
        return preferences(context).getInt("age", 0);
    }

    public static float heightCm(Context context) {
        return preferences(context).getFloat("height_cm", 0f);
    }

    public static String summary(Context context) {
        if (!configured(context)) return "Add age, height, and weight to connect your plan and trends.";
        return age(context) + " years  •  " + String.format(Locale.US, "%.0f cm", heightCm(context));
    }

    public static String workoutContext(Context context) {
        if (!configured(context)) return "Complete your private body profile for age-aware context. Your body measurements will never choose a lifting load for you.";
        if (age(context) >= 65) return "Include balance, mobility, and gradual progression. Choose load from comfort and form—not body measurements.";
        return "Use your profile for trend context only. Choose load from comfort, form, and experience—not body measurements.";
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private BodyProfileStore() {}
}
