package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
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
import org.maplibre.android.annotations.IconFactory;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
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
    private final Handler replayHandler = new Handler(Looper.getMainLooper());
    private Runnable replayRunnable;
    private Runnable pendingReplay;
    private boolean positioned;
    private boolean vectorStyleReady;
    /** Once a member touches the map, live updates never move their camera again. */
    private boolean userNavigated;
    private boolean routeCameraFramed;
    private boolean vectorCameraFramed;
    private boolean followLatestLocation;

    /** Receives normalized replay time without sending the private route anywhere. */
    public interface RouteReplayListener {
        void onReplayFrame(long elapsedMillis, float fraction);
        void onReplayFinished();
    }

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
        routePaint.setStrokeWidth(context.getResources().getDisplayMetrics().density * 8f);
        routePaint.setStrokeCap(Cap.ROUND);
        route = new Polyline(routePaint, AndroidGraphicFactory.INSTANCE);
        map.getLayerManager().getLayers().add(route);

        // Map touches belong to the map (including a one-finger pan), rather than its ScrollView.
        map.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_POINTER_DOWN) userNavigated = true;
            ViewParent parent = getParent();
            if (parent != null) parent.requestDisallowInterceptTouchEvent(action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL);
            return false;
        });
        addView(map, new FrameLayout.LayoutParams(-1, -1));
        // MapLibre supplies the polished vector surface. INVISIBLE is intentional here: unlike
        // GONE, it is measured and laid out before the renderer starts. A GONE MapView can
        // successfully load its style callback yet never receive a drawable surface, leaving
        // the older Mapsforge fallback visible with no indication that the selected visual map
        // failed to appear.
        MapLibre.getInstance(appContext);
        vectorMap = new org.maplibre.android.maps.MapView(context);
        vectorMap.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_POINTER_DOWN) userNavigated = true;
            ViewParent parent = getParent();
            if (parent != null) parent.requestDisallowInterceptTouchEvent(action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL);
            return false;
        });
        vectorMap.setVisibility(INVISIBLE);
        vectorMap.onCreate(null);
        vectorMap.onStart();
        vectorMap.onResume();
        vectorMap.getMapAsync(loaded -> {
            vectorController = loaded;
            loaded.getUiSettings().setCompassEnabled(true);
            loaded.getUiSettings().setRotateGesturesEnabled(true);
            loaded.getUiSettings().setLogoEnabled(true);
            loaded.getUiSettings().setAttributionEnabled(true);
            loaded.getUiSettings().setScrollGesturesEnabled(true);
            loaded.getUiSettings().setZoomGesturesEnabled(true);
            loaded.getUiSettings().setDoubleTapGesturesEnabled(true);
            loaded.setStyle(MapLibreOfflinePack.styleUrl(appContext), style -> {
                vectorStyleReady = true;
                vectorMap.setVisibility(VISIBLE);
                renderVectorRoute();
                if (pendingReplay != null) {
                    Runnable next = pendingReplay;
                    pendingReplay = null;
                    next.run();
                }
            });
        });
        addView(vectorMap, new FrameLayout.LayoutParams(-1, -1));
        setRoute(points);
    }

    public void setRoute(List<LiveTrackingStore.Point> points) {
        stopRouteReplay();
        currentPoints = points == null ? new ArrayList<>() : new ArrayList<>(points);
        boolean shouldFrame = !userNavigated && currentPoints.size() > 0 && (followLatestLocation || !routeCameraFramed);
        renderRoute(currentPoints, shouldFrame);
    }

    /** Enables live-session camera following; a manual map gesture still opts out. */
    public void setFollowLatestLocation(boolean enabled) {
        followLatestLocation = enabled;
        if (enabled) setRoute(currentPoints);
    }

    /** Plays a compact visual time-lapse from the first recorded point to the finish. */
    public void playRouteReplay(long sessionMillis, RouteReplayListener listener) {
        if (vectorController == null || !vectorStyleReady) {
            pendingReplay = () -> playRouteReplay(sessionMillis, listener);
            return;
        }
        stopRouteReplay();
        if (currentPoints.size() < 2) {
            if (listener != null) { listener.onReplayFrame(Math.max(0L, sessionMillis), 1f); listener.onReplayFinished(); }
            return;
        }
        final List<LiveTrackingStore.Point> fullRoute = new ArrayList<>(currentPoints);
        final long totalMillis = Math.max(1L, sessionMillis);
        final long replayDuration = Math.max(2800L, Math.min(6500L, fullRoute.size() * 34L));
        final long startedAt = android.os.SystemClock.elapsedRealtime();
        replayRunnable = new Runnable() {
            @Override public void run() {
                float fraction = Math.min(1f, (android.os.SystemClock.elapsedRealtime() - startedAt) / (float) replayDuration);
                int end = Math.max(1, Math.min(fullRoute.size(), 1 + Math.round(fraction * (fullRoute.size() - 1))));
                renderRoute(fullRoute.subList(0, end), false);
                if (listener != null) listener.onReplayFrame(Math.round(totalMillis * fraction), fraction);
                if (fraction < 1f) replayHandler.postDelayed(this, 33L);
                else {
                    replayRunnable = null;
                    renderRoute(fullRoute, false);
                    if (listener != null) listener.onReplayFinished();
                }
            }
        };
        replayHandler.post(replayRunnable);
    }

    public void stopRouteReplay() {
        if (replayRunnable != null) replayHandler.removeCallbacks(replayRunnable);
        replayRunnable = null;
        pendingReplay = null;
    }

    private void renderRoute(List<LiveTrackingStore.Point> points, boolean fitCamera) {
        ArrayList<LatLong> routePoints = new ArrayList<>();
        if (points != null) for (LiveTrackingStore.Point point : points) routePoints.add(new LatLong(point.latitude, point.longitude));
        route.setPoints(routePoints);
        if (!positioned || fitCamera) {
            LatLong center = routePoints.isEmpty() ? new LatLong(10.6918d, -61.2225d) : routePoints.get(routePoints.size() - 1);
            map.setCenter(center);
            map.setZoomLevel(routePoints.size() < 2 ? (byte) 18 : (byte) 18);
            positioned = true;
            if (routePoints.size() > 1) routeCameraFramed = true;
        }
        map.repaint();
        renderVectorRoute(points, fitCamera);
    }

    private void renderVectorRoute() {
        renderVectorRoute(currentPoints, !userNavigated && (followLatestLocation || !vectorCameraFramed) && currentPoints.size() > 0);
    }

    private void renderVectorRoute(List<LiveTrackingStore.Point> points, boolean fitCamera) {
        if (vectorController == null) return;
        vectorController.clear();
        ArrayList<LatLng> line = new ArrayList<>();
        for (LiveTrackingStore.Point point : points) line.add(new LatLng(point.latitude, point.longitude));
        if (line.size() > 1) vectorController.addPolyline(new PolylineOptions().addAll(line).color(0xFFADFF2F).width(9f));
        if (!line.isEmpty()) {
            vectorController.addMarker(new MarkerOptions().position(line.get(0)).icon(IconFactory.getInstance(appContext).fromBitmap(markerBitmap(false))).title("Start"));
            if (line.size() > 1) vectorController.addMarker(new MarkerOptions().position(line.get(line.size() - 1))
                    .icon(IconFactory.getInstance(appContext).fromBitmap(markerBitmap(true))).title("Current location · heading up"));
        }
        if (followLatestLocation && !line.isEmpty() && fitCamera) {
            CameraPosition current = vectorController.getCameraPosition();
            CameraPosition camera = new CameraPosition.Builder(current).target(line.get(line.size() - 1)).zoom(Math.max(18d, current.zoom))
                    .bearing(travelBearing(points)).tilt(current.tilt).build();
            vectorController.animateCamera(CameraUpdateFactory.newCameraPosition(camera));
            vectorCameraFramed = true;
        } else if (line.size() > 1 && fitCamera) {
            double north = -90d, south = 90d, east = -180d, west = 180d;
            for (LatLng point : line) { north = Math.max(north, point.getLatitude()); south = Math.min(south, point.getLatitude()); east = Math.max(east, point.getLongitude()); west = Math.min(west, point.getLongitude()); }
            vectorController.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.from(north, east, south, west), dp(28)));
            vectorCameraFramed = true;
        } else if (line.size() == 1 && fitCamera) {
            vectorController.moveCamera(CameraUpdateFactory.newLatLngZoom(line.get(0), 18d));
        } else if (line.isEmpty() && !vectorCameraFramed) {
            vectorController.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(10.6918d, -61.2225d), 12d));
        }
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }

    /** Uses the last movement vector to keep the navigation arrow at screen-top. */
    private float travelBearing(List<LiveTrackingStore.Point> points) {
        if (points == null || points.size() < 2) return vectorController == null ? 0f : (float) vectorController.getCameraPosition().bearing;
        LiveTrackingStore.Point from = points.get(points.size() - 2), to = points.get(points.size() - 1);
        double longitude = Math.toRadians(to.longitude - from.longitude);
        double firstLatitude = Math.toRadians(from.latitude), secondLatitude = Math.toRadians(to.latitude);
        double y = Math.sin(longitude) * Math.cos(secondLatitude);
        double x = Math.cos(firstLatitude) * Math.sin(secondLatitude)
                - Math.sin(firstLatitude) * Math.cos(secondLatitude) * Math.cos(longitude);
        return (float) ((Math.toDegrees(Math.atan2(y, x)) + 360d) % 360d);
    }

    /** North-pointing custom markers give the live map the same language as Google Maps. */
    private Bitmap markerBitmap(boolean arrow) {
        int size = dp(38);
        Bitmap image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        if (!arrow) {
            paint.setColor(Color.WHITE); canvas.drawCircle(size / 2f, size / 2f, dp(8), paint);
            paint.setColor(0xff38d9bd); canvas.drawCircle(size / 2f, size / 2f, dp(5), paint);
            return image;
        }
        Path head = new Path();
        head.moveTo(size / 2f, dp(2)); head.lineTo(size - dp(4), size - dp(4));
        head.lineTo(size / 2f, size - dp(9)); head.lineTo(dp(4), size - dp(4)); head.close();
        paint.setColor(Color.WHITE); paint.setStyle(android.graphics.Paint.Style.STROKE); paint.setStrokeWidth(dp(3)); canvas.drawPath(head, paint);
        paint.setColor(Color.rgb(255, 107, 95)); paint.setStyle(android.graphics.Paint.Style.FILL); canvas.drawPath(head, paint);
        return image;
    }

    @Override protected void onDetachedFromWindow() {
        stopRouteReplay();
        vectorMap.onPause();
        vectorMap.onStop();
        vectorMap.onDestroy();
        map.destroyAll();
        tileCache.destroy();
        dataStore.close();
        super.onDetachedFromWindow();
    }
}
