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
public final class AccountStoreTest {
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        AccountStore.clear(context);
    }

    @Test public void oldGenderCreationMigratesToDefaultNamedCoach() {
        AccountStore.create(context, "athlete@example.com", "Athlete", "password1", "Male");
        AccountStore.Profile profile = AccountStore.get(context);
        assertEquals("Leo", profile.coachName);
        assertEquals(CoachPreferences.FULL_DIALOGUE, profile.dialogueMode);
        assertFalse(profile.keepPhoneAwake);
    }

    @Test public void coachDialogueAndWakePreferencesPersistTogether() {
        AccountStore.create(context, "athlete@example.com", "Athlete", "password1", "Female");
        AccountStore.updateCoachPreferences(context, "Stephen", CoachPreferences.NO_DIALOGUE, true);
        AccountStore.Profile profile = AccountStore.get(context);
        assertEquals("Stephen", profile.coachName);
        assertEquals("Male", profile.trainer);
        assertEquals(CoachPreferences.NO_DIALOGUE, profile.dialogueMode);
        assertTrue(profile.keepPhoneAwake);
    }
}
