package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

/** Crash-safe snapshot for a standalone watch endurance session. */
public final class WearHealthSessionStore {
    private static final String FILE="wear_health_session";
    public static final class State {
        public final boolean active,paused; public final String mode,route;
        public final long startedAt,activeMillis; public final double distance,calories,speed,elevation,heartRate;
        State(SharedPreferences p){active=p.getBoolean("active",false);paused=p.getBoolean("paused",false);mode=p.getString("mode","walking");route=p.getString("route","");startedAt=p.getLong("started",0);activeMillis=p.getLong("duration",0);distance=p.getFloat("distance",0);calories=p.getFloat("calories",0);speed=p.getFloat("speed",0);elevation=p.getFloat("elevation",0);heartRate=p.getFloat("heart",0);}
    }
    static SharedPreferences prefs(Context c){return c.getSharedPreferences(FILE,Context.MODE_PRIVATE);}
    public static State load(Context c){return new State(prefs(c));}
    public static void begin(Context c,String mode){prefs(c).edit().clear().putBoolean("active",true).putString("mode",mode).putLong("started",System.currentTimeMillis()).apply();}
    public static void update(Context c,boolean paused,long duration,double distance,double calories,double speed,double elevation,double heart,String route){prefs(c).edit().putBoolean("active",true).putBoolean("paused",paused).putLong("duration",duration).putFloat("distance",(float)distance).putFloat("calories",(float)calories).putFloat("speed",(float)speed).putFloat("elevation",(float)elevation).putFloat("heart",(float)heart).putString("route",route).apply();}
    public static void finish(Context c){prefs(c).edit().putBoolean("active",false).putBoolean("paused",false).apply();}
    private WearHealthSessionStore(){}
}
