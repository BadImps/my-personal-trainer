package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Stores an independent weekly draft for each week in the current planning horizon. */
public final class WeeklyScheduleStore {
    private static final String FILE = "weekly_schedule_drafts";
    private static final String DAY_SEPARATOR = ",";
    private static final String TARGET_SEPARATOR = ";";
    private static final String FOCUS_SEPARATOR = "|";

    public static final class Schedule {
        public final boolean[] workoutDays;
        public final List<List<String>> targets;

        Schedule(boolean[] days, List<List<String>> focuses) {
            workoutDays = days;
            targets = focuses;
        }
    }

    public static Schedule load(Context context, Calendar weekStart, boolean[] fallbackDays, List<List<String>> fallbackTargets) {
        SharedPreferences preferences = preferences(context);
        String key = key(weekStart);
        String encodedDays = preferences.getString("days_" + key, "");
        String encodedTargets = preferences.getString("targets_" + key, "");
        if (encodedDays.isEmpty()) return copyOf(fallbackDays, fallbackTargets);
        return new Schedule(decodeDays(encodedDays, fallbackDays), decodeTargets(encodedTargets, fallbackTargets));
    }

    public static void save(Context context, Calendar weekStart, boolean[] days, List<List<String>> targets) {
        Schedule copy = copyOf(days, targets);
        preferences(context).edit()
                .putString("days_" + key(weekStart), encodeDays(copy.workoutDays))
                .putString("targets_" + key(weekStart), encodeTargets(copy.targets))
                .apply();
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().apply();
    }

    static String key(Calendar weekStart) {
        return new SimpleDateFormat("yyyy_MM_dd", Locale.US).format(weekStart.getTime());
    }

    private static Schedule copyOf(boolean[] days, List<List<String>> targets) {
        boolean[] copiedDays = new boolean[7];
        for (int i = 0; i < copiedDays.length; i++) copiedDays[i] = days != null && i < days.length && days[i];
        ArrayList<List<String>> copiedTargets = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            ArrayList<String> copy = new ArrayList<>();
            if (targets != null && i < targets.size() && targets.get(i) != null) {
                for (String target : targets.get(i)) if (target != null && !target.isEmpty() && copy.size() < 2) copy.add(target);
            }
            copiedTargets.add(copy);
        }
        return new Schedule(copiedDays, copiedTargets);
    }

    private static String encodeDays(boolean[] days) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (i > 0) result.append(DAY_SEPARATOR);
            result.append(days[i] ? '1' : '0');
        }
        return result.toString();
    }

    private static boolean[] decodeDays(String encoded, boolean[] fallback) {
        boolean[] output = new boolean[7];
        String[] values = encoded.split(DAY_SEPARATOR, -1);
        for (int i = 0; i < output.length; i++) output[i] = i < values.length ? "1".equals(values[i]) : fallback != null && i < fallback.length && fallback[i];
        return output;
    }

    private static String encodeTargets(List<List<String>> targets) {
        ArrayList<String> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            List<String> focuses = i < targets.size() ? targets.get(i) : null;
            days.add(focuses == null ? "" : String.join(FOCUS_SEPARATOR, focuses));
        }
        return String.join(TARGET_SEPARATOR, days);
    }

    private static List<List<String>> decodeTargets(String encoded, List<List<String>> fallback) {
        ArrayList<List<String>> output = new ArrayList<>();
        String[] days = encoded.split(TARGET_SEPARATOR, -1);
        for (int i = 0; i < 7; i++) {
            ArrayList<String> focuses = new ArrayList<>();
            String value = i < days.length ? days[i] : "";
            if (!value.isEmpty()) {
                for (String focus : value.split("\\|", -1)) if (!focus.isEmpty() && focuses.size() < 2) focuses.add(focus);
            } else if (fallback != null && i < fallback.size() && fallback.get(i) != null) {
                focuses.addAll(fallback.get(i));
            }
            output.add(focuses);
        }
        return output;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private WeeklyScheduleStore() {}
}
