package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

/** Small, local-only presentation preference store. It deliberately never contains health data. */
public final class AppearanceStore {
    public static final String MIDNIGHT = "Midnight";
    public static final String OCEAN = "Ocean";
    public static final String HIGH_CONTRAST = "High contrast";
    private static final String FILE = "appearance";
    private static final String THEME = "theme";
    private static final String REDUCED_MOTION = "reduced_motion";

    private AppearanceStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String theme(Context context) {
        return prefs(context).getString(THEME, MIDNIGHT);
    }

    public static void theme(Context context, String value) {
        prefs(context).edit().putString(THEME, value).apply();
    }

    public static boolean reducedMotion(Context context) {
        return prefs(context).getBoolean(REDUCED_MOTION, false);
    }

    public static void reducedMotion(Context context, boolean value) {
        prefs(context).edit().putBoolean(REDUCED_MOTION, value).apply();
    }
}
