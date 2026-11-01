package com.badimps.personaltrainer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Queries the installed Wear companion over the same signed Data Layer used for workouts. */
public final class WearOtaDiagnostics {
    public static final String REQUEST_PATH = "/ota/diagnostics/request";
    public static final String RESPONSE_PATH = "/ota/diagnostics/response";
    private static final long RESPONSE_TIMEOUT_MS = 8_000L;

    public static final class WatchVersion {
        public final String nodeName;
        public final String versionName;
        public final long versionCode;

        WatchVersion(String nodeName, String versionName, long versionCode) {
            this.nodeName = nodeName;
            this.versionName = versionName;
            this.versionCode = versionCode;
        }
    }

    public interface Callback { void onComplete(List<WatchVersion> watches, String error); }

    public static void requestInstalledVersions(Context context, Callback callback) {
        Handler main = new Handler(Looper.getMainLooper());
        try {
            Wearable.getNodeClient(context).getConnectedNodes().addOnSuccessListener(nodes -> {
                if (nodes.isEmpty()) {
                    callback.onComplete(new ArrayList<>(), "No paired Wear OS device is currently connected.");
                    return;
                }
                Map<String, String> names = new LinkedHashMap<>();
                for (Node node : nodes) names.put(node.getId(), node.getDisplayName());
                Map<String, WatchVersion> replies = new LinkedHashMap<>();
                MessageClient.OnMessageReceivedListener listener = event -> {
                    if (!RESPONSE_PATH.equals(event.getPath())) return;
                    WatchVersion version = decode(names.get(event.getSourceNodeId()), event.getData());
                    if (version != null) replies.put(event.getSourceNodeId(), version);
                };
                Wearable.getMessageClient(context).addListener(listener);
                Runnable finish = () -> {
                    Wearable.getMessageClient(context).removeListener(listener);
                    List<WatchVersion> result = new ArrayList<>(replies.values());
                    String error = result.size() == names.size() ? "" : "No version reply from "
                            + (names.size() - result.size()) + " connected watch. Keep the companion open or reconnect it, then refresh.";
                    callback.onComplete(result, error);
                };
                for (Node node : nodes) Wearable.getMessageClient(context).sendMessage(node.getId(), REQUEST_PATH,
                        "version".getBytes(StandardCharsets.UTF_8));
                main.postDelayed(finish, RESPONSE_TIMEOUT_MS);
            }).addOnFailureListener(error -> callback.onComplete(new ArrayList<>(),
                    "Wear OS services could not list paired devices: " + safeMessage(error)));
        } catch (RuntimeException error) {
            callback.onComplete(new ArrayList<>(), "Wear OS services are unavailable: " + safeMessage(error));
        }
    }

    private static WatchVersion decode(String nodeName, byte[] payload) {
        try {
            String[] parts = new String(payload, StandardCharsets.UTF_8).split("\\|", 2);
            if (parts.length != 2) return null;
            return new WatchVersion(nodeName == null || nodeName.isEmpty() ? "Wear OS device" : nodeName,
                    parts[0], Long.parseLong(parts[1]));
        } catch (RuntimeException ignored) { return null; }
    }

    private static String safeMessage(Throwable error) {
        String message = error == null ? "Unknown error" : error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }

    private WearOtaDiagnostics() { }
}
