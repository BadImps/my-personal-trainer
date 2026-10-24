package com.badimps.personaltrainer;

import android.net.Uri;

/** Validates the public mobile-client configuration before any network request is attempted. */
public final class SupabaseConfig {
    public static boolean isConfigured() {
        return isHttpsProjectUrl(BuildConfig.SUPABASE_URL)
                && BuildConfig.SUPABASE_ANON_KEY != null
                && BuildConfig.SUPABASE_ANON_KEY.trim().length() > 20;
    }

    public static boolean isHttpsProjectUrl(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        Uri uri = Uri.parse(value.trim());
        return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
    }

    public static String authEndpoint(String path) {
        String base = BuildConfig.SUPABASE_URL.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/auth/v1" + path;
    }

    private SupabaseConfig() { }
}
