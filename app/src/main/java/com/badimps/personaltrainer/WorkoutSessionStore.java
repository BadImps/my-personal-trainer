package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

/** Small durable snapshot shared by Home, the guided screen and the foreground notification. */
public final class WorkoutSessionStore {
    private static final String FILE = "active_guided_workout";

    public static final class State {
        public final boolean active;
        public final boolean paused;
        public final String title;
        public final String exercise;
        public final int exerciseIndex;
        public final int exerciseCount;
        public final int set;
        public final int sets;
        public final long startedAtEpochMillis;

        State(boolean active, boolean paused, String title, String exercise, int exerciseIndex,
              int exerciseCount, int set, int sets, long startedAtEpochMillis) {
            this.active = active;
            this.paused = paused;
            this.title = title;
            this.exercise = exercise;
            this.exerciseIndex = exerciseIndex;
            this.exerciseCount = exerciseCount;
            this.set = set;
            this.sets = sets;
            this.startedAtEpochMillis = startedAtEpochMillis;
        }
    }

    public static void save(Context context, String title, String exercise, int exerciseIndex,
                            int exerciseCount, int set, int sets, boolean paused) {
        SharedPreferences p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        long started = p.getLong("started", 0L);
        if (started <= 0L) started = System.currentTimeMillis();
        p.edit().putBoolean("active", true).putBoolean("paused", paused)
                .putString("title", title).putString("exercise", exercise)
                .putInt("exercise_index", exerciseIndex).putInt("exercise_count", exerciseCount)
                .putInt("set", set).putInt("sets", sets).putLong("started", started).apply();
    }

    public static State get(Context context) {
        SharedPreferences p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        return new State(p.getBoolean("active", false), p.getBoolean("paused", false),
                p.getString("title", "Workout"), p.getString("exercise", "Workout"),
                p.getInt("exercise_index", 0), p.getInt("exercise_count", 1),
                p.getInt("set", 1), p.getInt("sets", 1), p.getLong("started", 0L));
    }

    public static void setPaused(Context context, boolean paused) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean("paused", paused).apply();
    }

    public static void clear(Context context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private WorkoutSessionStore() {}
}
