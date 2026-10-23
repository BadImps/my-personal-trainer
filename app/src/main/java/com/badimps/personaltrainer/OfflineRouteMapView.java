package com.badimps.personaltrainer;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.widget.FrameLayout;

import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;
import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.PolylineOptions;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the selected, app-private Mapsforge country file directly. This is
 * deliberately not an online tile surface: saved country maps still draw with
 * mobile data and Wi-Fi disabled, while the route stays on the device.
 */
public final class OfflineRouteMapView extends FrameLayout {
    private final Context appContext;
    private final MapView map;
    private final Polyline route;
    private final TileCache tileCache;
    private final MapFile dataStore;
    private final org.maplibre.android.maps.MapView vectorMap;
    private org.maplibre.android.maps.MapLibreMap vectorController;
    private List<LiveTrackingStore.Point> currentPoints = new ArrayList<>();
    private boolean positioned;

    public OfflineRouteMapView(Context context, File mapFile, List<LiveTrackingStore.Point> points) {
        super(context);
        appContext = context.getApplicationContext();
        if (mapFile == null || !mapFile.isFile() || mapFile.length() == 0L) {
            throw new IllegalArgumentException("The selected offline map is unavailable.");
        }
        // Covers members who downloaded the original country map before MapLibre was added.
        MapLibreOfflinePack.ensureTrinidadAndTobago(appContext);
        AndroidGraphicFactory.createInstance(context.getApplicationContext());
        map = new MapView(context);
        map.setClickable(true);
        map.setBuiltInZoomControls(false);
        map.getMapScaleBar().setVisible(false);
        map.setZoomLevelMin((byte) 7);
        map.setZoomLevelMax((byte) 20);

        int tileSize = map.getModel().displayModel.getTileSize();
        tileCache = AndroidUtil.createTileCache(context, "offline-route-cache", tileSize, 1f,
                map.getModel().frameBufferModel.getOverdrawFactor());
        dataStore = new MapFile(mapFile);
        TileRendererLayer countryLayer = new TileRendererLayer(tileCache, dataStore,
                map.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
        countryLayer.setXmlRenderTheme(MapsforgeThemes.DEFAULT);
        map.getLayerManager().getLayers().add(countryLayer);

        Paint routePaint = AndroidGraphicFactory.INSTANCE.createPaint();
        routePaint.setStyle(Style.STROKE);
        routePaint.setColor(0xFFADFF2F);
        routePaint.setStrokeWidth(context.getResources().getDisplayMetrics().density * 5f);
        routePaint.setStrokeCap(Cap.ROUND);
        route = new Polyline(routePaint, AndroidGraphicFactory.INSTANCE);
        map.getLayerManager().getLayers().add(route);

        // A two-finger map gesture belongs to the map, rather than the surrounding scroll page.
        map.setOnTouchListener((view, event) -> {
            boolean mapGesture = event.getPointerCount() >= 2 || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP;
            ViewParent parent = getParent();
            if (parent != null) parent.requestDisallowInterceptTouchEvent(mapGesture);
            return false;
        });
        addView(map, new FrameLayout.LayoutParams(-1, -1));
        // MapLibre supplies the polished vector surface. Keep it invisible until its style has
        // actually loaded so that an offline or interrupted download never becomes a blank map.
        MapLibre.getInstance(appContext);
        vectorMap = new org.maplibre.android.maps.MapView(context);
        vectorMap.setVisibility(GONE);
        vectorMap.onCreate(null);
        vectorMap.onStart();
        vectorMap.onResume();
        vectorMap.getMapAsync(loaded -> {
            vectorController = loaded;
            loaded.getUiSettings().setCompassEnabled(true);
            loaded.getUiSettings().setLogoEnabled(true);
            loaded.getUiSettings().setAttributionEnabled(true);
            loaded.setStyle(MapLibreOfflinePack.styleUrl(appContext), style -> {
                vectorMap.setVisibility(VISIBLE);
                renderVectorRoute();
            });
        });
        addView(vectorMap, new FrameLayout.LayoutParams(-1, -1));
        setRoute(points);
    }

    public void setRoute(List<LiveTrackingStore.Point> points) {
        currentPoints = points == null ? new ArrayList<>() : new ArrayList<>(points);
        ArrayList<LatLong> routePoints = new ArrayList<>();
        if (points != null) for (LiveTrackingStore.Point point : points) routePoints.add(new LatLong(point.latitude, point.longitude));
        route.setPoints(routePoints);
        if (!positioned || !routePoints.isEmpty()) {
            LatLong center = routePoints.isEmpty() ? new LatLong(10.6918d, -61.2225d) : routePoints.get(routePoints.size() - 1);
            map.setCenter(center);
            map.setZoomLevel(routePoints.size() < 2 ? (byte) 13 : (byte) 15);
            positioned = true;
        }
        map.repaint();
        renderVectorRoute();
    }

    private void renderVectorRoute() {
        if (vectorController == null) return;
        vectorController.clear();
        ArrayList<LatLng> line = new ArrayList<>();
        for (LiveTrackingStore.Point point : currentPoints) line.add(new LatLng(point.latitude, point.longitude));
        if (line.size() > 1) vectorController.addPolyline(new PolylineOptions().addAll(line).color(0xFFADFF2F).width(6f));
        if (line.size() > 1) {
            double north = -90d, south = 90d, east = -180d, west = 180d;
            for (LatLng point : line) { north = Math.max(north, point.getLatitude()); south = Math.min(south, point.getLatitude()); east = Math.max(east, point.getLongitude()); west = Math.min(west, point.getLongitude()); }
            vectorController.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.from(north, east, south, west), 72));
        } else {
            vectorController.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(10.6918d, -61.2225d), 12d));
        }
    }

    @Override protected void onDetachedFromWindow() {
        vectorMap.onPause();
        vectorMap.onStop();
        vectorMap.onDestroy();
        map.destroyAll();
        tileCache.destroy();
        dataStore.close();
        super.onDetachedFromWindow();
    }
}
