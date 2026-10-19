package com.badimps.personaltrainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A practical Caribbean-food reference, expressed per 100 g edible portion.
 * Values are rounded composition references, never recipe prescriptions or product-label claims.
 */
public final class CaribbeanFoodLibrary {
    public static final String[] CATEGORIES = {
            "All", "Staples & grains", "Fruit", "Vegetables", "Beans & plant proteins",
            "Fish & seafood", "Meat, eggs & dairy", "Prepared Caribbean dishes",
            "Breads, snacks & treats", "Cooking basics"
    };

    public static final class Food {
        public final String name, category, preparation, note;
        public final int kcal, sodium;
        public final double protein, carbs, fibre, fat;

        Food(String name, String category, String preparation, int kcal, double protein,
             double carbs, double fibre, double fat, int sodium, String note) {
            this.name = name; this.category = category; this.preparation = preparation;
            this.kcal = kcal; this.protein = protein; this.carbs = carbs; this.fibre = fibre;
            this.fat = fat; this.sodium = sodium; this.note = note;
        }
    }

    private static Food f(String name, String category, String preparation, int kcal, double protein,
                          double carbs, double fibre, double fat, int sodium) {
        return new Food(name, category, preparation, kcal, protein, carbs, fibre, fat, sodium, "");
    }

    private static Food n(String name, String category, String preparation, int kcal, double protein,
                          double carbs, double fibre, double fat, int sodium, String note) {
        return new Food(name, category, preparation, kcal, protein, carbs, fibre, fat, sodium, note);
    }

    /** A composite-dish reference: rounded typical recipe values per 100 g, not a recipe guarantee. */
    private static Food d(String name, String preparation, int kcal, double protein,
                          double carbs, double fibre, double fat, int sodium) {
        return n(name, "Prepared Caribbean dishes", preparation, kcal, protein, carbs, fibre, fat, sodium,
                "Typical prepared-recipe reference per 100 g. Ingredients, oil, portion size, and salt vary by cook and island.");
    }

    /** Bakery and street-food references are also recipe-dependent and should not replace a package label. */
    private static Food s(String name, String preparation, int kcal, double protein,
                          double carbs, double fibre, double fat, int sodium) {
        return n(name, "Breads, snacks & treats", preparation, kcal, protein, carbs, fibre, fat, sodium,
                "Typical prepared-recipe reference per 100 g. Frying method, fillings, sugar, and salt can change the value substantially.");
    }

