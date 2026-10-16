package com.badimps.personaltrainer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.Locale;

/** Schedules only the locally chosen plan dates; no reminder data leaves the device. */
public final class WorkoutReminderScheduler {
    public static final String CHANNEL_ID = "coach_workout_reminders";
    public static final String ACTION_REMIND = "com.badimps.personaltrainer.ACTION_WORKOUT_REMINDER";
    private static final int REQUEST_BASE = 4800;
    private static final int SNOOZE_REQUEST = 4899;

    public static void schedule(Context context, PlanStore.SavedPlan plan) {
        cancel(context);
        if (plan == null || !plan.reminderEnabled) return;
        Calendar now = Calendar.getInstance();
        Calendar week = weekStart(plan.weekOffset);
        if ("Weekly".equals(plan.mode)) {
            String[] days = plan.weeklyDays.split(",", -1);
            for (int i = 0; i < 7; i++) {
                if (i < days.length && "1".equals(days[i])) scheduleIfFuture(context, dateAt(week, i, plan.reminderHour, plan.reminderMinute), REQUEST_BASE + i, plan.target);
            }
        } else if ("Daily".equals(plan.mode)) {
            scheduleIfFuture(context, dateAt(week, Math.max(0, Math.min(6, plan.scheduledDay)), plan.reminderHour, plan.reminderMinute), REQUEST_BASE, plan.target);
        } else {
            Calendar next = Calendar.getInstance();
            next.set(Calendar.HOUR_OF_DAY, plan.reminderHour);
            next.set(Calendar.MINUTE, plan.reminderMinute);
            next.set(Calendar.SECOND, 0);
            next.set(Calendar.MILLISECOND, 0);
            if (!next.after(now)) next.add(Calendar.DAY_OF_YEAR, 1);
            scheduleIfFuture(context, next, REQUEST_BASE, plan.target);
        }
    }

    public static void scheduleSnooze(Context context, int minutes) {
        Calendar later = Calendar.getInstance();
        later.add(Calendar.MINUTE, Math.max(1, minutes));
        scheduleIfFuture(context, later, SNOOZE_REQUEST, PlanStore.load(context) == null ? "Workout" : PlanStore.load(context).target);
    }

    public static void cancel(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        for (int request = REQUEST_BASE; request <= SNOOZE_REQUEST; request++) {
            if (request > REQUEST_BASE + 6 && request != SNOOZE_REQUEST) continue;
            alarms.cancel(pendingBroadcast(context, request, "Workout"));
        }
    }

    static String timeLabel(int hour, int minute) {
        return String.format(Locale.getDefault(), "%d:%02d %s", hour % 12 == 0 ? 12 : hour % 12, minute, hour >= 12 ? "PM" : "AM");
    }

    static void showNotification(Context context) {
        createChannel(context);
        AccountStore.Profile profile = AccountStore.get(context);
        Intent screen = new Intent(context, WorkoutReminderActivity.class);
        PendingIntent fullScreen = PendingIntent.getActivity(context, 6001, screen, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_plan)
                .setContentTitle("Hey, " + profile.username + ". It’s time to train.")
                .setContentText("Coach " + profile.coachName + " has your workout ready.")
                .setContentIntent(fullScreen)
                .setFullScreenIntent(fullScreen, true)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(6001, notification);
    }

    private static void scheduleIfFuture(Context context, Calendar when, int requestCode, String target) {
        if (when.getTimeInMillis() <= System.currentTimeMillis()) return;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        PendingIntent pending = pendingBroadcast(context, requestCode, target);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pending);
    }

    private static PendingIntent pendingBroadcast(Context context, int requestCode, String target) {
        Intent intent = new Intent(context, WorkoutReminderReceiver.class);
        intent.setAction(ACTION_REMIND);
        intent.putExtra("target", target);
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static Calendar weekStart(int offset) {
        Calendar week = Calendar.getInstance();
        week.setFirstDayOfWeek(Calendar.MONDAY);
        week.set(Calendar.HOUR_OF_DAY, 0);
        week.set(Calendar.MINUTE, 0);
        week.set(Calendar.SECOND, 0);
        week.set(Calendar.MILLISECOND, 0);
        int sinceMonday = (week.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
        week.add(Calendar.DAY_OF_YEAR, -sinceMonday + Math.max(0, offset) * 7);
        return week;
    }

    private static Calendar dateAt(Calendar weekStart, int day, int hour, int minute) {
        Calendar value = (Calendar) weekStart.clone();
        value.add(Calendar.DAY_OF_YEAR, day);
        value.set(Calendar.HOUR_OF_DAY, hour);
        value.set(Calendar.MINUTE, minute);
        value.set(Calendar.SECOND, 0);
        value.set(Calendar.MILLISECOND, 0);
        return value;
    }

    private static void createChannel(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Coach workout reminders", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Full-screen coach reminders for scheduled workouts.");
        manager.createNotificationChannel(channel);
    }

    private WorkoutReminderScheduler() {}
}
