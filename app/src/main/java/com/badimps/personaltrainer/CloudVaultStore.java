package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Private recovery vault for the app's user-created data. It deliberately excludes passwords and tokens. */
public final class CloudVaultStore {
    private static final List<String> FILES = Arrays.asList(
            "trainer_profile", "appearance", "body_profile", "coach_voice_labels", "endurance_routes",
            "interrupted_guided_workout", "journey_nexus", "live_endurance_tracking", "meal_plan",
            "saved_workout_plan", "endurance_appointments", "completed_set_times", "weekly_schedule_drafts", "weight_trend");
    private static final Set<String> PRIVATE_PROFILE_KEYS = new java.util.HashSet<>(Arrays.asList("salt", "passwordHash", "biometric_unlock", "remember_sign_in"));

    public interface Callback { void complete(boolean success, boolean restored, String message); }

    /** Pulls a previous vault when present; otherwise saves the current device data as its first recovery point. */
    public static void restoreOrCreate(Context context, Callback callback) {
        new Thread(() -> {
            try {
                JSONObject remote = fetch(context);
                if (remote != null) { restore(context, remote); callback.complete(true, true, "Your private cloud data was restored."); }
                else { upload(context); callback.complete(true, false, "Your private cloud backup is now active."); }
            } catch (Exception error) { callback.complete(false, false, "Cloud backup could not be reached. Your device data is unchanged."); }
        }, "cloud-vault-restore").start();
    }

    public static void backup(Context context, Callback callback) {
        new Thread(() -> {
            try { upload(context); callback.complete(true, false, "Private cloud backup updated."); }
            catch (Exception error) { callback.complete(false, false, "Cloud backup could not be updated. Your device data is safe on this phone."); }
        }, "cloud-vault-backup").start();
    }

    public static boolean signedIn(Context context) { return SupabaseConfig.isConfigured() && AuthSessionStore.get(context) != null; }

    private static JSONObject fetch(Context context) throws Exception {
        AuthSessionStore.Session session = requireSession(context);
        String userId = URLEncoder.encode(session.userId, StandardCharsets.UTF_8.name());
        HttpURLConnection connection = open("/rest/v1/member_vaults?select=payload&user_id=eq." + userId, session, "GET");
        int status = connection.getResponseCode();
        String response = read(status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream());
        connection.disconnect();
        if (status < 200 || status >= 300) throw new IllegalStateException("vault read failed");
        JSONArray records = new JSONArray(response);
        return records.length() == 0 ? null : records.getJSONObject(0).optJSONObject("payload");
    }

    private static void upload(Context context) throws Exception {
        AuthSessionStore.Session session = requireSession(context);
        JSONObject body = new JSONObject().put("user_id", session.userId).put("schema_version", 1)
                .put("payload", snapshot(context)).put("client_updated_at", java.time.Instant.now().toString());
        HttpURLConnection create = open("/rest/v1/member_vaults", session, "POST");
        create.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal");
        write(create, body);
        int status = create.getResponseCode();
        create.disconnect();
        if (status < 200 || status >= 300) throw new IllegalStateException("vault write failed");
    }

    private static JSONObject snapshot(Context context) throws Exception {
        JSONObject output = new JSONObject();
        for (String file : FILES) {
            JSONObject values = new JSONObject();
            for (Map.Entry<String, ?> entry : context.getSharedPreferences(file, Context.MODE_PRIVATE).getAll().entrySet()) {
                if ("trainer_profile".equals(file) && PRIVATE_PROFILE_KEYS.contains(entry.getKey())) continue;
                values.put(entry.getKey(), encode(entry.getValue()));
            }
            output.put(file, values);
        }
        return output;
    }

    private static void restore(Context context, JSONObject snapshot) throws Exception {
        for (String file : FILES) {
            JSONObject values = snapshot.optJSONObject(file);
            if (values == null) continue;
            SharedPreferences.Editor edit = context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear();
            java.util.Iterator<String> keys = values.keys();
            while (keys.hasNext()) {
                String key = keys.next(); JSONObject value = values.getJSONObject(key); String type = value.getString("t");
                if ("s".equals(type)) edit.putString(key, value.getString("v"));
                else if ("b".equals(type)) edit.putBoolean(key, value.getBoolean("v"));
                else if ("i".equals(type)) edit.putInt(key, value.getInt("v"));
                else if ("l".equals(type)) edit.putLong(key, value.getLong("v"));
                else if ("f".equals(type)) edit.putFloat(key, (float) value.getDouble("v"));
                else if ("ss".equals(type)) {
                    java.util.HashSet<String> set = new java.util.HashSet<>(); JSONArray array = value.getJSONArray("v");
                    for (int i = 0; i < array.length(); i++) set.add(array.getString(i)); edit.putStringSet(key, set);
                }
            }
            edit.apply();
        }
    }

    private static JSONObject encode(Object value) throws Exception {
        JSONObject encoded = new JSONObject();
        if (value instanceof String) return encoded.put("t", "s").put("v", value);
        if (value instanceof Boolean) return encoded.put("t", "b").put("v", value);
        if (value instanceof Integer) return encoded.put("t", "i").put("v", value);
        if (value instanceof Long) return encoded.put("t", "l").put("v", value);
        if (value instanceof Float) return encoded.put("t", "f").put("v", value);
        if (value instanceof Set) return encoded.put("t", "ss").put("v", new JSONArray((Set<?>) value));
        throw new IllegalArgumentException("Unsupported preference type");
    }

    private static AuthSessionStore.Session requireSession(Context context) {
        AuthSessionStore.Session session = AuthSessionStore.get(context);
        if (session == null) throw new IllegalStateException("No authenticated cloud account");
        return session;
    }

    private static HttpURLConnection open(String path, AuthSessionStore.Session session, String method) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(SupabaseConfig.url(path)).openConnection();
        connection.setConnectTimeout(15000); connection.setReadTimeout(20000); connection.setRequestMethod(method);
        connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY);
        connection.setRequestProperty("Authorization", "Bearer " + session.accessToken);
        connection.setRequestProperty("Content-Type", "application/json");
        return connection;
    }

    private static void write(HttpURLConnection connection, JSONObject body) throws Exception {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8); connection.setDoOutput(true); connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream output = connection.getOutputStream()) { output.write(bytes); }
    }
    private static String read(InputStream input) throws Exception {
        if (input == null) return ""; StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(input, StandardCharsets.UTF_8))) { String line; while ((line = reader.readLine()) != null) output.append(line); }
        return output.toString();
    }
    private CloudVaultStore() { }
}
