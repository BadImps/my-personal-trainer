package com.badimps.personaltrainer;

import java.util.Locale;

/**
 * Educational, adult-only reference calculations. These helpers deliberately avoid diagnoses,
 * calorie prescriptions, or a claim that age determines a safe walking distance.
 */
public final class BodyInsights {
    public static boolean usesAdultReference(int age) {
        return age >= 20;
    }

    public static float toKg(float weight, String unit) {
        return "lb".equals(unit) ? weight * 0.45359237f : weight;
    }

    public static float fromKg(float kilograms, String unit) {
        return "lb".equals(unit) ? kilograms / 0.45359237f : kilograms;
    }

    public static float bmi(float heightCm, float weight, String unit) {
        if (heightCm <= 0f || weight <= 0f) return 0f;
        float heightM = heightCm / 100f;
        return toKg(weight, unit) / (heightM * heightM);
    }

    /** BMI 18.5–24.9 is an adult screening reference, not a personal target or diagnosis. */
    public static float referenceWeightLowKg(float heightCm) {
        float heightM = heightCm / 100f;
        return 18.5f * heightM * heightM;
    }

    public static float referenceWeightHighKg(float heightCm) {
        float heightM = heightCm / 100f;
        return 24.9f * heightM * heightM;
    }

    public static String referenceRange(float heightCm, String unit) {
        String label = "lb".equals(unit) ? "lb" : "kg";
        return String.format(Locale.US, "%.1f–%.1f %s", fromKg(referenceWeightLowKg(heightCm), unit),
                fromKg(referenceWeightHighKg(heightCm), unit), label);
    }

    public static String paceDistance(float milesPerHour) {
        return String.format(Locale.US, "At %.1f mph: about %.1f mi in 1 hour • %.1f mi in 2 hours",
                milesPerHour, milesPerHour, milesPerHour * 2f);
    }

    public static String weeklyActivityContext(int plannedMinutes, int age) {
        String gradual = age >= 65
                ? "Prioritize balance, mobility, and gradual progression when that feels right."
                : "Build gradually and choose a pace that lets you recover well.";
        if (plannedMinutes <= 0) return "Add a plan to compare its scheduled minutes with a general weekly activity guideline. " + gradual;
        int remaining = Math.max(0, 150 - plannedMinutes);
        String minutes = remaining == 0
                ? "Your scheduled plan reaches 150 minutes."
                : "Your scheduled plan totals " + plannedMinutes + " minutes; " + remaining + " more minutes reaches 150.";
        return minutes + " General adult guidance also includes muscle-strengthening activity on 2 days each week. " + gradual;
    }

    private BodyInsights() { }
}
