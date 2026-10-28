package com.badimps.personaltrainer;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/** Keeps exactly one launcher alias enabled, so the app icon follows the selected coach. */
public final class LauncherIconManager {
    private LauncherIconManager() { }
    public static void apply(Context context, String coach) {
        PackageManager manager=context.getPackageManager(); String packageName=context.getPackageName();
        // Aliases are declared under the source package, while preview builds append an
        // application-id suffix.  Use the manifest class package so both distributions
        // resolve the same declared aliases instead of crashing during account creation.
        String manifestPackage=LauncherIconManager.class.getPackage().getName();
        String name = coach;
        if ("Female".equals(name)) name = "Mia";
        if ("Male".equals(name)) name = "Leo";
        String active="." + (name == null || name.isEmpty() ? "Mia" : name) + "Launcher";
        for (String alias : new String[]{".DefaultLauncher",".MiaLauncher",".AmyLauncher",".ClaudiaLauncher",".LeoLauncher",".MarkLauncher",".StephenLauncher"}) {
            int state=alias.equals(active)?PackageManager.COMPONENT_ENABLED_STATE_ENABLED:PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            manager.setComponentEnabledSetting(new ComponentName(packageName,manifestPackage+alias),state,PackageManager.DONT_KILL_APP);
        }
    }
}
