package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

/** Regression coverage for per-set timing and adaptive recovery. */
public final class WorkoutTimingTest {
    @Test public void firstTwoSetsNeverAddRecovery() {
        WorkoutTiming.SetTracker tracker = new WorkoutTiming.SetTracker(180);
        tracker.startSet(0); tracker.completeSet(30_000);
        tracker.startSet(30_000); WorkoutTiming.SetResult second = tracker.completeSet(62_000);
        assertFalse(second.slowerThanRecentPace);
        assertEquals(180, second.recoverySeconds);
    }

    @Test public void fasterThirdSetDoesNotAddRecovery() {
        assertFalse(WorkoutTiming.isMeaningfullySlower(Arrays.asList(30_000L, 32_000L), 29_000L));
    }

    @Test public void meaningfullySlowerThirdSetAddsTenSeconds() {
        WorkoutTiming.SetTracker tracker = new WorkoutTiming.SetTracker(180);
        tracker.startSet(0); tracker.completeSet(30_000);
        tracker.startSet(30_000); tracker.completeSet(60_000);
        tracker.startSet(60_000); WorkoutTiming.SetResult third = tracker.completeSet(100_000);
        assertTrue(third.slowerThanRecentPace);
        assertEquals(10, tracker.restBonusSeconds());
        assertEquals(190, third.recoverySeconds);
    }

    @Test public void tinyClockJitterDoesNotAddRecovery() {
        assertFalse(WorkoutTiming.isMeaningfullySlower(Arrays.asList(3_000L, 3_000L), 3_200L));
    }

    @Test public void recoveryNeverExceedsEightMinutes() {
        WorkoutTiming.SetTracker tracker = new WorkoutTiming.SetTracker(475);
        tracker.startSet(0); tracker.completeSet(1_000);
        tracker.startSet(1_000); tracker.completeSet(2_000);
        tracker.startSet(2_000); tracker.completeSet(4_000);
        assertEquals(480, tracker.recoverySeconds());
    }

    @Test public void customRecoveryIsClampedToFortyFiveSecondsThroughEightMinutes() {
        assertEquals(45, WorkoutTiming.clampRestSeconds(10));
        assertEquals(45, WorkoutTiming.clampRestSeconds(45));
        assertEquals(225, WorkoutTiming.clampRestSeconds(225));
        assertEquals(480, WorkoutTiming.clampRestSeconds(480));
        assertEquals(480, WorkoutTiming.clampRestSeconds(900));
    }

    @Test public void cooldownUsesOnlyOneThreeOrFiveMinutePresets() {
        assertEquals(60, WorkoutTiming.normalizeCooldownSeconds(45));
        assertEquals(180, WorkoutTiming.normalizeCooldownSeconds(150));
        assertEquals(300, WorkoutTiming.normalizeCooldownSeconds(480));
    }

    @Test public void setTimeUsesMinuteSecondAndTenthFormat() {
        assertEquals("01:05.4", WorkoutTiming.formatElapsedMillis(65_499));
    }
}
