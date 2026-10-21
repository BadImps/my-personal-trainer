package com.badimps.personaltrainer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

/** Keeps guided workout timing alive and exposes compact icon-only notification controls. */
public final class GuidedWorkoutService extends Service {
    public static final String ACTION_REFRESH = "com.badimps.personaltrainer.workout.REFRESH";
    public static final String ACTION_TOGGLE = "com.badimps.personaltrainer.workout.TOGGLE";
    public static final String ACTION_OPEN = "com.badimps.personaltrainer.workout.OPEN";
    public static final String ACTION_END = "com.badimps.personaltrainer.workout.END";
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
        }
        WorkoutSessionStore.State state = WorkoutSessionStore.get(this);
        if (!state.active) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(ID, notification(state));
        return START_STICKY;
    }

    private Notification notification(WorkoutSessionStore.State state) {
        int progress = Math.max(0, Math.min(state.exerciseCount, state.exerciseIndex + 1));
        RemoteViews controls = new RemoteViews(getPackageName(), R.layout.notification_guided_workout);
        controls.setTextViewText(R.id.workout_notification_title,
                state.paused ? "Workout paused" : state.title);
        controls.setTextViewText(R.id.workout_notification_detail,
                state.exercise + "  •  Set " + state.set + " of " + state.sets
                        + "  •  " + progress + " of " + state.exerciseCount);
        controls.setProgressBar(R.id.workout_notification_progress,
                Math.max(1, state.exerciseCount), progress, false);
        controls.setImageViewResource(R.id.workout_notification_toggle,
                state.paused ? R.drawable.ic_notification_play : R.drawable.ic_notification_pause);
        controls.setContentDescription(R.id.workout_notification_toggle,
                state.paused ? "Resume workout" : "Pause workout");
        controls.setOnClickPendingIntent(R.id.workout_notification_toggle,
                serviceIntent(ACTION_TOGGLE, 7303));
        controls.setOnClickPendingIntent(R.id.workout_notification_open,
                activityIntent(ACTION_OPEN, 7304));
        controls.setOnClickPendingIntent(R.id.workout_notification_end,
                activityIntent(ACTION_END, 7305));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_nav_workouts)
                .setColor(CoachHeroDesign.MINT)
                .setContentTitle(state.paused ? "Workout paused" : state.title)
                .setContentText(state.exercise + "  •  Set " + state.set + " of " + state.sets)
                .setSubText(progress + " of " + state.exerciseCount + " exercises")
                .setProgress(Math.max(1, state.exerciseCount), progress, false)
                .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
                .setContentIntent(activityIntent(ACTION_OPEN, 7302))
                .setCustomContentView(controls)
                .setCustomBigContentView(controls)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
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
