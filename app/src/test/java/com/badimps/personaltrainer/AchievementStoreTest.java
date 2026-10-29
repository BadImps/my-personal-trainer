package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Regression coverage for published Apex requirements and the level-rank ladder. */
public final class AchievementStoreTest {
    @Test public void apexTierDoesNotUnlockFromLifetimeSessionsOutsideItsWindow() {
        AchievementStore.ApexTier tier = new AchievementStore.ApexTier(
                "Momentum", "Recent rhythm", 2, 3, 14, 2);
        assertFalse(tier.unlocked(9));
    }

    @Test public void apexTierUnlocksOnlyWhenLevelAndRecentWindowRequirementBothPass() {
        AchievementStore.ApexTier tier = new AchievementStore.ApexTier(
                "Momentum", "Recent rhythm", 2, 3, 14, 3);
        assertFalse(tier.unlocked(1));
        assertTrue(tier.unlocked(2));
    }

    @Test public void rankLadderNamesEveryPublishedLevelRange() {
        assertEquals("BEGINNER", AchievementStore.rankForLevel(1).title);
        assertEquals("BEGINNER", AchievementStore.rankForLevel(10).title);
        assertEquals("NOVICE", AchievementStore.rankForLevel(11).title);
        assertEquals("NOVICE", AchievementStore.rankForLevel(25).title);
        assertEquals("CONTENDER", AchievementStore.rankForLevel(26).title);
        assertEquals("COMPETITOR", AchievementStore.rankForLevel(51).title);
        assertEquals("ATHLETE", AchievementStore.rankForLevel(76).title);
        assertEquals("ELITE", AchievementStore.rankForLevel(101).title);
        assertEquals("CHAMPION", AchievementStore.rankForLevel(151).title);
        assertEquals("APEX", AchievementStore.rankForLevel(201).title);
    }
}
