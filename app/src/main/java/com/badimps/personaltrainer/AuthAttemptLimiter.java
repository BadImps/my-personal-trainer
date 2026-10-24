package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Device-side friction that complements Supabase's authoritative server rate limits. */
public final class AuthAttemptLimiter {
    public static final String LOGIN = "login";
    public static final String RECOVERY_CODE = "recovery_code";
    public static final String RECOVERY_SEND = "recovery_send";
    private static final String FILE = "auth_attempt_limits";
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MILLIS = 15 * 60 * 1000L;
    private static final long RESEND_MILLIS = 60 * 1000L;

    public static boolean allowed(Context context, String action, String account) {
        return remainingMillis(context, action, account) <= 0L;
    }

    public static long remainingMillis(Context context, String action, String account) {
        long until = prefs(context).getLong(key(action, account) + "_until", 0L);
        return Math.max(0L, until - System.currentTimeMillis());
    }

    public static void recordFailure(Context context, String action, String account) {
        SharedPreferences preferences = prefs(context);
        String key = key(action, account);
        int failures = preferences.getInt(key + "_failures", 0) + 1;
        SharedPreferences.Editor edit = preferences.edit().putInt(key + "_failures", failures);
        if (failures >= MAX_FAILURES) edit.putLong(key + "_until", System.currentTimeMillis() + LOCK_MILLIS);
        edit.apply();
    }

    public static void recordRecoverySend(Context context, String account) {
        prefs(context).edit().putLong(key(RECOVERY_SEND, account) + "_until",
                System.currentTimeMillis() + RESEND_MILLIS).apply();
    }

    public static void clear(Context context, String action, String account) {
        String key = key(action, account);
        prefs(context).edit().remove(key + "_failures").remove(key + "_until").apply();
    }

    public static String waitMessage(Context context, String action, String account) {
        long seconds = Math.max(1L, (remainingMillis(context, action, account) + 999L) / 1000L);
        if (seconds >= 60L) return "Please wait " + ((seconds + 59L) / 60L) + " minutes before trying again.";
        return "Please wait " + seconds + " seconds before trying again.";
    }

    private static String key(String action, String account) {
        String normalized = account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
        return action + "_" + digest(normalized);
    }

    private static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : bytes) result.append(String.format(Locale.ROOT, "%02x", item));
            return result.toString();
        } catch (Exception ignored) { return Integer.toHexString(value.hashCode()); }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private AuthAttemptLimiter() { }
}
