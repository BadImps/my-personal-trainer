package com.badimps.personaltrainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Chooses a coach voice without coupling the selection policy to Android's TTS classes. */
public final class CoachVoiceSelector {
    public enum Gender { FEMALE, MALE, UNKNOWN }

    public static final class Candidate {
        public final String id;
        public final Locale locale;
        public final Set<String> features;
        public final int quality;
        public final int latency;
        public final boolean networkRequired;
        /** Null means no saved label; UNKNOWN means the listener deliberately kept it unlabelled. */
        public final Gender assignedGender;

        public Candidate(String id, Locale locale, Set<String> features, int quality, int latency, boolean networkRequired) {
            this(id, locale, features, quality, latency, networkRequired, null);
        }

        public Candidate(String id, Locale locale, Set<String> features, int quality, int latency,
                         boolean networkRequired, Gender assignedGender) {
            this.id = id == null ? "" : id;
            this.locale = locale == null ? Locale.ROOT : locale;
            this.features = features == null ? Collections.emptySet() : new HashSet<>(features);
            this.quality = quality;
            this.latency = latency;
            this.networkRequired = networkRequired;
            this.assignedGender = assignedGender;
        }
    }

    public static final class Selection {
        public final Candidate candidate;
        public final Gender detectedGender;
        public final boolean genderMatched;
        public final boolean accentMatched;

        Selection(Candidate candidate, Gender detectedGender, boolean genderMatched, boolean accentMatched) {
            this.candidate = candidate;
            this.detectedGender = detectedGender;
            this.genderMatched = genderMatched;
            this.accentMatched = accentMatched;
        }

        public boolean exactMatch() { return genderMatched && accentMatched; }
        public boolean needsPitchFallback() { return !genderMatched; }
    }

    public static Selection select(List<Candidate> available, CoachPreferences.CoachOption coach) {
        if (available == null || coach == null) return null;
        Gender wanted = "Male".equals(coach.gender) ? Gender.MALE : Gender.FEMALE;
        Candidate best = null;
        Gender bestGender = Gender.UNKNOWN;
        int bestScore = Integer.MIN_VALUE;
        for (Candidate candidate : new ArrayList<>(available)) {
            if (!"en".equalsIgnoreCase(candidate.locale.getLanguage()) || isNotInstalled(candidate)) continue;
            Gender detected = detectGender(candidate);
            // A known opposite-gender voice is never an acceptable fallback. An unlabelled voice
            // can be reviewed by the listener, but a female voice must not be assigned to a man.
            if (detected != Gender.UNKNOWN && detected != wanted) continue;
            int score = score(candidate, coach.locale, wanted, detected);
            if (best == null || score > bestScore || (score == bestScore && candidate.id.compareTo(best.id) < 0)) {
                best = candidate;
                bestGender = detected;
                bestScore = score;
            }
        }
        if (best == null) return null;
        return new Selection(best, bestGender, bestGender == wanted, sameCountry(best.locale, coach.locale));
    }

    public static Gender detectGender(Candidate candidate) {
        if (candidate == null) return Gender.UNKNOWN;
        if (candidate.assignedGender != null) return candidate.assignedGender;
        StringBuilder metadata = new StringBuilder(candidate.id);
        for (String feature : candidate.features) metadata.append(' ').append(feature);
        String normalized = metadata.toString().toLowerCase(Locale.ROOT);
        // Check female first and tokenize; "male" must never match the tail of "female".
        if (hasToken(normalized, "female") || hasToken(normalized, "feminine") || hasToken(normalized, "woman")) return Gender.FEMALE;
        if (hasToken(normalized, "male") || hasToken(normalized, "masculine") || hasToken(normalized, "man")) return Gender.MALE;
        return Gender.UNKNOWN;
    }

    private static int score(Candidate candidate, Locale wantedLocale, Gender wantedGender, Gender detectedGender) {
        int score = 0;
        if (detectedGender == wantedGender) score += 10_000;
        else if (detectedGender != Gender.UNKNOWN) score -= 10_000;
        if (sameCountry(candidate.locale, wantedLocale)) score += 2_000;
        if (candidate.locale.equals(wantedLocale)) score += 500;
        if (!candidate.networkRequired) score += 120;
        score += Math.max(0, candidate.quality) / 10;
        score -= Math.max(0, candidate.latency) / 20;
        return score;
    }

    private static boolean sameCountry(Locale first, Locale second) {
        String wanted = second == null ? "" : second.getCountry();
        return !wanted.isEmpty() && first != null && wanted.equalsIgnoreCase(first.getCountry());
    }

    private static boolean isNotInstalled(Candidate candidate) {
        for (String feature : candidate.features) if ("notInstalled".equalsIgnoreCase(feature)) return true;
        return false;
    }

    private static boolean hasToken(String value, String token) {
        String[] pieces = value.split("[^a-z0-9]+");
        for (String piece : pieces) if (token.equals(piece)) return true;
        return false;
    }

    private CoachVoiceSelector() { }
}
