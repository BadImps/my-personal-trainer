package com.badimps.personaltrainer;

import org.json.JSONObject;

import android.net.Uri;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small, dependency-free Supabase GoTrue client. It never receives a service-role key. */
public final class SupabaseAuthClient {
    public interface Callback<T> { void complete(T value, String error); }
    public static final class AuthResult {
        public final String accessToken, refreshToken, userId, email;
        AuthResult(String accessToken, String refreshToken, String userId, String email) {
            this.accessToken = accessToken; this.refreshToken = refreshToken; this.userId = userId; this.email = email;
        }
        AuthSessionStore.Session session() { return new AuthSessionStore.Session(accessToken, refreshToken, userId, email); }
    }

    private static final ExecutorService NETWORK = Executors.newSingleThreadExecutor();

    /**
     * Creates the Supabase Auth account. With email confirmation disabled, Supabase returns a
     * usable session immediately; callers must save it so the new member is recognised by the
     * Gym Lobby without an unnecessary second sign-in.
     */
    public static void signUp(String email, String password, Callback<AuthResult> callback) {
        JSONObject body = new JSONObject();
        put(body, "email", normalizeEmail(email)); put(body, "password", password);
        request("POST", "/signup?redirect_to=com.badimps.personaltrainer%3A%2F%2Fauth%2Fverified", body, null,
                (json, error) -> {
                    if (error != null) { callback.complete(null, friendly(error)); return; }
                    callback.complete(authResult(json, normalizeEmail(email)), null);
                });
    }

    public static void signIn(String email, String password, Callback<AuthResult> callback) {
        JSONObject body = new JSONObject();
        put(body, "email", normalizeEmail(email)); put(body, "password", password);
        request("POST", "/token?grant_type=password", body, null, (json, error) -> {
            if (error != null) { callback.complete(null, friendly(error)); return; }
            callback.complete(authResult(json, normalizeEmail(email)), null);
        });
    }

    public static void requestPasswordRecovery(String email, Callback<Boolean> callback) {
        JSONObject body = new JSONObject(); put(body, "email", normalizeEmail(email));
        request("POST", "/recover", body, null, (json, error) -> callback.complete(error == null, error == null ? null : friendly(error)));
    }

    public static void verifyRecoveryCode(String email, String token, Callback<AuthResult> callback) {
        JSONObject body = new JSONObject();
        put(body, "email", normalizeEmail(email)); put(body, "token", token); put(body, "type", "recovery");
        request("POST", "/verify", body, null, (json, error) -> {
            if (error != null) { callback.complete(null, friendly(error)); return; }
            callback.complete(authResult(json, normalizeEmail(email)), null);
        });
    }

    public static void updatePassword(String accessToken, String password, Callback<Boolean> callback) {
        JSONObject body = new JSONObject(); put(body, "password", password);
        request("PUT", "/user", body, accessToken, (json, error) -> callback.complete(error == null, error == null ? null : friendly(error)));
    }

    public static void refresh(String refreshToken, Callback<AuthResult> callback) {
        JSONObject body = new JSONObject(); put(body, "refresh_token", refreshToken);
        request("POST", "/token?grant_type=refresh_token", body, null, (json, error) -> {
            if (error != null) { callback.complete(null, friendly(error)); return; }
            callback.complete(authResult(json, ""), null);
        });
    }

    public static void signOut(String accessToken, Callback<Boolean> callback) {
        request("POST", "/logout", new JSONObject(), accessToken, (json, error) -> callback.complete(error == null, error));
    }

    /** Reads the verified Supabase session returned through the app's private auth deep link. */
    public static AuthResult fromRedirect(Uri uri) {
        if (uri == null || !"com.badimps.personaltrainer".equalsIgnoreCase(uri.getScheme())
                || !"auth".equalsIgnoreCase(uri.getHost())) return null;
        String fragment = uri.getFragment();
        if (fragment == null || fragment.isEmpty()) return null;
        Uri values = Uri.parse("https://local.invalid/?" + fragment);
        String access = values.getQueryParameter("access_token");
        String refresh = values.getQueryParameter("refresh_token");
        if (access == null || refresh == null) return null;
        String id = "", email = "";
        try {
            String[] parts = access.split("\\.");
            if (parts.length > 1) {
                JSONObject claims = new JSONObject(new String(Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING), StandardCharsets.UTF_8));
                id = claims.optString("sub", ""); email = claims.optString("email", "");
            }
        } catch (Exception ignored) { }
        return id.isEmpty() ? null : new AuthResult(access, refresh, id, email);
    }

    private interface JsonCallback { void complete(JSONObject json, String error); }
    private static void request(String method, String path, JSONObject body, String accessToken, JsonCallback callback) {
        if (!SupabaseConfig.isConfigured()) { callback.complete(null, "Secure account service is not configured yet."); return; }
        NETWORK.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(SupabaseConfig.authEndpoint(path));
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(15_000); connection.setReadTimeout(20_000);
                connection.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + (accessToken == null || accessToken.isEmpty() ? BuildConfig.SUPABASE_ANON_KEY : accessToken));
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(payload.length);
                try (OutputStream output = connection.getOutputStream()) { output.write(payload); }
                int status = connection.getResponseCode();
                String response = read(status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream());
                JSONObject json = response.isEmpty() ? new JSONObject() : new JSONObject(response);
                if (status >= 200 && status < 300) callback.complete(json, null);
                else callback.complete(null, errorMessage(json));
            } catch (Exception error) {
                callback.complete(null, "Unable to reach the secure account service. Check your connection and try again.");
            } finally { if (connection != null) connection.disconnect(); }
        });
    }

    private static AuthResult authResult(JSONObject json, String fallbackEmail) {
        JSONObject user = json.optJSONObject("user");
        String email = user == null ? fallbackEmail : user.optString("email", fallbackEmail);
        String id = user == null ? "" : user.optString("id", "");
        return new AuthResult(json.optString("access_token", ""), json.optString("refresh_token", ""), id, email);
    }
    private static String errorMessage(JSONObject json) {
        String value = json.optString("msg", json.optString("message", json.optString("error_description", json.optString("error", "Request could not be completed."))));
        return friendly(value);
    }
    private static String friendly(String value) {
        if (value == null) return "Request could not be completed.";
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("email not confirmed")) return "Verify your email before signing in.";
        if (lower.contains("invalid login")) return "That email or password is not correct.";
        if (lower.contains("already registered") || lower.contains("already been registered")) return "An account already exists for this email.";
        if (lower.contains("expired") || lower.contains("invalid token")) return "That verification code is invalid or has expired.";
        if (lower.contains("rate") || lower.contains("too many")) return "Too many attempts. Please wait before trying again.";
        return "The secure account request could not be completed. Please try again.";
    }
    private static String read(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) value.append(line);
        }
        return value.toString();
    }
    private static String normalizeEmail(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static void put(JSONObject json, String key, Object value) { try { json.put(key, value); } catch (Exception ignored) { } }
    private SupabaseAuthClient() { }
}
