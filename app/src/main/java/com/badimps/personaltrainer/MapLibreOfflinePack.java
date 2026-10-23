package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import org.maplibre.android.MapLibre;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.offline.OfflineManager;
import org.maplibre.android.offline.OfflineRegion;
import org.maplibre.android.offline.OfflineRegionDefinition;
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition;

import java.nio.charset.StandardCharsets;

/**
 * Owns the polished MapLibre vector pack used by Trinidad & Tobago routes.
 * OpenFreeMap explicitly permits bounded app offline downloads (3,000 tiles / 50 MB),
 * so this region intentionally stays below that limit and remains app-private.
 */
public final class MapLibreOfflinePack {
    private static final String PREFS = "maplibre_offline";
    private static final String KEY_MODE = "tt_pack_mode";

    /** Both options are optional on-device content, never APK/OTA payload. */
    public enum Mode {
        LOW_IMPACT("low", "Low Impact", "https://tiles.openfreemap.org/styles/positron", 13d),
        ENHANCED("enhanced", "Enhanced", "https://tiles.openfreemap.org/styles/bright", 14d);

        final String id, label, styleUrl;
        final double maxZoom;
        Mode(String id, String label, String styleUrl, double maxZoom) {
            this.id = id; this.label = label; this.styleUrl = styleUrl; this.maxZoom = maxZoom;
        }
    }

    public static boolean isTrinidadReady(Context context) {
        return isReady(context, selectedMode(context));
    }
    public static boolean isReady(Context context, Mode mode) { return prefs(context).getBoolean("tt_" + mode.id + "_ready", false); }
    public static Mode selectedMode(Context context) {
        return "enhanced".equals(prefs(context).getString(KEY_MODE, Mode.LOW_IMPACT.id)) ? Mode.ENHANCED : Mode.LOW_IMPACT;
    }
    public static void selectMode(Context context, Mode mode) { prefs(context).edit().putString(KEY_MODE, mode.id).apply(); }
    public static String styleUrl(Context context) { return selectedMode(context).styleUrl; }

    /** Starts a resumable vector download once. Existing Mapsforge data remains the safe fallback. */
    public static void ensureTrinidadAndTobago(Context context) {
        ensureTrinidadAndTobago(context, selectedMode(context));
    }

    /** Starts the selected bounded vector download once. Existing local streets stay the safe fallback. */
    public static void ensureTrinidadAndTobago(Context context, Mode mode) {
        Context app = context.getApplicationContext();
        String requestedKey = "tt_" + mode.id + "_requested";
        String readyKey = "tt_" + mode.id + "_ready";
        if (prefs(app).getBoolean(requestedKey, false)) return;
        prefs(app).edit().putBoolean(requestedKey, true).apply();
        MapLibre.getInstance(app);
        OfflineManager manager = OfflineManager.getInstance(app);
        OfflineRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                mode.styleUrl,
                // Northern Trinidad through Tobago, with modest padding for route starts and finishes.
                LatLngBounds.from(11.42d, -60.25d, 9.98d, -61.98d),
                7d, mode.maxZoom,
                app.getResources().getDisplayMetrics().density
        );
        String metadata = "my-personal-trainer:trinidad-tobago:" + mode.id + ":v1";
        manager.createOfflineRegion(definition, metadata.getBytes(StandardCharsets.UTF_8),
                new OfflineManager.CreateOfflineRegionCallback() {
                    @Override public void onCreate(OfflineRegion region) {
                        region.setObserver(new OfflineRegion.OfflineRegionObserver() {
                            @Override public void onStatusChanged(org.maplibre.android.offline.OfflineRegionStatus status) {
                                if (status.isComplete()) prefs(app).edit().putBoolean(readyKey, true).apply();
                            }
                            @Override public void onError(org.maplibre.android.offline.OfflineRegionError error) {
                                // Keep the local Mapsforge fallback available; a later app launch can retry.
                                prefs(app).edit().putBoolean(requestedKey, false).apply();
                            }
                            @Override public void mapboxTileCountLimitExceeded(long limit) {
                                prefs(app).edit().putBoolean(requestedKey, false).apply();
                            }
                        });
                        region.setDownloadState(OfflineRegion.STATE_ACTIVE);
                    }
                    @Override public void onError(String error) {
                        prefs(app).edit().putBoolean(requestedKey, false).apply();
                    }
                });
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    private MapLibreOfflinePack() { }
}
