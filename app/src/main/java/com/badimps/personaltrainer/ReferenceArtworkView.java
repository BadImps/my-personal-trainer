package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.LruCache;
import android.view.View;

/**
 * Displays an exact crop from a user-supplied visual specification. The source PNGs live in
 * drawable-nodpi, so Android does not density-rescale the artwork before the crop is applied.
 */
public final class ReferenceArtworkView extends View {
    private static final LruCache<Integer, Bitmap> BITMAPS = new LruCache<Integer, Bitmap>(16 * 1024) {
        @Override protected int sizeOf(Integer key, Bitmap value) { return value.getByteCount() / 1024; }
    };
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Bitmap bitmap;
    private Rect source = new Rect();
    private final RectF destination = new RectF();

    public ReferenceArtworkView(Context context) { super(context); }
    public ReferenceArtworkView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void setArtwork(int drawableResource, int left, int top, int right, int bottom) {
        bitmap = BITMAPS.get(drawableResource);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(getResources(), drawableResource);
            if (bitmap != null) BITMAPS.put(drawableResource, bitmap);
        }
        if (bitmap != null) {
            source.set(Math.max(0, left), Math.max(0, top), Math.min(bitmap.getWidth(), right), Math.min(bitmap.getHeight(), bottom));
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null || source.isEmpty()) return;
        float scale = Math.min(getWidth() / (float) source.width(), getHeight() / (float) source.height());
        float width = source.width() * scale;
        float height = source.height() * scale;
        float left = (getWidth() - width) / 2f;
        float top = (getHeight() - height) / 2f;
        destination.set(left, top, left + width, top + height);
        canvas.drawBitmap(bitmap, source, destination, paint);
    }
}
