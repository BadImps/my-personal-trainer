package com.badimps.personaltrainer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.*;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.wearable.*;

import java.nio.charset.StandardCharsets;

/** Round-safe Coach Hero controller for the phone-owned guided workout session. */
public final class WearWorkoutActivity extends Activity implements DataClient.OnDataChangedListener {
    private static final String STATE_PATH = "/workout/state", COMMAND_PATH = "/workout/command";
    private WorkoutView view;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable connectionPoll = new Runnable() {
        @Override public void run() {
            Wearable.getNodeClient(WearWorkoutActivity.this).getConnectedNodes()
                    .addOnSuccessListener(nodes -> view.setConnected(!nodes.isEmpty()));
            handler.postDelayed(this, 5_000L);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); view = new WorkoutView(); setContentView(view);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7402);
        String heartPermission = Build.VERSION.SDK_INT >= 36
                ? "android.permission.health.READ_HEART_RATE" : Manifest.permission.BODY_SENSORS;
        java.util.ArrayList<String> healthPermissions = new java.util.ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
            healthPermissions.add(Manifest.permission.ACTIVITY_RECOGNITION);
        if (checkSelfPermission(heartPermission) != PackageManager.PERMISSION_GRANTED)
            healthPermissions.add(heartPermission);
        if (!healthPermissions.isEmpty()) requestPermissions(healthPermissions.toArray(new String[0]), 7403);
    }
    @Override protected void onResume() {
        super.onResume(); Wearable.getDataClient(this).addListener(this);
        Wearable.getDataClient(this).getDataItems().addOnSuccessListener(items -> {
            for (DataItem item : items) if (STATE_PATH.equals(item.getUri().getPath())) update(DataMapItem.fromDataItem(item));
            items.release();
        });
        handler.post(connectionPoll);
        startGuidedHealthAfterPermission();
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==7403)startGuidedHealthAfterPermission();
    }
    private void startGuidedHealthAfterPermission(){
        SharedPreferences snapshot=getSharedPreferences("watch_workout_snapshot",MODE_PRIVATE);
        // The phone owns the warm-up/countdown.  Start watch measurement only after the phone
        // has entered a real workout phase, so the watch cannot create an independent session.
        String phase=snapshot.getString("phase", "");
        boolean collectingPhase="active".equals(phase)||"rest".equals(phase)
                ||"cooldown".equals(phase)||"finish_cooldown".equals(phase);
        if(!snapshot.getBoolean("active",false)||!collectingPhase)return;
        String heartPermission=Build.VERSION.SDK_INT>=36?"android.permission.health.READ_HEART_RATE":Manifest.permission.BODY_SENSORS;
        if(checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED
                ||checkSelfPermission(heartPermission)!=PackageManager.PERMISSION_GRANTED)return;
        SharedPreferences bridge=getSharedPreferences("watch_health_bridge",MODE_PRIVATE);
        String snapshotSession=snapshot.getString("session_id","");
        if(bridge.getBoolean("guided",false)&&snapshotSession.equals(bridge.getString("guided_session", ""))
                &&WearHealthSessionStore.load(this).active)return;
        bridge.edit().putBoolean("guided",true).putString("guided_session",snapshotSession)
                .putBoolean("paused",snapshot.getBoolean("paused",false)).apply();
        Intent health=new Intent(this,WearHealthExerciseService.class).setAction(WearHealthExerciseService.START)
                .putExtra("mode","guided").putExtra("session_id",snapshotSession)
                .putExtra("started",System.currentTimeMillis());
        if(Build.VERSION.SDK_INT>=26)startForegroundService(health);else startService(health);
    }
    @Override protected void onPause() {
        handler.removeCallbacks(connectionPoll); Wearable.getDataClient(this).removeListener(this); super.onPause();
    }
    @Override public void onDataChanged(DataEventBuffer events) {
        for (DataEvent event : events) if (event.getType() == DataEvent.TYPE_CHANGED
                && STATE_PATH.equals(event.getDataItem().getUri().getPath())) update(DataMapItem.fromDataItem(event.getDataItem()));
    }
    private void update(DataMapItem item) { runOnUiThread(() -> view.bind(
            item.getDataMap().getBoolean("active", false), item.getDataMap().getBoolean("paused", false),
            item.getDataMap().getString("exercise", "Workout"), item.getDataMap().getInt("set", 1),
            item.getDataMap().getInt("sets", 1), item.getDataMap().getInt("exercise_index", 0),
            item.getDataMap().getInt("exercise_count", 1), item.getDataMap().getString("phase", "active"),
            item.getDataMap().getInt("recovery_remaining", 0), item.getDataMap().getInt("recovery_total", 0),
            item.getDataMap().getString("upcoming_exercise", ""), item.getDataMap().getString("session_id", ""),
            item.getDataMap().getLong("revision", 0L), item.getDataMap().getLong("timer_elapsed", 0L),
            item.getDataMap().getLong("timer_snapshot_epoch", 0L), item.getDataMap().getInt("countdown_value", 0),
            item.getDataMap().getLong("recovery_deadline_epoch", 0L))); }
    private void send(String sessionId,String command) { if(sessionId==null||sessionId.isEmpty())return;Wearable.getNodeClient(this).getConnectedNodes().addOnSuccessListener(nodes -> {
        view.setConnected(!nodes.isEmpty());
        for (com.google.android.gms.wearable.Node node : nodes) Wearable.getMessageClient(this)
                .sendMessage(node.getId(), COMMAND_PATH, (sessionId+"|"+command+"|"+java.util.UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
    }); }

    private final class WorkoutView extends View {
        private static final int NAVY=0xff07182a, NAVY2=0xff0d2b42, INK=0xfff7fbff,
                MUTED=0xffa8bac8, MINT=0xff66e7cc, CORAL=0xffff725f;
        private static final long HOLD_MILLIS = 650L;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF previous=new RectF(), toggle=new RectF(), end=new RectF(), close=new RectF(), skip=new RectF();
        private final Handler holdHandler = new Handler(Looper.getMainLooper());
        private final SharedPreferences cache = getSharedPreferences("watch_workout_snapshot", Context.MODE_PRIVATE);
        private boolean active, paused, connected, controlsOpen, holdTriggered, touchDown;
        private String exercise="Workout", phase="active", upcoming="";
        private String sessionId=""; private long revision;
        private long timerElapsedMillis, timerSnapshotEpochMillis;
        private long recoveryDeadlineEpochMillis;
        private int countdownValue;
        private int set=1, sets=1, index, count=1, recoveryRemaining, recoveryTotal;
        private long activeScreenStarted = SystemClock.elapsedRealtime();
        private final Runnable openControls = () -> {
            if (!touchDown || !active || isRecovery()) return;
            holdTriggered=true; controlsOpen=true; vibrate(new long[]{0,28,45,42});
            announceForAccessibility("Workout controls opened"); invalidate();
        };

        WorkoutView() { super(WearWorkoutActivity.this); setLayerType(LAYER_TYPE_SOFTWARE,null); setFocusable(true); restore(); }
        void setConnected(boolean value) { if(connected!=value){connected=value;invalidate();} }
        void bind(boolean a,boolean z,String e,int s,int ss,int i,int c,String ph,int remaining,int total,String next,String sid,long rev,long elapsed,long snapshotEpoch,int countdown,long recoveryDeadline) {
            // Data Layer updates may arrive late after a reconnect.  A revision is generated on
            // every phone state mutation; equal or older snapshots for this session are duplicates.
            if(sid.equals(sessionId) && rev<=revision)return;
            active=a; paused=z; exercise=e; set=Math.max(1,s); sets=Math.max(1,ss); index=Math.max(0,i); count=Math.max(1,c);
            phase=ph==null?"active":ph; recoveryRemaining=Math.max(0,remaining); recoveryTotal=Math.max(0,total);
            upcoming=next==null?"":next; sessionId=sid==null?"":sid;revision=rev; activeScreenStarted=SystemClock.elapsedRealtime(); controlsOpen=false;
            timerElapsedMillis=Math.max(0L,elapsed);timerSnapshotEpochMillis=Math.max(0L,snapshotEpoch);countdownValue=Math.max(0,Math.min(5,countdown));
            recoveryDeadlineEpochMillis=Math.max(0L,recoveryDeadline);
            cache.edit().putBoolean("active",active).putBoolean("paused",paused).putString("exercise",exercise)
                    .putInt("set",set).putInt("sets",sets).putInt("index",index).putInt("count",count)
                    .putString("phase",phase).putInt("remaining",recoveryRemaining).putInt("total",recoveryTotal)
                    .putString("upcoming",upcoming).putString("session_id",sessionId).putLong("revision",revision)
                    .putLong("timer_elapsed",timerElapsedMillis).putLong("timer_snapshot_epoch",timerSnapshotEpochMillis)
                    .putInt("countdown_value",countdownValue).putLong("recovery_deadline_epoch",recoveryDeadlineEpochMillis).apply();
            setContentDescription(a?e+", set "+set+" of "+sets+", exercise "+(index+1)+" of "+count:
                    "No workout active. Start a workout on your phone."); invalidate();
        }
        private void restore(){active=cache.getBoolean("active",false);paused=cache.getBoolean("paused",false);exercise=cache.getString("exercise","Workout");set=cache.getInt("set",1);sets=cache.getInt("sets",1);index=cache.getInt("index",0);count=cache.getInt("count",1);phase=cache.getString("phase","active");recoveryRemaining=cache.getInt("remaining",0);recoveryTotal=cache.getInt("total",0);upcoming=cache.getString("upcoming","");sessionId=cache.getString("session_id","");revision=cache.getLong("revision",0L);timerElapsedMillis=cache.getLong("timer_elapsed",0L);timerSnapshotEpochMillis=cache.getLong("timer_snapshot_epoch",0L);countdownValue=cache.getInt("countdown_value",0);recoveryDeadlineEpochMillis=cache.getLong("recovery_deadline_epoch",0L);}

        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),u=Math.min(w,h)/100f;p.clearShadowLayer();p.setShader(new RadialGradient(w*.5f,h*.42f,w*.72f,NAVY2,NAVY,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);if(active){drawRim(c,w,h,u);if("countdown".equals(phase))drawCountdown(c,w,h,u);else if(controlsOpen)drawControls(c,w,h,u);else if(isRecovery())drawRecovery(c,w,h,u);else drawWorkout(c,w,h,u);}else drawWaiting(c,w,h,u);}
        private boolean isRecovery(){return"rest".equals(phase)||"cooldown".equals(phase)||"finish_cooldown".equals(phase);}
        private void drawRim(Canvas c,float w,float h,float u){int visibleRemaining=visibleRecoveryRemaining();float fraction=isRecovery()&&recoveryTotal>0?1f-Math.min(1f,visibleRemaining/(float)recoveryTotal):Math.min(1f,(index+(set/(float)Math.max(1,sets)))/Math.max(1,count));RectF rim=new RectF(3.2f*u,3.2f*u,w-3.2f*u,h-3.2f*u);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f*u);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(0xff173c51);c.drawArc(rim,-90,360,false,p);p.setColor(CORAL);p.setShadowLayer(2.2f*u,0,0,0x66ff725f);c.drawArc(rim,-90,360*fraction,false,p);p.clearShadowLayer();p.setStyle(Paint.Style.FILL);}
        private void drawWaiting(Canvas c,float w,float h,float u){text(c,"MY PERSONAL TRAINER",w/2,h*.27f,3.3f*u,MINT,true);dumbbell(c,w/2,h*.41f,7*u,MINT);text(c,"Ready when you are",w/2,h*.57f,6.1f*u,INK,true);text(c,"Start a guided workout",w/2,h*.66f,3.45f*u,MUTED,false);text(c,"on your phone",w/2,h*.72f,3.45f*u,MUTED,false);status(c,w,h,u);}
        private void drawCountdown(Canvas c,float w,float h,float u){text(c,"GET READY",w/2,h*.22f,3.6f*u,MINT,true);text(c,exercise,w/2,h*.34f,4.2f*u,MUTED,true);text(c,countdownValue>0?String.valueOf(countdownValue):"GO",w/2,h*.66f,20f*u,INK,true);text(c,"SET "+set+" OF "+sets,w/2,h*.78f,3.2f*u,MUTED,true);status(c,w,h,u);}
        private void drawWorkout(Canvas c,float w,float h,float u){text(c,paused?"WORKOUT PAUSED":"ACTIVE SET",w/2,h*.16f,3.3f*u,paused?CORAL:MINT,true);title(c,exercise,w/2,h*.285f,w*.74f,6.2f*u);pill(c,new RectF(w*.34f,h*.37f,w*.66f,h*.445f),0xff102f46,0x5566e7cc);text(c,"SET "+set+" OF "+sets,w/2,h*.423f,3.3f*u,INK,true);long runningDelta=paused||timerSnapshotEpochMillis<=0?0L:Math.max(0L,System.currentTimeMillis()-timerSnapshotEpochMillis);long seconds=Math.max(0L,timerElapsedMillis+runningDelta)/1000L;text(c,String.format(java.util.Locale.US,"%02d:%02d",seconds/60,seconds%60),w/2,h*.62f,10.8f*u,INK,true);text(c,(index+1)+" / "+count+" EXERCISES",w/2,h*.70f,3.1f*u,MUTED,true);check(c,w/2,h*.805f,4.2f*u,MINT);text(c,"TAP ANYWHERE",w/2,h*.875f,2.65f*u,MINT,true);text(c,"COMPLETE SET",w/2,h*.92f,2.65f*u,INK,true);status(c,w,h,u);if(!paused)postInvalidateDelayed(250L);}
        private void drawRecovery(Canvas c,float w,float h,float u){boolean cooldown="cooldown".equals(phase)||"finish_cooldown".equals(phase);text(c,cooldown?"COOLDOWN":"REST",w/2,h*.15f,3.4f*u,MINT,true);text(c,time(visibleRecoveryRemaining()),w/2,h*.40f,11.8f*u,INK,true);if(!upcoming.isEmpty()){text(c,"UP NEXT",w/2,h*.50f,2.7f*u,MINT,true);title(c,upcoming,w/2,h*.585f,w*.68f,4.4f*u);}float cy=h*.76f,radius=10.7f*u;toggle.set(w*.36f-radius,cy-radius,w*.36f+radius,cy+radius);skip.set(w*.64f-radius,cy-radius,w*.64f+radius,cy+radius);circle(c,toggle,paused?MINT:0xff102f46,u);if(paused)play(c,toggle.centerX(),toggle.centerY(),4*u,NAVY);else pause(c,toggle.centerX(),toggle.centerY(),3.8f*u,INK);circle(c,skip,0xff102f46,u);next(c,skip.centerX(),skip.centerY(),4.2f*u,INK);text(c,paused?"RESUME":"PAUSE",toggle.centerX(),h*.895f,2.4f*u,MUTED,true);text(c,cooldown?"SKIP COOL":"SKIP REST",skip.centerX(),h*.895f,2.4f*u,MUTED,true);status(c,w,h,u);if(!paused&&visibleRecoveryRemaining()>0)postInvalidateDelayed(1000L);}
        private void drawControls(Canvas c,float w,float h,float u){text(c,"WORKOUT CONTROLS",w/2,h*.18f,3.25f*u,MINT,true);float cy=h*.49f,radius=11.5f*u;previous.set(w*.23f-radius,cy-radius,w*.23f+radius,cy+radius);toggle.set(w*.50f-radius,cy-radius,w*.50f+radius,cy+radius);end.set(w*.77f-radius,cy-radius,w*.77f+radius,cy+radius);circle(c,previous,0xff102f46,u);previous(c,previous.centerX(),previous.centerY(),4*u,INK);circle(c,toggle,0xff0e4250,u);if(paused)play(c,toggle.centerX(),toggle.centerY(),4*u,INK);else pause(c,toggle.centerX(),toggle.centerY(),3.7f*u,INK);circle(c,end,0xff522a2e,u);stop(c,end.centerX(),end.centerY(),3.8f*u,CORAL);text(c,"PREVIOUS",previous.centerX(),h*.66f,2.25f*u,MUTED,true);text(c,paused?"RESUME":"PAUSE",toggle.centerX(),h*.66f,2.25f*u,MUTED,true);text(c,"END",end.centerX(),h*.66f,2.25f*u,MUTED,true);close.set(w*.42f,h*.75f,w*.58f,h*.91f);circle(c,close,0xff102f46,u);x(c,close.centerX(),close.centerY(),3.6f*u,INK);status(c,w,h,u);}
        private void status(Canvas c,float w,float h,float u){p.setColor(connected?MINT:CORAL);c.drawCircle(w*.5f,h*.965f,1.15f*u,p);}

        @Override public boolean onTouchEvent(MotionEvent event){if(!active||"countdown".equals(phase))return true;float x=event.getX(),y=event.getY();if(event.getAction()==MotionEvent.ACTION_DOWN){touchDown=true;holdTriggered=false;if(!controlsOpen&&!isRecovery())holdHandler.postDelayed(openControls,HOLD_MILLIS);invalidate();return true;}if(event.getAction()==MotionEvent.ACTION_CANCEL){resetTouch();return true;}if(event.getAction()==MotionEvent.ACTION_UP){holdHandler.removeCallbacks(openControls);touchDown=false;if(holdTriggered){holdTriggered=false;return true;}String command=hit(x,y);if(controlsOpen){if("close".equals(command)){controlsOpen=false;vibrate(new long[]{0,22});invalidate();return true;}if(command!=null)execute(command);}else if(isRecovery()){if(command!=null)execute(command);}else{execute("complete");}invalidate();performClick();return true;}return true;}
        private void execute(String command){if(command==null||sessionId.isEmpty())return;if(!connected){vibrate(new long[]{0,45,35,45});announceForAccessibility("Phone disconnected. Workout controls unavailable.");return;}vibrate("end".equals(command)?new long[]{0,35,35,70}:new long[]{0,28});send(sessionId,command);controlsOpen=false;}
        private void resetTouch(){holdHandler.removeCallbacks(openControls);touchDown=false;holdTriggered=false;invalidate();}
        @Override public boolean performClick(){super.performClick();return true;}
        private String hit(float x,float y){if(controlsOpen){if(previous.contains(x,y))return"previous";if(toggle.contains(x,y))return"toggle";if(end.contains(x,y))return"end";if(close.contains(x,y))return"close";return null;}if(isRecovery()){if(toggle.contains(x,y))return"toggle";if(skip.contains(x,y))return"skip";}return null;}

        private void vibrate(long[] pattern){Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null)v.vibrate(VibrationEffect.createWaveform(pattern,-1));}
        private int visibleRecoveryRemaining(){if(!isRecovery()||paused)return recoveryRemaining;if(recoveryDeadlineEpochMillis>0L)return(int)Math.max(0L,(recoveryDeadlineEpochMillis-System.currentTimeMillis()+999L)/1000L);long elapsed=Math.max(0L,(SystemClock.elapsedRealtime()-activeScreenStarted)/1000L);return Math.max(0,recoveryRemaining-(int)elapsed);}
        private String time(int seconds){return String.format(java.util.Locale.US,"%02d:%02d",seconds/60,seconds%60);}
        private void text(Canvas c,String v,float x,float y,float s,int color,boolean bold){p.clearShadowLayer();p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(color);p.setTextSize(s);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(v,x,y,p);}
        private void title(Canvas c,String value,float x,float y,float max,float size){p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(size);if(p.measureText(value)<=max){text(c,value,x,y,size,INK,true);return;}int split=value.lastIndexOf(' ',value.length()/2);if(split<1)split=value.indexOf(' ',value.length()/2);if(split<1){float f=size;while(f>size*.68f&&measure(value,f)>max)f--;text(c,value,x,y,f,INK,true);return;}text(c,value.substring(0,split),x,y-size*.10f,size*.88f,INK,true);text(c,value.substring(split+1),x,y+size*.82f,size*.88f,INK,true);}
        private float measure(String v,float s){p.setTextSize(s);return p.measureText(v);}
        private void pill(Canvas c,RectF r,int fill,int stroke){p.setStyle(Paint.Style.FILL);p.setColor(fill);p.setShadowLayer(6,0,3,0x66000000);c.drawRoundRect(r,r.height()/2,r.height()/2,p);p.clearShadowLayer();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(stroke);c.drawRoundRect(r,r.height()/2,r.height()/2,p);p.setStyle(Paint.Style.FILL);}
        private void circle(Canvas c,RectF r,int fill,float u){p.setColor(0x44000000);p.setShadowLayer(2.6f*u,0,1.2f*u,0x77000000);c.drawCircle(r.centerX(),r.centerY(),r.width()/2,p);p.clearShadowLayer();p.setColor(fill);c.drawCircle(r.centerX(),r.centerY(),r.width()/2-.7f*u,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.45f*u);p.setColor(fill==0xff522a2e?CORAL:MINT);c.drawCircle(r.centerX(),r.centerY(),r.width()/2-.7f*u,p);p.setStyle(Paint.Style.FILL);}
        private void previous(Canvas c,float x,float y,float s,int color){p.setColor(color);p.setStrokeWidth(s*.18f);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x-s,y-s,x-s,y+s,p);Path q=new Path();q.moveTo(x+s,y-s);q.lineTo(x-s*.55f,y);q.lineTo(x+s,y+s);q.close();c.drawPath(q,p);}
        private void pause(Canvas c,float x,float y,float s,int color){p.setColor(color);float b=s*.48f;c.drawRoundRect(x-s,y-s,x-s+b,y+s,b/2,b/2,p);c.drawRoundRect(x+s-b,y-s,x+s,y+s,b/2,b/2,p);}
        private void play(Canvas c,float x,float y,float s,int color){p.setColor(color);Path q=new Path();q.moveTo(x-s*.62f,y-s);q.lineTo(x+s,y);q.lineTo(x-s*.62f,y+s);q.close();c.drawPath(q,p);}
        private void next(Canvas c,float x,float y,float s,int color){p.setColor(color);Path q=new Path();q.moveTo(x-s,y-s);q.lineTo(x+s*.45f,y);q.lineTo(x-s,y+s);q.close();c.drawPath(q,p);c.drawRoundRect(x+s*.55f,y-s,x+s*.9f,y+s,s*.15f,s*.15f,p);}
        private void check(Canvas c,float x,float y,float s,int color){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(s*.42f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Path q=new Path();q.moveTo(x-s,y);q.lineTo(x-s*.25f,y+s*.72f);q.lineTo(x+s,y-s*.72f);c.drawPath(q,p);p.setStyle(Paint.Style.FILL);}
        private void stop(Canvas c,float x,float y,float s,int color){p.setColor(color);c.drawRoundRect(x-s,y-s,x+s,y+s,s*.2f,s*.2f,p);}
        private void x(Canvas c,float x,float y,float s,int color){p.setColor(color);p.setStrokeWidth(s*.45f);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x-s,y-s,x+s,y+s,p);c.drawLine(x+s,y-s,x-s,y+s,p);}
        private void dumbbell(Canvas c,float x,float y,float s,int color){p.setColor(color);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(s*.26f);c.drawLine(x-s*.55f,y,x+s*.55f,y,p);p.setStrokeWidth(s*.42f);c.drawLine(x-s*.68f,y-s*.38f,x-s*.68f,y+s*.38f,p);c.drawLine(x+s*.68f,y-s*.38f,x+s*.68f,y+s*.38f,p);p.setStrokeWidth(s*.24f);c.drawLine(x-s,y-s*.28f,x-s,y+s*.28f,p);c.drawLine(x+s,y-s*.28f,x+s,y+s*.28f,p);}
    }
}
