package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLngBounds;
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
    private Runnable replayRunnable;
    /** Replay renders at ~30fps, so it must not queue the normal camera animations. */
    private boolean replaying;
    /** Report replay is an overview, unlike the close follow-camera used during live tracking. */
    private boolean replayOverviewMode;
    private boolean overviewFramed;
    private List<LiveTrackingStore.Point> overviewPoints = new ArrayList<>();

    /** Report pages receive progress without owning Google Maps' rendering lifecycle. */
    public interface RouteReplayListener {
        void onReplayFrame(long elapsedMillis, float fraction);
        void onReplayFinished();
    }

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
        map.getUiSettings().setRotateGesturesEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
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

    /** Keeps a report replay framed from start to finish rather than following every GPS point. */
    public void setReplayOverviewMode(boolean enabled) {
        replayOverviewMode = enabled;
        overviewPoints = enabled ? new ArrayList<>(points) : new ArrayList<>();
        overviewFramed = false;
        render();
    }

    /** Replays a saved route on Google Maps using the same heading-up camera as live tracking. */
    public void playRouteReplay(long sessionMillis, RouteReplayListener listener) {
        stopRouteReplay();
        if (points.size() < 2) {
            if (listener != null) { listener.onReplayFrame(Math.max(0L, sessionMillis), 1f); listener.onReplayFinished(); }
            return;
        }
        final List<LiveTrackingStore.Point> fullRoute = new ArrayList<>(points);
        final long totalMillis = Math.max(1L, sessionMillis);
        final long replayDuration = Math.max(4200L, Math.min(9000L, fullRoute.size() * 45L));
        final long startedAt = android.os.SystemClock.elapsedRealtime();
        replaying = true;
        renderedPointCount = 0;
        replayRunnable = new Runnable() {
            @Override public void run() {
                float fraction = Math.min(1f, (android.os.SystemClock.elapsedRealtime() - startedAt) / (float) replayDuration);
                float routeIndex = fraction * (fullRoute.size() - 1);
                int complete = Math.max(0, Math.min(fullRoute.size() - 1, (int) Math.floor(routeIndex)));
                ArrayList<LiveTrackingStore.Point> frame = new ArrayList<>(fullRoute.subList(0, complete + 1));
                if (complete + 1 < fullRoute.size()) {
                    LiveTrackingStore.Point from = fullRoute.get(complete), to = fullRoute.get(complete + 1);
                    float between = routeIndex - complete;
                    frame.add(new LiveTrackingStore.Point(
                            from.latitude + (to.latitude - from.latitude) * between,
                            from.longitude + (to.longitude - from.longitude) * between));
                }
                setRoute(frame);
                if (listener != null) listener.onReplayFrame(Math.round(totalMillis * fraction), fraction);
                if (fraction < 1f) handler.postDelayed(this, 33L);
                else {
                    replayRunnable = null;
                    replaying = false;
                    setRoute(fullRoute);
                    if (listener != null) listener.onReplayFinished();
                }
            }
        };
        handler.post(replayRunnable);
    }

    public void stopRouteReplay() {
        if (replayRunnable != null) handler.removeCallbacks(replayRunnable);
        replayRunnable = null;
        replaying = false;
    }

    private void render() {
        if (map == null) return;
        map.clear();
        if (points.isEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(10.6918, -61.2225), 11f));
            return;
        }
        // High-contrast, larger route language keeps the live view legible at a glance.
        PolylineOptions line = new PolylineOptions().color(0xff38d9bd).width(dp(8));
        for (LiveTrackingStore.Point point : points) {
            LatLng latLng = new LatLng(point.latitude, point.longitude);
            line.add(latLng);
        }
        map.addPolyline(line);
        LatLng first = new LatLng(points.get(0).latitude, points.get(0).longitude);
        LatLng last = new LatLng(points.get(points.size() - 1).latitude, points.get(points.size() - 1).longitude);
        // The start stays a small dot. The live marker is a north-pointing arrow, independent
        // of route direction, so it remains a reliable orientation reference while the map rotates.
        map.addMarker(new MarkerOptions().position(first).icon(startDot()).anchor(.5f, .5f).title("Start"));
        float travelBearing = travelBearing();
        if (points.size() > 1) map.addMarker(new MarkerOptions().position(last).icon(northArrow())
                .anchor(.5f, .5f).flat(true).rotation(travelBearing).title("Current location · heading up"));
        if (replayOverviewMode) {
            frameOverviewIfNeeded();
            renderedPointCount = points.size();
            return;
        }
        // Live tracking follows the latest recorded fix. The route remains visible behind the
        // marker, while the camera stays useful for a person moving through the real world.
        float zoom = map.getCameraPosition() == null ? 18.25f : Math.max(18f, map.getCameraPosition().zoom);
        CameraPosition current = map.getCameraPosition();
        CameraPosition camera = new CameraPosition.Builder(current).target(last).zoom(zoom)
                .bearing(travelBearing).tilt(current == null ? 0f : current.tilt).build();
        if (renderedPointCount == 0 || replaying) {
            // A replay changes on every frame. Cancelling then moving prevents a backlog of
            // camera animations, which otherwise makes the map trail far behind the marker.
            map.stopAnimation();
            map.moveCamera(CameraUpdateFactory.newCameraPosition(camera));
        } else map.animateCamera(CameraUpdateFactory.newCameraPosition(camera));
        renderedPointCount = points.size();
    }

    private void frameOverviewIfNeeded() {
        if (overviewFramed || overviewPoints.isEmpty() || map == null) return;
        if (overviewPoints.size() == 1) {
            LiveTrackingStore.Point point = overviewPoints.get(0);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(point.latitude, point.longitude), 17f));
        } else {
            LatLngBounds.Builder bounds = new LatLngBounds.Builder();
            for (LiveTrackingStore.Point point : overviewPoints) bounds.include(new LatLng(point.latitude, point.longitude));
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), dp(38)));
        }
        overviewFramed = true;
    }

    /** Rotates the map underneath the fixed screen-up arrow using the last two GPS fixes. */
    private float travelBearing() {
        if (points.size() < 2) return map == null || map.getCameraPosition() == null ? 0f : map.getCameraPosition().bearing;
        LiveTrackingStore.Point from = points.get(points.size() - 2);
        LiveTrackingStore.Point to = points.get(points.size() - 1);
        double longitude = Math.toRadians(to.longitude - from.longitude);
        double firstLatitude = Math.toRadians(from.latitude), secondLatitude = Math.toRadians(to.latitude);
        double y = Math.sin(longitude) * Math.cos(secondLatitude);
        double x = Math.cos(firstLatitude) * Math.sin(secondLatitude)
                - Math.sin(firstLatitude) * Math.cos(secondLatitude) * Math.cos(longitude);
        return (float) ((Math.toDegrees(Math.atan2(y, x)) + 360d) % 360d);
    }

    private BitmapDescriptor startDot() { return BitmapDescriptorFactory.fromBitmap(markerBitmap(false)); }
    private BitmapDescriptor northArrow() { return BitmapDescriptorFactory.fromBitmap(markerBitmap(true)); }

    private Bitmap markerBitmap(boolean arrow) {
        int size = dp(38);
        Bitmap image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (!arrow) {
            paint.setColor(Color.WHITE); canvas.drawCircle(size / 2f, size / 2f, dp(8), paint);
            paint.setColor(0xff38d9bd); canvas.drawCircle(size / 2f, size / 2f, dp(5), paint);
            return image;
        }
        Path head = new Path();
        head.moveTo(size / 2f, dp(2)); head.lineTo(size - dp(4), size - dp(4));
        head.lineTo(size / 2f, size - dp(9)); head.lineTo(dp(4), size - dp(4)); head.close();
        paint.setColor(Color.WHITE); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(3)); canvas.drawPath(head, paint);
        paint.setColor(0xffff6b5f); paint.setStyle(Paint.Style.FILL); canvas.drawPath(head, paint);
        return image;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mapView.onResume();
    }

    @Override protected void onDetachedFromWindow() {
        handler.removeCallbacks(showFallback);
        stopRouteReplay();
        mapView.onPause();
        mapView.onDestroy();
        super.onDetachedFromWindow();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
