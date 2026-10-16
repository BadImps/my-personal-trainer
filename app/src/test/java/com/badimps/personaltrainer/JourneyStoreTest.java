package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class JourneyStoreTest {
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        JourneyStore.clear(context);
    }

    @Test public void workoutAndFuelSignalsShareOneSnapshot() {
        JourneyStore.recordWorkout(context, "Chest", 47);
        JourneyStore.toggleFuelGoal(context, JourneyStore.FUEL_PROTEIN);
        JourneyStore.toggleFuelGoal(context, JourneyStore.FUEL_PLANTS);

        JourneyStore.Snapshot snapshot = JourneyStore.snapshot(context);
        assertEquals(1, snapshot.workoutsThisWeek);
        assertEquals(47, snapshot.activeMinutesThisWeek);
        assertEquals("Chest", snapshot.lastWorkoutTarget);
        assertEquals(2, snapshot.todayFuelGoals());
        assertEquals(1, snapshot.fuelDaysThisWeek);
    }

    @Test public void tappingFuelGoalAgainClearsThatGoal() {
        JourneyStore.toggleFuelGoal(context, JourneyStore.FUEL_HYDRATION);
        JourneyStore.toggleFuelGoal(context, JourneyStore.FUEL_HYDRATION);
        assertEquals(0, JourneyStore.snapshot(context).todayFuelGoals());
        assertEquals(0, JourneyStore.snapshot(context).fuelDaysThisWeek);
    }
}
