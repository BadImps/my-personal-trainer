package com.badimps.personaltrainer;

import android.graphics.Color;

/**
 * Visual contract for the Coach Hero rebuild.
 *
 * Keep shared values here instead of scattering near-identical literal values through page
 * renderers.  Theme variants can still override the base palette in MainActivity, but page
 * geometry remains stable across Home, Workouts, Progress, Nutrition, Endurance, Scheduler,
 * Body, and Settings.
 */
public final class CoachHeroDesign {
    private CoachHeroDesign() { }

    public static final int NAVY = Color.rgb(16, 23, 42);
    public static final int NAVY_PANEL = Color.rgb(26, 58, 72);
    public static final int CORAL = Color.rgb(255, 107, 95);
    public static final int MINT = Color.rgb(119, 224, 196);
    public static final int PAPER = Color.rgb(247, 248, 252);
    public static final int INK = Color.rgb(43, 52, 77);
    public static final int MUTED = Color.rgb(107, 122, 147);
    public static final int CARD_LINE = Color.rgb(218, 226, 233);

    // The locked composition is based on an 8-point rhythm.
    public static final int GUTTER_DP = 22;
    public static final int SECTION_GAP_DP = 20;
    public static final int CARD_RADIUS_DP = 20;
    public static final int COMPACT_RADIUS_DP = 16;
    public static final int TOUCH_TARGET_DP = 48;
    public static final int PAGE_LABEL_SP = 12;
    public static final int PAGE_TITLE_SP = 31;
    public static final int BODY_SP = 16;
}
