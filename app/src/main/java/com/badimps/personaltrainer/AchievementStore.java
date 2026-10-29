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
 * <p>The detailed history remains on-device. Authenticated members may publish only the resulting
 * level, Momentum Points, Apex lane, and earned badge names through {@link GymLobbyClient} for an
 * optional friendly leaderboard. There is no calorie requirement or penalty for a missed day, and
 * every normal workout and coach remains available regardless of this system.</p>
 */
public final class AchievementStore {
    /** A stable, human-readable rank for every level range shown in Apex and the Gym Lobby. */
    public static final class Rank {
        public final String title;
        public final int firstLevel;
        public final int lastLevel;

        Rank(String title, int firstLevel, int lastLevel) {
            this.title = title;
            this.firstLevel = firstLevel;
            this.lastLevel = lastLevel;
        }

        public String rangeLabel() {
            return lastLevel == Integer.MAX_VALUE
                    ? "LEVEL " + firstLevel + "+"
                    : "LEVELS " + firstLevel + "–" + lastLevel;
        }
    }

    public static final class Badge {
        public final String family;
        public final String name;
        public final String description;
        public final int current;
        public final int required;
        /** 0–3: Copper, Bronze, Silver, Gold. Later family milestones remain Gold. */
        public final int tier;

        Badge(String family, String name, String description, int current, int required, int tier) {
            this.family = family;
            this.name = name;
            this.description = description;
            this.current = Math.max(0, current);
            this.required = Math.max(1, required);
            this.tier = Math.max(0, Math.min(3, tier));
        }

        public boolean unlocked() { return current >= required; }
        public int progressPercent() { return Math.min(100, Math.round(100f * current / required)); }
        public String progressLabel() { return unlocked() ? "Unlocked" : current + " / " + required; }
        public String tierName() { return new String[]{"COPPER", "BRONZE", "SILVER", "GOLD"}[tier]; }
        public int tierColor() { return new int[]{0xFFB87333, 0xFFCD7F32, 0xFFC0C0C0, 0xFFFFD54F}[tier]; }
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

        public Rank rank() { return rankForLevel(level); }
    }

    public static Snapshot snapshot(Context context) {
        List<JourneyStore.WorkoutRecord> records = JourneyStore.workouts(context);
        int sessions = records.size();
        int minutes = 0;
        int endurance = 0;
        Set<String> targets = new HashSet<>();
        Set<String> activeDays = new HashSet<>();
        for (JourneyStore.WorkoutRecord record : records) {
            minutes += Math.max(0, record.minutes);
            String target = record.target == null ? "Workout" : record.target.trim();
            if (!target.isEmpty()) targets.add(target.toLowerCase(Locale.ROOT));
            if (isEnduranceTarget(target)) endurance++;
            activeDays.add(dayKey(record.timestamp));
        }
        int streak = currentOrLatestStreak(activeDays);
        int xp = sessions * 100 + minutes * 2 + endurance * 40 + Math.max(0, targets.size() - 1) * 25;
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
                new int[]{2, 4, 6, 9}, targets.size(), "training focuses explored");
        addSeries(badges, "ENDURANCE", new String[]{"First Route", "Road Ready", "Distance Habit", "Route Keeper"},
                new int[]{1, 3, 10, 25}, endurance, "completed walk, run, or cycling sessions");

        ArrayList<ApexTier> tiers = new ArrayList<>();
        tiers.add(new ApexTier("Foundation", "Build a repeatable rhythm at your own pace.", 1, 0, 14, 0));
        tiers.add(new ApexTier("Momentum", "Complete three sessions in any 14-day window.", 2, 3, 14, sessionsInLastDays(records, 14)));
        tiers.add(new ApexTier("Performance", "Complete six sessions in any 28-day window.", 4, 8, 28, sessionsInLastDays(records, 28)));
        tiers.add(new ApexTier("Apex", "Complete twelve sessions in any 42-day window. Recovery days count as part of the plan.", 7, 20, 42, sessionsInLastDays(records, 42)));
        return new Snapshot(sessions, minutes, endurance, targets.size(), streak, xp, level, levelProgress,
                500 - levelProgress, badges, tiers);
    }

    public static Rank rankForLevel(int level) {
        int safeLevel = Math.max(1, level);
        if (safeLevel <= 10) return new Rank("BEGINNER", 1, 10);
        if (safeLevel <= 25) return new Rank("NOVICE", 11, 25);
        if (safeLevel <= 50) return new Rank("CONTENDER", 26, 50);
        if (safeLevel <= 75) return new Rank("COMPETITOR", 51, 75);
        if (safeLevel <= 100) return new Rank("ATHLETE", 76, 100);
        if (safeLevel <= 150) return new Rank("ELITE", 101, 150);
        if (safeLevel <= 200) return new Rank("CHAMPION", 151, 200);
        return new Rank("APEX", 201, Integer.MAX_VALUE);
    }

    private static void addSeries(List<Badge> badges, String family, String[] names, int[] requirements,
            int current, String unit) {
        for (int index = 0; index < Math.min(names.length, requirements.length); index++) {
            int required = requirements[index];
            badges.add(new Badge(family, names[index], "Reach " + required + " " + unit + ".", current, required, Math.min(index, 3)));
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
