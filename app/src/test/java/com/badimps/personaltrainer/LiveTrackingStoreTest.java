package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class LiveTrackingStoreTest {
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        LiveTrackingStore.clear(context);
    }

    @Test public void liveRouteStaysLocalAndCanBeFinished() {
        LiveTrackingStore.start(context, EndurancePlanStore.RUNNING);
        LiveTrackingStore.addPoint(context, 40.7128d, -74.0060d);
        LiveTrackingStore.addPoint(context, 40.7138d, -74.0060d);
        LiveTrackingStore.Session active = LiveTrackingStore.current(context);
        assertTrue(active.active);
        assertEquals(EndurancePlanStore.RUNNING, active.mode);
        assertEquals(2, active.points.size());
        assertTrue(active.distanceMeters > 100f);

        LiveTrackingStore.pause(context);
        assertTrue(LiveTrackingStore.current(context).paused);
        LiveTrackingStore.resume(context);
        LiveTrackingStore.Session finished = LiveTrackingStore.finish(context);
        assertNotNull(finished);
        assertFalse(LiveTrackingStore.current(context).active);
        assertEquals(2, LiveTrackingStore.last(context).points.size());
    }

    @Test public void longRouteKeepsItsStartAndFinishRatherThanOnlyTheTail() {
        LiveTrackingStore.start(context, EndurancePlanStore.WALKING);
        for (int index = 0; index < 725; index++) {
            LiveTrackingStore.addPoint(context, 10d + index * .00001d, -61d + index * .00001d);
        }

        LiveTrackingStore.Session session = LiveTrackingStore.current(context);
        assertTrue(session.points.size() <= 360);
        assertEquals(10d, session.points.get(0).latitude, .0000001d);
        assertEquals(10d + 724 * .00001d, session.points.get(session.points.size() - 1).latitude, .0000001d);
        assertTrue(session.distanceMeters > 1000f);
    }
}
