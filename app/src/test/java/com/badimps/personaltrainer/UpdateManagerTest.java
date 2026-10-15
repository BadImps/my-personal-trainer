package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Regression coverage for the in-app OTA progress indicator. */
public final class UpdateManagerTest {
    @Test public void unknownDownloadSizeUsesIndeterminateProgress() {
        assertEquals(-1, UpdateManager.progressPercent(1024, -1));
    }

    @Test public void downloadProgressIsCalculatedAndClamped() {
        assertEquals(0, UpdateManager.progressPercent(0, 1000));
        assertEquals(42, UpdateManager.progressPercent(420, 1000));
        assertEquals(100, UpdateManager.progressPercent(1200, 1000));
    }
}
