package com.badimps.personaltrainer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;
import java.io.File;

/**
 * A muted, local H.264 exercise loop. TextureView keeps the video inside the
 * rounded card that owns it, unlike a SurfaceView, and the view releases its
 * decoder whenever its screen leaves the window.
 */
public final class LoopingExerciseVideoView extends TextureView implements TextureView.SurfaceTextureListener {
    private final int videoResource;
    private final File videoFile;
    private Surface surface;
    private MediaPlayer player;
    private boolean attached;
    private int videoWidth;
    private int videoHeight;

    public LoopingExerciseVideoView(Context context, int videoResource) {
        super(context);
        this.videoResource = videoResource;
        this.videoFile = null;
        setSurfaceTextureListener(this);
        setOpaque(false);
        setAlpha(0f);
        // The remastered local loops are authored at 60 FPS. TextureView renders the
        // decoded cadence supported by the device without a custom display-mode request.
    }

    public LoopingExerciseVideoView(Context context, File videoFile) {
        super(context);
        this.videoResource = 0;
        this.videoFile = videoFile;
        setSurfaceTextureListener(this);
        setOpaque(false);
        setAlpha(0f);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        startIfPossible();
    }

    @Override protected void onDetachedFromWindow() {
        attached = false;
        releasePlayer();
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) startIfPossible();
        else if (player != null && player.isPlaying()) player.pause();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        applyCenterInside();
    }

    @Override public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
        surface = new Surface(texture);
        startIfPossible();
    }

    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
        applyCenterInside();
    }

    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
        releasePlayer();
        if (surface != null) {
            surface.release();
            surface = null;
        }
        return true;
    }

    @Override public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        // The platform renders decoded frames directly into this texture.
    }

    private void startIfPossible() {
        if (!attached || getWindowVisibility() != VISIBLE || surface == null || player != null || (videoResource == 0 && (videoFile == null || !videoFile.isFile()))) return;
        try {
            MediaPlayer created = new MediaPlayer();
            created.setSurface(surface);
            created.setVolume(0f, 0f);
            created.setLooping(true);
            if (videoFile != null) created.setDataSource(videoFile.getAbsolutePath());
            else {
                AssetFileDescriptor descriptor = getResources().openRawResourceFd(videoResource);
                if (descriptor == null) return;
                created.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                descriptor.close();
            }
            created.setOnVideoSizeChangedListener((mediaPlayer, width, height) -> {
                videoWidth = width;
                videoHeight = height;
                applyCenterInside();
            });
            created.setOnPreparedListener(mediaPlayer -> {
                if (attached && getWindowVisibility() == VISIBLE) mediaPlayer.start();
            });
            created.setOnErrorListener((mediaPlayer, what, extra) -> {
                releasePlayer();
                return true;
            });
            player = created;
            created.prepareAsync();
        } catch (IOException | IllegalStateException ignored) {
            releasePlayer();
        }
    }

    /** Keep the complete exercise visible instead of cutting off the athlete on wide cards. */
    private void applyCenterInside() {
        if (videoWidth <= 0 || videoHeight <= 0 || getWidth() <= 0 || getHeight() <= 0) return;
        float containedScale = Math.min(getWidth() / (float) videoWidth, getHeight() / (float) videoHeight);
        float containedWidth = videoWidth * containedScale;
        float containedHeight = videoHeight * containedScale;
        float dx = (getWidth() - containedWidth) / 2f;
        float dy = (getHeight() - containedHeight) / 2f;
        Matrix matrix = new Matrix();
        matrix.setRectToRect(
                new RectF(0f, 0f, getWidth(), getHeight()),
                new RectF(dx, dy, dx + containedWidth, dy + containedHeight),
                Matrix.ScaleToFit.FILL);
        setTransform(matrix);
        setAlpha(1f);
    }

    private void releasePlayer() {
        MediaPlayer active = player;
        player = null;
        if (active == null) return;
        try { active.setOnPreparedListener(null); } catch (Exception ignored) { }
        try { active.stop(); } catch (Exception ignored) { }
        active.release();
    }
}
