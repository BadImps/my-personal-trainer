package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Path;
import android.view.View;

/**
 * Isolated arm-focus diagrams derived from the same high-detail anatomy atlas as every
 * other training-area card. Biceps use the front model; triceps use the back model.
 */
final class ArmFocusDiagramView extends View {
    private final boolean biceps;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mutedAtlas = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint tricepsFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tricepsDefinition = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap atlas;

    ArmFocusDiagramView(Context context, boolean biceps) {
        super(context);
        this.biceps = biceps;
        atlas = BitmapFactory.decodeResource(getResources(), R.drawable.workout_anatomy_highlights);
        ColorMatrix desaturated = new ColorMatrix();
        desaturated.setSaturation(.08f);
        mutedAtlas.setColorFilter(new ColorMatrixColorFilter(desaturated));
        tricepsFill.setColor(Color.rgb(255, 105, 91));
        tricepsFill.setAlpha(210);
        tricepsDefinition.setColor(Color.rgb(194, 65, 60));
        tricepsDefinition.setAlpha(110);
        tricepsDefinition.setStyle(Paint.Style.STROKE);
        tricepsDefinition.setStrokeWidth(2.2f);
        tricepsDefinition.setStrokeCap(Paint.Cap.ROUND);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (atlas == null) return;
        // The source atlas is a 2×5 grid: front Arms at row 2 left, rear Back at row 4 left.
        Rect source = biceps ? new Rect(0, 307, 512, 614) : new Rect(0, 921, 512, 1228);
        RectF destination = new RectF(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(atlas, source, destination, mutedAtlas);

        float sx = getWidth() / 512f;
        float sy = getHeight() / 307f;
        if (biceps) {
            // Reveal the original coral muscle texture only over the front upper-arm bellies.
            canvas.save(); canvas.clipPath(frontBicepsMask(sx, sy));
            canvas.drawBitmap(atlas, source, destination, paint); canvas.restore();
        } else {
            Path triceps = rearTricepsMask(sx, sy);
            canvas.drawPath(triceps, tricepsFill);
            canvas.drawPath(rearTricepsDefinition(sx, sy), tricepsDefinition);
        }
    }

    private Path frontBicepsMask(float sx, float sy) {
        Path mask = new Path();
        mask.moveTo(181*sx, 83*sy); mask.cubicTo(168*sx, 96*sy, 169*sx, 123*sy, 180*sx, 139*sy);
        mask.cubicTo(190*sx, 151*sy, 208*sx, 150*sy, 217*sx, 135*sy);
        mask.cubicTo(224*sx, 117*sy, 218*sx, 93*sy, 204*sx, 84*sy); mask.close();
        mask.moveTo(308*sx, 84*sy); mask.cubicTo(294*sx, 93*sy, 288*sx, 117*sy, 295*sx, 135*sy);
        mask.cubicTo(304*sx, 150*sy, 322*sx, 151*sy, 332*sx, 139*sy);
        mask.cubicTo(343*sx, 123*sy, 344*sx, 96*sy, 331*sx, 83*sy); mask.close();
        return mask;
    }

    private Path rearTricepsMask(float sx, float sy) {
        Path mask = new Path();
        // Triceps are on the *rear inner upper arms*: below each rear deltoid, then running
        // toward the elbow.  These coordinates deliberately avoid the outer-arm/lat area.
        // The rear figure's arms sit immediately beside the torso (not at the outer
        // white-space edge). These values are taken from the atlas's real dark-pixel bounds.
        mask.moveTo(209*sx, 87*sy); mask.cubicTo(203*sx, 102*sy, 203*sx, 124*sy, 208*sx, 141*sy);
        mask.cubicTo(211*sx, 149*sy, 217*sx, 149*sy, 220*sx, 139*sy);
        mask.cubicTo(223*sx, 122*sy, 222*sx, 101*sy, 217*sx, 87*sy); mask.close();
        mask.moveTo(303*sx, 87*sy); mask.cubicTo(309*sx, 102*sy, 309*sx, 124*sy, 304*sx, 141*sy);
        mask.cubicTo(301*sx, 149*sy, 295*sx, 149*sy, 292*sx, 139*sy);
        mask.cubicTo(289*sx, 122*sy, 290*sx, 101*sy, 295*sx, 87*sy); mask.close();
        return mask;
    }

    private Path rearTricepsDefinition(float sx, float sy) {
        Path definition = new Path();
        definition.moveTo(213*sx, 91*sy); definition.cubicTo(211*sx, 108*sy, 212*sx, 125*sy, 214*sx, 140*sy);
        definition.moveTo(299*sx, 91*sy); definition.cubicTo(301*sx, 108*sy, 300*sx, 125*sy, 298*sx, 140*sy);
        return definition;
    }
}
