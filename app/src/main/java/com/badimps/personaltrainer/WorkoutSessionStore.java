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
        public final String phase;
        public final int recoverySecondsRemaining;
        public final int recoverySecondsTotal;
        public final String upcomingExercise;
        public final String sessionId;
        public final long revision;
        public final long timerElapsedMillis;
        public final long timerSnapshotEpochMillis;
        public final int countdownValue;
        public final long recoveryDeadlineEpochMillis;

        State(boolean active, boolean paused, String title, String exercise, int exerciseIndex,
              int exerciseCount, int set, int sets, long startedAtEpochMillis, String phase,
              int recoverySecondsRemaining, int recoverySecondsTotal, String upcomingExercise,
              String sessionId, long revision, long timerElapsedMillis,
              long timerSnapshotEpochMillis, int countdownValue,
              long recoveryDeadlineEpochMillis) {
            this.active = active;
            this.paused = paused;
            this.title = title;
            this.exercise = exercise;
            this.exerciseIndex = exerciseIndex;
            this.exerciseCount = exerciseCount;
            this.set = set;
            this.sets = sets;
            this.startedAtEpochMillis = startedAtEpochMillis;
            this.phase = phase;
            this.recoverySecondsRemaining = recoverySecondsRemaining;
            this.recoverySecondsTotal = recoverySecondsTotal;
            this.upcomingExercise = upcomingExercise;
            this.sessionId = sessionId;
            this.revision = revision;
            this.timerElapsedMillis = timerElapsedMillis;
            this.timerSnapshotEpochMillis = timerSnapshotEpochMillis;
            this.countdownValue = countdownValue;
            this.recoveryDeadlineEpochMillis = recoveryDeadlineEpochMillis;
        }
    }

    public static void save(Context context, String title, String exercise, int exerciseIndex,
                            int exerciseCount, int set, int sets, boolean paused,
                            String exerciseNames, String exerciseSetCounts) {
        save(context, title, exercise, exerciseIndex, exerciseCount, set, sets, paused,
                exerciseNames, exerciseSetCounts, "active", 0, 0, "");
    }

    public static void save(Context context, String title, String exercise, int exerciseIndex,
                            int exerciseCount, int set, int sets, boolean paused,
                            String exerciseNames, String exerciseSetCounts, String phase,
                            int recoverySecondsRemaining, int recoverySecondsTotal,
                            String upcomingExercise) {
        SharedPreferences p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        long started = p.getLong("started", 0L);
        String sessionId = p.getString("session_id", "");
        if (started <= 0L || sessionId.isEmpty()) { started = System.currentTimeMillis(); sessionId = java.util.UUID.randomUUID().toString(); }
        long revision = p.getLong("revision", 0L) + 1L;
        p.edit().putBoolean("active", true).putBoolean("paused", paused)
                .putString("title", title).putString("exercise", exercise)
                .putInt("exercise_index", exerciseIndex).putInt("exercise_count", exerciseCount)
                .putInt("set", set).putInt("sets", sets).putLong("started", started)
                .putString("exercise_names", exerciseNames).putString("exercise_set_counts", exerciseSetCounts)
                .putString("phase", phase == null ? "active" : phase)
                .putInt("recovery_remaining", Math.max(0, recoverySecondsRemaining))
                .putInt("recovery_total", Math.max(0, recoverySecondsTotal))
                .putString("upcoming_exercise", upcomingExercise == null ? "" : upcomingExercise)
                .putString("session_id", sessionId).putLong("revision", revision)
                .apply();
    }

    public static State get(Context context) {
        SharedPreferences p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        return new State(p.getBoolean("active", false), p.getBoolean("paused", false),
                p.getString("title", "Workout"), p.getString("exercise", "Workout"),
                p.getInt("exercise_index", 0), p.getInt("exercise_count", 1),
                p.getInt("set", 1), p.getInt("sets", 1), p.getLong("started", 0L),
                p.getString("phase", "active"), p.getInt("recovery_remaining", 0),
                p.getInt("recovery_total", 0), p.getString("upcoming_exercise", ""),
                p.getString("session_id", ""), p.getLong("revision", 0L),
                p.getLong("timer_elapsed", 0L), p.getLong("timer_snapshot_epoch", 0L),
                p.getInt("countdown_value", 0), p.getLong("recovery_deadline_epoch", 0L));
    }

    /** Adds a phone-owned clock anchor to the current snapshot without creating another session. */
    public static void updateTiming(Context context, long elapsedMillis, long snapshotEpochMillis,
                                    int countdownValue, long recoveryDeadlineEpochMillis) {
        SharedPreferences preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        // Every phone-owned mutation needs a strictly newer sequence number.  Wear Data Layer
        // delivery is asynchronous: without this, a delayed active/rest snapshot can overwrite
        // the most recent clock anchor after the watch wakes up.
        long revision = preferences.getLong("revision", 0L) + 1L;
        preferences.edit()
                .putLong("timer_elapsed", Math.max(0L, elapsedMillis))
                .putLong("timer_snapshot_epoch", Math.max(0L, snapshotEpochMillis))
                .putInt("countdown_value", Math.max(0, Math.min(5, countdownValue)))
                .putLong("recovery_deadline_epoch", Math.max(0L, recoveryDeadlineEpochMillis))
                .putLong("revision", revision).apply();
    }

    public static void markWorkoutStartedNow(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        preferences.edit().putLong("started", System.currentTimeMillis())
                .putLong("revision", preferences.getLong("revision", 0L) + 1L).apply();
    }

    public static void setPaused(Context context, boolean paused) {
        SharedPreferences preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        preferences.edit().putBoolean("paused", paused)
                .putLong("revision", preferences.getLong("revision", 0L) + 1L).apply();
    }

    /** Advances the durable public workout state for a lock-screen or watch completion action. */
    public static void completeCurrentSet(Context context) {
        State state = get(context);
        if (!state.active) return;
        int nextSet = state.set + 1;
        int nextExercise = state.exerciseIndex;
        if (nextSet > Math.max(1, state.sets)) {
            nextSet = 1;
            nextExercise = Math.min(Math.max(0, state.exerciseCount - 1), state.exerciseIndex + 1);
        }
        SharedPreferences preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        String[] names = preferences.getString("exercise_names", state.exercise).split(String.valueOf('\u001f'), -1);
        String[] setCounts = preferences.getString("exercise_set_counts", Integer.toString(state.sets)).split(",", -1);
        String nextName = nextExercise < names.length ? names[nextExercise] : state.exercise;
        int nextSets = state.sets;
        if (nextExercise < setCounts.length) {
            try { nextSets = Math.max(1, Integer.parseInt(setCounts[nextExercise])); }
            catch (NumberFormatException ignored) { }
        }
        preferences.edit().putString("exercise", nextName).putInt("sets", nextSets)
                .putInt("set", nextSet).putInt("exercise_index", nextExercise)
                .putBoolean("paused", false)
                .putLong("revision", preferences.getLong("revision", 0L) + 1L).apply();
    }

    public static void previousExercise(Context context) {
        State state = get(context);
        if (!state.active) return;
        SharedPreferences preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        int previous = Math.max(0, state.exerciseIndex - 1);
        String[] names = preferences.getString("exercise_names", state.exercise).split(String.valueOf('\u001f'), -1);
        String[] setCounts = preferences.getString("exercise_set_counts", Integer.toString(state.sets)).split(",", -1);
        String previousName = previous < names.length ? names[previous] : state.exercise;
        int previousSets = state.sets;
        if (previous < setCounts.length) {
            try { previousSets = Math.max(1, Integer.parseInt(setCounts[previous])); }
            catch (NumberFormatException ignored) { }
        }
        preferences.edit().putString("exercise", previousName).putInt("sets", previousSets)
                .putInt("exercise_index", previous).putInt("set", 1).putBoolean("paused", false)
                .putLong("revision", preferences.getLong("revision", 0L) + 1L).apply();
    }

    public static void clear(Context context) {
        SharedPreferences p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        long terminalRevision = p.getLong("revision", 0L) + 1L;
        String terminalSession = p.getString("session_id", "");
        p.edit().clear().putBoolean("active", false)
                .putString("session_id", terminalSession)
                .putLong("revision", terminalRevision).apply();
    }

    /** Rejects stale, duplicated, or phase-invalid commands before they reach the workout service. */
    public static synchronized boolean acceptWatchCommand(Context context, String sessionId, String command, String commandId) {
        State state = get(context);
        if (!state.active || sessionId == null || sessionId.isEmpty() || !sessionId.equals(state.sessionId)) return false;
        SharedPreferences p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        if (commandId != null && commandId.equals(p.getString("last_watch_command", ""))) return false;
        boolean recovery = "rest".equals(state.phase) || "cooldown".equals(state.phase) || "finish_cooldown".equals(state.phase);
        boolean allowed = "toggle".equals(command) || "end".equals(command)
                || ("complete".equals(command) && "active".equals(state.phase))
                || ("previous".equals(command) && "active".equals(state.phase) && state.exerciseIndex > 0)
                || ("skip".equals(command) && recovery);
        if (!allowed) return false;
        p.edit().putString("last_watch_command", commandId == null ? "" : commandId).apply();
        return true;
    }

    private WorkoutSessionStore() {}
}
