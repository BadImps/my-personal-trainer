package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Connects completed workouts and simple food-group check-ins in one private progress timeline. */
public final class JourneyStore {
    private static final String FILE = "journey_nexus";
    private static final String EVENT_SEPARATOR = "\n";
    private static final int MAX_EVENTS = 180;

    public static final int FUEL_PROTEIN = 1;
    public static final int FUEL_PLANTS = 2;
    public static final int FUEL_HYDRATION = 4;

    public static final class Snapshot {
        public final int workoutsThisWeek;
        public final int activeMinutesThisWeek;
        public final int fuelDaysThisWeek;
        public final String lastWorkoutTarget;
        public final int todayFuelMask;

        Snapshot(int workouts, int minutes, int fuelDays, String lastTarget, int fuelMask) {
            workoutsThisWeek = workouts;
            activeMinutesThisWeek = minutes;
            fuelDaysThisWeek = fuelDays;
            lastWorkoutTarget = lastTarget;
            todayFuelMask = fuelMask;
        }

        public int todayFuelGoals() {
            return Integer.bitCount(todayFuelMask & 7);
        }
    }

    public static void recordWorkout(Context context, String target, int activeMinutes) {
        SharedPreferences preferences = preferences(context);
        List<String> events = lines(preferences.getString("workouts", ""));
        events.add(System.currentTimeMillis() + "|" + Math.max(1, activeMinutes) + "|" + sanitize(target));
        trim(events);
        preferences.edit().putString("workouts", join(events)).apply();
    }

    public static void toggleFuelGoal(Context context, int goalFlag) {
        if (goalFlag != FUEL_PROTEIN && goalFlag != FUEL_PLANTS && goalFlag != FUEL_HYDRATION) return;
        SharedPreferences preferences = preferences(context);
        String today = dayKey(System.currentTimeMillis());
        int current = today.equals(preferences.getString("fuel_date", "")) ? preferences.getInt("fuel_mask", 0) : 0;
        int updated = current ^ goalFlag;
        List<String> days = lines(preferences.getString("fuel_days", ""));
        if (updated == 0) days.remove(today);
        else if (!days.contains(today)) days.add(today);
        trim(days);
        preferences.edit()
                .putString("fuel_date", today)
                .putInt("fuel_mask", updated)
                .putString("fuel_days", join(days))
                .apply();
    }

    public static Snapshot snapshot(Context context) {
        return snapshotAt(context, System.currentTimeMillis());
    }

    static Snapshot snapshotAt(Context context, long now) {
        SharedPreferences preferences = preferences(context);
        long weekStart = weekStart(now);
        int workouts = 0;
        int minutes = 0;
        String lastTarget = "";
        long lastTimestamp = Long.MIN_VALUE;
        for (String event : lines(preferences.getString("workouts", ""))) {
            String[] parts = event.split("\\|", 3);
            if (parts.length != 3) continue;
            try {
                long timestamp = Long.parseLong(parts[0]);
                int duration = Integer.parseInt(parts[1]);
                if (timestamp >= weekStart && timestamp <= now) {
                    workouts++;
                    minutes += Math.max(0, duration);
                }
                if (timestamp > lastTimestamp) {
                    lastTimestamp = timestamp;
                    lastTarget = parts[2];
                }
            } catch (NumberFormatException ignored) {}
        }
        int fuelDays = 0;
        for (String day : lines(preferences.getString("fuel_days", ""))) {
            try {
                long timestamp = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(day).getTime();
                if (timestamp >= weekStart && timestamp <= now) fuelDays++;
            } catch (Exception ignored) {}
        }
        String today = dayKey(now);
        int fuelMask = today.equals(preferences.getString("fuel_date", "")) ? preferences.getInt("fuel_mask", 0) : 0;
        return new Snapshot(workouts, minutes, fuelDays, lastTarget, fuelMask);
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().apply();
    }

    private static long weekStart(long now) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() > now) calendar.add(Calendar.DAY_OF_MONTH, -7);
        return calendar.getTimeInMillis();
    }

    private static String dayKey(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(timestamp));
    }

    private static String sanitize(String value) {
        return value == null ? "Workout" : value.replace("|", " ").replace("\n", " ");
    }

    private static List<String> lines(String value) {
        ArrayList<String> output = new ArrayList<>();
        if (value == null || value.isEmpty()) return output;
        for (String line : value.split(EVENT_SEPARATOR)) if (!line.trim().isEmpty()) output.add(line);
        return output;
    }

    private static String join(List<String> values) {
        return String.join(EVENT_SEPARATOR, values);
    }

    private static void trim(List<String> values) {
        while (values.size() > MAX_EVENTS) values.remove(0);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private JourneyStore() {}
}
