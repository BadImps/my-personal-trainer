package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Stores Supabase access credentials encrypted with a non-exportable Android Keystore key. */
public final class AuthSessionStore {
    private static final String FILE = "secure_auth_session";
    private static final String VALUE = "encrypted_session";
    private static final String ALIAS = "mpt_supabase_session_v1";

    public static final class Session {
        public final String accessToken, refreshToken, userId, email;
        Session(String accessToken, String refreshToken, String userId, String email) {
            this.accessToken = accessToken == null ? "" : accessToken;
            this.refreshToken = refreshToken == null ? "" : refreshToken;
            this.userId = userId == null ? "" : userId;
            this.email = email == null ? "" : email;
        }
        public boolean isUsable() { return !accessToken.isEmpty() && !refreshToken.isEmpty() && !userId.isEmpty(); }
    }

    public static void save(Context context, Session session) {
        try {
            JSONObject json = new JSONObject();
            json.put("access_token", session.accessToken);
            json.put("refresh_token", session.refreshToken);
            json.put("user_id", session.userId);
            json.put("email", session.email);
            byte[] plain = json.toString().getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] encrypted = cipher.doFinal(plain);
            byte[] iv = cipher.getIV();
            ByteBuffer buffer = ByteBuffer.allocate(4 + iv.length + encrypted.length);
            buffer.putInt(iv.length).put(iv).put(encrypted);
            preferences(context).edit().putString(VALUE, Base64.encodeToString(buffer.array(), Base64.NO_WRAP)).apply();
        } catch (Exception error) {
            clear(context);
            throw new IllegalStateException("Secure sign-in could not be saved on this phone.", error);
        }
    }

    public static Session load(Context context) {
        String encoded = preferences(context).getString(VALUE, "");
        if (encoded == null || encoded.isEmpty()) return null;
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.decode(encoded, Base64.NO_WRAP));
            int ivLength = buffer.getInt();
            if (ivLength < 12 || ivLength > 32 || buffer.remaining() <= ivLength) throw new IllegalStateException("Invalid session envelope");
            byte[] iv = new byte[ivLength]; buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()]; buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            JSONObject json = new JSONObject(new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8));
            Session session = new Session(json.optString("access_token"), json.optString("refresh_token"), json.optString("user_id"), json.optString("email"));
            return session.isUsable() ? session : null;
        } catch (Exception ignored) {
            clear(context);
            return null;
        }
    }

    public static void clear(Context context) { preferences(context).edit().clear().apply(); }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        java.security.Key existing = store.getKey(ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }

    private static SharedPreferences preferences(Context context) { return context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    private AuthSessionStore() { }
}
