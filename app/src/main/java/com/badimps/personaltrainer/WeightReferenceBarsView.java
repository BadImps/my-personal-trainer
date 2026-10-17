package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.Locale;

/**
 * A compact reference rail. It places the locally entered current weight and target against the
 * adult height-based screening band without inventing a trend from sparse check-ins.
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
        float left = dp(16), right = getWidth() - dp(16), trackY = dp(53);
        float minimum = Math.min(Math.min(current, target), low);
        float maximum = Math.max(Math.max(current, target), high);
        float extra = Math.max(3f, (maximum - minimum) * .18f);
        minimum -= extra;
        maximum += extra;

        float lowX = x(low, minimum, maximum, left, right);
        float highX = x(high, minimum, maximum, left, right);
        float targetX = x(target, minimum, maximum, left, right);
        float currentX = x(current, minimum, maximum, left, right);

        paint.setColor(Color.rgb(220, 228, 237));
        canvas.drawRoundRect(new RectF(left, trackY - dp(5), right, trackY + dp(5)), dp(5), dp(5), paint);
        paint.setColor(Color.rgb(119, 224, 196));
        canvas.drawRoundRect(new RectF(lowX, trackY - dp(9), highX, trackY + dp(9)), dp(9), dp(9), paint);
        paint.setTextSize(dp(10));
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        paint.setColor(Color.rgb(28, 143, 118));
        String rangeLabel = String.format(Locale.US, "Height-based reference: %.1f–%.1f %s", low, high, unit);
        canvas.drawText(rangeLabel, Math.max(left, (left + right - paint.measureText(rangeLabel)) / 2f), dp(15), paint);

        // Pin both bounds to the mint band itself so the visual range is never ambiguous.
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.rgb(60, 156, 132));
        canvas.drawLine(lowX, trackY - dp(17), lowX, trackY + dp(17), paint);
        canvas.drawLine(highX, trackY - dp(17), highX, trackY + dp(17), paint);
        paint.setTextSize(dp(9));
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        paint.setColor(Color.rgb(46, 120, 103));
        String upperBound = String.format(Locale.US, "%.1f %s", high, unit);
        String lowerBound = String.format(Locale.US, "%.1f %s", low, unit);
        canvas.drawText(lowerBound, lowX - paint.measureText(lowerBound) / 2f, dp(83), paint);
        canvas.drawText(upperBound, highX - paint.measureText(upperBound) / 2f, dp(83), paint);

        // The target is a ring and the current value is a solid coral point: distinguishable at a glance.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(Color.rgb(35, 61, 91));
        canvas.drawCircle(targetX, trackY, dp(8), paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 107, 95));
        canvas.drawCircle(currentX, trackY, dp(8), paint);
    }

    private float x(float value, float minimum, float maximum, float left, float right) {
        return left + ((value - minimum) / Math.max(1f, maximum - minimum)) * (right - left);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
