package com.badimps.personaltrainer;

import java.util.Calendar;

/** A deterministic seven-day rotation with transparent, ingredient-level reference facts. */
public final class NutritionRecommendation {
    public static final class Component {
        public final String name, serving;
        public final int kcal, sodium;
        public final double protein, carbs, fibre, fat;
        Component(String name, String serving, int kcal, double protein, double carbs, double fibre, double fat, int sodium) {
            this.name = name; this.serving = serving; this.kcal = kcal; this.protein = protein; this.carbs = carbs;
            this.fibre = fibre; this.fat = fat; this.sodium = sodium;
        }
    }

    public static final class Meal {
        public final String title, detail;
        public final Component[] components;
        Meal(String title, String detail, Component... components) { this.title = title; this.detail = detail; this.components = components; }
        public int kcal() { int value = 0; for (Component item : components) value += item.kcal; return value; }
        public int sodium() { int value = 0; for (Component item : components) value += item.sodium; return value; }
        public double protein() { return total(0); }
        public double carbs() { return total(1); }
        public double fibre() { return total(2); }
        public double fat() { return total(3); }
        private double total(int nutrient) { double value = 0; for (Component item : components) value += nutrient == 0 ? item.protein : nutrient == 1 ? item.carbs : nutrient == 2 ? item.fibre : item.fat; return value; }
    }

    public static final class Day {
        public final Meal breakfast, lunch;
        public final String breakfastTitle, breakfastDetail, lunchTitle, lunchDetail;
        Day(Meal breakfast, Meal lunch) { this.breakfast = breakfast; this.lunch = lunch; breakfastTitle = breakfast.title; breakfastDetail = breakfast.detail; lunchTitle = lunch.title; lunchDetail = lunch.detail; }
    }

    private static Component c(String name, String serving, int kcal, double protein, double carbs, double fibre, double fat, int sodium) { return new Component(name, serving, kcal, protein, carbs, fibre, fat, sodium); }
    private static Meal m(String title, String detail, Component... components) { return new Meal(title, detail, components); }

