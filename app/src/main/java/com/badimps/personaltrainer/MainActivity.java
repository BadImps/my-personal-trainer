package com.badimps.personaltrainer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.InputType;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.core.content.ContextCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.text.SimpleDateFormat;

/** The complete on-device My Personal Trainer experience. */
public final class MainActivity extends FragmentActivity {
    public static final String EXTRA_START_GUIDED_WORKOUT = "start_guided_workout";
    private int navy = CoachHeroDesign.NAVY;
    private int navyPanel = CoachHeroDesign.NAVY_PANEL;
    private int coral = CoachHeroDesign.CORAL;
    private int mint = CoachHeroDesign.MINT;
    private int paper = CoachHeroDesign.PAPER;
    private int ink = CoachHeroDesign.INK;
    private int muted = CoachHeroDesign.MUTED;
    private int cardLine = CoachHeroDesign.CARD_LINE;
    private boolean reducedMotion;
    /** Only one collectible card may be open at once, keeping the grid readable. */
    private FrameLayout flippedCoachCard;
    private View flippedCoachFront;
    private View flippedCoachBack;
    /** Image-led focuses for the approved plan-flow. Core includes trunk work; there is no Abs category. */
    private static final String[] WIZARD_TARGETS = {"Upper", "Lower", "Arms", "Chest", "Legs", "Full Body", "Back", "Shoulders", "Core", "Endurance"};

    private String goal = Coach.GOALS[1];
    private String target = "Full Body";
    private String environment = Coach.ENVIRONMENTS[0];
    private String selectedTrainer = "Female";
    private String planMode = Coach.PLAN_MODES[0];
    private String equipmentType = Coach.EQUIPMENT_TYPES[0];
    private final List<String> selectedEquipmentTypes = new ArrayList<>();
    private int sessionMinutes = 45;
    private boolean customDuration;
    private int trainingDays = 3;
    private int workingWeightKg = 20;
    private int planSets = 3;
    private int planReps = 10;
    private int planRestSeconds = 180;
    private int planCooldownSeconds = WorkoutTiming.DEFAULT_COOLDOWN_SECONDS;
    private boolean planReminderEnabled;
    private int planReminderHour = 18;
    private int planReminderMinute = 0;
    private final String[] weekDays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    /** Monday / Wednesday / Friday is the default weekly rhythm; unselected days are explicit rest days. */
    private boolean[] weeklyWorkoutDays = {true, false, true, false, true, false, false};
    /** Daily plans use one date; Weekly plans retain one or two focus areas for each training day. */
    private final List<String> selectedTargets = new ArrayList<>();
    private final List<List<String>> weeklyDayTargets = new ArrayList<>();
    private final List<List<Coach.Exercise>> weeklySessions = new ArrayList<>();
    private int activeScheduleDay = 2;
    private int dailyScheduledDay = 2;
    private int selectedWeekOffset;
    /** Home can browse confirmed schedules without changing the week being edited in the planner. */
    private int homeWeekOffset;
    private static final int HOME_UPCOMING_WEEK_COUNT = 8;
    private List<Coach.Exercise> session = new ArrayList<>();
    private List<Coach.Exercise> availableExercises = new ArrayList<>();
    private List<Coach.Exercise> selectedExercises = new ArrayList<>();
    private String selectionKey = "";
    private TextToSpeech voice;
    private CountDownTimer restTimer;
    private final Handler workoutHandler = new Handler(Looper.getMainLooper());
    /** Runs when an utterance actually reaches Android's audio pipeline. */
    private final Map<String, Runnable> speechStartCallbacks = new HashMap<>();
    private final Map<String, Runnable> speechDoneCallbacks = new HashMap<>();
    private boolean ttsReady;
    private Voice coachTtsVoice;
    private boolean coachVoiceVerified;
    private float coachSpeechPitch = 1f;
    private float coachSpeechRate = .92f;
    private String coachVoiceStatus = "Checking installed Android voices…";
    private ToneGenerator toneGenerator;
    private long speechSequence;
    private long speechGeneration;
    private Runnable setClockUpdate;
    private WorkoutTiming.SetTracker setTracker;
    private int timingExerciseIndex = -1;
    /** In-memory until the guided workout completes normally; emergency stops never persist it. */
    private final List<SetTimeStore.SetEntry> currentExerciseSetTimes = new ArrayList<>();
    private final List<SetTimeStore.ExerciseSummary> completedExerciseSetTimes = new ArrayList<>();
    private int openingPhraseIndex;
    private int completionPhraseIndex;
    private long guidedWorkoutStartedAt;
    /** Kept separately so the between-exercise report can show actual cooldown, not just set rest. */
    private long guidedSetRestMillis;
    private long guidedCooldownMillis;
    /** Rest completed for the exercise currently being shown in the cooldown report. */
    private long currentExerciseSetRestMillis;
    /** Recovery deliberately skipped by the user; it is removed from the workout allowance. */
    private long skippedCooldownMillis;
    private long skippedSetRestMillis;
    private long guidedWorkoutAllowanceMillis;
    private long recoverySegmentStartedAt = -1L;
    private boolean recoverySegmentIsCooldown;
    private Runnable workoutRemainingUpdate;
    /** The active scroll surface is retained so in-place option changes do not throw a user to the top. */
    private ScrollView activeScrollView;
    /** Non-null only while a legacy Workout wizard page is being composed. */
    private FrameLayout legacyWorkoutWizardHero;
    /** Permanent activity host: page changes replace its child, never the entire Android window. */
    private FrameLayout screenHost;
    private int pendingScrollRestore = -1;
    /** Each dashboard tab owns its reading position; bottom navigation must never throw it away. */
    private final Map<String, Integer> dashboardScrollPositions = new HashMap<>();
    /** The horizontally scrolling Coach Hero navigation is stateful too; rebuilding a tab must not snap it to Home. */
    private final Map<String, Integer> dashboardNavigationPositions = new HashMap<>();
    private String activeDashboardScrollKey = "";
    /** True only while a selector redraws its current page; full navigation still receives its entrance motion. */
    private boolean suppressNextPageEntrance;
    /** Android back gestures walk real app history before the activity can be closed. */
    private final ArrayDeque<String> pageHistory = new ArrayDeque<>();
    private String currentPageKey = "";
    private boolean restoringPageHistory;
    private boolean guidedSessionRecorded;
    private boolean guidedWorkoutAborted;
    private UpdateManager.Info pendingOtaInstall;
    private Runnable otaStatusPoll;
    private boolean otaVerificationInFlight;
    /** Changes whenever setContentView replaces a screen; detached OTA callbacks must not act. */
    private int pageGeneration;
    private int otaPageGeneration = -1;
    private boolean showingUpdatePage;
    private static final int REQUEST_LIVE_LOCATION = 4102;
    private static final int REQUEST_PROFILE_PHOTO = 4103;
    private static final int REQUEST_OFFLINE_MAP = 4104;
    private String pendingLiveTrackingMode;
    private String selectedProfilePhotoUri;
    private BroadcastReceiver liveTrackingReceiver;
    private int schedulerMonthOffset;
    private int homeCalendarMonthOffset;
    private Calendar schedulerSelectedDate = Calendar.getInstance();
    private String schedulerActivity = EndurancePlanStore.WALKING;
    private int schedulerHour = 18;
    private int schedulerMinute;
    private int schedulerDurationMinutes = 45;
    private String enduranceWizardMode = EndurancePlanStore.WALKING;
    private String endurancePlanMode = "Weekly";
    private boolean[] enduranceWorkoutDays = {true, false, true, false, true, false, false};
    private int enduranceDailyDay = 2;
    private String enduranceWizardName = "";
    private String enduranceWizardOrigin = "";
    private String enduranceWizardDestination = "";
    private float enduranceWizardDistanceKm = 3f;
    private int enduranceWizardMinutes = 35;
    private String enduranceWizardEffort = "Steady";
    /** Timer-only endurance session: no location request, map trace, or background GPS service. */
    private String manualEnduranceMode;
    private long manualEnduranceStartedAt;
    private Runnable manualEnduranceTimer;
    /** Guided endurance is timer-first; walking and running may add manual lap marks. */
    private String guidedEnduranceMode;
    private long guidedEnduranceStartedAt;
    private Runnable guidedEnduranceTimer;
    private final List<Long> guidedEnduranceLaps = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        // The activity is launched with AppTheme.Starting on Android 12+ so the
        // OS can show our branded splash icon. Switch to the regular app theme
        // before content is created.
        setTheme(R.style.AppTheme);
        super.onCreate(state);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (!returnToPreviousPage()) confirmExitApplication();
            }
        });
        applyAppearance();
        // Once Android has started the newly installed package, the old verified APK is no
        // longer useful. Remove only our app-private update artifact and its DownloadManager row.
        UpdateManager.cleanUpInstalledUpdate(this);
        voice = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                voice.setLanguage(Locale.US);
                voice.setSpeechRate(.92f);
                configureSpeechVoices();
                voice.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) { beginSpeech(utteranceId); }
                    @Override public void onError(String utteranceId) { failSpeech(utteranceId); }
                    @Override public void onDone(String utteranceId) { completeSpeech(utteranceId); }
                });
                ttsReady = true;
            }
        });
        pendingOtaInstall = UpdateManager.pendingInstallInfo(this);
        boolean hasLocalAccount = AccountStore.exists(this);
        if (hasLocalAccount) {
            AccountStore.Profile profile = AccountStore.get(this);
            selectedTrainer = profile.trainer;
            LauncherIconManager.apply(this, profile.coachName);
            applyKeepPhoneAwake(profile.keepPhoneAwake);
        }
        initialisePlanSelections();
        restoreSavedPlan();
        // Local AVD-only visual QA entry point. This branch is excluded from release builds
        // and avoids weakening the protected sign-in flow just to inspect a composition.
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_COACH_COLLECTION".equals(getIntent().getAction())) {
            showCoachProfiles();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_COACH_HUB".equals(getIntent().getAction())) {
            showCoachHub();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_COACH_PREFERENCES".equals(getIntent().getAction())) {
            showCoachPreferences();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_SPLASH_CONCEPT_ONE".equals(getIntent().getAction())) {
            showSplashConceptOnePreview();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_ENDURANCE_PLAN_TYPE".equals(getIntent().getAction())) {
            showEnduranceWizardPlanType();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_ENDURANCE_ACTIVITY".equals(getIntent().getAction())) {
            showEnduranceWizardActivity();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_ENDURANCE_SCHEDULE".equals(getIntent().getAction())) {
            showEnduranceWizardSchedule();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_ENDURANCE_ROUTE".equals(getIntent().getAction())) {
            showEnduranceWizardRoute();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_ENDURANCE_REVIEW".equals(getIntent().getAction())) {
            showEnduranceWizardReview();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_NUTRITION".equals(getIntent().getAction())) {
            showMeals();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_HOME".equals(getIntent().getAction())) {
            showHome();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_BODY".equals(getIntent().getAction())) {
            showBodyContext();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_WORKOUT_PLAN_TYPE".equals(getIntent().getAction())) {
            showCoachHeroWorkout();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_WORKOUT_TRAIN".equals(getIntent().getAction())) {
            showPlanTarget();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_WORKOUT_EQUIPMENT".equals(getIntent().getAction())) {
            showPlanEquipment();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_WORKOUT_SETUP".equals(getIntent().getAction())) {
            showPlanSetup();
            return;
        }
        if (BuildConfig.DEBUG && "com.badimps.personaltrainer.DEBUG_WORKOUT_REVIEW".equals(getIntent().getAction())) {
            showPlanReview();
            return;
        }
        if (hasLocalAccount) {
            AccountStore.Profile profile = AccountStore.get(this);
            // A saved account is always protected at launch. Android presents enrolled
            // biometrics or the device credential; the masked password form remains behind it
            // when the device has no credential or the member cancels.
            showLogin(true);
        } else {
            showWelcome();
        }
    }

    @Override protected void onDestroy() {
        stopLiveTrackingUiObserver();
        if (restTimer != null) restTimer.cancel();
        stopSetClock();
        stopOtaStatusPolling();
        workoutHandler.removeCallbacksAndMessages(null);
        speechDoneCallbacks.clear();
        if (voice != null) voice.shutdown();
        if (toneGenerator != null) toneGenerator.release();
        super.onDestroy();
    }

    /** AVD-only launch preview for the approved splash candidate; never included in release routing. */
    private void showSplashConceptOnePreview() {
        setSystemBars();
        FrameLayout splash = new FrameLayout(this);
        splash.setBackgroundColor(navy);
        ImageView artwork = new ImageView(this);
        artwork.setImageResource(R.drawable.splash_concept_one);
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artwork.setContentDescription("My Personal Trainer splash screen preview");
        splash.addView(artwork, new FrameLayout.LayoutParams(-1, -1));
        setContentView(splash);
        splash.setOnClickListener(view -> leaveSplashPreview());
        workoutHandler.postDelayed(this::leaveSplashPreview, 1800L);
    }

    private void leaveSplashPreview() {
        if (isFinishing() || screenHost != null) return;
        if (AccountStore.exists(this)) showLogin(false);
        else showWelcome();
    }

    @Override protected void onResume() {
        super.onResume();
        if (pendingOtaInstall != null) {
            if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) return;
            UpdateManager.Info ready = pendingOtaInstall;
            pendingOtaInstall = null;
            requestVerifiedUpdateInstall(ready);
            return;
        }
        if (showingUpdatePage) showUpdate(UpdateManager.savedInfo(this));
    }

    @Override protected void onPause() {
        stopOtaStatusPolling();
        super.onPause();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != REQUEST_LIVE_LOCATION) return;
        String mode = pendingLiveTrackingMode;
        pendingLiveTrackingMode = null;
        if (mode != null && hasLiveLocationPermission()) startLiveTrackingService(mode);
        else toast("Location access is needed to measure a live endurance session.");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OFFLINE_MAP && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri map = data.getData();
            try { getContentResolver().takePersistableUriPermission(map, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (SecurityException ignored) { }
            askForImportedMapCountry(map);
            return;
        }
        if (requestCode != REQUEST_PROFILE_PHOTO || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri photo = data.getData();
        try { getContentResolver().takePersistableUriPermission(photo, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (SecurityException ignored) { }
        selectedProfilePhotoUri = photo.toString();
        showProfileEditor();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    /** Applies one coherent palette before any screen creates its components. */
    private void applyAppearance() {
        String theme = AppearanceStore.theme(this);
        if (AppearanceStore.OCEAN.equals(theme)) {
            navy = Color.rgb(8, 31, 46); navyPanel = Color.rgb(17, 67, 81);
            coral = Color.rgb(255, 119, 103); mint = Color.rgb(102, 229, 210);
            paper = Color.rgb(246, 250, 252); ink = Color.rgb(27, 49, 65);
            muted = Color.rgb(84, 108, 124); cardLine = Color.rgb(201, 221, 226);
        } else if (AppearanceStore.HIGH_CONTRAST.equals(theme)) {
            navy = Color.rgb(10, 16, 30); navyPanel = Color.rgb(25, 43, 67);
            coral = Color.rgb(255, 125, 108); mint = Color.rgb(117, 244, 209);
            paper = Color.WHITE; ink = Color.rgb(13, 25, 47);
            muted = Color.rgb(62, 78, 106); cardLine = Color.rgb(181, 194, 213);
        } else {
            navy = Color.rgb(16, 23, 42); navyPanel = Color.rgb(26, 58, 72);
            coral = Color.rgb(255, 107, 95); mint = Color.rgb(119, 224, 196);
            paper = Color.rgb(247, 248, 252); ink = Color.rgb(43, 52, 77);
            muted = Color.rgb(107, 122, 147); cardLine = Color.rgb(218, 226, 233);
        }
        reducedMotion = AppearanceStore.reducedMotion(this);
    }

    private AccountStore.Profile profile() { return AccountStore.get(this); }

    private String coachName() { return profile().coachName; }

    private int coachPortrait(String name) {
        // Do not extract a fake headshot from a composited Coach Hero login screen.
        // The approved six login canvases are rendered intact by coachLoginArtwork().
        // Small identity surfaces use the approved app mark until individual source portraits
        // are supplied; this avoids displaying fragments of old or baked UI art.
        return R.drawable.trainer_launcher_premium_v3;
    }

    /** Exact full login references supplied by the user; coach selection changes only this artwork. */
    private int coachLoginArtwork(String name) {
        if ("Amy".equals(name)) return R.drawable.coach_hero_login_amy;
        if ("Claudia".equals(name)) return R.drawable.coach_hero_login_claudia;
        if ("Leo".equals(name)) return R.drawable.coach_hero_login_leo;
        if ("Mark".equals(name)) return R.drawable.coach_hero_login_mark;
        if ("Stephen".equals(name)) return R.drawable.coach_hero_login_stephen;
        return R.drawable.coach_hero_login_mia;
    }

    /** Full-height, background-only Coach Hero assets for the responsive sign-in surface. */
    private int coachHeroBackground(String name) {
        if ("Amy".equals(name)) return R.drawable.coach_hero_background_amy;
        if ("Claudia".equals(name)) return R.drawable.coach_hero_background_claudia;
        if ("Leo".equals(name)) return R.drawable.coach_hero_background_leo;
        if ("Mark".equals(name)) return R.drawable.coach_hero_background_mark;
        if ("Stephen".equals(name)) return R.drawable.coach_hero_background_stephen;
        return R.drawable.coach_hero_background_mia;
    }

    /**
     * Extract the coach's face from the approved Coach Hero login canvas for compact identity
     * surfaces.  The Hero artwork is intentionally a full-body composition, so CENTER_CROP
     * would land on a shoulder in a circular avatar.  This fixed focal crop keeps the genuine
     * supplied face — it does not generate or substitute another portrait.
     */
    private void setCoachHeroFace(ImageView view, String name) {
        view.setImageResource(coachLoginArtwork(name));
        view.setScaleType(ImageView.ScaleType.MATRIX);
        view.addOnLayoutChangeListener((changed, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            Drawable artwork = view.getDrawable();
            if (artwork == null || view.getWidth() == 0 || view.getHeight() == 0) return;
            float sourceWidth = artwork.getIntrinsicWidth();
            float sourceHeight = artwork.getIntrinsicHeight();
            if (sourceWidth <= 0 || sourceHeight <= 0) return;

            // Every approved Coach Hero login composition uses the same right-side face frame.
            // Frame a square around that face and scale it to the circular avatar.
            float scale = Math.max(view.getWidth(), view.getHeight()) / (sourceWidth * .43f);
            float faceCenterX = sourceWidth * .77f;
            float faceCenterY = sourceHeight * .27f;
            Matrix crop = new Matrix();
            crop.setScale(scale, scale);
            crop.postTranslate(view.getWidth() * .5f - faceCenterX * scale,
                    view.getHeight() * .5f - faceCenterY * scale);
            view.setImageMatrix(crop);
        });
    }

    private void applyKeepPhoneAwake(boolean keepAwake) {
        if (keepAwake) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void initialisePlanSelections() {
        // A fresh draft deliberately has no focus until the member chooses one. Do not
        // silently reapply the focus from an earlier plan when a date is tapped.
        if (selectedTargets.isEmpty() && target != null && !target.isEmpty()) selectedTargets.add(target);
        if (selectedEquipmentTypes.isEmpty()) selectedEquipmentTypes.add(equipmentType);
        while (weeklyDayTargets.size() < 7) weeklyDayTargets.add(new ArrayList<>());
        while (weeklySessions.size() < 7) weeklySessions.add(new ArrayList<>());
        for (int i = 0; i < weeklyWorkoutDays.length; i++) {
            if (weeklyWorkoutDays[i] && weeklyDayTargets.get(i).isEmpty() && target != null && !target.isEmpty()) {
                weeklyDayTargets.get(i).add(target);
            }
        }
    }

    /** Opens a genuinely blank workout draft so prior plan choices cannot leak into it. */
    private void beginWorkoutPlanDraft(String mode) {
        planMode = mode;
        target = "";
        selectedTargets.clear();
        weeklyDayTargets.clear();
        weeklySessions.clear();
        resetExerciseSelection();
    }

    private List<String> activeTargets() {
        initialisePlanSelections();
        return "Weekly".equals(planMode) ? weeklyDayTargets.get(activeScheduleDay) : selectedTargets;
    }

    private String focusSummary(List<String> focuses) {
        if (focuses == null || focuses.isEmpty()) return "Choose a focus";
        return String.join(" + ", focuses);
    }

    private String equipmentSummary() {
        initialisePlanSelections();
        return String.join(" + ", selectedEquipmentTypes);
    }

    private String encodeEquipmentTypes() {
        initialisePlanSelections();
        return String.join("|", selectedEquipmentTypes);
    }

    private void restoreEquipmentTypes(String encoded) {
        selectedEquipmentTypes.clear();
        if (encoded != null) {
            for (String value : encoded.split("\\|", -1)) {
                String normalized = normalizeEquipment(value.trim());
                for (String available : Coach.EQUIPMENT_TYPES) {
                    if (available.equals(normalized) && !selectedEquipmentTypes.contains(available) && selectedEquipmentTypes.size() < 2) {
                        selectedEquipmentTypes.add(available);
                    }
                }
            }
        }
        if (selectedEquipmentTypes.isEmpty()) selectedEquipmentTypes.add(Coach.EQUIPMENT_TYPES[0]);
        equipmentType = selectedEquipmentTypes.get(0);
    }

    private void syncPrimaryTarget() {
        List<String> focuses = activeTargets();
        if (!focuses.isEmpty()) target = focuses.get(0);
    }

    private Calendar selectedWeekStart() {
        Calendar week = Calendar.getInstance();
        week.setFirstDayOfWeek(Calendar.MONDAY);
        week.set(Calendar.HOUR_OF_DAY, 0);
        week.set(Calendar.MINUTE, 0);
        week.set(Calendar.SECOND, 0);
        week.set(Calendar.MILLISECOND, 0);
        int daysSinceMonday = (week.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
        week.add(Calendar.DAY_OF_YEAR, -daysSinceMonday + (selectedWeekOffset * 7));
        return week;
    }

    private Calendar selectedWeekDate(int index) {
        Calendar date = selectedWeekStart();
        date.add(Calendar.DAY_OF_YEAR, index);
        return date;
    }

    private String selectedWeekTitle() {
        Calendar start = selectedWeekStart();
        Calendar end = selectedWeekDate(6);
        SimpleDateFormat monthDay = new SimpleDateFormat("MMM d", Locale.getDefault());
        return "Week of " + monthDay.format(start.getTime()) + "–" + monthDay.format(end.getTime());
    }

    private String scheduleDayLabel(int index) {
        return weekDays[index] + " " + selectedWeekDate(index).get(Calendar.DAY_OF_MONTH);
    }

    private String scheduledDateSummary(int index) {
        return new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(selectedWeekDate(index).getTime());
    }

    /** Planning can move from the current week through the final week that begins this month. */
    private int maxSelectableWeekOffset() {
        Calendar current = Calendar.getInstance();
        current.setFirstDayOfWeek(Calendar.MONDAY);
        current.set(Calendar.HOUR_OF_DAY, 0);
        current.set(Calendar.MINUTE, 0);
        current.set(Calendar.SECOND, 0);
        current.set(Calendar.MILLISECOND, 0);
        int sinceMonday = (current.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
        current.add(Calendar.DAY_OF_YEAR, -sinceMonday);
        Calendar monthEnd = Calendar.getInstance();
        monthEnd.set(Calendar.DAY_OF_MONTH, monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        monthEnd.set(Calendar.HOUR_OF_DAY, 23);
        monthEnd.set(Calendar.MINUTE, 59);
        long days = Math.max(0L, (monthEnd.getTimeInMillis() - current.getTimeInMillis()) / (24L * 60L * 60L * 1000L));
        return (int) (days / 7L);
    }

    private boolean[] defaultWeeklyDays() {
        return new boolean[]{true, false, true, false, true, false, false};
    }

    private List<List<String>> defaultWeeklyTargets() {
        ArrayList<List<String>> defaults = new ArrayList<>();
        boolean[] days = defaultWeeklyDays();
        for (int i = 0; i < 7; i++) {
            ArrayList<String> focuses = new ArrayList<>();
            if (days[i]) focuses.add(target);
            defaults.add(focuses);
        }
        return defaults;
    }

    private void persistCurrentWeekSchedule() {
        if ("Weekly".equals(planMode)) {
            WeeklyScheduleStore.save(this, selectedWeekStart(), weeklyWorkoutDays, weeklyDayTargets);
        } else if ("Daily".equals(planMode)) {
            boolean[] days = new boolean[7];
            days[Math.max(0, Math.min(6, dailyScheduledDay))] = true;
            ArrayList<List<String>> focuses = emptyWeeklyTargets();
            focuses.get(Math.max(0, Math.min(6, dailyScheduledDay))).addAll(selectedTargets);
            WeeklyScheduleStore.save(this, selectedWeekStart(), days, focuses);
        }
    }

    private void loadSelectedWeekSchedule() {
        WeeklyScheduleStore.Schedule stored = WeeklyScheduleStore.load(this, selectedWeekStart(), defaultWeeklyDays(), defaultWeeklyTargets());
        weeklyWorkoutDays = stored.workoutDays;
        weeklyDayTargets.clear();
        weeklyDayTargets.addAll(stored.targets);
        initialisePlanSelections();
        syncTrainingDays();
        if (!weeklyWorkoutDays[activeScheduleDay]) {
            for (int i = 0; i < weeklyWorkoutDays.length; i++) if (weeklyWorkoutDays[i]) { activeScheduleDay = i; break; }
        }
        syncPrimaryTarget();
    }

    private void selectPlanningWeek(int requestedOffset) {
        if ("Weekly".equals(planMode)) persistCurrentWeekSchedule();
        selectedWeekOffset = Math.max(0, Math.min(maxSelectableWeekOffset(), requestedOffset));
        if ("Weekly".equals(planMode)) loadSelectedWeekSchedule();
    }

    private String[] wizardLabels() {
        // These are in the exact order members travel through the live wizard.
        // The previous Weekly/Daily ordering showed calendar/time as complete before
        // the member had selected their training area.
        if ("Weekly".equals(planMode)) return new String[]{"Plan", "Train", "Week", "Equipment", "Exercises", "Sets", "Review"};
        if ("Daily".equals(planMode)) return new String[]{"Plan", "Train", "Day", "Equipment", "Exercises", "Sets", "Review"};
        return new String[]{"Plan", "Train", "Time", "Equipment", "Exercises", "Sets", "Review"};
    }

    private int wizardStep(String label) {
        String[] labels = wizardLabels();
        for (int i = 0; i < labels.length; i++) if (label.equals(labels[i])) return i + 1;
        return 1;
    }

    /** Full-width, top-of-hero tracker shared by every Workout wizard screen. */
    private void addWorkoutWizardProgress(FrameLayout hero, String currentStep) {
        int active = Math.max(0, wizardStep(currentStep) - 1);
        addCoachHeroWizardSteps(hero, wizardLabels(), active);
    }

    /** The supplied reference was composed around a compact phone. Modern Android phones are taller. */
    private boolean hasTallPlanViewport() {
        float heightDp = getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density;
        return heightDp >= 820f;
    }

    private int planGridGap() { return hasTallPlanViewport() ? 8 : 5; }
    private int planTargetTileHeight() { return hasTallPlanViewport() ? 150 : 110; }
    private int planEquipmentTileHeight() { return hasTallPlanViewport() ? 184 : 145; }
    private int planExercisePosterSize() { return hasTallPlanViewport() ? 84 : 72; }
    private int planActionHeight() { return hasTallPlanViewport() ? 56 : 47; }
    private int planBackHeight() { return hasTallPlanViewport() ? 40 : 34; }

    private void elevatePlanSurface(View view, boolean selected) {
        applyPremiumTileDepth(view, selected);
    }

    /** Shared physical depth for tiles, cards, and actions across the Coach Hero suite. */
    private void applyPremiumTileDepth(View view, boolean selected) {
        view.setElevation(dp(selected ? 8 : 5));
        view.setTranslationZ(dp(selected ? 2 : 1));
        view.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        view.setClipToOutline(false);
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
        content.setPadding(dp(CoachHeroDesign.GUTTER_DP), dp(24), dp(CoachHeroDesign.GUTTER_DP), dp(36));
        return content;
    }

    /** A full-height surface used before a profile exists. */
    private LinearLayout signedOutPage() {
        beginNewPage();
        setSystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(navy);
        LinearLayout body = column();
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        mountScreen(scroll);
        activateScroll(scroll);
        animatePage(scroll);
        return body;
    }

    /**
     * Signed-in dashboard shell.
     *
     * The Coach Hero references define each page's header as part of that page's composition.
     * Do not inject a generic banner here: it changes the approved hierarchy before the page
     * renderer gets a chance to build its own hero. Navigation remains shared, while every
     * screen owns its visible header, artwork, and content rhythm.
     */
    private LinearLayout appPage(String activeScreen) {
        return appPage(activeScreen, activeScreen);
    }

    /** Allows a dedicated hub title while retaining the owning navigation section as selected. */
    private LinearLayout appPage(String heroTitle, String activeScreen) {
        beginNewPage();
        setSystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(navy);
        // Dashboard pages are split into two deliberate surfaces: the Coach Hero is full-bleed
        // like Home/Workouts, while only the text and cards below receive reading gutters.
        // Padding the former was the source of the visibly narrow Progress/Nutrition/Settings UI.
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(navy);
        page.setClipChildren(false);
        page.addView(coachHeroHeader(heroTitle), new LinearLayout.LayoutParams(-1, dp(250)));

        LinearLayout body = column();
        body.setPadding(dp(CoachHeroDesign.GUTTER_DP), dp(18), dp(CoachHeroDesign.GUTTER_DP), dp(28));
        body.setClipChildren(false);
        page.addView(body, new LinearLayout.LayoutParams(-1, -2));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(navy);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            shell.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        shell.addView(coachHeroBottomNavigation(activeScreen), new LinearLayout.LayoutParams(-1, dp(76)));
        mountScreen(shell);
        activeDashboardScrollKey = activeScreen;
        activateScroll(scroll);
        animatePage(shell);
        return body;
    }

    /** A focused collector screen uses one compact Coach Hero header instead of nesting it under Settings. */
    private LinearLayout coachCollectionPage() {
        beginNewPage();
        setSystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(navy);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(navy);
        FrameLayout hero = coachHeroBackdrop("Coach Collection");
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = heading("COACH COLLECTION", 12, mint);
        eyebrow.setLetterSpacing(.10f);
        copy.addView(eyebrow);
        copy.addView(heading("Choose your coach", 30, Color.WHITE));
        copy.addView(text("Your coach shapes the way you train.", 14, Color.rgb(221, 235, 246)));
        FrameLayout.LayoutParams copyParams = new FrameLayout.LayoutParams(dp(260), -2, Gravity.LEFT | Gravity.BOTTOM);
        copyParams.leftMargin = dp(22); copyParams.bottomMargin = dp(18);
        hero.addView(copy, copyParams);
        page.addView(hero, new LinearLayout.LayoutParams(-1, dp(190)));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(14), dp(18), dp(28));
        body.setClipChildren(false);
        page.addView(body, new LinearLayout.LayoutParams(-1, -2));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(navy);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            shell.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        shell.addView(coachHeroBottomNavigation("Settings"), new LinearLayout.LayoutParams(-1, dp(76)));
        mountScreen(shell);
        activeDashboardScrollKey = "CoachCollection";
        activateScroll(scroll);
        animatePage(shell);
        return body;
    }

    /** A dynamic hero uses the saved coach selection rather than a hard-coded portrait. */
    private FrameLayout coachHeroBackdrop(String surfaceName) {
        FrameLayout hero = new FrameLayout(this);
        hero.setClipChildren(true);
        hero.setBackgroundColor(navy);
        hero.setContentDescription("Coach " + coachName() + " " + surfaceName + " background");

        // Every Coach Hero surface deliberately uses the same tall, top-anchored source frame.
        // Individual pages may reveal different amounts of it, but never a different crop, scale,
        // or image treatment. This is the visual rule behind the Coach Hero design lock.
        ImageView coach = new ImageView(this);
        coach.setImageResource(coachHeroBackground(coachName()));
        coach.setScaleType(ImageView.ScaleType.CENTER_CROP);
        coach.setAlpha(.98f);
        coach.setContentDescription("Coach " + coachName());
        int anchoredArtworkHeight = Math.max(dp(720), getResources().getDisplayMetrics().heightPixels);
        hero.addView(coach, new FrameLayout.LayoutParams(-1, anchoredArtworkHeight, Gravity.TOP));

        View shade = new View(this);
        GradientDrawable shadeBackground = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(7, 24, 45), Color.argb(216, 7, 24, 45), Color.argb(22, 7, 24, 45)});
        shade.setBackground(shadeBackground);
        hero.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        return hero;
    }

    private FrameLayout coachHeroHeader(String activeScreen) {
        FrameLayout hero = coachHeroBackdrop(activeScreen + " dashboard header");

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setGravity(Gravity.CENTER_VERTICAL);
        TextView eyebrow = heading("COACH " + coachName().toUpperCase(Locale.ROOT) + " · " + activeScreen.toUpperCase(Locale.ROOT), 10, mint);
        eyebrow.setLetterSpacing(.09f);
        copy.addView(eyebrow);
        TextView title = heading(activeScreen, 30, Color.WHITE);
        title.setSingleLine(true);
        copy.addView(title);
        copy.addView(text(coachHeroSubtitle(activeScreen), 13, Color.rgb(221, 235, 246)));
        FrameLayout.LayoutParams copyParams = new FrameLayout.LayoutParams(dp(245), -2, Gravity.LEFT | Gravity.BOTTOM);
        copyParams.leftMargin = dp(18); copyParams.bottomMargin = dp(20);
        hero.addView(copy, copyParams);
        return hero;
    }

    private String coachHeroSubtitle(String screen) {
        if ("Progress".equals(screen)) return "Your private training story, in focus.";
        if ("Nutrition".equals(screen)) return "Fuel that fits your real life.";
        if ("Endurance".equals(screen)) return "Plan your distance. Own your pace.";
        if ("Scheduler".equals(screen)) return "Every session, one clear calendar.";
        if ("Body".equals(screen)) return "Your numbers, clearly and privately.";
        if ("Settings".equals(screen)) return "Make your coach experience yours.";
        return "Your personal training dashboard.";
    }

    /**
     * The lower dashboard bar deliberately remains one horizontally-scrollable, labelled row.
     * This protects labels at accessibility sizes without reintroducing the old competing rail.
     */
    private HorizontalScrollView coachHeroBottomNavigation(String activeScreen) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(12, 22, 41));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(7), dp(8), dp(8));
        addCoachHeroNavItem(row, "Home", R.drawable.ic_nav_home, activeScreen, view -> navigateDashboard("Home", this::showHome));
        addCoachHeroNavItem(row, "Workouts", R.drawable.ic_nav_workouts, activeScreen, view -> navigateDashboard("Workouts", this::showPlans));
        addCoachHeroNavItem(row, "Progress", R.drawable.ic_nav_progress, activeScreen, view -> navigateDashboard("Progress", this::showProgress));
        addCoachHeroNavItem(row, "Nutrition", R.drawable.ic_nav_nutrition, activeScreen, view -> navigateDashboard("Nutrition", this::showMeals));
        addCoachHeroNavItem(row, "Endurance", R.drawable.ic_nav_endurance, activeScreen, view -> navigateDashboard("Endurance", this::showEndurance));
        addCoachHeroNavItem(row, "Scheduler", R.drawable.ic_nav_plan, activeScreen, view -> navigateDashboard("Scheduler", this::showScheduler));
        addCoachHeroNavItem(row, "Body", R.drawable.ic_nav_body, activeScreen, view -> navigateDashboard("Body", this::showBodyContext));
        addCoachHeroNavItem(row, "Settings", R.drawable.ic_nav_settings, activeScreen, view -> navigateDashboard("Settings", this::showProfile));
        scroll.addView(row, new HorizontalScrollView.LayoutParams(-2, -1));
        scroll.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) ->
                dashboardNavigationPositions.put(activeScreen, scrollX));
        // A rebuilt strip is otherwise positioned at X=0, making Settings appear to jump back
        // to Home. Restore the tab's last position, or center its selected item on first visit.
        scroll.post(() -> {
            int maximum = Math.max(0, row.getWidth() - scroll.getWidth());
            int selectedIndex = coachHeroNavigationIndex(activeScreen);
            int focused = Math.max(0, selectedIndex * dp(80) - (scroll.getWidth() - dp(76)) / 2);
            int desired = dashboardNavigationPositions.containsKey(activeScreen)
                    ? dashboardNavigationPositions.get(activeScreen) : focused;
            scroll.scrollTo(Math.min(Math.max(0, desired), maximum), 0);
        });
        return scroll;
    }

    private int coachHeroNavigationIndex(String screen) {
        String[] screens = {"Home", "Workouts", "Progress", "Nutrition", "Endurance", "Scheduler", "Body", "Settings"};
        for (int index = 0; index < screens.length; index++) {
            if (screens[index].equals(screen)) return index;
        }
        return 0;
    }

    private void addCoachHeroNavItem(LinearLayout row, String label, int icon, String activeScreen, View.OnClickListener listener) {
        boolean selected = label.equals(activeScreen);
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(6), dp(3), dp(6), dp(2));
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription(selected ? label + ", current page" : "Open " + label);
        item.setBackground(surface(selected ? mint : Color.rgb(20, 43, 66), 16, selected ? 3 : 1, selected ? coral : Color.rgb(38, 69, 95)));
        item.setOnClickListener(listener);
        ImageView glyph = new ImageView(this);
        glyph.setImageResource(icon);
        glyph.setImageTintList(ColorStateList.valueOf(selected ? navy : Color.rgb(198, 215, 235)));
        glyph.setContentDescription(null);
        item.addView(glyph, new LinearLayout.LayoutParams(dp(23), dp(23)));
        TextView caption = text(label, 9, selected ? navy : Color.rgb(198, 215, 235));
        caption.setGravity(Gravity.CENTER);
        caption.setSingleLine(true);
        caption.setTypeface(Typeface.create("sans-serif", selected ? Typeface.BOLD : Typeface.NORMAL));
        item.addView(caption, new LinearLayout.LayoutParams(-1, dp(19)));
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dp(76), dp(60));
        itemParams.rightMargin = dp(4);
        row.addView(item, itemParams);
        applyPremiumTileDepth(item, selected);
        applyPressMotion(item);
    }

    /** Switch tabs without losing the vertical position the member intentionally reached. */
    private void navigateDashboard(String target, Runnable destination) {
        if (target.equals(activeDashboardScrollKey)) return;
        if (activeScrollView != null && !activeDashboardScrollKey.isEmpty()) {
            dashboardScrollPositions.put(activeDashboardScrollKey, activeScrollView.getScrollY());
        }
        Integer restore = dashboardScrollPositions.get(target);
        pendingScrollRestore = restore == null ? -1 : restore;
        stopGuidedRuntime();
        destination.run();
    }

    /**
     * The compact 360-unit composition approved for the workout wizard. Keeping this separate
     * from the broader home/profile shell prevents a wizard visual refit from changing those
     * screens or introducing a second navigation style.
     */
    private LinearLayout planPage() {
        return planPage("Workouts");
    }

    /** Legacy wizard content can opt into the correct Coach Hero section. */
    private LinearLayout planPage(String activeScreen) {
        beginNewPage();
        setSystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(navy);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setBackgroundColor(navy);
        body.setPadding(dp(CoachHeroDesign.GUTTER_DP), 0, dp(CoachHeroDesign.GUTTER_DP), dp(28));
        body.setClipChildren(false);
        FrameLayout wizardHero = coachHeroHeader(activeScreen);
        body.addView(wizardHero, new LinearLayout.LayoutParams(-1, dp(250)));
        legacyWorkoutWizardHero = "Workouts".equals(activeScreen) ? wizardHero : null;
        gap(body, 16);
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(navy);
        // Android 15 draws edge-to-edge by default; keep the coach and wizard clear of system text.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            shell.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        shell.addView(coachHeroBottomNavigation(activeScreen), new LinearLayout.LayoutParams(-1, dp(76)));
        mountScreen(shell);
        activeDashboardScrollKey = activeScreen;
        activateScroll(scroll);
        animatePage(scroll);
        return body;
    }

    /** Applies the shared bottom navigation to full-bleed Coach Hero wizard screens. */
    private void setCoachHeroWizardShell(ScrollView scroll, String activeScreen) {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(navy);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            shell.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        shell.addView(coachHeroBottomNavigation(activeScreen), new LinearLayout.LayoutParams(-1, dp(76)));
        mountScreen(shell);
        activeDashboardScrollKey = activeScreen;
        activateScroll(scroll);
        animatePage(shell);
    }

    /** Restores an explicitly requested in-place refresh only after the replacement page is laid out. */
    private void activateScroll(ScrollView scroll) {
        activeScrollView = scroll;
        if (pendingScrollRestore < 0) return;
        int restore = pendingScrollRestore;
        pendingScrollRestore = -1;
        // Restore on the first pre-draw. The old alpha hide/show was itself visible as a
        // flicker on physical devices when a choice rebuilt a selector page.
        scroll.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                if (scroll.getViewTreeObserver().isAlive()) {
                    scroll.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                scroll.scrollTo(0, restore);
                return true;
            }
        });
    }

    /**
     * Keeps the Android window, system-bar relationship, and navigation surface stable. Before
     * this host existed every selection called setContentView(), which caused a visible flash and
     * discarded view-level state while the replacement page measured.
     */
    private void mountScreen(View screen) {
        if (screenHost == null) {
            screenHost = new FrameLayout(this);
            screenHost.setBackgroundColor(navy);
            setContentView(screenHost);
        }
        screenHost.removeAllViews();
        screenHost.addView(screen, new FrameLayout.LayoutParams(-1, -1));
        screenHost.requestApplyInsets();
    }

    private void preserveScrollForRefresh() {
        pendingScrollRestore = activeScrollView == null ? -1 : activeScrollView.getScrollY();
    }

    private void refreshPlanKeepingScroll(Runnable refresh) {
        refreshCurrentPageKeepingScroll(refresh);
    }

    /** Rebuilds a selector page without replaying the fade/slide entrance or jumping its scroll position. */
    private void refreshCurrentPageKeepingScroll(Runnable refresh) {
        preserveScrollForRefresh();
        suppressNextPageEntrance = true;
        try {
            refresh.run();
        } finally {
            // A normal page rebuild consumes this in animatePage. Keep it scoped even if a caller does not rebuild.
            suppressNextPageEntrance = false;
        }
    }

    private void beginNewPage() {
        recordPageNavigation();
        stopLiveTrackingUiObserver();
        pageGeneration++;
        showingUpdatePage = false;
        stopOtaStatusPolling();
    }

    /**
     * Every page still owns its own renderer, but Android Back needs a lightweight route stack
     * to make a system back gesture feel like an in-app navigation gesture rather than an exit.
     */
    private void recordPageNavigation() {
        String next = callingPageKey();
        if (next.isEmpty()) return;
        boolean isInPlaceRefresh = suppressNextPageEntrance;
        if (!restoringPageHistory && !isInPlaceRefresh && !currentPageKey.isEmpty()
                && !currentPageKey.equals(next)) {
            pageHistory.push(currentPageKey);
        }
        currentPageKey = next;
    }

    private String callingPageKey() {
        String activityClass = getClass().getName();
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (!activityClass.equals(frame.getClassName())) continue;
            String method = frame.getMethodName();
            if (method.startsWith("show") && noArgumentPageMethod(method)) return method;
        }
        return "";
    }

    private boolean noArgumentPageMethod(String name) {
        try {
            getClass().getDeclaredMethod(name);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    private boolean returnToPreviousPage() {
        while (!pageHistory.isEmpty()) {
            String previous = pageHistory.pop();
            try {
                java.lang.reflect.Method renderer = getClass().getDeclaredMethod(previous);
                renderer.setAccessible(true);
                restoringPageHistory = true;
                renderer.invoke(this);
                return true;
            } catch (ReflectiveOperationException ignored) {
                // If a retired route is ever removed, continue to the nearest valid page.
            } finally {
                restoringPageHistory = false;
            }
        }
        return false;
    }

    private void confirmExitApplication() {
        final Dialog dialog = new Dialog(this);
        dialog.setCanceledOnTouchOutside(true);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(22), dp(22), dp(20));
        card.setBackground(surface(navyPanel, 24, 2, mint));

        TextView eyebrow = heading("MY PERSONAL TRAINER", 11, mint);
        eyebrow.setLetterSpacing(.13f);
        card.addView(eyebrow);
        gap(card, 8);
        card.addView(heading("Leave your\ntraining space?", 26, Color.WHITE));
        gap(card, 8);
        card.addView(text("Your plans, progress, and coach settings stay safely on this phone.",
                14, Color.rgb(204, 221, 238)));
        gap(card, 20);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        Button stay = action("Stay", Color.TRANSPARENT, mint);
        stay.setTextSize(15);
        stay.setBackground(surface(Color.TRANSPARENT, 15, 1, mint));
        stay.setOnClickListener(view -> dialog.dismiss());
        actions.addView(stay, new LinearLayout.LayoutParams(0, dp(52), 1));
        View spacer = new View(this);
        actions.addView(spacer, new LinearLayout.LayoutParams(dp(10), 1));
        Button exit = action("Exit app", coral, Color.WHITE);
        exit.setTextSize(15);
        exit.setOnClickListener(view -> {
            dialog.dismiss();
            finish();
        });
        actions.addView(exit, new LinearLayout.LayoutParams(0, dp(52), 1));
        card.addView(actions, new LinearLayout.LayoutParams(-1, dp(52)));

        dialog.setContentView(card);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels - dp(32), -2);
        }
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels - dp(32), -2);
        }
    }

    /**
     * Dashboard navigation replaces the current surface synchronously.  A page-level fade was
     * visibly read as a white/navy flash on some Android devices, so navigation intentionally
     * has no entrance animation; individual controls retain their press feedback.
     */
    private void animatePage(View view) {
        view.setAlpha(1f);
        view.setTranslationY(0f);
    }

    /** Supplies tangible press feedback for every shared action control. */
    private void applyPressMotion(View view) {
        if (reducedMotion) return;
        view.setOnTouchListener((touched, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touched.animate().scaleX(.975f).scaleY(.975f).setDuration(80L).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                touched.animate().scaleX(1f).scaleY(1f).setDuration(130L).start();
            }
            return false;
        });
    }

    /** Selected navigation gets one quiet acknowledgement, rather than a distracting loop. */
    private void animateSelectedIcon(ImageView icon, boolean selected) {
        if (reducedMotion || !selected) return;
        icon.setScaleX(.84f);
        icon.setScaleY(.84f);
        icon.setAlpha(.55f);
        icon.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(90L).setDuration(180L).start();
    }

    private LinearLayout planCoachRail() {
        AccountStore.Profile profile = AccountStore.get(this);
        String trainerName = profile.coachName;
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        rail.setPadding(dp(7), dp(hasTallPlanViewport() ? 13 : 7), dp(7), dp(12));
        rail.setBackgroundColor(navy);

        ImageView portrait = new ImageView(this);
        portrait.setImageResource(coachPortrait(trainerName));
        portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);
        portrait.setBackground(surface(navyPanel, 99, 1, mint));
        portrait.setClipToOutline(true);
        portrait.setContentDescription("Coach " + trainerName);
        int portraitSize = hasTallPlanViewport() ? 54 : 48;
        rail.addView(portrait, new LinearLayout.LayoutParams(dp(portraitSize), dp(portraitSize)));

        TextView coach = text("Coach\n" + trainerName, 9, Color.WHITE);
        coach.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams coachParams = new LinearLayout.LayoutParams(-1, -2);
        coachParams.topMargin = dp(3);
        rail.addView(coach, coachParams);

        View divider = new View(this);
        divider.setBackgroundColor(mint);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(dp(3), dp(hasTallPlanViewport() ? 46 : 36));
        dividerParams.topMargin = dp(hasTallPlanViewport() ? 18 : 13);
        dividerParams.bottomMargin = dp(planGridGap());
        rail.addView(divider, dividerParams);

        addPlanRailItem(rail, "Home", R.drawable.ic_nav_home, false, view -> navigateDashboard("Home", this::showHome));
        addPlanRailItem(rail, "Workouts", R.drawable.ic_nav_workouts, true, view -> navigateDashboard("Workouts", this::showPlans));
        addPlanRailItem(rail, "Progress", R.drawable.ic_nav_progress, false, view -> navigateDashboard("Progress", this::showProgress));
        addPlanRailItem(rail, "Nutrition", R.drawable.ic_nav_nutrition, false, view -> navigateDashboard("Nutrition", this::showMeals));
        addPlanRailItem(rail, "Endurance", R.drawable.ic_nav_endurance, false, view -> navigateDashboard("Endurance", this::showEndurance));
        addPlanRailItem(rail, "Scheduler", R.drawable.ic_nav_plan, false, view -> navigateDashboard("Scheduler", this::showScheduler));
        addPlanRailItem(rail, "Body", R.drawable.ic_nav_body, false, view -> navigateDashboard("Body", this::showBodyContext));
        addPlanRailItem(rail, "Settings", R.drawable.ic_nav_settings, false, view -> navigateDashboard("Settings", this::showProfile));
        return rail;
    }

    private void addPlanRailItem(LinearLayout rail, String label, int icon, boolean selected, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription(selected ? label + ", current screen" : "Open " + label);
        item.setOnClickListener(listener);
        item.setBackground(surface(selected ? mint : navyPanel, 14, selected ? 3 : 1, selected ? coral : Color.rgb(31, 76, 91)));
        elevatePlanSurface(item, selected);
        ImageView symbol = new ImageView(this);
        symbol.setImageResource(icon);
        symbol.setImageTintList(ColorStateList.valueOf(selected ? navy : mint));
        symbol.setContentDescription(null);
        item.addView(symbol, new LinearLayout.LayoutParams(dp(21), dp(21)));
        animateSelectedIcon(symbol, selected);
        TextView caption = text(label, 8, selected ? navy : mint);
        caption.setGravity(Gravity.CENTER);
        caption.setSingleLine(true);
        caption.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        item.addView(caption, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(60), dp(hasTallPlanViewport() ? 54 : 48));
        params.topMargin = dp(hasTallPlanViewport() ? 10 : 7);
        rail.addView(item, params);
        applyPressMotion(item);
    }

    private LinearLayout coachRail(String activeScreen) {
        AccountStore.Profile profile = AccountStore.get(this);
        String trainerName = profile.coachName;
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        rail.setPadding(dp(8), dp(24), dp(8), dp(10));
        rail.setBackgroundColor(navy);

        ImageView portrait = new ImageView(this);
        portrait.setImageResource(coachPortrait(trainerName));
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

        addRailItem(rail, "Home", R.drawable.ic_nav_home, activeScreen, view -> navigateDashboard("Home", this::showHome));
        addRailItem(rail, "Workouts", R.drawable.ic_nav_workouts, activeScreen, view -> navigateDashboard("Workouts", this::showPlans));
        addRailItem(rail, "Progress", R.drawable.ic_nav_progress, activeScreen, view -> navigateDashboard("Progress", this::showProgress));
        addRailItem(rail, "Nutrition", R.drawable.ic_nav_nutrition, activeScreen, view -> navigateDashboard("Nutrition", this::showMeals));
        addRailItem(rail, "Endurance", R.drawable.ic_nav_endurance, activeScreen, view -> navigateDashboard("Endurance", this::showEndurance));
        addRailItem(rail, "Scheduler", R.drawable.ic_nav_plan, activeScreen, view -> navigateDashboard("Scheduler", this::showScheduler));
        addRailItem(rail, "Body", R.drawable.ic_nav_body, activeScreen, view -> navigateDashboard("Body", this::showBodyContext));
        addRailItem(rail, "Settings", R.drawable.ic_nav_settings, activeScreen, view -> navigateDashboard("Settings", this::showProfile));
        return rail;
    }

    private void addRailItem(LinearLayout rail, String label, int icon, String activeScreen, View.OnClickListener listener) {
        boolean selected = label.equals(activeScreen);
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setBackground(surface(selected ? mint : navyPanel, 15, selected ? 3 : 1, selected ? coral : Color.rgb(35, 53, 78)));
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
        animateSelectedIcon(symbol, selected);

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
        applyPressMotion(item);
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

    /** Fixed visual hierarchy for the approved Private Body Context reference screen. */
    private void bodyReferenceTitle(LinearLayout body) {
        TextView label = heading("PRIVATE BODY CONTEXT", 12, mint);
        label.setLetterSpacing(.07f);
        body.addView(label);
        gap(body, 5);
        TextView headline = heading("Your numbers, clearly", 27, Color.WHITE);
        headline.setSingleLine(true);
        headline.setHorizontallyScrolling(false);
        body.addView(headline);
        gap(body, 5);
        body.addView(text("This information is private to you, always.", 16, Color.rgb(211, 225, 233)));
        gap(body, 15);
    }

    private LinearLayout bodyReferenceCard() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        view.setBackground(surface(paper, 18, 1, cardLine));
        applyPremiumTileDepth(view, false);
        return view;
    }

    private View bodyReferenceDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(222, 226, 235));
        return divider;
    }

    /** Typography, rhythm and spacing from the supplied 360-unit wizard artwork. */
    private void planTitle(LinearLayout body, String eyebrow, String main, String copy) {
        TextView label = heading(eyebrow.toUpperCase(Locale.ROOT), hasTallPlanViewport() ? 10 : 9, mint);
        label.setLetterSpacing(.07f);
        body.addView(label);
        gap(body, hasTallPlanViewport() ? 5 : 3);
        TextView headline = heading(main, hasTallPlanViewport() ? 30 : 26, Color.WHITE);
        headline.setLineSpacing(dp(0), .95f);
        body.addView(headline);
        if (copy != null && !copy.isEmpty()) {
            gap(body, hasTallPlanViewport() ? 8 : 5);
            TextView description = text(copy, hasTallPlanViewport() ? 14 : 12, Color.rgb(211, 225, 233));
            description.setLineSpacing(dp(2), 1f);
            body.addView(description);
        }
        gap(body, hasTallPlanViewport() ? 18 : 12);
    }

    private Button planPrimary(String label) {
        Button button = action(label, coral, navy);
        button.setTextSize(hasTallPlanViewport() ? 15 : 13);
        button.setBackground(surface(coral, 15, 0, Color.TRANSPARENT));
        button.setElevation(dp(4));
        button.setMinHeight(0);
        return button;
    }

    private Button planBack() {
        Button button = new Button(this);
        button.setText("Back");
        button.setContentDescription("Back");
        button.setAllCaps(false);
        button.setTextSize(hasTallPlanViewport() ? 14 : 12);
        button.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        button.setTextColor(mint);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, 0, 0, 0);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setMinHeight(0);
        return button;
    }

    private void addPlanButton(LinearLayout body, Button button) {
        body.addView(button, new LinearLayout.LayoutParams(-1, dp(planActionHeight())));
    }

    private void addPlanBack(LinearLayout body, Button button) {
        body.addView(button, new LinearLayout.LayoutParams(-1, dp(planBackHeight())));
    }

    private ReferenceArtworkView referenceArtwork(int source, int left, int top, int right, int bottom) {
        ReferenceArtworkView artwork = new ReferenceArtworkView(this);
        artwork.setArtwork(source, left, top, right, bottom);
        return artwork;
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(16), dp(16), dp(16), dp(16));
        view.setBackground(surface(paper, CoachHeroDesign.CARD_RADIUS_DP, 1, cardLine));
        applyPremiumTileDepth(view, false);
        return view;
    }

    /** Dark-panel surface used by the approved Home/Progress visual direction. */
    private LinearLayout darkCard() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(15), dp(14), dp(15), dp(14));
        view.setBackground(surface(Color.rgb(20, 38, 66), CoachHeroDesign.COMPACT_RADIUS_DP, 1, Color.rgb(35, 58, 91)));
        applyPremiumTileDepth(view, false);
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
        applyPremiumTileDepth(button, false);
        button.setMinHeight(dp(48));
        applyPressMotion(button);
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
            applyPremiumTileDepth(mediaCard, false);
            mediaCard.setClipToOutline(true);
            mediaCard.setClipChildren(true);
            mediaCard.setContentDescription("Coach " + coachName() + " demonstrating " + exercise.name);
            LoopingExerciseVideoView demonstration = new LoopingExerciseVideoView(this, video);
            mediaCard.addView(demonstration, new FrameLayout.LayoutParams(-1, -1));
            return mediaCard;
        }
        ExerciseAnimationView fallback = new ExerciseAnimationView(this, coachName(), exercise);
        fallback.setBackground(surface(navyPanel, 18, 1, mint));
        applyPremiumTileDepth(fallback, false);
        return fallback;
    }

    /** Posters make dense selection screens fast; playback is reserved for the focused exercise. */
    private ImageView exercisePoster(String trainer, Coach.Exercise exercise) {
        ImageView poster = new ImageView(this);
        int visual = ExerciseVisuals.forExercise(this, trainer, exercise.name);
        if (visual != 0) poster.setImageResource(visual);
        poster.setScaleType(ImageView.ScaleType.FIT_CENTER);
        poster.setBackground(surface(paper, 14, 1, cardLine));
        poster.setPadding(dp(1), dp(1), dp(1), dp(1));
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
        applyPasswordMask(field, inputType);
        field.setSingleLine(true);
        field.setPadding(dp(15), dp(7), dp(15), dp(7));
        field.setBackground(surface(paper, 14, 1, cardLine));
        field.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(54)));
        return field;
    }

    /**
     * The signed-out experience deliberately uses one visual system.  These fields are not
     * reused by the app's light forms: their contrast is tuned for the fixed full-hero login
     * canvas so a change of coach cannot accidentally turn into a change of login design.
     */
    private EditText heroLoginInput(String hint, int inputType) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setTextSize(14);
        field.setTextColor(Color.WHITE);
        field.setHintTextColor(Color.rgb(196, 210, 226));
        field.setInputType(inputType);
        applyPasswordMask(field, inputType);
        field.setSingleLine(true);
        field.setPadding(dp(13), dp(4), dp(13), dp(4));
        field.setCompoundDrawablePadding(dp(10));
        int icon = (inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
                && (inputType & InputType.TYPE_MASK_VARIATION) != InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                ? android.R.drawable.ic_lock_lock : android.R.drawable.ic_dialog_email;
        android.graphics.drawable.Drawable leadingIcon = getDrawable(icon);
        if (leadingIcon != null) {
            leadingIcon = leadingIcon.mutate();
            leadingIcon.setBounds(0, 0, dp(18), dp(18));
        }
        field.setCompoundDrawables(leadingIcon, null, null, null);
        field.setCompoundDrawableTintList(ColorStateList.valueOf(Color.rgb(230, 239, 247)));
        field.setBackground(surface(Color.argb(224, 24, 40, 63), 10, 1, Color.rgb(76, 99, 128)));
        field.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(48)));
        return field;
    }

    /** Enforce masking even when a device-wide "show passwords" preference is enabled. */
    private void applyPasswordMask(EditText field, int inputType) {
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
            // Keep every character masked as an asterisk, including while it is being typed.
            field.setTransformationMethod(AsteriskPasswordTransformation.INSTANCE);
        }
    }

    /** A deliberately always-masked password presentation for the local sign-in forms. */
    private static final class AsteriskPasswordTransformation implements android.text.method.TransformationMethod {
        private static final AsteriskPasswordTransformation INSTANCE = new AsteriskPasswordTransformation();

        @Override public CharSequence getTransformation(final CharSequence source, View view) {
            return new CharSequence() {
                @Override public int length() { return source.length(); }
                @Override public char charAt(int index) { return '*'; }
                @Override public CharSequence subSequence(int start, int end) {
                    StringBuilder masked = new StringBuilder(Math.max(0, end - start));
                    for (int index = start; index < end; index++) masked.append('*');
                    return masked;
                }
            };
        }

        @Override public void onFocusChanged(View view, CharSequence sourceText, boolean focused,
                                             int direction, android.graphics.Rect previouslyFocusedRect) {
            // No temporary reveal: the field remains masked on focus change as well.
        }
    }

    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show(); }

    // Signed-out flow

    private void showWelcome() {
        // There is deliberately no unsigned coach picker. A saved coach choice is respected
        // everywhere, including the signed-out hero. New members receive the default coach
        // until they make their own choice during registration or in Settings.
        showLogin();
    }

    private void showLogin() { showLogin(false); }

    /**
     * Uses the same locked hero form for password and biometric entry.  The prompt starts only
     * for a fresh launch with biometric unlock enabled; manual returns to Login never surprise
     * the member with another prompt.
     */
    private void showLogin(boolean autoRequestBiometric) {
        LinearLayout body = signedOutPage();
        // This is one locked template for every coach.  Do not branch the layout by coach:
        // Settings supplies only the selected portrait and its identifying label below.
        body.setPadding(0, 0, 0, 0);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        String heroCoach = AccountStore.exists(this) ? profile().coachName : CoachPreferences.defaultCoach(selectedTrainer);

        FrameLayout hero = new FrameLayout(this);
        hero.setBackgroundColor(navy);
        hero.setClipChildren(true);
        hero.setContentDescription("Welcome with Coach " + heroCoach);
        int fullHeroHeight = Math.max(dp(720), getResources().getDisplayMetrics().heightPixels);
        body.addView(hero, new LinearLayout.LayoutParams(-1, fullHeroHeight));

        // A full-height background is generated from the approved individual Coach Hero panel.
        // It contains no baked fields or labels, allowing the native form to adapt to a real
        // S23 Ultra canvas without stretching or leaving an empty lower region.
        ImageView coach = new ImageView(this);
        coach.setImageResource(coachHeroBackground(heroCoach));
        coach.setScaleType(ImageView.ScaleType.CENTER_CROP);
        coach.setAlpha(.98f);
        FrameLayout.LayoutParams coachParams = new FrameLayout.LayoutParams(-1, fullHeroHeight, Gravity.LEFT | Gravity.TOP);
        hero.addView(coach, coachParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int screenDp = (int) (getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
        int contentWidth = dp(Math.min(310, Math.max(274, screenDp - 92)));
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(contentWidth, -2, Gravity.LEFT | Gravity.TOP);
        contentParams.leftMargin = dp(24);
        contentParams.topMargin = dp(18);
        hero.addView(content, contentParams);

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark = new ImageView(this);
        // The premium running-dumbbell mark is the locked product icon used everywhere.
        mark.setImageResource(R.drawable.trainer_hero_mark_premium_transparent);
        mark.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        mark.setContentDescription("My Personal Trainer");
        brandRow.addView(mark, new LinearLayout.LayoutParams(dp(50), dp(50)));
        TextView brand = heading("My\nPersonal\nTrainer", 12, Color.WHITE);
        brand.setLetterSpacing(.01f);
        brand.setLineSpacing(0, .96f);
        brand.setPadding(dp(9), 0, 0, 0);
        brandRow.addView(brand);
        content.addView(brandRow);
        gap(content, 36);

        content.addView(heading("Welcome", 35, Color.WHITE));
        TextView motto = text("Your Health is Your Wealth", 16, Color.rgb(232, 239, 247));
        motto.setLetterSpacing(.01f);
        motto.setPadding(0, dp(5), 0, 0);
        content.addView(motto);
        // Preserve a clear facial area on tall screens: the fields begin over the coach's
        // torso, as in the approved Coach Hero reference, rather than cutting across the face.
        gap(content, 108);

        EditText email = heroLoginInput("Email address", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if (AccountStore.exists(this)) email.setText(profile().email);
        content.addView(email);
        gap(content, 10);
        EditText password = heroLoginInput("Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(password);
        gap(content, 7);
        LinearLayout accountRow = new LinearLayout(this);
        accountRow.setGravity(Gravity.CENTER_VERTICAL);
        CheckBox remember = new CheckBox(this);
        remember.setText("Remember me");
        remember.setTextColor(Color.WHITE);
        remember.setButtonTintList(ColorStateList.valueOf(mint));
        remember.setChecked(AccountStore.exists(this) && profile().rememberSignIn);
        remember.setTextSize(12);
        remember.setPadding(0, 0, 0, 0);
        accountRow.addView(remember, new LinearLayout.LayoutParams(0, dp(34), 1));
        TextView forgot = heading("Forgot password?", 12, mint);
        forgot.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        forgot.setClickable(true);
        forgot.setOnClickListener(view -> showForgotPassword());
        applyPressMotion(forgot);
        accountRow.addView(forgot, new LinearLayout.LayoutParams(0, dp(34), 1));
        content.addView(accountRow);
        Button signIn = action("Sign in  →", coral, Color.WHITE);
        signIn.setOnClickListener(view -> {
            if (AccountStore.authenticate(this, email.getText().toString(), password.getText().toString())) {
                AccountStore.Profile current = profile();
                AccountStore.updatePersonalProfile(this, current.username, current.photoUri, remember.isChecked(), current.biometricUnlock, "");
                openSignedInHome();
            } else {
                toast("That email or password does not match this profile.");
            }
        });
        content.addView(signIn, new LinearLayout.LayoutParams(-1, dp(50)));
        gap(content, 7);

        TextView register = text("Don’t have an account?  Register", 12, Color.WHITE);
        register.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        register.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        register.setPadding(0, 0, 0, 0);
        register.setClickable(true);
        register.setOnClickListener(view -> showRegistration());
        applyPressMotion(register);
        content.addView(register, new LinearLayout.LayoutParams(-1, dp(34)));

        TextView coachLabel = text("Coach " + heroCoach + " · " + loginCoachAccent(heroCoach), 11, Color.WHITE);
        coachLabel.setGravity(Gravity.CENTER);
        coachLabel.setPadding(dp(10), 0, dp(10), 0);
        coachLabel.setBackground(surface(Color.argb(160, 9, 22, 40), 99, 1, Color.rgb(75, 119, 143)));
        FrameLayout.LayoutParams coachLabelParams = new FrameLayout.LayoutParams(-2, dp(29), Gravity.RIGHT | Gravity.BOTTOM);
        coachLabelParams.rightMargin = dp(17);
        coachLabelParams.bottomMargin = dp(16);
        hero.addView(coachLabel, coachLabelParams);

        if (autoRequestBiometric && AccountStore.exists(this) && profile().biometricUnlock) {
            content.post(() -> {
                if (content.isAttachedToWindow()) authenticateWithBiometrics();
            });
        }
    }

    /**
     * Exact Coach Hero login artwork with accessible, real Android hit targets.  The target
     * image is never composited with another coach crop: coach choice maps one-to-one to one
     * approved artwork panel.
     */
    private void showLockedCoachHeroLogin(LinearLayout body, String heroCoach, boolean autoRequestBiometric) {
        FrameLayout hero = new FrameLayout(this);
        hero.setBackgroundColor(navy);
        hero.setClipChildren(true);
        hero.setContentDescription("Welcome with Coach " + heroCoach);
        int imageWidth = getResources().getDisplayMetrics().widthPixels;
        int imageHeight = Math.round(imageWidth * (596f / 434f));
        body.addView(hero, new LinearLayout.LayoutParams(-1, imageHeight));

        ImageView artwork = new ImageView(this);
        artwork.setImageResource(coachLoginArtwork(heroCoach));
        artwork.setScaleType(ImageView.ScaleType.FIT_START);
        artwork.setContentDescription("Coach Hero login artwork for " + heroCoach);
        hero.addView(artwork, new FrameLayout.LayoutParams(-1, imageHeight));

        float scale = imageWidth / 434f;
        int fieldX = Math.round(28 * scale);
        int fieldW = Math.round(318 * scale);
        int fieldH = Math.round(48 * scale);
        int emailY = Math.round(282 * scale);
        int passwordY = Math.round(342 * scale);

        EditText email = lockedArtworkInput("Email address", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, scale);
        if (AccountStore.exists(this)) email.setText(profile().email);
        addArtworkOverlay(hero, email, fieldX + Math.round(42 * scale), emailY, fieldW - Math.round(48 * scale), fieldH);
        EditText password = lockedArtworkInput("Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, scale);
        addArtworkOverlay(hero, password, fieldX + Math.round(42 * scale), passwordY, fieldW - Math.round(48 * scale), fieldH);

        final boolean[] remember = {AccountStore.exists(this) && profile().rememberSignIn};
        View rememberTarget = lockedArtworkTarget("Remember me", view -> remember[0] = !remember[0]);
        addArtworkOverlay(hero, rememberTarget, fieldX, Math.round(402 * scale), Math.round(155 * scale), Math.round(35 * scale));
        View forgotTarget = lockedArtworkTarget("Forgot password", view -> showForgotPassword());
        addArtworkOverlay(hero, forgotTarget, Math.round(216 * scale), Math.round(402 * scale), Math.round(130 * scale), Math.round(35 * scale));
        View signInTarget = lockedArtworkTarget("Sign in", view -> {
            if (AccountStore.authenticate(this, email.getText().toString(), password.getText().toString())) {
                AccountStore.Profile current = profile();
                AccountStore.updatePersonalProfile(this, current.username, current.photoUri, remember[0], current.biometricUnlock, "");
                openSignedInHome();
            } else {
                toast("That email or password does not match this profile.");
            }
        });
        addArtworkOverlay(hero, signInTarget, fieldX, Math.round(458 * scale), fieldW, Math.round(51 * scale));
        View registerTarget = lockedArtworkTarget("Register a new account", view -> showRegistration());
        addArtworkOverlay(hero, registerTarget, fieldX, Math.round(520 * scale), fieldW, Math.round(32 * scale));

        if (autoRequestBiometric && AccountStore.exists(this) && profile().biometricUnlock) {
            hero.post(() -> {
                if (hero.isAttachedToWindow()) authenticateWithBiometrics();
            });
        }
    }

    private EditText lockedArtworkInput(String hint, int inputType, float scale) {
        EditText field = new EditText(this);
        field.setHint(""); // The approved artwork already supplies the visible label and icon.
        field.setTextColor(Color.WHITE);
        field.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 14 * scale);
        field.setSingleLine(true);
        field.setInputType(inputType);
        applyPasswordMask(field, inputType);
        field.setPadding(0, 0, 0, 0);
        field.setBackgroundColor(Color.TRANSPARENT);
        field.setContentDescription(hint);
        return field;
    }

    private View lockedArtworkTarget(String description, View.OnClickListener listener) {
        View target = new View(this);
        target.setBackgroundColor(Color.TRANSPARENT);
        target.setContentDescription(description);
        target.setClickable(true);
        target.setFocusable(true);
        target.setOnClickListener(listener);
        return target;
    }

    private void addArtworkOverlay(FrameLayout parent, View child, int left, int top, int width, int height) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height, Gravity.LEFT | Gravity.TOP);
        params.leftMargin = left;
        params.topMargin = top;
        parent.addView(child, params);
    }

    private String loginCoachAccent(String coach) {
        if ("Mia".equals(coach) || "Leo".equals(coach)) return "Spanish";
        if ("Claudia".equals(coach) || "Stephen".equals(coach)) return "UK";
        return "US";
    }

    private void openSignedInHome() {
        AccountStore.Profile signedInProfile = profile();
        selectedTrainer = signedInProfile.trainer;
        configureSpeechVoices();
        LauncherIconManager.apply(this, signedInProfile.coachName);
        applyKeepPhoneAwake(signedInProfile.keepPhoneAwake);
        showHome();
    }

    private void showForgotPassword() {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20), 0, dp(20), 0);
        EditText email = input("Email address", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if (AccountStore.exists(this)) email.setText(profile().email);
        EditText password = input("New password — at least 8 characters", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(email); gap(form, 8); form.addView(password);
        new AlertDialog.Builder(this).setTitle("Reset password").setMessage("This profile is stored only on this device. Confirm its email and choose a new password.")
                .setView(form).setNegativeButton("Cancel", null).setPositiveButton("Reset", (dialog, which) -> {
                    if (AccountStore.resetPassword(this, email.getText().toString(), password.getText().toString())) { toast("Password updated."); showLogin(); }
                    else toast("Check the email and use at least 8 characters.");
                }).show();
    }

    private void authenticateWithBiometrics() {
        final int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        int availability = BiometricManager.from(this).canAuthenticate(authenticators);
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            toast("Set up a fingerprint, face unlock, or phone screen lock before using biometric sign-in.");
            return;
        }
        BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this), new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                openSignedInHome();
            }
            @Override public void onAuthenticationError(int code, CharSequence message) {
                super.onAuthenticationError(code, message);
                if (code != BiometricPrompt.ERROR_USER_CANCELED && code != BiometricPrompt.ERROR_NEGATIVE_BUTTON) toast(message == null ? "Biometric sign-in was not completed." : message.toString());
            }
        });
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock My Personal Trainer")
                .setSubtitle("Use your enrolled biometric or device screen lock")
                .setAllowedAuthenticators(authenticators)
                .build();
        prompt.authenticate(promptInfo);
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
            configureSpeechVoices();
            AccountStore.create(this, enteredEmail, usernameValue, passwordValue, selectedTrainer);
            LauncherIconManager.apply(this, AccountStore.get(this).coachName);
            showBodyProfileSetup(true);
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
        showCoachHeroHome();
    }

    /**
     * Coach Hero Home v1. The reference is rebuilt from real Android views so the workout,
     * weekly plan, and meal surfaces remain useful rather than becoming a static mockup.
     */
    private void showCoachHeroHome() {
        beginNewPage();
        setSystemBars();
        HomeSession nextSession = nextHomeSession();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(paper);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(paper);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(navy);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            shell.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        shell.addView(coachHeroBottomNavigation("Home"), new LinearLayout.LayoutParams(-1, dp(76)));
        mountScreen(shell);
        // Home owns a custom Coach Hero shell rather than appPage(), so it must bind its
        // dashboard key here. Without this, Home scroll position was stored under whichever
        // tab was open previously and returning to a tab appeared to reset it to the top.
        activeDashboardScrollKey = "Home";
        activateScroll(scroll);
        animatePage(shell);

        FrameLayout hero = coachHeroBackdrop("Home");
        root.addView(hero, new LinearLayout.LayoutParams(-1, dp(518)));

        LinearLayout heroCopy = new LinearLayout(this);
        heroCopy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = heading("T O D A Y", 11, Color.WHITE);
        eyebrow.setLetterSpacing(.16f);
        heroCopy.addView(eyebrow);
        View line = new View(this);
        line.setBackgroundColor(mint);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(dp(35), dp(3));
        lineParams.topMargin = dp(10); lineParams.bottomMargin = dp(22);
        heroCopy.addView(line, lineParams);
        heroCopy.addView(heading("Train with\npurpose.", 31, Color.WHITE));
        TextView subtitle = text("Stronger habits.\nA healthier you.", 17, Color.rgb(229, 237, 245));
        subtitle.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(10);
        heroCopy.addView(subtitle, subtitleParams);
        FrameLayout.LayoutParams copyParams = new FrameLayout.LayoutParams(dp(230), -2, Gravity.LEFT | Gravity.TOP);
        copyParams.leftMargin = dp(22); copyParams.topMargin = dp(34);
        hero.addView(heroCopy, copyParams);

        LinearLayout next = new LinearLayout(this);
        next.setOrientation(LinearLayout.VERTICAL);
        next.setPadding(dp(18), dp(17), dp(18), dp(16));
        next.setBackground(surface(Color.WHITE, 22, 0, Color.TRANSPARENT));
        LinearLayout nextTop = new LinearLayout(this);
        nextTop.setGravity(Gravity.CENTER_VERTICAL);
        ImageView nextGlyph = new ImageView(this);
        nextGlyph.setImageResource(R.drawable.ic_nav_workouts);
        nextGlyph.setImageTintList(ColorStateList.valueOf(coral));
        nextGlyph.setPadding(dp(12), dp(12), dp(12), dp(12));
        nextGlyph.setBackground(surface(Color.rgb(255, 236, 233), 18, 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams glyphParams = new LinearLayout.LayoutParams(dp(54), dp(54));
        glyphParams.rightMargin = dp(13);
        nextTop.addView(nextGlyph, glyphParams);
        LinearLayout nextText = new LinearLayout(this); nextText.setOrientation(LinearLayout.VERTICAL);
        TextView nextLabel = heading("NEXT WORKOUT", 10, muted); nextLabel.setLetterSpacing(.12f);
        nextText.addView(nextLabel);
        nextText.addView(heading(nextSession.title, 22, ink));
        nextTop.addView(nextText, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = heading("›", 36, muted); arrow.setGravity(Gravity.CENTER);
        nextTop.addView(arrow, new LinearLayout.LayoutParams(dp(36), dp(54)));
        next.addView(nextTop);
        next.addView(text(nextSession.detail, 12, muted));
        Button start = action("▶  Start workout", coral, Color.WHITE);
        start.setOnClickListener(view -> showPlans());
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(-1, dp(48));
        startParams.topMargin = dp(13);
        next.addView(start, startParams);
        FrameLayout.LayoutParams nextParams = new FrameLayout.LayoutParams(-1, -2, Gravity.LEFT | Gravity.BOTTOM);
        nextParams.leftMargin = dp(15); nextParams.rightMargin = dp(15); nextParams.bottomMargin = dp(16);
        hero.addView(next, nextParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(22), dp(16), dp(34));
        content.setBackgroundColor(paper);
        root.addView(content, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout weekHeader = new LinearLayout(this);
        weekHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView weekTitle = heading("YOUR WEEK", 12, muted); weekTitle.setLetterSpacing(.14f);
        weekHeader.addView(weekTitle, new LinearLayout.LayoutParams(0, dp(32), 1));
        TextView planLink = heading("View plan  ›", 14, muted); planLink.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        planLink.setClickable(true); planLink.setOnClickListener(view -> showPlans());
        weekHeader.addView(planLink, new LinearLayout.LayoutParams(dp(100), dp(32)));
        content.addView(weekHeader);

        LinearLayout days = new LinearLayout(this);
        days.setGravity(Gravity.CENTER);
        Calendar homeWeek = homeWeekStart(0);
        boolean homeWeekConfirmed = hasHomeWeekSchedule(0, homeWeek);
        WeeklyScheduleStore.Schedule savedWeek = homeWeekSchedule(0, homeWeek, homeWeekConfirmed);
        for (int i = 0; i < weekDays.length; i++) {
            Calendar date = homeDate(homeWeek, i);
            LinearLayout day = homeScheduleDayTile(date, i, savedWeek.workoutDays[i], homeWeekConfirmed, savedWeek, isToday(homeWeek, i));
            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(0, dp(112), 1);
            if (i > 0) dayParams.leftMargin = dp(4);
            days.addView(day, dayParams);
        }
        content.addView(days);
        gap(content, 22);

        LinearLayout fuelHeader = new LinearLayout(this); fuelHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView fuelTitle = heading("TODAY'S FUEL", 12, muted); fuelTitle.setLetterSpacing(.14f);
        fuelHeader.addView(fuelTitle, new LinearLayout.LayoutParams(0, dp(30), 1));
        TextView mealLink = heading("See nutrition  ›", 14, muted); mealLink.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        mealLink.setClickable(true); mealLink.setOnClickListener(view -> showMeals());
        fuelHeader.addView(mealLink, new LinearLayout.LayoutParams(dp(130), dp(30)));
        content.addView(fuelHeader);

        LinearLayout mealRow = new LinearLayout(this);
        mealRow.setGravity(Gravity.CENTER_VERTICAL);
        NutritionRecommendation.Day todayFood = todayNutritionDay();
        mealRow.addView(homeMealCard("BREAKFAST", todayFood.breakfast, homeMealImage(todayFood.breakfast, "BREAKFAST")), new LinearLayout.LayoutParams(0, dp(178), 1));
        LinearLayout.LayoutParams lunchParams = new LinearLayout.LayoutParams(0, dp(178), 1); lunchParams.leftMargin = dp(10);
        mealRow.addView(homeMealCard("LUNCH", todayFood.lunch, homeMealImage(todayFood.lunch, "LUNCH")), lunchParams);
        content.addView(mealRow);
    }

    private LinearLayout homeMealCard(String label, NutritionRecommendation.Meal meal, int imageRes) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(11));
        card.setBackground(surface(Color.WHITE, 20, 1, cardLine));
        TextView top = heading(label, 9, muted); top.setLetterSpacing(.13f); card.addView(top);
        TextView mealName = heading(meal.title, 15, ink); mealName.setMaxLines(2); LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2); nameParams.topMargin = dp(5); card.addView(mealName, nameParams);
        TextView nutrition = text(meal.kcal() + " cal   ·   " + String.format(Locale.US, "%.0f", meal.protein()) + "g P", 10, muted); card.addView(nutrition);
        ImageView mealImage = new ImageView(this);
        mealImage.setImageResource(imageRes);
        mealImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mealImage.setContentDescription(meal.title);
        mealImage.setBackground(surface(Color.rgb(238, 242, 247), 13, 0, Color.TRANSPARENT));
        mealImage.setClipToOutline(true);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(-1, 0, 1);
        imageParams.topMargin = dp(8);
        card.addView(mealImage, imageParams);
        card.setClickable(true); card.setContentDescription("View nutrition facts for " + meal.title); card.setOnClickListener(view -> showMealNutritionFacts(label, meal)); applyPressMotion(card);
        return card;
    }

    private NutritionRecommendation.Day todayNutritionDay() {
        return NutritionRecommendation.forToday(NutritionRegionStore.region(this));
    }

    private int homeMealImage(NutritionRecommendation.Meal meal, String label) {
        int generatedMealImage = mealImage(meal);
        if (generatedMealImage != 0) return generatedMealImage;
        return "BREAKFAST".equals(label) ? R.drawable.home_meal_protein_oats_berries : R.drawable.home_meal_grilled_chicken_bowl;
    }

    /** One locally bundled, generated food photograph for each supported rotation meal. */
    private int mealImage(NutritionRecommendation.Meal meal) {
        if (meal == null) return 0;
        switch (meal.title) {
            case "Overnight oats": return R.drawable.balanced_breakfast_overnight_oats;
            case "Egg and vegetable toast": return R.drawable.balanced_breakfast_egg_vegetable_toast;
            case "Yogurt fruit bowl": return R.drawable.balanced_breakfast_yogurt_fruit_bowl;
            case "Peanut-banana smoothie": return R.drawable.balanced_breakfast_peanut_banana_smoothie;
            case "Warm porridge": return R.drawable.balanced_breakfast_warm_porridge;
            case "Breakfast quesadilla": return R.drawable.balanced_breakfast_quesadilla;
            case "Cottage cheese and fruit": return R.drawable.balanced_breakfast_cottage_cheese_fruit;
            case "Chicken grain bowl": return R.drawable.balanced_lunch_chicken_grain_bowl;
            case "Tuna or chickpea wrap": return R.drawable.balanced_lunch_tuna_chickpea_wrap;
            case "Salmon or lentil plate": return R.drawable.balanced_lunch_salmon_lentil_plate;
            case "Turkey or tofu salad bowl": return R.drawable.balanced_lunch_turkey_tofu_salad_bowl;
            case "Bean and rice bowl": return R.drawable.balanced_lunch_bean_rice_bowl;
            case "Pasta with vegetables": return R.drawable.balanced_lunch_pasta_vegetables;
            case "Roasted vegetable pita": return R.drawable.balanced_lunch_roasted_vegetable_pita;
        }
        if (!NutritionRegionStore.TRINIDAD_TOBAGO.equals(NutritionRegionStore.region(this))) return 0;
        switch (meal.title) {
            case "Sada roti with channa": return R.drawable.tt_breakfast_sada_roti_channa;
            case "Coconut bake and egg": return R.drawable.tt_breakfast_coconut_bake_egg;
            case "Cornmeal porridge and egg": return R.drawable.tt_breakfast_cornmeal_porridge_egg;
            case "Bake and smoked herring": return R.drawable.tt_breakfast_bake_smoked_herring;
            case "Doubles with fruit": return R.drawable.tt_breakfast_doubles_fruit;
            case "Provision breakfast": return R.drawable.tt_breakfast_provision_breakfast;
            case "Aloo choka and roti": return R.drawable.tt_breakfast_aloo_choka_roti;
            case "Saltfish buljol and coconut bake": return R.drawable.tt_breakfast_saltfish_buljol_bake;
            case "Dhalpuri with channa": return R.drawable.tt_breakfast_dhalpuri_channa;
            case "Tomato choka, sada roti and egg": return R.drawable.tt_breakfast_tomato_choka_roti_egg;
            case "Aloo pie, yogurt and fruit": return R.drawable.tt_breakfast_aloo_pie_yogurt_fruit;
            case "Pholourie, channa and fruit": return R.drawable.tt_breakfast_pholourie_channa_fruit;
            case "Coconut bake, egg and avocado": return R.drawable.tt_breakfast_coconut_bake_egg_avocado;
            case "Provision and buljol plate": return R.drawable.tt_breakfast_provision_buljol;
            case "Chicken pelau bowl": return R.drawable.tt_lunch_chicken_pelau_bowl;
            case "Curry chicken roti": return R.drawable.tt_lunch_curry_chicken_roti;
            case "Stew chicken, rice and callaloo": return R.drawable.tt_lunch_stew_chicken_rice_callaloo;
            case "Fish and provision plate": return R.drawable.tt_lunch_fish_provision_plate;
            case "Channa and aloo roti": return R.drawable.tt_lunch_channa_aloo_roti;
            case "Turkey and rice bowl": return R.drawable.tt_lunch_turkey_rice_bowl;
            case "Curry channa rice bowl": return R.drawable.tt_lunch_curry_channa_rice_bowl;
            case "Bake and shark plate": return R.drawable.tt_lunch_bake_shark_plate;
            case "Tobago crab and dumpling": return R.drawable.tt_lunch_tobago_crab_dumpling;
            case "Curry crab rice bowl": return R.drawable.tt_lunch_curry_crab_rice_bowl;
            case "Geera pork rice plate": return R.drawable.tt_lunch_geera_pork_rice_plate;
            case "Chicken chow mein and vegetables": return R.drawable.tt_lunch_chicken_chow_mein;
            case "Fish broth": return R.drawable.tt_lunch_fish_broth;
            case "Callaloo stew, beans and rice": return R.drawable.tt_lunch_callaloo_stew_beans_rice;
            default: return 0;
        }
    }

    private void showHomeLegacy() {
        AccountStore.Profile profile = AccountStore.get(this);
        LinearLayout body = appPage("Home");
        title(body, "TODAY", "Train with purpose", "Showing up today builds a stronger tomorrow.");
        Calendar homeWeekStart = homeWeekStart(homeWeekOffset);
        boolean homeWeekConfirmed = hasHomeWeekSchedule(homeWeekOffset, homeWeekStart);
        WeeklyScheduleStore.Schedule homeSchedule = homeWeekSchedule(homeWeekOffset, homeWeekStart, homeWeekConfirmed);
        int homeTrainingDays = countTrainingDays(homeSchedule.workoutDays);

        // Surface the exact saved plan when one exists; a generic recommendation must not
        // silently replace the movements the user chose in the builder.
        List<Coach.Exercise> recommendation = session.isEmpty()
                ? Coach.recommendation(target, goal, environment)
                : session;
        Coach.Exercise first = recommendation.get(0);
        LinearLayout next = darkCard();
        LinearLayout nextRow = new LinearLayout(this);
        nextRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView nextIcon = new ImageView(this);
        nextIcon.setImageResource(R.drawable.ic_nav_plan);
        nextIcon.setImageTintList(ColorStateList.valueOf(navy));
        nextIcon.setPadding(dp(14), dp(14), dp(14), dp(14));
        nextIcon.setBackground(surface(mint, 14, 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams nextIconParams = new LinearLayout.LayoutParams(dp(66), dp(66));
        nextIconParams.rightMargin = dp(13);
        nextRow.addView(nextIcon, nextIconParams);
        LinearLayout nextCopy = new LinearLayout(this);
        nextCopy.setOrientation(LinearLayout.VERTICAL);
        TextView nextLabel = heading("NEXT WORKOUT", 11, mint);
        nextLabel.setLetterSpacing(.06f);
        nextCopy.addView(nextLabel);
        nextCopy.addView(heading(target + " strength", 21, Color.WHITE));
        nextCopy.addView(text(sessionMinutes + " min  •  " + goal, 13, Color.rgb(202, 216, 234)));
        nextRow.addView(nextCopy, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = heading("›", 32, mint);
        arrow.setGravity(Gravity.CENTER);
        nextRow.addView(arrow, new LinearLayout.LayoutParams(dp(30), dp(54)));
        next.addView(nextRow);
        gap(next, 12);
        Button start = primary("Start workout");
        start.setOnClickListener(view -> {
            if (session.isEmpty()) showPlans();
            else showWorkoutCountdown();
        });
        addButton(next, start);
        body.addView(next);

        gap(body, 20);
        body.addView(heading(homeWeekOffset == 0 ? "This week" : "Selected week", 21, Color.WHITE));
        String homeSummary = homeWeekConfirmed
                ? homeTrainingDays + " scheduled session" + (homeTrainingDays == 1 ? " · " : "s · ") + (7 - homeTrainingDays) + " Rest Day" + (7 - homeTrainingDays == 1 ? "" : "s")
                : "No sessions scheduled yet · choose a week to plan";
        body.addView(text(homeSummary, 14, mint));
        gap(body, 9);
        LinearLayout weekly = darkCard();
        addHomeWeekHeader(weekly, homeWeekStart);
        gap(weekly, 9);
        LinearLayout weekRow = new LinearLayout(this);
        weekRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < weekDays.length; i++) {
            boolean workoutDay = homeWeekConfirmed && homeSchedule.workoutDays[i];
            boolean isToday = homeWeekOffset == 0 && isToday(homeWeekStart, i);
            Calendar date = homeDate(homeWeekStart, i);
            LinearLayout day = homeScheduleDayTile(date, i, workoutDay, homeWeekConfirmed, homeSchedule, isToday);
            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(0, dp(90), 1);
            dayParams.rightMargin = i == weekDays.length - 1 ? 0 : dp(3);
            weekRow.addView(day, dayParams);
        }
        weekly.addView(weekRow);
        TextView legend = text("Mint = workout  ·  coral = endurance  ·  slate = rest" + (homeWeekConfirmed ? "" : "  ·  open = not planned"), 12, Color.rgb(202, 216, 234));
        legend.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams legendParams = new LinearLayout.LayoutParams(-1, -2);
        legendParams.topMargin = dp(8);
        weekly.addView(legend, legendParams);
        if (homeWeekOffset == 0) {
            Calendar nextWeekStart = homeWeekStart(1);
            boolean nextConfirmed = hasHomeWeekSchedule(1, nextWeekStart);
            if (nextConfirmed) addNextWeekHighlights(weekly, nextWeekStart, homeWeekSchedule(1, nextWeekStart, true));
        }
        body.addView(weekly);

        gap(body, 14);
        body.addView(heading("Upcoming weeks", 19, Color.WHITE));
        body.addView(text("Browse ahead without changing this week’s plan.", 13, mint));
        gap(body, 8);
        addUpcomingWeekCarousel(body);

        gap(body, 12);
        NutritionRecommendation.Day food = todayNutritionDay();
        LinearLayout fuel = darkCard();
        fuel.addView(heading("Today’s fuel", 19, Color.WHITE));
        fuel.addView(text("Two flexible ideas for today. Adapt them to your appetite, culture, allergies, and budget.", 13, Color.rgb(202, 216, 234)));
        gap(fuel, 8);
        addDarkMealSuggestion(fuel, "Breakfast", food.breakfastTitle, food.breakfastDetail);
        gap(fuel, 7);
        addDarkMealSuggestion(fuel, "Lunch", food.lunchTitle, food.lunchDetail);
        Button openNutrition = secondary("Open nutrition ideas");
        openNutrition.setOnClickListener(view -> showMeals());
        LinearLayout.LayoutParams nutritionParams = new LinearLayout.LayoutParams(-1, dp(42));
        nutritionParams.topMargin = dp(10);
        fuel.addView(openNutrition, nutritionParams);
        body.addView(fuel);

        gap(body, 12);
        LinearLayout month = darkCard();
        JourneyStore.MonthReport monthReport = JourneyStore.monthReport(this);
        month.addView(heading("Monthly consistency", 19, Color.WHITE));
        month.addView(text(monthReport.completedTotal + " completed guided plans  •  " + monthReport.minutesTotal + " active min", 13, Color.rgb(202, 216, 234)));
        gap(month, 8);
        addDarkMonthlyCompletionChart(month, monthReport);
        body.addView(month);

        gap(body, 12);
        LinearLayout summaryRow = new LinearLayout(this);
        summaryRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout sessions = darkCard();
        sessions.addView(heading(String.valueOf("Weekly".equals(planMode) ? trainingDays : session.size()), 28, Color.WHITE));
        sessions.addView(text("Weekly".equals(planMode) ? "planned sessions" : "planned session", 14, Color.rgb(202, 216, 234)));
        summaryRow.addView(sessions, new LinearLayout.LayoutParams(0, -2, 1));
        View space = new View(this);
        summaryRow.addView(space, new LinearLayout.LayoutParams(dp(9), 1));
        LinearLayout ready = darkCard();
        ready.addView(heading("Ready", 22, Color.WHITE));
        ready.addView(text("Your next session is prepared.", 14, Color.rgb(202, 216, 234)));
        summaryRow.addView(ready, new LinearLayout.LayoutParams(0, -2, 1));
        body.addView(summaryRow);

        gap(body, 14);
        Button plans = secondary("View my plan");
        plans.setOnClickListener(view -> showPlans());
        addButton(body, plans);
    }

    private Calendar homeWeekStart(int offset) {
        Calendar current = Calendar.getInstance();
        current.setFirstDayOfWeek(Calendar.MONDAY);
        int sinceMonday = (current.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
        current.add(Calendar.DAY_OF_YEAR, -sinceMonday);
        current.add(Calendar.DAY_OF_YEAR, Math.max(0, offset) * 7);
        return current;
    }

    private boolean hasHomeWeekSchedule(int offset, Calendar weekStart) {
        return WeeklyScheduleStore.hasSavedSchedule(this, weekStart);
    }

    private WeeklyScheduleStore.Schedule homeWeekSchedule(int offset, Calendar weekStart, boolean confirmed) {
        if (!confirmed) return new WeeklyScheduleStore.Schedule(new boolean[7], emptyWeeklyTargets());
        return WeeklyScheduleStore.load(this, weekStart, defaultWeeklyDays(), defaultWeeklyTargets());
    }

    private ArrayList<List<String>> emptyWeeklyTargets() {
        ArrayList<List<String>> targets = new ArrayList<>();
        for (int i = 0; i < 7; i++) targets.add(new ArrayList<>());
        return targets;
    }

    private void addHomeWeekHeader(LinearLayout container, Calendar weekStart) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView previous = homeWeekArrow("‹", homeWeekOffset > 0, view -> {
            homeWeekOffset--;
            refreshCurrentPageKeepingScroll(this::showHome);
        });
        header.addView(previous, new LinearLayout.LayoutParams(dp(38), dp(38)));
        TextView label = heading(homeWeekRange(weekStart), 18, ink);
        label.setGravity(Gravity.CENTER);
        header.addView(label, new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView next = homeWeekArrow("›", homeWeekOffset < HOME_UPCOMING_WEEK_COUNT, view -> {
            homeWeekOffset++;
            refreshCurrentPageKeepingScroll(this::showHome);
        });
        header.addView(next, new LinearLayout.LayoutParams(dp(38), dp(38)));
        container.addView(header);
    }

    private TextView homeWeekArrow(String symbol, boolean enabled, View.OnClickListener listener) {
        TextView arrow = heading(symbol, 29, enabled ? ink : cardLine);
        arrow.setGravity(Gravity.CENTER);
        arrow.setBackground(surface(enabled ? Color.rgb(240, 244, 249) : Color.rgb(247, 248, 252), 99, 1, cardLine));
        arrow.setClickable(enabled);
        arrow.setFocusable(enabled);
        if (enabled) arrow.setOnClickListener(listener);
        arrow.setContentDescription(("‹".equals(symbol) ? "Previous" : "Next") + " schedule week");
        return arrow;
    }

    private void addUpcomingWeekCarousel(LinearLayout body) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int offset = 1; offset <= HOME_UPCOMING_WEEK_COUNT; offset++) {
            Calendar weekStart = homeWeekStart(offset);
            boolean confirmed = hasHomeWeekSchedule(offset, weekStart);
            WeeklyScheduleStore.Schedule schedule = homeWeekSchedule(offset, weekStart, confirmed);
            LinearLayout tile = upcomingWeekTile(offset, weekStart, schedule, confirmed);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(206), -2);
            params.rightMargin = dp(9);
            row.addView(tile, params);
        }
        scroll.addView(row, new HorizontalScrollView.LayoutParams(-2, -2));
        body.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
        gap(body, 9);
        Button browse = lightAction("Browse all weeks");
        browse.setOnClickListener(view -> showMonthScheduleDialog());
        addButton(body, browse);
    }

    private LinearLayout upcomingWeekTile(int offset, Calendar weekStart, WeeklyScheduleStore.Schedule schedule, boolean confirmed) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(11), dp(10), dp(11), dp(10));
        boolean selected = offset == homeWeekOffset;
        tile.setBackground(surface(navyPanel, 15, selected ? 2 : 1, selected ? mint : Color.rgb(35, 58, 91)));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(view -> {
            homeWeekOffset = offset;
            refreshCurrentPageKeepingScroll(this::showHome);
        });
        tile.addView(heading(homeWeekRange(weekStart), 15, Color.WHITE));
        int sessions = countTrainingDays(schedule.workoutDays);
        tile.addView(text(confirmed ? "Scheduled training" : "No plan set yet", 12, Color.rgb(202, 216, 234)));
        LinearLayout blocks = new LinearLayout(this);
        blocks.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams blocksParams = new LinearLayout.LayoutParams(-1, -2);
        blocksParams.topMargin = dp(7);
        for (int day = 0; day < 7; day++) {
            if (!schedule.workoutDays[day]) continue;
            TextView block = text(weekDays[day] + " " + homeWeekDate(weekStart, day) + "  ·  " + homeFocusForDay(day, schedule), 11, navy);
            block.setPadding(dp(7), dp(4), dp(7), dp(4));
            block.setBackground(surface(mint, 7, 0, Color.TRANSPARENT));
            LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(-1, -2);
            blockParams.bottomMargin = dp(3);
            blocks.addView(block, blockParams);
        }
        if (confirmed && sessions == 0) blocks.addView(text("Rest week", 12, Color.rgb(202, 216, 234)));
        tile.addView(blocks, blocksParams);
        tile.setContentDescription(homeWeekRange(weekStart) + ", " + (confirmed ? sessions + " scheduled sessions" : "no plan set") + ". Open this week.");
        return tile;
    }

    private void addNextWeekHighlights(LinearLayout container, Calendar weekStart, WeeklyScheduleStore.Schedule schedule) {
        int count = countTrainingDays(schedule.workoutDays);
        if (count == 0) return;
        gap(container, 10);
        container.addView(heading("Next week · selected days", 13, ink));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int day = 0; day < 7; day++) {
            boolean workout = schedule.workoutDays[day];
            TextView item = text(weekDays[day] + "\n" + homeWeekDate(weekStart, day), 10, workout ? navy : muted);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(2), dp(5), dp(2), dp(5));
            item.setBackground(surface(workout ? mint : Color.rgb(232, 236, 244), 8, 0, Color.TRANSPARENT));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1);
            params.rightMargin = day == 6 ? 0 : dp(3);
            row.addView(item, params);
        }
        container.addView(row);
    }

    private void showMonthScheduleDialog() {
        Calendar month = Calendar.getInstance();
        month.set(Calendar.DAY_OF_MONTH, 1);
        month.add(Calendar.MONTH, homeCalendarMonthOffset);
        Dialog dialog = new Dialog(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(18));
        content.setBackgroundColor(paper);
        content.addView(heading("Training calendar", 22, ink));
        content.addView(text("Every scheduled strength session, rest day, and endurance appointment.", 13, muted));
        LinearLayout monthHeader = new LinearLayout(this);
        monthHeader.setGravity(Gravity.CENTER_VERTICAL);
        Button previous = action("‹", Color.WHITE, ink);
        previous.setContentDescription("Previous month");
        previous.setOnClickListener(view -> { homeCalendarMonthOffset--; dialog.dismiss(); showMonthScheduleDialog(); });
        monthHeader.addView(previous, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView monthLabel = heading(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.getTime()), 19, ink);
        monthLabel.setGravity(Gravity.CENTER);
        monthHeader.addView(monthLabel, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button next = action("›", Color.WHITE, ink);
        next.setContentDescription("Next month");
        next.setOnClickListener(view -> { homeCalendarMonthOffset++; dialog.dismiss(); showMonthScheduleDialog(); });
        monthHeader.addView(next, new LinearLayout.LayoutParams(dp(42), dp(42)));
        content.addView(monthHeader);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        String[] shortDays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String day : shortDays) grid.addView(monthCalendarHeader(day));
        int first = (month.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
        for (int blank = 0; blank < first; blank++) grid.addView(monthCalendarCell(null));
        int max = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int date = 1; date <= max; date++) {
            Calendar value = (Calendar) month.clone();
            value.set(Calendar.DAY_OF_MONTH, date);
            grid.addView(monthCalendarCell(value));
        }
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(-1, -2);
        gridParams.topMargin = dp(8);
        content.addView(grid, gridParams);
        TextView legend = text("Mint  workout     Coral  Run / Walk / Cycle     Slate  rest", 11, muted);
        legend.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams legendParams = new LinearLayout.LayoutParams(-1, -2);
        legendParams.topMargin = dp(10);
        content.addView(legend, legendParams);
        Button close = lightAction("Close");
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, dp(46));
        closeParams.topMargin = dp(16);
        content.addView(close, closeParams);
        dialog.setContentView(content);
        dialog.show();
    }

    private TextView monthCalendarHeader(String label) {
        TextView header = text(label, 10, muted);
        header.setGravity(Gravity.CENTER);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dp(42); params.height = dp(28);
        header.setLayoutParams(params);
        return header;
    }

    private LinearLayout monthCalendarCell(Calendar date) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dp(42); params.height = dp(60);
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        cell.setLayoutParams(params);
        if (date == null) return cell;
        boolean planned = isMonthWorkoutDay(date);
        List<SchedulerStore.Appointment> appointments = SchedulerStore.onDate(this, date);
        boolean endurance = !appointments.isEmpty();
        int fill = endurance && !planned ? coral : planned ? mint : Color.rgb(238, 242, 247);
        boolean today = SchedulerStore.dateKey(date).equals(SchedulerStore.dateKey(Calendar.getInstance()));
        cell.setPadding(dp(2), dp(3), dp(2), dp(2));
        cell.setBackground(surface(fill, 8, today ? 2 : 0, today ? coral : Color.TRANSPARENT));
        TextView number = text(String.valueOf(date.get(Calendar.DAY_OF_MONTH)), 12, planned || endurance ? navy : muted);
        number.setGravity(Gravity.CENTER);
        number.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        cell.addView(number, new LinearLayout.LayoutParams(-1, dp(22)));
        if (planned) {
            TextView plan = text("PLAN", 8, navy);
            plan.setGravity(Gravity.CENTER);
            cell.addView(plan, new LinearLayout.LayoutParams(-1, endurance ? dp(16) : dp(28)));
        }
        if (endurance) {
            TextView activity = text(enduranceDayLabel(appointments), 8, navy);
            activity.setGravity(Gravity.CENTER);
            activity.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            activity.setBackground(surface(coral, 5, 0, Color.TRANSPARENT));
            cell.addView(activity, new LinearLayout.LayoutParams(-1, planned ? dp(14) : dp(28)));
        } else if (!planned) {
            TextView rest = text("REST", 8, muted);
            rest.setGravity(Gravity.CENTER);
            cell.addView(rest, new LinearLayout.LayoutParams(-1, dp(28)));
        }
        cell.setClickable(true);
        cell.setContentDescription(new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(date.getTime()) + ", " + (planned ? "work plan" : "rest day") + (endurance ? ", " + enduranceDayLabel(appointments) : ""));
        cell.setOnClickListener(view -> showMonthDayDetail(date));
        return cell;
    }

    private void showMonthDayDetail(Calendar date) {
        List<SchedulerStore.Appointment> appointments = SchedulerStore.onDate(this, date);
        StringBuilder detail = new StringBuilder(isMonthWorkoutDay(date) ? "Work plan · " + schedulerStrengthSummary(date) : "Rest day");
        for (SchedulerStore.Appointment appointment : appointments) {
            detail.append("\n").append(appointment.mode).append(" · ").append(schedulerTime(appointment.when)).append(" · ").append(appointment.durationMinutes).append(" min");
        }
        new AlertDialog.Builder(this).setTitle(new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(date.getTime()))
                .setMessage(detail.toString()).setPositiveButton("Done", null).show();
    }

    private boolean isMonthWorkoutDay(Calendar date) {
        Calendar weekStart = (Calendar) date.clone();
        weekStart.setFirstDayOfWeek(Calendar.MONDAY);
        weekStart.add(Calendar.DAY_OF_YEAR, -((weekStart.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7));
        int day = (date.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
        WeeklyScheduleStore.Schedule schedule = WeeklyScheduleStore.load(this, weekStart, defaultWeeklyDays(), defaultWeeklyTargets());
        return schedule.workoutDays[day];
    }

    private String homeWeekRange(Calendar weekStart) {
        Calendar end = (Calendar) weekStart.clone();
        end.add(Calendar.DAY_OF_YEAR, 6);
        SimpleDateFormat startFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
        SimpleDateFormat endFormat = new SimpleDateFormat(weekStart.get(Calendar.MONTH) == end.get(Calendar.MONTH) ? "d" : "MMM d", Locale.getDefault());
        return startFormat.format(weekStart.getTime()) + "–" + endFormat.format(end.getTime());
    }

    private int homeWeekDate(Calendar weekStart, int index) {
        return homeDate(weekStart, index).get(Calendar.DAY_OF_MONTH);
    }

    private Calendar homeDate(Calendar weekStart, int index) {
        Calendar date = (Calendar) weekStart.clone();
        date.add(Calendar.DAY_OF_YEAR, index);
        return date;
    }

    /** Compact split tiles make a same-day strength plan and endurance booking immediately legible. */
    private LinearLayout homeScheduleDayTile(Calendar date, int dayIndex, boolean workoutDay, boolean confirmed,
                                               WeeklyScheduleStore.Schedule schedule, boolean today) {
        List<SchedulerStore.Appointment> appointments = SchedulerStore.onDate(this, date);
        boolean endurance = !appointments.isEmpty();
        int base = endurance && !workoutDay ? coral : workoutDay ? mint : Color.rgb(232, 236, 244);
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(2), dp(5), dp(2), dp(3));
        tile.setBackground(surface(base, 10, today ? 2 : 0, today ? Color.WHITE : Color.TRANSPARENT));
        TextView dateLabel = text(weekDays[dayIndex] + "\n" + homeWeekDate(date, 0), 9, (workoutDay || endurance) ? navy : muted);
        dateLabel.setGravity(Gravity.CENTER);
        dateLabel.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        tile.addView(dateLabel, new LinearLayout.LayoutParams(-1, dp(34)));
        if (workoutDay) {
            TextView plan = text(homeFocusForDay(dayIndex, schedule), 8, navy);
            plan.setGravity(Gravity.CENTER);
            plan.setMaxLines(2);
            tile.addView(plan, new LinearLayout.LayoutParams(-1, endurance ? dp(25) : dp(40)));
        } else if (!endurance) {
            TextView rest = text(confirmed ? "REST" : "OPEN", 8, muted);
            rest.setGravity(Gravity.CENTER);
            tile.addView(rest, new LinearLayout.LayoutParams(-1, dp(40)));
        }
        if (endurance) {
            TextView activity = text(enduranceDayLabel(appointments), 8, navy);
            activity.setGravity(Gravity.CENTER);
            activity.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            activity.setBackground(surface(coral, 6, 0, Color.TRANSPARENT));
            LinearLayout.LayoutParams activityParams = new LinearLayout.LayoutParams(-1, workoutDay ? dp(19) : dp(40));
            activityParams.leftMargin = dp(1); activityParams.rightMargin = dp(1);
            tile.addView(activity, activityParams);
        }
        String state = workoutDay ? homeFocusForDay(dayIndex, schedule) + " workout" : confirmed ? "Rest Day" : "Open";
        if (endurance) state += ", " + enduranceDayLabel(appointments);
        tile.setContentDescription(weekDays[dayIndex] + " " + homeWeekDate(date, 0) + ", " + state);
        return tile;
    }

    private String enduranceDayLabel(List<SchedulerStore.Appointment> appointments) {
        if (appointments.isEmpty()) return "";
        String label = appointmentAbbreviation(appointments.get(0).mode).toUpperCase(Locale.ROOT);
        return appointments.size() > 1 ? label + " +" + (appointments.size() - 1) : label;
    }

    private boolean isToday(Calendar weekStart, int index) {
        Calendar date = (Calendar) weekStart.clone();
        date.add(Calendar.DAY_OF_YEAR, index);
        Calendar today = Calendar.getInstance();
        return date.get(Calendar.YEAR) == today.get(Calendar.YEAR) && date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    private int countTrainingDays(boolean[] days) {
        int count = 0;
        for (boolean day : days) if (day) count++;
        return count;
    }

    private String homeFocusForDay(int index, WeeklyScheduleStore.Schedule schedule) {
        if (index >= 0 && index < schedule.targets.size() && !schedule.targets.get(index).isEmpty()) return focusSummary(schedule.targets.get(index));
        return target;
    }

    /** The Home hero must be a view of the user's schedule, never a demo workout. */
    private HomeSession nextHomeSession() {
        Calendar week = homeWeekStart(0);
        boolean confirmed = hasHomeWeekSchedule(0, week);
        WeeklyScheduleStore.Schedule schedule = homeWeekSchedule(0, week, confirmed);
        for (int i = 0; i < weekDays.length; i++) {
            Calendar date = homeDate(week, i);
            if (schedule.workoutDays[i]) {
                return new HomeSession(homeFocusForDay(i, schedule), sessionMinutes + " min  ·  Scheduled workout");
            }
            List<SchedulerStore.Appointment> appointments = SchedulerStore.onDate(this, date);
            if (!appointments.isEmpty()) {
                SchedulerStore.Appointment appointment = appointments.get(0);
                return new HomeSession(appointmentAbbreviation(appointment.mode), appointment.durationMinutes + " min  ·  Scheduled endurance");
            }
        }
        return new HomeSession("Build your\nfirst plan", "Choose a workout or endurance session");
    }

    private static final class HomeSession {
        final String title;
        final String detail;

        HomeSession(String title, String detail) {
            this.title = title;
            this.detail = detail;
        }
    }

    private void addMealSuggestion(LinearLayout container, String label, NutritionRecommendation.Meal meal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(surface(Color.rgb(240, 244, 249), 12, 0, Color.TRANSPARENT));
        row.setClickable(true);
        row.setFocusable(true);
        TextView mealLabel = heading(label.toUpperCase(Locale.ROOT), 10, Color.rgb(67, 98, 139));
        mealLabel.setLetterSpacing(.05f);
        row.addView(mealLabel);
        row.addView(heading(meal.title, 16, ink));
        row.addView(text(meal.detail, 12, muted));
        TextView facts = heading("View ingredient nutrition facts  ›", 12, mint);
        LinearLayout.LayoutParams factsParams = new LinearLayout.LayoutParams(-1, -2); factsParams.topMargin = dp(5); row.addView(facts, factsParams);
        row.setContentDescription("View " + label.toLowerCase(Locale.ROOT) + " nutrition facts for " + meal.title);
        row.setOnClickListener(view -> showMealNutritionFacts(label, meal));
        applyPressMotion(row);
        container.addView(row);
    }

    /** Dedicated ingredient breakdown for the selected daily meal; all totals are reference estimates. */
    private void showMealNutritionFacts(String mealLabel, NutritionRecommendation.Meal meal) {
        LinearLayout body = appPage("Nutrition");
        title(body, mealLabel.toUpperCase(Locale.ROOT) + " NUTRITION", meal.title, "Every component is listed separately so you can see how the meal total is formed. Values are practical serving estimates, not medical targets.");

        int imageRes = mealImage(meal);
        if (imageRes != 0) {
            ImageView mealPreview = new ImageView(this);
            mealPreview.setImageResource(imageRes);
            mealPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mealPreview.setContentDescription("Photo of " + meal.title);
            mealPreview.setClipToOutline(true);
            mealPreview.setBackground(surface(Color.WHITE, 20, 0, Color.TRANSPARENT));
            body.addView(mealPreview, new LinearLayout.LayoutParams(-1, dp(190)));
            gap(body, 12);
        }

        LinearLayout total = card();
        total.addView(heading("Meal total", 19, ink));
        total.addView(heading(meal.kcal() + " kcal", 31, navy));
        total.addView(text(String.format(Locale.US, "Protein %.1f g  •  Carbs %.1f g  •  Fibre %.1f g  •  Fat %.1f g", meal.protein(), meal.carbs(), meal.fibre(), meal.fat()), 13, muted));
        total.addView(text("Sodium " + meal.sodium() + " mg  •  Estimated from the listed portions", 12, muted));
        body.addView(total); gap(body, 12);

        body.addView(heading("What’s in this meal", 19, Color.WHITE));
        body.addView(text(meal.components.length + " components  •  Each amount is included in the total above", 13, Color.rgb(211, 225, 233))); gap(body, 8);
        for (NutritionRecommendation.Component component : meal.components) {
            LinearLayout item = card();
            LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(heading(component.name, 17, ink), new LinearLayout.LayoutParams(0, -2, 1));
            TextView serving = heading(component.serving, 13, navy); serving.setGravity(Gravity.RIGHT); header.addView(serving, new LinearLayout.LayoutParams(dp(88), -2));
            item.addView(header);
            item.addView(text(component.kcal + " kcal  •  Protein " + component.protein + " g  •  Carbs " + component.carbs + " g", 13, ink));
            item.addView(text("Fibre " + component.fibre + " g  •  Fat " + component.fat + " g  •  Sodium " + component.sodium + " mg", 12, muted));
            body.addView(item); gap(body, 8);
        }
        body.addView(text("Reference nutrition only. Recipes, brands, substitutions, cooking oil, salt, and serving sizes can change these values.", 12, Color.rgb(211, 225, 233))); gap(body, 10);
        Button back = secondary("Back to Today’s Fuel"); back.setOnClickListener(view -> showMeals()); addButton(body, back);
    }

    private void addDarkMealSuggestion(LinearLayout container, String label, String title, String detail) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(surface(Color.rgb(26, 48, 78), 11, 1, Color.rgb(35, 58, 91)));
        TextView mealLabel = heading(label.toUpperCase(Locale.ROOT), 10, mint);
        mealLabel.setLetterSpacing(.05f);
        row.addView(mealLabel);
        row.addView(heading(title, 16, Color.WHITE));
        row.addView(text(detail, 12, Color.rgb(202, 216, 234)));
        container.addView(row);
    }

    private void addMonthlyCompletionChart(LinearLayout container, JourneyStore.MonthReport report) {
        LinearLayout graph = new LinearLayout(this);
        graph.setOrientation(LinearLayout.HORIZONTAL);
        graph.setGravity(Gravity.BOTTOM);
        int highest = 1;
        for (int count : report.completedByWeek) highest = Math.max(highest, count);
        for (int i = 0; i < report.completedByWeek.length; i++) {
            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            TextView count = heading(String.valueOf(report.completedByWeek[i]), 12, ink);
            count.setGravity(Gravity.CENTER);
            column.addView(count, new LinearLayout.LayoutParams(-1, dp(19)));
            View bar = new View(this);
            int height = dp(14 + (72 * report.completedByWeek[i] / highest));
            bar.setBackground(surface(mint, 6, 0, Color.TRANSPARENT));
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(25), height);
            barParams.topMargin = dp(3);
            column.addView(bar, barParams);
            TextView label = text("W" + (i + 1), 10, muted);
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, dp(19));
            labelParams.topMargin = dp(4);
            column.addView(label, labelParams);
            LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(0, dp(121), 1);
            columnParams.rightMargin = i == report.completedByWeek.length - 1 ? 0 : dp(4);
            graph.addView(column, columnParams);
        }
        container.addView(graph);
    }

    private void addDarkMonthlyCompletionChart(LinearLayout container, JourneyStore.MonthReport report) {
        LinearLayout graph = new LinearLayout(this);
        graph.setOrientation(LinearLayout.HORIZONTAL);
        graph.setGravity(Gravity.BOTTOM);
        int highest = 1;
        for (int count : report.completedByWeek) highest = Math.max(highest, count);
        for (int i = 0; i < report.completedByWeek.length; i++) {
            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            TextView count = heading(String.valueOf(report.completedByWeek[i]), 12, Color.WHITE);
            count.setGravity(Gravity.CENTER);
            column.addView(count, new LinearLayout.LayoutParams(-1, dp(19)));
            View bar = new View(this);
            int height = dp(14 + (72 * report.completedByWeek[i] / highest));
            bar.setBackground(surface(mint, 6, 0, Color.TRANSPARENT));
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(25), height);
            barParams.topMargin = dp(3);
            column.addView(bar, barParams);
            TextView label = text("Week " + (i + 1), 10, Color.rgb(202, 216, 234));
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, dp(19));
            labelParams.topMargin = dp(4);
            column.addView(label, labelParams);
            LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(0, dp(121), 1);
            columnParams.rightMargin = i == report.completedByWeek.length - 1 ? 0 : dp(4);
            graph.addView(column, columnParams);
        }
        container.addView(graph);
    }

    private void showPlans() {
        showCoachHeroWorkout();
    }

    /**
     * Premium Coach Hero workout entry. This is deliberately composed from native controls so the
     * visual contract stays interactive: plan type, focus and equipment all feed the real wizard.
     */
    private void showCoachHeroWorkout() {
        beginNewPage();
        setSystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(navy);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(navy);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        setCoachHeroWizardShell(scroll, "Workouts");

        FrameLayout hero = coachHeroBackdrop("Workouts");
        root.addView(hero, new LinearLayout.LayoutParams(-1, dp(388)));

        addWorkoutWizardProgress(hero, "Plan");

        LinearLayout heroCopy = new LinearLayout(this);
        heroCopy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = heading("COACH " + coachName().toUpperCase(Locale.ROOT), 11, mint); eyebrow.setLetterSpacing(.16f);
        heroCopy.addView(eyebrow);
        TextView heading = heading("Plan your\nworkout", 34, Color.WHITE); heading.setLineSpacing(0, .92f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(dp(240), -2); titleParams.topMargin = dp(11);
        heroCopy.addView(heading, titleParams);
        TextView copy = text("Choose your schedule, focus, and equipment. I’ll build a session around you.", 15, Color.rgb(222, 233, 245));
        copy.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(dp(235), -2); copyParams.topMargin = dp(12);
        heroCopy.addView(copy, copyParams);
        FrameLayout.LayoutParams heroCopyParams = new FrameLayout.LayoutParams(dp(248), -2, Gravity.LEFT | Gravity.TOP);
        heroCopyParams.leftMargin = dp(20); heroCopyParams.topMargin = dp(98);
        hero.addView(heroCopy, heroCopyParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(20), dp(16), dp(34));
        content.setBackgroundColor(paper);
        root.addView(content, new LinearLayout.LayoutParams(-1, -2));

        TextView planEyebrow = heading("01  ·  PLAN TYPE", 11, muted); planEyebrow.setLetterSpacing(.12f);
        content.addView(planEyebrow);
        content.addView(heading("How do you want to train?", 23, ink));
        content.addView(text("Select the structure that fits your week.", 13, muted));
        gap(content, 13);
        LinearLayout planOptions = new LinearLayout(this);
        planOptions.setGravity(Gravity.CENTER_VERTICAL);
        planOptions.addView(coachHeroPlanOption("Weekly", "Build a routine", R.drawable.ic_plan_weekly), new LinearLayout.LayoutParams(0, dp(104), 1));
        LinearLayout.LayoutParams dailyParams = new LinearLayout.LayoutParams(0, dp(104), 1); dailyParams.leftMargin = dp(8);
        planOptions.addView(coachHeroPlanOption("Daily", "One focused day", R.drawable.ic_plan_daily), dailyParams);
        LinearLayout.LayoutParams customParams = new LinearLayout.LayoutParams(0, dp(104), 1); customParams.leftMargin = dp(8);
        planOptions.addView(coachHeroPlanOption("Custom", "Every detail", R.drawable.ic_plan_custom), customParams);
        content.addView(planOptions);
        gap(content, 18);
        LinearLayout hint = new LinearLayout(this); hint.setOrientation(LinearLayout.VERTICAL);
        hint.setPadding(dp(15), dp(14), dp(15), dp(14));
        hint.setBackground(surface(Color.rgb(225, 255, 246), 17, 0, Color.TRANSPARENT));
        hint.addView(heading("One decision at a time", 16, ink));
        hint.addView(text("Next, choose the body areas you want to train before placing them on your calendar.", 12, muted));
        content.addView(hint);
        gap(content, 16);
        Button continueButton = action("Next: choose training areas  →", coral, Color.WHITE);
        continueButton.setOnClickListener(view -> {
            if ("Daily".equals(planMode)) {
                if (sessionMinutes < 45) { sessionMinutes = 60; customDuration = false; }
                showPlanTarget();
            } else if ("Weekly".equals(planMode)) {
                if (sessionMinutes < 45) { sessionMinutes = 45; customDuration = false; }
                showPlanTarget();
            } else {
                showPlanTarget();
            }
        });
        content.addView(continueButton, new LinearLayout.LayoutParams(-1, dp(54)));
    }

    private LinearLayout coachHeroPlanOption(String mode, String detail, int iconRes) {
        boolean selected = mode.equals(planMode);
        LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL); tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(6), dp(9), dp(6), dp(7)); tile.setClickable(true); tile.setFocusable(true);
        tile.setBackground(surface(selected ? Color.rgb(255, 242, 239) : Color.WHITE, 17, selected ? 2 : 1, selected ? coral : cardLine));
        ImageView icon = new ImageView(this); icon.setImageResource(iconRes); icon.setImageTintList(ColorStateList.valueOf(selected ? coral : navy));
        tile.addView(icon, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView title = heading(mode, 13, ink); title.setGravity(Gravity.CENTER); tile.addView(title, new LinearLayout.LayoutParams(-1, dp(20)));
        TextView caption = text(detail, 9, muted); caption.setGravity(Gravity.CENTER); caption.setMaxLines(2); tile.addView(caption, new LinearLayout.LayoutParams(-1, dp(24)));
        tile.setOnClickListener(view -> {
            // Explicitly tapping a plan type starts a new draft, even if it is the
            // currently highlighted type. This prevents a prior Daily draft leaking in.
            beginWorkoutPlanDraft(mode);
            refreshCurrentPageKeepingScroll(this::showCoachHeroWorkout);
        });
        applyPressMotion(tile);
        return tile;
    }

    private LinearLayout coachHeroFocusOption(String value) {
        boolean selected = value.equalsIgnoreCase(target) || ("Upper body".equals(value) && "Upper".equalsIgnoreCase(target)) || ("Lower body".equals(value) && "Lower".equalsIgnoreCase(target));
        LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL); tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(4), dp(8), dp(4), dp(7)); tile.setClickable(true); tile.setFocusable(true);
        tile.setBackground(surface(selected ? Color.rgb(225, 255, 246) : Color.rgb(247, 249, 252), 15, selected ? 2 : 1, selected ? mint : cardLine));
        int iconRes;
        switch (value) {
            case "Upper body": iconRes = R.drawable.ic_nav_body; break;
            case "Lower body": iconRes = R.drawable.ic_nav_endurance; break;
            case "Arms": iconRes = R.drawable.ic_nav_workouts; break;
            case "Chest": iconRes = R.drawable.ic_home_complete; break;
            case "Back": iconRes = R.drawable.ic_nav_body; break;
            case "Shoulders": iconRes = R.drawable.ic_home_hiit; break;
            case "Core": iconRes = R.drawable.ic_nav_body; break;
            default: iconRes = R.drawable.ic_nav_endurance;
        }
        ImageView icon = new ImageView(this); icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(selected ? navy : muted));
        tile.addView(icon, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView label = heading(value, 10, ink); label.setGravity(Gravity.CENTER); label.setMaxLines(2);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, dp(31)); labelParams.topMargin = dp(4); tile.addView(label, labelParams);
        tile.setOnClickListener(view -> { target = value; refreshCurrentPageKeepingScroll(this::showCoachHeroWorkout); });
        applyPressMotion(tile);
        return tile;
    }

    /** Step 1: choose a planning rhythm before choosing movements. */
    private void showPlanType() {
        LinearLayout body = planPage();
        planTitle(body, "PLAN YOUR WORKOUT", "Build it your way", "Choose a schedule, body area, and equipment. Then select only the exercises you want.");
        addWizardProgress(body, wizardStep("Plan"));
        body.addView(heading("Choose your plan type", 21, Color.WHITE));
        gap(body, 10);
        body.addView(planTypeTile("Weekly", "Schedule workouts and rest days", R.drawable.ic_plan_weekly, true));
        gap(body, 9);
        body.addView(planTypeTile("Daily", "Build one focused session", R.drawable.ic_plan_daily, false));
        gap(body, 9);
        body.addView(planTypeTile("Custom", "Choose every detail yourself", R.drawable.ic_plan_custom, false));
        gap(body, 15);
        Button next = planPrimary("Next");
        next.setOnClickListener(view -> {
            if ("Daily".equals(planMode)) {
                if (sessionMinutes < 45) {
                    sessionMinutes = 60;
                    customDuration = false;
                }
                showPlanTarget();
            } else if ("Weekly".equals(planMode)) {
                if (sessionMinutes < 45) {
                    sessionMinutes = 45;
                    customDuration = false;
                }
                showPlanTarget();
            } else {
                showPlanTarget();
            }
        });
        addPlanButton(body, next);
        gap(body, 8);
        Button library = secondary("Browse exercise library");
        library.setOnClickListener(item -> showLibrary());
        addButton(body, library);
    }

    private void addWizardProgress(LinearLayout body, int activeStep) {
        // Legacy Workout steps (duration and setup) now use the same full-width
        // top rail as the Coach Hero wizard pages—not a second compact rail in
        // the scrolling content.
        if (legacyWorkoutWizardHero != null) {
            addCoachHeroWizardSteps(legacyWorkoutWizardHero, wizardLabels(), Math.max(0, activeStep - 1));
            legacyWorkoutWizardHero = null;
            return;
        }
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        String[] labels = wizardLabels();
        LinearLayout markers = new LinearLayout(this);
        markers.setOrientation(LinearLayout.HORIZONTAL);
        markers.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < labels.length; i++) {
            int step = i + 1;
            int markerSize = hasTallPlanViewport() ? 28 : 23;
            TextView marker = heading(step < activeStep ? "✓" : String.valueOf(step), hasTallPlanViewport() ? 15 : 13, step <= activeStep ? navy : Color.rgb(205, 215, 231));
            marker.setGravity(Gravity.CENTER);
            marker.setBackground(surface(step <= activeStep ? mint : navyPanel, 99, step <= activeStep ? 0 : 1, Color.rgb(95, 113, 139)));
            markers.addView(marker, new LinearLayout.LayoutParams(dp(markerSize), dp(markerSize)));
            if (i < labels.length - 1) {
                View connector = new View(this);
                connector.setBackgroundColor(step < activeStep ? mint : Color.rgb(75, 91, 117));
                LinearLayout.LayoutParams connectorParams = new LinearLayout.LayoutParams(0, dp(hasTallPlanViewport() ? 2 : 1), 1);
                connectorParams.leftMargin = dp(hasTallPlanViewport() ? 5 : 4);
                connectorParams.rightMargin = dp(hasTallPlanViewport() ? 5 : 4);
                markers.addView(connector, connectorParams);
            }
        }
        rail.addView(markers, new LinearLayout.LayoutParams(-1, dp(hasTallPlanViewport() ? 30 : 24)));
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < labels.length; i++) {
            TextView label = text(labels[i], hasTallPlanViewport() ? 9 : 8, i + 1 == activeStep ? mint : Color.rgb(205, 215, 231));
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            names.addView(label, new LinearLayout.LayoutParams(0, dp(hasTallPlanViewport() ? 21 : 17), 1));
        }
        rail.addView(names, new LinearLayout.LayoutParams(-1, dp(hasTallPlanViewport() ? 21 : 17)));
        body.addView(rail);
        gap(body, hasTallPlanViewport() ? 14 : 9);
    }

    private LinearLayout planTypeTile(String mode, String copy, int icon, boolean recommended) {
        boolean selected = mode.equals(planMode);
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(15), dp(12), dp(13), dp(12));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setContentDescription((selected ? "Selected plan type: " : "Choose plan type: ") + mode + ". " + copy);
        tile.setBackground(surface(paper, 19, selected ? 2 : 1, selected ? mint : cardLine));
        tile.setOnClickListener(view -> { planMode = mode; refreshCurrentPageKeepingScroll(this::showPlanType); });

        ImageView image = new ImageView(this);
        image.setImageResource(icon);
        image.setImageTintList(ColorStateList.valueOf(ink));
        tile.addView(image, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout detail = new LinearLayout(this);
        detail.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(0, -2, 1);
        detailParams.leftMargin = dp(13);
        tile.addView(detail, detailParams);
        detail.addView(heading(mode + " plan", 19, ink));
        detail.addView(text(copy, 13, muted));
        if (recommended && "Weekly".equals(mode)) {
            TextView badge = heading("Recommended", 10, navy);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(9), dp(5), dp(9), dp(5));
            badge.setBackground(surface(mint, 99, 0, Color.TRANSPARENT));
            tile.addView(badge, new LinearLayout.LayoutParams(-2, -2));
        }
        return tile;
    }

    /** Step 2, rebuilt in the Coach Hero Suite rather than the retired side-rail shell. */
    private void showPlanSchedule() {
        boolean weekly = "Weekly".equals(planMode);
        beginNewPage();
        setSystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(navy);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(navy);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        setCoachHeroWizardShell(scroll, "Workouts");

        FrameLayout hero = coachHeroBackdrop("Workouts schedule");
        root.addView(hero, new LinearLayout.LayoutParams(-1, dp(265)));

        LinearLayout heroCopy = new LinearLayout(this);
        heroCopy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = heading(weekly ? "WEEKLY PLAN" : "DAILY PLAN", 11, mint); eyebrow.setLetterSpacing(.14f);
        heroCopy.addView(eyebrow);
        TextView title = heading(weekly ? "Choose your\ntraining week" : "Choose your\nworkout day", 29, Color.WHITE);
        title.setLineSpacing(0, .92f); heroCopy.addView(title);
        TextView copy = text(weekly ? "Your plan stays separate for every week." : "Choose one date for a focused session.", 14, Color.rgb(225, 235, 246));
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(dp(240), -2); copyParams.topMargin = dp(9);
        heroCopy.addView(copy, copyParams);
        FrameLayout.LayoutParams heroCopyParams = new FrameLayout.LayoutParams(dp(260), -2, Gravity.LEFT | Gravity.TOP);
        heroCopyParams.leftMargin = dp(20); heroCopyParams.topMargin = dp(82);
        hero.addView(heroCopy, heroCopyParams);

        addWorkoutWizardProgress(hero, weekly ? "Week" : "Day");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(20), dp(16), dp(36));
        content.setBackgroundColor(paper);
        root.addView(content, new LinearLayout.LayoutParams(-1, -2));

        TextView scheduleLabel = heading(weekly ? "01  ·  TRAINING WEEK" : "01  ·  WORKOUT DAY", 11, muted); scheduleLabel.setLetterSpacing(.12f);
        content.addView(scheduleLabel);
        content.addView(heading(weekly ? "Place your training days" : "Pick your workout date", 23, ink));
        content.addView(text(weekly ? "Tap dates to switch between Train and Rest." : "Tap the one day you want to train.", 13, muted));
        gap(content, 14);

        // Calendar concept 4: an editorial, dark timeline that makes the week's
        // workout / recovery rhythm readable at a glance.
        LinearLayout calendar = new LinearLayout(this); calendar.setOrientation(LinearLayout.VERTICAL);
        calendar.setPadding(dp(16), dp(16), dp(16), dp(16));
        calendar.setBackground(surface(Color.rgb(10, 29, 50), 24, 1, Color.rgb(41, 83, 115)));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        Button previous = action("‹", Color.TRANSPARENT, Color.rgb(125, 163, 195)); previous.setTextSize(24); previous.setPadding(0, 0, 0, 0); previous.setMinHeight(0); previous.setEnabled(selectedWeekOffset > 0);
        previous.setContentDescription("Previous available week");
        previous.setOnClickListener(view -> { selectPlanningWeek(selectedWeekOffset - 1); refreshCurrentPageKeepingScroll(this::showPlanSchedule); });
        header.addView(previous, new LinearLayout.LayoutParams(dp(25), dp(42)));
        TextView weekTitle = heading(selectedWeekTitle().replace("Week of ", ""), 18, Color.WHITE); weekTitle.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        header.addView(weekTitle, new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout headerMeta = new LinearLayout(this); headerMeta.setOrientation(LinearLayout.VERTICAL); headerMeta.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView countText = heading(weekly ? trainingDays + " training days" : "1 training day", 9, Color.WHITE); countText.setGravity(Gravity.RIGHT); headerMeta.addView(countText, new LinearLayout.LayoutParams(-1, dp(18)));
        LinearLayout meter = new LinearLayout(this); meter.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        for (int part = 0; part < 5; part++) { View bar = new View(this); bar.setBackground(surface(part < Math.min(5, Math.max(1, trainingDays)) ? mint : Color.rgb(49, 77, 108), 99, 0, Color.TRANSPARENT)); LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(20), dp(7)); if (part > 0) barParams.leftMargin = dp(4); meter.addView(bar, barParams); }
        headerMeta.addView(meter, new LinearLayout.LayoutParams(-1, dp(13)));
        header.addView(headerMeta, new LinearLayout.LayoutParams(dp(118), dp(40)));
        Button nextWeek = action("›", Color.TRANSPARENT, Color.rgb(125, 163, 195)); nextWeek.setTextSize(24); nextWeek.setPadding(0, 0, 0, 0); nextWeek.setMinHeight(0); nextWeek.setEnabled(selectedWeekOffset < maxSelectableWeekOffset());
        nextWeek.setContentDescription("Next available week");
        nextWeek.setOnClickListener(view -> { selectPlanningWeek(selectedWeekOffset + 1); refreshCurrentPageKeepingScroll(this::showPlanSchedule); });
        header.addView(nextWeek, new LinearLayout.LayoutParams(dp(25), dp(42)));
        calendar.addView(header);

        LinearLayout days = new LinearLayout(this); days.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < weekDays.length; i++) {
            final int dayIndex = i;
            boolean selected = weekly ? weeklyWorkoutDays[i] : dailyScheduledDay == i;
            LinearLayout day = new LinearLayout(this); day.setOrientation(LinearLayout.VERTICAL); day.setGravity(Gravity.CENTER); day.setClickable(true); day.setFocusable(true);
            day.setPadding(dp(1), dp(4), dp(1), dp(2));
            day.setBackgroundColor(Color.TRANSPARENT);
            day.addView(heading(weekDays[i], 8, selected ? mint : Color.rgb(180, 200, 220)), new LinearLayout.LayoutParams(-1, dp(16)));
            TextView date = heading(String.valueOf(selectedWeekDate(i).get(Calendar.DAY_OF_MONTH)), 14, Color.WHITE); date.setGravity(Gravity.CENTER);
            day.addView(date, new LinearLayout.LayoutParams(-1, dp(23)));
            View dot = new View(this); dot.setBackground(surface(selected ? mint : Color.rgb(66, 91, 121), 99, selected ? 2 : 1, selected ? Color.rgb(159, 255, 231) : Color.rgb(101, 128, 159)));
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(selected ? 18 : 13), dp(selected ? 18 : 13)); dotParams.gravity = Gravity.CENTER; dotParams.topMargin = dp(2); day.addView(dot, dotParams);
            day.setOnClickListener(view -> {
                if (weekly) {
                    weeklyWorkoutDays[dayIndex] = !weeklyWorkoutDays[dayIndex];
                    if (weeklyWorkoutDays[dayIndex] && weeklyDayTargets.get(dayIndex).isEmpty()) weeklyDayTargets.get(dayIndex).addAll(selectedTargets);
                    activeScheduleDay = dayIndex; syncTrainingDays(); persistCurrentWeekSchedule();
                } else { dailyScheduledDay = dayIndex; }
                refreshCurrentPageKeepingScroll(this::showPlanSchedule);
            });
            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(0, dp(65), 1); if (i > 0) dayParams.leftMargin = dp(2);
            days.addView(day, dayParams);
        }
        LinearLayout.LayoutParams daysParams = new LinearLayout.LayoutParams(-1, dp(65)); daysParams.topMargin = dp(7); calendar.addView(days, daysParams);

        View timeline = new View(this); timeline.setBackgroundColor(Color.rgb(65, 111, 143));
        LinearLayout.LayoutParams timelineParams = new LinearLayout.LayoutParams(-1, dp(1)); timelineParams.leftMargin = dp(9); timelineParams.rightMargin = dp(9); timelineParams.topMargin = dp(-9); calendar.addView(timeline, timelineParams);

        LinearLayout cards = new LinearLayout(this); cards.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < weekDays.length; i++) {
            final int dayIndex = i;
            boolean selected = weekly ? weeklyWorkoutDays[i] : dailyScheduledDay == i;
            String scheduledFocus = selected && weekly && !weeklyDayTargets.get(i).isEmpty()
                    ? focusSummary(weeklyDayTargets.get(i)) : selected
                    ? focusSummary(selectedTargets) : "Rest";
            LinearLayout detail = new LinearLayout(this); detail.setOrientation(LinearLayout.VERTICAL); detail.setGravity(Gravity.CENTER); detail.setClickable(true);
            detail.setPadding(dp(2), dp(8), dp(2), dp(7));
            detail.setBackground(surface(selected ? Color.rgb(18, 63, 81) : Color.rgb(20, 46, 72), 10, selected ? 1 : 0, selected ? mint : Color.TRANSPARENT));
            ImageView icon = new ImageView(this);
            int iconRes = selected && "Endurance".equals(scheduledFocus) ? R.drawable.ic_nav_endurance
                    : selected ? R.drawable.ic_nav_workouts : R.drawable.ic_home_rest;
            icon.setImageResource(iconRes);
            icon.setImageTintList(ColorStateList.valueOf(selected ? mint : Color.rgb(154, 178, 205)));
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(-1, dp(22)); iconParams.leftMargin = dp(6); iconParams.rightMargin = dp(6); detail.addView(icon, iconParams);
            TextView label = heading(scheduledFocus, 9, selected ? Color.WHITE : Color.rgb(180, 200, 220)); label.setGravity(Gravity.CENTER); label.setSingleLine(true); detail.addView(label, new LinearLayout.LayoutParams(-1, dp(19)));
            TextView minutes = text(selected ? "45 min" : "Recover", 8, selected ? Color.rgb(180, 235, 222) : Color.rgb(132, 159, 188)); minutes.setGravity(Gravity.CENTER); detail.addView(minutes, new LinearLayout.LayoutParams(-1, dp(17)));
            detail.setOnClickListener(view -> {
                if (weekly) { weeklyWorkoutDays[dayIndex] = !weeklyWorkoutDays[dayIndex]; if (weeklyWorkoutDays[dayIndex] && weeklyDayTargets.get(dayIndex).isEmpty()) weeklyDayTargets.get(dayIndex).addAll(selectedTargets); activeScheduleDay = dayIndex; syncTrainingDays(); persistCurrentWeekSchedule(); }
                else { dailyScheduledDay = dayIndex; }
                refreshCurrentPageKeepingScroll(this::showPlanSchedule);
            });
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(0, dp(68), 1); if (i > 0) detailParams.leftMargin = dp(3); cards.addView(detail, detailParams);
        }
        LinearLayout.LayoutParams cardsParams = new LinearLayout.LayoutParams(-1, dp(68)); cardsParams.topMargin = dp(12); calendar.addView(cards, cardsParams);
        content.addView(calendar);

        if (weekly) {
            gap(content, 14);
            addWeeklyDayFocusAssignment(content);
            gap(content, 14);
            LinearLayout rhythm = new LinearLayout(this); rhythm.setOrientation(LinearLayout.VERTICAL);
            rhythm.setPadding(dp(15), dp(15), dp(15), dp(15)); rhythm.setBackground(surface(Color.WHITE, 20, 1, cardLine));
            rhythm.addView(heading("How many days this week?", 18, ink));
            rhythm.addView(text("Start with a rhythm, then place those days exactly where you want them.", 12, muted));
            LinearLayout counts = new LinearLayout(this); counts.setGravity(Gravity.CENTER_VERTICAL);
            for (int count : new int[]{2, 3, 4, 5}) {
                Button option = action(count + " days", count == trainingDays ? mint : Color.rgb(239, 243, 249), count == trainingDays ? navy : ink);
                option.setTextSize(11);
                option.setOnClickListener(view -> { applyTrainingDayCount(count); persistCurrentWeekSchedule(); refreshCurrentPageKeepingScroll(this::showPlanSchedule); });
                LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(0, dp(46), 1); if (count > 2) countParams.leftMargin = dp(6);
                counts.addView(option, countParams);
            }
            LinearLayout.LayoutParams countsParams = new LinearLayout.LayoutParams(-1, dp(46)); countsParams.topMargin = dp(11); rhythm.addView(counts, countsParams);
            content.addView(rhythm);
        }
        gap(content, 17);
        Button continueButton = action("Next: choose equipment  →", coral, Color.WHITE);
        continueButton.setOnClickListener(view -> {
            if (weekly && trainingDays == 0) { toast("Choose at least one workout day."); return; }
            if (weekly) persistCurrentWeekSchedule();
            showPlanEquipment();
        });
        content.addView(continueButton, new LinearLayout.LayoutParams(-1, dp(54)));
        Button back = new Button(this); back.setText("Back"); back.setTextColor(mint); back.setTextSize(14); back.setAllCaps(false); back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(view -> showPlanTarget());
        content.addView(back, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    /** Lets a weekly member assign the chosen training areas to each selected calendar day. */
    private void addWeeklyDayFocusAssignment(LinearLayout content) {
        LinearLayout assignment = card();
        assignment.addView(heading("ASSIGN TRAINING AREAS", 11, muted));
        assignment.addView(heading("What will you train each day?", 18, ink));
        assignment.addView(text("Select a training day, then choose one or two areas from your weekly choices.", 12, muted));
        HorizontalScrollView dayScroll = new HorizontalScrollView(this);
        dayScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout dayRow = new LinearLayout(this);
        dayRow.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < weeklyWorkoutDays.length; i++) {
            if (!weeklyWorkoutDays[i]) continue;
            final int dayIndex = i;
            boolean active = activeScheduleDay == i;
            Button day = action(scheduleDayLabel(i) + "\n" + focusSummary(weeklyDayTargets.get(i)), active ? mint : Color.rgb(234, 240, 246), active ? navy : ink);
            day.setTextSize(10);
            day.setOnClickListener(view -> { activeScheduleDay = dayIndex; syncPrimaryTarget(); refreshCurrentPageKeepingScroll(this::showPlanSchedule); });
            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(dp(116), dp(54));
            dayParams.rightMargin = dp(7);
            dayRow.addView(day, dayParams);
        }
        dayScroll.addView(dayRow, new HorizontalScrollView.LayoutParams(-2, -2));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, dp(58)); scrollParams.topMargin = dp(10);
        assignment.addView(dayScroll, scrollParams);

        List<String> choices = selectedTargets.isEmpty() ? java.util.Collections.singletonList(target) : selectedTargets;
        LinearLayout choicesRow = new LinearLayout(this);
        choicesRow.setGravity(Gravity.CENTER_VERTICAL);
        List<String> assigned = weeklyDayTargets.get(activeScheduleDay);
        for (String choice : choices) {
            boolean selected = assigned.contains(choice);
            Button choiceButton = action((selected ? "✓  " : "") + targetLabel(choice), selected ? mint : navyPanel, selected ? navy : mint);
            choiceButton.setTextSize(11);
            choiceButton.setOnClickListener(view -> {
                List<String> today = weeklyDayTargets.get(activeScheduleDay);
                if (today.contains(choice)) {
                    if (today.size() == 1) { toast("Keep one training area on a workout day."); return; }
                    today.remove(choice);
                } else if (today.size() < 2) today.add(choice);
                else { toast("Choose up to two areas for one training day."); return; }
                syncPrimaryTarget(); persistCurrentWeekSchedule(); refreshCurrentPageKeepingScroll(this::showPlanSchedule);
            });
            LinearLayout.LayoutParams choiceParams = new LinearLayout.LayoutParams(0, dp(44), 1);
            if (choicesRow.getChildCount() > 0) choiceParams.leftMargin = dp(7);
            choicesRow.addView(choiceButton, choiceParams);
        }
        assignment.addView(choicesRow, new LinearLayout.LayoutParams(-1, dp(44)));
        content.addView(assignment);
    }

    /** Retained only as a reference while the remaining legacy wizard screens are migrated. */
    private void showPlanScheduleLegacy() {
        boolean weekly = "Weekly".equals(planMode);
        LinearLayout body = planPage();
        planTitle(body, weekly ? "WEEKLY PLAN" : "DAILY PLAN", weekly ? "Choose your\ntraining week" : "Choose your\nworkout day", weekly
                ? "Plan this week or an upcoming week through month-end. Each week keeps its own training days and Rest Days."
                : "Choose one date this week or an upcoming week through month-end. Your daily plan stays attached to that date.");
        addWizardProgress(body, wizardStep(weekly ? "Week" : "Day"));

        LinearLayout picker = card();
        picker.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout pickerHeader = new LinearLayout(this);
        pickerHeader.setGravity(Gravity.CENTER_VERTICAL);
        Button previous = compactControl("‹");
        previous.setTextSize(24);
        previous.setContentDescription("Show the previous week");
        previous.setEnabled(selectedWeekOffset > 0);
        previous.setOnClickListener(view -> { selectPlanningWeek(selectedWeekOffset - 1); refreshCurrentPageKeepingScroll(this::showPlanSchedule); });
        pickerHeader.addView(previous, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView weekTitle = heading(selectedWeekTitle(), 17, ink);
        weekTitle.setGravity(Gravity.CENTER);
        pickerHeader.addView(weekTitle, new LinearLayout.LayoutParams(0, dp(44), 1));
        Button following = compactControl("›");
        following.setTextSize(24);
        following.setContentDescription("Show the next week");
        following.setEnabled(selectedWeekOffset < maxSelectableWeekOffset());
        following.setOnClickListener(view -> { selectPlanningWeek(selectedWeekOffset + 1); refreshCurrentPageKeepingScroll(this::showPlanSchedule); });
        pickerHeader.addView(following, new LinearLayout.LayoutParams(dp(44), dp(44)));
        picker.addView(pickerHeader);
        picker.addView(text(weekly ? "Tap a day to mark Train or Rest. Changes stay with this week only." : "Tap one day for your workout. Switching weeks never changes another week.", 12, muted));
        gap(picker, 10);

        LinearLayout days = new LinearLayout(this);
        days.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < weekDays.length; i++) {
            final int dayIndex = i;
            boolean selected = weekly ? weeklyWorkoutDays[i] : dailyScheduledDay == i;
            LinearLayout day = new LinearLayout(this);
            day.setOrientation(LinearLayout.VERTICAL);
            day.setGravity(Gravity.CENTER);
            day.setClickable(true);
            day.setFocusable(true);
            day.setContentDescription(scheduleDayLabel(i) + (selected ? ", selected workout day" : weekly ? ", rest day" : ", available workout day"));
            day.setBackground(surface(selected ? mint : Color.rgb(232, 236, 244), 12, selected ? 1 : 0, selected ? mint : Color.TRANSPARENT));
            TextView weekday = heading(weekDays[i], 9, selected ? navy : ink);
            weekday.setGravity(Gravity.CENTER);
            TextView date = heading(String.valueOf(selectedWeekDate(i).get(Calendar.DAY_OF_MONTH)), 15, selected ? navy : ink);
            date.setGravity(Gravity.CENTER);
            TextView state = text(selected ? "Train" : weekly ? "Rest" : "Open", 8, selected ? navy : muted);
            state.setGravity(Gravity.CENTER);
            day.addView(weekday, new LinearLayout.LayoutParams(-1, dp(17)));
            day.addView(date, new LinearLayout.LayoutParams(-1, dp(21)));
            day.addView(state, new LinearLayout.LayoutParams(-1, dp(16)));
            day.setOnClickListener(view -> {
                if (weekly) {
                    weeklyWorkoutDays[dayIndex] = !weeklyWorkoutDays[dayIndex];
                    if (weeklyWorkoutDays[dayIndex] && weeklyDayTargets.get(dayIndex).isEmpty()) {
                        weeklyDayTargets.get(dayIndex).add(target);
                    }
                    activeScheduleDay = dayIndex;
                    syncTrainingDays();
                    persistCurrentWeekSchedule();
                } else {
                    dailyScheduledDay = dayIndex;
                }
                refreshPlanKeepingScroll(this::showPlanSchedule);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(58), 1);
            params.rightMargin = i == weekDays.length - 1 ? 0 : dp(3);
            days.addView(day, params);
        }
        picker.addView(days);
        body.addView(picker);
        gap(body, 10);

        if (weekly) {
            LinearLayout rhythm = card();
            rhythm.addView(heading("How many days this week?", 17, ink));
            rhythm.addView(text("Start with a rhythm, then place those days exactly where you want them.", 12, muted));
            LinearLayout counts = new LinearLayout(this);
            counts.setOrientation(LinearLayout.HORIZONTAL);
            for (int count : new int[]{2, 3, 4, 5}) {
                Button option = count == trainingDays ? action(count + " days", mint, navy) : action(count + " days", Color.rgb(232, 236, 244), ink);
                option.setTextSize(11);
                option.setOnClickListener(view -> { applyTrainingDayCount(count); persistCurrentWeekSchedule(); refreshPlanKeepingScroll(this::showPlanSchedule); });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1);
                params.rightMargin = count == 5 ? 0 : dp(5);
                counts.addView(option, params);
            }
            LinearLayout.LayoutParams countsParams = new LinearLayout.LayoutParams(-1, -2);
            countsParams.topMargin = dp(9);
            rhythm.addView(counts, countsParams);
            body.addView(rhythm);
            gap(body, 12);
        }

        Button next = planPrimary(weekly ? "Next: assign workouts" : "Next: choose training areas");
        next.setOnClickListener(view -> {
            if (weekly && trainingDays == 0) {
                toast("Choose at least one workout day.");
                return;
            }
            if (weekly) persistCurrentWeekSchedule();
            showPlanTarget();
        });
        addPlanButton(body, next);
        Button back = planBack();
        back.setOnClickListener(view -> showPlanType());
        addPlanBack(body, back);
    }

    private void applyTrainingDayCount(int count) {
        boolean[] defaultRhythm = {true, false, true, false, true, false, false};
        for (int i = 0; i < weeklyWorkoutDays.length; i++) weeklyWorkoutDays[i] = false;
        int selected = 0;
        for (int i = 0; i < defaultRhythm.length && selected < count; i++) {
            if (defaultRhythm[i]) { weeklyWorkoutDays[i] = true; selected++; }
        }
        for (int i = 0; i < weeklyWorkoutDays.length && selected < count; i++) {
            if (!weeklyWorkoutDays[i]) { weeklyWorkoutDays[i] = true; selected++; }
        }
        syncTrainingDays();
        initialisePlanSelections();
    }

    /** Step 2: Coach Hero training-area page, shown before the date calendar. */
    private void showPlanTarget() {
        initialisePlanSelections();
        final boolean weekly = "Weekly".equals(planMode);
        // Weekly chooses an overall pool first. Each training day receives its own
        // assignment on the next calendar page, rather than copying one area to every day.
        final List<String> focuses = weekly ? selectedTargets : activeTargets();
        beginNewPage();
        setSystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setBackgroundColor(navy);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(navy);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        setCoachHeroWizardShell(scroll, "Workouts");

        FrameLayout hero = coachHeroBackdrop("Workouts training areas");
        root.addView(hero, new LinearLayout.LayoutParams(-1, dp(250)));
        LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = heading(weekly ? "WEEKLY PLAN · STEP 2" : "Custom".equals(planMode) ? "CUSTOM PLAN · STEP 2" : "DAILY PLAN · STEP 2", 10, mint); eyebrow.setLetterSpacing(.14f); copy.addView(eyebrow);
        TextView title = heading("What do you want\nto train?", 28, Color.WHITE); title.setLineSpacing(0, .93f); copy.addView(title);
        TextView subtitle = text("Choose up to two focus areas. Your calendar will use these to shape each session.", 13, Color.rgb(226, 236, 247));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(dp(255), -2); subtitleParams.topMargin = dp(8); copy.addView(subtitle, subtitleParams);
        FrameLayout.LayoutParams copyParams = new FrameLayout.LayoutParams(dp(274), -2, Gravity.LEFT | Gravity.TOP); copyParams.leftMargin = dp(20); copyParams.topMargin = dp(82); hero.addView(copy, copyParams);
        addWorkoutWizardProgress(hero, "Train");

        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(16), dp(20), dp(16), dp(36)); content.setBackgroundColor(paper);
        root.addView(content, new LinearLayout.LayoutParams(-1, -2));
        TextView section = heading("02  ·  TRAINING AREAS", 11, muted); section.setLetterSpacing(.12f); content.addView(section);
        content.addView(heading("Pick up to two areas", 23, ink));
        content.addView(text("Tap a card to add or remove it. The highlighted anatomy shows exactly what each choice trains.", 13, muted));
        gap(content, 13);
        for (int index = 0; index < WIZARD_TARGETS.length; index += 2) {
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            for (int offset = 0; offset < 2; offset++) {
                String choice = WIZARD_TARGETS[index + offset];
                LinearLayout tile = targetTile(choice, focuses.contains(choice));
                tile.setOnClickListener(view -> {
                    if (weekly) toggleWeeklyTrainingPool(choice); else toggleActiveTarget(choice);
                    session.clear(); resetExerciseSelection(); refreshCurrentPageKeepingScroll(this::showPlanTarget);
                });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(151), 1); if (offset == 0) params.rightMargin = dp(9); row.addView(tile, params);
            }
            content.addView(row); gap(content, 9);
        }
        LinearLayout selected = new LinearLayout(this); selected.setOrientation(LinearLayout.VERTICAL); selected.setPadding(dp(14), dp(12), dp(14), dp(12)); selected.setBackground(surface(Color.rgb(232, 255, 247), 16, 1, mint));
        selected.addView(heading("YOUR FOCUS", 10, Color.rgb(24, 107, 91)));
        selected.addView(text(focusSummary(focuses), 15, ink)); content.addView(selected);
        gap(content, 15);
        Button next = action((weekly || "Daily".equals(planMode)) ? "Next: choose training week  →" : "Next: choose workout length  →", coral, Color.WHITE);
        next.setOnClickListener(view -> {
            if (activeTargets().isEmpty()) { toast("Choose at least one training area."); return; }
            syncPrimaryTarget();
            if (weekly || "Daily".equals(planMode)) showPlanSchedule(); else showPlanDuration();
        });
        content.addView(next, new LinearLayout.LayoutParams(-1, dp(54)));
        Button back = new Button(this); back.setText("Back"); back.setTextSize(14); back.setTextColor(mint); back.setAllCaps(false); back.setBackgroundColor(Color.TRANSPARENT); back.setOnClickListener(view -> showCoachHeroWorkout()); content.addView(back, new LinearLayout.LayoutParams(-1, dp(46)));
    }

    /** Assign one or two focus areas to the single Daily date or the active Weekly date. */
    /** Retained until the legacy wizard shell is removed. */
    private void showPlanTargetLegacy() {
        initialisePlanSelections();
        boolean weekly = "Weekly".equals(planMode);
        if (weekly && !weeklyWorkoutDays[activeScheduleDay]) {
            for (int i = 0; i < weeklyWorkoutDays.length; i++) {
                if (weeklyWorkoutDays[i]) { activeScheduleDay = i; break; }
            }
        }
        List<String> focuses = activeTargets();
        LinearLayout body = planPage();
        String eyebrow = weekly ? "WEEKLY PLAN · " + scheduleDayLabel(activeScheduleDay).toUpperCase(Locale.ROOT)
                : "Daily".equals(planMode) ? "DAILY PLAN · " + scheduleDayLabel(dailyScheduledDay).toUpperCase(Locale.ROOT)
                : "CUSTOM PLAN";
        planTitle(body, eyebrow, "What do you want\nto train?", "Choose up to two focus areas. We’ll recommend movements that fit this workout.");
        addWizardProgress(body, wizardStep("Train"));
        if (weekly) addWeeklyAssignmentStrip(body);
        for (int index = 0; index < WIZARD_TARGETS.length; index += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int offset = 0; offset < 2; offset++) {
                String choice = WIZARD_TARGETS[index + offset];
                LinearLayout tile = targetTile(choice, focuses.contains(choice));
                tile.setOnClickListener(view -> {
                    toggleActiveTarget(choice);
                    session.clear();
                    resetExerciseSelection();
                    if (weekly) persistCurrentWeekSchedule();
                    refreshPlanKeepingScroll(this::showPlanTarget);
                });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(planTargetTileHeight()), 1);
                params.rightMargin = offset == 0 ? dp(planGridGap()) : 0;
                row.addView(tile, params);
            }
            body.addView(row);
            gap(body, planGridGap());
        }
        LinearLayout selection = card();
        selection.setPadding(dp(13), dp(11), dp(13), dp(11));
        selection.addView(heading(weekly ? scheduledDateSummary(activeScheduleDay) : scheduledDateSummary(dailyScheduledDay), 14, ink));
        selection.addView(text("Selected: " + focusSummary(focuses), 13, muted));
        body.addView(selection);
        gap(body, 10);
        Button next = planPrimary(("Daily".equals(planMode) || "Custom".equals(planMode)) ? "Next: choose workout length" : "Next: choose equipment");
        next.setOnClickListener(view -> {
            if (activeTargets().isEmpty()) {
                toast("Choose at least one training area.");
                return;
            }
            syncPrimaryTarget();
            if ("Daily".equals(planMode) || "Custom".equals(planMode)) showPlanDuration();
            else showPlanEquipment();
        });
        addPlanButton(body, next);
        Button back = planBack();
        back.setOnClickListener(view -> {
            if ("Weekly".equals(planMode) || "Daily".equals(planMode)) showPlanSchedule();
            else showPlanType();
        });
        addPlanBack(body, back);
    }

    private void addWeeklyAssignmentStrip(LinearLayout body) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout days = new LinearLayout(this);
        days.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < weeklyWorkoutDays.length; i++) {
            if (!weeklyWorkoutDays[i]) continue;
            final int dayIndex = i;
            boolean active = activeScheduleDay == i;
            Button day = action(scheduleDayLabel(i) + "\n" + focusSummary(weeklyDayTargets.get(i)), active ? mint : navyPanel, active ? navy : mint);
            day.setAllCaps(false);
            day.setTextSize(10);
            day.setGravity(Gravity.CENTER);
            day.setContentDescription(scheduleDayLabel(i) + ", " + focusSummary(weeklyDayTargets.get(i)) + (active ? ", currently editing" : ", edit this day"));
            day.setOnClickListener(view -> { activeScheduleDay = dayIndex; syncPrimaryTarget(); refreshCurrentPageKeepingScroll(this::showPlanTarget); });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(106), dp(52));
            params.rightMargin = dp(7);
            days.addView(day, params);
        }
        scroll.addView(days, new HorizontalScrollView.LayoutParams(-2, -2));
        body.addView(scroll, new LinearLayout.LayoutParams(-1, dp(57)));
        gap(body, 8);
    }

    private void toggleActiveTarget(String choice) {
        List<String> focuses = activeTargets();
        if (focuses.contains(choice)) {
            focuses.remove(choice);
        } else if (focuses.size() >= 2) {
            toast("Choose up to two training areas for one workout.");
        } else {
            focuses.add(choice);
        }
        syncPrimaryTarget();
    }

    /** Weekly planning starts with a reusable pool; days are assigned independently afterwards. */
    private void toggleWeeklyTrainingPool(String choice) {
        if (selectedTargets.contains(choice)) {
            selectedTargets.remove(choice);
        } else if (selectedTargets.size() >= 2) {
            toast("Choose up to two training areas for this week.");
            return;
        } else {
            selectedTargets.add(choice);
        }
        if (!selectedTargets.isEmpty()) target = selectedTargets.get(0);
    }

    private String targetLabel(String value) {
        if ("Upper".equals(value)) return "Upper body";
        if ("Lower".equals(value)) return "Lower body";
        if ("Full Body".equals(value)) return "Full body";
        return value;
    }

    private LinearLayout targetTile(String choice, boolean selected) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setContentDescription((selected ? "Selected training area: " : "Choose training area: ") + targetLabel(choice));
        tile.setBackground(surface(Color.WHITE, 18, selected ? 2 : 1, selected ? mint : Color.rgb(220, 229, 239)));
        tile.setClipToOutline(true);
        elevatePlanSurface(tile, selected);
        FrameLayout imageArea = new FrameLayout(this);
        imageArea.setBackgroundColor(Color.rgb(248, 250, 253));
        int targetIndex = 0;
        for (int index = 0; index < WIZARD_TARGETS.length; index++) if (WIZARD_TARGETS[index].equals(choice)) targetIndex = index;
        // One consistent anatomy atlas: the selected muscle group is coral-red on the same
        // human figure, so the picture conveys the choice without relying on text alone.
        int column = targetIndex % 2;
        int row = targetIndex / 2;
        int left = column == 0 ? 0 : 512;
        int right = column == 0 ? 512 : 1024;
        int[] rowTop = {0, 307, 614, 921, 1228};
        int[] rowBottom = {307, 614, 921, 1228, 1536};
        ReferenceArtworkView artwork = referenceArtwork(R.drawable.workout_anatomy_highlights,
                left, rowTop[row], right, rowBottom[row]);
        imageArea.addView(artwork, new FrameLayout.LayoutParams(-1, -1));
        if (selected) {
            TextView check = heading("✓", hasTallPlanViewport() ? 14 : 12, navy);
            check.setGravity(Gravity.CENTER);
            check.setBackground(surface(mint, 99, 0, Color.TRANSPARENT));
            int checkSize = hasTallPlanViewport() ? 25 : 21;
            FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(checkSize), dp(checkSize), Gravity.RIGHT | Gravity.TOP);
            checkParams.topMargin = dp(hasTallPlanViewport() ? 7 : 4);
            checkParams.rightMargin = dp(hasTallPlanViewport() ? 7 : 4);
            imageArea.addView(check, checkParams);
        }
        tile.addView(imageArea, new LinearLayout.LayoutParams(-1, 0, 1));
        View accent = new View(this); accent.setBackgroundColor(selected ? mint : Color.rgb(235, 240, 246));
        tile.addView(accent, new LinearLayout.LayoutParams(-1, dp(selected ? 3 : 1)));
        TextView name = heading(targetLabel(choice), hasTallPlanViewport() ? 13 : 11, ink);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        tile.addView(name, new LinearLayout.LayoutParams(-1, dp(hasTallPlanViewport() ? 34 : 25)));
        return tile;
    }

    /** Step 3: only equipment choices. This keeps the requested Machine-assisted category distinct and discoverable. */
    /** Daily and Custom plans size the recommendation before choosing equipment. */
    private void showPlanDuration() {
        LinearLayout body = planPage();
        planTitle(body, planMode.toUpperCase(Locale.ROOT) + " PLAN · " + focusSummary(activeTargets()).toUpperCase(Locale.ROOT), "How long do you\nwant to train?", "We’ll recommend a balanced range of movements that fits this time without rushing recovery.");
        addWizardProgress(body, wizardStep("Time"));

        LinearLayout lengthCard = card();
        lengthCard.addView(heading("Workout length", 19, ink));
        lengthCard.addView(text("Choose a time frame. You can still select every movement yourself on the next screens.", 13, muted));
        gap(lengthCard, 10);
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.HORIZONTAL);
        int[] minutes = {45, 60, 110};
        String[] labels = {"45 min", "60 min", "1 hr 50 min"};
        for (int i = 0; i < minutes.length; i++) {
            final int value = minutes[i];
            Button option = !customDuration && sessionMinutes == value ? action(labels[i], mint, navy) : action(labels[i], Color.rgb(232, 236, 244), ink);
            option.setTextSize(10);
            option.setOnClickListener(view -> { sessionMinutes = value; customDuration = false; resetExerciseSelection(); refreshPlanKeepingScroll(this::showPlanDuration); });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1);
            params.rightMargin = dp(i == minutes.length - 1 ? 0 : 5);
            options.addView(option, params);
        }
        lengthCard.addView(options);
        Button custom = customDuration ? action("Custom · " + sessionMinutes + " min", mint, navy) : action("Custom", navyPanel, mint);
        custom.setTextSize(12);
        LinearLayout.LayoutParams customParams = new LinearLayout.LayoutParams(-1, dp(46));
        customParams.topMargin = dp(7);
        custom.setOnClickListener(view -> {
            customDuration = true;
            if (sessionMinutes == 45 || sessionMinutes == 60 || sessionMinutes == 110) sessionMinutes = 75;
            refreshPlanKeepingScroll(this::showPlanDuration);
        });
        lengthCard.addView(custom, customParams);
        if (customDuration) addDurationStepper(lengthCard);
        body.addView(lengthCard);
        gap(body, 10);

        LinearLayout capacity = card();
        capacity.setPadding(dp(14), dp(12), dp(14), dp(12));
        capacity.addView(heading("Up to " + Coach.durationSummary(sessionMinutes), 16, ink));
        capacity.addView(text("Your exercise page starts with a time-appropriate shortlist. Add or remove movements freely.", 13, muted));
        body.addView(capacity);
        gap(body, 12);
        Button next = planPrimary("Next: choose equipment");
        next.setOnClickListener(view -> showPlanEquipment());
        addPlanButton(body, next);
        Button back = planBack();
        back.setOnClickListener(view -> showPlanTarget());
        addPlanBack(body, back);
    }

    private void addDurationStepper(LinearLayout container) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(heading("Custom minutes", 15, ink), new LinearLayout.LayoutParams(0, dp(44), 1));
        Button minus = compactControl("−");
        minus.setContentDescription("Decrease custom workout length by five minutes");
        minus.setOnClickListener(view -> { sessionMinutes = Math.max(20, sessionMinutes - 5); resetExerciseSelection(); refreshPlanKeepingScroll(this::showPlanDuration); });
        row.addView(minus, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView minutes = heading(sessionMinutes + " min", 14, ink);
        minutes.setGravity(Gravity.CENTER);
        row.addView(minutes, new LinearLayout.LayoutParams(dp(76), dp(42)));
        Button plus = compactControl("+");
        plus.setContentDescription("Increase custom workout length by five minutes");
        plus.setOnClickListener(view -> { sessionMinutes = Math.min(180, sessionMinutes + 5); resetExerciseSelection(); refreshPlanKeepingScroll(this::showPlanDuration); });
        row.addView(plus, new LinearLayout.LayoutParams(dp(42), dp(42)));
        container.addView(row);
    }

    /** Coach Hero Suite equipment step. */
    private void showPlanEquipment() {
        initialisePlanSelections();
        beginNewPage(); setSystemBars();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setBackgroundColor(navy);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(navy);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2)); setCoachHeroWizardShell(scroll, "Workouts");

        FrameLayout hero = coachHeroBackdrop("Workouts equipment"); root.addView(hero, new LinearLayout.LayoutParams(-1, dp(260)));
        LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = heading("PLAN YOUR WORKOUT · STEP 4", 10, mint); eyebrow.setLetterSpacing(.14f); copy.addView(eyebrow);
        TextView title = heading("Choose your\nequipment", 29, Color.WHITE); title.setLineSpacing(0, .93f); copy.addView(title);
        TextView subtitle = text("Select one or two categories. We’ll shape movements around your training area.", 13, Color.rgb(226, 236, 247));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(dp(258), -2); subtitleParams.topMargin = dp(8); copy.addView(subtitle, subtitleParams);
        FrameLayout.LayoutParams copyParams = new FrameLayout.LayoutParams(dp(274), -2, Gravity.LEFT | Gravity.TOP); copyParams.leftMargin = dp(20); copyParams.topMargin = dp(82); hero.addView(copy, copyParams);
        addWorkoutWizardProgress(hero, "Equipment");

        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(16), dp(20), dp(16), dp(36)); content.setBackgroundColor(paper); root.addView(content, new LinearLayout.LayoutParams(-1, -2));
        TextView section = heading("04  ·  EQUIPMENT", 11, muted); section.setLetterSpacing(.12f); content.addView(section);
        content.addView(heading("Train with what you have", 23, ink));
        content.addView(text("Pick up to two categories. Every exercise on the next page will fit your selection.", 13, muted));
        gap(content, 14);
        LinearLayout guidance = new LinearLayout(this); guidance.setGravity(Gravity.CENTER_VERTICAL); guidance.setPadding(dp(14), dp(11), dp(14), dp(11)); guidance.setBackground(surface(Color.rgb(11, 48, 67), 16, 1, Color.rgb(45, 90, 102)));
        TextView guidanceMark = heading("✦", 19, mint); guidanceMark.setGravity(Gravity.CENTER); guidance.addView(guidanceMark, new LinearLayout.LayoutParams(dp(30), dp(34)));
        LinearLayout guidanceCopy = new LinearLayout(this); guidanceCopy.setOrientation(LinearLayout.VERTICAL); guidanceCopy.addView(heading("BUILD YOUR EQUIPMENT MIX", 10, mint)); guidanceCopy.addView(text("One category is enough. Two gives your plan more variety.", 11, Color.rgb(218, 233, 241))); guidance.addView(guidanceCopy, new LinearLayout.LayoutParams(0, dp(38), 1));
        content.addView(guidance); gap(content, 12);
        for (int index = 0; index < Coach.EQUIPMENT_TYPES.length; index += 2) {
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            for (int offset = 0; offset < 2; offset++) {
                String equipment = Coach.EQUIPMENT_TYPES[index + offset];
                LinearLayout tile = equipmentTile(equipment, selectedEquipmentTypes.contains(equipment));
                tile.setOnClickListener(view -> {
                    if (selectedEquipmentTypes.contains(equipment)) { if (selectedEquipmentTypes.size() == 1) { toast("Keep at least one equipment category selected."); return; } selectedEquipmentTypes.remove(equipment); }
                    else if (selectedEquipmentTypes.size() >= 2) { toast("Choose up to two equipment categories per workout."); return; }
                    else selectedEquipmentTypes.add(equipment);
                    equipmentType = selectedEquipmentTypes.get(0); session.clear(); resetExerciseSelection(); refreshCurrentPageKeepingScroll(this::showPlanEquipment);
                });
                LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, dp(192), 1); if (offset == 0) tileParams.rightMargin = dp(9); row.addView(tile, tileParams);
            }
            content.addView(row); gap(content, 9);
        }
        LinearLayout selected = new LinearLayout(this); selected.setGravity(Gravity.CENTER_VERTICAL); selected.setPadding(dp(14), dp(12), dp(14), dp(12)); selected.setBackground(surface(Color.rgb(10, 43, 60), 18, 1, Color.rgb(51, 128, 138)));
        TextView selectionCount = heading(String.valueOf(selectedEquipmentTypes.size()), 24, navy); selectionCount.setGravity(Gravity.CENTER); selectionCount.setBackground(surface(mint, 99, 0, Color.TRANSPARENT)); selected.addView(selectionCount, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout selectedCopy = new LinearLayout(this); selectedCopy.setOrientation(LinearLayout.VERTICAL); selectedCopy.addView(heading("YOUR EQUIPMENT MIX", 10, mint)); selectedCopy.addView(text(equipmentSummary(), 15, Color.WHITE)); LinearLayout.LayoutParams selectedCopyParams = new LinearLayout.LayoutParams(0, dp(48), 1); selectedCopyParams.leftMargin = dp(11); selected.addView(selectedCopy, selectedCopyParams);
        TextView limit = text("of 2", 11, Color.rgb(177, 203, 216)); limit.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL); selected.addView(limit, new LinearLayout.LayoutParams(dp(36), dp(44)));
        content.addView(selected);
        if (selectedEquipmentTypes.contains("Machine-assisted")) {
            gap(content, 10); LinearLayout machines = new LinearLayout(this); machines.setOrientation(LinearLayout.VERTICAL); machines.setPadding(dp(14), dp(12), dp(14), dp(12)); machines.setBackground(surface(Color.rgb(11, 48, 67), 16, 1, Color.rgb(45, 90, 102)));
            machines.addView(heading("MACHINE-ASSISTED OPTIONS", 10, mint)); machines.addView(text(machineOptionsSummary(), 11, Color.rgb(221, 236, 239))); content.addView(machines);
        }
        gap(content, 15); Button next = action("Next: choose exercises  →", coral, Color.WHITE); next.setOnClickListener(view -> showExerciseSelection()); content.addView(next, new LinearLayout.LayoutParams(-1, dp(54)));
        Button back = new Button(this); back.setText("Back"); back.setTextColor(mint); back.setTextSize(14); back.setAllCaps(false); back.setBackgroundColor(Color.TRANSPARENT); back.setOnClickListener(view -> { if ("Weekly".equals(planMode) || "Daily".equals(planMode)) showPlanSchedule(); else showPlanDuration(); }); content.addView(back, new LinearLayout.LayoutParams(-1, dp(46)));
    }

    /** Retained until the legacy wizard shell is removed. */
    private void showPlanEquipmentLegacy() {
        initialisePlanSelections();
        LinearLayout body = planPage();
        planTitle(body, "PLAN YOUR WORKOUT", "Choose your\nequipment", "Select one or two categories. We’ll combine compatible " + focusSummary(activeTargets()).toLowerCase(Locale.ROOT) + " movements.");
        addWizardProgress(body, wizardStep("Equipment"));
        for (int index = 0; index < Coach.EQUIPMENT_TYPES.length; index += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int offset = 0; offset < 2; offset++) {
                String equipment = Coach.EQUIPMENT_TYPES[index + offset];
                LinearLayout tile = equipmentTile(equipment, selectedEquipmentTypes.contains(equipment));
                tile.setOnClickListener(view -> {
                    if (selectedEquipmentTypes.contains(equipment)) {
                        if (selectedEquipmentTypes.size() == 1) {
                            toast("Keep at least one equipment category selected.");
                            return;
                        }
                        selectedEquipmentTypes.remove(equipment);
                    } else if (selectedEquipmentTypes.size() >= 2) {
                        toast("Choose up to two equipment categories per workout.");
                        return;
                    } else {
                        selectedEquipmentTypes.add(equipment);
                    }
                    equipmentType = selectedEquipmentTypes.get(0);
                    session.clear();
                    resetExerciseSelection();
                    refreshPlanKeepingScroll(this::showPlanEquipment);
                });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(planEquipmentTileHeight()), 1);
                params.rightMargin = offset == 0 ? dp(planGridGap()) : 0;
                row.addView(tile, params);
            }
            body.addView(row);
            gap(body, planGridGap());
        }
        LinearLayout selection = card();
        selection.setPadding(dp(13), dp(10), dp(13), dp(10));
        selection.addView(heading(selectedEquipmentTypes.size() + " of 2 selected", 13, ink));
        selection.addView(text(equipmentSummary(), 12, muted));
        body.addView(selection);
        gap(body, 9);
        if (selectedEquipmentTypes.contains("Machine-assisted")) {
            LinearLayout supported = new LinearLayout(this);
            supported.setOrientation(LinearLayout.VERTICAL);
            supported.setPadding(dp(14), dp(11), dp(14), dp(11));
            supported.setBackground(surface(navyPanel, 15, 1, Color.rgb(45, 90, 102)));
            supported.setElevation(dp(2));
            supported.addView(heading("Machine-assisted options", hasTallPlanViewport() ? 13 : 11, mint));
            supported.addView(text(machineOptionsSummary(), hasTallPlanViewport() ? 10 : 8, Color.rgb(221, 236, 239)));
            body.addView(supported);
            gap(body, hasTallPlanViewport() ? 12 : 8);
        }
        Button next = planPrimary("Next: choose exercises");
        next.setOnClickListener(view -> showExerciseSelection());
        addPlanButton(body, next);
        Button back = planBack();
        back.setOnClickListener(view -> {
            if ("Daily".equals(planMode) || "Custom".equals(planMode)) showPlanDuration();
            else showPlanTarget();
        });
        addPlanBack(body, back);
    }

    private String machineOptionsSummary() {
        if ("Back".equals(target)) return "Lat pulldown · Cable row · High row · Assisted pull-up";
        if ("Shoulders".equals(target)) return "Shoulder press · Cable raise · Reverse pec deck · Face pull";
        if ("Core".equals(target)) return "Pallof press · Cable chop · Rotary torso · Captain’s chair";
        if ("Endurance".equals(target)) return "Treadmill · Bike · Rower · Elliptical";
        if ("Chest".equals(target)) return "Chest press · Cable press · Pec deck · Assisted dip";
        if ("Legs".equals(target) || "Lower".equals(target)) return "Leg press · Leg curl · Leg extension · Calf raise";
        if ("Arms".equals(target)) return "Cable curl · Pressdown · Assisted dip · Cable row";
        if ("Upper".equals(target)) return "Lat pulldown · Cable row · Shoulder press · Reverse fly";
        return "Chest press · Lat pulldown · Leg press · Seated cable row";
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
        else if ("At home".equals(label)) copy = "Household items and bands";
        else if ("Machine-assisted".equals(label)) copy = "Guided gym resistance";
        else copy = "No equipment";
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setContentDescription((selected ? "Selected equipment: " : "Choose equipment: ") + label + ". " + copy);
        tile.setBackground(surface(paper, 16, selected ? 3 : 1, selected ? mint : cardLine));
        // The artwork used to sit flush against the outer surface and paint over
        // the left/top/right portions of the selected border. Keep a precise
        // inset so the outline can visibly wrap the entire rounded card.
        int borderInset = dp(selected ? 4 : 1);
        tile.setPadding(borderInset, borderInset, borderInset, borderInset);
        tile.setClipToOutline(true);
        elevatePlanSurface(tile, selected);
        FrameLayout imageArea = new FrameLayout(this);
        // Clip the source artwork to a rounded inner panel. This preserves the
        // outer mint selection frame at every corner instead of letting the
        // square white image visually flatten the top of the card.
        imageArea.setBackground(surface(Color.WHITE, 12, 0, Color.TRANSPARENT));
        imageArea.setClipToOutline(true);
        int equipmentIndex = 0;
        for (int index = 0; index < Coach.EQUIPMENT_TYPES.length; index++) if (Coach.EQUIPMENT_TYPES[index].equals(label)) equipmentIndex = index;
        int column = equipmentIndex % 2;
        int row = equipmentIndex / 2;
        int left = column == 0 ? 2 : 629;
        int right = column == 0 ? 625 : 1252;
        int top = row == 0 ? 2 : 629;
        int bottom = row == 0 ? 625 : 1252;
        ReferenceArtworkView artwork = referenceArtwork(R.drawable.equipment_tiles, left, top, right, bottom);
        imageArea.addView(artwork, new FrameLayout.LayoutParams(-1, -1));
        if (selected) {
            TextView check = heading("✓", hasTallPlanViewport() ? 14 : 12, navy);
            check.setGravity(Gravity.CENTER);
            check.setBackground(surface(mint, 99, 0, Color.TRANSPARENT));
            int checkSize = hasTallPlanViewport() ? 25 : 21;
            FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(checkSize), dp(checkSize), Gravity.RIGHT | Gravity.TOP);
            checkParams.topMargin = dp(hasTallPlanViewport() ? 7 : 4);
            checkParams.rightMargin = dp(hasTallPlanViewport() ? 7 : 4);
            imageArea.addView(check, checkParams);
        }
        tile.addView(imageArea, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView name = heading(label, hasTallPlanViewport() ? 13 : 11, ink);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        tile.addView(name, new LinearLayout.LayoutParams(-1, dp(hasTallPlanViewport() ? 26 : 19)));
        TextView detail = text(copy, hasTallPlanViewport() ? 10 : 9, muted);
        detail.setGravity(Gravity.CENTER);
        detail.setMaxLines(2);
        detail.setLineSpacing(0f, 0.95f);
        tile.addView(detail, new LinearLayout.LayoutParams(-1, dp(hasTallPlanViewport() ? 32 : 24)));
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
        restoreEquipmentTypes(saved.equipment);
        sessionMinutes = saved.minutes;
        workingWeightKg = saved.weightKg;
        planSets = saved.sets;
        planReps = saved.reps;
        planRestSeconds = WorkoutTiming.clampRestSeconds(saved.restSeconds);
        planCooldownSeconds = WorkoutTiming.normalizeCooldownSeconds(saved.cooldownSeconds);
        planReminderEnabled = saved.reminderEnabled;
        planReminderHour = saved.reminderHour;
        planReminderMinute = saved.reminderMinute;
        weeklyWorkoutDays = decodeWeeklyDays(saved.weeklyDays);
        selectedWeekOffset = Math.max(0, Math.min(maxSelectableWeekOffset(), saved.weekOffset));
        dailyScheduledDay = Math.max(0, Math.min(6, saved.scheduledDay));
        activeScheduleDay = dailyScheduledDay;
        selectedTargets.clear();
        selectedTargets.addAll(decodeFocusTargets(saved.focusTargets, saved.target));
        restoreWeeklyTargets(saved.weeklyTargets, saved.target);
        customDuration = sessionMinutes != 45 && sessionMinutes != 60 && sessionMinutes != 110;
        syncTrainingDays();
        syncPrimaryTarget();
        if ("Weekly".equals(planMode)) {
            restoreWeeklySessions(saved.weeklyExerciseNames);
            if (!weeklyWorkoutDays[activeScheduleDay]) {
                for (int i = 0; i < weeklyWorkoutDays.length; i++) if (weeklyWorkoutDays[i]) { activeScheduleDay = i; break; }
            }
            session = new ArrayList<>(weeklySessions.get(activeScheduleDay));
            persistCurrentWeekSchedule();
        } else {
            session = prescribeNames(saved.exerciseNames, Coach.planOptions(activeTargets(), selectedEquipmentTypes));
        }
    }

    private List<String> decodeFocusTargets(String encoded, String fallback) {
        ArrayList<String> focuses = new ArrayList<>();
        if (encoded != null && !encoded.isEmpty()) {
            for (String value : encoded.split("\\|", -1)) {
                if (!value.isEmpty() && focuses.size() < 2) focuses.add(value);
            }
        }
        if (focuses.isEmpty()) focuses.add(fallback == null || fallback.isEmpty() ? "Full Body" : fallback);
        return focuses;
    }

    private void restoreWeeklyTargets(String encoded, String fallback) {
        initialisePlanSelections();
        String[] days = encoded == null ? new String[0] : encoded.split(";", -1);
        for (int i = 0; i < weeklyDayTargets.size(); i++) {
            weeklyDayTargets.get(i).clear();
            if (i < days.length) weeklyDayTargets.get(i).addAll(decodeFocusTargets(days[i], fallback));
            else if (weeklyWorkoutDays[i]) weeklyDayTargets.get(i).add(fallback);
        }
    }

    private String encodeFocusTargets(List<String> focuses) {
        return focuses == null ? "" : String.join("|", focuses);
    }

    private String encodeWeeklyTargets() {
        ArrayList<String> encoded = new ArrayList<>();
        for (List<String> focuses : weeklyDayTargets) encoded.add(encodeFocusTargets(focuses));
        return String.join(";", encoded);
    }

    private String encodeWeeklySessionNames() {
        ArrayList<String> days = new ArrayList<>();
        for (List<Coach.Exercise> exercises : weeklySessions) {
            ArrayList<String> names = new ArrayList<>();
            for (Coach.Exercise exercise : exercises) names.add(exercise.name);
            days.add(String.join("|", names));
        }
        return String.join(";", days);
    }

    private List<Coach.Exercise> prescribeNames(List<String> names, List<Coach.Exercise> choices) {
        ArrayList<Coach.Exercise> restored = new ArrayList<>();
        if (names == null) return restored;
        for (String name : names) {
            for (Coach.Exercise exercise : choices) {
                if (exercise.name.equals(name)) {
                    restored.add(Coach.prescribe(exercise, planSets, planReps, planRestSeconds));
                    break;
                }
            }
        }
        return restored;
    }

    private void restoreWeeklySessions(String encoded) {
        initialisePlanSelections();
        String[] days = encoded == null ? new String[0] : encoded.split(";", -1);
        for (int i = 0; i < weeklySessions.size(); i++) {
            weeklySessions.get(i).clear();
            if (!weeklyWorkoutDays[i]) continue;
            List<Coach.Exercise> choices = Coach.planOptions(weeklyDayTargets.get(i), selectedEquipmentTypes);
            List<String> names = i < days.length && !days[i].isEmpty()
                    ? java.util.Arrays.asList(days[i].split("\\|", -1)) : Collections.emptyList();
            List<Coach.Exercise> restored = prescribeNames(names, choices);
            if (restored.isEmpty()) {
                for (Coach.Exercise exercise : Coach.recommendedForDuration(choices, sessionMinutes)) {
                    restored.add(Coach.prescribe(exercise, planSets, planReps, planRestSeconds));
                }
            }
            weeklySessions.get(i).addAll(restored);
        }
    }

    private String normalizeEquipment(String savedEquipment) {
        if ("Home & household".equals(savedEquipment)) return "At home";
        if ("Machine assisted".equals(savedEquipment)) return "Machine-assisted";
        return savedEquipment;
    }

    private boolean[] decodeWeeklyDays(String encoded) {
        boolean[] days = {true, false, true, false, true, false, false};
        if (encoded == null) return days;
        String[] values = encoded.split(",", -1);
        if (values.length != 7) return days;
        for (int i = 0; i < values.length; i++) days[i] = "1".equals(values[i]);
        return days;
    }

    private void syncTrainingDays() {
        int total = 0;
        for (boolean selected : weeklyWorkoutDays) if (selected) total++;
        trainingDays = total;
    }

    private void prepareExerciseSelection() {
        String key = String.join("|", activeTargets()) + "|" + encodeEquipmentTypes() + "|" + sessionMinutes;
        if (key.equals(selectionKey) && !availableExercises.isEmpty()) return;
        selectionKey = key;
        availableExercises = Coach.planOptions(activeTargets(), selectedEquipmentTypes);
        selectedExercises = Coach.recommendedForDuration(availableExercises, sessionMinutes);
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

    /** Coach Hero Suite exercise-selection step. */
    private void showExerciseSelection() {
        prepareExerciseSelection();
        beginNewPage(); setSystemBars();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setBackgroundColor(navy);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(navy);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2)); setCoachHeroWizardShell(scroll, "Workouts");

        FrameLayout hero = coachHeroBackdrop("Workouts exercises"); root.addView(hero, new LinearLayout.LayoutParams(-1, dp(266)));
        LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = heading(planMode.toUpperCase(Locale.ROOT) + " PLAN · STEP 5", 10, mint); eyebrow.setLetterSpacing(.14f); copy.addView(eyebrow);
        TextView title = heading("Choose your\nexercises", 29, Color.WHITE); title.setLineSpacing(0, .93f); copy.addView(title);
        TextView subtitle = text(sessionMinutes + " minutes, made for " + focusSummary(activeTargets()).toLowerCase(Locale.ROOT) + ". Tap to select; press and hold a card for movement details.", 13, Color.rgb(226, 236, 247));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(dp(268), -2); subtitleParams.topMargin = dp(8); copy.addView(subtitle, subtitleParams);
        FrameLayout.LayoutParams copyParams = new FrameLayout.LayoutParams(dp(282), -2, Gravity.LEFT | Gravity.TOP); copyParams.leftMargin = dp(20); copyParams.topMargin = dp(82); hero.addView(copy, copyParams);
        addWorkoutWizardProgress(hero, "Exercises");

        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(16), dp(20), dp(16), dp(36)); content.setBackgroundColor(paper); root.addView(content, new LinearLayout.LayoutParams(-1, -2));
        TextView section = heading("05  ·  EXERCISES", 11, muted); section.setLetterSpacing(.12f); content.addView(section);
        content.addView(heading("Make the session your own", 23, ink));
        content.addView(text("Your recommended movements are selected. Keep them all, or tailor the session one card at a time.", 13, muted));
        gap(content, 14);
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER_VERTICAL); actions.setPadding(dp(12), dp(10), dp(12), dp(10)); actions.setBackground(surface(Color.rgb(11, 48, 67), 17, 1, Color.rgb(45, 90, 102)));
        Button all = action(selectedExercises.size() == availableExercises.size() ? "Clear all" : "Select all", Color.TRANSPARENT, mint); all.setTextSize(11); all.setMinHeight(0); all.setPadding(dp(6), 0, dp(6), 0); all.setBackground(surface(Color.TRANSPARENT, 99, 1, mint));
        all.setOnClickListener(view -> { if (selectedExercises.size() == availableExercises.size()) selectedExercises.clear(); else selectedExercises = new ArrayList<>(availableExercises); refreshCurrentPageKeepingScroll(this::showExerciseSelection); }); actions.addView(all, new LinearLayout.LayoutParams(dp(112), dp(40)));
        LinearLayout actionCopy = new LinearLayout(this); actionCopy.setOrientation(LinearLayout.VERTICAL); actionCopy.setGravity(Gravity.CENTER_VERTICAL); actionCopy.setPadding(dp(12), 0, 0, 0); actionCopy.addView(heading(selectedExercises.size() + " SELECTED", 10, mint)); actionCopy.addView(text("Hold any card for its demo", 10, Color.rgb(205, 224, 235))); actions.addView(actionCopy, new LinearLayout.LayoutParams(0, dp(40), 1));
        content.addView(actions); gap(content, 11);
        for (int index = 0; index < availableExercises.size(); index++) {
            Coach.Exercise exercise = availableExercises.get(index); content.addView(premiumExerciseCard(exercise, index, isSelected(exercise)), new LinearLayout.LayoutParams(-1, dp(136))); gap(content, 10);
        }
        Button next = action("Next: set up workout  →", coral, Color.WHITE); next.setOnClickListener(view -> { if (selectedExercises.isEmpty()) toast("Select at least one exercise before continuing."); else showPlanSetup(); }); content.addView(next, new LinearLayout.LayoutParams(-1, dp(54)));
        Button back = new Button(this); back.setText("Back"); back.setTextColor(mint); back.setTextSize(14); back.setAllCaps(false); back.setBackgroundColor(Color.TRANSPARENT); back.setOnClickListener(view -> showPlanEquipment()); content.addView(back, new LinearLayout.LayoutParams(-1, dp(46)));
    }

    private LinearLayout premiumExerciseCard(Coach.Exercise exercise, int index, boolean selected) {
        LinearLayout card = new LinearLayout(this); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(8), dp(8), dp(8), dp(8)); card.setClickable(true); card.setFocusable(true);
        card.setContentDescription((selected ? "Selected " : "Not selected ") + exercise.name + ". Tap to change selection. Press and hold for details and animation.");
        card.setBackground(surface(Color.WHITE, 18, selected ? 2 : 1, selected ? mint : Color.rgb(220, 229, 239))); elevatePlanSurface(card, selected);
        card.setOnClickListener(view -> { toggleExercise(exercise); refreshCurrentPageKeepingScroll(this::showExerciseSelection); });
        card.setOnLongClickListener(view -> { showExercisePreviewDialog(exercise); return true; });
        FrameLayout posterFrame = new FrameLayout(this); posterFrame.setBackground(surface(Color.rgb(246, 249, 252), 14, 1, Color.rgb(220, 229, 239))); posterFrame.setClipToOutline(true);
        posterFrame.addView(exerciseSelectionArtwork(exercise, index), new FrameLayout.LayoutParams(-1, -1)); card.addView(posterFrame, new LinearLayout.LayoutParams(dp(112), dp(118)));
        LinearLayout details = new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL); details.setPadding(dp(12), dp(3), dp(5), dp(3));
        details.addView(heading(exercise.name, 16, ink)); TextView area = text(exercise.target.toUpperCase(Locale.ROOT) + "  ·  " + exercise.description, 11, muted); area.setMaxLines(2); details.addView(area);
        TextView cue = text(exercise.cue, 11, Color.rgb(58, 111, 106)); cue.setMaxLines(2); LinearLayout.LayoutParams cueParams = new LinearLayout.LayoutParams(-1, 0, 1); cueParams.topMargin = dp(4); details.addView(cue, cueParams); card.addView(details, new LinearLayout.LayoutParams(0, dp(118), 1));
        TextView state = heading(selected ? "✓" : "+", 18, selected ? navy : Color.rgb(74, 105, 137)); state.setGravity(Gravity.CENTER); state.setBackground(surface(selected ? mint : Color.rgb(242, 246, 250), 99, selected ? 0 : 1, selected ? Color.TRANSPARENT : Color.rgb(164, 184, 205))); card.addView(state, new LinearLayout.LayoutParams(dp(36), dp(36)));
        return card;
    }

    /** Retained until the legacy wizard shell is removed. */
    private void showExerciseSelectionLegacy() {
        prepareExerciseSelection();
        LinearLayout body = planPage();
        planTitle(body, planMode.toUpperCase(Locale.ROOT) + " PLAN · " + focusSummary(activeTargets()).toUpperCase(Locale.ROOT), "Choose your\nexercises", "Your " + sessionMinutes + " minute recommendation is preselected. Add or remove movements to make it your own.");
        addWizardProgress(body, wizardStep("Exercises"));

        LinearLayout selectionBar = new LinearLayout(this);
        selectionBar.setOrientation(LinearLayout.HORIZONTAL);
        selectionBar.setGravity(Gravity.CENTER_VERTICAL);
        Button all = new Button(this);
        all.setText(selectedExercises.size() == availableExercises.size() ? "Clear all" : "Select all");
        all.setContentDescription(all.getText());
        all.setAllCaps(false);
        all.setTextSize(hasTallPlanViewport() ? 12 : 10);
        all.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        all.setTextColor(mint);
        all.setPadding(dp(7), 0, dp(7), 0);
        all.setMinHeight(0);
        all.setBackground(surface(navy, 99, 1, mint));
        all.setOnClickListener(view -> {
            if (selectedExercises.size() == availableExercises.size()) selectedExercises.clear();
            else selectedExercises = new ArrayList<>(availableExercises);
            refreshPlanKeepingScroll(this::showExerciseSelection);
        });
        selectionBar.addView(all, new LinearLayout.LayoutParams(dp(hasTallPlanViewport() ? 94 : 82), dp(hasTallPlanViewport() ? 36 : 27)));
        TextView selectionCount = heading(selectedExercises.size() + " selected", hasTallPlanViewport() ? 12 : 10, navy);
        selectionCount.setGravity(Gravity.CENTER);
        selectionCount.setPadding(dp(10), 0, dp(10), 0);
        selectionCount.setBackground(surface(mint, 99, 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(dp(hasTallPlanViewport() ? 84 : 68), dp(hasTallPlanViewport() ? 32 : 24));
        countParams.leftMargin = dp(0);
        countParams.weight = 1;
        selectionBar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        selectionBar.addView(selectionCount, countParams);
        body.addView(selectionBar);
        gap(body, hasTallPlanViewport() ? 12 : 7);

        for (int index = 0; index < availableExercises.size(); index++) {
            Coach.Exercise exercise = availableExercises.get(index);
            boolean selected = isSelected(exercise);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int rowPadding = hasTallPlanViewport() ? 8 : 5;
            row.setPadding(dp(rowPadding), dp(rowPadding), dp(rowPadding), dp(rowPadding));
            row.setBackground(surface(paper, 16, selected ? 2 : 1, selected ? mint : cardLine));
            row.setClipToOutline(true);
            elevatePlanSurface(row, selected);
            row.setClickable(true);
            row.setFocusable(true);
            row.setContentDescription((selected ? "Selected " : "Not selected ") + exercise.name + ". Tap to change selection. Press and hold for details and animation.");
            row.setOnClickListener(view -> {
                toggleExercise(exercise);
                refreshPlanKeepingScroll(this::showExerciseSelection);
            });
            row.setOnLongClickListener(view -> {
                showExercisePreviewDialog(exercise);
                return true;
            });
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            View poster = exerciseSelectionArtwork(exercise, index);
            int posterSize = planExercisePosterSize();
            LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(dp(posterSize), dp(posterSize));
            posterParams.rightMargin = dp(hasTallPlanViewport() ? 11 : 7);
            row.addView(poster, posterParams);
            details.addView(heading(exercise.name, hasTallPlanViewport() ? 13 : 11, ink));
            TextView area = text(exercise.target + "  ·  " + exercise.description, hasTallPlanViewport() ? 10 : 8, muted);
            area.setMaxLines(2);
            details.addView(area);
            TextView cue = text(exercise.cue, hasTallPlanViewport() ? 10 : 8, muted);
            cue.setMaxLines(2);
            details.addView(cue);
            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
            TextView check = heading(selected ? "✓" : "○", hasTallPlanViewport() ? 15 : 12, selected ? navy : muted);
            check.setGravity(Gravity.CENTER);
            check.setBackground(surface(selected ? mint : Color.TRANSPARENT, 99, selected ? 0 : 1, muted));
            int checkSize = hasTallPlanViewport() ? 29 : 22;
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(checkSize), dp(checkSize));
            checkParams.leftMargin = dp(hasTallPlanViewport() ? 6 : 3);
            row.addView(check, checkParams);
            body.addView(row);
            gap(body, planGridGap());
        }

        Button next = planPrimary("Next: set up workout");
        next.setOnClickListener(view -> {
            if (selectedExercises.isEmpty()) {
                toast("Select at least one exercise before continuing.");
            } else {
                showPlanSetup();
            }
        });
        addPlanButton(body, next);
        Button back = planBack();
        back.setOnClickListener(view -> showPlanEquipment());
        addPlanBack(body, back);
    }

    private View exerciseSelectionArtwork(Coach.Exercise exercise, int index) {
        return exercisePoster(AccountStore.get(this).trainer, exercise);
    }

    private void showExercisePreviewDialog(Coach.Exercise exercise) {
        Dialog dialog = new Dialog(this);
        LinearLayout popup = new LinearLayout(this);
        popup.setOrientation(LinearLayout.VERTICAL);
        popup.setPadding(dp(18), dp(18), dp(18), dp(18));
        popup.setBackground(surface(paper, 24, 1, cardLine));
        TextView eyebrow = heading("EXERCISE DETAILS", 11, coral);
        eyebrow.setLetterSpacing(.08f);
        popup.addView(eyebrow);
        gap(popup, 5);
        popup.addView(heading(exercise.name, 24, ink));
        popup.addView(text(exercise.target + "  •  " + equipmentSummary(), 13, muted));
        gap(popup, 12);
        popup.addView(exerciseMedia(profile().trainer, exercise, 220), new LinearLayout.LayoutParams(-1, dp(220)));
        gap(popup, 12);
        popup.addView(heading("What it works", 15, ink));
        popup.addView(text(exercise.description, 15, muted));
        gap(popup, 8);
        popup.addView(heading("Coach " + coachName() + "’s cue", 15, ink));
        popup.addView(text(exercise.cue, 15, muted));
        gap(popup, 14);
        Button close = primary("Close preview");
        close.setOnClickListener(view -> dialog.dismiss());
        addButton(popup, close);
        dialog.setContentView(popup);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * .88f), WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = dialog.getWindow().getAttributes();
            attributes.dimAmount = .68f;
            dialog.getWindow().setAttributes(attributes);
        }
    }

    /** Step 5: weekly days and working-set controls stay together before the irreversible build action. */
    private void showPlanSetup() {
        beginNewPage(); setSystemBars();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setBackgroundColor(navy);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(navy);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2)); setCoachHeroWizardShell(scroll, "Workouts");

        FrameLayout hero = coachHeroBackdrop("Workouts setup"); root.addView(hero, new LinearLayout.LayoutParams(-1, dp(260)));
        LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = heading(planMode.toUpperCase(Locale.ROOT) + " PLAN · STEP 6", 10, mint); eyebrow.setLetterSpacing(.14f); copy.addView(eyebrow);
        TextView title = heading("Set up your\nplan", 29, Color.WHITE); title.setLineSpacing(0, .93f); copy.addView(title);
        TextView subtitle = text("Tailor your schedule, working targets, recovery, and reminder before you build.", 13, Color.rgb(226, 236, 247));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(dp(270), -2); subtitleParams.topMargin = dp(8); copy.addView(subtitle, subtitleParams);
        FrameLayout.LayoutParams copyParams = new FrameLayout.LayoutParams(dp(274), -2, Gravity.LEFT | Gravity.TOP); copyParams.leftMargin = dp(20); copyParams.topMargin = dp(82); hero.addView(copy, copyParams);
        addWorkoutWizardProgress(hero, "Sets");

        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(16), dp(20), dp(16), dp(36)); body.setBackgroundColor(paper); root.addView(body, new LinearLayout.LayoutParams(-1, -2));
        TextView section = heading("06  ·  SET UP", 11, muted); section.setLetterSpacing(.12f); body.addView(section);
        body.addView(heading("Make this plan yours", 23, ink));
        body.addView(text("Choose comfortable targets now. You can fine-tune every setting later.", 13, muted));
        gap(body, 14);
        if ("Weekly".equals(planMode)) {
            LinearLayout schedule = card();
            schedule.addView(heading("Weekly schedule", 21, ink));
            schedule.addView(text(selectedWeekTitle() + " · " + trainingDays + " training days · " + (7 - trainingDays) + " rest days", 13, muted));
            gap(schedule, 10);
            LinearLayout dayRow = new LinearLayout(this);
            dayRow.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < weekDays.length; i++) {
                final int dayIndex = i;
                LinearLayout day = new LinearLayout(this);
                day.setOrientation(LinearLayout.VERTICAL);
                day.setGravity(Gravity.CENTER);
                day.setClickable(true);
                day.setFocusable(true);
                day.setContentDescription(weekDays[i] + (weeklyWorkoutDays[i] ? ", workout day" : ", rest day"));
                day.setBackground(surface(weeklyWorkoutDays[i] ? mint : Color.rgb(232, 236, 244), 11, 0, Color.TRANSPARENT));
                TextView label = heading(weekDays[i], 10, ink);
                label.setGravity(Gravity.CENTER);
                TextView state = heading(weeklyWorkoutDays[i] ? "✓" : "Rest", weeklyWorkoutDays[i] ? 20 : 9, weeklyWorkoutDays[i] ? navy : muted);
                state.setGravity(Gravity.CENTER);
                day.addView(label, new LinearLayout.LayoutParams(-1, dp(20)));
                day.addView(state, new LinearLayout.LayoutParams(-1, dp(29)));
                day.setOnClickListener(view -> {
                    weeklyWorkoutDays[dayIndex] = !weeklyWorkoutDays[dayIndex];
                    syncTrainingDays();
                    refreshPlanKeepingScroll(this::showPlanSetup);
                });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(51), 1);
                params.rightMargin = i == weekDays.length - 1 ? 0 : dp(3);
                dayRow.addView(day, params);
            }
            schedule.addView(dayRow);
            TextView summary = text(trainingDays + " sessions  •  " + (7 - trainingDays) + " rest days", 13, muted);
            LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
            summaryParams.topMargin = dp(10);
            schedule.addView(summary, summaryParams);
            body.addView(schedule);
            gap(body, 10);
        }

        LinearLayout settings = card();
        settings.addView(heading("Working sets", 21, ink));
        settings.addView(text("Choose comfortable starting targets. You can adjust them later.", 13, muted));
        gap(settings, 8);
        if (selectedEquipmentTypes.contains("Dumbbells") || selectedEquipmentTypes.contains("Machine-assisted")) {
            addWorkingStepper(settings, "Weight", workingWeightKg + " kg", () -> workingWeightKg = Math.max(0, workingWeightKg - 2), () -> workingWeightKg += 2);
        }
        addWorkingStepper(settings, "Sets", String.valueOf(planSets), () -> planSets = Math.max(1, planSets - 1), () -> planSets = Math.min(6, planSets + 1));
        addWorkingStepper(settings, "Reps", String.valueOf(planReps), () -> planReps = Math.max(1, planReps - 1), () -> planReps = Math.min(30, planReps + 1));
        LinearLayout rest = new LinearLayout(this);
        rest.setOrientation(LinearLayout.VERTICAL);
        LinearLayout restHeading = new LinearLayout(this);
        restHeading.setOrientation(LinearLayout.HORIZONTAL);
        restHeading.setGravity(Gravity.CENTER_VERTICAL);
        TextView restTitle = heading("Custom rest between sets", 15, ink);
        restHeading.addView(restTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView restValue = heading(restChoiceLabel(planRestSeconds), 15, navy);
        restValue.setGravity(Gravity.END);
        restHeading.addView(restValue, new LinearLayout.LayoutParams(dp(110), -2));
        rest.addView(restHeading);
        SeekBar restSlider = new SeekBar(this);
        int restSteps = (WorkoutTiming.MAX_REST_SECONDS - WorkoutTiming.MIN_REST_SECONDS) / WorkoutTiming.REST_STEP_SECONDS;
        restSlider.setMax(restSteps);
        restSlider.setKeyProgressIncrement(1);
        restSlider.setProgress((WorkoutTiming.clampRestSeconds(planRestSeconds) - WorkoutTiming.MIN_REST_SECONDS) / WorkoutTiming.REST_STEP_SECONDS);
        restSlider.setProgressTintList(ColorStateList.valueOf(mint));
        restSlider.setThumbTintList(ColorStateList.valueOf(coral));
        restSlider.setContentDescription("Custom rest between sets, " + restChoiceLabel(planRestSeconds));
        restSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                planRestSeconds = WorkoutTiming.MIN_REST_SECONDS + progress * WorkoutTiming.REST_STEP_SECONDS;
                restValue.setText(restChoiceLabel(planRestSeconds));
                seekBar.setContentDescription("Custom rest between sets, " + restChoiceLabel(planRestSeconds));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        rest.addView(restSlider, new LinearLayout.LayoutParams(-1, dp(44)));
        LinearLayout restRange = new LinearLayout(this);
        restRange.setOrientation(LinearLayout.HORIZONTAL);
        TextView minimum = text("45 sec", 12, muted);
        TextView maximum = text("8 min", 12, muted);
        maximum.setGravity(Gravity.END);
        restRange.addView(minimum, new LinearLayout.LayoutParams(0, -2, 1));
        restRange.addView(maximum, new LinearLayout.LayoutParams(0, -2, 1));
        rest.addView(restRange);
        settings.addView(rest);
        gap(settings, 14);
        LinearLayout cooldown = new LinearLayout(this);
        cooldown.setOrientation(LinearLayout.VERTICAL);
        cooldown.addView(heading("Cooldown between exercises", 15, ink));
        cooldown.addView(text("Choose a separate recovery preset before the next exercise begins.", 12, muted));
        gap(cooldown, 7);
        LinearLayout cooldownOptions = new LinearLayout(this);
        cooldownOptions.setOrientation(LinearLayout.HORIZONTAL);
        for (int seconds : WorkoutTiming.COOLDOWN_PRESET_SECONDS) {
            boolean selected = seconds == planCooldownSeconds;
            Button option = selected ? action(restChoiceLabel(seconds), mint, navy)
                    : action(restChoiceLabel(seconds), Color.rgb(232, 236, 244), ink);
            option.setTextSize(12);
            option.setContentDescription("Cooldown between exercises, " + restChoiceLabel(seconds) + (selected ? ", selected" : ""));
            option.setOnClickListener(view -> {
                planCooldownSeconds = seconds;
                refreshPlanKeepingScroll(this::showPlanSetup);
            });
            LinearLayout.LayoutParams optionParams = new LinearLayout.LayoutParams(0, dp(44), 1);
            optionParams.rightMargin = seconds == WorkoutTiming.COOLDOWN_PRESET_SECONDS[WorkoutTiming.COOLDOWN_PRESET_SECONDS.length - 1] ? 0 : dp(6);
            cooldownOptions.addView(option, optionParams);
        }
        cooldown.addView(cooldownOptions);
        settings.addView(cooldown);
        body.addView(settings);
        gap(body, 12);

        LinearLayout reminder = card();
        reminder.addView(heading("Coach workout reminder", 20, ink));
        reminder.addView(text("Set a local reminder for this plan. Coach " + coachName() + " will greet you full screen when it is time to train.", 13, muted));
        CheckBox reminderToggle = new CheckBox(this);
        reminderToggle.setText("Remind me for this plan");
        reminderToggle.setTextSize(15);
        reminderToggle.setTextColor(ink);
        reminderToggle.setButtonTintList(ColorStateList.valueOf(mint));
        reminderToggle.setChecked(planReminderEnabled);
        reminderToggle.setContentDescription("Coach workout reminder " + (planReminderEnabled ? "enabled" : "disabled"));
        reminderToggle.setOnCheckedChangeListener((button, checked) -> {
            planReminderEnabled = checked;
            if (checked) ensureNotificationPermission();
            refreshPlanKeepingScroll(this::showPlanSetup);
        });
        reminder.addView(reminderToggle);
        LinearLayout reminderControls = new LinearLayout(this);
        reminderControls.setOrientation(LinearLayout.HORIZONTAL);
        Button reminderTime = action("Time · " + WorkoutReminderScheduler.timeLabel(planReminderHour, planReminderMinute), planReminderEnabled ? mint : Color.rgb(232, 236, 244), ink);
        reminderTime.setTextSize(11);
        reminderTime.setEnabled(planReminderEnabled);
        reminderTime.setOnClickListener(view -> openReminderTimePicker());
        reminderControls.addView(reminderTime, new LinearLayout.LayoutParams(0, dp(44), 1));
        Button reminderPreview = action("Preview coach reminder", navyPanel, mint);
        reminderPreview.setTextSize(11);
        reminderPreview.setOnClickListener(view -> startActivity(new Intent(this, WorkoutReminderActivity.class)));
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        previewParams.leftMargin = dp(7);
        reminderControls.addView(reminderPreview, previewParams);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(-1, -2);
        controlsParams.topMargin = dp(7);
        reminder.addView(reminderControls, controlsParams);
        body.addView(reminder);
        gap(body, 12);
        Button build = action("Build my " + focusSummary(activeTargets()).toLowerCase(Locale.ROOT) + " " + planMode.toLowerCase(Locale.ROOT) + " plan  →", coral, Color.WHITE);
        build.setOnClickListener(view -> buildSelectedPlan());
        body.addView(build, new LinearLayout.LayoutParams(-1, dp(54)));
        Button back = new Button(this);
        back.setText("Back to exercise choices");
        back.setTextColor(mint);
        back.setTextSize(14);
        back.setAllCaps(false);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setContentDescription("Back to exercise choices");
        back.setOnClickListener(view -> showExerciseSelection());
        body.addView(back, new LinearLayout.LayoutParams(-1, dp(46)));
    }

    private void openReminderTimePicker() {
        new android.app.TimePickerDialog(this, (view, hour, minute) -> {
            planReminderHour = hour;
            planReminderMinute = minute;
            refreshPlanKeepingScroll(this::showPlanSetup);
        }, planReminderHour, planReminderMinute, false).show();
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 710);
        }
    }

    private void addWorkingStepper(LinearLayout container, String label, String value, Runnable decrease, Runnable increase) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = heading(label, 15, ink);
        row.addView(name, new LinearLayout.LayoutParams(0, dp(44), 1));
        Button minus = compactControl("−");
        minus.setContentDescription("Decrease " + label);
        minus.setOnClickListener(view -> {
            decrease.run();
            refreshPlanKeepingScroll(this::showPlanSetup);
        });
        row.addView(minus, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView number = heading(value, 15, ink);
        number.setGravity(Gravity.CENTER);
        row.addView(number, new LinearLayout.LayoutParams(dp(72), dp(42)));
        Button plus = compactControl("+");
        plus.setContentDescription("Increase " + label);
        plus.setOnClickListener(view -> {
            increase.run();
            refreshPlanKeepingScroll(this::showPlanSetup);
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
        if ("Weekly".equals(planMode) && trainingDays == 0) {
            toast("Choose at least one weekly workout day.");
            return;
        }
        if ("Weekly".equals(planMode)) {
            buildWeeklySessions();
            session = new ArrayList<>(weeklySessions.get(activeScheduleDay));
        } else {
            session = prescribeExercises(selectedExercises);
        }
        persistCurrentWeekSchedule();
        savePlanWithReminder();
        showPlanReady();
    }

    private void savePlanWithReminder() {
        PlanStore.save(this, planMode, target, encodeEquipmentTypes(), sessionMinutes, workingWeightKg, planSets, planReps, planRestSeconds, planCooldownSeconds, planReminderEnabled, planReminderHour, planReminderMinute, weeklyWorkoutDays, encodeFocusTargets(activeTargets()), encodeWeeklyTargets(), encodeWeeklySessionNames(), "Weekly".equals(planMode) ? activeScheduleDay : dailyScheduledDay, selectedWeekOffset, session);
        WorkoutReminderScheduler.schedule(this, PlanStore.load(this));
    }

    private List<Coach.Exercise> prescribeExercises(List<Coach.Exercise> choices) {
        ArrayList<Coach.Exercise> prescribed = new ArrayList<>();
        for (Coach.Exercise exercise : choices) prescribed.add(Coach.prescribe(exercise, planSets, planReps, planRestSeconds));
        return prescribed;
    }

    private void buildWeeklySessions() {
        initialisePlanSelections();
        for (int i = 0; i < weeklySessions.size(); i++) {
            weeklySessions.get(i).clear();
            if (!weeklyWorkoutDays[i]) continue;
            List<Coach.Exercise> choices;
            if (i == activeScheduleDay) choices = selectedExercises;
            else choices = Coach.recommendedForDuration(Coach.planOptions(weeklyDayTargets.get(i), selectedEquipmentTypes), sessionMinutes);
            weeklySessions.get(i).addAll(prescribeExercises(choices));
        }
    }

    private String planSchedule() {
        if ("Daily".equals(planMode)) return "scheduled for " + scheduledDateSummary(dailyScheduledDay);
        if ("Weekly".equals(planMode)) return trainingDays + " sessions in " + selectedWeekTitle();
        return "custom session";
    }

    private void showPlanReady() {
        LinearLayout body = planPage();
        planTitle(body, "PLAN READY", "Your " + focusSummary(activeTargets()).toLowerCase(Locale.ROOT) + " plan is ready", session.size() + " selected movements  •  " + sessionMinutes + " min  •  " + equipmentSummary());
        LinearLayout schedule = card();
        schedule.addView(heading("Weekly".equals(planMode) ? "Your weekly templates" : "Scheduled workout", 18, ink));
        schedule.addView(text("Ready when you are  •  " + planSchedule(), 14, muted));
        schedule.addView(text(planReminderEnabled ? "Coach reminder · " + WorkoutReminderScheduler.timeLabel(planReminderHour, planReminderMinute) : "No coach reminder set", 13, planReminderEnabled ? Color.rgb(50, 116, 103) : muted));
        body.addView(schedule);
        gap(body, 10);
        if ("Weekly".equals(planMode)) {
            addWeeklySessionPicker(body);
            gap(body, 10);
        }
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
            String load = (selectedEquipmentTypes.contains("Dumbbells") || selectedEquipmentTypes.contains("Machine-assisted")) ? "  •  " + workingWeightKg + " kg" : "";
            details.addView(text(exercise.sets + " sets × " + exercise.reps + " reps" + load + "  •  Set rest " + restChoiceLabel(exercise.restSeconds), 14, muted));
            item.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
            body.addView(item);
            gap(body, 8);
        }
        LinearLayout cue = card();
        cue.addView(heading("Coach " + coachName() + "'s focus", 18, ink));
        cue.addView(text("Move with control, choose a comfortable load, and stop if you feel pain.", 14, muted));
        gap(cue, 6);
        cue.addView(text(BodyProfileStore.workoutContext(this), 13, muted));
        gap(cue, 6);
        cue.addView(text("Between exercises: " + restChoiceLabel(planCooldownSeconds) + " cooldown.", 13, muted));
        body.addView(cue);
        gap(body, 12);
        LinearLayout nexus = card();
        nexus.addView(heading("Plan + fuel + progress", 17, ink));
        nexus.addView(text("This plan becomes a Progress completion when you finish. Nutrition uses " + sessionMinutes + " min and " + focusSummary(activeTargets()) + " to surface practical fuel ideas.", 14, muted));
        gap(nexus, 9);
        Button fuel = lightAction("Preview fuel for this plan");
        fuel.setOnClickListener(view -> showMeals());
        addButton(nexus, fuel);
        body.addView(nexus);
        gap(body, 12);
        Button start = planPrimary("Start guided workout");
        start.setOnClickListener(view -> showWorkoutCountdown());
        addPlanButton(body, start);
        gap(body, 10);
        Button scheduleLater = secondary("Schedule for later");
        scheduleLater.setOnClickListener(view -> {
            persistCurrentWeekSchedule();
            savePlanWithReminder();
            toast("Your " + planSchedule() + " is saved on this device.");
        });
        addButton(body, scheduleLater);
        gap(body, 10);
        Button edit = lightAction("Edit plan");
        edit.setOnClickListener(view -> showExerciseSelection());
        addButton(body, edit);
    }

    private void addWeeklySessionPicker(LinearLayout body) {
        LinearLayout picker = card();
        picker.setPadding(dp(12), dp(10), dp(12), dp(10));
        picker.addView(text("Open a day to review or start its saved workout.", 13, muted));
        for (int i = 0; i < weeklyWorkoutDays.length; i++) {
            if (!weeklyWorkoutDays[i]) continue;
            final int dayIndex = i;
            boolean active = dayIndex == activeScheduleDay;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(11), dp(8), dp(11), dp(8));
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackground(surface(active ? Color.rgb(226, 249, 243) : Color.rgb(240, 243, 248), 12, active ? 1 : 0, mint));
            row.setContentDescription(scheduleDayLabel(i) + ", " + focusSummary(weeklyDayTargets.get(i)) + ", " + weeklySessions.get(i).size() + " movements" + (active ? ", currently open" : ", open this day"));
            LinearLayout copy = new LinearLayout(this);
            copy.setOrientation(LinearLayout.VERTICAL);
            copy.addView(heading(scheduleDayLabel(i) + " · " + focusSummary(weeklyDayTargets.get(i)), 15, ink));
            copy.addView(text(weeklySessions.get(i).size() + " movements · " + sessionMinutes + " min", 12, muted));
            row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
            TextView open = heading(active ? "Open" : "View", 12, active ? navy : mint);
            open.setGravity(Gravity.CENTER);
            row.addView(open, new LinearLayout.LayoutParams(dp(48), dp(38)));
            row.setOnClickListener(view -> {
                activeScheduleDay = dayIndex;
                syncPrimaryTarget();
                session = new ArrayList<>(weeklySessions.get(dayIndex));
                showPlanReady();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.topMargin = dp(7);
            picker.addView(row, params);
        }
        body.addView(picker);
    }

    private void showProgress() {
        LinearLayout body = appPage("Progress");
        title(body, "PROGRESS NEXUS", "See the whole picture", "Your workout completions, active time, simple fuel check-ins, and private body trend now inform one next step.");
        JourneyStore.Snapshot snapshot = JourneyStore.snapshot(this);
        int planned = plannedSessionsThisWeek();

        LinearLayout history = card();
        history.addView(heading("This week’s completed plan history", 19, ink));
        List<JourneyStore.WorkoutRecord> completed = JourneyStore.workoutsThisWeek(this);
        if (completed.isEmpty()) {
            history.addView(text("No completed guided workouts yet this week. When you finish a plan, it appears here with its focus and guided time.", 14, muted));
        } else {
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
            for (JourneyStore.WorkoutRecord record : completed) {
                SetTimeStore.WorkoutSummary timing = SetTimeStore.loadWorkout(this, record.timestamp);
                EnduranceHistoryStore.Entry endurance = EnduranceHistoryStore.load(this, record.timestamp);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(9), dp(7), dp(9), dp(7));
                row.setBackground(surface(Color.rgb(240, 244, 249), 11, 0, Color.TRANSPARENT));
                TextView check = heading("✓", 18, navy);
                check.setGravity(Gravity.CENTER);
                check.setBackground(surface(mint, 99, 0, Color.TRANSPARENT));
                LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(30), dp(30));
                checkParams.rightMargin = dp(9);
                row.addView(check, checkParams);
                LinearLayout copy = new LinearLayout(this);
                copy.setOrientation(LinearLayout.VERTICAL);
                copy.addView(heading(record.target, 15, ink));
                copy.addView(text(dayFormat.format(record.timestamp) + "  •  " + record.minutes + " guided min", 12, muted));
                if (endurance != null) {
                    copy.addView(text(enduranceSummary(endurance) + "  •  view route", 12, mint));
                }
                if (timing != null && timing.totalActiveMillis > 0L) {
                    copy.addView(text("Set time " + WorkoutTiming.formatElapsedMillis(timing.totalActiveMillis)
                            + "  •  open leaderboard", 12, mint));
                }
                row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
                if (endurance != null) {
                    row.setClickable(true);
                    row.setFocusable(true);
                    row.setContentDescription(record.target + ", completed " + dayFormat.format(record.timestamp)
                            + ", " + enduranceSummary(endurance) + ". Open completed route.");
                    row.setOnClickListener(view -> showEnduranceSessionDetails(endurance));
                } else if (timing != null && timing.totalActiveMillis > 0L) {
                    row.setClickable(true);
                    row.setFocusable(true);
                    row.setContentDescription(record.target + ", completed " + dayFormat.format(record.timestamp)
                            + ", " + WorkoutTiming.formatElapsedMillis(timing.totalActiveMillis)
                            + " active set time. Open set-time leaderboard.");
                    row.setOnClickListener(view -> showWorkoutSetTimeDetails(record, timing));
                } else {
                    row.setContentDescription(record.target + ", completed " + dayFormat.format(record.timestamp)
                            + ", " + record.minutes + " guided minutes.");
                }
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
                rowParams.topMargin = dp(7);
                history.addView(row, rowParams);
            }
        }
        body.addView(history);
        gap(body, 12);

        Button allHistory = lightAction("View all workout history");
        allHistory.setOnClickListener(view -> showWorkoutHistory());
        addButton(body, allHistory);
        gap(body, 12);

        LinearLayout week = card();
        week.addView(heading("This week's rhythm", 19, ink));
        String completion = planned == 0 ? snapshot.workoutsThisWeek + " workouts completed"
                : snapshot.workoutsThisWeek + " of " + planned + " planned workouts completed";
        week.addView(text(completion, 15, muted));
        gap(week, 8);
        ProgressBar completionBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        completionBar.setMax(Math.max(1, planned));
        completionBar.setProgress(Math.min(snapshot.workoutsThisWeek, Math.max(1, planned)));
        completionBar.setProgressTintList(ColorStateList.valueOf(mint));
        completionBar.setProgressBackgroundTintList(ColorStateList.valueOf(cardLine));
        week.addView(completionBar, new LinearLayout.LayoutParams(-1, dp(9)));
        gap(week, 10);
        week.addView(text(snapshot.activeMinutesThisWeek + " guided minutes  •  " + snapshot.fuelDaysThisWeek + " fuel check-in days", 14, muted));
        if (!snapshot.lastWorkoutTarget.isEmpty()) week.addView(text("Last completed focus: " + snapshot.lastWorkoutTarget, 13, muted));
        body.addView(week);
        gap(body, 12);

        JourneyStore.MonthReport report = JourneyStore.monthReport(this);
        LinearLayout monthly = card();
        monthly.addView(heading("Monthly progression report", 19, ink));
        monthly.addView(text(report.completedTotal + " completed plans  •  " + report.minutesTotal + " guided min this month", 14, muted));
        gap(monthly, 8);
        addMonthlyCompletionChart(monthly, report);
        String insight = report.completedTotal == 0
                ? "Complete your first guided session to begin a private month-long progression record."
                : report.completedTotal == 1 ? "One plan complete—your next completed plan will make the weekly pattern easier to see."
                : "Your completed plans are building a more useful month-long consistency pattern.";
        monthly.addView(text(insight, 13, muted));
        body.addView(monthly);
        gap(body, 12);

        LinearLayout next = card();
        next.addView(heading("Your next best step", 19, ink));
        String nextStep;
        if (!BodyProfileStore.configured(this) || !WeightStore.configured(this)) {
            nextStep = "Complete your private age, height, and weight profile so trends have useful context.";
        } else if (session.isEmpty()) {
            nextStep = "Build a workout plan. Progress becomes useful after your first completed guided session.";
        } else if (snapshot.todayFuelGoals() < 3) {
            nextStep = "Your plan is ready. Check today's flexible fuel goals, then start when you feel ready.";
        } else {
            nextStep = "Your plan and fuel check-in are connected. Start your next session or review your trend.";
        }
        next.addView(text(nextStep, 15, muted));
        gap(next, 10);
        Button workout = primary(session.isEmpty() ? "Build my workout" : "Open my workout");
        workout.setOnClickListener(view -> { if (session.isEmpty()) showPlans(); else showPlanReady(); });
        addButton(next, workout);
        gap(next, 8);
        Button nutrition = lightAction("Open today's nutrition check-in");
        nutrition.setOnClickListener(view -> showMeals());
        addButton(next, nutrition);
        body.addView(next);
        gap(body, 12);

        LinearLayout profileCard = card();
        profileCard.addView(heading("Private body context", 18, ink));
        profileCard.addView(text(BodyProfileStore.summary(this), 14, muted));
        profileCard.addView(text("Current weight: " + WeightStore.latestLabel(this) + ". Measurements add context; they never diagnose health or choose a lifting load.", 13, muted));
        gap(profileCard, 10);
        Button editProfile = lightAction(BodyProfileStore.configured(this) ? "Open body context" : "Complete body profile");
        editProfile.setOnClickListener(view -> { if (BodyProfileStore.configured(this)) showBodyContext(); else showBodyProfileSetup(false); });
        addButton(profileCard, editProfile);
        body.addView(profileCard);
        gap(body, 12);

        LinearLayout trend = card();
        trend.addView(heading(WeightStore.configured(this) ? WeightStore.summary(this) : "Set a private starting point", 18, ink));
        trend.addView(text("Look for a multi-week direction alongside workout consistency and fueling—not a verdict from one number.", 14, muted));
        gap(trend, 10);
        Button weight = secondary("Weight trend check-in");
        weight.setOnClickListener(view -> showWeightTracker());
        addButton(trend, weight);
        body.addView(trend);
    }

    /** User-managed, local-only archive of completed workouts with a confirmation before deletion. */
    private void showWorkoutHistory() {
        LinearLayout body = appPage("Progress");
        title(body, "WORKOUT HISTORY", "Your completed sessions", "This archive lives only on this device. Removing an entry also removes its saved set-time details, but never changes your plan or schedule.");
        List<JourneyStore.WorkoutRecord> records = JourneyStore.workouts(this);
        if (records.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(heading("No completed workouts yet", 19, ink));
            empty.addView(text("Finish a guided workout or log an endurance session to see it here.", 14, muted));
            body.addView(empty);
        } else {
            SimpleDateFormat timestamp = new SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault());
            for (JourneyStore.WorkoutRecord record : records) {
                LinearLayout item = card();
                item.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL);
                copy.addView(heading(record.target, 17, ink));
                copy.addView(text(timestamp.format(record.timestamp) + "  •  " + record.minutes + " min", 13, muted));
                SetTimeStore.WorkoutSummary details = SetTimeStore.loadWorkout(this, record.timestamp);
                EnduranceHistoryStore.Entry endurance = EnduranceHistoryStore.load(this, record.timestamp);
                if (details != null && details.totalActiveMillis > 0L) copy.addView(text("Set time " + WorkoutTiming.formatElapsedMillis(details.totalActiveMillis), 12, mint));
                if (endurance != null) copy.addView(text(enduranceSummary(endurance) + "  •  private route saved", 12, mint));
                item.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
                if (endurance != null) {
                    item.setClickable(true);
                    item.setFocusable(true);
                    item.setContentDescription("Open " + endurance.mode + " route completed " + timestamp.format(record.timestamp));
                    item.setOnClickListener(view -> showEnduranceSessionDetails(endurance));
                }
                Button delete = action("Delete", paper, coral);
                delete.setTextSize(12);
                delete.setContentDescription("Delete " + record.target + " completed " + timestamp.format(record.timestamp));
                delete.setOnClickListener(view -> new AlertDialog.Builder(this)
                        .setTitle("Delete workout history?")
                        .setMessage("Remove " + record.target + " from your local progress history? This cannot be undone.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dialog, which) -> {
                            JourneyStore.removeWorkout(this, record.timestamp);
                            SetTimeStore.removeWorkout(this, record.timestamp);
                            EnduranceHistoryStore.remove(this, record.timestamp);
                            refreshCurrentPageKeepingScroll(this::showWorkoutHistory);
                        }).show());
                item.addView(delete, new LinearLayout.LayoutParams(dp(78), dp(42)));
                body.addView(item); gap(body, 9);
            }
        }
        Button back = secondary("Back to Progress"); back.setOnClickListener(view -> showProgress()); addButton(body, back);
    }

    /** Full local record for a finished walk, run, or ride; no third-party map or account is involved. */
    private void showEnduranceSessionDetails(EnduranceHistoryStore.Entry entry) {
        LinearLayout body = appPage("Progress");
        title(body, "COMPLETED ENDURANCE", entry.mode + " route", "A private, on-device record of this completed session.");
        LinearLayout overview = card();
        overview.addView(heading(enduranceSummary(entry), 22, ink));
        overview.addView(text(new SimpleDateFormat("EEEE, MMMM d · h:mm a", Locale.getDefault()).format(entry.timestamp), 14, muted));
        overview.addView(text(entry.points.isEmpty() ? "No route points were captured for this session." : entry.points.size() + " route points saved locally", 13, muted));
        body.addView(overview);
        gap(body, 12);
        if (!entry.points.isEmpty()) {
            LinearLayout route = card();
            route.addView(heading("Your route", 19, ink));
            if (OfflineMapStore.hasAnyMap(this)) {
                OfflineMapStore.MapRecord selectedMap = OfflineMapStore.selectedMap(this);
                route.addView(text("Offline " + (selectedMap == null ? "country" : selectedMap.country) + " map · your coral route stays private on this phone.", 13, muted));
                try {
                    OfflineRouteMapView map = new OfflineRouteMapView(this, OfflineMapStore.mapFile(this), entry.points);
                    LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(-1, dp(300));
                    mapParams.topMargin = dp(10);
                    route.addView(map, mapParams);
                } catch (RuntimeException error) { addPrivateRouteTrace(route, entry.points, dp(235)); }
                gap(route, 8);
                addTrinidadOfflineMapControl(route, () -> showEnduranceSessionDetails(entry));
            } else {
                route.addView(text("Choose a country map in Settings to place this private route on real streets while offline.", 13, muted));
                addPrivateRouteTrace(route, entry.points, dp(235));
                gap(route, 8);
                addTrinidadOfflineMapControl(route, () -> showEnduranceSessionDetails(entry));
            }
            body.addView(route);
            gap(body, 12);
        }
        Button back = secondary("Back to completed sessions");
        back.setOnClickListener(view -> showWorkoutHistory());
        addButton(body, back);
    }

    private String enduranceSummary(EnduranceHistoryStore.Entry entry) {
        String pace = entry.distanceMeters < 10f ? "pace —" : String.format(Locale.US, "%.1f min/km", (entry.activeMillis / 60000d) / (entry.distanceMeters / 1000d));
        return formatLiveDistance(entry.distanceMeters) + "  •  " + formatLiveDuration(entry.activeMillis) + "  •  " + pace;
    }

    private void addPrivateRouteTrace(LinearLayout parent, List<LiveTrackingStore.Point> points, int height) {
        RouteTraceView trace = new RouteTraceView(this); trace.setPoints(points);
        LinearLayout.LayoutParams traceParams = new LinearLayout.LayoutParams(-1, height); traceParams.topMargin = dp(10);
        parent.addView(trace, traceParams);
    }

    /** Offers one country map as an optional, integrity-checked local download. */
    private void addTrinidadOfflineMapControl(LinearLayout parent, Runnable refreshAfterChange) {
        if (OfflineMapStore.hasTrinidadAndTobago(this)) {
            parent.addView(text("Stored on this phone · " + Math.round(OfflineMapStore.trinidadAndTobagoBytes() / 1_000_000f) + " MB · © OpenStreetMap contributors", 12, mint));
            Button remove = secondary("Remove Trinidad & Tobago offline map");
            remove.setOnClickListener(view -> new AlertDialog.Builder(this).setTitle("Remove offline map?")
                    .setMessage("This removes only the downloaded street map. Your private completed routes remain on this phone.")
                    .setNegativeButton("Cancel", null).setPositiveButton("Remove", (dialog, which) -> { OfflineMapStore.removeTrinidadAndTobago(this); refreshAfterChange.run(); }).show());
            addButton(parent, remove);
            return;
        }
        TextView status = text("Map data © OpenStreetMap contributors, licensed under ODbL. Download once over Wi‑Fi; it works without mobile data afterwards.", 12, muted);
        parent.addView(status); gap(parent, 7);
        Button download = lightAction("Download Trinidad & Tobago map · 10 MB");
        download.setOnClickListener(view -> {
            download.setEnabled(false); status.setText("Preparing secure map download…");
            OfflineMapStore.downloadTrinidadAndTobago(this, new OfflineMapStore.Listener() {
                @Override public void onProgress(long downloaded, long total) { runOnUiThread(() -> status.setText("Downloading offline map · " + Math.min(100, Math.round(downloaded * 100f / Math.max(1L, total))) + "%")); }
                @Override public void onReady() { runOnUiThread(() -> { toast("Trinidad & Tobago is ready offline."); refreshAfterChange.run(); }); }
                @Override public void onError(String message) { runOnUiThread(() -> { status.setText(message); download.setEnabled(true); }); }
            });
        });
        addButton(parent, download);
    }

    /** Settings-owned country selector: managed downloads stay verified; other country packages are imported locally. */
    private void showOfflineMapCatalog() {
        LinearLayout body = appPage("Settings");
        title(body, "OFFLINE MAPS", "Country maps on your phone", "Download a country once, then view your private endurance route without mobile data.");
        LinearLayout notice = card();
        notice.addView(heading("Your route stays private", 18, ink));
        notice.addView(text("Map files are stored only in My Personal Trainer. Downloaded and imported files can be removed here at any time.", 14, muted));
        body.addView(notice); gap(body, 12);

        body.addView(heading("AVAILABLE COUNTRY DOWNLOAD", 11, mint));
        LinearLayout trinidad = card();
        trinidad.addView(heading("Trinidad & Tobago", 20, ink));
        trinidad.addView(text("Verified street map · 10 MB · works offline", 14, muted));
        gap(trinidad, 7);
        addTrinidadOfflineMapControl(trinidad, this::showOfflineMapCatalog);
        body.addView(trinidad); gap(body, 12);

        body.addView(heading("YOUR COUNTRY MAPS", 11, mint));
        List<OfflineMapStore.MapRecord> maps = OfflineMapStore.installedMaps(this);
        if (maps.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(text("No country map is installed yet.", 14, muted));
            body.addView(empty);
        } else {
            OfflineMapStore.MapRecord selected = OfflineMapStore.selectedMap(this);
            for (OfflineMapStore.MapRecord record : maps) {
                LinearLayout item = card();
                item.addView(heading(record.country, 18, ink));
                String detail = (record.verifiedDownload ? "Verified download" : "Imported Mapsforge file") + " · " + Math.max(1, Math.round(record.bytes / 1_000_000f)) + " MB";
                item.addView(text(detail, 13, muted)); gap(item, 8);
                Button use = record.id.equals(selected == null ? "" : selected.id) ? secondary("Used for my routes") : lightAction("Use for my routes");
                use.setEnabled(!record.id.equals(selected == null ? "" : selected.id));
                use.setOnClickListener(view -> { OfflineMapStore.selectMap(this, record.id); showOfflineMapCatalog(); });
                addButton(item, use); gap(item, 6);
                Button remove = secondary("Remove " + record.country + " map");
                remove.setOnClickListener(view -> new AlertDialog.Builder(this).setTitle("Remove " + record.country + "?")
                        .setMessage("This removes only the map. Your saved routes stay on this phone.")
                        .setNegativeButton("Cancel", null).setPositiveButton("Remove", (dialog, which) -> { OfflineMapStore.removeMap(this, record); showOfflineMapCatalog(); }).show());
                addButton(item, remove); body.addView(item); gap(body, 10);
            }
        }
        LinearLayout importCard = card();
        importCard.addView(heading("Add another country", 19, ink));
        importCard.addView(text("Choose a compatible Mapsforge .map country file already on your phone. It is copied into private app storage, checked before use, and then becomes available for your routes.", 14, muted));
        gap(importCard, 10);
        Button choose = lightAction("Choose country map file");
        choose.setOnClickListener(view -> {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/octet-stream").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(picker, REQUEST_OFFLINE_MAP);
        });
        addButton(importCard, choose); body.addView(importCard); gap(body, 12);
        body.addView(text("© OpenStreetMap contributors. Country map data may have its own open-data license. Use a country package that covers where you exercise.", 12, Color.rgb(202, 216, 234)));
        gap(body, 12); Button back = secondary("Back to settings"); back.setOnClickListener(view -> showProfile()); addButton(body, back);
    }

    private void askForImportedMapCountry(Uri map) {
        EditText country = input("Country or region name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        country.setHint("For example: Jamaica");
        new AlertDialog.Builder(this).setTitle("Name this offline map").setMessage("Use the country or region this map covers.").setView(country)
                .setNegativeButton("Cancel", null).setPositiveButton("Add map", (dialog, which) -> {
                    String name = country.getText().toString().trim();
                    if (name.isEmpty()) { toast("Enter the country or region name."); return; }
                    new Thread(() -> {
                        try { OfflineMapStore.importMap(this, map, name); runOnUiThread(() -> { toast(name + " is ready for offline routes."); showOfflineMapCatalog(); }); }
                        catch (Exception error) { runOnUiThread(() -> toast(error.getMessage() == null ? "Could not add that country map." : error.getMessage())); }
                    }, "offline-map-import").start();
                }).show();
    }

    /** Detailed progress record for a normally completed guided workout only. */
    private void showWorkoutSetTimeDetails(JourneyStore.WorkoutRecord record, SetTimeStore.WorkoutSummary timing) {
        LinearLayout body = appPage("Progress");
        String completed = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(record.timestamp);
        title(body, "SET-TIME PERFORMANCE", record.target,
                "Completed " + completed + ". Each exercise is ranked from fastest to slowest. Rest and cooldown time are never included in these totals.");

        LinearLayout overview = card();
        overview.addView(heading("SESSION TOTAL", 11, muted));
        overview.addView(heading(WorkoutTiming.formatElapsedMillis(timing.totalActiveMillis), 28, ink));
        overview.addView(text(timing.exerciseSummaries.size() + " completed exercise"
                + (timing.exerciseSummaries.size() == 1 ? "" : "s") + "  •  "
                + record.minutes + " guided min", 14, muted));
        body.addView(overview);

        if (timing.exerciseSummaries.isEmpty()) {
            gap(body, 12);
            body.addView(text("No set-time details were saved for this earlier workout.", 14, muted));
        } else {
            for (SetTimeStore.ExerciseSummary summary : timing.exerciseSummaries) {
                gap(body, 12);
                body.addView(heading(summary.exerciseName, 20, Color.WHITE));
                addSetTimesCard(body, summary, true);
            }
        }
        gap(body, 14);
        Button back = secondary("Back to Progress");
        back.setOnClickListener(view -> showProgress());
        addButton(body, back);
    }

    private int plannedSessionsThisWeek() {
        if (session.isEmpty()) return 0;
        return "Weekly".equals(planMode) ? Math.max(1, trainingDays) : 1;
    }

    /** Dedicated, local-only body context reached directly from the left navigation rail. */
    private void showBodyContext() {
        LinearLayout body = appPage("Body");
        bodyReferenceTitle(body);

        if (!BodyProfileStore.configured(this) || !WeightStore.configured(this)) {
            LinearLayout start = darkCard();
            start.addView(heading("Build your private context", 22, Color.WHITE));
            start.addView(text("Add your adult age, height, optional gender, and current weight. You can then see an adult screening reference, an adjustable walking-distance estimate, and how your planned activity compares with general guidance.", 14, Color.rgb(202, 216, 234)));
            gap(start, 11);
            Button complete = primary("Add my private body context");
            complete.setOnClickListener(view -> showBodyProfileSetup(false));
            addButton(start, complete);
            body.addView(start);
            gap(body, 12);
            body.addView(text("For anyone under 20, use an age- and sex-specific growth assessment with a qualified healthcare professional rather than an adult reference range.", 13, Color.rgb(211, 225, 233)));
            return;
        }

        int age = BodyProfileStore.age(this);
        float heightCm = BodyProfileStore.heightCm(this);
        String unit = WeightStore.unit(this);
        float current = WeightStore.latest(this);
        float targetWeight = WeightStore.target(this);
        String targetLabel = targetWeight > 0f
                ? String.format(Locale.US, "%.1f %s", targetWeight, unit)
                : "Not set";
        // Keep the reference scale meaningful until the optional target is supplied.
        float scaleTarget = targetWeight > 0f ? targetWeight : current;
        String distanceToTarget = targetWeight > 0f
                ? String.format(Locale.US, "%.1f %s to your selected target", Math.abs(current - targetWeight), unit)
                : "Set a personal target whenever you are ready";

        LinearLayout identity = bodyReferenceCard();
        LinearLayout metrics = new LinearLayout(this);
        metrics.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout currentMetric = new LinearLayout(this);
        currentMetric.setOrientation(LinearLayout.VERTICAL);
        currentMetric.setPadding(dp(12), dp(11), dp(12), dp(11));
        currentMetric.setBackground(surface(Color.rgb(255, 239, 237), 14, 0, Color.TRANSPARENT));
        currentMetric.addView(heading("CURRENT WEIGHT", 10, muted));
        currentMetric.addView(heading(WeightStore.latestLabel(this), 26, navy));
        currentMetric.addView(text("●  TODAY", 11, coral));
        metrics.addView(currentMetric, new LinearLayout.LayoutParams(0, -2, 1));
        View metricGap = new View(this);
        metrics.addView(metricGap, new LinearLayout.LayoutParams(dp(9), 1));
        LinearLayout targetMetric = new LinearLayout(this);
        targetMetric.setOrientation(LinearLayout.VERTICAL);
        targetMetric.setPadding(dp(12), dp(11), dp(12), dp(11));
        targetMetric.setBackground(surface(Color.rgb(230, 249, 243), 14, 0, Color.TRANSPARENT));
        targetMetric.addView(heading("PERSONAL TARGET", 10, muted));
        targetMetric.addView(heading(targetLabel, 23, navy));
        targetMetric.addView(text("◎  YOUR MARK", 11, navy));
        metrics.addView(targetMetric, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams metricsParams = new LinearLayout.LayoutParams(-1, -2);
        identity.addView(metrics, metricsParams);
        View identityDivider = bodyReferenceDivider();
        LinearLayout.LayoutParams identityDividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        identityDividerParams.topMargin = dp(12);
        identityDividerParams.bottomMargin = dp(8);
        identity.addView(identityDivider, identityDividerParams);
        identity.addView(text(distanceToTarget, 14, navy));
        identity.addView(text("Adult screening reference: " + BodyInsights.referenceRange(heightCm, unit) + " • screening only, not a diagnosis or ideal weight.", 12, muted));
        body.addView(identity);
        gap(body, 12);

        LinearLayout reference = bodyReferenceCard();
        if (BodyInsights.usesAdultReference(age)) {
            float referenceLow = BodyInsights.fromKg(BodyInsights.referenceWeightLowKg(heightCm), unit);
            float referenceHigh = BodyInsights.fromKg(BodyInsights.referenceWeightHighKg(heightCm), unit);
            reference.addView(heading("WEIGHT REFERENCE", 11, muted));
            TextView legend = text("●  Now · " + WeightStore.latestLabel(this) + "     ◎  Target · " + targetLabel, 11, ink);
            legend.setSingleLine(true);
            LinearLayout.LayoutParams legendParams = new LinearLayout.LayoutParams(-1, -2);
            legendParams.topMargin = dp(5);
            reference.addView(legend, legendParams);
            WeightReferenceBarsView chart = new WeightReferenceBarsView(this);
            chart.setValues(WeightStore.starting(this), current, scaleTarget,
                    referenceLow, referenceHigh, unit);
            LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(-1, dp(104));
            chartParams.topMargin = dp(3);
            reference.addView(chart, chartParams);
            String rangeStatus;
            int statusFill;
            if (current > referenceHigh) {
                rangeStatus = String.format(Locale.US, "Current: %.1f %s above upper reference", current - referenceHigh, unit);
                statusFill = Color.rgb(255, 235, 232);
            } else if (current < referenceLow) {
                rangeStatus = String.format(Locale.US, "Current: %.1f %s below lower reference", referenceLow - current, unit);
                statusFill = Color.rgb(232, 241, 255);
            } else {
                rangeStatus = "Current: within this height-based reference";
                statusFill = Color.rgb(230, 249, 243);
            }
            TextView status = heading(rangeStatus, 12, navy);
            status.setPadding(dp(11), dp(8), dp(11), dp(8));
            status.setBackground(surface(statusFill, 10, 0, Color.TRANSPARENT));
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
            statusParams.topMargin = dp(4);
            reference.addView(status, statusParams);
            reference.addView(text("Reference only — not a diagnosis or ideal weight.", 11, muted));
        } else {
            reference.addView(heading("Age-specific growth context", 19, Color.WHITE));
            reference.addView(text("For ages 18–19, this app does not apply an adult BMI range. Growth and weight context require a sex- and age-specific clinical chart, so talk with a qualified healthcare professional if you want an assessment.", 14, Color.rgb(202, 216, 234)));
        }
        body.addView(reference);
        gap(body, 12);

        LinearLayout movement = bodyReferenceCard();
        movement.addView(heading("TWO-MILE RUN GUIDE", 11, muted));
        movement.addView(heading(BodyInsights.twoMilePracticeRange(age), 32, navy));
        movement.addView(text("Age-aware practice range — adjustable, not a pass/fail test.", 14, muted));
        View movementDivider = bodyReferenceDivider();
        LinearLayout.LayoutParams movementDividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        movementDividerParams.topMargin = dp(11);
        movementDividerParams.bottomMargin = dp(11);
        movement.addView(movementDivider, movementDividerParams);
        TextView paceCopy = text("🏃  2 miles  •  your selected pace", 15, navy);
        paceCopy.setContentDescription("Two miles at your selected practice pace");
        movement.addView(paceCopy);
        body.addView(movement);
        gap(body, 12);

        LinearLayout activity = bodyReferenceCard();
        activity.addView(heading("THIS WEEK", 11, muted));
        int plannedMinutes = session.isEmpty() ? 0 : plannedSessionsThisWeek() * sessionMinutes;
        activity.addView(heading(plannedMinutes + " / 150 min", 30, navy));
        activity.addView(text("planned movement", 14, muted));
        body.addView(activity);
        gap(body, 12);

        int estimatedBurn = CalorieInsights.estimatedDailyBurn(this);
        int calorieTarget = CalorieTargetStore.target(this, estimatedBurn);
        LinearLayout calories = bodyReferenceCard();
        calories.addView(heading("DAILY CALORIE PLAN", 11, muted));
        LinearLayout calorieMetrics = new LinearLayout(this);
        calorieMetrics.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout burnMetric = new LinearLayout(this);
        burnMetric.setOrientation(LinearLayout.VERTICAL);
        burnMetric.setPadding(dp(12), dp(10), dp(12), dp(10));
        burnMetric.setBackground(surface(Color.rgb(231, 248, 245), 14, 0, Color.TRANSPARENT));
        burnMetric.addView(heading("EST. DAILY BURN", 10, muted));
        burnMetric.addView(heading(estimatedBurn + " kcal", 24, navy));
        burnMetric.addView(text(CalorieInsights.movementExplanation(this), 11, muted));
        calorieMetrics.addView(burnMetric, new LinearLayout.LayoutParams(0, -2, 1f));
        View calorieGap = new View(this);
        calorieMetrics.addView(calorieGap, new LinearLayout.LayoutParams(dp(9), 1));
        LinearLayout calorieTargetMetric = new LinearLayout(this);
        calorieTargetMetric.setOrientation(LinearLayout.VERTICAL);
        calorieTargetMetric.setPadding(dp(12), dp(10), dp(12), dp(10));
        calorieTargetMetric.setBackground(surface(Color.rgb(255, 239, 237), 14, 0, Color.TRANSPARENT));
        calorieTargetMetric.addView(heading("MY DAILY TARGET", 10, muted));
        calorieTargetMetric.addView(heading(calorieTarget + " kcal", 24, navy));
        calorieTargetMetric.addView(text("Your selected intake goal", 11, muted));
        calorieMetrics.addView(calorieTargetMetric, new LinearLayout.LayoutParams(0, -2, 1f));
        calories.addView(calorieMetrics);
        LinearLayout.LayoutParams calorieMetricsParams = (LinearLayout.LayoutParams) calorieMetrics.getLayoutParams();
        if (calorieMetricsParams != null) calorieMetricsParams.topMargin = dp(7);
        calories.addView(text("An estimate based on your private profile and daily movement choice. It is not medical advice or a required intake.", 12, muted));
        gap(calories, 8);
        Button setCalorieTarget = lightAction("Set daily calorie target");
        setCalorieTarget.setOnClickListener(view -> showCalorieTargetEditor());
        addButton(calories, setCalorieTarget);
        body.addView(calories);
        gap(body, 12);

        Button edit = secondary("Edit private body context");
        edit.setOnClickListener(view -> showBodyProfileSetup(false));
        addButton(body, edit);
        gap(body, 10);
        Button nutrition = lightAction("Open nutrition + Ask Coach");
        nutrition.setOnClickListener(view -> showMeals());
        addButton(body, nutrition);
        gap(body, 12);
        body.addView(text("If you are pregnant, have a medical condition, take medication that affects weight or activity, have an eating concern, or experience chest pain, dizziness, or unusual shortness of breath, use a qualified clinician for individualized advice.", 13, Color.rgb(211, 225, 233)));
    }

    private void showCalorieTargetEditor() {
        int estimate = CalorieInsights.estimatedDailyBurn(this);
        LinearLayout editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(dp(24), dp(12), dp(24), 0);
        TextView explanation = new TextView(this);
        explanation.setText("Your estimated daily burn is " + estimate + " kcal. Set an intake target you want the app to use when comparing meal ideas. This does not prescribe weight loss or gain.");
        explanation.setTextSize(14);
        explanation.setTextColor(Color.rgb(70, 83, 105));
        editor.addView(explanation);
        EditText target = new EditText(this);
        target.setInputType(InputType.TYPE_CLASS_NUMBER);
        target.setHint("Daily calorie target");
        target.setText(String.valueOf(CalorieTargetStore.target(this, estimate)));
        LinearLayout.LayoutParams targetParams = new LinearLayout.LayoutParams(-1, -2);
        targetParams.topMargin = dp(14);
        editor.addView(target, targetParams);
        TextView movementLabel = new TextView(this);
        movementLabel.setText("Daily movement");
        movementLabel.setTextSize(15);
        movementLabel.setTextColor(Color.rgb(22, 38, 61));
        LinearLayout.LayoutParams movementLabelParams = new LinearLayout.LayoutParams(-1, -2);
        movementLabelParams.topMargin = dp(13);
        editor.addView(movementLabel, movementLabelParams);
        RadioGroup movement = new RadioGroup(this);
        movement.setOrientation(RadioGroup.VERTICAL);
        String savedMovement = CalorieTargetStore.movement(this);
        RadioButton low = new RadioButton(this); low.setId(View.generateViewId()); low.setText("Lower movement"); low.setChecked(CalorieTargetStore.LOW.equals(savedMovement));
        RadioButton typical = new RadioButton(this); typical.setId(View.generateViewId()); typical.setText("Typical movement"); typical.setChecked(CalorieTargetStore.TYPICAL.equals(savedMovement));
        RadioButton active = new RadioButton(this); active.setId(View.generateViewId()); active.setText("Higher movement"); active.setChecked(CalorieTargetStore.ACTIVE.equals(savedMovement));
        movement.addView(low); movement.addView(typical); movement.addView(active);
        editor.addView(movement);
        new AlertDialog.Builder(this)
                .setTitle("Daily calorie target")
                .setView(editor)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        int enteredTarget = Integer.parseInt(target.getText().toString().trim());
                        String selectedMovement = low.isChecked() ? CalorieTargetStore.LOW
                                : active.isChecked() ? CalorieTargetStore.ACTIVE : CalorieTargetStore.TYPICAL;
                        CalorieTargetStore.save(this, enteredTarget, selectedMovement);
                        showBodyContext();
                    } catch (Exception ignored) {
                        toast("Enter a target between 1,000 and 6,000 kcal.");
                    }
                })
                .show();
    }

    private void showProfile() {
        AccountStore.Profile profile = AccountStore.get(this);
        LinearLayout body = appPage("Settings");
        title(body, "SETTINGS", "Account & preferences", "Your profile, security, coach, appearance, privacy, and updates.");

        LinearLayout identity = card();
        identity.setOrientation(LinearLayout.HORIZONTAL);
        ImageView profilePhoto = new ImageView(this);
        bindProfilePhoto(profilePhoto, profile);
        LinearLayout.LayoutParams profilePhotoParams = new LinearLayout.LayoutParams(dp(70), dp(70));
        profilePhotoParams.rightMargin = dp(14);
        identity.addView(profilePhoto, profilePhotoParams);
        LinearLayout identityCopy = new LinearLayout(this); identityCopy.setOrientation(LinearLayout.VERTICAL);
        identityCopy.addView(heading(profile.username, 19, ink));
        identityCopy.addView(text(profile.username + " · " + profile.email, 14, muted));
        identityCopy.addView(text("Tap to edit profile", 13, mint));
        identity.addView(identityCopy, new LinearLayout.LayoutParams(0, -2, 1));
        identity.setClickable(true); identity.setFocusable(true);
        identity.setOnClickListener(view -> showProfileEditor());
        body.addView(identity);
        gap(body, 12);

        body.addView(settingsGroup("ACCOUNT"));
        LinearLayout accountGroup = settingsPanel();
        accountGroup.addView(settingsRow("●", "Personal details", "Name, photo, remember me & device lock", view -> showProfileEditor()));
        accountGroup.addView(settingsDivider());
        accountGroup.addView(settingsRow("⌁", "Security", "Password and biometric unlock", view -> showSecuritySettings()));
        accountGroup.addView(settingsDivider());
        accountGroup.addView(settingsRow("◌", "Privacy", "Profile details stay on this device", null));
        body.addView(accountGroup);
        gap(body, 12);

        body.addView(settingsGroup("PREFERENCES"));
        LinearLayout preferencesGroup = settingsPanel();
        preferencesGroup.addView(settingsRow("★", "Coach Hub", profile.coachName + " · " + profile.dialogueMode, view -> showCoachCustomization()));
        preferencesGroup.addView(settingsDivider());
        preferencesGroup.addView(settingsRow("◐", "Appearance", AppearanceStore.theme(this) + " theme · " + (reducedMotion ? "Reduced motion" : "Expressive motion"), view -> showAppearanceOptions()));
        body.addView(preferencesGroup);
        gap(body, 12);

        body.addView(settingsGroup("APP"));
        LinearLayout appGroup = settingsPanel();
        appGroup.addView(settingsRow("⌖", "Offline maps", OfflineMapStore.hasAnyMap(this) ? "Country map ready for routes" : "Download a country for offline routes", view -> showOfflineMapCatalog()));
        appGroup.addView(settingsDivider());
        appGroup.addView(settingsRow("↻", "Updates", "Check for a newer version", view -> showUpdate()));
        body.addView(appGroup);
        gap(body, 18);
        Button signOut = secondary("Sign out and clear this device");
        signOut.setOnClickListener(view -> {
            AccountStore.clear(this);
            PlanStore.clear(this);
            WeeklyScheduleStore.clear(this);
            WorkoutReminderScheduler.cancel(this);
            WeightStore.clear(this);
            BodyProfileStore.clear(this);
            MealPlanStore.clear(this);
            EndurancePlanStore.clear(this);
            LiveTrackingStore.clear(this);
            SchedulerStore.clear(this);
            stopService(new Intent(this, LiveTrackingService.class));
            JourneyStore.clear(this);
            SetTimeStore.clear(this);
            session.clear();
            selectedExercises.clear();
            selectedTrainer = "Female";
            applyKeepPhoneAwake(false);
            showWelcome();
        });
        addButton(body, signOut);
    }

    /** WhatsApp-inspired settings groups: one clear profile row, concise rows, and muted dividers. */
    private TextView settingsGroup(String label) {
        TextView header = heading(label, 11, mint);
        header.setLetterSpacing(.08f);
        header.setPadding(dp(6), dp(4), 0, dp(6));
        return header;
    }

    private LinearLayout settingsPanel() {
        LinearLayout panel = card();
        panel.setPadding(0, 0, 0, 0);
        return panel;
    }

    private LinearLayout settingsRow(String symbol, String title, String summary, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(13), dp(14), dp(13));
        row.setBackgroundColor(Color.TRANSPARENT);
        ImageView icon = new ImageView(this);
        icon.setImageResource(settingsIcon(title));
        icon.setImageTintList(ColorStateList.valueOf(navy));
        icon.setPadding(dp(9), dp(9), dp(9), dp(9));
        icon.setBackground(surface(mint, 99, 1, Color.argb(90, 255, 255, 255)));
        row.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout words = new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
        TextView name = heading(title, 16, ink); words.addView(name);
        words.addView(text(summary, 13, muted));
        LinearLayout.LayoutParams wordParams = new LinearLayout.LayoutParams(0, -2, 1); wordParams.leftMargin = dp(13);
        row.addView(words, wordParams);
        TextView chevron = heading(onClick == null ? "" : "›", 28, muted); chevron.setGravity(Gravity.CENTER);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(24), dp(42)));
        if (onClick != null) { row.setClickable(true); row.setFocusable(true); row.setOnClickListener(onClick); applyPressMotion(row); }
        return row;
    }

    /** Replace text glyphs with one premium, device-consistent vector icon language. */
    private int settingsIcon(String title) {
        if ("Personal details".equals(title)) return R.drawable.ic_nav_account;
        if ("Security".equals(title) || "Appearance".equals(title) || "Updates".equals(title)) return R.drawable.ic_nav_settings;
        if ("Privacy".equals(title)) return R.drawable.ic_nav_body;
        if ("Customize your Coach".equals(title)) return R.drawable.ic_nav_workouts;
        return R.drawable.ic_nav_settings;
    }

    private View settingsDivider() {
        View divider = new View(this); divider.setBackgroundColor(cardLine);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1)); params.leftMargin = dp(65); divider.setLayoutParams(params);
        return divider;
    }

    /** Personal account details are separate from body context and from coach preferences. */
    private void showProfileEditor() {
        AccountStore.Profile profile = profile();
        if (selectedProfilePhotoUri == null) selectedProfilePhotoUri = profile.photoUri;
        LinearLayout body = appPage("Settings");
        title(body, "PROFILE", "Your personal profile", "Private account details stored on this device.");

        // A compact, familiar profile structure: identity first, then clear labelled account rows.
        LinearLayout photoCard = card();
        photoCard.setGravity(Gravity.CENTER_VERTICAL);
        photoCard.setOrientation(LinearLayout.HORIZONTAL);
        ImageView image = new ImageView(this);
        bindProfilePhoto(image, profile);
        photoCard.addView(image, new LinearLayout.LayoutParams(dp(86), dp(86)));
        LinearLayout photoCopy = new LinearLayout(this);
        photoCopy.setOrientation(LinearLayout.VERTICAL);
        photoCopy.setGravity(Gravity.CENTER_VERTICAL);
        photoCopy.addView(heading("Profile photo", 17, ink));
        TextView changePhoto = text(selectedProfilePhotoUri == null || selectedProfilePhotoUri.isEmpty() ? "Add a photo" : "Change photo", 14, mint);
        changePhoto.setPadding(0, dp(5), 0, 0);
        changePhoto.setClickable(true);
        changePhoto.setOnClickListener(view -> {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(picker, REQUEST_PROFILE_PHOTO);
        });
        applyPressMotion(changePhoto);
        photoCopy.addView(changePhoto);
        LinearLayout.LayoutParams chooseParams = new LinearLayout.LayoutParams(0, dp(86), 1); chooseParams.leftMargin = dp(16);
        photoCard.addView(photoCopy, chooseParams);
        body.addView(photoCard); gap(body, 12);

        body.addView(settingsGroup("ACCOUNT"));
        LinearLayout details = settingsPanel();
        EditText name = input("", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        name.setText(profile.username);
        name.setHint("Username");
        details.addView(profileEditableRow("Username", name));
        details.addView(settingsDivider());
        details.addView(profileReadOnlyRow("Email", profile.email));
        body.addView(details);
        gap(body, 12);

        body.addView(settingsGroup("SIGN-IN"));
        LinearLayout signIn = settingsPanel();
        CheckBox remember = new CheckBox(this); remember.setText("Remember this account on this device"); remember.setTextColor(ink); remember.setButtonTintList(ColorStateList.valueOf(mint)); remember.setChecked(profile.rememberSignIn);
        CheckBox biometric = new CheckBox(this); biometric.setText("Use biometrics to open my account"); biometric.setTextColor(ink); biometric.setButtonTintList(ColorStateList.valueOf(mint)); biometric.setChecked(profile.biometricUnlock);
        signIn.addView(remember);
        signIn.addView(biometric);
        body.addView(signIn);
        body.addView(text("Biometric unlock uses your phone’s enrolled fingerprint or face system. If it is unavailable, you can still use your password.", 13, Color.rgb(202, 216, 234)));
        gap(body, 12);
        Button save = primary("Save account settings");
        save.setOnClickListener(view -> {
            try {
                AccountStore.updatePersonalProfile(this, name.getText().toString(), selectedProfilePhotoUri, remember.isChecked(), biometric.isChecked(), "");
                toast("Account settings saved."); selectedProfilePhotoUri = null; showProfile();
            } catch (IllegalArgumentException error) { toast(error.getMessage()); }
        });
        addButton(body, save); gap(body, 8);
        Button back = secondary("Back to settings"); back.setOnClickListener(view -> { selectedProfilePhotoUri = null; showProfile(); }); addButton(body, back);
    }

    /** WhatsApp-inspired account row: a fixed label with the editable value clearly aligned beside it. */
    private LinearLayout profileEditableRow(String label, EditText field) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(7), dp(14), dp(7));
        TextView caption = heading(label, 15, ink);
        row.addView(caption, new LinearLayout.LayoutParams(dp(112), dp(48)));
        field.setTextSize(15);
        field.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        field.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        field.setPadding(dp(10), 0, dp(10), 0);
        field.setBackground(surface(Color.rgb(239, 246, 250), 12, 1, cardLine));
        row.addView(field, new LinearLayout.LayoutParams(0, dp(48), 1));
        return row;
    }

    private LinearLayout profileReadOnlyRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView caption = heading(label, 15, ink);
        row.addView(caption, new LinearLayout.LayoutParams(dp(112), dp(42)));
        TextView content = text(value, 14, muted);
        content.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        content.setSingleLine(true);
        content.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        row.addView(content, new LinearLayout.LayoutParams(0, dp(42), 1));
        return row;
    }

    private void showSecuritySettings() {
        LinearLayout body = appPage("Settings");
        title(body, "SECURITY", "Protect your account", "Confirm your current password before choosing a new one. Your password stays on this device.");
        LinearLayout notice = card();
        notice.addView(heading("Change password", 18, ink));
        notice.addView(text("Use at least 8 characters. Your current password is required to continue.", 14, muted));
        body.addView(notice); gap(body, 12);
        EditText current = input("Current password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); body.addView(current); gap(body, 9);
        EditText updated = input("New password — at least 8 characters", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); body.addView(updated); gap(body, 9);
        EditText confirm = input("Confirm new password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); body.addView(confirm); gap(body, 14);
        Button save = primary("Change password");
        save.setOnClickListener(view -> {
            String next = updated.getText().toString();
            if (!next.equals(confirm.getText().toString())) { toast("The new passwords do not match."); return; }
            if (AccountStore.changePassword(this, current.getText().toString(), next)) { toast("Password changed."); showProfile(); }
            else toast("Check your current password and use at least 8 characters.");
        });
        addButton(body, save); gap(body, 9);
        Button back = secondary("Back to settings"); back.setOnClickListener(view -> showProfile()); addButton(body, back);
    }

    private void bindProfilePhoto(ImageView view, AccountStore.Profile profile) {
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setBackground(surface(navyPanel, 99, 2, mint));
        view.setClipToOutline(true);
        String uri = selectedProfilePhotoUri != null ? selectedProfilePhotoUri : profile.photoUri;
        try {
            if (uri != null && !uri.isEmpty()) { view.setImageURI(Uri.parse(uri)); return; }
        } catch (RuntimeException ignored) { }
        view.setImageResource(coachPortrait(profile.coachName));
    }

    private void showAppearanceOptions() {
        final String[] themes = {AppearanceStore.MIDNIGHT, AppearanceStore.OCEAN, AppearanceStore.HIGH_CONTRAST};
        int checked = 0;
        for (int i = 0; i < themes.length; i++) if (themes[i].equals(AppearanceStore.theme(this))) checked = i;
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        options.setPadding(padding, dp(4), padding, 0);
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        final RadioButton[] themeOptions = new RadioButton[themes.length];
        for (int i = 0; i < themes.length; i++) {
            RadioButton option = new RadioButton(this);
            option.setId(View.generateViewId());
            option.setText(themes[i]);
            option.setTextSize(16);
            option.setMinHeight(dp(48));
            option.setContentDescription(themes[i] + " theme");
            group.addView(option);
            themeOptions[i] = option;
        }
        group.check(themeOptions[checked].getId());
        options.addView(group);
        CheckBox motion = new CheckBox(this);
        motion.setText("Reduce motion");
        motion.setTextSize(16);
        motion.setMinHeight(dp(48));
        motion.setChecked(AppearanceStore.reducedMotion(this));
        motion.setContentDescription("Reduce motion");
        options.addView(motion);
        options.addView(text("Reduced motion keeps all controls fully available while removing entrance and press animations.", 13, muted));
        new AlertDialog.Builder(this)
                .setTitle("Appearance")
                .setView(options)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    int selection = 0;
                    for (int i = 0; i < themeOptions.length; i++) {
                        if (themeOptions[i].isChecked()) { selection = i; break; }
                    }
                    AppearanceStore.theme(this, themes[selection]);
                    AppearanceStore.reducedMotion(this, motion.isChecked());
                    applyAppearance();
                    toast("Appearance updated.");
                    showProfile();
                })
                .show();
    }

    private void showBodyProfileSetup(boolean onboarding) {
        LinearLayout body = appPage("Settings");
        title(body, "PRIVATE PROFILE", onboarding ? "Connect your plan" : "Body & goal profile",
                "Age, height, gender, and weight stay on this device. They add context across Workouts, Progress, and Nutrition; they do not diagnose health or choose a lifting load.");

        LinearLayout consent = card();
        consent.addView(heading("Why these details help", 18, ink));
        consent.addView(text("Age can surface age-aware safety context. Height and weight support a private trend. Gender is optional and never inferred from your coach. Nutrition remains flexible and does not prescribe calories from these details.", 14, muted));
        body.addView(consent);
        gap(body, 12);

        EditText age = input("Age — adults 18+", InputType.TYPE_CLASS_NUMBER);
        if (BodyProfileStore.configured(this)) age.setText(String.valueOf(BodyProfileStore.age(this)));
        body.addView(age);
        gap(body, 9);
        EditText height = input("Height in centimetres", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (BodyProfileStore.configured(this)) height.setText(String.format(Locale.US, "%.0f", BodyProfileStore.heightCm(this)));
        body.addView(height);
        gap(body, 9);
        body.addView(heading("Your gender — optional", 17, Color.WHITE));
        RadioGroup gender = new RadioGroup(this);
        gender.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton female = profileGenderOption("Female", BodyProfileStore.GENDER_FEMALE.equals(BodyProfileStore.gender(this)));
        RadioButton male = profileGenderOption("Male", BodyProfileStore.GENDER_MALE.equals(BodyProfileStore.gender(this)));
        RadioButton notSpecified = profileGenderOption("Prefer not to say", !BodyProfileStore.GENDER_FEMALE.equals(BodyProfileStore.gender(this)) && !BodyProfileStore.GENDER_MALE.equals(BodyProfileStore.gender(this)));
        gender.addView(female);
        gender.addView(male);
        gender.addView(notSpecified);
        body.addView(gender);
        gap(body, 9);
        EditText current = input("Current weight", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (WeightStore.configured(this)) current.setText(String.format(Locale.US, "%.1f", WeightStore.latest(this)));
        body.addView(current);
        gap(body, 9);
        EditText targetWeight = input("Personal target weight — optional", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (WeightStore.configured(this)) targetWeight.setText(String.format(Locale.US, "%.1f", WeightStore.target(this)));
        body.addView(targetWeight);
        gap(body, 12);

        body.addView(heading("Weight unit", 17, Color.WHITE));
        RadioGroup units = new RadioGroup(this);
        units.setOrientation(RadioGroup.HORIZONTAL);
        boolean usePounds = WeightStore.configured(this) && "lb".equals(WeightStore.unit(this));
        units.addView(unitOption(R.id.unit_kg, "kg", !usePounds));
        units.addView(unitOption(R.id.unit_lb, "lb", usePounds));
        body.addView(units);
        gap(body, 14);

        Button save = primary("Save and connect my pages");
        save.setOnClickListener(view -> {
            try {
                int enteredAge = Integer.parseInt(age.getText().toString().trim());
                float enteredHeight = Float.parseFloat(height.getText().toString().trim());
                float enteredWeight = Float.parseFloat(current.getText().toString().trim());
                if (enteredWeight <= 0) throw new IllegalArgumentException();
                String targetText = targetWeight.getText().toString().trim();
                float enteredTarget = targetText.isEmpty() ? enteredWeight : Float.parseFloat(targetText);
                if (enteredTarget <= 0) throw new IllegalArgumentException();
                String unit = units.getCheckedRadioButtonId() == R.id.unit_lb ? "lb" : "kg";
                String enteredGender = female.isChecked() ? BodyProfileStore.GENDER_FEMALE
                        : male.isChecked() ? BodyProfileStore.GENDER_MALE : BodyProfileStore.GENDER_NOT_SET;
                BodyProfileStore.save(this, enteredAge, enteredHeight, enteredGender);
                WeightStore.updateProfile(this, enteredWeight, enteredTarget, unit);
                toast("Your private profile now connects all three pages.");
                if (onboarding) showPlans(); else showProfile();
            } catch (Exception ignored) {
                toast("Enter an adult age 18–100, height 100–250 cm, and positive weight.");
            }
        });
        addButton(body, save);
        gap(body, 9);
        Button skip = secondary(onboarding ? "Skip for now" : "Back to account");
        skip.setOnClickListener(view -> { if (onboarding) showPlans(); else showProfile(); });
        addButton(body, skip);
    }

    /** Coach Hub is the dedicated home for everything that belongs to the coach system. */
    private void showCoachCustomization() { showCoachHub(); }

    private void showCoachHub() {
        AccountStore.Profile profile = profile();
        LinearLayout body = appPage("Coach Hub", "Settings");
        title(body, "COACH HUB", "Your training partner", "Choose your coach, explore their card, and tailor how they guide every session.");

        LinearLayout active = darkCard();
        active.setOrientation(LinearLayout.HORIZONTAL);
        active.setGravity(Gravity.CENTER_VERTICAL);
        ImageView portrait = new ImageView(this);
        // Keep the hub consistent with Customize Your Coach: show the actual Coach Hero face.
        setCoachHeroFace(portrait, profile.coachName);
        portrait.setBackground(surface(navyPanel, 99, 1, mint));
        portrait.setClipToOutline(true);
        active.addView(portrait, new LinearLayout.LayoutParams(dp(76), dp(76)));
        LinearLayout activeCopy = new LinearLayout(this);
        activeCopy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = heading("ACTIVE COACH", 10, mint);
        eyebrow.setLetterSpacing(.13f);
        activeCopy.addView(eyebrow);
        activeCopy.addView(heading("Coach " + profile.coachName, 23, Color.WHITE));
        activeCopy.addView(text(CoachPreferences.coach(profile.coachName).accent + " · " + profile.dialogueMode, 13, Color.rgb(211, 225, 233)));
        LinearLayout.LayoutParams activeCopyParams = new LinearLayout.LayoutParams(0, -2, 1f);
        activeCopyParams.leftMargin = dp(14);
        active.addView(activeCopy, activeCopyParams);
        body.addView(active);
        gap(body, 14);

        LinearLayout collection = darkCard();
        collection.addView(heading("Coach Collection", 20, Color.WHITE));
        collection.addView(text("Explore Mint Foil cards, unlock new coaching styles, and choose an available coach.", 14, Color.rgb(211, 225, 233)));
        gap(collection, 10);
        Button browse = action("Open coach collection", mint, navy);
        browse.setOnClickListener(view -> showCoachProfiles());
        addButton(collection, browse);
        body.addView(collection);
        gap(body, 12);

        LinearLayout experience = darkCard();
        experience.addView(heading("Customize Your Coach", 20, Color.WHITE));
        experience.addView(text("Choose a coach, set voice dialogue, and decide whether the screen stays awake while you train.", 14, Color.rgb(211, 225, 233)));
        gap(experience, 10);
        Button preferences = secondary("Open Customize Your Coach");
        preferences.setOnClickListener(view -> showCoachPreferences());
        addButton(experience, preferences);
        body.addView(experience);
        gap(body, 12);
        Button back = secondary("Back to account");
        back.setOnClickListener(view -> showProfile());
        addButton(body, back);
    }

    /** Detailed controls reached from Coach Hub, rather than crowding the Settings page. */
    private void showCoachPreferences() {
        AccountStore.Profile profile = profile();
        LinearLayout body = appPage("Settings");
        title(body, "COACH HUB", "Customize Your Coach", "Choose who guides you, then fine-tune dialogue, voice, and wake behavior.");

        body.addView(heading("Female coaches", 21, Color.WHITE));
        gap(body, 8);
        for (CoachPreferences.CoachOption option : CoachPreferences.coaches()) {
            if ("Female".equals(option.gender)) addCoachChoice(body, option, option.name.equals(profile.coachName));
        }
        gap(body, 10);
        body.addView(heading("Male coaches", 21, Color.WHITE));
        gap(body, 8);
        for (CoachPreferences.CoachOption option : CoachPreferences.coaches()) {
            if ("Male".equals(option.gender)) addCoachChoice(body, option, option.name.equals(profile.coachName));
        }

        gap(body, 15);
        body.addView(heading("How much should your coach talk?", 21, Color.WHITE));
        gap(body, 8);
        for (String mode : CoachPreferences.DIALOGUE_MODES) {
            String explanation;
            if (CoachPreferences.FULL_DIALOGUE.equals(mode)) explanation = "All count-ins, form guidance, and set-completion encouragement.";
            else if (CoachPreferences.MINIMAL.equals(mode)) explanation = "Only get-ready prompts and spoken countdowns.";
            else if (CoachPreferences.NO_DIALOGUE.equals(mode)) explanation = "Low beeps for 3–1, then one high start beep.";
            else explanation = "No voice and no countdown beeps.";
            LinearLayout choice = card();
            choice.setBackground(surface(paper, 18, mode.equals(profile.dialogueMode) ? 2 : 1, mode.equals(profile.dialogueMode) ? mint : cardLine));
            choice.addView(heading((mode.equals(profile.dialogueMode) ? "✓  " : "") + mode, 17, ink));
            choice.addView(text(explanation, 14, muted));
            choice.setClickable(true);
            choice.setFocusable(true);
            choice.setContentDescription(mode + ". " + explanation + (mode.equals(profile.dialogueMode) ? " Selected" : ""));
            choice.setOnClickListener(view -> updateCoachSettings(profile.coachName, mode, profile.keepPhoneAwake));
            body.addView(choice);
            gap(body, 8);
        }

        LinearLayout wake = card();
        CheckBox keepAwake = new CheckBox(this);
        keepAwake.setText("Keep Phone Awake");
        keepAwake.setTextSize(17);
        keepAwake.setTextColor(ink);
        keepAwake.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        keepAwake.setButtonTintList(ColorStateList.valueOf(mint));
        keepAwake.setChecked(profile.keepPhoneAwake);
        wake.addView(keepAwake);
        wake.addView(text("Prevents the screen from sleeping while the app is open. Your normal timeout returns when this is off.", 14, muted));
        keepAwake.setOnCheckedChangeListener((button, checked) -> updateCoachSettings(profile.coachName, profile.dialogueMode, checked));
        body.addView(wake);
        gap(body, 12);

        LinearLayout note = card();
        note.addView(heading("Coach voice", 17, ink));
        note.addView(text(coachVoiceStatus, 13, muted));
        gap(note, 9);
        Button previewVoice = primary(coachVoiceVerified ? "Preview Coach " + profile.coachName + "’s voice" : "Assign a voice to Coach " + profile.coachName);
        previewVoice.setOnClickListener(view -> {
            if (coachVoiceVerified) speakCoach("Hi, I’m " + profile.coachName + ". Let’s get ready for your workout.", TextToSpeech.QUEUE_FLUSH, null);
            else showVoiceOrganizer();
        });
        addButton(note, previewVoice);
        gap(note, 7);
        Button organizeVoices = secondary("Preview & label installed voices");
        organizeVoices.setOnClickListener(view -> showVoiceOrganizer());
        addButton(note, organizeVoices);
        gap(note, 7);
        Button voiceSettings = lightAction("Open Android voice settings");
        voiceSettings.setOnClickListener(view -> openVoiceSettings());
        addButton(note, voiceSettings);
        body.addView(note);
        gap(body, 12);
        Button back = secondary("Back to Coach Hub");
        back.setOnClickListener(view -> showCoachHub());
        addButton(body, back);
    }

    /**
     * Design-lock Coach Collection: a deliberately simple, scrollable set of tall Mint Foil
     * cards. Details live behind a long-press so selection remains a clean collectible grid.
     */
    private void showCoachProfiles() {
        AccountStore.Profile profile = profile();
        LinearLayout body = coachCollectionPage();
        TextView hint = text("Press and hold a card to explore.", 13, mint);
        hint.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_nav_account, 0, 0, 0);
        hint.setCompoundDrawablePadding(dp(7));
        body.addView(hint);
        gap(body, 14);

        List<CoachPreferences.CoachOption> roster = new ArrayList<>();
        for (String name : new String[]{"Mia", "Leo", "Amy", "Mark", "Claudia", "Stephen"}) roster.add(CoachPreferences.coach(name));
        for (int index = 0; index < roster.size(); index += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            CoachPreferences.CoachOption first = roster.get(index);
            row.addView(mintFoilCoachCard(first, first.name.equals(profile.coachName)), coachCardGridParams(false));
            if (index + 1 < roster.size()) {
                CoachPreferences.CoachOption second = roster.get(index + 1);
                row.addView(mintFoilCoachCard(second, second.name.equals(profile.coachName)), coachCardGridParams(true));
            }
            body.addView(row, new LinearLayout.LayoutParams(-1, coachCardArtworkHeight()));
            gap(body, 12);
        }
        Button back = secondary("Back to coach settings");
        back.setOnClickListener(view -> showCoachCustomization());
        addButton(body, back);
    }

    private LinearLayout.LayoutParams coachCardGridParams(boolean second) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
        if (second) params.leftMargin = dp(10); else params.rightMargin = dp(10);
        return params;
    }

    /** Keeps the grid at the same 1030:1527 portrait ratio as the approved Mint Foil artwork. */
    private int coachCardArtworkHeight() {
        float screenWidthDp = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density;
        float cardWidthDp = Math.max(140f, (screenWidthDp - 36f - 10f) / 2f);
        return dp(Math.round(cardWidthDp / (1030f / 1527f)));
    }

    /** The only tier variance on the grid is the compact upper-right category emblem. */
    private int coachTierIcon(String coach) {
        if ("Amy".equals(coach) || "Mark".equals(coach)) return R.drawable.ic_coach_momentum;
        if ("Claudia".equals(coach) || "Stephen".equals(coach)) return R.drawable.ic_coach_performance;
        return R.drawable.ic_coach_foundation;
    }

    private String coachTier(String coach) {
        if ("Amy".equals(coach) || "Mark".equals(coach)) return "MOMENTUM";
        if ("Claudia".equals(coach) || "Stephen".equals(coach)) return "PERFORMANCE";
        return "FOUNDATION";
    }

    private String coachUnlockRequirement(String coach) {
        if ("Amy".equals(coach) || "Mark".equals(coach)) return "LEVEL 4 · 8 SESSIONS";
        if ("Claudia".equals(coach) || "Stephen".equals(coach)) return "LEVEL 8 · 20 SESSIONS";
        return "UNLOCKED";
    }

    private boolean coachIsFoundation(String coach) { return "Mia".equals(coach) || "Leo".equals(coach); }

    /**
     * The collection is design-locked to the approved Mint Foil artwork.  It is intentionally
     * rendered as complete card art rather than reconstructed out of generic Android surfaces:
     * that keeps the double frame, corner foils, nameplate and status ribbon exactly consistent.
     */
    private int coachCardArtwork(String coach) {
        if ("Leo".equals(coach)) return R.drawable.coach_card_leo;
        if ("Amy".equals(coach)) return R.drawable.coach_card_amy;
        if ("Mark".equals(coach)) return R.drawable.coach_card_mark;
        if ("Claudia".equals(coach)) return R.drawable.coach_card_claudia;
        if ("Stephen".equals(coach)) return R.drawable.coach_card_stephen;
        return R.drawable.coach_card_mia;
    }

    private int premiumCoachTierIcon(String coach) {
        if ("Amy".equals(coach) || "Mark".equals(coach)) return R.drawable.ic_coach_momentum_solid;
        if ("Claudia".equals(coach) || "Stephen".equals(coach)) return R.drawable.ic_coach_performance_solid;
        return R.drawable.ic_coach_foundation_solid;
    }

    private FrameLayout mintFoilCoachCard(CoachPreferences.CoachOption option, boolean selected) {
        FrameLayout card = new FrameLayout(this);
        card.setClickable(true);
        card.setFocusable(true);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setContentDescription("Coach " + option.name + ", " + coachTier(option.name) + ", " + coachUnlockRequirement(option.name));
        card.setBackgroundColor(Color.TRANSPARENT);

        FrameLayout frontFace = new FrameLayout(this);
        ImageView artwork = new ImageView(this);
        artwork.setImageResource(coachCardArtwork(option.name));
        // The approved artwork includes its own border, bevels, title plate and status strip.
        // Stretch only into the intentionally taller card slot requested for the collection.
        artwork.setScaleType(ImageView.ScaleType.FIT_XY);
        artwork.setAdjustViewBounds(false);
        artwork.setContentDescription(card.getContentDescription());
        frontFace.addView(artwork, new FrameLayout.LayoutParams(-1, -1));

        // A solid, mounted tier emblem intentionally covers the thin artwork glyph beneath it.
        FrameLayout emblem = new FrameLayout(this);
        // The emblem is mounted on an unoutlined navy lozenge: premium contrast, no boxed border.
        emblem.setBackground(surface(Color.argb(238, 4, 31, 50), 12, 0, Color.TRANSPARENT));
        emblem.setElevation(dp(3));
        ImageView emblemIcon = new ImageView(this);
        emblemIcon.setImageResource(premiumCoachTierIcon(option.name));
        emblemIcon.setContentDescription(coachTier(option.name) + " tier");
        emblem.addView(emblemIcon, new FrameLayout.LayoutParams(dp(21), dp(21), Gravity.CENTER));
        FrameLayout.LayoutParams emblemParams = new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.TOP | Gravity.RIGHT);
        emblemParams.topMargin = dp(14); emblemParams.rightMargin = dp(13);
        frontFace.addView(emblem, emblemParams);
        card.addView(frontFace, new FrameLayout.LayoutParams(-1, -1));

        FrameLayout statsBack = coachStatsCardBack(option);
        statsBack.setVisibility(View.GONE);
        card.addView(statsBack, new FrameLayout.LayoutParams(-1, -1));
        card.setTag(Boolean.FALSE);
        card.setCameraDistance(dp(12000));

        card.setOnClickListener(view -> {
            if (Boolean.TRUE.equals(card.getTag())) {
                flipCoachCard(card, frontFace, statsBack, false);
                return;
            }
            if (!coachIsUnlocked(option.name)) {
                lockedCoachFeedback(card, option.name);
                return;
            }
            AccountStore.updateCoachPreferences(this, option.name, profile().dialogueMode, profile().keepPhoneAwake);
            LauncherIconManager.apply(this, option.name);
            configureSpeechVoices();
            refreshCurrentPageKeepingScroll(this::showCoachProfiles);
        });
        card.setOnLongClickListener(view -> {
            flipCoachCard(card, frontFace, statsBack, !Boolean.TRUE.equals(card.getTag()));
            return true;
        });
        applyPressMotion(card);
        return card;
    }

    /** Back of a card uses the same mint, navy and foil language while revealing coach stats. */
    private FrameLayout coachStatsCardBack(CoachPreferences.CoachOption option) {
        FrameLayout back = new FrameLayout(this);
        back.setBackground(surface(Color.rgb(5, 27, 48), 20, 2, mint));

        View innerFrame = new View(this);
        innerFrame.setBackground(surface(Color.TRANSPARENT, 15, 1, Color.rgb(59, 154, 164)));
        FrameLayout.LayoutParams innerFrameParams = new FrameLayout.LayoutParams(-1, -1);
        innerFrameParams.setMargins(dp(7), dp(7), dp(7), dp(7));
        back.addView(innerFrame, innerFrameParams);

        View topFoil = new View(this);
        topFoil.setRotation(-42f);
        topFoil.setBackground(surface(Color.argb(230, 119, 224, 196), 2, 0, Color.TRANSPARENT));
        FrameLayout.LayoutParams foilParams = new FrameLayout.LayoutParams(dp(56), dp(9), Gravity.TOP | Gravity.LEFT);
        foilParams.leftMargin = dp(-8); foilParams.topMargin = dp(11);
        back.addView(topFoil, foilParams);
        View bottomFoil = new View(this);
        bottomFoil.setRotation(-42f);
        bottomFoil.setBackground(surface(Color.argb(205, 119, 224, 196), 2, 0, Color.TRANSPARENT));
        FrameLayout.LayoutParams lowerFoilParams = new FrameLayout.LayoutParams(dp(56), dp(9), Gravity.BOTTOM | Gravity.RIGHT);
        lowerFoilParams.rightMargin = dp(-8); lowerFoilParams.bottomMargin = dp(11);
        back.addView(bottomFoil, lowerFoilParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(17), dp(14), dp(17), dp(10));
        TextView eyebrow = heading("COACH STATS", 10, mint);
        eyebrow.setLetterSpacing(.16f);
        copy.addView(eyebrow);
        TextView title = heading("COACH " + option.name.toUpperCase(Locale.ROOT), 17, Color.WHITE);
        title.setLetterSpacing(.03f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(5);
        copy.addView(title, titleParams);
        TextView tier = heading(coachTier(option.name), 10, mint);
        tier.setLetterSpacing(.14f);
        LinearLayout.LayoutParams tierParams = new LinearLayout.LayoutParams(-1, -2);
        tierParams.topMargin = dp(2);
        copy.addView(tier, tierParams);

        String ability;
        String bestFor;
        String perk;
        if ("Mia".equals(option.name)) { ability = "FORM FOCUS"; bestFor = "Confident technique"; perk = "+1 Form Streak point"; }
        else if ("Leo".equals(option.name)) { ability = "STRENGTH BUILDER"; bestFor = "Steady strength"; perk = "Progression plans"; }
        else if ("Amy".equals(option.name)) { ability = "CONSISTENCY STREAK"; bestFor = "Efficient weekly habits"; perk = "Streak challenges"; }
        else if ("Mark".equals(option.name)) { ability = "POWER PROGRESSION"; bestFor = "Load, volume & tempo"; perk = "Progression plans"; }
        else if ("Claudia".equals(option.name)) { ability = "PRECISION PERFORMANCE"; bestFor = "Balanced progression"; perk = "Split variety"; }
        else { ability = "ENDURANCE DRIVE"; bestFor = "Conditioning intervals"; perk = "Endurance challenges"; }

        TextView abilityView = heading(ability, 12, Color.WHITE);
        abilityView.setPadding(dp(11), dp(10), dp(11), dp(10));
        abilityView.setBackground(surface(Color.rgb(10, 46, 69), 12, 1, Color.rgb(43, 109, 127)));
        LinearLayout.LayoutParams abilityParams = new LinearLayout.LayoutParams(-1, -2);
        abilityParams.topMargin = dp(9);
        copy.addView(abilityView, abilityParams);
        TextView best = text("BEST FOR\n" + bestFor, 10, Color.rgb(222, 236, 243));
        best.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams bestParams = new LinearLayout.LayoutParams(-1, -2);
        bestParams.topMargin = dp(8);
        copy.addView(best, bestParams);
        TextView perkView = text("COACH PERK\n" + perk, 10, Color.rgb(222, 236, 243));
        perkView.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams perkParams = new LinearLayout.LayoutParams(-1, -2);
        perkParams.topMargin = dp(5);
        copy.addView(perkView, perkParams);

        if (!coachIsUnlocked(option.name)) {
            int complete = completedCoachSessions();
            int required = coachSessionRequirement(option.name);
            int percent = Math.min(100, Math.round(100f * complete / Math.max(1, required)));
            TextView unlock = heading("UNLOCK PROGRESS · " + percent + "%", 9, mint);
            unlock.setLetterSpacing(.08f);
            LinearLayout.LayoutParams unlockParams = new LinearLayout.LayoutParams(-1, -2);
            unlockParams.topMargin = dp(7);
            copy.addView(unlock, unlockParams);
            LinearLayout track = new LinearLayout(this);
            track.setOrientation(LinearLayout.HORIZONTAL);
            track.setBackground(surface(Color.rgb(13, 49, 70), 5, 0, Color.TRANSPARENT));
            View fill = new View(this);
            fill.setBackground(surface(mint, 5, 0, Color.TRANSPARENT));
            View remainder = new View(this);
            track.addView(fill, new LinearLayout.LayoutParams(0, dp(6), Math.max(.01f, percent)));
            track.addView(remainder, new LinearLayout.LayoutParams(0, dp(6), Math.max(.01f, 100 - percent)));
            LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(-1, dp(6));
            trackParams.topMargin = dp(4);
            copy.addView(track, trackParams);
            TextView progress = text(complete + " / " + required + " sessions · Level " + coachLevel() + " / " + coachLevelRequirement(option.name), 8, Color.rgb(202, 222, 231));
            LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, -2);
            progressParams.topMargin = dp(2);
            copy.addView(progress, progressParams);
        } else {
            TextView flipHint = heading("TAP TO FLIP BACK", 9, mint);
            flipHint.setLetterSpacing(.12f);
            LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
            hintParams.topMargin = dp(10);
            copy.addView(flipHint, hintParams);
        }
        back.addView(copy, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_VERTICAL));
        return back;
    }

    private void flipCoachCard(FrameLayout card, View front, View back, boolean showBack) {
        if (showBack && flippedCoachCard != null && flippedCoachCard != card && Boolean.TRUE.equals(flippedCoachCard.getTag())) {
            flipCoachCard(flippedCoachCard, flippedCoachFront, flippedCoachBack, false);
        }
        card.animate().cancel();
        card.animate().rotationY(90f).setDuration(reducedMotion ? 0 : 150).withEndAction(() -> {
            front.setVisibility(showBack ? View.GONE : View.VISIBLE);
            back.setVisibility(showBack ? View.VISIBLE : View.GONE);
            card.setTag(showBack);
            if (showBack) {
                flippedCoachCard = card;
                flippedCoachFront = front;
                flippedCoachBack = back;
            } else if (flippedCoachCard == card) {
                flippedCoachCard = null;
                flippedCoachFront = null;
                flippedCoachBack = null;
            }
            card.setRotationY(-90f);
            card.animate().rotationY(0f).setDuration(reducedMotion ? 0 : 150).start();
        }).start();
    }

    private int completedCoachSessions() { return JourneyStore.workouts(this).size(); }

    private int coachSessionRequirement(String coach) {
        if ("Amy".equals(coach) || "Mark".equals(coach)) return 8;
        if ("Claudia".equals(coach) || "Stephen".equals(coach)) return 20;
        return 0;
    }

    private int coachLevelRequirement(String coach) {
        if ("Amy".equals(coach) || "Mark".equals(coach)) return 4;
        if ("Claudia".equals(coach) || "Stephen".equals(coach)) return 8;
        return 1;
    }

    private int coachLevel() { return Math.max(1, 1 + completedCoachSessions() / 2); }

    private boolean coachIsUnlocked(String coach) {
        return coachIsFoundation(coach) || (completedCoachSessions() >= coachSessionRequirement(coach)
                && coachLevel() >= coachLevelRequirement(coach));
    }

    private void lockedCoachFeedback(View card, String coach) {
        card.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        float offset = dp(6);
        card.animate().cancel();
        card.animate().translationX(offset).setDuration(reducedMotion ? 0 : 45).withEndAction(() ->
                card.animate().translationX(-offset).setDuration(reducedMotion ? 0 : 55).withEndAction(() ->
                        card.animate().translationX(offset / 2f).setDuration(reducedMotion ? 0 : 45).withEndAction(() ->
                                card.animate().translationX(0f).setDuration(reducedMotion ? 0 : 45).start()).start()).start()).start();
        Toast.makeText(this, "Locked — " + Math.max(0, coachSessionRequirement(coach) - completedCoachSessions()) + " more completed sessions needed", Toast.LENGTH_SHORT).show();
    }

    private void showCoachCardDetails(CoachPreferences.CoachOption option) {
        String ability;
        String bestFor;
        String perk;
        if ("Mia".equals(option.name)) { ability = "Form Focus"; bestFor = "Confident technique"; perk = "+1 Form Streak point"; }
        else if ("Leo".equals(option.name)) { ability = "Strength Builder"; bestFor = "Steady strength"; perk = "Progression plans"; }
        else if ("Amy".equals(option.name)) { ability = "Consistency Streak"; bestFor = "Efficient weekly habits"; perk = "Streak challenges"; }
        else if ("Mark".equals(option.name)) { ability = "Power Progression"; bestFor = "Load, volume & tempo"; perk = "Progression plans"; }
        else if ("Claudia".equals(option.name)) { ability = "Precision Performance"; bestFor = "Balanced progression"; perk = "Split variety"; }
        else { ability = "Endurance Drive"; bestFor = "Conditioning intervals"; perk = "Endurance challenges"; }
        new AlertDialog.Builder(this)
                .setTitle("Coach " + option.name + " · " + coachTier(option.name))
                .setMessage(ability + "\n\nBest for: " + bestFor + "\nCoach perk: " + perk + "\n\nEvery plan stays adjustable to your pace and recovery.")
                .setPositiveButton("Choose Coach " + option.name, (dialog, which) -> {
                    AccountStore.updateCoachPreferences(this, option.name, profile().dialogueMode, profile().keepPhoneAwake);
                    LauncherIconManager.apply(this, option.name);
                    configureSpeechVoices();
                    refreshCurrentPageKeepingScroll(this::showCoachProfiles);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private String coachCardSpecialty(String coach) {
        if ("Amy".equals(coach)) return "Balanced fitness";
        if ("Claudia".equals(coach)) return "Form & mobility";
        if ("Leo".equals(coach)) return "Strength coach";
        if ("Mark".equals(coach)) return "Performance coach";
        if ("Stephen".equals(coach)) return "Steady progress";
        return "Spanish strength guide";
    }

    private String coachCardSession(String coach) { return "Leo".equals(coach) || "Mark".equals(coach) ? "40 min" : "30 min"; }
    private String coachCardFocus(String coach) { return "Claudia".equals(coach) ? "Mobility" : "Amy".equals(coach) || "Stephen".equals(coach) ? "Balance" : "Strength"; }
    private String coachCardPace(String coach) { return "Claudia".equals(coach) || "Stephen".equals(coach) ? "Gentle pace" : "Steady pace"; }

    private void addCoachChoice(LinearLayout body, CoachPreferences.CoachOption option, boolean selected) {
        boolean unlocked = coachIsUnlocked(option.name);
        LinearLayout choice = new LinearLayout(this);
        choice.setOrientation(LinearLayout.HORIZONTAL);
        choice.setGravity(Gravity.CENTER_VERTICAL);
        choice.setPadding(dp(12), dp(10), dp(12), dp(10));
        choice.setBackground(surface(unlocked ? paper : Color.rgb(224, 230, 235), 19, selected && unlocked ? 2 : 1, selected && unlocked ? mint : cardLine));
        choice.setElevation(dp(selected && unlocked ? 5 : 1));
        choice.setAlpha(unlocked ? 1f : .52f);
        ImageView photo = new ImageView(this);
        // Coach Hero imagery is the source of truth: every selection tile shows that coach's face.
        setCoachHeroFace(photo, option.name);
        photo.setBackground(surface(navyPanel, 99, 1, mint));
        photo.setClipToOutline(true);
        choice.addView(photo, new LinearLayout.LayoutParams(dp(68), dp(68)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(13), 0, 0, 0);
        copy.addView(heading("Coach " + option.name, 18, unlocked ? ink : muted));
        copy.addView(text(unlocked ? option.accent + " · " + option.gender : "Locked · " + coachUnlockRequirement(option.name), 14, muted));
        choice.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        if (unlocked) {
            TextView mark = heading(selected ? "✓" : "○", 19, selected ? navy : muted);
            mark.setGravity(Gravity.CENTER);
            mark.setBackground(surface(selected ? mint : Color.TRANSPARENT, 99, selected ? 0 : 1, muted));
            choice.addView(mark, new LinearLayout.LayoutParams(dp(34), dp(34)));
            choice.setClickable(true);
            choice.setFocusable(true);
            choice.setContentDescription("Coach " + option.name + ", " + option.accent + (selected ? ", selected" : ""));
            choice.setOnClickListener(view -> updateCoachSettings(option.name, profile().dialogueMode, profile().keepPhoneAwake));
        } else {
            ImageView lock = new ImageView(this);
            lock.setImageResource(R.drawable.ic_coach_lock);
            lock.setImageTintList(ColorStateList.valueOf(Color.rgb(81, 103, 120)));
            lock.setContentDescription("Locked");
            choice.addView(lock, new LinearLayout.LayoutParams(dp(28), dp(28)));
            choice.setContentDescription("Coach " + option.name + " is locked. " + coachUnlockRequirement(option.name));
        }
        body.addView(choice);
        gap(body, 8);
    }

    private void updateCoachSettings(String coach, String dialogueMode, boolean keepAwake) {
        stopSpeech();
        AccountStore.updateCoachPreferences(this, coach, dialogueMode, keepAwake);
        AccountStore.Profile updated = profile();
        selectedTrainer = updated.trainer;
        LauncherIconManager.apply(this, updated.coachName);
        applyKeepPhoneAwake(updated.keepPhoneAwake);
        configureSpeechVoices();
        refreshCurrentPageKeepingScroll(this::showCoachPreferences);
    }

    private void openVoiceSettings() {
        try {
            startActivity(new Intent("com.android.settings.TTS_SETTINGS"));
        } catch (RuntimeException unavailable) {
            startActivity(new Intent("android.settings.ACCESSIBILITY_SETTINGS"));
        }
    }

    /** Taskmaster-style organizer: listen first, then explicitly label the installed system voice. */
    private void showVoiceOrganizer() {
        stopSpeech();
        LinearLayout body = appPage("Settings");
        title(body, "VOICE ORGANIZER", "Preview & label voices", "Android voice names do not reliably identify gender. Listen to each installed offline voice, then label it Male, Female, or Unlabelled. Your labels stay on this phone.");

        List<Voice> installed = offlineEnglishVoices();
        if (!ttsReady || installed.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(heading("No offline English voices found", 18, ink));
            empty.addView(text("Install English voice data in Android’s Text-to-Speech settings, then return here to preview and label it.", 14, muted));
            gap(empty, 10);
            Button settings = secondary("Open Android voice settings");
            settings.setOnClickListener(view -> openVoiceSettings());
            addButton(empty, settings);
            body.addView(empty);
        } else {
            body.addView(chip(installed.size() + (installed.size() == 1 ? " OFFLINE VOICE" : " OFFLINE VOICES")));
            gap(body, 12);
            String engine = currentVoiceEngine();
            for (Voice candidate : installed) {
                LinearLayout item = card();
                String savedLabel = CoachVoiceLabels.label(this, engine, candidate.getName());
                CoachVoiceSelector.Gender detected = CoachVoiceSelector.detectGender(voiceCandidate(candidate, engine));
                String visibleLabel = savedLabel;
                if ("Not reviewed".equals(savedLabel) && detected != CoachVoiceSelector.Gender.UNKNOWN) {
                    visibleLabel = "Detected " + voiceGenderName(detected) + " · tap Label to confirm";
                }
                item.addView(heading(voiceLocaleLabel(candidate) + "  ·  " + visibleLabel, 17, ink));
                TextView technicalName = text(candidate.getName(), 12, muted);
                technicalName.setMaxLines(2);
                item.addView(technicalName);
                gap(item, 10);

                LinearLayout actions = new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                Button preview = lightAction("Listen");
                preview.setOnClickListener(view -> previewInstalledVoice(candidate));
                LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(0, dp(48), 1);
                previewParams.rightMargin = dp(7);
                actions.addView(preview, previewParams);
                Button label = secondary("Label voice");
                label.setOnClickListener(view -> labelInstalledVoice(candidate));
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, dp(48), 1);
                labelParams.leftMargin = dp(7);
                actions.addView(label, labelParams);
                item.addView(actions, new LinearLayout.LayoutParams(-1, dp(48)));
                body.addView(item);
                gap(body, 9);
            }
        }

        Button done = primary("Done — match my coach");
        done.setOnClickListener(view -> {
            configureSpeechVoices();
            showCoachCustomization();
        });
        addButton(body, done);
        gap(body, 8);
        Button back = lightAction("Back to coach settings");
        back.setOnClickListener(view -> showCoachCustomization());
        addButton(body, back);
    }

    private void labelInstalledVoice(Voice candidate) {
        previewInstalledVoice(candidate);
        new AlertDialog.Builder(this)
                .setTitle(voiceLocaleLabel(candidate))
                .setMessage("Listen to the sample, then label this voice. This choice is saved on this phone and used to keep male and female coaches distinct.")
                .setPositiveButton("Male", (dialog, which) -> saveVoiceLabel(candidate, CoachVoiceSelector.Gender.MALE))
                .setNegativeButton("Female", (dialog, which) -> saveVoiceLabel(candidate, CoachVoiceSelector.Gender.FEMALE))
                .setNeutralButton("Keep Unlabelled", (dialog, which) -> saveVoiceLabel(candidate, CoachVoiceSelector.Gender.UNKNOWN))
                .show();
    }

    private void saveVoiceLabel(Voice candidate, CoachVoiceSelector.Gender gender) {
        CoachVoiceLabels.set(this, currentVoiceEngine(), candidate.getName(), gender);
        configureSpeechVoices();
        toast("Voice labelled " + voiceGenderName(gender));
        showVoiceOrganizer();
    }

    private void previewInstalledVoice(Voice candidate) {
        if (!ttsReady || voice == null) {
            toast("Wait for speech to finish loading.");
            return;
        }
        stopSpeech();
        voice.setVoice(candidate);
        voice.setPitch(1f);
        voice.setSpeechRate(.92f);
        voice.speak("This is an installed voice preview. Listen, then choose the matching label.", TextToSpeech.QUEUE_FLUSH, null, "voice-label-preview-" + (++speechSequence));
    }

    private List<Voice> offlineEnglishVoices() {
        ArrayList<Voice> result = new ArrayList<>();
        if (voice == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return result;
        Set<Voice> available = voice.getVoices();
        if (available == null) return result;
        for (Voice candidate : available) {
            Locale locale = candidate.getLocale();
            Set<String> features = candidate.getFeatures();
            boolean missing = features != null && features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
            if (locale != null && "en".equalsIgnoreCase(locale.getLanguage()) && !candidate.isNetworkConnectionRequired() && !missing) result.add(candidate);
        }
        result.sort((first, second) -> {
            int localeOrder = voiceLocaleLabel(first).compareToIgnoreCase(voiceLocaleLabel(second));
            return localeOrder != 0 ? localeOrder : first.getName().compareToIgnoreCase(second.getName());
        });
        return result;
    }

    private String voiceLocaleLabel(Voice candidate) {
        Locale locale = candidate.getLocale();
        if (locale == null) return "Unknown language";
        String language = locale.getDisplayLanguage(Locale.ENGLISH);
        String country = locale.getDisplayCountry(Locale.ENGLISH);
        return country.isEmpty() ? language : language + " (" + country + ")";
    }

    private String currentVoiceEngine() {
        if (voice == null || voice.getDefaultEngine() == null) return "android-default";
        return voice.getDefaultEngine();
    }

    private String voiceGenderName(CoachVoiceSelector.Gender gender) {
        if (gender == CoachVoiceSelector.Gender.MALE) return "Male";
        if (gender == CoachVoiceSelector.Gender.FEMALE) return "Female";
        return "Unlabelled";
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
                refreshCurrentPageKeepingScroll(this::showPlanFocus);
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
                refreshCurrentPageKeepingScroll(this::showPlanContext);
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
                refreshCurrentPageKeepingScroll(this::showPlanContext);
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
        beginNewPage(); setSystemBars();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setBackgroundColor(navy);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(navy);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2)); setCoachHeroWizardShell(scroll, "Workouts");

        FrameLayout hero = coachHeroBackdrop("Workouts review"); root.addView(hero, new LinearLayout.LayoutParams(-1, dp(260)));
        LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = heading(planMode.toUpperCase(Locale.ROOT) + " PLAN · STEP 7", 10, mint); eyebrow.setLetterSpacing(.14f); copy.addView(eyebrow);
        TextView title = heading("Review your\nplan", 29, Color.WHITE); title.setLineSpacing(0, .93f); copy.addView(title);
        TextView subtitle = text("Your tailored session is ready. Check the details, then save and start when you are ready.", 13, Color.rgb(226, 236, 247));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(dp(270), -2); subtitleParams.topMargin = dp(8); copy.addView(subtitle, subtitleParams);
        FrameLayout.LayoutParams copyParams = new FrameLayout.LayoutParams(dp(274), -2, Gravity.LEFT | Gravity.TOP); copyParams.leftMargin = dp(20); copyParams.topMargin = dp(82); hero.addView(copy, copyParams);
        addWorkoutWizardProgress(hero, "Review");

        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(16), dp(20), dp(16), dp(36)); body.setBackgroundColor(paper); root.addView(body, new LinearLayout.LayoutParams(-1, -2));
        TextView section = heading("07  ·  REVIEW", 11, muted); section.setLetterSpacing(.12f); body.addView(section);
        body.addView(heading("Your plan is ready", 23, ink));
        body.addView(text("Review it before you begin. You can revisit these choices whenever you need.", 13, muted));
        gap(body, 12);
        body.addView(chip(goal + "  •  " + environment + "  •  " + sessionMinutes + " min"));
        gap(body, 14);
        for (int i = 0; i < session.size(); i++) {
            Coach.Exercise exercise = session.get(i);
            LinearLayout item = card();
            item.addView(heading(String.format(Locale.US, "%02d  %s", i + 1, exercise.name), 18, ink));
            item.addView(text(exercise.sets + " sets × " + exercise.reps + " reps  •  Set rest " + restChoiceLabel(exercise.restSeconds), 14, muted));
            item.addView(text(exercise.cue, 14, muted));
            body.addView(item);
            gap(body, 8);
        }
        Button save = action("Save and start this plan  →", coral, Color.WHITE);
        save.setOnClickListener(view -> showPlans());
        body.addView(save, new LinearLayout.LayoutParams(-1, dp(54)));
        Button back = new Button(this); back.setText("Back to set up"); back.setTextColor(mint); back.setTextSize(14); back.setAllCaps(false); back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(view -> showPlanSetup());
        body.addView(back, new LinearLayout.LayoutParams(-1, dp(46)));
    }

    // Guided session

    private void showWorkoutCountdown() {
        if (session.isEmpty()) {
            toast("Build a workout plan before starting a guided session.");
            return;
        }
        if (restTimer != null) restTimer.cancel();
        stopSetClock();
        stopSpeech();
        timingExerciseIndex = -1;
        setTracker = null;
        currentExerciseSetTimes.clear();
        completedExerciseSetTimes.clear();
        guidedWorkoutStartedAt = SystemClock.elapsedRealtime();
        guidedWorkoutAllowanceMillis = Math.max(1L, sessionMinutes) * 60_000L;
        guidedSetRestMillis = 0L;
        guidedCooldownMillis = 0L;
        currentExerciseSetRestMillis = 0L;
        skippedSetRestMillis = 0L;
        skippedCooldownMillis = 0L;
        recoverySegmentStartedAt = -1L;
        recoverySegmentIsCooldown = false;
        stopWorkoutRemainingClock();
        guidedSessionRecorded = false;
        guidedWorkoutAborted = false;
        showSetCountIn(0, 1, true);
    }

    private void showSetCountIn(int index, int set, boolean newExercise) {
        if (guidedWorkoutAborted) return;
        if (index >= session.size()) {
            showComplete();
            return;
        }
        String[] setOpenings = {
                "Okay, let's begin in 5, 4, 3, 2, 1.",
                "Let's start in 5, 4, 3, 2, 1.",
                "Let's go in 5, 4, 3, 2, 1.",
                "Let's get ready in 5, 4, 3, 2, 1."
        };
        String phrase = newExercise
                ? "Let's start our new exercise in 5, 4, 3, 2, 1."
                : setOpenings[openingPhraseIndex++ % setOpenings.length];
        String dialogueMode = profile().dialogueMode;
        boolean audioCountdown = CoachPreferences.usesVoice(dialogueMode);
        LinearLayout body = appPage("Workouts");
        addWorkoutRemainingHeader(body);
        title(body, newExercise ? "NEW EXERCISE" : "NEXT SET", "Visual countdown", "Your set timer begins when the countdown finishes.");
        LinearLayout countCard = card();
        TextView count = heading("READY", 46, ink);
        count.setGravity(Gravity.CENTER);
        countCard.addView(count, new LinearLayout.LayoutParams(-1, dp(96)));
        body.addView(countCard);
        gap(body, 12);
        LinearLayout note = card();
        note.addView(heading(newExercise ? session.get(index).name : "Set " + set + " of " + session.get(index).sets, 17, ink));
        note.addView(text("Complete the whole set, then tap Set completed. The app records one time for the full set.", 14, muted));
        body.addView(note);
        runWorkoutCountIn(phrase, count, () -> showActive(index, set));
    }

    private void showActive(int index, int set) {
        if (guidedWorkoutAborted) return;
        if (index >= session.size()) {
            showComplete();
            return;
        }
        if (restTimer != null) restTimer.cancel();
        stopSetClock();
        Coach.Exercise exercise = session.get(index);
        if (timingExerciseIndex != index || setTracker == null) {
            timingExerciseIndex = index;
            setTracker = new WorkoutTiming.SetTracker(exercise.restSeconds);
            currentExerciseSetTimes.clear();
            currentExerciseSetRestMillis = 0L;
        }
        LinearLayout body = appPage("Workouts");
        addWorkoutRemainingHeader(body);
        title(body, "GUIDED WORKOUT", exercise.target, "Exercise " + (index + 1) + " of " + session.size() + "  •  Set " + set + " of " + exercise.sets);

        LinearLayout demoCard = card();
        demoCard.setPadding(0, 0, 0, 0);
        demoCard.addView(exerciseMedia(AccountStore.get(this).trainer, exercise, 235), new LinearLayout.LayoutParams(-1, dp(235)));
        body.addView(demoCard);
        gap(body, 14);
        body.addView(heading(exercise.name, 25, Color.WHITE));
        body.addView(text("Set " + set + " of " + exercise.sets + "  •  Work with controlled form", 17, mint));
        gap(body, 10);

        LinearLayout cue = card();
        cue.addView(heading("Coach cue", 15, ink));
        cue.addView(text(exercise.cue, 16, muted));
        body.addView(cue);
        gap(body, 12);

        LinearLayout timing = card();
        TextView timerLabel = heading("CURRENT SET TIME", 11, muted);
        timerLabel.setLetterSpacing(.07f);
        timing.addView(timerLabel);
        TextView setClock = heading("00:00.0", 44, ink);
        setClock.setGravity(Gravity.CENTER);
        setClock.setContentDescription("Current set elapsed time zero seconds");
        timing.addView(setClock, new LinearLayout.LayoutParams(-1, dp(70)));
        TextView paceStatus = text("Timing set " + set + ". Tap completed when the whole set is finished.", 14, muted);
        paceStatus.setGravity(Gravity.CENTER);
        timing.addView(paceStatus);
        gap(timing, 8);
        TextView history = text(setHistoryText(exercise), 13, muted);
        history.setGravity(Gravity.CENTER);
        timing.addView(history);
        body.addView(timing);
        gap(body, 12);

        LinearLayout counters = new LinearLayout(this);
        counters.setOrientation(LinearLayout.HORIZONTAL);
        addCounterValue(counters, "SET", set + " / " + exercise.sets);
        TextView recoveryValue = addCounterValue(counters, "RECOVERY", format(setTracker.recoverySeconds()));
        body.addView(counters);
        gap(body, 14);
        final long setStartedAt = SystemClock.elapsedRealtime();
        setTracker.startSet(setStartedAt);
        startSetClock(setClock, setStartedAt);
        Button done = primary("Set " + set + " completed");
        done.setContentDescription("Mark set " + set + " completed and record its total time");
        done.setOnClickListener(view -> {
            done.setEnabled(false);
            stopSetClock();
            WorkoutTiming.SetResult result = setTracker.completeSet(SystemClock.elapsedRealtime());
            currentExerciseSetTimes.add(new SetTimeStore.SetEntry(result.setNumber, result.durationMillis));
            history.setText(setHistoryText(exercise));
            recoveryValue.setText(format(result.recoverySeconds));
            if (result.slowerThanRecentPace) {
                paceStatus.setText("This set took longer · recovery increased by 10 seconds.");
                recoveryValue.setTextColor(coral);
            } else {
                paceStatus.setText("Set " + set + " recorded at " + WorkoutTiming.formatElapsedMillis(result.durationMillis) + ".");
            }
            finishSet(index, set, exercise, result.recoverySeconds);
        });
        addButton(body, done);
        gap(body, 10);
        if (CoachPreferences.FULL_DIALOGUE.equals(profile().dialogueMode)) {
            Button callout = secondary("Hear set guidance");
            callout.setOnClickListener(view -> say("Set " + set + ". Work with controlled form. " + exercise.cue));
            addButton(body, callout);
            gap(body, 10);
        }
        Button emergency = emergencyStopButton();
        emergency.setOnClickListener(view -> showEmergencyStopConfirmation(index, set, () -> showActive(index, set)));
        addButton(body, emergency);
    }

    private TextView addCounterValue(LinearLayout row, String label, String value) {
        LinearLayout item = card();
        item.setPadding(dp(10), dp(11), dp(10), dp(11));
        TextView itemLabel = heading(label, 10, muted);
        itemLabel.setLetterSpacing(.06f);
        item.addView(itemLabel);
        TextView valueView = heading(value, 17, ink);
        item.addView(valueView);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.rightMargin = dp(6);
        row.addView(item, params);
        return valueView;
    }

    private void startSetClock(TextView clock, long startedAt) {
        stopSetClock();
        setClockUpdate = new Runnable() {
            @Override public void run() {
                long elapsed = SystemClock.elapsedRealtime() - startedAt;
                clock.setText(WorkoutTiming.formatElapsedMillis(elapsed));
                clock.setContentDescription("Current set elapsed time " + WorkoutTiming.formatElapsedMillis(elapsed));
                workoutHandler.postDelayed(this, 100L);
            }
        };
        workoutHandler.post(setClockUpdate);
    }

    private void stopSetClock() {
        if (setClockUpdate != null) workoutHandler.removeCallbacks(setClockUpdate);
        setClockUpdate = null;
    }

    /** Remaining planned workout allowance; pauses do not stop it and skipped recovery reduces it. */
    private long workoutRemainingMillis() {
        if (guidedWorkoutStartedAt <= 0L) return 0L;
        long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - guidedWorkoutStartedAt);
        long skipped = skippedCooldownMillis + skippedSetRestMillis;
        return Math.max(0L, guidedWorkoutAllowanceMillis - elapsed - skipped);
    }

    private String skippedTimerLabel(String prefix, long millis) {
        return prefix + " −" + WorkoutTiming.formatElapsedMillis(millis);
    }

    /** Fixed workout-time strip repeated on every live guided-workout screen. */
    private void addWorkoutRemainingHeader(LinearLayout body) {
        stopWorkoutRemainingClock();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        // The status strip intentionally blends into the page instead of becoming another tile.
        header.setPadding(0, dp(3), 0, dp(3));
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setProgressTintList(ColorStateList.valueOf(mint));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(45, 69, 96)));
        progress.setContentDescription("Workout time remaining");
        header.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));
        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        TextView remaining = heading("", 10, Color.WHITE);
        remaining.setGravity(Gravity.RIGHT);
        status.addView(remaining, new LinearLayout.LayoutParams(0, -2, 1));
        TextView cooldownSkip = workoutTimerChip(mint);
        LinearLayout.LayoutParams cooldownParams = new LinearLayout.LayoutParams(-2, dp(23));
        cooldownParams.leftMargin = dp(5);
        status.addView(cooldownSkip, cooldownParams);
        TextView restSkip = workoutTimerChip(coral);
        LinearLayout.LayoutParams restParams = new LinearLayout.LayoutParams(-2, dp(23));
        restParams.leftMargin = dp(5);
        status.addView(restSkip, restParams);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(5);
        header.addView(status, statusParams);
        body.addView(header);
        gap(body, 9);

        workoutRemainingUpdate = new Runnable() {
            @Override public void run() {
                long remainingMillis = workoutRemainingMillis();
                int fraction = guidedWorkoutAllowanceMillis <= 0L ? 0
                        : (int) Math.round(1000d * remainingMillis / guidedWorkoutAllowanceMillis);
                progress.setProgress(Math.max(0, Math.min(1000, fraction)));
                remaining.setText(WorkoutTiming.formatElapsedMillis(remainingMillis));
                cooldownSkip.setText(skippedTimerLabel("COOL", skippedCooldownMillis));
                restSkip.setText(skippedTimerLabel("REST", skippedSetRestMillis));
                progress.setContentDescription(WorkoutTiming.formatElapsedMillis(remainingMillis) + " remaining; "
                        + skippedTimerLabel("cooldown skipped", skippedCooldownMillis) + "; "
                        + skippedTimerLabel("rest skipped", skippedSetRestMillis));
                if (!guidedWorkoutAborted && !guidedSessionRecorded) workoutHandler.postDelayed(this, 250L);
            }
        };
        workoutHandler.post(workoutRemainingUpdate);
    }

    private TextView workoutTimerChip(int color) {
        TextView chip = heading("", 8, navy);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(7), 0, dp(7), 0);
        chip.setSingleLine(true);
        chip.setBackground(surface(color, 99, 0, Color.TRANSPARENT));
        return chip;
    }

    private void stopWorkoutRemainingClock() {
        if (workoutRemainingUpdate != null) workoutHandler.removeCallbacks(workoutRemainingUpdate);
        workoutRemainingUpdate = null;
    }

    private String setHistoryText(Coach.Exercise exercise) {
        SetTimeStore.ExerciseSummary summary = currentExerciseSummary(exercise);
        if (!summary.hasSetEntries()) return "Completed set times will appear here.";
        StringBuilder output = new StringBuilder("SET TIME RECORDS  •  FASTEST FIRST\n");
        for (int index = 0; index < summary.leaderboard.size(); index++) {
            SetTimeStore.SetEntry entry = summary.leaderboard.get(index);
            if (index > 0) output.append("   •   ");
            output.append("Set ").append(entry.setNumber).append(" ")
                    .append(WorkoutTiming.formatElapsedMillis(entry.durationMillis));
            if (summary.isLeaderboardLeader(entry)) output.append("  PB");
        }
        return output.toString();
    }

    private SetTimeStore.ExerciseSummary currentExerciseSummary(Coach.Exercise exercise) {
        return new SetTimeStore.ExerciseSummary(exercise.name, exercise.target, currentExerciseSetTimes);
    }

    /** Freezes the current exercise once, so only a normally completed workout can later persist it. */
    private SetTimeStore.ExerciseSummary completeExerciseTiming(int index, Coach.Exercise exercise) {
        if (index >= 0 && index < completedExerciseSetTimes.size()) return completedExerciseSetTimes.get(index);
        SetTimeStore.ExerciseSummary summary = currentExerciseSummary(exercise);
        if (index == completedExerciseSetTimes.size()) completedExerciseSetTimes.add(summary);
        return summary;
    }

    /** Leaderboard card used directly above Emergency stop and on each completed-exercise page. */
    private void addSetTimesCard(LinearLayout container, SetTimeStore.ExerciseSummary summary, boolean includeTotal) {
        LinearLayout card = card();
        TextView label = heading("SET TIMES", 11, muted);
        label.setLetterSpacing(.08f);
        card.addView(label);
        card.addView(text("Fastest first • active work time only", 13, muted));
        if (!summary.hasSetEntries()) {
            card.addView(text("Complete a set to add its time here.", 14, muted));
            container.addView(card);
            return;
        }
        gap(card, 8);
        for (SetTimeStore.SetEntry entry : summary.leaderboard) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(11), dp(9), dp(11), dp(9));
            boolean best = summary.isLeaderboardLeader(entry);
            row.setBackground(surface(best ? Color.rgb(226, 249, 243) : Color.rgb(240, 244, 249), 13, best ? 1 : 0, best ? mint : Color.TRANSPARENT));
            LinearLayout setLabel = new LinearLayout(this);
            setLabel.setOrientation(LinearLayout.HORIZONTAL);
            setLabel.setGravity(Gravity.CENTER_VERTICAL);
            TextView set = heading("Set " + entry.setNumber, 15, ink);
            setLabel.addView(set, new LinearLayout.LayoutParams(-2, -2));
            if (best) {
                TextView star = heading("★", 20, coral);
                star.setContentDescription("Fastest set");
                LinearLayout.LayoutParams starParams = new LinearLayout.LayoutParams(-2, -2);
                starParams.leftMargin = dp(4);
                setLabel.addView(star, starParams);
                row.setElevation(dp(5));
            }
            row.addView(setLabel, new LinearLayout.LayoutParams(0, -2, 1));
            TextView duration = heading(WorkoutTiming.formatElapsedMillis(entry.durationMillis), 16, ink);
            duration.setGravity(Gravity.END);
            row.addView(duration, new LinearLayout.LayoutParams(dp(104), -2));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.topMargin = dp(6);
            card.addView(row, rowParams);
        }
        if (includeTotal) {
            gap(card, 10);
            LinearLayout total = new LinearLayout(this);
            total.setGravity(Gravity.CENTER_VERTICAL);
            TextView totalLabel = heading("TOTAL ACTIVE SET TIME", 11, muted);
            totalLabel.setLetterSpacing(.06f);
            total.addView(totalLabel, new LinearLayout.LayoutParams(0, -2, 1));
            total.addView(heading(WorkoutTiming.formatElapsedMillis(summary.totalActiveMillis), 18, ink));
            card.addView(total);
        }
        container.addView(card);
    }

    /** A per-exercise roll-up shown during the following cooldown, without changing its leaderboard. */
    private TextView[] addTotalExerciseCard(LinearLayout container, Coach.Exercise currentExercise) {
        long activeMillis = currentExercise == null ? 0L : currentExerciseSummary(currentExercise).totalActiveMillis;
        long setRestMillis = currentExerciseSetRestMillis;
        long totalMillis = activeMillis + setRestMillis;
        LinearLayout card = card();
        TextView label = heading("TOTAL EXERCISE", 11, muted);
        label.setLetterSpacing(.08f);
        card.addView(label);
        card.addView(text("Completed work and set rest for this exercise", 13, muted));
        gap(card, 8);
        TextView active = addWorkoutTotalRow(card, "Active set time", WorkoutTiming.formatElapsedMillis(activeMillis), false);
        TextView rest = addWorkoutTotalRow(card, "Set rest total", WorkoutTiming.formatElapsedMillis(setRestMillis), false);
        gap(card, 5);
        TextView grand = addWorkoutTotalRow(card, "GRAND TOTAL", WorkoutTiming.formatElapsedMillis(totalMillis), true);
        container.addView(card);
        return new TextView[]{active, rest, grand};
    }

    private TextView addWorkoutTotalRow(LinearLayout container, String label, String value, boolean grandTotal) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(11), dp(9), dp(11), dp(9));
        row.setBackground(surface(grandTotal ? Color.rgb(226, 249, 243) : Color.rgb(240, 244, 249), 13,
                grandTotal ? 1 : 0, grandTotal ? mint : Color.TRANSPARENT));
        TextView name = heading(label, grandTotal ? 13 : 14, grandTotal ? ink : muted);
        if (grandTotal) name.setLetterSpacing(.04f);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView duration = heading(value, grandTotal ? 18 : 16, ink);
        duration.setGravity(Gravity.END);
        row.addView(duration, new LinearLayout.LayoutParams(dp(112), -2));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(6);
        container.addView(row, rowParams);
        return duration;
    }

    private void updateTotalExerciseViews(TextView[] views, long activeMillis) {
        if (views == null || views.length < 3) return;
        long setRestMillis = currentExerciseSetRestMillis;
        views[0].setText(WorkoutTiming.formatElapsedMillis(activeMillis));
        views[1].setText(WorkoutTiming.formatElapsedMillis(setRestMillis));
        views[2].setText(WorkoutTiming.formatElapsedMillis(activeMillis + setRestMillis));
    }

    private long activeRecoverySegmentMillis() {
        return recoverySegmentStartedAt < 0L ? 0L : Math.max(0L, SystemClock.elapsedRealtime() - recoverySegmentStartedAt);
    }

    private void beginRecoverySegment(boolean cooldown) {
        if (recoverySegmentStartedAt < 0L) {
            recoverySegmentStartedAt = SystemClock.elapsedRealtime();
            recoverySegmentIsCooldown = cooldown;
        }
    }

    private void endRecoverySegment() {
        if (recoverySegmentStartedAt < 0L) return;
        if (recoverySegmentIsCooldown) guidedCooldownMillis += activeRecoverySegmentMillis();
        else {
            guidedSetRestMillis += activeRecoverySegmentMillis();
            currentExerciseSetRestMillis += activeRecoverySegmentMillis();
        }
        recoverySegmentStartedAt = -1L;
        recoverySegmentIsCooldown = false;
    }

    private void showExerciseSetSummary(int index, Coach.Exercise exercise) {
        if (guidedWorkoutAborted) return;
        stopSetClock();
        SetTimeStore.ExerciseSummary summary = completeExerciseTiming(index, exercise);
        boolean hasNextExercise = index + 1 < session.size();
        LinearLayout body = appPage("Workouts");
        addWorkoutRemainingHeader(body);
        title(body, "EXERCISE COMPLETE", exercise.name, "Your set leaderboard is ordered from fastest to slowest. The total below includes work-set time only, not rest or cooldown.");
        addSetTimesCard(body, summary, true);
        gap(body, 14);
        Button next = primary(hasNextExercise ? "Begin cooldown" : "Finish guided workout");
        next.setOnClickListener(view -> {
            if (hasNextExercise) showRest(index, exercise.sets, exercise, planCooldownSeconds, true, true);
            else showComplete();
        });
        addButton(body, next);
        gap(body, 9);
        Button emergency = emergencyStopButton();
        emergency.setOnClickListener(view -> showEmergencyStopConfirmation(index, exercise.sets,
                () -> showExerciseSetSummary(index, exercise)));
        addButton(body, emergency);
    }

    private void finishSet(int index, int set, Coach.Exercise exercise, int recoverySeconds) {
        if (guidedWorkoutAborted) return;
        stopSetClock();
        boolean exerciseComplete = set >= exercise.sets;
        boolean hasNextExercise = index + 1 < session.size();
        String completion;
        if (exerciseComplete && hasNextExercise) {
            completion = "Great, you have completed this exercise. Cooldown before you begin next exercise.";
        } else if (exerciseComplete) {
            completion = "Great, you have completed this exercise.";
        } else {
            String[] completions = {
                    "Congrats, you have completed this set. Time to take a rest.",
                    "Good work, you have completed this set. Time to take a rest.",
                    "This set is complete. Well done. Time to take a rest."
            };
            completion = completions[completionPhraseIndex++ % completions.length];
        }
        Runnable next = () -> continueAfterCompletedSet(index, set, exercise, recoverySeconds);
        if (CoachPreferences.usesCompletionDialogue(profile().dialogueMode)) {
            speakCoach(completion, TextToSpeech.QUEUE_FLUSH, next);
        } else {
            next.run();
        }
    }

    private void continueAfterCompletedSet(int index, int set, Coach.Exercise exercise, int recoverySeconds) {
        if (guidedWorkoutAborted) return;
        if (set >= exercise.sets) {
            completeExerciseTiming(index, exercise);
            if (index + 1 < session.size()) showRest(index, set, exercise, planCooldownSeconds, true, true);
            else showExerciseSetSummary(index, exercise);
            return;
        }
        showRest(index, set, exercise, recoverySeconds, true, false);
    }

    private void showRest(int index, int set, Coach.Exercise exercise, int secondsRemaining, boolean running, boolean betweenExercises) {
        if (guidedWorkoutAborted) return;
        stopSetClock();
        LinearLayout body = appPage("Workouts");
        addWorkoutRemainingHeader(body);
        title(body, betweenExercises ? "COOLDOWN TIMER" : "REST TIMER", betweenExercises ? "Cooldown before next exercise" : "Set complete · recover", betweenExercises ? "Your " + restChoiceLabel(planCooldownSeconds) + " exercise cooldown is separate from set rest. The next exercise begins after its visual count-in." : "Your custom set rest is " + restChoiceLabel(secondsRemaining) + ". Your next set begins after the visual count-in.");
        LinearLayout timerCard = card();
        TextView timer = heading(format(secondsRemaining), 52, ink);
        timer.setGravity(Gravity.CENTER);
        timer.setContentDescription((betweenExercises ? "Cooldown" : "Rest") + " time remaining " + secondsRemaining + " seconds");
        timerCard.addView(timer, new LinearLayout.LayoutParams(-1, dp(98)));
        TextView reminder = text(running ? "Breathe slowly. Your 5-to-1 count-in begins when recovery is complete." : "Countdown paused. Resume when you are ready.", 15, muted);
        reminder.setGravity(Gravity.CENTER);
        timerCard.addView(reminder);
        body.addView(timerCard);
        gap(body, 14);
        final int[] remaining = {secondsRemaining};
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        Button pause = running ? secondary("Pause") : action("Resume", mint, navy);
        pause.setContentDescription((running ? "Pause " : "Resume ") + (betweenExercises ? "cooldown" : "rest") + " countdown");
        pause.setOnClickListener(view -> {
            if (restTimer != null) restTimer.cancel();
            endRecoverySegment();
            stopSpeech();
            showRest(index, set, exercise, remaining[0], !running, betweenExercises);
        });
        LinearLayout.LayoutParams pauseParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        pauseParams.rightMargin = dp(7);
        controls.addView(pause, pauseParams);
        Button skip = lightAction(betweenExercises ? "Skip cooldown" : "Skip rest");
        skip.setOnClickListener(view -> {
            if (restTimer != null) restTimer.cancel();
            long skippedMillis = Math.max(0L, remaining[0]) * 1_000L;
            if (betweenExercises) skippedCooldownMillis += skippedMillis;
            else skippedSetRestMillis += skippedMillis;
            endRecoverySegment();
            stopSpeech();
            advance(index, set, exercise);
        });
        controls.addView(skip, new LinearLayout.LayoutParams(0, dp(52), 1));
        body.addView(controls);
        gap(body, 12);
        addSetTimesCard(body, currentExerciseSummary(exercise), false);
        gap(body, 12);
        final long activeSetMillis = currentExerciseSummary(exercise).totalActiveMillis;
        final TextView[] totalExercise;
        if (betweenExercises) {
            totalExercise = addTotalExerciseCard(body, exercise);
            gap(body, 12);
        } else {
            totalExercise = null;
        }
        Button emergency = emergencyStopButton();
        emergency.setOnClickListener(view -> showEmergencyStopConfirmation(index, set,
                () -> showRest(index, set, exercise, remaining[0], true, betweenExercises)));
        addButton(body, emergency);
        if (!running) return;
        beginRecoverySegment(betweenExercises);
        restTimer = new CountDownTimer(secondsRemaining * 1000L, 1000) {
            @Override public void onTick(long left) {
                int seconds = (int) Math.ceil(left / 1000d);
                remaining[0] = seconds;
                timer.setText(format(seconds));
                timer.setContentDescription((betweenExercises ? "Cooldown" : "Rest") + " time remaining " + seconds + " seconds");
                updateTotalExerciseViews(totalExercise, activeSetMillis);
            }

            @Override public void onFinish() {
                endRecoverySegment();
                timer.setText("00:00");
                updateTotalExerciseViews(totalExercise, activeSetMillis);
                advance(index, set, exercise);
            }
        }.start();
    }

    private Button emergencyStopButton() {
        Button button = action("Emergency stop", navy, coral);
        button.setBackground(surface(navy, 14, 1, coral));
        button.setTextSize(14);
        button.setContentDescription("Emergency stop. Ends the current guided workout after confirmation.");
        return button;
    }

    private void showEmergencyStopConfirmation(int index, int set, Runnable continueAction) {
        if (restTimer != null) restTimer.cancel();
        endRecoverySegment();
        stopSetClock();
        stopSpeech();
        LinearLayout body = appPage("Workouts");
        title(body, "EMERGENCY STOP", "End this guided\nworkout?", "This stops the current session. Your saved Daily or Weekly plan will remain available to restart later.");
        LinearLayout notice = card();
        notice.addView(heading("Your progress in this session will not be marked complete.", 17, ink));
        notice.addView(text("If you feel pain, dizziness, or unwell, stop safely and seek appropriate help.", 14, muted));
        body.addView(notice);
        gap(body, 16);
        Button end = primary("End guided workout");
        end.setOnClickListener(view -> {
            guidedWorkoutAborted = true;
            timingExerciseIndex = -1;
            setTracker = null;
            currentExerciseSetTimes.clear();
            completedExerciseSetTimes.clear();
            guidedSetRestMillis = 0L;
            guidedCooldownMillis = 0L;
            skippedSetRestMillis = 0L;
            skippedCooldownMillis = 0L;
            recoverySegmentStartedAt = -1L;
            recoverySegmentIsCooldown = false;
            stopWorkoutRemainingClock();
            showHome();
        });
        addButton(body, end);
        gap(body, 10);
        Button continueWorkout = secondary("Keep working out");
        continueWorkout.setOnClickListener(view -> continueAction.run());
        addButton(body, continueWorkout);
    }

    private void advance(int index, int set, Coach.Exercise exercise) {
        if (guidedWorkoutAborted) return;
        if (set < exercise.sets) showSetCountIn(index, set + 1, false);
        else showSetCountIn(index + 1, 1, true);
    }

    private String format(int seconds) {
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private String restChoiceLabel(int seconds) {
        int safe = WorkoutTiming.clampRestSeconds(seconds);
        int minutes = safe / 60;
        int remainder = safe % 60;
        if (minutes == 0) return remainder + " sec";
        if (remainder == 0) return minutes + (minutes == 1 ? " min" : " min");
        return minutes + " min " + remainder + " sec";
    }

    private void say(String words) {
        if (CoachPreferences.FULL_DIALOGUE.equals(profile().dialogueMode)) speakCoach(words, TextToSpeech.QUEUE_FLUSH, null);
    }

    /**
     * A visible five-to-one count-in is used before every guided workout set.
     *
     * The numeral is updated from TextToSpeech.onStart, not from a guessed
     * delay. That keeps the animation in lockstep with the word the member
     * hears, even when a device takes a moment to start speaking.
     */
    private void runWorkoutCountIn(String phrase, TextView countView, Runnable done) {
        String mode = profile().dialogueMode;
        stopSpeech();
        final long visualGeneration = speechGeneration;
        Runnable beginCountdown = () -> {
            if (visualGeneration != speechGeneration) return;
            if (CoachPreferences.usesBeeps(mode) && toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 72);
            }
            for (int index = 0; index < 5; index++) {
                final int remaining = 5 - index;
                final int delay = index * 1000;
                workoutHandler.postDelayed(() -> {
                    if (visualGeneration != speechGeneration) return;
                    Runnable showNumber = () -> {
                        if (visualGeneration != speechGeneration) return;
                        countView.setText(String.valueOf(remaining));
                        if (CoachPreferences.usesBeeps(mode) && toneGenerator != null) {
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 150);
                        }
                    };
                    if (CoachPreferences.usesVoice(mode)) {
                        speakCoachWithStart(String.valueOf(remaining), TextToSpeech.QUEUE_ADD, showNumber, null);
                    } else {
                        showNumber.run();
                    }
                }, delay);
            }
            workoutHandler.postDelayed(() -> {
                if (visualGeneration != speechGeneration) return;
                Runnable beginWorkout = () -> {
                    if (visualGeneration != speechGeneration) return;
                    countView.setText("GO");
                    if (CoachPreferences.usesBeeps(mode) && toneGenerator != null) {
                        toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 230);
                    }
                    done.run();
                };
                if (CoachPreferences.usesVoice(mode)) {
                    speakCoachWithStart("Let's go!", TextToSpeech.QUEUE_ADD, beginWorkout, null);
                } else {
                    beginWorkout.run();
                }
            }, 5000L);
        };
        if (CoachPreferences.FULL_DIALOGUE.equals(mode)) {
            speakCoach(countdownLeadIn(phrase), TextToSpeech.QUEUE_FLUSH, beginCountdown);
        } else {
            beginCountdown.run();
        }
    }

    /** Removes only the spoken numerals; they are voiced one-at-a-time in sync with the UI. */
    private String countdownLeadIn(String phrase) {
        String leadIn = phrase.replaceAll("(?i)\\s*in\\s*5\\s*,\\s*4\\s*,\\s*3\\s*,\\s*2\\s*,\\s*1\\s*\\.?$", ".").trim();
        return leadIn.length() == 0 ? "Get ready." : leadIn;
    }

    private void configureSpeechVoices() {
        if (voice == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        List<Voice> installed = offlineEnglishVoices();
        String activeCoach = AccountStore.exists(this) ? profile().coachName : CoachPreferences.defaultCoach(selectedTrainer);
        CoachPreferences.CoachOption option = CoachPreferences.coach(activeCoach);
        voice.setLanguage(option.locale);
        coachSpeechRate = "Spanish accent".equals(option.accent) ? .90f : "US accent".equals(option.accent) ? .95f : .92f;
        boolean male = "Male".equals(option.gender);
        coachSpeechPitch = male ? .86f : 1.08f;
        coachTtsVoice = voice.getVoice();
        coachVoiceVerified = false;
        if (installed.isEmpty()) {
            coachTtsVoice = null;
            coachVoiceStatus = "No offline English voice is installed. Open Android voice settings and install a " + option.gender.toLowerCase(Locale.ROOT) + " " + option.accent.replace(" accent", "") + " voice.";
            return;
        }
        ArrayList<CoachVoiceSelector.Candidate> candidates = new ArrayList<>();
        Map<String, Voice> voicesByName = new HashMap<>();
        String engine = currentVoiceEngine();
        for (Voice candidate : installed) {
            candidates.add(voiceCandidate(candidate, engine));
            voicesByName.put(candidate.getName(), candidate);
        }
        CoachVoiceSelector.Selection selection = CoachVoiceSelector.select(candidates, option);
        if (selection == null) {
            coachTtsVoice = null;
            coachVoiceStatus = "No installed voice is labelled " + option.gender + ". Use Preview & label installed voices to assign one; the wrong-gender voice will never be substituted.";
            return;
        }
        coachTtsVoice = voicesByName.get(selection.candidate.id);
        coachVoiceVerified = selection.genderMatched;
        if (coachVoiceVerified) coachSpeechPitch = 1f;
        String gender = option.gender.toLowerCase(Locale.ROOT);
        String accent = option.accent.replace(" accent", "");
        if (selection.exactMatch()) {
            coachVoiceStatus = "Coach " + option.name + " is matched to an installed " + gender + " " + accent + " voice.";
        } else if (selection.genderMatched) {
            coachVoiceStatus = "Coach " + option.name + " is using an installed " + gender + " English voice. Install a " + gender + " " + accent + " voice for an exact accent match.";
        } else {
            coachTtsVoice = null;
            coachVoiceStatus = "Coach " + option.name + " has no verified " + gender + " voice yet. Use Preview & label installed voices; coach speech stays off until a matching voice is confirmed.";
        }
    }

    private CoachVoiceSelector.Candidate voiceCandidate(Voice candidate, String engine) {
        return new CoachVoiceSelector.Candidate(candidate.getName(), candidate.getLocale(), candidate.getFeatures(),
                candidate.getQuality(), candidate.getLatency(), candidate.isNetworkConnectionRequired(),
                CoachVoiceLabels.assigned(this, engine, candidate.getName()));
    }

    private void speakCoach(String words, int queueMode, Runnable done) {
        if (!coachVoiceVerified || coachTtsVoice == null) {
            final long generation = speechGeneration;
            if (done != null) workoutHandler.postDelayed(() -> {
                if (generation == speechGeneration) done.run();
            }, 350L);
            return;
        }
        speakWithVoice(words, queueMode, coachTtsVoice, done);
    }

    /** Speaks an utterance and runs {@code started} at the exact audio start event. */
    private void speakCoachWithStart(String words, int queueMode, Runnable started, Runnable done) {
        if (!coachVoiceVerified || coachTtsVoice == null) {
            final long generation = speechGeneration;
            workoutHandler.post(() -> {
                if (generation == speechGeneration && started != null) started.run();
            });
            if (done != null) workoutHandler.postDelayed(() -> {
                if (generation == speechGeneration) done.run();
            }, 350L);
            return;
        }
        speakWithVoice(words, queueMode, coachTtsVoice, started, done);
    }

    private void speakWithVoice(String words, int queueMode, Voice selectedVoice, Runnable done) {
        speakWithVoice(words, queueMode, selectedVoice, null, done);
    }

    private void speakWithVoice(String words, int queueMode, Voice selectedVoice, Runnable started, Runnable done) {
        if (queueMode == TextToSpeech.QUEUE_FLUSH) {
            speechGeneration++;
            speechStartCallbacks.clear();
            speechDoneCallbacks.clear();
        }
        final long generation = speechGeneration;
        if (!ttsReady || voice == null) {
            if (done != null) workoutHandler.postDelayed(() -> {
                if (generation == speechGeneration) done.run();
            }, Math.max(900L, words.length() * 55L));
            return;
        }
        if (selectedVoice != null) voice.setVoice(selectedVoice);
        voice.setPitch(coachSpeechPitch);
        voice.setSpeechRate(coachSpeechRate);
        String utteranceId = "speech-" + (++speechSequence);
        if (started != null) speechStartCallbacks.put(utteranceId, started);
        if (done != null) speechDoneCallbacks.put(utteranceId, done);
        int result = voice.speak(words, queueMode, null, utteranceId);
        if (result == TextToSpeech.ERROR) {
            Runnable start = speechStartCallbacks.remove(utteranceId);
            speechDoneCallbacks.remove(utteranceId);
            if (start != null) workoutHandler.post(() -> {
                if (generation == speechGeneration) start.run();
            });
            if (done != null) workoutHandler.postDelayed(() -> {
                if (generation == speechGeneration) done.run();
            }, 900L);
        }
    }

    private void beginSpeech(String utteranceId) {
        workoutHandler.post(() -> {
            Runnable started = speechStartCallbacks.remove(utteranceId);
            if (started != null) started.run();
        });
    }

    private void failSpeech(String utteranceId) {
        workoutHandler.post(() -> {
            Runnable started = speechStartCallbacks.remove(utteranceId);
            if (started != null) started.run();
            Runnable done = speechDoneCallbacks.remove(utteranceId);
            if (done != null) done.run();
        });
    }

    private void completeSpeech(String utteranceId) {
        workoutHandler.post(() -> {
            Runnable done = speechDoneCallbacks.remove(utteranceId);
            if (done != null) done.run();
        });
    }

    private void stopSpeech() {
        speechGeneration++;
        speechStartCallbacks.clear();
        speechDoneCallbacks.clear();
        if (voice != null) voice.stop();
    }

    private void stopGuidedRuntime() {
        // Leaving a live guided screen is an abandonment, never a completed workout. This also
        // invalidates delayed count-in callbacks before they can write Progress data.
        if (guidedWorkoutStartedAt > 0L && !guidedSessionRecorded) guidedWorkoutAborted = true;
        if (restTimer != null) restTimer.cancel();
        endRecoverySegment();
        stopWorkoutRemainingClock();
        restTimer = null;
        stopSetClock();
        stopSpeech();
        stopEnduranceGuided();
    }

    // Library, progress and account utilities

    private void showLibrary() {
        String trainer = AccountStore.get(this).trainer;
        String trainerName = coachName();
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
        guidance.addView(text(exercise.sets + " sets × " + exercise.reps + " reps • Set rest " + restChoiceLabel(exercise.restSeconds), 15, muted));
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
                    refreshCurrentPageKeepingScroll(this::showWeightTracker);
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
                    refreshCurrentPageKeepingScroll(this::showWeightTracker);
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

    private RadioButton profileGenderOption(String label, boolean checked) {
        RadioButton option = new RadioButton(this);
        option.setId(View.generateViewId());
        option.setText(label);
        option.setTextColor(Color.WHITE);
        option.setTextSize(14);
        option.setButtonTintList(ColorStateList.valueOf(mint));
        option.setChecked(checked);
        return option;
    }

    private void showMeals() {
        LinearLayout body = appPage("Nutrition");
        title(body, "TODAY'S FUEL", "Simple food ideas, connected", "Save flexible meal ideas and check in on everyday habits. Progress shows the pattern beside your training consistency.");
        JourneyStore.Snapshot snapshot = JourneyStore.snapshot(this);

        LinearLayout mealRegion = card();
        mealRegion.addView(heading("Today’s meal region", 19, ink));
        mealRegion.addView(text("Your daily breakfast and lunch rotation is set to " + NutritionRegionStore.region(this) + ". It changes only when you choose another rotation.", 13, muted));
        gap(mealRegion, 8);
        Button regionButton = lightAction(NutritionRegionStore.region(this) + "  ·  Change");
        regionButton.setOnClickListener(view -> showNutritionRegionPicker());
        addButton(mealRegion, regionButton);
        body.addView(mealRegion);
        gap(body, 12);

        if (!session.isEmpty()) {
            LinearLayout connection = card();
            connection.addView(heading("Today’s movement: " + target + "  •  " + sessionMinutes + " min", 19, ink));
            connection.addView(text("Goal: " + goal + "  •  Equipment: " + equipmentSummary(), 14, muted));
            gap(connection, 7);
            connection.addView(text("Keep food choices familiar, practical, and suited to your appetite, culture, budget, allergies, and any clinical guidance.", 13, muted));
            body.addView(connection);
            gap(body, 12);
        }

        NutritionRecommendation.Day todayFood = todayNutritionDay();
        LinearLayout todayIdeas = card();
        todayIdeas.addView(heading("Today’s breakfast + lunch", 19, ink));
        todayIdeas.addView(text("Fresh ideas rotate each day. Save any idea you want to revisit; none are calorie targets or medical advice.", 13, muted));
        gap(todayIdeas, 8);
        addMealSuggestion(todayIdeas, "Breakfast", todayFood.breakfast);
        gap(todayIdeas, 7);
        addMealSuggestion(todayIdeas, "Lunch", todayFood.lunch);
        gap(todayIdeas, 10);
        addButton(todayIdeas, saveMealIdeaButton("Save breakfast idea", todayFood.breakfastTitle));
        gap(todayIdeas, 7);
        addButton(todayIdeas, saveMealIdeaButton("Save lunch idea", todayFood.lunchTitle));
        body.addView(todayIdeas);
        gap(body, 12);

        LinearLayout caribbean = card();
        caribbean.addView(heading("Caribbean Food Library", 19, ink));
        caribbean.addView(text(CaribbeanFoodLibrary.count() + " Caribbean ingredients, prepared dishes, street foods, bakery items, and cooking basics • Searchable reference facts per 100 g.", 14, muted));
        caribbean.addView(text("Single-food values are reference data. Prepared dishes are typical recipe estimates; seasoning, oil, salt, brand, and serving size can change the result.", 12, muted));
        gap(caribbean, 9);
        Button browseCaribbean = primary("Browse Caribbean foods");
        browseCaribbean.setOnClickListener(view -> showCaribbeanFoodLibrary("", "All"));
        addButton(caribbean, browseCaribbean);
        body.addView(caribbean);
        gap(body, 12);

        LinearLayout eatingOut = card();
        eatingOut.addView(heading("Eating out", 19, ink));
        eatingOut.addView(text("Simple ways to make a more balanced choice at familiar fast-food restaurants. Local menus and preparation can differ.", 13, muted));
        gap(eatingOut, 9);
        Button openEatingOut = lightAction("Browse smart fast-food choices");
        openEatingOut.setOnClickListener(view -> showEatingOutGuide());
        addButton(eatingOut, openEatingOut);
        body.addView(eatingOut);
        gap(body, 12);

        List<String> plannedMeals = MealPlanStore.meals(this);
        if (!plannedMeals.isEmpty()) {
            LinearLayout savedMeals = card();
            savedMeals.addView(heading("Saved food ideas", 19, ink));
            savedMeals.addView(text("Your locally saved meal ideas. Edit this list whenever your week changes.", 13, muted));
            for (String meal : plannedMeals) {
                gap(savedMeals, 7);
                Button remove = lightAction("✓  " + meal + "   Remove");
                remove.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                remove.setContentDescription("Remove " + meal + " from my meal plan");
                remove.setOnClickListener(view -> { MealPlanStore.remove(this, meal); refreshCurrentPageKeepingScroll(this::showMeals); });
                addButton(savedMeals, remove);
            }
            body.addView(savedMeals);
            gap(body, 12);
        }

        LinearLayout checkIn = card();
        checkIn.addView(heading("Today's simple fuel check-in", 19, ink));
        checkIn.addView(text(snapshot.todayFuelGoals() + " of 3 checked  •  Progress counts this as one day when you check any goal.", 13, muted));
        gap(checkIn, 9);
        addButton(checkIn, fuelGoalButton("Protein food included", JourneyStore.FUEL_PROTEIN, snapshot.todayFuelMask));
        gap(checkIn, 7);
        addButton(checkIn, fuelGoalButton("Fruit or colourful vegetables included", JourneyStore.FUEL_PLANTS, snapshot.todayFuelMask));
        gap(checkIn, 7);
        addButton(checkIn, fuelGoalButton("Hydration checked", JourneyStore.FUEL_HYDRATION, snapshot.todayFuelMask));
        body.addView(checkIn);
        gap(body, 12);

        if (!session.isEmpty()) {
            LinearLayout timing = card();
            timing.addView(heading("Fuel around your workout", 18, ink));
            timing.addView(text("Before: choose familiar food and fluids that feel comfortable before your " + sessionMinutes + "-minute session.", 14, muted));
            gap(timing, 5);
            timing.addView(text("After: return to a balanced meal or snack with a protein food, produce, and a grain or other carbohydrate you enjoy.", 14, muted));
            if (sessionMinutes >= 90) {
                gap(timing, 5);
                timing.addView(text("Longer session: plan access to fluids and avoid experimenting with unfamiliar foods during training.", 14, muted));
            }
            body.addView(timing);
            gap(body, 14);

            body.addView(heading("Two ideas for " + goal, 20, Color.WHITE));
            gap(body, 8);
            int ideaCount = 0;
            for (String meal : Coach.meals(goal)) {
                if (ideaCount++ == 2) break;
                LinearLayout mealCard = card();
                mealCard.addView(heading(meal, 18, ink));
                mealCard.addView(text("Adapt ingredients and portions to appetite, culture, budget, allergies, and medical guidance.", 14, muted));
                gap(mealCard, 8);
                addButton(mealCard, saveMealIdeaButton("Save this idea", meal));
                body.addView(mealCard);
                gap(body, 9);
            }
        }
        gap(body, 5);
        Button workout = primary(session.isEmpty() ? "Build a connected workout" : "Open connected workout");
        workout.setOnClickListener(view -> { if (session.isEmpty()) showPlans(); else showPlanReady(); });
        addButton(body, workout);
        gap(body, 8);
        Button progress = secondary("See workout + nutrition progress");
        progress.setOnClickListener(view -> showProgress());
        addButton(body, progress);
        gap(body, 12);
        body.addView(text("These are general wellness ideas, not medical nutrition treatment. For allergies, medical conditions, eating concerns, pregnancy, or specific weight-change targets, consult a qualified clinician or registered dietitian.", 13, Color.rgb(211, 225, 233)));
    }

    private void showEatingOutGuide() {
        LinearLayout body = appPage("Nutrition");
        title(body, "EATING OUT", "Smart fast-food choices", "Use this as a practical starting point, not a strict meal plan. Choose what is available locally and check the restaurant’s current nutrition information when you can.");

        addEatingOutRestaurant(body, "Subway", "6-inch chicken, turkey, or veggie sub", "Choose whole-grain bread when available, load up vegetables, and use mustard or vinegar instead of creamy sauces. Water or an unsweetened drink keeps the meal simpler.", "Watch the footlong size, extra cheese, cookies, and mayonnaise-based sauces.");
        addEatingOutRestaurant(body, "KFC", "A smaller chicken portion with a lighter side", "Choose grilled or roasted chicken if your restaurant offers it. Otherwise, keep the fried portion modest and choose corn, beans, or salad where available. Choose water.", "Watch fries, biscuits, sweet drinks, extra sauces, and sodium in fried or seasoned items.");
        addEatingOutRestaurant(body, "Burger King", "Single burger or grilled chicken option", "Keep the protein serving single, add vegetables, and ask for no cheese or mayonnaise if that fits your goal. Choose a lighter side or skip the combo upgrade.", "Watch double patties, bacon, creamy sauces, large fries, shakes, and sugary drinks.");
        addEatingOutRestaurant(body, "McDonald’s", "A basic protein choice with a light side", "A hamburger, small nuggets, or an egg-based breakfast item can be paired with apple slices or a side salad if available. Choose water or unsweetened coffee.", "Watch large combo sizing, fries, shakes, desserts, and sugary drinks.");

        LinearLayout note = card();
        note.addView(heading("Quick rule", 18, ink));
        note.addView(text("Prioritize a modest protein portion, vegetables when available, a lighter sauce, and water. Fast food can fit occasionally; portion size and sodium often matter as much as calories.", 14, muted));
        body.addView(note); gap(body, 10);
        body.addView(text("Menu recipes, portions, allergens, and nutrition values vary by restaurant and country. Check your local restaurant’s current nutrition information for exact values.", 12, Color.rgb(211, 225, 233)));
        gap(body, 10);
        Button back = secondary("Back to Nutrition"); back.setOnClickListener(view -> showMeals()); addButton(body, back);
    }

    private void addEatingOutRestaurant(LinearLayout body, String restaurant, String choice, String order, String watch) {
        LinearLayout card = card();
        card.addView(heading(restaurant, 20, ink));
        card.addView(heading("Better starting choice", 12, mint));
        card.addView(heading(choice, 16, navy));
        gap(card, 5);
        card.addView(text(order, 13, muted));
        gap(card, 5);
        card.addView(text("Consider limiting: " + watch, 12, Color.rgb(104, 78, 77)));
        body.addView(card); gap(body, 9);
    }

    private void showNutritionRegionPicker() {
        String[] regions = {NutritionRegionStore.TRINIDAD_TOBAGO, NutritionRegionStore.BALANCED};
        int selected = NutritionRegionStore.TRINIDAD_TOBAGO.equals(NutritionRegionStore.region(this)) ? 0 : 1;
        new AlertDialog.Builder(this)
                .setTitle("Choose meal rotation")
                .setSingleChoiceItems(regions, selected, (dialog, which) -> {
                    NutritionRegionStore.setRegion(this, regions[which]);
                    dialog.dismiss();
                    showMeals();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Button saveMealIdeaButton(String label, String meal) {
        boolean saved = MealPlanStore.meals(this).contains(meal);
        Button button = lightAction(saved ? "✓  Saved to my ideas" : "+  " + label);
        button.setContentDescription((saved ? "Saved " : "Save ") + meal);
        button.setOnClickListener(view -> {
            MealPlanStore.add(this, meal);
            toast("Saved to your local meal ideas.");
            showMeals();
        });
        return button;
    }

    private void showCaribbeanFoodLibrary(String query, String selectedCategory) {
        LinearLayout body = appPage("Nutrition");
        title(body, "CARIBBEAN FOOD LIBRARY", "Familiar dishes, clear reference facts", "Search ingredients, traditional meals, street foods, and bakery items per 100 g. Home recipes, brands, cooking methods, oil, and salt can change the result.");

        EditText search = input("Search: pelau, doubles, callaloo, snapper…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        search.setText(query);
        body.addView(search);
        gap(body, 8);
        Button runSearch = primary("Search foods");
        runSearch.setOnClickListener(view -> showCaribbeanFoodLibrary(search.getText().toString().trim(), "All"));
        addButton(body, runSearch);
        gap(body, 12);

        body.addView(heading("Browse by group", 18, Color.WHITE));
        HorizontalScrollView categoriesScroll = new HorizontalScrollView(this);
        categoriesScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout categories = new LinearLayout(this);
        categories.setOrientation(LinearLayout.HORIZONTAL);
        categoriesScroll.addView(categories);
        for (String category : CaribbeanFoodLibrary.CATEGORIES) {
            Button categoryButton = action((category.equals(selectedCategory) ? "✓  " : "") + category,
                    category.equals(selectedCategory) ? mint : Color.rgb(28, 64, 79),
                    category.equals(selectedCategory) ? navy : Color.WHITE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(40));
            params.rightMargin = dp(7);
            categoryButton.setOnClickListener(view -> showCaribbeanFoodLibrary("", category));
            categories.addView(categoryButton, params);
        }
        body.addView(categoriesScroll, new LinearLayout.LayoutParams(-1, dp(46)));
        gap(body, 12);

        List<CaribbeanFoodLibrary.Food> foods = CaribbeanFoodLibrary.search(query, selectedCategory);
        body.addView(text(foods.size() + " foods shown  •  kcal, protein, carbohydrate, fibre, fat, and sodium per 100 g", 13, Color.rgb(211, 225, 233)));
        gap(body, 8);
        for (CaribbeanFoodLibrary.Food food : foods) {
            LinearLayout foodCard = card();
            foodCard.addView(heading(food.name, 18, ink));
            foodCard.addView(text(food.category + "  •  " + food.preparation, 13, muted));
            gap(foodCard, 7);
            foodCard.addView(text(food.kcal + " kcal  •  Protein " + food.protein + " g  •  Carbs " + food.carbs + " g", 14, ink));
            foodCard.addView(text("Fibre " + food.fibre + " g  •  Fat " + food.fat + " g  •  Sodium " + food.sodium + " mg", 13, muted));
            if (!food.note.isEmpty()) foodCard.addView(text(food.note, 12, muted));
            body.addView(foodCard);
            gap(body, 8);
        }
        if (foods.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(heading("No matching food yet", 18, ink));
            empty.addView(text("Try a familiar name, an ingredient, another spelling, or a food group. The library includes both common foods and recipe-based dishes from across the Caribbean.", 14, muted));
            body.addView(empty);
        }
        gap(body, 10);
        body.addView(text("Sources: Caribbean Food and Nutrition Institute composition tables, indexed by FAO/INFOODS, and USDA FoodData Central reference datasets. Prepared-dish entries are rounded typical recipe estimates; they are not a substitute for a product label or clinical nutrition advice.", 12, Color.rgb(211, 225, 233)));
        gap(body, 10);
        Button back = secondary("Back to Today’s Fuel");
        back.setOnClickListener(view -> showMeals());
        addButton(body, back);
    }

    /**
     * A deliberate route-planning surface. Maps owns map data and navigation; this app only keeps
     * the plan the person chose and never requests their location in the background.
     */
    private void showEndurance() {
        if (manualEnduranceMode != null) {
            showManualEnduranceSession();
            return;
        }
        if (LiveTrackingStore.current(this).active) {
            showLiveTrackingSession();
            return;
        }
        // Endurance starts at the same first decision as Workouts: plan type.
        // Activity selection follows on the next wizard page.
        startEnduranceWizard(EndurancePlanStore.WALKING);
    }

    /** Three-step counterpart to the workout wizard, tailored for route-based movement. */
    private void startEnduranceWizard(String mode) {
        EndurancePlanStore.Plan existing = EndurancePlanStore.load(this, mode);
        enduranceWizardMode = mode;
        enduranceWizardName = existing == null ? mode + " route" : existing.name;
        enduranceWizardOrigin = existing == null ? "" : existing.origin;
        enduranceWizardDestination = existing == null ? "" : existing.destination;
        enduranceWizardDistanceKm = existing == null ? defaultDistance(mode) : existing.distanceKm;
        enduranceWizardMinutes = existing == null ? defaultMinutes(mode) : existing.minutes;
        enduranceWizardEffort = existing == null ? "Steady" : existing.effort;
        showEnduranceWizardPlanType();
    }

    /** Endurance follows the workout wizard rhythm, without workout-only steps. */
    private void showEnduranceWizardPlanType() {
        beginNewPage();
        setSystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(navy);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(navy);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        setCoachHeroWizardShell(scroll, "Endurance");

        // This is intentionally the same Coach Hero composition as Workouts.
        FrameLayout hero = coachHeroBackdrop("Endurance");
        root.addView(hero, new LinearLayout.LayoutParams(-1, dp(388)));
        String[] stepNames = {"PLAN", "ACTIVITY", "SCHEDULE", "ROUTE", "REVIEW"};
        addCoachHeroWizardSteps(hero, stepNames, 0);

        LinearLayout heroCopy = new LinearLayout(this);
        heroCopy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = heading("COACH " + coachName().toUpperCase(Locale.ROOT), 11, mint);
        eyebrow.setLetterSpacing(.16f);
        heroCopy.addView(eyebrow);
        TextView title = heading("Plan your\nendurance", 34, Color.WHITE);
        title.setLineSpacing(0, .92f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(dp(240), -2);
        titleParams.topMargin = dp(11);
        heroCopy.addView(title, titleParams);
        TextView intro = text("Choose your schedule and activity. I’ll shape a route and session around you.", 15, Color.rgb(222, 233, 245));
        intro.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(dp(235), -2);
        introParams.topMargin = dp(12);
        heroCopy.addView(intro, introParams);
        FrameLayout.LayoutParams heroCopyParams = new FrameLayout.LayoutParams(dp(248), -2, Gravity.LEFT | Gravity.TOP);
        heroCopyParams.leftMargin = dp(20);
        heroCopyParams.topMargin = dp(98);
        hero.addView(heroCopy, heroCopyParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(20), dp(16), dp(34));
        content.setBackgroundColor(paper);
        root.addView(content, new LinearLayout.LayoutParams(-1, -2));
        TextView planEyebrow = heading("01  ·  PLAN TYPE", 11, muted);
        planEyebrow.setLetterSpacing(.12f);
        content.addView(planEyebrow);
        content.addView(heading("How do you want to plan?", 23, ink));
        content.addView(text("Select the structure that fits your week.", 13, muted));
        gap(content, 13);
        LinearLayout planOptions = new LinearLayout(this);
        planOptions.setGravity(Gravity.CENTER_VERTICAL);
        planOptions.addView(coachHeroEndurancePlanOption("Weekly", "Build a routine", R.drawable.ic_plan_weekly), new LinearLayout.LayoutParams(0, dp(104), 1));
        LinearLayout.LayoutParams dailyParams = new LinearLayout.LayoutParams(0, dp(104), 1);
        dailyParams.leftMargin = dp(8);
        planOptions.addView(coachHeroEndurancePlanOption("Daily", "One focused day", R.drawable.ic_plan_daily), dailyParams);
        LinearLayout.LayoutParams customParams = new LinearLayout.LayoutParams(0, dp(104), 1);
        customParams.leftMargin = dp(8);
        planOptions.addView(coachHeroEndurancePlanOption("Custom", "Every detail", R.drawable.ic_plan_custom), customParams);
        content.addView(planOptions);
        gap(content, 18);
        LinearLayout hint = new LinearLayout(this);
        hint.setOrientation(LinearLayout.VERTICAL);
        hint.setPadding(dp(15), dp(14), dp(15), dp(14));
        hint.setBackground(surface(Color.rgb(225, 255, 246), 17, 0, Color.TRANSPARENT));
        hint.addView(heading("One decision at a time", 16, ink));
        hint.addView(text("Next, choose walking, running, or cycling before placing it on your calendar.", 12, muted));
        content.addView(hint);
        gap(content, 16);
        Button next = action("Next: choose activity  →", coral, Color.WHITE);
        next.setOnClickListener(view -> showEnduranceWizardActivity());
        content.addView(next, new LinearLayout.LayoutParams(-1, dp(54)));
    }

    private void addCoachHeroWizardSteps(FrameLayout hero, String[] stepNames, int activeIndex) {
        LinearLayout steps = new LinearLayout(this);
        steps.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < stepNames.length; i++) {
            boolean complete = i < activeIndex;
            boolean active = i == activeIndex;
            TextView marker = heading(complete ? "✓" : String.valueOf(i + 1), 12, (complete || active) ? navy : Color.WHITE);
            marker.setGravity(Gravity.CENTER);
            marker.setBackground(surface(complete ? mint : active ? coral : Color.TRANSPARENT, 99, (complete || active) ? 0 : 1, Color.rgb(170, 193, 220)));
            steps.addView(marker, new LinearLayout.LayoutParams(dp(28), dp(28)));
            if (i < stepNames.length - 1) {
                View connector = new View(this);
                connector.setBackgroundColor(i < activeIndex ? mint : Color.rgb(127, 150, 184));
                LinearLayout.LayoutParams connectorParams = new LinearLayout.LayoutParams(0, dp(2), 1);
                connectorParams.leftMargin = dp(5);
                connectorParams.rightMargin = dp(5);
                steps.addView(connector, connectorParams);
            }
        }
        FrameLayout.LayoutParams stepsParams = new FrameLayout.LayoutParams(dp(310), dp(32), Gravity.LEFT | Gravity.TOP);
        stepsParams.leftMargin = dp(18);
        stepsParams.topMargin = dp(12);
        hero.addView(steps, stepsParams);
        LinearLayout labels = new LinearLayout(this);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < stepNames.length; i++) {
            TextView name = heading(stepNames[i].toUpperCase(Locale.ROOT), 7, i == activeIndex ? Color.WHITE : Color.rgb(219, 229, 242));
            name.setGravity(Gravity.CENTER);
            labels.addView(name, new LinearLayout.LayoutParams(0, dp(16), 1));
        }
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(dp(310), dp(16), Gravity.LEFT | Gravity.TOP);
        labelParams.leftMargin = dp(18);
        labelParams.topMargin = dp(43);
        hero.addView(labels, labelParams);
    }

    private LinearLayout coachHeroEndurancePlanOption(String mode, String detail, int iconRes) {
        boolean selected = mode.equals(endurancePlanMode);
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(6), dp(9), dp(6), dp(7));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setBackground(surface(selected ? Color.rgb(255, 242, 239) : Color.WHITE, 17, selected ? 2 : 1, selected ? coral : cardLine));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(selected ? coral : navy));
        tile.addView(icon, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView title = heading(mode, 13, ink);
        title.setGravity(Gravity.CENTER);
        tile.addView(title, new LinearLayout.LayoutParams(-1, dp(20)));
        TextView caption = text(detail, 9, muted);
        caption.setGravity(Gravity.CENTER);
        caption.setMaxLines(2);
        tile.addView(caption, new LinearLayout.LayoutParams(-1, dp(24)));
        tile.setOnClickListener(view -> {
            endurancePlanMode = mode;
            refreshCurrentPageKeepingScroll(this::showEnduranceWizardPlanType);
        });
        applyPressMotion(tile);
        return tile;
    }

    /**
     * Full-bleed shell shared by every Endurance wizard step.  This deliberately mirrors
     * the Workout wizard: the linear progress rail sits at the very top of the hero, above
     * the page copy, while the controls use the whole paper-width below it.
     */
    private LinearLayout enduranceWizardPage(int step, String title, String subtitle) {
        beginNewPage(); setSystemBars();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setBackgroundColor(navy);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(navy);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2)); setCoachHeroWizardShell(scroll, "Endurance");

        FrameLayout hero = coachHeroBackdrop("Endurance step " + step); root.addView(hero, new LinearLayout.LayoutParams(-1, dp(260)));
        String[] labels = {"PLAN", "ACTIVITY", "SCHEDULE", "ROUTE", "REVIEW"};
        addCoachHeroWizardSteps(hero, labels, step - 1);
        LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = heading("PLAN YOUR ENDURANCE · STEP " + step, 10, mint); eyebrow.setLetterSpacing(.14f); copy.addView(eyebrow);
        TextView heading = heading(title, 29, Color.WHITE); heading.setLineSpacing(0, .93f); copy.addView(heading);
        TextView detail = text(subtitle, 13, Color.rgb(226, 236, 247)); LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(dp(270), -2); detailParams.topMargin = dp(8); copy.addView(detail, detailParams);
        FrameLayout.LayoutParams copyParams = new FrameLayout.LayoutParams(dp(274), -2, Gravity.LEFT | Gravity.TOP); copyParams.leftMargin = dp(20); copyParams.topMargin = dp(82); hero.addView(copy, copyParams);

        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(16), dp(20), dp(16), dp(36)); content.setBackgroundColor(paper); root.addView(content, new LinearLayout.LayoutParams(-1, -2));
        TextView section = heading(String.format(Locale.US, "%02d  ·  %s", step, labels[step - 1]), 11, muted); section.setLetterSpacing(.12f); content.addView(section);
        return content;
    }

    private void showEnduranceWizardActivity() {
        LinearLayout body = enduranceWizardPage(2, "Choose your\nactivity", "Build a simple outdoor or indoor session. Use live tracking, an external map, or a timer-only session.");
        body.addView(heading("Move your way", 23, ink));
        body.addView(text("Pick one activity for this endurance plan.", 13, muted));
        gap(body, 14);
        body.addView(enduranceModeTile(EndurancePlanStore.WALKING, "Walking", "Low-impact distance and pace")); gap(body, 8);
        body.addView(enduranceModeTile(EndurancePlanStore.RUNNING, "Running", "Pace-focused route or treadmill")); gap(body, 8);
        body.addView(enduranceModeTile(EndurancePlanStore.CYCLING, "Cycling", "Road, bike path, or indoor bike")); gap(body, 14);
        Button next = action("Next: choose schedule  →", coral, Color.WHITE); next.setOnClickListener(view -> showEnduranceWizardSchedule()); body.addView(next, new LinearLayout.LayoutParams(-1, dp(54)));
        Button back = new Button(this); back.setText("Back"); back.setTextColor(mint); back.setTextSize(14); back.setAllCaps(false); back.setBackgroundColor(Color.TRANSPARENT); back.setOnClickListener(view -> showEnduranceWizardPlanType()); body.addView(back, new LinearLayout.LayoutParams(-1, dp(46)));
    }

    private int enduranceActivityIcon(String mode) {
        if (EndurancePlanStore.RUNNING.equals(mode)) return R.drawable.ic_endurance_running;
        if (EndurancePlanStore.CYCLING.equals(mode)) return R.drawable.ic_endurance_cycling;
        return R.drawable.ic_endurance_walking;
    }

    private LinearLayout enduranceModeTile(String mode, String label, String copy) {
        boolean selected = mode.equals(enduranceWizardMode);
        LinearLayout tile = new LinearLayout(this); tile.setGravity(Gravity.CENTER_VERTICAL); tile.setPadding(dp(14), dp(13), dp(14), dp(13));
        tile.setBackground(surface(paper, 18, selected ? 2 : 1, selected ? mint : cardLine)); tile.setClickable(true); tile.setFocusable(true);
        ImageView icon = new ImageView(this); icon.setImageResource(enduranceActivityIcon(mode)); icon.setImageTintList(ColorStateList.valueOf(navy)); icon.setPadding(dp(11), dp(11), dp(11), dp(11)); icon.setBackground(surface(mint, 99, 0, Color.TRANSPARENT)); tile.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout copyColumn = new LinearLayout(this); copyColumn.setOrientation(LinearLayout.VERTICAL); copyColumn.addView(heading(label, 18, ink)); copyColumn.addView(text(copy, 13, muted));
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1); copyParams.leftMargin = dp(12); tile.addView(copyColumn, copyParams);
        if (selected) { TextView check = heading("✓", 18, navy); check.setGravity(Gravity.CENTER); check.setBackground(surface(mint, 99, 0, Color.TRANSPARENT)); tile.addView(check, new LinearLayout.LayoutParams(dp(32), dp(32))); }
        tile.setOnClickListener(view -> { enduranceWizardMode = mode; refreshCurrentPageKeepingScroll(this::showEnduranceWizardActivity); });
        return tile;
    }

    /** Schedule step shared by Daily and Weekly endurance plans. */
    private void showEnduranceWizardSchedule() {
        boolean weekly = "Weekly".equals(endurancePlanMode);
        LinearLayout body = enduranceWizardPage(3, weekly ? "Choose your\ntraining week" : "Choose your\nactivity day", weekly ? "Tap days to add or remove an endurance session. Rest days stay clear." : "Choose one day for this " + enduranceAction(enduranceWizardMode).toLowerCase(Locale.ROOT) + " session.");
        body.addView(heading(weekly ? "Place your sessions" : "Choose your day", 23, ink));
        body.addView(text(weekly ? "Build a balanced week with recovery left visible." : "Your selected activity will appear on this day across your plan.", 13, muted));
        gap(body, 14);
        addEndurancePremiumCalendar(body, weekly);
        if (weekly) { int count=0; for(boolean selected:enduranceWorkoutDays)if(selected)count++; LinearLayout rhythm=card(); rhythm.addView(heading(count+" endurance sessions",19,ink)); rhythm.addView(text("Use the days above to place walking, running, or cycling without changing your strength plan.",13,muted)); body.addView(rhythm); gap(body,12); }
        Button next=action("Next: set up route  →", coral, Color.WHITE); next.setOnClickListener(v->{if(weekly){boolean any=false;for(boolean selected:enduranceWorkoutDays)if(selected)any=true;if(!any){toast("Choose at least one endurance day.");return;}} showEnduranceWizardRoute();}); body.addView(next, new LinearLayout.LayoutParams(-1, dp(54))); Button back=new Button(this); back.setText("Back"); back.setTextColor(mint); back.setTextSize(14); back.setAllCaps(false); back.setBackgroundColor(Color.TRANSPARENT); back.setOnClickListener(v->showEnduranceWizardActivity()); body.addView(back, new LinearLayout.LayoutParams(-1, dp(46)));
    }

    /** Uses the same editorial timeline as the Workout planner, with endurance-specific activity icons. */
    private void addEndurancePremiumCalendar(LinearLayout body, boolean weekly) {
        int sessions = 0; for (boolean selected : enduranceWorkoutDays) if (selected) sessions++;
        LinearLayout calendar = new LinearLayout(this); calendar.setOrientation(LinearLayout.VERTICAL);
        calendar.setPadding(dp(16), dp(16), dp(16), dp(16));
        calendar.setBackground(surface(Color.rgb(10, 29, 50), 24, 1, Color.rgb(41, 83, 115)));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout activityBadge = new FrameLayout(this); activityBadge.setBackground(surface(Color.rgb(17, 62, 80), 99, 1, mint));
        ImageView glyph = new ImageView(this); glyph.setImageResource(enduranceActivityIcon(enduranceWizardMode)); glyph.setImageTintList(ColorStateList.valueOf(mint)); glyph.setPadding(dp(7), dp(7), dp(7), dp(7)); activityBadge.addView(glyph, new FrameLayout.LayoutParams(-1, -1));
        header.addView(activityBadge, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView weekTitle = heading(selectedWeekTitle().replace("Week of ", ""), 18, Color.WHITE); weekTitle.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams weekParams = new LinearLayout.LayoutParams(0, dp(42), 1); weekParams.leftMargin = dp(8); header.addView(weekTitle, weekParams);
        LinearLayout meta = new LinearLayout(this); meta.setOrientation(LinearLayout.VERTICAL); meta.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView count = heading(weekly ? sessions + " sessions" : "1 session", 10, Color.WHITE); count.setGravity(Gravity.RIGHT); meta.addView(count, new LinearLayout.LayoutParams(-1, dp(17)));
        LinearLayout meter = new LinearLayout(this); meter.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        int filled = weekly ? Math.min(5, Math.max(1, sessions)) : 1;
        for (int part = 0; part < 5; part++) { View bar = new View(this); bar.setBackground(surface(part < filled ? mint : Color.rgb(49, 77, 108), 99, 0, Color.TRANSPARENT)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(18), dp(6)); if (part > 0) p.leftMargin = dp(4); meter.addView(bar, p); }
        meta.addView(meter, new LinearLayout.LayoutParams(-1, dp(13))); header.addView(meta, new LinearLayout.LayoutParams(dp(104), dp(38))); calendar.addView(header);

        LinearLayout days = new LinearLayout(this); days.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < weekDays.length; i++) {
            final int day = i; boolean selected = weekly ? enduranceWorkoutDays[i] : enduranceDailyDay == i;
            LinearLayout item = new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER); item.setClickable(true); item.setFocusable(true); item.setPadding(dp(1), dp(4), dp(1), dp(2));
            item.addView(heading(weekDays[i], 8, selected ? mint : Color.rgb(180, 200, 220)), new LinearLayout.LayoutParams(-1, dp(16)));
            TextView date = heading(String.valueOf(selectedWeekDate(i).get(Calendar.DAY_OF_MONTH)), 14, Color.WHITE); date.setGravity(Gravity.CENTER); item.addView(date, new LinearLayout.LayoutParams(-1, dp(23)));
            View dot = new View(this); dot.setBackground(surface(selected ? mint : Color.rgb(66, 91, 121), 99, selected ? 2 : 1, selected ? Color.rgb(159, 255, 231) : Color.rgb(101, 128, 159))); LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(selected ? 18 : 13), dp(selected ? 18 : 13)); dotParams.gravity = Gravity.CENTER; dotParams.topMargin = dp(2); item.addView(dot, dotParams);
            item.setOnClickListener(v -> { if (weekly) enduranceWorkoutDays[day] = !enduranceWorkoutDays[day]; else enduranceDailyDay = day; refreshCurrentPageKeepingScroll(this::showEnduranceWizardSchedule); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(65), 1); if (i > 0) p.leftMargin = dp(2); days.addView(item, p);
        }
        LinearLayout.LayoutParams daysParams = new LinearLayout.LayoutParams(-1, dp(65)); daysParams.topMargin = dp(7); calendar.addView(days, daysParams);
        View timeline = new View(this); timeline.setBackgroundColor(Color.rgb(65, 111, 143)); LinearLayout.LayoutParams timelineParams = new LinearLayout.LayoutParams(-1, dp(1)); timelineParams.leftMargin = dp(9); timelineParams.rightMargin = dp(9); timelineParams.topMargin = dp(-9); calendar.addView(timeline, timelineParams);
        LinearLayout cards = new LinearLayout(this); cards.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < weekDays.length; i++) {
            final int day = i; boolean selected = weekly ? enduranceWorkoutDays[i] : enduranceDailyDay == i;
            LinearLayout detail = new LinearLayout(this); detail.setOrientation(LinearLayout.VERTICAL); detail.setGravity(Gravity.CENTER); detail.setClickable(true); detail.setPadding(dp(2), dp(7), dp(2), dp(6)); detail.setBackground(surface(selected ? Color.rgb(18, 63, 81) : Color.rgb(20, 46, 72), 10, selected ? 1 : 0, selected ? mint : Color.TRANSPARENT));
            ImageView icon = new ImageView(this); icon.setImageResource(selected ? enduranceActivityIcon(enduranceWizardMode) : R.drawable.ic_home_rest); icon.setImageTintList(ColorStateList.valueOf(selected ? mint : Color.rgb(154, 178, 205))); icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE); LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(-1, dp(22)); iconParams.leftMargin = dp(6); iconParams.rightMargin = dp(6); detail.addView(icon, iconParams);
            TextView label = heading(selected ? appointmentAbbreviation(enduranceWizardMode) : "Rest", 9, selected ? Color.WHITE : Color.rgb(180, 200, 220)); label.setGravity(Gravity.CENTER); label.setSingleLine(true); detail.addView(label, new LinearLayout.LayoutParams(-1, dp(19)));
            TextView note = text(selected ? "Planned" : "Recover", 8, selected ? Color.rgb(180, 235, 222) : Color.rgb(132, 159, 188)); note.setGravity(Gravity.CENTER); detail.addView(note, new LinearLayout.LayoutParams(-1, dp(17)));
            detail.setOnClickListener(v -> { if (weekly) enduranceWorkoutDays[day] = !enduranceWorkoutDays[day]; else enduranceDailyDay = day; refreshCurrentPageKeepingScroll(this::showEnduranceWizardSchedule); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(68), 1); if (i > 0) p.leftMargin = dp(3); cards.addView(detail, p);
        }
        LinearLayout.LayoutParams cardsParams = new LinearLayout.LayoutParams(-1, dp(68)); cardsParams.topMargin = dp(12); calendar.addView(cards, cardsParams);
        body.addView(calendar); gap(body, 12);
    }

    private void showEnduranceWizardRoute() {
        LinearLayout body = enduranceWizardPage(4, "Set your route\nand time", "Use Google Maps for a planned route, or leave the route blank for an indoor session.");
        body.addView(heading("Route details", 23, ink));
        body.addView(text("Set a clear name, distance, and duration for your " + enduranceAction(enduranceWizardMode).toLowerCase(Locale.ROOT) + ".", 13, muted));
        gap(body, 14);
        EditText name = input("Session name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); name.setText(enduranceWizardName); body.addView(name); gap(body, 8);
        EditText origin = input("Starting point (optional)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); origin.setText(enduranceWizardOrigin); body.addView(origin); gap(body, 8);
        EditText destination = input("Destination or loop landmark (optional)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); destination.setText(enduranceWizardDestination); body.addView(destination); gap(body, 8);
        EditText distance = input("Target distance in km", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); distance.setText(String.valueOf(enduranceWizardDistanceKm)); body.addView(distance); gap(body, 8);
        EditText minutes = input("Target minutes", InputType.TYPE_CLASS_NUMBER); minutes.setText(String.valueOf(enduranceWizardMinutes)); body.addView(minutes); gap(body, 10);
        body.addView(heading("Effort", 17, ink));
        RadioGroup effort = new RadioGroup(this); effort.setOrientation(RadioGroup.HORIZONTAL); addEnduranceEffort(effort, "Relaxed", enduranceWizardEffort); addEnduranceEffort(effort, "Steady", enduranceWizardEffort); addEnduranceEffort(effort, "Challenging", enduranceWizardEffort);
        for (int index = 0; index < effort.getChildCount(); index++) ((RadioButton) effort.getChildAt(index)).setTextColor(ink);
        body.addView(effort); gap(body, 13);
        Button maps = action("Open route in Google Maps", navyPanel, mint);
        maps.setOnClickListener(view -> openEnduranceMap(enduranceWizardMode, origin.getText().toString(), destination.getText().toString()));
        body.addView(maps, new LinearLayout.LayoutParams(-1, dp(52))); gap(body, 8);
        Button next = action("Next: review plan  →", coral, Color.WHITE);
        next.setOnClickListener(view -> {
            try {
                enduranceWizardName = name.getText().toString().trim(); enduranceWizardOrigin = origin.getText().toString().trim(); enduranceWizardDestination = destination.getText().toString().trim();
                enduranceWizardDistanceKm = Float.parseFloat(distance.getText().toString().trim()); enduranceWizardMinutes = Integer.parseInt(minutes.getText().toString().trim());
                int checked = effort.getCheckedRadioButtonId(); enduranceWizardEffort = checked == View.NO_ID ? "Steady" : ((RadioButton) effort.findViewById(checked)).getText().toString();
                if (enduranceWizardName.isEmpty()) { name.setError("Add a session name"); return; }
                if (enduranceWizardDistanceKm <= 0f || enduranceWizardMinutes < 5) throw new NumberFormatException();
                showEnduranceWizardReview();
            } catch (NumberFormatException error) { toast("Enter a positive distance and at least 5 minutes."); }
        }); body.addView(next, new LinearLayout.LayoutParams(-1, dp(54)));
        Button back = new Button(this); back.setText("Back"); back.setTextColor(mint); back.setTextSize(14); back.setAllCaps(false); back.setBackgroundColor(Color.TRANSPARENT); back.setOnClickListener(view -> showEnduranceWizardSchedule()); body.addView(back, new LinearLayout.LayoutParams(-1, dp(46)));
    }

    private void showEnduranceWizardReview() {
        LinearLayout body = enduranceWizardPage(5, "Review your\n" + enduranceAction(enduranceWizardMode), "Open your route if you need it, then start the in-app guided timer when you are ready.");
        body.addView(heading("Ready to move", 23, ink));
        body.addView(text("Your endurance plan is ready to save, track, or start right away.", 13, muted));
        gap(body, 14);
        LinearLayout review = card();
        review.addView(heading(appointmentAbbreviation(enduranceWizardMode) + " · " + enduranceWizardName, 20, ink));
        review.addView(text(formatDistance(enduranceWizardDistanceKm) + "  •  " + enduranceWizardMinutes + " min  •  " + enduranceWizardEffort, 14, muted));
        review.addView(text(enduranceWizardDestination.isEmpty() ? "Indoor or open route" : "Destination: " + enduranceWizardDestination, 13, muted)); body.addView(review); gap(body, 13);
        Button maps = action("Open route in Google Maps", navyPanel, mint);
        maps.setOnClickListener(view -> openEnduranceMap(enduranceWizardMode, enduranceWizardOrigin, enduranceWizardDestination));
        body.addView(maps, new LinearLayout.LayoutParams(-1, dp(52))); gap(body, 8);
        Button start = action("Start " + appointmentAbbreviation(enduranceWizardMode) + "  →", coral, Color.WHITE);
        start.setOnClickListener(view -> {
            try {
                EndurancePlanStore.save(this, enduranceWizardMode, enduranceWizardName, enduranceWizardOrigin, enduranceWizardDestination, enduranceWizardDistanceKm, enduranceWizardMinutes, enduranceWizardEffort);
                publishEnduranceWizardSchedule();
                startGuidedEndurance(enduranceWizardMode);
            } catch (IllegalArgumentException error) { toast(error.getMessage()); }
        }); body.addView(start, new LinearLayout.LayoutParams(-1, dp(54))); gap(body, 8);
        Button live = action("Start " + appointmentAbbreviation(enduranceWizardMode) + " with live tracking", mint, navy);
        live.setOnClickListener(view -> {
            try {
                EndurancePlanStore.save(this, enduranceWizardMode, enduranceWizardName, enduranceWizardOrigin, enduranceWizardDestination, enduranceWizardDistanceKm, enduranceWizardMinutes, enduranceWizardEffort);
                publishEnduranceWizardSchedule();
                requestLiveTracking(enduranceWizardMode);
            } catch (IllegalArgumentException error) { toast(error.getMessage()); }
        }); body.addView(live, new LinearLayout.LayoutParams(-1, dp(54))); gap(body, 8);
        Button save = action("Save endurance plan", navyPanel, mint);
        save.setOnClickListener(view -> {
            try { EndurancePlanStore.save(this, enduranceWizardMode, enduranceWizardName, enduranceWizardOrigin, enduranceWizardDestination, enduranceWizardDistanceKm, enduranceWizardMinutes, enduranceWizardEffort); publishEnduranceWizardSchedule(); toast("Endurance plan saved."); showEndurance(); }
            catch (IllegalArgumentException error) { toast(error.getMessage()); }
        }); body.addView(save, new LinearLayout.LayoutParams(-1, dp(52)));
        Button back = new Button(this); back.setText("Back"); back.setTextColor(mint); back.setTextSize(14); back.setAllCaps(false); back.setBackgroundColor(Color.TRANSPARENT); back.setOnClickListener(view -> showEnduranceWizardRoute()); body.addView(back, new LinearLayout.LayoutParams(-1, dp(46)));
    }

    /** Mirrors saved Endurance wizard choices into the shared Home/Scheduler calendar. */
    private void publishEnduranceWizardSchedule() {
        boolean weekly = "Weekly".equals(endurancePlanMode);
        if (weekly) {
            for (int day = 0; day < enduranceWorkoutDays.length; day++) {
                if (enduranceWorkoutDays[day]) SchedulerStore.saveIfMissing(this, enduranceWizardMode, selectedWeekDate(day), enduranceWizardMinutes);
            }
        } else {
            SchedulerStore.saveIfMissing(this, enduranceWizardMode, selectedWeekDate(enduranceDailyDay), enduranceWizardMinutes);
        }
    }

    /** Guided endurance shares the Coach 5-to-1 ritual with the strength workout flow. */
    private void startGuidedEndurance(String mode) {
        stopEnduranceGuided();
        guidedEnduranceMode = mode;
        guidedEnduranceLaps.clear();
        showEnduranceCountdown();
    }

    private void showEnduranceCountdown() {
        if (guidedEnduranceMode == null) return;
        LinearLayout body = appPage("Endurance");
        String action = enduranceAction(guidedEnduranceMode);
        title(body, "GUIDED ENDURANCE", "Start " + action, "Your timer begins when the visual countdown finishes.");
        LinearLayout countCard = card();
        TextView count = heading("5", 58, ink); count.setGravity(Gravity.CENTER); countCard.addView(count, new LinearLayout.LayoutParams(-1, dp(98)));
        body.addView(countCard);
        runWorkoutCountIn("Let’s begin in 5, 4, 3, 2, 1.", count, () -> {
            if (guidedEnduranceMode != null) {
                guidedEnduranceStartedAt = SystemClock.elapsedRealtime();
                showGuidedEnduranceActive();
            }
        });
    }

    private void showGuidedEnduranceActive() {
        if (guidedEnduranceMode == null) return;
        LinearLayout body = appPage("Endurance");
        final int sessionPage = pageGeneration;
        final String action = enduranceAction(guidedEnduranceMode);
        title(body, "GUIDED ENDURANCE", action + " in progress", "Your timer is running. " + ((EndurancePlanStore.WALKING.equals(guidedEnduranceMode) || EndurancePlanStore.RUNNING.equals(guidedEnduranceMode)) ? "Record a manual lap whenever you reach one." : "Keep a smooth, steady pace."));
        LinearLayout timerCard = card();
        timerCard.addView(heading(action.toUpperCase(Locale.ROOT) + " · LIVE TIMER", 13, muted));
        TextView elapsed = heading("00:00", 54, ink); elapsed.setGravity(Gravity.CENTER); elapsed.setContentDescription(action + " elapsed time"); timerCard.addView(elapsed, new LinearLayout.LayoutParams(-1, dp(92)));
        timerCard.addView(text("Target · " + formatDistance(enduranceWizardDistanceKm) + "  •  " + enduranceWizardMinutes + " min", 14, muted));
        body.addView(timerCard); gap(body, 12);
        final boolean supportsLaps = EndurancePlanStore.WALKING.equals(guidedEnduranceMode) || EndurancePlanStore.RUNNING.equals(guidedEnduranceMode);
        LinearLayout laps = card(); laps.addView(heading(supportsLaps ? "MANUAL LAPS" : "RIDE CHECKPOINTS", 13, muted));
        TextView lapSummary = text(supportsLaps ? "No laps recorded yet." : "Cycling uses the continuous timer.", 15, ink); laps.addView(lapSummary); body.addView(laps); gap(body, 12);
        if (supportsLaps) {
            Button lap = secondary("Record manual lap");
            lap.setOnClickListener(view -> {
                long elapsedMillis = Math.max(0L, SystemClock.elapsedRealtime() - guidedEnduranceStartedAt);
                guidedEnduranceLaps.add(elapsedMillis);
                lapSummary.setText(enduranceLapSummary());
                toast("Lap " + guidedEnduranceLaps.size() + " recorded · " + WorkoutTiming.formatElapsedMillis(elapsedMillis));
            }); addButton(body, lap); gap(body, 8);
        }
        Button finish = primary("Finish " + action);
        finish.setOnClickListener(view -> {
            long duration = Math.max(60_000L, SystemClock.elapsedRealtime() - guidedEnduranceStartedAt);
            JourneyStore.recordWorkout(this, "Endurance · " + guidedEnduranceMode, Math.max(1, Math.round(duration / 60_000f)));
            stopEnduranceGuided();
            toast(action + " saved to Progress.");
            showProgress();
        }); addButton(body, finish); gap(body, 8);
        Button end = secondary("End without saving"); end.setOnClickListener(view -> { stopEnduranceGuided(); showEndurance(); }); addButton(body, end);
        guidedEnduranceTimer = new Runnable() {
            @Override public void run() {
                if (guidedEnduranceMode == null || pageGeneration != sessionPage) return;
                elapsed.setText(formatLiveDuration(SystemClock.elapsedRealtime() - guidedEnduranceStartedAt));
                workoutHandler.postDelayed(this, 250L);
            }
        };
        workoutHandler.post(guidedEnduranceTimer);
    }

    private String enduranceLapSummary() {
        if (guidedEnduranceLaps.isEmpty()) return "No laps recorded yet.";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < guidedEnduranceLaps.size(); i++) {
            if (i > 0) result.append("  •  ");
            result.append("Lap ").append(i + 1).append(" ").append(WorkoutTiming.formatElapsedMillis(guidedEnduranceLaps.get(i)));
        }
        return result.toString();
    }

    private void stopEnduranceGuided() {
        if (guidedEnduranceTimer != null) workoutHandler.removeCallbacks(guidedEnduranceTimer);
        guidedEnduranceTimer = null;
        guidedEnduranceMode = null;
        guidedEnduranceStartedAt = 0L;
        guidedEnduranceLaps.clear();
    }

    /** One calendar joins strength templates, rest days, and separately booked endurance appointments. */
    private void showScheduler() {
        LinearLayout body = appPage("Scheduler");
        title(body, "SCHEDULER", "Your training calendar", "See existing work plans, rest days, and endurance appointments in one place. Tap a date to add or review a session.");
        Calendar month = schedulerMonth();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button previous = action("‹", navyPanel, mint);
        previous.setContentDescription("Previous month");
        previous.setOnClickListener(view -> {
            schedulerMonthOffset--;
            schedulerSelectedDate = schedulerMonth();
            refreshCurrentPageKeepingScroll(this::showScheduler);
        });
        header.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(46)));
        TextView label = heading(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.getTime()), 21, Color.WHITE);
        label.setGravity(Gravity.CENTER);
        header.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
        Button next = action("›", navyPanel, mint);
        next.setContentDescription("Next month");
        next.setOnClickListener(view -> {
            schedulerMonthOffset++;
            schedulerSelectedDate = schedulerMonth();
            refreshCurrentPageKeepingScroll(this::showScheduler);
        });
        header.addView(next, new LinearLayout.LayoutParams(dp(48), dp(46)));
        body.addView(header);
        gap(body, 10);

        LinearLayout calendarCard = card();
        calendarCard.addView(heading("MONTH VIEW", 11, muted));
        calendarCard.addView(text("Mint = work plan  •  Coral = endurance appointment  •  Slate = rest day", 12, muted));
        // Use weighted rows rather than fixed-width grid cells. The sidebar changes available
        // width on phones and tablets, so fixed 39dp cells could crop the last calendar days.
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        String[] labels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        int first = (month.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
        int max = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        int rows = (int) Math.ceil((first + max) / 7d);
        for (int rowIndex = -1; rowIndex < rows; rowIndex++) {
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            for (int column = 0; column < 7; column++) {
                TextView cell;
                int cellHeight;
                if (rowIndex == -1) {
                    cell = text(labels[column], 10, muted); cell.setGravity(Gravity.CENTER); cellHeight = 28;
                } else {
                    int index = rowIndex * 7 + column;
                    int day = index - first + 1;
                    Calendar date = null;
                    if (day >= 1 && day <= max) { date = (Calendar) month.clone(); date.set(Calendar.DAY_OF_MONTH, day); }
                    cell = schedulerCalendarCell(date); cellHeight = 56;
                }
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, dp(cellHeight), 1f);
                cellParams.setMargins(dp(1), dp(1), dp(1), dp(1));
                row.addView(cell, cellParams);
            }
            grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        calendarCard.addView(grid);
        body.addView(calendarCard);
        gap(body, 12);
        addSchedulerDayAgenda(body, schedulerSelectedDate);
        gap(body, 10);
        Button book = primary("Schedule endurance appointment");
        book.setOnClickListener(view -> showSchedulerActivity());
        addButton(body, book);
    }

    private Calendar schedulerMonth() {
        Calendar month = Calendar.getInstance();
        month.set(Calendar.DAY_OF_MONTH, 1);
        month.set(Calendar.HOUR_OF_DAY, 0); month.set(Calendar.MINUTE, 0); month.set(Calendar.SECOND, 0); month.set(Calendar.MILLISECOND, 0);
        month.add(Calendar.MONTH, schedulerMonthOffset);
        return month;
    }

    private TextView schedulerCalendarCell(Calendar date) {
        if (date == null) {
            TextView empty = text("", 10, muted);
            return empty;
        }
        List<SchedulerStore.Appointment> appointments = SchedulerStore.onDate(this, date);
        boolean planned = isMonthWorkoutDay(date);
        boolean selected = sameSchedulerDate(date, schedulerSelectedDate);
        String state = !appointments.isEmpty() ? appointmentAbbreviation(appointments.get(0).mode) : planned ? schedulerStrengthSummary(date) : "Rest";
        if (state.length() > 8) state = state.substring(0, 8);
        int fill = !appointments.isEmpty() ? coral : planned ? mint : Color.rgb(232, 236, 244);
        TextView cell = text(date.get(Calendar.DAY_OF_MONTH) + "\n" + state, 10, selected ? navy : (planned || !appointments.isEmpty() ? navy : muted));
        cell.setGravity(Gravity.CENTER);
        cell.setTypeface(Typeface.create("sans-serif", selected ? Typeface.BOLD : Typeface.NORMAL));
        cell.setBackground(surface(fill, 11, selected ? 3 : 0, selected ? coral : Color.TRANSPARENT));
        cell.setContentDescription(new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(date.getTime()) + ", " + state);
        cell.setClickable(true);
        cell.setOnClickListener(view -> { schedulerSelectedDate = (Calendar) date.clone(); refreshCurrentPageKeepingScroll(this::showScheduler); });
        return cell;
    }

    private void addSchedulerDayAgenda(LinearLayout body, Calendar date) {
        LinearLayout agenda = card();
        agenda.addView(heading(new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(date.getTime()), 19, ink));
        if (isMonthWorkoutDay(date)) agenda.addView(text("Work plan · " + schedulerStrengthSummary(date), 14, Color.rgb(46, 116, 103)));
        else agenda.addView(text("Rest day in your current work plan", 13, muted));
        List<SchedulerStore.Appointment> appointments = SchedulerStore.onDate(this, date);
        for (SchedulerStore.Appointment appointment : appointments) {
            gap(agenda, 8);
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            TextView copy = text(appointment.mode + " · " + schedulerTime(appointment.when) + " · " + appointment.durationMinutes + " min", 14, ink);
            row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
            Button remove = action("Remove", paper, coral);
            remove.setTextSize(12);
            remove.setOnClickListener(view -> { SchedulerStore.remove(this, appointment.id); refreshCurrentPageKeepingScroll(this::showScheduler); });
            row.addView(remove, new LinearLayout.LayoutParams(dp(78), dp(40)));
            agenda.addView(row);
        }
        if (appointments.isEmpty()) agenda.addView(text("No endurance appointment booked for this date.", 13, muted));
        body.addView(agenda);
    }

    private String schedulerStrengthSummary(Calendar date) {
        Calendar weekStart = (Calendar) date.clone(); weekStart.setFirstDayOfWeek(Calendar.MONDAY);
        weekStart.add(Calendar.DAY_OF_YEAR, -((weekStart.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7));
        int day = (date.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
        WeeklyScheduleStore.Schedule schedule = WeeklyScheduleStore.load(this, weekStart, defaultWeeklyDays(), defaultWeeklyTargets());
        return focusSummary(schedule.targets.get(day));
    }

    private boolean sameSchedulerDate(Calendar one, Calendar two) { return one != null && two != null && SchedulerStore.dateKey(one).equals(SchedulerStore.dateKey(two)); }
    private String appointmentAbbreviation(String mode) { return EndurancePlanStore.CYCLING.equals(mode) ? "Cycle" : EndurancePlanStore.RUNNING.equals(mode) ? "Run" : "Walk"; }
    private String schedulerTime(long when) { Calendar time = Calendar.getInstance(); time.setTimeInMillis(when); return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(time.getTime()); }

    /** Appointment wizard step 1: choose a single endurance activity. */
    private void showSchedulerActivity() {
        LinearLayout body = appPage("Scheduler");
        title(body, "SCHEDULE ENDURANCE", "Choose an activity", "Step 1 of 3. This appointment is separate from your workout-plan template and appears beside it on Scheduler.");
        addSchedulerModeChoice(body, EndurancePlanStore.WALKING, "Walking", "Plan a low-impact outdoor session.");
        gap(body, 10);
        addSchedulerModeChoice(body, EndurancePlanStore.RUNNING, "Running", "Set a route and duration for your run.");
        gap(body, 10);
        addSchedulerModeChoice(body, EndurancePlanStore.CYCLING, "Cycling", "Book a ride with a planned duration.");
        gap(body, 14);
        Button next = primary("Next: date and time");
        next.setOnClickListener(view -> showSchedulerDetails());
        addButton(body, next);
        gap(body, 8);
        Button back = secondary("Back to Scheduler"); back.setOnClickListener(view -> showScheduler()); addButton(body, back);
    }

    private void addSchedulerModeChoice(LinearLayout body, String mode, String label, String copy) {
        boolean selected = mode.equals(schedulerActivity);
        Button choice = action((selected ? "✓  " : "") + label + "\n" + copy, selected ? mint : paper, ink);
        choice.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        choice.setTextSize(16);
        choice.setOnClickListener(view -> { schedulerActivity = mode; refreshCurrentPageKeepingScroll(this::showSchedulerActivity); });
        body.addView(choice, new LinearLayout.LayoutParams(-1, dp(78)));
    }

    /** Appointment wizard steps 2 and 3: set timing, then explicitly save. */
    private void showSchedulerDetails() {
        LinearLayout body = appPage("Scheduler");
        title(body, "SCHEDULE ENDURANCE", "Set the appointment", "Step 2 of 3. Choose when you want to " + enduranceAction(schedulerActivity) + " and how long you plan to be active.");
        LinearLayout details = card();
        details.addView(heading(appointmentAbbreviation(schedulerActivity), 20, ink));
        Button date = lightAction("Date · " + new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(schedulerSelectedDate.getTime()));
        date.setOnClickListener(view -> new android.app.DatePickerDialog(this, (picker, year, month, day) -> {
            schedulerSelectedDate.set(year, month, day); refreshCurrentPageKeepingScroll(this::showSchedulerDetails);
        }, schedulerSelectedDate.get(Calendar.YEAR), schedulerSelectedDate.get(Calendar.MONTH), schedulerSelectedDate.get(Calendar.DAY_OF_MONTH)).show());
        addButton(details, date); gap(details, 8);
        Button time = lightAction("Time · " + String.format(Locale.US, "%02d:%02d", schedulerHour, schedulerMinute));
        time.setOnClickListener(view -> new android.app.TimePickerDialog(this, (picker, hour, minute) -> { schedulerHour = hour; schedulerMinute = minute; refreshCurrentPageKeepingScroll(this::showSchedulerDetails); }, schedulerHour, schedulerMinute, false).show());
        addButton(details, time); gap(details, 8);
        EditText duration = input("Planned minutes", InputType.TYPE_CLASS_NUMBER); duration.setText(String.valueOf(schedulerDurationMinutes)); details.addView(duration);
        body.addView(details); gap(body, 12);
        Button review = primary("Next: review appointment");
        review.setOnClickListener(view -> {
            try { schedulerDurationMinutes = Integer.parseInt(duration.getText().toString().trim()); showSchedulerReview(); }
            catch (NumberFormatException error) { duration.setError("Enter planned minutes"); }
        });
        addButton(body, review); gap(body, 8);
        Button back = secondary("Back to activity"); back.setOnClickListener(view -> showSchedulerActivity()); addButton(body, back);
    }

    private void showSchedulerReview() {
        LinearLayout body = appPage("Scheduler");
        title(body, "SCHEDULE ENDURANCE", "Review appointment", "Step 3 of 3. Save only when this date, time, and duration look right.");
        Calendar when = (Calendar) schedulerSelectedDate.clone(); when.set(Calendar.HOUR_OF_DAY, schedulerHour); when.set(Calendar.MINUTE, schedulerMinute); when.set(Calendar.SECOND, 0); when.set(Calendar.MILLISECOND, 0);
        LinearLayout summary = card();
        summary.addView(heading(appointmentAbbreviation(schedulerActivity) + " appointment", 21, ink));
        summary.addView(text(new SimpleDateFormat("EEEE, MMMM d · h:mm a", Locale.getDefault()).format(when.getTime()), 15, ink));
        summary.addView(text(schedulerDurationMinutes + " planned minutes", 14, muted));
        body.addView(summary); gap(body, 12);
        Button save = primary("Save to Scheduler");
        save.setOnClickListener(view -> {
            try { SchedulerStore.save(this, schedulerActivity, when, schedulerDurationMinutes); toast("Endurance appointment saved."); showScheduler(); }
            catch (IllegalArgumentException error) { toast(error.getMessage()); showSchedulerDetails(); }
        });
        addButton(body, save); gap(body, 8);
        Button back = secondary("Back to date and time"); back.setOnClickListener(view -> showSchedulerDetails()); addButton(body, back);
    }

    /** Sends a standard GPX file to a user-chosen map app; no mapping account or map data is bundled. */
    private void openTrackInOfflineMap(LiveTrackingStore.Session session) {
        try {
            File folder = new File(getCacheDir(), "tracks");
            if (!folder.exists() && !folder.mkdirs()) throw new IOException("Could not prepare route file");
            File track = new File(folder, "endurance-" + System.currentTimeMillis() + ".gpx");
            StringBuilder gpx = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"My Personal Trainer\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><name>")
                    .append(session.mode).append(" endurance</name><trkseg>");
            for (LiveTrackingStore.Point point : session.points) gpx.append("<trkpt lat=\"").append(point.latitude).append("\" lon=\"").append(point.longitude).append("\"/>");
            gpx.append("</trkseg></trk></gpx>");
            try (FileOutputStream output = new FileOutputStream(track)) { output.write(gpx.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", track);
            Intent share = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/gpx+xml")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Open route with"));
        } catch (IOException | RuntimeException error) {
            toast("Could not prepare this route for an offline map app.");
        }
    }

    private void showOfflineMapGuide() {
        new AlertDialog.Builder(this).setTitle("Use maps offline")
                .setMessage("For dependable walking, running, and cycling maps without mobile data, install an offline-map app such as OsmAnd and download your local region inside that app while on Wi‑Fi. My Personal Trainer records distance with GPS and can send each finished route to a compatible map app as a GPX file. Google Maps offline downloads cannot provide walking or cycling directions.")
                .setPositiveButton("Got it", null).show();
    }

    private void addEnduranceActivity(LinearLayout body, String mode, String name, String description) {
        EndurancePlanStore.Plan plan = EndurancePlanStore.load(this, mode);
        LinearLayout tile = card();
        tile.addView(heading(name, 21, ink));
        tile.addView(text(description, 14, muted));
        gap(tile, 8);
        if (plan == null) {
            tile.addView(text("No route saved yet.", 13, muted));
            gap(tile, 10);
            Button create = primary("Plan a " + name.toLowerCase(Locale.ROOT));
            create.setOnClickListener(view -> startEnduranceWizard(mode));
            addButton(tile, create);
        } else {
            tile.addView(text(plan.name + "  •  " + formatDistance(plan.distanceKm) + "  •  " + plan.minutes + " min  •  " + plan.effort, 14, ink));
            tile.addView(text("Destination: " + plan.destination, 13, muted));
            gap(tile, 9);
            Button map = lightAction("Open saved route in Maps");
            map.setOnClickListener(view -> openEnduranceMap(plan));
            addButton(tile, map);
            gap(tile, 7);
            Button complete = primary("Log " + plan.minutes + " min to Progress");
            complete.setOnClickListener(view -> {
                JourneyStore.recordWorkout(this, "Endurance · " + mode, plan.minutes);
                toast("Endurance session added to local Progress.");
                showProgress();
            });
            addButton(tile, complete);
            gap(tile, 7);
            Button edit = secondary("Edit or remove route");
            edit.setOnClickListener(view -> startEnduranceWizard(mode));
            addButton(tile, edit);
        }
        LiveTrackingStore.Session active = LiveTrackingStore.current(this);
        if (!active.active) {
            gap(tile, 8);
            Button timerOnly = lightAction("Start " + name.toLowerCase(Locale.ROOT) + " without live tracking");
            timerOnly.setOnClickListener(view -> startManualEndurance(mode));
            addButton(tile, timerOnly);
            gap(tile, 7);
            Button start = secondary("Start live " + name.toLowerCase(Locale.ROOT));
            start.setOnClickListener(view -> requestLiveTracking(mode));
            addButton(tile, start);
        }
        body.addView(tile);
    }

    /** A location-free alternative for treadmill, indoor-bike, or privacy-first endurance sessions. */
    private void startManualEndurance(String mode) {
        manualEnduranceMode = mode;
        manualEnduranceStartedAt = SystemClock.elapsedRealtime();
        showManualEnduranceSession();
    }

    private void showManualEnduranceSession() {
        LinearLayout body = appPage("Endurance");
        final int sessionPage = pageGeneration;
        String activity = appointmentAbbreviation(manualEnduranceMode);
        title(body, "ENDURANCE", activity + " in progress", "Timer-only session. Location, map tracing, and live tracking are off.");
        LinearLayout sessionCard = card();
        sessionCard.addView(heading(activity.toUpperCase(Locale.ROOT) + " · NO LIVE TRACKING", 17, ink));
        TextView elapsed = heading("00:00", 48, ink);
        elapsed.setGravity(Gravity.CENTER);
        elapsed.setContentDescription("Elapsed time");
        sessionCard.addView(elapsed, new LinearLayout.LayoutParams(-1, dp(86)));
        sessionCard.addView(text("Use this for indoor sessions, a treadmill, stationary bike, or whenever you prefer not to share location.", 14, muted));
        body.addView(sessionCard);
        gap(body, 12);
        Button finish = primary("Finish and save to Progress");
        finish.setOnClickListener(view -> {
            long duration = Math.max(60_000L, SystemClock.elapsedRealtime() - manualEnduranceStartedAt);
            int minutes = Math.max(1, Math.round(duration / 60_000f));
            JourneyStore.recordWorkout(this, "Endurance · " + manualEnduranceMode, minutes);
            endManualEnduranceSession();
            toast("Timer-only endurance session saved to Progress.");
            showProgress();
        });
        addButton(body, finish);
        gap(body, 8);
        Button cancel = secondary("Cancel session");
        cancel.setOnClickListener(view -> { endManualEnduranceSession(); showEndurance(); });
        addButton(body, cancel);
        manualEnduranceTimer = new Runnable() {
            @Override public void run() {
                if (manualEnduranceMode == null || pageGeneration != sessionPage) return;
                elapsed.setText(formatLiveDuration(SystemClock.elapsedRealtime() - manualEnduranceStartedAt));
                workoutHandler.postDelayed(this, 250L);
            }
        };
        workoutHandler.post(manualEnduranceTimer);
    }

    private void endManualEnduranceSession() {
        if (manualEnduranceTimer != null) workoutHandler.removeCallbacks(manualEnduranceTimer);
        manualEnduranceTimer = null;
        manualEnduranceMode = null;
        manualEnduranceStartedAt = 0L;
    }

    private void addLiveTrackingCard(LinearLayout body, LiveTrackingStore.Session live) {
        LinearLayout tracker = card();
        TextView state = heading("LIVE " + live.mode.toUpperCase(Locale.ROOT) + (live.paused ? " · PAUSED" : " · TRACKING"), 18, ink);
        TextView stats = text(liveTrackingStats(live), 20, ink);
        tracker.addView(state);
        tracker.addView(stats);
        OfflineMapStore.MapRecord selectedMap = OfflineMapStore.selectedMap(this);
        tracker.addView(text(OfflineMapStore.hasAnyMap(this)
                ? (selectedMap == null ? "A country street map is stored on this phone." : selectedMap.country + " streets are stored on this phone.") + " Your coral route is drawn locally."
                : "GPS route trace is local to this phone. Choose a country in Settings for a full offline map.", 13, muted));
        LiveRouteRenderer routeRenderer;
        if (OfflineMapStore.hasAnyMap(this)) {
            try {
                OfflineRouteMapView map = new OfflineRouteMapView(this, OfflineMapStore.mapFile(this), live.points);
                LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(-1, dp(235)); mapParams.topMargin = dp(10);
                tracker.addView(map, mapParams); routeRenderer = map::setRoute;
            } catch (RuntimeException error) {
                RouteTraceView trace = new RouteTraceView(this); trace.setPoints(live.points);
                tracker.addView(trace, new LinearLayout.LayoutParams(-1, dp(150))); routeRenderer = trace::setPoints;
            }
        } else {
            RouteTraceView trace = new RouteTraceView(this); trace.setPoints(live.points);
            LinearLayout.LayoutParams traceParams = new LinearLayout.LayoutParams(-1, dp(150)); traceParams.topMargin = dp(10);
            tracker.addView(trace, traceParams); routeRenderer = trace::setPoints;
            gap(tracker, 8); addTrinidadOfflineMapControl(tracker, this::showLiveTrackingSession);
        }
        gap(tracker, 10);
        Button pause = live.paused ? primary("Resume live tracking") : lightAction("Pause live tracking");
        pause.setOnClickListener(view -> {
            sendTrackingCommand(live.paused ? LiveTrackingService.ACTION_RESUME : LiveTrackingService.ACTION_PAUSE);
            workoutHandler.postDelayed(this::showLiveTrackingSession, 180L);
        });
        addButton(tracker, pause);
        gap(tracker, 7);
        Button finish = primary("Finish and save to Progress");
        finish.setOnClickListener(view -> {
            sendTrackingCommand(LiveTrackingService.ACTION_FINISH);
            workoutHandler.postDelayed(this::showWorkoutHistory, 300L);
        });
        addButton(tracker, finish);
        gap(tracker, 7);
        Button discard = secondary("Discard live session");
        discard.setOnClickListener(view -> {
            sendTrackingCommand(LiveTrackingService.ACTION_DISCARD);
            workoutHandler.postDelayed(this::showEndurance, 180L);
        });
        addButton(tracker, discard);
        body.addView(tracker);
        observeLiveTracking(state, stats, routeRenderer);
    }

    /** Dedicated in-app surface for an active GPS session; it remains available after navigation. */
    private void showLiveTrackingSession() {
        LiveTrackingStore.Session live = LiveTrackingStore.current(this);
        if (!live.active) {
            showEndurance();
            return;
        }
        LinearLayout body = appPage("Endurance");
        title(body, "LIVE ENDURANCE", enduranceAction(live.mode) + " in progress",
                "Your route and totals stay on this phone. You can safely leave this screen while Android keeps tracking.");
        addLiveTrackingCard(body, live);
        gap(body, 8);
        Button background = secondary("Keep tracking in background");
        background.setOnClickListener(view -> startEnduranceWizard(live.mode));
        addButton(body, background);
    }

    private void requestLiveTracking(String mode) {
        if (hasLiveLocationPermission()) {
            startLiveTrackingService(mode);
            return;
        }
        pendingLiveTrackingMode = mode;
        new AlertDialog.Builder(this).setTitle("Track your " + enduranceAction(mode))
                .setMessage("My Personal Trainer will use your location only while this live session is running to measure distance and draw your private route trace. Android will show an ongoing tracking notification. You can pause, finish, or discard it at any time.")
                .setNegativeButton("Not now", null)
                .setPositiveButton("Allow while tracking", (dialog, which) -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LIVE_LOCATION)).show();
    }

    private boolean hasLiveLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startLiveTrackingService(String mode) {
        Intent intent = new Intent(this, LiveTrackingService.class).setAction(LiveTrackingService.ACTION_START).putExtra(LiveTrackingService.EXTRA_MODE, mode);
        // Create visible local session state first so a slow service launch never throws the person
        // back into the planner. The service repeats this initialization when it reaches START.
        LiveTrackingStore.start(this, mode);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent); else startService(intent);
        } catch (RuntimeException error) {
            LiveTrackingStore.discard(this);
            toast("Android could not start live tracking. Try again after allowing location access.");
            showEndurance();
            return;
        }
        showLiveTrackingSession();
    }

    private void sendTrackingCommand(String action) {
        startService(new Intent(this, LiveTrackingService.class).setAction(action));
    }

    private interface LiveRouteRenderer { void setRoute(List<LiveTrackingStore.Point> points); }

    private void observeLiveTracking(TextView state, TextView stats, LiveRouteRenderer routeRenderer) {
        stopLiveTrackingUiObserver();
        liveTrackingReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                LiveTrackingStore.Session latest = LiveTrackingStore.current(MainActivity.this);
                if (!latest.active) return;
                state.setText("LIVE " + latest.mode.toUpperCase(Locale.ROOT) + (latest.paused ? " · PAUSED" : " · TRACKING"));
                stats.setText(liveTrackingStats(latest));
                routeRenderer.setRoute(latest.points);
            }
        };
        IntentFilter filter = new IntentFilter(LiveTrackingService.ACTION_UPDATE);
        ContextCompat.registerReceiver(this, liveTrackingReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void stopLiveTrackingUiObserver() {
        if (liveTrackingReceiver == null) return;
        try { unregisterReceiver(liveTrackingReceiver); } catch (IllegalArgumentException ignored) { }
        liveTrackingReceiver = null;
    }

    private String liveTrackingStats(LiveTrackingStore.Session session) {
        String pace = "—";
        if (session.distanceMeters >= 10f) pace = String.format(Locale.US, "%.1f min/km", (session.activeMillis / 60000d) / (session.distanceMeters / 1000d));
        return formatLiveDistance(session.distanceMeters) + "  •  " + formatLiveDuration(session.activeMillis) + "  •  " + pace;
    }

    private String formatLiveDistance(float meters) { return meters >= 1000f ? String.format(Locale.US, "%.2f km", meters / 1000f) : Math.round(meters) + " m"; }
    private String formatLiveDuration(long millis) { long seconds = millis / 1000L; return String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L); }

    private void showEndurancePlanner(String mode) {
        EndurancePlanStore.Plan existing = EndurancePlanStore.load(this, mode);
        LinearLayout body = appPage("Endurance");
        String action = enduranceAction(mode);
        title(body, "ENDURANCE ROUTE", "Plan your " + action, "Set a destination or loop landmark, a target distance, and a time. You choose whether to open an external map app.");

        EditText name = input("Route name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText origin = input("Starting point (optional)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText destination = input("Destination or loop landmark", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText distance = input("Target distance in km", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText minutes = input("Target minutes", InputType.TYPE_CLASS_NUMBER);
        float defaultDistance = defaultDistance(mode);
        int defaultMinutes = defaultMinutes(mode);
        name.setText(existing == null ? mode + " route" : existing.name);
        origin.setText(existing == null ? "" : existing.origin);
        destination.setText(existing == null ? "" : existing.destination);
        distance.setText(String.valueOf(existing == null ? defaultDistance : existing.distanceKm));
        minutes.setText(String.valueOf(existing == null ? defaultMinutes : existing.minutes));
        body.addView(name); gap(body, 8);
        body.addView(origin); gap(body, 8);
        body.addView(destination); gap(body, 8);
        body.addView(distance); gap(body, 8);
        body.addView(minutes); gap(body, 12);

        body.addView(heading("Effort", 18, Color.WHITE));
        RadioGroup effort = new RadioGroup(this);
        effort.setOrientation(LinearLayout.HORIZONTAL);
        effort.setPadding(0, dp(4), 0, dp(4));
        String selectedEffort = existing == null ? "Steady" : existing.effort;
        addEnduranceEffort(effort, "Relaxed", selectedEffort);
        addEnduranceEffort(effort, "Steady", selectedEffort);
        addEnduranceEffort(effort, "Challenging", selectedEffort);
        body.addView(effort);
        gap(body, 12);

        Button map = lightAction("Preview route in Maps");
        map.setOnClickListener(view -> {
            if (destination.getText().toString().trim().isEmpty()) {
                destination.setError("Add a destination or loop landmark first");
                return;
            }
            openEnduranceMap(mode, origin.getText().toString(), destination.getText().toString());
        });
        addButton(body, map);
        gap(body, 8);
        Button save = primary("Save " + action + " plan");
        save.setOnClickListener(view -> saveEndurancePlan(mode, name, origin, destination, distance, minutes, effort));
        addButton(body, save);
        if (existing != null) {
            gap(body, 8);
            Button remove = secondary("Remove saved route");
            remove.setOnClickListener(view -> {
                EndurancePlanStore.remove(this, mode);
                toast(mode + " route removed.");
                showEndurance();
            });
            addButton(body, remove);
        }
        gap(body, 8);
        Button back = secondary("Back to Endurance");
        back.setOnClickListener(view -> showEndurance());
        addButton(body, back);
    }

    private void addEnduranceEffort(RadioGroup group, String label, String selected) {
        RadioButton option = new RadioButton(this);
        option.setId(View.generateViewId());
        option.setText(label);
        option.setTextSize(13);
        option.setTextColor(Color.WHITE);
        option.setButtonTintList(ColorStateList.valueOf(mint));
        option.setChecked(label.equals(selected));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        group.addView(option, params);
    }

    private void saveEndurancePlan(String mode, EditText name, EditText origin, EditText destination,
                                   EditText distance, EditText minutes, RadioGroup effort) {
        try {
            float distanceKm = Float.parseFloat(distance.getText().toString().trim());
            int targetMinutes = Integer.parseInt(minutes.getText().toString().trim());
            int checked = effort.getCheckedRadioButtonId();
            String effortText = checked == View.NO_ID ? "Steady" : ((RadioButton) effort.findViewById(checked)).getText().toString();
            EndurancePlanStore.save(this, mode, name.getText().toString(), origin.getText().toString(),
                    destination.getText().toString(), distanceKm, targetMinutes, effortText);
            toast(mode + " route saved on this device.");
            showEndurance();
        } catch (NumberFormatException e) {
            toast("Enter a valid distance and target minutes.");
        } catch (IllegalArgumentException e) {
            toast(e.getMessage());
        }
    }

    private void openEnduranceMap(EndurancePlanStore.Plan plan) {
        openEnduranceMap(plan.mode, plan.origin, plan.destination);
    }

    /** Route planning is deliberately Google Maps-only; no alternate map app is offered. */
    private void openEnduranceMap(String mode, String origin, String destination) {
        String safeDestination = destination == null ? "" : destination.trim();
        if (safeDestination.isEmpty()) {
            toast("Add a Google Maps destination or loop landmark first.");
            return;
        }
        if (!isGoogleMapsInstalled()) {
            showGoogleMapsInstallPrompt();
            return;
        }
        Uri.Builder route = new Uri.Builder().scheme("https").authority("www.google.com")
                .appendPath("maps").appendPath("dir")
                .appendQueryParameter("api", "1")
                .appendQueryParameter("destination", safeDestination)
                .appendQueryParameter("travelmode", EndurancePlanStore.CYCLING.equals(mode) ? "bicycling" : "walking");
        if (origin != null && !origin.trim().isEmpty()) route.appendQueryParameter("origin", origin.trim());
        try {
            Intent googleMaps = new Intent(Intent.ACTION_VIEW, route.build());
            googleMaps.setPackage("com.google.android.apps.maps");
            startActivity(googleMaps);
        } catch (RuntimeException e) {
            showGoogleMapsInstallPrompt();
        }
    }

    private boolean isGoogleMapsInstalled() {
        try {
            getPackageManager().getPackageInfo("com.google.android.apps.maps", 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private void showGoogleMapsInstallPrompt() {
        new AlertDialog.Builder(this)
                .setTitle("Google Maps required")
                .setMessage("Walking, running, and cycling routes open only in Google Maps. Install Google Maps from the Play Store, then return here to continue.")
                .setNegativeButton("Not now", null)
                .setPositiveButton("Install Google Maps", (dialog, which) -> openGoogleMapsPlayStore())
                .show();
    }

    private void openGoogleMapsPlayStore() {
        Intent store = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
        store.setPackage("com.android.vending");
        try {
            startActivity(store);
        } catch (RuntimeException unavailable) {
            toast("Open Google Play and install Google Maps to plan routes.");
        }
    }

    private String enduranceAction(String mode) {
        if (EndurancePlanStore.CYCLING.equals(mode)) return "ride";
        return EndurancePlanStore.RUNNING.equals(mode) ? "run" : "walk";
    }

    private float defaultDistance(String mode) {
        if (EndurancePlanStore.CYCLING.equals(mode)) return 10f;
        return EndurancePlanStore.RUNNING.equals(mode) ? 5f : 3f;
    }

    private int defaultMinutes(String mode) {
        if (EndurancePlanStore.CYCLING.equals(mode)) return 45;
        return EndurancePlanStore.RUNNING.equals(mode) ? 35 : 35;
    }

    private String formatDistance(float distanceKm) {
        return distanceKm == (int) distanceKm ? ((int) distanceKm) + " km" : String.format(Locale.US, "%.1f km", distanceKm);
    }

    private String conciseNetworkError(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) return "";
        return message.length() > 110 ? message.substring(0, 110) + "…" : message;
    }

    private Button fuelGoalButton(String label, int flag, int mask) {
        boolean selected = (mask & flag) != 0;
        Button button = action((selected ? "✓  " : "○  ") + label, selected ? mint : Color.rgb(235, 240, 246), ink);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setContentDescription(label + (selected ? ", checked" : ", not checked"));
        button.setOnClickListener(view -> {
            JourneyStore.toggleFuelGoal(this, flag);
            refreshCurrentPageKeepingScroll(this::showMeals);
        });
        return button;
    }

    private void showUpdate() {
        showUpdate(null);
    }

    /** Renders a persisted update immediately, then refreshes the public channel in the background. */
    private void showUpdate(UpdateManager.Info knownInfo) {
        LinearLayout body = appPage("Settings");
        final int updatePage = pageGeneration;
        otaPageGeneration = updatePage;
        showingUpdatePage = true;
        title(body, "UPDATES", "Keep your coach current", "Version " + BuildConfig.VERSION_NAME);
        LinearLayout statusCard = card();
        TextView status = heading("Ready to check for an update", 18, ink);
        statusCard.addView(status);
        // Kept for the update-state renderer but intentionally not displayed: this page is customer-facing.
        TextView network = text("", 1, Color.TRANSPARENT);
        ProgressBar downloadProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        downloadProgress.setMax(100);
        downloadProgress.setProgressTintList(ColorStateList.valueOf(mint));
        downloadProgress.setProgressBackgroundTintList(ColorStateList.valueOf(cardLine));
        downloadProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(10));
        progressParams.topMargin = dp(16);
        statusCard.addView(downloadProgress, progressParams);
        TextView progressLabel = text("", 13, muted);
        progressLabel.setVisibility(View.GONE);
        statusCard.addView(progressLabel);
        body.addView(statusCard);
        gap(body, 12);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        body.addView(actions);

        UpdateManager.Info restored = knownInfo != null ? knownInfo : UpdateManager.savedInfo(this);
        if (hasSavedUpdateState(restored)) {
            renderUpdateState(restored, status, network, downloadProgress, progressLabel, actions);
        } else {
            Button check = primary("Check for update");
            check.setOnClickListener(view -> checkForUpdate(updatePage, status, network, downloadProgress, progressLabel, actions));
            addButton(actions, check);
        }
    }

    private void checkForUpdate(int updatePage, TextView status, TextView network, ProgressBar downloadProgress, TextView progressLabel, LinearLayout actions) {
        status.setText("Checking for an update…");
        actions.removeAllViews();
        new Thread(() -> {
            try {
                UpdateManager.Info info = UpdateManager.check();
                runOnUiThread(() -> {
                    if (!isCurrentUpdatePage(updatePage)) return;
                    if (info == null) {
                        UpdateManager.Info saved = UpdateManager.savedInfo(this);
                        if (hasSavedUpdateState(saved)) {
                            renderUpdateState(saved, status, network, downloadProgress, progressLabel, actions);
                            return;
                        }
                        status.setText("You have the latest version.");
                        Button check = primary("Check again");
                        check.setOnClickListener(view -> checkForUpdate(updatePage, status, network, downloadProgress, progressLabel, actions));
                        addButton(actions, check);
                        return;
                    }
                    renderUpdateState(info, status, network, downloadProgress, progressLabel, actions);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!isCurrentUpdatePage(updatePage)) return;
                    UpdateManager.Info saved = UpdateManager.savedInfo(this);
                    if (hasSavedUpdateState(saved)) {
                        renderUpdateState(saved, status, network, downloadProgress, progressLabel, actions);
                        return;
                    }
                    status.setText("Unable to check for an update.");
                    actions.removeAllViews();
                    Button retry = primary("Check for update");
                    retry.setOnClickListener(view -> checkForUpdate(updatePage, status, network, downloadProgress, progressLabel, actions));
                    addButton(actions, retry);
                });
            }
        }).start();
    }

    private boolean hasSavedUpdateState(UpdateManager.Info info) {
        if (info == null) return false;
        try {
            return UpdateManager.status(this, info).state != UpdateManager.STATE_NONE;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean isCurrentUpdatePage(int expectedPage) {
        return pageGeneration == expectedPage && otaPageGeneration == expectedPage;
    }

    private void renderUpdateState(UpdateManager.Info info, TextView status, TextView network, ProgressBar progress, TextView progressLabel, LinearLayout actions) {
        UpdateManager.DownloadStatus state;
        try {
            state = UpdateManager.status(this, info);
        } catch (RuntimeException error) {
            state = new UpdateManager.DownloadStatus(UpdateManager.STATE_FAILED, 0L, info.sizeBytes, conciseNetworkError(error));
        }
        network.setText("");
        actions.removeAllViews();
        progress.setVisibility(state.state == UpdateManager.STATE_NONE ? View.GONE : View.VISIBLE);
        progressLabel.setVisibility(state.state == UpdateManager.STATE_NONE ? View.GONE : View.VISIBLE);
        long total = state.totalBytes > 0L ? state.totalBytes : info.sizeBytes;
        int percent = UpdateManager.progressPercent(state.downloadedBytes, total);
        if (percent >= 0) {
            progress.setIndeterminate(false);
            progress.setProgress(percent, true);
            progressLabel.setText(percent + "%  •  " + formatDownloadSize(state.downloadedBytes) + " of " + formatDownloadSize(total));
        } else {
            progress.setIndeterminate(state.active());
            progressLabel.setText(state.downloadedBytes > 0L ? formatDownloadSize(state.downloadedBytes) + " downloaded" : "Waiting for download details…");
        }

        if (state.state == UpdateManager.STATE_NONE) {
            status.setText("Update available");
            Button download = primary("Download update");
            download.setOnClickListener(view -> confirmAndStartUpdateDownload(info));
            addButton(actions, download);
        } else if (state.state == UpdateManager.STATE_QUEUED || state.state == UpdateManager.STATE_DOWNLOADING || state.state == UpdateManager.STATE_PAUSED) {
            status.setText("Downloading update");
            if (state.state == UpdateManager.STATE_PAUSED) {
                boolean metered = UpdateManager.isMetered(this);
                Button resume = primary(metered ? "Resume download" : "Check download status");
                resume.setOnClickListener(view -> {
                    if (metered) confirmAndStartUpdateDownload(info);
                    else renderUpdateState(info, status, network, progress, progressLabel, actions);
                });
                addButton(actions, resume);
                gap(actions, 7);
            }
            Button cancel = lightAction("Cancel this download");
            cancel.setOnClickListener(view -> {
                UpdateManager.cancel(this, info);
                stopOtaStatusPolling();
                renderUpdateState(info, status, network, progress, progressLabel, actions);
            });
            addButton(actions, cancel);
            startOtaStatusPolling(info, status, network, progress, progressLabel, actions);
        } else if (state.state == UpdateManager.STATE_VERIFYING) {
            status.setText("Preparing update…");
            progress.setIndeterminate(true);
            progressLabel.setText("Download complete");
            verifyAndInstallUpdate(info);
        } else if (state.state == UpdateManager.STATE_READY_TO_INSTALL) {
            status.setText("Update ready to install");
            progress.setIndeterminate(false);
            progress.setProgress(100, true);
            progressLabel.setText("100%");
            Button install = primary("Install update");
            install.setOnClickListener(view -> requestVerifiedUpdateInstall(info));
            addButton(actions, install);
            gap(actions, 7);
            Button remove = lightAction("Remove download");
            remove.setOnClickListener(view -> {
                UpdateManager.cancel(this, info);
                renderUpdateState(info, status, network, progress, progressLabel, actions);
            });
            addButton(actions, remove);
        } else {
            status.setText("Update download stopped");
            progress.setIndeterminate(false);
            progress.setProgress(0);
            progressLabel.setText("");
            Button retry = primary("Download update");
            retry.setOnClickListener(view -> confirmAndStartUpdateDownload(info));
            addButton(actions, retry);
        }
    }

    private void confirmAndStartUpdateDownload(UpdateManager.Info info) {
        boolean metered = UpdateManager.isMetered(this);
        if (!metered) {
            startUpdateDownload(info, false);
            return;
        }
        String size = info.sizeBytes > 0L ? formatDownloadSize(info.sizeBytes) : "this update";
        UpdateManager.DownloadStatus existing;
        try {
            existing = UpdateManager.status(this, info);
        } catch (RuntimeException ignored) {
            existing = new UpdateManager.DownloadStatus(UpdateManager.STATE_NONE, 0L, info.sizeBytes, "");
        }
        String restart = existing.state == UpdateManager.STATE_PAUSED
                ? " The earlier Wi-Fi-only transfer will restart with mobile data enabled."
                : "";
        new AlertDialog.Builder(this)
                .setTitle("Use mobile data?")
                .setMessage("This update is " + size + ". Android will keep the download active through a temporary signal loss, but it may use your mobile-data allowance. Roaming is blocked." + restart)
                .setNegativeButton("Wait for Wi‑Fi", null)
                .setPositiveButton("Download on mobile", (dialog, which) -> startUpdateDownload(info, true))
                .show();
    }

    private void startUpdateDownload(UpdateManager.Info info, boolean allowMetered) {
        try {
            UpdateManager.enqueue(this, info, allowMetered);
            toast(allowMetered ? "Android is downloading the update over mobile data." : "Android is downloading the update.");
            // Render the saved transfer immediately. A momentary manifest outage must not hide
            // a DownloadManager job that has already begun.
            showUpdate(info);
        } catch (RuntimeException error) {
            toast("Could not start update: " + conciseNetworkError(error));
        }
    }

    private void startOtaStatusPolling(UpdateManager.Info info, TextView status, TextView network, ProgressBar progress, TextView progressLabel, LinearLayout actions) {
        if (otaStatusPoll != null) return;
        final int expectedPage = otaPageGeneration;
        otaStatusPoll = new Runnable() {
            @Override public void run() {
                otaStatusPoll = null;
                if (!isCurrentUpdatePage(expectedPage)) return;
                renderUpdateState(info, status, network, progress, progressLabel, actions);
            }
        };
        workoutHandler.postDelayed(otaStatusPoll, 850L);
    }

    private void stopOtaStatusPolling() {
        if (otaStatusPoll != null) workoutHandler.removeCallbacks(otaStatusPoll);
        otaStatusPoll = null;
    }

    private void verifyAndInstallUpdate(UpdateManager.Info info) {
        if (otaVerificationInFlight) return;
        final int expectedPage = otaPageGeneration;
        otaVerificationInFlight = true;
        new Thread(() -> {
            try {
                UpdateManager.verifyDownloaded(this, info);
                runOnUiThread(() -> {
                    otaVerificationInFlight = false;
                    if (!isCurrentUpdatePage(expectedPage)) return;
                    requestVerifiedUpdateInstall(info);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    otaVerificationInFlight = false;
                    UpdateManager.cancel(this, info);
                    if (!isCurrentUpdatePage(expectedPage)) return;
                    toast("Update could not be verified: " + conciseNetworkError(error));
                    showUpdate(info);
                });
            }
        }).start();
    }

    private void requestVerifiedUpdateInstall(UpdateManager.Info info) {
        try {
            boolean installerOpened = UpdateManager.requestInstall(this, info);
            if (!installerOpened) {
                pendingOtaInstall = info;
                toast("Allow installs for My Personal Trainer, then return here. The verified update will continue automatically.");
            }
        } catch (SecurityException | IllegalStateException error) {
            UpdateManager.cancel(this, info);
            toast("Could not open Android's installer: " + conciseNetworkError(error));
            showUpdate(info);
        } catch (Exception error) {
            // Preserve a verified APK if Android's installer activity is temporarily unavailable.
            // The user can retry without downloading the full file again.
            toast("Could not open Android's installer: " + conciseNetworkError(error));
            showUpdate(info);
        }
    }

    private String formatDownloadSize(long bytes) {
        if (bytes < 1_048_576L) return String.format(Locale.US, "%.1f KB", bytes / 1024d);
        return String.format(Locale.US, "%.1f MB", bytes / 1_048_576d);
    }

    private void showComplete() {
        if (guidedWorkoutAborted) {
            showHome();
            return;
        }
        if (restTimer != null) restTimer.cancel();
        stopSetClock();
        stopWorkoutRemainingClock();
        if (!guidedSessionRecorded) {
            long elapsed = guidedWorkoutStartedAt == 0 ? 60_000L : Math.max(60_000L, SystemClock.elapsedRealtime() - guidedWorkoutStartedAt);
            int activeMinutes = (int) Math.max(1, Math.round(elapsed / 60_000d));
            long completedAt = System.currentTimeMillis();
            JourneyStore.recordWorkoutAt(this, target, activeMinutes, completedAt);
            SetTimeStore.recordCompletedWorkout(this, completedAt, target, completedExerciseSetTimes);
            guidedSessionRecorded = true;
        }
        JourneyStore.Snapshot snapshot = JourneyStore.snapshot(this);
        LinearLayout body = appPage("Workouts");
        title(body, "SESSION COMPLETE", "Strong work", "Your " + target + " workout now appears in Progress. Complete a simple fuel check-in to close the loop.");
        LinearLayout linked = card();
        linked.addView(heading(snapshot.workoutsThisWeek + " workouts  •  " + snapshot.activeMinutesThisWeek + " guided min", 19, ink));
        linked.addView(text("This week  •  " + snapshot.fuelDaysThisWeek + " nutrition check-in days", 14, muted));
        body.addView(linked);
        gap(body, 12);
        Button fuel = primary("Log recovery fuel");
        fuel.setOnClickListener(view -> showMeals());
        addButton(body, fuel);
        gap(body, 8);
        Button progress = secondary("View connected progress");
        progress.setOnClickListener(view -> showProgress());
        addButton(body, progress);
        gap(body, 8);
        Button home = lightAction("Return home");
        home.setOnClickListener(view -> showHome());
        addButton(body, home);
    }
}
