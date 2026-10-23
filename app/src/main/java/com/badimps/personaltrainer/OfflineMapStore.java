package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/** Owns optional, app-private Mapsforge country files. GPS routes never leave the phone. */
public final class OfflineMapStore {
    public static final String TRINIDAD_AND_TOBAGO = "Trinidad & Tobago";
    private static final String FILE = "offline_maps";
    private static final String KEY_TT_READY = "tt_ready";
    private static final String KEY_IMPORTED = "imported_maps";
    private static final String KEY_SELECTED = "selected_map";
    private static final String MAP_NAME = "trinidad-tobago.map";
    private static final String TT_ID = "trinidad-tobago";
    private static final long TT_BYTES = 9_972_071L;
    private static final String TT_SHA256 = "d8ea03c08cf99a8820c1c73519efeb3423792db98d763203ba65ddd6589375ec";
    private static final String TT_URL = "https://badimps.github.io/my-personal-trainer/offline/trinidad-tobago.map";

    public static final class MapRecord {
        public final String id, country;
        public final File file;
        public final long bytes;
        public final boolean verifiedDownload;
        MapRecord(String id, String country, File file, long bytes, boolean verifiedDownload) {
            this.id = id; this.country = country; this.file = file; this.bytes = bytes; this.verifiedDownload = verifiedDownload;
        }
    }
    public interface Listener { void onProgress(long downloaded, long total); void onReady(); void onError(String message); }

    public static boolean hasTrinidadAndTobago(Context context) { return prefs(context).getBoolean(KEY_TT_READY, false) && trinidadFile(context).length() == TT_BYTES; }
    /** Kept for legacy callers; returns the currently selected country when available. */
    public static File mapFile(Context context) { return preferredMapFile(context); }
    public static File preferredMapFile(Context context) { MapRecord selected = selectedMap(context); return selected == null ? trinidadFile(context) : selected.file; }
    public static boolean hasAnyMap(Context context) { return selectedMap(context) != null; }
    public static void selectMap(Context context, String id) { prefs(context).edit().putString(KEY_SELECTED, id).apply(); }

    public static MapRecord selectedMap(Context context) {
        String selected = prefs(context).getString(KEY_SELECTED, "");
        List<MapRecord> maps = installedMaps(context);
        for (MapRecord record : maps) if (record.id.equals(selected)) return record;
        return maps.isEmpty() ? null : maps.get(0);
    }

    public static List<MapRecord> installedMaps(Context context) {
        List<MapRecord> result = new ArrayList<>();
        if (hasTrinidadAndTobago(context)) result.add(new MapRecord(TT_ID, TRINIDAD_AND_TOBAGO, trinidadFile(context), TT_BYTES, true));
        String serialized = prefs(context).getString(KEY_IMPORTED, "");
        if (!serialized.isEmpty()) for (String row : serialized.split("\\n")) {
            String[] fields = row.split("\\|", -1); if (fields.length != 3) continue;
            File map = new File(folder(context), fields[2]);
            if (map.isFile() && map.length() > 0L) result.add(new MapRecord(fields[0], fields[1], map, map.length(), false));
        }
        return result;
    }

    /** Copies and validates a user supplied country package before making it available to routes. */
    public static MapRecord importMap(Context context, Uri source, String country) throws Exception {
        Context app = context.getApplicationContext(); String cleanName = cleanCountry(country); String id = "import-" + System.currentTimeMillis();
        File target = new File(folder(app), id + ".map"); File temporary = new File(folder(app), id + ".download");
        if (!folder(app).exists() && !folder(app).mkdirs()) throw new IllegalStateException("Could not create offline map storage.");
        try (InputStream input = app.getContentResolver().openInputStream(source); FileOutputStream output = new FileOutputStream(temporary)) {
            if (input == null) throw new IllegalStateException("Could not read that map file.");
            byte[] buffer = new byte[32 * 1024]; int count; long copied = 0L;
            while ((count = input.read(buffer)) != -1) { copied += count; if (copied > 1_200_000_000L) throw new IllegalStateException("That map is over the 1.2 GB on-device limit."); output.write(buffer, 0, count); }
        } catch (Exception error) { temporary.delete(); throw error; }
        try {
            if (!temporary.renameTo(target)) throw new IllegalStateException("Could not store that map file.");
            MapFile verifier = new MapFile(target); verifier.close();
            String existing = prefs(app).getString(KEY_IMPORTED, ""); String row = id + "|" + cleanName + "|" + target.getName();
            prefs(app).edit().putString(KEY_IMPORTED, existing.isEmpty() ? row : existing + "\n" + row).putString(KEY_SELECTED, id).apply();
            return new MapRecord(id, cleanName, target, target.length(), false);
        } catch (Exception error) { target.delete(); throw new IllegalStateException("That file is not a compatible offline country map."); }
    }

