package com.badimps.personaltrainer;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Movie;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Renders exercise GIFs with Android's modern animated-image decoder when available.
 * The Movie fallback keeps API 26–27 devices supported.
 */
public final class AnimatedGifView extends View {
    private static final ExecutorService DECODER = Executors.newFixedThreadPool(2);
    private Drawable animatedDrawable;
    private Movie fallbackMovie;
    private long startTime = SystemClock.uptimeMillis();

    public AnimatedGifView(Context context, int resource) {
        super(context);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            DECODER.execute(() -> {
                Drawable decoded = decodeModern(context, resource);
                post(() -> applyModernDrawable(decoded));
            });
        } else {
            try (InputStream stream = context.getResources().openRawResource(resource)) {
                fallbackMovie = Movie.decodeStream(stream);
            } catch (Exception ignored) {
                fallbackMovie = null;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private static Drawable decodeModern(Context context, int resource) {
        try {
            ImageDecoder.Source source = ImageDecoder.createSource(context.getResources(), resource);
            return ImageDecoder.decodeDrawable(source);
        } catch (Exception ignored) {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private void applyModernDrawable(Drawable decoded) {
        if (decoded == null) return;
        animatedDrawable = decoded;
        animatedDrawable.setCallback(this);
        if (animatedDrawable instanceof AnimatedImageDrawable) {
            AnimatedImageDrawable animation = (AnimatedImageDrawable) animatedDrawable;
            animation.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
            if (getVisibility() == VISIBLE) animation.start();
        }
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        stopAnimation();
        super.onDetachedFromWindow();
    }

    @Override protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) startAnimation();
        else stopAnimation();
    }

    private void startAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) startModernAnimation();
        startTime = SystemClock.uptimeMillis();
        invalidate();
    }

    private void stopAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) stopModernAnimation();
    }

    @TargetApi(Build.VERSION_CODES.P)
    private void startModernAnimation() {
        if (animatedDrawable instanceof AnimatedImageDrawable) {
            AnimatedImageDrawable animation = (AnimatedImageDrawable) animatedDrawable;
            if (!animation.isRunning()) animation.start();
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private void stopModernAnimation() {
        if (animatedDrawable instanceof AnimatedImageDrawable) {
            ((AnimatedImageDrawable) animatedDrawable).stop();
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (animatedDrawable != null) {
            int sourceWidth = Math.max(1, animatedDrawable.getIntrinsicWidth());
            int sourceHeight = Math.max(1, animatedDrawable.getIntrinsicHeight());
            float scale = Math.min(getWidth() / (float) sourceWidth, getHeight() / (float) sourceHeight);
            int width = Math.round(sourceWidth * scale);
            int height = Math.round(sourceHeight * scale);
            int left = (getWidth() - width) / 2;
            int top = (getHeight() - height) / 2;
            animatedDrawable.setBounds(left, top, left + width, top + height);
            animatedDrawable.draw(canvas);
            return;
        }
        if (fallbackMovie == null) return;
        int duration = fallbackMovie.duration() <= 0 ? 1000 : fallbackMovie.duration();
        fallbackMovie.setTime((int) ((SystemClock.uptimeMillis() - startTime) % duration));
        float scale = Math.min(getWidth() / (float) fallbackMovie.width(), getHeight() / (float) fallbackMovie.height());
        float left = (getWidth() - fallbackMovie.width() * scale) / 2f;
        float top = (getHeight() - fallbackMovie.height() * scale) / 2f;
        canvas.save();
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        fallbackMovie.draw(canvas, 0, 0);
        canvas.restore();
        postInvalidateOnAnimation();
    }
}