    private static final Day[] DAYS = {
            new Day(
                    m("Overnight oats", "Oats, yogurt, berries, chia, and peanut butter.", c("Rolled oats", "40 g", 156, 6.8, 26.5, 4.2, 2.8, 2), c("Plain Greek yogurt", "170 g", 100, 17.5, 6.0, 0, .7, 65), c("Mixed berries", "80 g", 38, .6, 9.0, 3.5, .2, 1), c("Chia seeds", "12 g", 58, 2.0, 5.0, 4.1, 3.7, 2), c("Peanut butter", "16 g", 94, 4.0, 3.2, 1.0, 8.0, 3)),
                    m("Chicken grain bowl", "Chicken, brown rice, vegetables, avocado, and olive oil.", c("Grilled chicken breast", "120 g", 198, 37.2, 0, 0, 4.3, 89), c("Cooked brown rice", "150 g", 185, 4.1, 38.4, 2.4, 1.5, 6), c("Mixed vegetables", "120 g", 42, 2.4, 8.5, 3.4, .5, 45), c("Avocado", "50 g", 80, 1.0, 4.3, 3.4, 7.4, 4), c("Olive oil", "5 g", 44, 0, 0, 0, 5.0, 0))),
            new Day(
                    m("Egg and vegetable toast", "Eggs, whole-grain toast, callaloo or spinach, and tomato.", c("Boiled eggs", "2 large", 156, 12.6, 1.2, 0, 10.6, 124), c("Whole-grain toast", "2 slices", 160, 7.2, 28.0, 4.0, 2.4, 290), c("Cooked callaloo", "80 g", 17, 1.7, 3.0, 1.7, .2, 14), c("Tomato", "60 g", 11, .5, 2.3, .7, .1, 3)),
                    m("Tuna or chickpea wrap", "Tuna, chickpeas, crisp vegetables, and a whole-wheat wrap.", c("Tuna in water", "85 g", 94, 20.4, 0, 0, .7, 34), c("Chickpeas", "60 g", 98, 5.3, 16.4, 4.6, 1.6, 4), c("Whole-wheat wrap", "65 g", 190, 6.0, 33.0, 4.0, 4.0, 360), c("Salad vegetables", "100 g", 25, 1.2, 5.0, 2.0, .2, 30))),
            new Day(
                    m("Yogurt fruit bowl", "Yogurt, mango or papaya, oats, and nuts.", c("Plain Greek yogurt", "170 g", 100, 17.5, 6.0, 0, .7, 65), c("Mango or papaya", "140 g", 84, 1.0, 21.0, 2.2, .5, 3), c("Rolled oats", "30 g", 117, 5.1, 19.9, 3.2, 2.1, 2), c("Mixed nuts", "15 g", 91, 3.0, 3.0, 1.4, 8.0, 1)),
                    m("Salmon or lentil plate", "Salmon, sweet potato, greens, and lentils.", c("Baked salmon", "110 g", 227, 24.3, 0, 0, 13.6, 65), c("Baked sweet potato", "160 g", 144, 3.2, 33.1, 5.3, .3, 58), c("Cooked lentils", "80 g", 93, 7.2, 16.1, 6.3, .3, 2), c("Cooked greens", "80 g", 18, 2.4, 3.0, 1.9, .2, 50))),
            new Day(
                    m("Peanut-banana smoothie", "Milk, banana, oats, peanut butter, and cinnamon.", c("Low-fat milk", "250 ml", 105, 8.5, 12.0, 0, 2.5, 115), c("Banana", "118 g", 105, 1.3, 27.0, 3.1, .4, 1), c("Rolled oats", "30 g", 117, 5.1, 19.9, 3.2, 2.1, 2), c("Peanut butter", "16 g", 94, 4.0, 3.2, 1.0, 8.0, 3)),
                    m("Turkey or tofu salad bowl", "Turkey, black beans, salad vegetables, avocado, and a grain.", c("Roasted turkey breast", "110 g", 149, 33.0, 0, 0, 1.1, 62), c("Black beans", "80 g", 106, 7.1, 19.0, 7.0, .4, 1), c("Cooked quinoa", "100 g", 120, 4.4, 21.3, 2.8, 1.9, 7), c("Avocado and salad", "100 g", 110, 2.0, 8.0, 5.0, 7.4, 20))),
            new Day(
                    m("Warm porridge", "Oats, milk, banana, cinnamon, and egg.", c("Cooked oats", "235 g", 166, 5.9, 28.1, 4.0, 3.6, 7), c("Low-fat milk", "125 ml", 53, 4.3, 6.0, 0, 1.3, 58), c("Banana", "100 g", 89, 1.1, 22.8, 2.6, .3, 1), c("Boiled egg", "1 large", 78, 6.3, .6, 0, 5.3, 62)),
                    m("Bean and rice bowl", "Kidney beans, rice, vegetables, salsa, and yogurt.", c("Kidney beans", "120 g", 152, 10.4, 27.4, 7.7, .6, 1), c("Cooked brown rice", "130 g", 160, 3.5, 33.3, 2.1, 1.3, 5), c("Vegetables and salsa", "130 g", 45, 2.0, 9.0, 3.0, .3, 170), c("Plain yogurt", "60 g", 38, 3.2, 4.2, 0, 1.0, 42))),
            new Day(
                    m("Breakfast quesadilla", "Egg, black beans, cheese, vegetables, and a corn tortilla.", c("Egg", "1 large", 78, 6.3, .6, 0, 5.3, 62), c("Black beans", "60 g", 79, 5.3, 14.2, 5.2, .3, 1), c("Corn tortilla", "2 small", 104, 2.8, 21.6, 3.0, 1.4, 22), c("Cheese and vegetables", "55 g", 99, 6.0, 3.0, .6, 6.6, 175)),
                    m("Pasta with vegetables", "Whole-wheat pasta, tuna, tomato sauce, and vegetables.", c("Cooked whole-wheat pasta", "160 g", 238, 10.1, 49.4, 7.8, 1.4, 8), c("Tuna in water", "85 g", 94, 20.4, 0, 0, .7, 34), c("Tomato sauce", "100 g", 39, 1.5, 8.0, 1.7, .3, 330), c("Mixed vegetables", "120 g", 42, 2.4, 8.5, 3.4, .5, 45))),
            new Day(
                    m("Cottage cheese and fruit", "Cottage cheese, tropical fruit, toast, and seeds.", c("Low-fat cottage cheese", "170 g", 138, 19.0, 6.0, 0, 3.9, 624), c("Pineapple or papaya", "140 g", 65, .7, 16.3, 2.0, .2, 3), c("Whole-grain toast", "2 slices", 160, 7.2, 28.0, 4.0, 2.4, 290), c("Pumpkin seeds", "10 g", 56, 3.0, 1.5, .6, 4.9, 1)),
                    m("Roasted vegetable pita", "Hummus, roasted vegetables, whole-wheat pita, and fruit.", c("Whole-wheat pita", "65 g", 170, 6.0, 35.0, 4.0, 1.5, 310), c("Hummus", "60 g", 100, 4.7, 8.5, 2.4, 6.0, 220), c("Roasted vegetables", "150 g", 75, 2.5, 14.0, 4.0, 2.0, 70), c("Orange", "1 medium", 62, 1.2, 15.4, 3.1, .2, 0)))
    };

