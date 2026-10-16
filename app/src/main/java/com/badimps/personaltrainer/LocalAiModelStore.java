package com.badimps.personaltrainer;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Owns an optional user-supplied LiteRT-LM model; the APK never ships a large model or a key. */
public final class LocalAiModelStore {
    private static final String DIRECTORY = "local_ai";
    private static final String FILE = "coach-model.litertlm";
    private static final long MIN_MODEL_BYTES = 8L * 1024L * 1024L;

    public static File file(Context context) {
        File directory = new File(context.getFilesDir(), DIRECTORY);
        if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Could not prepare private AI storage");
        return new File(directory, FILE);
    }

    public static boolean installed(Context context) {
        File file = file(context);
        return file.isFile() && file.length() >= MIN_MODEL_BYTES;
    }

    public static long sizeBytes(Context context) {
        File file = file(context);
        return file.isFile() ? file.length() : 0L;
    }

    public static void install(Context context, Uri source) throws Exception {
        if (source == null) throw new IllegalArgumentException("Choose a downloaded .litertlm model");
        File destination = file(context);
        File temporary = new File(destination.getParentFile(), FILE + ".partial");
        try (InputStream input = context.getContentResolver().openInputStream(source);
             FileOutputStream output = new FileOutputStream(temporary)) {
            if (input == null) throw new IllegalArgumentException("Could not read the selected model");
            byte[] buffer = new byte[64 * 1024];
            long total = 0L;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > 6L * 1024L * 1024L * 1024L) throw new IllegalArgumentException("That model is too large for this app");
                output.write(buffer, 0, read);
            }
        } catch (Exception error) {
            temporary.delete();
            throw error;
        }
        if (temporary.length() < MIN_MODEL_BYTES) {
            temporary.delete();
            throw new IllegalArgumentException("This does not look like a complete LiteRT-LM model");
        }
        if (destination.exists() && !destination.delete()) {
            temporary.delete();
            throw new IllegalStateException("Could not replace the previous local model");
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete();
            throw new IllegalStateException("Could not finish installing the local model");
        }
    }

    public static void clear(Context context) {
        File file = file(context);
        if (file.exists() && !file.delete()) throw new IllegalStateException("Could not remove the local model");
    }

    private LocalAiModelStore() { }
}
