package com.badimps.personaltrainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Pure set-timing rules, kept independent from Android UI and text-to-speech. */
public final class WorkoutTiming {
    public static final int REST_BONUS_SECONDS = 10;
    public static final int MIN_REST_SECONDS = 45;
    public static final int MAX_REST_SECONDS = 8 * 60;
    public static final int REST_STEP_SECONDS = 15;
    public static final int DEFAULT_COOLDOWN_SECONDS = 3 * 60;
    public static final int[] COOLDOWN_PRESET_SECONDS = {60, 3 * 60, 5 * 60};
    private static final long MINIMUM_SLOWER_MARGIN_MS = 250L;
    private static final double SLOWER_MARGIN_RATIO = 0.05d;

    public static final class SetResult {
        public final int setNumber;
        public final long durationMillis;
        public final boolean slowerThanRecentPace;
        public final int recoverySeconds;

        private SetResult(int setNumber, long durationMillis, boolean slowerThanRecentPace, int recoverySeconds) {
            this.setNumber = setNumber;
            this.durationMillis = durationMillis;
            this.slowerThanRecentPace = slowerThanRecentPace;
            this.recoverySeconds = recoverySeconds;
        }
    }

    /** Tracks one exercise so completed-set times remain available until the next exercise. */
    public static final class SetTracker {
        private final int baseRestSeconds;
        private final List<Long> durations = new ArrayList<>();
        private long setStartedAtMillis = -1L;
        private int restBonusSeconds;

        public SetTracker(int baseRestSeconds) {
            this.baseRestSeconds = clampRestSeconds(baseRestSeconds);
        }

        public void startSet(long nowMillis) {
            setStartedAtMillis = nowMillis;
        }

        public SetResult completeSet(long nowMillis) {
            if (setStartedAtMillis < 0L) throw new IllegalStateException("Start the set before completing it.");
            long duration = Math.max(100L, nowMillis - setStartedAtMillis);
            boolean slower = isMeaningfullySlower(durations, duration);
            if (slower) restBonusSeconds = Math.min(MAX_REST_SECONDS - Math.min(baseRestSeconds, MAX_REST_SECONDS), restBonusSeconds + REST_BONUS_SECONDS);
            durations.add(duration);
            setStartedAtMillis = -1L;
            return new SetResult(durations.size(), duration, slower, recoverySeconds());
        }

        public int recoverySeconds() {
            return Math.min(MAX_REST_SECONDS, baseRestSeconds + restBonusSeconds);
        }

        public int restBonusSeconds() {
            return restBonusSeconds;
        }

        public List<Long> durations() {
            return Collections.unmodifiableList(durations);
        }
    }

    /**
     * A set is slower only after two recorded sets and only when it exceeds the average of the
     * previous two by both a useful percentage and a small anti-jitter floor.
     */
    public static boolean isMeaningfullySlower(List<Long> priorDurations, long currentDurationMillis) {
        if (priorDurations == null || priorDurations.size() < 2) return false;
        int end = priorDurations.size();
        long average = (priorDurations.get(end - 1) + priorDurations.get(end - 2)) / 2L;
        long margin = Math.max(MINIMUM_SLOWER_MARGIN_MS, Math.round(average * SLOWER_MARGIN_RATIO));
        return currentDurationMillis > average + margin;
    }

    public static int clampRestSeconds(int seconds) {
        return Math.max(MIN_REST_SECONDS, Math.min(MAX_REST_SECONDS, seconds));
    }

    /** Cooldown is intentionally preset-only so it cannot be confused with custom set recovery. */
    public static int normalizeCooldownSeconds(int seconds) {
        int closest = DEFAULT_COOLDOWN_SECONDS;
        int distance = Integer.MAX_VALUE;
        for (int preset : COOLDOWN_PRESET_SECONDS) {
            int candidateDistance = Math.abs(seconds - preset);
            if (candidateDistance < distance) {
                closest = preset;
                distance = candidateDistance;
            }
        }
        return closest;
    }

    public static String formatElapsedMillis(long millis) {
        long safe = Math.max(0L, millis);
        long minutes = safe / 60_000L;
        long seconds = (safe / 1_000L) % 60L;
        long tenths = (safe / 100L) % 10L;
        return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths);
    }

    private WorkoutTiming() {}
}
