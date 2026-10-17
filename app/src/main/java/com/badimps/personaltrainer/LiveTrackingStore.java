package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Local-only state for one explicit endurance tracking session. */
public final class LiveTrackingStore {
    private static final String FILE = "live_endurance_tracking";
    private static final int MAX_POINTS = 360;

    public static final class Point {
        public final double latitude;
        public final double longitude;
        Point(double latitude, double longitude) { this.latitude = latitude; this.longitude = longitude; }
    }

    public static final class Session {
        public final boolean active;
        public final boolean paused;
        public final String mode;
        public final long activeMillis;
        public final float distanceMeters;
        public final List<Point> points;
        Session(boolean active, boolean paused, String mode, long activeMillis, float distanceMeters, List<Point> points) {
            this.active = active; this.paused = paused; this.mode = mode; this.activeMillis = activeMillis;
            this.distanceMeters = distanceMeters; this.points = Collections.unmodifiableList(points);
        }
    }

    public static void start(Context context, String mode) {
        long now = System.currentTimeMillis();
        preferences(context).edit().clear()
                .putBoolean("active", true).putBoolean("paused", false)
                .putString("mode", normalizeMode(mode)).putLong("active_millis", 0L)
                .putLong("resumed_at", now).putFloat("distance_meters", 0f)
                .putString("points", "").apply();
    }

    public static void pause(Context context) {
        SharedPreferences p = preferences(context);
        if (!p.getBoolean("active", false) || p.getBoolean("paused", false)) return;
        preferences(context).edit().putLong("active_millis", activeMillis(p, System.currentTimeMillis()))
                .putBoolean("paused", true).apply();
    }

    public static void resume(Context context) {
        SharedPreferences p = preferences(context);
        if (!p.getBoolean("active", false) || !p.getBoolean("paused", false)) return;
        preferences(context).edit().putBoolean("paused", false).putLong("resumed_at", System.currentTimeMillis()).apply();
    }

    public static void addPoint(Context context, double latitude, double longitude) {
        SharedPreferences p = preferences(context);
        if (!p.getBoolean("active", false) || p.getBoolean("paused", false)) return;
        List<Point> points = parsePoints(p.getString("points", ""));
        float distance = p.getFloat("distance_meters", 0f);
        if (!points.isEmpty()) distance += metersBetween(points.get(points.size() - 1), new Point(latitude, longitude));
        points.add(new Point(latitude, longitude));
        while (points.size() > MAX_POINTS) points.remove(0);
        preferences(context).edit().putFloat("distance_meters", distance).putString("points", serialize(points)).apply();
    }

    public static Session current(Context context) { return read(preferences(context), true); }

    public static Session finish(Context context) {
        SharedPreferences p = preferences(context);
        Session session = read(p, true);
        if (!session.active) return null;
        preferences(context).edit().putBoolean("active", false).putBoolean("paused", false)
                .putLong("active_millis", session.activeMillis)
                .putString("last_mode", session.mode).putLong("last_millis", session.activeMillis)
                .putFloat("last_distance", session.distanceMeters).putString("last_points", serialize(session.points)).apply();
        return session;
    }

    public static Session last(Context context) {
        SharedPreferences p = preferences(context);
        if (!p.contains("last_mode")) return null;
        return new Session(false, false, p.getString("last_mode", EndurancePlanStore.WALKING),
                p.getLong("last_millis", 0L), p.getFloat("last_distance", 0f), parsePoints(p.getString("last_points", "")));
    }

    public static void discard(Context context) { preferences(context).edit().clear().apply(); }
    public static void clear(Context context) { discard(context); }

    private static Session read(SharedPreferences p, boolean includeLiveTime) {
        boolean active = p.getBoolean("active", false);
        boolean paused = p.getBoolean("paused", false);
        long millis = p.getLong("active_millis", 0L);
        if (active && !paused && includeLiveTime) millis = activeMillis(p, System.currentTimeMillis());
        return new Session(active, paused, p.getString("mode", EndurancePlanStore.WALKING), millis,
                p.getFloat("distance_meters", 0f), parsePoints(p.getString("points", "")));
    }

    private static long activeMillis(SharedPreferences p, long now) {
        return Math.max(0L, p.getLong("active_millis", 0L) + now - p.getLong("resumed_at", now));
    }

    private static List<Point> parsePoints(String value) {
        List<Point> points = new ArrayList<>();
        if (value == null || value.isEmpty()) return points;
        for (String pair : value.split("\\|")) {
            String[] values = pair.split(",");
            if (values.length != 2) continue;
            try { points.add(new Point(Double.parseDouble(values[0]), Double.parseDouble(values[1]))); }
            catch (NumberFormatException ignored) { }
        }
        return points;
    }

    private static String serialize(List<Point> points) {
        StringBuilder value = new StringBuilder();
        for (Point point : points) {
            if (value.length() > 0) value.append('|');
            value.append(String.format(Locale.US, "%.6f,%.6f", point.latitude, point.longitude));
        }
        return value.toString();
    }

    private static float metersBetween(Point one, Point two) {
        double earth = 6371000d;
        double latitude = Math.toRadians(two.latitude - one.latitude);
        double longitude = Math.toRadians(two.longitude - one.longitude);
        double a = Math.sin(latitude / 2d) * Math.sin(latitude / 2d)
                + Math.cos(Math.toRadians(one.latitude)) * Math.cos(Math.toRadians(two.latitude))
                * Math.sin(longitude / 2d) * Math.sin(longitude / 2d);
        return (float) (earth * 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a)));
    }

    private static String normalizeMode(String mode) {
        if (EndurancePlanStore.CYCLING.equals(mode) || EndurancePlanStore.RUNNING.equals(mode)) return mode;
        return EndurancePlanStore.WALKING;
    }
    private static SharedPreferences preferences(Context context) { return context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    private LiveTrackingStore() { }
}
