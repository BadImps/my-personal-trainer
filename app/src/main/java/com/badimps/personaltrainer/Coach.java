package com.badimps.personaltrainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Offline training templates. These are general fitness suggestions, not medical advice. */
public final class Coach {
    /** Training areas are deliberately separate from the equipment choice. */
    public static final String[] TARGETS = {"Upper", "Lower", "Arms", "Chest", "Legs", "Back & Core", "Cardio", "Mobility", "Full Body"};
    public static final String[] GOALS = {"Build strength", "Build muscle", "Reduce body fat", "Improve endurance", "Move & feel good"};
    public static final String[] ENVIRONMENTS = {"Gym", "Home — no equipment", "Home — dumbbells", "Outdoors"};
    public static final String[] PLAN_MODES = {"Weekly", "Daily", "Custom"};
    public static final String[] EQUIPMENT_TYPES = {"Dumbbells", "At home", "Machine-assisted", "Bodyweight"};

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
        add("Back & Core", ex("Bird Dog", "Back & Core", "Reach long without twisting.", "Core stability and back control."), ex("Dead Bug", "Back & Core", "Keep your low back gently grounded.", "Deep core control."), ex("Superman Hold", "Back & Core", "Lift lightly; keep your neck long.", "Back-body endurance."), ex("Side Plank", "Back & Core", "Press the floor away and stay tall.", "Oblique and shoulder stability."));
        add("Cardio", ex("Brisk March", "Cardio", "Land softly and swing your arms.", "Low-impact heart-rate work."), ex("Mountain Climber", "Cardio", "Keep shoulders stacked over hands.", "Core and conditioning."), ex("Step-up", "Cardio", "Use a stable step and drive through the whole foot.", "Leg and cardio conditioning."), ex("Jumping Jack", "Cardio", "Choose a step-out version if needed.", "Simple full-body cardio."));
        add("Mobility", ex("Cat Cow", "Mobility", "Move slowly with your breath.", "Gentle spinal mobility."), ex("World's Greatest Stretch", "Mobility", "Keep the range comfortable.", "Hips, hamstrings and upper back."), ex("90/90 Hip Switch", "Mobility", "Use your hands for support if needed.", "Hip rotation control."), ex("Shoulder Wall Slide", "Mobility", "Keep ribs soft and arms smooth.", "Shoulder mobility."));
        add("Full Body", ex("Squat to Press", "Full Body", "Stand strongly, then press overhead.", "Legs and shoulders together."), ex("Burpee", "Full Body", "Choose a step-back option when needed.", "Full-body conditioning."), ex("Renegade Row", "Full Body", "Resist twisting through your hips.", "Core and upper back."), ex("Mountain Climber", "Full Body", "Keep shoulders stacked over hands.", "Full-body conditioning."));
        add("Machine-assisted", ex("Machine Chest Press", "Chest", "Set the seat so handles meet mid-chest; stop short of locking out.", "Guided pressing with a machine."), ex("Lat Pulldown", "Upper", "Keep ribs down and pull toward your upper chest.", "Machine-assisted upper-back work."), ex("Leg Press", "Legs", "Keep your feet planted and do not lock your knees.", "Guided lower-body pressing."), ex("Seated Cable Row", "Upper", "Stay tall and pull the handle toward your ribs.", "Machine-assisted rowing."));
    }
    private static Exercise ex(String name, String target, String cue, String description) { return new Exercise(name, target, 3, 10, 60, cue, description); }
    @SafeVarargs private static void add(String group, Exercise... items) { LIBRARY.put(group, Arrays.asList(items)); }

    public static List<Exercise> recommendation(String target, String goal) { return recommendation(target, goal, ENVIRONMENTS[0]); }
    public static List<Exercise> recommendation(String target, String goal, String environment) {
        List<Exercise> base = new ArrayList<>(LIBRARY.get(target));
        if (base == null) base = new ArrayList<>(LIBRARY.get("Full Body"));
        int sets = goal.equals(GOALS[0]) ? 4 : goal.equals(GOALS[1]) ? 3 : goal.equals(GOALS[2]) ? 3 : goal.equals(GOALS[3]) ? 3 : 2;
        int reps = goal.equals(GOALS[0]) ? 6 : goal.equals(GOALS[1]) ? 10 : goal.equals(GOALS[2]) ? 12 : goal.equals(GOALS[3]) ? 15 : 12;
        int rest = goal.equals(GOALS[0]) ? 105 : goal.equals(GOALS[1]) ? 75 : goal.equals(GOALS[2]) ? 45 : goal.equals(GOALS[3]) ? 30 : 45;
        List<Exercise> plan = new ArrayList<>();
        for (Exercise e : base) plan.add(adapt(e, sets, reps, rest, environment));
        return plan;
    }
    private static Exercise adapt(Exercise e,int sets,int reps,int rest,String environment) {
        String name=e.name, cue=e.cue, description=e.description;
        if (environment.equals("Home — no equipment") || environment.equals("Outdoors")) {
            if(name.equals("Dumbbell Press")){name="Pike Push-up";cue="Press the floor away; keep your neck long.";description="No-equipment shoulder pressing.";}
            else if(name.equals("One-arm Row")){name="Prone Snow Angel";cue="Lift only as far as feels comfortable; move slowly.";description="A no-equipment upper-back option.";}
            else if(name.equals("Romanian Deadlift")){name="Single-leg Hip Hinge";cue="Reach the back leg long; keep hips square.";description="No-equipment hamstring control.";}
            else if(name.equals("Floor Press")||name.equals("Chest Fly")){name="Push-up";cue="Brace your core; press the floor away.";description="No-equipment chest training.";}
            else if(name.equals("Biceps Curl")||name.equals("Hammer Curl")){name="Isometric Towel Curl";cue="Pull steadily against the towel.";description="A simple no-equipment arm option.";}
        }
        return new Exercise(name,e.target,sets,reps,rest,cue,description);
    }

    /** Returns equipment-specific movements for the plan builder. */
    public static List<Exercise> planOptions(String target, String equipment) {
        if ("Dumbbells".equals(equipment)) return dumbbellOptions(target);
        if ("At home".equals(equipment) || "Home & household".equals(equipment)) return householdOptions(target);
        if ("Machine-assisted".equals(equipment) || "Machine assisted".equals(equipment)) return machineOptions(target);
        return bodyweightOptions(target);
    }

    /**
     * Combines one or two selected focus areas without duplicating a movement.  The wizard uses
     * this for Daily plans and for the active day of a Weekly plan.
     */
    public static List<Exercise> planOptions(List<String> targets, String equipment) {
        LinkedHashMap<String, Exercise> combined = new LinkedHashMap<>();
        if (targets != null) {
            for (String target : targets) {
                for (Exercise exercise : planOptions(target, equipment)) {
                    combined.put(exercise.name + "|" + exercise.target, exercise);
                }
            }
        }
        if (combined.isEmpty()) {
            for (Exercise exercise : planOptions("Full Body", equipment)) {
                combined.put(exercise.name + "|" + exercise.target, exercise);
            }
        }
        return new ArrayList<>(combined.values());
    }

    /** Combines one or two equipment categories and one or two focus areas without duplicates. */
    public static List<Exercise> planOptions(List<String> targets, List<String> equipmentTypes) {
        LinkedHashMap<String, Exercise> combined = new LinkedHashMap<>();
        if (equipmentTypes != null) {
            for (String equipment : equipmentTypes) {
                for (Exercise exercise : planOptions(targets, equipment)) {
                    combined.put(exercise.name + "|" + exercise.target, exercise);
                }
            }
        }
        if (combined.isEmpty()) {
            for (Exercise exercise : planOptions(targets, EQUIPMENT_TYPES[0])) {
                combined.put(exercise.name + "|" + exercise.target, exercise);
            }
        }
        return new ArrayList<>(combined.values());
    }

    /** The default shortlist fills a chosen session without forcing every catalogue item into it. */
    public static List<Exercise> recommendedForDuration(List<Exercise> options, int minutes) {
        int idealCount = minutes <= 45 ? 4 : minutes <= 60 ? 6 : minutes <= 110 ? 8 : 10;
        ArrayList<Exercise> recommendation = new ArrayList<>();
        if (options == null) return recommendation;
        for (Exercise exercise : options) {
            if (recommendation.size() >= idealCount) break;
            recommendation.add(exercise);
        }
        return recommendation;
    }

    public static String durationSummary(int minutes) {
        int movements = minutes <= 45 ? 4 : minutes <= 60 ? 6 : minutes <= 110 ? 8 : 10;
        return movements + " recommended movements · custom 45 sec–8 min rest";
    }

    /** Creates a session-specific copy without mutating the offline exercise catalogue. */
    public static Exercise prescribe(Exercise exercise, int sets, int reps, int restSeconds) {
        return new Exercise(exercise.name, exercise.target, sets, reps, restSeconds, exercise.cue, exercise.description);
    }

    private static List<Exercise> dumbbellOptions(String target) {
        if ("Chest".equals(target)) return options(
                ex("Floor Press", "Chest", "Pause lightly, then press smoothly.", "Dumbbell chest pressing from the floor."),
                ex("Chest Fly", "Chest", "Hug a big tree; use a light load.", "Dumbbell chest isolation."),
                ex("Dumbbell Press", "Chest", "Keep wrists stacked over elbows.", "Dumbbell pressing for chest and shoulders."));
        if ("Legs".equals(target) || "Lower".equals(target)) return options(
                ex("Goblet Squat", target, "Sit between your heels; stand tall.", "Dumbbell leg strength."),
                ex("Romanian Deadlift", target, "Hips travel back; keep the load close.", "Dumbbell hamstring and glute work."),
                ex("Reverse Lunge", target, "Step softly back; drive through the front foot.", "Single-leg dumbbell control."));
        if ("Arms".equals(target)) return options(
                ex("Biceps Curl", target, "Keep elbows beside your ribs.", "Dumbbell biceps work."),
                ex("Hammer Curl", target, "Keep palms facing in.", "Forearm and biceps work."),
                ex("Overhead Triceps Extension", target, "Keep elbows aimed forward.", "Dumbbell triceps work."));
        if ("Upper".equals(target)) return options(
                ex("Dumbbell Press", target, "Press tall; ribs stay down.", "Dumbbell shoulders and triceps."),
                ex("One-arm Row", target, "Pull elbow toward your hip.", "Dumbbell upper-back strength."),
                ex("Band Pull-apart", target, "Open your chest; squeeze shoulder blades.", "Upper-back posture support."));
        if ("Full Body".equals(target)) return options(
                ex("Squat to Press", target, "Stand strongly, then press overhead.", "Dumbbell full-body strength."),
                ex("Renegade Row", target, "Resist twisting through your hips.", "Dumbbell core and upper-back work."),
                ex("Goblet Squat", target, "Knees follow your toes.", "Dumbbell leg strength."));
        return copyLibrary(target);
    }

    private static List<Exercise> householdOptions(String target) {
        if ("Chest".equals(target)) return options(
                ex("Push-up", target, "Brace your core; press the floor away.", "No-equipment chest pressing."),
                ex("Incline Push-up", target, "Body moves as one unit.", "Use a sturdy counter or bench."),
                ex("Backpack Floor Press", target, "Keep the backpack secure and move slowly.", "A household-weight chest press."));
        if ("Legs".equals(target) || "Lower".equals(target)) return options(
                ex("Bodyweight Squat", target, "Knees follow your toes.", "Use a backpack only if it feels stable."),
                ex("Step-up", target, "Use a stable step and drive through the whole foot.", "A household step-up."),
                ex("Glute Bridge", target, "Ribs down; squeeze at the top.", "A floor-based hip exercise."));
        if ("Arms".equals(target)) return options(
                ex("Isometric Towel Curl", target, "Pull steadily against the towel.", "Use a towel for resistance."),
                ex("Bench Dip", target, "Shoulders stay away from ears.", "Use a stable chair or bench."),
                ex("Overhead Triceps Extension", target, "Move slowly and keep elbows forward.", "Use a light household object."));
        return copyLibrary(target);
    }

    private static List<Exercise> machineOptions(String target) {
        if ("Chest".equals(target)) return options(
                ex("Machine Chest Press", target, "Set handles at mid-chest; stop short of locking out.", "Guided machine pressing."),
                ex("Seated Cable Press", target, "Press forward without shrugging.", "Controlled cable chest pressing."),
                ex("Pec Deck Fly", target, "Keep a soft elbow and squeeze your chest.", "Machine-assisted chest isolation."),
                ex("Assisted Dip", target, "Lean slightly forward and lower with control.", "Guided chest and triceps pressing."));
        if ("Legs".equals(target) || "Lower".equals(target)) return options(
                ex("Leg Press", target, "Keep your feet planted; do not lock your knees.", "Guided lower-body pressing."),
                ex("Seated Leg Curl", target, "Move slowly and control the return.", "Machine hamstring work."),
                ex("Leg Extension", target, "Control the lift and lower; do not lock your knees.", "Machine quadriceps work."),
                ex("Calf Raise", target, "Pause high; lower slowly.", "Machine or supported calf work."));
        if ("Arms".equals(target)) return options(
                ex("Cable Biceps Curl", target, "Keep elbows beside your ribs.", "Cable biceps resistance."),
                ex("Cable Triceps Pressdown", target, "Keep elbows close to your body.", "Cable triceps resistance."),
                ex("Assisted Dip", target, "Keep shoulders away from your ears.", "Guided triceps pressing."),
                ex("Seated Cable Row", target, "Stay tall and pull toward your ribs.", "Machine-assisted upper-body pulling."));
        if ("Upper".equals(target)) return options(
                ex("Lat Pulldown", target, "Keep ribs down and pull toward your upper chest.", "Machine-assisted upper back."),
                ex("Seated Cable Row", target, "Stay tall and pull toward your ribs.", "Cable upper-back work."),
                ex("Machine Shoulder Press", target, "Keep ribs down and press smoothly.", "Guided shoulder pressing."),
                ex("Cable Reverse Fly", target, "Open your arms without shrugging.", "Cable rear-shoulder work."));
        return options(
                ex("Lat Pulldown", target, "Keep ribs down and pull toward your upper chest.", "Machine-assisted movement."),
                ex("Leg Press", target, "Keep your feet planted; do not lock your knees.", "Guided lower-body movement."),
                ex("Seated Cable Row", target, "Stay tall and pull toward your ribs.", "Controlled cable movement."),
                ex("Machine Chest Press", target, "Press forward with control.", "Guided upper-body pressing."));
    }

    private static List<Exercise> bodyweightOptions(String target) {
        if ("Chest".equals(target)) return options(
                ex("Push-up", target, "Brace your core; press the floor away.", "Bodyweight chest pressing."),
                ex("Incline Push-up", target, "Body moves as one unit.", "A scalable bodyweight press."),
                ex("Bench Dip", target, "Shoulders stay away from ears.", "Supported pressing for chest and triceps."));
        if ("Legs".equals(target) || "Lower".equals(target)) return options(
                ex("Bodyweight Squat", target, "Knees follow your toes.", "Bodyweight leg strength."),
                ex("Walking Lunge", target, "Stay tall as you step.", "Leg strength and balance."),
                ex("Wall Sit", target, "Press your back into the wall.", "Lower-body endurance."));
        if ("Arms".equals(target)) return options(
                ex("Bench Dip", target, "Shoulders stay away from ears.", "Bodyweight triceps work."),
                ex("Push-up", target, "Keep your elbows controlled.", "Chest and triceps pressing."),
                ex("Isometric Towel Curl", target, "Pull steadily against the towel.", "Simple arm resistance."));
        if ("Full Body".equals(target)) return options(
                ex("Burpee", target, "Choose a step-back option when needed.", "Full-body conditioning."),
                ex("Mountain Climber", target, "Keep shoulders stacked over hands.", "Core and conditioning."),
                ex("Squat to Press", target, "Use a strong squat and controlled reach.", "Full-body movement."));
        return copyLibrary(target);
    }

    private static List<Exercise> copyLibrary(String target) {
        List<Exercise> source = LIBRARY.get(target);
        if (source == null) source = LIBRARY.get("Full Body");
        return new ArrayList<>(source);
    }

    private static List<Exercise> options(Exercise... exercises) {
        return new ArrayList<>(Arrays.asList(exercises));
    }
    public static String goalSummary(String goal) {
        if(goal.equals(GOALS[0])) return "Likely focus: stronger movement patterns and confidence with gradually heavier loads.";
        if(goal.equals(GOALS[1])) return "Likely focus: more weekly training volume, controlled reps and recovery.";
        if(goal.equals(GOALS[2])) return "Likely focus: consistent activity, sustainable meals and a gradual body-weight trend.";
        if(goal.equals(GOALS[3])) return "Likely focus: improved work capacity and recovery between efforts.";
        return "Likely focus: a repeatable movement habit, mobility and energy.";
    }
    public static Map<String, List<Exercise>> library() { return LIBRARY; }
    public static List<String> meals(String goal) {
        if (goal.equals(GOALS[0])) return Arrays.asList("Eggs, oats, berries and yogurt", "Chicken, quinoa and roasted vegetables", "Salmon, potatoes and a green salad");
        if (goal.equals(GOALS[1])) return Arrays.asList("Greek yogurt, fruit and granola", "Turkey or tofu rice bowl with vegetables", "Beans, avocado and whole-grain toast");
        if (goal.equals(GOALS[2])) return Arrays.asList("Vegetable omelette with whole-grain toast", "Lentil salad with colourful vegetables", "Fish or tofu with beans and greens");
        if (goal.equals(GOALS[3])) return Arrays.asList("Oats, fruit and yogurt before training", "Chicken or chickpea grain bowl", "Pasta with vegetables and a protein source after training");
        return Arrays.asList("Overnight oats with fruit and seeds", "Colorful grain bowl with protein", "Vegetable soup, whole-grain bread and yogurt");
    }
    private Coach() {}
}
