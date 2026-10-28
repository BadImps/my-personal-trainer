package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Stores only the renewable Supabase session, encrypted with an Android Keystore key. */
public final class AuthSessionStore {
    private static final String FILE = "supabase_auth_session";
    private static final String KEY_ALIAS = "my_personal_trainer_vault_key_v1";

    public static final class Session {
        public final String accessToken;
        public final String refreshToken;
        public final String userId;
        Session(String accessToken, String refreshToken, String userId) {
            this.accessToken = accessToken; this.refreshToken = refreshToken; this.userId = userId;
        }
    }

    public static void save(Context context, Session session) {
        if (session == null || empty(session.accessToken) || empty(session.userId)) return;
        prefs(context).edit().putString("access", encrypt(session.accessToken))
                .putString("refresh", encrypt(session.refreshToken == null ? "" : session.refreshToken))
                .putString("user", encrypt(session.userId)).apply();
    }

    public static Session get(Context context) {
        try {
            String access = decrypt(prefs(context).getString("access", ""));
            String user = decrypt(prefs(context).getString("user", ""));
            if (empty(access) || empty(user)) return null;
            return new Session(access, decrypt(prefs(context).getString("refresh", "")), user);
        } catch (Exception ignored) { clear(context); return null; }
    }

    public static void clear(Context context) { prefs(context).edit().clear().apply(); }
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    private static boolean empty(String value) { return value == null || value.isEmpty(); }

    private static String encrypt(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(iv, Base64.NO_WRAP) + "." + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception error) { throw new IllegalStateException("Could not protect sign-in session", error); }
    }

    private static String decrypt(String value) throws Exception {
        if (value == null || value.isEmpty()) return "";
        String[] parts = value.split("\\.", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid encrypted session");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (!store.containsAlias(KEY_ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            generator.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) store.getEntry(KEY_ALIAS, null)).getSecretKey();
    }
    private AuthSessionStore() { }
}
