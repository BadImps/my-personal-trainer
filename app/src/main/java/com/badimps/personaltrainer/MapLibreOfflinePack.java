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
    public static final String STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";
    private static final String PREFS = "maplibre_offline";
    private static final String KEY_REQUESTED = "tt_pack_requested";
    private static final String KEY_READY = "tt_pack_ready";
    private static final String TT_METADATA = "my-personal-trainer:trinidad-tobago:v1";

    public static boolean isTrinidadReady(Context context) {
        return prefs(context).getBoolean(KEY_READY, false);
    }

    /** Starts a resumable vector download once. Existing Mapsforge data remains the safe fallback. */
    public static void ensureTrinidadAndTobago(Context context) {
        Context app = context.getApplicationContext();
        if (prefs(app).getBoolean(KEY_REQUESTED, false)) return;
        prefs(app).edit().putBoolean(KEY_REQUESTED, true).apply();
        MapLibre.getInstance(app);
        OfflineManager manager = OfflineManager.getInstance(app);
        OfflineRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                STYLE_URL,
                // Northern Trinidad through Tobago, with modest padding for route starts and finishes.
                LatLngBounds.from(11.42d, -60.25d, 9.98d, -61.98d),
                7d, 14d,
                app.getResources().getDisplayMetrics().density
        );
        manager.createOfflineRegion(definition, TT_METADATA.getBytes(StandardCharsets.UTF_8),
                new OfflineManager.CreateOfflineRegionCallback() {
                    @Override public void onCreate(OfflineRegion region) {
                        region.setObserver(new OfflineRegion.OfflineRegionObserver() {
                            @Override public void onStatusChanged(org.maplibre.android.offline.OfflineRegionStatus status) {
                                if (status.isComplete()) prefs(app).edit().putBoolean(KEY_READY, true).apply();
                            }
                            @Override public void onError(org.maplibre.android.offline.OfflineRegionError error) {
                                // Keep the local Mapsforge fallback available; a later app launch can retry.
                                prefs(app).edit().putBoolean(KEY_REQUESTED, false).apply();
                            }
                            @Override public void mapboxTileCountLimitExceeded(long limit) {
                                prefs(app).edit().putBoolean(KEY_REQUESTED, false).apply();
                            }
                        });
                        region.setDownloadState(OfflineRegion.STATE_ACTIVE);
                    }
                    @Override public void onError(String error) {
                        prefs(app).edit().putBoolean(KEY_REQUESTED, false).apply();
                    }
                });
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    private MapLibreOfflinePack() { }
}
