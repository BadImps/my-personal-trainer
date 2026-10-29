package com.badimps.personaltrainer;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Minimal authenticated client for the public Gym Lobby and private-lobby rankings. */
public final class GymLobbyClient {
    public interface Callback<T> { void complete(T value, String error); }

    public static final class Member {
        public final String id, name, apex;
        public final int level, points, rank;
        public final boolean buddy;
        public final List<String> achievements;

        Member(JSONObject json) {
            id = json.optString("id");
            name = json.optString("name", "Gym Member");
            apex = json.optString("apex", "Foundation");
            level = Math.max(1, json.optInt("level", 1));
            points = Math.max(0, json.optInt("points", 0));
            rank = Math.max(0, json.optInt("rank", 0));
            buddy = json.optBoolean("buddy", false);
            ArrayList<String> earned = new ArrayList<>();
            JSONArray values = json.optJSONArray("achievements");
            if (values != null) for (int index = 0; index < values.length(); index++) {
                String value = values.optString(index, "").trim();
                if (!value.isEmpty()) earned.add(value);
            }
            achievements = Collections.unmodifiableList(earned);
        }
    }

    public static final class Request {
        public final String id, kind, senderName, lobbyOwnerId, lobbyOwnerName;
        Request(JSONObject json) {
            id = json.optString("id");
            kind = json.optString("kind");
            senderName = json.optString("senderName", "Gym Member");
            lobbyOwnerId = json.optString("lobbyOwnerId");
            lobbyOwnerName = json.optString("lobbyOwnerName", senderName);
        }
    }

    public static final class Lobby {
        public final String ownerId, ownerName;
        Lobby(JSONObject json) {
            ownerId = json.optString("ownerId");
            ownerName = json.optString("ownerName", "Gym Member");
        }
    }

    /** A private social message. Only the sender and recipient receive it from Supabase. */
    public static final class Message {
        public final String id, senderId, senderName, body, createdAt;
        public final boolean unread;
        Message(JSONObject json) {
            id = json.optString("id"); senderId = json.optString("senderId");
            senderName = json.optString("senderName", "Gym Member");
            body = json.optString("body", ""); createdAt = json.optString("createdAt", "");
            unread = json.optBoolean("unread", false);
        }
    }

    public static final class Snapshot {
        public final List<Member> members;
        public final List<Request> requests;
        public final List<Lobby> lobbies;

        Snapshot(JSONObject json) {
            members = Collections.unmodifiableList(members(json.optJSONArray("members")));
            ArrayList<Request> incoming = new ArrayList<>();
            JSONArray requestValues = json.optJSONArray("requests");
            if (requestValues != null) for (int index = 0; index < requestValues.length(); index++) {
                JSONObject value = requestValues.optJSONObject(index);
                if (value != null) incoming.add(new Request(value));
            }
            requests = Collections.unmodifiableList(incoming);
            ArrayList<Lobby> joined = new ArrayList<>();
            JSONArray lobbyValues = json.optJSONArray("lobbies");
            if (lobbyValues != null) for (int index = 0; index < lobbyValues.length(); index++) {
                JSONObject value = lobbyValues.optJSONObject(index);
                if (value != null) joined.add(new Lobby(value));
            }
            lobbies = Collections.unmodifiableList(joined);
        }
    }

    public static final class Inbox {
        public final List<Request> requests;
        public final List<Message> messages;
        Inbox(JSONObject json) {
            ArrayList<Request> pending = new ArrayList<>();
            JSONArray requestValues = json.optJSONArray("requests");
            if (requestValues != null) for (int index = 0; index < requestValues.length(); index++) {
                JSONObject value = requestValues.optJSONObject(index); if (value != null) pending.add(new Request(value));
            }
            requests = Collections.unmodifiableList(pending);
            ArrayList<Message> received = new ArrayList<>();
            JSONArray messageValues = json.optJSONArray("messages");
            if (messageValues != null) for (int index = 0; index < messageValues.length(); index++) {
                JSONObject value = messageValues.optJSONObject(index); if (value != null) received.add(new Message(value));
            }
            messages = Collections.unmodifiableList(received);
        }
        public int unreadCount() {
            int unreadMessages = 0;
            for (Message message : messages) if (message.unread) unreadMessages++;
            return GymLobbyClient.unreadCount(requests.size(), unreadMessages);
        }
    }

    static int unreadCount(int pendingRequests, int unreadMessages) {
        return Math.max(0, pendingRequests) + Math.max(0, unreadMessages);
    }

    public static final class Leaderboard {
        public final String ownerId, ownerName;
        public final List<Member> members;
        Leaderboard(JSONObject json) {
            ownerId = json.optString("ownerId");
            ownerName = json.optString("ownerName", "Gym Member");
            members = Collections.unmodifiableList(members(json.optJSONArray("members")));
        }
    }

    private static final ExecutorService NETWORK = Executors.newSingleThreadExecutor();

    public static boolean available(Context context) {
        return SupabaseConfig.isConfigured() && session(context) != null;
    }

    public static void publishAndLoad(Context context, Callback<Snapshot> callback) {
        JSONObject profile = publicProfile(context);
        rpc(context, "gym_lobby_publish", profile, (ignored, publishError) -> {
            if (publishError != null) { callback.complete(null, publishError); return; }
            rpc(context, "gym_lobby_snapshot", new JSONObject(), (json, error) ->
                    callback.complete(error == null ? new Snapshot(json) : null, error));
        });
    }

    public static void publish(Context context) {
        if (!available(context)) return;
        rpc(context, "gym_lobby_publish", publicProfile(context), (ignored, error) -> { });
    }

