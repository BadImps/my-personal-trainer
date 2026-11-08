package com.badimps.personaltrainer;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Computes the private Achievement and Apex experience from completed local sessions.
 *
 * <p>There is deliberately no server score, social ranking, calorie requirement, or penalty for
 * a missed day. A member can keep using every normal workout and coach regardless of this system.
 * Apex is an optional consistency lane, not a prescription for intensity.</p>
 */
public final class AchievementStore {
    /** Shared rank result used by tests and every surface that presents the eight-level ladder. */
    public static final class Rank {
        public final String title;
        Rank(String title) { this.title = title; }
    }

    public static Rank rankForLevel(int level) {
        if (level <= 10) return new Rank("BEGINNER");
        if (level <= 25) return new Rank("NOVICE");
        if (level <= 50) return new Rank("CONTENDER");
        if (level <= 75) return new Rank("COMPETITOR");
        if (level <= 100) return new Rank("ATHLETE");
        if (level <= 150) return new Rank("ELITE");
        if (level <= 200) return new Rank("CHAMPION");
        return new Rank("APEX");
    }

    public static final class Badge {
        public final String family;
        public final String name;
        public final String description;
        public final int current;
        public final int required;

        Badge(String family, String name, String description, int current, int required) {
            this.family = family;
            this.name = name;
            this.description = description;
            this.current = Math.max(0, current);
            this.required = Math.max(1, required);
        }

        public boolean unlocked() { return current >= required; }
        public int progressPercent() { return Math.min(100, Math.round(100f * current / required)); }
        public String progressLabel() { return unlocked() ? "Unlocked" : current + " / " + required; }
    }

    public static final class ApexTier {
        public final String name;
        public final String purpose;
        public final int levelRequired;
        public final int sessionsRequired;
        public final int daysWindow;
        public final int sessionsInWindow;

        ApexTier(String name, String purpose, int levelRequired, int sessionsRequired,
                int daysWindow, int sessionsInWindow) {
            this.name = name;
            this.purpose = purpose;
            this.levelRequired = levelRequired;
            this.sessionsRequired = sessionsRequired;
            this.daysWindow = daysWindow;
            this.sessionsInWindow = sessionsInWindow;
        }

        /**
         * Apex tiers require both the member's overall foundation and the stated recent rhythm.
         * Keeping the window count here prevents UI copy and unlock behavior drifting apart.
         */
        public boolean unlocked(int level) {
            return level >= levelRequired
                    && sessionsInWindow >= sessionsRequired;
        }
    }

    public static final class Snapshot {
        public final int sessions;
        public final int guidedMinutes;
        public final int enduranceSessions;
        public final int focusVariety;
        public final int streakDays;
        public final int xp;
        public final int level;
        public final int levelProgress;
        public final int xpToNextLevel;
        public final List<Badge> badges;
        public final List<ApexTier> apexTiers;

        Snapshot(int sessions, int guidedMinutes, int enduranceSessions, int focusVariety,
                int streakDays, int xp, int level, int levelProgress, int xpToNextLevel,
                List<Badge> badges, List<ApexTier> apexTiers) {
            this.sessions = sessions;
            this.guidedMinutes = guidedMinutes;
            this.enduranceSessions = enduranceSessions;
            this.focusVariety = focusVariety;
            this.streakDays = streakDays;
            this.xp = xp;
            this.level = level;
            this.levelProgress = levelProgress;
            this.xpToNextLevel = xpToNextLevel;
            this.badges = Collections.unmodifiableList(badges);
            this.apexTiers = Collections.unmodifiableList(apexTiers);
        }

        public int unlockedBadgeCount() {
            int count = 0;
            for (Badge badge : badges) if (badge.unlocked()) count++;
            return count;
        }

        public ApexTier currentApexTier() {
            ApexTier current = apexTiers.get(0);
            for (ApexTier tier : apexTiers) if (tier.unlocked(level)) current = tier;
            return current;
        }
    }

