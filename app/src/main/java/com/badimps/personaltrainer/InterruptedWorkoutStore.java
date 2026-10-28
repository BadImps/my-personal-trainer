package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

/** Persists the safe set boundary of an emergency-stopped guided workout for the current day. */
public final class InterruptedWorkoutStore {
    private static final String FILE = "interrupted_guided_workout";

    public static final class State {
        public final String target;
        public final int exerciseIndex;
        public final int setNumber;
        public final long remainingMillis;

        State(String target, int exerciseIndex, int setNumber, long remainingMillis) {
            this.target = target;
            this.exerciseIndex = exerciseIndex;
            this.setNumber = setNumber;
            this.remainingMillis = remainingMillis;
        }
    }

    private InterruptedWorkoutStore() {}

    public static void save(Context context, String target, int exerciseIndex, int setNumber, long remainingMillis) {
        preferences(context).edit()
                .putLong("day", dayStart(System.currentTimeMillis()))
                .putString("target", target == null ? "" : target)
                .putInt("exercise", Math.max(0, exerciseIndex))
                .putInt("set", Math.max(1, setNumber))
                .putLong("remaining", Math.max(0L, remainingMillis))
                .apply();
    }

    public static State loadToday(Context context) {
        SharedPreferences values = preferences(context);
        if (values.getLong("day", -1L) != dayStart(System.currentTimeMillis())) {
            clear(context);
            return null;
        }
        String target = values.getString("target", "");
        if (target == null || target.isEmpty()) return null;
        return new State(target, values.getInt("exercise", 0), values.getInt("set", 1), values.getLong("remaining", 0L));
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static long dayStart(long timestamp) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(timestamp);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day.getTimeInMillis();
    }
}