    private static final Food[] FOODS = {
            f("Green banana", "Staples & grains", "boiled or steamed reference", 89, 1.1, 22.8, 2.6, .3, 1),
            f("Ripe plantain", "Staples & grains", "plain, raw reference", 122, 1.3, 31.9, 2.3, .4, 4),
            f("Cassava", "Staples & grains", "plain, boiled reference", 112, 1.0, 27.0, 1.8, .3, 14),
            f("Dasheen (taro)", "Staples & grains", "plain, cooked reference", 142, .5, 34.6, 5.1, .1, 1),
            f("Eddoe", "Staples & grains", "plain, cooked reference", 112, 1.5, 26.5, 4.1, .2, 2),
            f("Yellow yam", "Staples & grains", "plain, cooked reference", 118, 1.5, 27.9, 4.1, .2, 9),
            f("Sweet potato", "Staples & grains", "plain, baked reference", 90, 2.0, 20.7, 3.3, .2, 36),
            f("Breadfruit", "Staples & grains", "plain, cooked reference", 103, 1.1, 27.1, 4.9, .2, 2),
            f("White rice", "Staples & grains", "cooked without added fat", 130, 2.4, 28.2, .4, .3, 1),
            f("Brown rice", "Staples & grains", "cooked without added fat", 123, 2.7, 25.6, 1.6, 1.0, 4),
            f("Cornmeal porridge", "Staples & grains", "cooked in water, unsweetened", 71, 1.5, 15.1, 1.0, .5, 2),
            f("Oats", "Staples & grains", "cooked in water", 71, 2.5, 12.0, 1.7, 1.5, 2),
            f("Whole-wheat roti", "Staples & grains", "plain flatbread reference", 297, 10.0, 50.0, 7.0, 7.0, 430),
            f("Bammy (cassava flatbread)", "Staples & grains", "plain reference", 180, 1.4, 42.0, 2.0, .5, 18),
            f("Boiled corn", "Staples & grains", "without added butter or salt", 96, 3.4, 21.0, 2.4, 1.5, 1),

            f("Ackee", "Fruit", "canned, drained reference", 151, 2.9, .8, 2.7, 15.2, 240),
            f("Soursop", "Fruit", "raw pulp", 66, 1.0, 16.8, 3.3, .3, 14),
            f("Guava", "Fruit", "raw", 68, 2.6, 14.3, 5.4, 1.0, 2),
            f("Mango", "Fruit", "raw", 60, .8, 15.0, 1.6, .4, 1),
            f("Papaya", "Fruit", "raw", 43, .5, 10.8, 1.7, .3, 8),
            f("Pineapple", "Fruit", "raw", 50, .5, 13.1, 1.4, .1, 1),
            f("Passion fruit", "Fruit", "raw pulp", 97, 2.2, 23.4, 10.4, .7, 28),
            f("Star fruit (carambola)", "Fruit", "raw", 31, 1.0, 6.7, 2.8, .3, 2),
            f("Tamarind", "Fruit", "raw pulp", 239, 2.8, 62.5, 5.1, .6, 28),
            f("Coconut meat", "Fruit", "raw", 354, 3.3, 15.2, 9.0, 33.5, 20),
            f("Coconut water", "Fruit", "plain, unsweetened", 19, .7, 3.7, 1.1, .2, 105),
            f("Avocado (pear)", "Fruit", "raw", 160, 2.0, 8.5, 6.7, 14.7, 7),
            f("Banana", "Fruit", "raw", 89, 1.1, 22.8, 2.6, .3, 1),
            f("Sugar apple (sweetsop)", "Fruit", "raw", 94, 2.1, 24.0, 4.4, .3, 4),
            f("Sapodilla (naseberry)", "Fruit", "raw", 83, .4, 19.9, 5.3, 1.1, 12),
            f("Grapefruit", "Fruit", "raw", 42, .8, 10.7, 1.6, .1, 0),
            f("Orange", "Fruit", "raw", 47, .9, 11.8, 2.4, .1, 0),
            f("Lime", "Fruit", "raw", 30, .7, 10.5, 2.8, .2, 2),
            f("Watermelon", "Fruit", "raw", 30, .6, 7.6, .4, .2, 1),

            f("Callaloo (amaranth greens)", "Vegetables", "cooked without added fat", 21, 2.1, 3.8, 2.1, .2, 17),
            f("Okra", "Vegetables", "cooked without added fat", 22, 1.9, 4.5, 2.5, .2, 5),
            f("Christophene (chayote)", "Vegetables", "cooked without added fat", 24, .6, 5.1, 1.7, .2, 2),
            f("Caribbean pumpkin", "Vegetables", "cooked without added fat", 20, .7, 4.9, 1.1, .1, 1),
            f("Cabbage", "Vegetables", "cooked without added fat", 23, 1.3, 5.5, 2.3, .1, 18),
            f("Carrot", "Vegetables", "cooked without added fat", 35, .8, 8.2, 3.0, .2, 58),
            f("String beans", "Vegetables", "cooked without added fat", 35, 1.9, 7.9, 3.2, .3, 1),
            f("Sweet pepper", "Vegetables", "raw", 31, 1.0, 6.0, 2.1, .3, 4),
            f("Eggplant", "Vegetables", "cooked without added fat", 35, .8, 8.7, 2.5, .2, 1),
            f("Tomato", "Vegetables", "raw", 18, .9, 3.9, 1.2, .2, 5),
            f("Cucumber", "Vegetables", "raw", 15, .7, 3.6, .5, .1, 2),
            f("Bok choy", "Vegetables", "cooked without added fat", 13, 1.6, 2.2, 1.0, .2, 65),
            f("Spinach", "Vegetables", "cooked without added fat", 23, 3.0, 3.8, 2.4, .3, 70),
            f("Breadnut", "Vegetables", "boiled reference", 124, 5.0, 26.0, 4.0, .5, 5),

            f("Pigeon peas (gungo peas)", "Beans & plant proteins", "cooked without added salt", 121, 6.8, 22.9, 6.4, .4, 2),
            f("Red kidney beans", "Beans & plant proteins", "cooked without added salt", 127, 8.7, 22.8, 6.4, .5, 1),
            f("Black-eyed peas", "Beans & plant proteins", "cooked without added salt", 116, 7.7, 20.8, 6.5, .5, 3),
            f("Chickpeas", "Beans & plant proteins", "cooked without added salt", 164, 8.9, 27.4, 7.6, 2.6, 7),
            f("Lentils", "Beans & plant proteins", "cooked without added salt", 116, 9.0, 20.1, 7.9, .4, 2),
            f("Black beans", "Beans & plant proteins", "cooked without added salt", 132, 8.9, 23.7, 8.7, .5, 1),
            f("Split peas", "Beans & plant proteins", "cooked without added salt", 118, 8.3, 21.1, 8.3, .4, 2),
            f("Tofu", "Beans & plant proteins", "firm, plain", 144, 17.3, 2.8, 2.3, 8.7, 14),
            f("Peanut butter", "Beans & plant proteins", "smooth, unsalted reference", 588, 25.1, 20.0, 6.0, 50.4, 17),

            // Prepared dishes — broad regional coverage. Each is intentionally described as a recipe estimate.
            d("Pelau", "Trinidad-style chicken, rice, pigeon peas and coconut milk", 183, 10.2, 21.6, 2.1, 6.1, 220),
            d("Cook-up rice", "Guyana-style rice, beans or peas, coconut milk and protein", 176, 8.2, 25.1, 3.0, 4.6, 205),
            d("Rice and peas", "Jamaican-style rice, kidney beans and coconut milk", 158, 4.1, 27.2, 2.9, 3.7, 155),
            d("Arroz con gandules", "Puerto Rican-style rice with pigeon peas", 172, 4.6, 29.4, 3.1, 3.8, 240),
            d("Moros y cristianos", "Cuban-style black beans and rice", 155, 5.6, 27.4, 3.3, 2.3, 175),
            d("Diri kole ak pwa", "Haitian-style rice and beans", 164, 5.4, 28.1, 3.4, 2.7, 180),
            d("Roti with curry chicken", "whole-wheat roti with curried chicken filling", 244, 11.7, 27.1, 2.4, 9.5, 380),
            d("Roti with channa", "whole-wheat roti with curried chickpeas", 226, 8.1, 35.1, 6.1, 5.6, 330),
            d("Doubles", "bara with curried channa and chutney", 246, 7.2, 35.2, 5.5, 8.7, 360),
            d("Aloo pie", "fried dough filled with seasoned potato", 286, 4.9, 38.4, 3.1, 12.7, 390),
            d("Pholourie", "fried split-pea fritters", 276, 8.1, 35.4, 5.0, 11.2, 340),
            d("Sahina", "fried spinach or dasheen-leaf fritter", 214, 5.1, 22.2, 3.3, 12.0, 370),
            d("Bake and shark", "fried bake with seasoned fish and sauces", 268, 12.1, 29.8, 2.2, 11.2, 510),
            d("Callaloo stew", "Trinidad-style greens with coconut milk", 92, 3.4, 7.5, 3.2, 5.9, 210),
            d("Macaroni pie", "Caribbean baked macaroni and cheese", 221, 9.5, 24.2, 1.1, 10.1, 430),
            d("Stew chicken", "browned and stewed chicken", 196, 19.5, 8.2, .8, 9.2, 380),
            d("Curry chicken", "Caribbean curry chicken with sauce", 184, 19.2, 5.5, 1.0, 9.0, 310),
            d("Curry goat", "Caribbean curried goat stew", 201, 20.7, 5.0, .9, 10.7, 355),
            d("Brown stew chicken", "Jamaican-style sweet-savory chicken stew", 203, 18.4, 10.6, 1.0, 9.4, 405),
            d("Jerk chicken", "grilled chicken with jerk seasoning", 191, 24.2, 3.5, .7, 9.1, 430),
            d("Geera pork", "cumin-seasoned pork", 260, 22.1, 2.7, .7, 17.8, 470),
            d("Ackee and saltfish", "ackee with salted cod and vegetables", 204, 12.7, 3.6, 1.8, 16.1, 690),
            d("Saltfish buljol", "Trinidad-style salted-cod salad", 128, 16.8, 4.4, 1.5, 5.1, 760),
            d("Flying fish and cou-cou", "Barbadian cornmeal and flying-fish plate", 143, 11.6, 16.5, 1.9, 3.6, 290),
            d("Fish broth", "Caribbean fish soup with ground provisions", 82, 8.6, 8.1, 1.2, 1.9, 255),
            d("Provision soup", "root-vegetable soup with meat or fish", 91, 6.2, 12.4, 1.8, 2.1, 290),
            d("Crab and dumpling", "Tobago-style crab with flour dumplings", 179, 11.1, 23.5, 1.4, 4.4, 390),
            d("Curry crab", "curried crab in sauce", 136, 17.1, 4.2, 1.0, 5.2, 420),
            d("Conch salad", "Bahamas-style raw conch, citrus and vegetables", 87, 14.2, 5.8, 1.3, 1.0, 310),
            d("Cracked conch", "breaded and fried conch", 221, 15.6, 19.1, 1.1, 9.4, 480),
            d("Oil down", "Grenadian breadfruit, greens, coconut milk and salted meat", 176, 8.9, 20.4, 3.0, 7.4, 460),
            d("Green fig and saltfish", "St Lucian green banana with salted cod", 141, 11.8, 17.9, 2.8, 2.9, 650),
            d("Metemgee", "Guyana-style coconut-milk root-vegetable stew", 143, 5.6, 21.1, 3.1, 4.8, 265),
            d("Pepperpot", "Guyana-style cassareep beef stew", 146, 16.7, 6.4, 1.0, 6.1, 520),
            d("Chicken chow mein", "Caribbean-Chinese noodle stir-fry", 179, 10.2, 23.0, 1.8, 5.4, 460),
            d("Ital stew", "Jamaican plant-based stew with beans and vegetables", 116, 4.7, 17.6, 4.2, 3.2, 195),
            d("Mofongo", "Puerto Rican mashed plantain with pork or seafood", 245, 7.3, 35.2, 3.1, 9.2, 440),
            d("Mangu", "Dominican mashed green plantain", 132, 1.5, 28.8, 2.5, 1.8, 150),
            d("La bandera", "Dominican rice, beans and stewed meat plate", 174, 10.1, 23.5, 3.0, 4.7, 295),
            d("Sancocho", "Dominican multi-meat and root-vegetable soup", 105, 9.2, 10.1, 1.7, 3.9, 330),
            d("Soup joumou", "Haitian pumpkin and beef soup", 96, 7.2, 11.8, 1.5, 2.3, 270),
            d("Griot", "Haitian citrus-marinated fried pork", 307, 22.4, 3.3, .5, 22.5, 500),
            d("Tassot", "Haitian fried seasoned beef or goat", 279, 25.1, 4.0, .6, 18.1, 465),
            d("Ropa vieja", "Cuban shredded beef in tomato-pepper sauce", 165, 19.4, 5.2, 1.2, 7.8, 360),
            d("Picadillo", "Cuban minced beef with tomato and olives", 194, 15.7, 8.5, 1.4, 11.2, 510),
            d("Hudut", "Belizean coconut fish stew with mashed plantain", 151, 10.5, 18.6, 2.4, 5.1, 245),
            d("Rice and stew chicken", "Belizean-style rice with stewed chicken", 183, 10.8, 23.7, 1.6, 5.1, 330),
            d("Fungee and pepperpot", "Antiguan cornmeal fungus with vegetable stew", 139, 6.0, 22.8, 2.9, 3.2, 280),

            // Common bread, snack, dessert, and market-food references.
            s("Sada roti", "plain Trinidad-style griddled flatbread", 287, 8.8, 51.7, 3.0, 5.2, 405),
            s("Dhalpuri", "split-pea filled flatbread", 303, 9.3, 49.1, 5.5, 7.2, 420),
            s("Paratha (buss up shut)", "layered oil-brushed flatbread", 334, 8.2, 48.5, 2.8, 12.1, 430),
            s("Fry bake", "fried flour dough", 333, 6.8, 48.2, 1.9, 12.7, 395),
            s("Coconut bake", "baked coconut bread", 321, 6.1, 49.5, 2.3, 11.0, 360),
            s("Jamaican beef patty", "seasoned beef-filled pastry", 316, 10.1, 29.2, 1.4, 17.4, 610),
            s("Jamaican chicken patty", "seasoned chicken-filled pastry", 294, 11.2, 30.1, 1.2, 15.0, 585),
            s("Saltfish fritter", "salted-cod fried fritter", 242, 12.4, 22.0, 1.5, 11.4, 650),
            s("Fish cake", "Caribbean fried fish cake", 233, 11.6, 20.8, 1.1, 11.4, 560),
            s("Festival", "Jamaican fried cornmeal dumpling", 304, 5.2, 49.7, 2.0, 10.0, 315),
            s("Boiled dumpling", "plain flour dumpling", 170, 4.9, 35.6, 1.5, .6, 250),
            s("Cassava pone", "baked cassava and coconut pudding", 245, 2.2, 43.7, 2.9, 7.2, 120),
            s("Sweet potato pudding", "Jamaican-style sweet potato and coconut pudding", 226, 2.0, 40.8, 3.1, 6.1, 115),
            s("Black cake", "Caribbean fruit cake", 381, 4.2, 48.5, 2.6, 18.3, 205),
            s("Rum cake", "Caribbean butter-rum cake", 372, 4.6, 49.3, 1.0, 16.5, 290),
            s("Coconut drops", "coconut and brown-sugar sweet", 389, 2.8, 59.2, 5.1, 16.1, 170),
            s("Sugar cake", "coconut sugar confection", 420, 2.6, 67.1, 3.7, 16.4, 105),
            s("Tamarind ball", "tamarind and sugar confection", 336, 1.9, 82.0, 4.3, 1.0, 75),
            s("Toolum", "molasses and coconut sweet", 365, 2.3, 68.4, 4.9, 10.4, 135),
            s("Kurma", "spiced fried flour snack", 424, 6.3, 58.1, 1.9, 19.4, 290),

            f("Red snapper", "Fish & seafood", "cooked, dry heat", 128, 26.3, 0, 0, 1.6, 60),
            f("Flying fish", "Fish & seafood", "cooked reference", 111, 20.0, 0, 0, 3.0, 65),
            f("Kingfish", "Fish & seafood", "cooked reference", 134, 23.0, 0, 0, 4.3, 75),
            f("Tuna", "Fish & seafood", "cooked, dry heat", 132, 28.0, 0, 0, 1.3, 47),
            f("Sardines", "Fish & seafood", "canned in water, drained", 185, 24.6, 0, 0, 10.5, 307),
            f("Mackerel", "Fish & seafood", "cooked, dry heat", 262, 23.9, 0, 0, 17.8, 85),
            f("Shrimp", "Fish & seafood", "cooked without added salt", 99, 24.0, .2, 0, .3, 111),
            f("Conch", "Fish & seafood", "cooked reference", 130, 26.0, 3.0, 0, 1.2, 160),
            f("Lobster", "Fish & seafood", "cooked without added salt", 89, 19.0, 0, 0, .9, 486),
            n("Saltfish (salted cod)", "Fish & seafood", "salted, prepared reference", 290, 62.8, 0, 0, 2.4, 7027, "Sodium can be very high; soaking and preparation change it substantially."),

            f("Chicken breast", "Meat, eggs & dairy", "roasted, skinless", 165, 31.0, 0, 0, 3.6, 74),
            f("Chicken thigh", "Meat, eggs & dairy", "roasted, skinless", 179, 24.8, 0, 0, 8.2, 89),
            f("Turkey breast", "Meat, eggs & dairy", "roasted, skinless", 135, 30.0, 0, 0, 1.0, 56),
            f("Goat", "Meat, eggs & dairy", "cooked, lean reference", 143, 27.1, 0, 0, 3.0, 86),
            f("Beef", "Meat, eggs & dairy", "cooked, lean reference", 217, 26.1, 0, 0, 11.8, 58),
            f("Pork", "Meat, eggs & dairy", "cooked, lean reference", 196, 27.3, 0, 0, 8.9, 62),
            f("Egg", "Meat, eggs & dairy", "boiled", 155, 12.6, 1.1, 0, 10.6, 124),
            f("Cow milk", "Meat, eggs & dairy", "whole, plain", 61, 3.2, 4.8, 0, 3.3, 43),
            f("Plain yogurt", "Meat, eggs & dairy", "low-fat, plain", 63, 5.3, 7.0, 0, 1.6, 70),
            f("Cheddar cheese", "Meat, eggs & dairy", "plain", 403, 24.9, 1.3, 0, 33.1, 621),

            f("Coconut milk", "Cooking basics", "canned or fresh, unsweetened reference", 230, 2.3, 5.5, 2.2, 23.8, 15),
            f("Coconut cream", "Cooking basics", "unsweetened reference", 330, 3.6, 6.7, 2.0, 34.7, 18),
            f("Olive oil", "Cooking basics", "plain", 884, 0, 0, 0, 100.0, 2),
            f("Canola oil", "Cooking basics", "plain", 884, 0, 0, 0, 100.0, 0),
            f("Peanut oil", "Cooking basics", "plain", 884, 0, 0, 0, 100.0, 0),
            f("Honey", "Cooking basics", "plain", 304, .3, 82.4, .2, 0, 4),
            f("Brown sugar", "Cooking basics", "plain", 380, 0, 98.1, 0, 0, 28),
            f("Hot pepper sauce", "Cooking basics", "commercial reference", 44, 1.5, 9.3, 1.5, .9, 2200),
            f("Cocoa powder", "Cooking basics", "unsweetened", 228, 19.6, 57.9, 33.2, 13.7, 21),
            f("Ginger", "Cooking basics", "fresh", 80, 1.8, 17.8, 2.0, .8, 13),
            f("Garlic", "Cooking basics", "fresh", 149, 6.4, 33.1, 2.1, .5, 17)
    };

    public static int count() { return FOODS.length; }

    public static List<Food> search(String query, String category) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String group = category == null ? "All" : category;
        List<Food> result = new ArrayList<>();
        for (Food food : FOODS) {
            boolean groupMatches = "All".equals(group) || group.equals(food.category);
            boolean queryMatches = needle.isEmpty() || food.name.toLowerCase(Locale.ROOT).contains(needle)
                    || food.category.toLowerCase(Locale.ROOT).contains(needle)
                    || food.preparation.toLowerCase(Locale.ROOT).contains(needle);
            if (groupMatches && queryMatches) result.add(food);
        }
        return result;
    }

    private CaribbeanFoodLibrary() { }
}
