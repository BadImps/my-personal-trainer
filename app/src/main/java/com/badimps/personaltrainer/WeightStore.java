package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;

/** A local trend log. It compares direction against a personal target and never diagnoses health. */
public final class WeightStore {
    private static final String FILE="weight_trend";
    public static boolean configured(Context c){return p(c).contains("start");}
    public static void start(Context c,float current,float target,String unit){p(c).edit().putFloat("start",current).putFloat("latest",current).putFloat("target",target).putString("unit",unit).putLong("started",System.currentTimeMillis()).apply();}
    public static void updateProfile(Context c,float current,float target,String unit){
        SharedPreferences preferences=p(c);
        if(!configured(c)||!unit.equals(preferences.getString("unit","kg"))){start(c,current,target,unit);return;}
        preferences.edit().putFloat("latest",current).putFloat("target",target).apply();
    }
    public static void checkIn(Context c,float current){p(c).edit().putFloat("latest",current).apply();}
    public static float latest(Context c){return p(c).getFloat("latest",0);}
    public static float target(Context c){return p(c).getFloat("target",0);}
    public static String unit(Context c){return p(c).getString("unit","kg");}
    public static String latestLabel(Context c){return configured(c)?String.format(java.util.Locale.US,"%.1f %s",latest(c),unit(c)):"Not set";}
    public static void clear(Context c){p(c).edit().clear().apply();}
    public static String summary(Context c){SharedPreferences p=p(c); float start=p.getFloat("start",0),latest=p.getFloat("latest",0),target=p.getFloat("target",0); String unit=p.getString("unit","kg"); long days=Math.max(0,(System.currentTimeMillis()-p.getLong("started",System.currentTimeMillis()))/86400000L); float change=latest-start; boolean toward=target<start?change<0:target>start?change>0:true; String direction=toward?"moving toward":"not yet moving toward"; String delta=String.format(java.util.Locale.US,"%.1f",Math.abs(change)); if(days<7)return "Baseline saved. Check in again after about a week to see a useful trend."; if(Math.abs(change)<.05f)return "Your weight is steady so far. Progress can also show up in strength, energy and consistency."; return "Over "+days+" days, your logged weight changed "+delta+" "+unit+" and is "+direction+" your chosen target. Treat this as one signal, not a verdict.";}
    private static SharedPreferences p(Context c){return c.getSharedPreferences(FILE,Context.MODE_PRIVATE);}
    private WeightStore(){}
}
