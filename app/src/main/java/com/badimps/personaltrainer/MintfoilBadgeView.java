package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.HapticFeedbackConstants;
import android.widget.ImageView;

/**
 * Renders the icon-only inserts for the original Mintfoil portrait-card mount.
 */
public final class MintfoilBadgeView extends ImageView {
    private boolean unlocked;
    private int artwork = R.drawable.achievement_symbol_flame;
    private final GradientDrawable coin;

    public MintfoilBadgeView(Context context) {
        super(context);
        setScaleType(ScaleType.CENTER_CROP);
        setAdjustViewBounds(false);
        coin = new GradientDrawable();
        coin.setShape(GradientDrawable.OVAL);
        coin.setColor(Color.TRANSPARENT);
        setBackground(coin);
        setClipToOutline(true);
    }

    public void bind(AchievementStore.Badge badge, int index) {
        unlocked = badge.unlocked();
        artwork = artworkFor(badge);
        coin.setStroke(0, Color.TRANSPARENT);
        setImageResource(artwork);
        setAlpha(unlocked ? 1f : .52f);
        setContentDescription(badge.name + ", " + badge.progressLabel());
    }

    public void bindApex(String tierName, boolean isUnlocked, int index) {
        unlocked = isUnlocked;
        artwork = index >= 4 ? R.drawable.achievement_symbol_trophy : R.drawable.achievement_symbol_mountain;
        setImageResource(artwork);
        setAlpha(unlocked ? 1f : .52f);
        setContentDescription(tierName + " Apex tier" + (isUnlocked ? ", unlocked" : ", locked"));
    }

    /** The approved medal performs the acquisition motion; no generic substitute glyph is drawn. */
    public void playUnlock() {
        unlocked = true;
        setImageResource(artwork);
        setAlpha(1f);
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private int artworkFor(AchievementStore.Badge badge) {
        String name = badge.name == null ? "" : badge.name.toUpperCase();
        String family = badge.family == null ? "" : badge.family.toUpperCase();
        if (name.contains("FIRST STEP")) return R.drawable.achievement_symbol_mountain;
        if (name.contains("THREE IN MOTION")) return R.drawable.achievement_symbol_calendar;
        if (name.contains("STEADY SEVEN")) return R.drawable.achievement_symbol_lightning;
        if (name.contains("ROUTINE BUILDER")) return R.drawable.achievement_symbol_star;
        if (name.contains("RELENTLESS")) return R.drawable.achievement_symbol_trophy;
        if (family.contains("GUIDED")) return R.drawable.achievement_symbol_calendar;
        if (family.contains("RHYTHM")) return R.drawable.achievement_symbol_pulse;
        if (family.contains("EXPLORER")) return R.drawable.achievement_symbol_compass;
        if (family.contains("ENDURANCE")) return name.contains("ROAD") ? R.drawable.achievement_symbol_cycle : R.drawable.achievement_symbol_shoe;
        if (family.contains("STRENGTH")) return R.drawable.achievement_symbol_dumbbell;
        if (family.contains("RECOVERY")) return R.drawable.achievement_symbol_shield;
        if (family.contains("APEX")) return R.drawable.achievement_symbol_mountain;
        return R.drawable.achievement_symbol_star;
    }
}
