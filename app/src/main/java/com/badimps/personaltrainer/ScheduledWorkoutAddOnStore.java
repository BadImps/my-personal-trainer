package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Keeps exercise additions attached to a particular scheduled workout date.
 * The base plan remains untouched, while a member can safely add a library
 * movement to one or several future sessions.
 */
public final class ScheduledWorkoutAddOnStore {
    private static final String FILE = "scheduled_workout_add_ons";
    private static final String ROW = "\n";
    private static final String FIELD = "\t";

    public static final class Entry {
        public final String name;
        public final String target;

        Entry(String name, String target) {
            this.name = name;
            this.target = target;
        }
    }

    public static List<Entry> onDate(Context context, Calendar date) {
        ArrayList<Entry> entries = new ArrayList<>();
        String raw = preferences(context).getString(WeeklyScheduleStore.key(date), "");
        if (raw.isEmpty()) return entries;
        for (String row : raw.split(ROW, -1)) {
            String[] values = row.split(FIELD, -1);
            if (values.length == 2 && !values[0].isEmpty()) entries.add(new Entry(values[0], values[1]));
        }
        return entries;
    }

    /** Adds an exercise only once to a date, even when the chooser is reopened. */
    public static boolean add(Context context, Calendar date, Coach.Exercise exercise) {
        List<Entry> entries = onDate(context, date);
        for (Entry entry : entries) {
            if (entry.name.equals(exercise.name) && entry.target.equals(exercise.target)) return false;
        }
        entries.add(new Entry(clean(exercise.name), clean(exercise.target)));
        ArrayList<String> rows = new ArrayList<>();
        for (Entry entry : entries) rows.add(entry.name + FIELD + entry.target);
        preferences(context).edit().putString(WeeklyScheduleStore.key(date), String.join(ROW, rows)).apply();
        return true;
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().apply();
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\t', ' ');
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private ScheduledWorkoutAddOnStore() { }
}
