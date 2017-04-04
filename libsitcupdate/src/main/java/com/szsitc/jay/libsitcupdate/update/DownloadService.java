package com.szsitc.jay.libsitcupdate.update;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import com.szsitc.jay.libsitcupdate.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends IntentService {
    // 10-10 19:14:32.618: D/DownloadService(1926): 测试缓存：41234 32kb
    // 10-10 19:16:10.892: D/DownloadService(2069): 测试缓存：41170 1kb
    // 10-10 19:18:21.352: D/DownloadService(2253): 测试缓存：39899 10kb
    private static final int BUFFER_SIZE = 10 * 1024; // 8k ~ 32K
    private static final String TAG = "DownloadService";

    private static final int NOTIFICATION_ID = 0;

    private NotificationManager mNotifyManager;
    private Builder mBuilder;

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new Builder(this);

        //fixed get appname bug.
        ApplicationInfo applicationInfo = getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        String appName = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : getString(stringId);

        int icon = getApplicationInfo().icon;

        mBuilder.setContentTitle(appName).setSmallIcon(icon);
        String urlStr = intent.getStringExtra(Constants.APK_DOWNLOAD_URL);
        boolean isAPK = intent.getBooleanExtra(Constants.DOWNLOAD_APKORFIRMWARE,false);
        int apkCode = intent.getIntExtra(Constants.APK_VERSION_CODE,0);

        InputStream in = null;
        FileOutputStream out = null;
        Log.d(Constants.TAG,urlStr);
        Log.d(Constants.TAG,String.format("isAPK %s",isAPK?"True":"False"));
        try {
            URL url = new URL(urlStr);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(false);
            urlConnection.setConnectTimeout(10 * 1000);
            urlConnection.setReadTimeout(10 * 1000);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Charset", "UTF-8");
            urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");

            urlConnection.connect();
            long bytetotal = urlConnection.getContentLength();
            long bytesum = 0;
            int byteread = 0;
            in = urlConnection.getInputStream();
            //TODO:we'd better to save to filediretory,change later
            File dir = StorageUtils.getCacheDirectory(this);
            String apkName = urlStr.substring(urlStr.lastIndexOf("/") + 1, urlStr.length());
            File apkFile = new File(dir, apkName);
            out = new FileOutputStream(apkFile);
            byte[] buffer = new byte[BUFFER_SIZE];

            int oldProgress = 0;

            while ((byteread = in.read(buffer)) != -1) {
                bytesum += byteread;
                out.write(buffer, 0, byteread);

                int progress = (int) (bytesum * 100L / bytetotal);
                // 如果进度与之前进度相等，则不更新，如果更新太频繁，否则会造成界面卡顿
                if (progress != oldProgress) {
                    updateProgress(progress);
                }
                oldProgress = progress;
            }
            // 下载完成
            if(isAPK)
                installAPk(apkFile);
            else{
                Log.d("Install",String.format("Firmware does not need install %d bytes",bytesum));
                //updatefirmwarefile(apkName,apkCode);
                installFirmware(apkName,apkCode);
            }
            //delete notifycate
            mNotifyManager.cancel(NOTIFICATION_ID);
        } catch (Exception e) {
            Log.e(TAG, "download apk file error");
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {

                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {

                }
            }
        }
    }

    private void updateProgress(int progress) {
        //"正在下载:" + progress + "%"
        mBuilder.setContentText(this.getString(R.string.android_auto_update_download_progress, progress)).setProgress(100, progress, false);
        //setContentInent如果不设置在4.0+上没有问题，在4.0以下会报异常
        PendingIntent pendingintent = PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setContentIntent(pendingintent);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    //TODO save version code to file version.maybe acitive do this better.
    private int updatefirmwareversion(int apkcode){
        Log.d("APKCODE",String.format("code=%d",apkcode));
        try{
            FileOutputStream fos= openFileOutput("version", Context.MODE_PRIVATE);
            fos.write(apkcode);
            fos.close();
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
        return 0;
    }
    private int updatefirmwarefile(String apkname,int apkcode){
        //TODO store to filediretory.
        //1.update version file.
        updatefirmwareversion(apkcode);
        return 0;
    }

    private void installAPk(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //如果没有设置SDCard写权限，或者没有sdcard,apk文件保存在内存中，需要授予权限才能安装
        try {
            String[] command = {"chmod", "777", apkFile.toString()};
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
        } catch (IOException ignored) {
            Log.d("install","Failed");
        }

        //intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            //判断是否是AndroidN以及更高的版本8
            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(this,getString(R.string.less_provider_file_authorities), apkFile);
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            } else
            */
            {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            startActivity(intent);
        }catch (Exception e)
        {
            Log.e("install","install error");
            e.printStackTrace();
        }
    }

    private void installFirmware(String fwname,int versioncode){
        sendContentBroadcast(fwname,versioncode);
    }

    /**
     * 发送广播
     * @param name
     */
    protected void sendContentBroadcast(String name,int versioncode) {
        // TODO Auto-generated method stub
        Intent intent=new Intent();
        intent.setAction("com.szsitc.jay.update");
        intent.putExtra("firmwarename", name);
        intent.putExtra("versioncode", versioncode);
        sendBroadcast(intent);
    }

}
