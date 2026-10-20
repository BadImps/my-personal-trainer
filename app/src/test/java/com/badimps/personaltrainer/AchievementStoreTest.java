package com.badimps.personaltrainer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Regression coverage for the published Apex requirement text and its real unlock behavior. */
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
}
