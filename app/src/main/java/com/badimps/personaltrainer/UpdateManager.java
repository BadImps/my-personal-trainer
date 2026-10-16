package com.badimps.personaltrainer;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Durable, direct-APK update delivery for installations outside Google Play.
 *
 * <p>The platform DownloadManager owns the transfer so it can survive a brief cellular loss,
 * app backgrounding, and process recreation. This class never treats a completed download as an
 * update until the published SHA-256 is checked immediately before the Android installer opens.</p>
 */
public final class UpdateManager {
    public static final String ENDPOINT = "https://badimps.github.io/my-personal-trainer/latest.json";
    private static final String PREFS = "ota_download_state";
    private static final String KEY_ID = "download_id";
    private static final String KEY_CODE = "version_code";
    private static final String KEY_SHA = "sha256";
    private static final String KEY_NAME = "version_name";
    private static final String KEY_URL = "apk_url";
    private static final String KEY_SIZE = "apk_size";
    private static final String KEY_READY = "verified_ready";
    private static final String KEY_PERMISSION_PENDING = "installer_permission_pending";
    private static final String DIRECTORY = "ota";

    public static final int STATE_NONE = 0;
    public static final int STATE_QUEUED = 1;
    public static final int STATE_DOWNLOADING = 2;
    public static final int STATE_PAUSED = 3;
    public static final int STATE_VERIFYING = 4;
    public static final int STATE_READY_TO_INSTALL = 5;
    public static final int STATE_FAILED = 6;

    public static final class Info {
        public final int code;
        public final String name;
        public final String url;
        public final String sha;
        public final long sizeBytes;

        Info(int code, String name, String url, String sha, long sizeBytes) {
            this.code = code;
            this.name = name;
            this.url = url;
            this.sha = sha;
            this.sizeBytes = sizeBytes;
        }
    }

    public static final class DownloadStatus {
        public final int state;
        public final long downloadedBytes;
        public final long totalBytes;
        public final String detail;

        DownloadStatus(int state, long downloadedBytes, long totalBytes, String detail) {
            this.state = state;
            this.downloadedBytes = Math.max(0L, downloadedBytes);
            this.totalBytes = totalBytes;
            this.detail = detail == null ? "" : detail;
        }

        public boolean active() {
            return state == STATE_QUEUED || state == STATE_DOWNLOADING || state == STATE_PAUSED || state == STATE_VERIFYING;
        }
    }

    /** Checks the HTTPS manifest and returns a newer compatible package, if one exists. */
    public static Info check() throws Exception {
        JSONObject json = new JSONObject(read(ENDPOINT));
        String applicationId = json.optString("applicationId", BuildConfig.APPLICATION_ID);
        if (!BuildConfig.APPLICATION_ID.equals(applicationId)) throw new SecurityException("Update is for a different app");
        int code = json.getInt("versionCode");
        Info info = new Info(code, json.getString("versionName"), json.getString("apkUrl"),
                json.getString("sha256").toLowerCase(Locale.US), json.optLong("sizeBytes", -1L));
        validate(info);
        return info.code > BuildConfig.VERSION_CODE ? info : null;
    }

