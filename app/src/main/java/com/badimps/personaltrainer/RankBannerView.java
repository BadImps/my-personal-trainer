package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * Small, readable Gym Lobby rank banner. Each earned rank has a different material and
 * silhouette, while the written title remains the accessible source of the rank.
 */
public final class RankBannerView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String rank = "BEGINNER";

    public RankBannerView(Context context) {
        super(context);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        text.setTextAlign(Paint.Align.CENTER);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void bind(AchievementStore.Rank value) {
        rank = value == null ? "BEGINNER" : value.title;
        setContentDescription(rank + " rank");
        invalidate();
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = Math.round(dp(rank.length() >= 9 ? 132 : 116));
        int width = resolveSize(desiredWidth, widthMeasureSpec);
        int height = resolveSize(Math.round(dp(34)), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float inset = dp(1);
        RectF bounds = new RectF(inset, inset, w - inset, h - inset);
        switch (rank) {
            case "NOVICE": novice(canvas, bounds); break;
            case "CONTENDER": contender(canvas, bounds); break;
            case "COMPETITOR": competitor(canvas, bounds, Color.rgb(39, 174, 104)); break;
            case "ATHLETE": competitor(canvas, bounds, Color.rgb(207, 146, 45)); break;
            case "ELITE": elite(canvas, bounds); break;
            case "CHAMPION": champion(canvas, bounds); break;
            case "APEX": apex(canvas, bounds); break;
            default: beginner(canvas, bounds); break;
        }
        drawTitle(canvas, bounds, titleColor());
    }

    private void beginner(Canvas canvas, RectF r) {
        fillPath(canvas, clipped(r, dp(9)), Color.rgb(39, 46, 53));
        strokePath(canvas, clipped(inset(r, dp(1)), dp(8)), Color.rgb(140, 151, 161), dp(1));
        bolt(canvas, r.left + dp(11), r.centerY()); bolt(canvas, r.right - dp(11), r.centerY());
    }

    private void novice(Canvas canvas, RectF r) {
        fillPath(canvas, clipped(r, dp(9)), Color.rgb(20, 91, 191));
        strokePath(canvas, clipped(inset(r, dp(1)), dp(8)), Color.rgb(137, 197, 255), dp(1));
        strokePath(canvas, clipped(inset(r, dp(3)), dp(6)), Color.rgb(35, 55, 82), dp(1));
    }

    private void contender(Canvas canvas, RectF r) {
        fillPath(canvas, clipped(r, dp(9)), Color.rgb(10, 46, 54));
        strokePath(canvas, clipped(inset(r, dp(1)), dp(8)), Color.rgb(36, 212, 212), dp(2));
        paint.setColor(Color.rgb(36, 212, 212));
        canvas.drawRect(r.left + dp(7), r.centerY() - dp(1), r.left + dp(14), r.centerY() + dp(1), paint);
        canvas.drawRect(r.right - dp(14), r.centerY() - dp(1), r.right - dp(7), r.centerY() + dp(1), paint);
    }

    private void competitor(Canvas canvas, RectF r, int color) {
        float wing = dp(16);
        Path body = clipped(new RectF(r.left + wing, r.top + dp(2), r.right - wing, r.bottom - dp(2)), dp(6));
        fillPath(canvas, body, darken(color, .44f));
        strokePath(canvas, body, lighten(color, .2f), dp(1));
        paint.setColor(color);
        Path left = new Path(); left.moveTo(r.left, r.centerY()); left.lineTo(r.left + wing, r.top); left.lineTo(r.left + wing, r.bottom); left.close(); canvas.drawPath(left, paint);
        Path right = new Path(); right.moveTo(r.right, r.centerY()); right.lineTo(r.right - wing, r.top); right.lineTo(r.right - wing, r.bottom); right.close(); canvas.drawPath(right, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1)); paint.setColor(Color.rgb(219, 226, 227));
        canvas.drawLine(r.left + dp(5), r.top + dp(5), r.left + wing - dp(2), r.bottom - dp(5), paint);
        canvas.drawLine(r.right - dp(5), r.top + dp(5), r.right - wing + dp(2), r.bottom - dp(5), paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void elite(Canvas canvas, RectF r) {
        competitor(canvas, r, Color.rgb(122, 70, 207));
        paint.setColor(Color.rgb(213, 179, 255));
        diamond(canvas, r.centerX(), r.top + dp(3), dp(4));
        diamond(canvas, r.centerX(), r.bottom - dp(3), dp(3));
    }

    private void champion(Canvas canvas, RectF r) {
        fillPath(canvas, clipped(r, dp(10)), Color.rgb(35, 30, 20));
        strokePath(canvas, clipped(inset(r, dp(1)), dp(9)), Color.rgb(241, 195, 66), dp(2));
        strokePath(canvas, clipped(inset(r, dp(4)), dp(6)), Color.rgb(144, 93, 23), dp(1));
        diamond(canvas, r.centerX(), r.top + dp(3), dp(4));
    }

    private void apex(Canvas canvas, RectF r) {
        fillPath(canvas, clipped(r, dp(10)), Color.rgb(34, 35, 38));
        strokePath(canvas, clipped(inset(r, dp(1)), dp(9)), Color.rgb(121, 129, 138), dp(1));
        paint.setColor(Color.rgb(235, 86, 79));
        Path peak = new Path(); peak.moveTo(r.centerX() - dp(10), r.top + dp(4)); peak.lineTo(r.centerX(), r.top - dp(4)); peak.lineTo(r.centerX() + dp(10), r.top + dp(4)); peak.close(); canvas.drawPath(peak, paint);
        paint.setColor(Color.rgb(178, 47, 45));
        canvas.drawRect(r.left + dp(6), r.centerY() - dp(1), r.left + dp(17), r.centerY() + dp(1), paint);
        canvas.drawRect(r.right - dp(17), r.centerY() - dp(1), r.right - dp(6), r.centerY() + dp(1), paint);
    }

    private void drawTitle(Canvas canvas, RectF r, int color) {
        text.setColor(color);
        text.setTextSize(dp(rank.length() >= 9 ? 8.5f : 9.5f));
        Paint.FontMetrics metrics = text.getFontMetrics();
        float baseline = r.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(rank, r.centerX(), baseline, text);
    }

    private int titleColor() {
        if ("BEGINNER".equals(rank)) return Color.rgb(225, 230, 233);
        if ("NOVICE".equals(rank)) return Color.rgb(216, 240, 255);
        if ("CONTENDER".equals(rank)) return Color.rgb(130, 255, 248);
        if ("COMPETITOR".equals(rank)) return Color.rgb(213, 255, 228);
        if ("ATHLETE".equals(rank) || "CHAMPION".equals(rank)) return Color.rgb(255, 225, 142);
        if ("ELITE".equals(rank)) return Color.rgb(239, 213, 255);
        return Color.rgb(255, 157, 150);
    }

    private Path clipped(RectF r, float corner) {
        float cut = Math.min(corner, Math.min(r.width(), r.height()) / 3f);
        Path path = new Path();
        path.moveTo(r.left + cut, r.top); path.lineTo(r.right - cut, r.top); path.lineTo(r.right, r.centerY());
        path.lineTo(r.right - cut, r.bottom); path.lineTo(r.left + cut, r.bottom); path.lineTo(r.left, r.centerY()); path.close();
        return path;
    }

    private RectF inset(RectF r, float amount) { return new RectF(r.left + amount, r.top + amount, r.right - amount, r.bottom - amount); }
    private void fillPath(Canvas c, Path p, int color) { paint.setStyle(Paint.Style.FILL); paint.setShader(null); paint.setColor(color); c.drawPath(p, paint); }
    private void strokePath(Canvas c, Path p, int color, float width) { paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(width); paint.setColor(color); c.drawPath(p, paint); paint.setStyle(Paint.Style.FILL); }
    private void bolt(Canvas c, float x, float y) { paint.setColor(Color.rgb(190, 199, 203)); c.drawCircle(x, y, dp(3), paint); paint.setColor(Color.rgb(62, 69, 74)); c.drawCircle(x, y, dp(1), paint); }
    private void diamond(Canvas c, float x, float y, float radius) { Path d = new Path(); d.moveTo(x, y - radius); d.lineTo(x + radius, y); d.lineTo(x, y + radius); d.lineTo(x - radius, y); d.close(); c.drawPath(d, paint); }
    private int darken(int color, float factor) { return Color.rgb(Math.round(Color.red(color) * factor), Math.round(Color.green(color) * factor), Math.round(Color.blue(color) * factor)); }
    private int lighten(int color, float factor) { return Color.rgb(Math.min(255, Math.round(Color.red(color) + (255 - Color.red(color)) * factor)), Math.min(255, Math.round(Color.green(color) + (255 - Color.green(color)) * factor)), Math.min(255, Math.round(Color.blue(color) + (255 - Color.blue(color)) * factor))); }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
}
