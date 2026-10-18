package com.badimps.personaltrainer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Full-screen, coach-led reminder preview and notification destination. */
public final class WorkoutReminderActivity extends Activity {
    private static final int NAVY = Color.rgb(16, 23, 42);
    private static final int MINT = Color.rgb(119, 224, 196);
    private static final int CORAL = Color.rgb(255, 107, 95);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        AccountStore.Profile profile = AccountStore.get(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(dp(28), dp(36), dp(28), dp(32));
        body.setBackgroundColor(NAVY);

        ImageView coach = new ImageView(this);
        coach.setImageResource(portrait(profile.coachName));
        coach.setScaleType(ImageView.ScaleType.CENTER_CROP);
        coach.setClipToOutline(true);
        coach.setBackground(surface(NAVY, 120, 2, MINT));
        body.addView(coach, new LinearLayout.LayoutParams(dp(172), dp(172)));

        TextView name = text("COACH " + profile.coachName.toUpperCase(), 13, MINT, true);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-2, -2);
        nameParams.topMargin = dp(18);
        body.addView(name, nameParams);
        TextView greeting = text("Hey, " + profile.username + ".\nIt’s time to train.", 39, Color.WHITE, true);
        greeting.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams greetingParams = new LinearLayout.LayoutParams(-1, -2);
        greetingParams.topMargin = dp(14);
        body.addView(greeting, greetingParams);
        TextView copy = text("Your guided workout is ready when you are.", 17, Color.rgb(212, 224, 239), false);
        copy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(-1, -2);
        copyParams.topMargin = dp(13);
        body.addView(copy, copyParams);

        Button start = button("Start guided workout", CORAL, NAVY);
        start.setOnClickListener(view -> {
            Intent launch = new Intent(this, MainActivity.class);
            launch.putExtra(MainActivity.EXTRA_START_GUIDED_WORKOUT, true);
            launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(launch);
            finish();
        });
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(-1, dp(58));
        startParams.topMargin = dp(38);
        body.addView(start, startParams);
        Button snooze = button("Snooze 10 min", NAVY, MINT);
        snooze.setOnClickListener(view -> { WorkoutReminderScheduler.scheduleSnooze(this, 10); finish(); });
        LinearLayout.LayoutParams snoozeParams = new LinearLayout.LayoutParams(-1, dp(54));
        snoozeParams.topMargin = dp(11);
        body.addView(snooze, snoozeParams);
        Button dismiss = button("Not now", NAVY, Color.rgb(212, 224, 239));
        dismiss.setOnClickListener(view -> finish());
        LinearLayout.LayoutParams dismissParams = new LinearLayout.LayoutParams(-1, dp(46));
        dismissParams.topMargin = dp(4);
        body.addView(dismiss, dismissParams);
        setContentView(body);
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(android.graphics.Typeface.create("sans-serif", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        return view;
    }

    private Button button(String label, int fill, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16);
        button.setTextColor(color);
        button.setAllCaps(false);
        button.setBackground(surface(fill, 18, fill == NAVY ? 2 : 0, MINT));
        return button;
    }

    private GradientDrawable surface(int fill, int radius, int stroke, int strokeColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (stroke > 0) shape.setStroke(dp(stroke), strokeColor);
        return shape;
    }

    private int portrait(String coach) {
        // A compact reminder has no approved standalone coach portrait. Use the selected
        // premium app mark rather than a fragment cropped out of the login composition.
        return R.drawable.trainer_launcher_premium_v3;
    }
}
