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

    public static final class WorkoutRecord {
        public final long timestamp;
        public final int minutes;
        public final String target;

        WorkoutRecord(long timestamp, int minutes, String target) {
            this.timestamp = timestamp;
            this.minutes = minutes;
            this.target = target;
        }
    }

    /** Calendar-month totals grouped by the week in which a guided workout was completed. */
    public static final class MonthReport {
        public final int[] completedByWeek;
        public final int[] minutesByWeek;
        public final int completedTotal;
        public final int minutesTotal;

        MonthReport(int[] completedByWeek, int[] minutesByWeek, int completedTotal, int minutesTotal) {
            this.completedByWeek = completedByWeek;
            this.minutesByWeek = minutesByWeek;
            this.completedTotal = completedTotal;
            this.minutesTotal = minutesTotal;
        }
    }

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
        recordWorkoutAt(context, target, activeMinutes, System.currentTimeMillis());
    }

    static void recordWorkoutAt(Context context, String target, int activeMinutes, long timestamp) {
        SharedPreferences preferences = preferences(context);
        List<String> events = lines(preferences.getString("workouts", ""));
        events.add(timestamp + "|" + Math.max(1, activeMinutes) + "|" + sanitize(target));
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

    public static List<WorkoutRecord> workoutsThisWeek(Context context) {
        long now = System.currentTimeMillis();
        long start = weekStart(now);
        ArrayList<WorkoutRecord> output = new ArrayList<>();
        for (WorkoutRecord record : workoutRecords(context)) if (record.timestamp >= start && record.timestamp <= now) output.add(record);
        output.sort((left, right) -> Long.compare(right.timestamp, left.timestamp));
        return output;
    }

    /** Complete local workout history, newest first, for the user-managed Progress archive. */
    public static List<WorkoutRecord> workouts(Context context) {
        ArrayList<WorkoutRecord> output = new ArrayList<>(workoutRecords(context));
        output.sort((left, right) -> Long.compare(right.timestamp, left.timestamp));
        return output;
    }

    /** Removes exactly one completed-workout record; no schedules, profile, or fuel data are touched. */
    public static void removeWorkout(Context context, long timestamp) {
        List<String> events = lines(preferences(context).getString("workouts", ""));
        String prefix = timestamp + "|";
        for (int index = events.size() - 1; index >= 0; index--) {
            if (events.get(index).startsWith(prefix)) { events.remove(index); break; }
        }
        preferences(context).edit().putString("workouts", join(events)).apply();
    }

    public static MonthReport monthReport(Context context) {
        Calendar now = Calendar.getInstance();
        int maxWeek = Math.max(1, now.getActualMaximum(Calendar.WEEK_OF_MONTH));
        int[] completed = new int[maxWeek];
        int[] minutes = new int[maxWeek];
        int totalCompleted = 0;
        int totalMinutes = 0;
        int month = now.get(Calendar.MONTH);
        int year = now.get(Calendar.YEAR);
        for (WorkoutRecord record : workoutRecords(context)) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(record.timestamp);
            if (date.get(Calendar.MONTH) != month || date.get(Calendar.YEAR) != year) continue;
            int index = Math.max(0, Math.min(maxWeek - 1, date.get(Calendar.WEEK_OF_MONTH) - 1));
            completed[index]++;
            minutes[index] += record.minutes;
            totalCompleted++;
            totalMinutes += record.minutes;
        }
        return new MonthReport(completed, minutes, totalCompleted, totalMinutes);
    }

    static Snapshot snapshotAt(Context context, long now) {
        SharedPreferences preferences = preferences(context);
        long weekStart = weekStart(now);
        int workouts = 0;
        int minutes = 0;
        String lastTarget = "";
        long lastTimestamp = Long.MIN_VALUE;
        for (WorkoutRecord record : workoutRecords(context)) {
                if (record.timestamp >= weekStart && record.timestamp <= now) {
                    workouts++;
                    minutes += Math.max(0, record.minutes);
                }
                if (record.timestamp > lastTimestamp) {
                    lastTimestamp = record.timestamp;
                    lastTarget = record.target;
                }
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

    private static List<WorkoutRecord> workoutRecords(Context context) {
        ArrayList<WorkoutRecord> output = new ArrayList<>();
        for (String event : lines(preferences(context).getString("workouts", ""))) {
            String[] parts = event.split("\\|", 3);
            if (parts.length != 3) continue;
            try {
                output.add(new WorkoutRecord(Long.parseLong(parts[0]), Math.max(1, Integer.parseInt(parts[1])), parts[2]));
            } catch (NumberFormatException ignored) { }
        }
        return output;
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
