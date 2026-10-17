package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class EndurancePlanStoreTest {
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        EndurancePlanStore.clear(context);
    }

    @Test public void plansRemainSeparatedByActivity() {
        EndurancePlanStore.save(context, EndurancePlanStore.WALKING, "Park loop", "Home", "Central Park", 3.2f, 38, "Steady");
        EndurancePlanStore.save(context, EndurancePlanStore.CYCLING, "River route", "", "River trail", 12f, 48, "Challenging");

        EndurancePlanStore.Plan walk = EndurancePlanStore.load(context, EndurancePlanStore.WALKING);
        assertNotNull(walk);
        assertEquals("Park loop", walk.name);
        assertEquals(3.2f, walk.distanceKm, .01f);
        assertEquals(48, EndurancePlanStore.load(context, EndurancePlanStore.CYCLING).minutes);

        EndurancePlanStore.remove(context, EndurancePlanStore.WALKING);
        assertNull(EndurancePlanStore.load(context, EndurancePlanStore.WALKING));
        assertNotNull(EndurancePlanStore.load(context, EndurancePlanStore.CYCLING));
    }
}
