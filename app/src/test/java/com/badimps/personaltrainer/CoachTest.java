package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CoachTest {
    @Test public void strengthPlanUsesHeavierScheme() {
        Coach.Exercise exercise = Coach.recommendation("Upper", "Build strength").get(0);
        assertEquals(4, exercise.sets);
        assertEquals(6, exercise.reps);
        assertEquals(105, exercise.restSeconds);
    }
    @Test public void wellbeingPlanUsesAccessibleScheme() {
        Coach.Exercise exercise = Coach.recommendation("Legs", "Move & feel good").get(0);
        assertEquals(2, exercise.sets);
        assertEquals(12, exercise.reps);
        assertEquals(45, exercise.restSeconds);
    }
    @Test public void everySupportedFocusBuildsAPlan() {
        for (String focus : Coach.TARGETS) assertFalse(Coach.recommendation(focus, "Build muscle").isEmpty());
    }
    @Test public void everyGoalHasMealIdeas() {
        for (String goal : Coach.GOALS) assertFalse(Coach.meals(goal).isEmpty());
    }
    @Test public void homePlanSubstitutesGymOnlyMove() {
        Coach.Exercise exercise = Coach.recommendation("Upper", "Build muscle", "Home — no equipment").get(0);
        assertEquals("Pike Push-up", exercise.name);
    }
    @Test public void eachGoalHasAnOutcomeSummary() {
        for (String goal : Coach.GOALS) assertFalse(Coach.goalSummary(goal).isEmpty());
    }
    @Test public void expandedFocusesHaveRecommendationsForEveryEquipmentChoice() {
        String[] focuses = {"Back", "Shoulders", "Core", "Endurance"};
        for (String focus : focuses) {
            for (String equipment : Coach.EQUIPMENT_TYPES) {
                assertFalse(focus + " should work with " + equipment, Coach.planOptions(focus, equipment).isEmpty());
            }
        }
    }
    @Test public void coreIsTheOnlyTrunkCategoryInThePlanner() {
        boolean hasAbsCategory = false;
        for (String focus : Coach.TARGETS) if ("Abs".equals(focus)) hasAbsCategory = true;
        assertFalse(hasAbsCategory);
        assertTrue(Coach.planOptions("Core", "Bodyweight").size() >= 3);
    }

    @Test public void standardizedMediaNamesStayInTheirCorrectBodyAreas() {
        assertTrue(hasExercise("Back", "Machine-assisted", "Machine Row"));
        assertTrue(hasExercise("Back", "Dumbbells", "Dumbbell Reverse Fly"));
        assertTrue(hasExercise("Shoulders", "Machine-assisted", "Machine Reverse Fly"));
        assertTrue(hasExercise("Shoulders", "Machine-assisted", "Face Pull"));
        assertTrue(hasExercise("Shoulders", "Dumbbells", "Dumbbell Front Raise"));
        assertTrue(hasExercise("Chest", "Dumbbells", "Dumbbell Fly"));
        assertTrue(hasExercise("Chest", "Machine-assisted", "Seated Dip Machine"));
        assertTrue(hasExercise("Biceps", "Dumbbells", "Dumbbell Curl"));
        assertTrue(hasExercise("Legs", "Machine-assisted", "Machine Calf Raise"));
        assertTrue(hasExercise("Full Body", "Dumbbells", "Dumbbell Thruster"));
    }

    private static boolean hasExercise(String focus, String equipment, String name) {
        for (Coach.Exercise exercise : Coach.planOptions(focus, equipment)) {
            if (name.equals(exercise.name) && focus.equals(exercise.target)) return true;
        }
        return false;
    }
}
