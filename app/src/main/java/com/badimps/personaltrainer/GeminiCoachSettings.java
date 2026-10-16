package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

/** Stores an optional user-owned HTTPS Gemini gateway address; no Gemini key is kept in the app. */
public final class GeminiCoachSettings {
    private static final String FILE = "gemini_food_coach";

    public static String endpoint(Context context) {
        return preferences(context).getString("endpoint", "");
    }

    public static boolean sharesBodyContext(Context context) {
        return preferences(context).getBoolean("share_body_context", false);
    }

    public static boolean configured(Context context) {
        return !endpoint(context).isEmpty();
    }

    public static void save(Context context, String endpoint, boolean shareBodyContext) {
        String value = endpoint == null ? "" : endpoint.trim();
        Uri uri = Uri.parse(value);
        if (value.isEmpty() || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("Use a secure HTTPS service address");
        }
        preferences(context).edit().putString("endpoint", value)
                .putBoolean("share_body_context", shareBodyContext).apply();
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private GeminiCoachSettings() { }
}
