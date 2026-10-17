package com.badimps.personaltrainer;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CaribbeanFoodLibraryTest {
    @Test public void providesABroadOfflineReference() {
        assertTrue(CaribbeanFoodLibrary.count() >= 80);
        assertTrue(CaribbeanFoodLibrary.search("", "All").size() >= 80);
    }

    @Test public void findsCommonFoodsAndRespectsGroups() {
        List<CaribbeanFoodLibrary.Food> breadfruit = CaribbeanFoodLibrary.search("breadfruit", "All");
        assertFalse(breadfruit.isEmpty());
        assertTrue(CaribbeanFoodLibrary.search("", "Fish & seafood").size() >= 8);
        assertTrue(CaribbeanFoodLibrary.search("", "Fruit").size() >= 15);
    }
}
