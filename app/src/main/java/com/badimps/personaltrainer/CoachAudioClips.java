package com.badimps.personaltrainer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Locale;

/**
 * Plays the compact, coach-specific cue library bundled in assets/coach_audio.
 * Unrecognised or dynamic copy deliberately falls back to Android TTS.
 */
public final class CoachAudioClips {
    private static final class Request {
        final String asset;
        final Runnable started;
        final Runnable completed;
        Request(String asset, Runnable started, Runnable completed) {
            this.asset = asset;
            this.started = started;
            this.completed = completed;
        }
    }

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayDeque<Request> queue = new ArrayDeque<>();
    private MediaPlayer player;

    public CoachAudioClips(Context context) { this.context = context.getApplicationContext(); }

    /** Returns null when a phrase needs the system speech fallback. */
    public static String cueFor(String words) {
        String value = normalise(words);
        if (value.equals("lets go") && words != null && words.trim().endsWith("!")) return "go";
        if (value.matches("hi,? im (mia|amy|claudia|leo|mark|stephen),? your personal trainer.*")) return "welcome";
        if (value.equals("lets start our new exercise")) return "new_exercise";
        if (value.equals("okay, lets begin")) return "set_opening_1";
        if (value.equals("lets start")) return "set_opening_2";
        if (value.equals("lets go")) return "set_opening_3";
        if (value.equals("lets get ready")) return "set_opening_4";
        if (value.equals("5") || value.equals("4") || value.equals("3") || value.equals("2") || value.equals("1")) return "count_" + value;
        if (value.equals("congrats, you have completed this set. time to take a rest")) return "set_complete_1";
        if (value.equals("good work, you have completed this set. time to take a rest")) return "set_complete_2";
        if (value.equals("this set is complete. well done. time to take a rest")) return "set_complete_3";
        if (value.equals("great, you have completed this exercise. cooldown before you begin next exercise")) return "exercise_complete_next";
        if (value.equals("great, you have completed this exercise")) return "exercise_complete_final";
        if (value.equals("one minute")) return "one_minute";
        if (value.equals("30 seconds remaining")) return "thirty_seconds";
        if (value.equals("20 seconds remaining")) return "twenty_seconds";
        if (value.equals("10 seconds remaining")) return "ten_seconds";
        return null;
    }

    public boolean play(String coachId, String cue, boolean flush, Runnable started, Runnable completed) {
        if (coachId == null || cue == null) return false;
        String asset = "coach_audio/" + coachId + "/" + cue + ".ogg";
        try (AssetFileDescriptor ignored = context.getAssets().openFd(asset)) {
            if (flush) stop();
            queue.addLast(new Request(asset, started, completed));
            if (player == null) playNext();
            return true;
        } catch (IOException missing) {
            return false;
        }
    }

    public void stop() {
        queue.clear();
        if (player != null) {
            try { player.stop(); } catch (IllegalStateException ignored) { }
            player.release();
            player = null;
        }
    }

    private void playNext() {
        Request request = queue.pollFirst();
        if (request == null) return;
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(request.asset);
            player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setOnPreparedListener(media -> {
                if (request.started != null) main.post(request.started);
                media.start();
            });
            player.setOnCompletionListener(media -> {
                media.release();
                player = null;
                if (request.completed != null) main.post(request.completed);
                playNext();
            });
            player.setOnErrorListener((media, what, extra) -> {
                media.release();
                player = null;
                if (request.started != null) main.post(request.started);
                if (request.completed != null) main.post(request.completed);
                playNext();
                return true;
            });
            player.prepareAsync();
        } catch (IOException error) {
            if (player != null) { player.release(); player = null; }
            if (request.started != null) main.post(request.started);
            if (request.completed != null) main.post(request.completed);
            playNext();
        }
    }

    private static String normalise(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'').replace('\u2018', '\'')
                .replace("'", "")
                .replaceAll("[.!?]+$", "").trim();
    }
}
