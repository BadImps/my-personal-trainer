package com.badimps.personaltrainer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Optional, verified coach-motion library. The core app stays useful offline with its native
 * motion guide; a downloaded pack adds high-quality looping MP4 demonstrations. */
public final class MediaPackManager {
    public static final String CATALOG_URL = "https://badimps.github.io/my-personal-trainer/media-packs.json";
    private static final String ROOT = "coach-motion";

    public static final class Pack {
        public final String id, label, coach, version, url, sha;
        public final long sizeBytes;
        Pack(String id, String label, String coach, String version, String url, String sha, long sizeBytes) {
            this.id = id; this.label = label; this.coach = coach; this.version = version;
            this.url = url; this.sha = sha; this.sizeBytes = sizeBytes;
        }
    }

    public interface Progress { void onProgress(long complete, long total); }

    public static Pack[] fetchCatalog() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(CATALOG_URL).openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "MyPersonalTrainer/" + BuildConfig.VERSION_NAME);
        try (InputStream input = connection.getInputStream(); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) throw new IllegalStateException("Media catalog is unavailable");
            byte[] buffer = new byte[8192];
            for (int count; (count = input.read(buffer)) != -1;) output.write(buffer, 0, count);
            JSONArray entries = new JSONObject(output.toString("UTF-8")).getJSONArray("packs");
            Pack[] packs = new Pack[entries.length()];
            for (int index = 0; index < entries.length(); index++) {
                JSONObject entry = entries.getJSONObject(index);
                packs[index] = new Pack(entry.getString("id"), entry.getString("label"), entry.getString("coach"),
                        entry.getString("version"), entry.getString("url"), entry.getString("sha256").toLowerCase(Locale.US), entry.getLong("sizeBytes"));
            }
            return packs;
        } finally { connection.disconnect(); }
    }

    public static boolean isInstalled(Context context, Pack pack) {
        return marker(context, pack).isFile();
    }

    public static File localLoop(Context context, String trainer, String exerciseName) {
        if (!ExerciseVideos.hasDedicatedLoop(exerciseName)) return null;
        String coach = "Male".equals(trainer) ? "leo" : "mia";
        File installed = findInstalledCoachPack(context, coach);
        if (installed == null) return null;
        File loop = new File(installed, coach + "_" + ExerciseVisuals.resourceStem(exerciseName) + ".mp4");
        return loop.isFile() && loop.length() > 0L ? loop : null;
    }

    /** Downloads to private temporary storage, verifies the archive, then atomically expands only
     * safe relative MP4 paths. A failed download never replaces a working installed pack. */
    public static void downloadAndInstall(Context context, Pack pack, Progress progress) throws Exception {
        validate(pack);
        File temp = new File(context.getCacheDir(), pack.id + ".download");
        File staging = new File(context.getCacheDir(), pack.id + ".staging");
        deleteRecursively(staging);
        if (!staging.mkdirs()) throw new IllegalStateException("Could not prepare media storage");
        HttpURLConnection connection = (HttpURLConnection) new URL(pack.url).openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(60_000);
        try {
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) throw new IllegalStateException("Media download is unavailable");
            long total = connection.getContentLengthLong() > 0L ? connection.getContentLengthLong() : pack.sizeBytes;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new BufferedInputStream(connection.getInputStream()); FileOutputStream output = new FileOutputStream(temp)) {
                byte[] buffer = new byte[64 * 1024]; long written = 0L;
                for (int count; (count = input.read(buffer)) != -1;) { output.write(buffer, 0, count); digest.update(buffer, 0, count); written += count; if (progress != null) progress.onProgress(written, total); }
            }
            if (pack.sizeBytes > 0L && temp.length() != pack.sizeBytes) throw new SecurityException("Media pack size did not match");
            if (!hex(digest.digest()).equalsIgnoreCase(pack.sha)) throw new SecurityException("Media pack verification failed");
            unzipSafely(temp, staging);
            File destination = new File(root(context), pack.id);
            deleteRecursively(destination);
            if (!staging.renameTo(destination)) throw new IllegalStateException("Could not install media pack");
            marker(context, pack).getParentFile().mkdirs();
            if (!marker(context, pack).createNewFile()) throw new IllegalStateException("Could not finish media-pack install");
        } finally {
            connection.disconnect();
            if (temp.exists()) temp.delete();
            deleteRecursively(staging);
        }
    }

    public static void remove(Context context, Pack pack) { deleteRecursively(new File(root(context), pack.id)); File mark = marker(context, pack); if (mark.exists()) mark.delete(); }
    public static boolean isMetered(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = manager == null ? null : manager.getActiveNetwork();
        NetworkCapabilities caps = manager == null || network == null ? null : manager.getNetworkCapabilities(network);
        return caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
    }
    private static void validate(Pack pack) {
        if (pack == null || !pack.id.matches("[a-z0-9_-]+") || !"https".equalsIgnoreCase(android.net.Uri.parse(pack.url).getScheme()) || !pack.sha.matches("(?i)[0-9a-f]{64}") || pack.sizeBytes <= 0L) throw new IllegalArgumentException("Invalid media pack");
    }
    private static File root(Context context) { File root = new File(context.getFilesDir(), ROOT); if (!root.exists()) root.mkdirs(); return root; }
    private static File findInstalledCoachPack(Context context, String coach) {
        File marks = new File(root(context), ".installed");
        File[] candidates = marks.listFiles((dir, name) -> name.startsWith(coach + "-motion-") && name.length() > (coach + "-motion-").length());
        if (candidates == null || candidates.length == 0) return null;
        String name = candidates[0].getName();
        int finalDash = name.lastIndexOf('-');
        if (finalDash <= 0) return null;
        return new File(root(context), name.substring(0, finalDash));
    }
    private static File marker(Context context, Pack pack) { return new File(new File(root(context), ".installed"), pack.id + "-" + pack.version); }
    private static void unzipSafely(File archive, File destination) throws Exception {
        String base = destination.getCanonicalPath() + File.separator;
        try (ZipInputStream input = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            for (ZipEntry entry; (entry = input.getNextEntry()) != null;) {
                if (entry.isDirectory() || !entry.getName().matches("(mia|leo)_[a-z0-9_]+\\.mp4")) continue;
                File output = new File(destination, entry.getName());
                if (!output.getCanonicalPath().startsWith(base)) throw new SecurityException("Unsafe media path");
                try (FileOutputStream stream = new FileOutputStream(output)) { byte[] buffer = new byte[64 * 1024]; for (int count; (count = input.read(buffer)) != -1;) stream.write(buffer, 0, count); }
            }
        }
    }
    private static void deleteRecursively(File file) { if (file == null || !file.exists()) return; if (file.isDirectory()) { File[] children = file.listFiles(); if (children != null) for (File child : children) deleteRecursively(child); } file.delete(); }
    private static String hex(byte[] bytes) { StringBuilder result = new StringBuilder(); for (byte value : bytes) result.append(String.format(Locale.US, "%02x", value)); return result.toString(); }
    private MediaPackManager() { }
}
