package com.badimps.personaltrainer;

import android.content.Intent;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/** Updates the system workout surface even while the full watch activity is not visible. */
public final class WearWorkoutDataService extends WearableListenerService {
    private static final String STATE_PATH = "/workout/state";

    @Override public void onDataChanged(DataEventBuffer events) {
        for (DataEvent event : events) {
            if (event.getType() != DataEvent.TYPE_CHANGED
                    || !STATE_PATH.equals(event.getDataItem().getUri().getPath())) continue;
            DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
            boolean wasGuided = getSharedPreferences("watch_health_bridge",MODE_PRIVATE).getBoolean("guided",false);
            boolean isActive = map.getBoolean("active",false);
            boolean wasPaused = getSharedPreferences("watch_health_bridge",MODE_PRIVATE).getBoolean("paused",false);
            boolean isPaused = map.getBoolean("paused",false);
            String sessionId = map.getString("session_id","");
            long revision = map.getLong("revision",0L);
            android.content.SharedPreferences snapshot = getSharedPreferences("watch_workout_snapshot", MODE_PRIVATE);
            String cachedSession = snapshot.getString("session_id", "");
            long cachedRevision = snapshot.getLong("revision", -1L);
            // Reject delayed state from the same phone session.  This is essential after watch
            // sleep/reconnect: an old rest snapshot must never reset a current active timer.
            if (sessionId.equals(cachedSession) && revision <= cachedRevision) continue;
            String phase = map.getString("phase", "");
            boolean collectingPhase = "active".equals(phase) || "rest".equals(phase)
                    || "cooldown".equals(phase) || "finish_cooldown".equals(phase);
            String heartPermission=android.os.Build.VERSION.SDK_INT>=36
                    ?"android.permission.health.READ_HEART_RATE":android.Manifest.permission.BODY_SENSORS;
            boolean healthGranted=checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)==android.content.pm.PackageManager.PERMISSION_GRANTED
                    &&checkSelfPermission(heartPermission)==android.content.pm.PackageManager.PERMISSION_GRANTED;
            String bridgedSession=getSharedPreferences("watch_health_bridge",MODE_PRIVATE).getString("guided_session","");
            // A new phone session must never reuse the previous session's Health Services
            // stream.  Stop the old stream first; the next authoritative snapshot starts the new one.
            if(isActive&&wasGuided&&!sessionId.equals(bridgedSession)){
                getSharedPreferences("watch_health_bridge",MODE_PRIVATE).edit().putBoolean("guided",false).remove("guided_session").apply();
                startService(new Intent(this,WearHealthExerciseService.class).setAction(WearHealthExerciseService.END));
            } else if(isActive&&collectingPhase&&!wasGuided&&!WearHealthSessionStore.load(this).active&&healthGranted){
                getSharedPreferences("watch_health_bridge",MODE_PRIVATE).edit().putBoolean("guided",true).putString("guided_session",sessionId).apply();
                Intent health=new Intent(this,WearHealthExerciseService.class).setAction(WearHealthExerciseService.START)
                        .putExtra("mode","guided").putExtra("started",map.getLong("started",System.currentTimeMillis()))
                        .putExtra("session_id",sessionId);
                if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(health);else startService(health);
            }else if(!isActive&&wasGuided){
                getSharedPreferences("watch_health_bridge",MODE_PRIVATE).edit().putBoolean("guided",false).remove("guided_session").apply();
                startService(new Intent(this,WearHealthExerciseService.class).setAction(WearHealthExerciseService.END));
            }else if(isActive&&wasGuided&&isPaused!=wasPaused){
                startService(new Intent(this,WearHealthExerciseService.class)
                        .setAction(isPaused?WearHealthExerciseService.PAUSE:WearHealthExerciseService.RESUME));
            }
            getSharedPreferences("watch_health_bridge",MODE_PRIVATE).edit().putBoolean("paused",isPaused).apply();
            snapshot.edit()
                    .putBoolean("active", map.getBoolean("active", false))
                    .putBoolean("paused", map.getBoolean("paused", false))
                    .putString("exercise", map.getString("exercise", "Workout"))
                    .putInt("set", map.getInt("set", 1)).putInt("sets", map.getInt("sets", 1))
                    .putInt("index", map.getInt("exercise_index", 0))
                    .putInt("count", map.getInt("exercise_count", 1))
                    .putString("phase", map.getString("phase", "active"))
                    .putInt("remaining", map.getInt("recovery_remaining", 0))
                    .putInt("total", map.getInt("recovery_total", 0))
                    .putString("upcoming", map.getString("upcoming_exercise", ""))
                    .putLong("timer_elapsed",map.getLong("timer_elapsed",0L))
                    .putLong("timer_snapshot_epoch",map.getLong("timer_snapshot_epoch",0L))
                    .putInt("countdown_value",map.getInt("countdown_value",0))
                    .putLong("recovery_deadline_epoch",map.getLong("recovery_deadline_epoch",0L)).apply();
            snapshot.edit()
                    .putString("session_id",sessionId).putLong("revision",revision).apply();
            WearWorkoutSurface.update(this, map);
            openNewPhoneWorkoutOnce(isActive, sessionId, map.getString("phase", "active"));
            androidx.wear.tiles.TileService.getUpdater(this)
                    .requestUpdate(CoachWorkoutTileService.class);
        }
    }

    /** Opens one watch surface per phone-owned workout; ordinary state updates never relaunch it. */
    private void openNewPhoneWorkoutOnce(boolean active, String sessionId, String phase) {
        if (!active || !"countdown".equals(phase) || sessionId == null || sessionId.isEmpty()) return;
        android.content.SharedPreferences preferences = getSharedPreferences("watch_workout_launch", MODE_PRIVATE);
        if (sessionId.equals(preferences.getString("opened_session", ""))) return;
        // Commit before launching so a duplicate urgent DataItem cannot create a second activity.
        if (!preferences.edit().putString("opened_session", sessionId).commit()) return;
        Intent open = new Intent(this, WearWorkoutActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try { startActivity(open); }
        catch (android.content.ActivityNotFoundException | SecurityException ignored) {
            // The synchronized tile/notification remains available if watch policy blocks
            // background activity launches while the device is locked or in theatre mode.
        }
    }
}
