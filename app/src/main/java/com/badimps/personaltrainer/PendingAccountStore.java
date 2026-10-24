package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

/** Registration details stay on the phone while Supabase verifies only the email identity. */
public final class PendingAccountStore {
    private static final String FILE = "pending_account";

    public static void save(Context context, String email, String username, String trainer) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putString("email", email == null ? "" : email.trim())
                .putString("username", username == null ? "" : username.trim())
                .putString("trainer", trainer == null ? "Female" : trainer)
                .apply();
    }

    public static boolean matches(Context context, String email) {
        return email != null && email.trim().equalsIgnoreCase(preferences(context).getString("email", ""));
    }

    public static String username(Context context) { return preferences(context).getString("username", "Athlete"); }
    public static String trainer(Context context) { return preferences(context).getString("trainer", "Female"); }
    public static void clear(Context context) { preferences(context).edit().clear().apply(); }
    private static SharedPreferences preferences(Context context) { return context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    private PendingAccountStore() { }
}
