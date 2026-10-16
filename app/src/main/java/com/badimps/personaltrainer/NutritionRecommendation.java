package com.badimps.personaltrainer;

import java.util.Calendar;

/** A small, deterministic seven-day meal rotation. Ideas stay general rather than prescriptive. */
public final class NutritionRecommendation {
    public static final class Day {
        public final String breakfastTitle;
        public final String breakfastDetail;
        public final String lunchTitle;
        public final String lunchDetail;

        Day(String breakfastTitle, String breakfastDetail, String lunchTitle, String lunchDetail) {
            this.breakfastTitle = breakfastTitle;
            this.breakfastDetail = breakfastDetail;
            this.lunchTitle = lunchTitle;
            this.lunchDetail = lunchDetail;
        }
    }

    private static final Day[] DAYS = {
            new Day("Overnight oats", "Oats, yogurt or milk, berries, and seeds.", "Chicken grain bowl", "Chicken or beans, grains, colourful vegetables, and avocado."),
            new Day("Egg and vegetable toast", "Eggs or tofu, greens, and whole-grain toast.", "Tuna or chickpea wrap", "Protein, crisp vegetables, and a piece of fruit."),
            new Day("Yogurt fruit bowl", "Yogurt or soy yogurt, fruit, oats, and nuts.", "Salmon or lentil plate", "A familiar protein, potatoes or rice, and greens."),
            new Day("Peanut-banana smoothie", "Milk or soy milk, banana, oats, and peanut butter.", "Turkey or tofu salad bowl", "Protein, beans, crunchy vegetables, and bread or grains."),
            new Day("Warm porridge", "Oats, sliced fruit, cinnamon, and a protein food.", "Bean and rice bowl", "Beans, rice, salsa, vegetables, and optional yogurt."),
            new Day("Breakfast quesadilla", "Egg or beans, cheese if desired, and vegetables.", "Pasta with vegetables", "Pasta, tuna or lentils, tomato sauce, and a side salad."),
            new Day("Cottage cheese and fruit", "Cottage cheese or soy alternative, fruit, and toast.", "Roasted vegetable pita", "Hummus, roasted vegetables, and a satisfying grain." )
    };

    public static Day forToday() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int index = (day + 5) % DAYS.length; // Monday starts the rotation.
        return DAYS[index];
    }

    static Day forDayOfWeek(int calendarDayOfWeek) {
        return DAYS[(calendarDayOfWeek + 5) % DAYS.length];
    }

    private NutritionRecommendation() {}
}
