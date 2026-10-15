package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Displays an exact crop from a user-supplied visual specification. The source PNGs live in
 * drawable-nodpi, so Android does not density-rescale the artwork before the crop is applied.
 */
public final class ReferenceArtworkView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Bitmap bitmap;
    private Rect source = new Rect();

    public ReferenceArtworkView(Context context) { super(context); }
    public ReferenceArtworkView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void setArtwork(int drawableResource, int left, int top, int right, int bottom) {
        bitmap = BitmapFactory.decodeResource(getResources(), drawableResource);
        source.set(left, top, right, bottom);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null || source.isEmpty()) return;
        canvas.drawBitmap(bitmap, source, new Rect(0, 0, getWidth(), getHeight()), paint);
    }
}
