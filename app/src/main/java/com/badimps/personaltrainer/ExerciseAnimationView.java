package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** Lightweight, coach-branded illustrated movement guide for exercises without exact filmed loops. */
public final class ExerciseAnimationView extends View {
    private static final int NAVY = Color.rgb(16, 23, 42);
    private static final int TEAL = Color.rgb(20, 69, 83);
    private static final int MINT = Color.rgb(119, 224, 196);
    private static final int CORAL = Color.rgb(255, 107, 95);
    private static final int WHITE = Color.rgb(244, 248, 252);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String coach;
    private final Coach.Exercise exercise;
    private final boolean reduceMotion;
    private final long startedAt = android.os.SystemClock.uptimeMillis();

    public ExerciseAnimationView(Context context, String coach, Coach.Exercise exercise) {
        super(context);
        this.coach = coach;
        this.exercise = exercise;
        this.reduceMotion = AppearanceStore.reducedMotion(context);
        paint.setStrokeCap(Paint.Cap.ROUND);
        setContentDescription("Animated movement guide for " + exercise.name + ". Coach " + coach + ". " + exercise.cue);
    }

    @Override protected void onDraw(Canvas canvas) {
        float width = getWidth(), height = getHeight();
        canvas.drawColor(NAVY);
        paint.setStyle(Paint.Style.FILL); paint.setColor(TEAL);
        canvas.drawRoundRect(new RectF(width * .05f, height * .07f, width * .95f, height * .86f), height * .065f, height * .065f, paint);
        String pattern = patternFor(exercise.name);
        float cycle = reduceMotion ? .45f : ((android.os.SystemClock.uptimeMillis() - startedAt) % 2400L) / 2400f;
        float eased = (float) ((1d - Math.cos(cycle * Math.PI * 2d)) * .5d);
        drawPath(canvas, width, height, pattern);
        drawFigure(canvas, width * .36f, height * .58f, height * .30f, pose(pattern, .05f), Color.argb(45, 196, 235, 230), false);
        drawFigure(canvas, width * .64f, height * .58f, height * .30f, pose(pattern, .95f), Color.argb(45, 196, 235, 230), false);
        drawFigure(canvas, width * .50f, height * .58f, height * .34f, pose(pattern, eased), WHITE, true);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); paint.setTextSize(dp(12)); paint.setColor(MINT);
        canvas.drawText("COACH " + coach.toUpperCase() + " · MOTION GUIDE", width * .10f, height * .16f, paint);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL)); paint.setTextSize(dp(11)); paint.setColor(Color.rgb(210, 226, 234));
        canvas.drawText("Illustrated form path · 30 fps", width * .10f, height * .94f, paint);
        if (!reduceMotion) postInvalidateDelayed(33L);
    }

    private String patternFor(String name) {
        String value = name.toLowerCase();
        if (value.contains("squat") || value.contains("lunge") || value.contains("leg press") || value.contains("step-up") || value.contains("wall sit")) return "squat";
        if (value.contains("push") || value.contains("press") || value.contains("dip") || value.contains("fly")) return "press";
        if (value.contains("row") || value.contains("pull") || value.contains("curl") || value.contains("face")) return "pull";
        if (value.contains("plank") || value.contains("dead bug") || value.contains("bird dog") || value.contains("climber") || value.contains("pallof")) return "core";
        if (value.contains("march") || value.contains("jack") || value.contains("burpee") || value.contains("bike") || value.contains("treadmill") || value.contains("elliptical")) return "cardio";
        return "squat";
    }

    private float pose(String pattern, float progress) { return "press".equals(pattern) ? 1f - progress : "core".equals(pattern) ? progress * .55f : progress; }

    private void drawPath(Canvas canvas, float width, float height, String pattern) {
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(3)); paint.setColor(MINT);
        RectF bounds = "press".equals(pattern) ? new RectF(width * .67f, height * .26f, width * .82f, height * .66f)
                : "pull".equals(pattern) ? new RectF(width * .20f, height * .34f, width * .38f, height * .67f)
                : "core".equals(pattern) ? new RectF(width * .62f, height * .51f, width * .84f, height * .68f)
                : new RectF(width * .67f, height * .44f, width * .83f, height * .72f);
        canvas.drawArc(bounds, 215f, 225f, false, paint);
        paint.setStyle(Paint.Style.FILL); canvas.drawCircle(bounds.centerX(), bounds.bottom, dp(5), paint); canvas.drawCircle(bounds.right, bounds.centerY(), dp(5), paint);
    }

    private void drawFigure(Canvas canvas, float x, float y, float size, float movement, int color, boolean active) {
        float head = size * .12f, shoulderY = y - size * .22f;
        float hipY = y + size * .10f + (movement * size * .16f), kneeY = y + size * .34f + (movement * size * .04f), footY = y + size * .55f;
        float spread = size * (.18f + movement * .09f);
        paint.setColor(color); paint.setStyle(Paint.Style.FILL); canvas.drawCircle(x, shoulderY - head * 1.35f, head, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(size * .09f);
        canvas.drawLine(x, shoulderY, x, hipY, paint);
        canvas.drawLine(x, shoulderY, x - size * .23f, shoulderY + size * (.12f + movement * .18f), paint);
        canvas.drawLine(x, shoulderY, x + size * .23f, shoulderY + size * (.12f + movement * .18f), paint);
        canvas.drawLine(x, hipY, x - spread, kneeY, paint); canvas.drawLine(x, hipY, x + spread, kneeY, paint);
        canvas.drawLine(x - spread, kneeY, x - size * .20f, footY, paint); canvas.drawLine(x + spread, kneeY, x + size * .20f, footY, paint);
        if (active) { paint.setStyle(Paint.Style.FILL); paint.setColor(CORAL); canvas.drawRoundRect(new RectF(x - size * .17f, shoulderY + size * .10f, x + size * .17f, shoulderY + size * .21f), size * .05f, size * .05f, paint); }
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}
