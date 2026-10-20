package com.badimps.personaltrainer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Offline, scalable Mintfoil achievement medal. A single native renderer keeps all badge
 * families consistent without shipping a separate raster asset for every milestone.
 */
public final class MintfoilBadgeView extends View {
    private static final int NAVY = Color.rgb(13, 29, 49);
    private static final int MINT = Color.rgb(119, 224, 196);
    private static final int CORAL = Color.rgb(255, 107, 95);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String family = "CONSISTENCY";
    private boolean unlocked;
    private int tier;
    private float reveal = 1f;

    public MintfoilBadgeView(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

    public void bind(AchievementStore.Badge badge, int index) {
        family = badge.family;
        unlocked = badge.unlocked();
        tier = Math.max(0, index);
        setContentDescription(badge.name + ", " + badge.progressLabel());
        invalidate();
    }

    public void bindApex(String tierName, boolean isUnlocked, int index) {
        family = "APEX " + tierName;
        unlocked = isUnlocked;
        tier = index + 4;
        setContentDescription(tierName + " Apex tier" + (isUnlocked ? ", unlocked" : ", locked"));
        invalidate();
    }

    /** The acquisition motion is explicit; browsing a grid never starts dozens of animations. */
    public void playUnlock() {
        unlocked = true;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(760L);
        animator.addUpdateListener(value -> { reveal = (Float) value.getAnimatedValue(); invalidate(); });
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        animator.start();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float side = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = side * .41f;
        paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(side * .06f, 0f, side * .035f, Color.argb(130, 0, 0, 0));
        paint.setColor(Color.rgb(7, 18, 33));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(side * .045f);
        paint.setShader(new LinearGradient(cx - radius, cy - radius, cx + radius, cy + radius,
                unlocked ? new int[]{Color.WHITE, MINT, Color.rgb(30, 85, 80)} : new int[]{Color.rgb(114, 128, 145), Color.rgb(55, 66, 82)},
                null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * .89f, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(unlocked ? Color.rgb(18, 53, 63) : Color.rgb(55, 65, 78));
        canvas.drawCircle(cx, cy, radius * .78f, paint);
        if (unlocked && reveal < 1f) {
            paint.setColor(Color.argb((int) ((1f - reveal) * 220), 119, 224, 196));
            canvas.drawCircle(cx, cy, radius * (1f + reveal * .6f), paint);
        }
        paint.setColor(unlocked ? (tier >= 4 ? CORAL : MINT) : Color.rgb(151, 164, 178));
        paint.setStrokeWidth(side * .07f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        drawGlyph(canvas, cx, cy, radius * .48f);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawGlyph(Canvas canvas, float cx, float cy, float r) {
        String value = family.toUpperCase();
        if (value.contains("GUIDED")) { // clock
            canvas.drawCircle(cx, cy, r * .72f, paint); canvas.drawLine(cx, cy, cx, cy - r * .42f, paint); canvas.drawLine(cx, cy, cx + r * .36f, cy, paint);
        } else if (value.contains("RHYTHM")) { // pulse
            Path path = new Path(); path.moveTo(cx-r, cy); path.lineTo(cx-r*.5f, cy); path.lineTo(cx-r*.22f, cy-r*.48f); path.lineTo(cx, cy+r*.42f); path.lineTo(cx+r*.22f, cy-r*.18f); path.lineTo(cx+r, cy-r*.18f); canvas.drawPath(path, paint);
        } else if (value.contains("EXPLORER")) { // compass
            canvas.drawCircle(cx, cy, r*.72f, paint); canvas.drawLine(cx-r*.45f, cy+r*.45f, cx+r*.45f, cy-r*.45f, paint); canvas.drawLine(cx-r*.1f, cy-r*.55f, cx+r*.55f, cy+r*.1f, paint);
        } else if (value.contains("ENDURANCE")) { // running route
            canvas.drawLine(cx-r*.7f, cy+r*.4f, cx-r*.15f, cy-r*.1f, paint); canvas.drawLine(cx-r*.15f, cy-r*.1f, cx+r*.45f, cy+r*.35f, paint); canvas.drawCircle(cx-r*.28f, cy-r*.52f, r*.13f, paint);
        } else if (value.contains("APEX")) { // mountain
            Path path = new Path(); path.moveTo(cx-r, cy+r*.62f); path.lineTo(cx-r*.12f, cy-r*.72f); path.lineTo(cx+r*.12f, cy-r*.32f); path.lineTo(cx+r*.44f, cy-r*.68f); path.lineTo(cx+r, cy+r*.62f); canvas.drawPath(path, paint);
        } else { // consistency: flame
            Path path = new Path(); path.moveTo(cx, cy+r*.72f); path.cubicTo(cx-r*.72f, cy+r*.1f, cx-r*.18f, cy-r*.7f, cx+r*.1f, cy-r*.92f); path.cubicTo(cx+r*.82f, cy-r*.1f, cx+r*.5f, cy+r*.48f, cx, cy+r*.72f); canvas.drawPath(path, paint);
        }
    }
}
