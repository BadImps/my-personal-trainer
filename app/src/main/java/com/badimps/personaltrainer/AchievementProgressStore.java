package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Durable, on-device progression data for Apex. JourneyStore remains the editable activity
 * timeline; this store retains lifetime achievement totals even after that timeline is trimmed.
 */
public final class AchievementProgressStore {
    private static final String FILE = "achievement_progress_v1";
    private static final String SEP = "\n";
    private static final Object LOCK = new Object();
    private static final int CURRENT_SCHEMA = 1;

    public static final class Mastery {
        public final int sessions;
        public final String material;
        public final int goldTier;
        public final int nextThreshold;
        public final boolean complete;

        Mastery(int sessions, String material, int goldTier, int nextThreshold, boolean complete) {
            this.sessions = sessions;
            this.material = material;
            this.goldTier = goldTier;
            this.nextThreshold = nextThreshold;
            this.complete = complete;
        }

        public String label() {
            if ("Gold".equals(material) && goldTier > 0) return "GOLD · TIER " + goldTier;
            return material.toUpperCase(Locale.ROOT);
        }

        public String progressLabel() {
            return complete ? sessions + " sessions · Legacy Mastery complete"
                    : sessions + " / " + nextThreshold + " sessions";
        }

        public int progressPercent() {
            if (complete) return 100;
            int previous = previousThreshold(sessions);
            int span = Math.max(1, nextThreshold - previous);
            return Math.max(0, Math.min(100, Math.round(100f * (sessions - previous) / span)));
        }
    }

    public static final class Quest {
        public final String id;
        public final String title;
        public final String detail;
        public final int current;
        public final int required;

        Quest(String id, String title, String detail, int current, int required) {
            this.id = id;
            this.title = title;
            this.detail = detail;
            this.current = Math.max(0, current);
            this.required = Math.max(1, required);
        }

        public boolean complete() { return current >= required; }
        public int progressPercent() { return Math.min(100, Math.round(100f * current / required)); }
        public String progressLabel() { return complete() ? "Complete" : current + " / " + required; }
    }

    public static final class ChapterGoal {
        public final String label;
        public final int current;
        public final int required;

        ChapterGoal(String label, int current, int required) {
            this.label = label;
            this.current = Math.max(0, current);
            this.required = Math.max(1, required);
        }

        public boolean complete() { return current >= required; }
    }

    public static final class Chapter {
        public final String id;
        public final String title;
        public final String description;
        public final long startedAt;
        public final boolean paused;
        public final boolean claimed;
        public final List<ChapterGoal> goals;

        Chapter(String id, String title, String description, long startedAt, boolean paused,
                boolean claimed, List<ChapterGoal> goals) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.startedAt = startedAt;
            this.paused = paused;
            this.claimed = claimed;
            this.goals = Collections.unmodifiableList(goals);
        }

        public boolean complete() {
            if (goals.isEmpty()) return false;
            for (ChapterGoal goal : goals) if (!goal.complete()) return false;
            return true;
        }