    public static void send(Context context, String targetId, String kind, Callback<Boolean> callback) {
        JSONObject body = new JSONObject();
        put(body, "target_id", targetId); put(body, "action_kind", kind);
        rpc(context, "gym_lobby_send", body, (ignored, error) -> callback.complete(error == null, error));
    }

    public static void respond(Context context, String requestId, boolean accept, Callback<Boolean> callback) {
        JSONObject body = new JSONObject();
        put(body, "request_id", requestId); put(body, "accept_request", accept);
        rpc(context, "gym_lobby_respond", body, (ignored, error) -> callback.complete(error == null, error));
    }

    public static void inbox(Context context, Callback<Inbox> callback) {
        rpc(context, "gym_lobby_inbox", new JSONObject(), (json, error) -> {
            if (error == null && json.has("error")) error = json.optString("error");
            callback.complete(error == null ? new Inbox(json) : null, error);
        });
    }

    public static void sendMessage(Context context, String targetId, String message, Callback<Boolean> callback) {
        JSONObject body = new JSONObject(); put(body, "target_id", targetId); put(body, "message_body", message == null ? "" : message.trim());
        rpc(context, "gym_lobby_send_message", body, (ignored, error) -> callback.complete(error == null, error));
    }

    public static void markMessagesRead(Context context, Callback<Boolean> callback) {
        rpc(context, "gym_lobby_mark_messages_read", new JSONObject(), (ignored, error) -> callback.complete(error == null, error));
    }

    public static void leaderboard(Context context, String ownerId, Callback<Leaderboard> callback) {
        JSONObject body = new JSONObject(); put(body, "lobby_owner", ownerId);
        rpc(context, "gym_lobby_leaderboard", body, (json, error) -> {
            if (error == null && json.has("error")) error = json.optString("error");
            callback.complete(error == null ? new Leaderboard(json) : null, error);
        });
    }

    static String requestTitle(String kind) {
        if ("buddy".equals(kind)) return "Gym Buddy request";
        if ("invite".equals(kind)) return "Private lobby invitation";
        if ("join".equals(kind)) return "Private lobby join request";
        return "Gym Lobby request";
    }

    static String requestDescription(String kind, String senderName, String lobbyOwnerName) {
        String sender = senderName == null || senderName.trim().isEmpty() ? "A Gym Member" : senderName.trim();
        String owner = lobbyOwnerName == null || lobbyOwnerName.trim().isEmpty() ? sender : lobbyOwnerName.trim();
        if ("buddy".equals(kind)) return sender + " wants to become your Gym Buddy.";
        if ("invite".equals(kind)) return sender + " invited you to " + owner + "’s private lobby.";
        if ("join".equals(kind)) return sender + " wants to join " + owner + "’s private lobby.";
        return sender + " sent you a Gym Lobby request.";
    }

    static String rankLabel(int rank) {
        if (rank == 1) return "1ST";
        if (rank == 2) return "2ND";
        if (rank == 3) return "3RD";
        return "#" + Math.max(1, rank);
    }

    private static JSONObject publicProfile(Context context) {
        AccountStore.Profile account = AccountStore.get(context);
        AchievementStore.Snapshot progress = AchievementStore.snapshot(context);
        JSONArray earned = new JSONArray();
        for (AchievementStore.Badge badge : progress.badges) if (badge.unlocked()) earned.put(badge.name);
        JSONObject body = new JSONObject();
        put(body, "member_name", account.username);
        put(body, "member_level", progress.level);
        put(body, "member_points", progress.xp);
        put(body, "member_apex", progress.currentApexTier().name);
        put(body, "member_achievements", earned);
        return body;
    }

    private static void rpc(Context context, String function, JSONObject body, Callback<JSONObject> callback) {
        AuthSessionStore.Session auth = session(context);
        if (auth == null) { callback.complete(null, "Sign in to your verified account to use the Gym Lobby."); return; }
        NETWORK.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(SupabaseConfig.restEndpoint("/rpc/" + function)).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15_000); connection.setReadTimeout(20_000);
                connection.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + auth.accessToken);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(payload.length);
                try (OutputStream output = connection.getOutputStream()) { output.write(payload); }
                int status = connection.getResponseCode();
                String response = read(status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream());
                if (status < 200 || status >= 300) {
                    JSONObject error = response.isEmpty() ? new JSONObject() : new JSONObject(response);
                    callback.complete(null, friendly(error.optString("message", "Gym Lobby request failed.")));
                    return;
                }
                callback.complete(response.isEmpty() ? new JSONObject() : new JSONObject(response), null);
            } catch (Exception error) {
                callback.complete(null, "Could not reach the Gym Lobby. Check your connection and try again.");
            } finally { if (connection != null) connection.disconnect(); }
        });
    }

    private static AuthSessionStore.Session session(Context context) {
        AuthSessionStore.Session value = AuthSessionStore.load(context);
        return value != null && value.isUsable() ? value : null;
    }

    private static ArrayList<Member> members(JSONArray values) {
        ArrayList<Member> result = new ArrayList<>();
        if (values != null) for (int index = 0; index < values.length(); index++) {
            JSONObject value = values.optJSONObject(index);
            if (value != null) result.add(new Member(value));
        }
        return result;
    }

    private static String friendly(String value) {
        String message = value == null ? "" : value.toLowerCase();
        if (message.contains("jwt") || message.contains("authentication")) return "Your secure session expired. Sign in again to use the Gym Lobby.";
        if (message.contains("duplicate") || message.contains("unique")) return "That request is already waiting for a response.";
        return value == null || value.trim().isEmpty() ? "Gym Lobby request failed." : value;
    }

    private static String read(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) value.append(line);
        }
        return value.toString();
    }

    private static void put(JSONObject json, String key, Object value) {
        try { json.put(key, value); } catch (Exception ignored) { }
    }

    private GymLobbyClient() { }
}
