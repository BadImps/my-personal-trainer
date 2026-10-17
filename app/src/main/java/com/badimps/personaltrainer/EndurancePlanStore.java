package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores route-planning details locally; it never requests or stores live location. */
public final class EndurancePlanStore {
    public static final String WALKING = "Walking";
    public static final String RUNNING = "Running";
    public static final String CYCLING = "Cycling";

    public static final class Plan {
        public final String mode, name, origin, destination, effort;
        public final float distanceKm;
        public final int minutes;

        Plan(String mode, String name, String origin, String destination, float distanceKm, int minutes, String effort) {
            this.mode = mode; this.name = name; this.origin = origin; this.destination = destination;
            this.distanceKm = distanceKm; this.minutes = minutes; this.effort = effort;
        }
    }

    public static Plan load(Context context, String mode) {
        SharedPreferences p = preferences(context);
        String key = key(mode);
        if (!p.getBoolean(key + "_saved", false)) return null;
        return new Plan(normalizeMode(mode), p.getString(key + "_name", "My route"),
                p.getString(key + "_origin", ""), p.getString(key + "_destination", ""),
                p.getFloat(key + "_distance", 3f), p.getInt(key + "_minutes", 30),
                p.getString(key + "_effort", "Steady"));
    }

    public static void save(Context context, String mode, String name, String origin, String destination,
                            float distanceKm, int minutes, String effort) {
        if (destination == null || destination.trim().isEmpty()) throw new IllegalArgumentException("Destination is required");
        if (distanceKm <= 0f || distanceKm > 500f) throw new IllegalArgumentException("Distance is out of range");
        if (minutes < 5 || minutes > 1440) throw new IllegalArgumentException("Duration is out of range");
        String key = key(mode);
        preferences(context).edit()
                .putBoolean(key + "_saved", true)
                .putString(key + "_name", clean(name, defaultName(normalizeMode(mode))))
                .putString(key + "_origin", clean(origin, ""))
                .putString(key + "_destination", destination.trim())
                .putFloat(key + "_distance", distanceKm)
                .putInt(key + "_minutes", minutes)
                .putString(key + "_effort", clean(effort, "Steady"))
                .apply();
    }

    /** Removes one route without disturbing plans for the other two activities. */
    public static void remove(Context context, String mode) {
        String key = key(mode);
        preferences(context).edit()
                .remove(key + "_saved")
                .remove(key + "_name")
                .remove(key + "_origin")
                .remove(key + "_destination")
                .remove(key + "_distance")
                .remove(key + "_minutes")
                .remove(key + "_effort")
                .apply();
    }

    /** Used only when the user signs out and chooses to clear this device. */
    public static void clear(Context context) { preferences(context).edit().clear().apply(); }

    private static String key(String mode) { return normalizeMode(mode).toLowerCase(java.util.Locale.ROOT); }
    private static String normalizeMode(String mode) {
        if (RUNNING.equals(mode) || CYCLING.equals(mode)) return mode;
        return WALKING;
    }
    private static String defaultName(String mode) { return mode + " route"; }
    private static String clean(String value, String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? fallback : clean.replace("\n", " ");
    }
    private static SharedPreferences preferences(Context context) { return context.getSharedPreferences("endurance_routes", Context.MODE_PRIVATE); }
    private EndurancePlanStore() { }
}
