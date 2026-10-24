package com.badimps.personaltrainer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

/** Keeps guided workout timing alive and exposes compact icon-only notification controls. */
public final class GuidedWorkoutService extends Service {
    public static final String ACTION_REFRESH = "com.badimps.personaltrainer.workout.REFRESH";
    public static final String ACTION_TOGGLE = "com.badimps.personaltrainer.workout.TOGGLE";
    public static final String ACTION_OPEN = "com.badimps.personaltrainer.workout.OPEN";
    public static final String ACTION_END = "com.badimps.personaltrainer.workout.END";
    public static final String ACTION_COMPLETE = "com.badimps.personaltrainer.workout.COMPLETE";
    public static final String ACTION_PREVIOUS = "com.badimps.personaltrainer.workout.PREVIOUS";
    private static final String CHANNEL = "guided_workout";
    private static final int ID = 7301;

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Active workout",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Controls an active guided workout");
            channel.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_REFRESH : intent.getAction();
        if (ACTION_TOGGLE.equals(action)) {
            WorkoutSessionStore.State state = WorkoutSessionStore.get(this);
            WorkoutSessionStore.setPaused(this, !state.paused);
        } else if (ACTION_COMPLETE.equals(action)) {
            WorkoutSessionStore.completeCurrentSet(this);
        } else if (ACTION_PREVIOUS.equals(action)) {
            WorkoutSessionStore.previousExercise(this);
        } else if (ACTION_END.equals(action)) {
            WorkoutSessionStore.clear(this);
        }
        WorkoutSessionStore.State state = WorkoutSessionStore.get(this);
        if (!state.active) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(ID, notification(state));
        WearWorkoutSync.publish(this, state);
        return START_STICKY;
    }

    private Notification notification(WorkoutSessionStore.State state) {
        int progress = Math.max(0, Math.min(state.exerciseCount, state.exerciseIndex + 1));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_nav_workouts)
                .setColor(CoachHeroDesign.MINT)
                .setContentTitle(state.paused ? "Workout paused" : state.title)
                .setContentText(state.exercise + "  •  Set " + state.set + " of " + state.sets)
                .setSubText(progress + " of " + state.exerciseCount + " exercises")
                .setProgress(Math.max(1, state.exerciseCount), progress, false)
                .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setWhen(state.startedAtEpochMillis > 0L ? state.startedAtEpochMillis : System.currentTimeMillis())
                .setUsesChronometer(true).setShowWhen(true)
                .setContentIntent(activityIntent(ACTION_OPEN, 7302))
                .addAction(R.drawable.ic_notification_previous, "Previous", serviceIntent(ACTION_PREVIOUS, 7303))
                .addAction(state.paused ? R.drawable.ic_notification_play : R.drawable.ic_notification_pause,
                        state.paused ? "Resume" : "Pause", serviceIntent(ACTION_TOGGLE, 7304))
                .addAction(R.drawable.ic_notification_complete, "Complete set", serviceIntent(ACTION_COMPLETE, 7305))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        state.exercise + "  •  Set " + state.set + " of " + state.sets
                                + "\nWorkout " + progress + " of " + state.exerciseCount));
        if (Build.VERSION.SDK_INT >= 36) builder.setRequestPromotedOngoing(true);
        return builder.build();
    }

    private PendingIntent serviceIntent(String action, int requestCode) {
        Intent intent = new Intent(this, GuidedWorkoutService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent activityIntent(String action, int requestCode) {
        Intent intent = new Intent(this, MainActivity.class).setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
