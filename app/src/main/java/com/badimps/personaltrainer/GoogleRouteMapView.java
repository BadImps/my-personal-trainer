package com.badimps.personaltrainer;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Maps view used for a live endurance session. It self-falls back to the app's private
 * route trace if map tiles cannot load; MapLibre remains the no-key/offline path in MainActivity.
 */
public final class GoogleRouteMapView extends FrameLayout implements OnMapReadyCallback {
    private MapView mapView;
    private RouteTraceView privateTrace;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean mapLoaded;
    private final Runnable showFallback = () -> {
        if (!mapLoaded) {
            mapView.setVisibility(GONE);
            privateTrace.setVisibility(VISIBLE);
        }
    };
    private GoogleMap map;
    private List<LiveTrackingStore.Point> points = new ArrayList<>();
    private int renderedPointCount;

    public GoogleRouteMapView(Context context, List<LiveTrackingStore.Point> initialPoints) {
        super(context);
        privateTrace = new RouteTraceView(context);
        privateTrace.setVisibility(GONE);
        addView(privateTrace, new FrameLayout.LayoutParams(-1, -1));
        mapView = new MapView(context);
        addView(mapView, new FrameLayout.LayoutParams(-1, -1));
        mapView.onCreate((Bundle) null);
        mapView.getMapAsync(this);
        setRoute(initialPoints);
        handler.postDelayed(showFallback, 8_000L);
    }

    @Override public void onMapReady(GoogleMap readyMap) {
        map = readyMap;
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setOnMapLoadedCallback(() -> {
            mapLoaded = true;
            handler.removeCallbacks(showFallback);
            mapView.setVisibility(VISIBLE);
            privateTrace.setVisibility(GONE);
        });
        render();
    }

    public void setRoute(List<LiveTrackingStore.Point> route) {
        points = route == null ? new ArrayList<>() : new ArrayList<>(route);
        privateTrace.setPoints(points);
        render();
    }

    private void render() {
        if (map == null) return;
        map.clear();
        if (points.isEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(10.6918, -61.2225), 11f));
            return;
        }
        PolylineOptions line = new PolylineOptions().color(0xff38d9bd).width(dp(5));
        for (LiveTrackingStore.Point point : points) {
            LatLng latLng = new LatLng(point.latitude, point.longitude);
            line.add(latLng);
        }
        map.addPolyline(line);
        LatLng first = new LatLng(points.get(0).latitude, points.get(0).longitude);
        LatLng last = new LatLng(points.get(points.size() - 1).latitude, points.get(points.size() - 1).longitude);
        map.addMarker(new MarkerOptions().position(first).title("Start"));
        if (points.size() > 1) map.addMarker(new MarkerOptions().position(last).title("Current position"));
        // Live tracking follows the latest recorded fix. The route remains visible behind the
        // marker, while the camera stays useful for a person moving through the real world.
        float zoom = map.getCameraPosition() == null ? 17f : Math.max(16f, map.getCameraPosition().zoom);
        if (renderedPointCount == 0) map.moveCamera(CameraUpdateFactory.newLatLngZoom(last, zoom));
        else map.animateCamera(CameraUpdateFactory.newLatLngZoom(last, zoom));
        renderedPointCount = points.size();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mapView.onResume();
    }

    @Override protected void onDetachedFromWindow() {
        handler.removeCallbacks(showFallback);
        mapView.onPause();
        mapView.onDestroy();
        super.onDetachedFromWindow();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
