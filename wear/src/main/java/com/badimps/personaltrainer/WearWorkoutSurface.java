package com.badimps.personaltrainer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.wear.ongoing.OngoingActivity;
import androidx.wear.ongoing.Status;

import com.google.android.gms.wearable.DataMap;

/** One-tap return surface for the phone-owned workout; it does not start a second timer. */
final class WearWorkoutSurface {
    private static final String CHANNEL = "active_workout";
    private static final int ID = 7401;

    static void update(Context context, DataMap state) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        if (!state.getBoolean("active", false)) { manager.cancel(ID); return; }
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Active workout",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the current guided workout available on your watch");
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
        Intent open = new Intent(context, WearWorkoutActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent touch = PendingIntent.getActivity(context, ID, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String exercise = state.getString("exercise", "Workout");
        int set = Math.max(1, state.getInt("set", 1));
        int sets = Math.max(1, state.getInt("sets", 1));
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(exercise)
                .setContentText("Set " + set + " of " + sets)
                .setCategory(NotificationCompat.CATEGORY_WORKOUT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true).setSilent(true).setOngoing(true).setContentIntent(touch);
        Status status = new Status.Builder().addTemplate(exercise + " · Set " + set + " of " + sets).build();
        OngoingActivity ongoing = new OngoingActivity.Builder(context, ID, notification)
                .setStaticIcon(R.drawable.ic_launcher_foreground)
                .setTouchIntent(touch)
                .setStatus(status)
                .build();
        ongoing.apply(context);
        manager.notify(ID, notification.build());
    }

    private WearWorkoutSurface() { }
}
