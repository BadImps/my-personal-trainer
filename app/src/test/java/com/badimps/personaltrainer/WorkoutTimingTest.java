package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

/** Regression coverage for per-rep timing and adaptive recovery. */
public final class WorkoutTimingTest {
    @Test public void firstTwoRepsNeverAddRecovery() {
        WorkoutTiming.RepTracker tracker = new WorkoutTiming.RepTracker(180);
        tracker.startRep(0); tracker.completeRep(3_000);
        tracker.startRep(3_000); WorkoutTiming.RepResult second = tracker.completeRep(6_200);
        assertFalse(second.slowerThanRecentPace);
        assertEquals(180, second.recoverySeconds);
    }

    @Test public void fasterThirdRepDoesNotAddRecovery() {
        assertFalse(WorkoutTiming.isMeaningfullySlower(Arrays.asList(3_000L, 3_200L), 2_900L));
    }

    @Test public void meaningfullySlowerThirdRepAddsTenSeconds() {
        WorkoutTiming.RepTracker tracker = new WorkoutTiming.RepTracker(180);
        tracker.startRep(0); tracker.completeRep(3_000);
        tracker.startRep(3_000); tracker.completeRep(6_000);
        tracker.startRep(6_000); WorkoutTiming.RepResult third = tracker.completeRep(10_000);
        assertTrue(third.slowerThanRecentPace);
        assertEquals(10, tracker.restBonusSeconds());
        assertEquals(190, third.recoverySeconds);
    }

    @Test public void tinyClockJitterDoesNotAddRecovery() {
        assertFalse(WorkoutTiming.isMeaningfullySlower(Arrays.asList(3_000L, 3_000L), 3_200L));
    }

    @Test public void recoveryNeverExceedsFiveMinutes() {
        WorkoutTiming.RepTracker tracker = new WorkoutTiming.RepTracker(295);
        tracker.startRep(0); tracker.completeRep(1_000);
        tracker.startRep(1_000); tracker.completeRep(2_000);
        tracker.startRep(2_000); tracker.completeRep(4_000);
        assertEquals(300, tracker.recoverySeconds());
    }

    @Test public void repTimeUsesMinuteSecondAndTenthFormat() {
        assertEquals("01:05.4", WorkoutTiming.formatRepMillis(65_499));
    }
}
