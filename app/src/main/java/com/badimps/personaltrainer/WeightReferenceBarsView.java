package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.Locale;

/**
 * A compact journey graph for the user's locally logged starting/current/target weights.
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
        float left = dp(22), right = getWidth() - dp(22), top = dp(23), bottom = dp(70);
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
        String rangeLabel = String.format(Locale.US, "Height-based reference: %.1f–%.1f %s", low, high, unit);
        canvas.drawText(rangeLabel, left, dp(12), paint);

        // Pin both bounds to the mint band itself so the visual range is never ambiguous.
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.rgb(60, 156, 132));
        canvas.drawLine(left, y(high, minimum, maximum, top, bottom), right, y(high, minimum, maximum, top, bottom), paint);
        canvas.drawLine(left, y(low, minimum, maximum, top, bottom), right, y(low, minimum, maximum, top, bottom), paint);
        paint.setTextSize(dp(9));
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        paint.setColor(Color.rgb(46, 120, 103));
        String upperBound = String.format(Locale.US, "%.1f %s", high, unit);
        String lowerBound = String.format(Locale.US, "%.1f %s", low, unit);
        canvas.drawText(upperBound, left + dp(5), y(high, minimum, maximum, top, bottom) - dp(4), paint);
        canvas.drawText(lowerBound, left + dp(5), y(low, minimum, maximum, top, bottom) + dp(12), paint);

        float startX = left + dp(18);
        float currentX = left + (right - left) / 2f;
        float targetX = right - dp(18);
        float startY = y(start, minimum, maximum, top, bottom);
        float currentY = y(current, minimum, maximum, top, bottom);
        float targetY = y(target, minimum, maximum, top, bottom);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.rgb(75, 91, 121));
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(startX, startY);
        path.lineTo(currentX, currentY);
        path.lineTo(targetX, targetY);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        point(canvas, startX, startY, start, Color.rgb(75, 91, 121), "Start");
        point(canvas, currentX, currentY, current, Color.rgb(255, 107, 95), "Today");
        point(canvas, targetX, targetY, target, Color.rgb(16, 23, 42), "Target");
    }

    private void point(Canvas canvas, float x, float y, float value, int color, String label) {
        paint.setColor(color);
        canvas.drawCircle(x, y, dp(8), paint);
        if ("Target".equals(label)) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.rgb(119, 224, 196));
            paint.setStrokeWidth(dp(3));
            canvas.drawCircle(x, y, dp(12), paint);
            paint.setStyle(Paint.Style.FILL);
        }
        paint.setTextSize(dp(10));
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        paint.setColor(Color.rgb(16, 23, 42));
        String number = String.format(Locale.US, "%.1f", value);
        canvas.drawText(number, x - paint.measureText(number) / 2f, Math.max(dp(18), y - dp(12)), paint);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        paint.setColor(Color.rgb(86, 100, 126));
        canvas.drawText(label, x - paint.measureText(label) / 2f, dp(94), paint);
    }

    private float y(float value, float minimum, float maximum, float top, float bottom) {
        return bottom - ((value - minimum) / Math.max(1f, maximum - minimum)) * (bottom - top);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
