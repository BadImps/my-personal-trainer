package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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

    @Test public void passwordAuthenticationRejectsWrongPassword() {
        AccountStore.create(context, "athlete@example.com", "Athlete", "password1", "Female");
        assertTrue(AccountStore.authenticate(context, "athlete@example.com", "password1"));
        assertFalse(AccountStore.authenticate(context, "athlete@example.com", "wrong-password"));
        assertEquals("pbkdf2-sha256-v1", context.getSharedPreferences("trainer_profile", Context.MODE_PRIVATE)
                .getString("password_scheme", ""));
    }

    @Test public void verifiedLegacyPasswordMigratesWithoutLockingOutTheProfile() throws Exception {
        byte[] salt = new byte[] {1, 2, 3, 4};
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        StringBuilder legacyHash = new StringBuilder();
        for (byte value : digest.digest("password1".getBytes(StandardCharsets.UTF_8))) legacyHash.append(String.format("%02x", value));
        SharedPreferences.Editor editor = context.getSharedPreferences("trainer_profile", Context.MODE_PRIVATE).edit();
        editor.putBoolean("created", true).putString("email", "athlete@example.com").putString("username", "Athlete")
                .putString("salt", "01020304").putString("passwordHash", legacyHash.toString()).remove("password_scheme").commit();

        assertTrue(AccountStore.authenticate(context, "athlete@example.com", "password1"));
        assertEquals("pbkdf2-sha256-v1", context.getSharedPreferences("trainer_profile", Context.MODE_PRIVATE)
                .getString("password_scheme", ""));
    }
}
