package com.badimps.personaltrainer;

import android.content.Context;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Small, local-only training insights. This deliberately offers a recommendation rather than
 * changing a member's load or prescription automatically.
 */
public final class TrainingIntelligence {
    public enum Readiness { READY, RECOVERING, REST_DAY }

    public static final class RecoveryInsight {
        public final Readiness readiness;
        public final String headline;
        public final String detail;
        public final int recentSessions;

        RecoveryInsight(Readiness readiness, String headline, String detail, int recentSessions) {
            this.readiness = readiness;
            this.headline = headline;
            this.detail = detail;
            this.recentSessions = recentSessions;
        }
    }

    public static final class ProgressionAdvice {
        public final String headline;
        public final String detail;
        public final String nextTarget;

        ProgressionAdvice(String headline, String detail, String nextTarget) {
            this.headline = headline;
            this.detail = detail;
            this.nextTarget = nextTarget;
        }
    }

    /**
     * A conservative readiness signal based only on completion records. It is not a medical
     * recovery score and never blocks a session.
     */
    public static RecoveryInsight recovery(Context context, String focus) {
        long now = System.currentTimeMillis();
        long oneDayAgo = now - 24L * 60L * 60L * 1000L;
        long threeDaysAgo = now - 72L * 60L * 60L * 1000L;
        int lastDay = 0;
        int recent = 0;
        JourneyStore.WorkoutRecord latestMatching = null;
        for (JourneyStore.WorkoutRecord record : JourneyStore.workouts(context)) {
            if (!matchesFocus(record.target, focus)) continue;
            if (record.timestamp >= oneDayAgo) lastDay++;
            if (record.timestamp >= threeDaysAgo) recent++;
            if (latestMatching == null) latestMatching = record;
        }
        if (lastDay >= 2) {
            return new RecoveryInsight(Readiness.REST_DAY, "Recovery suggested",
                    "You completed " + lastDay + " related sessions in the last 24 hours. A mobility day or a different area may feel better.", recent);
        }
        if (lastDay == 1) {
            return new RecoveryInsight(Readiness.RECOVERING, "Recovering",
                    "You trained this area in the last day. Keep the next session comfortable or choose a different focus.", recent);
        }
        String detail = recent == 0
                ? "No matching session is recorded in the last three days. Start at a comfortable effort."
                : "Your last related session is outside the 24-hour recovery window. Check how you feel before progressing.";
        return new RecoveryInsight(Readiness.READY, "Ready when you are", detail, recent);
    }

    /** Returns a transparent next-session suggestion from completed set-time data. */
    public static ProgressionAdvice progression(Context context, String focus, int sets, int reps) {
        List<SetTimeStore.WorkoutSummary> workouts = SetTimeStore.loadCompletedWorkouts(context);
        int matching = 0;
        long totalActive = 0L;
        for (SetTimeStore.WorkoutSummary workout : workouts) {
            if (!matchesFocus(workout.target, focus)) continue;
            matching++;
            totalActive += workout.totalActiveMillis;
        }
        if (matching == 0) {
            return new ProgressionAdvice("Establish your baseline",
                    "Finish this plan with controlled form first. Your completed set times will create a local baseline.",
                    sets + " sets × " + reps + " reps");
        }
        if (matching == 1) {
            return new ProgressionAdvice("Repeat before progressing",
                    "One comparable session is saved. Repeat the target once more, then decide whether to add a rep or a small load.",
                    sets + " sets × " + reps + " reps");
        }
        long average = totalActive / Math.max(1, matching);
        return new ProgressionAdvice("Progression check ready",
                String.format(Locale.US, "%d comparable sessions are saved (average active set time %s). If form and recovery both feel good, choose one small progression.",
                        matching, WorkoutTiming.formatElapsedMillis(average)),
                sets + " sets × " + (reps + 1) + " reps  ·  or keep reps and add a small load");
    }

    private static boolean matchesFocus(String recordedTarget, String focus) {
        if (recordedTarget == null || focus == null || focus.trim().isEmpty()) return true;
        String record = recordedTarget.toLowerCase(Locale.ROOT);
        String target = focus.toLowerCase(Locale.ROOT);
        return record.contains(target) || target.contains(record.replace("custom · ", ""));
    }

    private TrainingIntelligence() { }
}
