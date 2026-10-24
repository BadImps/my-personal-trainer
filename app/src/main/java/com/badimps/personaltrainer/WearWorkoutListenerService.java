package com.badimps.personaltrainer;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;

/** Accepts commands only through Wear OS' same-package, same-signature Data Layer channel. */
public final class WearWorkoutListenerService extends WearableListenerService {
    @Override public void onMessageReceived(MessageEvent event) {
        if (!WearWorkoutSync.COMMAND_PATH.equals(event.getPath())) return;
        String command = new String(event.getData(), StandardCharsets.UTF_8);
        String action;
        if ("toggle".equals(command)) action = GuidedWorkoutService.ACTION_TOGGLE;
        else if ("complete".equals(command)) action = GuidedWorkoutService.ACTION_COMPLETE;
        else if ("previous".equals(command)) action = GuidedWorkoutService.ACTION_PREVIOUS;
        else if ("end".equals(command)) action = GuidedWorkoutService.ACTION_END;
        else return;
        Intent intent = new Intent(this, GuidedWorkoutService.class).setAction(action);
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
    }
}
