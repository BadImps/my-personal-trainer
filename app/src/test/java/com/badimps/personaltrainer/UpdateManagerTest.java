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

    @Test public void storagePreflightLeavesDownloadHeadroom() {
        assertEquals(1_150L, UpdateManager.requiredBytes(1_000L));
        assertEquals(34_500_000L, UpdateManager.requiredBytes(30_000_000L));
    }

    @Test public void readableByteLabelsRemainUsefulForMobileDataConsent() {
        assertEquals("512.0 KB", UpdateManager.readableBytes(512L * 1024L));
        assertEquals("1.0 MB", UpdateManager.readableBytes(1_048_576L));
    }
}
