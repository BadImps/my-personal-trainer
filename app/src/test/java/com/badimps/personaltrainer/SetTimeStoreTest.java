package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Regression coverage for local completed-workout set-time history. */
@RunWith(RobolectricTestRunner.class)
public final class SetTimeStoreTest {
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        SetTimeStore.clear(context);
    }

    @Test public void exerciseSummaryRanksFastestFirstAndKeepsSetNumberTieOrder() {
        ArrayList<SetTimeStore.SetEntry> mutableEntries = new ArrayList<>(Arrays.asList(
                new SetTimeStore.SetEntry(2, 30_000L),
                new SetTimeStore.SetEntry(1, 30_000L),
                new SetTimeStore.SetEntry(3, 25_000L)));
        SetTimeStore.ExerciseSummary summary = new SetTimeStore.ExerciseSummary(
                "Squat", "Legs", mutableEntries);
        mutableEntries.clear();

        assertEquals(3, summary.setEntries.size());
        assertEquals(3, summary.leaderboard.get(0).setNumber);
        assertEquals(1, summary.leaderboard.get(1).setNumber);
        assertEquals(2, summary.leaderboard.get(2).setNumber);
        assertEquals(85_000L, summary.totalActiveMillis);
        assertTrue(summary.isFastest(3));
        assertTrue(summary.isPersonalBest(3));
        assertTrue(summary.isLeaderboardLeader(summary.leaderboard.get(0)));
        assertFalse(summary.isPersonalBest(1));
    }

    @Test public void equalFastestTimesAreBothPersonalBestsButFirstLeaderboardRowIsDeterministic() {
        SetTimeStore.ExerciseSummary summary = new SetTimeStore.ExerciseSummary("Press", "Chest", Arrays.asList(
                new SetTimeStore.SetEntry(3, 20_000L),
                new SetTimeStore.SetEntry(1, 20_000L),
                new SetTimeStore.SetEntry(2, 23_000L)));

        assertEquals(1, summary.fastestSet().get().setNumber);
        assertTrue(summary.isFastest(1));
        assertTrue(summary.isFastest(3));
        assertTrue(summary.isPersonalBest(1));
        assertTrue(summary.isPersonalBest(3));
        assertTrue(summary.isLeaderboardLeader(summary.leaderboard.get(0)));
        assertFalse(summary.isLeaderboardLeader(summary.leaderboard.get(1)));
    }

    @Test public void explicitlyRecordedWorkoutRoundTripsByTimestampAndTarget() {
        long completionTimestamp = 1_234_567L;
        List<SetTimeStore.ExerciseSummary> exercises = Arrays.asList(
                new SetTimeStore.ExerciseSummary("Squat | pause", "Legs", Arrays.asList(
                        new SetTimeStore.SetEntry(1, 32_100L),
                        new SetTimeStore.SetEntry(2, 29_900L))),
                new SetTimeStore.ExerciseSummary("Row", "Back", Arrays.asList(
                        new SetTimeStore.SetEntry(1, 41_000L))));

        SetTimeStore.recordCompletedWorkout(context, completionTimestamp, "Lower | Pull", exercises);

        SetTimeStore.WorkoutSummary loaded = SetTimeStore.loadWorkoutDetails(context, completionTimestamp);
        assertNotNull(loaded);
        assertEquals(completionTimestamp, loaded.completedAtMillis);
        assertEquals("Lower | Pull", loaded.target);
        assertEquals(2, loaded.exerciseSummaries.size());
        assertEquals("Squat | pause", loaded.exerciseSummaries.get(0).exerciseName);
        assertEquals(103_000L, loaded.totalActiveMillis);
        assertEquals(2, loaded.exerciseSummaries.get(0).leaderboard.get(0).setNumber);
        assertEquals(1, SetTimeStore.loadCompletedWorkouts(context).size());
    }

    @Test public void listIsNewestFirstAndReRecordingTimestampReplacesItsGroup() {
        SetTimeStore.recordCompletedWorkout(context, 100L, "Older", Arrays.asList(
                new SetTimeStore.ExerciseSummary("First", "Older", Arrays.asList(
                        new SetTimeStore.SetEntry(1, 1_000L)))));
        SetTimeStore.recordCompletedWorkout(context, 200L, "Newest", Arrays.asList(
                new SetTimeStore.ExerciseSummary("Second", "Newest", Arrays.asList(
                        new SetTimeStore.SetEntry(1, 2_000L)))));
        SetTimeStore.recordCompletedWorkout(context, 100L, "Replaced", Arrays.asList(
                new SetTimeStore.ExerciseSummary("Replacement", "Replaced", Arrays.asList(
                        new SetTimeStore.SetEntry(1, 3_000L)))));

        List<SetTimeStore.WorkoutSummary> history = SetTimeStore.loadCompletedWorkouts(context);
        assertEquals(2, history.size());
        assertEquals(200L, history.get(0).completedAtMillis);
        assertEquals(100L, history.get(1).completedAtMillis);
        assertEquals("Replaced", history.get(1).target);
        assertEquals("Replacement", history.get(1).exerciseSummaries.get(0).exerciseName);
    }

    @Test public void unknownOrMalformedOldPreferenceDataIsIgnoredWithoutCrashing() {
        context.getSharedPreferences("completed_set_times", Context.MODE_PRIVATE).edit()
                .putString("completed_workout_index", "old-format\nthis-is-not-a-record")
                .putString("completed_workout_99", "old-format")
                .apply();

        assertNull(SetTimeStore.loadWorkout(context, 99L));
        assertTrue(SetTimeStore.loadCompletedWorkouts(context).isEmpty());
    }
}
