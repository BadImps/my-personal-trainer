package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

/** Guards the weekly/rest-day contract added by the five-step plan wizard. */
@RunWith(RobolectricTestRunner.class)
public final class PlanStoreTest {
    @Test public void weeklyWorkoutAndRestDaysSurvivePlanSave() {
        Context context = RuntimeEnvironment.getApplication();
        PlanStore.clear(context);
        try {
            List<Coach.Exercise> exercises = Coach.planOptions("Chest", "Machine-assisted");
            boolean[] schedule = {true, false, true, false, true, false, false};
            PlanStore.save(context, "Weekly", "Chest", "Machine-assisted", 30, 20, 3, 10, 90, schedule, exercises);

            PlanStore.SavedPlan saved = PlanStore.load(context);
            assertNotNull(saved);
            assertEquals("Weekly", saved.mode);
            assertEquals("Chest", saved.target);
            assertEquals("Machine-assisted", saved.equipment);
            assertEquals("1,0,1,0,1,0,0", saved.weeklyDays);
            assertEquals(4, saved.exerciseNames.size());
        } finally {
            PlanStore.clear(context);
        }
    }
}
