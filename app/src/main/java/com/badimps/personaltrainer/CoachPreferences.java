package com.badimps.personaltrainer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Stable coach identities and audio-policy values shared by settings and workout playback. */
public final class CoachPreferences {
    public static final String FULL_DIALOGUE = "Full Dialogue";
    public static final String MINIMAL = "Minimal";
    public static final String NO_DIALOGUE = "No Dialogue";
    public static final String NO_SOUND = "No Sound";
    public static final String[] DIALOGUE_MODES = {FULL_DIALOGUE, MINIMAL, NO_DIALOGUE, NO_SOUND};

    public static final class CoachOption {
        public final String name, gender, accent;
        public final Locale locale;

        CoachOption(String name, String gender, String accent, Locale locale) {
            this.name = name;
            this.gender = gender;
            this.accent = accent;
            this.locale = locale;
        }
    }

    private static final List<CoachOption> COACHES = Arrays.asList(
            new CoachOption("Mia", "Female", "Spanish accent", new Locale("en", "ES")),
            new CoachOption("Amy", "Female", "US accent", Locale.US),
            new CoachOption("Claudia", "Female", "UK accent", Locale.UK),
            new CoachOption("Leo", "Male", "Spanish accent", new Locale("en", "ES")),
            new CoachOption("Mark", "Male", "US accent", Locale.US),
            new CoachOption("Stephen", "Male", "UK accent", Locale.UK)
    );

    public static List<CoachOption> coaches() { return COACHES; }

    public static CoachOption coach(String name) {
        if (name != null) {
            for (CoachOption option : COACHES) if (option.name.equals(name)) return option;
        }
        return COACHES.get(0);
    }

    public static String defaultCoach(String gender) { return "Male".equals(gender) ? "Leo" : "Mia"; }

    public static String normalizeDialogueMode(String value) {
        if (value != null) for (String mode : DIALOGUE_MODES) if (mode.equals(value)) return mode;
        return FULL_DIALOGUE;
    }

    public static boolean usesVoice(String mode) {
        return FULL_DIALOGUE.equals(mode) || MINIMAL.equals(mode);
    }

    public static boolean usesCompletionDialogue(String mode) { return FULL_DIALOGUE.equals(mode); }

    public static boolean usesBeeps(String mode) { return NO_DIALOGUE.equals(mode); }

    private CoachPreferences() { }
}
