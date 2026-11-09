package com.badimps.personaltrainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, comfort-first catalogue for the optional post-workout cool-down.
 * These are gentle mobility suggestions, not treatment or a promise to prevent soreness/injury.
 * Each motion has a local Strength Level animation in {@link ExerciseWebp}.
 */
public final class CooldownStretchLibrary {
    private CooldownStretchLibrary() { }

    public static final class Move {
        public final Coach.Exercise exercise;
        public final String timing;

        Move(String name, String focus, String cue, String timing) {
            this.exercise = new Coach.Exercise(name, focus, 1, 1, 0, cue,
                    "Gentle cool-down mobility for " + focus.toLowerCase() + ".");
            this.timing = timing;
        }
    }

    private static Move move(String name, String focus, String cue, String timing) {
        return new Move(name, focus, cue, timing);
    }

    /**
     * Returns up to two compatible motions. A multi-area session gets one move from each of its
     * first two areas, avoiding an overly busy 5-minute recovery screen.
     */
    public static List<Move> forTargets(List<String> targets) {
        LinkedHashMap<String, Move> choices = new LinkedHashMap<>();
        if (targets != null) {
            for (String target : targets) {
                for (Move move : movesFor(target)) {
                    choices.putIfAbsent(move.exercise.name, move);
                    if (choices.size() >= 2) return new ArrayList<>(choices.values());
                }
            }
        }
        for (Move fallback : movesFor("Full Body")) {
            choices.putIfAbsent(fallback.exercise.name, fallback);
            if (choices.size() >= 2) break;
        }
        return new ArrayList<>(choices.values());
    }

    private static List<Move> movesFor(String rawTarget) {
        String target = rawTarget == null ? "" : rawTarget;
        ArrayList<Move> moves = new ArrayList<>();
        switch (target) {
            case "Lower":
            case "Legs":
                moves.add(move("90/90 Hip Switch", "Hips and legs", "Use your hands for support. Keep the range easy.", "45 sec · alternate sides"));
                moves.add(move("World's Greatest Stretch", "Hips, hamstrings and upper back", "Keep the range comfortable; do not force the lunge.", "45 sec · alternate sides"));
                break;
            case "Back":
                moves.add(move("Cat Cow", "Spine and back", "Move one vertebra at a time with a calm breath.", "45 sec · slow breathing"));
                moves.add(move("Prone Snow Angel", "Upper back", "Keep your neck long and use a small, pain-free range.", "45 sec · easy reps"));
                break;
            case "Shoulders":
                moves.add(move("Shoulder Wall Slide", "Shoulders", "Keep ribs soft and stop before any pinch.", "45 sec · slow slides"));
                moves.add(move("Prone Snow Angel", "Rear shoulders and upper back", "Reach long without shrugging your shoulders.", "45 sec · easy reps"));
                break;
            case "Arms":
            case "Biceps":
            case "Triceps":
                moves.add(move("Shoulder Wall Slide", "Arms and shoulders", "Keep the movement smooth; lower the range if it pinches.", "45 sec · slow slides"));
                moves.add(move("World's Greatest Stretch", "Arms, chest and hips", "Open gently through the chest; never force the shoulder.", "45 sec · alternate sides"));
                break;
            case "Chest":
                moves.add(move("Shoulder Wall Slide", "Chest and shoulders", "Keep ribs down and move only through a comfortable range.", "45 sec · slow slides"));
                moves.add(move("Cat Cow", "Chest and upper back", "Use your breath to make the motion light and unforced.", "45 sec · slow breathing"));
                break;
            case "Core":
                moves.add(move("Cat Cow", "Core and spine", "Move gently; choose a smaller range for a sensitive back.", "45 sec · slow breathing"));
                moves.add(move("Bird Dog", "Core control", "Reach long without twisting. This is gentle movement, not a hard set.", "45 sec · alternate sides"));
                break;
            case "Endurance":
                moves.add(move("Brisk March", "Whole body reset", "Ease into a very slow march and let your breathing settle.", "60 sec · easy pace"));
                moves.add(move("90/90 Hip Switch", "Hips", "Use your hands for support and keep the range comfortable.", "45 sec · alternate sides"));
                break;
            case "Mobility":
                moves.add(move("Cat Cow", "Spinal mobility", "Move slowly with your breath.", "45 sec · slow breathing"));
                moves.add(move("90/90 Hip Switch", "Hip mobility", "Use your hands for support if needed.", "45 sec · alternate sides"));
                break;
            case "Upper":
                moves.add(move("Shoulder Wall Slide", "Upper body", "Keep ribs soft and arms smooth.", "45 sec · slow slides"));
                moves.add(move("Cat Cow", "Upper back", "Move smoothly with a long neck.", "45 sec · slow breathing"));
                break;
            case "Full Body":
            default:
                moves.add(move("World's Greatest Stretch", "Full body", "Keep the range comfortable and do not force the lunge.", "45 sec · alternate sides"));
                moves.add(move("Shoulder Wall Slide", "Upper body", "Keep ribs soft and arms smooth.", "45 sec · slow slides"));
                break;
        }
        return moves;
    }
}
