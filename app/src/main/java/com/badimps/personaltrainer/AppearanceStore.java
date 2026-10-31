package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

/** Small, local-only presentation preference store. It deliberately never contains health data. */
public final class AppearanceStore {
    public static final String MIDNIGHT = "Midnight";
    public static final String OCEAN = "Ocean";
    public static final String HIGH_CONTRAST = "High contrast";
    public static final String NAV_CLASSIC = "Classic";
    public static final String NAV_ENAMEL = "Enamel sport";
    public static final String NAV_PULSE = "Pulse performance";
    public static final String NAV_ICE = "Ice alloy";
    public static final String NAV_CHAMPION = "Championship";
    public static final String NAV_SPECTRUM = "Night spectrum";
    public static final String[] NAV_THEMES = {NAV_CLASSIC, NAV_ENAMEL, NAV_PULSE, NAV_ICE, NAV_CHAMPION, NAV_SPECTRUM};
    private static final String FILE = "appearance";
    private static final String THEME = "theme";
    private static final String REDUCED_MOTION = "reduced_motion";
    private static final String NAV_THEME = "navigation_icon_theme";

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

    public static String navigationTheme(Context context) {
        String stored = prefs(context).getString(NAV_THEME, NAV_CLASSIC);
        for (String theme : NAV_THEMES) if (theme.equals(stored)) return theme;
        return NAV_CLASSIC;
    }

    public static void navigationTheme(Context context, String value) {
        String selected = NAV_CLASSIC;
        for (String theme : NAV_THEMES) if (theme.equals(value)) selected = theme;
        prefs(context).edit().putString(NAV_THEME, selected).apply();
    }
}
