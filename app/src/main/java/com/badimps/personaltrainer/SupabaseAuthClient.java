package com.badimps.personaltrainer;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Minimal, dependency-free Supabase Auth client. Network calls must run off the UI thread. */
public final class SupabaseAuthClient {
    public static final class Result {
        public final AuthSessionStore.Session session;
        public final String error;
        public final boolean confirmationPending;
        public final String username;
        public final String trainer;
        Result(AuthSessionStore.Session session, String error, boolean confirmationPending, String username, String trainer) {
            this.session = session; this.error = error; this.confirmationPending = confirmationPending;
            this.username = username; this.trainer = trainer;
        }
        public boolean isSuccess() { return session != null; }
    }

    public static Result signUp(String email, String password, String username, String trainer) {
        try {
            JSONObject metadata = new JSONObject().put("username", username.trim()).put("trainer", trainer);
            return request("/auth/v1/signup", email, password, metadata);
        } catch (Exception ignored) {
            return new Result(null, "Account details could not be prepared.", false, "", "Female");
        }
    }
    public static Result signIn(String email, String password) { return request("/auth/v1/token?grant_type=password", email, password, null); }

    private static Result request(String path, String email, String password, JSONObject metadata) {
        if (!SupabaseConfig.isConfigured()) return new Result(null, "Cloud backup is not configured in this build.", false, "", "Female");
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(SupabaseConfig.url(path)).openConnection();
            connection.setConnectTimeout(15000); connection.setReadTimeout(20000);
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            JSONObject request = new JSONObject().put("email", email.trim()).put("password", password);
            if (metadata != null) request.put("data", metadata);
            byte[] body = request.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream stream = connection.getOutputStream()) { stream.write(body); }
            int status = connection.getResponseCode();
            JSONObject response = new JSONObject(read(status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream()));
            if (status < 200 || status >= 300) return new Result(null, response.optString("msg", response.optString("error_description", "Sign-in was not completed.")), false, "", "Female");
            JSONObject user = response.optJSONObject("user");
            String access = response.optString("access_token", "");
            String userId = user == null ? "" : user.optString("id", "");
            JSONObject userMetadata = user == null ? null : user.optJSONObject("user_metadata");
            String username = userMetadata == null ? "Athlete" : userMetadata.optString("username", "Athlete");
            String trainer = userMetadata == null ? "Female" : userMetadata.optString("trainer", "Female");
            if (access.isEmpty() && !userId.isEmpty()) return new Result(null, null, true, username, trainer);
            if (access.isEmpty() || userId.isEmpty()) return new Result(null, "Sign-in was not completed.", false, username, trainer);
            return new Result(new AuthSessionStore.Session(access, response.optString("refresh_token", ""), userId), null, false, username, trainer);
        } catch (Exception error) { return new Result(null, "Could not reach secure account service. Check your connection and try again.", false, "", "Female"); }
        finally { if (connection != null) connection.disconnect(); }
    }

    private static String read(InputStream input) throws Exception {
        if (input == null) return "{}";
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) value.append(line);
        }
        return value.length() == 0 ? "{}" : value.toString();
    }
    private SupabaseAuthClient() { }
}
