package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** A compact, offline, looping movement demonstration for the selected exercise. */
public final class ExerciseAnimationView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase;
    public ExerciseAnimationView(Context context) { super(context); paint.setStrokeCap(Paint.Cap.ROUND); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); float w=getWidth(), h=getHeight();
        c.drawColor(Color.rgb(16,23,42));
        paint.setColor(Color.rgb(119,224,196)); paint.setStyle(Paint.Style.FILL);
        c.drawCircle(w/2f, h*.23f, h*.085f, paint);
        float pulse=(float)Math.sin(phase)*h*.07f;
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(h*.07f); paint.setColor(Color.rgb(255,107,95));
        c.drawLine(w/2f,h*.34f,w/2f,h*.63f+pulse,paint);
        c.drawLine(w/2f,h*.41f,w*.29f,h*.51f-pulse,paint); c.drawLine(w/2f,h*.41f,w*.71f,h*.51f-pulse,paint);
        c.drawLine(w/2f,h*.62f+pulse,w*.36f,h*.84f,paint); c.drawLine(w/2f,h*.62f+pulse,w*.64f,h*.84f,paint);
        paint.setColor(Color.argb(80,119,224,196)); paint.setStrokeWidth(3); c.drawOval(new RectF(w*.19f,h*.84f,w*.81f,h*.91f),paint);
        phase += .12f; postInvalidateDelayed(45);
    }
}
