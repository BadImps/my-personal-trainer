package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

public final class CoachVoiceSelectorTest {
    private CoachVoiceSelector.Candidate voice(String id, Locale locale) {
        return new CoachVoiceSelector.Candidate(id, locale, Collections.emptySet(), 400, 100, false);
    }

    @Test public void maleTokenDoesNotMatchInsideFemale() {
        CoachVoiceSelector.Selection selection = CoachVoiceSelector.select(Arrays.asList(
                voice("en-gb-female-2-local", Locale.UK),
                voice("en-gb-male-1-local", Locale.UK)
        ), CoachPreferences.coach("Stephen"));

        assertEquals("en-gb-male-1-local", selection.candidate.id);
        assertEquals(CoachVoiceSelector.Gender.MALE, selection.detectedGender);
        assertTrue(selection.exactMatch());
    }

    @Test public void femaleCoachGetsFemaleVoiceWithRequestedAccent() {
        CoachVoiceSelector.Selection selection = CoachVoiceSelector.select(Arrays.asList(
                voice("en-us-female-local", Locale.US),
                voice("en-gb-female-local", Locale.UK),
                voice("en-gb-male-local", Locale.UK)
        ), CoachPreferences.coach("Claudia"));

        assertEquals("en-gb-female-local", selection.candidate.id);
        assertTrue(selection.genderMatched);
        assertTrue(selection.accentMatched);
    }

    @Test public void unknownRegionalVoiceUsesPitchFallbackInsteadOfOppositeGender() {
        CoachVoiceSelector.Selection selection = CoachVoiceSelector.select(Arrays.asList(
                voice("en-us-female-local", Locale.US),
                voice("en-us-standard-local", Locale.US)
        ), CoachPreferences.coach("Mark"));

        assertEquals("en-us-standard-local", selection.candidate.id);
        assertEquals(CoachVoiceSelector.Gender.UNKNOWN, selection.detectedGender);
        assertTrue(selection.accentMatched);
        assertTrue(selection.needsPitchFallback());
        assertFalse(selection.exactMatch());
    }

    @Test public void listenerLabelOverridesAmbiguousEngineName() {
        CoachVoiceSelector.Candidate labelledMale = new CoachVoiceSelector.Candidate(
                "en-us-x-ambiguous-local", Locale.US, Collections.emptySet(), 400, 100, false,
                CoachVoiceSelector.Gender.MALE);

        assertEquals(CoachVoiceSelector.Gender.MALE, CoachVoiceSelector.detectGender(labelledMale));
        CoachVoiceSelector.Selection selection = CoachVoiceSelector.select(
                Collections.singletonList(labelledMale), CoachPreferences.coach("Mark"));
        assertNotNull(selection);
        assertTrue(selection.genderMatched);
    }

    @Test public void knownFemaleVoiceIsNeverUsedForMaleCoach() {
        assertNull(CoachVoiceSelector.select(Collections.singletonList(
                voice("en-us-female-local", Locale.US)), CoachPreferences.coach("Mark")));
    }
}