    /** Starts a durable system download. Mobile data must be explicitly allowed by the caller. */
    public static long enqueue(Context context, Info info, boolean allowMetered) {
        validate(info);
        DownloadStatus existing = status(context, info);
        // A Wi-Fi-only request paused by Android needs a fresh request when the person explicitly
        // changes their mind and permits mobile data.
        if (existing.active() && !(allowMetered && existing.state == STATE_PAUSED)) return savedId(context);
        long freeBytes = availableBytes(context);
        if (freeBytes > 0L && info.sizeBytes > 0L && freeBytes < requiredBytes(info.sizeBytes)) {
            throw new IllegalStateException("Free up at least " + readableBytes(requiredBytes(info.sizeBytes)) + " before downloading this update");
        }

        cancelDifferentOrFinishedDownload(context, info);
        File destination = downloadFile(context, info);
        File parent = destination.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Could not prepare update storage");
        if (destination.exists() && !destination.delete()) throw new IllegalStateException("Could not replace the previous update download");

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(info.url));
        request.setTitle("My Personal Trainer " + info.name);
        request.setDescription("Downloading verified update");
        request.setMimeType("application/vnd.android.package-archive");
        request.setAllowedOverMetered(allowMetered);
        request.setAllowedOverRoaming(false);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, DIRECTORY + "/" + destination.getName());
        long id = downloads(context).enqueue(request);
        preferences(context).edit()
                .putLong(KEY_ID, id)
                .putInt(KEY_CODE, info.code)
                .putString(KEY_SHA, info.sha)
                .putString(KEY_NAME, info.name)
                .putString(KEY_URL, info.url)
                .putLong(KEY_SIZE, info.sizeBytes)
                .putBoolean(KEY_READY, false)
                .putBoolean(KEY_PERMISSION_PENDING, false)
                .apply();
        return id;
    }

    /** Restores a persisted transfer after a process restart or a transient manifest failure. */
    public static Info savedInfo(Context context) {
        SharedPreferences prefs = preferences(context);
        Info info = new Info(prefs.getInt(KEY_CODE, -1), prefs.getString(KEY_NAME, ""),
                prefs.getString(KEY_URL, ""), prefs.getString(KEY_SHA, ""), prefs.getLong(KEY_SIZE, -1L));
        try {
            validate(info);
            return info;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** Restores an install handoff if Android recreated the app while Settings was open. */
    public static Info pendingInstallInfo(Context context) {
        if (!preferences(context).getBoolean(KEY_PERMISSION_PENDING, false)) return null;
        return savedInfo(context);
    }

    /** Returns persisted platform-download state so the screen can be restored after a relaunch. */
    public static DownloadStatus status(Context context, Info info) {
        if (!matchesSavedInfo(context, info)) return new DownloadStatus(STATE_NONE, 0L, info.sizeBytes, "");
        if (preferences(context).getBoolean(KEY_READY, false) && downloadFile(context, info).isFile()) {
            long downloaded = info.sizeBytes > 0L ? info.sizeBytes : downloadFile(context, info).length();
            return new DownloadStatus(STATE_READY_TO_INSTALL, downloaded, info.sizeBytes, "Verified and ready to install");
        }
        long id = savedId(context);
        if (id <= 0L) return new DownloadStatus(STATE_NONE, 0L, info.sizeBytes, "");
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
        try (Cursor cursor = downloads(context).query(query)) {
            if (cursor == null || !cursor.moveToFirst()) return new DownloadStatus(STATE_FAILED, 0L, info.sizeBytes, "Android no longer has this download. Please retry.");
            long downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            long total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            if (total <= 0L) total = info.sizeBytes;
            int state = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
            if (state == DownloadManager.STATUS_PENDING) return new DownloadStatus(STATE_QUEUED, downloaded, total, "Queued by Android");
            if (state == DownloadManager.STATUS_RUNNING) return new DownloadStatus(STATE_DOWNLOADING, downloaded, total, "Downloading securely");
            if (state == DownloadManager.STATUS_PAUSED) return new DownloadStatus(STATE_PAUSED, downloaded, total, pausedReason(reason));
            if (state == DownloadManager.STATUS_SUCCESSFUL) return new DownloadStatus(STATE_VERIFYING, downloaded, total, "Download finished — verify before installing");
            return new DownloadStatus(STATE_FAILED, downloaded, total, failedReason(reason));
        }
    }

    /** Re-hashes the completed file and records that exact verified artifact is ready to install. */
    public static void verifyDownloaded(Context context, Info info) throws Exception {
        validate(info);
        File apk = downloadFile(context, info);
        if (!apk.isFile()) throw new IllegalStateException("The downloaded update file is missing. Please retry.");
        if (info.sizeBytes > 0L && apk.length() != info.sizeBytes) {
            throw new SecurityException("Download size did not match the published update");
        }
        if (!sha(apk).equalsIgnoreCase(info.sha)) throw new SecurityException("Downloaded update did not match its published SHA-256");
        preferences(context).edit().putBoolean(KEY_READY, true).apply();
    }

    /**
     * Opens Android's installer for a previously verified package. Returns false only when the
     * person must first grant the normal "install unknown apps" permission for this sideloaded app.
     */
    public static boolean requestInstall(Context context, Info info) throws Exception {
        if (!matchesSavedInfo(context, info) || !preferences(context).getBoolean(KEY_READY, false)) {
            throw new SecurityException("Verify the downloaded update before installing it");
        }
        File apk = downloadFile(context, info);
        if (!apk.isFile()) throw new IllegalStateException("The verified update file is missing. Please retry.");
        // Do a fresh final check immediately before sharing this external-files artifact with the
        // package installer. A saved boolean alone is not treated as proof of integrity.
        verifyDownloaded(context, info);
        validateArchive(context, info, apk);
        if (Build.VERSION.SDK_INT >= 26 && !context.getPackageManager().canRequestPackageInstalls()) {
            preferences(context).edit().putBoolean(KEY_PERMISSION_PENDING, true).apply();
            context.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return false;
        }
        preferences(context).edit().putBoolean(KEY_PERMISSION_PENDING, false).apply();
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".files", apk);
        Intent install = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(install);
        return true;
    }

    /** Compatibility helper for callers that need verification and launch in one user action. */
    public static boolean verifyAndRequestInstall(Context context, Info info) throws Exception {
        verifyDownloaded(context, info);
        return requestInstall(context, info);
    }

    /** Safely removes an unfinished platform transfer owned by this app. */
    public static void cancel(Context context, Info info) {
        if (matchesSavedInfo(context, info)) {
            long id = savedId(context);
            if (id > 0L) downloads(context).remove(id);
            File apk = downloadFile(context, info);
            if (apk.exists()) apk.delete();
            preferences(context).edit().clear().apply();
        }
    }

    public static boolean isMetered(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager != null && manager.isActiveNetworkMetered();
    }

    public static String networkSummary(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return "Connection status unavailable";
        Network active = manager.getActiveNetwork();
        NetworkCapabilities capabilities = active == null ? null : manager.getNetworkCapabilities(active);
        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return "No internet connection";
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return "Connection needs sign-in or validation";
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) return "Unmetered connection";
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Mobile data (metered)";
        return "Metered connection";
    }

    static int progressPercent(long downloaded, long total) {
        if (total <= 0L) return -1;
        return (int) Math.max(0L, Math.min(100L, downloaded * 100L / total));
    }

    static String readableBytes(long bytes) {
        if (bytes < 1_048_576L) return String.format(Locale.US, "%.1f KB", bytes / 1024d);
        return String.format(Locale.US, "%.1f MB", bytes / 1_048_576d);
    }

    static long requiredBytes(long apkBytes) {
        return Math.max(apkBytes, Math.round(apkBytes * 1.15d));
    }

    private static void validate(Info info) {
        if (info == null || info.code <= 0 || info.name == null || info.name.trim().isEmpty()) throw new IllegalArgumentException("Invalid update details");
        Uri uri = Uri.parse(info.url == null ? "" : info.url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) throw new SecurityException("Updates must use HTTPS");
        if (info.sha == null || !info.sha.matches("(?i)[0-9a-f]{64}")) throw new SecurityException("Update SHA-256 is invalid");
        if (info.sizeBytes == 0L || info.sizeBytes < -1L) throw new SecurityException("Update size is invalid");
    }

    private static void cancelDifferentOrFinishedDownload(Context context, Info info) {
        SharedPreferences prefs = preferences(context);
        if (!matchesSavedInfo(context, info)) {
            long oldId = prefs.getLong(KEY_ID, 0L);
            if (oldId > 0L) downloads(context).remove(oldId);
            prefs.edit().clear().apply();
        } else {
            long oldId = prefs.getLong(KEY_ID, 0L);
            if (oldId > 0L) downloads(context).remove(oldId);
        }
    }

    private static boolean matchesSavedInfo(Context context, Info info) {
        SharedPreferences prefs = preferences(context);
        return prefs.getInt(KEY_CODE, -1) == info.code && info.sha.equalsIgnoreCase(prefs.getString(KEY_SHA, ""));
    }

    private static long savedId(Context context) {
        return preferences(context).getLong(KEY_ID, 0L);
    }

    private static File downloadFile(Context context, Info info) {
        File root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (root == null) throw new IllegalStateException("App-specific download storage is unavailable");
        return new File(new File(root, DIRECTORY), "My-Personal-Trainer-" + info.code + ".apk");
    }

    private static long availableBytes(Context context) {
        try {
            File root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            return root == null ? -1L : root.getUsableSpace();
        } catch (RuntimeException ignored) {
            return -1L;
        }
    }

    private static DownloadManager downloads(Context context) {
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) throw new IllegalStateException("Android download service is unavailable");
        return manager;
    }

    @SuppressWarnings("deprecation")
    private static void validateArchive(Context context, Info info, File apk) {
        PackageInfo archive = context.getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
        if (archive == null || !context.getPackageName().equals(archive.packageName)) {
            throw new SecurityException("Downloaded package is not My Personal Trainer");
        }
        long archiveVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ? archive.getLongVersionCode() : archive.versionCode;
        if (archiveVersion != info.code) {
            throw new SecurityException("Downloaded package version did not match the update channel");
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String pausedReason(int reason) {
        if (reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI) return "Waiting for an allowed network";
        if (reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK) return "Waiting for a connection";
        if (reason == DownloadManager.PAUSED_WAITING_TO_RETRY) return "Android will retry this download";
        return "Download paused by Android";
    }

    private static String failedReason(int reason) {
        if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) return "Not enough storage for this update";
        if (reason == DownloadManager.ERROR_DEVICE_NOT_FOUND) return "Update storage is unavailable";
        if (reason == DownloadManager.ERROR_HTTP_DATA_ERROR) return "The network connection was interrupted";
        if (reason == DownloadManager.ERROR_TOO_MANY_REDIRECTS) return "The update link redirected too many times";
        if (reason == DownloadManager.ERROR_UNHANDLED_HTTP_CODE) return "The update server returned an unexpected response";
        if (reason == DownloadManager.ERROR_CANNOT_RESUME) return "Android could not resume this download";
        return "Android could not complete this download";
    }

    private static String read(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(20_000);
        connection.setInstanceFollowRedirects(true);
        try {
            int response = connection.getResponseCode();
            if (response < 200 || response >= 300) throw new IllegalStateException("Update channel returned HTTP " + response);
            try (InputStream input = connection.getInputStream(); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                for (int read; (read = input.read(buffer)) != -1;) output.write(buffer, 0, read);
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String sha(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            for (int read; (read = input.read(buffer)) != -1;) digest.update(buffer, 0, read);
        }
        StringBuilder hex = new StringBuilder();
        for (byte value : digest.digest()) hex.append(String.format(Locale.US, "%02x", value));
        return hex.toString();
    }

    private UpdateManager() { }
}
