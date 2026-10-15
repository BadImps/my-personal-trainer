package com.badimps.personaltrainer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
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
    private List<Coach.Exercise> session = new ArrayList<>();
    private TextToSpeech voice;
    private CountDownTimer restTimer;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        voice = new TextToSpeech(this, result -> { if (result == TextToSpeech.SUCCESS) voice.setLanguage(Locale.getDefault()); });
        if (AccountStore.exists(this)) showHome(); else showRegistration();
    }
    @Override protected void onDestroy() { if (restTimer != null) restTimer.cancel(); if (voice != null) voice.shutdown(); super.onDestroy(); }
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private TextView text(String value, int sp, int color) { TextView t=new TextView(this); t.setText(value); t.setTextSize(sp); t.setTextColor(color); t.setLineSpacing(dp(3),1); return t; }
    private LinearLayout page() { LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(22),dp(24),dp(22),dp(32)); body.setBackgroundColor(paper); ScrollView s=new ScrollView(this); s.addView(body); setContentView(s); return body; }
    private void gap(LinearLayout box, int n) { View v=new View(this); box.addView(v,new LinearLayout.LayoutParams(1,dp(n))); }
    private Button button(String label) { Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(Color.WHITE); b.setBackgroundColor(coral); b.setPadding(dp(12),dp(10),dp(12),dp(10)); b.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(52))); return b; }
    private TextView badge(String value) { TextView t=text(value,13,navy); t.setPadding(dp(12),dp(8),dp(12),dp(8)); t.setBackgroundColor(mint); return t; }
    private void header(LinearLayout b, String eyebrow, String title, String subtitle) { b.addView(text(eyebrow.toUpperCase(Locale.ROOT),12,coral)); gap(b,5); b.addView(text(title,30,navy)); gap(b,8); b.addView(text(subtitle,16,Color.rgb(71,83,108))); gap(b,20); }

    private void showHome() {
        AccountStore.Profile profile=AccountStore.get(this);
        String coach=profile.trainer.equals("Male") ? "Coach Leo" : "Coach Mia";
        LinearLayout b=page(); header(b,"MY PERSONAL TRAINER","Hi, "+profile.username+".",coach+" is ready to build a plan. Choose a focus and goal for your offline session.");
        TextView today=badge("TODAY  •  " + target + "  •  " + environment); b.addView(today); gap(b,18);
        Button start=button("Build my workout"); start.setOnClickListener(v->showWizard()); b.addView(start); gap(b,10);
        Button library=button("Explore exercise library"); library.setBackgroundColor(navy); library.setOnClickListener(v->showLibrary()); b.addView(library); gap(b,10);
        Button fuel=button("Meal ideas for my goal"); fuel.setBackgroundColor(Color.rgb(45,91,101)); fuel.setOnClickListener(v->showMeals()); b.addView(fuel); gap(b,22);
        Button weight=button("Weight trend check-in"); weight.setBackgroundColor(Color.rgb(76,108,151)); weight.setOnClickListener(v->showWeightTracker()); b.addView(weight); gap(b,10);
        Button updates=button("Check for OTA update"); updates.setBackgroundColor(Color.rgb(91,102,126)); updates.setOnClickListener(v->showUpdate()); b.addView(updates); gap(b,22);
        b.addView(text("A note from your coach",18,navy)); gap(b,6); b.addView(text("Adjust load, range, pace or rest when needed. Stop if you feel pain, and get qualified medical or coaching advice for injuries, conditions or nutrition needs.",14,Color.rgb(71,83,108)));
    }

    private void showRegistration() {
        LinearLayout b=page(); header(b,"WELCOME","Create your trainer profile","Your email, username and password stay on this device in this version. Choose the coach persona you prefer.");
        EditText email=input("Email address",InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); b.addView(email); gap(b,9);
        EditText username=input("Username",InputType.TYPE_CLASS_TEXT); b.addView(username); gap(b,9);
        EditText password=input("Password (8 or more characters)",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); b.addView(password); gap(b,16);
        b.addView(text("Choose your personal trainer",18,navy)); RadioGroup trainers=new RadioGroup(this); trainers.setOrientation(RadioGroup.VERTICAL);
        RadioButton female=new RadioButton(this); female.setId(R.id.trainer_female); female.setText("Female personal trainer — Coach Mia"); female.setTextSize(16); female.setTextColor(navy); female.setChecked(true); trainers.addView(female);
        RadioButton male=new RadioButton(this); male.setId(R.id.trainer_male); male.setText("Male personal trainer — Coach Leo"); male.setTextSize(16); male.setTextColor(navy); trainers.addView(male); b.addView(trainers); gap(b,18);
        Button create=button("Create my profile"); create.setOnClickListener(v->{String e=email.getText().toString().trim(),u=username.getText().toString().trim(),p=password.getText().toString(); if(!e.contains("@")||!e.contains(".")){toast("Enter a valid email address.");return;} if(u.length()<3){toast("Choose a username with at least 3 characters.");return;} if(p.length()<8){toast("Use a password with at least 8 characters.");return;} AccountStore.create(this,e,u,p,trainers.getCheckedRadioButtonId()==R.id.trainer_male?"Male":"Female"); showHome();}); b.addView(create);
    }
    private EditText input(String hint,int kind) { EditText e=new EditText(this); e.setHint(hint); e.setTextSize(16); e.setTextColor(navy); e.setHintTextColor(Color.rgb(91,102,126)); e.setInputType(kind); e.setPadding(dp(12),dp(8),dp(12),dp(8)); e.setBackgroundColor(Color.WHITE); e.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(54))); return e; }
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
        gap(b,20); Button home=button("Back to home");home.setBackgroundColor(navy);home.setOnClickListener(v->showHome());b.addView(home);
    }

    private void showUpdate() {
        LinearLayout b=page(); header(b,"APP UPDATES","Keep your coach current.","Updates are checked over HTTPS. A download must match its published SHA-256, then Android verifies the signing identity before it installs.");
        TextView status=text("Checking the update channel…",17,navy); b.addView(status);
        Button home=button("Back to home"); home.setBackgroundColor(navy); gap(b,20); b.addView(home); home.setOnClickListener(v->showHome());
        new Thread(()-> { try { UpdateManager.Info info=UpdateManager.check(); runOnUiThread(()-> { if(info==null){status.setText("You already have the latest published version.");return;} status.setText("Version "+info.name+" is ready to download."); Button install=button("Download and install "+info.name); install.setOnClickListener(v->downloadUpdate(info,status)); b.addView(install,1); }); } catch(Exception e) { runOnUiThread(()->status.setText("No update is published yet. Check again after the OTA channel is released.")); } }).start();
    }
    private void downloadUpdate(UpdateManager.Info info, TextView status) { status.setText("Downloading and verifying update…"); new Thread(()-> { try { UpdateManager.downloadAndInstall(this,info); runOnUiThread(()->status.setText("Verified. Android will now ask you to confirm the install.")); } catch(Exception e) { runOnUiThread(()->status.setText("Update could not be verified: "+e.getMessage())); } }).start(); }

    private void showWizard() {
        LinearLayout b=page(); header(b,"WORKOUT WIZARD","What are we training today?","Step 1 of 2 — your answers shape exercise order, sets, reps and rest.");
        b.addView(text("Your current goal",18,navy)); RadioGroup goals=new RadioGroup(this); goals.setOrientation(LinearLayout.VERTICAL);
        int[] goalIds={R.id.goal_strength,R.id.goal_muscle,R.id.goal_fat_loss,R.id.goal_endurance,R.id.goal_wellbeing};
        for (int i=0;i<Coach.GOALS.length;i++) { RadioButton r=new RadioButton(this); r.setText(Coach.GOALS[i]); r.setTextSize(16); r.setTextColor(navy); r.setPadding(0,dp(7),0,dp(7)); r.setId(goalIds[i]); r.setChecked(goal.equals(Coach.GOALS[i])); goals.addView(r); }
        goals.setOnCheckedChangeListener((g,id)->goal=id==R.id.goal_strength?Coach.GOALS[0]:id==R.id.goal_muscle?Coach.GOALS[1]:id==R.id.goal_fat_loss?Coach.GOALS[2]:id==R.id.goal_endurance?Coach.GOALS[3]:Coach.GOALS[4]); b.addView(goals); gap(b,18);
        b.addView(text("Choose a focus",18,navy)); gap(b,6); LinearLayout rows=new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL);
        for (String choice:Coach.TARGETS) { Button pick=button(choice); pick.setTextColor(navy); pick.setBackgroundColor(choice.equals(target)?mint:Color.WHITE); pick.setOnClickListener(v->{ target=choice; showWizard(); }); rows.addView(pick); LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)pick.getLayoutParams(); lp.height=dp(44); lp.bottomMargin=dp(7); pick.setLayoutParams(lp); }
        b.addView(rows); gap(b,14); b.addView(text("Where are you training?",18,navy)); gap(b,6);
        LinearLayout places=new LinearLayout(this); places.setOrientation(LinearLayout.VERTICAL);
        for(String place:Coach.ENVIRONMENTS){Button pick=button(place);pick.setTextColor(navy);pick.setBackgroundColor(place.equals(environment)?mint:Color.WHITE);pick.setOnClickListener(v->{environment=place;showWizard();});places.addView(pick);LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)pick.getLayoutParams();lp.height=dp(44);lp.bottomMargin=dp(7);pick.setLayoutParams(lp);} b.addView(places); gap(b,12);
        Button next=button("Create my " + target + " plan"); next.setOnClickListener(v->{session=Coach.recommendation(target,goal,environment); showPlan();}); b.addView(next);
    }

    private void showPlan() {
        LinearLayout b=page(); header(b,"YOUR PLAN",""+target+" session",""+goal+" • "+environment+" • " + session.size() + " exercises • about " + session.size()*8 + " minutes");
        b.addView(text(Coach.goalSummary(goal),16,Color.rgb(71,83,108))); gap(b,14);
        for (int i=0;i<session.size();i++) { Coach.Exercise e=session.get(i); TextView card=text((i+1)+"  "+e.name+"\n"+e.sets+" sets × "+e.reps+" reps  •  "+e.restSeconds+" sec rest\n"+e.cue,16,navy); card.setPadding(dp(15),dp(13),dp(15),dp(13)); card.setBackgroundColor(Color.WHITE); b.addView(card); gap(b,8); }
        gap(b,10); Button go=button("Start guided workout"); go.setOnClickListener(v->showActive(0,1)); b.addView(go); gap(b,10); Button back=button("Adjust choices"); back.setBackgroundColor(navy); back.setOnClickListener(v->showWizard()); b.addView(back);
    }

    private void showActive(int index, int set) {
        if (index >= session.size()) { showComplete(); return; }
        Coach.Exercise e=session.get(index); LinearLayout b=page(); header(b,"GUIDED WORKOUT",""+e.name,"Exercise "+(index+1)+" of "+session.size()+"  •  Set "+set+" of "+e.sets);
        ExerciseAnimationView demo=new ExerciseAnimationView(this); b.addView(demo,new LinearLayout.LayoutParams(-1,dp(240))); gap(b,12);
        b.addView(text(e.sets+" sets × "+e.reps+" reps",24,navy)); gap(b,5); b.addView(text(e.description+"\nCoach cue: "+e.cue,16,Color.rgb(71,83,108))); gap(b,14);
        Button callout=button("Call out rep guidance"); callout.setBackgroundColor(navy); callout.setOnClickListener(v->say("Set "+set+". Complete "+e.reps+" controlled reps. "+e.cue)); b.addView(callout); gap(b,9);
        Button done=button("I finished this set"); done.setOnClickListener(v->startRest(index,set,e)); b.addView(done); gap(b,9);
        Button leave=button("End workout"); leave.setBackgroundColor(Color.rgb(91,102,126)); leave.setOnClickListener(v->showHome()); b.addView(leave);
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
        LinearLayout b=page(); header(b,"EXERCISE LIBRARY","Every move, explained.","Choose movements that match your equipment and ability. The animated demonstration is shown during guided sessions.");
        ImageView art=new ImageView(this); art.setImageResource(com.badimps.personaltrainer.R.drawable.workout_library); art.setScaleType(ImageView.ScaleType.CENTER_CROP); b.addView(art,new LinearLayout.LayoutParams(-1,dp(210))); gap(b,16);
        for (String group:Coach.TARGETS) { b.addView(text(group,21,navy)); gap(b,5); for(Coach.Exercise e:Coach.library().get(group)) { b.addView(text(e.name+" — "+e.description+"\n"+e.cue,15,Color.rgb(71,83,108))); gap(b,10); } gap(b,9); }
        Button home=button("Back to home"); home.setOnClickListener(v->showHome()); b.addView(home);
    }
    private void showMeals() {
        LinearLayout b=page(); header(b,"FUEL IDEAS","Simple meals for "+goal,"Food recommendations are flexible ideas — adjust portions and ingredients to your preferences, budget, allergies and dietary needs.");
        for(String meal:Coach.meals(goal)) { TextView item=text("•  "+meal,17,navy); item.setPadding(dp(15),dp(14),dp(15),dp(14)); item.setBackgroundColor(Color.WHITE); b.addView(item); gap(b,8); }
        gap(b,8); b.addView(text("General fuel suggestion",19,navy)); gap(b,5); b.addView(text("Aim for protein and colourful plants across the day, carbohydrates around training if useful, and regular hydration. For medical nutrition or weight-change targets, consult a registered dietitian.",15,Color.rgb(71,83,108))); gap(b,20);
        Button home=button("Back to home"); home.setOnClickListener(v->showHome()); b.addView(home);
    }
    private void showComplete() { LinearLayout b=page(); header(b,"SESSION COMPLETE","Strong work.","You completed your "+target+" plan. Save the feeling, hydrate, and let your next session start with a fresh choice."); Button home=button("Return home"); home.setOnClickListener(v->showHome()); b.addView(home); }
}
