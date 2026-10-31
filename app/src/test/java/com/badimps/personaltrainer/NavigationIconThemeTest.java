package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class NavigationIconThemeTest {
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void classicRemainsDefaultAndKeepsExistingIcons() {
        assertEquals(AppearanceStore.NAV_CLASSIC, AppearanceStore.navigationTheme(context));
        NavigationIconTheme theme = NavigationIconTheme.forName(AppearanceStore.navigationTheme(context));
        assertFalse(theme.custom);
        assertEquals(R.drawable.ic_nav_workouts, theme.icon(R.drawable.ic_nav_workouts));
    }

    @Test public void everyNewThemePersistsAndSuppliesTheFullNavigation() {
        int[] originals = {R.drawable.ic_nav_home, R.drawable.ic_nav_workouts,
                R.drawable.ic_nav_library_premium, R.drawable.ic_nav_progress,
                R.drawable.ic_nav_nutrition, R.drawable.ic_nav_endurance,
                R.drawable.ic_nav_plan, R.drawable.ic_nav_body,
                R.drawable.ic_nav_apex, R.drawable.ic_nav_settings};
        for (int i = 1; i < AppearanceStore.NAV_THEMES.length; i++) {
            String name = AppearanceStore.NAV_THEMES[i];
            AppearanceStore.navigationTheme(context, name);
            assertEquals(name, AppearanceStore.navigationTheme(context));
            NavigationIconTheme theme = NavigationIconTheme.forName(name);
            assertTrue(theme.custom);
            for (int icon : originals) {
                int replacement = theme.icon(icon);
                assertNotEquals(icon, replacement);
                assertTrue(context.getResources().getDrawable(replacement, context.getTheme()) != null);
            }
        }
    }

    @Test public void unknownPreferenceFallsBackToClassic() {
        AppearanceStore.navigationTheme(context, "missing theme");
        assertEquals(AppearanceStore.NAV_CLASSIC, AppearanceStore.navigationTheme(context));
    }
}
