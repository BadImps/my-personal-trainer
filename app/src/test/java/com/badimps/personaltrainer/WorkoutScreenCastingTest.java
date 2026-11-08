package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;

import android.provider.Settings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class WorkoutScreenCastingTest {
    @Test
    public void pickerIntentOpensAndroidCastSettings() {
        assertEquals(Settings.ACTION_CAST_SETTINGS, WorkoutScreenCasting.pickerIntent().getAction());
    }
}
