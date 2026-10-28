package com.badimps.personaltrainer;

import android.content.Context;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.StandardCharsets;

/** Publishes one canonical phone-owned workout snapshot to every paired Wear OS device. */
public final class WearWorkoutSync {
    public static final String STATE_PATH = "/workout/state";
    public static final String COMMAND_PATH = "/workout/command";
    /** Wakes the installed companion when a phone workout becomes active. */
    public static final String OPEN_PATH = "/workout/open";

    public static void publish(Context context, WorkoutSessionStore.State state) {
        PutDataMapRequest request = PutDataMapRequest.create(STATE_PATH);
        request.getDataMap().putBoolean("active", state.active);
        request.getDataMap().putBoolean("paused", state.paused);
        request.getDataMap().putString("title", state.title);
        request.getDataMap().putString("exercise", state.exercise);
        request.getDataMap().putInt("exercise_index", state.exerciseIndex);
        request.getDataMap().putInt("exercise_count", state.exerciseCount);
        request.getDataMap().putInt("set", state.set);
        request.getDataMap().putInt("sets", state.sets);
        request.getDataMap().putLong("started", state.startedAtEpochMillis);
        request.getDataMap().putLong("revision", System.currentTimeMillis());
        Wearable.getDataClient(context).putDataItem(request.asPutDataRequest().setUrgent());
        // A DataItem remains available for reconnects, while this message gives an installed
        // companion an immediate, best-effort wake-up on the start of a live session.
        if (state.active) {
            Wearable.getNodeClient(context).getConnectedNodes().addOnSuccessListener(nodes -> {
                for (com.google.android.gms.wearable.Node node : nodes) {
                    Wearable.getMessageClient(context).sendMessage(node.getId(), OPEN_PATH,
                            "open".getBytes(StandardCharsets.UTF_8));
                }
            });
        }
    }

    private WearWorkoutSync() { }
}
