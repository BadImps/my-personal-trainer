package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** Holds a device-local profile only; it deliberately makes no network account claim. */
public final class AccountStore {
    private static final String FILE="trainer_profile";
    public static final class Profile {
        public final String email, username, trainer, coachName, dialogueMode;
        public final boolean keepPhoneAwake;
        Profile(String e, String u, String t, String coach, String dialogue, boolean awake) {
            email=e; username=u; trainer=t; coachName=coach; dialogueMode=dialogue; keepPhoneAwake=awake;
        }
    }
    public static boolean exists(Context c) { return c.getSharedPreferences(FILE,Context.MODE_PRIVATE).getBoolean("created",false); }
    public static Profile get(Context c) {
        SharedPreferences p=c.getSharedPreferences(FILE,Context.MODE_PRIVATE);
        String gender=p.getString("trainer","Female");
        String coach=p.getString("coach_name",CoachPreferences.defaultCoach(gender));
        CoachPreferences.CoachOption option=CoachPreferences.coach(coach);
        return new Profile(p.getString("email",""),p.getString("username","Athlete"),option.gender,option.name,
                CoachPreferences.normalizeDialogueMode(p.getString("dialogue_mode",CoachPreferences.FULL_DIALOGUE)),
                p.getBoolean("keep_phone_awake",false));
    }
    public static void create(Context c,String email,String username,String password,String trainer) {
        byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
        c.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit().putBoolean("created",true).putString("email",email.trim()).putString("username",username.trim()).putString("trainer",trainer).putString("coach_name",CoachPreferences.defaultCoach(trainer)).putString("dialogue_mode",CoachPreferences.FULL_DIALOGUE).putBoolean("keep_phone_awake",false).putString("salt",hex(salt)).putString("passwordHash",hash(salt,password)).apply();
    }
    public static void updateCoachPreferences(Context c,String coachName,String dialogueMode,boolean keepPhoneAwake) {
        CoachPreferences.CoachOption coach=CoachPreferences.coach(coachName);
        c.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit()
                .putString("trainer",coach.gender)
                .putString("coach_name",coach.name)
                .putString("dialogue_mode",CoachPreferences.normalizeDialogueMode(dialogueMode))
                .putBoolean("keep_phone_awake",keepPhoneAwake)
                .apply();
    }
    public static boolean authenticate(Context c,String email,String password) {
        SharedPreferences p=c.getSharedPreferences(FILE,Context.MODE_PRIVATE);
        if (!p.getBoolean("created",false) || !p.getString("email","").equalsIgnoreCase(email.trim())) return false;
        try { return hash(unhex(p.getString("salt","")),password).equals(p.getString("passwordHash","")); }
        catch(Exception ignored) { return false; }
    }
    public static void clear(Context c) { c.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit().clear().apply(); }
    private static String hash(byte[] salt,String value) { try { MessageDigest d=MessageDigest.getInstance("SHA-256"); d.update(salt); return hex(d.digest(value.getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
    private static String hex(byte[] bytes) { StringBuilder b=new StringBuilder(); for(byte x:bytes)b.append(String.format("%02x",x)); return b.toString(); }
    private static byte[] unhex(String value) { byte[] out=new byte[value.length()/2];for(int i=0;i<out.length;i++)out[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);return out; }
    private AccountStore() {}
}
