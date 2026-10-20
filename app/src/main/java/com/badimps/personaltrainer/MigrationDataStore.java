package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** User-selected, local export/import used for a device move or the one-time signing migration.
 * It intentionally excludes downloaded maps and coach-video packs: they are verified downloads
 * that can be selected again after restore. */
public final class MigrationDataStore {
    private static final String ENTRY = "my-personal-trainer-backup.json";
    private static final int SCHEMA = 1;
    private static final String[] PREFERENCES = {
            "trainer_profile", "appearance", "body_profile", "coach_voice_labels", "calorie_target",
            "endurance_history", "endurance_routes", "live_endurance_tracking", "journey_nexus",
            "meal_plan", "nutrition_region", "saved_workout_plan", "completed_set_times",
            "endurance_appointments", "weekly_schedule_drafts", "weight_trend"
    };

    public static void exportTo(Context context, Uri destination) throws Exception {
        JSONObject backup = new JSONObject();
        backup.put("schema", SCHEMA);
        backup.put("applicationId", BuildConfig.APPLICATION_ID);
        backup.put("createdAt", System.currentTimeMillis());
        JSONObject preferences = new JSONObject();
        for (String file : PREFERENCES) preferences.put(file, encode(context.getSharedPreferences(file, Context.MODE_PRIVATE).getAll()));
        backup.put("preferences", preferences);
        OutputStream rawStream = context.getContentResolver().openOutputStream(destination, "w");
        if (rawStream == null) throw new IllegalStateException("Could not open the selected backup location");
        try (OutputStream stream = rawStream; ZipOutputStream zip = new ZipOutputStream(stream)) {
            zip.putNextEntry(new ZipEntry(ENTRY));
            zip.write(backup.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    public static void importFrom(Context context, Uri source) throws Exception {
        JSONObject backup = readBackup(context, source);
        if (backup.getInt("schema") != SCHEMA || !BuildConfig.APPLICATION_ID.equals(backup.getString("applicationId"))) {
            throw new SecurityException("This is not a My Personal Trainer backup");
        }
        JSONObject preferences = backup.getJSONObject("preferences");
        for (String file : PREFERENCES) {
            if (!preferences.has(file)) continue;
            SharedPreferences.Editor editor = context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear();
            decodeInto(editor, preferences.getJSONObject(file));
            if (!editor.commit()) throw new IllegalStateException("Could not restore " + file);
        }
    }

    private static JSONObject readBackup(Context context, Uri source) throws Exception {
        InputStream rawStream = context.getContentResolver().openInputStream(source);
        if (rawStream == null) throw new IllegalArgumentException("Could not open the selected backup file");
        try (InputStream stream = rawStream; ZipInputStream zip = new ZipInputStream(stream)) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                if (!ENTRY.equals(entry.getName())) continue;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                for (int count; (count = zip.read(buffer)) != -1;) output.write(buffer, 0, count);
                return new JSONObject(output.toString("UTF-8"));
            }
        }
        throw new IllegalArgumentException("Backup file has no trainer data");
    }

    private static JSONObject encode(Map<String, ?> values) throws Exception {
        JSONObject result = new JSONObject();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object value = entry.getValue(); JSONObject item = new JSONObject();
            if (value instanceof Boolean) { item.put("type", "boolean"); item.put("value", (Boolean) value); }
            else if (value instanceof Integer) { item.put("type", "int"); item.put("value", (Integer) value); }
            else if (value instanceof Long) { item.put("type", "long"); item.put("value", (Long) value); }
            else if (value instanceof Float) { item.put("type", "float"); item.put("value", ((Float) value).doubleValue()); }
            else if (value instanceof Set) { item.put("type", "set"); item.put("value", new JSONArray((Set<?>) value)); }
            else { item.put("type", "string"); item.put("value", String.valueOf(value)); }
            result.put(entry.getKey(), item);
        }
        return result;
    }

    private static void decodeInto(SharedPreferences.Editor editor, JSONObject values) throws Exception {
        java.util.Iterator<String> keys = values.keys();
        while (keys.hasNext()) {
            String key = keys.next(); JSONObject item = values.getJSONObject(key); String type = item.getString("type");
            if ("boolean".equals(type)) editor.putBoolean(key, item.getBoolean("value"));
            else if ("int".equals(type)) editor.putInt(key, item.getInt("value"));
            else if ("long".equals(type)) editor.putLong(key, item.getLong("value"));
            else if ("float".equals(type)) editor.putFloat(key, (float) item.getDouble("value"));
            else if ("set".equals(type)) { JSONArray array = item.getJSONArray("value"); Set<String> set = new HashSet<>(); for (int index = 0; index < array.length(); index++) set.add(array.getString(index)); editor.putStringSet(key, set); }
            else editor.putString(key, item.optString("value", ""));
        }
    }

    private MigrationDataStore() { }
}
