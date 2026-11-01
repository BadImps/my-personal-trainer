package com.badimps.personaltrainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Owns completed guided-workout results independently of contiguous exercise positions.
 * A skipped exercise may leave an index gap without causing later completed results to vanish.
 */
final class WorkoutCompletionLedger {
    private final TreeMap<Integer, SetTimeStore.ExerciseSummary> completed = new TreeMap<>();
    private final Set<Integer> skipped = new TreeSet<>();

    SetTimeStore.ExerciseSummary freeze(int exerciseIndex, SetTimeStore.ExerciseSummary summary) {
        if (exerciseIndex < 0) return summary;
        SetTimeStore.ExerciseSummary existing = completed.get(exerciseIndex);
        if (existing != null) return existing;
        completed.put(exerciseIndex, summary);
        skipped.remove(exerciseIndex);
        return summary;
    }

    /** Records every exercise bypassed by a forward selection from the cooldown browser. */
    void selectFromCooldown(int completedExerciseIndex, int selectedExerciseIndex) {
        if (selectedExerciseIndex <= completedExerciseIndex) {
            rewindFrom(selectedExerciseIndex);
            return;
        }
        for (int index = completedExerciseIndex + 1; index < selectedExerciseIndex; index++) {
            skipped.add(index);
            completed.remove(index);
        }
        skipped.remove(selectedExerciseIndex);
    }

    void skip(int exerciseIndex) {
        if (exerciseIndex < 0) return;
        completed.remove(exerciseIndex);
        skipped.add(exerciseIndex);
    }

    void rewindFrom(int exerciseIndex) {
        if (exerciseIndex < 0) return;
        completed.tailMap(exerciseIndex, true).clear();
        skipped.removeIf(index -> index >= exerciseIndex);
    }

    int nextExerciseIndex(int currentIndex, int exerciseCount) {
        int next = currentIndex + 1;
        while (next < exerciseCount && skipped.contains(next)) next++;
        return next;
    }

    List<SetTimeStore.ExerciseSummary> completedSummaries() {
        return Collections.unmodifiableList(new ArrayList<>(completed.values()));
    }

    boolean isSkipped(int exerciseIndex) { return skipped.contains(exerciseIndex); }

    void clear() { completed.clear(); skipped.clear(); }
}
