package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/** Downloads one audited, optional vector map. Map data never leaves the device after download. */
public final class OfflineMapStore {
    public static final String TRINIDAD_AND_TOBAGO = "Trinidad & Tobago";
    private static final String FILE = "offline_maps";
    private static final String KEY_TT_READY = "tt_ready";
    private static final String MAP_NAME = "trinidad-tobago.map";
    private static final long TT_BYTES = 9_972_071L;
    private static final String TT_SHA256 = "d8ea03c08cf99a8820c1c73519efeb3423792db98d763203ba65ddd6589375ec";
    private static final String TT_URL = "https://badimps.github.io/my-personal-trainer/offline/trinidad-tobago.map";

    public interface Listener {
        void onProgress(long downloaded, long total);
        void onReady();
        void onError(String message);
    }

    public static boolean hasTrinidadAndTobago(Context context) {
        return prefs(context).getBoolean(KEY_TT_READY, false) && mapFile(context).length() == TT_BYTES;
    }

    public static File mapFile(Context context) {
        File folder = new File(context.getFilesDir(), "offline-maps");
        return new File(folder, MAP_NAME);
    }

    public static void downloadTrinidadAndTobago(Context context, Listener listener) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            File destination = mapFile(app);
            File folder = destination.getParentFile();
            File temporary = new File(folder, MAP_NAME + ".download");
            try {
                if (hasTrinidadAndTobago(app)) { listener.onReady(); return; }
                if (!folder.exists() && !folder.mkdirs()) throw new IllegalStateException("Could not create offline map storage.");
                if (temporary.exists() && !temporary.delete()) throw new IllegalStateException("Could not replace an incomplete map download.");
                HttpURLConnection connection = (HttpURLConnection) new URL(TT_URL).openConnection();
                connection.setConnectTimeout(20_000);
                connection.setReadTimeout(30_000);
                connection.setRequestProperty("Accept-Encoding", "identity");
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IllegalStateException("Map download is unavailable right now.");
                long total = connection.getContentLengthLong() > 0L ? connection.getContentLengthLong() : TT_BYTES;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long downloaded = 0L;
                try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[32 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                        digest.update(buffer, 0, count);
                        downloaded += count;
                        listener.onProgress(downloaded, total);
                    }
                } finally { connection.disconnect(); }
                if (downloaded != TT_BYTES || !TT_SHA256.equalsIgnoreCase(hex(digest.digest()))) {
                    throw new IllegalStateException("The offline map did not pass its integrity check.");
                }
                if (destination.exists() && !destination.delete()) throw new IllegalStateException("Could not replace the previous offline map.");
                if (!temporary.renameTo(destination)) throw new IllegalStateException("Could not store the offline map.");
                prefs(app).edit().putBoolean(KEY_TT_READY, true).apply();
                listener.onReady();
            } catch (Exception error) {
                temporary.delete();
                listener.onError(error.getMessage() == null ? "Could not download the offline map." : error.getMessage());
            }
        }, "offline-map-download").start();
    }

    public static void removeTrinidadAndTobago(Context context) {
        File map = mapFile(context);
        if (map.exists()) map.delete();
        prefs(context).edit().remove(KEY_TT_READY).apply();
    }

    public static long trinidadAndTobagoBytes() { return TT_BYTES; }
    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    private static String hex(byte[] bytes) { StringBuilder value = new StringBuilder(); for (byte b : bytes) value.append(String.format("%02x", b)); return value.toString(); }
    private OfflineMapStore() { }
}
