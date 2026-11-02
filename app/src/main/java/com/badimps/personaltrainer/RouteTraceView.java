package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.Collections;
import java.util.List;

/** A private route trace: it deliberately draws no third-party map tiles or network imagery. */
public final class RouteTraceView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint point = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<LiveTrackingStore.Point> points = Collections.emptyList();

    public RouteTraceView(Context context) {
        super(context);
        line.setColor(Color.rgb(119, 224, 196)); line.setStyle(Paint.Style.STROKE); line.setStrokeWidth(dp(5));
        line.setStrokeCap(Paint.Cap.ROUND); line.setStrokeJoin(Paint.Join.ROUND);
        point.setStyle(Paint.Style.FILL);
        setBackgroundColor(Color.rgb(21, 42, 65));
    }

    public void setPoints(List<LiveTrackingStore.Point> points) { this.points = points == null ? Collections.emptyList() : points; invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (points.isEmpty()) return;
        double minLat = points.get(0).latitude, maxLat = minLat, minLon = points.get(0).longitude, maxLon = minLon;
        for (LiveTrackingStore.Point p : points) { minLat = Math.min(minLat, p.latitude); maxLat = Math.max(maxLat, p.latitude); minLon = Math.min(minLon, p.longitude); maxLon = Math.max(maxLon, p.longitude); }
        double latRange = Math.max(.00001d, maxLat - minLat), lonRange = Math.max(.00001d, maxLon - minLon);
        float pad = dp(15); Path path = new Path();
        for (int i = 0; i < points.size(); i++) {
            LiveTrackingStore.Point p = points.get(i);
            float x = (float) (pad + (p.longitude - minLon) / lonRange * (getWidth() - 2 * pad));
            float y = (float) (getHeight() - pad - (p.latitude - minLat) / latRange * (getHeight() - 2 * pad));
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        canvas.drawPath(path, line);
        LiveTrackingStore.Point start = points.get(0), end = points.get(points.size() - 1);
        float startX = xFor(start, minLon, lonRange, pad), startY = yFor(start, minLat, latRange, pad);
        point.setColor(Color.WHITE); canvas.drawCircle(startX, startY, dp(6), point);
        point.setColor(Color.rgb(56, 217, 189)); canvas.drawCircle(startX, startY, dp(4), point);
        // The fallback trace has no map bearing, so the live arrow is deliberately screen-north.
        float endX = xFor(end, minLon, lonRange, pad), endY = yFor(end, minLat, latRange, pad);
        Path arrow = new Path();
        arrow.moveTo(endX, endY - dp(8)); arrow.lineTo(endX + dp(7), endY + dp(7));
        arrow.lineTo(endX, endY + dp(3)); arrow.lineTo(endX - dp(7), endY + dp(7)); arrow.close();
        point.setColor(Color.WHITE); point.setStyle(Paint.Style.STROKE); point.setStrokeWidth(dp(2)); canvas.drawPath(arrow, point);
        point.setStyle(Paint.Style.FILL); point.setColor(Color.rgb(255, 107, 95)); canvas.drawPath(arrow, point);
    }
    private float xFor(LiveTrackingStore.Point p, double min, double range, float pad) { return (float) (pad + (p.longitude - min) / range * (getWidth() - 2 * pad)); }
    private float yFor(LiveTrackingStore.Point p, double min, double range, float pad) { return (float) (getHeight() - pad - (p.latitude - min) / range * (getHeight() - 2 * pad)); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}
