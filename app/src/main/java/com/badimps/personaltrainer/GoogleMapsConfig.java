package com.badimps.personaltrainer;

import android.content.Context;

import com.google.android.libraries.places.api.Places;

/** Keeps Google Maps opt-in. MapLibre remains available whenever this client is not configured. */
public final class GoogleMapsConfig {
    private static volatile boolean initialized;

    public static boolean isConfigured() {
        String key = BuildConfig.GOOGLE_MAPS_API_KEY;
        return key != null && key.trim().length() > 20 && !"DEFAULT_API_KEY".equals(key);
    }

    public static boolean initializePlaces(Context context) {
        if (!isConfigured()) return false;
        if (!initialized) {
            synchronized (GoogleMapsConfig.class) {
                if (!initialized) {
                    try {
                        if (!Places.isInitialized()) {
                            Places.initializeWithNewPlacesApiEnabled(context.getApplicationContext(),
                                    BuildConfig.GOOGLE_MAPS_API_KEY);
                        }
                        initialized = true;
                    } catch (RuntimeException ignored) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private GoogleMapsConfig() { }
}
