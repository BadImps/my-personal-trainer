package com.badimps.personaltrainer;

import android.content.Context;

/** Transparent, non-clinical daily-energy estimate for the private Body page. */
public final class CalorieInsights {
    public static int estimatedDailyBurn(Context context) {
        float kg = WeightStore.latest(context);
        if ("lb".equals(WeightStore.unit(context))) kg *= 0.45359237f;
        float bmr;
        String gender = BodyProfileStore.gender(context);
        if (BodyProfileStore.GENDER_MALE.equals(gender)) {
            bmr = 10f * kg + 6.25f * BodyProfileStore.heightCm(context) - 5f * BodyProfileStore.age(context) + 5f;
        } else if (BodyProfileStore.GENDER_FEMALE.equals(gender)) {
            bmr = 10f * kg + 6.25f * BodyProfileStore.heightCm(context) - 5f * BodyProfileStore.age(context) - 161f;
        } else {
            // When gender is intentionally not supplied, use the midpoint rather than inferring it.
            bmr = 10f * kg + 6.25f * BodyProfileStore.heightCm(context) - 5f * BodyProfileStore.age(context) - 78f;
        }
        return roundTo25(bmr * activityFactor(CalorieTargetStore.movement(context)));
    }

    public static String movementExplanation(Context context) {
        String movement = CalorieTargetStore.movement(context);
        if (CalorieTargetStore.LOW.equals(movement)) return "Lower movement day";
        if (CalorieTargetStore.ACTIVE.equals(movement)) return "Higher movement day";
        return "Typical movement day";
    }

    private static float activityFactor(String movement) {
        if (CalorieTargetStore.LOW.equals(movement)) return 1.40f;
        if (CalorieTargetStore.ACTIVE.equals(movement)) return 1.80f;
        return 1.60f;
    }

    private static int roundTo25(float value) { return Math.round(value / 25f) * 25; }

    private CalorieInsights() {}
}
