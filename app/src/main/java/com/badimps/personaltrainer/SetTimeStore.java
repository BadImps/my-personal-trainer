package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Persists completed-workout set times locally.
 *
 * <p>This store deliberately has no awareness of workout lifecycle events. A caller must invoke
 * {@link #recordCompletedWorkout(Context, long, String, List)} only after a workout finishes
 * normally. That keeps emergency-stopped or abandoned workout data out of the saved history.
 */
public final class SetTimeStore {
    private static final String FILE = "completed_set_times";
    private static final String INDEX_KEY = "completed_workout_index";
    private static final String DETAILS_PREFIX = "completed_workout_";
    private static final String FORMAT_VERSION = "v1";
    private static final String LINE_SEPARATOR = "\n";
    private static final String FIELD_SEPARATOR = "|";
    private static final String SET_SEPARATOR = ",";
    private static final String SET_FIELD_SEPARATOR = ":";

    /** An immutable duration for one numbered set. Durations are stored in milliseconds. */
    public static final class SetEntry {
        public final int setNumber;
        public final long durationMillis;

        public SetEntry(int setNumber, long durationMillis) {
            if (setNumber < 1) throw new IllegalArgumentException("setNumber must be at least one");
            if (durationMillis < 0L) throw new IllegalArgumentException("durationMillis cannot be negative");
            this.setNumber = setNumber;
            this.durationMillis = durationMillis;
        }

        public int setNumber() {
            return setNumber;
        }

        public long durationMillis() {
            return durationMillis;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof SetEntry)) return false;
            SetEntry entry = (SetEntry) other;
            return setNumber == entry.setNumber && durationMillis == entry.durationMillis;
        }

        @Override public int hashCode() {
            int result = Integer.hashCode(setNumber);
            return 31 * result + Long.hashCode(durationMillis);
        }

        @Override public String toString() {
            return "SetEntry{" + "setNumber=" + setNumber + ", durationMillis=" + durationMillis + '}';
        }
    }

    /**
     * Immutable summary for one exercise. {@link #setEntries} remains in completed-set order;
     * {@link #leaderboard} is ordered from fastest to slowest, then by set number for ties.
     */
    public static final class ExerciseSummary {
        public final String exerciseName;
        public final String target;
        public final List<SetEntry> setEntries;
        public final List<SetEntry> leaderboard;
        public final long totalActiveMillis;

        public ExerciseSummary(String exerciseName, String target, List<SetEntry> setEntries) {
            this.exerciseName = labelOrFallback(exerciseName, "Exercise");
            this.target = labelOrFallback(target, "Workout");
            this.setEntries = immutableSetEntries(setEntries);

            ArrayList<SetEntry> ranked = new ArrayList<>(this.setEntries);
            ranked.sort(Comparator
                    .comparingLong((SetEntry entry) -> entry.durationMillis)
                    .thenComparingInt(entry -> entry.setNumber));
            this.leaderboard = Collections.unmodifiableList(ranked);

            long total = 0L;
            for (SetEntry entry : this.setEntries) total = saturatedAdd(total, entry.durationMillis);
            this.totalActiveMillis = total;
        }

        public List<SetEntry> setEntries() {
            return setEntries;
        }

        public List<SetEntry> leaderboard() {
            return leaderboard;
        }

        /** Alias that reads naturally at UI call sites. */
        public List<SetEntry> leaderboardEntries() {
            return leaderboard;
        }

        public long totalActiveMillis() {
            return totalActiveMillis;
        }

        /** The fastest entry, with the lowest set number winning an exact-duration tie. */
        public Optional<SetEntry> fastestSet() {
            return leaderboard.isEmpty() ? Optional.empty() : Optional.of(leaderboard.get(0));
        }

        /** Convenience for Java UI code that prefers a nullable value over Optional. */
        public SetEntry fastestSetOrNull() {
            return leaderboard.isEmpty() ? null : leaderboard.get(0);
        }

        /** True for every entry tied for the fastest completed duration. */
        public boolean isFastest(SetEntry entry) {
            return entry != null
                    && setEntries.contains(entry)
                    && !leaderboard.isEmpty()
                    && entry.durationMillis == leaderboard.get(0).durationMillis;
        }

        /** True when the specified set is tied for the fastest completed duration. */
        public boolean isFastest(int setNumber) {
            for (SetEntry entry : setEntries) if (entry.setNumber == setNumber && isFastest(entry)) return true;
            return false;
        }

        /** Personal-best is synonymous with fastest within this completed exercise. */
        public boolean isPersonalBest(SetEntry entry) {
            return isFastest(entry);
        }

        /** Personal-best is synonymous with fastest within this completed exercise. */
        public boolean isPersonalBest(int setNumber) {
            return isFastest(setNumber);
        }

        /**
         * Returns true only for the first leaderboard row. It gives a UI one deterministic row
         * to label "Personal best" when two sets share the same fastest duration.
         */
        public boolean isLeaderboardLeader(SetEntry entry) {
            return entry != null && !leaderboard.isEmpty() && leaderboard.get(0).equals(entry);
        }

        public boolean hasSetEntries() {
            return !setEntries.isEmpty();
        }
    }

    /** A completed workout and all of its immutable exercise summaries. */
    public static final class WorkoutSummary {
        public final long completedAtMillis;
        public final String target;
        public final List<ExerciseSummary> exerciseSummaries;
        /** Alias for callers that prefer the shorter collection name. */
        public final List<ExerciseSummary> exercises;
        public final long totalActiveMillis;

        public WorkoutSummary(long completedAtMillis, String target, List<ExerciseSummary> exerciseSummaries) {
            if (completedAtMillis < 0L) throw new IllegalArgumentException("completedAtMillis cannot be negative");
            this.completedAtMillis = completedAtMillis;
            this.target = labelOrFallback(target, "Workout");
            this.exerciseSummaries = immutableExerciseSummaries(exerciseSummaries);
            this.exercises = this.exerciseSummaries;

            long total = 0L;
            for (ExerciseSummary summary : this.exerciseSummaries) total = saturatedAdd(total, summary.totalActiveMillis);
            this.totalActiveMillis = total;
        }

        public long completedAtMillis() {
            return completedAtMillis;
        }

        public List<ExerciseSummary> exerciseSummaries() {
            return exerciseSummaries;
        }

        public long totalActiveMillis() {
            return totalActiveMillis;
        }
    }

    /**
     * Saves a completed workout under an explicit completion timestamp. Calling this method is
     * the only way this class writes workout data.
     */
    public static void recordCompletedWorkout(Context context, long completedAtMillis, String target,
            List<ExerciseSummary> exerciseSummaries) {
        WorkoutSummary workout = new WorkoutSummary(completedAtMillis, target, exerciseSummaries);
        SharedPreferences preferences = preferences(context);
        Map<Long, WorkoutIndex> byTimestamp = new LinkedHashMap<>();
        for (WorkoutIndex index : readIndex(safeString(preferences, INDEX_KEY))) {
            byTimestamp.put(index.completedAtMillis, index);
        }
        byTimestamp.put(workout.completedAtMillis, new WorkoutIndex(workout.completedAtMillis, workout.target));

        ArrayList<WorkoutIndex> updatedIndex = new ArrayList<>(byTimestamp.values());
        updatedIndex.sort((left, right) -> Long.compare(right.completedAtMillis, left.completedAtMillis));
        preferences.edit()
                .putString(INDEX_KEY, encodeIndex(updatedIndex))
                .putString(detailsKey(workout.completedAtMillis), encodeWorkout(workout))
                .apply();
    }

    /** Saves a completed workout now and returns the timestamp that identifies its details. */
    public static long recordCompletedWorkout(Context context, String target,
            List<ExerciseSummary> exerciseSummaries) {
        long completedAtMillis = System.currentTimeMillis();
        recordCompletedWorkout(context, completedAtMillis, target, exerciseSummaries);
        return completedAtMillis;
    }

    /** Loads one completed workout's set-time details, or {@code null} when none can be read. */
    public static WorkoutSummary loadWorkout(Context context, long completedAtMillis) {
        if (completedAtMillis < 0L) return null;
        return decodeWorkout(completedAtMillis, safeString(preferences(context), detailsKey(completedAtMillis)));
    }

    /** Alias that makes the timestamp-based detail lookup explicit at call sites. */
    public static WorkoutSummary loadWorkoutDetails(Context context, long completedAtMillis) {
        return loadWorkout(context, completedAtMillis);
    }

    /** Returns all readable completed workouts with newest completion timestamps first. */
    public static List<WorkoutSummary> loadCompletedWorkouts(Context context) {
        SharedPreferences preferences = preferences(context);
        ArrayList<WorkoutSummary> workouts = new ArrayList<>();
        for (WorkoutIndex index : readIndex(safeString(preferences, INDEX_KEY))) {
            WorkoutSummary workout = decodeWorkout(index.completedAtMillis,
                    safeString(preferences, detailsKey(index.completedAtMillis)));
            if (workout != null) workouts.add(workout);
        }
        workouts.sort((left, right) -> Long.compare(right.completedAtMillis, left.completedAtMillis));
        return Collections.unmodifiableList(workouts);
    }

    /** Alias for a concise history-style call site. */
    public static List<WorkoutSummary> completedWorkouts(Context context) {
        return loadCompletedWorkouts(context);
    }

    /** Removes only set-time history; it does not affect JourneyStore or any other app data. */
    public static void clear(Context context) {
        preferences(context).edit().clear().apply();
    }

    private static List<SetEntry> immutableSetEntries(List<SetEntry> entries) {
        ArrayList<SetEntry> copy = new ArrayList<>();
        if (entries != null) {
            for (SetEntry entry : entries) {
                if (entry == null) throw new IllegalArgumentException("setEntries cannot contain null");
                copy.add(entry);
            }
        }
        return Collections.unmodifiableList(copy);
    }

    private static List<ExerciseSummary> immutableExerciseSummaries(List<ExerciseSummary> summaries) {
        ArrayList<ExerciseSummary> copy = new ArrayList<>();
        if (summaries != null) {
            for (ExerciseSummary summary : summaries) {
                if (summary == null) throw new IllegalArgumentException("exerciseSummaries cannot contain null");
                copy.add(summary);
            }
        }
        return Collections.unmodifiableList(copy);
    }

    private static String encodeWorkout(WorkoutSummary workout) {
        StringBuilder output = new StringBuilder(FORMAT_VERSION)
                .append(LINE_SEPARATOR)
                .append(encodeString(workout.target));
        for (ExerciseSummary summary : workout.exerciseSummaries) {
            output.append(LINE_SEPARATOR)
                    .append(encodeString(summary.exerciseName))
                    .append(FIELD_SEPARATOR)
                    .append(encodeString(summary.target))
                    .append(FIELD_SEPARATOR)
                    .append(encodeSets(summary.setEntries));
        }
        return output.toString();
    }

    private static WorkoutSummary decodeWorkout(long completedAtMillis, String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;
        String[] rows = encoded.split(LINE_SEPARATOR, -1);
        if (rows.length < 2 || !FORMAT_VERSION.equals(rows[0])) return null;
        String target = decodeString(rows[1]);
        if (target == null) return null;

        ArrayList<ExerciseSummary> summaries = new ArrayList<>();
        for (int index = 2; index < rows.length; index++) {
            String[] fields = rows[index].split("\\|", -1);
            if (fields.length != 3) continue;
            String exerciseName = decodeString(fields[0]);
            String exerciseTarget = decodeString(fields[1]);
            if (exerciseName == null || exerciseTarget == null) continue;
            summaries.add(new ExerciseSummary(exerciseName, exerciseTarget, decodeSets(fields[2])));
        }
        try {
            return new WorkoutSummary(completedAtMillis, target, summaries);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String encodeSets(List<SetEntry> entries) {
        StringBuilder output = new StringBuilder();
        for (SetEntry entry : entries) {
            if (output.length() > 0) output.append(SET_SEPARATOR);
            output.append(entry.setNumber).append(SET_FIELD_SEPARATOR).append(entry.durationMillis);
        }
        return output.toString();
    }

    private static List<SetEntry> decodeSets(String encoded) {
        ArrayList<SetEntry> entries = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) return entries;
        for (String set : encoded.split(SET_SEPARATOR, -1)) {
            String[] fields = set.split(SET_FIELD_SEPARATOR, -1);
            if (fields.length != 2) continue;
            try {
                int setNumber = Integer.parseInt(fields[0]);
                long durationMillis = Long.parseLong(fields[1]);
                if (setNumber > 0 && durationMillis >= 0L) entries.add(new SetEntry(setNumber, durationMillis));
            } catch (NumberFormatException ignored) {
                // An old or partially corrupted set must not block the rest of the history.
            }
        }
        return entries;
    }

    private static String encodeIndex(List<WorkoutIndex> values) {
        StringBuilder output = new StringBuilder(FORMAT_VERSION);
        for (WorkoutIndex value : values) {
            output.append(LINE_SEPARATOR)
                    .append(value.completedAtMillis)
                    .append(FIELD_SEPARATOR)
                    .append(encodeString(value.target));
        }
        return output.toString();
    }

    private static List<WorkoutIndex> readIndex(String encoded) {
        ArrayList<WorkoutIndex> output = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) return output;
        String[] rows = encoded.split(LINE_SEPARATOR, -1);
        if (rows.length == 0 || !FORMAT_VERSION.equals(rows[0])) return output;

        Map<Long, WorkoutIndex> byTimestamp = new LinkedHashMap<>();
        for (int index = 1; index < rows.length; index++) {
            String[] fields = rows[index].split("\\|", -1);
            if (fields.length != 2) continue;
            try {
                long completedAtMillis = Long.parseLong(fields[0]);
                String target = decodeString(fields[1]);
                if (completedAtMillis >= 0L && target != null) {
                    byTimestamp.put(completedAtMillis, new WorkoutIndex(completedAtMillis, target));
                }
            } catch (NumberFormatException ignored) {
                // Ignore one malformed index row and keep the rest of the readable history.
            }
        }
        output.addAll(byTimestamp.values());
        output.sort((left, right) -> Long.compare(right.completedAtMillis, left.completedAtMillis));
        return output;
    }

    private static String encodeString(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns null instead of throwing when an older preference is not valid Base64. */
    private static String decodeString(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String safeString(SharedPreferences preferences, String key) {
        try {
            return preferences.getString(key, "");
        } catch (ClassCastException ignored) {
            return "";
        }
    }

    private static String detailsKey(long completedAtMillis) {
        return DETAILS_PREFIX + completedAtMillis;
    }

    private static String labelOrFallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value;
    }

    private static long saturatedAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static SharedPreferences preferences(Context context) {
        return Objects.requireNonNull(context, "context").getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static final class WorkoutIndex {
        final long completedAtMillis;
        final String target;

        WorkoutIndex(long completedAtMillis, String target) {
            this.completedAtMillis = completedAtMillis;
            this.target = target;
        }
    }

    private SetTimeStore() {}
}
