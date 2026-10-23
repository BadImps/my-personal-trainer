package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

/** A local trend log. It compares direction against a personal target and never diagnoses health. */
public final class WeightStore {
    private static final String FILE="weight_trend";
    private static final String HISTORY="history";
    private static final long DAY_MS=86_400_000L;
    public static final class Point {
        public final long recordedAt;
        public final float weight;
        Point(long recordedAt,float weight){this.recordedAt=recordedAt;this.weight=weight;}
    }
    public static boolean configured(Context c){return p(c).contains("start");}
    public static void start(Context c,float current,float target,String unit){
        long now=System.currentTimeMillis();
        p(c).edit().putFloat("start",current).putFloat("latest",current).putFloat("target",target).putString("unit",unit).putLong("started",now).putString(HISTORY,now+":"+current).apply();
    }
    public static void updateProfile(Context c,float current,float target,String unit){
        SharedPreferences preferences=p(c);
        if(!configured(c)||!unit.equals(preferences.getString("unit","kg"))){start(c,current,target,unit);return;}
        float previous=preferences.getFloat("latest",0f);
        preferences.edit().putFloat("latest",current).putFloat("target",target).apply();
        if(Math.abs(previous-current)>=.01f) append(c,current,System.currentTimeMillis());
    }
    public static void checkIn(Context c,float current){p(c).edit().putFloat("latest",current).apply();append(c,current,System.currentTimeMillis());}
    public static float latest(Context c){return p(c).getFloat("latest",0);}
    public static float starting(Context c){return p(c).getFloat("start",0);}
    public static float target(Context c){return p(c).getFloat("target",0);}
    public static String unit(Context c){return p(c).getString("unit","kg");}
    public static String latestLabel(Context c){return configured(c)?String.format(java.util.Locale.US,"%.1f %s",latest(c),unit(c)):"Not set";}
    /** Small local trend log: one daily point is enough for the chart and keeps storage bounded. */
    public static List<Point> history(Context c){
        ArrayList<Point> result=new ArrayList<>(); String raw=p(c).getString(HISTORY,"");
        if(raw!=null&&!raw.isEmpty()) for(String token:raw.split(",")){
            String[] bits=token.split(":"); if(bits.length!=2)continue;
            try{result.add(new Point(Long.parseLong(bits[0]),Float.parseFloat(bits[1])));}catch(NumberFormatException ignored){}
        }
        if(result.isEmpty()&&configured(c))result.add(new Point(p(c).getLong("started",System.currentTimeMillis()),starting(c)));
        return result;
    }
    private static void append(Context c,float value,long when){
        List<Point> points=history(c); if(!points.isEmpty()&&when-points.get(points.size()-1).recordedAt<DAY_MS){
            points.set(points.size()-1,new Point(when,value));
        }else points.add(new Point(when,value));
        while(points.size()>16)points.remove(0);
        StringBuilder raw=new StringBuilder(); for(Point point:points){if(raw.length()>0)raw.append(',');raw.append(point.recordedAt).append(':').append(point.weight);} p(c).edit().putString(HISTORY,raw.toString()).apply();
    }
    public static void clear(Context c){p(c).edit().clear().apply();}
    public static String summary(Context c){SharedPreferences p=p(c); float start=p.getFloat("start",0),latest=p.getFloat("latest",0),target=p.getFloat("target",0); String unit=p.getString("unit","kg"); long days=Math.max(0,(System.currentTimeMillis()-p.getLong("started",System.currentTimeMillis()))/86400000L); float change=latest-start; boolean toward=target<start?change<0:target>start?change>0:true; String direction=toward?"moving toward":"not yet moving toward"; String delta=String.format(java.util.Locale.US,"%.1f",Math.abs(change)); if(days<7)return "Baseline saved. Check in again after about a week to see a useful trend."; if(Math.abs(change)<.05f)return "Your weight is steady so far. Progress can also show up in strength, energy and consistency."; return "Over "+days+" days, your logged weight changed "+delta+" "+unit+" and is "+direction+" your chosen target. Treat this as one signal, not a verdict.";}
    private static SharedPreferences p(Context c){return c.getSharedPreferences(FILE,Context.MODE_PRIVATE);}
    private WeightStore(){}
}
