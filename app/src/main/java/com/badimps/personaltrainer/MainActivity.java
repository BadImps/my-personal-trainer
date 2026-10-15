package com.badimps.personaltrainer;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private final int navy=Color.rgb(16,23,42), coral=Color.rgb(255,107,95), mint=Color.rgb(119,224,196), paper=Color.rgb(247,248,252);
    private String goal=Coach.GOALS[1], target="Full Body", environment=Coach.ENVIRONMENTS[0];
    private int sessionMinutes=30, trainingDays=3;
    private List<Coach.Exercise> session = new ArrayList<>();
    private TextToSpeech voice;
    private CountDownTimer restTimer;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        voice = new TextToSpeech(this, result -> { if (result == TextToSpeech.SUCCESS) voice.setLanguage(Locale.getDefault()); });
        if (AccountStore.exists(this)) LauncherIconManager.apply(this,AccountStore.get(this).trainer);
        showWelcome();
    }
    @Override protected void onDestroy() { if (restTimer != null) restTimer.cancel(); if (voice != null) voice.shutdown(); super.onDestroy(); }
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private TextView text(String value, int sp, int color) { TextView t=new TextView(this); t.setText(value); t.setTextSize(sp); t.setTextColor(color); t.setLineSpacing(dp(3),1); return t; }
    private GradientDrawable surface(int fill, int radius, int stroke, int strokeColor) { GradientDrawable bg=new GradientDrawable();bg.setColor(fill);bg.setCornerRadius(dp(radius));if(stroke>0)bg.setStroke(dp(stroke),strokeColor);return bg; }
    private LinearLayout page() { return page(true); }
    private LinearLayout page(boolean includeCoachRail) {
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(22),dp(24),dp(22),dp(32)); body.setBackgroundColor(paper);
        ScrollView scroll=new ScrollView(this);scroll.addView(body);
        if(includeCoachRail&&AccountStore.exists(this)) { LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.HORIZONTAL);shell.setBackgroundColor(navy);LinearLayout rail=new LinearLayout(this);rail.setOrientation(LinearLayout.VERTICAL);rail.setGravity(Gravity.CENTER_HORIZONTAL);rail.setPadding(dp(8),dp(24),dp(8),dp(8));String trainer=AccountStore.get(this).trainer;ImageView portrait=new ImageView(this);portrait.setImageResource("Male".equals(trainer)?R.drawable.leo_launcher:R.drawable.mia_launcher);portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);portrait.setBackground(surface(mint,99,2,Color.WHITE));portrait.setClipToOutline(true);rail.addView(portrait,new LinearLayout.LayoutParams(dp(62),dp(62)));gap(rail,6);TextView coach=text("Coach\n"+("Male".equals(trainer)?"Leo":"Mia"),11,Color.WHITE);coach.setGravity(Gravity.CENTER);rail.addView(coach,new LinearLayout.LayoutParams(-1,-2));View line=new View(this);line.setBackgroundColor(mint);LinearLayout.LayoutParams lineParams=new LinearLayout.LayoutParams(dp(4),dp(44));lineParams.topMargin=dp(22);rail.addView(line,lineParams);shell.addView(rail,new LinearLayout.LayoutParams(dp(86),-1));shell.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));setContentView(shell); }
        else setContentView(scroll);return body;
    }
    private void gap(LinearLayout box, int n) { View v=new View(this); box.addView(v,new LinearLayout.LayoutParams(1,dp(n))); }
    private Button button(String label) { Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(Color.WHITE); b.setBackground(surface(coral,14,0,Color.TRANSPARENT)); b.setElevation(dp(3)); b.setPadding(dp(12),dp(10),dp(12),dp(10)); b.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(52))); b.setOnTouchListener((v,event)->{if(event.getAction()==0)v.animate().scaleX(.975f).scaleY(.975f).setDuration(80).start();else if(event.getAction()==1||event.getAction()==3)v.animate().scaleX(1f).scaleY(1f).setDuration(130).setInterpolator(new DecelerateInterpolator()).start();return false;}); return b; }
    private void tint(Button b,int color) { b.setBackground(surface(color,14,0,Color.TRANSPARENT)); }
    private TextView badge(String value) { TextView t=text(value,13,navy); t.setPadding(dp(12),dp(8),dp(12),dp(8)); t.setBackground(surface(mint,99,0,Color.TRANSPARENT)); return t; }
    private LinearLayout darkPage() { LinearLayout b=page();b.setBackgroundColor(navy);return b; }
    private TextView darkCard(String value,int sp) { TextView card=text(value,sp,navy);card.setPadding(dp(16),dp(15),dp(16),dp(15));card.setBackground(surface(Color.WHITE,18,0,Color.TRANSPARENT));card.setElevation(dp(3));return card; }
    private void header(LinearLayout b, String eyebrow, String title, String subtitle) { b.addView(text(eyebrow.toUpperCase(Locale.ROOT),12,coral)); gap(b,5); b.addView(text(title,30,navy)); gap(b,8); b.addView(text(subtitle,16,Color.rgb(71,83,108))); gap(b,20); }
    private void nav(LinearLayout b, String active) {
        gap(b,22); LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels={"Home","Plans","Library","Progress","Account"};
        String[] icons={"●","▣","▶","↗","◌"}; int i=0;
        for(String label:labels){Button item=new Button(this);item.setText(icons[i++]+"\n"+label);item.setAllCaps(false);item.setTextSize(10);item.setTextColor(label.equals(active)?navy:Color.WHITE);item.setBackground(surface(label.equals(active)?mint:navy,12,0,Color.TRANSPARENT));item.setOnClickListener(v->{if(label.equals("Home"))showHome();else if(label.equals("Plans"))showPlans();else if(label.equals("Library"))showLibrary();else if(label.equals("Progress"))showProgress();else showProfile();});row.addView(item,new LinearLayout.LayoutParams(0,dp(50),1));} b.addView(row);
    }

    private void showHome() {
        AccountStore.Profile profile=AccountStore.get(this);LinearLayout b=darkPage();b.addView(text("Today",34,Color.WHITE));gap(b,4);b.addView(text("Showing up today builds a stronger tomorrow.",16,Color.rgb(211,231,230)));gap(b,18);
        LinearLayout next=new LinearLayout(this);next.setOrientation(LinearLayout.VERTICAL);next.setPadding(dp(14),dp(14),dp(14),dp(14));next.setBackground(surface(Color.WHITE,20,0,Color.TRANSPARENT));next.setElevation(dp(4));List<Coach.Exercise> sample=Coach.library().get(target);if(sample!=null&&!sample.isEmpty()){int visual=ExerciseVisuals.forExercise(this,profile.trainer,sample.get(0).name);if(visual!=0){AnimatedGifView gif=new AnimatedGifView(this,visual);next.addView(gif,new LinearLayout.LayoutParams(-1,dp(150)));}}next.addView(text("NEXT WORKOUT",12,Color.rgb(76,108,151)));next.addView(text(target+" strength",22,navy));next.addView(text(sessionMinutes+" min  •  "+goal+"  •  "+environment,14,Color.rgb(71,83,108)));Button start=button("▶  Start workout");start.setOnClickListener(v->{session=Coach.recommendation(target,goal,environment);showActive(0,1);});next.addView(start);b.addView(next);gap(b,16);
        b.addView(text("Weekly progress",20,Color.WHITE));gap(b,8);b.addView(darkCard("Mon     Tue     Wed     Thu     Fri\n  ✓       ✓       ◌       ○       ○\n100%   100%    60%      0%      0%",16));gap(b,12);LinearLayout stats=new LinearLayout(this);TextView sessions=darkCard("✦  "+trainingDays+"\nWorkouts this week",16);TextView momentum=darkCard("↗  Keep going\nYour next session is ready",16);stats.addView(sessions,new LinearLayout.LayoutParams(0,-2,1));View spacer=new View(this);stats.addView(spacer,new LinearLayout.LayoutParams(dp(8),1));stats.addView(momentum,new LinearLayout.LayoutParams(0,-2,1));b.addView(stats);gap(b,14);Button plan=button("View my plan");tint(plan,Color.rgb(26,58,72));plan.setOnClickListener(v->showPlans());b.addView(plan);
    }

    private void showPlans() {
        if(session.isEmpty())session=Coach.recommendation(target,goal,environment);LinearLayout b=darkPage();b.addView(text("My Plan",34,Color.WHITE));gap(b,14);LinearLayout days=new LinearLayout(this);String[] weekdays={"Mon\n12","Tue\n13","Wed\n14","Thu\n15","Fri\n16","Sat\n17","Sun\n18"};for(int i=0;i<weekdays.length;i++){TextView day=text(weekdays[i],13,i==2?navy:Color.WHITE);day.setGravity(Gravity.CENTER);day.setPadding(dp(4),dp(10),dp(4),dp(10));day.setBackground(surface(i==2?mint:Color.rgb(28,58,76),12,0,Color.TRANSPARENT));days.addView(day,new LinearLayout.LayoutParams(0,-2,1));}b.addView(days);gap(b,16);b.addView(text("STRENGTH DAY  •  "+sessionMinutes+" MIN",12,mint));TextView title=text(target+" strength",24,Color.WHITE);b.addView(title);gap(b,10);for(int i=0;i<session.size();i++){Coach.Exercise e=session.get(i);TextView row=darkCard((i+1)+"   "+e.name+"\n     "+e.sets+" sets × "+e.reps+" reps",16);b.addView(row);gap(b,7);}Button open=button("View session");open.setOnClickListener(v->showActive(0,1));b.addView(open);gap(b,8);Button adjust=button("Adjust this plan");tint(adjust,Color.rgb(26,58,72));adjust.setOnClickListener(v->showWizard());b.addView(adjust);
    }

    private void showProgress() {
        LinearLayout b=page(); header(b,"PROGRESS","Build consistency.","Track a private weight trend, revisit your food ideas, and keep your next workout simple.");
        if(WeightStore.configured(this)) b.addView(badge(WeightStore.summary(this))); else b.addView(badge("SET A PRIVATE STARTING POINT"));gap(b,15);
        Button weight=button("Weight trend check-in");weight.setOnClickListener(v->showWeightTracker());b.addView(weight);gap(b,9);
        Button fuel=button("Fuel ideas for "+goal);fuel.setBackgroundColor(Color.rgb(45,91,101));fuel.setOnClickListener(v->showMeals());b.addView(fuel);gap(b,15);
        b.addView(text("Keep it useful",18,navy));gap(b,5);b.addView(text("Use trends as a personal check-in, not as a diagnosis. Adjust load, range, pace or rest when needed.",15,Color.rgb(71,83,108)));nav(b,"Progress");
    }

    private void showProfile() {
        AccountStore.Profile profile=AccountStore.get(this); String coach="Male".equals(profile.trainer)?"Coach Leo":"Coach Mia";
        LinearLayout b=page();header(b,"ACCOUNT",""+profile.username,"Your trainer, preferences, and updates stay on this device.");
        b.addView(badge(coach+"  •  "+profile.email));gap(b,16);b.addView(text("Privacy",19,navy));gap(b,5);b.addView(text("This version stores your profile locally and does not send your registration details to a server.",15,Color.rgb(71,83,108)));gap(b,14);
        Button updates=button("↻  Check for OTA update");tint(updates,Color.rgb(91,102,126));updates.setOnClickListener(v->showUpdate());b.addView(updates);gap(b,9);Button plans=button("✦  Adjust workout preferences");plans.setOnClickListener(v->showWizard());b.addView(plans);gap(b,9);Button logout=button("Sign out");tint(logout,Color.rgb(91,102,126));logout.setOnClickListener(v->showWelcome());b.addView(logout);nav(b,"Account");
    }

    private void showWelcome() {
        LinearLayout b=page(false);b.setBackgroundColor(navy);gap(b,20);TextView welcome=text("Welcome",42,Color.WHITE);welcome.setGravity(Gravity.CENTER);b.addView(welcome,new LinearLayout.LayoutParams(-1,-2));gap(b,6);TextView motto=text("Your Health is Your Wealth",17,mint);motto.setGravity(Gravity.CENTER);b.addView(motto,new LinearLayout.LayoutParams(-1,-2));gap(b,28);
        LinearLayout coaches=new LinearLayout(this);coaches.setOrientation(LinearLayout.HORIZONTAL);String[] names={"Mia","Leo"};int[] portraits={R.drawable.mia_launcher,R.drawable.leo_launcher};for(int i=0;i<2;i++){LinearLayout person=new LinearLayout(this);person.setOrientation(LinearLayout.VERTICAL);person.setGravity(Gravity.CENTER);ImageView image=new ImageView(this);image.setImageResource(portraits[i]);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(surface(Color.rgb(25,57,71),99,2,mint));image.setClipToOutline(true);person.addView(image,new LinearLayout.LayoutParams(dp(132),dp(132)));gap(person,8);TextView name=text("Coach "+names[i],15,Color.WHITE);name.setGravity(Gravity.CENTER);person.addView(name);coaches.addView(person,new LinearLayout.LayoutParams(0,-2,1));}b.addView(coaches);gap(b,30);
        Button login=button("Sign in");login.setOnClickListener(v->showLogin());b.addView(login);gap(b,10);Button create=button("Create account");tint(create,Color.rgb(26,58,72));create.setOnClickListener(v->showRegistration());b.addView(create);
    }

    private void showLogin() {
        LinearLayout b=page(false);b.setBackgroundColor(navy);gap(b,36);TextView title=text("Welcome back",32,Color.WHITE);b.addView(title);gap(b,7);b.addView(text("Your coach and plan are ready when you are.",16,mint));gap(b,24);
        EditText email=input("Email address",InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);b.addView(email);gap(b,9);EditText password=input("Password",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);b.addView(password);gap(b,16);
        Button login=button("Sign in");login.setOnClickListener(v->{if(AccountStore.authenticate(this,email.getText().toString(),password.getText().toString())){LauncherIconManager.apply(this,AccountStore.get(this).trainer);showHome();}else toast("That email or password does not match this local profile.");});b.addView(login);gap(b,9);Button back=button("Back");tint(back,Color.rgb(26,58,72));back.setOnClickListener(v->showWelcome());b.addView(back);
    }

    private void showRegistration() {
        LinearLayout b=page(false); header(b,"WELCOME","Create your trainer profile","Your email, username and password stay on this device in this version. Choose the coach persona you prefer.");
        ImageView cast=new ImageView(this); cast.setImageResource(R.drawable.trainer_cast); cast.setScaleType(ImageView.ScaleType.CENTER_CROP); b.addView(cast,new LinearLayout.LayoutParams(-1,dp(190))); gap(b,16);
        EditText email=input("Email address",InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); b.addView(email); gap(b,9);
        EditText username=input("Username",InputType.TYPE_CLASS_TEXT); b.addView(username); gap(b,9);
        EditText password=input("Password (8 or more characters)",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); b.addView(password); gap(b,16);
        b.addView(text("Choose your personal trainer",18,navy)); RadioGroup trainers=new RadioGroup(this); trainers.setOrientation(RadioGroup.VERTICAL);
        RadioButton female=new RadioButton(this); female.setId(R.id.trainer_female); female.setText("Female personal trainer — Coach Mia"); female.setTextSize(16); female.setTextColor(navy); female.setChecked(true); trainers.addView(female);
        RadioButton male=new RadioButton(this); male.setId(R.id.trainer_male); male.setText("Male personal trainer — Coach Leo"); male.setTextSize(16); male.setTextColor(navy); trainers.addView(male); b.addView(trainers); gap(b,18);
        Button create=button("Create my profile"); create.setOnClickListener(v->{String e=email.getText().toString().trim(),u=username.getText().toString().trim(),p=password.getText().toString(); if(!e.contains("@")||!e.contains(".")){toast("Enter a valid email address.");return;} if(u.length()<3){toast("Choose a username with at least 3 characters.");return;} if(p.length()<8){toast("Use a password with at least 8 characters.");return;} String selected=trainers.getCheckedRadioButtonId()==R.id.trainer_male?"Male":"Female";AccountStore.create(this,e,u,p,selected);LauncherIconManager.apply(this,selected);showPlanGoal();}); b.addView(create);
    }
    private EditText input(String hint,int kind) { EditText e=new EditText(this); e.setHint(hint); e.setTextSize(16); e.setTextColor(navy); e.setHintTextColor(Color.rgb(91,102,126)); e.setInputType(kind); e.setPadding(dp(14),dp(8),dp(14),dp(8)); e.setBackground(surface(Color.WHITE,12,1,Color.rgb(215,224,220))); e.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(54))); return e; }
    private void toast(String value) { Toast.makeText(this,value,Toast.LENGTH_SHORT).show(); }

    private void showWeightTracker() {
        LinearLayout b=page(); header(b,"WEIGHT TREND","Check your direction, kindly.","This private log compares your entries with your own target. It does not diagnose health, body composition, or fitness.");
        if (!WeightStore.configured(this)) {
            EditText current=input("Starting weight",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); b.addView(current); gap(b,9);
            EditText targetWeight=input("Personal target weight",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); b.addView(targetWeight); gap(b,12);
            b.addView(text("Unit",18,navy)); RadioGroup units=new RadioGroup(this); units.setOrientation(RadioGroup.HORIZONTAL); RadioButton kg=new RadioButton(this);kg.setId(R.id.unit_kg);kg.setText("kg");kg.setChecked(true);kg.setTextColor(navy);units.addView(kg);RadioButton lb=new RadioButton(this);lb.setId(R.id.unit_lb);lb.setText("lb");lb.setTextColor(navy);units.addView(lb);b.addView(units);gap(b,18);
            Button save=button("Save my starting point"); save.setOnClickListener(v->{try{float now=Float.parseFloat(current.getText().toString());float targetValue=Float.parseFloat(targetWeight.getText().toString());if(now<=0||targetValue<=0)throw new NumberFormatException();WeightStore.start(this,now,targetValue,units.getCheckedRadioButtonId()==R.id.unit_lb?"lb":"kg");showWeightTracker();}catch(Exception e){toast("Enter positive weight values to begin.");}}); b.addView(save);
        } else {
            b.addView(text(WeightStore.summary(this),17,navy)); gap(b,18); EditText current=input("Today's weight",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); b.addView(current); gap(b,12);
            Button check=button("Save check-in"); check.setOnClickListener(v->{try{float value=Float.parseFloat(current.getText().toString());if(value<=0)throw new NumberFormatException();WeightStore.checkIn(this,value);showWeightTracker();}catch(Exception e){toast("Enter a positive weight to check in.");}}); b.addView(check); gap(b,16);
            b.addView(text("If weight changes unexpectedly, or food and weight are causing distress, pause the goal and speak with a qualified healthcare professional.",14,Color.rgb(71,83,108)));
        }
        nav(b,"Progress");
    }

    private void showUpdate() {
        LinearLayout b=page(); header(b,"APP UPDATES","Keep your coach current.","Updates are checked over HTTPS. A download must match its published SHA-256, then Android verifies the signing identity before it installs.");
        TextView status=text("Checking the update channel…",17,navy); b.addView(status);
        Button home=button("Back to profile"); home.setBackgroundColor(navy); gap(b,20); b.addView(home); home.setOnClickListener(v->showProfile());
        new Thread(()-> { try { UpdateManager.Info info=UpdateManager.check(); runOnUiThread(()-> { if(info==null){status.setText("You already have the latest published version.");return;} status.setText("Version "+info.name+" is ready to download."); Button install=button("Download and install "+info.name); install.setOnClickListener(v->downloadUpdate(info,status)); b.addView(install,1); }); } catch(Exception e) { runOnUiThread(()->status.setText("No update is published yet. Check again after the OTA channel is released.")); } }).start();
    }
    private void downloadUpdate(UpdateManager.Info info, TextView status) { status.setText("Downloading and verifying update…"); new Thread(()-> { try { UpdateManager.downloadAndInstall(this,info); runOnUiThread(()->status.setText("Verified. Android will now ask you to confirm the install.")); } catch(Exception e) { runOnUiThread(()->status.setText("Update could not be verified: "+e.getMessage())); } }).start(); }

    private void showWizard() { showPlanGoal(); }
    private void step(LinearLayout b,int number,String title,String copy) { b.addView(badge("PLAN SETUP  •  STEP "+number+" OF 4"));gap(b,14);header(b,"PLAN WIZARD",title,copy); }
    private void showPlanGoal() {
        LinearLayout b=page();step(b,1,"Choose your goal","Your goal adjusts rep targets, rest and the coaching emphasis.");
        RadioGroup goals=new RadioGroup(this);int[] ids={R.id.goal_strength,R.id.goal_muscle,R.id.goal_fat_loss,R.id.goal_endurance,R.id.goal_wellbeing};
        for(int i=0;i<Coach.GOALS.length;i++){RadioButton option=new RadioButton(this);option.setId(ids[i]);option.setText(Coach.GOALS[i]);option.setTextSize(16);option.setTextColor(navy);option.setPadding(0,dp(8),0,dp(8));option.setChecked(goal.equals(Coach.GOALS[i]));goals.addView(option);}goals.setOnCheckedChangeListener((g,id)->goal=id==R.id.goal_strength?Coach.GOALS[0]:id==R.id.goal_muscle?Coach.GOALS[1]:id==R.id.goal_fat_loss?Coach.GOALS[2]:id==R.id.goal_endurance?Coach.GOALS[3]:Coach.GOALS[4]);b.addView(goals);gap(b,16);Button next=button("Continue to workout focus");next.setOnClickListener(v->showPlanFocus());b.addView(next);
    }
    private void showPlanFocus() {
        LinearLayout b=page();step(b,2,"What do you want to train?","Tap a focus to see an animated preview from your selected coach.");
        List<Coach.Exercise> choices=Coach.library().get(target);if(choices!=null&&!choices.isEmpty()){Coach.Exercise first=choices.get(0);int visual=ExerciseVisuals.forExercise(this,AccountStore.get(this).trainer,first.name);if(visual!=0){AnimatedGifView gif=new AnimatedGifView(this,visual);gif.setBackground(surface(Color.WHITE,18,1,Color.rgb(215,224,220)));b.addView(gif,new LinearLayout.LayoutParams(-1,dp(190)));gap(b,9);}b.addView(text("PREVIEW  •  "+first.name,14,navy));gap(b,12);}
        for(String choice:Coach.TARGETS){Button pick=button((choice.equals(target)?"✓  ":"   ")+choice);tint(pick,choice.equals(target)?coral:navy);pick.setOnClickListener(v->{target=choice;showPlanFocus();});b.addView(pick);LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)pick.getLayoutParams();lp.height=dp(44);lp.bottomMargin=dp(7);pick.setLayoutParams(lp);}gap(b,8);Button next=button("Continue to training setup");next.setOnClickListener(v->showPlanContext());b.addView(next);Button back=button("Back");tint(back,navy);back.setOnClickListener(v->showPlanGoal());b.addView(back);
    }
    private void showPlanContext() {
        LinearLayout b=page();step(b,3,"Set your training context","Only compatible movements are recommended for your chosen location.");b.addView(text("Where will you train?",18,navy));gap(b,6);
        for(String place:Coach.ENVIRONMENTS){Button pick=button((place.equals(environment)?"✓  ":"   ")+place);tint(pick,place.equals(environment)?coral:navy);pick.setOnClickListener(v->{environment=place;showPlanContext();});b.addView(pick);LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)pick.getLayoutParams();lp.height=dp(44);lp.bottomMargin=dp(7);pick.setLayoutParams(lp);}gap(b,9);b.addView(text("Session length",18,navy));LinearLayout times=new LinearLayout(this);for(int minutes:new int[]{20,30,45}){Button pick=button(minutes+" min");tint(pick,minutes==sessionMinutes?coral:navy);pick.setOnClickListener(v->{sessionMinutes=minutes;showPlanContext();});times.addView(pick,new LinearLayout.LayoutParams(0,dp(48),1));}b.addView(times);gap(b,16);Button next=button("Review my plan");next.setOnClickListener(v->showPlanReview());b.addView(next);Button back=button("Back");tint(back,navy);back.setOnClickListener(v->showPlanFocus());b.addView(back);
    }
    private void showPlanReview() { session=Coach.recommendation(target,goal,environment);LinearLayout b=page();step(b,4,"Your plan is ready","Review it before you begin. You can revisit these choices any time.");b.addView(badge(goal+"  •  "+environment+"  •  "+sessionMinutes+" min"));gap(b,14);for(int i=0;i<session.size();i++){Coach.Exercise e=session.get(i);TextView card=text("0"+(i+1)+"  "+e.name+"\n"+e.sets+" sets × "+e.reps+" reps  •  "+e.restSeconds+" sec rest",16,navy);card.setPadding(dp(15),dp(13),dp(15),dp(13));card.setBackground(surface(Color.WHITE,16,1,Color.rgb(215,224,220)));b.addView(card);gap(b,8);}Button create=button("Save and start this plan");create.setOnClickListener(v->showPlan());b.addView(create);Button back=button("Back");tint(back,navy);back.setOnClickListener(v->showPlanContext());b.addView(back); }

    private void showPlan() {
        LinearLayout b=page(); header(b,"YOUR PLAN",""+target+" session",""+goal+" • "+environment+" • " + session.size() + " exercises • about " + session.size()*8 + " minutes");
        b.addView(text(Coach.goalSummary(goal),16,Color.rgb(71,83,108))); gap(b,14);
        for (int i=0;i<session.size();i++) { Coach.Exercise e=session.get(i); TextView card=text("0"+(i+1)+"  "+e.name+"\n"+e.sets+" sets × "+e.reps+" reps  •  "+e.restSeconds+" sec rest\n"+e.cue,16,navy); card.setPadding(dp(15),dp(13),dp(15),dp(13)); card.setBackground(surface(Color.WHITE,16,1,Color.rgb(215,224,220))); card.setElevation(dp(2)); b.addView(card); gap(b,8); }
        gap(b,10); Button go=button("Start guided workout"); go.setOnClickListener(v->showActive(0,1)); b.addView(go); gap(b,10); Button back=button("Adjust choices"); back.setBackgroundColor(navy); back.setOnClickListener(v->showWizard()); b.addView(back);
    }

    private void showActive(int index, int set) {
        if (index >= session.size()) { showComplete(); return; }
        Coach.Exercise e=session.get(index); LinearLayout b=darkPage();b.addView(text("Upper body",31,Color.WHITE));gap(b,5);b.addView(text("Exercise "+(index+1)+" of "+session.size()+"     Set "+set+" of "+e.sets,14,mint));gap(b,14);
        int visual=ExerciseVisuals.forExercise(this,AccountStore.get(this).trainer,e.name);
        if(visual!=0){AnimatedGifView demo=new AnimatedGifView(this,visual);demo.setBackground(surface(Color.WHITE,20,0,Color.TRANSPARENT));b.addView(demo,new LinearLayout.LayoutParams(-1,dp(240)));}
        else {ExerciseAnimationView demo=new ExerciseAnimationView(this);b.addView(demo,new LinearLayout.LayoutParams(-1,dp(240)));}
        gap(b,12);
        b.addView(text(e.name,24,Color.WHITE));gap(b,3);b.addView(text(e.sets+" sets × "+e.reps+" reps",17,mint));gap(b,10);TextView cue=darkCard("Coach cue\n"+e.cue,15);b.addView(cue);gap(b,12);LinearLayout counters=new LinearLayout(this);TextView setCard=darkCard("Set\n"+set+" / "+e.sets,17);TextView restCard=darkCard("Rest\n"+format(e.restSeconds),17);TextView repCard=darkCard("Reps\n0 / "+e.reps,17);counters.addView(setCard,new LinearLayout.LayoutParams(0,-2,1));counters.addView(restCard,new LinearLayout.LayoutParams(0,-2,1));counters.addView(repCard,new LinearLayout.LayoutParams(0,-2,1));b.addView(counters);gap(b,14);
        Button done=button("✓  Complete set"); done.setOnClickListener(v->startRest(index,set,e)); b.addView(done); gap(b,8);Button callout=button("Call out rep guidance");tint(callout,Color.rgb(26,58,72));callout.setOnClickListener(v->say("Set "+set+". Complete "+e.reps+" controlled reps. "+e.cue));b.addView(callout);
    }

    private void startRest(int index, int set, Coach.Exercise e) {
        LinearLayout b=page(); header(b,"REST CLOCK","Recover, then go again.","Systematic rest between sets helps you train consistently.");
        TextView timer=text(format(e.restSeconds),52,navy); timer.setGravity(Gravity.CENTER); b.addView(timer,new LinearLayout.LayoutParams(-1,dp(100))); b.addView(text("Breathe slowly. Sip water if you need it. Your next set will begin when the timer ends.",16,Color.rgb(71,83,108))); gap(b,16);
        Button skip=button("Skip rest and continue"); skip.setOnClickListener(v->{if(restTimer!=null)restTimer.cancel(); advance(index,set,e);}); b.addView(skip);
        say("Set complete. Rest for "+e.restSeconds+" seconds.");
        restTimer=new CountDownTimer(e.restSeconds*1000L,1000) {
            public void onTick(long left) { int seconds=(int)Math.ceil(left/1000d); timer.setText(format(seconds)); if(seconds==10) say("Ten seconds remaining."); }
            public void onFinish() { say("Rest complete. Next set."); advance(index,set,e); }
        }.start();
    }
    private String format(int seconds) { return String.format(Locale.US,"%02d:%02d",seconds/60,seconds%60); }
    private void advance(int index,int set,Coach.Exercise e) { if(set<e.sets) showActive(index,set+1); else showActive(index+1,1); }
    private void say(String words) { if(voice!=null) voice.speak(words,TextToSpeech.QUEUE_FLUSH,null,"coach"); }

    private void showLibrary() {
        String trainer=AccountStore.get(this).trainer;
        LinearLayout b=page(); header(b,"EXERCISE LIBRARY","Every move, animated.","Each move has a looping Coach "+("Male".equals(trainer)?"Leo":"Mia")+" demonstration. Choose a movement that matches your equipment and ability.");
        for (String group:Coach.TARGETS) { b.addView(text("✦  "+group,21,navy)); gap(b,5); for(Coach.Exercise e:Coach.library().get(group)) { LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setPadding(dp(10),dp(10),dp(10),dp(10));card.setBackground(surface(Color.WHITE,18,1,Color.rgb(215,224,220)));card.setElevation(dp(2));int visual=ExerciseVisuals.forExercise(this,trainer,e.name);if(visual!=0){AnimatedGifView image=new AnimatedGifView(this,visual);card.addView(image,new LinearLayout.LayoutParams(dp(136),dp(102)));}TextView details=text(e.name+"\n"+e.description+"\n"+e.cue,14,Color.rgb(71,83,108));details.setPadding(dp(12),0,0,0);card.addView(details,new LinearLayout.LayoutParams(0,-2,1));b.addView(card);gap(b,10); } gap(b,9); }
        nav(b,"Library");
    }
    private void showMeals() {
        LinearLayout b=page(); header(b,"FUEL IDEAS","Simple meals for "+goal,"Food recommendations are flexible ideas — adjust portions and ingredients to your preferences, budget, allergies and dietary needs.");
        for(String meal:Coach.meals(goal)) { TextView item=text("•  "+meal,17,navy); item.setPadding(dp(15),dp(14),dp(15),dp(14)); item.setBackgroundColor(Color.WHITE); b.addView(item); gap(b,8); }
        gap(b,8); b.addView(text("General fuel suggestion",19,navy)); gap(b,5); b.addView(text("Aim for protein and colourful plants across the day, carbohydrates around training if useful, and regular hydration. For medical nutrition or weight-change targets, consult a registered dietitian.",15,Color.rgb(71,83,108))); gap(b,20);
        nav(b,"Progress");
    }
    private void showComplete() { LinearLayout b=page(); header(b,"SESSION COMPLETE","Strong work.","You completed your "+target+" plan. Save the feeling, hydrate, and let your next session start with a fresh choice."); Button home=button("Return home"); home.setOnClickListener(v->showHome()); b.addView(home); }
}
