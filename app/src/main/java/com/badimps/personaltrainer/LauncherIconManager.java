package com.badimps.personaltrainer;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/** Keeps exactly one launcher alias enabled, so the app icon follows the selected coach. */
public final class LauncherIconManager {
    private LauncherIconManager() { }
    public static void apply(Context context, String trainer) {
        PackageManager manager=context.getPackageManager(); String packageName=context.getPackageName();
        String active="Male".equals(trainer)?".LeoLauncher":".MiaLauncher";
        for (String alias : new String[]{".DefaultLauncher",".MiaLauncher",".LeoLauncher"}) {
            int state=alias.equals(active)?PackageManager.COMPONENT_ENABLED_STATE_ENABLED:PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            manager.setComponentEnabledSetting(new ComponentName(packageName,packageName+alias),state,PackageManager.DONT_KILL_APP);
        }
    }
}
