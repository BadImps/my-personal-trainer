package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Lightweight determinate-looking ring used by the live set timer. */
public final class WorkoutTimerRingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progress = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private float fraction;

    public WorkoutTimerRingView(Context context, int mint) {
        super(context);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.ROUND);
        track.setColor(Color.rgb(36, 73, 94));
        progress.setStyle(Paint.Style.STROKE);
        progress.setStrokeCap(Paint.Cap.ROUND);
        progress.setColor(mint);
    }

    public void setElapsedMillis(long elapsedMillis) {
        // A set has no fixed duration. One calm revolution per minute communicates live motion
        // without pretending the member must finish against a deadline.
        fraction = (elapsedMillis % 60_000L) / 60_000f;
        invalidate();
    }

    /** Sets a fixed-session fraction for rest and cooldown countdowns. */
    public void setProgressFraction(float value) {
        fraction = Math.max(0f, Math.min(1f, value));
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float stroke = Math.max(8f, getWidth() * .055f);
        track.setStrokeWidth(stroke);
        progress.setStrokeWidth(stroke);
        float inset = stroke / 2f + 2f;
        bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
        canvas.drawArc(bounds, -90f, 360f, false, track);
        canvas.drawArc(bounds, -90f, Math.max(8f, 360f * fraction), false, progress);
    }
}
