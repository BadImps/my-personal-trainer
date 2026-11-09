package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class CooldownStretchLibraryTest {
    @Test public void everyTrainingAreaHasTwoGentleCooldownMovements() {
        for (String target : Coach.TARGETS) {
            List<CooldownStretchLibrary.Move> moves = CooldownStretchLibrary.forTargets(Collections.singletonList(target));
            assertEquals(target, 2, moves.size());
            for (CooldownStretchLibrary.Move move : moves) {
                assertNotNull(move.exercise);
                assertFalse(move.exercise.name.isEmpty());
                assertFalse(move.exercise.cue.isEmpty());
                assertFalse(move.timing.isEmpty());
            }
        }
    }

    @Test public void multiTargetSessionUsesTwoDistinctCards() {
        List<CooldownStretchLibrary.Move> moves = CooldownStretchLibrary.forTargets(
                java.util.Arrays.asList("Chest", "Lower", "Core"));
        assertEquals(2, moves.size());
        assertFalse(moves.get(0).exercise.name.equals(moves.get(1).exercise.name));
    }
}
