package com.badimps.personaltrainer;

import android.content.Context;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

/** Publishes one canonical phone-owned workout snapshot to every paired Wear OS device. */
public final class WearWorkoutSync {
    public static final String STATE_PATH = "/workout/state";
    public static final String COMMAND_PATH = "/workout/command";

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
    }

    private WearWorkoutSync() { }
}
