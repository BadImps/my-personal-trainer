package com.badimps.personaltrainer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/** HTTPS OTA manifest client. It only accepts a newer build with the published SHA-256. */
public final class UpdateManager {
    public static final String ENDPOINT="https://badimps.github.io/my-personal-trainer/latest.json";
    public static final class Info { public final int code; public final String name,url,sha; Info(int c,String n,String u,String s){code=c;name=n;url=u;sha=s;} }
    public static Info check() throws Exception {
        JSONObject j=new JSONObject(read(ENDPOINT)); int code=j.getInt("versionCode");
        return code>BuildConfig.VERSION_CODE ? new Info(code,j.getString("versionName"),j.getString("apkUrl"),j.getString("sha256")) : null;
    }
    public static void downloadAndInstall(Context context, Info info) throws Exception {
        File dir=new File(context.getCacheDir(),"updates"); if(!dir.exists()&&!dir.mkdirs()) throw new IllegalStateException("Cannot create update storage");
        File apk=new File(dir,"trainer-"+info.code+".apk"); HttpURLConnection c=(HttpURLConnection)new URL(info.url).openConnection(); c.setConnectTimeout(15000); c.setReadTimeout(30000); c.setInstanceFollowRedirects(true);
        try(InputStream in=c.getInputStream(); FileOutputStream out=new FileOutputStream(apk)) { byte[] b=new byte[8192]; for(int n;(n=in.read(b))!=-1;)out.write(b,0,n); }
        if(!sha(apk).equalsIgnoreCase(info.sha)) { apk.delete(); throw new SecurityException("Downloaded update did not match its published SHA-256"); }
        if(Build.VERSION.SDK_INT>=26 && !context.getPackageManager().canRequestPackageInstalls()) { context.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:"+context.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return; }
        Uri uri=FileProvider.getUriForFile(context,context.getPackageName()+".files",apk);
        context.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private static String read(String address) throws Exception { HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection(); c.setConnectTimeout(12000); c.setReadTimeout(12000); try(InputStream in=c.getInputStream(); java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){ byte[] b=new byte[4096]; for(int n;(n=in.read(b))!=-1;)out.write(b,0,n); return new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8); } }
    private static String sha(File f) throws Exception { MessageDigest d=MessageDigest.getInstance("SHA-256"); try(InputStream in=new java.io.FileInputStream(f)){byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte x:d.digest())s.append(String.format("%02x",x));return s.toString(); }
    private UpdateManager() {}
}
