package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Independent, local appointments for planned walking, running, and cycling sessions. */
public final class SchedulerStore {
    private static final String FILE = "endurance_appointments";
    private static final String KEY = "appointments";
    private static final String ROW = "\\n";
    private static final String FIELD = "\\|";
    private static final int MAX = 180;

    public static final class Appointment {
        public final String id, mode;
        public final long when;
        public final int durationMinutes;
        Appointment(String id, String mode, long when, int durationMinutes) { this.id = id; this.mode = mode; this.when = when; this.durationMinutes = durationMinutes; }
    }

    public static List<Appointment> onDate(Context context, Calendar date) {
        String key = dateKey(date);
        List<Appointment> matches = new ArrayList<>();
        for (Appointment item : all(context)) if (key.equals(dateKey(item.when))) matches.add(item);
        return matches;
    }

    public static void save(Context context, String mode, Calendar when, int durationMinutes) {
        if (durationMinutes < 5 || durationMinutes > 720) throw new IllegalArgumentException("Choose a duration from 5 to 720 minutes.");
        List<Appointment> values = all(context);
        values.add(new Appointment(String.valueOf(System.currentTimeMillis()), normalizeMode(mode), when.getTimeInMillis(), durationMinutes));
        Collections.sort(values, Comparator.comparingLong(value -> value.when));
        while (values.size() > MAX) values.remove(0);
        write(context, values);
    }

    /** Adds a wizard-planned session once, so Home and Scheduler can share the same calendar data. */
    public static void saveIfMissing(Context context, String mode, Calendar when, int durationMinutes) {
        String day = dateKey(when);
        String normalized = normalizeMode(mode);
        for (Appointment item : all(context)) {
            if (normalized.equals(item.mode) && day.equals(dateKey(item.when)) && item.durationMinutes == durationMinutes) return;
        }
        save(context, normalized, when, durationMinutes);
    }

    public static void remove(Context context, String id) {
        List<Appointment> values = all(context);
        values.removeIf(value -> value.id.equals(id));
        write(context, values);
    }

    /** Removes every endurance appointment on one day. Used when a member cancels that day's plan. */
    public static void removeOnDate(Context context, Calendar date) {
        String day = dateKey(date);
        List<Appointment> values = all(context);
        values.removeIf(value -> day.equals(dateKey(value.when)));
        write(context, values);
    }

    /** Moves every endurance appointment on a day while preserving its time, type, and duration. */
    public static void moveOnDate(Context context, Calendar from, Calendar to) {
        String sourceDay = dateKey(from);
        List<Appointment> moved = new ArrayList<>();
        for (Appointment value : all(context)) {
            if (!sourceDay.equals(dateKey(value.when))) {
                moved.add(value);
                continue;
            }
            Calendar time = Calendar.getInstance();
            time.setTimeInMillis(value.when);
            time.set(Calendar.YEAR, to.get(Calendar.YEAR));
            time.set(Calendar.MONTH, to.get(Calendar.MONTH));
            time.set(Calendar.DAY_OF_MONTH, to.get(Calendar.DAY_OF_MONTH));
            moved.add(new Appointment(value.id, value.mode, time.getTimeInMillis(), value.durationMinutes));
        }
        Collections.sort(moved, Comparator.comparingLong(value -> value.when));
        write(context, moved);
    }

    public static void clear(Context context) { preferences(context).edit().clear().apply(); }

    private static List<Appointment> all(Context context) {
        String raw = preferences(context).getString(KEY, "");
        List<Appointment> values = new ArrayList<>();
        if (raw.isEmpty()) return values;
        for (String row : raw.split(ROW)) {
            String[] data = row.split(FIELD, -1);
            if (data.length != 4) continue;
            try { values.add(new Appointment(data[0], normalizeMode(data[1]), Long.parseLong(data[2]), Integer.parseInt(data[3]))); }
            catch (NumberFormatException ignored) { }
        }
        Collections.sort(values, Comparator.comparingLong(value -> value.when));
        return values;
    }

    private static void write(Context context, List<Appointment> values) {
        StringBuilder encoded = new StringBuilder();
        for (Appointment value : values) {
            if (encoded.length() > 0) encoded.append('\n');
            encoded.append(value.id).append('|').append(value.mode).append('|').append(value.when).append('|').append(value.durationMinutes);
        }
        preferences(context).edit().putString(KEY, encoded.toString()).apply();
    }

    public static String dateKey(Calendar date) { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.getTime()); }
    private static String dateKey(long when) { Calendar date = Calendar.getInstance(); date.setTimeInMillis(when); return dateKey(date); }
    private static String normalizeMode(String mode) { return EndurancePlanStore.CYCLING.equals(mode) || EndurancePlanStore.RUNNING.equals(mode) ? mode : EndurancePlanStore.WALKING; }
    private static SharedPreferences preferences(Context context) { return context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    private SchedulerStore() { }
}