    /** Trinidad & Tobago rotation: familiar breakfasts and lunches, each with the same transparent component facts. */
    private static final Day[] TRINIDAD_TOBAGO_DAYS = {
            new Day(
                    m("Sada roti with channa", "Sada roti, curried channa, grilled plantain, and cucumber.", c("Sada roti", "1 medium", 210, 6.5, 38.0, 2.2, 4.0, 295), c("Curried channa", "150 g", 220, 10.5, 32.0, 8.5, 6.0, 230), c("Grilled plantain", "80 g", 98, 1.0, 25.5, 1.8, .4, 3), c("Cucumber", "80 g", 12, .5, 2.9, .4, .1, 2)),
                    m("Chicken pelau bowl", "Chicken pelau, pigeon peas, vegetables, and avocado.", c("Chicken pelau", "250 g", 410, 23.0, 48.0, 4.1, 13.0, 480), c("Pigeon peas", "60 g", 73, 4.1, 13.7, 3.8, .2, 1), c("Mixed vegetables", "120 g", 42, 2.4, 8.5, 3.4, .5, 45), c("Avocado", "50 g", 80, 1.0, 4.3, 3.4, 7.4, 4))),
            new Day(
                    m("Coconut bake and egg", "Coconut bake, boiled eggs, tomato, and fruit.", c("Coconut bake", "90 g", 289, 5.5, 44.6, 2.1, 9.9, 324), c("Boiled eggs", "2 large", 156, 12.6, 1.2, 0, 10.6, 124), c("Tomato and cucumber", "100 g", 18, .9, 3.8, 1.1, .2, 4), c("Orange", "1 medium", 62, 1.2, 15.4, 3.1, .2, 0)),
                    m("Curry chicken roti", "Whole-wheat roti, curry chicken, channa, and salad.", c("Whole-wheat roti", "90 g", 267, 9.0, 45.0, 6.3, 6.3, 387), c("Curry chicken", "130 g", 239, 24.9, 7.2, 1.3, 11.7, 403), c("Curried channa", "70 g", 103, 4.9, 14.9, 4.0, 2.8, 107), c("Salad vegetables", "100 g", 25, 1.2, 5.0, 2.0, .2, 30))),
            new Day(
                    m("Cornmeal porridge and egg", "Cornmeal porridge, milk, boiled egg, and papaya.", c("Cornmeal porridge", "250 g", 178, 4.0, 37.8, 2.5, 1.3, 52), c("Low-fat milk", "125 ml", 53, 4.3, 6.0, 0, 1.3, 58), c("Boiled egg", "1 large", 78, 6.3, .6, 0, 5.3, 62), c("Papaya", "140 g", 60, .7, 15.1, 2.4, .4, 11)),
                    m("Stew chicken, rice and callaloo", "Stewed chicken, rice, callaloo, and pumpkin.", c("Stew chicken", "130 g", 255, 25.4, 10.7, 1.0, 12.0, 527), c("Cooked brown rice", "150 g", 185, 4.1, 38.4, 2.4, 1.5, 6), c("Callaloo stew", "120 g", 110, 4.1, 9.0, 3.8, 7.0, 252), c("Caribbean pumpkin", "100 g", 20, .7, 4.9, 1.1, .1, 1))),
            new Day(
                    m("Bake and smoked herring", "Baked flour bread, smoked herring, tomato, and cucumber.", c("Coconut bake", "80 g", 257, 4.9, 39.6, 1.9, 8.8, 288), c("Smoked herring", "55 g", 119, 17.1, 0, 0, 5.0, 690), c("Tomato and cucumber", "120 g", 22, 1.1, 4.6, 1.4, .2, 5), c("Avocado", "40 g", 64, .8, 3.4, 2.7, 5.9, 3)),
                    m("Fish and provision plate", "Grilled fish, boiled green fig, dasheen, and vegetables.", c("Grilled red snapper", "130 g", 166, 34.2, 0, 0, 2.1, 78), c("Boiled green banana", "130 g", 116, 1.4, 29.6, 3.4, .4, 1), c("Boiled dasheen", "100 g", 142, .5, 34.6, 5.1, .1, 1), c("Steamed vegetables", "120 g", 42, 2.4, 8.5, 3.4, .5, 45))),
            new Day(
                    m("Doubles with fruit", "Doubles with curried channa, cucumber chutney, and fruit.", c("Bara", "2 small", 220, 5.8, 32.0, 2.0, 8.8, 280), c("Curried channa", "120 g", 176, 8.4, 25.6, 6.8, 4.8, 184), c("Cucumber chutney", "40 g", 24, .4, 5.0, .5, .1, 80), c("Mango", "100 g", 60, .8, 15.0, 1.6, .4, 1)),
                    m("Channa and aloo roti", "Roti with curried chickpeas, potato, vegetables, and yogurt.", c("Whole-wheat roti", "90 g", 267, 9.0, 45.0, 6.3, 6.3, 387), c("Channa and aloo", "180 g", 250, 10.0, 38.0, 8.3, 6.5, 290), c("Mixed vegetables", "100 g", 35, 2.0, 7.0, 2.8, .4, 38), c("Plain yogurt", "80 g", 50, 4.2, 5.6, 0, 1.3, 56))),
            new Day(
                    m("Provision breakfast", "Boiled green fig, sweet potato, egg, and callaloo.", c("Boiled green banana", "100 g", 89, 1.0, 22.8, 2.6, .3, 1), c("Boiled sweet potato", "120 g", 108, 2.4, 24.8, 4.0, .2, 43), c("Boiled egg", "1 large", 78, 6.3, .6, 0, 5.3, 62), c("Cooked callaloo", "100 g", 21, 2.1, 3.8, 2.1, .2, 17)),
                    m("Turkey and rice bowl", "Turkey, rice, pigeon peas, vegetables, and avocado.", c("Roasted turkey breast", "130 g", 176, 39.0, 0, 0, 1.3, 73), c("Cooked brown rice", "140 g", 172, 3.8, 35.8, 2.2, 1.4, 6), c("Pigeon peas", "80 g", 97, 5.4, 18.3, 5.1, .3, 2), c("Vegetables and avocado", "120 g", 128, 2.4, 11.0, 5.0, 8.0, 32))),
            new Day(
                    m("Aloo choka and roti", "Roasted potato choka, sada roti, tomato, cucumber, and boiled egg.", c("Sada roti", "1 medium", 210, 6.5, 38.0, 2.2, 4.0, 295), c("Aloo choka", "180 g", 210, 4.2, 32.0, 5.5, 6.0, 240), c("Boiled egg", "1 large", 78, 6.3, .6, 0, 5.3, 62), c("Tomato and cucumber", "120 g", 22, 1.1, 4.6, 1.4, .2, 5)),
                    m("Curry channa rice bowl", "Curried channa, brown rice, pumpkin, vegetables, and avocado.", c("Curried channa", "170 g", 250, 11.9, 36.3, 9.6, 6.8, 260), c("Cooked brown rice", "150 g", 185, 4.1, 38.4, 2.4, 1.5, 6), c("Caribbean pumpkin", "100 g", 20, .7, 4.9, 1.1, .1, 1), c("Vegetables and avocado", "120 g", 128, 2.4, 11.0, 5.0, 8.0, 32)))
    };

