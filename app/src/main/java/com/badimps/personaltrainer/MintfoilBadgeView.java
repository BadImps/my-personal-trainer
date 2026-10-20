package com.badimps.personaltrainer;

import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.widget.ImageView;

/**
 * Renders the approved Mintfoil artwork from the Canvas design-lock reference.
 *
 * The artwork is deliberately raster-backed rather than redrawn with Canvas primitives:
 * every medal crop comes from the user-approved Apex Achievement Board reference stored
 * under artifacts/design-lock/apex-achievement-board/v1.
 */
public final class MintfoilBadgeView extends ImageView {
    private boolean unlocked;
    private int artwork = R.drawable.apex_badge_flame;

    public MintfoilBadgeView(Context context) {
        super(context);
        setScaleType(ScaleType.CENTER_CROP);
        setAdjustViewBounds(false);
    }

    public void bind(AchievementStore.Badge badge, int index) {
        unlocked = badge.unlocked();
        artwork = artworkFor(badge.family, index);
        setImageResource(artwork);
        // Locked cards retain their real Mintfoil art, but are visibly unavailable.
        setAlpha(unlocked ? 1f : .34f);
        setContentDescription(badge.name + ", " + badge.progressLabel());
    }

    public void bindApex(String tierName, boolean isUnlocked, int index) {
        unlocked = isUnlocked;
        artwork = index >= 4 ? R.drawable.apex_badge_trophy : R.drawable.apex_badge_mountain;
        setImageResource(artwork);
        setAlpha(unlocked ? 1f : .34f);
        setContentDescription(tierName + " Apex tier" + (isUnlocked ? ", unlocked" : ", locked"));
    }

    /** The approved medal performs the acquisition motion; no generic substitute glyph is drawn. */
    public void playUnlock() {
        unlocked = true;
        setImageResource(artwork);
        setAlpha(1f);
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private int artworkFor(String family, int index) {
        String value = family == null ? "" : family.toUpperCase();
        if (value.contains("APEX")) return R.drawable.apex_badge_mountain;
        if (value.contains("GUIDED")) return R.drawable.apex_badge_calendar;
        if (value.contains("RHYTHM")) return R.drawable.apex_badge_pulse;
        if (value.contains("EXPLORER")) return R.drawable.apex_badge_compass;
        if (value.contains("ENDURANCE")) return index % 2 == 0
                ? R.drawable.apex_badge_shoe : R.drawable.apex_badge_cycle;
        if (value.contains("STRENGTH")) return R.drawable.apex_badge_dumbbell;
        if (value.contains("RECOVERY")) return R.drawable.apex_badge_shield;
        if (index >= 5) return R.drawable.apex_badge_trophy;
        if (index >= 3) return R.drawable.apex_badge_star;
        return R.drawable.apex_badge_flame;
    }
}
