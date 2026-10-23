package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Private, on-device archive for finished GPS endurance sessions. */
public final class EnduranceHistoryStore {
    private static final String FILE = "endurance_history";
    private static final String KEY = "entries";
    private static final int MAX_ENTRIES = 60;
    private static final int MAX_ROUTE_POINTS_PER_ENTRY = 180;

    public static final class Entry {
        public final long timestamp;
        public final String mode;
        public final long activeMillis;
        public final float distanceMeters;
        public final List<LiveTrackingStore.Point> points;

        Entry(long timestamp, String mode, long activeMillis, float distanceMeters, List<LiveTrackingStore.Point> points) {
            this.timestamp = timestamp;
            this.mode = mode;
            this.activeMillis = activeMillis;
            this.distanceMeters = distanceMeters;
            this.points = Collections.unmodifiableList(points);
        }
    }

    public static void record(Context context, long timestamp, LiveTrackingStore.Session session) {
        if (session == null) return;
        record(context, timestamp, session.mode, session.activeMillis, session.distanceMeters, session.points);
    }

    /** Records timer-only and guided endurance sessions too; their route list may intentionally be empty. */
    public static void record(Context context, long timestamp, String mode, long activeMillis,
                              float distanceMeters, List<LiveTrackingStore.Point> routePoints) {
        List<String> entries = lines(prefs(context).getString(KEY, ""));
        List<LiveTrackingStore.Point> points = compact(routePoints);
        entries.add(timestamp + "|" + safe(mode) + "|" + Math.max(0L, activeMillis)
                + "|" + Math.max(0f, distanceMeters) + "|" + encodePoints(points));
        while (entries.size() > MAX_ENTRIES) entries.remove(0);
        prefs(context).edit().putString(KEY, String.join("\n", entries)).apply();
    }

    public static Entry load(Context context, long timestamp) {
        String prefix = timestamp + "|";
        for (String encoded : lines(prefs(context).getString(KEY, ""))) {
            if (!encoded.startsWith(prefix)) continue;
            Entry entry = decode(encoded);
            if (entry != null) return entry;
        }
        return null;
    }

    public static void remove(Context context, long timestamp) {
        List<String> entries = lines(prefs(context).getString(KEY, ""));
        String prefix = timestamp + "|";
        for (int index = entries.size() - 1; index >= 0; index--) {
            if (entries.get(index).startsWith(prefix)) { entries.remove(index); break; }
        }
        prefs(context).edit().putString(KEY, String.join("\n", entries)).apply();
    }

    private static Entry decode(String encoded) {
        String[] parts = encoded.split("\\|", 5);
        if (parts.length != 5) return null;
        try {
            return new Entry(Long.parseLong(parts[0]), parts[1], Long.parseLong(parts[2]),
                    Float.parseFloat(parts[3]), decodePoints(parts[4]));
        } catch (NumberFormatException ignored) { return null; }
    }

    private static List<LiveTrackingStore.Point> compact(List<LiveTrackingStore.Point> source) {
        ArrayList<LiveTrackingStore.Point> output = new ArrayList<>();
        if (source == null || source.isEmpty()) return output;
        int stride = Math.max(1, (int) Math.ceil(source.size() / (double) MAX_ROUTE_POINTS_PER_ENTRY));
        for (int index = 0; index < source.size(); index += stride) output.add(source.get(index));
        LiveTrackingStore.Point last = source.get(source.size() - 1);
        if (output.get(output.size() - 1) != last) output.add(last);
        return output;
    }

    private static String encodePoints(List<LiveTrackingStore.Point> points) {
        StringBuilder value = new StringBuilder();
        for (LiveTrackingStore.Point point : points) {
            if (value.length() > 0) value.append(';');
            value.append(String.format(Locale.US, "%.6f,%.6f", point.latitude, point.longitude));
        }
        return value.toString();
    }

    private static List<LiveTrackingStore.Point> decodePoints(String encoded) {
        ArrayList<LiveTrackingStore.Point> points = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) return points;
        for (String pair : encoded.split(";")) {
            String[] values = pair.split(",");
            if (values.length != 2) continue;
            try { points.add(new LiveTrackingStore.Point(Double.parseDouble(values[0]), Double.parseDouble(values[1]))); }
            catch (NumberFormatException ignored) { }
        }
        return points;
    }

    private static String safe(String value) { return value == null ? EndurancePlanStore.WALKING : value.replace("|", " ").replace("\n", " "); }
    private static List<String> lines(String value) { ArrayList<String> result = new ArrayList<>(); if (value != null && !value.isEmpty()) for (String line : value.split("\n")) if (!line.trim().isEmpty()) result.add(line); return result; }
    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    private EnduranceHistoryStore() { }
}
