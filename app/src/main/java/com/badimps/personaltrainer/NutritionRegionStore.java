package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores the meal-region preference locally; no food preferences leave the device. */
public final class NutritionRegionStore {
    public static final String BALANCED = "Balanced rotation";
    public static final String TRINIDAD_TOBAGO = "Trinidad & Tobago";
    private static final String FILE = "nutrition_region";
    private static final String KEY_REGION = "region";

    public static String region(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_REGION, TRINIDAD_TOBAGO);
    }

    public static void setRegion(Context context, String value) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_REGION, value).apply();
    }

    private NutritionRegionStore() { }
}
