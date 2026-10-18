package com.badimps.personaltrainer;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Holds a device-local profile only; it deliberately makes no network account claim. */
public final class AccountStore {
    private static final String FILE="trainer_profile";
    private static final String PASSWORD_SCHEME = "pbkdf2-sha256-v1";
    private static final int PASSWORD_ITERATIONS = 120_000;
    private static final int PASSWORD_KEY_BITS = 256;
    public static final class Profile {
        public final String email, username, trainer, coachName, dialogueMode, photoUri;
        public final boolean keepPhoneAwake, rememberSignIn, biometricUnlock;
        Profile(String e, String u, String t, String coach, String dialogue, boolean awake, String photo, boolean remember, boolean biometric) {
            email=e; username=u; trainer=t; coachName=coach; dialogueMode=dialogue; keepPhoneAwake=awake;
            photoUri=photo; rememberSignIn=remember; biometricUnlock=biometric;
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
                p.getBoolean("keep_phone_awake",false), p.getString("photo_uri", ""),
                p.getBoolean("remember_sign_in", false), p.getBoolean("biometric_unlock", false));
    }
    public static void create(Context c,String email,String username,String password,String trainer) {
        byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
        c.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit().putBoolean("created",true).putString("email",email.trim()).putString("username",username.trim()).putString("trainer",trainer).putString("coach_name",CoachPreferences.defaultCoach(trainer)).putString("dialogue_mode",CoachPreferences.FULL_DIALOGUE).putBoolean("keep_phone_awake",false).putBoolean("remember_sign_in",false).putBoolean("biometric_unlock",false).putString("photo_uri", "").putString("salt",hex(salt)).putString("password_scheme", PASSWORD_SCHEME).putString("passwordHash",passwordHash(salt,password)).apply();
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
        try {
            byte[] salt = unhex(p.getString("salt", ""));
            String expected = p.getString("passwordHash", "");
            boolean currentScheme = PASSWORD_SCHEME.equals(p.getString("password_scheme", ""));
            String actual = currentScheme ? passwordHash(salt, password) : legacyHash(salt, password);
            boolean authenticated = constantTimeEquals(actual, expected);
            // Existing local accounts are upgraded only after their old password was verified.
            if (authenticated && !currentScheme) {
                p.edit().putString("password_scheme", PASSWORD_SCHEME)
                        .putString("passwordHash", passwordHash(salt, password)).apply();
            }
            return authenticated;
        }
        catch(Exception ignored) { return false; }
    }
    public static void updatePersonalProfile(Context c, String username, String photoUri, boolean remember, boolean biometric, String newPassword) {
        if (username == null || username.trim().length() < 3) throw new IllegalArgumentException("Choose a name with at least 3 characters.");
        SharedPreferences p=c.getSharedPreferences(FILE,Context.MODE_PRIVATE);
        SharedPreferences.Editor edit=p.edit().putString("username", username.trim()).putString("photo_uri", photoUri == null ? "" : photoUri).putBoolean("remember_sign_in",remember).putBoolean("biometric_unlock",biometric);
        if (newPassword != null && !newPassword.isEmpty()) {
            if (newPassword.length() < 8) throw new IllegalArgumentException("Use a password with at least 8 characters.");
            byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
            edit.putString("salt",hex(salt)).putString("password_scheme", PASSWORD_SCHEME).putString("passwordHash",passwordHash(salt,newPassword));
        }
        edit.apply();
    }
    public static boolean resetPassword(Context c, String email, String password) {
        if (!exists(c) || !get(c).email.equalsIgnoreCase(email == null ? "" : email.trim()) || password == null || password.length() < 8) return false;
        byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
        c.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit().putString("salt",hex(salt)).putString("password_scheme", PASSWORD_SCHEME).putString("passwordHash",passwordHash(salt,password)).putBoolean("remember_sign_in",false).apply();
        return true;
    }
    /** Changes a local password only after the existing password has been verified. */
    public static boolean changePassword(Context c, String currentPassword, String newPassword) {
        Profile profile = get(c);
        if (!authenticate(c, profile.email, currentPassword) || newPassword == null || newPassword.length() < 8) return false;
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        c.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit()
                .putString("salt", hex(salt)).putString("password_scheme", PASSWORD_SCHEME)
                .putString("passwordHash", passwordHash(salt, newPassword))
                .apply();
        return true;
    }
    public static void clear(Context c) { c.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit().clear().apply(); }
    /** Kept only to verify and migrate profiles created before v5.17. */
    private static String legacyHash(byte[] salt,String value) { try { MessageDigest d=MessageDigest.getInstance("SHA-256"); d.update(salt); return hex(d.digest(value.getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
    private static String passwordHash(byte[] salt, String value) {
        char[] password = value == null ? new char[0] : value.toCharArray();
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, PASSWORD_ITERATIONS, PASSWORD_KEY_BITS);
            try { return hex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded()); }
            finally { spec.clearPassword(); }
        } catch (Exception e) { throw new IllegalStateException(e); }
        finally { java.util.Arrays.fill(password, '\0'); }
    }
    private static boolean constantTimeEquals(String one, String two) {
        return MessageDigest.isEqual(one.getBytes(StandardCharsets.US_ASCII), two.getBytes(StandardCharsets.US_ASCII));
    }
    private static String hex(byte[] bytes) { StringBuilder b=new StringBuilder(); for(byte x:bytes)b.append(String.format("%02x",x)); return b.toString(); }
    private static byte[] unhex(String value) { byte[] out=new byte[value.length()/2];for(int i=0;i<out.length;i++)out[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);return out; }
    private AccountStore() {}
}