    public static Snapshot snapshot(Context context) {
        List<JourneyStore.WorkoutRecord> records = JourneyStore.workouts(context);
        AchievementProgressStore.Snapshot durable = AchievementProgressStore.snapshot(context);
        int sessions = durable.lifetimeSessions;
        int minutes = durable.lifetimeMinutes;
        int endurance = durable.lifetimeEnduranceSessions;
        Set<String> activeDays = new HashSet<>();
        for (JourneyStore.WorkoutRecord record : records) {
            activeDays.add(dayKey(record.timestamp));
        }
        int streak = currentOrLatestStreak(activeDays);
        int xp = sessions * 100 + minutes * 2 + endurance * 40 + Math.max(0, durable.focusVariety - 1) * 25;
        int level = Math.max(1, xp / 500 + 1);
        int levelProgress = xp % 500;
        ArrayList<Badge> badges = new ArrayList<>();
        addSeries(badges, "CONSISTENCY", new String[]{"First Step", "Three in Motion", "Steady Seven", "Routine Builder", "Relentless"},
                new int[]{1, 3, 7, 20, 50}, sessions, "completed sessions");
        addSeries(badges, "GUIDED TIME", new String[]{"Half Hour", "Focused Hour", "Training Block", "Time Builder", "Hours of Work"},
                new int[]{30, 60, 180, 600, 1500}, minutes, "guided minutes");
        addSeries(badges, "RHYTHM", new String[]{"Back Again", "Three-Day Rhythm", "Five-Day Rhythm", "Week of Momentum", "Two-Week Rhythm"},
                new int[]{2, 3, 5, 7, 14}, streak, "consecutive active days");
        addSeries(badges, "EXPLORER", new String[]{"Two Focuses", "Balanced Builder", "Movement Explorer", "Complete Toolkit"},
                new int[]{2, 4, 6, 9}, durable.focusVariety, "training focuses explored");
        addSeries(badges, "ENDURANCE", new String[]{"First Route", "Road Ready", "Distance Habit", "Route Keeper"},
                new int[]{1, 3, 10, 25}, endurance, "completed walk, run, or cycling sessions");
        addSeries(badges, "RECOVERY", new String[]{"Warm Start", "Recovery Rhythm", "Finish Well"},
                new int[]{1, 5, 25}, durable.warmupsCompleted + durable.cooldownsCompleted,
                "completed five-minute warm-up or cool-down timers");
        addSeries(badges, "RETURN", new String[]{"Ready Again", "Steady Return", "Resilient Rhythm"},
                new int[]{1, 3, 10}, durable.returnsAfterBreak, "healthy returns after a seven-day break");

        ArrayList<ApexTier> tiers = new ArrayList<>();
        tiers.add(new ApexTier("Foundation", "Build a repeatable rhythm at your own pace.", 1, 0, 14, 0));
        tiers.add(new ApexTier("Momentum", "Complete three sessions in any 14-day window.", 2, 3, 14, sessionsInLastDays(records, 14)));
        tiers.add(new ApexTier("Performance", "Complete six sessions in any 28-day window.", 4, 6, 28, sessionsInLastDays(records, 28)));
        tiers.add(new ApexTier("Apex", "Complete twelve sessions in any 42-day window. Recovery days count as part of the plan.", 7, 12, 42, sessionsInLastDays(records, 42)));
        return new Snapshot(sessions, minutes, endurance, durable.focusVariety, streak, xp, level, levelProgress,
                500 - levelProgress, badges, tiers);
    }

    private static void addSeries(List<Badge> badges, String family, String[] names, int[] requirements,
            int current, String unit) {
        for (int index = 0; index < Math.min(names.length, requirements.length); index++) {
            int required = requirements[index];
            badges.add(new Badge(family, names[index], "Reach " + required + " " + unit + ".", current, required));
        }
    }

    private static boolean isEnduranceTarget(String target) {
        String value = target.toLowerCase(Locale.ROOT);
        return value.contains("walk") || value.contains("run") || value.contains("cycl") || value.contains("endurance");
    }

    private static int sessionsInLastDays(List<JourneyStore.WorkoutRecord> records, int days) {
        long threshold = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L;
        int count = 0;
        for (JourneyStore.WorkoutRecord record : records) if (record.timestamp >= threshold) count++;
        return count;
    }

    private static int currentOrLatestStreak(Set<String> activeDays) {
        if (activeDays.isEmpty()) return 0;
        Calendar cursor = Calendar.getInstance();
        if (!activeDays.contains(dayKey(cursor.getTimeInMillis()))) cursor.add(Calendar.DAY_OF_YEAR, -1);
        int streak = 0;
        while (activeDays.contains(dayKey(cursor.getTimeInMillis()))) {
            streak++;
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        }
        return streak;
    }

    private static String dayKey(long millis) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(millis);
        return day.get(Calendar.YEAR) + ":" + day.get(Calendar.DAY_OF_YEAR);
    }

    private AchievementStore() { }
}
