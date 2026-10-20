package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** Premium non-video fallback: no stick figures, only the Coach Mia or Coach Leo visual. */
public final class ExerciseAnimationView extends View {
    private static final int NAVY = Color.rgb(16, 23, 42);
    private static final int MINT = Color.rgb(119, 224, 196);
    private static final int CORAL = Color.rgb(255, 107, 95);
    private static final int WHITE = Color.rgb(244, 248, 252);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final String coach;
    private final Coach.Exercise exercise;
    private final boolean reduceMotion;
    private final long startedAt = android.os.SystemClock.uptimeMillis();
    private final Bitmap portrait;

    public ExerciseAnimationView(Context context, String coach, Coach.Exercise exercise) {
        super(context);
        this.coach = displayCoach(coach);
        this.exercise = exercise;
        this.reduceMotion = AppearanceStore.reducedMotion(context);
        portrait = BitmapFactory.decodeResource(getResources(), portraitFor(coach));
        setContentDescription("Coach " + this.coach + " movement preview for " + exercise.name + ". " + exercise.cue);
    }

    @Override protected void onDraw(Canvas canvas) {
        float width = getWidth(), height = getHeight();
        canvas.drawColor(NAVY);
        if (width <= 0 || height <= 0 || portrait == null) return;
        float phase = reduceMotion ? .5f : ((android.os.SystemClock.uptimeMillis() - startedAt) % 3000L) / 3000f;
        float breathe = (float) Math.sin(phase * Math.PI * 2d);
        float zoom = 1.035f + .025f * breathe;
        float sourceWidth = portrait.getWidth() / zoom, sourceHeight = portrait.getHeight() / zoom;
        float sourceLeft = (portrait.getWidth() - sourceWidth) * .5f + breathe * portrait.getWidth() * .018f;
        float sourceTop = (portrait.getHeight() - sourceHeight) * .5f - breathe * portrait.getHeight() * .012f;
        Rect source = new Rect(Math.max(0, Math.round(sourceLeft)), Math.max(0, Math.round(sourceTop)),
                Math.min(portrait.getWidth(), Math.round(sourceLeft + sourceWidth)), Math.min(portrait.getHeight(), Math.round(sourceTop + sourceHeight)));
        RectF destination = new RectF(width * .04f, height * .06f, width * .96f, height * .88f);
        canvas.drawBitmap(portrait, source, destination, paint);

        paint.setShader(new android.graphics.LinearGradient(0, height * .46f, 0, height * .91f,
                new int[]{Color.TRANSPARENT, Color.argb(220, 9, 23, 42), NAVY}, new float[]{0f, .56f, 1f}, android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRoundRect(destination, height * .06f, height * .06f, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1)); paint.setColor(MINT);
        canvas.drawRoundRect(destination, height * .06f, height * .06f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); paint.setTextSize(dp(11)); paint.setColor(MINT);
        canvas.drawText("COACH " + coach.toUpperCase() + " · MOTION PREVIEW", width * .10f, height * .16f, paint);
        paint.setTextSize(dp(20)); paint.setColor(WHITE); canvas.drawText(exercise.name, width * .10f, height * .69f, paint);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL)); paint.setTextSize(dp(12)); paint.setColor(Color.rgb(218, 230, 239));
        drawTrimmed(canvas, exercise.cue, width * .10f, height * .77f, width * .78f);
        paint.setColor(CORAL); canvas.drawCircle(width * .12f, height * .835f, dp(4), paint);
        paint.setColor(WHITE); paint.setTextSize(dp(11)); canvas.drawText("Download the Coach Motion pack for the full exercise loop", width * .16f, height * .85f, paint);
        if (!reduceMotion) postInvalidateDelayed(33L);
    }

    private void drawTrimmed(Canvas canvas, String value, float left, float baseline, float maxWidth) {
        String copy = value == null ? "" : value;
        while (paint.measureText(copy) > maxWidth && copy.length() > 4) copy = copy.substring(0, copy.length() - 2);
        canvas.drawText(copy.equals(value) ? copy : copy + "…", left, baseline, paint);
    }
    private int portraitFor(String value) { return isMale(value) ? R.drawable.coach_hero_background_leo : R.drawable.coach_hero_background_mia; }
    private String displayCoach(String value) { return isMale(value) ? "Leo" : "Mia"; }
    private boolean isMale(String value) { return "Leo".equals(value) || "Mark".equals(value) || "Stephen".equals(value) || "Male".equals(value); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}
