package com.badimps.personaltrainer;

import android.content.Context;
import android.widget.FrameLayout;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** A real, touchable vector map backed only by the locally stored country map file. */
public final class OfflineRouteMapView extends FrameLayout {
    private final MapView mapView;
    private final Polyline route;
    private final byte mapZoomMinimum;
    private final byte mapZoomMaximum;
    private boolean positioned;

    public OfflineRouteMapView(Context context, File map, List<LiveTrackingStore.Point> points) {
        super(context);
        if (AndroidGraphicFactory.INSTANCE == null) AndroidGraphicFactory.createInstance(context.getApplicationContext());
        mapView = new MapView(context);
        mapView.setBuiltInZoomControls(false);
        mapView.getMapScaleBar().setVisible(true);
        mapView.getModel().displayModel.setBackgroundColor(0xff0b1930);
        // Keep the small route preview cache in app memory. A file-backed cache can be denied or
        // return stale blank tiles on individual Android devices, even when the country file is valid.
        TileCache cache = new InMemoryTileCache(64);
        MapFile data = new MapFile(map);
        mapZoomMinimum = data.getMapFileInfo().zoomLevelMin;
        mapZoomMaximum = data.getMapFileInfo().zoomLevelMax;
        mapView.setZoomLevelMin(mapZoomMinimum);
        mapView.setZoomLevelMax(mapZoomMaximum);
        mapView.getLayerManager().getLayers().add(AndroidUtil.createTileRendererLayer(cache,
                mapView.getModel().mapViewPosition, data, MapsforgeThemes.DEFAULT));
        Paint routePaint = AndroidGraphicFactory.INSTANCE.createPaint();
        routePaint.setColor(0xffff665c);
        routePaint.setStyle(Style.STROKE);
        routePaint.setStrokeWidth(7f);
        route = new Polyline(routePaint, AndroidGraphicFactory.INSTANCE);
        mapView.getLayerManager().getLayers().add(route);
        addView(mapView, new FrameLayout.LayoutParams(-1, -1));
        setRoute(points);
    }

    public void setRoute(List<LiveTrackingStore.Point> points) {
        ArrayList<LatLong> routePoints = new ArrayList<>();
        if (points != null) for (LiveTrackingStore.Point point : points) routePoints.add(new LatLong(point.latitude, point.longitude));
        route.setPoints(routePoints);
        if (!positioned) {
            LatLong center = center(routePoints);
            LatLong initialCenter = center == null ? new LatLong(10.6918d, -61.2225d) : center;
            byte requested = zoomFor(routePoints);
            byte initialZoom = (byte) Math.max(mapZoomMinimum, Math.min(mapZoomMaximum, requested));
            // Set the underlying position now and again after the Android view has a measured size.
            // Some devices otherwise leave a newly embedded MapView at its empty default position.
            mapView.getModel().mapViewPosition.setCenter(initialCenter);
            mapView.getModel().mapViewPosition.setZoomLevel(initialZoom);
            mapView.post(() -> {
                mapView.getModel().mapViewPosition.setCenter(initialCenter);
                mapView.getModel().mapViewPosition.setZoomLevel(initialZoom);
                mapView.getLayerManager().redrawLayers();
            });
            positioned = true;
        }
        mapView.getLayerManager().redrawLayers();
        mapView.repaint();
    }

    @Override protected void onDetachedFromWindow() {
        mapView.destroyAll();
        super.onDetachedFromWindow();
    }

    private static LatLong center(List<LatLong> points) {
        if (points.isEmpty()) return null;
        double minLat = points.get(0).latitude, maxLat = minLat, minLon = points.get(0).longitude, maxLon = minLon;
        for (LatLong point : points) { minLat = Math.min(minLat, point.latitude); maxLat = Math.max(maxLat, point.latitude); minLon = Math.min(minLon, point.longitude); maxLon = Math.max(maxLon, point.longitude); }
        return new LatLong((minLat + maxLat) / 2d, (minLon + maxLon) / 2d);
    }

    private static byte zoomFor(List<LatLong> points) {
        if (points.size() < 2) return 13;
        double minLat = points.get(0).latitude, maxLat = minLat, minLon = points.get(0).longitude, maxLon = minLon;
        for (LatLong point : points) { minLat = Math.min(minLat, point.latitude); maxLat = Math.max(maxLat, point.latitude); minLon = Math.min(minLon, point.longitude); maxLon = Math.max(maxLon, point.longitude); }
        double span = Math.max(maxLat - minLat, maxLon - minLon);
        if (span < .003d) return 16;
        if (span < .012d) return 14;
        if (span < .05d) return 12;
        return 10;
    }
}
