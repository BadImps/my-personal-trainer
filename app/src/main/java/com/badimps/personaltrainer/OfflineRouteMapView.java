package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Color;
import android.widget.FrameLayout;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** OpenStreetMap-style live map with normal local tile caching and a private route overlay. */
public final class OfflineRouteMapView extends FrameLayout {
    private final MapView map;
    private final Polyline route;
    private boolean positioned;

    /** mapFile is retained in the API so saved offline-country selections remain compatible. */
    public OfflineRouteMapView(Context context, File mapFile, List<LiveTrackingStore.Point> points) {
        super(context);
        File cache = new File(context.getCacheDir(), "osm-live-cache");
        Configuration.getInstance().setUserAgentValue(context.getPackageName() + "/MyPersonalTrainer");
        Configuration.getInstance().setOsmdroidBasePath(cache);
        Configuration.getInstance().setOsmdroidTileCache(cache);
        map = new MapView(context);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);
        route = new Polyline();
        route.getOutlinePaint().setColor(Color.rgb(173, 255, 47));
        route.getOutlinePaint().setStrokeWidth(context.getResources().getDisplayMetrics().density * 5f);
        map.getOverlays().add(route);
        addView(map, new FrameLayout.LayoutParams(-1, -1));
        setRoute(points);
    }

    public void setRoute(List<LiveTrackingStore.Point> points) {
        ArrayList<GeoPoint> routePoints = new ArrayList<>();
        if (points != null) for (LiveTrackingStore.Point point : points) routePoints.add(new GeoPoint(point.latitude, point.longitude));
        route.setPoints(routePoints);
        if (!positioned || !routePoints.isEmpty()) {
            GeoPoint center = routePoints.isEmpty() ? new GeoPoint(10.6918d, -61.2225d) : routePoints.get(routePoints.size() - 1);
            map.getController().setCenter(center);
            map.getController().setZoom(routePoints.size() < 2 ? 13d : 15d);
            positioned = true;
        }
        map.invalidate();
    }

    @Override protected void onDetachedFromWindow() { map.onDetach(); super.onDetachedFromWindow(); }
}
