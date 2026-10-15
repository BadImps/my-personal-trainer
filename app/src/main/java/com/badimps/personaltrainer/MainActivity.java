package com.badimps.personaltrainer;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The complete on-device My Personal Trainer experience. */
public final class MainActivity extends Activity {
    private final int navy = Color.rgb(16, 23, 42);
    private final int navyPanel = Color.rgb(26, 58, 72);
    private final int coral = Color.rgb(255, 107, 95);
    private final int mint = Color.rgb(119, 224, 196);
    private final int paper = Color.rgb(247, 248, 252);
    private final int ink = Color.rgb(43, 52, 77);
    private final int muted = Color.rgb(107, 122, 147);
    private final int cardLine = Color.rgb(218, 226, 233);

    private String goal = Coach.GOALS[1];
    private String target = "Full Body";
    private String environment = Coach.ENVIRONMENTS[0];
    private String selectedTrainer = "Female";
    private String planMode = Coach.PLAN_MODES[0];
    private String equipmentType = Coach.EQUIPMENT_TYPES[0];
    private int sessionMinutes = 30;
    private int trainingDays = 3;
    private int workingWeightKg = 20;
    private int planSets = 3;
    private int planReps = 10;
    private int planRestSeconds = 90;
    private List<Coach.Exercise> session = new ArrayList<>();
    private List<Coach.Exercise> availableExercises = new ArrayList<>();
    private List<Coach.Exercise> selectedExercises = new ArrayList<>();
    private String selectionKey = "";
    private TextToSpeech voice;
    private CountDownTimer restTimer;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        voice = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) voice.setLanguage(Locale.getDefault());
        });
        boolean hasLocalAccount = AccountStore.exists(this);
        if (hasLocalAccount) {
            selectedTrainer = AccountStore.get(this).trainer;
            LauncherIconManager.apply(this, selectedTrainer);
        }
        restoreSavedPlan();
        if (hasLocalAccount) {
            showHome();
        } else {
            showWelcome();
        }
    }

    @Override protected void onDestroy() {
        if (restTimer != null) restTimer.cancel();
        if (voice != null) voice.shutdown();
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable surface(int fill, int radius, int stroke, int strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(dp(radius));
        if (stroke > 0) background.setStroke(dp(stroke), strokeColor);
        return background;
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        view.setLineSpacing(dp(3), 1f);
        return view;
    }

    private TextView heading(String value, int sp, int color) {
        TextView view = text(value, sp, color);
        view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        view.setLineSpacing(dp(1), 1f);
        return view;
    }

    private void setSystemBars() {
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);
        getWindow().getDecorView().setSystemUiVisibility(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
    }

    private LinearLayout column() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(navy);
        content.setPadding(dp(22), dp(24), dp(22), dp(36));
        return content;
    }

    /** A full-height surface used before a profile exists. */
    private LinearLayout signedOutPage() {
        setSystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(navy);
        LinearLayout body = column();
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);
        return body;
    }

    /** A full-height app page with exactly one navigation system: the coach rail. */
    private LinearLayout appPage(String activeScreen) {
        setSystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(navy);
        LinearLayout body = column();
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        shell.setBackgroundColor(navy);
        shell.addView(coachRail(activeScreen), new LinearLayout.LayoutParams(dp(82), -1));
        shell.addView(scroll, new LinearLayout.LayoutParams(0, -1, 1));
        setContentView(shell);
        return body;
    }

    private LinearLayout coachRail(String activeScreen) {
        AccountStore.Profile profile = AccountStore.get(this);
        String trainerName = "Male".equals(profile.trainer) ? "Leo" : "Mia";
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        rail.setPadding(dp(8), dp(24), dp(8), dp(10));
        rail.setBackgroundColor(navy);

        ImageView portrait = new ImageView(this);
        portrait.setImageResource("Male".equals(profile.trainer) ? R.drawable.leo_launcher : R.drawable.mia_launcher);
        portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);
        portrait.setBackground(surface(navyPanel, 99, 2, mint));
        portrait.setClipToOutline(true);
        portrait.setContentDescription("Coach " + trainerName);
        rail.addView(portrait, new LinearLayout.LayoutParams(dp(58), dp(58)));

        TextView coach = text("Coach\n" + trainerName, 11, Color.WHITE);
        coach.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams coachParams = new LinearLayout.LayoutParams(-1, -2);
        coachParams.topMargin = dp(5);
        rail.addView(coach, coachParams);

        View divider = new View(this);
        divider.setBackgroundColor(mint);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(dp(4), dp(34));
        dividerParams.topMargin = dp(17);
        dividerParams.bottomMargin = dp(9);
        rail.addView(divider, dividerParams);

        addRailItem(rail, "Home", R.drawable.ic_nav_home, activeScreen, view -> showHome());
        addRailItem(rail, "Workouts", R.drawable.ic_nav_plan, activeScreen, view -> showPlans());
        addRailItem(rail, "Progress", R.drawable.ic_nav_progress, activeScreen, view -> showProgress());
        addRailItem(rail, "Nutrition", R.drawable.ic_nav_nutrition, activeScreen, view -> showMeals());
        addRailItem(rail, "Settings", R.drawable.ic_nav_settings, activeScreen, view -> showProfile());
        return rail;
    }

    private void addRailItem(LinearLayout rail, String label, int icon, String activeScreen, View.OnClickListener listener) {
        boolean selected = label.equals(activeScreen);
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setBackground(surface(selected ? mint : navyPanel, 15, selected ? 2 : 1, selected ? coral : Color.rgb(35, 53, 78)));
        item.setContentDescription(selected ? label + ", current screen" : "Open " + label);
        item.setTooltipText(label);
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(listener);

        ImageView symbol = new ImageView(this);
        symbol.setImageResource(icon);
        symbol.setImageTintList(ColorStateList.valueOf(selected ? navy : mint));
        symbol.setContentDescription(null);
        symbol.setPadding(dp(2), dp(2), dp(2), dp(2));
        item.addView(symbol, new LinearLayout.LayoutParams(dp(26), dp(26)));

        TextView caption = text(label, 9, selected ? navy : mint);
        caption.setGravity(Gravity.CENTER);
        caption.setSingleLine(true);
        caption.setTypeface(Typeface.create("sans-serif", selected ? Typeface.BOLD : Typeface.NORMAL));
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(-1, -2);
        captionParams.topMargin = dp(1);
        item.addView(caption, captionParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(64), dp(61));
        params.topMargin = dp(7);
        rail.addView(item, params);
    }

    private void gap(LinearLayout box, int size) {
        View spacer = new View(this);
        box.addView(spacer, new LinearLayout.LayoutParams(1, dp(size)));
    }

    private void title(LinearLayout body, String eyebrow, String main, String copy) {
        TextView label = heading(eyebrow.toUpperCase(Locale.ROOT), 12, mint);
        label.setLetterSpacing(.08f);
        body.addView(label);
        gap(body, 6);
        body.addView(heading(main, 31, Color.WHITE));
        if (copy != null && !copy.isEmpty()) {
            gap(body, 7);
            body.addView(text(copy, 16, Color.rgb(211, 225, 233)));
        }
        gap(body, 20);
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(16), dp(16), dp(16), dp(16));
        view.setBackground(surface(paper, 20, 1, cardLine));
        view.setElevation(dp(2));
        return view;
    }

    private TextView chip(String value) {
        TextView view = heading(value, 12, navy);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(12), dp(7), dp(12), dp(7));
        view.setBackground(surface(mint, 99, 0, Color.TRANSPARENT));
        return view;
    }

    private Button action(String label, int fill, int foreground) {
        Button button = new Button(this);
        button.setText(label);
        button.setContentDescription(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        button.setTextColor(foreground);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(9), dp(12), dp(9));
        button.setBackground(surface(fill, 15, 0, Color.TRANSPARENT));
        button.setElevation(dp(2));
        button.setMinHeight(dp(48));
        return button;
    }

    private Button primary(String label) { return action(label, coral, navy); }
    private Button secondary(String label) { return action(label, navyPanel, mint); }
    private Button lightAction(String label) { return action(label, paper, navy); }

    /** One stable video component for every in-app exercise demonstration. */
    private View exerciseMedia(String trainer, Coach.Exercise exercise, int height) {
        int video = ExerciseVideos.forExercise(this, trainer, exercise.name);
        if (video != 0) {
            FrameLayout mediaCard = new FrameLayout(this);
            mediaCard.setBackground(surface(navyPanel, 18, 1, mint));
            mediaCard.setClipToOutline(true);
            mediaCard.setClipChildren(true);
            mediaCard.setContentDescription("Coach " + ("Male".equals(trainer) ? "Leo" : "Mia") + " demonstrating " + exercise.name);
            LoopingExerciseVideoView demonstration = new LoopingExerciseVideoView(this, video);
            mediaCard.addView(demonstration, new FrameLayout.LayoutParams(-1, -1));
            return mediaCard;
        }
        ExerciseAnimationView fallback = new ExerciseAnimationView(this);
        fallback.setContentDescription("Exercise illustration for " + exercise.name);
        fallback.setBackground(surface(navyPanel, 18, 1, mint));
        return fallback;
    }

    /** Posters make dense selection screens fast; playback is reserved for the focused exercise. */
    private ImageView exercisePoster(String trainer, Coach.Exercise exercise) {
        ImageView poster = new ImageView(this);
        int visual = ExerciseVisuals.forExercise(this, trainer, exercise.name);
        if (visual != 0) poster.setImageResource(visual);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackground(surface(navyPanel, 14, 0, Color.TRANSPARENT));
        poster.setClipToOutline(true);
        poster.setContentDescription(null);
        return poster;
    }

    private void addButton(LinearLayout body, Button button) {
        body.addView(button, new LinearLayout.LayoutParams(-1, dp(54)));
    }

    private EditText input(String hint, int inputType) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setTextSize(16);
        field.setTextColor(ink);
        field.setHintTextColor(muted);
        field.setInputType(inputType);
        field.setSingleLine(true);
        field.setPadding(dp(15), dp(7), dp(15), dp(7));
        field.setBackground(surface(paper, 14, 1, cardLine));
        field.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(54)));
        return field;
    }

    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show(); }

    // Signed-out flow

    private void showWelcome() {
        LinearLayout body = signedOutPage();
        gap(body, 18);
        TextView welcome = heading("Welcome", 42, Color.WHITE);
        welcome.setGravity(Gravity.CENTER);
        body.addView(welcome, new LinearLayout.LayoutParams(-1, -2));
        gap(body, 5);
        TextView motto = text("Your Health is Your Wealth", 17, mint);
        motto.setGravity(Gravity.CENTER);
        body.addView(motto, new LinearLayout.LayoutParams(-1, -2));
        gap(body, 24);

        LinearLayout coaches = new LinearLayout(this);
        coaches.setOrientation(LinearLayout.HORIZONTAL);
        addWelcomeCoach(coaches, "Female", "Mia", R.drawable.mia_launcher);
        addWelcomeCoach(coaches, "Male", "Leo", R.drawable.leo_launcher);
        body.addView(coaches);
        gap(body, 12);

        TextView selection = text("Selected trainer: Coach " + ("Male".equals(selectedTrainer) ? "Leo" : "Mia"), 13, mint);
        selection.setGravity(Gravity.CENTER);
        body.addView(selection, new LinearLayout.LayoutParams(-1, -2));
        gap(body, 22);

        Button signIn = primary("Sign in");
        signIn.setOnClickListener(view -> showLogin());
        addButton(body, signIn);
        gap(body, 10);
        Button create = secondary("Create account");
        create.setOnClickListener(view -> showRegistration());
        addButton(body, create);
    }

    private void addWelcomeCoach(LinearLayout row, String value, String name, int portraitRes) {
        boolean selected = value.equals(selectedTrainer);
        LinearLayout person = new LinearLayout(this);
        person.setOrientation(LinearLayout.VERTICAL);
        person.setGravity(Gravity.CENTER);
        person.setPadding(dp(5), dp(8), dp(5), dp(8));
        person.setBackground(surface(selected ? navyPanel : navy, 22, selected ? 2 : 1, selected ? mint : Color.rgb(35, 53, 78)));
        person.setContentDescription("Choose Coach " + name + (selected ? ", selected" : ""));
        person.setClickable(true);
        person.setFocusable(true);
        person.setOnClickListener(view -> {
            selectedTrainer = value;
            showWelcome();
        });

        ImageView photo = new ImageView(this);
        photo.setImageResource(portraitRes);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setBackground(surface(navyPanel, 99, 2, selected ? mint : Color.WHITE));
        photo.setClipToOutline(true);
        photo.setContentDescription("Coach " + name);
        person.addView(photo, new LinearLayout.LayoutParams(dp(122), dp(122)));
        gap(person, 7);
        TextView label = heading("Coach " + name, 15, Color.WHITE);
        label.setGravity(Gravity.CENTER);
        person.addView(label, new LinearLayout.LayoutParams(-1, -2));
        if (selected) {
            TextView selectedLabel = heading("SELECTED", 10, mint);
            selectedLabel.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams selectedParams = new LinearLayout.LayoutParams(-1, -2);
            selectedParams.topMargin = dp(2);
            person.addView(selectedLabel, selectedParams);
        }
        LinearLayout.LayoutParams personParams = new LinearLayout.LayoutParams(0, -2, 1);
        personParams.leftMargin = dp(4);
        personParams.rightMargin = dp(4);
        row.addView(person, personParams);
    }

    private void showLogin() {
        LinearLayout body = signedOutPage();
        gap(body, 42);
        title(body, "SIGN IN", "Welcome back", "Your coach, plan, and progress are ready when you are.");
        EditText email = input("Email address", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        body.addView(email);
        gap(body, 10);
        EditText password = input("Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        body.addView(password);
        gap(body, 18);
        Button signIn = primary("Sign in");
        signIn.setOnClickListener(view -> {
            if (AccountStore.authenticate(this, email.getText().toString(), password.getText().toString())) {
                selectedTrainer = AccountStore.get(this).trainer;
                LauncherIconManager.apply(this, selectedTrainer);
                showHome();
            } else {
                toast("That email or password does not match this profile.");
            }
        });
        addButton(body, signIn);
        gap(body, 10);
        Button back = secondary("Back");
        back.setOnClickListener(view -> showWelcome());
        addButton(body, back);
    }

    private void showRegistration() {
        LinearLayout body = signedOutPage();
        title(body, "CREATE ACCOUNT", "Set up your coach", "Your account stays on this device. You can adjust your plan at any time.");
        EditText email = input("Email address", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        body.addView(email);
        gap(body, 10);
        EditText username = input("Username", InputType.TYPE_CLASS_TEXT);
        body.addView(username);
        gap(body, 10);
        EditText password = input("Password — at least 8 characters", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        body.addView(password);
        gap(body, 18);
        body.addView(heading("Your trainer", 18, Color.WHITE));
        gap(body, 7);

        RadioGroup trainers = new RadioGroup(this);
        trainers.setOrientation(RadioGroup.VERTICAL);
        trainers.setContentDescription("Choose your trainer");
        addTrainerOption(trainers, R.id.trainer_female, "Coach Mia — female trainer", "Female".equals(selectedTrainer));
        addTrainerOption(trainers, R.id.trainer_male, "Coach Leo — male trainer", "Male".equals(selectedTrainer));
        body.addView(trainers);
        gap(body, 18);

        Button create = primary("Create my profile");
        create.setOnClickListener(view -> {
            String enteredEmail = email.getText().toString().trim();
            String usernameValue = username.getText().toString().trim();
            String passwordValue = password.getText().toString();
            if (!enteredEmail.contains("@") || !enteredEmail.contains(".")) {
                toast("Enter a valid email address.");
                return;
            }
            if (usernameValue.length() < 3) {
                toast("Choose a username with at least 3 characters.");
                return;
            }
            if (passwordValue.length() < 8) {
                toast("Use a password with at least 8 characters.");
                return;
            }
            selectedTrainer = trainers.getCheckedRadioButtonId() == R.id.trainer_male ? "Male" : "Female";
            AccountStore.create(this, enteredEmail, usernameValue, passwordValue, selectedTrainer);
            LauncherIconManager.apply(this, selectedTrainer);
            showPlans();
        });
        addButton(body, create);
        gap(body, 10);
        Button back = secondary("Back");
        back.setOnClickListener(view -> showWelcome());
        addButton(body, back);
    }

    private void addTrainerOption(RadioGroup group, int id, String label, boolean checked) {
        RadioButton option = new RadioButton(this);
        option.setId(id);
        option.setText(label);
        option.setTextSize(16);
        option.setTextColor(Color.WHITE);
        option.setButtonTintList(ColorStateList.valueOf(mint));
        option.setPadding(dp(3), dp(8), dp(3), dp(8));
        option.setChecked(checked);
        group.addView(option, new RadioGroup.LayoutParams(-1, dp(48)));
    }

    // Main app

    private void showHome() {
        AccountStore.Profile profile = AccountStore.get(this);
        LinearLayout body = appPage("Home");
        title(body, "TODAY", "Train with purpose", "Showing up today builds a stronger tomorrow.");

        // Surface the exact saved plan when one exists; a generic recommendation must not
        // silently replace the movements the user chose in the builder.
        List<Coach.Exercise> recommendation = session.isEmpty()
                ? Coach.recommendation(target, goal, environment)
                : session;
        Coach.Exercise first = recommendation.get(0);
        LinearLayout next = card();
        next.addView(exerciseMedia(profile.trainer, first, 170), new LinearLayout.LayoutParams(-1, dp(170)));
        gap(next, 12);
        TextView nextLabel = heading("NEXT WORKOUT", 12, Color.rgb(67, 98, 139));
        nextLabel.setLetterSpacing(.06f);
        next.addView(nextLabel);
        gap(next, 3);
        next.addView(heading(target + " strength", 23, ink));
        next.addView(text(sessionMinutes + " min  •  " + goal + "  •  " + environment, 14, muted));
        gap(next, 12);
        Button start = primary("Start workout");
        start.setOnClickListener(view -> {
            if (session.isEmpty()) showPlans();
            else showActive(0, 1);
        });
        addButton(next, start);
        body.addView(next);

        gap(body, 20);
        body.addView(heading("This week", 21, Color.WHITE));
        body.addView(text("3 of " + trainingDays + " planned sessions complete", 14, mint));
        gap(body, 9);
        LinearLayout weekly = card();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        String[] states = {"✓", "✓", "✓", "—", "—", "—", "—"};
        LinearLayout weekRow = new LinearLayout(this);
        weekRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < days.length; i++) {
            TextView day = text(days[i] + "\n" + states[i], 12, i < 3 ? navy : muted);
            day.setGravity(Gravity.CENTER);
            day.setLineSpacing(dp(5), 1);
            day.setContentDescription(days[i] + (i < 3 ? ", workout complete" : ", rest or upcoming day"));
            weekRow.addView(day, new LinearLayout.LayoutParams(0, dp(49), 1));
        }
        weekly.addView(weekRow);
        TextView legend = text("✓ completed  •  — upcoming", 12, muted);
        legend.setGravity(Gravity.CENTER);
        weekly.addView(legend, new LinearLayout.LayoutParams(-1, -2));
        body.addView(weekly);

        gap(body, 12);
        LinearLayout summaryRow = new LinearLayout(this);
        summaryRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout sessions = card();
        sessions.addView(heading("3", 28, ink));
        sessions.addView(text("sessions completed", 14, muted));
        summaryRow.addView(sessions, new LinearLayout.LayoutParams(0, -2, 1));
        View space = new View(this);
        summaryRow.addView(space, new LinearLayout.LayoutParams(dp(9), 1));
        LinearLayout ready = card();
        ready.addView(heading("Ready", 22, ink));
        ready.addView(text("Your next session is prepared.", 14, muted));
        summaryRow.addView(ready, new LinearLayout.LayoutParams(0, -2, 1));
        body.addView(summaryRow);

        gap(body, 14);
        Button plans = secondary("View my plan");
        plans.setOnClickListener(view -> showPlans());
        addButton(body, plans);
    }

    private void showPlans() {
        LinearLayout body = appPage("Workouts");
        title(body, "PLAN YOUR WORKOUT", "Build it your way", "Choose a schedule, body area, and equipment. Then select only the exercises you want.");

        body.addView(heading("Plan type", 18, Color.WHITE));
        gap(body, 7);
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < Coach.PLAN_MODES.length; i++) {
            String mode = Coach.PLAN_MODES[i];
            Button option = planToggle(mode, mode.equals(planMode));
            option.setOnClickListener(view -> {
                planMode = mode;
                showPlans();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(50), 1);
            params.rightMargin = dp(i == Coach.PLAN_MODES.length - 1 ? 0 : 6);
            modes.addView(option, params);
        }
        body.addView(modes);
        gap(body, 16);

        body.addView(heading("What do you want to train?", 18, Color.WHITE));
        gap(body, 7);
        for (int index = 0; index < Coach.TARGETS.length; index += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int offset = 0; offset < 2 && index + offset < Coach.TARGETS.length; offset++) {
                String choice = Coach.TARGETS[index + offset];
                Button option = planToggle(choice, choice.equals(target));
                option.setOnClickListener(view -> {
                    target = choice;
                    session.clear();
                    resetExerciseSelection();
                    showPlans();
                });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1);
                params.rightMargin = offset == 0 ? dp(7) : 0;
                row.addView(option, params);
            }
            if (index + 1 >= Coach.TARGETS.length) row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
            body.addView(row);
            gap(body, 7);
        }
        gap(body, 9);

        body.addView(heading("Choose your equipment", 18, Color.WHITE));
        gap(body, 7);
        for (int index = 0; index < Coach.EQUIPMENT_TYPES.length; index += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int offset = 0; offset < 2; offset++) {
                String equipment = Coach.EQUIPMENT_TYPES[index + offset];
                LinearLayout option = equipmentTile(equipment, equipment.equals(equipmentType));
                option.setOnClickListener(view -> {
                    equipmentType = equipment;
                    session.clear();
                    resetExerciseSelection();
                    showExerciseSelection();
                });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(142), 1);
                params.rightMargin = offset == 0 ? dp(7) : 0;
                row.addView(option, params);
            }
            body.addView(row);
            gap(body, 7);
        }

        if (!session.isEmpty()) {
            gap(body, 8);
            LinearLayout current = card();
            current.addView(heading("Current plan", 18, ink));
            current.addView(text(session.size() + " selected movements  •  " + sessionMinutes + " min  •  " + planSchedule(), 14, muted));
            gap(current, 10);
            Button start = primary("Start guided workout");
            start.setOnClickListener(view -> showActive(0, 1));
            addButton(current, start);
            body.addView(current);
        }
        gap(body, 4);
        Button library = secondary("Browse exercise library");
        library.setOnClickListener(item -> showLibrary());
        addButton(body, library);
    }

    private Button planToggle(String label, boolean selected) {
        Button button = action((selected ? "✓  " : "") + label, selected ? mint : navyPanel, selected ? navy : mint);
        button.setTextSize(11);
        button.setAllCaps(false);
        return button;
    }

    private LinearLayout equipmentTile(String label, boolean selected) {
        String copy;
        if ("Dumbbells".equals(label)) copy = "Free weights";
        else if ("Home & household".equals(label)) copy = "Simple home items";
        else if ("Machine assisted".equals(label)) copy = "Guided gym equipment";
        else copy = "No equipment";
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(8), dp(8), dp(8), dp(8));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setContentDescription((selected ? "Selected equipment: " : "Choose equipment: ") + label + ". " + copy);
        tile.setBackground(surface(selected ? mint : navyPanel, 17, selected ? 2 : 1, selected ? coral : Color.rgb(35, 53, 78)));
        List<Coach.Exercise> examples = Coach.planOptions(target, label);
        if (!examples.isEmpty()) {
            tile.addView(exercisePoster(AccountStore.get(this).trainer, examples.get(0)), new LinearLayout.LayoutParams(-1, dp(72)));
            gap(tile, 5);
        }
        TextView name = heading((selected ? "✓ " : "") + label, 11, selected ? navy : Color.WHITE);
        name.setSingleLine(true);
        tile.addView(name);
        TextView detail = text(copy, 9, selected ? navy : mint);
        detail.setSingleLine(true);
        tile.addView(detail);
        return tile;
    }

    private void resetExerciseSelection() {
        availableExercises.clear();
        selectedExercises.clear();
        selectionKey = "";
    }

    private void restoreSavedPlan() {
        PlanStore.SavedPlan saved = PlanStore.load(this);
        if (saved == null) return;
        planMode = saved.mode;
        target = saved.target;
        equipmentType = saved.equipment;
        sessionMinutes = saved.minutes;
        workingWeightKg = saved.weightKg;
        planSets = saved.sets;
        planReps = saved.reps;
        planRestSeconds = saved.restSeconds;
        List<Coach.Exercise> choices = Coach.planOptions(target, equipmentType);
        session = new ArrayList<>();
        for (String name : saved.exerciseNames) {
            for (Coach.Exercise exercise : choices) {
                if (exercise.name.equals(name)) {
                    session.add(Coach.prescribe(exercise, planSets, planReps, planRestSeconds));
                    break;
                }
            }
        }
    }

    private void prepareExerciseSelection() {
        String key = target + "|" + equipmentType;
        if (key.equals(selectionKey) && !availableExercises.isEmpty()) return;
        selectionKey = key;
        availableExercises = Coach.planOptions(target, equipmentType);
        selectedExercises = new ArrayList<>(availableExercises);
    }

    private boolean isSelected(Coach.Exercise exercise) {
        for (Coach.Exercise selected : selectedExercises) if (selected.name.equals(exercise.name)) return true;
        return false;
    }

    private void toggleExercise(Coach.Exercise exercise) {
        if (isSelected(exercise)) {
            for (int i = 0; i < selectedExercises.size(); i++) {
                if (selectedExercises.get(i).name.equals(exercise.name)) {
                    selectedExercises.remove(i);
                    break;
                }
            }
        } else selectedExercises.add(exercise);
    }

    private void showExerciseSelection() {
        prepareExerciseSelection();
        LinearLayout body = appPage("Workouts");
        title(body, planMode.toUpperCase(Locale.ROOT) + " PLAN", target + " · " + equipmentType, "Select every movement you want. You can use all of them or build a smaller session.");

        Button all = selectedExercises.size() == availableExercises.size() ? secondary("Clear all") : lightAction("Select all");
        all.setOnClickListener(view -> {
            if (selectedExercises.size() == availableExercises.size()) selectedExercises.clear();
            else selectedExercises = new ArrayList<>(availableExercises);
            showExerciseSelection();
        });
        addButton(body, all);
        gap(body, 6);
        TextView selectionCount = text(selectedExercises.size() + " of " + availableExercises.size() + " selected", 14, mint);
        body.addView(selectionCount);
        gap(body, 9);

        for (Coach.Exercise exercise : availableExercises) {
            boolean selected = isSelected(exercise);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(15), dp(13), dp(12), dp(13));
            row.setBackground(surface(selected ? Color.rgb(231, 250, 246) : paper, 18, selected ? 2 : 1, selected ? mint : cardLine));
            row.setClickable(true);
            row.setFocusable(true);
            row.setContentDescription((selected ? "Selected " : "Not selected ") + exercise.name + ". " + exercise.description);
            row.setOnClickListener(view -> {
                toggleExercise(exercise);
                showExerciseSelection();
            });
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            ImageView poster = exercisePoster(AccountStore.get(this).trainer, exercise);
            LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(dp(58), dp(74));
            posterParams.rightMargin = dp(11);
            row.addView(poster, posterParams);
            details.addView(heading(exercise.name, 17, ink));
            details.addView(text(exercise.description, 13, muted));
            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
            TextView check = heading(selected ? "✓" : "○", 25, selected ? mint : muted);
            check.setGravity(Gravity.CENTER);
            row.addView(check, new LinearLayout.LayoutParams(dp(38), dp(42)));
            body.addView(row);
            gap(body, 8);
        }

        LinearLayout settings = card();
        settings.addView(heading("Your working sets", 19, ink));
        settings.addView(text("Adjust these before building your plan. You can edit a session later.", 13, muted));
        gap(settings, 10);
        if ("Dumbbells".equals(equipmentType) || "Machine assisted".equals(equipmentType)) {
            addStepper(settings, "Weight", workingWeightKg + " kg", () -> workingWeightKg = Math.max(0, workingWeightKg - 2), () -> workingWeightKg += 2);
        }
        addStepper(settings, "Sets", String.valueOf(planSets), () -> planSets = Math.max(1, planSets - 1), () -> planSets = Math.min(6, planSets + 1));
        addStepper(settings, "Reps", String.valueOf(planReps), () -> planReps = Math.max(1, planReps - 1), () -> planReps = Math.min(30, planReps + 1));
        addStepper(settings, "Rest", planRestSeconds + " sec", () -> planRestSeconds = Math.max(15, planRestSeconds - 15), () -> planRestSeconds = Math.min(300, planRestSeconds + 15));
        body.addView(settings);
        gap(body, 12);

        Button build = primary("Build my " + target.toLowerCase(Locale.ROOT) + " plan");
        build.setOnClickListener(view -> buildSelectedPlan());
        addButton(body, build);
        gap(body, 10);
        Button back = secondary("Back to plan choices");
        back.setOnClickListener(view -> showPlans());
        addButton(body, back);
    }

    private void addStepper(LinearLayout container, String label, String value, Runnable decrease, Runnable increase) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = heading(label, 15, ink);
        row.addView(name, new LinearLayout.LayoutParams(0, dp(44), 1));
        Button minus = compactControl("−");
        minus.setContentDescription("Decrease " + label);
        minus.setOnClickListener(view -> {
            decrease.run();
            showExerciseSelection();
        });
        row.addView(minus, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView number = heading(value, 15, ink);
        number.setGravity(Gravity.CENTER);
        row.addView(number, new LinearLayout.LayoutParams(dp(72), dp(42)));
        Button plus = compactControl("+");
        plus.setContentDescription("Increase " + label);
        plus.setOnClickListener(view -> {
            increase.run();
            showExerciseSelection();
        });
        row.addView(plus, new LinearLayout.LayoutParams(dp(42), dp(42)));
        container.addView(row);
    }

    private Button compactControl(String label) {
        Button button = action(label, navyPanel, mint);
        button.setTextSize(18);
        button.setPadding(0, 0, 0, 0);
        button.setMinHeight(0);
        return button;
    }

    private void buildSelectedPlan() {
        if (selectedExercises.isEmpty()) {
            toast("Select at least one exercise before building your plan.");
            return;
        }
        session = new ArrayList<>();
        for (Coach.Exercise exercise : selectedExercises) {
            session.add(Coach.prescribe(exercise, planSets, planReps, planRestSeconds));
        }
        PlanStore.save(this, planMode, target, equipmentType, sessionMinutes, workingWeightKg, planSets, planReps, planRestSeconds, session);
        showPlanReady();
    }

    private String planSchedule() {
        if ("Daily".equals(planMode)) return "daily plan";
        if ("Weekly".equals(planMode)) return trainingDays + " sessions this week";
        return "custom session";
    }

    private void showPlanReady() {
        LinearLayout body = appPage("Workouts");
        title(body, "PLAN READY", "Your " + target.toLowerCase(Locale.ROOT) + " plan is ready", session.size() + " selected movements  •  " + sessionMinutes + " min  •  " + equipmentType);
        LinearLayout schedule = card();
        schedule.addView(heading("Today", 18, ink));
        schedule.addView(text("Ready when you are  •  " + planSchedule(), 14, muted));
        body.addView(schedule);
        gap(body, 10);
        for (Coach.Exercise exercise : session) {
            LinearLayout item = card();
            item.setOrientation(LinearLayout.HORIZONTAL);
            ImageView poster = exercisePoster(AccountStore.get(this).trainer, exercise);
            LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(dp(66), dp(74));
            posterParams.rightMargin = dp(12);
            item.addView(poster, posterParams);
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.addView(heading(exercise.name, 18, ink));
            String load = ("Dumbbells".equals(equipmentType) || "Machine assisted".equals(equipmentType)) ? "  •  " + workingWeightKg + " kg" : "";
            details.addView(text(exercise.sets + " sets × " + exercise.reps + " reps" + load + "  •  Rest " + exercise.restSeconds + " sec", 14, muted));
            item.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
            body.addView(item);
            gap(body, 8);
        }
        LinearLayout cue = card();
        cue.addView(heading("Coach " + ("Male".equals(AccountStore.get(this).trainer) ? "Leo" : "Mia") + "'s focus", 18, ink));
        cue.addView(text("Move with control, choose a comfortable load, and stop if you feel pain.", 14, muted));
        body.addView(cue);
        gap(body, 12);
        Button start = primary("Start guided workout");
        start.setOnClickListener(view -> showActive(0, 1));
        addButton(body, start);
        gap(body, 10);
        Button scheduleLater = secondary("Schedule for later");
        scheduleLater.setOnClickListener(view -> {
            PlanStore.save(this, planMode, target, equipmentType, sessionMinutes, workingWeightKg, planSets, planReps, planRestSeconds, session);
            toast("Your " + planSchedule() + " is saved on this device.");
        });
        addButton(body, scheduleLater);
        gap(body, 10);
        Button edit = lightAction("Edit plan");
        edit.setOnClickListener(view -> showExerciseSelection());
        addButton(body, edit);
    }

    private void showProgress() {
        LinearLayout body = appPage("Progress");
        title(body, "PROGRESS", "Build consistency", "Track a private weight trend, revisit food ideas, and keep your next workout simple.");

        LinearLayout trend = card();
        trend.addView(heading(WeightStore.configured(this) ? WeightStore.summary(this) : "Set a private starting point", 18, ink));
        trend.addView(text("Your check-ins stay on this device and are for personal reflection, not diagnosis.", 14, muted));
        gap(trend, 12);
        Button weight = primary("Weight trend check-in");
        weight.setOnClickListener(view -> showWeightTracker());
        addButton(trend, weight);
        body.addView(trend);
        gap(body, 12);

        LinearLayout fuel = card();
        fuel.addView(heading("Fuel ideas for " + goal, 18, ink));
        fuel.addView(text("Flexible meal ideas based on your training focus, preferences, and needs.", 14, muted));
        gap(fuel, 12);
        Button meals = lightAction("Explore meal ideas");
        meals.setOnClickListener(view -> showMeals());
        addButton(fuel, meals);
        body.addView(fuel);
        gap(body, 16);

        body.addView(heading("Keep it useful", 20, Color.WHITE));
        gap(body, 5);
        body.addView(text("Use the trend as a personal check-in. Adjust load, range, pace, or rest when needed.", 15, Color.rgb(211, 225, 233)));
    }

    private void showProfile() {
        AccountStore.Profile profile = AccountStore.get(this);
        String coach = "Male".equals(profile.trainer) ? "Coach Leo" : "Coach Mia";
        LinearLayout body = appPage("Settings");
        title(body, "ACCOUNT", profile.username, "Your trainer, preferences, and update controls live here.");

        LinearLayout identity = card();
        identity.addView(chip(coach));
        gap(identity, 10);
        identity.addView(heading(profile.email, 18, ink));
        identity.addView(text("Your local profile is stored on this device.", 14, muted));
        body.addView(identity);
        gap(body, 12);

        LinearLayout privacy = card();
        privacy.addView(heading("Privacy", 19, ink));
        privacy.addView(text("This version stores your registration details locally and does not send them to an account server.", 14, muted));
        body.addView(privacy);
        gap(body, 12);

        Button updates = lightAction("Check for OTA update");
        updates.setOnClickListener(view -> showUpdate());
        addButton(body, updates);
        gap(body, 10);
        Button preferences = primary("Adjust workout preferences");
        preferences.setOnClickListener(view -> showWizard());
        addButton(body, preferences);
        gap(body, 10);
        Button signOut = secondary("Sign out and clear this device");
        signOut.setOnClickListener(view -> {
            AccountStore.clear(this);
            PlanStore.clear(this);
            WeightStore.clear(this);
            session.clear();
            selectedExercises.clear();
            selectedTrainer = "Female";
            showWelcome();
        });
        addButton(body, signOut);
    }

    // Plan wizard

    private void showWizard() { showPlans(); }

    private void wizardTitle(LinearLayout body, int step, String main, String copy) {
        body.addView(chip("PLAN SETUP  •  STEP " + step + " OF 4"));
        gap(body, 14);
        title(body, "PLAN WIZARD", main, copy);
    }

    private void showPlanGoal() {
        LinearLayout body = appPage("Workouts");
        wizardTitle(body, 1, "Choose your goal", "Your goal adjusts rep targets, rest, and coaching emphasis.");
        RadioGroup options = new RadioGroup(this);
        options.setOrientation(RadioGroup.VERTICAL);
        int[] ids = {R.id.goal_strength, R.id.goal_muscle, R.id.goal_fat_loss, R.id.goal_endurance, R.id.goal_wellbeing};
        for (int i = 0; i < Coach.GOALS.length; i++) {
            RadioButton option = new RadioButton(this);
            option.setId(ids[i]);
            option.setText(Coach.GOALS[i]);
            option.setTextSize(16);
            option.setTextColor(Color.WHITE);
            option.setButtonTintList(ColorStateList.valueOf(mint));
            option.setPadding(dp(3), dp(7), dp(3), dp(7));
            option.setChecked(goal.equals(Coach.GOALS[i]));
            options.addView(option, new RadioGroup.LayoutParams(-1, dp(46)));
        }
        options.setOnCheckedChangeListener((group, id) -> {
            if (id == R.id.goal_strength) goal = Coach.GOALS[0];
            else if (id == R.id.goal_muscle) goal = Coach.GOALS[1];
            else if (id == R.id.goal_fat_loss) goal = Coach.GOALS[2];
            else if (id == R.id.goal_endurance) goal = Coach.GOALS[3];
            else goal = Coach.GOALS[4];
        });
        body.addView(options);
        gap(body, 18);
        Button next = primary("Continue to workout focus");
        next.setOnClickListener(view -> showPlanFocus());
        addButton(body, next);
    }

    private void showPlanFocus() {
        LinearLayout body = appPage("Workouts");
        wizardTitle(body, 2, "Choose a workout focus", "Pick what you want to train. Your selected coach will demonstrate compatible movements.");
        List<Coach.Exercise> choices = Coach.library().get(target);
        if (choices != null && !choices.isEmpty()) {
            Coach.Exercise first = choices.get(0);
            LinearLayout preview = card();
            preview.setPadding(0, 0, 0, 0);
            preview.addView(exerciseMedia(AccountStore.get(this).trainer, first, 178), new LinearLayout.LayoutParams(-1, dp(178)));
            TextView label = heading("PREVIEW  •  " + first.name, 14, ink);
            label.setPadding(dp(16), dp(10), dp(16), dp(14));
            preview.addView(label);
            body.addView(preview);
            gap(body, 12);
        }
        for (String choice : Coach.TARGETS) {
            Button option = choice.equals(target) ? primary("Selected  •  " + choice) : secondary(choice);
            option.setOnClickListener(view -> {
                target = choice;
                showPlanFocus();
            });
            addButton(body, option);
            gap(body, 7);
        }
        Button next = primary("Continue to training setup");
        next.setOnClickListener(view -> showPlanContext());
        addButton(body, next);
        gap(body, 10);
        Button back = secondary("Back");
        back.setOnClickListener(view -> showPlanGoal());
        addButton(body, back);
    }

    private void showPlanContext() {
        LinearLayout body = appPage("Workouts");
        wizardTitle(body, 3, "Set your training context", "Only compatible movements are recommended for the place where you plan to train.");
        body.addView(heading("Where will you train?", 18, Color.WHITE));
        gap(body, 7);
        for (String place : Coach.ENVIRONMENTS) {
            Button option = place.equals(environment) ? primary("Selected  •  " + place) : secondary(place);
            option.setOnClickListener(view -> {
                environment = place;
                showPlanContext();
            });
            addButton(body, option);
            gap(body, 7);
        }
        gap(body, 5);
        body.addView(heading("Session length", 18, Color.WHITE));
        gap(body, 7);
        LinearLayout lengths = new LinearLayout(this);
        lengths.setOrientation(LinearLayout.HORIZONTAL);
        for (int minutes : new int[]{20, 30, 45}) {
            Button length = minutes == sessionMinutes ? primary(minutes + " min") : secondary(minutes + " min");
            length.setOnClickListener(view -> {
                sessionMinutes = minutes;
                showPlanContext();
            });
            LinearLayout.LayoutParams lengthParams = new LinearLayout.LayoutParams(0, dp(50), 1);
            lengthParams.rightMargin = dp(minutes == 45 ? 0 : 7);
            lengths.addView(length, lengthParams);
        }
        body.addView(lengths);
        gap(body, 18);
        Button next = primary("Review my plan");
        next.setOnClickListener(view -> showPlanReview());
        addButton(body, next);
        gap(body, 10);
        Button back = secondary("Back");
        back.setOnClickListener(view -> showPlanFocus());
        addButton(body, back);
    }

    private void showPlanReview() {
        session = Coach.recommendation(target, goal, environment);
        LinearLayout body = appPage("Workouts");
        wizardTitle(body, 4, "Your plan is ready", "Review it before you begin. You can revisit these choices whenever you need.");
        body.addView(chip(goal + "  •  " + environment + "  •  " + sessionMinutes + " min"));
        gap(body, 14);
        for (int i = 0; i < session.size(); i++) {
            Coach.Exercise exercise = session.get(i);
            LinearLayout item = card();
            item.addView(heading(String.format(Locale.US, "%02d  %s", i + 1, exercise.name), 18, ink));
            item.addView(text(exercise.sets + " sets × " + exercise.reps + " reps  •  " + exercise.restSeconds + " sec rest", 14, muted));
            item.addView(text(exercise.cue, 14, muted));
            body.addView(item);
            gap(body, 8);
        }
        Button save = primary("Save and start this plan");
        save.setOnClickListener(view -> showPlans());
        addButton(body, save);
        gap(body, 10);
        Button back = secondary("Back");
        back.setOnClickListener(view -> showPlanContext());
        addButton(body, back);
    }

    // Guided session

    private void showActive(int index, int set) {
        if (index >= session.size()) {
            showComplete();
            return;
        }
        Coach.Exercise exercise = session.get(index);
        LinearLayout body = appPage("Workouts");
        title(body, "GUIDED WORKOUT", exercise.target, "Exercise " + (index + 1) + " of " + session.size() + "  •  Set " + set + " of " + exercise.sets);

        LinearLayout demoCard = card();
        demoCard.setPadding(0, 0, 0, 0);
        demoCard.addView(exerciseMedia(AccountStore.get(this).trainer, exercise, 235), new LinearLayout.LayoutParams(-1, dp(235)));
        body.addView(demoCard);
        gap(body, 14);
        body.addView(heading(exercise.name, 25, Color.WHITE));
        body.addView(text(exercise.sets + " sets × " + exercise.reps + " reps", 17, mint));
        gap(body, 10);

        LinearLayout cue = card();
        cue.addView(heading("Coach cue", 15, ink));
        cue.addView(text(exercise.cue, 16, muted));
        body.addView(cue);
        gap(body, 12);

        LinearLayout counters = new LinearLayout(this);
        counters.setOrientation(LinearLayout.HORIZONTAL);
        addCounter(counters, "SET", set + " / " + exercise.sets);
        addCounter(counters, "REST", format(exercise.restSeconds));
        addCounter(counters, "REPS", "0 / " + exercise.reps);
        body.addView(counters);
        gap(body, 14);
        Button done = primary("Reps completed");
        done.setOnClickListener(view -> startRest(index, set, exercise));
        addButton(body, done);
        gap(body, 10);
        Button callout = secondary("Hear rep guidance");
        callout.setOnClickListener(view -> say("Set " + set + ". Complete " + exercise.reps + " controlled reps. " + exercise.cue));
        addButton(body, callout);
    }

    private void addCounter(LinearLayout row, String label, String value) {
        LinearLayout item = card();
        item.setPadding(dp(10), dp(11), dp(10), dp(11));
        TextView itemLabel = heading(label, 10, muted);
        itemLabel.setLetterSpacing(.06f);
        item.addView(itemLabel);
        item.addView(heading(value, 17, ink));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.rightMargin = dp(6);
        row.addView(item, params);
    }

    private void startRest(int index, int set, Coach.Exercise exercise) {
        LinearLayout body = appPage("Workouts");
        title(body, "REST CLOCK", "Recover, then go again", "Your next set begins when the timer ends.");
        LinearLayout timerCard = card();
        TextView timer = heading(format(exercise.restSeconds), 52, ink);
        timer.setGravity(Gravity.CENTER);
        timer.setContentDescription("Rest time remaining " + exercise.restSeconds + " seconds");
        timerCard.addView(timer, new LinearLayout.LayoutParams(-1, dp(98)));
        TextView reminder = text("Breathe slowly. At 10 seconds, Coach Mia will count you in.", 15, muted);
        reminder.setGravity(Gravity.CENTER);
        timerCard.addView(reminder);
        body.addView(timerCard);
        gap(body, 18);
        Button skip = primary("Skip rest and continue");
        skip.setOnClickListener(view -> {
            if (restTimer != null) restTimer.cancel();
            advance(index, set, exercise);
        });
        addButton(body, skip);
        say("Set complete. Rest for " + exercise.restSeconds + " seconds.");
        restTimer = new CountDownTimer(exercise.restSeconds * 1000L, 1000) {
            @Override public void onTick(long left) {
                int seconds = (int) Math.ceil(left / 1000d);
                timer.setText(format(seconds));
                timer.setContentDescription("Rest time remaining " + seconds + " seconds");
                if (seconds <= 10 && seconds >= 1) say(String.valueOf(seconds));
            }

            @Override public void onFinish() {
                say("Let's go.");
                advance(index, set, exercise);
            }
        }.start();
    }

    private void advance(int index, int set, Coach.Exercise exercise) {
        if (set < exercise.sets) showActive(index, set + 1);
        else showActive(index + 1, 1);
    }

    private String format(int seconds) {
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private void say(String words) {
        if (voice != null) voice.speak(words, TextToSpeech.QUEUE_FLUSH, null, "coach");
    }

    // Library, progress and account utilities

    private void showLibrary() {
        String trainer = AccountStore.get(this).trainer;
        String trainerName = "Male".equals(trainer) ? "Leo" : "Mia";
        LinearLayout body = appPage("Workouts");
        title(body, "EXERCISE LIBRARY", "Every move, filmed", "Open a movement for a smooth, looping Coach " + trainerName + " demonstration and a concise form cue.");
        for (String group : Coach.TARGETS) {
            body.addView(heading(group, 22, Color.WHITE));
            gap(body, 8);
            List<Coach.Exercise> exercises = Coach.library().get(group);
            for (Coach.Exercise exercise : exercises) {
                LinearLayout item = card();
                item.setOrientation(LinearLayout.HORIZONTAL);
                ImageView poster = exercisePoster(trainer, exercise);
                LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(dp(82), dp(104));
                posterParams.rightMargin = dp(12);
                item.addView(poster, posterParams);
                LinearLayout details = new LinearLayout(this);
                details.setOrientation(LinearLayout.VERTICAL);
                details.addView(heading(exercise.name, 19, ink));
                details.addView(text(exercise.description, 15, muted));
                TextView cue = text("Coach cue: " + exercise.cue, 14, ink);
                LinearLayout.LayoutParams cueParams = new LinearLayout.LayoutParams(-1, -2);
                cueParams.topMargin = dp(5);
                details.addView(cue, cueParams);
                item.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
                item.setContentDescription(exercise.name + ". " + exercise.description + " Coach cue: " + exercise.cue);
                item.setClickable(true);
                item.setFocusable(true);
                item.setOnClickListener(view -> showExerciseDetail(exercise));
                body.addView(item);
                gap(body, 10);
            }
            gap(body, 10);
        }
    }

    private void showExerciseDetail(Coach.Exercise exercise) {
        String trainer = AccountStore.get(this).trainer;
        LinearLayout body = appPage("Workouts");
        title(body, "EXERCISE DEMO", exercise.name, "Smooth local loop • " + exercise.target + " • Offline-ready");
        LinearLayout demoCard = card();
        demoCard.setPadding(0, 0, 0, 0);
        demoCard.addView(exerciseMedia(trainer, exercise, 270), new LinearLayout.LayoutParams(-1, dp(270)));
        body.addView(demoCard);
        gap(body, 13);
        LinearLayout guidance = card();
        guidance.addView(heading("Coach cue", 16, ink));
        guidance.addView(text(exercise.cue, 16, muted));
        gap(guidance, 8);
        guidance.addView(heading("Suggested starting point", 16, ink));
        guidance.addView(text(exercise.sets + " sets × " + exercise.reps + " reps • Rest " + exercise.restSeconds + " sec", 15, muted));
        body.addView(guidance);
        gap(body, 12);
        Button choose = primary("Add to my " + target.toLowerCase(Locale.ROOT) + " plan");
        choose.setOnClickListener(view -> {
            target = exercise.target;
            showPlans();
        });
        addButton(body, choose);
        gap(body, 10);
        Button back = secondary("Back to exercise library");
        back.setOnClickListener(view -> showLibrary());
        addButton(body, back);
    }

    private void showWeightTracker() {
        LinearLayout body = appPage("Progress");
        title(body, "WEIGHT TREND", "Check your direction, kindly", "Your private log compares entries with your own target. It does not diagnose health, body composition, or fitness.");
        if (!WeightStore.configured(this)) {
            EditText current = input("Starting weight", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            body.addView(current);
            gap(body, 10);
            EditText targetWeight = input("Personal target weight", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            body.addView(targetWeight);
            gap(body, 15);
            body.addView(heading("Unit", 18, Color.WHITE));
            RadioGroup units = new RadioGroup(this);
            units.setOrientation(RadioGroup.HORIZONTAL);
            RadioButton kg = unitOption(R.id.unit_kg, "kg", true);
            RadioButton lb = unitOption(R.id.unit_lb, "lb", false);
            units.addView(kg);
            units.addView(lb);
            body.addView(units);
            gap(body, 18);
            Button save = primary("Save my starting point");
            save.setOnClickListener(view -> {
                try {
                    float now = Float.parseFloat(current.getText().toString());
                    float goalWeight = Float.parseFloat(targetWeight.getText().toString());
                    if (now <= 0 || goalWeight <= 0) throw new NumberFormatException();
                    WeightStore.start(this, now, goalWeight, units.getCheckedRadioButtonId() == R.id.unit_lb ? "lb" : "kg");
                    showWeightTracker();
                } catch (Exception ignored) {
                    toast("Enter positive weight values to begin.");
                }
            });
            addButton(body, save);
        } else {
            LinearLayout summary = card();
            summary.addView(heading(WeightStore.summary(this), 20, ink));
            summary.addView(text("A private trend is more useful than any single check-in.", 14, muted));
            body.addView(summary);
            gap(body, 14);
            EditText current = input("Today's weight", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            body.addView(current);
            gap(body, 12);
            Button checkIn = primary("Save check-in");
            checkIn.setOnClickListener(view -> {
                try {
                    float value = Float.parseFloat(current.getText().toString());
                    if (value <= 0) throw new NumberFormatException();
                    WeightStore.checkIn(this, value);
                    showWeightTracker();
                } catch (Exception ignored) {
                    toast("Enter a positive weight to check in.");
                }
            });
            addButton(body, checkIn);
            gap(body, 16);
            body.addView(text("If weight changes unexpectedly, or food and weight are causing distress, pause the goal and speak with a qualified healthcare professional.", 14, Color.rgb(211, 225, 233)));
        }
    }

    private RadioButton unitOption(int id, String label, boolean checked) {
        RadioButton option = new RadioButton(this);
        option.setId(id);
        option.setText(label);
        option.setTextColor(Color.WHITE);
        option.setTextSize(16);
        option.setButtonTintList(ColorStateList.valueOf(mint));
        option.setChecked(checked);
        return option;
    }

    private void showMeals() {
        LinearLayout body = appPage("Nutrition");
        title(body, "FUEL IDEAS", "Simple meals for " + goal, "Use these as flexible ideas. Adjust portions and ingredients to your preferences, budget, allergies, and dietary needs.");
        for (String meal : Coach.meals(goal)) {
            LinearLayout mealCard = card();
            mealCard.addView(heading(meal, 18, ink));
            mealCard.addView(text("A balanced option to adapt around your day and training.", 14, muted));
            body.addView(mealCard);
            gap(body, 9);
        }
        gap(body, 7);
        body.addView(heading("General fuel suggestion", 19, Color.WHITE));
        gap(body, 5);
        body.addView(text("Aim for protein and colourful plants across the day, carbohydrates around training if useful, and regular hydration. For medical nutrition or weight-change targets, consult a registered dietitian.", 15, Color.rgb(211, 225, 233)));
    }

    private void showUpdate() {
        LinearLayout body = appPage("Settings");
        title(body, "APP UPDATES", "Keep your coach current", "Updates are checked over HTTPS, verified against the published SHA-256, and then verified again by Android's signing identity.");
        LinearLayout statusCard = card();
        TextView status = heading("Checking the update channel…", 18, ink);
        statusCard.addView(status);
        body.addView(statusCard);
        gap(body, 18);
        Button back = secondary("Back to account");
        back.setOnClickListener(view -> showProfile());
        addButton(body, back);
        new Thread(() -> {
            try {
                UpdateManager.Info info = UpdateManager.check();
                runOnUiThread(() -> {
                    if (info == null) {
                        status.setText("You already have the latest published version.");
                        return;
                    }
                    status.setText("Version " + info.name + " is ready to download.");
                    Button install = primary("Download and install " + info.name);
                    install.setOnClickListener(view -> downloadUpdate(info, status));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(54));
                    params.bottomMargin = dp(10);
                    body.addView(install, 1, params);
                });
            } catch (Exception ignored) {
                runOnUiThread(() -> status.setText("The update channel could not be reached. Try again when you have a connection."));
            }
        }).start();
    }

    private void downloadUpdate(UpdateManager.Info info, TextView status) {
        status.setText("Downloading and verifying update…");
        new Thread(() -> {
            try {
                UpdateManager.downloadAndInstall(this, info);
                runOnUiThread(() -> status.setText("Verified. Android will now ask you to confirm the install."));
            } catch (Exception error) {
                runOnUiThread(() -> status.setText("Update could not be verified: " + error.getMessage()));
            }
        }).start();
    }

    private void showComplete() {
        LinearLayout body = appPage("Workouts");
        title(body, "SESSION COMPLETE", "Strong work", "You completed your " + target + " plan. Hydrate, recover, and let your next session start with a fresh choice.");
        Button home = primary("Return home");
        home.setOnClickListener(view -> showHome());
        addButton(body, home);
    }
}
