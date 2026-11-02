package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Private, on-device saved live routes used as personal time-trial maps. */
public final class CustomRouteStore {
    private static final String FILE = "custom_endurance_maps";
    private static final String KEY = "maps";
    private static final int MAX_MAPS = 20;
    private static final int MAX_POINTS = 220;

    public static final class Map {
        public final String id, name, mode;
        public final long baselineMillis, bestMillis;
        public final float distanceMeters;
        public final int attempts;
        public final List<LiveTrackingStore.Point> points;

        Map(String id, String name, String mode, long baselineMillis, long bestMillis, float distanceMeters,
            int attempts, List<LiveTrackingStore.Point> points) {
            this.id = id; this.name = name; this.mode = mode; this.baselineMillis = baselineMillis;
            this.bestMillis = bestMillis; this.distanceMeters = distanceMeters; this.attempts = attempts;
            this.points = Collections.unmodifiableList(points);
        }
    }

    public static List<Map> all(Context context) {
        ArrayList<Map> result = new ArrayList<>();
        String source = prefs(context).getString(KEY, "");
        if (source == null || source.isEmpty()) return result;
        for (String row : source.split("\\n")) {
            Map map = decode(row);
            if (map != null) result.add(map);
        }
        return result;
    }

    public static Map create(Context context, String name, String mode, long baselineMillis, float distanceMeters,
                             List<LiveTrackingStore.Point> route) {
        if (route == null || route.size() < 2) throw new IllegalArgumentException("Track a route before saving it as a custom map.");
        String cleanName = clean(name, mode + " time trial");
        Map created = new Map(UUID.randomUUID().toString(), cleanName, normalizeMode(mode), Math.max(1L, baselineMillis),
                Math.max(1L, baselineMillis), Math.max(0f, distanceMeters), 0, compact(route));
        List<Map> maps = all(context);
        maps.add(0, created);
        while (maps.size() > MAX_MAPS) maps.remove(maps.size() - 1);
        save(context, maps);
        return created;
    }

    public static void recordAttempt(Context context, String id, long durationMillis, float distanceMeters) {
        if (id == null || id.trim().isEmpty()) return;
        List<Map> maps = all(context);
        boolean changed = false;
        for (int index = 0; index < maps.size(); index++) {
            Map map = maps.get(index);
            if (!id.equals(map.id)) continue;
            long duration = Math.max(1L, durationMillis);
            long best = map.bestMillis <= 0L ? duration : Math.min(map.bestMillis, duration);
            maps.set(index, new Map(map.id, map.name, map.mode, map.baselineMillis, best,
                    distanceMeters > 0f ? distanceMeters : map.distanceMeters, map.attempts + 1, map.points));
            changed = true;
            break;
        }
        if (changed) save(context, maps);
    }

    public static void remove(Context context, String id) {
        List<Map> maps = all(context);
        for (int index = maps.size() - 1; index >= 0; index--) if (id.equals(maps.get(index).id)) maps.remove(index);
        save(context, maps);
    }

    private static void save(Context context, List<Map> maps) {
        StringBuilder source = new StringBuilder();
        for (Map map : maps) {
            if (source.length() > 0) source.append('\n');
            source.append(encode(map));
        }
        prefs(context).edit().putString(KEY, source.toString()).apply();
    }

    private static String encode(Map map) {
        return map.id + "|" + value(map.name) + "|" + value(map.mode) + "|" + map.baselineMillis + "|"
                + map.bestMillis + "|" + map.distanceMeters + "|" + map.attempts + "|" + points(map.points);
    }

    private static Map decode(String row) {
        String[] fields = row.split("\\|", 8);
        if (fields.length != 8) return null;
        try {
            return new Map(fields[0], text(fields[1]), normalizeMode(text(fields[2])), Long.parseLong(fields[3]),
                    Long.parseLong(fields[4]), Float.parseFloat(fields[5]), Integer.parseInt(fields[6]), parsePoints(fields[7]));
        } catch (IllegalArgumentException ignored) { return null; }
    }

    private static String value(String text) { return Base64.encodeToString(text.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP | Base64.URL_SAFE); }
    private static String text(String value) { return new String(Base64.decode(value, Base64.NO_WRAP | Base64.URL_SAFE), StandardCharsets.UTF_8); }
    private static String points(List<LiveTrackingStore.Point> route) {
        StringBuilder result = new StringBuilder();
        for (LiveTrackingStore.Point point : route) {
            if (result.length() > 0) result.append(';');
            result.append(String.format(Locale.US, "%.6f,%.6f", point.latitude, point.longitude));
        }
        return result.toString();
    }
    private static List<LiveTrackingStore.Point> parsePoints(String encoded) {
        ArrayList<LiveTrackingStore.Point> result = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) return result;
        for (String pair : encoded.split(";")) {
            String[] point = pair.split(",");
            if (point.length != 2) continue;
            try { result.add(new LiveTrackingStore.Point(Double.parseDouble(point[0]), Double.parseDouble(point[1]))); }
            catch (NumberFormatException ignored) { }
        }
        return result;
    }
    private static List<LiveTrackingStore.Point> compact(List<LiveTrackingStore.Point> source) {
        ArrayList<LiveTrackingStore.Point> output = new ArrayList<>();
        int stride = Math.max(1, (int) Math.ceil(source.size() / (double) MAX_POINTS));
        for (int index = 0; index < source.size(); index += stride) output.add(source.get(index));
        LiveTrackingStore.Point last = source.get(source.size() - 1);
        if (output.get(output.size() - 1) != last) output.add(last);
        return output;
    }
    private static String clean(String value, String fallback) {
        String result = value == null ? "" : value.trim().replace("\n", " ").replace("|", " ");
        return result.isEmpty() ? fallback : result;
    }
    private static String normalizeMode(String value) {
        if (EndurancePlanStore.RUNNING.equals(value) || EndurancePlanStore.CYCLING.equals(value)) return value;
        return EndurancePlanStore.WALKING;
    }
    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    private CustomRouteStore() { }
}
