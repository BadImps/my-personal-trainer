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

@RunWith(RobolectricTestRunner.class)
public final class BodyProfileStoreTest {
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        BodyProfileStore.clear(context);
    }

    @Test public void bodyProfileRemainsLocalAndReadable() {
        assertFalse(BodyProfileStore.configured(context));
        BodyProfileStore.save(context, 42, 178f);
        assertTrue(BodyProfileStore.configured(context));
        assertEquals(42, BodyProfileStore.age(context));
        assertEquals(178f, BodyProfileStore.heightCm(context), .01f);
        assertTrue(BodyProfileStore.summary(context).contains("42 years"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void minorsAreOutsideAdultPersonalizationScope() {
        BodyProfileStore.save(context, 17, 170f);
    }
}
