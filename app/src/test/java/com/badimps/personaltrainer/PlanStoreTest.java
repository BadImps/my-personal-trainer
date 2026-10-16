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
            assertEquals(180, saved.cooldownSeconds);
        } finally {
            PlanStore.clear(context);
        }
    }

    @Test public void weeklyAssignmentsAndDailyCalendarChoiceSurvivePlanSave() {
        Context context = RuntimeEnvironment.getApplication();
        PlanStore.clear(context);
        try {
            List<Coach.Exercise> exercises = Coach.planOptions("Chest", "Machine-assisted");
            boolean[] schedule = {true, false, true, false, true, false, false};
            PlanStore.save(context, "Weekly", "Chest", "Machine-assisted", 60, 20, 3, 10, 180, schedule,
                    "Chest|Arms", "Chest; ;Chest|Arms; ;Legs; ; ", "Machine Chest Press|Pec Deck Fly;;Cable Biceps Curl", 2, 1, exercises);

            PlanStore.SavedPlan saved = PlanStore.load(context);
            assertNotNull(saved);
            assertEquals("Chest|Arms", saved.focusTargets);
            assertEquals(2, saved.scheduledDay);
            assertEquals(1, saved.weekOffset);
            assertEquals(180, saved.restSeconds);
            assertEquals("Machine Chest Press|Pec Deck Fly;;Cable Biceps Curl", saved.weeklyExerciseNames);
        } finally {
            PlanStore.clear(context);
        }
    }

    @Test public void customRestAndPresetCooldownPersistSeparately() {
        Context context = RuntimeEnvironment.getApplication();
        PlanStore.clear(context);
        try {
            List<Coach.Exercise> exercises = Coach.planOptions("Chest", "Bodyweight");
            boolean[] schedule = {false, false, true, false, false, false, false};
            PlanStore.save(context, "Daily", "Chest", "Bodyweight", 45, 0, 4, 10, 75, 300,
                    schedule, "Chest", "", "", 2, 1, exercises);

            PlanStore.SavedPlan saved = PlanStore.load(context);
            assertNotNull(saved);
            assertEquals(75, saved.restSeconds);
            assertEquals(300, saved.cooldownSeconds);
        } finally {
            PlanStore.clear(context);
        }
    }
}
