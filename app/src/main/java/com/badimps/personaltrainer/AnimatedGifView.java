package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.SystemClock;
import android.view.View;
import java.io.InputStream;

/** Lightweight offline GIF view; avoids a network image dependency. */
public final class AnimatedGifView extends View {
    private final Movie movie;
    private final long start = SystemClock.uptimeMillis();
    public AnimatedGifView(Context context, int resource) {
        super(context);
        InputStream stream = context.getResources().openRawResource(resource);
        movie = Movie.decodeStream(stream);
        try { stream.close(); } catch (Exception ignored) { }
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (movie == null) return;
        int duration = movie.duration() <= 0 ? 1000 : movie.duration();
        movie.setTime((int) ((SystemClock.uptimeMillis() - start) % duration));
        float scale = Math.max(getWidth() / (float) movie.width(), getHeight() / (float) movie.height());
        float x = (getWidth() - movie.width() * scale) / 2f, y = (getHeight() - movie.height() * scale) / 2f;
        canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale); movie.draw(canvas, 0, 0); canvas.restore(); postInvalidateOnAnimation();
    }
}
