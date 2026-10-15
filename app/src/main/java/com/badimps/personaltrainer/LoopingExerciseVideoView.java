package com.badimps.personaltrainer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;

/**
 * A muted, local H.264 exercise loop. TextureView keeps the video inside the
 * rounded card that owns it, unlike a SurfaceView, and the view releases its
 * decoder whenever its screen leaves the window.
 */
public final class LoopingExerciseVideoView extends TextureView implements TextureView.SurfaceTextureListener {
    private final int videoResource;
    private Surface surface;
    private MediaPlayer player;
    private boolean attached;
    private int videoWidth;
    private int videoHeight;

    public LoopingExerciseVideoView(Context context, int videoResource) {
        super(context);
        this.videoResource = videoResource;
        setSurfaceTextureListener(this);
        setOpaque(true);
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
        if (!attached || getWindowVisibility() != VISIBLE || surface == null || player != null || videoResource == 0) return;
        try (AssetFileDescriptor descriptor = getResources().openRawResourceFd(videoResource)) {
            if (descriptor == null) return;
            MediaPlayer created = new MediaPlayer();
            created.setSurface(surface);
            created.setVolume(0f, 0f);
            created.setLooping(true);
            created.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
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
        float scaleX = containedWidth / getWidth();
        float scaleY = containedHeight / getHeight();
        float dx = (getWidth() - containedWidth) / 2f;
        float dy = (getHeight() - containedHeight) / 2f;
        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY);
        matrix.postTranslate(dx, dy);
        setTransform(matrix);
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
