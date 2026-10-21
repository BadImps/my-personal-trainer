package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;

/** Local, muted animated WebP loop. Android 9+ uses the platform decoder; older devices show its poster frame. */
public final class LoopingExerciseWebpView extends ImageView {
    private final int drawableId;
    private AnimatedImageDrawable animation;

    public LoopingExerciseWebpView(Context context, int drawableId) {
        super(context);
        this.drawableId = drawableId;
        setScaleType(ScaleType.FIT_CENTER);
        setAdjustViewBounds(false);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        load();
    }

    @Override protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void load() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            loadAnimatedApi28();
            return;
        }
        setImageResource(drawableId);
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private void loadAnimatedApi28() {
        new Thread(() -> {
            Drawable decoded = null;
            try {
                decoded = ImageDecoder.decodeDrawable(ImageDecoder.createSource(getResources(), drawableId));
            } catch (Exception ignored) {
                // A normal resource fallback below keeps the exercise card usable.
            }
            final Drawable result = decoded;
            post(() -> {
                if (!isAttachedToWindow()) return;
                if (result instanceof AnimatedImageDrawable) {
                    animation = (AnimatedImageDrawable) result;
                    setImageDrawable(result);
                    animation.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
                    animation.start();
                } else {
                    setImageResource(drawableId);
                }
            });
        }, "exercise-webp-decoder").start();
    }

    private void stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && animation != null) {
            animation.stop();
            animation = null;
        }
    }
}
