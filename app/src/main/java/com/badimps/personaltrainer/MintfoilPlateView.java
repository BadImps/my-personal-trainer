package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.widget.ImageView;

/** Clips the original Mintfoil artwork to its iron-card silhouette. */
public final class MintfoilPlateView extends ImageView {
    public MintfoilPlateView(Context context) {
        super(context);
        setScaleType(ScaleType.CENTER_CROP);
        setAdjustViewBounds(false);
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) { super.onDraw(canvas); return; }
        float x = w * .070f;
        float y = h * .040f;
        Path plate = new Path();
        plate.moveTo(x, 0); plate.lineTo(w - x, 0);
        plate.lineTo(w, y); plate.lineTo(w, h - y);
        plate.lineTo(w - x, h); plate.lineTo(x, h);
        plate.lineTo(0, h - y); plate.lineTo(0, y);
        plate.close();
        int save = canvas.save();
        canvas.clipPath(plate);
        super.onDraw(canvas);
        canvas.restoreToCount(save);
    }
}
