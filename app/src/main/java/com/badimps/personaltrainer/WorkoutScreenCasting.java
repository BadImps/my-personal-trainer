package com.badimps.personaltrainer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.Settings;

/** Small Android-platform bridge for the free whole-screen workout mirroring flow. */
final class WorkoutScreenCasting {
    private WorkoutScreenCasting() { }

    static Intent pickerIntent() {
        return new Intent(Settings.ACTION_CAST_SETTINGS);
    }

    static boolean openSystemPicker(Activity activity) {
        Intent picker = pickerIntent();
        if (picker.resolveActivity(activity.getPackageManager()) == null) return false;
        try {
            activity.startActivity(picker);
            return true;
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            return false;
        }
    }
}
