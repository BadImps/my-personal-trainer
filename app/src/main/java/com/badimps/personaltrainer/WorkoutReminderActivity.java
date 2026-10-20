package com.badimps.personaltrainer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Full-screen Coach Hero reminder destination with the same actions as the approved Taskmaster-style prompt. */
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

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(NAVY);
        ImageView hero = new ImageView(this);
        hero.setImageResource(heroBackground(profile.coachName));
        hero.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(hero, new FrameLayout.LayoutParams(-1, -1));

        TextView scrim = new TextView(this);
        scrim.setBackgroundColor(Color.argb(92, 8, 16, 32));
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(28), dp(20), dp(28), dp(28));
        TextView coach = text("COACH " + profile.coachName.toUpperCase(), 12, MINT, true);
        coach.setLetterSpacing(.13f);
        content.addView(coach);
        TextView greeting = text("Hey, " + profile.username + ".\nIt’s time to train.", 38, Color.WHITE, true);
        LinearLayout.LayoutParams greetingParams = new LinearLayout.LayoutParams(-1, -2);
        greetingParams.topMargin = dp(10);
        content.addView(greeting, greetingParams);
        TextView copy = text("Your guided workout is ready when you are.", 16, Color.rgb(225, 234, 244), false);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(-1, -2);
        copyParams.topMargin = dp(10);
        content.addView(copy, copyParams);

        Button start = button("Start guided workout  →", CORAL, Color.WHITE, 0);
        start.setOnClickListener(view -> {
            Intent launch = new Intent(this, MainActivity.class);
            launch.putExtra(MainActivity.EXTRA_START_GUIDED_WORKOUT, true);
            launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(launch);
            finish();
        });
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(-1, dp(58));
        startParams.topMargin = dp(28);
        content.addView(start, startParams);

        Button remindLater = button("Remind me later", NAVY, MINT, 2);
        remindLater.setOnClickListener(view -> showRemindMeLaterPicker());
        LinearLayout.LayoutParams laterParams = new LinearLayout.LayoutParams(-1, dp(54));
        laterParams.topMargin = dp(11);
        content.addView(remindLater, laterParams);

        Button dismiss = button("Not now", Color.TRANSPARENT, Color.rgb(227, 235, 244), 0);
        dismiss.setOnClickListener(view -> finish());
        LinearLayout.LayoutParams dismissParams = new LinearLayout.LayoutParams(-1, dp(43));
        dismissParams.topMargin = dp(3);
        content.addView(dismiss, dismissParams);
        root.addView(content, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        setContentView(root);
    }

    private void showRemindMeLaterPicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(20), dp(18), dp(20), dp(20));
        sheet.setBackground(surface(Color.rgb(16, 30, 49), 24, 1, Color.rgb(57, 88, 107)));
        sheet.addView(text("Remind me later", 20, Color.WHITE, true));
        TextView copy = text("Choose when Coach " + AccountStore.get(this).coachName + " should check back in.", 13, Color.rgb(202, 217, 231), false);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(-1, -2);
        copyParams.topMargin = dp(5); copyParams.bottomMargin = dp(14);
        sheet.addView(copy, copyParams);
        LinearLayout choices = new LinearLayout(this);
        choices.setGravity(Gravity.CENTER);
        int[] minutes = {10, 20, 30, 45, 60};
        String[] labels = {"10 min", "20 min", "30 min", "45 min", "1 hour"};
        for (int i = 0; i < minutes.length; i++) {
            final int value = minutes[i];
            Button choice = button(labels[i], Color.rgb(27, 49, 72), MINT, 1);
            choice.setTextSize(10);
            choice.setOnClickListener(view -> {
                WorkoutReminderScheduler.scheduleSnooze(this, value);
                dialog.dismiss();
                finish();
            });
            LinearLayout.LayoutParams choiceParams = new LinearLayout.LayoutParams(0, dp(45), 1);
            if (i > 0) choiceParams.leftMargin = dp(5);
            choices.addView(choice, choiceParams);
        }
        sheet.addView(choices);
        Button cancel = button("Not now", Color.TRANSPARENT, Color.rgb(208, 224, 237), 0);
        cancel.setOnClickListener(view -> dialog.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, dp(42));
        cancelParams.topMargin = dp(8);
        sheet.addView(cancel, cancelParams);
        dialog.setContentView(sheet);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(-1, -2);
            window.setGravity(Gravity.BOTTOM);
            window.setDimAmount(.38f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(-1, -2);
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

    private Button button(String label, int fill, int color, int stroke) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16);
        button.setTextColor(color);
        button.setAllCaps(false);
        button.setBackground(surface(fill, 18, stroke, stroke > 0 ? MINT : Color.TRANSPARENT));
        return button;
    }

    private GradientDrawable surface(int fill, int radius, int stroke, int strokeColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (stroke > 0) shape.setStroke(dp(stroke), strokeColor);
        return shape;
    }

    /** Uses exactly the selected Coach Hero Suite asset, never a launcher/icon substitute. */
    private int heroBackground(String coach) {
        if ("Amy".equalsIgnoreCase(coach)) return R.drawable.coach_hero_background_amy;
        if ("Claudia".equalsIgnoreCase(coach)) return R.drawable.coach_hero_background_claudia;
        if ("Leo".equalsIgnoreCase(coach)) return R.drawable.coach_hero_background_leo;
        if ("Mark".equalsIgnoreCase(coach)) return R.drawable.coach_hero_background_mark;
        if ("Stephen".equalsIgnoreCase(coach)) return R.drawable.coach_hero_background_stephen;
        return R.drawable.coach_hero_background_mia;
    }
}
