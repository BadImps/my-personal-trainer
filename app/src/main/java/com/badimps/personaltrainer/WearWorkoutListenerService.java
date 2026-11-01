package com.badimps.personaltrainer;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;

/** Accepts commands only through Wear OS' same-package, same-signature Data Layer channel. */
public final class WearWorkoutListenerService extends WearableListenerService {
    public static final String ACTION_COMMAND_APPLIED = "com.badimps.personaltrainer.WEAR_COMMAND_APPLIED";
    @Override public void onMessageReceived(MessageEvent event) {
        if (!WearWorkoutSync.COMMAND_PATH.equals(event.getPath())) return;
        String payload = new String(event.getData(), StandardCharsets.UTF_8);
        String[] parts = payload.split("\\|", 3);
        if (parts.length != 3) return;
        String sessionId = parts[0], command = parts[1], commandId = parts[2];
        if (!WorkoutSessionStore.acceptWatchCommand(this, sessionId, command, commandId)) {
            WearWorkoutSync.publish(this, WorkoutSessionStore.get(this));
            return;
        }
        String action;
        if ("toggle".equals(command)) action = GuidedWorkoutService.ACTION_TOGGLE;
        else if ("complete".equals(command)) action = GuidedWorkoutService.ACTION_COMPLETE;
        else if ("previous".equals(command)) action = GuidedWorkoutService.ACTION_PREVIOUS;
        else if ("skip".equals(command)) action = GuidedWorkoutService.ACTION_SKIP_RECOVERY;
        else if ("end".equals(command)) action = GuidedWorkoutService.ACTION_END;
        else return;
        Intent intent = new Intent(this, GuidedWorkoutService.class).setAction(action)
                .putExtra("watch_session_id", sessionId).putExtra("watch_command_id", commandId);
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        sendBroadcast(new Intent(ACTION_COMMAND_APPLIED).setPackage(getPackageName())
                .putExtra("session_id", sessionId).putExtra("command", command));
    }
}
