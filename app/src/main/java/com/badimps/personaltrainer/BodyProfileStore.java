package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/** Stores the optional, device-local body profile used to add context without diagnosing health. */
public final class BodyProfileStore {
    private static final String FILE = "body_profile";
    public static final String GENDER_NOT_SET = "Not specified";
    public static final String GENDER_FEMALE = "Female";
    public static final String GENDER_MALE = "Male";

    public static boolean configured(Context context) {
        return preferences(context).getBoolean("configured", false);
    }

    public static void save(Context context, int age, float heightCm) {
        save(context, age, heightCm, GENDER_NOT_SET);
    }

    public static void save(Context context, int age, float heightCm, String gender) {
        if (age < 18 || age > 100) throw new IllegalArgumentException("Age must be between 18 and 100");
        if (heightCm < 100f || heightCm > 250f) throw new IllegalArgumentException("Height must be between 100 and 250 cm");
        if (!GENDER_FEMALE.equals(gender) && !GENDER_MALE.equals(gender) && !GENDER_NOT_SET.equals(gender)) {
            throw new IllegalArgumentException("Select Female, Male, or Not specified");
        }
        preferences(context).edit()
                .putBoolean("configured", true)
                .putInt("age", age)
                .putFloat("height_cm", heightCm)
                .putString("gender", gender)
                .apply();
    }

    public static int age(Context context) {
        return preferences(context).getInt("age", 0);
    }

    public static float heightCm(Context context) {
        return preferences(context).getFloat("height_cm", 0f);
    }

    /** Optional self-described gender. It is never inferred from the chosen coach. */
    public static String gender(Context context) {
        return preferences(context).getString("gender", GENDER_NOT_SET);
    }

    public static float walkingPaceMph(Context context) {
        return preferences(context).getFloat("walking_pace_mph", 3.0f);
    }

    public static void setWalkingPaceMph(Context context, float paceMph) {
        if (paceMph < 2f || paceMph > 5f) throw new IllegalArgumentException("Pace must be between 2 and 5 mph");
        preferences(context).edit().putFloat("walking_pace_mph", paceMph).apply();
    }

    public static String summary(Context context) {
        if (!configured(context)) return "Add age, height, and weight to connect your plan and trends.";
        String gender = gender(context);
        return age(context) + " years  •  " + String.format(Locale.US, "%.0f cm", heightCm(context))
                + (GENDER_NOT_SET.equals(gender) ? "" : "  •  " + gender);
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
