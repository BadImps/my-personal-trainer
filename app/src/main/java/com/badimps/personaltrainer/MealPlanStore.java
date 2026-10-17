package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Keeps the meal ideas a person explicitly saves on this device. */
public final class MealPlanStore {
    private static final String FILE = "meal_plan";
    private static final String KEY = "selected_meals";

    public static List<String> meals(Context context) {
        Set<String> saved = preferences(context).getStringSet(KEY, new LinkedHashSet<>());
        return new ArrayList<>(saved == null ? new LinkedHashSet<>() : saved);
    }

    public static void add(Context context, String meal) {
        String value = meal == null ? "" : meal.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Meal is required");
        LinkedHashSet<String> saved = new LinkedHashSet<>(meals(context));
        saved.add(value);
        preferences(context).edit().putStringSet(KEY, saved).apply();
    }

    public static void remove(Context context, String meal) {
        LinkedHashSet<String> saved = new LinkedHashSet<>(meals(context));
        saved.remove(meal);
        preferences(context).edit().putStringSet(KEY, saved).apply();
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private MealPlanStore() { }
}
