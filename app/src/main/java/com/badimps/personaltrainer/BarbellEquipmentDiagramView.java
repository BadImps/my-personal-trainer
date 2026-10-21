package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** A clean, high-contrast Olympic barbell diagram for the equipment selector. */
final class BarbellEquipmentDiagramView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    BarbellEquipmentDiagramView(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale = Math.min(getWidth() / 300f, getHeight() / 170f);
        float dx = (getWidth() - 300f * scale) / 2f;
        float dy = (getHeight() - 170f * scale) / 2f;
        canvas.save(); canvas.translate(dx, dy); canvas.scale(scale, scale);
        paint.setColor(Color.argb(25, 8, 30, 50)); paint.setShadowLayer(10, 0, 5, Color.argb(45, 7, 27, 46));
        canvas.drawOval(new RectF(35, 132, 265, 151), paint); paint.clearShadowLayer();
        paint.setShader(new LinearGradient(40, 0, 260, 0,
                new int[]{Color.rgb(56, 78, 99), Color.WHITE, Color.rgb(56, 78, 99)},
                new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(48, 78, 252, 92), 7, 7, paint); paint.setShader(null);
        paint.setColor(Color.rgb(19, 47, 67));
        canvas.drawRoundRect(new RectF(78, 73, 94, 97), 4, 4, paint); canvas.drawRoundRect(new RectF(206, 73, 222, 97), 4, 4, paint);
        paint.setColor(Color.rgb(36, 71, 94));
        canvas.drawRoundRect(new RectF(35, 52, 69, 118), 9, 9, paint); canvas.drawRoundRect(new RectF(231, 52, 265, 118), 9, 9, paint);
        paint.setColor(Color.rgb(12, 32, 53));
        canvas.drawRoundRect(new RectF(19, 43, 44, 127), 9, 9, paint); canvas.drawRoundRect(new RectF(256, 43, 281, 127), 9, 9, paint);
        paint.setColor(Color.rgb(111, 235, 207));
        canvas.drawRoundRect(new RectF(32, 62, 36, 108), 2, 2, paint); canvas.drawRoundRect(new RectF(264, 62, 268, 108), 2, 2, paint);
        canvas.restore();
    }
}
