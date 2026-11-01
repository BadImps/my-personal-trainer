package com.badimps.personaltrainer;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;

/** Opens the companion controller when the paired phone begins a guided workout. */
public final class WearWorkoutWakeService extends WearableListenerService {
    private static final String OPEN_PATH = "/workout/open";
    private static final String OTA_DIAGNOSTICS_REQUEST_PATH = "/ota/diagnostics/request";
    private static final String OTA_DIAGNOSTICS_RESPONSE_PATH = "/ota/diagnostics/response";

    @Override public void onMessageReceived(MessageEvent event) {
        if (OTA_DIAGNOSTICS_REQUEST_PATH.equals(event.getPath())) {
            String version = BuildConfig.VERSION_NAME + "|" + BuildConfig.VERSION_CODE;
            Wearable.getMessageClient(this).sendMessage(event.getSourceNodeId(), OTA_DIAGNOSTICS_RESPONSE_PATH,
                    version.getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (!OPEN_PATH.equals(event.getPath())) return;
        Intent open = new Intent(this, WearWorkoutActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(open);
    }
}
