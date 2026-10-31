package com.badimps.personaltrainer;

import android.graphics.Color;

/** Local-only navigation treatment; never changes app-wide or approved coach artwork. */
final class NavigationIconTheme {
    final boolean custom;
    final int selectedTile, inactiveTile, selectedBorder, inactiveBorder, activeText, inactiveText;
    private final int[] icons;

    private static final int[] ENAMEL = {
            R.drawable.ic_theme_enamel_home, R.drawable.ic_theme_enamel_workouts,
            R.drawable.ic_theme_enamel_library, R.drawable.ic_theme_enamel_progress,
            R.drawable.ic_theme_enamel_nutrition, R.drawable.ic_theme_enamel_endurance,
            R.drawable.ic_theme_enamel_scheduler, R.drawable.ic_theme_enamel_body,
            R.drawable.ic_theme_enamel_apex, R.drawable.ic_theme_enamel_settings
    };
    private static final int[] PULSE = {
            R.drawable.ic_theme_pulse_home, R.drawable.ic_theme_pulse_workouts,
            R.drawable.ic_theme_pulse_library, R.drawable.ic_theme_pulse_progress,
            R.drawable.ic_theme_pulse_nutrition, R.drawable.ic_theme_pulse_endurance,
            R.drawable.ic_theme_pulse_scheduler, R.drawable.ic_theme_pulse_body,
            R.drawable.ic_theme_pulse_apex, R.drawable.ic_theme_pulse_settings
    };
    private static final int[] ICE = {
            R.drawable.ic_theme_ice_home, R.drawable.ic_theme_ice_workouts,
            R.drawable.ic_theme_ice_library, R.drawable.ic_theme_ice_progress,
            R.drawable.ic_theme_ice_nutrition, R.drawable.ic_theme_ice_endurance,
            R.drawable.ic_theme_ice_scheduler, R.drawable.ic_theme_ice_body,
            R.drawable.ic_theme_ice_apex, R.drawable.ic_theme_ice_settings
    };
    private static final int[] CHAMPION = {
            R.drawable.ic_theme_champion_home, R.drawable.ic_theme_champion_workouts,
            R.drawable.ic_theme_champion_library, R.drawable.ic_theme_champion_progress,
            R.drawable.ic_theme_champion_nutrition, R.drawable.ic_theme_champion_endurance,
            R.drawable.ic_theme_champion_scheduler, R.drawable.ic_theme_champion_body,
            R.drawable.ic_theme_champion_apex, R.drawable.ic_theme_champion_settings
    };
    private static final int[] SPECTRUM = {
            R.drawable.ic_theme_spectrum_home, R.drawable.ic_theme_spectrum_workouts,
            R.drawable.ic_theme_spectrum_library, R.drawable.ic_theme_spectrum_progress,
            R.drawable.ic_theme_spectrum_nutrition, R.drawable.ic_theme_spectrum_endurance,
            R.drawable.ic_theme_spectrum_scheduler, R.drawable.ic_theme_spectrum_body,
            R.drawable.ic_theme_spectrum_apex, R.drawable.ic_theme_spectrum_settings
    };

    private NavigationIconTheme(int[] icons, String selected, String inactive, String border,
                                String text, String secondary) {
        this.icons = icons;
        custom = icons != null;
        selectedTile = Color.parseColor(selected);
        inactiveTile = Color.parseColor(inactive);
        selectedBorder = Color.parseColor(border);
        inactiveBorder = Color.parseColor("#365161");
        activeText = Color.parseColor(text);
        inactiveText = Color.parseColor(secondary);
    }

    static NavigationIconTheme forName(String name) {
        if (AppearanceStore.NAV_ENAMEL.equals(name))
            return new NavigationIconTheme(ENAMEL, "#24574F", "#153F43", "#63E4C9", "#F2FFF9", "#A9D8D0");
        if (AppearanceStore.NAV_PULSE.equals(name))
            return new NavigationIconTheme(PULSE, "#704149", "#382D3B", "#FF8B78", "#FFF4F0", "#E8BDB9");
        if (AppearanceStore.NAV_ICE.equals(name))
            return new NavigationIconTheme(ICE, "#2B5379", "#223F63", "#AFD6FF", "#F1F8FF", "#B8D5ED");
        if (AppearanceStore.NAV_CHAMPION.equals(name))
            return new NavigationIconTheme(CHAMPION, "#62503A", "#41372F", "#FFD37D", "#FFF8E9", "#E6D2A7");
        if (AppearanceStore.NAV_SPECTRUM.equals(name))
            return new NavigationIconTheme(SPECTRUM, "#493759", "#302A45", "#D7A7FF", "#FFF5FF", "#D5C9E5");
        return new NavigationIconTheme(null, "#000000", "#000000", "#000000", "#FFFFFF", "#FFFFFF");
    }

    int icon(int legacyIcon) {
        if (!custom) return legacyIcon;
        if (legacyIcon == R.drawable.ic_nav_home) return icons[0];
        if (legacyIcon == R.drawable.ic_nav_workouts) return icons[1];
        if (legacyIcon == R.drawable.ic_nav_library_premium) return icons[2];
        if (legacyIcon == R.drawable.ic_nav_progress) return icons[3];
        if (legacyIcon == R.drawable.ic_nav_nutrition) return icons[4];
        if (legacyIcon == R.drawable.ic_nav_endurance) return icons[5];
        if (legacyIcon == R.drawable.ic_nav_plan) return icons[6];
        if (legacyIcon == R.drawable.ic_nav_body) return icons[7];
        if (legacyIcon == R.drawable.ic_nav_apex) return icons[8];
        if (legacyIcon == R.drawable.ic_nav_settings) return icons[9];
        return legacyIcon;
    }
}
