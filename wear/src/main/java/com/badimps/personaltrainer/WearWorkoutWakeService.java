package com.badimps.personaltrainer;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/** Opens the companion controller when the paired phone begins a guided workout. */
public final class WearWorkoutWakeService extends WearableListenerService {
    private static final String OPEN_PATH = "/workout/open";

    @Override public void onMessageReceived(MessageEvent event) {
        if (!OPEN_PATH.equals(event.getPath())) return;
        Intent open = new Intent(this, WearWorkoutActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(open);
    }
}