    public static void removeMap(Context context, MapRecord record) {
        if (TT_ID.equals(record.id)) { removeTrinidadAndTobago(context); return; }
        record.file.delete(); List<String> rows = new ArrayList<>();
        for (String row : prefs(context).getString(KEY_IMPORTED, "").split("\\n")) if (!row.startsWith(record.id + "|")) rows.add(row);
        StringBuilder joined = new StringBuilder(); for (String row : rows) if (!row.isEmpty()) { if (joined.length() > 0) joined.append('\n'); joined.append(row); }
        SharedPreferences.Editor edit = prefs(context).edit().putString(KEY_IMPORTED, joined.toString());
        if (record.id.equals(prefs(context).getString(KEY_SELECTED, ""))) edit.remove(KEY_SELECTED); edit.apply();
    }

    public static void downloadTrinidadAndTobago(Context context, Listener listener) {
        Context app = context.getApplicationContext(); new Thread(() -> {
            File destination = trinidadFile(app), parent = destination.getParentFile(), temporary = new File(parent, MAP_NAME + ".download");
            try {
                if (hasTrinidadAndTobago(app)) { listener.onReady(); return; }
                if (!parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Could not create offline map storage.");
                if (temporary.exists() && !temporary.delete()) throw new IllegalStateException("Could not replace an incomplete map download.");
                HttpURLConnection connection = (HttpURLConnection) new URL(TT_URL).openConnection(); connection.setConnectTimeout(20_000); connection.setReadTimeout(30_000); connection.setRequestProperty("Accept-Encoding", "identity"); connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IllegalStateException("Map download is unavailable right now.");
                long total = connection.getContentLengthLong() > 0L ? connection.getContentLengthLong() : TT_BYTES; MessageDigest digest = MessageDigest.getInstance("SHA-256"); long downloaded = 0L;
                try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[32 * 1024]; int count;
                    while ((count = input.read(buffer)) != -1) { output.write(buffer, 0, count); digest.update(buffer, 0, count); downloaded += count; listener.onProgress(downloaded, total); }
                } finally { connection.disconnect(); }
                if (downloaded != TT_BYTES || !TT_SHA256.equalsIgnoreCase(hex(digest.digest()))) throw new IllegalStateException("The offline map did not pass its integrity check.");
                if (destination.exists() && !destination.delete()) throw new IllegalStateException("Could not replace the previous offline map.");
                if (!temporary.renameTo(destination)) throw new IllegalStateException("Could not store the offline map.");
                prefs(app).edit().putBoolean(KEY_TT_READY, true).putString(KEY_SELECTED, TT_ID).apply();
                // The compact local map is usable immediately. In parallel, prepare the richer
                // MapLibre vector pack; it resumes safely if Android pauses the download.
                MapLibreOfflinePack.ensureTrinidadAndTobago(app);
                listener.onReady();
            } catch (Exception error) { temporary.delete(); listener.onError(error.getMessage() == null ? "Could not download the offline map." : error.getMessage()); }
        }, "offline-map-download").start();
    }

    public static void removeTrinidadAndTobago(Context context) { File map = trinidadFile(context); if (map.exists()) map.delete(); SharedPreferences.Editor edit = prefs(context).edit().remove(KEY_TT_READY); if (TT_ID.equals(prefs(context).getString(KEY_SELECTED, ""))) edit.remove(KEY_SELECTED); edit.apply(); }
    public static long trinidadAndTobagoBytes() { return TT_BYTES; }
    private static File trinidadFile(Context context) { return new File(folder(context), MAP_NAME); }
    private static File folder(Context context) { return new File(context.getFilesDir(), "offline-maps"); }
    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    private static String cleanCountry(String value) { String result = value == null ? "" : value.replace('|', ' ').replace('\n', ' ').trim(); return result.isEmpty() ? "Imported country map" : result; }
    private static String hex(byte[] bytes) { StringBuilder value = new StringBuilder(); for (byte b : bytes) value.append(String.format("%02x", b)); return value.toString(); }
    private OfflineMapStore() { }
}
