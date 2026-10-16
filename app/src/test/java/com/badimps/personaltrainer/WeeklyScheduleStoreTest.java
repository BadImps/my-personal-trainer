package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class WeeklyScheduleStoreTest {
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        WeeklyScheduleStore.clear(context);
    }

    @Test public void upcomingWeekDoesNotOverwriteCurrentWeek() {
        boolean[] currentDays = {true, false, false, false, false, false, false};
        boolean[] upcomingDays = {false, true, false, false, false, false, false};
        List<List<String>> currentTargets = targets("Chest");
        List<List<String>> upcomingTargets = targets("Legs");
        Calendar current = Calendar.getInstance();
        current.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Calendar upcoming = (Calendar) current.clone();
        upcoming.add(Calendar.DAY_OF_YEAR, 7);

        WeeklyScheduleStore.save(context, current, currentDays, currentTargets);
        WeeklyScheduleStore.save(context, upcoming, upcomingDays, upcomingTargets);

        WeeklyScheduleStore.Schedule restoredCurrent = WeeklyScheduleStore.load(context, current, new boolean[7], new ArrayList<>());
        WeeklyScheduleStore.Schedule restoredUpcoming = WeeklyScheduleStore.load(context, upcoming, new boolean[7], new ArrayList<>());
        assertTrue(restoredCurrent.workoutDays[0]);
        assertFalse(restoredCurrent.workoutDays[1]);
        assertTrue(restoredUpcoming.workoutDays[1]);
        assertEquals("Chest", restoredCurrent.targets.get(0).get(0));
        assertEquals("Legs", restoredUpcoming.targets.get(0).get(0));
    }

    @Test public void savedScheduleIsDistinctFromPlannerFallback() {
        Calendar week = Calendar.getInstance();
        week.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        assertFalse(WeeklyScheduleStore.hasSavedSchedule(context, week));

        WeeklyScheduleStore.save(context, week, new boolean[]{true, false, false, false, false, false, false}, targets("Upper"));

        assertTrue(WeeklyScheduleStore.hasSavedSchedule(context, week));
        WeeklyScheduleStore.clear(context);
        assertFalse(WeeklyScheduleStore.hasSavedSchedule(context, week));
    }

    private static List<List<String>> targets(String focus) {
        ArrayList<List<String>> all = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            ArrayList<String> day = new ArrayList<>();
            if (i == 0) day.add(focus);
            all.add(day);
        }
        return all;
    }
}
