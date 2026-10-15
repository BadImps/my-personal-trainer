package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CoachPreferencesTest {
    @Test public void rosterContainsThreeCoachesPerGenderWithRequestedAccents() {
        assertEquals(6, CoachPreferences.coaches().size());
        assertEquals("Spanish accent", CoachPreferences.coach("Mia").accent);
        assertEquals("US accent", CoachPreferences.coach("Amy").accent);
        assertEquals("UK accent", CoachPreferences.coach("Claudia").accent);
        assertEquals("Spanish accent", CoachPreferences.coach("Leo").accent);
        assertEquals("US accent", CoachPreferences.coach("Mark").accent);
        assertEquals("UK accent", CoachPreferences.coach("Stephen").accent);
    }

    @Test public void dialogueModesMapToDistinctRuntimePolicies() {
        assertTrue(CoachPreferences.usesVoice(CoachPreferences.FULL_DIALOGUE));
        assertTrue(CoachPreferences.usesVoice(CoachPreferences.MINIMAL));
        assertTrue(CoachPreferences.usesCompletionDialogue(CoachPreferences.FULL_DIALOGUE));
        assertFalse(CoachPreferences.usesCompletionDialogue(CoachPreferences.MINIMAL));
        assertTrue(CoachPreferences.usesBeeps(CoachPreferences.NO_DIALOGUE));
        assertFalse(CoachPreferences.usesBeeps(CoachPreferences.NO_SOUND));
    }
}
