package com.badimps.personaltrainer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarm entry point kept intentionally thin so the full coach experience is an Activity. */
public final class WorkoutReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        WorkoutReminderScheduler.showNotification(context);
    }
}
