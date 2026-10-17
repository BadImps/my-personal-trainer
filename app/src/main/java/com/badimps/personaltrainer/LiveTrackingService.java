package com.badimps.personaltrainer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

/** Foreground-only GPS capture for an explicit, user-visible endurance session. */
public final class LiveTrackingService extends Service implements LocationListener {
    public static final String ACTION_START = "com.badimps.personaltrainer.tracking.START";
    public static final String ACTION_PAUSE = "com.badimps.personaltrainer.tracking.PAUSE";
    public static final String ACTION_RESUME = "com.badimps.personaltrainer.tracking.RESUME";
    public static final String ACTION_FINISH = "com.badimps.personaltrainer.tracking.FINISH";
    public static final String ACTION_DISCARD = "com.badimps.personaltrainer.tracking.DISCARD";
    public static final String ACTION_UPDATE = "com.badimps.personaltrainer.tracking.UPDATE";
    public static final String EXTRA_MODE = "tracking_mode";
    private static final String CHANNEL = "endurance_live_tracking";
    private static final int NOTIFICATION_ID = 2601;

    private final Handler handler = new Handler();
    private LocationManager locationManager;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            broadcast();
            LiveTrackingStore.Session session = LiveTrackingStore.current(LiveTrackingService.this);
            if (session.active && !session.paused) handler.postDelayed(this, 1000L);
        }
    };

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_START.equals(action)) {
            LiveTrackingStore.start(this, intent.getStringExtra(EXTRA_MODE));
            ensureChannel();
            startForeground(NOTIFICATION_ID, notification());
            requestLocations();
            handler.removeCallbacks(tick);
            handler.post(tick);
        } else if (ACTION_PAUSE.equals(action)) {
            LiveTrackingStore.pause(this);
            stopLocations();
            handler.removeCallbacks(tick);
            updateNotification();
            broadcast();
        } else if (ACTION_RESUME.equals(action)) {
            LiveTrackingStore.resume(this);
            requestLocations();
            handler.removeCallbacks(tick);
            handler.post(tick);
            updateNotification();
            broadcast();
        } else if (ACTION_FINISH.equals(action)) {
            finishSession();
        } else if (ACTION_DISCARD.equals(action)) {
            LiveTrackingStore.discard(this);
            stopTracking();
        }
        return START_NOT_STICKY;
    }

    private void finishSession() {
        LiveTrackingStore.Session session = LiveTrackingStore.finish(this);
        if (session != null) {
            int minutes = Math.max(1, (int) Math.round(session.activeMillis / 60000d));
            JourneyStore.recordWorkout(this, "Endurance · " + session.mode, minutes);
        }
        stopTracking();
    }

    private void requestLocations() {
        if (!hasLocationPermission()) return;
        if (locationManager == null) locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 3f, this);
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 5f, this);
            }
        } catch (SecurityException ignored) { }
    }

    private void stopLocations() {
        if (locationManager == null) return;
        try { locationManager.removeUpdates(this); } catch (SecurityException ignored) { }
    }

    @Override public void onLocationChanged(Location location) {
        if (location == null || (location.hasAccuracy() && location.getAccuracy() > 65f)) return;
        LiveTrackingStore.Session before = LiveTrackingStore.current(this);
        if (!before.active || before.paused) return;
        if (!before.points.isEmpty()) {
            LiveTrackingStore.Point last = before.points.get(before.points.size() - 1);
            float[] jump = new float[1];
            Location.distanceBetween(last.latitude, last.longitude, location.getLatitude(), location.getLongitude(), jump);
            if (jump[0] > 250f) return;
        }
        LiveTrackingStore.addPoint(this, location.getLatitude(), location.getLongitude());
        updateNotification();
        broadcast();
    }

    private void stopTracking() {
        stopLocations();
        handler.removeCallbacks(tick);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        broadcast();
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void ensureChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Live endurance tracking", NotificationManager.IMPORTANCE_LOW));
    }

    private void updateNotification() { ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification()); }

    private android.app.Notification notification() {
        LiveTrackingStore.Session session = LiveTrackingStore.current(this);
        String state = session.paused ? "Paused" : "Tracking";
        String text = state + " " + session.mode + " · " + formatDistance(session.distanceMeters) + " · " + formatDuration(session.activeMillis);
        Intent open = new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 9, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String controlAction = session.paused ? ACTION_RESUME : ACTION_PAUSE;
        String controlLabel = session.paused ? "Resume" : "Pause";
        return new NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_nav_endurance)
                .setContentTitle("Live endurance tracking").setContentText(text).setOngoing(true).setContentIntent(content)
                .addAction(0, controlLabel, serviceIntent(controlAction, 10))
                .addAction(0, "Finish", serviceIntent(ACTION_FINISH, 11)).build();
    }

    private PendingIntent serviceIntent(String action, int requestCode) {
        Intent command = new Intent(this, LiveTrackingService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, command, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void broadcast() { sendBroadcast(new Intent(ACTION_UPDATE).setPackage(getPackageName())); }
    private String formatDistance(float meters) { return meters >= 1000f ? String.format(java.util.Locale.US, "%.2f km", meters / 1000f) : Math.round(meters) + " m"; }
    private String formatDuration(long millis) { long seconds = millis / 1000L; return String.format(java.util.Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L); }
    @Override public void onProviderEnabled(String provider) { requestLocations(); }
    @Override public void onProviderDisabled(String provider) { broadcast(); }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { stopLocations(); handler.removeCallbacks(tick); super.onDestroy(); }
}