    /** Independent Trinidad & Tobago breakfast choices, including prepared dishes from CaribbeanFoodLibrary. */
    private static final Meal[] TRINIDAD_TOBAGO_BREAKFASTS = {
            TRINIDAD_TOBAGO_DAYS[0].breakfast, TRINIDAD_TOBAGO_DAYS[1].breakfast,
            TRINIDAD_TOBAGO_DAYS[2].breakfast, TRINIDAD_TOBAGO_DAYS[3].breakfast,
            TRINIDAD_TOBAGO_DAYS[4].breakfast, TRINIDAD_TOBAGO_DAYS[5].breakfast,
            TRINIDAD_TOBAGO_DAYS[6].breakfast,
            m("Saltfish buljol and coconut bake", "Saltfish buljol, coconut bake, cucumber, and tomato.", c("Saltfish buljol", "100 g", 128, 16.8, 4.4, 1.5, 5.1, 760), c("Coconut bake", "70 g", 225, 4.3, 34.7, 1.6, 7.7, 252), c("Tomato and cucumber", "120 g", 22, 1.1, 4.6, 1.4, .2, 5)),
            m("Dhalpuri with channa", "Dhalpuri, curried channa, cucumber, and orange.", c("Dhalpuri", "80 g", 242, 7.4, 39.3, 4.4, 5.8, 336), c("Curried channa", "120 g", 176, 8.4, 25.6, 6.8, 4.8, 184), c("Cucumber", "80 g", 12, .5, 2.9, .4, .1, 2), c("Orange", "1 medium", 62, 1.2, 15.4, 3.1, .2, 0)),
            m("Tomato choka, sada roti and egg", "Roasted tomato choka, sada roti, boiled egg, and avocado.", c("Sada roti", "1 medium", 210, 6.5, 38.0, 2.2, 4.0, 295), c("Tomato choka", "150 g", 68, 2.4, 10.5, 3.5, 2.2, 110), c("Boiled egg", "1 large", 78, 6.3, .6, 0, 5.3, 62), c("Avocado", "40 g", 64, .8, 3.4, 2.7, 5.9, 3)),
            m("Aloo pie, yogurt and fruit", "Aloo pie with plain yogurt, cucumber, and mango.", c("Aloo pie", "100 g", 286, 4.9, 38.4, 3.1, 12.7, 390), c("Plain yogurt", "120 g", 76, 6.4, 8.4, 0, 1.9, 84), c("Cucumber", "80 g", 12, .5, 2.9, .4, .1, 2), c("Mango", "100 g", 60, .8, 15.0, 1.6, .4, 1)),
            m("Pholourie, channa and fruit", "Pholourie with curried channa, cucumber chutney, and pineapple.", c("Pholourie", "80 g", 221, 6.5, 28.3, 4.0, 9.0, 272), c("Curried channa", "100 g", 147, 7.0, 21.3, 5.7, 4.0, 153), c("Cucumber chutney", "40 g", 24, .4, 5.0, .5, .1, 80), c("Pineapple", "100 g", 50, .5, 13.1, 1.4, .1, 1)),
            m("Coconut bake, egg and avocado", "Coconut bake, boiled eggs, avocado, and tomato.", c("Coconut bake", "75 g", 241, 4.6, 37.1, 1.8, 8.3, 270), c("Boiled eggs", "2 large", 156, 12.6, 1.2, 0, 10.6, 124), c("Avocado", "50 g", 80, 1.0, 4.3, 3.4, 7.4, 4), c("Tomato", "80 g", 14, .7, 3.1, 1.0, .2, 4)),
            m("Provision and buljol plate", "Boiled green fig, sweet potato, saltfish buljol, and callaloo.", c("Boiled green banana", "120 g", 107, 1.3, 27.4, 3.1, .4, 1), c("Boiled sweet potato", "100 g", 90, 2.0, 20.7, 3.3, .2, 36), c("Saltfish buljol", "80 g", 102, 13.4, 3.5, 1.2, 4.1, 608), c("Cooked callaloo", "100 g", 21, 2.1, 3.8, 2.1, .2, 17))
    };

