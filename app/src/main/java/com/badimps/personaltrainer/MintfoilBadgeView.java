package com.badimps.personaltrainer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.HapticFeedbackConstants;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Renders the icon-only Mintfoil inserts for the Canvas-locked card mount. The card plate
 * owns the single physical coin rim and landscape; this view must never introduce another.
 */
public final class MintfoilBadgeView extends AppCompatImageView {
    private boolean unlocked;
    private int artwork = R.drawable.achievement_symbol_flame;
    private final GradientDrawable coin;

    public MintfoilBadgeView(Context context) {
        super(context);
        setScaleType(ScaleType.CENTER_CROP);
        setAdjustViewBounds(false);
        // Board artwork is delivered as a square inner-mount crop. Clip it to the circular
        // recess, but do not add a second metal bezel: the portrait card already provides
        // the one physical Mintfoil mount around it.
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
        // Locked cards remain visibly identifiable; the surrounding card copy states the
        // status explicitly, so dimming is restrained rather than washing out the symbol.
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

    /** One evolving session coin keeps its icon while its physical material marks long-term progress. */
    public void bindMastery(AchievementProgressStore.Mastery mastery) {
        unlocked = mastery.sessions >= 5;
        artwork = R.drawable.achievement_symbol_trophy;
        setImageResource(artwork);
        setAlpha(unlocked ? 1f : .55f);
        int material = Color.rgb(105, 228, 210);
        if ("Copper".equals(mastery.material)) material = Color.rgb(201, 126, 82);
        else if ("Bronze".equals(mastery.material)) material = Color.rgb(165, 104, 62);
        else if ("Silver".equals(mastery.material)) material = Color.rgb(198, 211, 224);
        else if ("Gold".equals(mastery.material)) material = Color.rgb(235, 190, 78);
        coin.setStroke(Math.max(1, Math.round(getResources().getDisplayMetrics().density * 2f)), material);
        setContentDescription("Session Mastery, " + mastery.label() + ", " + mastery.progressLabel());
    }

    /** The approved medal performs the acquisition motion; no generic substitute glyph is drawn. */
    public void playUnlock() {
        unlocked = true;
        setImageResource(artwork);
        setAlpha(1f);
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    /**
     * Every achievement is a distinct collectible.  Do not route early badges through a
     * progress-tier fallback: that made First Step, Three in Motion, and Steady Seven appear
     * as the same flame despite representing different milestones.
     */
    private int artworkFor(AchievementStore.Badge badge) {
        String name = badge.name == null ? "" : badge.name.toUpperCase();
        String family = badge.family == null ? "" : badge.family.toUpperCase();
        if (name.contains("FIRST STEP")) return R.drawable.achievement_symbol_mountain;
        if (name.contains("THREE IN MOTION")) return R.drawable.achievement_symbol_calendar;
        if (name.contains("STEADY SEVEN")) return R.drawable.achievement_symbol_lightning;
        if (name.contains("ROUTINE BUILDER")) return R.drawable.achievement_symbol_star;
        if (name.contains("RELENTLESS")) return R.drawable.achievement_symbol_trophy;

        if (family.contains("GUIDED")) {
            if (name.contains("HALF HOUR")) return R.drawable.achievement_symbol_pulse;
            if (name.contains("FOCUSED HOUR")) return R.drawable.achievement_symbol_calendar;
            if (name.contains("TRAINING BLOCK")) return R.drawable.achievement_symbol_dumbbell;
            if (name.contains("TIME BUILDER")) return R.drawable.achievement_symbol_star;
            return R.drawable.achievement_symbol_trophy;
        }
        if (family.contains("RHYTHM")) {
            if (name.contains("BACK AGAIN")) return R.drawable.achievement_symbol_lightning;
            if (name.contains("THREE-DAY")) return R.drawable.achievement_symbol_pulse;
            if (name.contains("FIVE-DAY")) return R.drawable.achievement_symbol_star;
            if (name.contains("WEEK")) return R.drawable.achievement_symbol_calendar;
            return R.drawable.achievement_symbol_trophy;
        }
        if (family.contains("EXPLORER")) {
            if (name.contains("TWO FOCUSES")) return R.drawable.achievement_symbol_compass;
            if (name.contains("BALANCED")) return R.drawable.achievement_symbol_star;
            if (name.contains("MOVEMENT")) return R.drawable.achievement_symbol_mountain;
            return R.drawable.achievement_symbol_trophy;
        }
        if (family.contains("ENDURANCE")) {
            if (name.contains("FIRST ROUTE")) return R.drawable.achievement_symbol_shoe;
            if (name.contains("ROAD READY")) return R.drawable.achievement_symbol_cycle;
            if (name.contains("DISTANCE")) return R.drawable.achievement_symbol_compass;
            return R.drawable.achievement_symbol_trophy;
        }
        if (family.contains("RECOVERY")) {
            if (name.contains("WARM START")) return R.drawable.achievement_symbol_shield;
            if (name.contains("RECOVERY RHYTHM")) return R.drawable.achievement_symbol_pulse;
            return R.drawable.achievement_symbol_star;
        }
        if (family.contains("RETURN")) {
            if (name.contains("READY AGAIN")) return R.drawable.achievement_symbol_lightning;
            if (name.contains("STEADY RETURN")) return R.drawable.achievement_symbol_shield;
            return R.drawable.achievement_symbol_trophy;
        }
        if (family.contains("STRENGTH")) return R.drawable.achievement_symbol_dumbbell;
        if (family.contains("APEX")) return R.drawable.achievement_symbol_mountain;
        return R.drawable.achievement_symbol_star;
    }
}
