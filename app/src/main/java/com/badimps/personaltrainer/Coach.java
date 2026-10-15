package com.badimps.personaltrainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Offline training templates. These are general fitness suggestions, not medical advice. */
public final class Coach {
    public static final String[] TARGETS = {"Upper", "Lower", "Arms", "Chest", "Legs", "Full Body"};
    public static final String[] GOALS = {"Build strength", "Build muscle", "Move & feel good"};

    public static final class Exercise {
        public final String name, target, cue, description;
        public final int sets, reps, restSeconds;
        Exercise(String name, String target, int sets, int reps, int rest, String cue, String description) {
            this.name = name; this.target = target; this.sets = sets; this.reps = reps; this.restSeconds = rest;
            this.cue = cue; this.description = description;
        }
    }

    private static final Map<String, List<Exercise>> LIBRARY = new LinkedHashMap<>();
    static {
        add("Upper", ex("Dumbbell Press", "Upper", "Press tall; ribs stay down.", "Shoulders and triceps."), ex("One-arm Row", "Upper", "Pull elbow toward your hip.", "Upper-back strength."), ex("Incline Push-up", "Upper", "Move as one strong plank.", "Scalable pressing."), ex("Band Pull-apart", "Upper", "Open your chest; squeeze shoulder blades.", "Posture support."));
        add("Lower", ex("Goblet Squat", "Lower", "Sit between your heels; stand tall.", "Quads and glutes."), ex("Romanian Deadlift", "Lower", "Hips travel back; keep the load close.", "Hamstrings and glutes."), ex("Reverse Lunge", "Lower", "Step softly back; drive through front foot.", "Single-leg control."), ex("Glute Bridge", "Lower", "Ribs down; squeeze at the top.", "Hip extension."));
        add("Arms", ex("Biceps Curl", "Arms", "Keep elbows beside your ribs.", "Biceps isolation."), ex("Overhead Triceps Extension", "Arms", "Keep elbows aimed forward.", "Triceps strength."), ex("Hammer Curl", "Arms", "Keep palms facing in.", "Forearms and biceps."), ex("Bench Dip", "Arms", "Shoulders stay away from ears.", "Triceps bodyweight work."));
        add("Chest", ex("Push-up", "Chest", "Brace your core; press the floor away.", "Chest, shoulders and triceps."), ex("Floor Press", "Chest", "Pause lightly, then press smoothly.", "Chest pressing with a safe range."), ex("Chest Fly", "Chest", "Hug a big tree; use a light load.", "Chest isolation."), ex("Incline Push-up", "Chest", "Body moves as one unit.", "A scalable push-up."));
        add("Legs", ex("Bodyweight Squat", "Legs", "Knees follow your toes.", "Leg strength and mobility."), ex("Walking Lunge", "Legs", "Stay tall as you step.", "Quads, glutes and balance."), ex("Calf Raise", "Legs", "Pause high, lower slowly.", "Calf strength."), ex("Wall Sit", "Legs", "Press your back into the wall.", "Leg endurance."));
        add("Full Body", ex("Squat to Press", "Full Body", "Stand strongly, then press overhead.", "Legs and shoulders together."), ex("Burpee", "Full Body", "Choose a step-back option when needed.", "Full-body conditioning."), ex("Renegade Row", "Full Body", "Resist twisting through your hips.", "Core and upper back."), ex("Mountain Climber", "Full Body", "Keep shoulders stacked over hands.", "Full-body conditioning."));
    }
    private static Exercise ex(String name, String target, String cue, String description) { return new Exercise(name, target, 3, 10, 60, cue, description); }
    @SafeVarargs private static void add(String group, Exercise... items) { LIBRARY.put(group, Arrays.asList(items)); }

    public static List<Exercise> recommendation(String target, String goal) {
        List<Exercise> base = new ArrayList<>(LIBRARY.get(target));
        if (base == null) base = new ArrayList<>(LIBRARY.get("Full Body"));
        int sets = goal.equals(GOALS[0]) ? 4 : goal.equals(GOALS[1]) ? 3 : 2;
        int reps = goal.equals(GOALS[0]) ? 6 : goal.equals(GOALS[1]) ? 10 : 12;
        int rest = goal.equals(GOALS[0]) ? 105 : goal.equals(GOALS[1]) ? 75 : 45;
        List<Exercise> plan = new ArrayList<>();
        for (Exercise e : base) plan.add(new Exercise(e.name, e.target, sets, reps, rest, e.cue, e.description));
        return plan;
    }
    public static Map<String, List<Exercise>> library() { return LIBRARY; }
    public static List<String> meals(String goal) {
        if (goal.equals(GOALS[0])) return Arrays.asList("Eggs, oats, berries and yogurt", "Chicken, quinoa and roasted vegetables", "Salmon, potatoes and a green salad");
        if (goal.equals(GOALS[1])) return Arrays.asList("Greek yogurt, fruit and granola", "Turkey or tofu rice bowl with vegetables", "Beans, avocado and whole-grain toast");
        return Arrays.asList("Overnight oats with fruit and seeds", "Colorful grain bowl with protein", "Vegetable soup, whole-grain bread and yogurt");
    }
    private Coach() {}
}
