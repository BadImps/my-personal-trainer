package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.Locale;

/** Small accessible chart that compares self-entered weight with an adult screening reference band. */
public final class ReferenceWeightRangeView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float current, target, low, high;
    private String unit = "kg";

    public ReferenceWeightRangeView(Context context) {
        super(context);
        setMinimumHeight(dp(118));
    }

    public void setValues(float current, float target, float low, float high, String unit) {
        this.current = current;
        this.target = target;
        this.low = low;
        this.high = high;
        this.unit = unit;
        setContentDescription(String.format(Locale.US,
                "Weight reference chart. Adult screening reference %.1f to %.1f %s. Current %.1f %s. Personal target %.1f %s.",
                low, high, unit, current, unit, target, unit));
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = dp(10), right = getWidth() - dp(10), trackY = dp(56);
        float min = Math.min(Math.min(current, target), low);
        float max = Math.max(Math.max(current, target), high);
        float extra = Math.max(4f, (max - min) * .16f);
        min -= extra;
        max += extra;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(220, 228, 237));
        canvas.drawRoundRect(new RectF(left, trackY - dp(5), right, trackY + dp(5)), dp(5), dp(5), paint);
        paint.setColor(Color.rgb(119, 224, 196));
        canvas.drawRoundRect(new RectF(x(low, min, max, left, right), trackY - dp(8), x(high, min, max, left, right), trackY + dp(8)), dp(8), dp(8), paint);
        paint.setTextSize(dp(12));
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        paint.setColor(Color.rgb(86, 100, 126));
        canvas.drawText("Adult screening reference", left, dp(18), paint);
        canvas.drawText(String.format(Locale.US, "%.0f", low), x(low, min, max, left, right) - dp(8), dp(84), paint);
        canvas.drawText(String.format(Locale.US, "%.0f", high), x(high, min, max, left, right) - dp(8), dp(84), paint);
        // Place close markers' captions on opposite sides so the values remain legible.
        marker(canvas, x(target, min, max, left, right), trackY, Color.rgb(16, 23, 42), "Target " + round(target), false);
        marker(canvas, x(current, min, max, left, right), trackY, Color.rgb(255, 107, 95), "Now " + round(current), true);
    }

    private void marker(Canvas canvas, float x, float y, int color, String label, boolean labelOnRight) {
        paint.setColor(color);
        canvas.drawCircle(x, y, dp(7), paint);
        paint.setStrokeWidth(dp(2));
        canvas.drawLine(x, y + dp(8), x, y + dp(20), paint);
        paint.setTextSize(dp(11));
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        float approximateLabelWidth = dp(labelOnRight ? 62 : 78);
        float labelX = labelOnRight ? Math.min(x + dp(9), getWidth() - approximateLabelWidth)
                : Math.max(dp(4), x - approximateLabelWidth);
        canvas.drawText(label, labelX, y + dp(35), paint);
    }

    private float x(float value, float min, float max, float left, float right) {
        return left + ((value - min) / Math.max(1f, max - min)) * (right - left);
    }

    private String round(float value) {
        return String.format(Locale.US, "%.0f %s", value, unit);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
