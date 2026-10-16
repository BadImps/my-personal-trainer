package com.badimps.personaltrainer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A deliberately small, transparent nutrition guide that always runs locally. It is not a food
 * database or a medical calculator: its ranges are only broad common-portion estimates.
 */
public final class OfflineFoodCoach {
    public static final class Result {
        public final String summary;
        public final String calories;
        public final String healthNote;
        public final String planFit;
        public final boolean hasEstimate;

        Result(String summary, String calories, String healthNote, String planFit, boolean hasEstimate) {
            this.summary = summary;
            this.calories = calories;
            this.healthNote = healthNote;
            this.planFit = planFit;
            this.hasEstimate = hasEstimate;
        }
    }

    private static final class Food {
        final String token, label, group;
        final int low, high;
        Food(String token, String label, String group, int low, int high) {
            this.token = token;
            this.label = label;
            this.group = group;
            this.low = low;
            this.high = high;
        }
    }

    private static final Food[] FOODS = {
            new Food("greek yogurt", "Greek yogurt", "protein", 100, 220),
            new Food("protein shake", "protein shake", "protein", 120, 300),
            new Food("chicken", "chicken", "protein", 180, 360),
            new Food("turkey", "turkey", "protein", 160, 330),
            new Food("salmon", "salmon", "protein", 240, 460),
            new Food("tuna", "tuna", "protein", 100, 240),
            new Food("beef", "beef", "protein", 230, 520),
            new Food("tofu", "tofu", "protein", 140, 300),
            new Food("beans", "beans", "protein", 130, 300),
            new Food("lentils", "lentils", "protein", 130, 280),
            new Food("egg", "eggs", "protein", 140, 330),
            new Food("eggs", "eggs", "protein", 140, 330),
            new Food("yogurt", "yogurt", "protein", 100, 240),
            new Food("rice", "rice", "carbohydrate", 180, 420),
            new Food("oatmeal", "oatmeal", "carbohydrate", 160, 360),
            new Food("oats", "oats", "carbohydrate", 160, 360),
            new Food("pasta", "pasta", "carbohydrate", 220, 550),
            new Food("bread", "bread", "carbohydrate", 140, 360),
            new Food("potato", "potatoes", "carbohydrate", 130, 340),
            new Food("potatoes", "potatoes", "carbohydrate", 130, 340),
            new Food("quinoa", "quinoa", "carbohydrate", 180, 380),
            new Food("banana", "banana", "produce", 80, 150),
            new Food("berries", "berries", "produce", 40, 130),
            new Food("fruit", "fruit", "produce", 50, 180),
            new Food("vegetable", "vegetables", "produce", 40, 180),
            new Food("vegetables", "vegetables", "produce", 40, 180),
            new Food("salad", "salad", "produce", 90, 450),
            new Food("broccoli", "broccoli", "produce", 30, 130),
            new Food("avocado", "avocado", "fat", 160, 360),
            new Food("nuts", "nuts", "fat", 160, 380),
            new Food("peanut butter", "peanut butter", "fat", 180, 380),
            new Food("olive oil", "olive oil", "fat", 120, 360),
            new Food("pizza", "pizza", "prepared food", 500, 1100),
            new Food("burger", "burger", "prepared food", 450, 1100),
            new Food("fries", "fries", "prepared food", 280, 700),
            new Food("fried", "fried food", "prepared food", 250, 800),
            new Food("cake", "cake", "sweet food", 280, 720),
            new Food("cookie", "cookies", "sweet food", 180, 600),
            new Food("soda", "soda", "sweet drink", 120, 320),
            new Food("juice", "juice", "sweet drink", 100, 320)
    };

    public static Result analyse(String meal, String goal, String workoutFocus, int workoutMinutes) {
        String safeMeal = meal == null ? "" : meal.trim();
        String normalized = " " + safeMeal.toLowerCase(Locale.US).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ") + " ";
        List<Food> matched = new ArrayList<>();
        Set<String> usedTokens = new LinkedHashSet<>();
        int low = 0;
        int high = 0;
        Set<String> groups = new LinkedHashSet<>();
        List<String> labels = new ArrayList<>();
        for (Food food : FOODS) {
            if (!normalized.contains(" " + food.token + " ") || overlaps(food.token, usedTokens)) continue;
            matched.add(food);
            usedTokens.add(food.token);
            low += food.low;
            high += food.high;
            groups.add(food.group);
            labels.add(food.label);
        }
        if (matched.isEmpty()) {
            return new Result("I can give a local food-pattern check, but I need a little more detail about the ingredients or a package label.",
                    "No local estimate yet", "Add the main ingredients, cooking oil or sauce, and portion size for a more useful broad range.",
                    localPlanFit(goal, false, false, workoutFocus, workoutMinutes), false);
        }
        low = Math.max(50, Math.min(low, 2_000));
        high = Math.max(low + 50, Math.min(high, 2_800));
        boolean protein = groups.contains("protein");
        boolean produce = groups.contains("produce");
        String labelList = join(labels, 3);
        String summary = "Local check found " + labelList + ". "
                + (protein ? "It includes a protein food. " : "Consider whether you want a protein food alongside it. ")
                + (produce ? "It also includes produce." : "Adding fruit or vegetables is one flexible way to round it out.");
        String health = groups.contains("prepared food") || groups.contains("sweet food") || groups.contains("sweet drink")
                ? "This can still fit a flexible plan; portions, drinks, sauces, and how often it appears matter more than a single label."
                : "This is a broad common-portion estimate. Cooking oil, sauces, brands, and portions can change it substantially.";
        return new Result(summary, low + "–" + high + " kcal (local broad estimate)", health,
                localPlanFit(goal, protein, produce, workoutFocus, workoutMinutes), true);
    }

    private static boolean overlaps(String candidate, Set<String> usedTokens) {
        for (String used : usedTokens) if (used.contains(candidate) || candidate.contains(used)) return true;
        return false;
    }

    private static String localPlanFit(String goal, boolean protein, boolean produce, String workoutFocus, int workoutMinutes) {
        String context = workoutMinutes > 0 ? " for your " + workoutMinutes + "-minute " + (workoutFocus == null ? "workout" : workoutFocus) + " session" : "";
        if ("Build muscle".equals(goal) || "Build strength".equals(goal)) {
            return protein ? "Its protein food can support a balanced eating pattern" + context + "; adjust portions to your own hunger and needs."
                    : "For a strength or muscle goal" + context + ", you may want to pair it with a protein food you enjoy.";
        }
        if ("Improve endurance".equals(goal)) return "For endurance work" + context + ", familiar food and fluids that sit comfortably are usually more useful than trying a new meal on training day.";
        if ("Reduce body fat".equals(goal)) return "For a body-fat goal, this is not a pass/fail food. Build meals around satisfaction, protein foods, produce, and your overall routine.";
        return produce ? "It can fit your flexible plan" + context + "." : "It can fit your flexible plan" + context + "; add what helps you feel satisfied and prepared.";
    }

    private static String join(List<String> labels, int maximum) {
        List<String> visible = labels.size() > maximum ? labels.subList(0, maximum) : labels;
        if (visible.size() == 1) return visible.get(0);
        if (visible.size() == 2) return visible.get(0) + " and " + visible.get(1);
        return visible.get(0) + ", " + visible.get(1) + ", and " + visible.get(2);
    }

    private OfflineFoodCoach() { }
}
