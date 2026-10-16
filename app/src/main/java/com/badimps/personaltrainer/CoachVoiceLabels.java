package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Device-local voice labels modelled after Taskmaster's voice organizer.
 * Android Voice exposes language and quality, but not a reliable gender field, so the listener's
 * previewed label is the source of truth for coach selection.
 */
public final class CoachVoiceLabels {
    private static final String FILE = "coach_voice_labels";
    private static final String PREFIX = "gender:";

    public static CoachVoiceSelector.Gender assigned(Context context, String engine, String voiceName) {
        String value = preferences(context).getString(key(engine, voiceName), null);
        if (value == null) return null;
        if ("Male".equals(value)) return CoachVoiceSelector.Gender.MALE;
        if ("Female".equals(value)) return CoachVoiceSelector.Gender.FEMALE;
        return CoachVoiceSelector.Gender.UNKNOWN;
    }

    public static String label(Context context, String engine, String voiceName) {
        CoachVoiceSelector.Gender assigned = assigned(context, engine, voiceName);
        if (assigned == CoachVoiceSelector.Gender.MALE) return "Male";
        if (assigned == CoachVoiceSelector.Gender.FEMALE) return "Female";
        if (assigned == CoachVoiceSelector.Gender.UNKNOWN) return "Unlabelled";
        return "Not reviewed";
    }

    public static void set(Context context, String engine, String voiceName, CoachVoiceSelector.Gender gender) {
        String value = gender == CoachVoiceSelector.Gender.MALE ? "Male"
                : gender == CoachVoiceSelector.Gender.FEMALE ? "Female" : "Unlabelled";
        preferences(context).edit().putString(key(engine, voiceName), value).apply();
    }

    static String key(String engine, String voiceName) {
        return PREFIX + safe(engine) + ":" + safe(voiceName);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private CoachVoiceLabels() { }
}
