package com.frozenassets.app.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FoodCategory {
    // USDA recommended storage times in days
    public static final int DEFAULT_DURATION = 180; // 6 months default
    public static final int CHICKEN_DURATION = 270; // 9 months per USDA
    public static final int BEEF_DURATION = 365; // 12 months per USDA
    public static final int PORK_DURATION = 180; // 6 months per USDA
    public static final int FISH_DURATION = 180; // 6 months per USDA
    public static final int COOKED_MEALS_DURATION = 90; // 3 months general guideline
    public static final int VEGETABLES_DURATION = 240; // 8 months general guideline
    public static final int FRUITS_DURATION = 240; // 8 months general guideline
    public static final int OTHER_DURATION = 90; // 3 months default

    // Category names
    public static final String CHICKEN = "Chicken";
    public static final String BEEF = "Beef";
    public static final String PORK = "Pork";
    public static final String FISH = "Fish";
    public static final String COOKED_MEALS = "Cooked Meals";
    public static final String VEGETABLES = "Vegetables";
    public static final String FRUITS = "Fruits";
    public static final String OTHER = "Other";

    private static final Map<String, Integer> categoryDurations = new HashMap<>();

    static {
        categoryDurations.put(CHICKEN, CHICKEN_DURATION);
        categoryDurations.put(BEEF, BEEF_DURATION);
        categoryDurations.put(PORK, PORK_DURATION);
        categoryDurations.put(FISH, FISH_DURATION);
        categoryDurations.put(COOKED_MEALS, COOKED_MEALS_DURATION);
        categoryDurations.put(VEGETABLES, VEGETABLES_DURATION);
        categoryDurations.put(FRUITS, FRUITS_DURATION);
        categoryDurations.put(OTHER, OTHER_DURATION);
    }

    // Get default categories
    public static List<String> getDefaultCategories() {
        return Arrays.asList(
                CHICKEN,
                BEEF,
                PORK,
                FISH,
                COOKED_MEALS,
                VEGETABLES,
                FRUITS,
                OTHER
        );
    }

    // Get default duration for a category in days
    public static int getDefaultDurationForCategory(String category) {
        return categoryDurations.getOrDefault(category, DEFAULT_DURATION);
    }

    // Get expiration duration for a category in milliseconds
    public static long getDurationForCategory(String category) {
        int days = categoryDurations.getOrDefault(category, DEFAULT_DURATION);
        return TimeUnit.DAYS.toMillis(days);
    }

    // Update duration for a category
    public static void setDurationForCategory(String category, int days) {
        if (days > 0) {
            categoryDurations.put(category, days);
        }
    }

    // Common food tags
    public static final List<String> COMMON_TAGS = Arrays.asList(
            "Raw",
            "Cooked",
            "Leftover",
            "Meal Prep",
            "Breakfast",
            "Lunch",
            "Dinner",
            "Dessert",
            "Snack",
            "Organic",
            "Veggie",
            "Fruit",
            "Meat",
            "Seafood"
    );
}