    /** Independent Trinidad & Tobago lunch choices, including prepared dishes from CaribbeanFoodLibrary. */
    private static final Meal[] TRINIDAD_TOBAGO_LUNCHES = {
            TRINIDAD_TOBAGO_DAYS[0].lunch, TRINIDAD_TOBAGO_DAYS[1].lunch,
            TRINIDAD_TOBAGO_DAYS[2].lunch, TRINIDAD_TOBAGO_DAYS[3].lunch,
            TRINIDAD_TOBAGO_DAYS[4].lunch, TRINIDAD_TOBAGO_DAYS[5].lunch,
            TRINIDAD_TOBAGO_DAYS[6].lunch,
            m("Bake and shark plate", "Bake and shark with salad, cucumber, and pineapple.", c("Bake and shark", "180 g", 482, 21.8, 53.6, 4.0, 20.2, 918), c("Fresh salad", "100 g", 25, 1.2, 5.0, 2.0, .2, 30), c("Pineapple", "100 g", 50, .5, 13.1, 1.4, .1, 1)),
            m("Tobago crab and dumpling", "Crab and dumpling with callaloo and cucumber.", c("Crab and dumpling", "260 g", 465, 28.9, 61.1, 3.6, 11.4, 1014), c("Cooked callaloo", "100 g", 21, 2.1, 3.8, 2.1, .2, 17), c("Cucumber", "80 g", 12, .5, 2.9, .4, .1, 2)),
            m("Curry crab rice bowl", "Curry crab, brown rice, pigeon peas, pumpkin, and vegetables.", c("Curry crab", "170 g", 231, 29.1, 7.1, 1.7, 8.8, 714), c("Cooked brown rice", "140 g", 172, 3.8, 35.8, 2.2, 1.4, 6), c("Pigeon peas", "70 g", 85, 4.8, 16.0, 4.5, .3, 2), c("Caribbean pumpkin", "100 g", 20, .7, 4.9, 1.1, .1, 1)),
            m("Geera pork rice plate", "Geera pork, brown rice, callaloo, and cucumber.", c("Geera pork", "120 g", 312, 26.5, 3.2, .8, 21.4, 564), c("Cooked brown rice", "150 g", 185, 4.1, 38.4, 2.4, 1.5, 6), c("Cooked callaloo", "100 g", 21, 2.1, 3.8, 2.1, .2, 17), c("Cucumber", "80 g", 12, .5, 2.9, .4, .1, 2)),
            m("Chicken chow mein and vegetables", "Caribbean-style chicken chow mein with extra vegetables and fruit.", c("Chicken chow mein", "280 g", 501, 28.6, 64.4, 5.0, 15.1, 1288), c("Steamed vegetables", "120 g", 42, 2.4, 8.5, 3.4, .5, 45), c("Orange", "1 medium", 62, 1.2, 15.4, 3.1, .2, 0)),
            m("Fish broth", "Caribbean fish soup with ground provisions and vegetables.", c("Fish broth", "350 g", 287, 30.1, 28.4, 4.2, 6.7, 893)),
            m("Callaloo stew, beans and rice", "Callaloo stew, pigeon peas, brown rice, pumpkin, and avocado.", c("Callaloo stew", "180 g", 166, 6.2, 13.5, 5.8, 10.6, 378), c("Pigeon peas", "100 g", 121, 6.8, 22.9, 6.4, .4, 2), c("Cooked brown rice", "140 g", 172, 3.8, 35.8, 2.2, 1.4, 6), c("Caribbean pumpkin and avocado", "130 g", 128, 2.2, 10.3, 4.5, 8.1, 14))
    };

