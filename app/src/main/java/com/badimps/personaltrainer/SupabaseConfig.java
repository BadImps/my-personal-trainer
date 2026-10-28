package com.badimps.personaltrainer;

/** Public client configuration. Never place a Supabase secret/service key in Android. */
public final class SupabaseConfig {
    public static boolean isConfigured() {
        return BuildConfig.SUPABASE_URL.startsWith("https://")
                && BuildConfig.SUPABASE_PUBLISHABLE_KEY.startsWith("sb_publishable_");
    }

    public static String url(String path) {
        return BuildConfig.SUPABASE_URL + path;
    }

    private SupabaseConfig() { }
}
