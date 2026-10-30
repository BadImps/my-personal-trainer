package com.badimps.personaltrainer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.*;
import android.view.*;

/** One-time, watch-local health consent and endurance-mode picker. */
@android.annotation.SuppressLint("WrongConstant")
public final class WearHealthConsentActivity extends Activity {
    private String pending="walking";
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(new ConsentView());}
    private void start(String mode){pending=mode;java.util.ArrayList<String> p=new java.util.ArrayList<>();if(checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.ACTIVITY_RECOGNITION);if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.ACCESS_FINE_LOCATION);String heart=Build.VERSION.SDK_INT>=36?"android.permission.health.READ_HEART_RATE":Manifest.permission.BODY_SENSORS;if(checkSelfPermission(heart)!=PackageManager.PERMISSION_GRANTED)p.add(heart);if(p.isEmpty())launch();else requestPermissions(p.toArray(new String[0]),8140);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==8140){for(int value:g)if(value!=PackageManager.PERMISSION_GRANTED)return;launch();}}
    private void launch(){Intent s=new Intent(this,WearHealthExerciseService.class).setAction(WearHealthExerciseService.START).putExtra("mode",pending);if(Build.VERSION.SDK_INT>=26)startForegroundService(s);else startService(s);startActivity(new Intent(this,WearHealthActivity.class));finish();}
    private final class ConsentView extends View {final Paint p=new Paint(1);ConsentView(){super(WearHealthConsentActivity.this);setContentDescription("Start standalone endurance. Health data stays on your devices.");}protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();c.drawColor(0xff07182a);t(c,"WATCH ENDURANCE",w/2,h*.17f,16,0xff66e7cc,true);t(c,"Heart rate, route and calories",w/2,h*.29f,13,Color.WHITE,false);t(c,"stay private on your devices",w/2,h*.36f,13,0xffa8bac8,false);button(c,w*.16f,h*.47f,w*.84f,h*.59f,"WALK");button(c,w*.16f,h*.63f,w*.84f,h*.75f,"RUN");button(c,w*.16f,h*.79f,w*.84f,h*.91f,"CYCLE");}public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()!=1)return true;float y=e.getY(),h=getHeight();if(y>h*.47&&y<h*.59)start("walking");else if(y>h*.63&&y<h*.75)start("running");else if(y>h*.79&&y<h*.91)start("cycling");return true;}void button(Canvas c,float l,float top,float r,float b,String s){p.setColor(0xff103b4b);p.setShadowLayer(8,0,3,0x88000000);c.drawRoundRect(l,top,r,b,22,22,p);p.clearShadowLayer();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xffff725f);c.drawRoundRect(l,top,r,b,22,22,p);p.setStyle(Paint.Style.FILL);t(c,s,(l+r)/2,(top+b)/2+5,15,Color.WHITE,true);}void t(Canvas c,String s,float x,float y,float z,int color,boolean bold){p.setColor(color);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(z);p.setTypeface(Typeface.create("sans",bold?1:0));c.drawText(s,x,y,p);}}
}
