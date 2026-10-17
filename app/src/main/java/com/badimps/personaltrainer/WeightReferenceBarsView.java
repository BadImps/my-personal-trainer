package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.Locale;

/**
 * A compact, factual comparison of the user's locally logged starting/current/target weights.
 * The mint band is an adult screening reference only; it is not a diagnosis or a prescribed goal.
 */
public final class WeightReferenceBarsView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float start, current, target, low, high;
    private String unit = "kg";

    public WeightReferenceBarsView(Context context) {
        super(context);
        setMinimumHeight(dp(104));
    }

    public void setValues(float start, float current, float target, float low, float high, String unit) {
        this.start = start;
        this.current = current;
        this.target = target;
        this.low = low;
        this.high = high;
        this.unit = unit;
        setContentDescription(String.format(Locale.US,
                "Weight comparison chart. Starting %.1f %s. Current %.1f %s. Target %.1f %s. Adult screening reference %.1f to %.1f %s.",
                start, unit, current, unit, target, unit, low, high, unit));
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = dp(16), right = getWidth() - dp(16), top = dp(19), bottom = dp(73);
        float minimum = Math.min(Math.min(Math.min(start, current), target), low);
        float maximum = Math.max(Math.max(Math.max(start, current), target), high);
        float extra = Math.max(3f, (maximum - minimum) * .18f);
        minimum -= extra;
        maximum += extra;

        paint.setColor(Color.rgb(119, 224, 196));
        paint.setAlpha(76);
        canvas.drawRoundRect(new RectF(left, y(high, minimum, maximum, top, bottom), right,
                y(low, minimum, maximum, top, bottom)), dp(7), dp(7), paint);
        paint.setAlpha(255);
        paint.setTextSize(dp(10));
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        paint.setColor(Color.rgb(86, 100, 126));
        canvas.drawText("Adult screening reference band", left, dp(12), paint);

        float step = (right - left) / 3f;
        bar(canvas, left + step * .5f, start, minimum, maximum, top, bottom, Color.rgb(75, 91, 121), "Start");
        bar(canvas, left + step * 1.5f, current, minimum, maximum, top, bottom, Color.rgb(255, 107, 95), "Now");
        bar(canvas, left + step * 2.5f, target, minimum, maximum, top, bottom, Color.rgb(119, 224, 196), "Target");
    }

    private void bar(Canvas canvas, float centerX, float value, float minimum, float maximum,
                     float top, float bottom, int color, String label) {
        float valueY = y(value, minimum, maximum, top, bottom);
        paint.setColor(color);
        canvas.drawRoundRect(new RectF(centerX - dp(12), valueY, centerX + dp(12), bottom), dp(6), dp(6), paint);
        paint.setTextSize(dp(10));
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        paint.setColor(Color.rgb(16, 23, 42));
        String number = String.format(Locale.US, "%.1f", value);
        canvas.drawText(number, centerX - paint.measureText(number) / 2f, Math.max(dp(18), valueY - dp(5)), paint);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        paint.setColor(Color.rgb(86, 100, 126));
        canvas.drawText(label, centerX - paint.measureText(label) / 2f, dp(94), paint);
    }

    private float y(float value, float minimum, float maximum, float top, float bottom) {
        return bottom - ((value - minimum) / Math.max(1f, maximum - minimum)) * (bottom - top);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
