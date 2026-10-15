package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** Holds a device-local profile only; it deliberately makes no network account claim. */
public final class AccountStore {
    private static final String FILE="trainer_profile";
    public static final class Profile { public final String email, username, trainer; Profile(String e,String u,String t){email=e;username=u;trainer=t;} }
    public static boolean exists(Context c) { return c.getSharedPreferences(FILE,Context.MODE_PRIVATE).getBoolean("created",false); }
    public static Profile get(Context c) { SharedPreferences p=c.getSharedPreferences(FILE,Context.MODE_PRIVATE); return new Profile(p.getString("email",""),p.getString("username","Athlete"),p.getString("trainer","Female")); }
    public static void create(Context c,String email,String username,String password,String trainer) {
        byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
        c.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit().putBoolean("created",true).putString("email",email.trim()).putString("username",username.trim()).putString("trainer",trainer).putString("salt",hex(salt)).putString("passwordHash",hash(salt,password)).apply();
    }
    private static String hash(byte[] salt,String value) { try { MessageDigest d=MessageDigest.getInstance("SHA-256"); d.update(salt); return hex(d.digest(value.getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
    private static String hex(byte[] bytes) { StringBuilder b=new StringBuilder(); for(byte x:bytes)b.append(String.format("%02x",x)); return b.toString(); }
    private AccountStore() {}
}