    public static Day forToday() { Calendar calendar = Calendar.getInstance(); return DAYS[(calendar.get(Calendar.DAY_OF_WEEK) + 5) % DAYS.length]; }
    public static Day forToday(String region) {
        Calendar calendar = Calendar.getInstance();
        if (NutritionRegionStore.TRINIDAD_TOBAGO.equals(region)) {
            int day = calendar.get(Calendar.DAY_OF_YEAR);
            return new Day(TRINIDAD_TOBAGO_BREAKFASTS[day % TRINIDAD_TOBAGO_BREAKFASTS.length],
                    TRINIDAD_TOBAGO_LUNCHES[(day * 3 + 2) % TRINIDAD_TOBAGO_LUNCHES.length]);
        }
        return forDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK), region);
    }
    static Day forDayOfWeek(int calendarDayOfWeek) { return DAYS[(calendarDayOfWeek + 5) % DAYS.length]; }
    static Day forDayOfWeek(int calendarDayOfWeek, String region) {
        Day[] rotation = NutritionRegionStore.TRINIDAD_TOBAGO.equals(region) ? TRINIDAD_TOBAGO_DAYS : DAYS;
        return rotation[(calendarDayOfWeek + 5) % rotation.length];
    }
    private NutritionRecommendation() {}
}