        public int completedGoals() {
            int count = 0;
            for (ChapterGoal goal : goals) if (goal.complete()) count++;
            return count;
        }
    }

    public static final class Snapshot {
        public final int lifetimeSessions;
        public final int lifetimeMinutes;
        public final int lifetimeEnduranceSessions;
        public final int focusVariety;
        public final int warmupsCompleted;
        public final int cooldownsCompleted;
        public final int returnsAfterBreak;
        public final Mastery mastery;
        public final List<Quest> quests;
        public final Chapter chapter;

        Snapshot(int sessions, int minutes, int endurance, int focusVariety, int warmups,
                int cooldowns, int returns, Mastery mastery, List<Quest> quests, Chapter chapter) {
            this.lifetimeSessions = sessions;
            this.lifetimeMinutes = minutes;
            this.lifetimeEnduranceSessions = endurance;
            this.focusVariety = focusVariety;
            this.warmupsCompleted = warmups;
            this.cooldownsCompleted = cooldowns;
            this.returnsAfterBreak = returns;
            this.mastery = mastery;
            this.quests = Collections.unmodifiableList(quests);
            this.chapter = chapter;
        }
    }

    static void recordCompletedSession(Context context, long timestamp, String target, int minutes) {
        synchronized (LOCK) {
            ensureMigrated(context);
            SharedPreferences preferences = preferences(context);
            Set<String> processed = values(preferences.getString("processed_sessions", ""));
            String id = sessionId(timestamp, target, minutes);
            if (processed.contains(id)) return;
            long previous = preferences.getLong("last_completed_at", 0L);
            processed.add(id);
            Set<String> focuses = values(preferences.getString("focuses", ""));
            String focus = normalizeFocus(target);
            if (!focus.isEmpty()) focuses.add(focus);
            SharedPreferences.Editor edit = preferences.edit()
                    .putInt("lifetime_sessions", preferences.getInt("lifetime_sessions", 0) + 1)
                    .putInt("lifetime_minutes", preferences.getInt("lifetime_minutes", 0) + Math.max(1, minutes))
                    .putString("processed_sessions", join(processed))
                    .putString("focuses", join(focuses))
                    .putLong("last_completed_at", Math.max(previous, timestamp));
            if (isEndurance(target)) edit.putInt("lifetime_endurance", preferences.getInt("lifetime_endurance", 0) + 1);
            if (previous > 0L && timestamp > previous && timestamp - previous >= 7L * 24L * 60L * 60L * 1000L) {
                edit.putInt("returns_after_break", preferences.getInt("returns_after_break", 0) + 1);
            }
            edit.commit();
        }
    }

    /** Records only a fully completed five-minute preparation/recovery timer, at most once per day. */
    public static void recordRecoveryCompletion(Context context, boolean warmup) {
        synchronized (LOCK) {
            ensureMigrated(context);
            SharedPreferences preferences = preferences(context);
            String prefix = warmup ? "warmup" : "cooldown";
            Set<String> processed = values(preferences.getString("processed_recovery", ""));
            String key = prefix + ":" + dayKey(System.currentTimeMillis());
            if (processed.contains(key)) return;
            processed.add(key);
            String eventsKey = warmup ? "warmup_events" : "cooldown_events";
            List<String> events = lines(preferences.getString(eventsKey, ""));
            events.add(String.valueOf(System.currentTimeMillis()));
            preferences.edit()
                    .putString("processed_recovery", join(processed))
                    .putString(eventsKey, join(events))
                    .putInt(warmup ? "warmups" : "cooldowns", preferences.getInt(warmup ? "warmups" : "cooldowns", 0) + 1)
                    .commit();
        }
    }

    public static Snapshot snapshot(Context context) {
        synchronized (LOCK) {
            ensureMigrated(context);
            SharedPreferences preferences = preferences(context);
            List<JourneyStore.WorkoutRecord> records = JourneyStore.workouts(context);
            int sessions = preferences.getInt("lifetime_sessions", 0);
            int minutes = preferences.getInt("lifetime_minutes", 0);
            int endurance = preferences.getInt("lifetime_endurance", 0);
            int warmups = preferences.getInt("warmups", 0);
            int cooldowns = preferences.getInt("cooldowns", 0);
            List<Quest> quests = weeklyQuests(records, preferences);
            return new Snapshot(sessions, minutes, endurance,
                    values(preferences.getString("focuses", "")).size(), warmups, cooldowns,
                    preferences.getInt("returns_after_break", 0), masteryFor(sessions), quests,
                    activeChapter(records, preferences));
        }
    }

    public static void selectChapter(Context context, String id) {
        if (!isKnownChapter(id)) return;
        synchronized (LOCK) {
            ensureMigrated(context);
            preferences(context).edit().putString("active_chapter", id)
                    .putLong("chapter_started_at", System.currentTimeMillis())
                    .putBoolean("chapter_paused", false).commit();
        }
    }

    public static void setChapterPaused(Context context, boolean paused) {
        synchronized (LOCK) { preferences(context).edit().putBoolean("chapter_paused", paused).commit(); }
    }

    public static void claimChapter(Context context, String id) {
        synchronized (LOCK) {
            SharedPreferences preferences = preferences(context);
            Set<String> claimed = values(preferences.getString("claimed_chapters", ""));
            claimed.add(id);
            preferences.edit().putString("claimed_chapters", join(claimed)).commit();
        }
    }

    static void clear(Context context) { preferences(context).edit().clear().commit(); }

    public static Mastery masteryFor(int sessions) {
        int value = Math.max(0, sessions);
        if (value < 5) return new Mastery(value, "Starter", 0, 5, false);
        if (value < 20) return new Mastery(value, "Copper", 0, 20, false);
        if (value < 75) return new Mastery(value, "Bronze", 0, 75, false);
        if (value < 120) return new Mastery(value, "Silver", 0, 120, false);
        int tier = Math.min(100, (value - 120) / 20);
        boolean complete = tier >= 100;
        return new Mastery(value, "Gold", tier, complete ? 2120 : 120 + (tier + 1) * 20, complete);
    }

    private static List<Quest> weeklyQuests(List<JourneyStore.WorkoutRecord> records, SharedPreferences preferences) {
        long start = weekStart(System.currentTimeMillis());
        int guided = 0;
        int endurance = 0;
        for (JourneyStore.WorkoutRecord record : records) if (record.timestamp >= start) {
            if (isEndurance(record.target)) endurance++; else guided++;
        }
        int recovery = countEventsAfter(preferences.getString("warmup_events", ""), start)
                + countEventsAfter(preferences.getString("cooldown_events", ""), start);
        ArrayList<Quest> quests = new ArrayList<>();
        quests.add(new Quest("guided", "Guided momentum", "Complete two guided workouts this week.", guided, 2));
        quests.add(new Quest("endurance", "Route ready", "Complete one walk, run, or ride this week.", endurance, 1));
        quests.add(new Quest("recovery", "Recover with purpose", "Complete a five-minute warm-up or cool-down this week.", recovery, 1));
        return quests;
    }

    private static Chapter activeChapter(List<JourneyStore.WorkoutRecord> records, SharedPreferences preferences) {
        String id = preferences.getString("active_chapter", "");
        if (!isKnownChapter(id)) return null;
        long started = preferences.getLong("chapter_started_at", System.currentTimeMillis());
        int guided = 0, endurance = 0, minutes = 0;
        Set<String> focuses = new HashSet<>();
        for (JourneyStore.WorkoutRecord record : records) if (record.timestamp >= started) {
            minutes += Math.max(0, record.minutes);
            if (isEndurance(record.target)) endurance++; else guided++;
            String focus = normalizeFocus(record.target);
            if (!focus.isEmpty()) focuses.add(focus);
        }
        int warmups = countEventsAfter(preferences.getString("warmup_events", ""), started);
        int cooldowns = countEventsAfter(preferences.getString("cooldown_events", ""), started);
        ArrayList<ChapterGoal> goals = new ArrayList<>();
        String title;
        String description;
        if ("momentum".equals(id)) {
            title = "Build Momentum"; description = "A flexible four-milestone route for a repeatable rhythm.";
            goals.add(new ChapterGoal("Complete guided sessions", guided, 4));
            goals.add(new ChapterGoal("Complete any sessions", guided + endurance, 6));
            goals.add(new ChapterGoal("Explore training focuses", focuses.size(), 3));
            goals.add(new ChapterGoal("Complete recovery timers", warmups + cooldowns, 2));
        } else if ("endurance".equals(id)) {
            title = "Endurance Path"; description = "Build route confidence at your own pace.";
            goals.add(new ChapterGoal("Complete endurance routes", endurance, 3));
            goals.add(new ChapterGoal("Log endurance minutes", enduranceMinutes(records, started), 60));
            goals.add(new ChapterGoal("Complete guided sessions", guided, 2));
            goals.add(new ChapterGoal("Complete a recovery timer", warmups + cooldowns, 1));
        } else if ("balanced".equals(id)) {
            title = "Balanced Athlete"; description = "Combine strength, endurance, variety, and recovery.";
            goals.add(new ChapterGoal("Complete guided sessions", guided, 2));
            goals.add(new ChapterGoal("Complete endurance routes", endurance, 2));
            goals.add(new ChapterGoal("Explore training focuses", focuses.size(), 2));
            goals.add(new ChapterGoal("Complete recovery timers", warmups + cooldowns, 2));
        } else {
            title = "Foundation Reset"; description = "A supportive four-milestone route. Pause any time; nothing is lost.";
            goals.add(new ChapterGoal("Complete any sessions", guided + endurance, 4));
            goals.add(new ChapterGoal("Log active minutes", minutes, 60));
            goals.add(new ChapterGoal("Complete a warm-up", warmups, 1));
            goals.add(new ChapterGoal("Complete a cool-down", cooldowns, 1));
        }
        boolean claimed = values(preferences.getString("claimed_chapters", "")).contains(id);
        return new Chapter(id, title, description, started, preferences.getBoolean("chapter_paused", false), claimed, goals);
    }

    private static int enduranceMinutes(List<JourneyStore.WorkoutRecord> records, long started) {
        int total = 0;
        for (JourneyStore.WorkoutRecord record : records) if (record.timestamp >= started && isEndurance(record.target)) total += Math.max(0, record.minutes);
        return total;
    }

    private static void ensureMigrated(Context context) {
        SharedPreferences preferences = preferences(context);
        if (preferences.getInt("schema", 0) >= CURRENT_SCHEMA) return;
        Set<String> ids = new HashSet<>();
        Set<String> focuses = new HashSet<>();
        int sessions = 0, minutes = 0, endurance = 0;
        long last = 0L;
        for (JourneyStore.WorkoutRecord record : JourneyStore.workouts(context)) {
            String id = sessionId(record.timestamp, record.target, record.minutes);
            if (!ids.add(id)) continue;
            sessions++;
            minutes += Math.max(1, record.minutes);
            if (isEndurance(record.target)) endurance++;
            String focus = normalizeFocus(record.target);
            if (!focus.isEmpty()) focuses.add(focus);
            last = Math.max(last, record.timestamp);
        }
        preferences.edit().putInt("schema", CURRENT_SCHEMA).putInt("lifetime_sessions", sessions)
                .putInt("lifetime_minutes", minutes).putInt("lifetime_endurance", endurance)
                .putString("processed_sessions", join(ids)).putString("focuses", join(focuses))
                .putLong("last_completed_at", last).commit();
    }

    private static int previousThreshold(int sessions) {
        if (sessions < 5) return 0;
        if (sessions < 20) return 5;
        if (sessions < 75) return 20;
        if (sessions < 120) return 75;
        return 120 + Math.min(100, Math.max(0, (sessions - 120) / 20)) * 20;
    }

    private static boolean isKnownChapter(String id) {
        return "foundation".equals(id) || "momentum".equals(id) || "endurance".equals(id) || "balanced".equals(id);
    }

    private static int countEventsAfter(String value, long timestamp) {
        int count = 0;
        for (String line : lines(value)) try { if (Long.parseLong(line) >= timestamp) count++; } catch (NumberFormatException ignored) { }
        return count;
    }

    private static String sessionId(long timestamp, String target, int minutes) {
        return timestamp + ":" + Math.max(1, minutes) + ":" + normalizeFocus(target);
    }

    private static boolean isEndurance(String target) {
        String value = target == null ? "" : target.toLowerCase(Locale.ROOT);
        return value.contains("walk") || value.contains("run") || value.contains("cycl") || value.contains("endurance");
    }

    private static String normalizeFocus(String target) {
        return target == null ? "" : target.toLowerCase(Locale.ROOT).replace('|', ' ').replace('\n', ' ').trim();
    }

    private static long weekStart(long now) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() > now) calendar.add(Calendar.DAY_OF_MONTH, -7);
        return calendar.getTimeInMillis();
    }

    private static String dayKey(long timestamp) {
        Calendar calendar = Calendar.getInstance(); calendar.setTimeInMillis(timestamp);
        return calendar.get(Calendar.YEAR) + ":" + calendar.get(Calendar.DAY_OF_YEAR);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static Set<String> values(String value) { return new HashSet<>(lines(value)); }

    private static List<String> lines(String value) {
        ArrayList<String> lines = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) return lines;
        for (String line : value.split(SEP)) if (!line.trim().isEmpty()) lines.add(line.trim());
        return lines;
    }

    private static String join(Iterable<String> values) {
        ArrayList<String> sorted = new ArrayList<>();
        for (String value : values) if (value != null && !value.trim().isEmpty()) sorted.add(value.trim());
        Collections.sort(sorted);
        return String.join(SEP, sorted);
    }

    private AchievementProgressStore() { }
}
