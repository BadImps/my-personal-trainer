package com.badimps.personaltrainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Offline training templates. These are general fitness suggestions, not medical advice. */
public final class Coach {
    /** Training areas are deliberately separate from the equipment choice. */
    /**
     * The planning focuses intentionally keep Back, Shoulders, Core and Endurance separate.
     * There is no standalone "Abs" category: trunk work belongs under the more complete Core focus.
     */
    public static final String[] TARGETS = {"Upper", "Lower", "Arms", "Biceps", "Triceps", "Chest", "Legs", "Back", "Shoulders", "Core", "Endurance", "Mobility", "Full Body"};
    public static final String[] GOALS = {"Build strength", "Build muscle", "Reduce body fat", "Improve endurance", "Move & feel good"};
    public static final String[] ENVIRONMENTS = {"Gym", "Home — no equipment", "Home — dumbbells", "Outdoors"};
    public static final String[] PLAN_MODES = {"Weekly", "Daily", "Custom"};
    public static final String[] EQUIPMENT_TYPES = {"Dumbbells", "At home", "Machine-assisted", "Bodyweight", "Barbell"};

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
        add("Upper", ex("Dumbbell Press", "Upper", "Press tall; ribs stay down.", "Dumbbell shoulders and triceps."), ex("Dumbbell One-arm Row", "Upper", "Pull elbow toward your hip.", "Dumbbell upper-back strength."), ex("Incline Push-up", "Upper", "Move as one strong plank.", "Counter-supported pressing."), ex("Prone Y Raise", "Upper", "Reach long without shrugging.", "No-equipment upper-back control."));
        add("Lower", ex("Goblet Squat", "Lower", "Sit between your heels; stand tall.", "Quads and glutes."), ex("Romanian Deadlift", "Lower", "Hips travel back; keep the load close.", "Hamstrings and glutes."), ex("Reverse Lunge", "Lower", "Step softly back; drive through front foot.", "Single-leg control."), ex("Glute Bridge", "Lower", "Ribs down; squeeze at the top.", "Hip extension."));
        add("Arms", ex("Dumbbell Curl", "Arms", "Keep elbows beside your ribs.", "Dumbbell biceps isolation."), ex("Overhead Triceps Extension", "Arms", "Keep elbows aimed forward.", "Triceps strength."), ex("Hammer Curl", "Arms", "Keep palms facing in.", "Forearms and biceps."), ex("Bench Dips", "Arms", "Shoulders stay away from ears.", "Triceps bodyweight work."));
        add("Biceps", ex("Dumbbell Curl", "Biceps", "Keep elbows beside your ribs.", "Dumbbell biceps isolation."), ex("Hammer Curl", "Biceps", "Keep palms facing in.", "Biceps and forearm control."), ex("Cable Biceps Curl", "Biceps", "Keep your elbows still as you curl.", "Cable biceps resistance."));
        add("Triceps", ex("Overhead Triceps Extension", "Triceps", "Keep elbows aimed forward.", "Triceps isolation."), ex("Cable Triceps Pressdown", "Triceps", "Keep elbows close to your body.", "Cable triceps resistance."), ex("Bench Dips", "Triceps", "Shoulders stay away from ears.", "Supported triceps pressing."));
        add("Chest", ex("Push-up", "Chest", "Brace your core; press the floor away.", "Chest, shoulders and triceps."), ex("Floor Press", "Chest", "Pause lightly, then press smoothly.", "Chest pressing with a safe range."), ex("Dumbbell Fly", "Chest", "Hug a big tree; use a light load.", "Dumbbell chest isolation."), ex("Incline Push-up", "Chest", "Body moves as one unit.", "A scalable push-up."));
        add("Legs", ex("Bodyweight Squat", "Legs", "Knees follow your toes.", "Leg strength and mobility."), ex("Walking Lunge", "Legs", "Stay tall as you step.", "Quads, glutes and balance."), ex("Bodyweight Calf Raise", "Legs", "Pause high, lower slowly.", "Bodyweight calf strength."), ex("Wall Sit", "Legs", "Press your back into the wall.", "Leg endurance."));
        add("Back", ex("One-arm Row", "Back", "Pull your elbow toward your hip without twisting.", "Upper-back pulling strength."), ex("Lat Pulldown", "Back", "Keep ribs down and pull toward the upper chest.", "Vertical back pulling."), ex("Seated Cable Row", "Back", "Sit tall and pull toward your ribs.", "Mid-back control."), ex("Prone Snow Angel", "Back", "Move slowly and keep the neck long.", "No-equipment upper-back control."));
        add("Shoulders", ex("Dumbbell Press", "Shoulders", "Press tall while keeping ribs down.", "Dumbbell shoulder and triceps strength."), ex("Dumbbell Lateral Raise", "Shoulders", "Lift only to shoulder height with a soft elbow.", "Dumbbell side-shoulder control."), ex("Dumbbell Reverse Fly", "Shoulders", "Open your arms with control.", "Dumbbell rear-shoulder work."), ex("Shoulder Wall Slide", "Shoulders", "Keep the range smooth and comfortable.", "No-equipment shoulder control."));
        add("Core", ex("Dead Bug", "Core", "Keep your low back gently grounded.", "Deep trunk stability."), ex("Bird Dog", "Core", "Reach long without rotating through your torso.", "Cross-body core control."), ex("Side Plank", "Core", "Press the floor away and keep a long line.", "Lateral trunk stability."), ex("Mountain Climber", "Core", "Keep shoulders stacked over hands.", "Core control with conditioning."));
        add("Endurance", ex("Brisk March", "Endurance", "Land softly and swing your arms.", "Low-impact aerobic conditioning."), ex("Step-up", "Endurance", "Use a stable step and drive through the whole foot.", "Leg and cardio conditioning."), ex("Jumping Jack", "Endurance", "Choose a step-out version if needed.", "Full-body aerobic work."), ex("Mountain Climber", "Endurance", "Keep shoulders stacked over hands.", "Higher-intensity conditioning."));
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
            else if(name.equals("Floor Press")||name.equals("Dumbbell Fly")){name="Push-up";cue="Brace your core; press the floor away.";description="No-equipment chest training.";}
            else if(name.equals("Dumbbell Curl")||name.equals("Hammer Curl")){name="Self-resisted Biceps Curl";cue="Use your free hand for light, steady resistance.";description="A no-equipment biceps option.";}
        }
        return new Exercise(name,e.target,sets,reps,rest,cue,description);
    }

    /** Returns equipment-specific movements for the plan builder. */
    public static List<Exercise> planOptions(String target, String equipment) {
        if ("Biceps".equals(target)) return bicepsOptions(equipment);
        if ("Triceps".equals(target)) return tricepsOptions(equipment);
        if ("Barbell".equals(equipment)) return barbellOptions(target);
        if ("Dumbbells".equals(equipment)) return dumbbellOptions(target);
        if ("At home".equals(equipment) || "Home & household".equals(equipment)) return householdOptions(target);
        if ("Machine-assisted".equals(equipment) || "Machine assisted".equals(equipment)) return machineOptions(target);
        return bodyweightOptions(target);
    }

    private static List<Exercise> barbellOptions(String target) {
        if ("Biceps".equals(target)) return options(
                ex("Barbell Curl", target, "Keep elbows beside your ribs and lower slowly.", "Straight-bar biceps work."),
                ex("Reverse Barbell Curl", target, "Keep wrists neutral and control the return.", "Biceps and forearm control."),
                ex("Drag Curl", target, "Keep the bar close and move with control.", "Barbell biceps emphasis."));
        if ("Triceps".equals(target)) return options(
                ex("Close-Grip Barbell Bench Press", target, "Keep elbows close and press smoothly.", "Barbell triceps pressing."),
                ex("Barbell Skull Crusher", target, "Keep upper arms still and use a controlled range.", "Lying barbell triceps work."),
                ex("Barbell Floor Press", target, "Keep elbows close and press smoothly.", "Triceps-focused barbell press."));
        if ("Chest".equals(target)) return options(
                ex("Barbell Bench Press", target, "Keep feet planted and press with control.", "Classic barbell chest press."),
                ex("Incline Barbell Press", target, "Keep shoulders down and wrists stacked.", "Upper-chest barbell pressing."),
                ex("Barbell Floor Press", target, "Pause lightly, then press smoothly.", "Barbell chest pressing from the floor."));
        if ("Back".equals(target)) return options(
                ex("Barbell Row", target, "Hinge at the hips and pull toward your lower ribs.", "Barbell upper-back pulling."),
                ex("Barbell Shrug", target, "Lift straight up and lower slowly.", "Upper-trap barbell work."),
                ex("Pendlay Row", target, "Reset between repetitions and keep your torso steady.", "Barbell back strength."));
        if ("Shoulders".equals(target)) return options(
                ex("Barbell Overhead Press", target, "Keep ribs down and press smoothly.", "Standing barbell shoulder press."),
                ex("Barbell Landmine Press", target, "Press forward and up with a stable stance.", "Angled barbell shoulder pressing."),
                ex("Barbell Front Raise", target, "Lift only to shoulder height with a soft elbow.", "Controlled barbell front-shoulder work."));
        if ("Legs".equals(target) || "Lower".equals(target)) return options(
                ex("Barbell Back Squat", target, "Brace first; keep knees tracking your toes.", "Barbell lower-body strength."),
                ex("Barbell Romanian Deadlift", target, "Keep the bar close as hips travel back.", "Barbell hamstring and glute work."),
                ex("Barbell Hip Thrust", target, "Pause at the top without arching your back.", "Barbell glute strength."));
        if ("Core".equals(target)) return options(
                ex("Barbell Landmine Rotation", target, "Rotate smoothly and keep your stance stable.", "Barbell rotational core work."),
                ex("Barbell Rollout", target, "Keep ribs down and use a comfortable range.", "Barbell trunk control."),
                ex("Barbell Landmine Anti-Rotation Hold", target, "Brace your trunk and resist twisting.", "Barbell core stability."));
        if ("Full Body".equals(target)) return options(
                ex("Barbell Thruster", target, "Stand strongly from the squat, then press overhead.", "Barbell full-body strength."),
                ex("Barbell Deadlift", target, "Push the floor away and keep the bar close.", "Barbell total-body pull."),
                ex("Barbell Clean Pull", target, "Drive through your legs and keep the bar close.", "Explosive barbell full-body work."));
        return options(
                ex("Barbell Bench Press", target, "Keep feet planted and press smoothly.", "Barbell pushing strength."),
                ex("Barbell Row", target, "Pull toward your lower ribs with control.", "Barbell pulling strength."),
                ex("Barbell Overhead Press", target, "Keep ribs down and press smoothly.", "Barbell upper-body strength."));
    }

    private static List<Exercise> bicepsOptions(String equipment) {
        if ("Barbell".equals(equipment)) return options(
                ex("Barbell Curl", "Biceps", "Keep elbows beside your ribs and lower slowly.", "Barbell biceps work."),
                ex("Barbell Reverse Curl", "Biceps", "Keep wrists neutral and control the return.", "Barbell biceps and forearm work."),
                ex("Barbell Drag Curl", "Biceps", "Keep the bar close and move with control.", "Barbell biceps emphasis."));
        if ("Machine-assisted".equals(equipment) || "Machine assisted".equals(equipment)) return options(
                ex("Cable Biceps Curl", "Biceps", "Keep elbows beside your ribs.", "Cable biceps resistance."),
                ex("Preacher Curl Machine", "Biceps", "Keep your upper arm supported and lower slowly.", "Guided biceps curl."),
                ex("Cable Hammer Curl", "Biceps", "Keep palms facing each other.", "Cable biceps and forearm work."));
        if ("At home".equals(equipment) || "Home & household".equals(equipment)) return options(
                ex("Backpack Curl", "Biceps", "Keep elbows still and move slowly.", "Household-weight biceps work."),
                ex("Isometric Towel Curl", "Biceps", "Pull steadily against the towel.", "Towel resistance for biceps."),
                ex("Water Bottle Hammer Curl", "Biceps", "Keep palms facing in.", "Light household biceps work."));
        if ("Bodyweight".equals(equipment)) return options(
                ex("Self-resisted Biceps Curl", "Biceps", "Use your free hand for light, steady resistance.", "No-equipment biceps resistance."),
                ex("Biceps Flex Hold", "Biceps", "Squeeze the biceps without holding an object.", "No-equipment biceps isometric."),
                ex("Self-resisted Hammer Curl", "Biceps", "Keep palms facing in and resist with your free hand.", "No-equipment biceps and forearm control."));
        return options(
                ex("Dumbbell Curl", "Biceps", "Keep elbows beside your ribs.", "Dumbbell biceps work."),
                ex("Dumbbell Hammer Curl", "Biceps", "Keep palms facing in.", "Dumbbell biceps and forearm work."),
                ex("Dumbbell Concentration Curl", "Biceps", "Brace your elbow and curl with control.", "Focused dumbbell biceps work."));
    }

    private static List<Exercise> tricepsOptions(String equipment) {
        if ("Barbell".equals(equipment)) return options(
                ex("Close-Grip Barbell Floor Press", "Triceps", "Keep elbows close and press smoothly.", "Barbell triceps pressing without a bench."),
                ex("Barbell Skull Crusher", "Triceps", "Keep upper arms still and use a controlled range.", "Lying barbell triceps work."),
                ex("Barbell JM Press", "Triceps", "Lower with control and keep elbows pointed forward.", "Barbell triceps pressing."));
        if ("Machine-assisted".equals(equipment) || "Machine assisted".equals(equipment)) return options(
                ex("Cable Triceps Pressdown", "Triceps", "Keep elbows close to your body.", "Cable triceps resistance."),
                ex("Seated Dip Machine", "Triceps", "Keep shoulders away from your ears.", "Guided triceps pressing."),
                ex("Cable Overhead Extension", "Triceps", "Keep elbows pointed forward.", "Cable overhead triceps work."));
        if ("At home".equals(equipment) || "Home & household".equals(equipment)) return options(
                ex("Chair Triceps Dip", "Triceps", "Use a stable chair and keep shoulders down.", "Household supported triceps press."),
                ex("Water Bottle Overhead Extension", "Triceps", "Move slowly and keep elbows forward.", "Light household triceps work."),
                ex("Counter Triceps Press", "Triceps", "Use a sturdy counter and keep a long body line.", "At-home triceps press."));
        if ("Bodyweight".equals(equipment)) return options(
                ex("Diamond Push-up", "Triceps", "Keep your elbows controlled and scale from knees if needed.", "Bodyweight triceps pressing."),
                ex("Close-grip Push-up", "Triceps", "Keep your body in one long line and press the floor away.", "No-equipment triceps pressing."),
                ex("Triceps Push-up Hold", "Triceps", "Keep your body in one long line and press the floor away.", "No-equipment triceps support."));
        return options(
                ex("Dumbbell Overhead Triceps Extension", "Triceps", "Keep elbows aimed forward.", "Dumbbell triceps work."),
                ex("Dumbbell Triceps Kickback", "Triceps", "Keep your upper arm still as you straighten the elbow.", "Dumbbell triceps isolation."),
                ex("Dumbbell Close-Grip Floor Press", "Triceps", "Keep elbows close and press smoothly.", "Dumbbell triceps pressing."));
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
        if ("Back".equals(target)) return options(
                ex("Dumbbell One-arm Row", target, "Pull your elbow toward your hip; avoid twisting.", "Dumbbell upper-back pulling."),
                ex("Dumbbell Reverse Fly", target, "Keep a soft elbow and raise with control.", "Dumbbell rear-shoulder and upper-back work."),
                ex("Dumbbell Pullover", target, "Keep ribs down and move only through a comfortable range.", "Dumbbell lats and trunk control."));
        if ("Shoulders".equals(target)) return options(
                ex("Dumbbell Press", target, "Press tall; ribs stay down.", "Dumbbell shoulder pressing."),
                ex("Dumbbell Lateral Raise", target, "Lift to shoulder height with a soft elbow.", "Dumbbell side-shoulder control."),
                ex("Dumbbell Front Raise", target, "Keep the torso still and use a light load.", "Dumbbell front-shoulder control."));
        if ("Core".equals(target)) return options(
                ex("Dumbbell Dead Bug", target, "Move slowly while keeping your low back gently grounded.", "Loaded trunk stability."),
                ex("Dumbbell Suitcase Carry", target, "Stand tall and do not lean into the weight.", "One-sided dumbbell carry for core control."),
                ex("Dumbbell Renegade Row", target, "Resist twisting through your hips.", "Dumbbell core and upper-back control."));
        if ("Endurance".equals(target)) return options(
                ex("Dumbbell Thruster", target, "Use a controlled squat and smooth overhead press.", "Full-body dumbbell conditioning."),
                ex("Dumbbell March", target, "Stand tall, move steadily and keep the load close.", "Low-impact loaded conditioning."),
                ex("Dumbbell High Pull", target, "Drive through your legs and guide the dumbbells close to your body.", "Full-body dumbbell conditioning."));
        if ("Chest".equals(target)) return options(
                ex("Floor Press", "Chest", "Pause lightly, then press smoothly.", "Dumbbell chest pressing from the floor."),
                ex("Dumbbell Fly", "Chest", "Hug a big tree; use a light load.", "Dumbbell chest isolation."),
                ex("Dumbbell Squeeze Press", "Chest", "Press the dumbbells together as you move smoothly.", "Dumbbell chest pressing without a bench."));
        if ("Legs".equals(target) || "Lower".equals(target)) return options(
                ex("Dumbbell Goblet Squat", target, "Sit between your heels; stand tall.", "Dumbbell leg strength."),
                ex("Dumbbell Romanian Deadlift", target, "Hips travel back; keep the load close.", "Dumbbell hamstring and glute work."),
                ex("Dumbbell Reverse Lunge", target, "Step softly back; drive through the front foot.", "Single-leg dumbbell control."));
        if ("Arms".equals(target)) return options(
                ex("Dumbbell Curl", target, "Keep elbows beside your ribs.", "Dumbbell biceps work."),
                ex("Dumbbell Hammer Curl", target, "Keep palms facing in.", "Dumbbell forearm and biceps work."),
                ex("Dumbbell Overhead Triceps Extension", target, "Keep elbows aimed forward.", "Dumbbell triceps work."));
        if ("Upper".equals(target)) return options(
                ex("Dumbbell Press", target, "Press tall; ribs stay down.", "Dumbbell shoulders and triceps."),
                ex("Dumbbell One-arm Row", target, "Pull elbow toward your hip.", "Dumbbell upper-back strength."),
                ex("Dumbbell Reverse Fly", target, "Open your arms with control.", "Dumbbell upper-back posture work."));
        if ("Full Body".equals(target)) return options(
                ex("Dumbbell Thruster", target, "Stand strongly, then press overhead.", "Dumbbell full-body strength."),
                ex("Dumbbell Renegade Row", target, "Resist twisting through your hips.", "Dumbbell core and upper-back work."),
                ex("Dumbbell Goblet Squat", target, "Knees follow your toes.", "Dumbbell leg strength."));
        if ("Mobility".equals(target)) return options(
                ex("Dumbbell Halo", target, "Move the dumbbell slowly around your head with ribs down.", "Dumbbell shoulder mobility."),
                ex("Dumbbell Windmill", target, "Keep the load stable and move through a comfortable range.", "Dumbbell hip and shoulder mobility."),
                ex("Dumbbell Arm Bar", target, "Keep the dumbbell vertical and roll only as far as comfortable.", "Dumbbell shoulder-control mobility."));
        return dumbbellOptions("Full Body");
    }

    private static List<Exercise> householdOptions(String target) {
        if ("Back".equals(target)) return options(
                ex("Backpack Row", target, "Keep the load close and pull your elbow toward your hip.", "Household-weight back training."),
                ex("Towel Pull-apart", target, "Keep the towel taut and squeeze your shoulder blades.", "Light home upper-back work."),
                ex("Backpack Reverse Fly", target, "Use a light backpack and move with control.", "Household-weight rear-back work."));
        if ("Shoulders".equals(target)) return options(
                ex("Water Bottle Lateral Raise", target, "Use a light load and lift only to shoulder height.", "Household shoulder control."),
                ex("Water Bottle Shoulder Press", target, "Press smoothly and keep your ribs down.", "Household shoulder pressing."),
                ex("Backpack Front Raise", target, "Lift only to shoulder height with a stable torso.", "Household front-shoulder work."));
        if ("Core".equals(target)) return options(
                ex("Backpack Dead Bug", target, "Keep the backpack steady and your low back gently grounded.", "Household-weight core stability."),
                ex("Backpack Suitcase March", target, "Stay tall and resist leaning as you march.", "Household carry for core control."),
                ex("Towel Plank Drag", target, "Drag the towel slowly without twisting your hips.", "Household core control."));
        if ("Endurance".equals(target)) return options(
                ex("Stair Step-up", target, "Use a stable household stair and drive through the whole foot.", "Home cardio using stairs."),
                ex("Backpack March", target, "Keep the load secure and march at a steady pace.", "Loaded household conditioning."),
                ex("Water Bottle Carry", target, "Walk smoothly with bottles held at your sides.", "Household carry conditioning."));
        if ("Chest".equals(target)) return options(
                ex("Counter Incline Push-up", target, "Body moves as one unit.", "Chest pressing using a sturdy household counter."),
                ex("Backpack Floor Press", target, "Keep the backpack secure and move slowly.", "A household-weight chest press."),
                ex("Towel Chest Press", target, "Press your palms steadily into a taut towel without holding your breath.", "Low-load at-home chest isometric."));
        if ("Legs".equals(target) || "Lower".equals(target)) return options(
                ex("Backpack Squat", target, "Keep the backpack secure and knees following your toes.", "Household-weight leg strength."),
                ex("Stair Step-up", target, "Use a stable household stair and drive through the whole foot.", "Household leg conditioning."),
                ex("Chair Sit-to-Stand", target, "Stand smoothly from a stable chair without rocking.", "Household lower-body strength."));
        if ("Arms".equals(target)) return options(
                ex("Isometric Towel Curl", target, "Pull steadily against the towel.", "Use a towel for resistance."),
                ex("Chair Triceps Dip", target, "Shoulders stay away from ears.", "Use a stable household chair."),
                ex("Household Overhead Triceps Extension", target, "Move slowly and keep elbows forward.", "Use a light household object."));
        if ("Upper".equals(target)) return options(
                ex("Backpack Row", target, "Pull the backpack toward your hip with control.", "Household upper-back strength."),
                ex("Counter Incline Push-up", target, "Move as one strong plank.", "Household counter pressing."),
                ex("Water Bottle Shoulder Press", target, "Keep ribs down and press smoothly.", "Household shoulder pressing."));
        if ("Full Body".equals(target)) return options(
                ex("Backpack Squat to Reach", target, "Stand tall and reach with a secure backpack.", "Household full-body movement."),
                ex("Stair Step-up", target, "Use a stable household stair.", "Household full-body conditioning."),
                ex("Water Bottle Carry", target, "Walk smoothly with bottles held at your sides.", "Household carry conditioning."));
        if ("Mobility".equals(target)) return options(
                ex("Towel Shoulder Pass-through", target, "Use a light towel and move through a comfortable range.", "Household shoulder mobility."),
                ex("Chair Hip Opener", target, "Use a stable chair and keep the range comfortable.", "Household hip mobility."),
                ex("Wall Calf Stretch", target, "Use a wall for balance and keep the back heel down.", "Household lower-leg mobility."));
        return householdOptions("Full Body");
    }

    private static List<Exercise> machineOptions(String target) {
        if ("Back".equals(target)) return options(
                ex("Lat Pulldown", target, "Keep ribs down and pull toward your upper chest.", "Selectorized vertical pulling."),
                ex("Seated Cable Row", target, "Stay tall and pull the handle toward your ribs.", "Cable mid-back work."),
                ex("Machine Row", target, "Pull through your elbows without shrugging.", "Guided upper-back pulling."),
                ex("Assisted Pull-up", target, "Keep your shoulders down as you pull.", "Counterweighted back strength."));
        if ("Shoulders".equals(target)) return options(
                ex("Machine Shoulder Press", target, "Keep ribs down and press smoothly.", "Guided shoulder pressing."),
                ex("Cable Lateral Raise", target, "Lift with control and keep a soft elbow.", "Cable side-shoulder resistance."),
                ex("Machine Reverse Fly", target, "Open your arms without shrugging.", "Machine rear-shoulder work."),
                ex("Face Pull", target, "Draw the rope toward eye level with control.", "Cable rear-shoulder support."));
        if ("Core".equals(target)) return options(
                ex("Cable Pallof Press", target, "Press straight out and resist rotation.", "Cable anti-rotation core work."),
                ex("Cable Wood Chop", target, "Rotate smoothly through your upper body; keep hips stable.", "Cable rotational core control."),
                ex("Rotary Torso Machine", target, "Use a comfortable range and move without jerking.", "Guided rotational trunk work."),
                ex("Captain's Chair Knee Raise", target, "Keep the movement controlled; avoid swinging.", "Supported core control."));
        if ("Endurance".equals(target)) return options(
                ex("Treadmill Incline Walk", target, "Choose a sustainable pace and keep a tall posture.", "Low-impact machine cardio."),
                ex("Stationary Bike", target, "Set the seat comfortably and pedal at a steady rhythm.", "Joint-friendly aerobic work."),
                ex("Rowing Machine", target, "Drive with legs, then pull; return in control.", "Full-body machine conditioning."),
                ex("Elliptical Trainer", target, "Keep a smooth, comfortable stride.", "Low-impact aerobic conditioning."));
        if ("Chest".equals(target)) return options(
                ex("Machine Chest Press", target, "Set handles at mid-chest; stop short of locking out.", "Guided machine pressing."),
                ex("Seated Cable Press", target, "Press forward without shrugging.", "Controlled cable chest pressing."),
                ex("Pec Deck Fly", target, "Keep a soft elbow and squeeze your chest.", "Machine-assisted chest isolation."),
                ex("Seated Dip Machine", target, "Lean slightly forward and lower with control.", "Guided chest and triceps pressing."));
        if ("Legs".equals(target) || "Lower".equals(target)) return options(
                ex("Leg Press", target, "Keep your feet planted; do not lock your knees.", "Guided lower-body pressing."),
                ex("Seated Leg Curl", target, "Move slowly and control the return.", "Machine hamstring work."),
                ex("Leg Extension", target, "Control the lift and lower; do not lock your knees.", "Machine quadriceps work."),
                ex("Machine Calf Raise", target, "Pause high; lower slowly.", "Machine calf work."));
        if ("Arms".equals(target)) return options(
                ex("Cable Biceps Curl", target, "Keep elbows beside your ribs.", "Cable biceps resistance."),
                ex("Cable Triceps Pressdown", target, "Keep elbows close to your body.", "Cable triceps resistance."),
                ex("Seated Dip Machine", target, "Keep shoulders away from your ears.", "Guided triceps pressing."),
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
        if ("Back".equals(target)) return options(
                ex("Prone Snow Angel", target, "Move slowly and keep your neck long.", "No-equipment upper-back control."),
                ex("Superman Hold", target, "Lift lightly and keep the neck long.", "Back-body endurance."),
                ex("Prone Y Raise", target, "Reach long without shrugging.", "Bodyweight upper-back and shoulder control."));
        if ("Shoulders".equals(target)) return options(
                ex("Pike Push-up", target, "Press the floor away; keep your neck long.", "Bodyweight shoulder pressing."),
                ex("Shoulder Wall Slide", target, "Keep ribs soft and arms smooth.", "Shoulder mobility and control."),
                ex("Plank Shoulder Tap", target, "Keep your hips as still as possible.", "Bodyweight shoulder stability."));
        if ("Core".equals(target)) return options(
                ex("Dead Bug", target, "Keep your low back gently grounded.", "Deep trunk stability."),
                ex("Bird Dog", target, "Reach long without twisting.", "Cross-body core control."),
                ex("Side Plank", target, "Press the floor away and stay tall.", "Lateral trunk stability."),
                ex("Mountain Climber", target, "Keep shoulders stacked over hands.", "Core control with conditioning."));
        if ("Endurance".equals(target)) return options(
                ex("Brisk March", target, "Land softly and keep a steady pace.", "Low-impact aerobic work."),
                ex("Jumping Jack", target, "Choose a step-out version if needed.", "Full-body aerobic conditioning."),
                ex("High Knees", target, "Lift your knees to a comfortable height and land softly.", "No-equipment aerobic conditioning."),
                ex("Burpee", target, "Choose a step-back option when needed.", "Higher-intensity full-body conditioning."));
        if ("Chest".equals(target)) return options(
                ex("Push-up", target, "Brace your core; press the floor away.", "Bodyweight chest pressing."),
                ex("Knee Push-up", target, "Keep a long line from knees through shoulders.", "Scaled no-equipment chest pressing."),
                ex("Wide Push-up", target, "Keep your body braced and press the floor away.", "No-equipment chest pressing."));
        if ("Legs".equals(target) || "Lower".equals(target)) return options(
                ex("Bodyweight Squat", target, "Knees follow your toes.", "Bodyweight leg strength."),
                ex("Walking Lunge", target, "Stay tall as you step.", "Leg strength and balance."),
                ex("Wall Sit", target, "Press your back into the wall.", "Lower-body endurance."));
        if ("Arms".equals(target)) return options(
                ex("Diamond Push-up", target, "Keep your elbows controlled.", "No-equipment triceps pressing."),
                ex("Close-grip Push-up", target, "Keep your body in one long line.", "No-equipment arm pressing."),
                ex("Triceps Push-up Hold", target, "Press the floor away and keep your core braced.", "No-equipment triceps support."));
        if ("Biceps".equals(target)) return options(
                ex("Self-resisted Biceps Curl", target, "Use your free hand for light, steady resistance.", "No-equipment biceps resistance."),
                ex("Biceps Flex Hold", target, "Squeeze the biceps without holding an object.", "No-equipment biceps isometric."),
                ex("Self-resisted Hammer Curl", target, "Keep palms facing in and resist with your free hand.", "No-equipment biceps and forearm control."));
        if ("Triceps".equals(target)) return options(
                ex("Diamond Push-up", target, "Keep elbows controlled and scale from knees if needed.", "No-equipment triceps pressing."),
                ex("Close-grip Push-up", target, "Keep your body in one long line.", "No-equipment triceps pressing."),
                ex("Triceps Push-up Hold", target, "Press the floor away and keep your core braced.", "No-equipment triceps support."));
        if ("Full Body".equals(target)) return options(
                ex("Burpee", target, "Choose a step-back option when needed.", "Full-body conditioning."),
                ex("Mountain Climber", target, "Keep shoulders stacked over hands.", "Core and conditioning."),
                ex("Squat to Reach", target, "Use a strong squat and a controlled overhead reach.", "No-equipment full-body movement."));
        if ("Upper".equals(target)) return options(
                ex("Push-up", target, "Brace your core and press the floor away.", "No-equipment upper-body pressing."),
                ex("Prone Y Raise", target, "Reach long without shrugging.", "No-equipment upper-back control."),
                ex("Plank Shoulder Tap", target, "Keep hips as still as possible.", "No-equipment shoulder stability."));
        if ("Mobility".equals(target)) return options(
                ex("Cat Cow", target, "Move slowly with your breath.", "No-equipment spinal mobility."),
                ex("World's Greatest Stretch", target, "Keep the range comfortable.", "No-equipment hip and upper-back mobility."),
                ex("90/90 Hip Switch", target, "Use a comfortable range and stay tall.", "No-equipment hip rotation control."));
        return bodyweightOptions("Full Body");
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
