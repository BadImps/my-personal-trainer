package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/** Regression coverage for the two records that must survive an early user-controlled finish. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class CriticalFlowPersistenceTest {
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        LiveTrackingStore.clear(context);
    }

    @Test public void earlyWorkoutKeepsCompletedTimingAndDefersLaterExercises() {
        WorkoutCompletionLedger ledger = new WorkoutCompletionLedger();
        SetTimeStore.ExerciseSummary completed = new SetTimeStore.ExerciseSummary(
                "Push Up", "Chest", Collections.singletonList(new SetTimeStore.SetEntry(1, 12_400L)));
        ledger.freeze(0, completed);

        Calendar today = Calendar.getInstance();
        Coach.Exercise row = new Coach.Exercise("Dumbbell Fly", "Chest", 3, 10, 60,
                "Move with control.", "Deferred after an early finish.");
        Coach.Exercise press = new Coach.Exercise("Chest Press", "Chest", 4, 8, 75,
                "Brace and press.", "Deferred after an early finish.");
        DeferredWorkoutStore.save(context, today, Arrays.asList(row, press));

        List<SetTimeStore.ExerciseSummary> recorded = ledger.completedSummaries();
        assertEquals(1, recorded.size());
        assertEquals("Push Up", recorded.get(0).exerciseName);
        assertEquals(12_400L, recorded.get(0).totalActiveMillis);

        List<Coach.Exercise> remaining = DeferredWorkoutStore.onDate(context, today);
        assertEquals(2, remaining.size());
        assertEquals("Dumbbell Fly", remaining.get(0).name);
        assertEquals("Chest Press", remaining.get(1).name);
        DeferredWorkoutStore.removeOnDate(context, today);
        assertTrue(DeferredWorkoutStore.onDate(context, today).isEmpty());
    }

    @Test public void finishedEndurancePersistsDistanceAndPaintableRoute() {
        LiveTrackingStore.start(context, EndurancePlanStore.RUNNING);
        LiveTrackingStore.addPoint(context, 10.66000d, -61.51000d);
        LiveTrackingStore.addPoint(context, 10.66045d, -61.50970d);
        LiveTrackingStore.addPoint(context, 10.66090d, -61.50925d);

        LiveTrackingStore.Session finished = LiveTrackingStore.finish(context);
        assertNotNull(finished);
        assertEquals(3, finished.points.size());
        assertTrue(finished.distanceMeters > 100f);

        long completedAt = 1_800_000_000_123L;
        EnduranceHistoryStore.record(context, completedAt, finished);
        EnduranceHistoryStore.Entry saved = EnduranceHistoryStore.load(context, completedAt);
        assertNotNull(saved);
        assertEquals(EndurancePlanStore.RUNNING, saved.mode);
        assertEquals(3, saved.points.size());
        assertTrue(saved.distanceMeters > 100f);
        assertEquals(10.66000d, saved.points.get(0).latitude, .000001d);
        assertEquals(10.66090d, saved.points.get(2).latitude, .000001d);
        EnduranceHistoryStore.remove(context, completedAt);
    }
}
